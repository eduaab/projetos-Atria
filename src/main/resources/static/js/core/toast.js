const DURACAO_MS = 4000;

/** Mensagem rápida no canto da tela. tipo: 'info' | 'sucesso' | 'erro'. */
export function toast(mensagem, tipo = 'info') {
    const elemento = document.createElement('div');
    elemento.className = `toast toast--${tipo}`;
    elemento.setAttribute('role', tipo === 'erro' ? 'alert' : 'status');
    elemento.textContent = mensagem;
    document.getElementById('toasts').appendChild(elemento);
    setTimeout(() => elemento.remove(), DURACAO_MS);
}
