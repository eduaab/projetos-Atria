package br.com.atria.model;

import br.com.atria.model.enums.TipoInteracao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Interacao implements Identificavel {
    private Long id;
    private final Long titularId;
    private final Long plataformaId;
    private final TipoInteracao tipo;
    private final LocalDateTime data;
    private final BigDecimal valor;

    public Interacao(Long titularId, Long plataformaId, TipoInteracao tipo, LocalDateTime data, BigDecimal valor) {
        this.titularId = titularId;
        this.plataformaId = plataformaId;
        this.tipo = tipo;
        this.data = data;
        this.valor = valor;
    }

    public boolean isCompra() {
        return tipo == TipoInteracao.COMPRA_FINALIZADA && valor != null;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getTitularId() { return titularId; }
    public Long getPlataformaId() { return plataformaId; }
    public TipoInteracao getTipo() { return tipo; }
    public LocalDateTime getData() { return data; }
    public BigDecimal getValor() { return valor; }
}
