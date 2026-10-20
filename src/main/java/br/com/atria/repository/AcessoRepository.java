package br.com.atria.repository;

import br.com.atria.model.Acesso;

import java.util.List;

public interface AcessoRepository extends CrudRepositorio<Acesso> {
    List<Acesso> findByConexaoIdIn(Iterable<Long> conexaoIds);
}
