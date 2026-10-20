package br.com.atria.mapper;

import br.com.atria.dto.response.InteracaoResponse;
import br.com.atria.model.Interacao;
import br.com.atria.model.Plataforma;
import br.com.atria.model.enums.TipoInteracao;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class InteracaoMapper {
    private static final Set<TipoInteracao> TIPOS_EM_DESTAQUE = Set.of(TipoInteracao.RECLAMACAO, TipoInteracao.SOLICITACAO_LGPD);

    /** {@code plataforma} pode ser nula (ex.: solicitação recebida por e-mail). */
    public InteracaoResponse paraResponse(Interacao interacao, Plataforma plataforma) {
        return new InteracaoResponse(interacao.getId(), interacao.getTipo(), interacao.getTipo().getDescricao(),
                plataforma == null ? null : plataforma.getNome(), interacao.getData(), interacao.getValor(),
                TIPOS_EM_DESTAQUE.contains(interacao.getTipo()));
    }
}
