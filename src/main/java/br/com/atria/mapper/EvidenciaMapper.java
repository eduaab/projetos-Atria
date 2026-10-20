package br.com.atria.mapper;

import br.com.atria.dto.response.EvidenciaResponse;
import br.com.atria.model.Evidencia;
import br.com.atria.model.SolicitacaoLgpd;
import org.springframework.stereotype.Component;

@Component
public class EvidenciaMapper {

    public EvidenciaResponse paraResponse(Evidencia evidencia, SolicitacaoLgpd solicitacao) {
        return new EvidenciaResponse(evidencia.getId(), evidencia.getProtocolo(),
                solicitacao.getTipo(), solicitacao.getTipo().getDescricao(),
                solicitacao.getCanalOrigem(), solicitacao.getCanalOrigem().getDescricao(),
                solicitacao.getAbertaEm(), solicitacao.getConcluidaEm(), evidencia.getGeradaEm(),
                evidencia.getCpfMascarado(), evidencia.getPlataformas(), evidencia.getHashComprovante());
    }
}
