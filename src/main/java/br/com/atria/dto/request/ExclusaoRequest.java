package br.com.atria.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ExclusaoRequest(
        @NotNull(message = "é obrigatória")
        @AssertTrue(message = "a exclusão precisa ser confirmada")
        Boolean confirmacao) {
}
