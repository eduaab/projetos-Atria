"""
Etapa 2 do gerador: transforma o estado simulado em tabelas do modelo (core limpo, "verdade" da simulação).
Depois, export_raw.py suja essa verdade para criar o staging; prep.py faz o caminho de volta.
"""
import hashlib
import pickle
import sys
from datetime import datetime, timedelta
from pathlib import Path

import numpy as np
import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_world as W  # noqa: E402  (só reaproveita constantes e funções de calendário)

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "data" / "core"
OUT.mkdir(parents=True, exist_ok=True)
rng = np.random.default_rng(W.SEED + 1)
END_TS = datetime(W.END.year, W.END.month, W.END.day, 23, 59, 59)

est = pickle.load(open(ROOT / "data" / "_estado.pkl", "rb"))
pedidos = pd.DataFrame(est["pedidos"])
itens = pd.DataFrame(est["itens"], columns=["pedido_id", "produto_id", "quantidade", "preco_unitario", "custo_unitario"])
conversas = pd.DataFrame(est["conversas"])
posts = pd.DataFrame(est["posts"])
carrinhos = pd.DataFrame(est["carrinhos"])
reput = pd.DataFrame(est["reputacao"])
clientes_estado = est["clientes"]
pedidos = pedidos.rename(columns={c:"x"+c for c in ["_entrega","_envio","_cancel_ts","_recl_flag","_omni","_af","_espera_envio"]})

PID = W.PID
# --------------------------------------------------------------------------- clientes
linhas = []
for loja, c in clientes_estado.items():
    for i, cid in enumerate(c["ids"]):
        linhas.append((cid, loja, i, c["cidade"][i], W.UF[loja], len(c["mask"][i]), c["omni"][i]))
cli = pd.DataFrame(linhas, columns=["cliente_id", "loja_id", "idx", "cidade", "uf", "n_canais_compra", "omnichannel"])
mapa = {(r.loja_id, r.idx): r.cliente_id for r in cli.itertuples()}
pedidos["cliente_id"] = [mapa[(l, i)] for l, i in zip(pedidos.loja_id, pedidos.cliente_idx)]
conversas["cliente_id"] = [mapa.get((l, i)) if i == i and i is not None else None for l, i in zip(conversas.loja_id, conversas.cliente_idx)]
conversas["cliente_id"] = conversas["cliente_id"].astype("Int64")
primeira = pedidos.groupby("cliente_id").data_pedido.min().dt.date
cli["primeira_compra"] = cli.cliente_id.map(primeira)
cli = cli[cli.primeira_compra.notna()].copy()
SAL = "atria-simulacao-2026"
cli["cliente_chave"] = [hashlib.sha256(f"{SAL}{c}".encode()).hexdigest()[:16] for c in cli.cliente_id]
cli = cli.drop(columns="idx")

# --------------------------------------------------------------------------- reclamações
rec = []
for p in pedidos[pedidos.x_recl_flag].itertuples():
    aberta = p.x_entrega + timedelta(days=float(rng.uniform(0.5, 9)))
    if aberta > END_TS:
        continue
    af = W.fator_atria(p.loja_id, aberta.date())
    horas = 60 * (1 - 0.40 * af) * float(np.exp(rng.normal(0, 0.7)))
    resolvida = aberta + timedelta(hours=horas)
    tipo = str(rng.choice(["troca", "devolucao", "produto_diferente_ou_defeito"], p=[.45, .35, .20]))
    desfecho = str(rng.choice(["reembolso", "troca", "improcedente"], p={"troca": [.05, .90, .05], "devolucao": [.92, .03, .05],
                                                                      "produto_diferente_ou_defeito": [.55, .35, .10]}[tipo]))
    rec.append(dict(reclamacao_id=len(rec) + 1, pedido_id=p.pedido_id, loja_id=p.loja_id, plataforma=p.plataforma, tipo=tipo,
                    aberta_em=aberta, resolvida_em=resolvida if resolvida <= END_TS else None,
                    desfecho=desfecho if resolvida <= END_TS else None))
