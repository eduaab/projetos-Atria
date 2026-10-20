/** Preferências do navegador (localStorage), sempre protegidas contra falhas de acesso. */
const CHAVE_CONEXOES = 'atria.conexoes';

function ler(chave, padrao) {
    try {
        const valor = localStorage.getItem(chave);
        return valor === null ? padrao : JSON.parse(valor);
    } catch {
        return padrao;
    }
}

function salvar(chave, valor) {
    try {
        localStorage.setItem(chave, JSON.stringify(valor));
    } catch {
        // navegador sem armazenamento: o app funciona, só não lembra a preferência
    }
}

/** Onboarding aparece enquanto não houver nenhuma conexão salva no navegador. */
export const conexoesSalvas = () => ler(CHAVE_CONEXOES, []);
export const salvarConexoes = slugs => salvar(CHAVE_CONEXOES, slugs);
