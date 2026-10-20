package br.com.atria.model.enums;

public enum CanalOrigem {
    WHATSAPP("WhatsApp"),
    INSTAGRAM("Instagram"),
    EMAIL("E-mail"),
    MARKETPLACE("Marketplace"),
    TELEFONE("Telefone"),
    PAINEL("Painel Atria");

    private final String descricao;

    CanalOrigem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
