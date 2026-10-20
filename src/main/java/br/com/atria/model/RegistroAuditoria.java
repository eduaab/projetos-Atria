package br.com.atria.model;

import br.com.atria.model.enums.AcaoAuditoria;

import java.time.LocalDateTime;

public class RegistroAuditoria implements Identificavel {
    private Long id;
    private final Long lojistaId;
    private final AcaoAuditoria acao;
    private final String descricao;
    private final String entidade;
    private final Long entidadeId;
    private final LocalDateTime ocorridoEm;

    public RegistroAuditoria(Long lojistaId, AcaoAuditoria acao, String descricao,
                             String entidade, Long entidadeId, LocalDateTime ocorridoEm) {
        this.lojistaId = lojistaId;
        this.acao = acao;
        this.descricao = descricao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.ocorridoEm = ocorridoEm;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getLojistaId() { return lojistaId; }
    public AcaoAuditoria getAcao() { return acao; }
    public String getDescricao() { return descricao; }
    public String getEntidade() { return entidade; }
    public Long getEntidadeId() { return entidadeId; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
}
