package br.com.atria.relatorio;

import br.com.atria.dto.response.AlertaResponse;
import br.com.atria.dto.response.AuditoriaResponse;
import br.com.atria.dto.response.ConexaoResponse;
import br.com.atria.dto.response.EvidenciaResponse;
import br.com.atria.dto.response.RelatorioIndicadoresResponse;
import br.com.atria.dto.response.SolicitacaoResponse;
import br.com.atria.model.Lojista;

import java.time.LocalDateTime;
import java.util.List;

/** Tudo o que os relatórios em PDF usam, já consolidado a partir dos dados em memória. */
public record DadosRelatorio(Lojista lojista, RelatorioIndicadoresResponse indicadores,
                             List<SolicitacaoResponse> solicitacoes, List<AlertaResponse> alertas,
                             List<EvidenciaResponse> evidencias, List<AuditoriaResponse> acoes,
                             List<ConexaoResponse> conexoes, LocalDateTime geradoEm) {
}
