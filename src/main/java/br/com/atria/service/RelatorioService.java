package br.com.atria.service;

import br.com.atria.dto.response.RelatorioIndicadoresResponse;
import br.com.atria.model.Alerta;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.model.enums.Prioridade;
import br.com.atria.relatorio.ArquivoPdf;
import br.com.atria.relatorio.DadosRelatorio;
import br.com.atria.relatorio.GeradorRelatorioPdf;
import br.com.atria.relatorio.TipoRelatorio;
import br.com.atria.service.regra.CalculadoraIndiceConformidade;
import br.com.atria.service.regra.CalculadoraPrazo;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Consolida os indicadores de conformidade dos últimos 12 meses e gera os relatórios em PDF. */
@Service
public class RelatorioService {
    private static final int MESES_DO_PERIODO = 12;
    private static final int ACOES_NO_RELATORIO = 25;

    private final SolicitacaoService solicitacaoService;
    private final AlertaService alertaService;
    private final AcessoService acessoService;
    private final ConexaoService conexaoService;
    private final TitularService titularService;
    private final MapeamentoService mapeamentoService;
    private final EvidenciaService evidenciaService;
    private final AuditoriaService auditoria;
    private final LojistaAtualService lojistaAtual;
    private final CalculadoraIndiceConformidade calculadoraIndice;
    private final CalculadoraPrazo calculadoraPrazo;
    private final GeradorRelatorioPdf geradorPdf;
    private final Clock clock;

    public RelatorioService(SolicitacaoService solicitacaoService, AlertaService alertaService,
                            AcessoService acessoService, ConexaoService conexaoService, TitularService titularService,
                            MapeamentoService mapeamentoService, EvidenciaService evidenciaService,
                            AuditoriaService auditoria, LojistaAtualService lojistaAtual,
                            CalculadoraIndiceConformidade calculadoraIndice, CalculadoraPrazo calculadoraPrazo,
                            GeradorRelatorioPdf geradorPdf, Clock clock) {
        this.solicitacaoService = solicitacaoService;
        this.alertaService = alertaService;
        this.acessoService = acessoService;
        this.conexaoService = conexaoService;
        this.titularService = titularService;
        this.mapeamentoService = mapeamentoService;
        this.evidenciaService = evidenciaService;
        this.auditoria = auditoria;
        this.lojistaAtual = lojistaAtual;
        this.calculadoraIndice = calculadoraIndice;
        this.calculadoraPrazo = calculadoraPrazo;
        this.geradorPdf = geradorPdf;
        this.clock = clock;
    }

    public RelatorioIndicadoresResponse indicadores() {
        LocalDate fim = LocalDate.now(clock);
        LocalDate inicio = fim.minusMonths(MESES_DO_PERIODO);
        List<SolicitacaoLgpd> solicitacoes = solicitacaoService.doLojista().stream()
                .filter(s -> !s.getAbertaEm().toLocalDate().isBefore(inicio))
                .toList();
        List<SolicitacaoLgpd> atendidas = solicitacoes.stream().filter(s -> !s.isAberta()).toList();
        long noPrazo = atendidas.stream().filter(s -> !s.getConcluidaEm().toLocalDate().isAfter(s.getPrazo())).count();
        long atrasadas = solicitacoes.stream().filter(calculadoraPrazo::estaVencida).count();

        List<Alerta> alertas = alertaService.doLojista();
        long alertasAbertos = alertas.stream().filter(Alerta::isAberto).count();
        long alertasAltaAbertos = alertas.stream().filter(a -> a.isAberto() && a.getPrioridade() == Prioridade.ALTA).count();
        long acessosIndevidos = acessoService.acessosIndevidos().size();

        return new RelatorioIndicadoresResponse(inicio, fim,
                calculadoraIndice.calcular(atrasadas, acessosIndevidos, alertasAltaAbertos),
                solicitacoes.size(), atendidas.size(), noPrazo, atrasadas, tempoMedioEmDias(atendidas),
                alertas.size() - alertasAbertos, alertasAbertos, acessoService.contarRevogados(), acessosIndevidos,
                conexaoService.idsPlataformasConectadas().size(), mapeamentoService.resumo().titularesMapeados(),
                titularService.contarAnonimizados());
    }

    public ArquivoPdf gerarPdf(String tipoSlug) {
        TipoRelatorio tipo = TipoRelatorio.deSlug(tipoSlug);
        LocalDateTime agora = LocalDateTime.now(clock);
        DadosRelatorio dados = new DadosRelatorio(lojistaAtual.obter(), indicadores(),
                solicitacaoService.listar(null, null, null), alertaService.listar(null, null, null),
                evidenciaService.listar(), auditoria.listar(ACOES_NO_RELATORIO), conexaoService.listar(), agora);
        byte[] pdf = geradorPdf.gerar(tipo, dados);
        auditoria.registrar(AcaoAuditoria.RELATORIO_GERADO, tipo.getTitulo() + " exportado em PDF.", "Relatorio", null);
        return new ArquivoPdf("atria-" + tipo.getSlug() + "-" + agora.toLocalDate() + ".pdf", pdf);
    }

    private double tempoMedioEmDias(List<SolicitacaoLgpd> atendidas) {
        double media = atendidas.stream()
                .mapToLong(s -> Duration.between(s.getAbertaEm(), s.getConcluidaEm()).toHours())
                .average().orElse(0) / 24.0;
        return Math.round(media * 10) / 10.0;
    }
}
