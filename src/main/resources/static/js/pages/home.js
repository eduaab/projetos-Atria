import { $, comBotaoOcupado, debounce, html, on, render } from '../core/dom.js';
import { data } from '../core/format.js';
import { navegar } from '../core/router.js';
import { toast } from '../core/toast.js';
import { obterDashboard } from '../services/dashboardService.js';
import { buscarTitulares } from '../services/titularService.js';
import { baixarRelatorio } from '../services/relatorioService.js';
import { cardAlerta } from '../components/alertCard.js';
import { calendario } from '../components/calendar.js';
import { linhaCliente } from '../components/clienteRow.js';
import { estadoVazio } from '../components/emptyState.js';
import { carregando, comEstados } from '../components/loading.js';
import { cartaoEstatistica } from '../components/statCard.js';
import { icone } from '../components/icons.js';
import { indicadorPrazo } from '../components/statusBadge.js';

const RELATORIOS = [
    { tipo: 'esg', nome: 'Relatório ESG' },
    { tipo: 'anpd', nome: 'Relatório ANPD' },
    { tipo: 'comercial', nome: 'Diferencial Comercial' },
];

function saudacao() {
    const hora = new Date().getHours();
    if (hora < 12) return 'Bom dia!';
    return hora < 18 ? 'Boa tarde!' : 'Boa noite!';
}

export default {
    mount(el) {
        const estado = { mes: new Date(), prazos: [], diaSelecionado: null };

        render(el, html`
            <header class="cabecalho-pagina">
                <div><p class="sobretitulo" id="home-loja">Atria</p><h1>${saudacao()}</h1><p>Tudo o que precisa da sua atenção hoje, em um só lugar.</p></div>
            </header>
            <div id="home-dados" class="pagina"></div>
            <section class="painel">
                <h2>Central de clientes</h2>
                <label class="campo-busca">${icone('busca')}
                    <span class="visualmente-oculto">Buscar titular</span>
                    <input type="search" id="home-busca" placeholder="Buscar titular por nome, CPF, e-mail ou telefone..." autocomplete="off">
                </label>
                <div id="home-resultados" class="lista-linhas"></div>
            </section>
            <div class="grade-2">
                <section class="painel">
                    <h2>Gerar Relatórios</h2>
                    ${RELATORIOS.map(r => html`<div class="linha-relatorio"><span>${r.nome}</span>
                        <button class="botao botao--pequeno" data-acao="baixar-relatorio" data-tipo="${r.tipo}">${icone('download')} Baixar</button></div>`)}
                    <a href="#/relatorios" class="link-voltar">Ver todos os indicadores</a>
                </section>
                <section class="painel">
                    <div id="home-calendario"></div>
                    <div id="home-prazos-dia" class="lista-linhas"></div>
                </section>
            </div>`);

        const desenharCalendario = () => {
            render($('#home-calendario', el), calendario(estado.mes, estado.prazos, estado.diaSelecionado));
            const doDia = estado.prazos.filter(p => p.prazo === estado.diaSelecionado);
            render($('#home-prazos-dia', el), estado.diaSelecionado
                ? (doDia.length ? html`${doDia.map(p => html`<div class="linha-branca"><div class="linha-branca__textos">
                        <strong>${p.titularNome}</strong><span>${p.tipoDescricao} · ${p.protocolo}</span></div>
                        <div class="linha-branca__lado">${indicadorPrazo(p.nivelPrazo, p.diasRestantes)}
                        <button class="botao botao--pequeno" data-acao="ver-titular" data-id="${p.titularId}">Abrir</button></div></div>`)}`
                    : estadoVazio(`Nenhum prazo em ${data(estado.diaSelecionado)}.`, 'check'))
                : html``);
        };

        comEstados($('#home-dados', el), obterDashboard, dashboard => {
            $('#home-loja', el).textContent = dashboard.nomeLoja;
            document.getElementById('nome-loja').textContent = dashboard.nomeLoja;
            const c = dashboard.contadores;
            render($('#home-dados', el), html`
                <section class="grade-estatisticas">
                    ${cartaoEstatistica('Clientes cadastrados', c.titulares)}
                    ${cartaoEstatistica('Plataformas conectadas', c.plataformasConectadas, 'sucesso')}
                    ${cartaoEstatistica('Solicitações em aberto', c.solicitacoesAbertas, 'aviso')}
                    ${cartaoEstatistica('Alertas de prioridade alta', c.alertasAltaPrioridade, 'alerta')}
                </section>
                <section class="painel">
                    <div class="painel__cabecalho"><h2>Quadro de avisos</h2><a href="#/alertas" class="botao botao--pequeno">Central de alertas</a></div>
                    <p class="frase-resumo ${c.alertasAbertos === 0 ? 'frase-resumo--ok' : ''}">${dashboard.fraseResumo}</p>
                    <div class="lista-cards">
                        ${dashboard.alertasPrioritarios.length ? dashboard.alertasPrioritarios.map(cardAlerta) : estadoVazio('Nenhum alerta em aberto.')}
                    </div>
                </section>`);
            estado.prazos = dashboard.prazos;
            desenharCalendario();
        }, 4);
        render($('#home-calendario', el), carregando(2));

        const buscar = debounce(async termo => {
            const alvo = $('#home-resultados', el);
            if (!termo) return render(alvo, html``);
            render(alvo, carregando(2));
            try {
                const lista = await buscarTitulares(termo);
                render(alvo, lista.length ? html`${lista.slice(0, 5).map(linhaCliente)}` : estadoVazio('Nenhum titular encontrado.', 'busca'));
            } catch (erro) {
                render(alvo, html``);
                toast(erro.message, 'erro');
            }
        }, 300);
        $('#home-busca', el).addEventListener('input', e => buscar(e.target.value.trim()));

        on(el, 'click', '[data-acao]', (e, alvo) => {
            switch (alvo.dataset.acao) {
                case 'ver-titular': return navegar(`clientes/${alvo.dataset.id}`);
                case 'ver-alerta': return navegar(`alertas/${alvo.dataset.id}`);
                case 'baixar-relatorio':
                    return comBotaoOcupado(alvo, 'Gerando...', () => baixarRelatorio(alvo.dataset.tipo))
                        .then(nome => toast(`${nome} baixado.`, 'sucesso'))
                        .catch(erro => toast(erro.message, 'erro'));
                case 'mes-anterior':
                    estado.mes = new Date(estado.mes.getFullYear(), estado.mes.getMonth() - 1, 1);
                    return desenharCalendario();
                case 'mes-seguinte':
                    estado.mes = new Date(estado.mes.getFullYear(), estado.mes.getMonth() + 1, 1);
                    return desenharCalendario();
                case 'selecionar-dia':
                    estado.diaSelecionado = estado.diaSelecionado === alvo.dataset.dia ? null : alvo.dataset.dia;
                    return desenharCalendario();
                default:
                    return undefined;
            }
        });
    },
};
