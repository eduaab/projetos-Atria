"""Exporta agregados para o painel embutido na Atria (JSON em src/main/resources/analise)."""
import json, sys, numpy as np, pandas as pd
from pathlib import Path
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import make_pipeline
from sklearn.metrics import roc_curve, precision_recall_curve, roc_auc_score, average_precision_score, confusion_matrix, accuracy_score, precision_score, recall_score, f1_score
import statsmodels.formula.api as smf
R=Path(__file__).resolve().parent.parent; CD=R/"data/core"
OUT=Path(sys.argv[1]) if len(sys.argv)>1 else R/"out/analise"; OUT.mkdir(parents=True,exist_ok=True)
LOJA=1; END=pd.Timestamp("2026-11-20 23:59:59"); REF="2026-11-20"
plat=pd.read_csv(CD/"dim_plataforma.csv"); SLUG=plat.set_index("plataforma_id").slug.to_dict(); NOME=plat.set_index("slug").nome.to_dict()
ped=pd.read_csv(CD/"fato_pedido.csv",parse_dates=["data_pedido","data_envio","prometido_envio_ate"]); ped["slug"]=ped.plataforma_id.map(SLUG)
conv=pd.read_csv(CD/"fato_conversa.csv",parse_dates=["iniciada_em"]); post=pd.read_csv(CD/"fato_post_instagram.csv",parse_dates=["publicado_em"])
rec=pd.read_csv(CD/"fato_reclamacao.csv",parse_dates=["aberta_em","resolvida_em"]); loja=pd.read_csv(CD/"dim_loja.csv",parse_dates=["data_adocao_atria"]); cli=pd.read_csv(CD/"dim_cliente.csv",parse_dates=["primeira_compra"])
rep=pd.read_csv(CD/"fato_reputacao_semana.csv"); pend=pd.read_csv(CD/"fato_pendencia.csv",parse_dates=["desde","resolvida_em"])
def dump(nome,obj): (OUT/f"{nome}.json").write_text(json.dumps(obj,ensure_ascii=False,separators=(",",":"),default=float),encoding="utf-8")
FAIXAS=[15,60,240,1440]
def faixa(m): return np.searchsorted(FAIXAS,m,side="left")  # 0:<=15,1:<=60,2:<=240,3:<=1440,4:>1440
# ---- 1 diário (loja 1)
p=ped[ped.loja_id==LOJA].copy(); p["dia"]=p.data_pedido.dt.strftime("%Y-%m-%d")
recp=set(rec.pedido_id); p["recl"]=p.pedido_id.isin(recp)
p["ativo"]=p.status!="CANCELADO"
g=p.groupby(["dia","slug"]).apply(lambda d: pd.Series(dict(n=len(d),rec=round(d.valor_itens[d.ativo].sum(),2),luc=round(d.lucro_liquido.sum(),2),can=int((~d.ativo).sum()),atr=int(d.atrasado.sum()),rcl=int(d.recl.sum()),dev=int((d.status=="DEVOLVIDO").sum()))),include_groups=False).reset_index()
dump("diario",[dict(d=r.dia,p=r.slug,n=int(r.n),rec=r.rec,luc=r.luc,can=int(r.can),atr=int(r.atr),rcl=int(r.rcl),dev=int(r.dev)) for r in g.itertuples()])
# ---- 2 atendimento (loja 1) por dia×canal
c=conv[conv.loja_id==LOJA].copy(); c["dia"]=c.iniciada_em.dt.strftime("%Y-%m-%d")
rows=[]
for (dia,canal),d in c.groupby(["dia","canal"]):
    r=d[d.sem_resposta==0]; f=np.bincount(faixa(r.tempo_primeira_resposta_min.values),minlength=5) if len(r) else np.zeros(5,int)
    k=d[d.converteu_7d.notna() & (d.sem_resposta==0)]; kf=faixa(k.tempo_primeira_resposta_min.values) if len(k) else np.array([],int)
    kk=np.bincount(kf,minlength=5); vv=np.bincount(kf,weights=k.converteu_7d.astype(float).values,minlength=5) if len(k) else np.zeros(5)
    rows.append(dict(d=dia,c=canal,n=len(d),sr=int(d.sem_resposta.sum()),f=f.tolist(),k=kk.tolist(),v=[int(x) for x in vv],mi=round(float(r.tempo_primeira_resposta_min.sum()),0)))
