import { escapeHtml, raw } from '../core/dom.js';

/**
 * Gráficos em SVG puro para o Painel (sem bibliotecas, no estilo do restante do front).
 * Todo texto vindo de dados passa por escapeHtml; os números são formatados aqui.
 */
const COR = { 1: 'var(--cor-grafico-1)', 2: 'var(--cor-grafico-2)', unica: 'var(--cor-grafico-unica)' };
const EIXO = 'var(--cor-texto-suave)';
const GRADE = 'var(--cor-borda)';

const fmtNum = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
export const fmt = {
    num: v => fmtNum.format(v),
    dec: v => new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v),
    pct: v => `${fmtNum.format(v)}%`,
    moeda: v => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(v),
    mil: v => (Math.abs(v) >= 1000 ? `${fmtNum.format(v / 1000)} mil` : fmtNum.format(v)),
};

function escala(min, max, a, b) {
    const amplitude = max - min || 1;
    return v => a + ((v - min) / amplitude) * (b - a);
}

/** Marcas "redondas" (1, 2, 5 × potência de 10) entre min e max. */
function ticks(min, max, n = 4) {
    const bruto = (max - min) / n || 1;
    const pot = 10 ** Math.floor(Math.log10(bruto));
    const passo = [1, 2, 2.5, 5, 10].map(f => f * pot).find(p => p >= bruto) || bruto;
    const inicio = Math.ceil(min / passo) * passo;
    const marcas = [];
    for (let v = inicio; v <= max + passo * 1e-6; v += passo) marcas.push(Number(v.toFixed(6)));
    return marcas;
}

/**
 * Linhas com várias séries. series: [{ nome, cor: 1|2|'unica', valores: number[] }], rotulos: string[] (um por ponto).
 * marcador: { indice, rotulo } desenha uma linha vertical (ex.: data de adoção da Atria).
 * faixa: { de, ate, rotulo } sombreia um trecho do eixo X (índices), ex.: período de adaptação.
 */
export function linhas({ series, rotulos, formato = fmt.num, marcador = null, faixa = null, altura = 260, largura = 760, minY = null, titulo = '' }) {
    const L = largura, A = altura, m = { e: 54, d: 14, t: 14, b: 30 };
    const todos = series.flatMap(s => s.valores).filter(v => v !== null && v !== undefined);
    const lo = minY ?? Math.min(0, ...todos);
    const hi = Math.max(...todos) * 1.08 || 1;
    m.e = Math.max(54, 16 + 7 * Math.max(...ticks(lo, hi).map(t => formato(t).length)));
    const sx = escala(0, Math.max(1, rotulos.length - 1), m.e, L - m.d);
    const sy = escala(lo, hi, A - m.b, m.t);
    const grade = ticks(lo, hi).map(t => `<line x1="${m.e}" x2="${L - m.d}" y1="${sy(t)}" y2="${sy(t)}" stroke="${GRADE}" stroke-width="1"/>`
        + `<text x="${m.e - 8}" y="${sy(t) + 4}" text-anchor="end" font-size="11" fill="${EIXO}">${escapeHtml(formato(t))}</text>`).join('');
    const passoRotulo = Math.max(1, Math.ceil(rotulos.length / 8));
    const eixoX = rotulos.map((r, i) => (i % passoRotulo === 0
        ? `<text x="${sx(i)}" y="${A - 8}" text-anchor="middle" font-size="11" fill="${EIXO}">${escapeHtml(r)}</text>` : '')).join('');
    const caminhos = series.map(s => {
        const d = s.valores.map((v, i) => (v === null || v === undefined ? '' : `${i === 0 || s.valores[i - 1] == null ? 'M' : 'L'}${sx(i).toFixed(1)},${sy(v).toFixed(1)}`)).join(' ');
        const pontos = rotulos.length <= 40 ? s.valores.map((v, i) => (v == null ? '' : `<circle cx="${sx(i)}" cy="${sy(v)}" r="3.2" fill="${COR[s.cor]}"><title>${escapeHtml(s.nome)} · ${escapeHtml(rotulos[i])}: ${escapeHtml(formato(v))}</title></circle>`)).join('') : '';
        return `<path d="${d}" fill="none" stroke="${COR[s.cor]}" stroke-width="2.4" stroke-linejoin="round"/>${pontos}`;
    }).join('');
    const marc = marcador ? `<line x1="${sx(marcador.indice)}" x2="${sx(marcador.indice)}" y1="${m.t}" y2="${A - m.b}" stroke="var(--cor-roxo-titulo)" stroke-width="1.5" stroke-dasharray="5 4"/>`
        + `<text x="${sx(marcador.indice) + 6}" y="${m.t + 12}" font-size="11" font-weight="700" fill="var(--cor-roxo-titulo)">${escapeHtml(marcador.rotulo)}</text>` : '';
    const sombra = faixa ? `<rect x="${sx(faixa.de)}" y="${m.t}" width="${Math.max(0, sx(faixa.ate) - sx(faixa.de))}" height="${A - m.b - m.t}" fill="var(--cor-lilas-claro)" opacity=".7"/>`
        + `<text x="${(sx(faixa.de) + sx(faixa.ate)) / 2}" y="${A - m.b - 8}" text-anchor="middle" font-size="11" fill="${EIXO}">${escapeHtml(faixa.rotulo)}</text>` : '';
    // áreas de captura de dica para séries longas
    const dica = rotulos.length > 40 ? rotulos.map((r, i) => `<rect x="${sx(i) - 3}" y="${m.t}" width="6" height="${A - m.b - m.t}" fill="transparent"><title>${escapeHtml(r)}: ${series.map(s => `${escapeHtml(s.nome)} ${s.valores[i] == null ? '–' : escapeHtml(formato(s.valores[i]))}`).join(' · ')}</title></rect>`).join('') : '';
    const legenda = series.length > 1 ? `<div class="legenda" aria-hidden="true">${series.map(s => `<span><i style="background:${COR[s.cor]}"></i>${escapeHtml(s.nome)}</span>`).join('')}</div>` : '';
    const tabela = `<table class="visualmente-oculto"><caption>${escapeHtml(titulo)}</caption><tr><th>Período</th>${series.map(s => `<th>${escapeHtml(s.nome)}</th>`).join('')}</tr>`
        + rotulos.map((r, i) => `<tr><td>${escapeHtml(r)}</td>${series.map(s => `<td>${s.valores[i] == null ? '' : escapeHtml(formato(s.valores[i]))}</td>`).join('')}</tr>`).join('') + '</table>';
    return raw(`${legenda}<svg class="grafico-svg" viewBox="0 0 ${L} ${A}" role="img" aria-label="${escapeHtml(titulo)}" preserveAspectRatio="xMidYMid meet">${sombra}${grade}${eixoX}${marc}${caminhos}${dica}</svg>${tabela}`);
}

