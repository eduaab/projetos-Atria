package br.com.atria.controller;

import br.com.atria.dto.request.ExclusaoRequest;
import br.com.atria.dto.response.ExclusaoPreviaResponse;
import br.com.atria.dto.response.ExclusaoResponse;
import br.com.atria.service.ExclusaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/titulares/{id}/exclusao")
public class ExclusaoController {
    private final ExclusaoService service;

    public ExclusaoController(ExclusaoService service) {
        this.service = service;
    }

    @GetMapping("/previa")
    public ExclusaoPreviaResponse previa(@PathVariable Long id) {
        return service.previa(id);
    }

    @PostMapping
    public ResponseEntity<ExclusaoResponse> executar(@PathVariable Long id, @Valid @RequestBody ExclusaoRequest requisicao) {
        ExclusaoResponse resposta = service.executar(id);
        return ResponseEntity.created(URI.create("/api/evidencias/" + resposta.protocolo() + "/comprovante")).body(resposta);
    }
}
