package br.com.atria.service;

import br.com.atria.model.Alerta;
import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.repository.AlertaRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/** Resolve os alertas de uma origem quando o problema é tratado (acesso revogado, solicitação concluída). */
@Service
public class ResolucaoAlertaService {
    private final AlertaRepository alertaRepository;
    private final Clock clock;

    public ResolucaoAlertaService(AlertaRepository alertaRepository, Clock clock) {
        this.alertaRepository = alertaRepository;
        this.clock = clock;
    }

    public void resolverPorOrigem(OrigemAlerta tipoOrigem, Long origemId) {
        LocalDateTime agora = LocalDateTime.now(clock);
        for (Alerta alerta : alertaRepository.findByTipoOrigemAndOrigemId(tipoOrigem, origemId)) {
            if (alerta.isAberto()) {
                alerta.resolver(agora);
                alertaRepository.save(alerta);
            }
        }
    }
}
