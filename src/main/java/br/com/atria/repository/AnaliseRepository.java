package br.com.atria.repository;

import br.com.atria.model.analise.Registros.ClienteResumo;
import br.com.atria.model.analise.Registros.EfeitoSemana;
import br.com.atria.model.analise.Registros.LinhaAtendimento;
import br.com.atria.model.analise.Registros.LinhaDiaria;
import br.com.atria.model.analise.Registros.LinhaReputacao;
import br.com.atria.model.analise.Registros.PedidoCliente;
import br.com.atria.model.analise.Registros.PostInstagram;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;

/** Acesso aos dados analíticos (simulados) usados no Painel. Somente leitura. */
public interface AnaliseRepository {
    List<LinhaDiaria> diario();

    List<LinhaAtendimento> atendimento();

    List<PostInstagram> posts();

    List<LinhaReputacao> reputacao();

    List<ClienteResumo> clientes();

    List<PedidoCliente> pedidosCliente();

    List<EfeitoSemana> efeitoSemanal();

    /** Resultado pronto da estimativa do Efeito Atria (diferença em diferenças). */
    JsonNode efeitoDid();

    /** Pendências em aberto na data de referência (com os dados de cada conversa, pedido ou reclamação), usadas para carregar a Atria. */
    JsonNode pendencias();

    /** Parâmetros da regressão logística de conversas (treinada nos notebooks), para a Atria calcular a chance de venda. */
    JsonNode modeloConversa();

    /** Métricas e curvas dos modelos treinados nos notebooks. */
    JsonNode modelos();

    JsonNode regressao();

    /** Último dia com dados: é o "ontem" do Painel. */
    LocalDate dataReferencia();
}
