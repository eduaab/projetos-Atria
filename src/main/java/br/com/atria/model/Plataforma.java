package br.com.atria.model;

import br.com.atria.model.enums.TipoPlataforma;

import java.util.List;

public class Plataforma implements Identificavel {
    private Long id;
    private final String slug;
    private final String nome;
    private final TipoPlataforma tipo;
    private final List<String> escopos;

    public Plataforma(String slug, String nome, TipoPlataforma tipo, List<String> escopos) {
        this.slug = slug;
        this.nome = nome;
        this.tipo = tipo;
        this.escopos = List.copyOf(escopos);
    }

    public boolean isCanalDeConversa() {
        return tipo == TipoPlataforma.MENSAGERIA || tipo == TipoPlataforma.REDE_SOCIAL;
    }

    public boolean isComercial() {
        return tipo == TipoPlataforma.MARKETPLACE || tipo == TipoPlataforma.LOJA_PROPRIA;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public String getNome() { return nome; }
    public TipoPlataforma getTipo() { return tipo; }
    public List<String> getEscopos() { return escopos; }
}
