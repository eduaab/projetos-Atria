package br.com.atria.model.enums;

public enum NivelPrazo {
    VERDE("Mais de 7 dias"),
    AMARELO("De 3 a 7 dias"),
    LARANJA("Até 2 dias"),
    VERMELHO("Vencido");

    private final String descricao;

    NivelPrazo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
