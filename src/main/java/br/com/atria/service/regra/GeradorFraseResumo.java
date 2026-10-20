package br.com.atria.service.regra;

import br.com.atria.model.enums.TipoAlerta;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta a frase da Home, agrupando os alertas por tipo e plataforma, por exemplo:
 * "Você tem 1 cliente sem resposta no Instagram, 1 reclamação não atendida no WhatsApp
 * e 2 pedidos não enviados no Mercado Livre."
 */
@Component
public class GeradorFraseResumo {
    public static final String SEM_PENDENCIAS = "Nenhuma pendência no momento. Está tudo em dia!";

    private static final Map<TipoAlerta, String[]> EXPRESSOES = Map.of(
            TipoAlerta.CLIENTE_SEM_RESPOSTA, new String[]{"cliente sem resposta", "clientes sem resposta"},
            TipoAlerta.RECLAMACAO_PENDENTE, new String[]{"reclamação não atendida", "reclamações não atendidas"},
            TipoAlerta.PEDIDO_NAO_ENVIADO, new String[]{"pedido não enviado", "pedidos não enviados"},
            TipoAlerta.ACESSO_NAO_REVOGADO, new String[]{"acesso de ex-funcionário não revogado", "acessos de ex-funcionários não revogados"},
            TipoAlerta.PRAZO_LGPD, new String[]{"solicitação LGPD com prazo correndo", "solicitações LGPD com prazo correndo"});

    public String gerar(List<ItemResumo> itens) {
        if (itens.isEmpty()) return SEM_PENDENCIAS;

        Map<ItemResumo, Long> contagem = new TreeMap<>(Comparator
                .comparing(ItemResumo::tipo)
                .thenComparing(ItemResumo::plataforma, Comparator.nullsLast(Comparator.naturalOrder())));
        contagem.putAll(itens.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting())));

        List<String> partes = new ArrayList<>();
        contagem.forEach((item, quantidade) -> partes.add(descrever(item, quantidade)));
        return "Você tem " + juntar(partes) + ".";
    }

    private String descrever(ItemResumo item, long quantidade) {
        String[] expressao = EXPRESSOES.get(item.tipo());
        String texto = quantidade + " " + (quantidade == 1 ? expressao[0] : expressao[1]);
        return item.plataforma() == null ? texto : texto + " " + ConcordanciaPlataforma.comPreposicao(item.plataforma());
    }

    private String juntar(List<String> partes) {
        if (partes.size() == 1) return partes.get(0);
        String inicio = String.join(", ", partes.subList(0, partes.size() - 1));
        return inicio + " e " + partes.get(partes.size() - 1);
    }
}
