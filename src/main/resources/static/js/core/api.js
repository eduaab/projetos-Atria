const BASE = '/api';

export class ErroApi extends Error {
    constructor(mensagem, status) {
        super(mensagem);
        this.status = status;
    }
}

async function enviar(caminho, opcoes = {}) {
    try {
        return await fetch(BASE + caminho, opcoes);
    } catch {
        throw new ErroApi('Não foi possível falar com o servidor. Verifique se a Atria está rodando.', 0);
    }
}

async function exigirSucesso(resposta) {
    if (resposta.ok) return resposta;
    let mensagem = `Erro ${resposta.status}`;
    try {
        mensagem = (await resposta.json()).mensagem || mensagem;
    } catch {
        // corpo sem JSON: mantém a mensagem padrão
    }
    throw new ErroApi(mensagem, resposta.status);
}

async function requisitar(metodo, caminho, corpo) {
    const opcoes = { method: metodo, headers: { Accept: 'application/json' } };
    if (corpo !== undefined) {
        opcoes.headers['Content-Type'] = 'application/json';
        opcoes.body = JSON.stringify(corpo);
    }
    const resposta = await exigirSucesso(await enviar(caminho, opcoes));
    return resposta.status === 204 ? null : resposta.json();
}

export const get = caminho => requisitar('GET', caminho);
export const post = (caminho, corpo) => requisitar('POST', caminho, corpo);
export const patch = (caminho, corpo) => requisitar('PATCH', caminho, corpo);
export const del = caminho => requisitar('DELETE', caminho);

/** Monta "?a=1&b=2" ignorando valores vazios. */
export function query(parametros) {
    const busca = new URLSearchParams();
    Object.entries(parametros).forEach(([chave, valor]) => {
        if (valor !== undefined && valor !== null && valor !== '') busca.set(chave, valor);
    });
    const texto = busca.toString();
    return texto ? `?${texto}` : '';
}

/** Baixa um arquivo (PDF) usando o nome enviado no Content-Disposition. */
export async function baixar(caminho) {
    const resposta = await exigirSucesso(await enviar(caminho));
    const disposicao = resposta.headers.get('Content-Disposition') || '';
    const nome = /filename="?([^";]+)"?/.exec(disposicao)?.[1] || 'atria.pdf';
    const url = URL.createObjectURL(await resposta.blob());
    const link = document.createElement('a');
    link.href = url;
    link.download = nome;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    return nome;
}
