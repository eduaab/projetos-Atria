package br.com.atria.service;

import br.com.atria.dto.response.EvidenciaResponse;
import br.com.atria.exception.RecursoNaoEncontradoException;
import br.com.atria.mapper.EvidenciaMapper;
import br.com.atria.model.Evidencia;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.relatorio.ArquivoPdf;
import br.com.atria.relatorio.GeradorComprovantePdf;
import br.com.atria.repository.EvidenciaRepository;
import br.com.atria.repository.SolicitacaoLgpdRepository;
import br.com.atria.service.regra.GeradorHashComprovante;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Comprovantes de solicitações atendidas, que continuam existindo depois da anonimização. */
@Service
public class EvidenciaService {
    private final EvidenciaRepository evidenciaRepository;
    private final SolicitacaoLgpdRepository solicitacaoRepository;
    private final TitularService titularService;
    private final LojistaAtualService lojistaAtual;
    private final GeradorHashComprovante geradorHash;
    private final GeradorComprovantePdf geradorComprovante;
    private final AuditoriaService auditoria;
    private final EvidenciaMapper mapper;
    private final Clock clock;

    public EvidenciaService(EvidenciaRepository evidenciaRepository, SolicitacaoLgpdRepository solicitacaoRepository,
                            TitularService titularService, LojistaAtualService lojistaAtual,
                            GeradorHashComprovante geradorHash, GeradorComprovantePdf geradorComprovante,
                            AuditoriaService auditoria, EvidenciaMapper mapper, Clock clock) {
        this.evidenciaRepository = evidenciaRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.titularService = titularService;
        this.lojistaAtual = lojistaAtual;
        this.geradorHash = geradorHash;
        this.geradorComprovante = geradorComprovante;
        this.auditoria = auditoria;
        this.mapper = mapper;
        this.clock = clock;
    }

    public Evidencia registrar(SolicitacaoLgpd solicitacao, String cpfMascarado, List<String> plataformas) {
        LocalDateTime agora = LocalDateTime.now(clock);
        String hash = geradorHash.gerar(solicitacao.getProtocolo(), solicitacao.getTipo().name(), cpfMascarado,
                String.join(",", plataformas), agora.toString());
        return evidenciaRepository.save(new Evidencia(solicitacao.getId(), agora, cpfMascarado, plataformas, hash,
                solicitacao.getProtocolo()));
    }

    public List<EvidenciaResponse> listar() {
        Map<Long, SolicitacaoLgpd> solicitacoes = solicitacoesDoLojista();
        return evidenciaRepository.findBySolicitacaoIdIn(solicitacoes.keySet()).stream()
                .sorted(Comparator.comparing(Evidencia::getGeradaEm).reversed())
                .map(evidencia -> mapper.paraResponse(evidencia, solicitacoes.get(evidencia.getSolicitacaoId())))
                .toList();
    }

    public ArquivoPdf gerarComprovante(String protocolo) {
        Map<Long, SolicitacaoLgpd> solicitacoes = solicitacoesDoLojista();
        Evidencia evidencia = evidenciaRepository.findByProtocolo(protocolo)
                .filter(e -> solicitacoes.containsKey(e.getSolicitacaoId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comprovante " + protocolo + " não encontrado."));
        EvidenciaResponse dados = mapper.paraResponse(evidencia, solicitacoes.get(evidencia.getSolicitacaoId()));
        byte[] pdf = geradorComprovante.gerar(dados, lojistaAtual.obter(), LocalDateTime.now(clock));
        auditoria.registrar(AcaoAuditoria.RELATORIO_GERADO, "Comprovante " + evidencia.getProtocolo() + " exportado em PDF.",
                "Evidencia", evidencia.getId());
        return new ArquivoPdf("atria-comprovante-" + evidencia.getProtocolo() + ".pdf", pdf);
    }

    private Map<Long, SolicitacaoLgpd> solicitacoesDoLojista() {
        return solicitacaoRepository.findByTitularIdIn(titularService.idsDoLojista()).stream()
                .collect(Collectors.toMap(SolicitacaoLgpd::getId, Function.identity()));
    }
}