dump("atendimento",rows)
# ---- 3 posts
q=post[post.loja_id==LOJA]; dump("posts",[dict(id=int(r.post_id),t=r.tipo,d=r.publicado_em.strftime("%Y-%m-%d"),h=int(r.hora),al=int(r.alcance),cu=int(r.curtidas),co=int(r.comentarios),sa=int(r.salvamentos),cp=int(r.compartilhamentos),cv=int(r.conversas_48h_atribuidas)) for r in q.itertuples()])
# ---- 4 reputação
rp=rep[rep.loja_id==LOJA].copy(); rp["slug"]=rp.plataforma_id.map(SLUG)
dump("reputacao",[dict(p=r.slug,s=r.semana,n=int(r.pedidos_4s),a=r.taxa_atraso,c=r.taxa_cancelamento,r=r.taxa_reclamacao,sc=r.score,nv=r.nivel,pp=int(r.pontos_penalidade)) for r in rp.itertuples()])
# ---- 5 clientes (pseudônimos)
pc=ped[(ped.loja_id==LOJA)&(ped.status!="CANCELADO")]
agg=pc.groupby("cliente_id").agg(n=("pedido_id","size"),g=("valor_itens","sum"),pri=("data_pedido","min"),ult=("data_pedido","max"),canais=("slug","nunique")).reset_index()
agg=agg.merge(cli[["cliente_id","cidade","omnichannel"]],on="cliente_id")
dump("clientes",[dict(i=int(r.cliente_id),n=int(r.n),g=round(r.g,2),pri=r.pri.strftime("%Y-%m-%d"),ult=r.ult.strftime("%Y-%m-%d"),cd=r.cidade,om=int(r.canais>1)) for r in agg.itertuples()])
dump("pedidos_cliente",[dict(i=int(r.cliente_id),d=r.data_pedido.strftime("%Y-%m-%d"),p=r.slug,v=round(r.valor_itens,2)) for r in pc.itertuples()])
# ---- 6 modelo de prioridade das mensagens abertas (logística do notebook 03)
cc=conv.dropna(subset=["converteu_7d"]).copy(); cc["y"]=cc.converteu_7d.astype(int); cc["origem_post"]=cc.origem_post_id.notna().astype(int)
cc["log_resp"]=np.log1p(cc.tempo_primeira_resposta_min.fillna(14400))
num=["log_resp","hora","dia_semana","cliente_recorrente","valor_historico","msgs_cliente","enviou_midia","origem_post"]; cat=["intencao","canal"]
pre=ColumnTransformer([("n",StandardScaler(),num),("c",OneHotEncoder(handle_unknown="ignore"),cat)])
CORTE=pd.Timestamp("2026-07-01"); tr=cc[cc.iniciada_em<CORTE]; te=cc[cc.iniciada_em>=CORTE]
mdl=make_pipeline(pre,LogisticRegression(max_iter=1000)).fit(tr[num+cat],tr.y); pt=mdl.predict_proba(te[num+cat])[:,1]
ptr=mdl.predict_proba(tr[num+cat])[:,1]; ths=np.linspace(.05,.8,76); thr=ths[int(np.argmax([ (lambda yh: f1_score(tr.y,yh))(ptr>=t) for t in ths]))]; yh=(pt>=thr).astype(int)
fpr,tpr,_=roc_curve(te.y,pt); pr_,rc_,_=precision_recall_curve(te.y,pt); ix=np.linspace(0,len(fpr)-1,40).astype(int); ix2=np.linspace(0,len(pr_)-1,40).astype(int)
cm=confusion_matrix(te.y,yh).tolist()
# reclamação (notebook 03, modelo 2)
d2=ped.merge(cli[["cliente_id","omnichannel"]],on="cliente_id",how="left"); d2=d2[d2.status!="CANCELADO"].copy(); d2["y"]=d2.pedido_id.isin(set(rec.pedido_id)).astype(int)
d2=d2[d2.data_pedido<pd.Timestamp("2026-10-20")]; d2["esp"]=(d2.data_envio-d2.data_pedido).dt.total_seconds()/3600; d2["esp"]=d2.esp.fillna(d2.esp.median())
d2["omni"]=d2.omnichannel.astype(int); d2["tem_evento"]=d2.evento.notna().astype(int); d2["canal"]=d2.slug
n2=["valor_itens","desconto_pct","qtd_itens","esp","atrasado","omni","tem_evento"]; c2=["categoria_principal","canal"]
pre2=ColumnTransformer([("n",StandardScaler(),n2),("c",OneHotEncoder(handle_unknown="ignore"),c2)])
t2=d2[d2.data_pedido<CORTE]; e2=d2[d2.data_pedido>=CORTE]; m2=make_pipeline(pre2,LogisticRegression(max_iter=1000,class_weight="balanced")).fit(t2[n2+c2],t2.y); p2=m2.predict_proba(e2[n2+c2])[:,1]
thr2=np.quantile(m2.predict_proba(t2[n2+c2])[:,1][t2.y==1],.40); y2=(p2>=thr2).astype(int)
lr=mdl[-1]; names=num+list(mdl[0].named_transformers_["c"].get_feature_names_out(cat))
dump("modelos",dict(
 conversao=dict(auc=round(roc_auc_score(te.y,pt),3),ap=round(average_precision_score(te.y,pt),3),taxa_base=round(float(te.y.mean()),3),limiar=round(float(thr),2),
   acuracia=round(accuracy_score(te.y,yh),3),precisao=round(precision_score(te.y,yh),3),recall=round(recall_score(te.y,yh),3),f1=round(f1_score(te.y,yh),3),matriz=cm,
   roc=[[round(float(fpr[i]),3),round(float(tpr[i]),3)] for i in ix],pr=[[round(float(rc_[i]),3),round(float(pr_[i]),3)] for i in ix2],
   coef=sorted([dict(n=n,v=round(float(v),2)) for n,v in zip(names,lr.coef_[0])],key=lambda x:x["v"]),n_treino=int(len(tr)),n_teste=int(len(te))),
 reclamacao=dict(auc=round(roc_auc_score(e2.y,p2),3),ap=round(average_precision_score(e2.y,p2),3),taxa_base=round(float(e2.y.mean()),3),acuracia=round(accuracy_score(e2.y,y2),3),
   precisao=round(precision_score(e2.y,y2),3),recall=round(recall_score(e2.y,y2),3),f1=round(f1_score(e2.y,y2),3),matriz=confusion_matrix(e2.y,y2).tolist(),
   tx_no_prazo=round(float(e2[e2.atrasado==0].y.mean()),3),tx_atrasado=round(float(e2[e2.atrasado==1].y.mean()),3))))
