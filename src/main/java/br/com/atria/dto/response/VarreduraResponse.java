package br.com.atria.dto.response;

import java.time.LocalDateTime;

public record VarreduraResponse(int alertasCriados, int alertasReclassificados, long alertasAbertos,
                                String fraseResumo, LocalDateTime executadaEm) {
}
