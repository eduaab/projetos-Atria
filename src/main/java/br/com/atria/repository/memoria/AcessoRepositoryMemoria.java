package br.com.atria.repository.memoria;

import br.com.atria.model.Acesso;
import br.com.atria.repository.AcessoRepository;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class AcessoRepositoryMemoria extends RepositorioMemoria<Acesso> implements AcessoRepository {

    @Override
    public List<Acesso> findByConexaoIdIn(Iterable<Long> conexaoIds) {
        Set<Long> ids = new HashSet<>();
        conexaoIds.forEach(ids::add);
        return filtrar(a -> ids.contains(a.getConexaoId()));
    }
}
