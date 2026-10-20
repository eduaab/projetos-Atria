package br.com.atria.mapper;

import br.com.atria.dto.response.AlertaResponse;
import br.com.atria.model.Alerta;
import br.com.atria.model.Plataforma;
import br.com.atria.model.Titular;
import org.springframework.stereotype.Component;

@Component
public class AlertaMapper {

    /**
     * @param plataforma        plataforma do alerta (pode ser nula)
     * @param titular           titular afetado (pode ser nulo)
     * @param acessoRevogavelId id do acesso que ainda pode ser revogado, quando o alerta for de acesso
     * @param diasRestantes     dias até o prazo, quando o alerta for de solicitação LGPD
     */
    public AlertaResponse paraResponse(Alerta alerta, Plataforma plataforma, Titular titular,
                                       Long acessoRevogavelId, Long diasRestantes) {
        return new AlertaResponse(alerta.getId(), alerta.getTipo(), alerta.getTipo().getDescricao(),
                alerta.getPrioridade(), alerta.getStatus(), alerta.getStatus().getDescricao(),
                alerta.getTitulo(), alerta.getDescricao(),
                plataforma == null ? null : plataforma.getSlug(), plataforma == null ? null : plataforma.getNome(),
                alerta.getTipoOrigem(), alerta.getTipoOrigem().getDescricao(), alerta.getOrigemId(),
                alerta.getClientesAfetados(),
                titular == null ? null : titular.getId(), titular == null ? null : titular.getNomeExibicao(),
                titular == null ? null : titular.getEmail(),
                acessoRevogavelId, diasRestantes, alerta.getCriadoEm(), alerta.getResolvidoEm());
    }
}
