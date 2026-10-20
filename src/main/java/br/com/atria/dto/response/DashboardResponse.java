package br.com.atria.dto.response;

import java.util.List;

/** Home: contadores, frase-resumo, alertas prioritários, prazos (calendário) e cards de status. */
public record DashboardResponse(String nomeLoja, ContadoresResponse contadores, String fraseResumo,
                                List<AlertaResponse> alertasPrioritarios, List<SolicitacaoResponse> prazos,
                                StatusRiscoResponse statusAnpd, StatusRiscoResponse statusMarketplace) {
}
