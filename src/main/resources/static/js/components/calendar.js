import { html } from '../core/dom.js';
import { chaveDia } from '../core/format.js';
import { icone } from './icons.js';

const SEMANA = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB'];
const GRAVIDADE = ['VERMELHO', 'LARANJA', 'AMARELO', 'VERDE'];
const FORMATO_MES = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' });

/** Calendário mensal com os dias de prazo marcados pela faixa mais grave do dia. */
export function calendario(mes, prazos, diaSelecionado) {
    const porDia = new Map();
    prazos.forEach(p => porDia.set(p.prazo, [...(porDia.get(p.prazo) || []), p]));
    const primeiroDia = new Date(mes.getFullYear(), mes.getMonth(), 1);
    const totalDias = new Date(mes.getFullYear(), mes.getMonth() + 1, 0).getDate();
    const hoje = chaveDia(new Date());

    const dias = [];
    for (let i = 0; i < primeiroDia.getDay(); i++) dias.push(html`<span></span>`);
    for (let dia = 1; dia <= totalDias; dia++) {
        const chave = chaveDia(new Date(mes.getFullYear(), mes.getMonth(), dia));
        const doDia = porDia.get(chave) || [];
        const nivel = GRAVIDADE.find(g => doDia.some(p => p.nivelPrazo === g));
        const classes = ['calendario__dia',
            chave === hoje ? 'calendario__dia--hoje' : '',
            nivel ? `calendario__dia--prazo calendario__dia--${nivel.toLowerCase()}` : '',
            chave === diaSelecionado ? 'calendario__dia--selecionado' : ''].join(' ');
        const rotulo = doDia.length ? `${dia}: ${doDia.length} prazo(s)` : String(dia);
        dias.push(html`<button class="${classes}" data-acao="selecionar-dia" data-dia="${chave}" aria-label="${rotulo}">${dia}</button>`);
    }

    return html`
        <div class="calendario__cabecalho">
            <span class="calendario__titulo">${FORMATO_MES.format(mes)}</span>
            <div class="linha-acoes">
                <button class="botao botao--icone" data-acao="mes-anterior" aria-label="Mês anterior">${icone('voltar')}</button>
                <button class="botao botao--icone" data-acao="mes-seguinte" aria-label="Próximo mês" style="transform: scaleX(-1)">${icone('voltar')}</button>
            </div>
        </div>
        <div class="calendario__grade">
            ${SEMANA.map(d => html`<span class="calendario__semana">${d}</span>`)}
            ${dias}
        </div>`;
}
