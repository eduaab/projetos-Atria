package br.com.atria.controller;

import br.com.atria.dto.response.ConexaoDetalheResponse;
import br.com.atria.dto.response.ConexaoResponse;
import br.com.atria.service.ConexaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/conexoes")
public class ConexaoController {
    private final ConexaoService service;

    public ConexaoController(ConexaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConexaoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{slug}")
    public ConexaoDetalheResponse detalhe(@PathVariable String slug) {
        return service.detalhe(slug);
    }

    @PostMapping("/{slug}")
    public ResponseEntity<ConexaoResponse> conectar(@PathVariable String slug) {
        ConexaoResponse conexao = service.conectar(slug);
        return ResponseEntity.created(URI.create("/api/conexoes/" + conexao.plataforma().slug())).body(conexao);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> desconectar(@PathVariable String slug) {
        service.desconectar(slug);
        return ResponseEntity.noContent().build();
    }
}
