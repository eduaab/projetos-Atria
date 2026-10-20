import { $, $$, comBotaoOcupado, esperar, html, on, render } from '../core/dom.js';
import { data, dataHora, moeda } from '../core/format.js';
import { navegar } from '../core/router.js';
import { toast } from '../core/toast.js';
import {
    buscarTitulares, excluirTitular, obterPreviaExclusao, obterTitular,
} from '../services/titularService.js';
import { alterarStatusSolicitacao, listarSolicitacoes, registrarSolicitacao } from '../services/solicitacaoService.js';
import { baixarComprovante } from '../services/relatorioService.js';
import { linhaCliente } from '../components/clienteRow.js';
import { estadoVazio } from '../components/emptyState.js';
import { icone } from '../components/icons.js';
import { comEstados } from '../components/loading.js';
import { abrirModal } from '../components/modal.js';
import { linhaPlataformaTitular } from '../components/platformRow.js';
import { badgeStatus, indicadorPrazo } from '../components/statusBadge.js';

const TIPOS = [['EXCLUSAO', 'Exclusão de dados'], ['ACESSO', 'Acesso aos dados'], ['CORRECAO', 'Correção de dados'], ['PORTABILIDADE', 'Portabilidade']];
const CANAIS = [['WHATSAPP', 'WhatsApp'], ['INSTAGRAM', 'Instagram'], ['EMAIL', 'E-mail'], ['MARKETPLACE', 'Marketplace'], ['TELEFONE', 'Telefone'], ['PAINEL', 'Painel Atria']];
const STATUS = [['PENDENTE', 'Pendente'], ['EM_ANDAMENTO', 'Em andamento'], ['ATRASADA', 'Atrasada'], ['CONCLUIDA', 'Concluída']];

const opcoes = (lista, rotuloTodos) => html`
    ${rotuloTodos ? html`<option value="">${rotuloTodos}</option>` : ''}
    ${lista.map(([valor, rotulo]) => html`<option value="${valor}">${rotulo}</option>`)}`;

export default {
    mount(el, parametros) {
        if (parametros.id) montarPerfil(el, parametros.id);
        else montarLista(el);
    },
};

/* ---------------- Lista: busca de titulares + solicitações ---------------- */

function montarLista(el) {
    render(el, html`
        <header class="cabecalho-pagina"><div><h1>Central de clientes</h1>
            <p>Encontre um cliente por nome, CPF, e-mail ou telefone e veja onde os dados dele aparecem.</p></div></header>
        <form class="barra-filtros" id="clientes-form" role="search">
            <label class="campo-busca">${icone('busca')}<span class="visualmente-oculto">Buscar titular</span>
                <input type="search" id="clientes-busca" placeholder="Buscar por nome, CPF, e-mail ou telefone..." autocomplete="off"></label>
            <button class="botao botao--primario" type="submit">Buscar</button>
        </form>
        <section class="painel">
            <div class="painel__cabecalho"><h2>Lista de clientes</h2><span id="clientes-total" class="badge badge--neutro badge--sem-ponto"></span></div>
            <div id="clientes-lista" class="lista-linhas"></div>
        </section>
        <section class="painel">
            <div class="painel__cabecalho"><h2>Solicitações dos titulares</h2>
                <div class="barra-filtros">
                    <select class="seletor" id="filtro-status" aria-label="Filtrar por status">${opcoes(STATUS, 'Todos os status')}</select>
                    <select class="seletor" id="filtro-tipo" aria-label="Filtrar por tipo">${opcoes(TIPOS, 'Todos os tipos')}</select>
                    <select class="seletor" id="filtro-canal" aria-label="Filtrar por canal">${opcoes(CANAIS, 'Todos os canais')}</select>
                </div>
            </div>
            <div id="clientes-solicitacoes"></div>
        </section>`);

    const carregarClientes = () => {
        const busca = $('#clientes-busca', el).value.trim();
        return comEstados($('#clientes-lista', el), () => buscarTitulares(busca), lista => {
            $('#clientes-total', el).textContent = `${lista.length} encontrado(s)`;
            render($('#clientes-lista', el), lista.length ? html`${lista.map(linhaCliente)}`
                : estadoVazio('Nenhum titular encontrado. Titulares anonimizados não aparecem na busca.', 'busca'));
        });
    };

    const carregarSolicitacoes = () => comEstados($('#clientes-solicitacoes', el), () => listarSolicitacoes({
        status: $('#filtro-status', el).value, tipo: $('#filtro-tipo', el).value, canal: $('#filtro-canal', el).value,
    }), lista => render($('#clientes-solicitacoes', el), lista.length ? tabelaSolicitacoes(lista) : estadoVazio('Nenhuma solicitação com esses filtros.')));

    $('#clientes-form', el).addEventListener('submit', e => {
        e.preventDefault();
        carregarClientes();
    });
    on(el, 'change', 'select.seletor[id^="filtro-"]', carregarSolicitacoes);
    on(el, 'change', '[data-acao="status-solicitacao"]', async (e, select) => {
        if (!select.value) return;
        select.disabled = true;
        try {
            await alterarStatusSolicitacao(select.dataset.id, select.value);
            toast('Status da solicitação atualizado.', 'sucesso');
        } catch (erro) {
            toast(erro.message, 'erro');
        }
        carregarSolicitacoes();
    });
    on(el, 'click', '[data-acao="ver-titular"]', (e, alvo) => navegar(`clientes/${alvo.dataset.id}`));

    carregarClientes();
    carregarSolicitacoes();
}

