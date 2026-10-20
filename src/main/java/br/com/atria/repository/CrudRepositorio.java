package br.com.atria.repository;

import java.util.List;
import java.util.Optional;

/**
 * Operações básicas de persistência, com os mesmos nomes do Spring Data.
 * Na migração para JPA, cada repositório passa a estender {@code JpaRepository<T, Long>}.
 */
public interface CrudRepositorio<T> {
    T save(T entidade);

    Optional<T> findById(Long id);

    List<T> findAll();

    void deleteById(Long id);

    long count();
}
