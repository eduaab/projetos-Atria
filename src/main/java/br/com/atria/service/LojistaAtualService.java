package br.com.atria.service;

import br.com.atria.config.AtriaProperties;
import br.com.atria.exception.RecursoNaoEncontradoException;
import br.com.atria.model.Lojista;
import br.com.atria.repository.LojistaRepository;
import org.springframework.stereotype.Service;

/** Lojista atendido pela aplicação. Enquanto não há login, é o configurado em {@code atria.lojista-atual-id}. */
@Service
public class LojistaAtualService {
    private final AtriaProperties propriedades;
    private final LojistaRepository lojistaRepository;

    public LojistaAtualService(AtriaProperties propriedades, LojistaRepository lojistaRepository) {
        this.propriedades = propriedades;
        this.lojistaRepository = lojistaRepository;
    }

    public Long id() {
        return propriedades.lojistaAtualId();
    }

    public Lojista obter() {
        return lojistaRepository.findById(id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lojista " + id() + " não encontrado."));
    }
}
