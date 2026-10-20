package br.com.atria.model.enums;

public enum TipoPendencia {
    MENSAGEM_SEM_RESPOSTA("Mensagem sem resposta"),
    RECLAMACAO_PENDENTE("Reclamação pendente"),
    PEDIDO_NAO_ENVIADO("Pedido não enviado");

    private final String descricao;

    TipoPendencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
