package br.com.atria.repository.memoria;

import br.com.atria.model.IdentidadePlataforma;
import br.com.atria.repository.IdentidadePlataformaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class IdentidadePlataformaRepositoryMemoria extends RepositorioMemoria<IdentidadePlataforma>
        implements IdentidadePlataformaRepository {

    @Override
    public List<IdentidadePlataforma> findByTitularId(Long titularId) {
        return filtrar(i -> Objects.equals(i.getTitularId(), titularId));
    }

    @Override
    public List<IdentidadePlataforma> findByPlataformaId(Long plataformaId) {
        return filtrar(i -> Objects.equals(i.getPlataformaId(), plataformaId));
    }

    @Override
    public void deleteByTitularId(Long titularId) {
        removerSe(i -> Objects.equals(i.getTitularId(), titularId));
    }
}
