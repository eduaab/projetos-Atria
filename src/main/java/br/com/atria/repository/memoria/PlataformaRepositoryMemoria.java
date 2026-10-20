package br.com.atria.repository.memoria;

import br.com.atria.model.Plataforma;
import br.com.atria.repository.PlataformaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PlataformaRepositoryMemoria extends RepositorioMemoria<Plataforma> implements PlataformaRepository {

    @Override
    public Optional<Plataforma> findBySlug(String slug) {
        return buscarPrimeiro(p -> p.getSlug().equalsIgnoreCase(slug));
    }
}
