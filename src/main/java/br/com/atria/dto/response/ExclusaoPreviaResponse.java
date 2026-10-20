package br.com.atria.dto.response;

import java.util.List;

public record ExclusaoPreviaResponse(Long titularId, String titularNome, List<CampoPessoalResponse> dadosApagados,
                                     List<String> dadosPreservados, List<String> plataformas,
                                     String protocoloSolicitacao) {
}
