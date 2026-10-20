package br.com.atria.model.enums;

public enum TipoPlataforma {
    MARKETPLACE("Marketplace"),
    LOJA_PROPRIA("Loja própria"),
    MENSAGERIA("Mensageria"),
    REDE_SOCIAL("Rede social");

    private final String descricao;

    TipoPlataforma(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