rec = pd.DataFrame(rec)
recl_ped = rec.set_index("pedido_id")

# --------------------------------------------------------------------------- pedidos finais
def status(r):
    if r.cancelado:
        return "CANCELADO"
    if r.pedido_id in recl_ped.index:
        x = recl_ped.loc[r.pedido_id]
        if x.desfecho == "reembolso" and pd.notna(x.resolvida_em):
            return "DEVOLVIDO"
    if r.x_entrega <= END_TS:
        return "ENTREGUE"
    if r.x_envio <= END_TS:
        return "EM_TRANSITO"
    return "PENDENTE_ENVIO"


pedidos["status"] = pedidos.apply(status, axis=1)
pedidos["data_envio"] = [e if (e <= END_TS and not c) else pd.NaT for e, c in zip(pedidos.x_envio, pedidos.cancelado)]
pedidos["data_entrega"] = [e if (e <= END_TS and not c) else pd.NaT for e, c in zip(pedidos.x_entrega, pedidos.cancelado)]
pedidos["data_cancelamento"] = pedidos.x_cancel_ts
pedidos["plataforma_id"] = pedidos.plataforma.map(PID)
# valor reembolsado e custo de devolução
reemb = recl_ped[(recl_ped.desfecho == "reembolso")].index
pedidos["valor_reembolsado"] = np.where(pedidos.pedido_id.isin(reemb) & (pedidos.status == "DEVOLVIDO"), pedidos.valor_itens, 0.0)
pedidos["custo_devolucao"] = np.where(pedidos.valor_reembolsado > 0, pedidos.frete_custo + 0.3 * pedidos.custo_produtos, 0.0).round(2)
# pedido cancelado: não há receita, nem custo de produto, nem frete (cancelou antes do envio); taxa não é cobrada
cancel = pedidos.status == "CANCELADO"
pedidos["receita_efetiva"] = np.where(cancel, 0.0, pedidos.valor_itens - pedidos.valor_reembolsado).round(2)
pedidos["atrasado"] = pedidos.atrasado.astype(int)
pedidos["cancelado"] = pedidos.cancelado.astype(int)
pedidos["conversa_id"] = pedidos.conversa_id.astype("Int64")
fato_pedido = pedidos[["pedido_id", "loja_id", "plataforma_id", "cliente_id", "data_pedido", "status", "valor_itens", "desconto_pct",
                       "frete_cobrado", "taxa_plataforma", "frete_custo", "custo_produtos", "valor_reembolsado", "custo_devolucao",
                       "prometido_envio_ate", "data_envio", "data_entrega", "data_cancelamento", "atrasado", "conversa_id",
                       "categoria_principal", "evento", "qtd_itens"]].copy()
# taxa e frete não existem para pedido cancelado
fato_pedido.loc[cancel, ["taxa_plataforma", "frete_custo", "frete_cobrado"]] = 0.0
fato_pedido.loc[cancel, "custo_produtos"] = 0.0
fato_pedido["lucro_liquido"] = (fato_pedido.valor_itens - fato_pedido.valor_reembolsado + fato_pedido.frete_cobrado
                                - fato_pedido.custo_produtos - fato_pedido.taxa_plataforma - fato_pedido.frete_custo
                                - fato_pedido.custo_devolucao).round(2)
fato_pedido.loc[cancel, "valor_itens"] = pedidos.loc[cancel, "valor_itens"]  # valor do pedido cancelado fica visível
fato_pedido.loc[cancel, "lucro_liquido"] = 0.0
# produtos devolvidos voltam ao estoque em 70% (já considerado em custo_devolucao)

