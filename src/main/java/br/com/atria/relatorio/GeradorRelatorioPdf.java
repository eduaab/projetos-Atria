package br.com.atria.relatorio;

import br.com.atria.dto.response.AlertaResponse;
import br.com.atria.dto.response.RelatorioIndicadoresResponse;
import br.com.atria.model.Lojista;
import br.com.atria.model.enums.StatusAlerta;
import br.com.atria.service.regra.FormatoData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** Monta os relatórios ESG, ANPD e Diferencial Comercial a partir de {@link DadosRelatorio}. */
@Component
public class GeradorRelatorioPdf {

    public byte[] gerar(TipoRelatorio tipo, DadosRelatorio dados) {
        RelatorioIndicadoresResponse ind = dados.indicadores();
        String periodo = FormatoData.data(ind.periodoInicio()) + " a " + FormatoData.data(ind.periodoFim());
        DocumentoPdf pdf = new DocumentoPdf(tipo.getTitulo(), descreverLoja(dados.lojista()), periodo, dados.geradoEm());
        switch (tipo) {
            case ESG -> esg(pdf, dados);
            case ANPD -> anpd(pdf, dados);
            case COMERCIAL -> comercial(pdf, dados);
        }
        return pdf.concluir();
    }

    private void esg(DocumentoPdf pdf, DadosRelatorio dados) {
        RelatorioIndicadoresResponse ind = dados.indicadores();
        pdf.paragrafo("Este relatório demonstra as práticas de governança de dados pessoais da loja (pilar G do ESG), "
                + "com base nos registros mantidos pela Atria.");
        pdf.secao("Indicadores de governança").indicadores(List.of(
                item("Índice de conformidade", ind.indiceConformidade() + "/100"),
                item("Solicitações atendidas", ind.solicitacoesAtendidas() + " de " + ind.solicitacoesRecebidas()),
                item("Atendidas no prazo", percentual(ind.solicitacoesAtendidasNoPrazo(), ind.solicitacoesAtendidas())),
                item("Tempo médio de atendimento", dias(ind.tempoMedioAtendimentoDias())),
                item("Riscos resolvidos", String.valueOf(ind.alertasResolvidos())),
                item("Acessos revogados", String.valueOf(ind.acessosRevogados())),
                item("Plataformas monitoradas", String.valueOf(ind.plataformasConectadas())),
                item("Clientes mapeados", String.valueOf(ind.titularesMapeados())),
                item("Titulares anonimizados", String.valueOf(ind.titularesAnonimizados()))));
        solicitacoesAtendidas(pdf, dados);
        riscos(pdf, "Riscos resolvidos", dados.alertas().stream().filter(a -> a.status() == StatusAlerta.RESOLVIDO).toList());
        historicoDeAcoes(pdf, dados);
    }

    private void anpd(DocumentoPdf pdf, DadosRelatorio dados) {
        RelatorioIndicadoresResponse ind = dados.indicadores();
        pdf.paragrafo("Consolidado do atendimento aos direitos dos titulares (art. 18 da LGPD) e dos riscos monitorados. "
                + "As solicitações são identificadas apenas pelo protocolo.");
        pdf.secao("Indicadores de conformidade").indicadores(List.of(
                item("Índice de conformidade", ind.indiceConformidade() + "/100"),
                item("Solicitações recebidas", String.valueOf(ind.solicitacoesRecebidas())),
                item("Solicitações atrasadas", String.valueOf(ind.solicitacoesAtrasadas())),
                item("Atendidas no prazo", percentual(ind.solicitacoesAtendidasNoPrazo(), ind.solicitacoesAtendidas())),
                item("Alertas em aberto", String.valueOf(ind.alertasAbertos())),
                item("Acessos indevidos ativos", String.valueOf(ind.acessosIndevidosAtivos()))));
        pdf.secao("Solicitações de titulares").tabela(
                new String[]{"Protocolo", "Tipo", "Canal", "Aberta em", "Prazo", "Status"},
                dados.solicitacoes().stream().map(s -> new String[]{s.protocolo(), s.tipoDescricao(), s.canalDescricao(),
                        FormatoData.data(s.abertaEm()), FormatoData.data(s.prazo()), s.statusDescricao()}).toList(),
                new float[]{1.3f, 1.4f, 1f, 1f, 1f, 1f});
        riscos(pdf, "Riscos em aberto", dados.alertas().stream().filter(a -> a.status() != StatusAlerta.RESOLVIDO).toList());
        solicitacoesAtendidas(pdf, dados);
    }

