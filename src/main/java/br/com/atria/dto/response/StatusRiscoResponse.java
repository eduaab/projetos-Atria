package br.com.atria.dto.response;

public record StatusRiscoResponse(boolean emConformidade, long pendencias, String mensagem) {
}
