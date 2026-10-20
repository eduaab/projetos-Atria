package br.com.atria.controller;

import br.com.atria.dto.response.PlataformaResponse;
import br.com.atria.service.PlataformaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plataformas")
public class PlataformaController {
    private final PlataformaService service;

    public PlataformaController(PlataformaService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlataformaResponse> listar() {
        return service.listar();
    }
}
