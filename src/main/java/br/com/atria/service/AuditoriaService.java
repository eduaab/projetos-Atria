package br.com.atria.service;

import br.com.atria.dto.response.AuditoriaResponse;
import br.com.atria.mapper.AuditoriaMapper;
import br.com.atria.model.RegistroAuditoria;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.repository.RegistroAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Histórico de ações. As descrições nunca levam dados pessoais do titular, só ids e protocolos. */
@Service
public class AuditoriaService {
    private static final int LIMITE_PADRAO = 100;

    private final RegistroAuditoriaRepository repository;
    private final LojistaAtualService lojistaAtual;
    private final AuditoriaMapper mapper;
    private final Clock clock;

    public AuditoriaService(RegistroAuditoriaRepository repository, LojistaAtualService lojistaAtual,
                            AuditoriaMapper mapper, Clock clock) {
        this.repository = repository;
        this.lojistaAtual = lojistaAtual;
        this.mapper = mapper;
        this.clock = clock;
    }

    public void registrar(AcaoAuditoria acao, String descricao, String entidade, Long entidadeId) {
        repository.save(new RegistroAuditoria(lojistaAtual.id(), acao, descricao, entidade, entidadeId, LocalDateTime.now(clock)));
    }

    public List<AuditoriaResponse> listar(Integer limite) {
        int maximo = limite == null || limite <= 0 ? LIMITE_PADRAO : limite;
        return repository.findByLojistaIdOrderByOcorridoEmDesc(lojistaAtual.id()).stream()
                .limit(maximo)
                .map(mapper::paraResponse)
                .toList();
    }
}
