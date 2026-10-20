package br.com.atria.relatorio;

import br.com.atria.dto.response.EvidenciaResponse;
import br.com.atria.model.Lojista;
import br.com.atria.model.enums.TipoSolicitacao;
import br.com.atria.service.regra.FormatoData;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** Comprovante de atendimento de uma solicitação do titular, sem dados pessoais além do CPF mascarado. */
@Component
public class GeradorComprovantePdf {

    public byte[] gerar(EvidenciaResponse evidencia, Lojista lojista, LocalDateTime geradoEm) {
        DocumentoPdf pdf = new DocumentoPdf("Comprovante de Atendimento LGPD",
                lojista.getNomeLoja() + " – Protocolo " + evidencia.protocolo(),
                FormatoData.data(evidencia.abertaEm()) + " a " + FormatoData.data(evidencia.concluidaEm()), geradoEm);

        pdf.secao("Dados da solicitação").indicadores(List.of(
                new String[]{"Protocolo", evidencia.protocolo()},
                new String[]{"Tipo", evidencia.tipoDescricao()},
                new String[]{"Canal de origem", evidencia.canalDescricao()},
                new String[]{"Aberta em", FormatoData.dataHora(evidencia.abertaEm())},
                new String[]{"Concluída em", FormatoData.dataHora(evidencia.concluidaEm())},
                new String[]{"CPF do titular", evidencia.cpfMascarado()}));

        pdf.secao("Plataformas envolvidas").paragrafo(evidencia.plataformas().isEmpty()
                ? "Nenhuma plataforma vinculada ao titular."
                : String.join(", ", evidencia.plataformas()));

        pdf.secao("Declaração");
        if (evidencia.tipoSolicitacao() == TipoSolicitacao.EXCLUSAO) {
            pdf.paragrafo("A Atria excluiu os dados pessoais do titular que mantinha em sua base (nome, CPF, e-mail, telefone e CEP) "
                    + "e anonimizou o registro. Os dados existentes nas plataformas de origem não são alterados pela Atria; "
                    + "a exclusão nessas plataformas deve ser feita diretamente em cada uma delas.");
        } else {
            pdf.paragrafo("A solicitação do titular foi atendida e registrada pela loja por meio da Atria.");
        }
        pdf.paragrafo("Este comprovante não contém dados pessoais e é mantido como prova do atendimento, conforme a LGPD.");

        pdf.secao("Integridade").paragrafo("Hash SHA-256 do comprovante: " + evidencia.hashComprovante())
                .paragrafo("Comprovante emitido em " + FormatoData.dataHora(evidencia.geradaEm()) + ".");
        return pdf.concluir();
    }
}