function tabelaSolicitacoes(lista) {
    return html`
        <div class="tabela-rolagem"><table class="tabela">
            <thead><tr><th>Protocolo</th><th>Titular</th><th>Tipo</th><th>Canal</th><th>Aberta em</th><th>Prazo</th><th>Status</th><th>Ação</th></tr></thead>
            <tbody>${lista.map(s => html`<tr>
                <td class="codigo">${s.protocolo}</td>
                <td><button class="botao botao--texto" data-acao="ver-titular" data-id="${s.titularId}">${s.titularNome}</button></td>
                <td>${s.tipoDescricao}</td><td>${s.canalDescricao}</td><td>${data(s.abertaEm)}</td>
                <td>${indicadorPrazo(s.nivelPrazo, s.diasRestantes, s.status === 'CONCLUIDA')}</td>
                <td>${badgeStatus(s.status, s.statusDescricao)}</td>
                <td>${acaoSolicitacao(s)}</td></tr>`)}</tbody>
        </table></div>`;
}

function acaoSolicitacao(s) {
    if (s.status === 'CONCLUIDA') return html`<span class="badge badge--sucesso badge--sem-ponto">Atendida</span>`;
    if (s.tipo === 'EXCLUSAO') {
        return html`<button class="botao botao--pequeno" data-acao="ver-titular" data-id="${s.titularId}">Abrir exclusão</button>`;
    }
    const proximos = s.status === 'ATRASADA' ? [['CONCLUIDA', 'Concluir']]
        : [['PENDENTE', 'Pendente'], ['EM_ANDAMENTO', 'Em andamento'], ['CONCLUIDA', 'Concluir']].filter(([v]) => v !== s.status);
    return html`<select class="seletor" data-acao="status-solicitacao" data-id="${s.id}" aria-label="Alterar status de ${s.protocolo}">
        <option value="">Alterar...</option>${opcoes(proximos)}</select>`;
}

/* ---------------- Perfil do cliente + exclusão ---------------- */

function montarPerfil(el, id) {
    render(el, html`
        <a href="#/clientes" class="link-voltar">${icone('voltar')} Voltar para Clientes</a>
        <h1>Perfil do Cliente</h1>
        <div id="perfil-resultado"></div>
        <div id="perfil" class="pagina"></div>`);

    const carregar = () => comEstados($('#perfil', el), () => obterTitular(id), titular => desenharPerfil(el, titular), 4);

    on(el, 'click', '[data-acao]', (e, alvo) => {
        switch (alvo.dataset.acao) {
            case 'excluir': return iniciarExclusao(el, id, carregar);
            case 'nova-solicitacao': return abrirNovaSolicitacao(id, carregar);
            case 'baixar-comprovante':
                return comBotaoOcupado(alvo, 'Gerando...', () => baixarComprovante(alvo.dataset.protocolo))
                    .then(() => toast('Comprovante baixado.', 'sucesso'))
                    .catch(erro => toast(erro.message, 'erro'));
            default: return undefined;
        }
    });
    carregar();
}