# regressão posts (todas as lojas, como no notebook)
post["sc"]=post.salvamentos+post.compartilhamentos; ols=smf.ols("conversas_48h_atribuidas ~ sc",post).fit(); s=post.sample(500,random_state=1)
dump("regressao",dict(pontos=[[int(a),int(b)] for a,b in zip(s.sc,s.conversas_48h_atribuidas)],inter=round(float(ols.params.Intercept),3),incl=round(float(ols.params.sc),4),r2=round(float(ols.rsquared),3),n=int(len(post))))
# ---- 7 pendências abertas (loja 1) + prioridade por modelo
a=pend[(pend.loja_id==LOJA)&(pend.desfecho=="aberta")]
msg=a[a.tipo=="MENSAGEM_SEM_RESPOSTA"].merge(conv[["conversa_id","intencao","primeira_mensagem","canal","hora","dia_semana","cliente_recorrente","valor_historico","msgs_cliente","enviou_midia","origem_post_id"]],left_on="origem_id",right_on="conversa_id")
msg["origem_post"]=msg.origem_post_id.notna().astype(int); msg["log_resp"]=np.log1p(msg.duracao_h*60)
msg["score"]=mdl.predict_proba(msg[num+cat])[:,1]
# ---- modelo de conversa para rodar dentro da Atria (parâmetros da regressão logística + amostra para validar o Java)
_sc=mdl[0].named_transformers_["n"]; _oh=mdl[0].named_transformers_["c"]; _lr=mdl[-1]; _co=_lr.coef_[0]; _k=len(num)
_cats=[]; _pos=_k
for _nome,_cs in zip(cat,_oh.categories_):
    _cats.append(dict(nome=_nome,valores=[str(x) for x in _cs],coef=[float(x) for x in _co[_pos:_pos+len(_cs)]])); _pos+=len(_cs)
dump("modelo_conversa",dict(intercepto=float(_lr.intercept_[0]),
    numericas=[dict(nome=n_,media=float(m_),escala=float(e_),coef=float(c_)) for n_,m_,e_,c_ in zip(num,_sc.mean_,_sc.scale_,_co[:_k])],categoricas=_cats))
