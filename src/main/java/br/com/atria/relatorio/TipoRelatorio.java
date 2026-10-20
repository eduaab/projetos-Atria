package br.com.atria.relatorio;

import br.com.atria.exception.RecursoNaoEncontradoException;

import java.util.Arrays;

public enum TipoRelatorio {
    ESG("esg", "Relatório ESG – Governança (Pilar G)"),
    ANPD("anpd", "Relatório de Conformidade ANPD"),
    COMERCIAL("comercial", "Diferencial Comercial – Compromisso com a Privacidade");

    private final String slug;
    private final String titulo;

    TipoRelatorio(String slug, String titulo) {
        this.slug = slug;
        this.titulo = titulo;
    }

    public static TipoRelatorio deSlug(String slug) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.slug.equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relatório '" + slug + "' não existe. Use esg, anpd ou comercial."));
    }

    public String getSlug() {
        return slug;
    }

    public String getTitulo() {
        return titulo;
    }
}
