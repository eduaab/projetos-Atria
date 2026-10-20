package br.com.atria.model;

/** Entidade com identificador numérico, gerado pelo repositório ao salvar. */
public interface Identificavel {
    Long getId();

    void setId(Long id);
}
