package br.com.atria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** "O que precisa de mim" (Painel) e Central de alertas (e Home) precisam mostrar as mesmas ocorrências. */
@SpringBootTest
@AutoConfigureMockMvc
class PainelEAlertasIntegradosTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper json;

    private JsonNode consultar(String caminho) throws Exception {
        return json.readTree(mockMvc.perform(get(caminho)).andReturn().getResponse().getContentAsString());
    }

    @Test
    void cadaItemDoPainelTemUmAlertaAbertoDoMesmoTipoEPlataforma() throws Exception {
        JsonNode painel = consultar("/api/painel/pendencias");
        JsonNode alertas = consultar("/api/alertas");
        List<Long> idsAlertas = new ArrayList<>();
        alertas.forEach(a -> idsAlertas.add(a.get("id").asLong()));

        int total = painel.get("mensagens").size() + painel.get("pedidos").size() + painel.get("reclamacoes").size();
        assertThat(total).isGreaterThan(10);
        long abertosDeCanal = 0;
        for (JsonNode a : alertas) {
            String tipo = a.get("tipo").asText();
            if (!"ACESSO_NAO_REVOGADO".equals(tipo) && !"PRAZO_LGPD".equals(tipo) && !"RESOLVIDO".equals(a.get("status").asText())) abertosDeCanal++;
        }
        assertThat(abertosDeCanal).isEqualTo(total);

        for (String grupo : new String[]{"mensagens", "pedidos", "reclamacoes"}) {
            for (JsonNode item : painel.get(grupo)) {
                assertThat(item.get("alertaId").isNull()).isFalse();
                JsonNode alerta = consultar("/api/alertas/" + item.get("alertaId").asLong());
                assertThat(alerta.get("status").asText()).isNotEqualTo("RESOLVIDO");
                assertThat(alerta.get("plataformaSlug").asText()).isEqualTo(item.get("canal").asText());
            }
        }
        assertThat(idsAlertas).doesNotHaveDuplicates();
    }

    @Test
    void mensagensVemOrdenadasPelaChanceDeVendaCalculadaNaAtria() throws Exception {
        JsonNode mensagens = consultar("/api/painel/pendencias").get("mensagens");
        double anterior = 1;
        for (JsonNode m : mensagens) {
            double score = m.get("score").asDouble();
            assertThat(score).isBetween(0.0, 1.0).isLessThanOrEqualTo(anterior);
            anterior = score;
        }
        assertThat(mensagens.get(0).get("score").asDouble()).isGreaterThan(mensagens.get(mensagens.size() - 1).get("score").asDouble());
    }

    @Test
    void homeUsaOsMesmosAlertasDoPainel() throws Exception {
        JsonNode painel = consultar("/api/painel/pendencias");
        JsonNode dashboard = consultar("/api/dashboard");
        int pendenciasDeCanal = painel.get("mensagens").size() + painel.get("pedidos").size() + painel.get("reclamacoes").size();
        assertThat(dashboard.toString()).contains("\"alertasAbertos\"");
        assertThat(dashboard.get("contadores").get("alertasAbertos").asInt()).isGreaterThanOrEqualTo(pendenciasDeCanal);
    }
}
