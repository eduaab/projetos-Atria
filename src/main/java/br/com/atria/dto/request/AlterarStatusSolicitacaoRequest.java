package br.com.atria.dto.request;

import br.com.atria.model.enums.StatusSolicitacao;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusSolicitacaoRequest(@NotNull(message = "é obrigatório") StatusSolicitacao status) {
}
