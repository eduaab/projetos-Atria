package br.com.atria.repository.memoria;

import br.com.atria.model.Conexao;
import br.com.atria.repository.ConexaoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class ConexaoRepositoryMemoria extends RepositorioMemoria<Conexao> implements ConexaoRepository {

    @Override
    public List<Conexao> findByLojistaId(Long lojistaId) {
        return filtrar(c -> Objects.equals(c.getLojistaId(), lojistaId));
    }

    @Override
    public Optional<Conexao> findByLojistaIdAndPlataformaId(Long lojistaId, Long plataformaId) {
        return buscarPrimeiro(c -> Objects.equals(c.getLojistaId(), lojistaId)
                && Objects.equals(c.getPlataformaId(), plataformaId));
    }
}
