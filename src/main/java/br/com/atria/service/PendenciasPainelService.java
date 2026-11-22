package br.com.atria.service;

import br.com.atria.dto.response.PainelResponse.PendenciaMensagem;
import br.com.atria.dto.response.PainelResponse.PendenciaPedido;
import br.com.atria.dto.response.PainelResponse.PendenciaReclamacao;
import br.com.atria.dto.response.PainelResponse.Pendencias;
import br.com.atria.model.Alerta;
import br.com.atria.model.DetalhePendencia;
import br.com.atria.model.PendenciaCanal;
import br.com.atria.model.Plataforma;
import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.model.enums.TipoAlerta;
import br.com.atria.model.enums.TipoPendencia;
import br.com.atria.repository.AlertaRepository;
import br.com.atria.repository.PendenciaCanalRepository;
import br.com.atria.service.regra.ClassificadorPrioridade;
import br.com.atria.service.regra.ModeloConversa;
import br.com.atria.service.regra.OcorrenciaRisco;
import br.com.atria.service.regra.Plural;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * "O que precisa de mim agora": as pendências de canal abertas, as mesmas que geram os alertas da Central de alertas
 * e da Home. As mensagens vêm ordenadas pela chance de venda, calculada pela própria Atria ({@link ModeloConversa}) com
 * o tempo que a mensagem já está parada. Só entram plataformas conectadas, como na varredura de alertas.
 */
@Service
public class PendenciasPainelService {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter PRAZO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PendenciaCanalRepository pendencias;
    private final AlertaRepository alertas;
    private final LojistaAtualService lojistaAtual;
    private final PlataformaService plataformas;
    private final ClassificadorPrioridade classificador;
    private final ModeloConversa modelo;
    private final ConexaoService conexoes;
    private final Clock clock;

    public PendenciasPainelService(PendenciaCanalRepository pendencias, AlertaRepository alertas, LojistaAtualService lojistaAtual,
                                   PlataformaService plataformas, ClassificadorPrioridade classificador, ModeloConversa modelo, ConexaoService conexoes, Clock clock) {
        this.pendencias = pendencias;
        this.alertas = alertas;
        this.lojistaAtual = lojistaAtual;
        this.plataformas = plataformas;
        this.classificador = classificador;
        this.modelo = modelo;
        this.conexoes = conexoes;
        this.clock = clock;
    }

    public Pendencias pendencias() {
        LocalDateTime agora = LocalDateTime.now(clock);
        Map<Long, Plataforma> porId = plataformas.mapaPorId();
        Set<Long> conectadas = conexoes.idsPlataformasConectadas(); // plataformas desconectadas não geram alertas, nem aparecem aqui
        List<PendenciaCanal> abertas = pendencias.findByLojistaIdAndResolvidaFalse(lojistaAtual.id()).stream()
                .filter(p -> p.getDetalhe() != null && conectadas.contains(p.getPlataformaId())).toList();

        List<PendenciaMensagem> mensagens = abertas.stream().filter(p -> p.getTipo() == TipoPendencia.MENSAGEM_SEM_RESPOSTA).map(p -> {
            DetalhePendencia d = p.getDetalhe();
            Plataforma canal = porId.get(p.getPlataformaId());
            double chance = modelo.chanceDeVenda(new ModeloConversa.Entrada(minutos(p, agora), d.hora(), d.diaSemana(), d.clienteRecorrente(),
                    d.valorHistorico(), d.mensagensCliente(), d.enviouMidia(), d.origemPost(), d.intencao(), canal.getSlug()));
            return new PendenciaMensagem(d.referencia(), alertaDe(p), canal.getSlug(), d.intencao(), d.texto(), horas(p, agora),
                    prioridade(p, agora), Math.round(chance * 1000) / 1000.0);
        }).sorted(Comparator.comparingDouble(PendenciaMensagem::score).reversed()).toList();

        List<PendenciaPedido> pedidos = abertas.stream().filter(p -> p.getTipo() == TipoPendencia.PEDIDO_NAO_ENVIADO).map(p -> {
            DetalhePendencia d = p.getDetalhe();
            return new PendenciaPedido(d.referencia(), alertaDe(p), porId.get(p.getPlataformaId()).getSlug(), d.valorPedido(), horas(p, agora),
                    PRAZO.format(d.prazoEnvio()), prioridade(p, agora));
        }).sorted(Comparator.comparingDouble(PendenciaPedido::horas).reversed()).toList();

        List<PendenciaReclamacao> reclamacoes = abertas.stream().filter(p -> p.getTipo() == TipoPendencia.RECLAMACAO_PENDENTE).map(p -> {
            DetalhePendencia d = p.getDetalhe();
            return new PendenciaReclamacao(d.referencia(), alertaDe(p), d.pedido(), porId.get(p.getPlataformaId()).getSlug(), d.tipoReclamacao(),
                    horas(p, agora), prioridade(p, agora));
        }).sorted(Comparator.comparingDouble(PendenciaReclamacao::horas).reversed()).toList();

        int total = mensagens.size() + pedidos.size() + reclamacoes.size();
        String leitura = total == 0 ? "Nada esperando a sua ação agora."
                : "Há " + Plural.de(total, "item esperando", "itens esperando") + " a sua ação: "
                + Plural.de(mensagens.size(), "mensagem sem resposta", "mensagens sem resposta") + ", "
                + Plural.de(pedidos.size(), "pedido acima do prazo de separação", "pedidos acima do prazo de separação") + " e "
                + Plural.de(reclamacoes.size(), "reclamação aberta", "reclamações abertas") + ".";
        return new Pendencias(mensagens, pedidos, reclamacoes, leitura);
    }

    private static double minutos(PendenciaCanal p, LocalDateTime agora) {
        return Math.max(0, Duration.between(p.getDesde(), agora).toMinutes());
    }

    private static double horas(PendenciaCanal p, LocalDateTime agora) {
        return Math.round(minutos(p, agora) / 6.0) / 10.0;
    }

    private String prioridade(PendenciaCanal p, LocalDateTime agora) {
        TipoAlerta tipo = switch (p.getTipo()) {
            case MENSAGEM_SEM_RESPOSTA -> TipoAlerta.CLIENTE_SEM_RESPOSTA;
            case RECLAMACAO_PENDENTE -> TipoAlerta.RECLAMACAO_PENDENTE;
            case PEDIDO_NAO_ENVIADO -> TipoAlerta.PEDIDO_NAO_ENVIADO;
        };
        return classificador.classificar(OcorrenciaRisco.deCanal(tipo, p.getDesde(), agora)).name();
    }

    private Long alertaDe(PendenciaCanal p) {
        return alertas.findByTipoOrigemAndOrigemId(OrigemAlerta.PENDENCIA, p.getId()).stream().findFirst().map(Alerta::getId).orElse(null);
    }
}
