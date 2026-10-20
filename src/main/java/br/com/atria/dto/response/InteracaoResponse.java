package br.com.atria.dto.response;

import br.com.atria.model.enums.TipoInteracao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** {@code destaque} marca interações que pedem atenção (reclamações e solicitações LGPD). */
public record InteracaoResponse(Long id, TipoInteracao tipo, String descricao, String plataforma,
                                LocalDateTime data, BigDecimal valor, boolean destaque) {
}
