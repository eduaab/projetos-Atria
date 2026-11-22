package br.com.atria.service;

import br.com.atria.dto.response.PainelResponse.*;
import br.com.atria.model.analise.Registros.*;
import br.com.atria.repository.AnaliseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static br.com.atria.service.regra.FormatoPainel.*;

/**
 * Calcula os indicadores do Painel a partir dos dados analíticos (simulados). Todo recorte por período termina na
 * data de referência (o último dia com dados, o "ontem" do Painel) e é comparado ao período anterior de mesmo tamanho.
 */
@Service
public class PainelService {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final List<Integer> PERIODOS = List.of(7, 30, 60, 120, 365, 0);
    private static final int LOJA_ATUAL = 1;
    private static final String[] ROTULO_FAIXA = {"até 15 min", "15–60 min", "1–4 h", "4–24 h", "mais de 24 h"};
    private static final Map<String, String> NOMES = Map.of("nuvemshop", "Nuvemshop", "mercado-livre", "Mercado Livre",
            "shopee", "Shopee", "shein", "Shein", "tiktok-shop", "TikTok Shop", "whatsapp", "WhatsApp", "instagram", "Instagram");

    private final AnaliseRepository dados;
    private final ObjectMapper json;

    public PainelService(AnaliseRepository dados, ObjectMapper json) {
        this.dados = dados;
        this.json = json;
    }

    // ------------------------------------------------------------------ recortes
    private record Janela(LocalDate inicio, LocalDate fim) {
        boolean contem(String iso) {
            LocalDate d = LocalDate.parse(iso);
            return !d.isBefore(inicio) && !d.isAfter(fim);
        }
    }

    /** Período em dias; 0 significa todo o histórico. Valores fora da lista caem no padrão de 30 dias. */
    private int efetivo(int periodo) {
        int p = PERIODOS.contains(periodo) ? periodo : 30;
        return p == 0 ? (int) (dados.dataReferencia().toEpochDay() - primeiroDia().toEpochDay() + 1) : p;
    }

    private LocalDate primeiroDia() {
        return dados.diario().stream().map(l -> LocalDate.parse(l.d())).min(Comparator.naturalOrder()).orElse(dados.dataReferencia());
    }

    private Janela atual(int periodo) {
        LocalDate ref = dados.dataReferencia();
        return new Janela(ref.minusDays(efetivo(periodo) - 1L), ref);
    }

    private Janela anterior(int periodo) {
        Janela a = atual(periodo);
        int dias = efetivo(periodo);
        return new Janela(a.inicio().minusDays(dias), a.inicio().minusDays(1));
    }

    /** Só há comparação quando o período anterior inteiro tem dados. */
    private boolean temAnterior(int periodo) {
        return !anterior(periodo).inicio().isBefore(primeiroDia());
    }

    private String rotuloPeriodo(int periodo) {
        return periodo == 0 ? "todo o histórico" : "os últimos " + efetivo(periodo) + " dias";
    }

    private static boolean todas(String plataforma) {
        return plataforma == null || plataforma.isBlank() || "todas".equals(plataforma);
    }

    private List<LinhaDiaria> vendas(Janela j, String plataforma) {
        return dados.diario().stream().filter(l -> j.contem(l.d()) && (todas(plataforma) || l.p().equals(plataforma))).toList();
    }

    private record Soma(int n, double rec, double luc, int can, int atr, int rcl, int dev) {
        static Soma de(Collection<LinhaDiaria> ls) {
            return new Soma(ls.stream().mapToInt(LinhaDiaria::n).sum(), ls.stream().mapToDouble(LinhaDiaria::rec).sum(),
                    ls.stream().mapToDouble(LinhaDiaria::luc).sum(), ls.stream().mapToInt(LinhaDiaria::can).sum(),
                    ls.stream().mapToInt(LinhaDiaria::atr).sum(), ls.stream().mapToInt(LinhaDiaria::rcl).sum(),
                    ls.stream().mapToInt(LinhaDiaria::dev).sum());
        }

