package br.com.atria.dto.response;

import br.com.atria.model.enums.TipoDado;

import java.time.LocalDateTime;

public record DadoIdentificadoResponse(Long id, Long titularId, String titularNome, String cpfMascarado,
                                       String plataformaSlug, String plataformaNome, TipoDado tipoDado,
                                       String tipoDadoDescricao, LocalDateTime encontradoEm) {
}
