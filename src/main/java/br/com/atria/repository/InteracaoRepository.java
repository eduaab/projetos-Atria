package br.com.atria.repository;

import br.com.atria.model.Interacao;

import java.util.List;

public interface InteracaoRepository extends CrudRepositorio<Interacao> {
    List<Interacao> findByTitularIdOrderByDataDesc(Long titularId);
}
