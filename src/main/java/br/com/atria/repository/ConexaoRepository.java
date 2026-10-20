package br.com.atria.repository;

import br.com.atria.model.Conexao;

import java.util.List;
import java.util.Optional;

public interface ConexaoRepository extends CrudRepositorio<Conexao> {
    List<Conexao> findByLojistaId(Long lojistaId);

    Optional<Conexao> findByLojistaIdAndPlataformaId(Long lojistaId, Long plataformaId);
}
