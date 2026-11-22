package br.com.atria.service;

import br.com.atria.dto.response.PainelResponse.Atendimento;
import br.com.atria.dto.response.PainelResponse.Canais;
import br.com.atria.dto.response.PainelResponse.Clientes;
import br.com.atria.dto.response.PainelResponse.Efeito;
import br.com.atria.dto.response.PainelResponse.IndicadorEfeito;
import br.com.atria.dto.response.PainelResponse.Posts;
import br.com.atria.dto.response.PainelResponse.Indicador;
import br.com.atria.dto.response.PainelResponse.Resumo;
import br.com.atria.dto.response.PainelResponse.Serie;
import br.com.atria.repository.memoria.AnaliseRepositoryMemoria;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Confere que os números do Painel fecham entre si, com os dados analíticos de verdade (sem mocks). */
class PainelServiceTest {
    private static PainelService servico;
    private static AnaliseRepositoryMemoria repositorio;

    @BeforeAll
    static void iniciar() {
        ObjectMapper json = new ObjectMapper();
        repositorio = new AnaliseRepositoryMemoria(json);
        servico = new PainelService(repositorio, json);
    }

    private static Indicador indicador(Resumo resumo, String id) {
        return resumo.periodo().stream().filter(i -> i.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void dataDeReferenciaEOUltimoDiaComDados() {
        assertThat(repositorio.dataReferencia()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(servico.meta().referencia()).isEqualTo("2026-11-20");
    }

    @Test
    void receitaDoPeriodoEIgualASomaDosCanais() {
        for (int dias : new int[]{7, 30, 60, 120, 365, 0}) {
            Resumo resumo = servico.resumo(dias, "todas");
            Canais canais = servico.canais(dias);
            double somaCanais = canais.canais().stream().mapToDouble(c -> c.receita()).sum();
            assertThat(indicador(resumo, "rec").valor()).isCloseTo(somaCanais, within(0.01));
        }
    }

    @Test
    void filtroDePlataformaNaoPodeSerMaiorQueOTotal() {
        double total = indicador(servico.resumo(120, "todas"), "rec").valor();
        for (String plataforma : new String[]{"nuvemshop", "mercado-livre", "shopee", "shein", "tiktok-shop"}) {
            assertThat(indicador(servico.resumo(120, plataforma), "rec").valor()).isLessThanOrEqualTo(total);
        }
    }

    @Test
    void serieDiariaFechaComOTotalESemanalSoTemSemanasCompletas() {
        Serie diaria = servico.serie(30, "todas");
        assertThat(diaria.granularidade()).isEqualTo("dia");
        assertThat(diaria.pontos().stream().mapToDouble(p -> p.receita()).sum())
                .isCloseTo(indicador(servico.resumo(30, "todas"), "rec").valor(), within(0.01));
        Serie semanal = servico.serie(120, "todas");
        assertThat(semanal.granularidade()).isEqualTo("semana");
        assertThat(semanal.pontos()).allSatisfy(p -> assertThat(LocalDate.parse(p.data()).getDayOfWeek().getValue()).isEqualTo(1));
        assertThat(semanal.pontos().stream().mapToDouble(p -> p.receita()).sum())
                .isLessThanOrEqualTo(indicador(servico.resumo(120, "todas"), "rec").valor());
        assertThat(servico.serie(0, null).granularidade()).isEqualTo("mês");
        assertThat(servico.serie(0, null).pontos()).allSatisfy(p -> assertThat(p.data()).endsWith("-01"));
    }

    @Test
    void todoOHistoricoComecaNoPrimeiroDiaESemComparacao() {
        Resumo tudo = servico.resumo(0, "todas");
        assertThat(tudo.periodoInicio()).isEqualTo(servico.meta().primeiroDia());
        assertThat(tudo.periodo()).allSatisfy(i -> assertThat(i.comparacao()).isNull());
        assertThat(servico.resumo(365, "todas").periodo()).allSatisfy(i -> assertThat(i.comparacao()).isNull());
        assertThat(servico.resumo(120, "todas").periodo()).allSatisfy(i -> assertThat(i.comparacao()).isNotNull());
        assertThat(servico.meta().periodos()).containsExactly(7, 30, 60, 120, 365, 0);
    }

    @Test
    void periodoInvalidoCaiNoPadraoDeTrintaDias() {
        assertThat(servico.resumo(13, null).periodoInicio()).isEqualTo(servico.resumo(30, null).periodoInicio());
        assertThat(servico.resumo(30, null).periodoInicio()).isEqualTo("2026-10-22");
    }

    @Test
    void margemELucroSobreReceitaComPercentuaisPlausiveis() {
        Resumo resumo = servico.resumo(365, "todas");
        double margem = indicador(resumo, "mar").valor();
        assertThat(margem).isBetween(20.0, 60.0);
        assertThat(indicador(resumo, "luc").valor() / indicador(resumo, "rec").valor() * 100).isCloseTo(margem, within(0.01));
        assertThat(indicador(resumo, "prb").valor()).isBetween(0.0, 30.0);
    }

    @Test
    void atendimentoTemFaixasPercentuaisValidos() {
        Atendimento a = servico.atendimento(90, "todos");
        assertThat(a.faixas()).hasSize(5);
        assertThat(a.faixas()).allSatisfy(f -> assertThat(f.conversao()).isBetween(0.0, 100.0));
        assertThat(a.respondidasAte1h() + a.semResposta()).isLessThanOrEqualTo(100.0);
        int soma = servico.atendimento(90, "whatsapp").conversas() + servico.atendimento(90, "instagram").conversas();
        assertThat(soma).isEqualTo(a.conversas());
    }

    @Test
    void clientesNovosMaisRecorrentesFechamComOTotal() {
        Clientes c = servico.clientes(90, "todas");
        assertThat(c.novos() + c.recorrentes()).isEqualTo(c.ativos());
        assertThat(c.segmentos().stream().mapToInt(s -> s.clientes()).sum()).isEqualTo(c.ativos());
        assertThat(c.cidades()).isNotEmpty();
    }

    @Test
    void efeitoAtriaComparaAProprioLojaAntesEDepoisSemOutrasLojas() {
        Efeito e = servico.efeito();
        assertThat(e.adocao()).isEqualTo("2025-12-01");
        assertThat(e.fimAdaptacao()).isEqualTo("2026-01-26");
        assertThat(e.serie().size()).isGreaterThan(50);
        assertThat(e.indicadores()).hasSize(4);
        assertThat(e.indicadoresRedes()).hasSize(3);
        assertThat(e.indicadoresRedes()).allSatisfy(i -> assertThat(i.antes()).isGreaterThan(0));
        assertThat(e.serieRedes()).hasSizeGreaterThan(12);
        IndicadorEfeito r = e.indicadores().get(0);
        assertThat(r.depois()).isGreaterThan(r.antes());
        assertThat(e.indicadores().get(2).depois()).isLessThan(e.indicadores().get(2).antes());
        assertThat(e.leitura()).doesNotContainIgnoringCase("simulad").doesNotContain("comparação com outras");
    }

    @Test
    void postsAgrupamEmTresFaixasPorEngajamento() {
        Posts p = servico.posts(365);
        assertThat(p.engajamento()).hasSize(3);
        assertThat(p.engajamento().stream().mapToInt(f -> f.posts()).sum()).isEqualTo(p.tipos().stream().mapToInt(t -> t.posts()).sum());
        assertThat(p.engajamento().get(2).conversasPorPost()).isGreaterThan(p.engajamento().get(0).conversasPorPost());
    }

    @Test
    void reputacaoTemUmaLinhaPorMarketplaceComNivelValido() {
        var rep = servico.reputacao();
        assertThat(rep.marketplaces()).hasSize(4);
        assertThat(rep.marketplaces()).allSatisfy(m -> assertThat(m.nivel()).isIn("verde", "amarelo", "laranja", "vermelho"));
    }
}