/** Barras horizontais com valor formatado. itens: [{ rotulo, valor, destaque?, nota? }]. */
export function barras({ itens, formato = fmt.num, max = null, titulo = '' }) {
    const topo = max ?? Math.max(1, ...itens.map(i => i.valor));
    return raw(`<div class="barras-h2" role="img" aria-label="${escapeHtml(titulo)}">${itens.map(i => `
        <div class="barra-h2" title="${escapeHtml(i.rotulo)}: ${escapeHtml(formato(i.valor))}">
            <span class="barra-h2__rotulo">${escapeHtml(i.rotulo)}</span>
            <div class="barra-h__trilho"><div class="barra-h__valor ${i.destaque ? 'barra-h__valor--destaque' : ''}" style="width:${Math.max(1, Math.round(i.valor * 100 / topo))}%"></div></div>
            <strong>${escapeHtml(formato(i.valor))}</strong>
            ${i.nota ? `<small class="barra-h2__nota">${escapeHtml(i.nota)}</small>` : ''}
        </div>`).join('')}</div>`);
}

/** Miniatura de tendência. */
export function minilinha(valores, cor = 'unica') {
    const L = 120, A = 34;
    if (valores.length < 2) return raw('');
    const sx = escala(0, valores.length - 1, 2, L - 2), sy = escala(Math.min(...valores), Math.max(...valores), A - 3, 3);
    const d = valores.map((v, i) => `${i ? 'L' : 'M'}${sx(i).toFixed(1)},${sy(v).toFixed(1)}`).join(' ');
    return raw(`<svg viewBox="0 0 ${L} ${A}" class="minilinha" aria-hidden="true"><path d="${d}" fill="none" stroke="${COR[cor]}" stroke-width="2"/></svg>`);
}

/**
 * Antes e depois, uma linha por indicador: duas barras (antes em cinza, depois em destaque) com o valor ao lado.
 * itens: [{ rotulo, antes, depois, unidade: '%'|'h', melhorSeMenor }]
 */
export function antesDepois({ itens, rotuloAntes, rotuloDepois, titulo = '' }) {
    const un = (v, u) => (u === 'h' ? `${fmtNum.format(v)} h` : u === 'n' ? fmtNum.format(v) : fmt.pct(v));
    const linha = i => {
        const topo = Math.max(i.antes, i.depois) || 1;
        const melhorou = i.melhorSeMenor ? i.depois < i.antes : i.depois > i.antes;
        const seta = i.depois === i.antes ? '=' : (i.depois > i.antes ? '▲' : '▼');
        return `<div class="ad-linha" title="${escapeHtml(i.rotulo)}: ${escapeHtml(un(i.antes, i.unidade))} antes, ${escapeHtml(un(i.depois, i.unidade))} depois">
            <strong class="ad-linha__nome">${escapeHtml(i.rotulo)}</strong>
            <div class="ad-linha__barras">
                <div class="ad-barra"><span>Antes</span><div class="barra-h__trilho"><div class="barra-h__valor ad-antes" style="width:${Math.max(2, Math.round(i.antes * 100 / topo))}%"></div></div><b>${escapeHtml(un(i.antes, i.unidade))}</b></div>
                <div class="ad-barra"><span>Depois</span><div class="barra-h__trilho"><div class="barra-h__valor ad-depois" style="width:${Math.max(2, Math.round(i.depois * 100 / topo))}%"></div></div><b>${escapeHtml(un(i.depois, i.unidade))}</b></div>
            </div>
            <span class="ad-selo ${melhorou ? 'ad-selo--bom' : 'ad-selo--ruim'}">${seta} ${melhorou ? 'melhorou' : 'piorou'}</span>
        </div>`;
    };
    return raw(`<div class="antes-depois" role="img" aria-label="${escapeHtml(titulo)}"><p class="antes-depois__legenda"><span><i class="ad-antes"></i>${escapeHtml(rotuloAntes)}</span><span><i class="ad-depois"></i>${escapeHtml(rotuloDepois)}</span></p>${itens.map(linha).join('')}</div>`);
}
