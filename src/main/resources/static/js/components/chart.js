import { html } from '../core/dom.js';
import { data } from '../core/format.js';

/** Barras horizontais de uma série só (ex.: dados por tipo). itens: [{ descricao, quantidade }]. */
export function barrasHorizontais(itens) {
    const maximo = Math.max(1, ...itens.map(i => i.quantidade));
    return html`
        <div class="barras-horizontais">
            ${itens.map(i => html`
                <div class="barra-h" title="${i.descricao}: ${i.quantidade}">
                    <span>${i.descricao}</span>
                    <div class="barra-h__trilho"><div class="barra-h__valor" style="width:${Math.round(i.quantidade * 100 / maximo)}%"></div></div>
                    <strong>${i.quantidade}</strong>
                </div>`)}
        </div>`;
}

/** Colunas agrupadas por semana: alertas novos x resolvidos, com legenda e dica ao passar o mouse. */
export function colunasOcorrencias(serie) {
    const maximo = Math.max(1, ...serie.flatMap(p => [p.abertos, p.resolvidos]));
    const altura = valor => `${Math.round(valor * 100 / maximo)}%`;
    return html`
        <div class="legenda" aria-hidden="true">
            <span><i style="background:var(--cor-grafico-1)"></i>Novos</span>
            <span><i style="background:var(--cor-grafico-2)"></i>Resolvidos</span>
        </div>
        <div class="grafico-colunas" role="img" aria-label="Alertas novos e resolvidos nas últimas ${serie.length} semanas">
            ${serie.map(p => html`
                <div class="grafico-colunas__grupo" tabindex="0">
                    <span class="grafico-colunas__dica">Semana de ${data(p.semana)}: ${p.abertos} novos, ${p.resolvidos} resolvidos</span>
                    <div class="grafico-colunas__barra grafico-colunas__barra--1" style="height:${altura(p.abertos)}"></div>
                    <div class="grafico-colunas__barra grafico-colunas__barra--2" style="height:${altura(p.resolvidos)}"></div>
                </div>`)}
        </div>
        <div class="grafico-rotulos">${serie.map(p => html`<span>${data(p.semana).slice(0, 5)}</span>`)}</div>
        <table class="visualmente-oculto">
            <caption>Alertas por semana</caption>
            <tr><th>Semana</th><th>Novos</th><th>Resolvidos</th></tr>
            ${serie.map(p => html`<tr><td>${data(p.semana)}</td><td>${p.abertos}</td><td>${p.resolvidos}</td></tr>`)}
        </table>`;
}
