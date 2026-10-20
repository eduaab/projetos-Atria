import { $, esperar, html, on, render } from '../core/dom.js';
import { data, dataHora } from '../core/format.js';
import { conexoesSalvas, salvarConexoes } from '../core/state.js';
import { toast } from '../core/toast.js';
import {
    conectar, desconectar, listarConexoes, obterConexao,
} from '../services/conexaoService.js';
import { revogarAcesso } from '../services/alertaService.js';
import { barrasHorizontais } from '../components/chart.js';
import { estadoVazio } from '../components/emptyState.js';
import { icone, iconePlataforma } from '../components/icons.js';
import { comEstados } from '../components/loading.js';
import { abrirModal } from '../components/modal.js';
import { linhaConexao } from '../components/platformRow.js';
import { cartaoEstatistica } from '../components/statCard.js';
import { badgeStatus } from '../components/statusBadge.js';
import { abasConexoes } from '../components/abasConexoes.js';

const PASSOS = [
    { icone: 'escudo', titulo: 'Bem-vinda à Atria', texto: 'A Atria reúne as pendências de todas as suas plataformas e avisa o que precisa de atenção: clientes sem resposta, reclamações, pedidos parados e prazos da LGPD.' },
    { icone: 'conexao', titulo: 'Conecte suas plataformas', texto: 'Autorize a Atria a ler os dados do WhatsApp, Instagram, da sua loja e dos marketplaces. Você escolhe o que liberar e pode desconectar quando quiser.' },
    { icone: 'busca', titulo: 'Saiba onde estão os dados dos clientes', texto: 'Com as plataformas conectadas, a Atria mapeia os dados de cada cliente e ajuda você a atender pedidos de acesso e exclusão dentro do prazo.' },
];

export default {
    mount(el, parametros) {
        if (parametros.slug) return montarDetalhe(el, parametros.slug);
        if (conexoesSalvas().length === 0) return montarOnboarding(el, 0);
        return montarHub(el);
    },
};

/* ---------------- Onboarding (primeiro acesso) ---------------- */

function montarOnboarding(el, indice) {
    const passo = PASSOS[indice];
    const ultimo = indice === PASSOS.length - 1;
    render(el, html`
        <section class="onboarding" aria-live="polite">
            <div class="onboarding__passos" aria-label="Passo ${indice + 1} de ${PASSOS.length}">
                ${PASSOS.map((p, i) => html`<i class="${i <= indice ? 'ativo' : ''}"></i>`)}</div>
            <div class="onboarding__icone">${icone(passo.icone)}</div>
            <h1>${passo.titulo}</h1>
            <p>${passo.texto}</p>
            <div class="linha-acoes">
                ${indice > 0 ? html`<button class="botao" data-passo="${indice - 1}">Voltar</button>` : ''}
                ${ultimo ? html`<button class="botao botao--primario" data-acao="concluir">Ir para o Hub de Conexões</button>`
                    : html`<button class="botao botao--primario" data-passo="${indice + 1}">Próximo</button>`}
            </div>
            ${ultimo ? '' : html`<button class="botao botao--texto" data-acao="concluir">Pular introdução</button>`}
        </section>`);
    el.onclick = e => {
        const passoAlvo = e.target.closest('[data-passo]');
        if (passoAlvo) montarOnboarding(el, Number(passoAlvo.dataset.passo));
        if (e.target.closest('[data-acao="concluir"]')) {
            el.onclick = null;
            montarHub(el, true);
        }
    };
}

/* ---------------- Hub de Conexões ---------------- */

