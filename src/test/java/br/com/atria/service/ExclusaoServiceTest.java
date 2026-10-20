package br.com.atria.service;

import br.com.atria.dto.response.ExclusaoPreviaResponse;
import br.com.atria.dto.response.ExclusaoResponse;
import br.com.atria.exception.RegraNegocioException;
import br.com.atria.model.Evidencia;
import br.com.atria.model.Plataforma;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.Titular;
import br.com.atria.model.enums.AcaoAuditoria;
import br.com.atria.model.enums.CanalOrigem;
import br.com.atria.model.enums.StatusSolicitacao;
import br.com.atria.model.enums.TipoDado;
import br.com.atria.model.enums.TipoPlataforma;
import br.com.atria.model.enums.TipoSolicitacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.com.atria.RelogioFixo.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExclusaoServiceTest {
    private CenarioServicos cenario;
    private Titular roberto;

    @BeforeEach
    void montarCenario() {
        cenario = new CenarioServicos();
        Plataforma whatsapp = cenario.plataforma("whatsapp", "WhatsApp", TipoPlataforma.MENSAGERIA, true);
        Plataforma nuvemshop = cenario.plataforma("nuvemshop", "Nuvemshop", TipoPlataforma.LOJA_PROPRIA, true);
        roberto = cenario.titular("Roberto Lucas Almeida", "123.456.789-09", "roberto@exemplo.com");
        cenario.vincular(roberto, whatsapp, TipoDado.NOME, TipoDado.TELEFONE);
        cenario.vincular(roberto, nuvemshop, TipoDado.NOME, TipoDado.CPF, TipoDado.EMAIL);
    }

    @Test
    void previaMostraOQueSeraApagadoEPreservado() {
        ExclusaoPreviaResponse previa = cenario.exclusaoService.previa(roberto.getId());

        assertThat(previa.dadosApagados()).hasSize(5);
        assertThat(previa.plataformas()).containsExactly("Nuvemshop", "WhatsApp");
        assertThat(previa.dadosPreservados()).anyMatch(item -> item.contains("***.***.789-**"));
    }

    @Test
    void exclusaoAnonimizaRemoveVinculosConcluiSolicitacaoEGeraEvidencia() {
        SolicitacaoLgpd pedido = cenario.solicitacao(roberto, TipoSolicitacao.EXCLUSAO, 3);

        ExclusaoResponse resposta = cenario.exclusaoService.executar(roberto.getId());

        assertThat(resposta.protocolo()).isEqualTo(pedido.getProtocolo());
        assertThat(resposta.cpfMascarado()).isEqualTo("***.***.789-**");
        assertThat(resposta.hashComprovante()).hasSize(64);
        assertThat(roberto.isAnonimizado()).isTrue();
        assertThat(roberto.getNome()).isNull();
        assertThat(cenario.identidades.findByTitularId(roberto.getId())).isEmpty();
        assertThat(cenario.dados.findByTitularId(roberto.getId())).isEmpty();
        assertThat(pedido.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
        assertThat(pedido.getConcluidaEm()).isEqualTo(AGORA);

        Evidencia evidencia = cenario.evidencias.findByProtocolo(pedido.getProtocolo()).orElseThrow();
        assertThat(evidencia.getPlataformas()).containsExactly("Nuvemshop", "WhatsApp");
        assertThat(cenario.auditoria.findAll()).extracting(r -> r.getAcao()).contains(AcaoAuditoria.TITULAR_EXCLUIDO);
        assertThat(cenario.auditoria.findAll()).noneMatch(r -> r.getDescricao().contains("Roberto"));
    }

    @Test
    void semSolicitacaoAbertaCriaUmaComProtocoloAutomatico() {
        ExclusaoResponse resposta = cenario.exclusaoService.executar(roberto.getId());

        assertThat(resposta.protocolo()).matches("ATR-[0-9A-F]{8}");
        SolicitacaoLgpd criada = cenario.solicitacoes.findByProtocolo(resposta.protocolo()).orElseThrow();
        assertThat(criada.getCanalOrigem()).isEqualTo(CanalOrigem.PAINEL);
        assertThat(criada.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
    }

    @Test
    void titularAnonimizadoSaiDaBuscaMasSegueNoHistorico() {
        cenario.exclusaoService.executar(roberto.getId());

        assertThat(cenario.titularService.buscar("Roberto")).isEmpty();
        assertThat(cenario.titularService.buscar("789")).isEmpty();
        assertThat(cenario.titularService.buscar("roberto@exemplo.com")).isEmpty();
        assertThat(cenario.titularService.detalhe(roberto.getId()).nome()).isEqualTo("Titular anonimizado #" + roberto.getId());
    }

    @Test
    void excluirDeNovoEhErroDeRegra() {
        cenario.exclusaoService.executar(roberto.getId());

        assertThatThrownBy(() -> cenario.exclusaoService.executar(roberto.getId()))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(cenario.evidencias.count()).isEqualTo(1);
    }
}
