package br.com.atria.repository;

import br.com.atria.model.Usuario;

import java.util.List;

public interface UsuarioRepository extends CrudRepositorio<Usuario> {
    List<Usuario> findByLojistaId(Long lojistaId);
}
