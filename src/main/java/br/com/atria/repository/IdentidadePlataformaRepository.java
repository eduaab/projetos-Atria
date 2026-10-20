package br.com.atria.repository;

import br.com.atria.model.IdentidadePlataforma;

import java.util.List;

public interface IdentidadePlataformaRepository extends CrudRepositorio<IdentidadePlataforma> {
    List<IdentidadePlataforma> findByTitularId(Long titularId);

    List<IdentidadePlataforma> findByPlataformaId(Long plataformaId);

    void deleteByTitularId(Long titularId);
}
