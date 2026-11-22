package br.com.atria.controller;

import br.com.atria.dto.response.PainelResponse.*;
import br.com.atria.service.PainelService;
import br.com.atria.service.PendenciasPainelService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints do Painel. Os filtros são sempre opcionais: período em dias (7, 30, 60, 120 ou 365; 0 para todo o histórico) e plataforma de venda. */
@RestController
@RequestMapping("/api/painel")
public class PainelController {
    private final PainelService service;
    private final PendenciasPainelService pendencias;

    public PainelController(PainelService service, PendenciasPainelService pendencias) {
        this.service = service;
        this.pendencias = pendencias;
    }

    @GetMapping("/meta")
    public Meta meta() {
        return service.meta();
    }

    @GetMapping("/resumo")
    public Resumo resumo(@RequestParam(defaultValue = "30") int periodo, @RequestParam(required = false) String plataforma) {
        return service.resumo(periodo, plataforma);
    }

    @GetMapping("/serie")
    public Serie serie(@RequestParam(defaultValue = "30") int periodo, @RequestParam(required = false) String plataforma) {
        return service.serie(periodo, plataforma);
    }

    @GetMapping("/canais")
    public Canais canais(@RequestParam(defaultValue = "30") int periodo) {
        return service.canais(periodo);
    }

    @GetMapping("/atendimento")
    public Atendimento atendimento(@RequestParam(defaultValue = "30") int periodo, @RequestParam(defaultValue = "todos") String canal) {
        return service.atendimento(periodo, canal);
    }

    @GetMapping("/posts")
    public Posts posts(@RequestParam(defaultValue = "30") int periodo) {
        return service.posts(periodo);
    }

    @GetMapping("/clientes")
    public Clientes clientes(@RequestParam(defaultValue = "30") int periodo, @RequestParam(required = false) String plataforma) {
        return service.clientes(periodo, plataforma);
    }

    @GetMapping("/reputacao")
    public Reputacao reputacao() {
        return service.reputacao();
    }

    @GetMapping("/pendencias")
    public Pendencias pendencias() {
        return pendencias.pendencias();
    }

    @GetMapping("/modelos")
    public JsonNode modelos() {
        return service.modelos();
    }

    @GetMapping("/efeito-atria")
    public Efeito efeito() {
        return service.efeito();
    }
}
