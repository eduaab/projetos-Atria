package br.com.atria.model.analise;

import java.util.List;

/**
 * Registros lidos dos arquivos JSON em {@code resources/analise}, gerados pela trilha de Análise e Visualização de Dados
 * (dados simulados da Flor de Lis Moda e de lojas de comparação). Os nomes curtos seguem o JSON.
 */
public final class Registros {
    private Registros() {
    }

    /** Vendas do dia por plataforma: n pedidos, rec receita, luc lucro líquido, can cancelados, atr atrasados, rcl com reclamação, dev devolvidos. */
    public record LinhaDiaria(String d, String p, int n, double rec, double luc, int can, int atr, int rcl, int dev) {
    }

    /** Conversas do dia por canal: n conversas, sr sem resposta, f respondidas por faixa de tempo, k e v conversas com desfecho conhecido e convertidas por faixa, mi soma de minutos. */
    public record LinhaAtendimento(String d, String c, int n, int sr, List<Integer> f, List<Integer> k, List<Integer> v, double mi) {
    }

    /** Post do Instagram: al alcance, cu curtidas, co comentários, sa salvamentos, cp compartilhamentos, cv conversas abertas em 48 h. */
    public record PostInstagram(int id, String t, String d, int h, int al, int cu, int co, int sa, int cp, int cv) {
    }

    /** Reputação semanal em um marketplace: a atraso, c cancelamento, r reclamação, sc score, nv nível, pp pontos de penalidade. */
    public record LinhaReputacao(String p, String s, int n, double a, double c, double r, double sc, String nv, int pp) {
    }

    /** Cliente pseudonimizado: n pedidos, g gasto total, pri primeira compra, ult última, cd cidade, om comprou em mais de um canal. */
    public record ClienteResumo(int i, int n, double g, String pri, String ult, String cd, int om) {
    }

    public record PedidoCliente(int i, String d, String p, double v) {
    }

    /** Semana de uma loja (l): n conversas, u respondidas em até 1 h, sr sem resposta, md mediana de resposta, pd pedidos, at atrasados, rc reclamações resolvidas, rh horas somadas. */
    public record EfeitoSemana(int l, String s, int n, int u, int sr, Double md, int pd, int at, int rc, double rh, double rec) {
    }
}
