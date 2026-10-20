package br.com.atria.service;

import br.com.atria.model.Acesso;
import br.com.atria.model.Alerta;
import br.com.atria.model.PendenciaCanal;
import br.com.atria.model.Plataforma;
import br.com.atria.model.SolicitacaoLgpd;
import br.com.atria.model.Titular;
import br.com.atria.model.Usuario;
import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.model.enums.TipoAlerta;
import br.com.atria.service.regra.CalculadoraPrazo;
import br.com.atria.service.regra.ClassificadorPrioridade;
import br.com.atria.service.regra.ConcordanciaPlataforma;
import br.com.atria.service.regra.FormatoData;
import br.com.atria.service.regra.OcorrenciaRisco;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Monta um alerta (título, descrição e prioridade) a partir de cada tipo de ocorrência.
 * Os textos não levam o nome do titular, que é resolvido na consulta e some após a anonimização.
 */
@Component
public class FabricaAlertas {
    private final ClassificadorPrioridade classificador;
    private final CalculadoraPrazo calculadoraPrazo;

    public FabricaAlertas(ClassificadorPrioridade classificador, CalculadoraPrazo calculadoraPrazo) {
        this.classificador = classificador;
        this.calculadoraPrazo = calculadoraPrazo;
    }

    public Alerta dePendencia(PendenciaCanal pendencia, Plataforma plataforma, Titular titular, LocalDateTime agora) {
        TipoAlerta tipo = switch (pendencia.getTipo()) {
            case MENSAGEM_SEM_RESPOSTA -> TipoAlerta.CLIENTE_SEM_RESPOSTA;
            case RECLAMACAO_PENDENTE -> TipoAlerta.RECLAMACAO_PENDENTE;
            case PEDIDO_NAO_ENVIADO -> TipoAlerta.PEDIDO_NAO_ENVIADO;
        };
        String desde = FormatoData.dataHora(pendencia.getDesde());
        String local = ConcordanciaPlataforma.comPreposicao(plataforma.getNome());
        String titulo = switch (tipo) {
            case CLIENTE_SEM_RESPOSTA -> "Cliente sem resposta " + local;
            case RECLAMACAO_PENDENTE -> "Reclamação não atendida " + local;
            default -> "Pedido não enviado " + local;
        };
        String descricao = switch (tipo) {
            case CLIENTE_SEM_RESPOSTA -> "Cliente aguarda resposta desde " + desde + ".";
            case RECLAMACAO_PENDENTE -> "Reclamação aberta em " + desde + " ainda sem tratamento.";
            default -> "Pedido aguardando envio desde " + desde + ".";
        };
        return new Alerta(pendencia.getLojistaId(), plataforma.getId(), titular.getId(), tipo,
                classificador.classificar(OcorrenciaRisco.deCanal(tipo, pendencia.getDesde(), agora)),
                titulo, descricao, 1, OrigemAlerta.PENDENCIA, pendencia.getId(), pendencia.getDesde());
    }

    public Alerta deAcesso(Acesso acesso, Usuario usuario, Plataforma plataforma, int clientesExpostos) {
        String descricao = usuario.getNome() + " (" + usuario.getPapel().getDescricao() + ") foi desligado(a) em "
                + FormatoData.data(usuario.getDataDesligamento()) + ", mas ainda tem acesso " + ConcordanciaPlataforma.comPreposicao(plataforma.getNome())
                + " pela Atria. Revogue o acesso para proteger os dados de " + clientesExpostos + " cliente(s).";
        return new Alerta(usuario.getLojistaId(), plataforma.getId(), null, TipoAlerta.ACESSO_NAO_REVOGADO,
                classificador.classificar(OcorrenciaRisco.deAcessoNaoRevogado()),
                "Acesso não revogado de ex-funcionário", descricao, clientesExpostos,
                OrigemAlerta.ACESSO, acesso.getId(), usuario.getDataDesligamento().atStartOfDay());
    }

    public Alerta deSolicitacao(SolicitacaoLgpd solicitacao, Titular titular) {
        long dias = calculadoraPrazo.diasRestantes(solicitacao.getPrazo());
        String situacao = dias < 0 ? "venceu há " + Math.abs(dias) + " dia(s)"
                : dias == 0 ? "vence hoje" : "vence em " + dias + " dia(s)";
        String descricao = "Solicitação " + solicitacao.getProtocolo()
                + " (recebida via " + solicitacao.getCanalOrigem().getDescricao() + ") " + situacao
                + ". Prazo: " + FormatoData.data(solicitacao.getPrazo()) + ".";
        return new Alerta(titular.getLojistaId(), null, titular.getId(), TipoAlerta.PRAZO_LGPD,
                classificador.classificar(OcorrenciaRisco.dePrazo(dias)),
                "Prazo LGPD: " + solicitacao.getTipo().getDescricao(), descricao, 1,
                OrigemAlerta.SOLICITACAO, solicitacao.getId(), solicitacao.getAbertaEm());
    }
}
