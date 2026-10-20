package br.com.atria.service.regra;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MascaradorCpfTest {

    @Test
    void mostraApenasOsDigitosSeteANove() {
        assertThat(MascaradorCpf.mascarar("123.456.789-09")).isEqualTo("***.***.789-**");
        assertThat(MascaradorCpf.mascarar("12345678909")).isEqualTo("***.***.789-**");
    }

    @Test
    void cpfAusenteOuInvalidoViraMascaraVazia() {
        assertThat(MascaradorCpf.mascarar(null)).isEqualTo("***.***.***-**");
        assertThat(MascaradorCpf.mascarar("123")).isEqualTo("***.***.***-**");
    }
}
