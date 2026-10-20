package br.com.atria.model.enums;

public enum AcaoAuditoria {
    CONEXAO_CRIADA("Plataforma conectada"),
    CONEXAO_REVOGADA("Plataforma desconectada"),
    VARREDURA_EXECUTADA("Varredura executada"),
    ALERTA_STATUS_ALTERADO("Status de alerta alterado"),
    ACESSO_REVOGADO("Acesso revogado"),
    SOLICITACAO_REGISTRADA("Solicitação registrada"),
    SOLICITACAO_STATUS_ALTERADO("Status de solicitação alterado"),
    SOLICITACAO_CONCLUIDA("Solicitação concluída"),
    TITULAR_EXCLUIDO("Dados do titular excluídos"),
    RELATORIO_GERADO("Relatório gerado");

    private final String descricao;

    AcaoAuditoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