function desenharPerfil(el, t) {
    render($('#perfil', el), html`
        <section class="banner-perfil">
            <span class="avatar avatar--grande" aria-hidden="true"></span>
            <div class="banner-perfil__dados">
                <strong>${t.nome}</strong>
                <span>Telefone: ${t.telefone || '—'}</span>
                <span>E-mail: ${t.email || '—'}</span>
                <span>CPF: ${t.cpf || '—'}</span>
                <span>CEP: ${t.cep || '—'}</span>
                <strong>Gastos Totais: ${moeda(t.gastosTotais)}</strong>
            </div>
        </section>
        ${t.anonimizado ? html`<p class="caixa-aviso">Os dados pessoais deste titular foram excluídos em ${dataHora(t.anonimizadoEm)}.
            O histórico abaixo é mantido, sem dados pessoais, como comprovante do atendimento.</p>` : ''}
        <div class="grade-detalhe">
            <section class="painel">
                <h2>Dados Sensíveis do Cliente</h2>
                <p class="painel__subtitulo">Foram detectados dados nas seguintes plataformas:</p>
                <div class="lista-linhas" id="perfil-plataformas">
                    ${t.plataformas.length ? t.plataformas.map(linhaPlataformaTitular)
                        : estadoVazio(t.anonimizado ? 'Os vínculos com plataformas foram removidos.' : 'Nenhuma plataforma vinculada.')}
                </div>
            </section>
            <section class="painel">
                <h2>Histórico de Interação com o Cliente</h2>
                <div class="rolagem">
                    ${t.interacoes.length ? t.interacoes.map(i => html`
                        <div class="cartao-interacao ${i.destaque ? 'cartao-interacao--destaque' : ''}">
                            <div><strong>${i.descricao}</strong>${i.valor ? html` <span>· ${moeda(i.valor)}</span>` : ''}<br><span>${i.plataforma || 'Canal externo'}</span></div>
                            <span>Data: ${data(i.data)}</span>
                        </div>`) : estadoVazio('Sem interações registradas.')}
                </div>
            </section>
        </div>
        <section class="painel">
            <div class="painel__cabecalho"><h2>Solicitações LGPD</h2>
                ${t.anonimizado ? '' : html`<button class="botao botao--pequeno" data-acao="nova-solicitacao">${icone('mais')} Registrar solicitação</button>`}</div>
            ${t.solicitacoes.length ? html`<div class="tabela-rolagem"><table class="tabela">
                <thead><tr><th>Protocolo</th><th>Tipo</th><th>Canal</th><th>Aberta em</th><th>Prazo</th><th>Status</th><th></th></tr></thead>
                <tbody>${t.solicitacoes.map(s => html`<tr><td class="codigo">${s.protocolo}</td><td>${s.tipoDescricao}</td><td>${s.canalDescricao}</td>
                    <td>${data(s.abertaEm)}</td><td>${indicadorPrazo(s.nivelPrazo, s.diasRestantes, s.status === 'CONCLUIDA')}</td>
                    <td>${badgeStatus(s.status, s.statusDescricao)}</td>
                    <td>${s.status === 'CONCLUIDA' ? html`<button class="botao botao--pequeno" data-acao="baixar-comprovante" data-protocolo="${s.protocolo}">${icone('download')} Comprovante</button>` : ''}</td></tr>`)}</tbody>
            </table></div>` : estadoVazio('Nenhuma solicitação registrada.')}
        </section>
        ${t.anonimizado ? '' : html`<button class="botao botao--perigo botao--largo" data-acao="excluir">${icone('lixeira')} Deletar todos os dados</button>`}`);
}

