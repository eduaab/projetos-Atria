package br.com.atria.dto.response;

import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.NivelPrazo;
import br.com.atria.model.enums.StatusSolicitacao;
import br.com.atria.model.enums.TipoSolicitacao;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitacaoResponse(Long id, String protocolo, Long titularId, String titularNome, String titularEmail,
                                  TipoSolicitacao tipo, String tipoDescricao,
                                  CanalOrigem canalOrigem, String canalDescricao,
                                  LocalDateTime abertaEm, LocalDate prazo, long diasRestantes, NivelPrazo nivelPrazo,
                                  StatusSolicitacao status, String statusDescricao, LocalDateTime concluidaEm) {
}
