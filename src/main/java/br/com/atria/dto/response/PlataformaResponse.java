package br.com.atria.dto.response;

import br.com.atria.model.enums.TipoPlataforma;

import java.util.List;

public record PlataformaResponse(Long id, String slug, String nome, TipoPlataforma tipo,
                                 String tipoDescricao, List<String> escopos) {
}
