package br.com.atria.model;

import java.time.LocalDateTime;

/** Cliente final da loja, dono dos dados pessoais (titular na LGPD). */
public class Titular implements Identificavel {
    private Long id;
    private final Long lojistaId;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;
    private String cep;
    private final LocalDateTime criadoEm;
    private LocalDateTime anonimizadoEm;

    public Titular(Long lojistaId, String nome, String cpf, String email, String telefone, String cep, LocalDateTime criadoEm) {
        this.lojistaId = lojistaId;
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.telefone = telefone;
        this.cep = cep;
        this.criadoEm = criadoEm;
    }

    /** Apaga os dados pessoais guardados pela Atria, mantendo apenas o registro técnico. */
    public void anonimizar(LocalDateTime instante) {
        this.nome = null;
        this.cpf = null;
        this.email = null;
        this.telefone = null;
        this.cep = null;
        this.anonimizadoEm = instante;
    }

    public boolean isAnonimizado() {
        return anonimizadoEm != null;
    }

    public String getNomeExibicao() {
        return isAnonimizado() ? "Titular anonimizado #" + id : nome;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getLojistaId() { return lojistaId; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getCep() { return cep; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAnonimizadoEm() { return anonimizadoEm; }
}
