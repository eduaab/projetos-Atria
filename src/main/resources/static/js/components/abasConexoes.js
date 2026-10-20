import { html } from '../core/dom.js';

/** Abas da área de Conexões: plataformas e mapeamento de dados. */
export const abasConexoes = ativa => html`
    <nav class="abas" aria-label="Seções de conexões">
        <a href="#/conexoes" class="${ativa === 'plataformas' ? 'ativa' : ''}">Plataformas</a>
        <a href="#/mapeamento" class="${ativa === 'mapeamento' ? 'ativa' : ''}">Mapeamento de dados</a>
    </nav>`;
