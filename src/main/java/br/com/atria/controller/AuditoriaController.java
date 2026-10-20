package br.com.atria.controller;

import br.com.atria.dto.response.AuditoriaResponse;
import br.com.atria.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditoriaResponse> listar(@RequestParam(required = false) Integer limite) {
        return service.listar(limite);
    }
}
