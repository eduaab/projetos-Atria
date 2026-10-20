import { baixar, get, query } from '../core/api.js';

export const obterIndicadoresRelatorio = () => get('/relatorios/indicadores');
export const baixarRelatorio = tipo => baixar(`/relatorios/${tipo}`);
export const listarEvidencias = () => get('/evidencias');
export const baixarComprovante = protocolo => baixar(`/evidencias/${encodeURIComponent(protocolo)}/comprovante`);
export const listarAuditoria = limite => get(`/auditoria${query({ limite })}`);
