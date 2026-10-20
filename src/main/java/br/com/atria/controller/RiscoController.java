package br.com.atria.controller;

import br.com.atria.dto.response.OcorrenciasResponse;
import br.com.atria.dto.response.RiscoIndicadoresResponse;
import br.com.atria.service.RiscoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/riscos")
public class RiscoController {
    private final RiscoService service;

    public RiscoController(RiscoService service) {
        this.service = service;
    }

    @GetMapping("/indicadores")
    public RiscoIndicadoresResponse indicadores() {
        return service.indicadores();
    }

    @GetMapping("/ocorrencias")
    public OcorrenciasResponse ocorrencias() {
        return service.ocorrencias();
    }
}
