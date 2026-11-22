package br.com.atria.service.regra;

import br.com.atria.repository.AnaliseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Chance de uma conversa virar pedido em 7 dias, calculada pela própria Atria.
 * É a regressão logística treinada no notebook 03: variáveis numéricas padronizadas, intenção e canal em
 * "uma coluna por categoria". Os parâmetros vêm de {@code analise/modelo_conversa.json}; a conferência contra
 * as notas do Python está em {@code ModeloConversaTest}.
 */
@Component
public class ModeloConversa {
    /** Dados de uma conversa. Os nomes de intenção e canal seguem os do treino (ex.: "compra", "whatsapp"). */
    public record Entrada(double minutosEsperando, int hora, int diaSemana, boolean clienteRecorrente, double valorHistorico,
                          int mensagensCliente, boolean enviouMidia, boolean origemPost, String intencao, String canal) {
    }

    private record Numerica(double media, double escala, double coeficiente) {
    }

    private final double intercepto;
    private final Map<String, Numerica> numericas = new HashMap<>();
    private final Map<String, Map<String, Double>> categoricas = new HashMap<>();

    @Autowired
    public ModeloConversa(AnaliseRepository dados) {
        this(dados.modeloConversa());
    }

    public ModeloConversa(JsonNode parametros) {
        this.intercepto = parametros.get("intercepto").asDouble();
        parametros.get("numericas").forEach(n -> numericas.put(n.get("nome").asText(),
                new Numerica(n.get("media").asDouble(), n.get("escala").asDouble(), n.get("coef").asDouble())));
        parametros.get("categoricas").forEach(c -> {
            Map<String, Double> pesos = new HashMap<>();
            for (int i = 0; i < c.get("valores").size(); i++) {
                pesos.put(c.get("valores").get(i).asText(), c.get("coef").get(i).asDouble());
            }
            categoricas.put(c.get("nome").asText(), pesos);
        });
    }

    /** Probabilidade (0 a 1) de a conversa virar pedido em 7 dias. */
    public double chanceDeVenda(Entrada e) {
        double z = intercepto
                + termo("log_resp", Math.log1p(e.minutosEsperando()))
                + termo("hora", e.hora())
                + termo("dia_semana", e.diaSemana())
                + termo("cliente_recorrente", e.clienteRecorrente() ? 1 : 0)
                + termo("valor_historico", e.valorHistorico())
                + termo("msgs_cliente", e.mensagensCliente())
                + termo("enviou_midia", e.enviouMidia() ? 1 : 0)
                + termo("origem_post", e.origemPost() ? 1 : 0)
                + categoricas.get("intencao").getOrDefault(e.intencao(), 0.0)
                + categoricas.get("canal").getOrDefault(e.canal(), 0.0);
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private double termo(String nome, double valor) {
        Numerica n = numericas.get(nome);
        return n.coeficiente() * (valor - n.media()) / n.escala();
    }
}
