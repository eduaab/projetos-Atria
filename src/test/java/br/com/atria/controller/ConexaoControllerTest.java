package br.com.atria.controller;

import br.com.atria.dto.response.ConexaoResponse;
import br.com.atria.dto.response.PlataformaResponse;
import br.com.atria.exception.RegraNegocioException;
import br.com.atria.model.enums.StatusConexao;
import br.com.atria.model.enums.TipoPlataforma;
import br.com.atria.service.ConexaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.atria.RelogioFixo.AGORA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConexaoController.class)
@Import(ConfiguracaoTesteWeb.class)
class ConexaoControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    ConexaoService service;

    @Test
    void conectarDevolve201ComDataDeConexao() throws Exception {
        PlataformaResponse shopee = new PlataformaResponse(5L, "shopee", "Shopee", TipoPlataforma.MARKETPLACE, "Marketplace", List.of("Ler pedidos"));
        given(service.conectar("shopee")).willReturn(new ConexaoResponse(shopee, StatusConexao.ATIVA, true, AGORA, null, AGORA));

        mockMvc.perform(post("/api/conexoes/shopee"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/conexoes/shopee"))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.conectadaEm").value("2026-10-09T10:00:00"));
    }

    @Test
    void desconectarDevolve204() throws Exception {
        mockMvc.perform(delete("/api/conexoes/whatsapp")).andExpect(status().isNoContent());
        verify(service).desconectar("whatsapp");
    }

    @Test
    void desconectarPlataformaNaoConectadaDevolve422() throws Exception {
        willThrow(new RegraNegocioException("Shein não está conectada.")).given(service).desconectar("shein");

        mockMvc.perform(delete("/api/conexoes/shein"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Shein não está conectada."));
    }
}
