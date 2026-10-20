package br.com.atria.repository.memoria;

import br.com.atria.model.Usuario;
import br.com.atria.repository.UsuarioRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class UsuarioRepositoryMemoria extends RepositorioMemoria<Usuario> implements UsuarioRepository {

    @Override
    public List<Usuario> findByLojistaId(Long lojistaId) {
        return filtrar(u -> Objects.equals(u.getLojistaId(), lojistaId));
    }
}
