package br.com.atria.controller;

import br.com.atria.dto.response.ExclusaoResponse;
import br.com.atria.exception.RegraNegocioException;
import br.com.atria.service.ExclusaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.atria.RelogioFixo.AGORA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExclusaoController.class)
@Import(ConfiguracaoTesteWeb.class)
class ExclusaoControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    ExclusaoService service;

    @Test
    void exclusaoConfirmadaDevolve201ComProtocolo() throws Exception {
        given(service.executar(1L)).willReturn(new ExclusaoResponse("ATR-1A2B3C4D", 1L, AGORA, "***.***.789-**",
                List.of("WhatsApp"), "abc"));

        mockMvc.perform(post("/api/titulares/1/exclusao").contentType(MediaType.APPLICATION_JSON).content("{\"confirmacao\": true}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/evidencias/ATR-1A2B3C4D/comprovante"))
                .andExpect(jsonPath("$.protocolo").value("ATR-1A2B3C4D"));
    }

    @Test
    void exclusaoSemConfirmacaoDevolve400() throws Exception {
        mockMvc.perform(post("/api/titulares/1/exclusao").contentType(MediaType.APPLICATION_JSON).content("{\"confirmacao\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("confirmacao: a exclusão precisa ser confirmada"));
        verify(service, never()).executar(anyLong());
    }

    @Test
    void exclusaoRepetidaDevolve422NoFormatoPadrao() throws Exception {
        given(service.executar(1L)).willThrow(new RegraNegocioException("Os dados deste titular já foram excluídos em 09/10/2026."));

        mockMvc.perform(post("/api/titulares/1/exclusao").contentType(MediaType.APPLICATION_JSON).content("{\"confirmacao\": true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.erro").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.mensagem").value("Os dados deste titular já foram excluídos em 09/10/2026."))
                .andExpect(jsonPath("$.caminho").value("/api/titulares/1/exclusao"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