        int vendidos() { return n - can; }
        double ticket() { return divisao(rec, vendidos()); }
        double margem() { return divisao(luc, rec) * 100; }
        double problemas() { return divisao(can + dev, n) * 100; }
    }

    // ------------------------------------------------------------------ meta
    public Meta meta() {
        List<Opcao> plataformas = new ArrayList<>(List.of(new Opcao("todas", "Todas as plataformas")));
        dados.diario().stream().map(LinhaDiaria::p).distinct().sorted().forEach(p -> plataformas.add(new Opcao(p, nome(p))));
        return new Meta(dados.dataReferencia().toString(), primeiroDia().toString(), plataformas, PERIODOS);
    }

    private static String nome(String codigo) {
        return NOMES.getOrDefault(codigo, codigo);
    }

    // ------------------------------------------------------------------ resumo (como foi ontem / período)
    public Resumo resumo(int periodo, String plataforma) {
        LocalDate ref = dados.dataReferencia();
        Janela j = atual(periodo);
        Janela anterior = anterior(periodo);
        boolean comparar = temAnterior(periodo);
        Soma ontem = Soma.de(vendas(new Janela(ref, ref), plataforma));
        List<Soma> mesmoDia = IntStream.rangeClosed(1, 4).mapToObj(s -> ref.minusWeeks(s))
                .map(d -> Soma.de(vendas(new Janela(d, d), plataforma))).toList();
        double mediaRec = mesmoDia.stream().mapToDouble(Soma::rec).average().orElse(0);
        double mediaLuc = mesmoDia.stream().mapToDouble(Soma::luc).average().orElse(0);
        double mediaPed = mesmoDia.stream().mapToDouble(Soma::vendidos).average().orElse(0);
        String dia = ref.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
        String base = "média das últimas 4 " + dia + "s";
        List<Indicador> indOntem = List.of(
                new Indicador("rec", "Receita", ontem.rec(), mediaRec, "moeda", true, base),
                new Indicador("luc", "Lucro líquido", ontem.luc(), mediaLuc, "moeda", true, base),
                new Indicador("ped", "Pedidos", ontem.vendidos(), mediaPed, "numero", true, base));
        Soma s = Soma.de(vendas(j, plataforma));
        Soma a = Soma.de(vendas(anterior, plataforma));
        String rotuloAnt = efetivo(periodo) + " dias anteriores";
        double[] atend = atendimentoPeriodo(j, "todos");
        double[] atendAnt = atendimentoPeriodo(anterior, "todos");
        List<Indicador> indPeriodo = List.of(
                new Indicador("rec", "Receita", s.rec(), comparar ? a.rec() : null, "moeda", true, rotuloAnt),
                new Indicador("luc", "Lucro líquido", s.luc(), comparar ? a.luc() : null, "moeda", true, rotuloAnt),
                new Indicador("mar", "Margem líquida", s.margem(), comparar ? a.margem() : null, "percentual", true, rotuloAnt),
                new Indicador("ped", "Pedidos", (double) s.vendidos(), comparar ? (double) a.vendidos() : null, "numero", true, rotuloAnt),
                new Indicador("tic", "Ticket médio", s.ticket(), comparar ? a.ticket() : null, "moeda", true, rotuloAnt),
                new Indicador("prb", "Cancelados e devolvidos", s.problemas(), comparar ? a.problemas() : null, "percentual", false, rotuloAnt),
                new Indicador("r1h", "Mensagens respondidas em até 1 h", atend[0], comparar ? atendAnt[0] : null, "percentual", true, rotuloAnt),
                new Indicador("sre", "Mensagens sem resposta", atend[1], comparar ? atendAnt[1] : null, "percentual", false, rotuloAnt));
        String onde = todas(plataforma) ? "em todas as plataformas" : "na " + nome(plataforma);
        String leitura = String.format(PT_BR, "Ontem (%s, %s) a loja vendeu %s em %s pedidos %s, %s em relação à %s. Em %s: receita de %s e margem líquida de %s%s.",
                dia, ref.format(DateTimeFormatter.ofPattern("dd/MM")), moeda(ontem.rec()), inteiro(ontem.vendidos()), onde,
                variacaoTexto(ontem.rec(), mediaRec), base, rotuloPeriodo(periodo), moeda(s.rec()), percentual(s.margem()),
                comparar ? String.format(PT_BR, " (receita %s sobre os %d dias anteriores)", variacaoTexto(s.rec(), a.rec()), efetivo(periodo)) : "");
        return new Resumo(ref.toString(), j.inicio().toString(), indOntem, indPeriodo, leitura);
    }

