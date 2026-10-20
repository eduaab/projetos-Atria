package br.com.atria.dto.response;

import br.com.atria.model.enums.StatusConexao;

import java.time.LocalDateTime;

/** Situação de uma plataforma do catálogo para o lojista; {@code status} é nulo se nunca foi conectada. */
public record ConexaoResponse(PlataformaResponse plataforma, StatusConexao status, boolean conectada,
                              LocalDateTime conectadaEm, LocalDateTime desconectadaEm,
                              LocalDateTime ultimaSincronizacao) {
}
