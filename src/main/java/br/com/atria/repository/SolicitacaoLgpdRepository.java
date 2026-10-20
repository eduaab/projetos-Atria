package br.com.atria.repository;

import br.com.atria.model.SolicitacaoLgpd;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SolicitacaoLgpdRepository extends CrudRepositorio<SolicitacaoLgpd> {
    List<SolicitacaoLgpd> findByTitularId(Long titularId);

    List<SolicitacaoLgpd> findByTitularIdIn(Collection<Long> titularIds);

    Optional<SolicitacaoLgpd> findByProtocolo(String protocolo);
}
