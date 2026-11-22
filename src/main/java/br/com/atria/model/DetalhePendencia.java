package br.com.atria.model;

import java.time.LocalDateTime;

/**
 * Dados operacionais de uma pendência de canal: o que o lojista precisa ver para agir e o que o modelo de conversa
 * precisa para estimar a chance de venda. Os campos de cada tipo ficam nulos nos outros tipos.
 */
public record DetalhePendencia(Long referencia,
                               // mensagem sem resposta
                               String intencao, String texto, Integer hora, Integer diaSemana, Boolean clienteRecorrente,
                               Double valorHistorico, Integer mensagensCliente, Boolean enviouMidia, Boolean origemPost,
                               // pedido não enviado
                               Double valorPedido, LocalDateTime prazoEnvio,
                               // reclamação
                               String tipoReclamacao, Long pedido) {

    public static DetalhePendencia deMensagem(long conversa, String intencao, String texto, int hora, int diaSemana,
                                              boolean recorrente, double valorHistorico, int mensagens, boolean midia, boolean post) {
        return new DetalhePendencia(conversa, intencao, texto, hora, diaSemana, recorrente, valorHistorico, mensagens, midia, post,
                null, null, null, null);
    }

    public static DetalhePendencia dePedido(long pedido, double valor, LocalDateTime prazoEnvio) {
        return new DetalhePendencia(pedido, null, null, null, null, null, null, null, null, null, valor, prazoEnvio, null, null);
    }

    public static DetalhePendencia deReclamacao(long reclamacao, long pedido, String tipo) {
        return new DetalhePendencia(reclamacao, null, null, null, null, null, null, null, null, null, null, null, tipo, pedido);
    }
}
