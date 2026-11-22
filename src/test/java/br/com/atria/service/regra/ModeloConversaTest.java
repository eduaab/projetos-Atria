package br.com.atria.service.regra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** O modelo em Java precisa dar a mesma nota que o Python (scikit-learn) deu a 300 conversas do período de teste. */
class ModeloConversaTest {
    private static JsonNode ler(String arquivo) throws Exception {
        try (InputStream in = new ClassPathResource("analise/" + arquivo).getInputStream()) {
            return new ObjectMapper().readTree(in);
        }
    }

    @Test
    void dasMesmasNotasQueOPythonEmTrezentasConversas() throws Exception {
        ModeloConversa modelo = new ModeloConversa(ler("modelo_conversa.json"));
        JsonNode amostra = ler("validacao_conversa.json");
        assertThat(amostra).hasSize(300);
        double maiorDiferenca = 0;
        for (JsonNode c : amostra) {
            double java = modelo.chanceDeVenda(new ModeloConversa.Entrada(c.get("min").asDouble(), c.get("hora").asInt(), c.get("dia").asInt(),
                    c.get("rec").asInt() == 1, c.get("vh").asDouble(), c.get("msgs").asInt(), c.get("midia").asInt() == 1,
                    c.get("post").asInt() == 1, c.get("intencao").asText(), c.get("canal").asText()));
            maiorDiferenca = Math.max(maiorDiferenca, Math.abs(java - c.get("esperado").asDouble()));
            assertThat(java).isCloseTo(c.get("esperado").asDouble(), within(1e-6));
        }
        System.out.println("Maior diferença entre Java e Python: " + maiorDiferenca);
    }

    @Test
    void demoraDiminuiAChanceEIntencaoDeCompraAumenta() throws Exception {
        ModeloConversa modelo = new ModeloConversa(ler("modelo_conversa.json"));
        var rapida = new ModeloConversa.Entrada(10, 15, 2, false, 100, 3, false, false, "compra", "whatsapp");
        var lenta = new ModeloConversa.Entrada(1500, 15, 2, false, 100, 3, false, false, "compra", "whatsapp");
        var reclamacao = new ModeloConversa.Entrada(10, 15, 2, false, 100, 3, false, false, "reclamacao", "whatsapp");
        assertThat(modelo.chanceDeVenda(rapida)).isGreaterThan(modelo.chanceDeVenda(lenta));
        assertThat(modelo.chanceDeVenda(rapida)).isGreaterThan(modelo.chanceDeVenda(reclamacao));
    }
}
