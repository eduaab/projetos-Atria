package br.com.atria.seed;

import br.com.atria.model.Acesso;
import br.com.atria.model.Alerta;
import br.com.atria.model.Conexao;
import br.com.atria.model.DadoIdentificado;
import br.com.atria.model.Evidencia;
import br.com.atria.model.IdentidadePlataforma;
import br.com.atria.model.Interacao;
import br.com.atria.model.Lojista;
import br.com.atria.model.PendenciaCanal;
import br.com.atria.model.Plataforma;
import br.com.atria.model.RegistroAuditoria;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.Titular;
import br.com.atria.model.Usuario;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.MetodoVinculo;
import br.com.atria.model.enums.PapelUsuario;
import br.com.atria.model.enums.StatusSolicitacao;
import br.com.atria.model.enums.TipoDado;
import br.com.atria.model.enums.TipoInteracao;
import br.com.atria.model.enums.TipoPendencia;
import br.com.atria.model.enums.TipoPlataforma;
import br.com.atria.model.enums.TipoSolicitacao;
import br.com.atria.repository.AcessoRepository;
import br.com.atria.repository.AlertaRepository;
import br.com.atria.repository.ConexaoRepository;
import br.com.atria.repository.DadoIdentificadoRepository;
import br.com.atria.repository.EvidenciaRepository;
import br.com.atria.repository.IdentidadePlataformaRepository;
import br.com.atria.repository.InteracaoRepository;
import br.com.atria.repository.LojistaRepository;
import br.com.atria.repository.PendenciaCanalRepository;
import br.com.atria.repository.PlataformaRepository;
import br.com.atria.repository.RegistroAuditoriaRepository;
import br.com.atria.repository.SolicitacaoLgpdRepository;
import br.com.atria.repository.TitularRepository;
import br.com.atria.repository.UsuarioRepository;
import br.com.atria.service.AlertaService;
import br.com.atria.service.FabricaAlertas;
import br.com.atria.service.regra.CalculadoraPrazo;
import br.com.atria.service.regra.GeradorHashComprovante;
import br.com.atria.service.regra.GeradorProtocolo;
import br.com.atria.service.regra.MascaradorCpf;
import br.com.atria.service.regra.NormalizadorTexto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Popula os repositórios em memória com a loja fictícia "Flor de Lis Moda". Todas as datas são
 * relativas ao {@link Clock}, então os prazos e alertas fazem sentido em qualquer dia da apresentação.
 */
