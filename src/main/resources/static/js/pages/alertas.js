import { $, comBotaoOcupado, html, on, render } from '../core/dom.js';
import { dataHora, plural, textoPrazo } from '../core/format.js';
import { navegar } from '../core/router.js';
import { toast } from '../core/toast.js';
import {
    alterarStatusAlerta, executarVarredura, listarAlertas, obterAlerta, revogarAcesso,
} from '../services/alertaService.js';
import { listarPlataformas } from '../services/conexaoService.js';
import { cardAlerta } from '../components/alertCard.js';
import { estadoVazio } from '../components/emptyState.js';
import { icone } from '../components/icons.js';
import { comEstados } from '../components/loading.js';
import { abrirModal } from '../components/modal.js';
import { cartaoEstatistica } from '../components/statCard.js';
import { badgePrioridade, badgeStatus } from '../components/statusBadge.js';

export default {
    mount(el, parametros) {
        if (parametros.id) montarDetalhe(el, parametros.id);
        else montarLista(el);
    },
};

function montarLista(el) {
    render(el, html`
        <header class="cabecalho-pagina">
            <div><h1>Central de Alertas</h1><p>Monitoramento contínuo das suas plataformas, com prioridade e contexto.</p></div>
            <button class="botao botao--primario" data-acao="varredura">${icone('atualizar')} Executar varredura</button>
        </header>
        <div class="grade-estatisticas" id="alertas-resumo"></div>
        <section class="painel">
            <div class="painel__cabecalho"><h2>Alertas</h2>
                <div class="barra-filtros">
                    <select class="seletor" id="filtro-prioridade" aria-label="Filtrar por prioridade">
                        <option value="">Todas as prioridades</option><option value="ALTA">Alta</option><option value="MEDIA">Média</option><option value="BAIXA">Baixa</option></select>
                    <select class="seletor" id="filtro-status" aria-label="Filtrar por status">
                        <option value="">Todos os status</option><option value="ABERTO" selected>Abertos</option><option value="EM_ANALISE">Em análise</option><option value="RESOLVIDO">Resolvidos</option></select>
                    <select class="seletor" id="filtro-plataforma" aria-label="Filtrar por plataforma"><option value="">Todas as plataformas</option></select>
                </div>
            </div>
            <div id="alertas-lista" class="lista-cards"></div>
        </section>`);

    listarPlataformas().then(plataformas => {
        $('#filtro-plataforma', el).insertAdjacentHTML('beforeend', String(html`${plataformas.map(p => html`<option value="${p.slug}">${p.nome}</option>`)}`));
    }).catch(() => {});

    const carregar = () => comEstados($('#alertas-lista', el), () => listarAlertas({
        prioridade: $('#filtro-prioridade', el).value,
        status: $('#filtro-status', el).value,
        plataforma: $('#filtro-plataforma', el).value,
    }), lista => {
        const contar = p => lista.filter(a => a.prioridade === p).length;
        render($('#alertas-resumo', el), html`
            ${cartaoEstatistica('Alertas na lista', lista.length)}
            ${cartaoEstatistica('Prioridade alta', contar('ALTA'), 'alerta')}
            ${cartaoEstatistica('Prioridade média', contar('MEDIA'), 'aviso')}
            ${cartaoEstatistica('Prioridade baixa', contar('BAIXA'))}`);
        render($('#alertas-lista', el), lista.length ? html`${lista.map(cardAlerta)}` : estadoVazio('Nenhum alerta com esses filtros.'));
    }, 4);

    on(el, 'change', '.seletor', carregar);
    on(el, 'click', '[data-acao]', (e, alvo) => {
        if (alvo.dataset.acao === 'ver-alerta') navegar(`alertas/${alvo.dataset.id}`);
        if (alvo.dataset.acao === 'varredura') {
            comBotaoOcupado(alvo, 'Varrendo plataformas...', executarVarredura)
                .then(r => {
                    toast(r.alertasCriados ? `${plural(r.alertasCriados, 'novo alerta', 'novos alertas')} encontrado(s).`
                        : 'Varredura concluída: nenhum alerta novo.', 'sucesso');
                    carregar();
                })
                .catch(erro => toast(erro.message, 'erro'));
        }
    });
    carregar();
}