_v=te.sample(300,random_state=7); _pv=mdl.predict_proba(_v[num+cat])[:,1]
dump("validacao_conversa",[dict(min=float(np.expm1(r.log_resp)),hora=int(r.hora),dia=int(r.dia_semana),rec=int(r.cliente_recorrente),vh=float(r.valor_historico),msgs=int(r.msgs_cliente),midia=int(r.enviou_midia),post=int(r.origem_post),intencao=r.intencao,canal=r.canal,esperado=float(p_)) for r,p_ in zip(_v.itertuples(),_pv)])
pp=ped[ped.pedido_id.isin(a[a.tipo=="PEDIDO_NAO_ENVIADO"].origem_id)].merge(a[a.tipo=="PEDIDO_NAO_ENVIADO"][["origem_id","duracao_h","prioridade"]],left_on="pedido_id",right_on="origem_id")
rc=rec[rec.reclamacao_id.isin(a[a.tipo=="RECLAMACAO_PENDENTE"].origem_id)].merge(a[a.tipo=="RECLAMACAO_PENDENTE"][["origem_id","duracao_h","prioridade"]],left_on="reclamacao_id",right_on="origem_id")
dump("pendencias",dict(referencia=REF,
 mensagens=[dict(id=int(r.conversa_id),canal=r.canal,intencao=r.intencao,texto=r.primeira_mensagem,horas=round(r.duracao_h,1),prio=r.prioridade,min=round(float(r.duracao_h*60),2),hora=int(r.hora),dia=int(r.dia_semana),rec=int(r.cliente_recorrente),vh=round(float(r.valor_historico),2),msgs=int(r.msgs_cliente),midia=int(r.enviou_midia),post=int(r.origem_post)) for r in msg.sort_values("score",ascending=False).itertuples()],
 pedidos=[dict(id=int(r.pedido_id),canal=r.slug,valor=round(r.valor_itens,2),horas=round(r.duracao_h,1),prazo=r.prometido_envio_ate.strftime("%Y-%m-%d %H:%M"),prio=r.prioridade) for r in pp.sort_values("duracao_h",ascending=False).itertuples()],
 reclamacoes=[dict(id=int(r.reclamacao_id),pedido=int(r.pedido_id),canal=SLUG[int(r.plataforma_id)],tipo=r.tipo,horas=round(r.duracao_h,1),prio=r.prioridade) for r in rc.sort_values("duracao_h",ascending=False).itertuples()]))
