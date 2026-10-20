package br.com.atria.repository;

import br.com.atria.model.PendenciaCanal;

import java.util.List;

public interface PendenciaCanalRepository extends CrudRepositorio<PendenciaCanal> {
    List<PendenciaCanal> findByLojistaIdAndResolvidaFalse(Long lojistaId);
}
