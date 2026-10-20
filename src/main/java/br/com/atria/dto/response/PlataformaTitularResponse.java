package br.com.atria.dto.response;

import java.util.List;

public record PlataformaTitularResponse(String slug, String nome, String idExterno,
                                        String metodoVinculo, List<String> tiposDado) {
}
