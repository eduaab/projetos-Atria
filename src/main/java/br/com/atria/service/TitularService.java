package br.com.atria.service;

import br.com.atria.dto.response.InteracaoResponse;
import br.com.atria.dto.response.PlataformaTitularResponse;
import br.com.atria.dto.response.SolicitacaoResponse;
import br.com.atria.dto.response.TitularDetalheResponse;
import br.com.atria.dto.response.TitularResumoResponse;
import br.com.atria.exception.RecursoNaoEncontradoException;
import br.com.atria.mapper.InteracaoMapper;
import br.com.atria.mapper.SolicitacaoMapper;
import br.com.atria.mapper.TitularMapper;
import br.com.atria.model.DadoIdentificado;
import br.com.atria.model.IdentidadePlataforma;
import br.com.atria.model.Interacao;
import br.com.atria.model.Plataforma;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.Titular;
import br.com.atria.repository.DadoIdentificadoRepository;
import br.com.atria.repository.IdentidadePlataformaRepository;
import br.com.atria.repository.InteracaoRepository;
import br.com.atria.repository.SolicitacaoLgpdRepository;
import br.com.atria.repository.TitularRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static br.com.atria.service.regra.NormalizadorTexto.normalizar;
import static br.com.atria.service.regra.NormalizadorTexto.somenteDigitos;

/** Central de clientes: busca unificada e perfil do titular. Nada é alterado nas plataformas de origem. */
@Service
public class TitularService {
    private static final int MINIMO_DIGITOS_BUSCA = 3;

    private final TitularRepository titularRepository;
    private final IdentidadePlataformaRepository identidadeRepository;
    private final DadoIdentificadoRepository dadoRepository;
    private final InteracaoRepository interacaoRepository;
    private final SolicitacaoLgpdRepository solicitacaoRepository;
    private final PlataformaService plataformaService;
    private final LojistaAtualService lojistaAtual;
    private final TitularMapper titularMapper;
    private final InteracaoMapper interacaoMapper;
    private final SolicitacaoMapper solicitacaoMapper;

    public TitularService(TitularRepository titularRepository, IdentidadePlataformaRepository identidadeRepository,
                          DadoIdentificadoRepository dadoRepository, InteracaoRepository interacaoRepository,
                          SolicitacaoLgpdRepository solicitacaoRepository, PlataformaService plataformaService,
                          LojistaAtualService lojistaAtual, TitularMapper titularMapper,
                          InteracaoMapper interacaoMapper, SolicitacaoMapper solicitacaoMapper) {
        this.titularRepository = titularRepository;
        this.identidadeRepository = identidadeRepository;
        this.dadoRepository = dadoRepository;
        this.interacaoRepository = interacaoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.plataformaService = plataformaService;
        this.lojistaAtual = lojistaAtual;
        this.titularMapper = titularMapper;
        this.interacaoMapper = interacaoMapper;
        this.solicitacaoMapper = solicitacaoMapper;
    }

    /** Busca por nome, e-mail, CPF ou telefone. Titulares anonimizados nunca aparecem. */
    public List<TitularResumoResponse> buscar(String busca) {
        String termo = normalizar(busca);
        String digitos = somenteDigitos(busca);
        Set<Long> comSolicitacaoAberta = solicitacaoRepository.findByTitularIdIn(idsDoLojista()).stream()
                .filter(SolicitacaoLgpd::isAberta)
                .map(SolicitacaoLgpd::getTitularId)
                .collect(Collectors.toSet());

        return ativos().stream()
                .filter(titular -> corresponde(titular, termo, digitos))
                .sorted(Comparator.comparing(titular -> normalizar(titular.getNome())))
                .map(titular -> titularMapper.paraResumo(titular, contarPlataformas(titular.getId()),
                        comSolicitacaoAberta.contains(titular.getId())))
                .toList();
    }

