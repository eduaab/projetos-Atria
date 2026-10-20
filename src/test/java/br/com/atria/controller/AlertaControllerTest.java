package br.com.atria.controller;

import br.com.atria.dto.request.AlterarStatusAlertaRequest;
import br.com.atria.dto.response.AlertaResponse;
import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.model.enums.Prioridade;
import br.com.atria.model.enums.StatusAlerta;
import br.com.atria.model.enums.TipoAlerta;
import br.com.atria.service.AlertaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static br.com.atria.RelogioFixo.AGORA;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertaController.class)
@Import(ConfiguracaoTesteWeb.class)
class AlertaControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    AlertaService service;

    @Test
    void alterarStatusDoAlerta() throws Exception {
        given(service.alterarStatus(5L, new AlterarStatusAlertaRequest(StatusAlerta.RESOLVIDO))).willReturn(alerta(StatusAlerta.RESOLVIDO));

        mockMvc.perform(patch("/api/alertas/5/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"RESOLVIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.resolvidoEm").value("2026-10-09T10:00:00"));
    }

    @Test
    void statusInvalidoDevolve400() throws Exception {
        mockMvc.perform(patch("/api/alertas/5/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"QUALQUER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void filtroDePrioridadeInvalidoDevolve400() throws Exception {
        mockMvc.perform(get("/api/alertas").param("prioridade", "URGENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para o parâmetro 'prioridade'."));
    }

    private AlertaResponse alerta(StatusAlerta status) {
        return new AlertaResponse(5L, TipoAlerta.CLIENTE_SEM_RESPOSTA, "Cliente sem resposta", Prioridade.MEDIA, status,
                status.getDescricao(), "Cliente sem resposta no Instagram", "Cliente aguarda resposta.", "instagram", "Instagram",
                OrigemAlerta.PENDENCIA, "Pendência de canal", 3L, 1, 2L, "Ana", "ana@exemplo.com", null, null,
                AGORA.minusHours(6), AGORA);
    }
}