    /** [% respondidas em até 1 h, % sem resposta, tempo médio em minutos] das conversas da janela. */
    private double[] atendimentoPeriodo(Janela j, String canal) {
        List<LinhaAtendimento> ls = conversas(j, canal);
        double n = ls.stream().mapToInt(LinhaAtendimento::n).sum();
        double ate1h = ls.stream().mapToInt(l -> l.f().get(0) + l.f().get(1)).sum();
        double sr = ls.stream().mapToInt(LinhaAtendimento::sr).sum();
        double respondidas = ls.stream().mapToInt(l -> l.f().stream().mapToInt(Integer::intValue).sum()).sum();
        double minutos = ls.stream().mapToDouble(LinhaAtendimento::mi).sum();
        return new double[]{divisao(ate1h, n) * 100, divisao(sr, n) * 100, divisao(minutos, respondidas)};
    }

    private List<LinhaAtendimento> conversas(Janela j, String canal) {
        return dados.atendimento().stream().filter(l -> j.contem(l.d()) && (todas(canal) || "todos".equals(canal) || l.c().equals(canal))).toList();
    }

    // ------------------------------------------------------------------ série
    /** Granularidade da série: dia (até 60 dias), semana (até 12 meses) ou mês (todo o histórico). */
    private static String granularidade(int periodo) {
        return periodo == 0 ? "mês" : periodo <= 60 ? "dia" : "semana";
    }

    private static LocalDate inicioDoGrupo(LocalDate d, String gran) {
        return switch (gran) {
            case "semana" -> d.with(DayOfWeek.MONDAY);
            case "mês" -> d.withDayOfMonth(1);
            default -> d;
        };
    }

    private static LocalDate fimDoGrupo(LocalDate inicio, String gran) {
        return switch (gran) {
            case "semana" -> inicio.plusDays(6);
            case "mês" -> inicio.withDayOfMonth(inicio.lengthOfMonth());
            default -> inicio;
        };
    }

    /** Semanas e meses só entram na série quando completos dentro dos dados, para o último ponto não parecer uma queda. */
    private static boolean completo(LocalDate inicio, String gran, Janela j) {
        LocalDate fim = fimDoGrupo(inicio, gran);
        return !inicio.isBefore(j.inicio()) && !fim.isAfter(j.fim());
    }

    public Serie serie(int periodo, String plataforma) {
        Janela j = atual(periodo);
        String gran = granularidade(periodo);
        Map<LocalDate, List<LinhaDiaria>> grupos = vendas(j, plataforma).stream().collect(Collectors.groupingBy(
                l -> inicioDoGrupo(LocalDate.parse(l.d()), gran), TreeMap::new, Collectors.toList()));
        List<Ponto> pontos = grupos.entrySet().stream().filter(e -> completo(e.getKey(), gran, j)).map(e -> {
            Soma s = Soma.de(e.getValue());
            return new Ponto(e.getKey().toString(), s.rec(), s.luc(), s.vendidos());
        }).toList();
        String leitura = "";
        if (pontos.size() >= 2) {
            Ponto melhor = pontos.stream().max(Comparator.comparingDouble(Ponto::receita)).orElseThrow();
            Ponto pior = pontos.stream().min(Comparator.comparingDouble(Ponto::receita)).orElseThrow();
            DateTimeFormatter f = DateTimeFormatter.ofPattern("mês".equals(gran) ? "MM/yyyy" : "dd/MM");
            boolean fem = "semana".equals(gran);
            leitura = String.format(PT_BR, "%s de maior receita foi %s (%s); %s de menor foi %s (%s).", fem ? "A semana" : "dia".equals(gran) ? "O dia" : "O mês",
                    f.format(LocalDate.parse(melhor.data())), moeda(melhor.receita()), fem ? "a" : "o", f.format(LocalDate.parse(pior.data())), moeda(pior.receita()));
        }
        return new Serie(gran, pontos, leitura);
    }

