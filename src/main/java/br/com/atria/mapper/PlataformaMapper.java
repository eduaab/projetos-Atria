package br.com.atria.mapper;

import br.com.atria.dto.response.PlataformaResponse;
import br.com.atria.model.Plataforma;
import org.springframework.stereotype.Component;

@Component
public class PlataformaMapper {

    public PlataformaResponse paraResponse(Plataforma plataforma) {
        return new PlataformaResponse(plataforma.getId(), plataforma.getSlug(), plataforma.getNome(),
                plataforma.getTipo(), plataforma.getTipo().getDescricao(), plataforma.getEscopos());
    }
}
