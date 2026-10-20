package br.com.atria.service.regra;

import br.com.atria.config.AtriaProperties;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.enums.NivelPrazo;
import br.com.atria.model.enums.StatusSolicitacao;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Prazos das solicitações do titular: prazo = abertura + N dias (15 por padrão). */
@Component
public class CalculadoraPrazo {
    private final Clock clock;
    private final int prazoDias;

    public CalculadoraPrazo(Clock clock, AtriaProperties propriedades) {
        this.clock = clock;
        this.prazoDias = propriedades.prazoLgpdDias();
    }

    public LocalDate calcularPrazo(LocalDateTime abertaEm) {
        return abertaEm.toLocalDate().plusDays(prazoDias);
    }

    /** Dias de hoje até o prazo; negativo quando já venceu. */
    public long diasRestantes(LocalDate prazo) {
        return ChronoUnit.DAYS.between(LocalDate.now(clock), prazo);
    }

    public boolean estaVencida(SolicitacaoLgpd solicitacao) {
        return solicitacao.isAberta() && diasRestantes(solicitacao.getPrazo()) < 0;
    }

    public StatusSolicitacao statusEfetivo(SolicitacaoLgpd solicitacao) {
        return estaVencida(solicitacao) ? StatusSolicitacao.ATRASADA : solicitacao.getStatus();
    }

    /** Faixa visual do prazo: verde acima de 7 dias, amarelo de 3 a 7, laranja até 2 e vermelho vencido. */
    public NivelPrazo classificarNivel(long diasRestantes) {
        if (diasRestantes < 0) return NivelPrazo.VERMELHO;
        if (diasRestantes <= 2) return NivelPrazo.LARANJA;
        if (diasRestantes <= 7) return NivelPrazo.AMARELO;
        return NivelPrazo.VERDE;
    }
}
