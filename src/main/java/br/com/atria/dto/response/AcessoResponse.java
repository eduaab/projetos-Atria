package br.com.atria.dto.response;

import br.com.atria.model.enums.PapelUsuario;

import java.time.LocalDateTime;

public record AcessoResponse(Long id, String usuarioNome, PapelUsuario papel, boolean usuarioDesligado,
                             String plataforma, LocalDateTime concedidoEm, LocalDateTime revogadoEm) {
}
