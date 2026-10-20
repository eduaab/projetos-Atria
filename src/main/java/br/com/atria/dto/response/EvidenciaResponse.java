package br.com.atria.dto.response;

import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.TipoSolicitacao;

import java.time.LocalDateTime;
import java.util.List;

public record EvidenciaResponse(Long id, String protocolo, TipoSolicitacao tipoSolicitacao, String tipoDescricao,
                                CanalOrigem canalOrigem, String canalDescricao, LocalDateTime abertaEm,
                                LocalDateTime concluidaEm, LocalDateTime geradaEm, String cpfMascarado,
                                List<String> plataformas, String hashComprovante) {
}
