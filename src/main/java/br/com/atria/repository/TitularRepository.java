package br.com.atria.repository;

import br.com.atria.model.Titular;

import java.util.List;

public interface TitularRepository extends CrudRepositorio<Titular> {
    List<Titular> findByLojistaId(Long lojistaId);
}
