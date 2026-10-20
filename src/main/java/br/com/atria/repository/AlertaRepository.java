package br.com.atria.repository;

import br.com.atria.model.Alerta;
import br.com.atria.model.enums.OrigemAlerta;

import java.util.List;

public interface AlertaRepository extends CrudRepositorio<Alerta> {
    List<Alerta> findByLojistaId(Long lojistaId);

    List<Alerta> findByTipoOrigemAndOrigemId(OrigemAlerta tipoOrigem, Long origemId);

    boolean existsByTipoOrigemAndOrigemId(OrigemAlerta tipoOrigem, Long origemId);
}
