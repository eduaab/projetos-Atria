package br.com.atria.service.regra;

import java.text.Normalizer;
import java.util.Locale;

/** Normalização usada nas buscas: minúsculas, sem acentos, ou só dígitos. */
public final class NormalizadorTexto {

    private NormalizadorTexto() {
    }

    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }

    public static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }
}