    private void comercial(DocumentoPdf pdf, DadosRelatorio dados) {
        RelatorioIndicadoresResponse ind = dados.indicadores();
        Lojista loja = dados.lojista();
        pdf.paragrafo(loja.getNomeLoja() + " utiliza a Atria desde " + FormatoData.data(loja.getDataAdesaoAtria())
                + " para monitorar continuamente os dados dos seus clientes e responder às solicitações de privacidade dentro do prazo legal. "
                + "Este documento pode ser apresentado a parceiros e marketplaces como evidência de boas práticas.");
        pdf.secao("Destaques").indicadores(List.of(
                item("Índice de conformidade", ind.indiceConformidade() + "/100"),
                item("Atendidas no prazo", percentual(ind.solicitacoesAtendidasNoPrazo(), ind.solicitacoesAtendidas())),
                item("Tempo médio de resposta", dias(ind.tempoMedioAtendimentoDias())),
                item("Riscos resolvidos", String.valueOf(ind.alertasResolvidos())),
                item("Canais monitorados", String.valueOf(ind.plataformasConectadas())),
                item("Acessos revogados", String.valueOf(ind.acessosRevogados()))));
        pdf.secao("Canais monitorados pela Atria").tabela(
                new String[]{"Plataforma", "Tipo", "Conectada desde", "Última sincronização"},
                dados.conexoes().stream().filter(c -> c.conectada()).map(c -> new String[]{c.plataforma().nome(),
                        c.plataforma().tipoDescricao(), FormatoData.data(c.conectadaEm()), FormatoData.dataHora(c.ultimaSincronizacao())}).toList(),
                new float[]{1.4f, 1.2f, 1.2f, 1.4f});
        pdf.secao("Práticas adotadas")
                .paragrafo("• Monitoramento contínuo de mensagens, reclamações e pedidos em todos os canais conectados.")
                .paragrafo("• Atendimento às solicitações dos titulares em até 15 dias, com protocolo e comprovante.")
                .paragrafo("• Exclusão de dados pessoais com anonimização e registro auditável, sem perder o histórico de atendimento.")
                .paragrafo("• Revogação imediata do acesso de funcionários desligados.");
    }

    private void solicitacoesAtendidas(DocumentoPdf pdf, DadosRelatorio dados) {
        pdf.secao("Solicitações atendidas (comprovantes)").tabela(
                new String[]{"Protocolo", "Tipo", "Concluída em", "CPF", "Plataformas"},
                dados.evidencias().stream().map(e -> new String[]{e.protocolo(), e.tipoDescricao(),
                        FormatoData.data(e.concluidaEm()), e.cpfMascarado(), String.join(", ", e.plataformas())}).toList(),
                new float[]{1.2f, 1.3f, 1f, 1.1f, 2f});
    }

    private void riscos(DocumentoPdf pdf, String titulo, List<AlertaResponse> alertas) {
        pdf.secao(titulo).tabela(
                new String[]{"Detectado em", "Prioridade", "Ocorrência", "Plataforma", "Resolvido em"},
                alertas.stream().map(a -> new String[]{FormatoData.data(a.criadoEm()), a.prioridade().getDescricao(),
                        a.titulo(), a.plataformaNome() == null ? "-" : a.plataformaNome(), FormatoData.data(a.resolvidoEm())}).toList(),
                new float[]{1f, 0.8f, 2.4f, 1.1f, 1f});
    }

    private void historicoDeAcoes(DocumentoPdf pdf, DadosRelatorio dados) {
        pdf.secao("Histórico de ações").tabela(
                new String[]{"Data", "Ação", "Descrição"},
                dados.acoes().stream().map(a -> new String[]{FormatoData.dataHora(a.ocorridoEm()), a.acaoDescricao(), a.descricao()}).toList(),
                new float[]{1.2f, 1.3f, 3.5f});
    }

    private String descreverLoja(Lojista loja) {
        return loja.getNomeLoja() + " – " + loja.getSegmento() + " – " + loja.getCidade() + "/" + loja.getUf();
    }

    private static String[] item(String rotulo, String valor) {
        return new String[]{rotulo, valor};
    }

    private static String percentual(long parte, long total) {
        return total == 0 ? "-" : Math.round(parte * 100.0 / total) + "%";
    }

    private static String dias(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f dias", valor);
    }
}
