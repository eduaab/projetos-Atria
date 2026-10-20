package br.com.atria.controller;

import br.com.atria.dto.response.SaudeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/health")
public class SaudeController {
    private final Clock clock;

    public SaudeController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping
    public SaudeResponse verificar() {
        return new SaudeResponse("UP", "Atria", LocalDateTime.now(clock));
    }
}
