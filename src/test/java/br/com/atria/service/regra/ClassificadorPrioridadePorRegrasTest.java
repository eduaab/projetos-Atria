package br.com.atria.service.regra;

import br.com.atria.model.enums.Prioridade;
import br.com.atria.model.enums.TipoAlerta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static br.com.atria.RelogioFixo.AGORA;
import static org.assertj.core.api.Assertions.assertThat;

class ClassificadorPrioridadePorRegrasTest {
    private final ClassificadorPrioridade classificador = new ClassificadorPrioridadePorRegras();

    @ParameterizedTest(name = "{0} há {1} h → {2}")
    @CsvSource({
            "RECLAMACAO_PENDENTE, 25, ALTA",
            "RECLAMACAO_PENDENTE, 24, BAIXA",
            "CLIENTE_SEM_RESPOSTA, 5, MEDIA",
            "CLIENTE_SEM_RESPOSTA, 4, BAIXA",
            "PEDIDO_NAO_ENVIADO, 25, MEDIA",
            "PEDIDO_NAO_ENVIADO, 23, BAIXA"})
    void classificaPendenciasDeCanalPeloTempoEmAberto(TipoAlerta tipo, int horas, Prioridade esperada) {
        OcorrenciaRisco ocorrencia = OcorrenciaRisco.deCanal(tipo, AGORA.minusHours(horas), AGORA);
        assertThat(classificador.classificar(ocorrencia)).isEqualTo(esperada);
    }

    @ParameterizedTest(name = "prazo com {0} dia(s) → {1}")
    @CsvSource({"-4, ALTA", "0, ALTA", "2, ALTA", "3, MEDIA", "7, MEDIA", "8, BAIXA", "15, BAIXA"})
    void classificaPrazoLgpd(long dias, Prioridade esperada) {
        assertThat(classificador.classificar(OcorrenciaRisco.dePrazo(dias))).isEqualTo(esperada);
    }

    @Test
    void acessoNaoRevogadoEhSempreAlta() {
        assertThat(classificador.classificar(OcorrenciaRisco.deAcessoNaoRevogado())).isEqualTo(Prioridade.ALTA);
    }
}
