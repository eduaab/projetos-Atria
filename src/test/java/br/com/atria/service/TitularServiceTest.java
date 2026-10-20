package br.com.atria.service;

import br.com.atria.dto.response.TitularResumoResponse;
import br.com.atria.model.Titular;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TitularServiceTest {
    private CenarioServicos cenario;

    @BeforeEach
    void montarCenario() {
        cenario = new CenarioServicos();
        cenario.titular("Darci Henrique Ayres", "529.982.247-25", "darci@exemplo.com");
        cenario.titular("Débora Campos", "111.444.777-35", "debora@exemplo.com");
    }

    @Test
    void buscaPorNomeIgnoraAcentoEMaiusculas() {
        assertThat(cenario.titularService.buscar("DEBORA")).extracting(TitularResumoResponse::nome).containsExactly("Débora Campos");
    }

    @Test
    void buscaPorCpfComOuSemPontuacaoEMascaraNaListagem() {
        assertThat(cenario.titularService.buscar("529.982")).singleElement()
                .extracting(TitularResumoResponse::cpfMascarado).isEqualTo("***.***.247-**");
        assertThat(cenario.titularService.buscar("52998224725")).hasSize(1);
    }

    @Test
    void buscaPorEmailETelefone() {
        assertThat(cenario.titularService.buscar("darci@")).hasSize(1);
        assertThat(cenario.titularService.buscar("98888")).hasSize(2);
    }

    @Test
    void buscaVaziaListaTodosEmOrdemAlfabetica() {
        assertThat(cenario.titularService.buscar("")).extracting(TitularResumoResponse::nome)
                .containsExactly("Darci Henrique Ayres", "Débora Campos");
    }

    @Test
    void detalheMostraCpfCompleto() {
        Titular darci = cenario.titulares.findAll().get(0);
        assertThat(cenario.titularService.detalhe(darci.getId()).cpf()).isEqualTo("529.982.247-25");
    }
}