async function iniciarExclusao(el, id, recarregar) {
    let previa;
    try {
        previa = await obterPreviaExclusao(id);
    } catch (erro) {
        return toast(erro.message, 'erro');
    }
    abrirModal({
        titulo: `Excluir os dados de ${previa.titularNome}`,
        corpo: html`
            <p>A Atria apaga apenas o que ela própria armazena. Nada é alterado nas plataformas de origem.</p>
            <div class="grade-2">
                <div><h3>Será apagado</h3><ul class="lista-check lista-check--apagar">
                    ${previa.dadosApagados.map(c => html`<li>${icone('x')}<span><strong>${c.campo}:</strong> ${c.valor || '—'}</span></li>`)}
                    <li>${icone('x')}<span>Vínculos com ${previa.plataformas.length} plataforma(s)</span></li></ul></div>
                <div><h3>Será preservado</h3><ul class="lista-check lista-check--manter">
                    ${previa.dadosPreservados.map(item => html`<li>${icone('check')}<span>${item}</span></li>`)}</ul></div>
            </div>
            <label class="confirmacao"><input type="checkbox" id="confirmar-exclusao">
                Confirmo que o titular pediu a exclusão e entendo que esta ação não pode ser desfeita.</label>`,
        acoes: [
            { id: 'cancelar', rotulo: 'Cancelar' },
            { id: 'confirmar', rotulo: 'Excluir dados', classe: 'botao--perigo' },
        ],
        aoAbrir(modal) {
            const botao = modal.botao('confirmar');
            botao.disabled = true;
            modal.corpo.querySelector('#confirmar-exclusao').addEventListener('change', e => { botao.disabled = !e.target.checked; });
        },
        aoAgir(acao, modal) {
            modal.fechar();
            executarExclusao(el, id, recarregar);
        },
    });
}

/** Feedback visual por plataforma: cada vínculo é marcado como removido em sequência. */
async function executarExclusao(el, id, recarregar) {
    const botao = $('[data-acao="excluir"]', el);
    botao.disabled = true;
    render(botao, html`<span class="girando" aria-hidden="true"></span> Excluindo dados...`);
    const linhas = $$('[data-status-remocao]', el);
    for (const status of linhas) {
        status.className = 'status-remocao status-remocao--andamento';
        render(status, html`<span class="girando" aria-hidden="true"></span> Removendo vínculo...`);
        await esperar(280);
    }
    try {
        const resultado = await excluirTitular(id);
        linhas.forEach(status => {
            status.className = 'status-remocao status-remocao--feito';
            render(status, html`${icone('check')} Vínculo removido`);
        });
        await esperar(700);
        toast(`Dados excluídos. Protocolo ${resultado.protocolo}.`, 'sucesso');
        render($('#perfil-resultado', el), html`
            <section class="caixa-sucesso" role="status">
                <span>${icone('check')}</span>
                <strong>Exclusão concluída. Protocolo ${resultado.protocolo}</strong>
                <span>Nome, CPF, e-mail, telefone e CEP foram apagados da Atria e o registro foi anonimizado.
                    Plataformas desvinculadas: ${resultado.plataformas.join(', ') || 'nenhuma'}.</span>
                <span class="codigo">Hash do comprovante: ${resultado.hashComprovante}</span>
                <div class="linha-acoes">
                    <button class="botao botao--sucesso" data-acao="baixar-comprovante" data-protocolo="${resultado.protocolo}">${icone('download')} Baixar comprovante (PDF)</button>
                    <a class="botao" href="#/relatorios">Ver solicitações atendidas</a>
                </div>
            </section>`);
        recarregar();
    } catch (erro) {
        toast(erro.message, 'erro');
        recarregar();
    }
}

function abrirNovaSolicitacao(titularId, recarregar) {
    abrirModal({
        titulo: 'Registrar solicitação do titular',
        corpo: html`
            <label class="rotulo-campo">Tipo<select class="seletor" id="nova-tipo">${opcoes(TIPOS)}</select></label>
            <label class="rotulo-campo">Canal de origem<select class="seletor" id="nova-canal">${opcoes(CANAIS)}</select></label>
            <p class="caixa-aviso">O prazo de atendimento é de 15 dias a partir de hoje. Um protocolo é gerado automaticamente.</p>`,
        acoes: [{ id: 'cancelar', rotulo: 'Cancelar' }, { id: 'salvar', rotulo: 'Registrar', classe: 'botao--primario' }],
        async aoAgir(acao, modal) {
            modal.carregando(true);
            try {
                const criada = await registrarSolicitacao({
                    titularId: Number(titularId),
                    tipo: modal.corpo.querySelector('#nova-tipo').value,
                    canalOrigem: modal.corpo.querySelector('#nova-canal').value,
                });
                modal.fechar();
                toast(`Solicitação registrada. Protocolo ${criada.protocolo}.`, 'sucesso');
                recarregar();
            } catch (erro) {
                modal.carregando(false);
                toast(erro.message, 'erro');
            }
        },
    });
}
