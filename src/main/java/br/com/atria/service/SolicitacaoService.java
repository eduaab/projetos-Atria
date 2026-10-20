package br.com.atria.service;

import br.com.atria.dto.request.AlterarStatusSolicitacaoRequest;
import br.com.atria.dto.request.NovaSolicitacaoRequest;
import br.com.atria.dto.response.SolicitacaoResponse;
import br.com.atria.exception.RecursoNaoEncontradoException;
import br.com.atria.exception.RegraNegocioException;
import br.com.atria.mapper.SolicitacaoMapper;
import br.com.atria.model.Evidencia;
import br.com.atria.model.Interacao;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.Titular;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.model.enums.StatusSolicitacao;
import br.com.atria.model.enums.TipoInteracao;
import br.com.atria.model.enums.TipoSolicitacao;
import br.com.atria.repository.InteracaoRepository;
import br.com.atria.repository.SolicitacaoLgpdRepository;
import br.com.atria.service.regra.CalculadoraPrazo;
import br.com.atria.service.regra.GeradorProtocolo;
import br.com.atria.service.regra.MascaradorCpf;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Solicitações do titular (LGPD): registro, prazos, mudança de status e conclusão com comprovante. */
@Service
public class SolicitacaoService {
    private final SolicitacaoLgpdRepository repository;
    private final InteracaoRepository interacaoRepository;
    private final TitularService titularService;
    private final EvidenciaService evidenciaService;
    private final ResolucaoAlertaService resolucaoAlerta;
    private final AuditoriaService auditoria;
    private final CalculadoraPrazo calculadoraPrazo;
    private final GeradorProtocolo geradorProtocolo;
    private final SolicitacaoMapper mapper;
    private final Clock clock;

    public SolicitacaoService(SolicitacaoLgpdRepository repository, InteracaoRepository interacaoRepository,
                              TitularService titularService, EvidenciaService evidenciaService,
                              ResolucaoAlertaService resolucaoAlerta, AuditoriaService auditoria,
                              CalculadoraPrazo calculadoraPrazo, GeradorProtocolo geradorProtocolo,
                              SolicitacaoMapper mapper, Clock clock) {
        this.repository = repository;
        this.interacaoRepository = interacaoRepository;
        this.titularService = titularService;
        this.evidenciaService = evidenciaService;
        this.resolucaoAlerta = resolucaoAlerta;
        this.auditoria = auditoria;
        this.calculadoraPrazo = calculadoraPrazo;
        this.geradorProtocolo = geradorProtocolo;
        this.mapper = mapper;
        this.clock = clock;
    }

