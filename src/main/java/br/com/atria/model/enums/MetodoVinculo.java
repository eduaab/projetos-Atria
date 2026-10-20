package br.com.atria.model.enums;

public enum MetodoVinculo {
    CPF("CPF"),
    EMAIL("E-mail"),
    TELEFONE("Telefone"),
    MANUAL("Manual");

    private final String descricao;

    MetodoVinculo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
