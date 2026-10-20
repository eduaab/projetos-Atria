import { html } from '../core/dom.js';
import { textoPrazo } from '../core/format.js';

const CLASSE_PRIORIDADE = { ALTA: 'alta', MEDIA: 'media', BAIXA: 'baixa' };
const ROTULO_PRIORIDADE = { ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' };
const CLASSE_STATUS = {
    ABERTO: 'alta', EM_ANALISE: 'media', RESOLVIDO: 'sucesso',
    PENDENTE: 'neutro', EM_ANDAMENTO: 'media', CONCLUIDA: 'sucesso', ATRASADA: 'vermelho',
    ATIVA: 'sucesso', REVOGADA: 'neutro', ERRO: 'alta',
};
const CLASSE_NIVEL = { VERDE: 'verde', AMARELO: 'amarelo', LARANJA: 'laranja', VERMELHO: 'vermelho' };

export const badgePrioridade = prioridade =>
    html`<span class="badge badge--${CLASSE_PRIORIDADE[prioridade]}">Prioridade ${ROTULO_PRIORIDADE[prioridade]}</span>`;

export const badgeStatus = (status, descricao) =>
    html`<span class="badge badge--${CLASSE_STATUS[status] || 'neutro'}">${descricao || status}</span>`;

/** Indicador de prazo restante com cor por faixa (verde, amarelo, laranja, vermelho). */
export const indicadorPrazo = (nivel, dias, concluida = false) => concluida
    ? html`<span class="badge badge--sucesso">Concluída</span>`
    : html`<span class="badge badge--${CLASSE_NIVEL[nivel]}">${textoPrazo(dias)}</span>`;

export const rotuloPrioridade = prioridade => ROTULO_PRIORIDADE[prioridade];
