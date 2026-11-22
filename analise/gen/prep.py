"""Preparação: staging bruto -> dados limpos, com log de cada passo (entrega 2)."""
import re, json, numpy as np, pandas as pd
from pathlib import Path
R=Path(__file__).resolve().parent.parent; RAW=R/"data/raw"; CLN=R/"data/clean"; CORE=R/"data/core"; DOCS=R/"docs"
log=[]
def L(tabela,passo,antes,depois,motivo): log.append(dict(tabela=tabela,passo=passo,linhas_antes=antes,linhas_depois=depois,afetadas=antes-depois if antes!=depois else None,decisao=motivo))
# ---------- pedidos
p=pd.read_csv(RAW/"pedidos_export.csv"); n0=len(p)
p=p.drop_duplicates(); L("pedidos","remover linhas 100% repetidas (exportação duplicada)",n0,len(p),"linha idêntica não carrega informação nova")
n=len(p); p=p.drop_duplicates("pedido_id",keep="first"); L("pedidos","remover pedido_id repetido (reenvio de webhook)",n,len(p),"mantém a primeira ocorrência; o reenvio repete o mesmo pedido")
def num(v):
    if isinstance(v,str) and "R$" in v:
        return float(v.replace("R$","").replace(".","").replace(",",".").strip())
    return float(v)
na=0
for c in ["valor_itens","frete_cobrado","taxa_plataforma","frete_custo","custo_produtos"]:
    na+=p[c].map(lambda v:isinstance(v,str) and "R$" in v).sum(); p[c]=p[c].map(num)
L("pedidos","converter 'R$ 1.234,56' (Shopee/Shein) para número",len(p),len(p),f"{na} valores de texto convertidos")
mp={"cancelled":"CANCELADO","CANCELLED":"CANCELADO","Cancelado":"CANCELADO","closed":"ENTREGUE","delivered":"ENTREGUE","COMPLETED":"ENTREGUE","Entregue":"ENTREGUE",
 "refunded":"DEVOLVIDO","RETURNED":"DEVOLVIDO","Devolvido":"DEVOLVIDO","REFUNDED":"DEVOLVIDO","paid":"EM_TRANSITO","shipped":"EM_TRANSITO","SHIPPED":"EM_TRANSITO","Em trânsito":"EM_TRANSITO","IN_TRANSIT":"EM_TRANSITO",
 "ready_to_ship":"PENDENTE_ENVIO","READY_TO_SHIP":"PENDENTE_ENVIO","Aguardando envio":"PENDENTE_ENVIO","AWAITING_SHIPMENT":"PENDENTE_ENVIO"}
p["status"]=p.status_origem.map(mp); assert p.status.notna().all()
L("pedidos","padronizar status de 5 plataformas em 5 estados da Atria",len(p),len(p),f"{p.status_origem.nunique()} nomes -> {p.status.nunique()} estados")
# fuso: marketplaces em UTC (-3h para Fortaleza/Recife), Nuvemshop já local
def hora(s,pl):
    if pl=="nuvemshop": return pd.to_datetime(s,format="%d/%m/%Y %H:%M:%S")
    return pd.to_datetime(s,format="%Y-%m-%dT%H:%M:%SZ")-pd.Timedelta(hours=3)
p["data_pedido"]=[hora(s,pl) for s,pl in zip(p.data_pedido,p.plataforma_origem)]
for c in ["prometido_envio_ate","data_envio","data_entrega","data_cancelamento"]: p[c]=pd.to_datetime(p[c])
L("pedidos","converter datas de UTC para horário local e unificar formato",len(p),len(p),"marketplaces exportam UTC; Nuvemshop em horário local")
nn=p.desconto_pct.isna().sum(); p["desconto_pct"]=p.desconto_pct.fillna(0); L("pedidos","desconto ausente -> 0",len(p),len(p),f"{nn} nulos; ausência de campo significa sem desconto")
# ---------- clientes: unificação de identidade
c=pd.read_csv(RAW/"clientes_export.csv"); n0=len(c)
c["email"]=c.email.str.lower().str.strip(); c["cidade"]=c.cidade.str.title()
c["telefone"]=c.telefone.map(lambda t:re.sub(r"\D","",t)); L("clientes","normalizar e-mail (minúsculas), cidade (Title) e telefone (só dígitos)",n0,len(c),"base para unificar identidade")
c["_k"]=c.cliente_id%500000
c=c.sort_values("cliente_id").drop_duplicates("_k",keep="first"); L("clientes","unificar mesma pessoa cadastrada em dois canais (omnichannel)",n0,len(c),"IDs +500000 eram a mesma pessoa; mantém o ID original")
c=c.drop(columns="_k")
ids=set(c.cliente_id); n=len(p); orf=~p.cliente_id.isin(ids); p=p[~orf]; L("pedidos","remover pedidos com cliente inexistente",n,len(p),"cliente_id sem cadastro: não dá para analisar o comportamento do cliente")
# ---------- conversas
v=pd.read_csv(RAW/"conversas_export.csv")
def mins(t):
    if pd.isna(t) or t=="": return np.nan
    m=re.match(r"(-?\d+)(min|s)",t); x=float(m.group(1)); return x/60 if m.group(2)=="s" else x
v["tempo_primeira_resposta_min"]=v.tempo_resp_texto.map(mins); L("conversas","converter '12min' e '720s' para minutos",len(v),len(v),"unidade mista")
bad=(v.tempo_primeira_resposta_min<0)|(v.tempo_primeira_resposta_min>14400)
v.loc[bad,"tempo_primeira_resposta_min"]=np.nan; L("conversas","tempos negativos ou acima de 10 dias viram nulo",len(v),len(v),f"{int(bad.sum())} valores impossíveis (relógio errado ou fila travada)")
v=v.drop(columns="tempo_resp_texto")
# ---------- verificações de sanidade
chk={}
chk["pedido_id único"]=bool(p.pedido_id.is_unique)
chk["valores negativos"]=int((p[["valor_itens","taxa_plataforma","frete_custo"]]<0).sum().sum())
chk["envio antes do pedido"]=int((p.data_envio<p.data_pedido).sum())
chk["entrega antes do envio"]=int((p.data_entrega<p.data_envio).sum())
chk["cliente órfão"]=int((~p.cliente_id.isin(set(c.cliente_id))).sum())
core=pd.read_csv(CORE/"fato_pedido.csv"); chk["pedidos perdidos por cliente órfão (injetados)"]=int(len(core)-len(p))
chk["receita limpa vs verdade (diferença R$)"]=round(float(p.valor_itens.sum()-core.valor_itens.sum()),2)
p.to_csv(CLN/"pedidos.csv",index=False); c.to_csv(CLN/"clientes.csv",index=False); v.to_csv(CLN/"conversas.csv",index=False)
pd.DataFrame(log).to_csv(DOCS/"log_preparacao.csv",index=False); json.dump(chk,open(DOCS/"checagens_qualidade.json","w"),ensure_ascii=False,indent=1,default=str)
print(pd.DataFrame(log)[["tabela","passo","linhas_antes","linhas_depois"]].to_string()); print(json.dumps(chk,ensure_ascii=False,indent=1,default=str))
