package br.com.atria.dto.response;

import br.com.atria.model.enums.AcaoAuditoria;

import java.time.LocalDateTime;

public record AuditoriaResponse(Long id, AcaoAuditoria acao, String acaoDescricao, String descricao,
                                String entidade, Long entidadeId, LocalDateTime ocorridoEm) {
}