    // ------------------------------------------------------------------ canais (onde ganho e perco)
    public Canais canais(int periodo) {
        Map<String, List<LinhaDiaria>> atual = vendas(atual(periodo), null).stream().collect(Collectors.groupingBy(LinhaDiaria::p));
        Map<String, List<LinhaDiaria>> ant = vendas(anterior(periodo), null).stream().collect(Collectors.groupingBy(LinhaDiaria::p));
        List<Canal> canais = atual.entrySet().stream().map(e -> {
            Soma s = Soma.de(e.getValue());
            Soma a = Soma.de(ant.getOrDefault(e.getKey(), List.of()));
            return new Canal(e.getKey(), nome(e.getKey()), s.vendidos(), s.rec(), s.luc(), s.margem(), divisao(s.can(), s.n()) * 100,
                    divisao(s.atr(), s.vendidos()) * 100, divisao(s.rcl(), s.vendidos()) * 100, temAnterior(periodo) && a.rec() > 0 ? a.margem() : null);
        }).sorted(Comparator.comparingDouble(Canal::receita).reversed()).toList();
        String leitura = "";
        if (canais.size() >= 2) {
            Canal maior = canais.get(0);
            Canal melhor = canais.stream().max(Comparator.comparingDouble(Canal::margem)).orElseThrow();
            Canal pior = canais.stream().min(Comparator.comparingDouble(Canal::margem)).orElseThrow();
            Canal piorAtraso = canais.stream().max(Comparator.comparingDouble(Canal::atraso)).orElseThrow();
            String parteMargem = maior.codigo().equals(melhor.codigo())
                    ? String.format(PT_BR, "%s traz a maior receita (%s) e também a melhor margem líquida (%s)", maior.nome(), moeda(maior.receita()), percentual(melhor.margem()))
                    : String.format(PT_BR, "%s traz a maior receita (%s), mas a melhor margem líquida é a de %s (%s)", maior.nome(), moeda(maior.receita()), melhor.nome(), percentual(melhor.margem()));
            leitura = String.format(PT_BR, "%s; a pior margem é a de %s (%s). O maior índice de pedidos atrasados está em %s (%s).",
                    parteMargem, pior.nome(), percentual(pior.margem()), piorAtraso.nome(), percentual(piorAtraso.atraso()));
        }
        return new Canais(canais, leitura);
    }