function montarHub(el, vindoDoOnboarding = false) {
    let conexoes = [];
    render(el, html`
        ${abasConexoes('plataformas')}
        <header class="cabecalho-pagina"><div><h1>Hub de Conexões</h1><p>Plataformas conectadas ao seu comércio</p></div></header>
        ${vindoDoOnboarding ? html`<p class="caixa-aviso">Escolha uma plataforma e clique em <strong>Conectar</strong> para autorizar o acesso da Atria.</p>` : ''}
        <section class="painel"><div id="hub-lista" class="lista-linhas"></div></section>`);

    const carregar = () => comEstados($('#hub-lista', el), listarConexoes, lista => {
        conexoes = lista;
        salvarConexoes(lista.filter(c => c.conectada).map(c => c.plataforma.slug).concat(vindoDoOnboarding ? ['onboarding'] : []));
        render($('#hub-lista', el), html`${lista.map(linhaConexao)}`);
    }, 5);

    on(el, 'click', '[data-acao]', (e, alvo) => {
        const conexao = conexoes.find(c => c.plataforma.slug === alvo.dataset.slug);
        if (!conexao) return;
        if (alvo.dataset.acao === 'conectar') abrirAutorizacao(conexao.plataforma, carregar);
        if (alvo.dataset.acao === 'desconectar') confirmarDesconexao(conexao.plataforma, carregar);
    });
    carregar();
}

/** Autorização simulada no estilo OAuth: mostra os escopos e conecta após um carregamento curto. */
export function abrirAutorizacao(plataforma, aoConectar) {
    abrirModal({
        titulo: `Conectar ${plataforma.nome}`,
        corpo: html`
            <div class="oauth__topo">
                <span class="oauth__marca"><img src="/img/Logo.png" alt="Atria"></span>
                ${icone('conexao')}
                <span class="icone-plataforma" style="width:56px;height:56px">${iconePlataforma(plataforma.slug)}</span>
            </div>
            <p><strong>A Atria quer acessar sua conta ${plataforma.nome}:</strong></p>
            <ul class="lista-check lista-check--manter">${plataforma.escopos.map(e => html`<li>${icone('check')}<span>${e}</span></li>`)}</ul>
            <p class="caixa-aviso">A Atria apenas lê esses dados para identificar pendências e mapear os dados dos clientes. Nada é alterado em ${plataforma.nome}, e você pode desconectar quando quiser.</p>
            <p id="oauth-status" hidden><span class="girando" aria-hidden="true"></span> Autorizando com ${plataforma.nome}...</p>`,
        acoes: [{ id: 'cancelar', rotulo: 'Cancelar' }, { id: 'autorizar', rotulo: 'Autorizar', classe: 'botao--primario' }],
        async aoAgir(acao, modal) {
            modal.carregando(true);
            modal.corpo.querySelector('#oauth-status').hidden = false;
            try {
                await esperar(900);
                await conectar(plataforma.slug);
                modal.fechar();
                toast(`${plataforma.nome} conectada. Execute uma varredura para buscar novos alertas.`, 'sucesso');
                aoConectar();
            } catch (erro) {
                modal.carregando(false);
                modal.corpo.querySelector('#oauth-status').hidden = true;
                toast(erro.message, 'erro');
            }
        },
    });
}

function confirmarDesconexao(plataforma, aoDesconectar) {
    abrirModal({
        titulo: `Desconectar ${plataforma.nome}?`,
        corpo: html`<p>A Atria deixa de monitorar ${plataforma.nome}. Os dados já identificados não são apagados, mas deixam de contar no mapeamento até você conectar de novo.</p>`,
        acoes: [{ id: 'cancelar', rotulo: 'Cancelar' }, { id: 'desconectar', rotulo: 'Desconectar', classe: 'botao--perigo' }],
        async aoAgir(acao, modal) {
            modal.carregando(true);
            try {
                await desconectar(plataforma.slug);
                modal.fechar();
                toast(`${plataforma.nome} desconectada.`, 'sucesso');
                aoDesconectar();
            } catch (erro) {
                modal.carregando(false);
                toast(erro.message, 'erro');
            }
        },
    });
}

/* ---------------- Detalhe da integração ---------------- */

