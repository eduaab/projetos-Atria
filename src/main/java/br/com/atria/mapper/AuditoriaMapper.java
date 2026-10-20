package br.com.atria.mapper;

import br.com.atria.dto.response.AuditoriaResponse;
import br.com.atria.model.RegistroAuditoria;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaMapper {

    public AuditoriaResponse paraResponse(RegistroAuditoria registro) {
        return new AuditoriaResponse(registro.getId(), registro.getAcao(), registro.getAcao().getDescricao(),
                registro.getDescricao(), registro.getEntidade(), registro.getEntidadeId(), registro.getOcorridoEm());
    }
}
