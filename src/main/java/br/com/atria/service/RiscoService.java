package br.com.atria.service;

import br.com.atria.dto.response.AlertaResponse;
import br.com.atria.dto.response.ContagemResponse;
import br.com.atria.dto.response.OcorrenciasResponse;
import br.com.atria.dto.response.PontoOcorrenciaResponse;
import br.com.atria.dto.response.RiscoIndicadoresResponse;
import br.com.atria.dto.response.StatusRiscoResponse;
import br.com.atria.model.Alerta;
import br.com.atria.model.Plataforma;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.enums.NivelPrazo;
import br.com.atria.model.enums.Prioridade;
import br.com.atria.service.regra.CalculadoraPrazo;
import br.com.atria.service.regra.Plural;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Riscos e Prazos: métricas, status de exposição ANPD, riscos de marketplace e histórico de ocorrências. */
@Service
public class RiscoService {
    private static final int SEMANAS_DE_HISTORICO = 8;
    private static final int LIMITE_EVENTOS = 50;

    private final SolicitacaoService solicitacaoService;
    private final AlertaService alertaService;
    private final AcessoService acessoService;
    private final PlataformaService plataformaService;
    private final CalculadoraPrazo calculadoraPrazo;
    private final Clock clock;

    public RiscoService(SolicitacaoService solicitacaoService, AlertaService alertaService, AcessoService acessoService,
                        PlataformaService plataformaService, CalculadoraPrazo calculadoraPrazo, Clock clock) {
        this.solicitacaoService = solicitacaoService;
        this.alertaService = alertaService;
        this.acessoService = acessoService;
        this.plataformaService = plataformaService;
        this.calculadoraPrazo = calculadoraPrazo;
        this.clock = clock;
    }

    public RiscoIndicadoresResponse indicadores() {
        List<SolicitacaoLgpd> abertas = solicitacaoService.abertas();
        Map<NivelPrazo, Long> porNivel = abertas.stream().collect(Collectors.groupingBy(
                s -> calculadoraPrazo.classificarNivel(calculadoraPrazo.diasRestantes(s.getPrazo())), Collectors.counting()));
        List<Alerta> alertas = alertaService.doLojista();
        List<Alerta> abertos = alertas.stream().filter(Alerta::isAberto).toList();
        Map<Prioridade, Long> porPrioridade = abertos.stream().collect(Collectors.groupingBy(Alerta::getPrioridade, Collectors.counting()));
        List<ContagemResponse> alertasPorPrioridade = Arrays.stream(Prioridade.values())
                .map(p -> new ContagemResponse(p.name(), p.getDescricao(), porPrioridade.getOrDefault(p, 0L)))
                .toList();

        return new RiscoIndicadoresResponse(abertas.size(), porNivel.getOrDefault(NivelPrazo.VERMELHO, 0L),
                porNivel.getOrDefault(NivelPrazo.LARANJA, 0L), porNivel.getOrDefault(NivelPrazo.AMARELO, 0L),
                porNivel.getOrDefault(NivelPrazo.VERDE, 0L), abertos.size(), alertasPorPrioridade,
                alertas.size() - abertos.size(), statusAnpd(), statusMarketplace());
    }

    /** Exposição à ANPD: solicitações vencidas ou a vencer em até 2 dias e acessos indevidos ativos. */
    public StatusRiscoResponse statusAnpd() {
        long solicitacoesCriticas = solicitacaoService.abertas().stream()
                .filter(s -> calculadoraPrazo.diasRestantes(s.getPrazo()) <= 2)
                .count();
        long acessos = acessoService.acessosIndevidos().size();
        long total = solicitacoesCriticas + acessos;
        if (total == 0) {
            return new StatusRiscoResponse(true, 0, "Nenhuma pendência com risco de exposição à ANPD.");
        }
        return new StatusRiscoResponse(false, total,
                Plural.de(solicitacoesCriticas, "solicitação vencida ou a vencer em até 2 dias", "solicitações vencidas ou a vencer em até 2 dias")
                        + " e " + Plural.de(acessos, "acesso indevido ativo", "acessos indevidos ativos") + ".");
    }

    /** Riscos de marketplace: alertas abertos em marketplaces e na loja própria. */
    public StatusRiscoResponse statusMarketplace() {
        Map<Long, Plataforma> plataformas = plataformaService.mapaPorId();
        long pendencias = alertaService.abertos().stream()
                .filter(a -> a.getPlataformaId() != null && plataformas.get(a.getPlataformaId()).isComercial())
                .count();
        if (pendencias == 0) {
            return new StatusRiscoResponse(true, 0, "Nenhuma pendência crítica nos marketplaces.");
        }
        return new StatusRiscoResponse(false, pendencias,
                Plural.de(pendencias, "pendência aberta", "pendências abertas") + " em marketplaces e loja própria.");
    }

    public OcorrenciasResponse ocorrencias() {
        List<Alerta> alertas = alertaService.doLojista();
        LocalDate semanaAtual = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<PontoOcorrenciaResponse> serie = new ArrayList<>();
        for (int i = SEMANAS_DE_HISTORICO - 1; i >= 0; i--) {
            LocalDate inicio = semanaAtual.minusWeeks(i);
            serie.add(new PontoOcorrenciaResponse(inicio,
                    contarNaSemana(alertas, Alerta::getCriadoEm, inicio),
                    contarNaSemana(alertas, Alerta::getResolvidoEm, inicio)));
        }
        List<AlertaResponse> eventos = alertas.stream()
                .sorted(Comparator.comparing(Alerta::getCriadoEm).reversed())
                .limit(LIMITE_EVENTOS)
                .map(alertaService::paraResponse)
                .toList();
        return new OcorrenciasResponse(serie, eventos);
    }

    private long contarNaSemana(List<Alerta> alertas, Function<Alerta, LocalDateTime> data, LocalDate inicio) {
        LocalDate fim = inicio.plusWeeks(1);
        return alertas.stream()
                .map(data)
                .filter(d -> d != null && !d.toLocalDate().isBefore(inicio) && d.toLocalDate().isBefore(fim))
                .count();
    }
}
