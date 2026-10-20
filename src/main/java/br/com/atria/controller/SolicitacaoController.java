package br.com.atria.controller;

import br.com.atria.dto.request.AlterarStatusSolicitacaoRequest;
import br.com.atria.dto.request.NovaSolicitacaoRequest;
import br.com.atria.dto.response.SolicitacaoResponse;
import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.StatusSolicitacao;
import br.com.atria.model.enums.TipoSolicitacao;
import br.com.atria.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/solicitacoes")
public class SolicitacaoController {
    private final SolicitacaoService service;

    public SolicitacaoController(SolicitacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<SolicitacaoResponse> listar(@RequestParam(required = false) StatusSolicitacao status,
                                            @RequestParam(required = false) TipoSolicitacao tipo,
                                            @RequestParam(required = false) CanalOrigem canal) {
        return service.listar(status, tipo, canal);
    }

    @PostMapping
    public ResponseEntity<SolicitacaoResponse> registrar(@Valid @RequestBody NovaSolicitacaoRequest requisicao) {
        SolicitacaoResponse criada = service.registrar(requisicao);
        return ResponseEntity.created(URI.create("/api/solicitacoes/" + criada.id())).body(criada);
    }

    @PatchMapping("/{id}/status")
    public SolicitacaoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusSolicitacaoRequest requisicao) {
        return service.alterarStatus(id, requisicao);
    }
}