    // ------------------------------------------------------------------ atendimento
    public Atendimento atendimento(int periodo, String canal) {
        Janela j = atual(periodo);
        List<LinhaAtendimento> ls = conversas(j, canal);
        double[] m = atendimentoPeriodo(j, canal);
        int total = ls.stream().mapToInt(LinhaAtendimento::n).sum();
        List<FaixaConversao> faixas = IntStream.range(0, ROTULO_FAIXA.length).mapToObj(i -> {
            int k = ls.stream().mapToInt(l -> l.k().get(i)).sum();
            int v = ls.stream().mapToInt(l -> l.v().get(i)).sum();
            return new FaixaConversao(ROTULO_FAIXA[i], k, divisao(v, k) * 100);
        }).toList();
        // série semanal: janela de pelo menos 12 semanas para enxergar tendência
        LocalDate ref = dados.dataReferencia();
        Janela longa = new Janela(ref.minusDays(Math.max(efetivo(periodo), 84) - 1L), ref);
        Map<String, List<LinhaAtendimento>> porSemana = conversas(longa, canal).stream().collect(Collectors.groupingBy(
                l -> LocalDate.parse(l.d()).with(DayOfWeek.MONDAY).toString(), TreeMap::new, Collectors.toList()));
        List<PontoPct> semanal = porSemana.entrySet().stream().filter(e -> completo(LocalDate.parse(e.getKey()), "semana", longa)).map(e -> {
            double n = e.getValue().stream().mapToInt(LinhaAtendimento::n).sum();
            double u = e.getValue().stream().mapToInt(l -> l.f().get(0) + l.f().get(1)).sum();
            return new PontoPct(e.getKey(), divisao(u, n) * 100);
        }).toList();
        String leitura = "";
        FaixaConversao rapida = faixas.get(0).conversas() > 0 ? faixas.get(0) : faixas.get(1);
        FaixaConversao lenta = faixas.get(3);
        if (total > 0) {
            leitura = String.format(PT_BR, "No período foram %s conversas; %s foram respondidas em até 1 hora e %s ficaram sem resposta. "
                            + "Quem recebe resposta %s fecha pedido em %s dos casos; quem espera %s, em %s.",
                    inteiro(total), percentual(m[0]), percentual(m[1]), rapida.faixa(), percentual(rapida.conversao()), lenta.faixa(), percentual(lenta.conversao()));
        }
        return new Atendimento(todas(canal) ? "todos" : canal, total, m[0], m[1], m[2], faixas, semanal, leitura);
    }

    // ------------------------------------------------------------------ posts
    public Posts posts(int periodo) {
        Janela j = atual(periodo);
        List<PostInstagram> ps = dados.posts().stream().filter(p -> j.contem(p.d())).toList();
        Map<String, List<PostInstagram>> porTipo = ps.stream().collect(Collectors.groupingBy(PostInstagram::t));
        List<ResumoTipoPost> tipos = porTipo.entrySet().stream().map(e -> new ResumoTipoPost(e.getKey(), e.getValue().size(),
                e.getValue().stream().mapToInt(PostInstagram::al).average().orElse(0),
                e.getValue().stream().mapToInt(PostInstagram::cv).average().orElse(0),
                e.getValue().stream().mapToDouble(p -> divisao(p.cu() + p.co() + p.sa() + p.cp(), p.al()) * 100).average().orElse(0)))
                .sorted(Comparator.comparingDouble(ResumoTipoPost::conversasPorPost).reversed()).toList();
        List<PostTop> melhores = ps.stream().sorted(Comparator.comparingInt(PostInstagram::cv).reversed()).limit(5)
                .map(p -> new PostTop(p.id(), p.t(), p.d(), p.al(), p.sa() + p.cp(), p.cu(), p.cv())).toList();
        // três faixas de mesmo tamanho pelo total de salvamentos e compartilhamentos
        List<FaixaPost> engajamento = new ArrayList<>();
        if (ps.size() >= 9) {
            List<PostInstagram> ord = ps.stream().sorted(Comparator.comparingInt(p -> p.sa() + p.cp())).toList();
            String[] nomes = {"Menos salvos e compartilhados", "Intermediários", "Mais salvos e compartilhados"};
            for (int k = 0; k < 3; k++) {
                List<PostInstagram> parte = ord.subList(ord.size() * k / 3, ord.size() * (k + 1) / 3);
                int lo = parte.get(0).sa() + parte.get(0).cp();
                int hi = parte.get(parte.size() - 1).sa() + parte.get(parte.size() - 1).cp();
                engajamento.add(new FaixaPost(nomes[k], lo == hi ? lo + "" : lo + " a " + hi, parte.size(),
                        parte.stream().mapToInt(PostInstagram::cv).average().orElse(0)));
            }
        }
        String leitura = "";
        if (!tipos.isEmpty()) {
            ResumoTipoPost topo = tipos.get(0);
            ResumoTipoPost ultimo = tipos.get(tipos.size() - 1);
            leitura = String.format(PT_BR, "Neste período, %s gera em média %s conversas por post, contra %s de %s.",
                    rotuloTipo(topo.tipo()), decimal(topo.conversasPorPost()), decimal(ultimo.conversasPorPost()), rotuloTipo(ultimo.tipo()));
            if (engajamento.size() == 3) {
                leitura += String.format(PT_BR, " Os posts mais salvos e compartilhados abrem %s conversas cada, contra %s dos menos salvos.",
                        decimal(engajamento.get(2).conversasPorPost()), decimal(engajamento.get(0).conversasPorPost()));
            }
        }
        return new Posts(tipos, melhores, engajamento, leitura);
    }

