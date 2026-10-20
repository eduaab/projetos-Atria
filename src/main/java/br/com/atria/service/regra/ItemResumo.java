package br.com.atria.service.regra;

import br.com.atria.model.enums.TipoAlerta;

/** Um alerta aberto reduzido ao que a frase-resumo precisa: tipo e plataforma (opcional). */
public record ItemResumo(TipoAlerta tipo, String plataforma) {
}
