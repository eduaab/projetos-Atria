package br.com.atria.service.regra;

import br.com.atria.model.enums.TipoAlerta;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Dados de uma ocorrência que o classificador de prioridade precisa conhecer.
 *
 * @param tempoEmAberto há quanto tempo a situação existe (pendências de canal)
 * @param diasRestantes dias até o prazo (solicitações LGPD)
 */
public record OcorrenciaRisco(TipoAlerta tipo, Duration tempoEmAberto, Long diasRestantes) {

    public static OcorrenciaRisco deCanal(TipoAlerta tipo, LocalDateTime desde, LocalDateTime agora) {
        return new OcorrenciaRisco(tipo, Duration.between(desde, agora), null);
    }

    public static OcorrenciaRisco dePrazo(long diasRestantes) {
        return new OcorrenciaRisco(TipoAlerta.PRAZO_LGPD, null, diasRestantes);
    }

    public static OcorrenciaRisco deAcessoNaoRevogado() {
        return new OcorrenciaRisco(TipoAlerta.ACESSO_NAO_REVOGADO, null, null);
    }
}