function montarDetalhe(el, slug) {
    render(el, html`<a href="#/conexoes" class="link-voltar">${icone('voltar')} Voltar para o Hub</a><div id="integracao" class="pagina"></div>`);
    let detalhe;
    const carregar = () => comEstados($('#integracao', el), () => obterConexao(slug), dados => {
        detalhe = dados;
        desenharDetalhe(el, dados);
    }, 4);

    on(el, 'click', '[data-acao]', (e, alvo) => {
        const plataforma = detalhe?.conexao.plataforma;
        if (alvo.dataset.acao === 'conectar') abrirAutorizacao(plataforma, carregar);
        if (alvo.dataset.acao === 'desconectar') confirmarDesconexao(plataforma, carregar);
        if (alvo.dataset.acao === 'revogar') {
            alvo.disabled = true;
            revogarAcesso(alvo.dataset.id)
                .then(acesso => { toast(`Acesso de ${acesso.usuarioNome} revogado.`, 'sucesso'); carregar(); })
                .catch(erro => { alvo.disabled = false; toast(erro.message, 'erro'); });
        }
    });
    carregar();
}

function desenharDetalhe(el, d) {
    const c = d.conexao;
    const p = c.plataforma;
    render($('#integracao', el), html`
        <header class="cabecalho-pagina">
            <div class="linha-branca__principal"><span class="icone-plataforma">${iconePlataforma(p.slug)}</span>
                <div><h1>${p.nome}</h1><p>${p.tipoDescricao}</p></div></div>
            ${c.conectada ? html`<button class="botao" data-acao="desconectar">Desconectar</button>`
                : html`<button class="botao botao--primario" data-acao="conectar">${icone('entrar')} Conectar</button>`}
        </header>
        <div class="grade-info">
            <div class="info"><span>Status</span><strong>${c.conectada ? badgeStatus('ATIVA', 'Conectada') : badgeStatus('REVOGADA', c.status ? 'Desconectada' : 'Não conectada')}</strong></div>
            <div class="info"><span>Conectada em</span><strong>${dataHora(c.conectadaEm)}</strong></div>
            <div class="info"><span>Última sincronização</span><strong>${dataHora(c.ultimaSincronizacao)}</strong></div>
            ${c.desconectadaEm ? html`<div class="info"><span>Desconectada em</span><strong>${dataHora(c.desconectadaEm)}</strong></div>` : ''}
        </div>
        <div class="grade-2">
            <section class="painel">
                <h2>Dados identificados por tipo</h2>
                ${c.conectada ? html`<div class="grade-estatisticas">${cartaoEstatistica('Clientes identificados', d.clientes, 'sucesso')}</div>${barrasHorizontais(d.dadosPorTipo)}`
                    : estadoVazio('Conecte a plataforma para a Atria identificar os dados dos clientes.', 'cadeado')}
            </section>
            <section class="painel">
                <h2>Permissões solicitadas</h2>
                <ul class="lista-check lista-check--manter">${p.escopos.map(e => html`<li>${icone('check')}<span>${e}</span></li>`)}</ul>
            </section>
        </div>
        <section class="painel">
            <h2>Acessos da equipe</h2>
            ${d.acessos.length ? html`<div class="tabela-rolagem"><table class="tabela">
                <thead><tr><th>Usuário</th><th>Papel</th><th>Concedido em</th><th>Situação</th><th></th></tr></thead>
                <tbody>${d.acessos.map(a => html`<tr>
                    <td>${a.usuarioNome} ${a.usuarioDesligado ? html`<span class="badge badge--alta">Desligado(a)</span>` : ''}</td>
                    <td>${a.papel}</td><td>${data(a.concedidoEm)}</td>
                    <td>${a.revogadoEm ? `Revogado em ${data(a.revogadoEm)}` : 'Ativo'}</td>
                    <td>${a.revogadoEm ? '' : html`<button class="botao botao--pequeno ${a.usuarioDesligado ? 'botao--perigo' : ''}" data-acao="revogar" data-id="${a.id}">${icone('cadeado')} Revogar</button>`}</td>
                </tr>`)}</tbody></table></div>` : estadoVazio('Nenhum acesso concedido nesta plataforma.')}
        </section>`);
}
