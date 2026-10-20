package br.com.atria.mapper;

import br.com.atria.dto.response.InteracaoResponse;
import br.com.atria.dto.response.PlataformaTitularResponse;
import br.com.atria.dto.response.SolicitacaoResponse;
import br.com.atria.dto.response.TitularDetalheResponse;
import br.com.atria.dto.response.TitularResumoResponse;
import br.com.atria.model.Titular;
import br.com.atria.service.regra.MascaradorCpf;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class TitularMapper {

    /** Listagens nunca expõem o CPF completo. */
    public TitularResumoResponse paraResumo(Titular titular, int quantidadePlataformas, boolean possuiSolicitacaoAberta) {
        return new TitularResumoResponse(titular.getId(), titular.getNomeExibicao(), MascaradorCpf.mascarar(titular.getCpf()),
                titular.getEmail(), titular.getTelefone(), quantidadePlataformas, possuiSolicitacaoAberta);
    }

    public TitularDetalheResponse paraDetalhe(Titular titular, BigDecimal gastosTotais,
                                              List<PlataformaTitularResponse> plataformas,
                                              List<InteracaoResponse> interacoes,
                                              List<SolicitacaoResponse> solicitacoes) {
        return new TitularDetalheResponse(titular.getId(), titular.getNomeExibicao(), titular.isAnonimizado(),
                titular.getCpf(), titular.getEmail(), titular.getTelefone(), titular.getCep(),
                titular.getCriadoEm(), titular.getAnonimizadoEm(), gastosTotais, plataformas, interacoes, solicitacoes);
    }
}
