package br.com.atria.repository;

import br.com.atria.model.Evidencia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EvidenciaRepository extends CrudRepositorio<Evidencia> {
    Optional<Evidencia> findByProtocolo(String protocolo);

    List<Evidencia> findBySolicitacaoIdIn(Collection<Long> solicitacaoIds);
}
