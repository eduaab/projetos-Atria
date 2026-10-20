package br.com.atria.repository.memoria;

import br.com.atria.model.Interacao;
import br.com.atria.repository.InteracaoRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Repository
public class InteracaoRepositoryMemoria extends RepositorioMemoria<Interacao> implements InteracaoRepository {

    @Override
    public List<Interacao> findByTitularIdOrderByDataDesc(Long titularId) {
        return filtrar(i -> Objects.equals(i.getTitularId(), titularId)).stream()
                .sorted(Comparator.comparing(Interacao::getData).reversed())
                .toList();
    }
}
