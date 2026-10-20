package br.com.atria.controller;

import br.com.atria.dto.response.RelatorioIndicadoresResponse;
import br.com.atria.service.RelatorioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {
    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/indicadores")
    public RelatorioIndicadoresResponse indicadores() {
        return service.indicadores();
    }

    @GetMapping("/{tipo}")
    public ResponseEntity<byte[]> baixar(@PathVariable String tipo) {
        return RespostaPdf.de(service.gerarPdf(tipo));
    }
}
