import { $, comBotaoOcupado, html, on, render } from '../core/dom.js';
import { dataHora } from '../core/format.js';
import { navegar } from '../core/router.js';
import { toast } from '../core/toast.js';
import { obterIndicadoresRisco, obterOcorrencias } from '../services/riscoService.js';
import { listarSolicitacoes } from '../services/solicitacaoService.js';
import { baixarRelatorio } from '../services/relatorioService.js';
import { cardSolicitacao } from '../components/alertCard.js';
import { colunasOcorrencias } from '../components/chart.js';
import { estadoVazio } from '../components/emptyState.js';
import { icone } from '../components/icons.js';
import { comEstados } from '../components/loading.js';
import { cartaoEstatistica } from '../components/statCard.js';
import { cartaoStatus } from '../components/statusCard.js';
import { badgePrioridade, badgeStatus } from '../components/statusBadge.js';

export default {
    mount(el) {
        let solicitacoes = [];

        render(el, html`
            <header class="cabecalho-pagina"><div><h1>Riscos e Prazos</h1>
                <p>Solicitações em aberto, prazos restantes e exposição da loja.</p></div></header>
            <div id="riscos-metricas"></div>
            <section class="painel">
                <h2>Solicitações em aberto</h2>
                <div class="barra-filtros">
                    <label class="campo-busca">${icone('busca')}<span class="visualmente-oculto">Buscar</span>
                        <input type="search" id="riscos-busca" placeholder="Buscar por cliente, protocolo ou tipo..."></label>
                    <select class="seletor" id="filtro-nivel" aria-label="Filtrar por risco">
                        <option value="">Filtrar por: todos os prazos</option>
                        <option value="VERMELHO">Vencidas</option><option value="LARANJA">Até 2 dias</option>
                        <option value="AMARELO">De 3 a 7 dias</option><option value="VERDE">Mais de 7 dias</option></select>
                    <select class="seletor" id="filtro-status" aria-label="Filtrar por status">
                        <option value="">Todos os status</option><option value="PENDENTE">Pendente</option>
                        <option value="EM_ANDAMENTO">Em andamento</option><option value="ATRASADA">Atrasada</option></select>
                </div>
                <div id="riscos-lista" class="lista-cards"></div>
            </section>
            <div class="grade-2" id="riscos-status"></div>
            <section class="painel">
                <h2>Histórico de ocorrências</h2>
                <p class="painel__subtitulo">Alertas novos e resolvidos por semana, e as ocorrências mais recentes.</p>
                <div id="riscos-ocorrencias"></div>
            </section>`);

        const desenharLista = () => {
            const termo = $('#riscos-busca', el).value.trim().toLowerCase();
            const nivel = $('#filtro-nivel', el).value;
            const status = $('#filtro-status', el).value;
            const filtradas = solicitacoes.filter(s => (!nivel || s.nivelPrazo === nivel) && (!status || s.status === status)
                && (!termo || `${s.titularNome} ${s.protocolo} ${s.tipoDescricao} ${s.canalDescricao}`.toLowerCase().includes(termo)));
            render($('#riscos-lista', el), filtradas.length ? html`${filtradas.map(cardSolicitacao)}` : estadoVazio('Nenhuma solicitação em aberto com esses filtros.'));
        };

        comEstados($('#riscos-metricas', el), obterIndicadoresRisco, ind => {
            render($('#riscos-metricas', el), html`<div class="grade-estatisticas">
                ${cartaoEstatistica('Solicitações em aberto', ind.solicitacoesAbertas)}
                ${cartaoEstatistica('Vencidas', ind.solicitacoesAtrasadas, 'alerta')}
                ${cartaoEstatistica('Vencem em até 2 dias', ind.vencendoEmAte2Dias, 'alerta')}
                ${cartaoEstatistica('Vencem em 3 a 7 dias', ind.vencendoEm3a7Dias, 'aviso')}
                ${cartaoEstatistica('Alertas em aberto', ind.alertasAbertos)}
                ${cartaoEstatistica('Riscos resolvidos', ind.alertasResolvidos, 'sucesso')}
            </div>`);
            render($('#riscos-status', el), html`
                ${cartaoStatus('Status de Exposição ANPD', ind.statusAnpd,
                    html`<button class="botao botao--contorno-branco" data-acao="baixar-relatorio" data-tipo="anpd">${icone('download')} Baixar relatório</button>`)}
                ${cartaoStatus('Riscos de Marketplace', ind.statusMarketplace,
                    html`<a class="botao botao--contorno-branco" href="#/alertas">Ver alertas</a>`)}`);
        }, 2);

        comEstados($('#riscos-lista', el), () => listarSolicitacoes(), lista => {
            solicitacoes = lista.filter(s => s.status !== 'CONCLUIDA');
            desenharLista();
        }, 4);

        comEstados($('#riscos-ocorrencias', el), obterOcorrencias, ocorrencias => render($('#riscos-ocorrencias', el), html`
            ${colunasOcorrencias(ocorrencias.serie)}
            <div class="tabela-rolagem"><table class="tabela">
                <thead><tr><th>Detectado em</th><th>Ocorrência</th><th>Prioridade</th><th>Status</th><th>Resolvido em</th></tr></thead>
                <tbody>${ocorrencias.eventos.slice(0, 12).map(a => html`<tr>
                    <td>${dataHora(a.criadoEm)}</td>
                    <td><a href="#/alertas/${a.id}">${a.titulo}</a></td>
                    <td>${badgePrioridade(a.prioridade)}</td><td>${badgeStatus(a.status, a.statusDescricao)}</td>
                    <td>${dataHora(a.resolvidoEm)}</td></tr>`)}</tbody>
            </table></div>`), 3);

        on(el, 'input', '#riscos-busca', desenharLista);
        on(el, 'change', '.seletor', desenharLista);
        on(el, 'click', '[data-acao]', (e, alvo) => {
            if (alvo.dataset.acao === 'ver-titular') navegar(`clientes/${alvo.dataset.id}`);
            if (alvo.dataset.acao === 'baixar-relatorio') {
                comBotaoOcupado(alvo, 'Gerando...', () => baixarRelatorio(alvo.dataset.tipo))
                    .then(nome => toast(`${nome} baixado.`, 'sucesso'))
                    .catch(erro => toast(erro.message, 'erro'));
            }
        });
    },
};