    private static String rotuloTipo(String t) {
        return switch (t) {
            case "reels" -> "Reels";
            case "carrossel" -> "carrossel";
            default -> "foto";
        };
    }

    // ------------------------------------------------------------------ clientes
    public Clientes clientes(int periodo, String plataforma) {
        Janela j = atual(periodo);
        Map<Integer, ClienteResumo> porId = dados.clientes().stream().collect(Collectors.toMap(ClienteResumo::i, c -> c));
        List<PedidoCliente> pedidos = dados.pedidosCliente().stream().filter(p -> j.contem(p.d()) && (todas(plataforma) || p.p().equals(plataforma))).toList();
        Set<Integer> ativos = pedidos.stream().map(PedidoCliente::i).collect(Collectors.toSet());
        int novos = (int) ativos.stream().filter(i -> j.contem(porId.get(i).pri())).count();
        // recompra em 90 dias: clientes cuja 1ª compra ocorreu entre 365 e 90 dias antes da referência
        LocalDate ref = dados.dataReferencia();
        Map<Integer, List<LocalDate>> datas = dados.pedidosCliente().stream().collect(Collectors.groupingBy(PedidoCliente::i,
                Collectors.mapping(p -> LocalDate.parse(p.d()), Collectors.toList())));
        List<Integer> coorte = dados.clientes().stream().filter(c -> {
            LocalDate pri = LocalDate.parse(c.pri());
            return !pri.isBefore(ref.minusDays(365)) && !pri.isAfter(ref.minusDays(90));
        }).map(ClienteResumo::i).toList();
        long voltaram = coorte.stream().filter(i -> {
            LocalDate pri = LocalDate.parse(porId.get(i).pri());
            return datas.get(i).stream().anyMatch(d -> d.isAfter(pri) && !d.isAfter(pri.plusDays(90)));
        }).count();
        double recompra = divisao(voltaram, coorte.size()) * 100;
        List<Segmento> segmentos = List.of(
                segmento("Compra em mais de um canal", ativos.stream().map(porId::get).filter(c -> c.om() == 1).toList()),
                segmento("Compra em um canal só", ativos.stream().map(porId::get).filter(c -> c.om() == 0).toList()));
        Map<String, Long> cidades = ativos.stream().collect(Collectors.groupingBy(i -> porId.get(i).cd(), Collectors.counting()));
        List<CidadeCliente> topCidades = cidades.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
                .map(e -> new CidadeCliente(e.getKey(), e.getValue().intValue())).toList();
        String leitura = "";
        if (!ativos.isEmpty() && segmentos.get(0).clientes() > 0 && segmentos.get(1).clientes() > 0) {
            Segmento o = segmentos.get(0);
            Segmento u = segmentos.get(1);
            leitura = String.format(PT_BR, "%s clientes compraram no período; %s são novos e %s já eram da casa. Quem compra em mais de um canal já gastou %s em média (%s pedidos), "
                            + "contra %s (%s pedidos) de quem compra em um canal só. Isso mostra que esses clientes valem mais, não que incentivar um segundo canal cause mais compras. "
                            + "%s dos clientes captados há 3 a 12 meses voltaram a comprar em até 90 dias.",
                    inteiro(ativos.size()), inteiro(novos), inteiro(ativos.size() - novos), moeda(o.gastoMedio()), decimal(o.pedidosPorCliente()),
                    moeda(u.gastoMedio()), decimal(u.pedidosPorCliente()), percentual(recompra));
        }
        return new Clientes(ativos.size(), novos, ativos.size() - novos, recompra, segmentos, topCidades, leitura);
    }

