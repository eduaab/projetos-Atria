package br.com.atria.repository;

import br.com.atria.model.Plataforma;

import java.util.Optional;

public interface PlataformaRepository extends CrudRepositorio<Plataforma> {
    Optional<Plataforma> findBySlug(String slug);
}
