package br.com.atria.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TitularDetalheResponse(Long id, String nome, boolean anonimizado, String cpf, String email,
                                     String telefone, String cep, LocalDateTime criadoEm, LocalDateTime anonimizadoEm,
                                     BigDecimal gastosTotais, List<PlataformaTitularResponse> plataformas,
                                     List<InteracaoResponse> interacoes, List<SolicitacaoResponse> solicitacoes) {
}
