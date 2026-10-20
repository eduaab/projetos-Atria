package br.com.atria.controller;

import br.com.atria.dto.response.AcessoResponse;
import br.com.atria.service.AcessoService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/acessos")
public class AcessoController {
    private final AcessoService service;

    public AcessoController(AcessoService service) {
        this.service = service;
    }

    @PostMapping("/{id}/revogacao")
    public AcessoResponse revogar(@PathVariable Long id) {
        return service.revogar(id);
    }
}
