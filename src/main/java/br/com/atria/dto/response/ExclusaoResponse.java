package br.com.atria.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ExclusaoResponse(String protocolo, Long titularId, LocalDateTime concluidaEm, String cpfMascarado,
                               List<String> plataformas, String hashComprovante) {
}
