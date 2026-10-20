package br.com.atria.model;

import br.com.atria.model.enums.TipoPendencia;

import java.time.LocalDateTime;

/** Situação pendente detectada em um canal (cenário fictício que alimenta os alertas). */
public class PendenciaCanal implements Identificavel {
    private Long id;
    private final Long lojistaId;
    private final Long plataformaId;
    private final Long titularId;
    private final TipoPendencia tipo;
    private final LocalDateTime desde;
    private boolean resolvida;

    public PendenciaCanal(Long lojistaId, Long plataformaId, Long titularId, TipoPendencia tipo, LocalDateTime desde) {
        this.lojistaId = lojistaId;
        this.plataformaId = plataformaId;
        this.titularId = titularId;
        this.tipo = tipo;
        this.desde = desde;
    }

    public void resolver() {
        this.resolvida = true;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getLojistaId() { return lojistaId; }
    public Long getPlataformaId() { return plataformaId; }
    public Long getTitularId() { return titularId; }
    public TipoPendencia getTipo() { return tipo; }
    public LocalDateTime getDesde() { return desde; }
    public boolean isResolvida() { return resolvida; }
}
