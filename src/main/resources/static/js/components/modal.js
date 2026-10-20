import { $, html, render } from '../core/dom.js';
import { icone } from './icons.js';

/**
 * Abre um modal. acoes: [{ id, rotulo, classe }]. A ação "cancelar" fecha sozinha;
 * as demais chamam aoAgir(id, modal). O modal expõe corpo, fechar() e carregando(bool).
 */
export function abrirModal({ titulo, corpo, acoes = [], aoAgir = () => {}, aoAbrir = () => {} }) {
    const fundo = document.createElement('div');
    fundo.className = 'modal-fundo';
    render(fundo, html`
        <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-titulo">
            <header class="modal__cabecalho">
                <h2 id="modal-titulo">${titulo}</h2>
                <button class="botao botao--icone botao--texto" data-modal-acao="cancelar" aria-label="Fechar">${icone('x')}</button>
            </header>
            <div class="modal__corpo">${corpo}</div>
            <footer class="modal__acoes">
                ${acoes.map(a => html`<button class="botao ${a.classe || ''}" data-modal-acao="${a.id}">${a.rotulo}</button>`)}
            </footer>
        </div>`);

    const focoAnterior = document.activeElement;
    const janela = $('.modal', fundo);
    const modal = {
        corpo: $('.modal__corpo', fundo),
        botao: id => $(`[data-modal-acao="${id}"]`, fundo),
        carregando(ativo) {
            janela.classList.toggle('modal--carregando', ativo);
        },
        fechar() {
            fundo.remove();
            document.removeEventListener('keydown', aoTeclar);
            focoAnterior?.focus?.();
        },
    };
    const aoTeclar = e => {
        if (e.key === 'Escape' && !janela.classList.contains('modal--carregando')) modal.fechar();
    };

    fundo.addEventListener('click', e => {
        if (e.target === fundo && !janela.classList.contains('modal--carregando')) return modal.fechar();
        const botao = e.target.closest('[data-modal-acao]');
        if (!botao || botao.disabled) return;
        if (botao.dataset.modalAcao === 'cancelar') return modal.fechar();
        aoAgir(botao.dataset.modalAcao, modal);
    });
    document.addEventListener('keydown', aoTeclar);
    document.getElementById('modais').appendChild(fundo);
    (janela.querySelector('input, select, [data-modal-acao]:not([data-modal-acao="cancelar"])') || janela).focus();
    aoAbrir(modal);
    return modal;
}
