import { get, query } from '../core/api.js';

export const obterResumoMapeamento = () => get('/mapeamento/resumo');
export const listarDadosIdentificados = (filtros = {}) => get(`/mapeamento/dados${query(filtros)}`);
