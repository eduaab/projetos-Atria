package br.com.atria.model.enums;

public enum StatusAlerta {
    ABERTO("Aberto"),
    EM_ANALISE("Em análise"),
    RESOLVIDO("Resolvido");

    private final String descricao;

    StatusAlerta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
