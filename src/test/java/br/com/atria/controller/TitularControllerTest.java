package br.com.atria.controller;

import br.com.atria.dto.response.TitularResumoResponse;
import br.com.atria.exception.RecursoNaoEncontradoException;
import br.com.atria.service.TitularService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TitularController.class)
@Import(ConfiguracaoTesteWeb.class)
class TitularControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    TitularService service;

    @Test
    void buscaDaCatDevolveCpfMascarado() throws Exception {
        given(service.buscar("roberto")).willReturn(List.of(new TitularResumoResponse(1L, "Roberto Lucas Almeida",
                "***.***.789-**", "roberto@exemplo.com", "(81) 98100-1234", 4, true)));

        mockMvc.perform(get("/api/titulares").param("busca", "roberto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Roberto Lucas Almeida"))
                .andExpect(jsonPath("$[0].cpfMascarado").value("***.***.789-**"))
                .andExpect(jsonPath("$[0].cpf").doesNotExist());
    }

    @Test
    void titularInexistenteDevolve404NoFormatoPadrao() throws Exception {
        given(service.detalhe(99L)).willThrow(new RecursoNaoEncontradoException("Titular 99 não encontrado."));

        mockMvc.perform(get("/api/titulares/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Titular 99 não encontrado."))
                .andExpect(jsonPath("$.caminho").value("/api/titulares/99"));
    }
}
