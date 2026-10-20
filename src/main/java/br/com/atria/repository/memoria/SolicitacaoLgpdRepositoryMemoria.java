package br.com.atria.repository.memoria;

import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.repository.SolicitacaoLgpdRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class SolicitacaoLgpdRepositoryMemoria extends RepositorioMemoria<SolicitacaoLgpd>
        implements SolicitacaoLgpdRepository {

    @Override
    public List<SolicitacaoLgpd> findByTitularId(Long titularId) {
        return filtrar(s -> Objects.equals(s.getTitularId(), titularId));
    }

    @Override
    public List<SolicitacaoLgpd> findByTitularIdIn(Collection<Long> titularIds) {
        return filtrar(s -> titularIds.contains(s.getTitularId()));
    }

    @Override
    public Optional<SolicitacaoLgpd> findByProtocolo(String protocolo) {
        return buscarPrimeiro(s -> s.getProtocolo().equalsIgnoreCase(protocolo));
    }
}
