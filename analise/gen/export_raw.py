"""Suja a 'verdade' (data/core) para criar o staging bruto, como se viesse de exportações diferentes de cada plataforma."""
import numpy as np, pandas as pd
from pathlib import Path
R=Path(__file__).resolve().parent.parent/"data"; C=R/"core"; O=R/"raw"; O.mkdir(exist_ok=True)
rng=np.random.default_rng(777)
ped=pd.read_csv(C/"fato_pedido.csv"); plat=pd.read_csv(C/"dim_plataforma.csv").set_index("plataforma_id")
cli=pd.read_csv(C/"dim_cliente.csv"); conv=pd.read_csv(C/"fato_conversa.csv")
brs=lambda v:"R$ "+f"{v:,.2f}".replace(",","X").replace(".",",").replace("X",".")
# pedidos: cada plataforma exporta de um jeito
nomes={"CANCELADO":{"nuvemshop":"cancelled","mercado-livre":"cancelled","shopee":"CANCELLED","shein":"Cancelado","tiktok-shop":"CANCELLED"},
 "ENTREGUE":{"nuvemshop":"closed","mercado-livre":"delivered","shopee":"COMPLETED","shein":"Entregue","tiktok-shop":"COMPLETED"},
 "DEVOLVIDO":{"nuvemshop":"closed","mercado-livre":"refunded","shopee":"RETURNED","shein":"Devolvido","tiktok-shop":"REFUNDED"},
 "EM_TRANSITO":{"nuvemshop":"paid","mercado-livre":"shipped","shopee":"SHIPPED","shein":"Em trânsito","tiktok-shop":"IN_TRANSIT"},
 "PENDENTE_ENVIO":{"nuvemshop":"paid","mercado-livre":"ready_to_ship","shopee":"READY_TO_SHIP","shein":"Aguardando envio","tiktok-shop":"AWAITING_SHIPMENT"}}
r=ped.copy(); slug=r.plataforma_id.map(plat.slug)
r["status_origem"]=[nomes[s][p] for s,p in zip(r.status,slug)]
r["plataforma_origem"]=slug
for c in ["valor_itens","frete_cobrado","taxa_plataforma","frete_custo","custo_produtos"]:
    r[c]=[brs(v) if p in("shopee","shein") else v for v,p in zip(r[c],slug)]
# fuso: marketplaces exportam em UTC (+3h), loja própria em horário local
dt=pd.to_datetime(r.data_pedido)
r["data_pedido"]=[ (d+pd.Timedelta(hours=3)).strftime("%Y-%m-%dT%H:%M:%SZ") if p!="nuvemshop" else d.strftime("%d/%m/%Y %H:%M:%S") for d,p in zip(dt,slug)]
r=r.drop(columns=["status","plataforma_id","lucro_liquido","valor_reembolsado","custo_devolucao","atrasado","evento"])
# nulos de preenchimento: desconto ausente em 8% das linhas
r.loc[rng.random(len(r))<.08,"desconto_pct"]=np.nan
# duplicatas: reenvio de webhook (1,6%) e linha repetida de exportação (0,4%)
dup=r.sample(frac=.016,random_state=1); dup2=r.sample(frac=.004,random_state=2)
r=pd.concat([r,dup,dup2]).sample(frac=1,random_state=3).reset_index(drop=True)
# pedidos órfãos: cliente que não existe
r.loc[rng.choice(len(r),30,replace=False),"cliente_id"]=999999
r.to_csv(O/"pedidos_export.csv",index=False)
# clientes: nome/e-mail em formatos diferentes por canal, identidade duplicada
c=cli.copy(); c["email"]=[f"cli{i}@mail.com" for i in c.cliente_id]
c["email"]=[e.upper() if rng.random()<.2 else e for e in c.email]
c["cidade"]=[x.lower() if rng.random()<.1 else x for x in c.cidade]
c["telefone"]=[f"(81) 9{rng.integers(1000,9999)}-{rng.integers(1000,9999)}" if rng.random()<.5 else f"81 9{rng.integers(10000000,99999999)}" for _ in range(len(c))]
dup=c[c.omnichannel==True].sample(frac=.5,random_state=4).copy(); dup["cliente_id"]=dup.cliente_id+500000  # mesma pessoa cadastrada de novo em outro canal
dup["email"]=dup.email.str.lower(); 
c=pd.concat([c,dup]).reset_index(drop=True); c.to_csv(O/"clientes_export.csv",index=False)
# conversas: tempo de resposta em segundos e minutos misturados, valores impossíveis
v=conv.copy(); v["tempo_resp_texto"]=[("" if pd.isna(x) else (f"{x:.0f}min" if rng.random()<.6 else f"{x*60:.0f}s")) for x in v.tempo_primeira_resposta_min]
v=v.drop(columns="tempo_primeira_resposta_min")
v.loc[rng.choice(len(v),15,replace=False),"tempo_resp_texto"]="-5min"   # negativos
v.loc[rng.choice(len(v),10,replace=False),"tempo_resp_texto"]="99999min"  # outliers
v.to_csv(O/"conversas_export.csv",index=False)
print({f.name:len(pd.read_csv(f)) for f in O.glob("*.csv")})
