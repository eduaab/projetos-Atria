package br.com.atria.model.enums;

public enum TipoSolicitacao {
    EXCLUSAO("Exclusão de dados"),
    ACESSO("Acesso aos dados"),
    CORRECAO("Correção de dados"),
    PORTABILIDADE("Portabilidade");

    private final String descricao;

    TipoSolicitacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
