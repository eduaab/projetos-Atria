package br.com.atria.controller;

import br.com.atria.exception.RecursoNaoEncontradoException;
import br.com.atria.relatorio.ArquivoPdf;
import br.com.atria.service.RelatorioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RelatorioController.class)
@Import(ConfiguracaoTesteWeb.class)
class RelatorioControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    RelatorioService service;

    @Test
    void downloadDoPdfComoAnexo() throws Exception {
        byte[] pdf = "%PDF-1.4 teste".getBytes(StandardCharsets.US_ASCII);
        given(service.gerarPdf("esg")).willReturn(new ArquivoPdf("atria-esg-2026-10-09.pdf", pdf));

        mockMvc.perform(get("/api/relatorios/esg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("atria-esg-2026-10-09.pdf")))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void tipoDeRelatorioInexistenteDevolve404() throws Exception {
        given(service.gerarPdf("xyz")).willThrow(new RecursoNaoEncontradoException("Relatório 'xyz' não existe."));

        mockMvc.perform(get("/api/relatorios/xyz")).andExpect(status().isNotFound());
    }
}
