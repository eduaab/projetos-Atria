package br.com.atria.dto.response;

import java.util.List;

public record MapeamentoResumoResponse(long totalTitulares, long titularesMapeados, long totalDados,
                                       List<ClientesPlataformaResponse> clientesPorPlataforma,
                                       List<ContagemResponse> dadosPorTipo,
                                       List<IndicadorProblemaResponse> indicadores) {
}
