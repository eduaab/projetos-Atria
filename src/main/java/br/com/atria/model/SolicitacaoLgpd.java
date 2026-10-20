package br.com.atria.model;

import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.StatusSolicitacao;
import br.com.atria.model.enums.TipoSolicitacao;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SolicitacaoLgpd implements Identificavel {
    private Long id;
    private final Long titularId;
    private final TipoSolicitacao tipo;
    private final CanalOrigem canalOrigem;
    private final LocalDateTime abertaEm;
    private final LocalDate prazo;
    private LocalDateTime concluidaEm;
    private StatusSolicitacao status;
    private final String protocolo;

    public SolicitacaoLgpd(Long titularId, TipoSolicitacao tipo, CanalOrigem canalOrigem,
                           LocalDateTime abertaEm, LocalDate prazo, String protocolo) {
        this.titularId = titularId;
        this.tipo = tipo;
        this.canalOrigem = canalOrigem;
        this.abertaEm = abertaEm;
        this.prazo = prazo;
        this.protocolo = protocolo;
        this.status = StatusSolicitacao.PENDENTE;
    }

    public void alterarStatus(StatusSolicitacao novoStatus) {
        this.status = novoStatus;
    }

    public void concluir(LocalDateTime instante) {
        this.status = StatusSolicitacao.CONCLUIDA;
        this.concluidaEm = instante;
    }

    public boolean isAberta() {
        return status != StatusSolicitacao.CONCLUIDA;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getTitularId() { return titularId; }
    public TipoSolicitacao getTipo() { return tipo; }
    public CanalOrigem getCanalOrigem() { return canalOrigem; }
    public LocalDateTime getAbertaEm() { return abertaEm; }
    public LocalDate getPrazo() { return prazo; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public StatusSolicitacao getStatus() { return status; }
    public String getProtocolo() { return protocolo; }
}
