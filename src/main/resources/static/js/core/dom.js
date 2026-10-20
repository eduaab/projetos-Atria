const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '`': '&#96;' };

/** HTML já seguro (produzido pela tag html ou por constantes do código). */
class HtmlSeguro {
    constructor(texto) {
        this.texto = texto;
    }

    toString() {
        return this.texto;
    }
}

export function escapeHtml(valor) {
    if (valor === null || valor === undefined) return '';
    return String(valor).replace(/[&<>"'`]/g, caractere => ESCAPES[caractere]);
}

/** Marca um trecho como HTML confiável. Use só com texto fixo do próprio código, nunca com dados. */
export function raw(texto) {
    return new HtmlSeguro(texto);
}

function interpolar(valor) {
    if (valor instanceof HtmlSeguro) return valor.texto;
    if (Array.isArray(valor)) return valor.map(interpolar).join('');
    if (valor === null || valor === undefined || valor === false) return '';
    return escapeHtml(valor);
}

/** Template tag que escapa todo valor interpolado: html`<p>${nome}</p>`. */
export function html(partes, ...valores) {
    return new HtmlSeguro(partes.reduce((acumulado, parte, i) =>
        acumulado + parte + (i < valores.length ? interpolar(valores[i]) : ''), ''));
}

export function render(elemento, conteudo) {
    if (!(conteudo instanceof HtmlSeguro)) throw new Error('render() aceita apenas conteúdo criado com html``');
    elemento.innerHTML = conteudo.texto;
}

export const $ = (seletor, raiz = document) => raiz.querySelector(seletor);
export const $$ = (seletor, raiz = document) => [...raiz.querySelectorAll(seletor)];

/** Delegação de eventos: um listener na raiz trata os cliques de todos os filhos que casam com o seletor. */
export function on(raiz, evento, seletor, tratador) {
    raiz.addEventListener(evento, e => {
        const alvo = e.target.closest(seletor);
        if (alvo && raiz.contains(alvo)) tratador(e, alvo);
    });
}

/** Desabilita o botão e troca o texto enquanto a ação roda. */
export async function comBotaoOcupado(botao, textoOcupado, acao) {
    const original = botao.innerHTML;
    botao.disabled = true;
    render(botao, html`<span class="girando" aria-hidden="true"></span> ${textoOcupado}`);
    try {
        return await acao();
    } finally {
        if (botao.isConnected) {
            botao.disabled = false;
            botao.innerHTML = original;
        }
    }
}

export const esperar = ms => new Promise(resolve => setTimeout(resolve, ms));

export function debounce(funcao, ms) {
    let temporizador;
    return (...args) => {
        clearTimeout(temporizador);
        temporizador = setTimeout(() => funcao(...args), ms);
    };
}