@Component
public class CargaDadosFicticios implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(CargaDadosFicticios.class);
    private static final List<String> CONECTADAS_NO_INICIO = List.of("whatsapp", "instagram", "nuvemshop", "mercado-livre");
    private static final Set<String> COMERCIAIS = Set.of("nuvemshop", "mercado-livre", "shopee", "shein", "tiktok-shop");

    private static final List<TitularFicticio> TITULARES = List.of(
            TitularFicticio.de("Roberto Lucas Almeida", "whatsapp instagram tiktok-shop nuvemshop"),
            TitularFicticio.de("Darci Henrique Ayres", "instagram mercado-livre shopee"),
            TitularFicticio.de("Mariana Souza Lima", "instagram mercado-livre"),
            TitularFicticio.de("Carlos Henrique Melo", "whatsapp tiktok-shop"),
            TitularFicticio.de("Fernanda Oliveira Castro", "nuvemshop whatsapp instagram mercado-livre"),
            TitularFicticio.de("Juliana Pereira Barros", "instagram nuvemshop"),
            TitularFicticio.de("Larissa Gomes Teixeira", "whatsapp"),
            TitularFicticio.de("Beatriz Cavalcanti Rocha", "mercado-livre shopee shein"),
            TitularFicticio.de("Renata Moura Farias", "nuvemshop"),
            TitularFicticio.de("Aline Ribeiro Duarte", "instagram whatsapp"),
            TitularFicticio.de("Gabriela Nunes Pacheco", "shein instagram"),
            TitularFicticio.de("Vanessa Lopes Arruda", "mercado-livre"),
            TitularFicticio.de("Thiago Martins Siqueira", "whatsapp mercado-livre"),
            TitularFicticio.de("Luana Freitas Bezerra", "instagram nuvemshop whatsapp"),
            TitularFicticio.de("Débora Campos Lins", "tiktok-shop instagram"),
            TitularFicticio.de("Priscila Andrade Moraes", "nuvemshop"),
            TitularFicticio.de("Rafaela Costa Brandão", "whatsapp instagram"),
            TitularFicticio.de("Isabela Vieira Montenegro", "shopee"),
            TitularFicticio.de("Natália Barbosa Queiroz", "mercado-livre nuvemshop"),
            TitularFicticio.de("Letícia Araújo Cordeiro", "instagram"),
            TitularFicticio.de("Helena Dias Carvalho", "whatsapp nuvemshop instagram mercado-livre shein"),
            TitularFicticio.de("Tatiane Correia Maciel", "nuvemshop whatsapp"),
            TitularFicticio.de("Paulo Roberto Sales", "mercado-livre"),
            TitularFicticio.de("Sabrina Melo Guimarães", "instagram tiktok-shop"),
            TitularFicticio.de("Cláudia Ramos Pimentel", "whatsapp"),
            TitularFicticio.de("Ana Beatriz Souza", "instagram nuvemshop"),
            TitularFicticio.de("Ana B. Souza", "whatsapp"),
            TitularFicticio.de("Marcos Vinícius Lima", "mercado-livre nuvemshop"),
            TitularFicticio.de("Marcos V. Lima", "whatsapp").comEmail("mv.lima@exemplo.com").comTelefoneDe(27),
            TitularFicticio.de("Eduarda Fonseca Leal", "instagram whatsapp"));

    private final LojistaRepository lojistaRepository;
    private final PlataformaRepository plataformaRepository;
    private final ConexaoRepository conexaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AcessoRepository acessoRepository;
    private final TitularRepository titularRepository;
    private final IdentidadePlataformaRepository identidadeRepository;
    private final DadoIdentificadoRepository dadoRepository;
    private final InteracaoRepository interacaoRepository;
    private final SolicitacaoLgpdRepository solicitacaoRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final PendenciaCanalRepository pendenciaRepository;
    private final AlertaRepository alertaRepository;
    private final RegistroAuditoriaRepository auditoriaRepository;
    private final AlertaService alertaService;
    private final FabricaAlertas fabricaAlertas;
    private final CalculadoraPrazo calculadoraPrazo;
    private final GeradorProtocolo geradorProtocolo;
    private final GeradorHashComprovante geradorHash;
    private final Clock clock;

    private final Random aleatorio = new Random(42);

    public CargaDadosFicticios(LojistaRepository lojistaRepository, PlataformaRepository plataformaRepository,
                               ConexaoRepository conexaoRepository, UsuarioRepository usuarioRepository,
                               AcessoRepository acessoRepository, TitularRepository titularRepository,
                               IdentidadePlataformaRepository identidadeRepository, DadoIdentificadoRepository dadoRepository,
                               InteracaoRepository interacaoRepository, SolicitacaoLgpdRepository solicitacaoRepository,
                               EvidenciaRepository evidenciaRepository, PendenciaCanalRepository pendenciaRepository,
                               AlertaRepository alertaRepository, RegistroAuditoriaRepository auditoriaRepository,
                               AlertaService alertaService, FabricaAlertas fabricaAlertas, CalculadoraPrazo calculadoraPrazo,
                               GeradorProtocolo geradorProtocolo, GeradorHashComprovante geradorHash, Clock clock) {
        this.lojistaRepository = lojistaRepository;
        this.plataformaRepository = plataformaRepository;
        this.conexaoRepository = conexaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.acessoRepository = acessoRepository;
        this.titularRepository = titularRepository;
        this.identidadeRepository = identidadeRepository;
        this.dadoRepository = dadoRepository;
        this.interacaoRepository = interacaoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.pendenciaRepository = pendenciaRepository;
        this.alertaRepository = alertaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.alertaService = alertaService;
        this.fabricaAlertas = fabricaAlertas;
        this.calculadoraPrazo = calculadoraPrazo;
        this.geradorProtocolo = geradorProtocolo;
        this.geradorHash = geradorHash;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (lojistaRepository.count() > 0) return;
        LocalDateTime agora = LocalDateTime.now(clock);

        Lojista lojista = lojistaRepository.save(new Lojista("Flor de Lis Moda", "flor-de-lis-moda", "Moda feminina",
                "PE", "Recife", agora.toLocalDate().minusMonths(8)));
        Map<String, Plataforma> plataformas = criarPlataformas();
        Map<String, Conexao> conexoes = criarConexoes(lojista, plataformas, agora);
        criarUsuariosEAcessos(lojista, conexoes, agora);
        List<Titular> titulares = criarTitulares(lojista, plataformas, agora);
        criarInteracoes(titulares, plataformas, agora);
        criarSolicitacoes(titulares, plataformas, agora);
        criarPendenciasEHistoricoDeAlertas(lojista, titulares, plataformas, agora);
        criarAuditoriaAntiga(lojista, conexoes, plataformas, agora);

        alertaService.executarVarredura();
        LOG.info("Dados fictícios carregados: {} titulares, {} alertas.", titularRepository.count(), alertaRepository.count());
    }

    private Map<String, Plataforma> criarPlataformas() {
        Map<String, Plataforma> plataformas = new LinkedHashMap<>();
        List.of(
                new Plataforma("whatsapp", "WhatsApp", TipoPlataforma.MENSAGERIA,
                        List.of("Ler mensagens recebidas", "Ver contatos dos clientes", "Ver status de atendimento")),
                new Plataforma("instagram", "Instagram", TipoPlataforma.REDE_SOCIAL,
                        List.of("Ler mensagens diretas", "Ler comentários das publicações", "Ver perfil de quem compra")),
                new Plataforma("nuvemshop", "Nuvemshop", TipoPlataforma.LOJA_PROPRIA,
                        List.of("Ler pedidos", "Ler cadastro de clientes", "Ler carrinhos abandonados")),
                new Plataforma("mercado-livre", "Mercado Livre", TipoPlataforma.MARKETPLACE,
                        List.of("Ler pedidos", "Ler perguntas e reclamações", "Ver dados de envio")),
                new Plataforma("shopee", "Shopee", TipoPlataforma.MARKETPLACE,
                        List.of("Ler pedidos", "Ler conversas com compradores", "Ver dados de envio")),
                new Plataforma("shein", "Shein", TipoPlataforma.MARKETPLACE,
                        List.of("Ler pedidos", "Ver avaliações de clientes")),
                new Plataforma("tiktok-shop", "TikTok Shop", TipoPlataforma.MARKETPLACE,
                        List.of("Ler pedidos", "Ler mensagens de compradores", "Ver dados de envio"))
        ).forEach(p -> plataformas.put(p.getSlug(), plataformaRepository.save(p)));
        return plataformas;
    }

    private Map<String, Conexao> criarConexoes(Lojista lojista, Map<String, Plataforma> plataformas, LocalDateTime agora) {
        Map<String, Conexao> conexoes = new LinkedHashMap<>();
        int diasAtras = 230;
        for (String slug : CONECTADAS_NO_INICIO) {
            Conexao conexao = new Conexao(lojista.getId(), plataformas.get(slug).getId());
            conexao.conectar(agora.minusDays(diasAtras).withHour(10));
            conexao.sincronizar(agora.minusMinutes(5 + diasAtras % 40));
            conexoes.put(slug, conexaoRepository.save(conexao));
            diasAtras -= 45;
        }
        return conexoes;
    }

    private void criarUsuariosEAcessos(Lojista lojista, Map<String, Conexao> conexoes, LocalDateTime agora) {
        LocalDateTime inicio = agora.minusMonths(8);
        Usuario dona = salvarUsuario(lojista, "Juliana Ferraz", "juliana@flordelis.exemplo.com", PapelUsuario.DONO, inicio, null);
        Usuario atendente = salvarUsuario(lojista, "Camila Rocha", "camila@flordelis.exemplo.com", PapelUsuario.ATENDENTE, inicio.plusMonths(1), null);
        Usuario estoquista = salvarUsuario(lojista, "Diego Santos", "diego@flordelis.exemplo.com", PapelUsuario.ESTOQUISTA, inicio.plusMonths(2), null);
        Usuario desligada = salvarUsuario(lojista, "Patrícia Lima", "patricia@flordelis.exemplo.com", PapelUsuario.ATENDENTE,
                inicio.plusMonths(1), agora.minusDays(12));

        conexoes.values().forEach(c -> conceder(dona, c, c.getConectadaEm()));
        conceder(atendente, conexoes.get("whatsapp"), agora.minusMonths(7));
        conceder(atendente, conexoes.get("instagram"), agora.minusMonths(7));
        conceder(estoquista, conexoes.get("nuvemshop"), agora.minusMonths(6));
        conceder(estoquista, conexoes.get("mercado-livre"), agora.minusMonths(6));
        conceder(desligada, conexoes.get("whatsapp"), agora.minusMonths(7));
        Acesso revogado = conceder(desligada, conexoes.get("instagram"), agora.minusMonths(7));
        revogado.revogar(agora.minusDays(12).withHour(18));
        acessoRepository.save(revogado);
    }

    private Usuario salvarUsuario(Lojista lojista, String nome, String email, PapelUsuario papel,
                                  LocalDateTime admissao, LocalDateTime desligamento) {
        return usuarioRepository.save(new Usuario(lojista.getId(), nome, email, papel, admissao.toLocalDate(),
                desligamento == null ? null : desligamento.toLocalDate()));
    }

    private Acesso conceder(Usuario usuario, Conexao conexao, LocalDateTime quando) {
        return acessoRepository.save(new Acesso(usuario.getId(), conexao.getId(), quando));
    }

    private List<Titular> criarTitulares(Lojista lojista, Map<String, Plataforma> plataformas, LocalDateTime agora) {
        List<Titular> titulares = new ArrayList<>();
        for (int i = 0; i < TITULARES.size(); i++) {
            TitularFicticio molde = TITULARES.get(i);
            int indiceTelefone = molde.mesmoTelefoneDe() >= 0 ? molde.mesmoTelefoneDe() : i;
            Titular titular = titularRepository.save(new Titular(lojista.getId(), molde.nome(),
                    CpfFicticio.aPartirDe(123_456_789L + i * 7_919_731L),
                    molde.emailFixo() != null ? molde.emailFixo() : emailDe(molde.nome()),
                    telefoneDe(indiceTelefone), String.format("5%04d-%03d", (1000 + i * 311) % 10000, (i * 97) % 1000),
                    agora.minusDays(30 + aleatorio.nextInt(380))));
            titulares.add(titular);
            if (!molde.nome().equals("Paulo Roberto Sales")) {
                molde.plataformas().forEach(slug -> identificar(titular, plataformas.get(slug), agora));
            }
        }
        return titulares;
    }

    private void identificar(Titular titular, Plataforma plataforma, LocalDateTime agora) {
        LocalDateTime quando = agora.minusDays(1 + aleatorio.nextInt(200));
        String primeiroNome = NormalizadorTexto.normalizar(titular.getNome().split(" ")[0]);
        String sobrenome = NormalizadorTexto.normalizar(titular.getNome().substring(titular.getNome().lastIndexOf(' ') + 1));
        switch (plataforma.getSlug()) {
            case "whatsapp" -> {
                vincular(titular, plataforma, titular.getTelefone(), MetodoVinculo.TELEFONE, quando, TipoDado.NOME, TipoDado.TELEFONE);
                if (titular.getId() % 6 == 1) registrarDado(titular, plataforma, TipoDado.CPF, quando);
            }
            case "instagram" -> {
                vincular(titular, plataforma, "@" + primeiroNome + "." + sobrenome, MetodoVinculo.MANUAL, quando, TipoDado.NOME);
                if (titular.getId() % 9 == 2) registrarDado(titular, plataforma, TipoDado.CPF, quando);
            }
            case "nuvemshop" -> vincular(titular, plataforma, titular.getEmail(), MetodoVinculo.EMAIL, quando,
                    TipoDado.NOME, TipoDado.CPF, TipoDado.EMAIL, TipoDado.TELEFONE, TipoDado.ENDERECO);
            case "mercado-livre" -> vincular(titular, plataforma, "ML-" + (40_000_000 + titular.getId() * 7_331), MetodoVinculo.CPF, quando,
                    TipoDado.NOME, TipoDado.CPF, TipoDado.TELEFONE, TipoDado.ENDERECO);
            case "shopee" -> vincular(titular, plataforma, "SHP-" + (8_100_000 + titular.getId() * 913), MetodoVinculo.CPF, quando,
                    TipoDado.NOME, TipoDado.CPF, TipoDado.ENDERECO);
            case "shein" -> vincular(titular, plataforma, titular.getEmail(), MetodoVinculo.EMAIL, quando,
                    TipoDado.NOME, TipoDado.EMAIL, TipoDado.ENDERECO);
            default -> vincular(titular, plataforma, "@" + primeiroNome + "_" + sobrenome, MetodoVinculo.MANUAL, quando, TipoDado.NOME);
        }
    }

    private void vincular(Titular titular, Plataforma plataforma, String idExterno, MetodoVinculo metodo,
                          LocalDateTime quando, TipoDado... tipos) {
        identidadeRepository.save(new IdentidadePlataforma(titular.getId(), plataforma.getId(), idExterno, metodo, quando));
        for (TipoDado tipo : tipos) {
            registrarDado(titular, plataforma, tipo, quando);
        }
    }

    private void registrarDado(Titular titular, Plataforma plataforma, TipoDado tipo, LocalDateTime quando) {
        dadoRepository.save(new DadoIdentificado(titular.getId(), plataforma.getId(), tipo, quando));
    }

    private void criarInteracoes(List<Titular> titulares, Map<String, Plataforma> plataformas, LocalDateTime agora) {
        for (int i = 0; i < titulares.size(); i++) {
            Titular titular = titulares.get(i);
            List<String> slugs = TITULARES.get(i).plataformas();
            List<String> lojas = slugs.stream().filter(COMERCIAIS::contains).toList();
            List<String> canaisDeVenda = lojas.isEmpty() ? slugs : lojas;

            int compras = 1 + aleatorio.nextInt(5);
            for (int c = 0; c < compras; c++) {
                BigDecimal valor = BigDecimal.valueOf(59.90 + aleatorio.nextInt(40) * 10).setScale(2, RoundingMode.HALF_UP);
                interagir(titular, plataformas.get(sortear(canaisDeVenda)), TipoInteracao.COMPRA_FINALIZADA, agora.minusDays(3 + aleatorio.nextInt(360)), valor);
            }
            if (aleatorio.nextInt(3) == 0) {
                interagir(titular, plataformas.get(sortear(canaisDeVenda)), TipoInteracao.CARRINHO_ABANDONADO, agora.minusDays(1 + aleatorio.nextInt(90)), null);
            }
            slugs.stream().filter(s -> s.equals("whatsapp") || s.equals("instagram")).forEach(s ->
                    interagir(titular, plataformas.get(s), TipoInteracao.MENSAGEM, agora.minusDays(aleatorio.nextInt(120)).minusHours(aleatorio.nextInt(12)), null));
            if (aleatorio.nextInt(6) == 0) {
                interagir(titular, plataformas.get(sortear(slugs)), TipoInteracao.RECLAMACAO, agora.minusDays(20 + aleatorio.nextInt(200)), null);
            }
        }
    }

    private void criarSolicitacoes(List<Titular> titulares, Map<String, Plataforma> plataformas, LocalDateTime agora) {
        abrirSolicitacao(titulares.get(0), TipoSolicitacao.EXCLUSAO, CanalOrigem.INSTAGRAM, agora.minusDays(11), StatusSolicitacao.PENDENTE);
        abrirSolicitacao(titulares.get(1), TipoSolicitacao.ACESSO, CanalOrigem.WHATSAPP, agora.minusDays(14), StatusSolicitacao.EM_ANDAMENTO);
        abrirSolicitacao(titulares.get(12), TipoSolicitacao.CORRECAO, CanalOrigem.EMAIL, agora.minusDays(18), StatusSolicitacao.PENDENTE);
        abrirSolicitacao(titulares.get(18), TipoSolicitacao.EXCLUSAO, CanalOrigem.MARKETPLACE, agora.minusHours(2), StatusSolicitacao.PENDENTE);
        abrirSolicitacao(titulares.get(10), TipoSolicitacao.PORTABILIDADE, CanalOrigem.EMAIL, agora.minusDays(6), StatusSolicitacao.PENDENTE);
        abrirSolicitacao(titulares.get(16), TipoSolicitacao.EXCLUSAO, CanalOrigem.TELEFONE, agora.minusDays(9), StatusSolicitacao.EM_ANDAMENTO);

        Titular mariana = titulares.get(2);
        SolicitacaoLgpd acesso = abrirSolicitacao(mariana, TipoSolicitacao.ACESSO, CanalOrigem.INSTAGRAM, agora.minusDays(60), StatusSolicitacao.PENDENTE);
        concluirComEvidencia(acesso, MascaradorCpf.mascarar(mariana.getCpf()), List.of("Instagram", "Mercado Livre"), agora.minusDays(52));

        Titular paulo = titulares.get(22);
        SolicitacaoLgpd exclusao = abrirSolicitacao(paulo, TipoSolicitacao.EXCLUSAO, CanalOrigem.WHATSAPP, agora.minusDays(40), StatusSolicitacao.PENDENTE);
        LocalDateTime conclusao = agora.minusDays(30);
        String cpfMascarado = MascaradorCpf.mascarar(paulo.getCpf());
        paulo.anonimizar(conclusao);
        titularRepository.save(paulo);
        concluirComEvidencia(exclusao, cpfMascarado, List.of(plataformas.get("mercado-livre").getNome()), conclusao);
    }

    private SolicitacaoLgpd abrirSolicitacao(Titular titular, TipoSolicitacao tipo, CanalOrigem canal,
                                             LocalDateTime abertaEm, StatusSolicitacao status) {
        SolicitacaoLgpd solicitacao = new SolicitacaoLgpd(titular.getId(), tipo, canal, abertaEm,
                calculadoraPrazo.calcularPrazo(abertaEm), geradorProtocolo.gerar());
        solicitacao.alterarStatus(status);
        interagir(titular, null, TipoInteracao.SOLICITACAO_LGPD, abertaEm, null);
        return solicitacaoRepository.save(solicitacao);
    }

    private void concluirComEvidencia(SolicitacaoLgpd solicitacao, String cpfMascarado, List<String> plataformas, LocalDateTime quando) {
        solicitacao.concluir(quando);
        solicitacaoRepository.save(solicitacao);
        String hash = geradorHash.gerar(solicitacao.getProtocolo(), solicitacao.getTipo().name(), cpfMascarado,
                String.join(",", plataformas), quando.toString());
        evidenciaRepository.save(new Evidencia(solicitacao.getId(), quando, cpfMascarado, plataformas, hash, solicitacao.getProtocolo()));
    }

    private void criarPendenciasEHistoricoDeAlertas(Lojista lojista, List<Titular> titulares,
                                                    Map<String, Plataforma> plataformas, LocalDateTime agora) {
        pendencia(lojista, titulares.get(9), plataformas.get("instagram"), TipoPendencia.MENSAGEM_SEM_RESPOSTA, agora.minusHours(6));
        pendencia(lojista, titulares.get(19), plataformas.get("instagram"), TipoPendencia.MENSAGEM_SEM_RESPOSTA, agora.minusHours(2));
        pendencia(lojista, titulares.get(6), plataformas.get("whatsapp"), TipoPendencia.RECLAMACAO_PENDENTE, agora.minusHours(30));
        pendencia(lojista, titulares.get(24), plataformas.get("whatsapp"), TipoPendencia.MENSAGEM_SEM_RESPOSTA, agora.minusHours(5));
        pendencia(lojista, titulares.get(2), plataformas.get("mercado-livre"), TipoPendencia.PEDIDO_NAO_ENVIADO, agora.minusDays(2));
        pendencia(lojista, titulares.get(11), plataformas.get("mercado-livre"), TipoPendencia.PEDIDO_NAO_ENVIADO, agora.minusHours(50));
        pendencia(lojista, titulares.get(27), plataformas.get("mercado-livre"), TipoPendencia.RECLAMACAO_PENDENTE, agora.minusHours(10));
        pendencia(lojista, titulares.get(15), plataformas.get("nuvemshop"), TipoPendencia.PEDIDO_NAO_ENVIADO, agora.minusDays(3));
        pendencia(lojista, titulares.get(5), plataformas.get("nuvemshop"), TipoPendencia.PEDIDO_NAO_ENVIADO, agora.minusHours(20));
        pendencia(lojista, titulares.get(17), plataformas.get("shopee"), TipoPendencia.PEDIDO_NAO_ENVIADO, agora.minusDays(1).minusHours(3));

        historico(lojista, titulares.get(4), plataformas.get("whatsapp"), TipoPendencia.MENSAGEM_SEM_RESPOSTA, agora.minusDays(45), 3);
        historico(lojista, titulares.get(7), plataformas.get("mercado-livre"), TipoPendencia.RECLAMACAO_PENDENTE, agora.minusDays(38), 30);
        historico(lojista, titulares.get(13), plataformas.get("instagram"), TipoPendencia.MENSAGEM_SEM_RESPOSTA, agora.minusDays(30), 2);
        historico(lojista, titulares.get(20), plataformas.get("nuvemshop"), TipoPendencia.PEDIDO_NAO_ENVIADO, agora.minusDays(24), 26);
        historico(lojista, titulares.get(21), plataformas.get("whatsapp"), TipoPendencia.RECLAMACAO_PENDENTE, agora.minusDays(16), 20);
        historico(lojista, titulares.get(29), plataformas.get("instagram"), TipoPendencia.MENSAGEM_SEM_RESPOSTA, agora.minusDays(9), 5);
        historico(lojista, titulares.get(3), plataformas.get("whatsapp"), TipoPendencia.MENSAGEM_SEM_RESPOSTA, agora.minusDays(4), 1);
    }

    private PendenciaCanal pendencia(Lojista lojista, Titular titular, Plataforma plataforma, TipoPendencia tipo, LocalDateTime desde) {
        if (tipo == TipoPendencia.RECLAMACAO_PENDENTE) {
            interagir(titular, plataforma, TipoInteracao.RECLAMACAO, desde, null);
        }
        return pendenciaRepository.save(new PendenciaCanal(lojista.getId(), plataforma.getId(), titular.getId(), tipo, desde));
    }

    /** Pendência antiga já tratada, com o alerta resolvido algumas horas depois, para o histórico de ocorrências. */
    private void historico(Lojista lojista, Titular titular, Plataforma plataforma, TipoPendencia tipo,
                           LocalDateTime desde, int horasAteResolver) {
        PendenciaCanal pendencia = pendencia(lojista, titular, plataforma, tipo, desde);
        LocalDateTime resolvidoEm = desde.plusHours(horasAteResolver);
        Alerta alerta = fabricaAlertas.dePendencia(pendencia, plataforma, titular, resolvidoEm);
        alerta.resolver(resolvidoEm);
        alertaRepository.save(alerta);
        pendencia.resolver();
        pendenciaRepository.save(pendencia);
    }

    private void criarAuditoriaAntiga(Lojista lojista, Map<String, Conexao> conexoes,
                                      Map<String, Plataforma> plataformas, LocalDateTime agora) {
        conexoes.forEach((slug, conexao) -> auditar(lojista, AcaoAuditoria.CONEXAO_CRIADA,
                "Conexão com " + plataformas.get(slug).getNome() + " autorizada.", "Conexao", conexao.getId(), conexao.getConectadaEm()));
        auditar(lojista, AcaoAuditoria.ACESSO_REVOGADO, "Acesso de Patrícia Lima ao Instagram revogado.", "Acesso", null,
                agora.minusDays(12).withHour(18));
        solicitacaoRepository.findAll().stream().filter(s -> !s.isAberta()).forEach(s -> auditar(lojista,
                AcaoAuditoria.SOLICITACAO_CONCLUIDA, "Solicitação " + s.getProtocolo() + " (" + s.getTipo().getDescricao()
                        + ") concluída com comprovante.", "SolicitacaoLgpd", s.getId(), s.getConcluidaEm()));
        auditar(lojista, AcaoAuditoria.RELATORIO_GERADO, "Relatório ESG – Governança (Pilar G) exportado em PDF.", "Relatorio", null,
                agora.minusDays(20).withHour(9));
        alertaRepository.findAll().forEach(a -> auditar(lojista, AcaoAuditoria.ALERTA_STATUS_ALTERADO,
                "Alerta #" + a.getId() + " (" + a.getTipo().getDescricao() + ") passou de Aberto para Resolvido.", "Alerta", a.getId(), a.getResolvidoEm()));
    }

    private void auditar(Lojista lojista, AcaoAuditoria acao, String descricao, String entidade, Long entidadeId, LocalDateTime quando) {
        auditoriaRepository.save(new RegistroAuditoria(lojista.getId(), acao, descricao, entidade, entidadeId, quando));
    }

    private void interagir(Titular titular, Plataforma plataforma, TipoInteracao tipo, LocalDateTime data, BigDecimal valor) {
        interacaoRepository.save(new Interacao(titular.getId(), plataforma == null ? null : plataforma.getId(), tipo, data, valor));
    }

    private String sortear(List<String> opcoes) {
        return opcoes.get(aleatorio.nextInt(opcoes.size()));
    }

    private static String emailDe(String nome) {
        String[] partes = NormalizadorTexto.normalizar(nome).replace(".", "").split(" ");
        return partes[0] + "." + partes[partes.length - 1] + "@exemplo.com";
    }

    private static String telefoneDe(int indice) {
        return String.format("(81) 9%04d-%04d", 8100 + (indice * 37) % 1900, (1234 + indice * 4567) % 10000);
    }
}
