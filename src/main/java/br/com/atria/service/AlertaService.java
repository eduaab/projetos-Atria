package br.com.atria.service;

import br.com.atria.dto.request.AlterarStatusAlertaRequest;
import br.com.atria.dto.response.AlertaResponse;
import br.com.atria.dto.response.VarreduraResponse;
import br.com.atria.exception.RecursoNaoEncontradoException;
import br.com.atria.exception.RegraNegocioException;
import br.com.atria.mapper.AlertaMapper;
import br.com.atria.model.Acesso;
import br.com.atria.model.Alerta;
import br.com.atria.model.IdentidadePlataforma;
import br.com.atria.model.Plataforma;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.model.enums.Prioridade;
import br.com.atria.model.enums.StatusAlerta;
import br.com.atria.model.enums.TipoAlerta;
import br.com.atria.repository.AlertaRepository;
import br.com.atria.repository.IdentidadePlataformaRepository;
import br.com.atria.repository.PendenciaCanalRepository;
import br.com.atria.service.regra.CalculadoraPrazo;
import br.com.atria.service.regra.GeradorFraseResumo;
import br.com.atria.service.regra.ItemResumo;
import br.com.atria.service.regra.Plural;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Central de Alertas: varredura (monitoramento simulado), consulta e mudança de status. */
@Service
public class AlertaService {
    private static final Comparator<Alerta> ORDEM_DE_ATENCAO = Comparator
            .comparing(Alerta::isAberto).reversed()
            .thenComparing(Alerta::getPrioridade)
            .thenComparing(Alerta::getCriadoEm, Comparator.reverseOrder());

    private final AlertaRepository alertaRepository;
    private final PendenciaCanalRepository pendenciaRepository;
    private final IdentidadePlataformaRepository identidadeRepository;
    private final SolicitacaoService solicitacaoService;
    private final AcessoService acessoService;
    private final ConexaoService conexaoService;
    private final PlataformaService plataformaService;
    private final TitularService titularService;
    private final FabricaAlertas fabrica;
    private final GeradorFraseResumo geradorFrase;
    private final CalculadoraPrazo calculadoraPrazo;
    private final AuditoriaService auditoria;
    private final LojistaAtualService lojistaAtual;
    private final AlertaMapper mapper;
    private final Clock clock;

    public AlertaService(AlertaRepository alertaRepository, PendenciaCanalRepository pendenciaRepository,
                         IdentidadePlataformaRepository identidadeRepository, SolicitacaoService solicitacaoService,
                         AcessoService acessoService, ConexaoService conexaoService, PlataformaService plataformaService,
                         TitularService titularService, FabricaAlertas fabrica, GeradorFraseResumo geradorFrase,
                         CalculadoraPrazo calculadoraPrazo, AuditoriaService auditoria,
                         LojistaAtualService lojistaAtual, AlertaMapper mapper, Clock clock) {
        this.alertaRepository = alertaRepository;
        this.pendenciaRepository = pendenciaRepository;
        this.identidadeRepository = identidadeRepository;
        this.solicitacaoService = solicitacaoService;
        this.acessoService = acessoService;
        this.conexaoService = conexaoService;
        this.plataformaService = plataformaService;
        this.titularService = titularService;
        this.fabrica = fabrica;
        this.geradorFrase = geradorFrase;
        this.calculadoraPrazo = calculadoraPrazo;
        this.auditoria = auditoria;
        this.lojistaAtual = lojistaAtual;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Percorre pendências de canal, acessos e solicitações e cria um alerta por ocorrência.
     * É idempotente: uma origem que já tem alerta não ganha outro; só a prioridade dos abertos é recalculada.
     */
    public VarreduraResponse executarVarredura() {
        LocalDateTime agora = LocalDateTime.now(clock);
        int criados = 0;
        int reclassificados = 0;
        for (Alerta ocorrencia : detectarOcorrencias(agora)) {
            List<Alerta> existentes = alertaRepository.findByTipoOrigemAndOrigemId(ocorrencia.getTipoOrigem(), ocorrencia.getOrigemId());
            if (existentes.isEmpty()) {
                alertaRepository.save(ocorrencia);
                criados++;
            } else {
                reclassificados += reclassificar(existentes, ocorrencia.getPrioridade());
            }
        }
        auditoria.registrar(AcaoAuditoria.VARREDURA_EXECUTADA, "Varredura concluída: "
                + Plural.de(criados, "alerta novo", "alertas novos") + ", "
                + Plural.de(reclassificados, "reclassificado", "reclassificados") + ".", "Alerta", null);
        return new VarreduraResponse(criados, reclassificados, abertos().size(), fraseResumo(), agora);
    }

    public List<AlertaResponse> listar(Prioridade prioridade, StatusAlerta status, String plataformaSlug) {
        Long plataformaId = plataformaSlug == null || plataformaSlug.isBlank() ? null : plataformaService.buscarPorSlug(plataformaSlug).getId();
        return doLojista().stream()
                .filter(a -> prioridade == null || a.getPrioridade() == prioridade)
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> plataformaId == null || plataformaId.equals(a.getPlataformaId()))
                .sorted(ORDEM_DE_ATENCAO)
                .map(this::paraResponse)
                .toList();
    }

    public AlertaResponse detalhe(Long id) {
        return paraResponse(buscarEntidade(id));
    }

