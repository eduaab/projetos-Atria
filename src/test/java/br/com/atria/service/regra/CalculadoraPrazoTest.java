package br.com.atria.service.regra;

import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.NivelPrazo;
import br.com.atria.model.enums.StatusSolicitacao;
import br.com.atria.model.enums.TipoSolicitacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static br.com.atria.RelogioFixo.AGORA;
import static br.com.atria.RelogioFixo.CLOCK;
import static br.com.atria.RelogioFixo.PROPRIEDADES;
import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraPrazoTest {
    private final CalculadoraPrazo calculadora = new CalculadoraPrazo(CLOCK, PROPRIEDADES);

    @Test
    void prazoEhAberturaMaisQuinzeDias() {
        assertThat(calculadora.calcularPrazo(LocalDateTime.of(2026, 10, 1, 23, 59))).isEqualTo(LocalDate.of(2026, 10, 16));
    }

    @Test
    void diasRestantesUsamOClock() {
        assertThat(calculadora.diasRestantes(LocalDate.of(2026, 10, 10))).isEqualTo(1);
        assertThat(calculadora.diasRestantes(LocalDate.of(2026, 10, 9))).isZero();
        assertThat(calculadora.diasRestantes(LocalDate.of(2026, 10, 6))).isEqualTo(-3);
    }

    @Test
    void solicitacaoVencidaEAbertaFicaAtrasada() {
        SolicitacaoLgpd vencida = solicitacaoAbertaHa(16);
        assertThat(calculadora.estaVencida(vencida)).isTrue();
        assertThat(calculadora.statusEfetivo(vencida)).isEqualTo(StatusSolicitacao.ATRASADA);
    }

    @Test
    void solicitacaoNoUltimoDiaAindaNaoVenceu() {
        assertThat(calculadora.estaVencida(solicitacaoAbertaHa(15))).isFalse();
    }

    @Test
    void solicitacaoConcluidaNuncaFicaAtrasada() {
        SolicitacaoLgpd concluida = solicitacaoAbertaHa(30);
        concluida.concluir(AGORA.minusDays(20));
        assertThat(calculadora.estaVencida(concluida)).isFalse();
        assertThat(calculadora.statusEfetivo(concluida)).isEqualTo(StatusSolicitacao.CONCLUIDA);
    }

    @ParameterizedTest
    @CsvSource({"-1,VERMELHO", "0,LARANJA", "2,LARANJA", "3,AMARELO", "7,AMARELO", "8,VERDE", "15,VERDE"})
    void classificaFaixaVisualDoPrazo(long dias, NivelPrazo esperado) {
        assertThat(calculadora.classificarNivel(dias)).isEqualTo(esperado);
    }

    private SolicitacaoLgpd solicitacaoAbertaHa(int dias) {
        LocalDateTime abertura = AGORA.minusDays(dias);
        return new SolicitacaoLgpd(1L, TipoSolicitacao.ACESSO, CanalOrigem.EMAIL, abertura,
                calculadora.calcularPrazo(abertura), "ATR-TESTE001");
    }
}
