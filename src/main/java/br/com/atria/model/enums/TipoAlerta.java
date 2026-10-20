package br.com.atria.model.enums;

public enum TipoAlerta {
    CLIENTE_SEM_RESPOSTA("Cliente sem resposta"),
    RECLAMACAO_PENDENTE("Reclamação pendente"),
    PEDIDO_NAO_ENVIADO("Pedido não enviado"),
    ACESSO_NAO_REVOGADO("Acesso não revogado"),
    PRAZO_LGPD("Prazo LGPD");

    private final String descricao;

    TipoAlerta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
