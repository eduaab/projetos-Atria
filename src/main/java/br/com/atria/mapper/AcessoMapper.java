package br.com.atria.mapper;

import br.com.atria.dto.response.AcessoResponse;
import br.com.atria.model.Acesso;
import br.com.atria.model.Plataforma;
import br.com.atria.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class AcessoMapper {

    public AcessoResponse paraResponse(Acesso acesso, Usuario usuario, Plataforma plataforma) {
        return new AcessoResponse(acesso.getId(), usuario.getNome(), usuario.getPapel(), usuario.isDesligado(),
                plataforma.getNome(), acesso.getConcedidoEm(), acesso.getRevogadoEm());
    }
}
