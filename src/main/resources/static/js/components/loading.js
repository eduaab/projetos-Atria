import { html, render } from '../core/dom.js';
import { toast } from '../core/toast.js';
import { icone } from './icons.js';

export const carregando = (linhas = 3) => html`
    <div class="esqueleto" aria-busy="true" aria-label="Carregando">
        ${Array.from({ length: linhas }, () => html`<div class="esqueleto__linha"></div>`)}
    </div>`;

export const estadoErro = mensagem => html`
    <div class="estado-erro" role="alert">
        <span>${mensagem}</span>
        <button class="botao botao--pequeno" data-acao="recarregar">${icone('atualizar')} Tentar novamente</button>
    </div>`;

/**
 * Busca e desenha com os três estados: carregando (esqueleto), erro (mensagem + tentar de novo + toast)
 * e sucesso (chama desenhar). Devolve os dados ou null em caso de erro.
 */
export async function comEstados(alvo, buscar, desenhar, linhas = 3) {
    render(alvo, carregando(linhas));
    try {
        const dados = await buscar();
        desenhar(dados);
        return dados;
    } catch (erro) {
        render(alvo, estadoErro(erro.message));
        alvo.querySelector('[data-acao="recarregar"]')
            .addEventListener('click', () => comEstados(alvo, buscar, desenhar, linhas));
        toast(erro.message, 'erro');
        return null;
    }
}
