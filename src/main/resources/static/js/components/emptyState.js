import { html } from '../core/dom.js';
import { icone } from './icons.js';

export const estadoVazio = (texto, nomeIcone = 'check') =>
    html`<div class="estado-vazio">${icone(nomeIcone)}<span>${texto}</span></div>`;
