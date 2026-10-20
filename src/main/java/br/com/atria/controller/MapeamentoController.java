package br.com.atria.controller;

import br.com.atria.dto.response.DadoIdentificadoResponse;
import br.com.atria.dto.response.MapeamentoResumoResponse;
import br.com.atria.model.enums.TipoDado;
import br.com.atria.service.MapeamentoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mapeamento")
public class MapeamentoController {
    private final MapeamentoService service;

    public MapeamentoController(MapeamentoService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public MapeamentoResumoResponse resumo() {
        return service.resumo();
    }

    @GetMapping("/dados")
    public List<DadoIdentificadoResponse> dados(@RequestParam(required = false) String plataforma,
                                                @RequestParam(required = false) TipoDado tipo) {
        return service.dados(plataforma, tipo);
    }
}
