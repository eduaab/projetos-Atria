package br.com.atria.model;

import br.com.atria.model.enums.MetodoVinculo;

import java.time.LocalDateTime;

/** Vínculo entre um titular e o seu identificador em uma plataforma (base da Central de clientes). */
public class IdentidadePlataforma implements Identificavel {
    private Long id;
    private final Long titularId;
    private final Long plataformaId;
    private final String idExterno;
    private final MetodoVinculo metodoVinculo;
    private final LocalDateTime vinculadoEm;

    public IdentidadePlataforma(Long titularId, Long plataformaId, String idExterno, MetodoVinculo metodoVinculo, LocalDateTime vinculadoEm) {
        this.titularId = titularId;
        this.plataformaId = plataformaId;
        this.idExterno = idExterno;
        this.metodoVinculo = metodoVinculo;
        this.vinculadoEm = vinculadoEm;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getTitularId() { return titularId; }
    public Long getPlataformaId() { return plataformaId; }
    public String getIdExterno() { return idExterno; }
    public MetodoVinculo getMetodoVinculo() { return metodoVinculo; }
    public LocalDateTime getVinculadoEm() { return vinculadoEm; }
}
