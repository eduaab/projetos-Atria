import { get } from '../core/api.js';

export const obterIndicadoresRisco = () => get('/riscos/indicadores');
export const obterOcorrencias = () => get('/riscos/ocorrencias');
