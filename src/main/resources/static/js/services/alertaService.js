import { get, patch, post, query } from '../core/api.js';

export const listarAlertas = (filtros = {}) => get(`/alertas${query(filtros)}`);
export const obterAlerta = id => get(`/alertas/${id}`);
export const alterarStatusAlerta = (id, status) => patch(`/alertas/${id}/status`, { status });
export const executarVarredura = () => post('/alertas/varredura');
export const revogarAcesso = id => post(`/acessos/${id}/revogacao`);
