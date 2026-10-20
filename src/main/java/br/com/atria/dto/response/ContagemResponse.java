package br.com.atria.dto.response;

/** Par genérico "código → quantidade", com a descrição pronta para exibir. */
public record ContagemResponse(String codigo, String descricao, long quantidade) {
}
