package br.com.atria.service;

import br.com.atria.dto.response.PlataformaResponse;
import br.com.atria.exception.RecursoNaoEncontradoException;
import br.com.atria.mapper.PlataformaMapper;
import br.com.atria.model.Plataforma;
import br.com.atria.repository.PlataformaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlataformaService {
    private final PlataformaRepository repository;
    private final PlataformaMapper mapper;

    public PlataformaService(PlataformaRepository repository, PlataformaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<PlataformaResponse> listar() {
        return todas().stream().map(mapper::paraResponse).toList();
    }

    public List<Plataforma> todas() {
        return repository.findAll();
    }

    public Plataforma buscarPorSlug(String slug) {
        return repository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plataforma '" + slug + "' não encontrada."));
    }

    public Plataforma buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plataforma " + id + " não encontrada."));
    }

    public Optional<Plataforma> buscarOpcional(Long id) {
        return repository.findById(id);
    }

    public Map<Long, Plataforma> mapaPorId() {
        return todas().stream().collect(Collectors.toMap(Plataforma::getId, Function.identity()));
    }
}
