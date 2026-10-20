package br.com.atria.dto.response;

import java.time.LocalDate;

public record PontoOcorrenciaResponse(LocalDate semana, long abertos, long resolvidos) {
}
