import { $, comBotaoOcupado, html, on, render } from '../core/dom.js';
import { data, dataHora } from '../core/format.js';
import { toast } from '../core/toast.js';
import {
    baixarComprovante, baixarRelatorio, listarAuditoria, listarEvidencias, obterIndicadoresRelatorio,
} from '../services/relatorioService.js';
import { listarAlertas } from '../services/alertaService.js';
import { estadoVazio } from '../components/emptyState.js';
import { icone } from '../components/icons.js';
import { comEstados } from '../components/loading.js';
import { cartaoEstatistica } from '../components/statCard.js';
import { badgePrioridade } from '../components/statusBadge.js';

const MODELOS = [
    { tipo: 'esg', titulo: 'Relatório ESG', texto: 'Pilar G (governança): indicadores, solicitações atendidas, riscos resolvidos e histórico de ações.' },
    { tipo: 'anpd', titulo: 'Conformidade ANPD', texto: 'Atendimento aos direitos dos titulares, prazos, riscos em aberto e comprovantes emitidos.' },
    { tipo: 'comercial', titulo: 'Diferencial Comercial', texto: 'Modelo para parceiros e marketplaces: destaques de conformidade e práticas adotadas.' },
];

const percentual = (parte, total) => (total ? `${Math.round(parte * 100 / total)}%` : '—');

export default {
    mount(el) {
        render(el, html`
            <header class="cabecalho-pagina"><div><h1>Relatórios</h1>
                <p>Indicadores consolidados de conformidade e documentos para exportar em PDF.</p></div></header>
            <div id="rel-indicadores"></div>
            <section class="painel">
                <h2>Exportar relatórios</h2>
                <div class="grade-3">${MODELOS.map(m => html`
                    <article class="cartao-relatorio"><h3>${m.titulo}</h3><p>${m.texto}</p>
                        <button class="botao botao--primario" data-acao="baixar-relatorio" data-tipo="${m.tipo}">${icone('download')} Baixar PDF</button></article>`)}</div>
            </section>
            <section class="painel"><h2>Solicitações atendidas</h2><div id="rel-evidencias"></div></section>
            <section class="painel"><h2>Riscos resolvidos</h2><div id="rel-riscos"></div></section>
            <section class="painel"><h2>Histórico de ações</h2><div id="rel-auditoria"></div></section>`);

        comEstados($('#rel-indicadores', el), obterIndicadoresRelatorio, i => render($('#rel-indicadores', el), html`
            <section class="painel">
                <div class="painel__cabecalho"><h2>Indicadores de conformidade</h2><span class="badge badge--neutro badge--sem-ponto">${data(i.periodoInicio)} a ${data(i.periodoFim)}</span></div>
                <div class="indice"><span class="indice__valor">${i.indiceConformidade}</span>
                    <div><strong>Índice de conformidade (0 a 100)</strong><p class="painel__subtitulo" style="margin:0">Desconta solicitações atrasadas, acessos indevidos e alertas de prioridade alta em aberto.</p></div></div>
                <div class="grade-estatisticas">
                    ${cartaoEstatistica('Solicitações recebidas', i.solicitacoesRecebidas)}
                    ${cartaoEstatistica('Solicitações atendidas', i.solicitacoesAtendidas, 'sucesso')}
                    ${cartaoEstatistica('Atendidas no prazo', percentual(i.solicitacoesAtendidasNoPrazo, i.solicitacoesAtendidas), 'sucesso')}
                    ${cartaoEstatistica('Tempo médio (dias)', i.tempoMedioAtendimentoDias.toLocaleString('pt-BR'))}
                    ${cartaoEstatistica('Solicitações atrasadas', i.solicitacoesAtrasadas, 'alerta')}
                    ${cartaoEstatistica('Riscos resolvidos', i.alertasResolvidos, 'sucesso')}
                    ${cartaoEstatistica('Acessos revogados', i.acessosRevogados)}
                    ${cartaoEstatistica('Titulares anonimizados', i.titularesAnonimizados)}
                </div>
            </section>`), 2);

        comEstados($('#rel-evidencias', el), listarEvidencias, lista => render($('#rel-evidencias', el), lista.length ? html`
            <div class="tabela-rolagem"><table class="tabela">
                <thead><tr><th>Protocolo</th><th>Tipo</th><th>Canal</th><th>Concluída em</th><th>CPF</th><th>Plataformas</th><th></th></tr></thead>
                <tbody>${lista.map(e => html`<tr><td class="codigo">${e.protocolo}</td><td>${e.tipoDescricao}</td><td>${e.canalDescricao}</td>
                    <td>${dataHora(e.concluidaEm)}</td><td class="codigo">${e.cpfMascarado}</td><td>${e.plataformas.join(', ') || '—'}</td>
                    <td><button class="botao botao--pequeno" data-acao="baixar-comprovante" data-protocolo="${e.protocolo}">${icone('download')} Comprovante</button></td></tr>`)}</tbody>
            </table></div>` : estadoVazio('Nenhuma solicitação atendida ainda.')));

        comEstados($('#rel-riscos', el), () => listarAlertas({ status: 'RESOLVIDO' }), lista => render($('#rel-riscos', el), lista.length ? html`
            <div class="tabela-rolagem"><table class="tabela">
                <thead><tr><th>Detectado em</th><th>Ocorrência</th><th>Prioridade</th><th>Resolvido em</th></tr></thead>
                <tbody>${lista.map(a => html`<tr><td>${dataHora(a.criadoEm)}</td><td><a href="#/alertas/${a.id}">${a.titulo}</a></td>
                    <td>${badgePrioridade(a.prioridade)}</td><td>${dataHora(a.resolvidoEm)}</td></tr>`)}</tbody>
            </table></div>` : estadoVazio('Nenhum risco resolvido ainda.')));

        comEstados($('#rel-auditoria', el), () => listarAuditoria(50), lista => render($('#rel-auditoria', el), lista.length ? html`
            <div class="tabela-rolagem" style="max-height:420px;overflow-y:auto"><table class="tabela">
                <thead><tr><th>Data</th><th>Ação</th><th>Descrição</th></tr></thead>
                <tbody>${lista.map(a => html`<tr><td style="white-space:nowrap">${dataHora(a.ocorridoEm)}</td><td>${a.acaoDescricao}</td><td>${a.descricao}</td></tr>`)}</tbody>
            </table></div>` : estadoVazio('Nenhuma ação registrada.')));

        on(el, 'click', '[data-acao]', (e, alvo) => {
            const tarefa = alvo.dataset.acao === 'baixar-relatorio'
                ? () => baixarRelatorio(alvo.dataset.tipo)
                : () => baixarComprovante(alvo.dataset.protocolo);
            comBotaoOcupado(alvo, 'Gerando...', tarefa)
                .then(nome => toast(`${nome} baixado.`, 'sucesso'))
                .catch(erro => toast(erro.message, 'erro'));
        });
    },
};