function montarDetalhe(el, id) {
    render(el, html`<a href="#/alertas" class="link-voltar">${icone('voltar')} Voltar para a Central de Alertas</a><div id="alerta"></div>`);
    const carregar = () => comEstados($('#alerta', el), () => obterAlerta(id), alerta => desenharDetalhe(el, alerta), 3);

    on(el, 'click', '[data-acao]', (e, alvo) => {
        const acao = alvo.dataset.acao;
        if (acao === 'status') {
            comBotaoOcupado(alvo, 'Salvando...', () => alterarStatusAlerta(id, alvo.dataset.status))
                .then(() => { toast('Status do alerta atualizado.', 'sucesso'); carregar(); })
                .catch(erro => toast(erro.message, 'erro'));
        }
        if (acao === 'revogar') confirmarRevogacao(alvo.dataset.acesso, carregar);
    });
    carregar();
}

function desenharDetalhe(el, a) {
    const acoesStatus = [
        a.status !== 'EM_ANALISE' && a.status !== 'RESOLVIDO' ? html`<button class="botao" data-acao="status" data-status="EM_ANALISE">Marcar em análise</button>` : '',
        a.status !== 'RESOLVIDO' && !a.acessoRevogavelId ? html`<button class="botao botao--sucesso" data-acao="status" data-status="RESOLVIDO">${icone('check')} Marcar como resolvido</button>` : '',
        a.status === 'RESOLVIDO' ? html`<button class="botao" data-acao="status" data-status="ABERTO">Reabrir alerta</button>` : '',
    ];
    render($('#alerta', el), html`
        <section class="painel">
            <div class="linha-acoes">${badgePrioridade(a.prioridade)} ${badgeStatus(a.status, a.statusDescricao)} <span class="badge badge--neutro">${a.tipoDescricao}</span></div>
            <h1>${a.titulo}</h1>
            <p>${a.descricao}</p>
            <div class="grade-info">
                <div class="info"><span>Plataforma</span><strong>${a.plataformaNome || '—'}</strong></div>
                <div class="info"><span>Origem</span><strong>${a.origemDescricao} #${a.origemId}</strong></div>
                <div class="info"><span>Detectado em</span><strong>${dataHora(a.criadoEm)}</strong></div>
                <div class="info"><span>Clientes afetados</span><strong>${a.clientesAfetados}</strong></div>
                ${a.diasRestantes !== null ? html`<div class="info"><span>Prazo</span><strong>${textoPrazo(a.diasRestantes)}</strong></div>` : ''}
                ${a.resolvidoEm ? html`<div class="info"><span>Resolvido em</span><strong>${dataHora(a.resolvidoEm)}</strong></div>` : ''}
                ${a.titularId ? html`<div class="info"><span>Titular</span><strong><a href="#/clientes/${a.titularId}">${a.titularNome}</a></strong></div>` : ''}
            </div>
            ${a.acessoRevogavelId ? html`<div class="caixa-aviso">Este alerta só é resolvido quando o acesso for revogado.
                <div class="linha-acoes" style="margin-top:0.6rem"><button class="botao botao--perigo" data-acao="revogar" data-acesso="${a.acessoRevogavelId}">${icone('cadeado')} Revogar acesso</button></div></div>` : ''}
            <div class="linha-acoes">${acoesStatus}</div>
        </section>`);
}

function confirmarRevogacao(acessoId, recarregar) {
    abrirModal({
        titulo: 'Revogar acesso',
        corpo: html`<p>O ex-funcionário perde imediatamente o acesso a esta plataforma pela Atria. A ação fica registrada na auditoria.</p>`,
        acoes: [{ id: 'cancelar', rotulo: 'Cancelar' }, { id: 'revogar', rotulo: 'Revogar agora', classe: 'botao--perigo' }],
        async aoAgir(acao, modal) {
            modal.carregando(true);
            try {
                const acesso = await revogarAcesso(acessoId);
                modal.fechar();
                toast(`Acesso de ${acesso.usuarioNome} ao ${acesso.plataforma} revogado.`, 'sucesso');
                recarregar();
            } catch (erro) {
                modal.carregando(false);
                toast(erro.message, 'erro');
            }
        },
    });
}
