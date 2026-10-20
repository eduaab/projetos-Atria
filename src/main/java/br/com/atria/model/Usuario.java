package br.com.atria.model;

import br.com.atria.model.enums.PapelUsuario;

import java.time.LocalDate;

public class Usuario implements Identificavel {
    private Long id;
    private final Long lojistaId;
    private final String nome;
    private final String email;
    private final PapelUsuario papel;
    private final LocalDate dataAdmissao;
    private LocalDate dataDesligamento;

    public Usuario(Long lojistaId, String nome, String email, PapelUsuario papel, LocalDate dataAdmissao, LocalDate dataDesligamento) {
        this.lojistaId = lojistaId;
        this.nome = nome;
        this.email = email;
        this.papel = papel;
        this.dataAdmissao = dataAdmissao;
        this.dataDesligamento = dataDesligamento;
    }

    public boolean isDesligado() {
        return dataDesligamento != null;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getLojistaId() { return lojistaId; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public PapelUsuario getPapel() { return papel; }
    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public LocalDate getDataDesligamento() { return dataDesligamento; }
}
