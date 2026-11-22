package br.com.atria.dto.response;

import java.util.List;

/** Respostas do Painel (indicadores, séries e leituras prontas para exibir). Porcentagens vão de 0 a 100. */
public final class PainelResponse {
    private PainelResponse() {
    }

    public record Opcao(String codigo, String nome) {
    }

    public record Meta(String referencia, String primeiroDia, List<Opcao> plataformas, List<Integer> periodos) {
    }

    /** formato: "moeda", "percentual", "numero" ou "horas". comparacao é o valor do período de referência da comparação. */
    public record Indicador(String id, String rotulo, double valor, Double comparacao, String formato, boolean altaEhBom, String rotuloComparacao) {
    }

    public record Resumo(String referencia, String periodoInicio, List<Indicador> ontem, List<Indicador> periodo, String leitura) {
    }

    public record Ponto(String data, double receita, double lucro, int pedidos) {
    }

    public record Serie(String granularidade, List<Ponto> pontos, String leitura) {
    }

    public record Canal(String codigo, String nome, int pedidos, double receita, double lucro, double margem,
                        double cancelamento, double atraso, double reclamacao, Double margemAnterior) {
    }

    public record Canais(List<Canal> canais, String leitura) {
    }

    public record FaixaConversao(String faixa, int conversas, double conversao) {
    }

    public record PontoPct(String semana, double valor) {
    }

    public record Atendimento(String canal, int conversas, double respondidasAte1h, double semResposta, double tempoMedioMin,
                              List<FaixaConversao> faixas, List<PontoPct> semanal, String leitura) {
    }

    public record ResumoTipoPost(String tipo, int posts, double alcanceMedio, double conversasPorPost, double engajamentoMedio) {
    }

    public record PostTop(int id, String tipo, String data, int alcance, int salvamentosEcompartilhamentos, int curtidas, int conversas) {
    }

    /** Posts agrupados em terços por salvamentos + compartilhamentos; intervalo descreve a faixa. */
    public record FaixaPost(String faixa, String intervalo, int posts, double conversasPorPost) {
    }

    public record Posts(List<ResumoTipoPost> tipos, List<PostTop> melhores, List<FaixaPost> engajamento, String leitura) {
    }

    public record Segmento(String nome, int clientes, double ticketMedio, double pedidosPorCliente, double gastoMedio) {
    }

    public record CidadeCliente(String cidade, int clientes) {
    }

    public record Clientes(int ativos, int novos, int recorrentes, double taxaRecompra90d, List<Segmento> segmentos,
                           List<CidadeCliente> cidades, String leitura) {
    }

    public record MarketplaceReputacao(String codigo, String nome, String nivel, String nivelAnterior, double score,
                                       double atraso, double cancelamento, double reclamacao, int penalidade, List<Double> serie) {
    }

    public record Reputacao(String semana, List<MarketplaceReputacao> marketplaces, String leitura) {
    }

    /** Antes e depois da própria loja. melhorSeMenor: true quando um valor menor é melhor (atraso, sem resposta). */
    public record IndicadorEfeito(String id, String rotulo, String unidade, double antes, double depois, boolean melhorSeMenor) {
    }

    public record Efeito(String adocao, String fimAdaptacao, int semanasAntes, List<PontoPct> serie, List<IndicadorEfeito> indicadores,
                         List<PontoPct> serieRedes, List<IndicadorEfeito> indicadoresRedes, String leitura) {
    }

    public record PendenciaMensagem(long id, Long alertaId, String canal, String intencao, String texto, double horas, String prio, double score) {
    }

    public record PendenciaPedido(long id, Long alertaId, String canal, double valor, double horas, String prazo, String prio) {
    }

    public record PendenciaReclamacao(long id, Long alertaId, long pedido, String canal, String tipo, double horas, String prio) {
    }

    /** O que precisa do lojista agora: as mesmas ocorrências abertas da Central de alertas, com a chance de venda das mensagens. */
    public record Pendencias(List<PendenciaMensagem> mensagens, List<PendenciaPedido> pedidos, List<PendenciaReclamacao> reclamacoes, String leitura) {
    }
}
