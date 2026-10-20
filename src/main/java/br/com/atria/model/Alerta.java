package br.com.atria.model;

import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.model.enums.Prioridade;
import br.com.atria.model.enums.StatusAlerta;
import br.com.atria.model.enums.TipoAlerta;

import java.time.LocalDateTime;

/**
 * Alerta gerado pela varredura. A origem ({@code tipoOrigem} + {@code origemId}) aponta para a
 * pendência, o acesso ou a solicitação que o gerou e impede que a varredura duplique alertas.
 */
public class Alerta implements Identificavel {
    private Long id;
    private final Long lojistaId;
    private final Long plataformaId;
    private final Long titularId;
    private final TipoAlerta tipo;
    private Prioridade prioridade;
    private StatusAlerta status;
    private final String titulo;
    private final String descricao;
    private final int clientesAfetados;
    private final OrigemAlerta tipoOrigem;
    private final Long origemId;
    private final LocalDateTime criadoEm;
    private LocalDateTime resolvidoEm;

    public Alerta(Long lojistaId, Long plataformaId, Long titularId, TipoAlerta tipo, Prioridade prioridade,
                  String titulo, String descricao, int clientesAfetados,
                  OrigemAlerta tipoOrigem, Long origemId, LocalDateTime criadoEm) {
        this.lojistaId = lojistaId;
        this.plataformaId = plataformaId;
        this.titularId = titularId;
        this.tipo = tipo;
        this.prioridade = prioridade;
        this.titulo = titulo;
        this.descricao = descricao;
        this.clientesAfetados = clientesAfetados;
        this.tipoOrigem = tipoOrigem;
        this.origemId = origemId;
        this.criadoEm = criadoEm;
        this.status = StatusAlerta.ABERTO;
    }

    public void alterarStatus(StatusAlerta novoStatus, LocalDateTime instante) {
        this.status = novoStatus;
        this.resolvidoEm = novoStatus == StatusAlerta.RESOLVIDO ? instante : null;
    }

    public void resolver(LocalDateTime instante) {
        alterarStatus(StatusAlerta.RESOLVIDO, instante);
    }

    public void reclassificar(Prioridade novaPrioridade) {
        this.prioridade = novaPrioridade;
    }

    public boolean isAberto() {
        return status != StatusAlerta.RESOLVIDO;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getLojistaId() { return lojistaId; }
    public Long getPlataformaId() { return plataformaId; }
    public Long getTitularId() { return titularId; }
    public TipoAlerta getTipo() { return tipo; }
    public Prioridade getPrioridade() { return prioridade; }
    public StatusAlerta getStatus() { return status; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public int getClientesAfetados() { return clientesAfetados; }
    public OrigemAlerta getTipoOrigem() { return tipoOrigem; }
    public Long getOrigemId() { return origemId; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getResolvidoEm() { return resolvidoEm; }
}
