import { html } from '../core/dom.js';
import { plural } from '../core/format.js';

export function linhaCliente(titular) {
    return html`
        <div class="linha-branca linha-branca--pilula">
            <div class="linha-branca__principal">
                <span class="avatar avatar--escuro" aria-hidden="true"></span>
                <div class="linha-branca__textos">
                    <strong>${titular.nome}</strong>
                    <span>CPF: ${titular.cpfMascarado} · ${titular.email}</span>
                    <span>${titular.telefone} · ${plural(titular.quantidadePlataformas, 'plataforma', 'plataformas')}</span>
                </div>
            </div>
            <div class="linha-branca__lado">
                ${titular.possuiSolicitacaoAberta ? html`<span class="badge badge--alta">Solicitação aberta</span>` : ''}
                <button class="botao" data-acao="ver-titular" data-id="${titular.id}">Visualizar</button>
            </div>
        </div>`;
}
