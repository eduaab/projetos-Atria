package br.com.atria.model;

import java.time.LocalDateTime;
import java.util.List;

/** Comprovante de atendimento de uma solicitação, sem dado pessoal além do CPF mascarado. */
public class Evidencia implements Identificavel {
    private Long id;
    private final Long solicitacaoId;
    private final LocalDateTime geradaEm;
    private final String cpfMascarado;
    private final List<String> plataformas;
    private final String hashComprovante;
    private final String protocolo;

    public Evidencia(Long solicitacaoId, LocalDateTime geradaEm, String cpfMascarado,
                     List<String> plataformas, String hashComprovante, String protocolo) {
        this.solicitacaoId = solicitacaoId;
        this.geradaEm = geradaEm;
        this.cpfMascarado = cpfMascarado;
        this.plataformas = List.copyOf(plataformas);
        this.hashComprovante = hashComprovante;
        this.protocolo = protocolo;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getSolicitacaoId() { return solicitacaoId; }
    public LocalDateTime getGeradaEm() { return geradaEm; }
    public String getCpfMascarado() { return cpfMascarado; }
    public List<String> getPlataformas() { return plataformas; }
    public String getHashComprovante() { return hashComprovante; }
    public String getProtocolo() { return protocolo; }
}