    public TitularDetalheResponse detalhe(Long id) {
        Titular titular = buscarEntidade(id);
        Map<Long, Plataforma> plataformas = plataformaService.mapaPorId();
        List<Interacao> interacoes = interacaoRepository.findByTitularIdOrderByDataDesc(id);

        BigDecimal gastosTotais = interacoes.stream()
                .filter(Interacao::isCompra)
                .map(Interacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<InteracaoResponse> historico = interacoes.stream()
                .map(interacao -> interacaoMapper.paraResponse(interacao, plataformas.get(interacao.getPlataformaId())))
                .toList();
        List<SolicitacaoResponse> solicitacoes = solicitacaoRepository.findByTitularId(id).stream()
                .sorted(Comparator.comparing(SolicitacaoLgpd::getAbertaEm).reversed())
                .map(solicitacao -> solicitacaoMapper.paraResponse(solicitacao, titular))
                .toList();

        return titularMapper.paraDetalhe(titular, gastosTotais, plataformasDoTitular(id, plataformas), historico, solicitacoes);
    }

    public Titular buscarEntidade(Long id) {
        return titularRepository.findById(id)
                .filter(titular -> Objects.equals(titular.getLojistaId(), lojistaAtual.id()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Titular " + id + " não encontrado."));
    }

    public Titular salvar(Titular titular) {
        return titularRepository.save(titular);
    }

    public List<Titular> doLojista() {
        return titularRepository.findByLojistaId(lojistaAtual.id());
    }

    public List<Titular> ativos() {
        return doLojista().stream().filter(titular -> !titular.isAnonimizado()).toList();
    }

    public Set<Long> idsDoLojista() {
        return doLojista().stream().map(Titular::getId).collect(Collectors.toSet());
    }

    public long contarAnonimizados() {
        return doLojista().stream().filter(Titular::isAnonimizado).count();
    }

    /** Nomes das plataformas em que o titular foi identificado, em ordem alfabética. */
    public List<String> nomesPlataformas(Long titularId) {
        Map<Long, Plataforma> plataformas = plataformaService.mapaPorId();
        return identidadeRepository.findByTitularId(titularId).stream()
                .map(identidade -> plataformas.get(identidade.getPlataformaId()).getNome())
                .distinct()
                .sorted()
                .toList();
    }

    private List<PlataformaTitularResponse> plataformasDoTitular(Long titularId, Map<Long, Plataforma> plataformas) {
        List<DadoIdentificado> dados = dadoRepository.findByTitularId(titularId);
        return identidadeRepository.findByTitularId(titularId).stream()
                .map(identidade -> paraPlataformaTitular(identidade, plataformas.get(identidade.getPlataformaId()), dados))
                .sorted(Comparator.comparing(PlataformaTitularResponse::nome))
                .toList();
    }

    private PlataformaTitularResponse paraPlataformaTitular(IdentidadePlataforma identidade, Plataforma plataforma,
                                                            List<DadoIdentificado> dados) {
        List<String> tipos = dados.stream()
                .filter(dado -> dado.getPlataformaId().equals(plataforma.getId()))
                .map(dado -> dado.getTipoDado().getDescricao())
                .toList();
        return new PlataformaTitularResponse(plataforma.getSlug(), plataforma.getNome(), identidade.getIdExterno(),
                identidade.getMetodoVinculo().getDescricao(), tipos);
    }

    private int contarPlataformas(Long titularId) {
        return (int) identidadeRepository.findByTitularId(titularId).stream()
                .map(IdentidadePlataforma::getPlataformaId).distinct().count();
    }

    private boolean corresponde(Titular titular, String termo, String digitos) {
        if (termo.isEmpty()) return true;
        if (normalizar(titular.getNome()).contains(termo) || normalizar(titular.getEmail()).contains(termo)) return true;
        return digitos.length() >= MINIMO_DIGITOS_BUSCA
                && (somenteDigitos(titular.getCpf()).contains(digitos) || somenteDigitos(titular.getTelefone()).contains(digitos));
    }
}
