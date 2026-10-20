package br.com.atria.repository.memoria;

import br.com.atria.model.Lojista;
import br.com.atria.repository.LojistaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class LojistaRepositoryMemoria extends RepositorioMemoria<Lojista> implements LojistaRepository {
}
