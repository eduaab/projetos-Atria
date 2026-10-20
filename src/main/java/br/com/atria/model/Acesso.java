package br.com.atria.model;

import java.time.LocalDateTime;

/** Permissão de um usuário da loja para operar uma conexão. */
public class Acesso implements Identificavel {
    private Long id;
    private final Long usuarioId;
    private final Long conexaoId;
    private final LocalDateTime concedidoEm;
    private LocalDateTime revogadoEm;

    public Acesso(Long usuarioId, Long conexaoId, LocalDateTime concedidoEm) {
        this.usuarioId = usuarioId;
        this.conexaoId = conexaoId;
        this.concedidoEm = concedidoEm;
    }

    public void revogar(LocalDateTime instante) {
        this.revogadoEm = instante;
    }

    public boolean isAtivo() {
        return revogadoEm == null;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getConexaoId() { return conexaoId; }
    public LocalDateTime getConcedidoEm() { return concedidoEm; }
    public LocalDateTime getRevogadoEm() { return revogadoEm; }
}
