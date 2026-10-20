package br.com.atria.repository;

import br.com.atria.model.DadoIdentificado;

import java.util.List;

public interface DadoIdentificadoRepository extends CrudRepositorio<DadoIdentificado> {
    List<DadoIdentificado> findByTitularId(Long titularId);

    List<DadoIdentificado> findByPlataformaId(Long plataformaId);

    void deleteByTitularId(Long titularId);
}
