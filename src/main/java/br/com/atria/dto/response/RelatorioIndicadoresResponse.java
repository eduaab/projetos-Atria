package br.com.atria.dto.response;

import java.time.LocalDate;

public record RelatorioIndicadoresResponse(LocalDate periodoInicio, LocalDate periodoFim, int indiceConformidade,
                                           long solicitacoesRecebidas, long solicitacoesAtendidas,
                                           long solicitacoesAtendidasNoPrazo, long solicitacoesAtrasadas,
                                           double tempoMedioAtendimentoDias, long alertasResolvidos,
                                           long alertasAbertos, long acessosRevogados, long acessosIndevidosAtivos,
                                           long plataformasConectadas, long titularesMapeados,
                                           long titularesAnonimizados) {
}