    public List<SolicitacaoResponse> listar(StatusSolicitacao status, TipoSolicitacao tipo, CanalOrigem canal) {
        Map<Long, Titular> titulares = titularService.doLojista().stream()
                .collect(Collectors.toMap(Titular::getId, Function.identity()));
        return doLojista().stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> tipo == null || s.getTipo() == tipo)
                .filter(s -> canal == null || s.getCanalOrigem() == canal)
                .sorted(Comparator.comparing(SolicitacaoLgpd::isAberta).reversed().thenComparing(SolicitacaoLgpd::getPrazo))
                .map(s -> mapper.paraResponse(s, titulares.get(s.getTitularId())))
                .toList();
    }

    public List<SolicitacaoResponse> listarAbertas() {
        return listar(null, null, null).stream().filter(s -> s.status() != StatusSolicitacao.CONCLUIDA).toList();
    }

    /** Todas as solicitações do lojista, já com o status ATRASADA aplicado às vencidas. */
    public List<SolicitacaoLgpd> doLojista() {
        List<SolicitacaoLgpd> solicitacoes = repository.findByTitularIdIn(titularService.idsDoLojista());
        solicitacoes.stream()
                .filter(s -> calculadoraPrazo.estaVencida(s) && s.getStatus() != StatusSolicitacao.ATRASADA)
                .forEach(s -> {
                    s.alterarStatus(StatusSolicitacao.ATRASADA);
                    repository.save(s);
                });
        return solicitacoes;
    }

    public List<SolicitacaoLgpd> abertas() {
        return doLojista().stream().filter(SolicitacaoLgpd::isAberta).toList();
    }

    public Optional<SolicitacaoLgpd> buscarOpcional(Long id) {
        return repository.findById(id).filter(s -> titularService.idsDoLojista().contains(s.getTitularId()));
    }

    public Optional<SolicitacaoLgpd> buscarExclusaoAberta(Long titularId) {
        return repository.findByTitularId(titularId).stream()
                .filter(s -> s.isAberta() && s.getTipo() == TipoSolicitacao.EXCLUSAO)
                .findFirst();
    }

    public SolicitacaoResponse registrar(NovaSolicitacaoRequest requisicao) {
        Titular titular = titularService.buscarEntidade(requisicao.titularId());
        if (titular.isAnonimizado()) {
            throw new RegraNegocioException("Este titular já foi anonimizado e não pode abrir novas solicitações.");
        }
        return mapper.paraResponse(abrir(titular, requisicao.tipo(), requisicao.canalOrigem()), titular);
    }

    public SolicitacaoLgpd obterOuCriarExclusao(Titular titular) {
        return buscarExclusaoAberta(titular.getId())
                .orElseGet(() -> abrir(titular, TipoSolicitacao.EXCLUSAO, CanalOrigem.PAINEL));
    }

    public SolicitacaoResponse alterarStatus(Long id, AlterarStatusSolicitacaoRequest requisicao) {
        SolicitacaoLgpd solicitacao = buscarOpcional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação " + id + " não encontrada."));
        StatusSolicitacao novoStatus = requisicao.status();
        validarMudanca(solicitacao, novoStatus);

        Titular titular = titularService.buscarEntidade(solicitacao.getTitularId());
        if (novoStatus == StatusSolicitacao.CONCLUIDA) {
            concluir(solicitacao, MascaradorCpf.mascarar(titular.getCpf()), titularService.nomesPlataformas(titular.getId()));
        } else {
            StatusSolicitacao anterior = solicitacao.getStatus();
            solicitacao.alterarStatus(novoStatus);
            repository.save(solicitacao);
            auditoria.registrar(AcaoAuditoria.SOLICITACAO_STATUS_ALTERADO, "Solicitação " + solicitacao.getProtocolo()
                    + " passou de " + anterior.getDescricao() + " para " + novoStatus.getDescricao() + ".",
                    "SolicitacaoLgpd", id);
        }
        return mapper.paraResponse(solicitacao, titular);
    }

    /** Conclui a solicitação, gera o comprovante e resolve o alerta de prazo correspondente. */
    public Evidencia concluir(SolicitacaoLgpd solicitacao, String cpfMascarado, List<String> plataformas) {
        solicitacao.concluir(LocalDateTime.now(clock));
        repository.save(solicitacao);
        Evidencia evidencia = evidenciaService.registrar(solicitacao, cpfMascarado, plataformas);
        resolucaoAlerta.resolverPorOrigem(OrigemAlerta.SOLICITACAO, solicitacao.getId());
        auditoria.registrar(AcaoAuditoria.SOLICITACAO_CONCLUIDA, "Solicitação " + solicitacao.getProtocolo()
                + " (" + solicitacao.getTipo().getDescricao() + ") concluída com comprovante.", "SolicitacaoLgpd", solicitacao.getId());
        return evidencia;
    }

    private void validarMudanca(SolicitacaoLgpd solicitacao, StatusSolicitacao novoStatus) {
        if (!solicitacao.isAberta()) {
            throw new RegraNegocioException("A solicitação " + solicitacao.getProtocolo() + " já foi concluída.");
        }
        if (novoStatus == StatusSolicitacao.ATRASADA) {
            throw new RegraNegocioException("O status Atrasada é definido automaticamente pelo prazo.");
        }
        if (novoStatus == solicitacao.getStatus()) {
            throw new RegraNegocioException("A solicitação já está com o status " + novoStatus.getDescricao() + ".");
        }
        if (calculadoraPrazo.estaVencida(solicitacao) && novoStatus != StatusSolicitacao.CONCLUIDA) {
            throw new RegraNegocioException("A solicitação está vencida: ela só pode ser concluída.");
        }
        if (novoStatus == StatusSolicitacao.CONCLUIDA && solicitacao.getTipo() == TipoSolicitacao.EXCLUSAO) {
            throw new RegraNegocioException("Solicitações de exclusão são concluídas pelo botão \"Deletar todos os dados\" do titular.");
        }
    }

    private SolicitacaoLgpd abrir(Titular titular, TipoSolicitacao tipo, CanalOrigem canal) {
        LocalDateTime agora = LocalDateTime.now(clock);
        SolicitacaoLgpd solicitacao = repository.save(new SolicitacaoLgpd(titular.getId(), tipo, canal, agora,
                calculadoraPrazo.calcularPrazo(agora), geradorProtocolo.gerar()));
        interacaoRepository.save(new Interacao(titular.getId(), null, TipoInteracao.SOLICITACAO_LGPD, agora, null));
        auditoria.registrar(AcaoAuditoria.SOLICITACAO_REGISTRADA, "Solicitação " + solicitacao.getProtocolo() + " ("
                + tipo.getDescricao() + ") registrada via " + canal.getDescricao() + " para o titular #" + titular.getId() + ".",
                "SolicitacaoLgpd", solicitacao.getId());
        return solicitacao;
    }
}
