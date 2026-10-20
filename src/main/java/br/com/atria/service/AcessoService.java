package br.com.atria.service;

import br.com.atria.dto.response.AcessoResponse;
import br.com.atria.exception.RecursoNaoEncontradoException;
import br.com.atria.exception.RegraNegocioException;
import br.com.atria.mapper.AcessoMapper;
import br.com.atria.model.Acesso;
import br.com.atria.model.Conexao;
import br.com.atria.model.Plataforma;
import br.com.atria.model.Usuario;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.repository.AcessoRepository;
import br.com.atria.repository.ConexaoRepository;
import br.com.atria.repository.UsuarioRepository;
import br.com.atria.service.regra.FormatoData;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acessos de funcionários às conexões, incluindo a revogação em um clique. */
@Service
public class AcessoService {
    private final AcessoRepository acessoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConexaoRepository conexaoRepository;
    private final PlataformaService plataformaService;
    private final LojistaAtualService lojistaAtual;
    private final ResolucaoAlertaService resolucaoAlerta;
    private final AuditoriaService auditoria;
    private final AcessoMapper mapper;
    private final Clock clock;

    public AcessoService(AcessoRepository acessoRepository, UsuarioRepository usuarioRepository,
                         ConexaoRepository conexaoRepository, PlataformaService plataformaService,
                         LojistaAtualService lojistaAtual, ResolucaoAlertaService resolucaoAlerta,
                         AuditoriaService auditoria, AcessoMapper mapper, Clock clock) {
        this.acessoRepository = acessoRepository;
        this.usuarioRepository = usuarioRepository;
        this.conexaoRepository = conexaoRepository;
        this.plataformaService = plataformaService;
        this.lojistaAtual = lojistaAtual;
        this.resolucaoAlerta = resolucaoAlerta;
        this.auditoria = auditoria;
        this.mapper = mapper;
        this.clock = clock;
    }

    public List<AcessoResponse> listarPorConexao(Conexao conexao) {
        Plataforma plataforma = plataformaService.buscarPorId(conexao.getPlataformaId());
        return acessoRepository.findByConexaoIdIn(List.of(conexao.getId())).stream()
                .map(acesso -> mapper.paraResponse(acesso, usuarioDe(acesso), plataforma))
                .toList();
    }

    /** Acessos ainda ativos de funcionários já desligados. */
    public List<Acesso> acessosIndevidos() {
        return acessosDoLojista().stream()
                .filter(acesso -> acesso.isAtivo() && usuarioDe(acesso).isDesligado())
                .toList();
    }

    public long contarRevogados() {
        return acessosDoLojista().stream().filter(acesso -> !acesso.isAtivo()).count();
    }

    public Optional<Acesso> buscar(Long id) {
        return acessosDoLojista().stream().filter(acesso -> acesso.getId().equals(id)).findFirst();
    }

    public Usuario usuarioDe(Acesso acesso) {
        return usuarioRepository.findById(acesso.getUsuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + acesso.getUsuarioId() + " não encontrado."));
    }

    public Plataforma plataformaDe(Acesso acesso) {
        Conexao conexao = conexaoRepository.findById(acesso.getConexaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conexão " + acesso.getConexaoId() + " não encontrada."));
        return plataformaService.buscarPorId(conexao.getPlataformaId());
    }

    public AcessoResponse revogar(Long id) {
        Acesso acesso = buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Acesso " + id + " não encontrado."));
        if (!acesso.isAtivo()) {
            throw new RegraNegocioException("Este acesso já foi revogado em " + FormatoData.dataHora(acesso.getRevogadoEm()) + ".");
        }
        acesso.revogar(LocalDateTime.now(clock));
        acessoRepository.save(acesso);
        resolucaoAlerta.resolverPorOrigem(OrigemAlerta.ACESSO, id);

        Usuario usuario = usuarioDe(acesso);
        Plataforma plataforma = plataformaDe(acesso);
        auditoria.registrar(AcaoAuditoria.ACESSO_REVOGADO,
                "Acesso de " + usuario.getNome() + " ao " + plataforma.getNome() + " revogado.", "Acesso", id);
        return mapper.paraResponse(acesso, usuario, plataforma);
    }

    private List<Acesso> acessosDoLojista() {
        List<Long> conexoes = conexaoRepository.findByLojistaId(lojistaAtual.id()).stream().map(Conexao::getId).toList();
        return acessoRepository.findByConexaoIdIn(conexoes);
    }
}