# ---- 8 Efeito Atria: painel semanal de 4 lojas + DiD
cw=conv.merge(loja[["loja_id","data_adocao_atria"]],on="loja_id"); cw["semana"]=(cw.iniciada_em.dt.normalize()-pd.to_timedelta(cw.iniciada_em.dt.weekday,unit="D"))
cw["ate1h"]=((cw.sem_resposta==0)&(cw.tempo_primeira_resposta_min<=60)).astype(int)
W=cw.groupby(["loja_id","semana"]).agg(conv=("conversa_id","size"),ate1h=("ate1h","sum"),semresp=("sem_resposta","sum"),med=("tempo_primeira_resposta_min","median")).reset_index()
pw=ped[ped.status!="CANCELADO"].copy(); pw["semana"]=(pw.data_pedido.dt.normalize()-pd.to_timedelta(pw.data_pedido.dt.weekday,unit="D"))
P=pw.groupby(["loja_id","semana"]).agg(ped=("pedido_id","size"),atr=("atrasado","sum"),receita=("valor_itens","sum")).reset_index()
rr=rec.copy(); rr["semana"]=(rr.aberta_em.dt.normalize()-pd.to_timedelta(rr.aberta_em.dt.weekday,unit="D")); rr["h"]=(rr.resolvida_em-rr.aberta_em).dt.total_seconds()/3600
RW=rr.dropna(subset=["h"]).groupby(["loja_id","semana"]).agg(rcl=("h","size"),rcl_h=("h","sum")).reset_index()
panel=W.merge(P,on=["loja_id","semana"],how="outer").merge(RW,on=["loja_id","semana"],how="left").fillna({"rcl":0,"rcl_h":0}); panel=panel[panel.semana<=pd.Timestamp("2026-11-16")]  # só semanas completas
ad=loja.set_index("loja_id").data_adocao_atria
panel["ad"]=panel.loja_id.map(ad); panel["expo"]=[0.0 if pd.isna(a_) else float(np.clip(((s-a_).days+1)/56,0,1)) if s>=a_ else 0.0 for s,a_ in zip(panel.semana,panel.ad)]
dump("efeito_semanal",[dict(l=int(r.loja_id),s=r.semana.strftime("%Y-%m-%d"),n=int(r.conv),u=int(r.ate1h),sr=int(r.semresp),md=None if pd.isna(r.med) else round(float(r.med),0),pd=int(r.ped),at=int(r.atr),rc=int(r.rcl),rh=round(float(r.rcl_h),1),rec=round(float(r.receita),0)) for r in panel.itertuples()])
rng=np.random.default_rng(3)
METR={"resp_1h":("ate1h","conv",100,"pp"),"sem_resposta":("semresp","conv",100,"pp"),"atraso_envio":("atr","ped",100,"pp"),"resolucao_h":("rcl_h","rcl",1,"h")}
panel=panel.rename(columns={"ate1h":"ate1h","semresp":"semresp","atr":"atr","ped":"ped","conv":"conv"})
def razao(df,a_,b_): s_=df[b_].sum(); return np.nan if s_==0 else df[a_].sum()/s_
res={}
for loja_id in [1,2,3]:
    a0=ad[loja_id]; ini=a0-pd.Timedelta(weeks=12); pos_ini=a0+pd.Timedelta(weeks=8)
    pre_w=panel[(panel.semana>=ini)&(panel.semana<a0)]; pos_w=panel[panel.semana>=pos_ini]
    out={}
    for k,(a_,b_,esc,un) in METR.items():
        def est(rs):
            t0=razao(pre_w[pre_w.loja_id==loja_id],a_,b_); t1=razao(pos_w[pos_w.loja_id==loja_id],a_,b_); c0=razao(pre_w[pre_w.loja_id==4],a_,b_); c1=razao(pos_w[pos_w.loja_id==4],a_,b_)
            return t0,t1,c0,c1
        t0,t1,c0,c1=est(None); did=((t1-t0)-(c1-c0))*esc
        bs=[]
        sem_pre=sorted(pre_w.semana.unique()); sem_pos=sorted(pos_w.semana.unique())
        for _ in range(400):
            sp=rng.choice(sem_pre,len(sem_pre)); so=rng.choice(sem_pos,len(sem_pos))
            f=lambda L,s_list: pd.concat([L[L.semana==x] for x in s_list])
            T0=razao(f(pre_w[pre_w.loja_id==loja_id],sp),a_,b_);T1=razao(f(pos_w[pos_w.loja_id==loja_id],so),a_,b_);C0=razao(f(pre_w[pre_w.loja_id==4],sp),a_,b_);C1=razao(f(pos_w[pos_w.loja_id==4],so),a_,b_)
            bs.append(((T1-T0)-(C1-C0))*esc)
        lo,hi=np.nanpercentile(bs,[2.5,97.5])
        out[k]=dict(trat_antes=round(t0*esc,2),trat_depois=round(t1*esc,2),ctrl_antes=round(c0*esc,2),ctrl_depois=round(c1*esc,2),did=round(did,2),ic=[round(lo,2),round(hi,2)],un=un)
    res[str(loja_id)]=dict(nome=loja[loja.loja_id==loja_id].nome.iloc[0],adocao=a0.strftime("%Y-%m-%d"),metricas=out,semanas_pre=int(len(sem_pre)),semanas_pos=int(len(sem_pos)))
# TWFE (exposição contínua) como checagem
tw={}
pn=panel.copy(); pn["loja"]=pn.loja_id.astype(str); pn["sem_s"]=pn.semana.dt.strftime("%Y-%m-%d")
pn["resp_1h"]=pn.ate1h/pn.conv*100; pn["sem_resposta"]=pn.semresp/pn.conv*100; pn["atraso_envio"]=pn.atr/pn.ped*100
for k in ["resp_1h","sem_resposta","atraso_envio"]:
    m=smf.ols(f"{k} ~ expo + C(loja) + C(sem_s)",pn).fit(cov_type="HC1"); tw[k]=dict(coef=round(float(m.params["expo"]),2),ic=[round(float(x),2) for x in m.conf_int().loc["expo"]])
dump("efeito_did",dict(lojas=res,twfe=tw,nota="Exposição=1 depois de 8 semanas de adoção. 3 lojas tratadas e 1 controle: indicativo, não prova causal."))
print("ok",{f.name:f.stat().st_size//1024 for f in OUT.glob("*.json")})
print(json.dumps(res["1"],ensure_ascii=False)[:900]); print(tw)
