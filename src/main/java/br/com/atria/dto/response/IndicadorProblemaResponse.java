package br.com.atria.dto.response;

import br.com.atria.model.enums.Prioridade;

import java.util.List;

public record IndicadorProblemaResponse(String codigo, String titulo, String descricao, long quantidade,
                                        Prioridade severidade, List<Long> titularIds) {
}
