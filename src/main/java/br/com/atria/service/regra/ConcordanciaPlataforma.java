package br.com.atria.service.regra;

import java.util.Set;

/** Concordância de gênero com o nome da plataforma: "no Instagram", "na Shopee". */
public final class ConcordanciaPlataforma {
    private static final Set<String> NOMES_FEMININOS = Set.of("Shopee", "Shein", "Nuvemshop");

    private ConcordanciaPlataforma() {
    }

    public static String comPreposicao(String nomePlataforma) {
        return (NOMES_FEMININOS.contains(nomePlataforma) ? "na " : "no ") + nomePlataforma;
    }
}
