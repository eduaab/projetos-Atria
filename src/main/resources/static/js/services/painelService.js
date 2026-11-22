import { get, query } from '../core/api.js';

export const obterMeta = () => get('/painel/meta');
export const obterResumo = (periodo, plataforma) => get(`/painel/resumo${query({ periodo, plataforma })}`);
export const obterSerie = (periodo, plataforma) => get(`/painel/serie${query({ periodo, plataforma })}`);
export const obterCanais = periodo => get(`/painel/canais${query({ periodo })}`);
export const obterAtendimento = (periodo, canal) => get(`/painel/atendimento${query({ periodo, canal })}`);
export const obterPosts = periodo => get(`/painel/posts${query({ periodo })}`);
export const obterClientesPainel = (periodo, plataforma) => get(`/painel/clientes${query({ periodo, plataforma })}`);
export const obterReputacao = () => get('/painel/reputacao');
export const obterPendencias = () => get('/painel/pendencias');
export const obterEfeito = () => get('/painel/efeito-atria');
export const obterModelos = () => get('/painel/modelos');
