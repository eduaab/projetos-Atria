package br.com.atria.dto.response;

import java.util.List;

public record ConexaoDetalheResponse(ConexaoResponse conexao, long clientes,
                                     List<ContagemResponse> dadosPorTipo, List<AcessoResponse> acessos) {
}
