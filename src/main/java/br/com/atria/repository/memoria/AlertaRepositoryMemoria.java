package br.com.atria.repository.memoria;

import br.com.atria.model.Alerta;
import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.repository.AlertaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class AlertaRepositoryMemoria extends RepositorioMemoria<Alerta> implements AlertaRepository {

    @Override
    public List<Alerta> findByLojistaId(Long lojistaId) {
        return filtrar(a -> Objects.equals(a.getLojistaId(), lojistaId));
    }

    @Override
    public List<Alerta> findByTipoOrigemAndOrigemId(OrigemAlerta tipoOrigem, Long origemId) {
        return filtrar(a -> a.getTipoOrigem() == tipoOrigem && Objects.equals(a.getOrigemId(), origemId));
    }

    @Override
    public boolean existsByTipoOrigemAndOrigemId(OrigemAlerta tipoOrigem, Long origemId) {
        return !findByTipoOrigemAndOrigemId(tipoOrigem, origemId).isEmpty();
    }
}