    /** Segmento calculado sobre o histórico completo dos clientes que compraram no período. */
    private static Segmento segmento(String nome, List<ClienteResumo> cs) {
        double pedidos = cs.stream().mapToInt(ClienteResumo::n).sum();
        double gasto = cs.stream().mapToDouble(ClienteResumo::g).sum();
        return new Segmento(nome, cs.size(), divisao(gasto, pedidos), divisao(pedidos, cs.size()), divisao(gasto, cs.size()));
    }

    // ------------------------------------------------------------------ reputação
    public Reputacao reputacao() {
        Map<String, List<LinhaReputacao>> porMp = dados.reputacao().stream().collect(Collectors.groupingBy(LinhaReputacao::p, TreeMap::new, Collectors.toList()));
        List<MarketplaceReputacao> mps = new ArrayList<>();
        String ultimaSemana = "";
        for (var e : porMp.entrySet()) {
            List<LinhaReputacao> serie = e.getValue().stream().sorted(Comparator.comparing(LinhaReputacao::s)).toList();
            LinhaReputacao u = serie.get(serie.size() - 1);
            LinhaReputacao ant = serie.get(Math.max(0, serie.size() - 5));
            ultimaSemana = u.s();
            mps.add(new MarketplaceReputacao(e.getKey(), nome(e.getKey()), u.nv(), ant.nv(), u.sc(), u.a() * 100, u.c() * 100, u.r() * 100, u.pp(),
                    serie.subList(Math.max(0, serie.size() - 26), serie.size()).stream().map(LinhaReputacao::sc).toList()));
        }
        mps.sort(Comparator.comparingDouble(MarketplaceReputacao::score));
        String leitura = "";
        if (!mps.isEmpty()) {
            MarketplaceReputacao pior = mps.get(0);
            long fora = mps.stream().filter(m -> !"verde".equals(m.nivel())).count();
            leitura = String.format(PT_BR, "%s fora da faixa verde. O mais frágil é %s (nível %s, atraso de %s dos pedidos nas últimas 4 semanas).",
                    fora == 0 ? "Nenhum marketplace está" : fora == mps.size() ? "Todos os " + mps.size() + " marketplaces estão" : fora + " de " + mps.size() + " marketplaces estão",
                    pior.nome(), pior.nivel(), percentual(pior.atraso()));
        }
        return new Reputacao(ultimaSemana, mps, leitura);
    }

    // ------------------------------------------------------------------ pendências, modelos, efeito
    public JsonNode modelos() {
        return dados.modelos();
    }

