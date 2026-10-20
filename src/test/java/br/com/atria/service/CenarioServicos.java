package br.com.atria.service;

import br.com.atria.mapper.AcessoMapper;
import br.com.atria.mapper.AlertaMapper;
import br.com.atria.mapper.AuditoriaMapper;
import br.com.atria.mapper.ConexaoMapper;
import br.com.atria.mapper.EvidenciaMapper;
import br.com.atria.mapper.InteracaoMapper;
import br.com.atria.mapper.PlataformaMapper;
import br.com.atria.mapper.SolicitacaoMapper;
import br.com.atria.mapper.TitularMapper;
import br.com.atria.model.Acesso;
import br.com.atria.model.Conexao;
import br.com.atria.model.DadoIdentificado;
import br.com.atria.model.IdentidadePlataforma;
import br.com.atria.model.Lojista;
import br.com.atria.model.PendenciaCanal;
import br.com.atria.model.Plataforma;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.Titular;
import br.com.atria.model.Usuario;
import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.MetodoVinculo;
import br.com.atria.model.enums.PapelUsuario;
import br.com.atria.model.enums.TipoDado;
import br.com.atria.model.enums.TipoPendencia;
import br.com.atria.model.enums.TipoPlataforma;
import br.com.atria.model.enums.TipoSolicitacao;
import br.com.atria.relatorio.GeradorComprovantePdf;
import br.com.atria.repository.memoria.AcessoRepositoryMemoria;
import br.com.atria.repository.memoria.AlertaRepositoryMemoria;
import br.com.atria.repository.memoria.ConexaoRepositoryMemoria;
import br.com.atria.repository.memoria.DadoIdentificadoRepositoryMemoria;
import br.com.atria.repository.memoria.EvidenciaRepositoryMemoria;
import br.com.atria.repository.memoria.IdentidadePlataformaRepositoryMemoria;
import br.com.atria.repository.memoria.InteracaoRepositoryMemoria;
import br.com.atria.repository.memoria.LojistaRepositoryMemoria;
import br.com.atria.repository.memoria.PendenciaCanalRepositoryMemoria;
import br.com.atria.repository.memoria.PlataformaRepositoryMemoria;
import br.com.atria.repository.memoria.RegistroAuditoriaRepositoryMemoria;
import br.com.atria.repository.memoria.SolicitacaoLgpdRepositoryMemoria;
import br.com.atria.repository.memoria.TitularRepositoryMemoria;
import br.com.atria.repository.memoria.UsuarioRepositoryMemoria;
import br.com.atria.service.regra.AnonimizadorTitular;
import br.com.atria.service.regra.CalculadoraPrazo;
import br.com.atria.service.regra.ClassificadorPrioridadePorRegras;
import br.com.atria.service.regra.GeradorFraseResumo;
import br.com.atria.service.regra.GeradorHashComprovante;
import br.com.atria.service.regra.GeradorProtocolo;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static br.com.atria.RelogioFixo.AGORA;
import static br.com.atria.RelogioFixo.CLOCK;
import static br.com.atria.RelogioFixo.PROPRIEDADES;

/** Monta os services reais sobre repositórios em memória vazios, sem Spring, para testes de regra. */
class CenarioServicos {
    final Clock clock = CLOCK;
    final LojistaRepositoryMemoria lojistas = new LojistaRepositoryMemoria();
    final PlataformaRepositoryMemoria plataformas = new PlataformaRepositoryMemoria();
    final ConexaoRepositoryMemoria conexoes = new ConexaoRepositoryMemoria();
    final UsuarioRepositoryMemoria usuarios = new UsuarioRepositoryMemoria();
    final AcessoRepositoryMemoria acessos = new AcessoRepositoryMemoria();
    final TitularRepositoryMemoria titulares = new TitularRepositoryMemoria();
    final IdentidadePlataformaRepositoryMemoria identidades = new IdentidadePlataformaRepositoryMemoria();
    final DadoIdentificadoRepositoryMemoria dados = new DadoIdentificadoRepositoryMemoria();
    final InteracaoRepositoryMemoria interacoes = new InteracaoRepositoryMemoria();
    final SolicitacaoLgpdRepositoryMemoria solicitacoes = new SolicitacaoLgpdRepositoryMemoria();
    final EvidenciaRepositoryMemoria evidencias = new EvidenciaRepositoryMemoria();
    final PendenciaCanalRepositoryMemoria pendencias = new PendenciaCanalRepositoryMemoria();
    final AlertaRepositoryMemoria alertas = new AlertaRepositoryMemoria();
    final RegistroAuditoriaRepositoryMemoria auditoria = new RegistroAuditoriaRepositoryMemoria();

