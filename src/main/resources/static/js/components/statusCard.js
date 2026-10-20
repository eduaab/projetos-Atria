import { html } from '../core/dom.js';
import { icone } from './icons.js';

/** Card verde com check quando não há pendência; com cor de alerta e contagem quando há. */
export function cartaoStatus(titulo, status, acao = '') {
    const ok = status.emConformidade;
    return html`
        <section class="cartao-status ${ok ? 'cartao-status--ok' : 'cartao-status--pendente'}">
            <h2>${titulo}</h2>
            <div class="cartao-status__icone">${ok ? icone('check') : html`<span class="cartao-status__contagem">${status.pendencias}</span>`}</div>
            <p>${status.mensagem}</p>
            ${acao}
        </section>`;
}
