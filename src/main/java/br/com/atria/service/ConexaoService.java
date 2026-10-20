package br.com.atria.service;

import br.com.atria.dto.response.ConexaoDetalheResponse;
import br.com.atria.dto.response.ConexaoResponse;
import br.com.atria.dto.response.ContagemResponse;
import br.com.atria.exception.RegraNegocioException;
import br.com.atria.mapper.ConexaoMapper;
import br.com.atria.model.Conexao;
import br.com.atria.model.IdentidadePlataforma;
import br.com.atria.model.Plataforma;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.model.enums.TipoDado;
import br.com.atria.repository.ConexaoRepository;
import br.com.atria.repository.DadoIdentificadoRepository;
import br.com.atria.repository.IdentidadePlataformaRepository;
import br.com.atria.model.DadoIdentificado;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Hub de Conexões: conectar e desconectar plataformas (autorização simulada). */
@Service
public class ConexaoService {
    private final ConexaoRepository conexaoRepository;
    private final IdentidadePlataformaRepository identidadeRepository;
    private final DadoIdentificadoRepository dadoRepository;
    private final PlataformaService plataformaService;
    private final AcessoService acessoService;
    private final LojistaAtualService lojistaAtual;
    private final AuditoriaService auditoria;
    private final ConexaoMapper mapper;
    private final Clock clock;

    public ConexaoService(ConexaoRepository conexaoRepository, IdentidadePlataformaRepository identidadeRepository,
                          DadoIdentificadoRepository dadoRepository, PlataformaService plataformaService,
                          AcessoService acessoService, LojistaAtualService lojistaAtual,
                          AuditoriaService auditoria, ConexaoMapper mapper, Clock clock) {
        this.conexaoRepository = conexaoRepository;
        this.identidadeRepository = identidadeRepository;
        this.dadoRepository = dadoRepository;
        this.plataformaService = plataformaService;
        this.acessoService = acessoService;
        this.lojistaAtual = lojistaAtual;
        this.auditoria = auditoria;
        this.mapper = mapper;
        this.clock = clock;
    }

    public List<ConexaoResponse> listar() {
        Map<Long, Conexao> porPlataforma = conexaoRepository.findByLojistaId(lojistaAtual.id()).stream()
                .collect(Collectors.toMap(Conexao::getPlataformaId, Function.identity()));
        return plataformaService.todas().stream()
                .map(plataforma -> mapper.paraResponse(plataforma, porPlataforma.get(plataforma.getId())))
                .toList();
    }

    /** Os dados identificados só contam enquanto a plataforma está conectada. */
    public ConexaoDetalheResponse detalhe(String slug) {
        Plataforma plataforma = plataformaService.buscarPorSlug(slug);
        Conexao conexao = buscarConexao(plataforma).orElse(null);
        boolean ativa = conexao != null && conexao.isAtiva();

        long clientes = ativa ? identidadeRepository.findByPlataformaId(plataforma.getId()).stream()
                .map(IdentidadePlataforma::getTitularId).distinct().count() : 0;
        Map<TipoDado, Long> contagem = ativa ? dadoRepository.findByPlataformaId(plataforma.getId()).stream()
                .collect(Collectors.groupingBy(DadoIdentificado::getTipoDado, Collectors.counting())) : Map.of();
        List<ContagemResponse> dadosPorTipo = Arrays.stream(TipoDado.values())
                .map(tipo -> new ContagemResponse(tipo.name(), tipo.getDescricao(), contagem.getOrDefault(tipo, 0L)))
                .toList();

        return new ConexaoDetalheResponse(mapper.paraResponse(plataforma, conexao), clientes, dadosPorTipo,
                conexao == null ? List.of() : acessoService.listarPorConexao(conexao));
    }

    public ConexaoResponse conectar(String slug) {
        Plataforma plataforma = plataformaService.buscarPorSlug(slug);
        Conexao conexao = buscarConexao(plataforma).orElseGet(() -> new Conexao(lojistaAtual.id(), plataforma.getId()));
        if (conexao.isAtiva()) {
            throw new RegraNegocioException(plataforma.getNome() + " já está conectada.");
        }
        conexao.conectar(LocalDateTime.now(clock));
        conexaoRepository.save(conexao);
        auditoria.registrar(AcaoAuditoria.CONEXAO_CRIADA, "Conexão com " + plataforma.getNome()
                + " autorizada (" + String.join(", ", plataforma.getEscopos()) + ").", "Conexao", conexao.getId());
        return mapper.paraResponse(plataforma, conexao);
    }

    public void desconectar(String slug) {
        Plataforma plataforma = plataformaService.buscarPorSlug(slug);
        Conexao conexao = buscarConexao(plataforma).filter(Conexao::isAtiva)
                .orElseThrow(() -> new RegraNegocioException(plataforma.getNome() + " não está conectada."));
        conexao.desconectar(LocalDateTime.now(clock));
        conexaoRepository.save(conexao);
        auditoria.registrar(AcaoAuditoria.CONEXAO_REVOGADA, "Conexão com " + plataforma.getNome() + " desfeita.",
                "Conexao", conexao.getId());
    }

    public Set<Long> idsPlataformasConectadas() {
        return conexaoRepository.findByLojistaId(lojistaAtual.id()).stream()
                .filter(Conexao::isAtiva)
                .map(Conexao::getPlataformaId)
                .collect(Collectors.toSet());
    }

    private Optional<Conexao> buscarConexao(Plataforma plataforma) {
        return conexaoRepository.findByLojistaIdAndPlataformaId(lojistaAtual.id(), plataforma.getId());
    }
}
