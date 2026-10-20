package br.com.atria.repository.memoria;

import br.com.atria.model.RegistroAuditoria;
import br.com.atria.repository.RegistroAuditoriaRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Repository
public class RegistroAuditoriaRepositoryMemoria extends RepositorioMemoria<RegistroAuditoria>
        implements RegistroAuditoriaRepository {

    @Override
    public List<RegistroAuditoria> findByLojistaIdOrderByOcorridoEmDesc(Long lojistaId) {
        return filtrar(r -> Objects.equals(r.getLojistaId(), lojistaId)).stream()
                .sorted(Comparator.comparing(RegistroAuditoria::getOcorridoEm).reversed())
                .toList();
    }
}
