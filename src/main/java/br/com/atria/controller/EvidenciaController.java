package br.com.atria.controller;

import br.com.atria.dto.response.EvidenciaResponse;
import br.com.atria.service.EvidenciaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evidencias")
public class EvidenciaController {
    private final EvidenciaService service;

    public EvidenciaController(EvidenciaService service) {
        this.service = service;
    }

    @GetMapping
    public List<EvidenciaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{protocolo}/comprovante")
    public ResponseEntity<byte[]> comprovante(@PathVariable String protocolo) {
        return RespostaPdf.de(service.gerarComprovante(protocolo));
    }
}
