package br.com.atria.dto.response;

import java.time.LocalDateTime;

public record SaudeResponse(String status, String aplicacao, LocalDateTime verificadoEm) {
}
