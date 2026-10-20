package br.com.atria.model.enums;

public enum TipoInteracao {
    COMPRA_FINALIZADA("Compra finalizada"),
    CARRINHO_ABANDONADO("Carrinho abandonado"),
    MENSAGEM("Mensagem"),
    RECLAMACAO("Reclamação"),
    SOLICITACAO_LGPD("Solicitação LGPD");

    private final String descricao;

    TipoInteracao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
