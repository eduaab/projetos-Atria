import { $, html, on, render } from '../core/dom.js';
import { dataHora } from '../core/format.js';
import { listarPlataformas } from '../services/conexaoService.js';
import { listarDadosIdentificados, obterResumoMapeamento } from '../services/mapeamentoService.js';
import { barrasHorizontais } from '../components/chart.js';
import { estadoVazio } from '../components/emptyState.js';
import { iconePlataforma } from '../components/icons.js';
import { comEstados } from '../components/loading.js';
import { cartaoEstatistica } from '../components/statCard.js';
import { abasConexoes } from '../components/abasConexoes.js';

const TIPOS = [['NOME', 'Nome'], ['CPF', 'CPF'], ['EMAIL', 'E-mail'], ['TELEFONE', 'Telefone'], ['ENDERECO', 'Endereço']];
const MAX_LINKS_TITULARES = 8;

export default {
    mount(el) {
        render(el, html`
            ${abasConexoes('mapeamento')}
            <header class="cabecalho-pagina"><div><h1>Mapeamento de dados</h1>
                <p>Onde estão os dados dos seus clientes. Só contam as plataformas conectadas.</p></div></header>
            <div id="mapa-resumo" class="pagina"></div>
            <section class="painel">
                <div class="painel__cabecalho"><h2>Dados identificados</h2>
                    <div class="barra-filtros">
                        <select class="seletor" id="filtro-plataforma" aria-label="Filtrar por plataforma"><option value="">Todas as plataformas</option></select>
                        <select class="seletor" id="filtro-tipo" aria-label="Filtrar por tipo de dado"><option value="">Todos os tipos</option>
                            ${TIPOS.map(([v, r]) => html`<option value="${v}">${r}</option>`)}</select>
                    </div>
                </div>
                <div id="mapa-tabela"></div>
            </section>`);

        listarPlataformas().then(plataformas => {
            $('#filtro-plataforma', el).insertAdjacentHTML('beforeend', String(html`${plataformas.map(p => html`<option value="${p.slug}">${p.nome}</option>`)}`));
        }).catch(() => {});

        comEstados($('#mapa-resumo', el), obterResumoMapeamento, resumo => render($('#mapa-resumo', el), html`
            <div class="grade-estatisticas">
                ${cartaoEstatistica('Clientes cadastrados', resumo.totalTitulares)}
                ${cartaoEstatistica('Clientes mapeados', resumo.titularesMapeados, 'sucesso')}
                ${cartaoEstatistica('Dados identificados', resumo.totalDados)}
            </div>
            <section class="painel">
                <h2>Clientes por plataforma</h2>
                <div class="grade-estatisticas">
                    ${resumo.clientesPorPlataforma.map(p => html`
                        <a href="#/conexoes/${p.slug}" class="cartao-plataforma ${p.conectada ? '' : 'cartao-plataforma--desconectada'}" style="text-decoration:none;color:inherit">
                            <span class="icone-plataforma">${iconePlataforma(p.slug)}</span>
                            <div><strong>${p.conectada ? p.clientes : '—'}</strong><span>${p.nome}${p.conectada ? '' : ' · não conectada'}</span></div>
                        </a>`)}
                </div>
            </section>
            <div class="grade-2">
                <section class="painel"><h2>Dados por tipo</h2>${barrasHorizontais(resumo.dadosPorTipo)}</section>
                <section class="painel"><h2>Indicadores de possíveis problemas</h2>
                    <div class="lista-linhas">${resumo.indicadores.map(indicador)}</div></section>
            </div>`), 4);

        const carregarTabela = () => comEstados($('#mapa-tabela', el), () => listarDadosIdentificados({
            plataforma: $('#filtro-plataforma', el).value, tipo: $('#filtro-tipo', el).value,
        }), dados => render($('#mapa-tabela', el), dados.length ? html`
            <p class="painel__subtitulo" style="margin-top:0">${dados.length} registro(s)</p>
            <div class="tabela-rolagem" style="max-height:480px;overflow-y:auto"><table class="tabela">
                <thead><tr><th>Cliente</th><th>CPF</th><th>Plataforma</th><th>Tipo de dado</th><th>Encontrado em</th></tr></thead>
                <tbody>${dados.map(d => html`<tr><td><a href="#/clientes/${d.titularId}">${d.titularNome}</a></td><td class="codigo">${d.cpfMascarado}</td>
                    <td>${d.plataformaNome}</td><td>${d.tipoDadoDescricao}</td><td>${dataHora(d.encontradoEm)}</td></tr>`)}</tbody>
            </table></div>` : estadoVazio('Nenhum dado identificado com esses filtros. A plataforma pode estar desconectada.', 'busca')), 4);

        on(el, 'change', '.seletor', carregarTabela);
        carregarTabela();
    },
};

function indicador(i) {
    const links = i.titularIds.slice(0, MAX_LINKS_TITULARES);
    return html`
        <article class="cartao-indicador ${i.severidade === 'ALTA' ? 'cartao-indicador--alta' : ''}">
            <div class="painel__cabecalho"><h3>${i.titulo}</h3><span class="cartao-indicador__numero">${i.quantidade}</span></div>
            <p>${i.descricao}</p>
            ${links.length ? html`<div>${links.map(id => html`<a class="chip" href="#/clientes/${id}">Cliente #${id}</a>`)}
                ${i.titularIds.length > links.length ? html`<span class="chip">+${i.titularIds.length - links.length}</span>` : ''}</div>` : ''}
            ${i.codigo === 'ACESSO_EX_FUNCIONARIO' && i.quantidade > 0 ? html`<a class="botao botao--pequeno" href="#/alertas" style="width:fit-content">Ver alerta e revogar</a>` : ''}
        </article>`;
}
