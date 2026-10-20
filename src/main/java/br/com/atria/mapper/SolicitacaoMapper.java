package br.com.atria.mapper;

import br.com.atria.dto.response.SolicitacaoResponse;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.Titular;
import br.com.atria.model.enums.StatusSolicitacao;
import br.com.atria.service.regra.CalculadoraPrazo;
import org.springframework.stereotype.Component;

@Component
public class SolicitacaoMapper {
    private final CalculadoraPrazo calculadoraPrazo;

    public SolicitacaoMapper(CalculadoraPrazo calculadoraPrazo) {
        this.calculadoraPrazo = calculadoraPrazo;
    }

    public SolicitacaoResponse paraResponse(SolicitacaoLgpd solicitacao, Titular titular) {
        long dias = calculadoraPrazo.diasRestantes(solicitacao.getPrazo());
        StatusSolicitacao status = calculadoraPrazo.statusEfetivo(solicitacao);
        return new SolicitacaoResponse(solicitacao.getId(), solicitacao.getProtocolo(), titular.getId(),
                titular.getNomeExibicao(), titular.getEmail(),
                solicitacao.getTipo(), solicitacao.getTipo().getDescricao(),
                solicitacao.getCanalOrigem(), solicitacao.getCanalOrigem().getDescricao(),
                solicitacao.getAbertaEm(), solicitacao.getPrazo(), dias, calculadoraPrazo.classificarNivel(dias),
                status, status.getDescricao(), solicitacao.getConcluidaEm());
    }
}