# --------------------------------------------------------------------------- conversas finais
ped_conv = pedidos.dropna(subset=["conversa_id"]).set_index("conversa_id")
conversas["pedido_id"] = conversas.conversa_id.map(ped_conv.pedido_id).astype("Int64")
t_ped = conversas.conversa_id.map(ped_conv.data_pedido)
conversas["_dias_ate_pedido"] = (t_ped - conversas.iniciada_em).dt.total_seconds() / 86400
conversas["converteu_7d"] = np.where(conversas._dias_ate_pedido.notna() & (conversas._dias_ate_pedido <= 7), 1, 0)
censurada = conversas.iniciada_em > (END_TS - timedelta(days=7))
conversas["converteu_7d"] = conversas.converteu_7d.astype("Int64")
conversas.loc[censurada, "converteu_7d"] = pd.NA
conversas["plataforma_id"] = conversas.canal.map(PID)
conv_pub = conversas[["conversa_id", "loja_id", "plataforma_id", "canal", "iniciada_em", "hora", "dia_semana", "intencao",
                      "primeira_mensagem", "cliente_id", "cliente_recorrente", "valor_historico", "origem_post_id", "msgs_cliente",
                      "enviou_midia", "sem_resposta", "tempo_primeira_resposta_min", "pedido_id", "converteu_7d"]]
verdade = conversas[["conversa_id", "_prob_real"]].rename(columns={"_prob_real": "prob_real"})

# --------------------------------------------------------------------------- posts finais
cont = conversas.dropna(subset=["origem_post_id"]).groupby("origem_post_id").size()
posts["conversas_48h_atribuidas"] = posts.post_id.map(cont).fillna(0).astype(int)
posts["plataforma_id"] = PID["instagram"]
posts["engajamento_total"] = posts.curtidas + posts.comentarios + posts.salvamentos + posts.compartilhamentos
posts["taxa_engajamento"] = (posts.engajamento_total / posts.alcance).round(4)

# --------------------------------------------------------------------------- pendências (o que a Atria monitora)
LIMIAR = {"MENSAGEM_SEM_RESPOSTA": 1.0, "PEDIDO_NAO_ENVIADO": 12.0, "RECLAMACAO_PENDENTE": 6.0}


def prioridade(tipo, horas):
    if tipo == "RECLAMACAO_PENDENTE":
        return "ALTA" if horas > 24 else "BAIXA"
    if tipo == "MENSAGEM_SEM_RESPOSTA":
        return "MEDIA" if horas > 4 else "BAIXA"
    return "MEDIA" if horas > 24 else "BAIXA"


pend = []
# mensagens: espera > 1 h (sem resposta expira em 72 h)
for r in conversas.itertuples():
    desde = r.iniciada_em
    if r.sem_resposta:
        fim, desfecho = desde + timedelta(hours=72), "expirada"
    else:
        fim, desfecho = desde + timedelta(minutes=float(r.tempo_primeira_resposta_min)), "respondida"
    if fim > END_TS:
        fim, desfecho = None, "aberta"
    horas = ((fim or END_TS) - desde).total_seconds() / 3600
    if horas > LIMIAR["MENSAGEM_SEM_RESPOSTA"]:
        pend.append((r.loja_id, r.canal, "MENSAGEM_SEM_RESPOSTA", r.conversa_id, desde, fim, desfecho, horas))
for r in pedidos.itertuples():
    desde = r.data_pedido
    if r.cancelado:
        fim, desfecho = r.x_cancel_ts, "cancelado"
    elif r.x_envio <= END_TS:
        fim, desfecho = r.x_envio, "enviado"
    else:
        fim, desfecho = None, "aberta"
    horas = ((fim or END_TS) - desde).total_seconds() / 3600
    if horas > LIMIAR["PEDIDO_NAO_ENVIADO"]:
        pend.append((r.loja_id, r.plataforma, "PEDIDO_NAO_ENVIADO", r.pedido_id, desde, fim, desfecho, horas))
