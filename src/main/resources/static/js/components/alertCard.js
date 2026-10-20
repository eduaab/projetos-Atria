import { html } from '../core/dom.js';
import { dataHora, plural, textoPrazo } from '../core/format.js';

const CLASSE_PRIORIDADE = { ALTA: '', MEDIA: 'card-pilula--media', BAIXA: 'card-pilula--baixa' };

function pilula({ classe, titulo, subtitulo, rotulo, destaque, botao }) {
    return html`
        <article class="card-pilula ${classe}">
            <span class="card-pilula__exclamacao" aria-hidden="true">!</span>
            <span class="avatar" aria-hidden="true"></span>
            <div class="card-pilula__pessoa"><strong>${titulo}</strong><span>${subtitulo}</span></div>
            <span class="card-pilula__divisor" aria-hidden="true"></span>
            <div class="card-pilula__meta"><span>${rotulo}</span><strong>${destaque}</strong></div>
            ${botao}
        </article>`;
}

/** Card de solicitação do titular (Figma): nome, e-mail, tipo e prazo restante. */
export function cardSolicitacao(solicitacao) {
    const concluida = solicitacao.status === 'CONCLUIDA';
    return pilula({
        classe: concluida ? 'card-pilula--resolvido' : (solicitacao.diasRestantes > 7 ? 'card-pilula--baixa' : ''),
        titulo: solicitacao.titularNome,
        subtitulo: solicitacao.titularEmail ? `E-mail: ${solicitacao.titularEmail}` : `Protocolo ${solicitacao.protocolo}`,
        rotulo: `${solicitacao.tipoDescricao} · via ${solicitacao.canalDescricao}`,
        destaque: concluida ? 'Concluída' : textoPrazo(solicitacao.diasRestantes),
        botao: html`<button class="botao botao--contorno-branco" data-acao="ver-titular" data-id="${solicitacao.titularId}">Visualizar</button>`,
    });
}

/** Card de alerta com cor por prioridade, plataforma, data/hora e clientes afetados. */
export function cardAlerta(alerta) {
    const resolvido = alerta.status === 'RESOLVIDO';
    const local = alerta.plataformaNome || alerta.origemDescricao;
    return pilula({
        classe: resolvido ? 'card-pilula--resolvido' : CLASSE_PRIORIDADE[alerta.prioridade],
        titulo: alerta.titulo,
        subtitulo: alerta.titularNome ? `${alerta.titularNome} · ${local}` : local,
        rotulo: `${alerta.statusDescricao} · ${dataHora(alerta.criadoEm)} · ${plural(alerta.clientesAfetados, 'cliente afetado', 'clientes afetados')}`,
        destaque: alerta.diasRestantes !== null && alerta.diasRestantes !== undefined
            ? textoPrazo(alerta.diasRestantes) : `Prioridade ${alerta.prioridade === 'MEDIA' ? 'média' : alerta.prioridade.toLowerCase()}`,
        botao: html`<button class="botao botao--contorno-branco" data-acao="ver-alerta" data-id="${alerta.id}">Visualizar</button>`,
    });
}
