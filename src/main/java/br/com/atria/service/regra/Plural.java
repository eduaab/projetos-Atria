package br.com.atria.service.regra;

/** Concordância de número: "1 solicitação", "2 solicitações". */
public final class Plural {

    private Plural() {
    }

    public static String de(long quantidade, String singular, String plural) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }
}
