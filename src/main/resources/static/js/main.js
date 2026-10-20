import { iniciar, registrar } from './core/router.js';
import { html, render } from './core/dom.js';
import { icone } from './components/icons.js';
import home from './pages/home.js';
import clientes from './pages/clientes.js';
import alertas from './pages/alertas.js';
import riscos from './pages/riscos.js';
import conexoes from './pages/conexoes.js';
import mapeamento from './pages/mapeamento.js';
import relatorios from './pages/relatorios.js';

const MENU = [
    { rota: 'home', rotulo: 'Home', icone: 'casa' },
    { rota: 'clientes', rotulo: 'Clientes', icone: 'usuarios' },
    { rota: 'alertas', rotulo: 'Alertas', icone: 'sino' },
    { rota: 'riscos', rotulo: 'Riscos', icone: 'escudo' },
    { rota: 'conexoes', rotulo: 'Conexões', icone: 'conexao' },
    { rota: 'relatorios', rotulo: 'Relatórios', icone: 'documento' },
];

render(document.getElementById('menu'), html`${MENU.map(item => html`
    <li><a class="menu__item" href="#/${item.rota}" data-menu="${item.rota}">${icone(item.icone)}<span>${item.rotulo}</span></a></li>`)}`);

registrar('home', home, 'home');
registrar('clientes', clientes, 'clientes');
registrar('clientes/:id', clientes, 'clientes');
registrar('alertas', alertas, 'alertas');
registrar('alertas/:id', alertas, 'alertas');
registrar('riscos', riscos, 'riscos');
registrar('conexoes', conexoes, 'conexoes');
registrar('conexoes/:slug', conexoes, 'conexoes');
registrar('mapeamento', mapeamento, 'conexoes');
registrar('relatorios', relatorios, 'relatorios');

iniciar(document.getElementById('app'));
