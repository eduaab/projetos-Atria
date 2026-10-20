package br.com.atria.controller;

import br.com.atria.dto.response.TitularDetalheResponse;
import br.com.atria.dto.response.TitularResumoResponse;
import br.com.atria.service.TitularService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/titulares")
public class TitularController {
    private final TitularService service;

    public TitularController(TitularService service) {
        this.service = service;
    }

    @GetMapping
    public List<TitularResumoResponse> buscar(@RequestParam(defaultValue = "") String busca) {
        return service.buscar(busca);
    }

    @GetMapping("/{id}")
    public TitularDetalheResponse detalhe(@PathVariable Long id) {
        return service.detalhe(id);
    }
}
