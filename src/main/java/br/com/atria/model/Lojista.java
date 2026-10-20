package br.com.atria.model;

import java.time.LocalDate;

public class Lojista implements Identificavel {
    private Long id;
    private final String nomeLoja;
    private final String slug;
    private final String segmento;
    private final String uf;
    private final String cidade;
    private final LocalDate dataAdesaoAtria;

    public Lojista(String nomeLoja, String slug, String segmento, String uf, String cidade, LocalDate dataAdesaoAtria) {
        this.nomeLoja = nomeLoja;
        this.slug = slug;
        this.segmento = segmento;
        this.uf = uf;
        this.cidade = cidade;
        this.dataAdesaoAtria = dataAdesaoAtria;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getNomeLoja() { return nomeLoja; }
    public String getSlug() { return slug; }
    public String getSegmento() { return segmento; }
    public String getUf() { return uf; }
    public String getCidade() { return cidade; }
    public LocalDate getDataAdesaoAtria() { return dataAdesaoAtria; }
}
