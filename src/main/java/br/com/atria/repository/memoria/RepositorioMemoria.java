package br.com.atria.repository.memoria;

import br.com.atria.model.Identificavel;
import br.com.atria.repository.CrudRepositorio;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/** Base dos repositórios em memória: guarda as entidades em um mapa e gera IDs sequenciais. */
public abstract class RepositorioMemoria<T extends Identificavel> implements CrudRepositorio<T> {
    private final Map<Long, T> dados = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    @Override
    public T save(T entidade) {
        if (entidade.getId() == null) {
            entidade.setId(sequencia.incrementAndGet());
        }
        dados.put(entidade.getId(), entidade);
        return entidade;
    }

    @Override
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(id).map(dados::get);
    }

    @Override
    public List<T> findAll() {
        return filtrar(entidade -> true);
    }

    @Override
    public void deleteById(Long id) {
        dados.remove(id);
    }

    @Override
    public long count() {
        return dados.size();
    }

    protected List<T> filtrar(Predicate<T> criterio) {
        return dados.values().stream()
                .filter(criterio)
                .sorted(Comparator.comparing(Identificavel::getId))
                .toList();
    }

    protected Optional<T> buscarPrimeiro(Predicate<T> criterio) {
        return filtrar(criterio).stream().findFirst();
    }

    protected void removerSe(Predicate<T> criterio) {
        dados.values().removeIf(criterio);
    }
}
