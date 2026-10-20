package br.com.atria.service.regra;

import br.com.atria.exception.RegraNegocioException;
import br.com.atria.model.Titular;
import org.junit.jupiter.api.Test;

import static br.com.atria.RelogioFixo.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnonimizadorTitularTest {
    private final AnonimizadorTitular anonimizador = new AnonimizadorTitular();

    @Test
    void previaListaOsCincoCamposPessoais() {
        assertThat(anonimizador.camposQueSeraoApagados(titular()))
                .extracting(CampoPessoal::campo)
                .containsExactly("Nome", "CPF", "E-mail", "Telefone", "CEP");
    }

    @Test
    void anonimizarApagaDadosPessoaisEDevolveCpfMascarado() {
        Titular titular = titular();
        String cpfMascarado = anonimizador.anonimizar(titular, AGORA);

        assertThat(cpfMascarado).isEqualTo("***.***.789-**");
        assertThat(titular.getNome()).isNull();
        assertThat(titular.getCpf()).isNull();
        assertThat(titular.getEmail()).isNull();
        assertThat(titular.getTelefone()).isNull();
        assertThat(titular.getCep()).isNull();
        assertThat(titular.getAnonimizadoEm()).isEqualTo(AGORA);
        assertThat(titular.getNomeExibicao()).isEqualTo("Titular anonimizado #7");
    }

    @Test
    void anonimizarDeNovoEhErroDeRegra() {
        Titular titular = titular();
        anonimizador.anonimizar(titular, AGORA);

        assertThatThrownBy(() -> anonimizador.anonimizar(titular, AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foram excluídos");
        assertThatThrownBy(() -> anonimizador.camposQueSeraoApagados(titular)).isInstanceOf(RegraNegocioException.class);
    }

    private Titular titular() {
        Titular titular = new Titular(1L, "Roberto Lucas Almeida", "123.456.789-09", "roberto@exemplo.com",
                "(81) 99999-0001", "50000-000", AGORA.minusYears(1));
        titular.setId(7L);
        return titular;
    }
}
