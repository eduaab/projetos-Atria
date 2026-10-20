package br.com.atria.model.enums;

public enum PapelUsuario {
    DONO("Dono"),
    ATENDENTE("Atendente"),
    ESTOQUISTA("Estoquista");

    private final String descricao;

    PapelUsuario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
