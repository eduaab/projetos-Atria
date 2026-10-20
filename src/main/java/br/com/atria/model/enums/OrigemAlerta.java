package br.com.atria.model.enums;

public enum OrigemAlerta {
    PENDENCIA("Pendência de canal"),
    ACESSO("Acesso de usuário"),
    SOLICITACAO("Solicitação LGPD");

    private final String descricao;

    OrigemAlerta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
