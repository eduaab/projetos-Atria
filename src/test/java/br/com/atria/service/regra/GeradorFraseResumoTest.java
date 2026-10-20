package br.com.atria.service.regra;

import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.atria.model.enums.TipoAlerta.ACESSO_NAO_REVOGADO;
import static br.com.atria.model.enums.TipoAlerta.CLIENTE_SEM_RESPOSTA;
import static br.com.atria.model.enums.TipoAlerta.PEDIDO_NAO_ENVIADO;
import static br.com.atria.model.enums.TipoAlerta.PRAZO_LGPD;
import static br.com.atria.model.enums.TipoAlerta.RECLAMACAO_PENDENTE;
import static org.assertj.core.api.Assertions.assertThat;

class GeradorFraseResumoTest {
    private final GeradorFraseResumo gerador = new GeradorFraseResumo();

    @Test
    void agrupaPorTipoEPlataformaComPluralCorreto() {
        String frase = gerador.gerar(List.of(
                new ItemResumo(PEDIDO_NAO_ENVIADO, "Mercado Livre"),
                new ItemResumo(CLIENTE_SEM_RESPOSTA, "Instagram"),
                new ItemResumo(RECLAMACAO_PENDENTE, "WhatsApp"),
                new ItemResumo(PEDIDO_NAO_ENVIADO, "Mercado Livre")));

        assertThat(frase).isEqualTo("Você tem 1 cliente sem resposta no Instagram, 1 reclamação não atendida no WhatsApp "
                + "e 2 pedidos não enviados no Mercado Livre.");
    }

    @Test
    void usaPreposicaoFemininaEItensSemPlataforma() {
        String frase = gerador.gerar(List.of(
                new ItemResumo(RECLAMACAO_PENDENTE, "Shopee"),
                new ItemResumo(RECLAMACAO_PENDENTE, "Shopee"),
                new ItemResumo(PRAZO_LGPD, null)));

        assertThat(frase).isEqualTo("Você tem 2 reclamações não atendidas na Shopee e 1 solicitação LGPD com prazo correndo.");
    }

    @Test
    void umUnicoItemNaoUsaConjuncao() {
        assertThat(gerador.gerar(List.of(new ItemResumo(ACESSO_NAO_REVOGADO, "WhatsApp"))))
                .isEqualTo("Você tem 1 acesso de ex-funcionário não revogado no WhatsApp.");
    }

    @Test
    void semAlertasDizQueEstaTudoEmDia() {
        assertThat(gerador.gerar(List.of())).isEqualTo(GeradorFraseResumo.SEM_PENDENCIAS);
    }
}
