package br.com.atria.repository.memoria;

import br.com.atria.model.DadoIdentificado;
import br.com.atria.repository.DadoIdentificadoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class DadoIdentificadoRepositoryMemoria extends RepositorioMemoria<DadoIdentificado>
        implements DadoIdentificadoRepository {

    @Override
    public List<DadoIdentificado> findByTitularId(Long titularId) {
        return filtrar(d -> Objects.equals(d.getTitularId(), titularId));
    }

    @Override
    public List<DadoIdentificado> findByPlataformaId(Long plataformaId) {
        return filtrar(d -> Objects.equals(d.getPlataformaId(), plataformaId));
    }

    @Override
    public void deleteByTitularId(Long titularId) {
        removerSe(d -> Objects.equals(d.getTitularId(), titularId));
    }
}
