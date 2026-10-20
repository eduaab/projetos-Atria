package br.com.atria.mapper;

import br.com.atria.dto.response.ConexaoResponse;
import br.com.atria.model.Conexao;
import br.com.atria.model.Plataforma;
import org.springframework.stereotype.Component;

@Component
public class ConexaoMapper {
    private final PlataformaMapper plataformaMapper;

    public ConexaoMapper(PlataformaMapper plataformaMapper) {
        this.plataformaMapper = plataformaMapper;
    }

    /** {@code conexao} pode ser nula quando a plataforma nunca foi conectada. */
    public ConexaoResponse paraResponse(Plataforma plataforma, Conexao conexao) {
        if (conexao == null) {
            return new ConexaoResponse(plataformaMapper.paraResponse(plataforma), null, false, null, null, null);
        }
        return new ConexaoResponse(plataformaMapper.paraResponse(plataforma), conexao.getStatus(), conexao.isAtiva(),
                conexao.getConectadaEm(), conexao.getDesconectadaEm(), conexao.getUltimaSincronizacao());
    }
}
