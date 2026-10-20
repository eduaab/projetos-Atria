package br.com.atria.model.enums;

public enum TipoDado {
    NOME("Nome"),
    CPF("CPF"),
    EMAIL("E-mail"),
    TELEFONE("Telefone"),
    ENDERECO("Endereço");

    private final String descricao;

    TipoDado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
