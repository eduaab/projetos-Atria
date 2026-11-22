import { $, html, on, raw, render } from '../core/dom.js';
import { data } from '../core/format.js';
import { obterDashboard } from '../services/dashboardService.js';
import { obterIndicadoresRelatorio } from '../services/relatorioService.js';
import * as api from '../services/painelService.js';
import { antesDepois, barras, fmt, linhas, minilinha } from '../components/graficos.js';
import { estadoVazio } from '../components/emptyState.js';
import { icone, iconePlataforma } from '../components/icons.js';
import { comEstados } from '../components/loading.js';

const FORMATOS = { moeda: fmt.moeda, percentual: fmt.pct, numero: fmt.num, horas: v => `${fmt.num(v)} h` };
const NIVEL = { verde: 'sucesso', amarelo: 'media', laranja: 'alta', vermelho: 'alta' };
const NOME_CANAL = { whatsapp: 'WhatsApp', instagram: 'Instagram', 'mercado-livre': 'Mercado Livre', shopee: 'Shopee', shein: 'Shein', nuvemshop: 'Nuvemshop', 'tiktok-shop': 'TikTok Shop' };
const INTENCAO = { compra: 'Quer comprar', duvida_produto: 'Dúvida sobre produto', pos_venda: 'Pós-venda', reclamacao: 'Reclamação', troca_devolucao: 'Troca ou devolução' };
const TIPO_RECLAMACAO = { troca: 'Troca', devolucao: 'Devolução', produto_diferente_ou_defeito: 'Produto diferente ou com defeito' };
const rotuloDia = iso => data(iso).slice(0, 5);
const rotuloData = iso => `${data(iso).slice(0, 6)}${data(iso).slice(8)}`;
const rotuloMes = iso => `${data(iso).slice(3, 5)}/${data(iso).slice(8)}`;
const leitura = texto => (texto ? html`<p class="leitura">${texto}</p>` : html``);
const PERIODOS = [[7, '7 dias'], [30, '30 dias'], [60, '60 dias'], [120, '120 dias'], [365, '12 meses'], [0, 'Todo o histórico']];
const nomePeriodo = p => (p === 0 ? 'Todo o histórico' : `Últimos ${p} dias`);

function variacao(ind) {
    if (ind.comparacao === null || ind.comparacao === undefined) return html``;
    const pontos = ind.formato === 'percentual';
    const dif = pontos ? ind.valor - ind.comparacao : (ind.comparacao ? (ind.valor - ind.comparacao) * 100 / Math.abs(ind.comparacao) : 0);
    const bom = ind.altaEhBom ? dif >= 0 : dif <= 0;
    const neutro = Math.abs(dif) < 0.05;
    const sinal = dif >= 0 ? '+' : '−';
    const texto = pontos ? `${sinal}${fmt.num(Math.abs(dif))} pp` : `${sinal}${fmt.num(Math.abs(dif))}%`;
    return html`<small class="kpi__var ${neutro ? '' : (bom ? 'kpi__var--bom' : 'kpi__var--ruim')}" title="Comparado a: ${ind.rotuloComparacao}">
        ${neutro ? '=' : (dif >= 0 ? '▲' : '▼')} ${texto} <span>vs ${ind.rotuloComparacao}</span></small>`;
}

const kpi = ind => html`<div class="kpi"><span>${ind.rotulo}</span><strong>${(FORMATOS[ind.formato] || fmt.num)(ind.valor)}</strong>${variacao(ind)}</div>`;

const secao = (id, titulo, subtitulo = '') => html`
    <section class="painel painel--secao" id="${id}" aria-labelledby="${id}-t">
        <div class="painel__cabecalho"><div><h2 id="${id}-t">${titulo}</h2>${subtitulo ? html`<p class="painel__subtitulo painel__subtitulo--solto">${subtitulo}</p>` : ''}</div></div>
        <div id="${id}-corpo" class="painel__corpo"></div>
    </section>`;

