import { get, patch, post, query } from '../core/api.js';

export const listarSolicitacoes = (filtros = {}) => get(`/solicitacoes${query(filtros)}`);
export const registrarSolicitacao = dados => post('/solicitacoes', dados);
export const alterarStatusSolicitacao = (id, status) => patch(`/solicitacoes/${id}/status`, { status });
