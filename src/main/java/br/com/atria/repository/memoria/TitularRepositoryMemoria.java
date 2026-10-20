package br.com.atria.repository.memoria;

import br.com.atria.model.Titular;
import br.com.atria.repository.TitularRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class TitularRepositoryMemoria extends RepositorioMemoria<Titular> implements TitularRepository {

    @Override
    public List<Titular> findByLojistaId(Long lojistaId) {
        return filtrar(t -> Objects.equals(t.getLojistaId(), lojistaId));
    }
}
