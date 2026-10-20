const FORMATO_DATA = new Intl.DateTimeFormat('pt-BR');
const FORMATO_DATA_HORA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const FORMATO_MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/** Converte "2026-10-09" ou "2026-10-09T10:00:00" em Date no fuso local. */
export function paraData(iso) {
    return new Date(iso.length === 10 ? `${iso}T00:00:00` : iso);
}

export const data = iso => (iso ? FORMATO_DATA.format(paraData(iso)) : '-');
export const dataHora = iso => (iso ? FORMATO_DATA_HORA.format(paraData(iso)) : '-');
export const moeda = valor => FORMATO_MOEDA.format(valor || 0);
export const plural = (quantidade, singular, pluralTexto) => `${quantidade} ${quantidade === 1 ? singular : pluralTexto}`;

export function textoPrazo(dias) {
    if (dias === null || dias === undefined) return '';
    if (dias < 0) return `Vencido há ${plural(-dias, 'dia', 'dias')}`;
    if (dias === 0) return 'Vence hoje';
    return `Prazo: ${plural(dias, 'dia', 'dias')}`;
}

export function tempoRelativo(iso) {
    const minutos = Math.round((Date.now() - paraData(iso).getTime()) / 60000);
    if (minutos < 60) return `há ${plural(Math.max(minutos, 1), 'minuto', 'minutos')}`;
    const horas = Math.round(minutos / 60);
    if (horas < 48) return `há ${horas} h`;
    return `há ${Math.round(horas / 24)} dias`;
}

export function chaveDia(date) {
    const mes = String(date.getMonth() + 1).padStart(2, '0');
    const dia = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${mes}-${dia}`;
}
