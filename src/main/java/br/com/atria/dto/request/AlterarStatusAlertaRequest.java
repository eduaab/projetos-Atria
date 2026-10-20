package br.com.atria.dto.request;

import br.com.atria.model.enums.StatusAlerta;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusAlertaRequest(@NotNull(message = "é obrigatório") StatusAlerta status) {
}
