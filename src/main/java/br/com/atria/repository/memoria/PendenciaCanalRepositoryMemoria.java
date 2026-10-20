package br.com.atria.repository.memoria;

import br.com.atria.model.PendenciaCanal;
import br.com.atria.repository.PendenciaCanalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class PendenciaCanalRepositoryMemoria extends RepositorioMemoria<PendenciaCanal>
        implements PendenciaCanalRepository {

    @Override
    public List<PendenciaCanal> findByLojistaIdAndResolvidaFalse(Long lojistaId) {
        return filtrar(p -> Objects.equals(p.getLojistaId(), lojistaId) && !p.isResolvida());
    }
}
