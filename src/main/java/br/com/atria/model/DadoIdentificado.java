package br.com.atria.model;

import br.com.atria.model.enums.TipoDado;

import java.time.LocalDateTime;

/** Tipo de dado pessoal de um titular encontrado em uma plataforma pelo mapeamento. */
public class DadoIdentificado implements Identificavel {
    private Long id;
    private final Long titularId;
    private final Long plataformaId;
    private final TipoDado tipoDado;
    private final LocalDateTime encontradoEm;

    public DadoIdentificado(Long titularId, Long plataformaId, TipoDado tipoDado, LocalDateTime encontradoEm) {
        this.titularId = titularId;
        this.plataformaId = plataformaId;
        this.tipoDado = tipoDado;
        this.encontradoEm = encontradoEm;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getTitularId() { return titularId; }
    public Long getPlataformaId() { return plataformaId; }
    public TipoDado getTipoDado() { return tipoDado; }
    public LocalDateTime getEncontradoEm() { return encontradoEm; }
}
