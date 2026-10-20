import { get, post, query } from '../core/api.js';

export const buscarTitulares = busca => get(`/titulares${query({ busca })}`);
export const obterTitular = id => get(`/titulares/${id}`);
export const obterPreviaExclusao = id => get(`/titulares/${id}/exclusao/previa`);
export const excluirTitular = id => post(`/titulares/${id}/exclusao`, { confirmacao: true });
