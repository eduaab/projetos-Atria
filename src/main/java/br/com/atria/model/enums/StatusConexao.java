package br.com.atria.model.enums;

public enum StatusConexao {
    ATIVA("Ativa"),
    PENDENTE("Pendente"),
    REVOGADA("Revogada"),
    ERRO("Erro");

    private final String descricao;

    StatusConexao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
