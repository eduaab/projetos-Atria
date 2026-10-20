package br.com.atria.dto.response;

public record ContadoresResponse(long titulares, long plataformasConectadas, long solicitacoesAbertas,
                                 long alertasAbertos, long alertasAltaPrioridade) {
}
