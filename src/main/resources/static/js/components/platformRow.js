import { html } from '../core/dom.js';
import { data, tempoRelativo } from '../core/format.js';
import { icone, iconePlataforma } from './icons.js';
import { badgeStatus } from './statusBadge.js';

/** Linha do Hub de Conexões: conectada mostra link externo; não conectada mostra "conectar". */
export function linhaConexao(conexao) {
    const p = conexao.plataforma;
    const detalhe = conexao.conectada
        ? `Conectada em ${data(conexao.conectadaEm)} · sincronizada ${tempoRelativo(conexao.ultimaSincronizacao)}`
        : conexao.desconectadaEm ? `Desconectada em ${data(conexao.desconectadaEm)}` : 'Ainda não conectada';
    return html`
        <div class="linha-branca ${conexao.conectada ? 'linha-branca--conectada' : ''}">
            <div class="linha-branca__principal">
                <span class="icone-plataforma">${iconePlataforma(p.slug)}</span>
                <div class="linha-branca__textos"><strong>${p.nome}</strong><span>${p.tipoDescricao} · ${detalhe}</span></div>
            </div>
            <div class="linha-branca__lado">
                ${conexao.conectada
                    ? html`${badgeStatus('ATIVA', 'Conectada')}
                        <button class="botao botao--pequeno" data-acao="desconectar" data-slug="${p.slug}">Desconectar</button>
                        <a class="botao botao--icone" href="#/conexoes/${p.slug}" title="Detalhes de ${p.nome}" aria-label="Detalhes de ${p.nome}">${icone('externo')}</a>`
                    : html`<button class="botao botao--primario botao--pequeno" data-acao="conectar" data-slug="${p.slug}">${icone('entrar')} Conectar</button>
                        <a class="botao botao--icone" href="#/conexoes/${p.slug}" title="Detalhes de ${p.nome}" aria-label="Detalhes de ${p.nome}">${icone('externo')}</a>`}
            </div>
        </div>`;
}

/** Linha do perfil do titular: plataforma em que os dados foram encontrados. */
export function linhaPlataformaTitular(plataforma) {
    return html`
        <div class="linha-branca" data-plataforma="${plataforma.slug}">
            <div class="linha-branca__principal">
                <span class="icone-plataforma">${iconePlataforma(plataforma.slug)}</span>
                <div class="linha-branca__textos">
                    <strong>${plataforma.nome}</strong>
                    <span>${plataforma.idExterno} · vínculo por ${plataforma.metodoVinculo}</span>
                    <div>${plataforma.tiposDado.map(tipo => html`<span class="chip">${tipo}</span>`)}</div>
                </div>
            </div>
            <span class="status-remocao" data-status-remocao>
                <a class="botao botao--icone" href="#/conexoes/${plataforma.slug}" title="Ver integração" aria-label="Ver integração ${plataforma.nome}">${icone('externo')}</a>
            </span>
        </div>`;
}
