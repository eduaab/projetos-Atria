import { del, get, post } from '../core/api.js';

export const listarPlataformas = () => get('/plataformas');
export const listarConexoes = () => get('/conexoes');
export const obterConexao = slug => get(`/conexoes/${slug}`);
export const conectar = slug => post(`/conexoes/${slug}`);
export const desconectar = slug => del(`/conexoes/${slug}`);
