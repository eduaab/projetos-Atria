-- Atria / AVD: modelo analítico (estrela simplificada). PostgreSQL 14+.
CREATE SCHEMA IF NOT EXISTS atria_dw;
SET search_path = atria_dw;
CREATE TABLE dim_loja (loja_id int PRIMARY KEY, nome text NOT NULL, cidade text, uf char(2), data_adocao_atria date, grupo text CHECK (grupo IN ('tratada','controle')));
CREATE TABLE dim_plataforma (plataforma_id int PRIMARY KEY, slug text UNIQUE NOT NULL, nome text NOT NULL, tipo text NOT NULL);
CREATE TABLE loja_plataforma (loja_id int REFERENCES dim_loja, plataforma_id int REFERENCES dim_plataforma, conectada_desde date, PRIMARY KEY (loja_id, plataforma_id));
CREATE TABLE dim_produto (produto_id int PRIMARY KEY, loja_id int REFERENCES dim_loja, sku text NOT NULL, nome text, categoria text, preco_base numeric(10,2), custo numeric(10,2));
CREATE TABLE dim_cliente (cliente_id int PRIMARY KEY, loja_id int REFERENCES dim_loja, cidade text, uf char(2), n_canais_compra int, omnichannel boolean, primeira_compra date, cliente_chave char(16) UNIQUE);
CREATE TABLE dim_calendario (dia date PRIMARY KEY, evento text, dia_semana int, semana date, mes char(7));
CREATE TABLE fato_pedido (pedido_id int PRIMARY KEY, loja_id int NOT NULL REFERENCES dim_loja, plataforma_id int NOT NULL REFERENCES dim_plataforma, cliente_id int REFERENCES dim_cliente,
  data_pedido timestamp NOT NULL, status text NOT NULL CHECK (status IN ('PENDENTE_ENVIO','EM_TRANSITO','ENTREGUE','CANCELADO','DEVOLVIDO')),
  valor_itens numeric(12,2) NOT NULL CHECK (valor_itens >= 0), desconto_pct numeric(5,3), frete_cobrado numeric(10,2), taxa_plataforma numeric(10,2), frete_custo numeric(10,2),
  custo_produtos numeric(12,2), valor_reembolsado numeric(12,2), custo_devolucao numeric(10,2), prometido_envio_ate timestamp, data_envio timestamp, data_entrega timestamp,
  data_cancelamento timestamp, atrasado boolean, conversa_id int, categoria_principal text, evento text, qtd_itens int, lucro_liquido numeric(12,2));
CREATE TABLE fato_item_pedido (pedido_id int REFERENCES fato_pedido, produto_id int REFERENCES dim_produto, quantidade int, preco_unitario numeric(10,2), custo_unitario numeric(10,2));
CREATE TABLE fato_reclamacao (reclamacao_id int PRIMARY KEY, pedido_id int REFERENCES fato_pedido, loja_id int REFERENCES dim_loja, tipo text, aberta_em timestamp, resolvida_em timestamp, desfecho text, plataforma_id int REFERENCES dim_plataforma);
CREATE TABLE fato_carrinho_abandonado (carrinho_id int PRIMARY KEY, loja_id int REFERENCES dim_loja, data date, valor numeric(10,2), recuperado boolean, plataforma_id int REFERENCES dim_plataforma);
CREATE TABLE fato_post_instagram (post_id int PRIMARY KEY, loja_id int REFERENCES dim_loja, tipo text, publicado_em timestamp, hora int, dia_semana int, seguidores_na_data int, alcance int, visualizacoes int, curtidas int, comentarios int, salvamentos int, compartilhamentos int, tempo_medio_s numeric(5,1), conversas_48h_atribuidas int, engajamento_total int, taxa_engajamento numeric(7,4), plataforma_id int REFERENCES dim_plataforma);
CREATE TABLE fato_conversa (conversa_id int PRIMARY KEY, loja_id int REFERENCES dim_loja, plataforma_id int REFERENCES dim_plataforma, canal text, iniciada_em timestamp, hora int, dia_semana int, intencao text, primeira_mensagem text, cliente_id int, cliente_recorrente boolean, valor_historico numeric(12,2), origem_post_id int REFERENCES fato_post_instagram, msgs_cliente int, enviou_midia boolean, sem_resposta boolean, tempo_primeira_resposta_min numeric(10,1), pedido_id int, converteu_7d boolean);
CREATE TABLE fato_reputacao_semana (loja_id int REFERENCES dim_loja, plataforma_id int REFERENCES dim_plataforma, semana date, pedidos_4s int, taxa_atraso numeric(6,4), taxa_cancelamento numeric(6,4), taxa_reclamacao numeric(6,4), score numeric(6,4), nivel text, pontos_penalidade int, PRIMARY KEY (loja_id, plataforma_id, semana));
CREATE TABLE fato_pendencia (pendencia_id int PRIMARY KEY, loja_id int REFERENCES dim_loja, tipo text, origem_id int, desde timestamp, resolvida_em timestamp, desfecho text, duracao_h numeric(10,2), prioridade text, plataforma_id int REFERENCES dim_plataforma, monitorada_pela_atria boolean);
CREATE INDEX ix_ped_loja_data ON fato_pedido (loja_id, data_pedido);
CREATE INDEX ix_conv_loja_data ON fato_conversa (loja_id, iniciada_em);
CREATE INDEX ix_pend_loja_tipo ON fato_pendencia (loja_id, tipo, desde);
CREATE VIEW vw_margem_canal AS SELECT loja_id, plataforma_id, date_trunc('month', data_pedido)::date AS mes, count(*) AS pedidos, sum(valor_itens) AS receita, sum(lucro_liquido) AS lucro FROM fato_pedido WHERE status <> 'CANCELADO' GROUP BY 1,2,3;