export default {
    mount(el) {
        const estado = { periodo: 30, plataforma: 'todas', canal: 'todos' };
        const FIXO = 30; // as demais seções mostram sempre os últimos 30 dias; só "Como foi ontem e o período" tem seletor
        render(el, html`
            <header class="cabecalho-pagina">
                <div><p class="sobretitulo" id="painel-loja">Atria</p><h1>Painel do dia</h1>
                    <p id="painel-sub">O que aconteceu, o que está em risco e o que precisa de você.</p></div>
                <div class="barra-filtros" role="group" aria-label="Filtros do painel">
                    <label class="filtro"><span>Plataforma de venda</span>
                        <select class="seletor" id="f-plataforma"><option value="todas">Todas</option></select></label>
                </div>
            </header>
            <nav class="atalhos" aria-label="Seções do painel">
                <a href="#" data-ir="p-pendencias">O que precisa de mim</a><a href="#" data-ir="p-ontem">Ontem e período</a><a href="#" data-ir="p-canais">Ganhos e perdas</a>
                <a href="#" data-ir="p-atendimento">Atendimento</a><a href="#" data-ir="p-reputacao">Reputação</a><a href="#" data-ir="p-clientes">Clientes</a>
                <a href="#" data-ir="p-efeito">Efeito Atria</a><a href="#" data-ir="p-modelos">Modelos</a></nav>
            ${secao('p-pendencias', 'O que precisa de mim agora')}
            ${secao('p-ontem', 'Como foi ontem e o período')}
            ${secao('p-canais', 'Onde estou ganhando e onde estou perdendo', 'Últimos 30 dias.')}
            ${secao('p-atendimento', 'Canais de relacionamento', 'Últimos 30 dias.')}
            ${secao('p-reputacao', 'Reputação nos marketplaces')}
            ${secao('p-clientes', 'Quem são meus clientes', 'Últimos 30 dias.')}
            ${secao('p-efeito', 'Efeito Atria', 'Como a loja estava antes e como está depois de começar a usar a Atria.')}
            ${secao('p-modelos', 'Bastidores: o que o histórico ensina')}
            ${secao('p-lgpd', 'Conformidade e LGPD')}`);

        const corpo = id => $(`#${id}-corpo`, el);
        corpo('p-ontem').insertAdjacentHTML('beforebegin', '<div class="barra-periodo" role="group" aria-label="Período analisado"><span>Período</span><span id="chips-periodo" class="barra-filtros"></span></div>');

        // ---------- seções que dependem dos filtros
        const desenharOntem = () => comEstados(corpo('p-ontem'), async () => {
            const [resumo, serie] = await Promise.all([api.obterResumo(estado.periodo, estado.plataforma), api.obterSerie(estado.periodo, estado.plataforma)]);
            return { resumo, serie };
        }, ({ resumo, serie }) => {
            render(corpo('p-ontem'), html`
                <h3>Ontem (${data(resumo.referencia)})</h3>
                <div class="grade-kpi grade-kpi--3">${resumo.ontem.map(kpi)}</div>
                <h3>${nomePeriodo(estado.periodo)}</h3>
                <div class="grade-kpi">${resumo.periodo.map(kpi)}</div>
                <h3>Receita e lucro líquido por ${serie.granularidade}</h3>
                ${linhas({ titulo: `Receita e lucro por ${serie.granularidade}`, rotulos: serie.pontos.map(p => (serie.granularidade === 'dia' ? rotuloDia(p.data) : serie.granularidade === 'mês' ? rotuloMes(p.data) : `sem ${rotuloDia(p.data)}`)),
                    series: [{ nome: 'Receita', cor: 1, valores: serie.pontos.map(p => p.receita) }, { nome: 'Lucro líquido', cor: 2, valores: serie.pontos.map(p => p.lucro) }], formato: fmt.moeda })}
                ${leitura(`${resumo.leitura} ${serie.leitura}`)}`);
        }, 4);

        const desenharCanais = () => comEstados(corpo('p-canais'), () => api.obterCanais(FIXO), c => {
            render(corpo('p-canais'), html`
                <div class="grade-2 grade-2--solta">
                    <div><h3>Receita</h3>${barras({ titulo: 'Receita por plataforma', itens: c.canais.map(x => ({ rotulo: x.nome, valor: x.receita })), formato: fmt.moeda })}</div>
                    <div><h3>Margem líquida</h3>${barras({ titulo: 'Margem líquida por plataforma', max: 60, itens: [...c.canais].sort((a, b) => b.margem - a.margem).map(x => ({ rotulo: x.nome, valor: x.margem, destaque: true })), formato: fmt.pct })}</div>
                </div>
                <div class="tabela-rolagem"><table class="tabela"><caption class="visualmente-oculto">Indicadores por plataforma</caption>
                    <thead><tr><th>Plataforma</th><th>Pedidos</th><th>Receita</th><th>Lucro</th><th>Margem</th><th>Cancelados</th><th>Atrasados</th><th>Reclamações</th></tr></thead>
                    <tbody>${c.canais.map(x => html`<tr><td><span class="linha-icone">${iconePlataforma(x.codigo)} ${x.nome}</span></td><td>${fmt.num(x.pedidos)}</td><td>${fmt.moeda(x.receita)}</td><td>${fmt.moeda(x.lucro)}</td>
                        <td><strong>${fmt.pct(x.margem)}</strong>${x.margemAnterior === null ? '' : html` <small class="${x.margem >= x.margemAnterior ? 'texto-bom' : 'texto-ruim'}">(${x.margem >= x.margemAnterior ? '+' : '−'}${fmt.num(Math.abs(x.margem - x.margemAnterior))} pp)</small>`}</td>
                        <td>${fmt.pct(x.cancelamento)}</td><td>${fmt.pct(x.atraso)}</td><td>${fmt.pct(x.reclamacao)}</td></tr>`)}</tbody></table></div>
                ${leitura(c.leitura)}`);
        }, 4);

        const desenharAtendimento = () => comEstados(corpo('p-atendimento'), async () => {
            const [a, p] = await Promise.all([api.obterAtendimento(FIXO, estado.canal), api.obterPosts(FIXO)]);
            return { a, p };
        }, ({ a, p }) => {
            const chips = [['todos', 'Todos'], ['whatsapp', 'WhatsApp'], ['instagram', 'Instagram']].map(([cod, nome]) =>
                html`<button class="chip-filtro ${estado.canal === cod ? 'ativo' : ''}" data-canal="${cod}" aria-pressed="${estado.canal === cod}">${nome}</button>`);
            render(corpo('p-atendimento'), html`
                <div class="barra-filtros" role="group" aria-label="Canal de atendimento">${chips}</div>
                <div class="grade-kpi grade-kpi--3">
                    <div class="kpi"><span>Conversas</span><strong>${fmt.num(a.conversas)}</strong></div>
                    <div class="kpi"><span>Respondidas em até 1 h</span><strong>${fmt.pct(a.respondidasAte1h)}</strong></div>
                    <div class="kpi"><span>Sem resposta</span><strong>${fmt.pct(a.semResposta)}</strong></div></div>
                <div class="grade-2 grade-2--solta">
                    <div><h3>A demora custa venda</h3><p class="painel__subtitulo painel__subtitulo--solto">De cada 100 conversas, quantas viram pedido, conforme o tempo até a primeira resposta.</p>
                        ${barras({ titulo: 'Conversão por tempo de resposta', max: 60, itens: a.faixas.map(f => ({ rotulo: f.faixa, valor: f.conversao, nota: `${fmt.num(f.conversas)} conversas` })), formato: fmt.pct })}</div>
                    <div><h3>Respondidas em até 1 h, por semana</h3>
                        ${linhas({ titulo: 'Percentual de conversas respondidas em até 1 hora por semana', rotulos: a.semanal.map(s => rotuloDia(s.semana)), series: [{ nome: '% em até 1 h', cor: 2, valores: a.semanal.map(s => s.valor) }], formato: fmt.pct, altura: 300, largura: 520 })}</div>
                </div>
                ${leitura(a.leitura)}
                <h3>Instagram: o que gera conversa</h3>
                ${p.tipos.length ? html`<div class="grade-2 grade-2--solta">
                    <div><h4>Conversas abertas por post, por formato</h4>
                        ${barras({ titulo: 'Conversas abertas por post, por formato', itens: p.tipos.map(t => ({ rotulo: t.tipo === 'reels' ? 'Reels' : t.tipo === 'carrossel' ? 'Carrossel' : 'Foto', valor: t.conversasPorPost, nota: `${t.posts} posts` })), formato: fmt.num })}</div>
                    <div><h4>Conversas por post, conforme salvamentos e compartilhamentos</h4>
                        ${p.engajamento.length ? barras({ titulo: 'Conversas abertas por post, por faixa de salvamentos e compartilhamentos', itens: p.engajamento.map(f => ({ rotulo: f.faixa, valor: f.conversasPorPost, destaque: true, nota: `${f.intervalo} salvamentos + compartilhamentos` })), formato: fmt.num }) : estadoVazio('Posts insuficientes neste período.')}</div></div>
                    <div class="tabela-rolagem"><table class="tabela"><caption class="visualmente-oculto">Melhores posts</caption><thead><tr><th>Melhores posts</th><th>Data</th><th>Alcance</th><th>Salv.+compart.</th><th>Conversas</th></tr></thead>
                    <tbody>${p.melhores.map(m => html`<tr><td>#${m.id} · ${m.tipo}</td><td>${data(m.data)}</td><td>${fmt.num(m.alcance)}</td><td>${fmt.num(m.salvamentosEcompartilhamentos)}</td><td><strong>${m.conversas}</strong></td></tr>`)}</tbody></table></div>
                    ${leitura(p.leitura)}` : estadoVazio('Nenhum post neste período.')}`);
        }, 5);

        const desenharClientes = () => comEstados(corpo('p-clientes'), () => api.obterClientesPainel(FIXO, estado.plataforma), c => {
            render(corpo('p-clientes'), html`
                <div class="grade-kpi grade-kpi--4">
                    <div class="kpi"><span>Clientes que compraram</span><strong>${fmt.num(c.ativos)}</strong></div>
                    <div class="kpi"><span>Novos</span><strong>${fmt.num(c.novos)}</strong></div>
                    <div class="kpi"><span>Já eram da casa</span><strong>${fmt.num(c.recorrentes)}</strong></div>
                    <div class="kpi"><span>Voltam em até 90 dias</span><strong>${fmt.pct(c.taxaRecompra90d)}</strong><small class="kpi__var"><span>captados há 3 a 12 meses</span></small></div></div>
                <div class="grade-2 grade-2--solta">
                    <div><h3>Quem compra em mais de um canal vale mais?</h3><div class="tabela-rolagem"><table class="tabela"><caption class="visualmente-oculto">Segmentos de clientes</caption>
                        <thead><tr><th>Segmento</th><th>Clientes</th><th>Pedidos no histórico</th><th>Gasto médio no histórico</th></tr></thead>
                        <tbody>${c.segmentos.map(s => html`<tr><td>${s.nome}</td><td>${fmt.num(s.clientes)}</td><td>${fmt.num(s.pedidosPorCliente)}</td><td>${fmt.moeda(s.gastoMedio)}</td></tr>`)}</tbody></table></div></div>
                    <div><h3>Principais cidades</h3>${barras({ titulo: 'Clientes por cidade', itens: c.cidades.map(x => ({ rotulo: x.cidade, valor: x.clientes })) })}</div></div>
                ${leitura(c.leitura)}`);
        }, 3);

        // ---------- seções fixas
        comEstados(corpo('p-pendencias'), api.obterPendencias, p => {
            render(corpo('p-pendencias'), html`
                <div class="grade-3 grade-3--topo">
                    <div class="coluna-pend"><h3>${icone('mensagem')} Mensagens paradas (${p.mensagens.length})</h3><p class="painel__subtitulo painel__subtitulo--solto">As de cima têm mais chance de virar venda.</p>
                        ${p.mensagens.length ? p.mensagens.map(m => html`<div class="linha-pend"><div><strong>${INTENCAO[m.intencao] || m.intencao}</strong><span>${NOME_CANAL[m.canal] || m.canal} · parada há ${fmt.num(m.horas)} h</span><em>“${m.texto}”</em></div>
                            <div class="chance" title="Chance estimada de virar pedido se respondida agora"><span class="chance__barra"><i style="width:${Math.round(m.score * 100)}%"></i></span><b>${Math.round(m.score * 100)}%</b></div></div>`) : estadoVazio('Nenhuma mensagem parada.', 'check')}</div>
                    <div class="coluna-pend"><h3>${icone('sacola')} Pedidos para despachar (${p.pedidos.length})</h3><p class="painel__subtitulo painel__subtitulo--solto">Mais de 12 h sem envio.</p>
                        ${p.pedidos.length ? p.pedidos.map(x => html`<div class="linha-pend"><div><strong>Pedido #${x.id}</strong><span>${NOME_CANAL[x.canal] || x.canal} · ${fmt.moeda(x.valor)}</span><em>Prazo prometido: ${x.prazo.slice(8, 10)}/${x.prazo.slice(5, 7)} ${x.prazo.slice(11)}</em></div><span class="badge badge--${x.prio.toLowerCase()}">${fmt.num(x.horas)} h parado</span></div>`) : estadoVazio('Nenhum pedido parado.', 'check')}</div>
                    <div class="coluna-pend"><h3>${icone('alerta')} Reclamações abertas (${p.reclamacoes.length})</h3><p class="painel__subtitulo painel__subtitulo--solto">Sem resolução há mais de 6 h.</p>
                        ${p.reclamacoes.length ? p.reclamacoes.map(x => html`<div class="linha-pend"><div><strong>${TIPO_RECLAMACAO[x.tipo] || x.tipo}</strong><span>${NOME_CANAL[x.canal] || x.canal} · pedido #${x.pedido}</span></div><span class="badge badge--${x.prio.toLowerCase()}">${fmt.num(x.horas)} h aberta</span></div>`) : estadoVazio('Nenhuma reclamação aberta.', 'check')}</div></div>
                ${leitura(p.leitura)}
                <div class="linha-acoes"><a class="botao botao--pequeno" href="#/alertas">Ver na Central de alertas</a></div>`);
        }, 4);

        comEstados(corpo('p-reputacao'), api.obterReputacao, r => {
            render(corpo('p-reputacao'), html`
                <div class="grade-cards-rep">${r.marketplaces.map(m => html`<article class="cartao-rep">
                    <header><span class="linha-icone">${iconePlataforma(m.codigo)} <strong>${m.nome}</strong></span><span class="badge badge--${NIVEL[m.nivel] || 'neutro'}">${m.nivel}</span></header>
                    <div class="cartao-rep__serie">${minilinha(m.serie, 2)}<small>últimas 26 semanas</small></div>
                    <dl><div><dt>Atraso</dt><dd>${fmt.pct(m.atraso)}</dd></div><div><dt>Cancelamento</dt><dd>${fmt.pct(m.cancelamento)}</dd></div>
                        <div><dt>Reclamação</dt><dd>${fmt.pct(m.reclamacao)}</dd></div><div><dt>Penalidade</dt><dd>${m.penalidade} pts</dd></div></dl>
                    ${m.nivel !== m.nivelAnterior ? html`<small class="cartao-rep__mudou">Há 4 semanas estava em ${m.nivelAnterior}</small>` : html``}</article>`)}</div>
                ${leitura(r.leitura)}`);
        }, 3);

        comEstados(corpo('p-efeito'), api.obterEfeito, e => {
            const idx = e.serie.findIndex(s => s.semana >= e.adocao);
            const fim = e.serie.findIndex(s => s.semana >= e.fimAdaptacao);
            const im = e.serieRedes.findIndex(s => s.semana >= e.adocao.slice(0, 8) + '01');
            render(corpo('p-efeito'), html`
                <h3>Mensagens respondidas em até 1 hora, por semana</h3>
                ${linhas({ titulo: 'Percentual semanal de conversas respondidas em até 1 hora', rotulos: e.serie.map(s => rotuloData(s.semana)),
                    series: [{ nome: 'Respondidas em até 1 h', cor: 1, valores: e.serie.map(s => s.valor) }], formato: fmt.pct,
                    marcador: idx >= 0 ? { indice: idx, rotulo: `Início da Atria: ${data(e.adocao)}` } : null,
                    faixa: idx >= 0 && fim > idx ? { de: idx, ate: fim, rotulo: 'adaptação' } : null })}
                <h3>Antes e depois: atendimento e envio</h3>
                ${antesDepois({ titulo: 'Indicadores de atendimento e envio, antes e depois da Atria', rotuloAntes: `${e.semanasAntes} semanas antes`, rotuloDepois: 'Depois da adaptação (a partir da 8ª semana)',
                    itens: e.indicadores.map(i => ({ rotulo: i.rotulo, antes: i.antes, depois: i.depois, unidade: i.unidade, melhorSeMenor: i.melhorSeMenor })) })}
                <h3>Redes sociais: conversas abertas por post, por mês</h3>
                ${linhas({ titulo: 'Média mensal de conversas abertas por post no Instagram', rotulos: e.serieRedes.map(s => rotuloMes(s.semana)),
                    series: [{ nome: 'Conversas por post', cor: 2, valores: e.serieRedes.map(s => s.valor) }], formato: fmt.num,
                    marcador: im >= 0 ? { indice: im, rotulo: `Início da Atria` } : null })}
                <h3>Antes e depois: Instagram</h3>
                ${antesDepois({ titulo: 'Engajamento no Instagram, antes e depois da Atria', rotuloAntes: `${e.semanasAntes} semanas antes`, rotuloDepois: 'Depois da adaptação (a partir da 8ª semana)',
                    itens: e.indicadoresRedes.map(i => ({ rotulo: i.rotulo, antes: i.antes, depois: i.depois, unidade: i.unidade, melhorSeMenor: i.melhorSeMenor })) })}
                ${leitura(e.leitura)}`);
        }, 4);

        comEstados(corpo('p-modelos'), api.obterModelos, m => {
            const c = m.conversao, r = m.reclamacao;
            render(corpo('p-modelos'), html`
                <div class="grade-2 grade-2--solta">
                    <div><h3>Quais mensagens responder primeiro</h3>
                        <p class="painel__subtitulo painel__subtitulo--solto">De cada 100 conversas, quantas viram pedido em 7 dias.</p>
                        ${barras({ titulo: 'Conversas que viram pedido: média geral e as indicadas pelo modelo', max: 100, itens: [
                            { rotulo: 'Qualquer conversa', valor: c.taxa_base * 100 },
                            { rotulo: 'Indicadas pelo modelo', valor: c.precisao * 100, destaque: true }], formato: fmt.pct })}
                        ${leitura(`O modelo encontra ${Math.round(c.recall * 10)} de cada 10 conversas que viram pedido. Por isso as mensagens paradas aparecem ordenadas pela chance de venda.`)}</div>
                    <div><h3>O atraso puxa reclamação</h3>
                        <p class="painel__subtitulo painel__subtitulo--solto">De cada 100 pedidos, quantos geram reclamação.</p>
                        ${barras({ titulo: 'Pedidos com reclamação: enviados no prazo e com atraso', max: 100, itens: [
                            { rotulo: 'Enviados no prazo', valor: r.tx_no_prazo * 100 },
                            { rotulo: 'Enviados com atraso', valor: r.tx_atrasado * 100, destaque: true }], formato: fmt.pct })}
                        ${leitura('Prever uma reclamação pedido a pedido é difícil; o que a loja controla é enviar no prazo.')}</div></div>`);
        }, 3);

        comEstados(corpo('p-lgpd'), async () => {
            const [d, ind] = await Promise.all([obterDashboard(), obterIndicadoresRelatorio()]);
            return { d, ind };
        }, ({ d, ind }) => {
            $('#painel-loja', el).textContent = d.nomeLoja;
            render(corpo('p-lgpd'), html`
                <div class="grade-kpi grade-kpi--4">
                    <div class="kpi"><span>Índice de conformidade</span><strong>${ind.indiceConformidade}</strong></div>
                    <div class="kpi"><span>Solicitações em aberto</span><strong>${d.contadores.solicitacoesAbertas}</strong></div>
                    <div class="kpi"><span>Solicitações atrasadas</span><strong>${ind.solicitacoesAtrasadas}</strong></div>
                    <div class="kpi"><span>Acessos indevidos ativos</span><strong>${ind.acessosIndevidosAtivos}</strong></div></div>
                <div class="linha-acoes"><a class="botao botao--pequeno" href="#/riscos">Ver riscos e prazos</a><a class="botao botao--pequeno" href="#/relatorios">Ver relatórios</a></div>`);
        }, 2);

        api.obterMeta().then(meta => {
            const alvo = $('#f-plataforma', el);
            alvo.innerHTML = '';
            meta.plataformas.forEach(p => { const o = document.createElement('option'); o.value = p.codigo; o.textContent = p.nome; alvo.appendChild(o); });
            $('#painel-sub', el).textContent = `O que aconteceu, o que está em risco e o que precisa de você. Dados até ${data(meta.referencia)}.`;
        }).catch(() => undefined);

        const aplicarFiltros = () => { desenharOntem(); desenharCanais(); desenharAtendimento(); desenharClientes(); };
        aplicarFiltros();
        const chips = () => { $('#chips-periodo', el).innerHTML = PERIODOS.map(([v, nome]) =>
            `<button class="chip-filtro ${estado.periodo === v ? 'ativo' : ''}" data-periodo="${v}" aria-pressed="${estado.periodo === v}">${nome}</button>`).join(''); };
        chips();
        on(el, 'click', '[data-periodo]', (e, alvo) => { estado.periodo = Number(alvo.dataset.periodo); chips(); desenharOntem(); });
        $('#f-plataforma', el).addEventListener('change', e => { estado.plataforma = e.target.value; desenharOntem(); desenharClientes(); });
        on(el, 'click', '[data-canal]', (e, alvo) => { estado.canal = alvo.dataset.canal; desenharAtendimento(); });
        on(el, 'click', '[data-ir]', (e, alvo) => { e.preventDefault(); document.getElementById(alvo.dataset.ir)?.scrollIntoView({ behavior: 'smooth', block: 'start' }); });
    },
};
