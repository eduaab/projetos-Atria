package br.com.atria.seed;

import java.util.List;

/**
 * Molde de um titular do seed.
 *
 * @param plataformas      slugs das plataformas onde o titular foi identificado
 * @param emailFixo        e-mail forçado (para simular cadastro duplicado); nulo = gerado pelo nome
 * @param mesmoTelefoneDe  índice de outro titular cujo telefone é reaproveitado; -1 = telefone próprio
 */
record TitularFicticio(String nome, List<String> plataformas, String emailFixo, int mesmoTelefoneDe) {

    static TitularFicticio de(String nome, String plataformas) {
        return new TitularFicticio(nome, List.of(plataformas.split(" ")), null, -1);
    }

    TitularFicticio comEmail(String email) {
        return new TitularFicticio(nome, plataformas, email, mesmoTelefoneDe);
    }

    TitularFicticio comTelefoneDe(int indice) {
        return new TitularFicticio(nome, plataformas, emailFixo, indice);
    }
}
