package br.com.atria.mapper;

import br.com.atria.dto.response.DadoIdentificadoResponse;
import br.com.atria.model.DadoIdentificado;
import br.com.atria.model.Plataforma;
import br.com.atria.model.Titular;
import br.com.atria.service.regra.MascaradorCpf;
import org.springframework.stereotype.Component;

@Component
public class DadoIdentificadoMapper {

    public DadoIdentificadoResponse paraResponse(DadoIdentificado dado, Titular titular, Plataforma plataforma) {
        return new DadoIdentificadoResponse(dado.getId(), titular.getId(), titular.getNomeExibicao(),
                MascaradorCpf.mascarar(titular.getCpf()), plataforma.getSlug(), plataforma.getNome(),
                dado.getTipoDado(), dado.getTipoDado().getDescricao(), dado.getEncontradoEm());
    }
}
