package br.com.atria.service;

import br.com.atria.dto.response.CampoPessoalResponse;
import br.com.atria.dto.response.ExclusaoPreviaResponse;
import br.com.atria.dto.response.ExclusaoResponse;
import br.com.atria.model.Evidencia;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.Titular;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.repository.DadoIdentificadoRepository;
import br.com.atria.repository.IdentidadePlataformaRepository;
import br.com.atria.service.regra.AnonimizadorTitular;
import br.com.atria.service.regra.MascaradorCpf;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Exclusão (purga) dos dados do titular. A Atria apaga só o que ela guarda e preserva,
 * sem dado pessoal, o registro que comprova o atendimento.
 */
@Service
public class ExclusaoService {
    private final TitularService titularService;
    private final SolicitacaoService solicitacaoService;
    private final IdentidadePlataformaRepository identidadeRepository;
    private final DadoIdentificadoRepository dadoRepository;
    private final AnonimizadorTitular anonimizador;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public ExclusaoService(TitularService titularService, SolicitacaoService solicitacaoService,
                           IdentidadePlataformaRepository identidadeRepository, DadoIdentificadoRepository dadoRepository,
                           AnonimizadorTitular anonimizador, AuditoriaService auditoria, Clock clock) {
        this.titularService = titularService;
        this.solicitacaoService = solicitacaoService;
        this.identidadeRepository = identidadeRepository;
        this.dadoRepository = dadoRepository;
        this.anonimizador = anonimizador;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    public ExclusaoPreviaResponse previa(Long titularId) {
        Titular titular = titularService.buscarEntidade(titularId);
        List<CampoPessoalResponse> apagados = anonimizador.camposQueSeraoApagados(titular).stream()
                .map(campo -> new CampoPessoalResponse(campo.campo(), campo.valor()))
                .toList();
        List<String> plataformas = titularService.nomesPlataformas(titularId);
        String protocolo = solicitacaoService.buscarExclusaoAberta(titularId).map(SolicitacaoLgpd::getProtocolo).orElse(null);

        List<String> preservados = List.of(
                "Protocolo da solicitação: " + (protocolo == null ? "gerado na confirmação" : protocolo),
                "Datas de abertura e conclusão da solicitação",
                "Plataformas onde havia dados: " + (plataformas.isEmpty() ? "nenhuma" : String.join(", ", plataformas)),
                "CPF mascarado: " + MascaradorCpf.mascarar(titular.getCpf()),
                "Histórico de compras e interações, sem dados pessoais");
        return new ExclusaoPreviaResponse(titularId, titular.getNome(), apagados, preservados, plataformas, protocolo);
    }

    public ExclusaoResponse executar(Long titularId) {
        Titular titular = titularService.buscarEntidade(titularId);
        List<String> plataformas = titularService.nomesPlataformas(titularId);

        String cpfMascarado = anonimizador.anonimizar(titular, LocalDateTime.now(clock));
        titularService.salvar(titular);
        identidadeRepository.deleteByTitularId(titularId);
        dadoRepository.deleteByTitularId(titularId);

        SolicitacaoLgpd solicitacao = solicitacaoService.obterOuCriarExclusao(titular);
        Evidencia evidencia = solicitacaoService.concluir(solicitacao, cpfMascarado, plataformas);
        auditoria.registrar(AcaoAuditoria.TITULAR_EXCLUIDO, "Dados pessoais do titular #" + titularId
                + " excluídos e anonimizados (protocolo " + evidencia.getProtocolo() + ").", "Titular", titularId);

        return new ExclusaoResponse(evidencia.getProtocolo(), titularId, solicitacao.getConcluidaEm(), cpfMascarado,
                plataformas, evidencia.getHashComprovante());
    }
}
