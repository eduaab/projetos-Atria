import { html } from '../core/dom.js';

/** variante: '' | 'alerta' | 'sucesso' | 'aviso' */
export const cartaoEstatistica = (rotulo, valor, variante = '') => html`
    <div class="cartao-estatistica ${variante ? `cartao-estatistica--${variante}` : ''}">
        <strong>${valor}</strong><span>${rotulo}</span>
    </div>`;
