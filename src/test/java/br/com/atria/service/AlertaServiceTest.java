package br.com.atria.service;

import br.com.atria.dto.request.AlterarStatusAlertaRequest;
import br.com.atria.dto.response.AlertaResponse;
import br.com.atria.dto.response.VarreduraResponse;
import br.com.atria.exception.RegraNegocioException;
import br.com.atria.model.Acesso;
import br.com.atria.model.PendenciaCanal;
import br.com.atria.model.Plataforma;
import br.com.atria.model.Titular;
import br.com.atria.model.enums.Prioridade;
import br.com.atria.model.enums.StatusAlerta;
import br.com.atria.model.enums.TipoAlerta;
import br.com.atria.model.enums.TipoPendencia;
import br.com.atria.model.enums.TipoPlataforma;
import br.com.atria.model.enums.TipoSolicitacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.atria.RelogioFixo.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertaServiceTest {
    private CenarioServicos cenario;
    private Plataforma whatsapp;
    private Plataforma instagram;
    private Plataforma mercadoLivre;
    private Titular ana;
    private Titular bia;

    @BeforeEach
    void montarCenario() {
        cenario = new CenarioServicos();
        whatsapp = cenario.plataforma("whatsapp", "WhatsApp", TipoPlataforma.MENSAGERIA, true);
        instagram = cenario.plataforma("instagram", "Instagram", TipoPlataforma.REDE_SOCIAL, true);
        mercadoLivre = cenario.plataforma("mercado-livre", "Mercado Livre", TipoPlataforma.MARKETPLACE, true);
        Plataforma shopee = cenario.plataforma("shopee", "Shopee", TipoPlataforma.MARKETPLACE, false);
        ana = cenario.titular("Ana Teste", "111.444.777-35", "ana@exemplo.com");
        bia = cenario.titular("Bia Teste", "529.982.247-25", "bia@exemplo.com");

        cenario.pendencia(ana, instagram, TipoPendencia.MENSAGEM_SEM_RESPOSTA, AGORA.minusHours(6));
        cenario.pendencia(bia, whatsapp, TipoPendencia.RECLAMACAO_PENDENTE, AGORA.minusHours(30));
        cenario.pendencia(ana, mercadoLivre, TipoPendencia.PEDIDO_NAO_ENVIADO, AGORA.minusDays(2));
        cenario.pendencia(bia, mercadoLivre, TipoPendencia.PEDIDO_NAO_ENVIADO, AGORA.minusHours(3));
        cenario.pendencia(bia, shopee, TipoPendencia.PEDIDO_NAO_ENVIADO, AGORA.minusDays(3));
    }

    @Test
    void varreduraCriaUmAlertaPorOcorrenciaEmPlataformaConectada() {
        VarreduraResponse resultado = cenario.alertaService.executarVarredura();

        assertThat(resultado.alertasCriados()).isEqualTo(4);
        assertThat(cenario.alertas.findAll()).extracting(a -> a.getPlataformaId()).doesNotContain(cenario.plataformas.findBySlug("shopee").orElseThrow().getId());
    }

    @Test
    void varreduraEhIdempotente() {
        cenario.alertaService.executarVarredura();
        long total = cenario.alertas.count();

        VarreduraResponse segunda = cenario.alertaService.executarVarredura();

        assertThat(segunda.alertasCriados()).isZero();
        assertThat(cenario.alertas.count()).isEqualTo(total);
    }

    @Test
    void varreduraNaoRecriaAlertaJaResolvido() {
        cenario.alertaService.executarVarredura();
        Long id = cenario.alertas.findAll().get(0).getId();
        cenario.alertaService.alterarStatus(id, new AlterarStatusAlertaRequest(StatusAlerta.RESOLVIDO));

        assertThat(cenario.alertaService.executarVarredura().alertasCriados()).isZero();
    }

    @Test
    void classificaAPrioridadeDeCadaAlerta() {
        cenario.alertaService.executarVarredura();

        List<AlertaResponse> alertas = cenario.alertaService.listar(null, null, null);
        assertThat(alertas).filteredOn(a -> a.tipo() == TipoAlerta.RECLAMACAO_PENDENTE).singleElement()
                .extracting(AlertaResponse::prioridade).isEqualTo(Prioridade.ALTA);
        assertThat(alertas).filteredOn(a -> a.tipo() == TipoAlerta.CLIENTE_SEM_RESPOSTA).singleElement()
                .extracting(AlertaResponse::prioridade).isEqualTo(Prioridade.MEDIA);
        assertThat(alertas).filteredOn(a -> a.tipo() == TipoAlerta.PEDIDO_NAO_ENVIADO)
                .extracting(AlertaResponse::prioridade).containsExactlyInAnyOrder(Prioridade.MEDIA, Prioridade.BAIXA);
        assertThat(alertas.get(0).prioridade()).isEqualTo(Prioridade.ALTA);
    }

    @Test
    void geraAlertasDeAcessoEDePrazoLgpd() {
        Acesso acesso = cenario.acessoDeExFuncionario(whatsapp);
        cenario.solicitacao(ana, TipoSolicitacao.ACESSO, 14);

        cenario.alertaService.executarVarredura();

        List<AlertaResponse> alertas = cenario.alertaService.listar(Prioridade.ALTA, null, null);
        assertThat(alertas).anySatisfy(a -> {
            assertThat(a.tipo()).isEqualTo(TipoAlerta.ACESSO_NAO_REVOGADO);
            assertThat(a.acessoRevogavelId()).isEqualTo(acesso.getId());
        });
        assertThat(alertas).anySatisfy(a -> {
            assertThat(a.tipo()).isEqualTo(TipoAlerta.PRAZO_LGPD);
            assertThat(a.diasRestantes()).isEqualTo(1);
        });
    }

    @Test
    void fraseResumoAgrupaOsAlertasAbertos() {
        cenario.alertaService.executarVarredura();

        assertThat(cenario.alertaService.fraseResumo()).isEqualTo("Você tem 1 cliente sem resposta no Instagram, "
                + "1 reclamação não atendida no WhatsApp e 2 pedidos não enviados no Mercado Livre.");
    }

    @Test
    void resolverAlertaDePendenciaMarcaAPendenciaComoResolvida() {
        cenario.alertaService.executarVarredura();
        AlertaResponse alerta = cenario.alertaService.listar(null, null, "instagram").get(0);

        AlertaResponse resolvido = cenario.alertaService.alterarStatus(alerta.id(), new AlterarStatusAlertaRequest(StatusAlerta.RESOLVIDO));

        assertThat(resolvido.status()).isEqualTo(StatusAlerta.RESOLVIDO);
        assertThat(resolvido.resolvidoEm()).isEqualTo(AGORA);
        PendenciaCanal pendencia = cenario.pendencias.findById(alerta.origemId()).orElseThrow();
        assertThat(pendencia.isResolvida()).isTrue();
    }

    @Test
    void alertaDeAcessoSoEhResolvidoRevogandoOAcesso() {
        Acesso acesso = cenario.acessoDeExFuncionario(whatsapp);
        cenario.alertaService.executarVarredura();
        AlertaResponse alerta = cenario.alertaService.listar(null, null, null).stream()
                .filter(a -> a.tipo() == TipoAlerta.ACESSO_NAO_REVOGADO).findFirst().orElseThrow();

        assertThatThrownBy(() -> cenario.alertaService.alterarStatus(alerta.id(), new AlterarStatusAlertaRequest(StatusAlerta.RESOLVIDO)))
                .isInstanceOf(RegraNegocioException.class);

        cenario.acessoService.revogar(acesso.getId());

        AlertaResponse depois = cenario.alertaService.detalhe(alerta.id());
        assertThat(depois.status()).isEqualTo(StatusAlerta.RESOLVIDO);
        assertThat(depois.acessoRevogavelId()).isNull();
        assertThatThrownBy(() -> cenario.acessoService.revogar(acesso.getId())).isInstanceOf(RegraNegocioException.class);
    }
}
