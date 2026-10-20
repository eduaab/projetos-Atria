package br.com.atria.controller;

import br.com.atria.dto.request.AlterarStatusAlertaRequest;
import br.com.atria.dto.response.AlertaResponse;
import br.com.atria.dto.response.VarreduraResponse;
import br.com.atria.model.enums.Prioridade;
import br.com.atria.model.enums.StatusAlerta;
import br.com.atria.service.AlertaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {
    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlertaResponse> listar(@RequestParam(required = false) Prioridade prioridade,
                                       @RequestParam(required = false) StatusAlerta status,
                                       @RequestParam(required = false) String plataforma) {
        return service.listar(prioridade, status, plataforma);
    }

    @GetMapping("/{id}")
    public AlertaResponse detalhe(@PathVariable Long id) {
        return service.detalhe(id);
    }

    @PatchMapping("/{id}/status")
    public AlertaResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusAlertaRequest requisicao) {
        return service.alterarStatus(id, requisicao);
    }

    @PostMapping("/varredura")
    public VarreduraResponse executarVarredura() {
        return service.executarVarredura();
    }
}
