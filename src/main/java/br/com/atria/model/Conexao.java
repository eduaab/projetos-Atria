package br.com.atria.model;

import br.com.atria.model.enums.StatusConexao;

import java.time.LocalDateTime;

public class Conexao implements Identificavel {
    private Long id;
    private final Long lojistaId;
    private final Long plataformaId;
    private StatusConexao status;
    private LocalDateTime conectadaEm;
    private LocalDateTime desconectadaEm;
    private LocalDateTime ultimaSincronizacao;

    public Conexao(Long lojistaId, Long plataformaId) {
        this.lojistaId = lojistaId;
        this.plataformaId = plataformaId;
        this.status = StatusConexao.PENDENTE;
    }

    public void conectar(LocalDateTime instante) {
        this.status = StatusConexao.ATIVA;
        this.conectadaEm = instante;
        this.desconectadaEm = null;
        this.ultimaSincronizacao = instante;
    }

    public void desconectar(LocalDateTime instante) {
        this.status = StatusConexao.REVOGADA;
        this.desconectadaEm = instante;
    }

    public void sincronizar(LocalDateTime instante) {
        this.ultimaSincronizacao = instante;
    }

    public boolean isAtiva() {
        return status == StatusConexao.ATIVA;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getLojistaId() { return lojistaId; }
    public Long getPlataformaId() { return plataformaId; }
    public StatusConexao getStatus() { return status; }
    public LocalDateTime getConectadaEm() { return conectadaEm; }
    public LocalDateTime getDesconectadaEm() { return desconectadaEm; }
    public LocalDateTime getUltimaSincronizacao() { return ultimaSincronizacao; }
}
