package br.com.atria.dto.response;

import java.util.List;

public record RiscoIndicadoresResponse(long solicitacoesAbertas, long solicitacoesAtrasadas,
                                       long vencendoEmAte2Dias, long vencendoEm3a7Dias, long noPrazo,
                                       long alertasAbertos, List<ContagemResponse> alertasPorPrioridade,
                                       long alertasResolvidos,
                                       StatusRiscoResponse statusAnpd, StatusRiscoResponse statusMarketplace) {
}