    /** Antes e depois da própria loja (sem comparar com outras lojas). */
    public Efeito efeito() {
        JsonNode loja = dados.efeitoDid().get("lojas").get(String.valueOf(LOJA_ATUAL));
        LocalDate adocao = LocalDate.parse(loja.get("adocao").asText());
        LocalDate fimAdaptacao = adocao.plusWeeks(8);
        List<PontoPct> serie = dados.efeitoSemanal().stream().filter(e -> e.l() == LOJA_ATUAL && e.n() >= 20)
                .filter(e -> !LocalDate.parse(e.s()).plusDays(6).isAfter(dados.dataReferencia()))
                .sorted(Comparator.comparing(EfeitoSemana::s)).map(e -> new PontoPct(e.s(), divisao(e.u(), e.n()) * 100)).toList();
        JsonNode m = loja.get("metricas");
        List<IndicadorEfeito> ind = List.of(
                indicador(m, "resp_1h", "Mensagens respondidas em até 1 hora", "%", false),
                indicador(m, "sem_resposta", "Mensagens sem resposta", "%", true),
                indicador(m, "atraso_envio", "Pedidos enviados com atraso", "%", true),
                indicador(m, "resolucao_h", "Tempo para resolver uma reclamação", "h", true));
        IndicadorEfeito r = ind.get(0);
        IndicadorEfeito at = ind.get(2);
        IndicadorEfeito rs = ind.get(3);

        // Instagram da própria loja: mesma janela de antes (12 semanas) e de depois (a partir da 8ª semana)
        Janela antes = new Janela(adocao.minusWeeks(loja.get("semanas_pre").asInt()), adocao.minusDays(1));
        Janela depois = new Janela(fimAdaptacao, dados.dataReferencia());
        List<PostInstagram> pa = dados.posts().stream().filter(p -> antes.contem(p.d())).toList();
        List<PostInstagram> pd = dados.posts().stream().filter(p -> depois.contem(p.d())).toList();
        List<IndicadorEfeito> redes = List.of(
                redes("alcance", "Alcance médio por post", pa, pd, PostInstagram::al),
                redes("salvos", "Salvamentos e compartilhamentos por post", pa, pd, p -> p.sa() + p.cp()),
                redes("conversas", "Conversas abertas por post", pa, pd, PostInstagram::cv));
        Map<LocalDate, List<PostInstagram>> porMes = dados.posts().stream().collect(Collectors.groupingBy(
                p -> LocalDate.parse(p.d()).withDayOfMonth(1), TreeMap::new, Collectors.toList()));
        List<PontoPct> serieRedes = porMes.entrySet().stream().filter(e -> completo(e.getKey(), "mês", new Janela(e.getKey(), dados.dataReferencia())))
                .map(e -> new PontoPct(e.getKey().toString(), e.getValue().stream().mapToInt(PostInstagram::cv).average().orElse(0))).toList();
        IndicadorEfeito cv = redes.get(2);
        IndicadorEfeito al = redes.get(0);

        String leitura = String.format(PT_BR, "Antes da Atria, %s das mensagens eram respondidas em até 1 hora; depois do período de adaptação, %s. "
                        + "Os pedidos enviados com atraso foram de %s para %s e a resolução de reclamações caiu de %s h para %s h. "
                        + "No Instagram, as conversas abertas por post foram de %s para %s e o alcance médio de %s para %s. "
                        + "É a comparação da loja com ela mesma: outras mudanças no período, como a época do ano, também pesam.",
                percentual(r.antes()), percentual(r.depois()), percentual(at.antes()), percentual(at.depois()),
                inteiro(rs.antes()), inteiro(rs.depois()), decimal(cv.antes()), decimal(cv.depois()), inteiro(al.antes()), inteiro(al.depois()));
        return new Efeito(adocao.toString(), fimAdaptacao.toString(), loja.get("semanas_pre").asInt(), serie, ind, serieRedes, redes, leitura);
    }

    private static IndicadorEfeito redes(String id, String rotulo, List<PostInstagram> antes, List<PostInstagram> depois,
                                         java.util.function.ToIntFunction<PostInstagram> f) {
        return new IndicadorEfeito(id, rotulo, "n", antes.stream().mapToInt(f).average().orElse(0), depois.stream().mapToInt(f).average().orElse(0), false);
    }

    private static IndicadorEfeito indicador(JsonNode m, String chave, String rotulo, String un, boolean melhorSeMenor) {
        return new IndicadorEfeito(chave, rotulo, un, m.get(chave).get("trat_antes").asDouble(), m.get(chave).get("trat_depois").asDouble(), melhorSeMenor);
    }

}
