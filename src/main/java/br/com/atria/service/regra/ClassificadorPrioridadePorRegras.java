package br.com.atria.service.regra;

import br.com.atria.model.enums.Prioridade;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * ALTA: reclamação pendente há mais de 24 h, prazo LGPD com até 2 dias (ou vencido), acesso de ex-funcionário.
 * MÉDIA: cliente sem resposta há mais de 4 h, pedido não enviado há mais de 1 dia, prazo LGPD entre 3 e 7 dias.
 * BAIXA: o restante.
 */
@Component
public class ClassificadorPrioridadePorRegras implements ClassificadorPrioridade {
    private static final Duration LIMITE_RECLAMACAO = Duration.ofHours(24);
    private static final Duration LIMITE_SEM_RESPOSTA = Duration.ofHours(4);
    private static final Duration LIMITE_PEDIDO = Duration.ofDays(1);

    @Override
    public Prioridade classificar(OcorrenciaRisco ocorrencia) {
        return switch (ocorrencia.tipo()) {
            case ACESSO_NAO_REVOGADO -> Prioridade.ALTA;
            case RECLAMACAO_PENDENTE -> passouDe(ocorrencia, LIMITE_RECLAMACAO) ? Prioridade.ALTA : Prioridade.BAIXA;
            case CLIENTE_SEM_RESPOSTA -> passouDe(ocorrencia, LIMITE_SEM_RESPOSTA) ? Prioridade.MEDIA : Prioridade.BAIXA;
            case PEDIDO_NAO_ENVIADO -> passouDe(ocorrencia, LIMITE_PEDIDO) ? Prioridade.MEDIA : Prioridade.BAIXA;
            case PRAZO_LGPD -> classificarPrazo(ocorrencia.diasRestantes());
        };
    }

    private boolean passouDe(OcorrenciaRisco ocorrencia, Duration limite) {
        return ocorrencia.tempoEmAberto() != null && ocorrencia.tempoEmAberto().compareTo(limite) > 0;
    }

    private Prioridade classificarPrazo(Long diasRestantes) {
        if (diasRestantes == null) return Prioridade.BAIXA;
        if (diasRestantes <= 2) return Prioridade.ALTA;
        if (diasRestantes <= 7) return Prioridade.MEDIA;
        return Prioridade.BAIXA;
    }
}
