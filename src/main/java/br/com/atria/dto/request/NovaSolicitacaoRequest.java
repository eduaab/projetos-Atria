package br.com.atria.dto.request;

import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.TipoSolicitacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NovaSolicitacaoRequest(
        @NotNull(message = "é obrigatório") @Positive Long titularId,
        @NotNull(message = "é obrigatório") TipoSolicitacao tipo,
        @NotNull(message = "é obrigatório") CanalOrigem canalOrigem) {
}
