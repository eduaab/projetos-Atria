package br.com.atria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sobe a aplicação inteira com os dados fictícios e confere que todos os módulos conversam. */
@SpringBootTest
@AutoConfigureMockMvc
class AtriaApplicationIntegracaoTest {
    @Autowired
    MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/api/health", "/api/dashboard", "/api/plataformas", "/api/conexoes", "/api/conexoes/whatsapp",
            "/api/mapeamento/resumo", "/api/mapeamento/dados?plataforma=nuvemshop&tipo=CPF", "/api/titulares?busca=darci",
            "/api/titulares/2", "/api/titulares/2/exclusao/previa", "/api/solicitacoes?status=PENDENTE", "/api/alertas?prioridade=ALTA",
            "/api/alertas/1", "/api/riscos/indicadores", "/api/riscos/ocorrencias", "/api/relatorios/indicadores",
            "/api/evidencias", "/api/auditoria"})
    void endpointsDeConsultaRespondem200(String caminho) throws Exception {
        mockMvc.perform(get(caminho)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"esg", "anpd", "comercial"})
    void relatoriosSaoPdfsDeVerdade(String tipo) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/relatorios/" + tipo))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        assertThat(new String(resultado.getResponse().getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void seedTrazOsCenariosDoPrototipo() throws Exception {
        mockMvc.perform(get("/api/titulares").param("busca", "Roberto Lucas"))
                .andExpect(jsonPath("$[0].nome").value("Roberto Lucas Almeida"));
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.nomeLoja").value("Flor de Lis Moda"))
                .andExpect(jsonPath("$.contadores.plataformasConectadas").value(4))
                .andExpect(jsonPath("$.statusAnpd.emConformidade").value(false));
    }
}
