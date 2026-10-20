package br.com.atria.dto.response;

import java.util.List;

public record OcorrenciasResponse(List<PontoOcorrenciaResponse> serie, List<AlertaResponse> eventos) {
}
