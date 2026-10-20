package br.com.atria.repository.memoria;

import br.com.atria.model.Evidencia;
import br.com.atria.repository.EvidenciaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class EvidenciaRepositoryMemoria extends RepositorioMemoria<Evidencia> implements EvidenciaRepository {

    @Override
    public Optional<Evidencia> findByProtocolo(String protocolo) {
        return buscarPrimeiro(e -> e.getProtocolo().equalsIgnoreCase(protocolo));
    }

    @Override
    public List<Evidencia> findBySolicitacaoIdIn(Collection<Long> solicitacaoIds) {
        return filtrar(e -> solicitacaoIds.contains(e.getSolicitacaoId()));
    }
}
