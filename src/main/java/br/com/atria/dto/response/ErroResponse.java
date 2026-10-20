package br.com.atria.dto.response;

import java.time.OffsetDateTime;

public record ErroResponse(OffsetDateTime timestamp, int status, String erro, String mensagem, String caminho) {
}