    final CalculadoraPrazo calculadoraPrazo = new CalculadoraPrazo(clock, PROPRIEDADES);
    final LojistaAtualService lojistaAtual = new LojistaAtualService(PROPRIEDADES, lojistas);
    final AuditoriaService auditoriaService = new AuditoriaService(auditoria, lojistaAtual, new AuditoriaMapper(), clock);
    final PlataformaService plataformaService = new PlataformaService(plataformas, new PlataformaMapper());
    final ResolucaoAlertaService resolucaoAlerta = new ResolucaoAlertaService(alertas, clock);
    final AcessoService acessoService = new AcessoService(acessos, usuarios, conexoes, plataformaService, lojistaAtual,
            resolucaoAlerta, auditoriaService, new AcessoMapper(), clock);
    final ConexaoService conexaoService = new ConexaoService(conexoes, identidades, dados, plataformaService, acessoService,
            lojistaAtual, auditoriaService, new ConexaoMapper(new PlataformaMapper()), clock);
    final SolicitacaoMapper solicitacaoMapper = new SolicitacaoMapper(calculadoraPrazo);
    final TitularService titularService = new TitularService(titulares, identidades, dados, interacoes, solicitacoes,
            plataformaService, lojistaAtual, new TitularMapper(), new InteracaoMapper(), solicitacaoMapper);
    final EvidenciaService evidenciaService = new EvidenciaService(evidencias, solicitacoes, titularService, lojistaAtual,
            new GeradorHashComprovante(), new GeradorComprovantePdf(), auditoriaService, new EvidenciaMapper(), clock);
    final SolicitacaoService solicitacaoService = new SolicitacaoService(solicitacoes, interacoes, titularService,
            evidenciaService, resolucaoAlerta, auditoriaService, calculadoraPrazo, new GeradorProtocolo(), solicitacaoMapper, clock);
    final ExclusaoService exclusaoService = new ExclusaoService(titularService, solicitacaoService, identidades, dados,
            new AnonimizadorTitular(), auditoriaService, clock);
    final AlertaService alertaService = new AlertaService(alertas, pendencias, identidades, solicitacaoService, acessoService,
            conexaoService, plataformaService, titularService,
            new FabricaAlertas(new ClassificadorPrioridadePorRegras(), calculadoraPrazo), new GeradorFraseResumo(),
            calculadoraPrazo, auditoriaService, lojistaAtual, new AlertaMapper(), clock);

    final Lojista lojista = lojistas.save(new Lojista("Loja Teste", "loja-teste", "Moda", "PE", "Recife", AGORA.toLocalDate()));

    Plataforma plataforma(String slug, String nome, TipoPlataforma tipo, boolean conectada) {
        Plataforma plataforma = plataformas.save(new Plataforma(slug, nome, tipo, List.of("Ler pedidos")));
        if (conectada) {
            Conexao conexao = new Conexao(lojista.getId(), plataforma.getId());
            conexao.conectar(AGORA.minusDays(30));
            conexoes.save(conexao);
        }
        return plataforma;
    }

    Titular titular(String nome, String cpf, String email) {
        return titulares.save(new Titular(lojista.getId(), nome, cpf, email, "(81) 98888-0000", "50000-000", AGORA.minusMonths(6)));
    }

    void vincular(Titular titular, Plataforma plataforma, TipoDado... tipos) {
        identidades.save(new IdentidadePlataforma(titular.getId(), plataforma.getId(), "id-" + titular.getId(), MetodoVinculo.EMAIL, AGORA.minusDays(10)));
        for (TipoDado tipo : tipos) {
            dados.save(new DadoIdentificado(titular.getId(), plataforma.getId(), tipo, AGORA.minusDays(10)));
        }
    }

    PendenciaCanal pendencia(Titular titular, Plataforma plataforma, TipoPendencia tipo, LocalDateTime desde) {
        return pendencias.save(new PendenciaCanal(lojista.getId(), plataforma.getId(), titular.getId(), tipo, desde));
    }

    Acesso acessoDeExFuncionario(Plataforma plataforma) {
        Usuario usuario = usuarios.save(new Usuario(lojista.getId(), "Ex Funcionária", "ex@exemplo.com", PapelUsuario.ATENDENTE,
                AGORA.toLocalDate().minusYears(1), AGORA.toLocalDate().minusDays(5)));
        Conexao conexao = conexoes.findByLojistaIdAndPlataformaId(lojista.getId(), plataforma.getId()).orElseThrow();
        return acessos.save(new Acesso(usuario.getId(), conexao.getId(), AGORA.minusMonths(6)));
    }

    SolicitacaoLgpd solicitacao(Titular titular, TipoSolicitacao tipo, int diasAtras) {
        LocalDateTime abertura = AGORA.minusDays(diasAtras);
        return solicitacoes.save(new SolicitacaoLgpd(titular.getId(), tipo, CanalOrigem.WHATSAPP, abertura,
                calculadoraPrazo.calcularPrazo(abertura), "ATR-" + String.format("%08d", diasAtras)));
    }
}