    public AlertaResponse alterarStatus(Long id, AlterarStatusAlertaRequest requisicao) {
        Alerta alerta = buscarEntidade(id);
        StatusAlerta novoStatus = requisicao.status();
        if (alerta.getStatus() == novoStatus) {
            throw new RegraNegocioException("O alerta já está com o status " + novoStatus.getDescricao() + ".");
        }
        if (novoStatus == StatusAlerta.RESOLVIDO) {
            exigirOrigemTratada(alerta);
            resolverPendenciaDeOrigem(alerta);
        }
        StatusAlerta anterior = alerta.getStatus();
        alerta.alterarStatus(novoStatus, LocalDateTime.now(clock));
        alertaRepository.save(alerta);
        auditoria.registrar(AcaoAuditoria.ALERTA_STATUS_ALTERADO, "Alerta #" + id + " (" + alerta.getTipo().getDescricao()
                + ") passou de " + anterior.getDescricao() + " para " + novoStatus.getDescricao() + ".", "Alerta", id);
        return paraResponse(alerta);
    }

    public List<AlertaResponse> prioritarios(int limite) {
        return abertos().stream().sorted(ORDEM_DE_ATENCAO).limit(limite).map(this::paraResponse).toList();
    }

    /** Frase da Home. Prazos LGPD só entram quando faltam 7 dias ou menos (prioridade média ou alta). */
    public String fraseResumo() {
        Map<Long, Plataforma> plataformas = plataformaService.mapaPorId();
        List<ItemResumo> itens = abertos().stream()
                .filter(a -> a.getTipo() != TipoAlerta.PRAZO_LGPD || a.getPrioridade() != Prioridade.BAIXA)
                .map(a -> new ItemResumo(a.getTipo(), a.getPlataformaId() == null ? null : plataformas.get(a.getPlataformaId()).getNome()))
                .toList();
        return geradorFrase.gerar(itens);
    }

    public List<Alerta> doLojista() {
        return alertaRepository.findByLojistaId(lojistaAtual.id());
    }

    public List<Alerta> abertos() {
        return doLojista().stream().filter(Alerta::isAberto).toList();
    }

    public AlertaResponse paraResponse(Alerta alerta) {
        Plataforma plataforma = alerta.getPlataformaId() == null ? null : plataformaService.buscarOpcional(alerta.getPlataformaId()).orElse(null);
        Long acessoRevogavel = alerta.getTipoOrigem() == OrigemAlerta.ACESSO
                && acessoService.buscar(alerta.getOrigemId()).filter(Acesso::isAtivo).isPresent() ? alerta.getOrigemId() : null;
        Long diasRestantes = alerta.getTipoOrigem() == OrigemAlerta.SOLICITACAO
                ? solicitacaoService.buscarOpcional(alerta.getOrigemId()).filter(SolicitacaoLgpd::isAberta)
                        .map(s -> calculadoraPrazo.diasRestantes(s.getPrazo())).orElse(null)
                : null;
        return mapper.paraResponse(alerta, plataforma,
                alerta.getTitularId() == null ? null : titularService.buscarEntidade(alerta.getTitularId()),
                acessoRevogavel, diasRestantes);
    }

    private List<Alerta> detectarOcorrencias(LocalDateTime agora) {
        Set<Long> conectadas = conexaoService.idsPlataformasConectadas();
        Map<Long, Plataforma> plataformas = plataformaService.mapaPorId();
        List<Alerta> ocorrencias = new ArrayList<>();

        pendenciaRepository.findByLojistaIdAndResolvidaFalse(lojistaAtual.id()).stream()
                .filter(p -> conectadas.contains(p.getPlataformaId()))
                .forEach(p -> ocorrencias.add(fabrica.dePendencia(p, plataformas.get(p.getPlataformaId()),
                        titularService.buscarEntidade(p.getTitularId()), agora)));
        acessoService.acessosIndevidos().forEach(acesso -> {
            Plataforma plataforma = acessoService.plataformaDe(acesso);
            ocorrencias.add(fabrica.deAcesso(acesso, acessoService.usuarioDe(acesso), plataforma, clientesNaPlataforma(plataforma)));
        });
        solicitacaoService.abertas().forEach(s ->
                ocorrencias.add(fabrica.deSolicitacao(s, titularService.buscarEntidade(s.getTitularId()))));
        return ocorrencias;
    }

    private int reclassificar(List<Alerta> existentes, Prioridade prioridadeAtual) {
        int alterados = 0;
        for (Alerta alerta : existentes) {
            if (alerta.isAberto() && alerta.getPrioridade() != prioridadeAtual) {
                alerta.reclassificar(prioridadeAtual);
                alertaRepository.save(alerta);
                alterados++;
            }
        }
        return alterados;
    }

    private void exigirOrigemTratada(Alerta alerta) {
        if (alerta.getTipoOrigem() == OrigemAlerta.ACESSO && acessoService.buscar(alerta.getOrigemId()).filter(Acesso::isAtivo).isPresent()) {
            throw new RegraNegocioException("Revogue o acesso para resolver este alerta.");
        }
        if (alerta.getTipoOrigem() == OrigemAlerta.SOLICITACAO
                && solicitacaoService.buscarOpcional(alerta.getOrigemId()).filter(SolicitacaoLgpd::isAberta).isPresent()) {
            throw new RegraNegocioException("Conclua a solicitação para resolver este alerta.");
        }
    }

    private void resolverPendenciaDeOrigem(Alerta alerta) {
        if (alerta.getTipoOrigem() != OrigemAlerta.PENDENCIA) return;
        pendenciaRepository.findById(alerta.getOrigemId()).ifPresent(pendencia -> {
            pendencia.resolver();
            pendenciaRepository.save(pendencia);
        });
    }

    private int clientesNaPlataforma(Plataforma plataforma) {
        return (int) identidadeRepository.findByPlataformaId(plataforma.getId()).stream()
                .map(IdentidadePlataforma::getTitularId).distinct().count();
    }

    private Alerta buscarEntidade(Long id) {
        return alertaRepository.findById(id)
                .filter(a -> Objects.equals(a.getLojistaId(), lojistaAtual.id()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta " + id + " não encontrado."));
    }
}
