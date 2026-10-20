package br.com.atria.dto.response;

public record TitularResumoResponse(Long id, String nome, String cpfMascarado, String email, String telefone,
                                    int quantidadePlataformas, boolean possuiSolicitacaoAberta) {
}
