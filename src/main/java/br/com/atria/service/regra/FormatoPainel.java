package br.com.atria.service.regra;

import java.text.NumberFormat;
import java.util.Locale;

/** Formatação de números para os textos de leitura do Painel (pt-BR). */
public final class FormatoPainel {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private FormatoPainel() {
    }

    public static String moeda(double valor) {
        NumberFormat f = NumberFormat.getCurrencyInstance(PT_BR);
        f.setMaximumFractionDigits(0);
        return f.format(valor);
    }

    public static String percentual(double valor) {
        return String.format(PT_BR, "%.1f%%", valor);
    }

    public static String inteiro(double valor) {
        return String.format(PT_BR, "%,.0f", valor);
    }

    public static String decimal(double valor) {
        return String.format(PT_BR, "%.1f", valor);
    }

    /** Variação relativa em % entre dois valores, ou nulo quando a base é zero. */
    public static Double variacao(double atual, Double base) {
        if (base == null || base == 0) {
            return null;
        }
        return (atual - base) * 100 / Math.abs(base);
    }

    public static String variacaoTexto(double atual, Double base) {
        Double v = variacao(atual, base);
        if (v == null) {
            return "sem base de comparação";
        }
        return String.format(PT_BR, "%s%.1f%%", v >= 0 ? "+" : "−", Math.abs(v));
    }

    public static double divisao(double a, double b) {
        return b == 0 ? 0 : a / b;
    }
}
