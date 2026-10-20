package br.com.atria.repository;

import br.com.atria.model.RegistroAuditoria;

import java.util.List;

public interface RegistroAuditoriaRepository extends CrudRepositorio<RegistroAuditoria> {
    List<RegistroAuditoria> findByLojistaIdOrderByOcorridoEmDesc(Long lojistaId);
}