for r in rec.itertuples():
    desde = r.aberta_em
    fim = r.resolvida_em if pd.notna(r.resolvida_em) else None
    horas = ((fim or END_TS) - desde).total_seconds() / 3600
    if horas > LIMIAR["RECLAMACAO_PENDENTE"]:
        pend.append((r.loja_id, r.plataforma, "RECLAMACAO_PENDENTE", r.reclamacao_id, desde, fim, "resolvida" if fim else "aberta", horas))
fato_pend = pd.DataFrame(pend, columns=["loja_id", "plataforma", "tipo", "origem_id", "desde", "resolvida_em", "desfecho", "duracao_h"])
fato_pend["prioridade"] = [prioridade(t, h) for t, h in zip(fato_pend.tipo, fato_pend.duracao_h)]
fato_pend["plataforma_id"] = fato_pend.plataforma.map(PID)
adoc = {l: a for l, _, _, _, a in W.LOJAS}
fato_pend["monitorada_pela_atria"] = [int(adoc[l] is not None and d.date() >= adoc[l]) for l, d in zip(fato_pend.loja_id, fato_pend.desde)]
fato_pend["duracao_h"] = fato_pend.duracao_h.round(2)
fato_pend = fato_pend.sort_values("desde").reset_index(drop=True)
fato_pend.insert(0, "pendencia_id", np.arange(1, len(fato_pend) + 1))
fato_pend = fato_pend.drop(columns="plataforma")

# --------------------------------------------------------------------------- reputação, carrinhos, dimensões
reput["plataforma_id"] = reput.plataforma.map(PID)
reput = reput.drop(columns="plataforma")
carrinhos["plataforma_id"] = carrinhos.plataforma.map(PID)
carrinhos = carrinhos.drop(columns="plataforma")
carrinhos.insert(0, "carrinho_id", np.arange(1, len(carrinhos) + 1))
rec["plataforma_id"] = rec.plataforma.map(PID)
rec = rec.drop(columns="plataforma")

dim_loja = pd.DataFrame([(i, n, c, u, a, "tratada" if a else "controle") for i, n, c, u, a in W.LOJAS],
                        columns=["loja_id", "nome", "cidade", "uf", "data_adocao_atria", "grupo"])
dim_plat = pd.DataFrame(W.PLATAFORMAS, columns=["plataforma_id", "slug", "nome", "tipo"])
lp = []
for loja, plats in W.DEMANDA.items():
    for p in list(plats) + ["whatsapp", "instagram"]:
        lp.append((loja, PID[p], W.START))
loja_plat = pd.DataFrame(lp, columns=["loja_id", "plataforma_id", "conectada_desde"])
dim_prod = W.PROD.drop(columns="popularidade")
cal = pd.DataFrame({"dia": pd.date_range(W.START, W.END)})
cal["evento"] = [W.evento_do_dia(d.date()) for d in cal.dia]
cal["dia_semana"] = cal.dia.dt.weekday
cal["semana"] = (cal.dia - pd.to_timedelta(cal.dia.dt.weekday, unit="D")).dt.date
cal["mes"] = cal.dia.dt.strftime("%Y-%m")

tabelas = dict(dim_loja=dim_loja, dim_plataforma=dim_plat, loja_plataforma=loja_plat, dim_produto=dim_prod, dim_cliente=cli,
               dim_calendario=cal, fato_pedido=fato_pedido, fato_item_pedido=itens, fato_reclamacao=rec,
               fato_carrinho_abandonado=carrinhos, fato_conversa=conv_pub,
               fato_post_instagram=posts.drop(columns=["plataforma_id"]).assign(plataforma_id=PID["instagram"]),
               fato_reputacao_semana=reput, fato_pendencia=fato_pend)
for nome, df in tabelas.items():
    df.to_csv(OUT / f"{nome}.csv", index=False)
verdade.to_csv(OUT.parent / "_verdade_simulacao_conversas.csv", index=False)
print({k: len(v) for k, v in tabelas.items()})
