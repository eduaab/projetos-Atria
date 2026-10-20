const rotas = [];

/** Registra uma rota por hash. Segmentos com ":" viram parâmetros: "clientes/:id" casa com "#/clientes/12". */
export function registrar(padrao, pagina, menu) {
    rotas.push({ partes: padrao.split('/'), pagina, menu });
}

export function navegar(caminho) {
    location.hash = caminho.startsWith('#') ? caminho : `#/${caminho}`;
}

function resolver(hash) {
    const segmentos = hash.replace(/^#\/?/, '').split('/').filter(Boolean);
    for (const rota of rotas) {
        if (rota.partes.length !== segmentos.length) continue;
        const parametros = {};
        const casou = rota.partes.every((parte, i) => {
            if (parte.startsWith(':')) {
                parametros[parte.slice(1)] = decodeURIComponent(segmentos[i]);
                return true;
            }
            return parte === segmentos[i];
        });
        if (casou) return { rota, parametros };
    }
    return null;
}

function marcarMenu(menu) {
    document.querySelectorAll('[data-menu]').forEach(item => {
        const ativo = item.dataset.menu === menu;
        item.classList.toggle('ativo', ativo);
        if (ativo) item.setAttribute('aria-current', 'page');
        else item.removeAttribute('aria-current');
    });
}

export function iniciar(app) {
    const aplicar = () => {
        const resultado = resolver(location.hash);
        if (!resultado) {
            history.replaceState(null, '', '#/home');
            return aplicar();
        }
        marcarMenu(resultado.rota.menu);
        const container = document.createElement('div');
        container.className = 'pagina';
        app.replaceChildren(container);
        app.scrollTop = 0;
        resultado.rota.pagina.mount(container, resultado.parametros);
    };
    window.addEventListener('hashchange', aplicar);
    aplicar();
}
