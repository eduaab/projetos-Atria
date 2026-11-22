import nbformat as nbf
from pathlib import Path
N=Path(__file__).resolve().parent.parent/"notebooks"
def nb(cells,name):
    n=nbf.v4.new_notebook(); n.metadata["kernelspec"]={"name":"python3","display_name":"Python 3","language":"python"}
    n.cells=[nbf.v4.new_markdown_cell(t) if k=="m" else nbf.v4.new_code_cell(t) for k,t in cells]; nbf.write(n,N/name)
SETUP='''import pandas as pd, numpy as np, matplotlib.pyplot as plt, warnings; warnings.filterwarnings("ignore")
plt.rcParams.update({"figure.dpi":110,"axes.spines.top":False,"axes.spines.right":False,"font.size":10})
D="../data/core/"; ped=pd.read_csv(D+"fato_pedido.csv",parse_dates=["data_pedido"]); conv=pd.read_csv(D+"fato_conversa.csv",parse_dates=["iniciada_em"])
post=pd.read_csv(D+"fato_post_instagram.csv",parse_dates=["publicado_em"]); plat=pd.read_csv(D+"dim_plataforma.csv").set_index("plataforma_id"); loja=pd.read_csv(D+"dim_loja.csv",parse_dates=["data_adocao_atria"])
ped["canal"]=ped.plataforma_id.map(plat.nome); ped["mes"]=ped.data_pedido.dt.to_period("M")'''
nb([("m","""# 01 · Análise exploratória (EDA) — Atria
**Pergunta guia:** onde o lojista ganha e onde perde? O que se repete, o que é exceção, e o que a Atria deveria mostrar logo cedo?
Os dados são **simulados** (4 lojas de moda, 7 plataformas, dez/2024 a nov/2026) e foram limpos pelo pipeline `prep.py` (log em `docs/log_preparacao.csv`)."""),
("c",SETUP),
("m","## 1. Visão geral e qualidade\nPrimeiro conferimos tamanho, tipos e nulos. Os nulos que existem são esperados (envio de pedido cancelado, resposta de conversa sem resposta)."),
("c","""print(ped.shape, conv.shape, post.shape)
print(ped.isna().mean().round(3)[lambda s:s>0])
ped[["valor_itens","taxa_plataforma","frete_custo","custo_produtos","lucro_liquido"]].describe().round(1)"""),
("m","## 2. Distribuições\nO ticket tem cauda longa à direita: poucos pedidos grandes puxam a média para cima, então a **mediana** é a medida honesta."),
("c","""v=ped[ped.status!="CANCELADO"].valor_itens
fig,ax=plt.subplots(1,2,figsize=(11,3.4)); ax[0].hist(v,bins=50,color="#4C78A8"); ax[0].axvline(v.median(),c="k",ls="--"); ax[0].set_title("Valor do pedido (R$)")
ax[1].hist(np.log10(v),bins=50,color="#72B7B2"); ax[1].set_title("mesma variável em log10"); plt.tight_layout(); plt.savefig("../out/fig/eda_ticket.png"); plt.show()
print("média",v.mean().round(1),"mediana",v.median().round(1),"assimetria",round(v.skew(),2))"""),
("m","## 3. Receita não é lucro: economia por canal\nA mesma receita rende margens muito diferentes. Aqui aparece o primeiro achado de negócio."),
("c","""g=ped[ped.status!="CANCELADO"].groupby("canal").agg(pedidos=("pedido_id","size"),receita=("valor_itens","sum"),lucro=("lucro_liquido","sum"),ticket_mediano=("valor_itens","median"))
g["margem_%"]=(100*g.lucro/g.receita).round(1); g=g.sort_values("receita",ascending=False); display(g.round(0))
fig,ax=plt.subplots(figsize=(7,3.2)); (g["margem_%"]).plot.bar(ax=ax,color="#4C78A8"); ax.set_ylabel("margem líquida (%)"); ax.set_title("Margem líquida por canal"); plt.tight_layout(); plt.savefig("../out/fig/eda_margem.png"); plt.show()"""),
("m","**Interpretação.** A loja própria (Nuvemshop) e o TikTok Shop têm margem bem acima dos marketplaces clássicos, porque a comissão é menor. Nos marketplaces clássicos (Mercado Livre, Shopee, Shein) a margem líquida fica em torno de 33–34%, contra 43% no TikTok Shop e 48% na loja própria; o TikTok Shop ainda tem poucos pedidos. (Taxas e fretes são hipóteses da simulação.) Para o vendedor, isso muda a pergunta de “onde eu vendo mais?” para “onde eu **fico** com mais?”."),
("m","## 4. Tempo e sazonalidade"),
("c","""s=ped[ped.status!="CANCELADO"].groupby("mes").valor_itens.sum()/1000
fig,ax=plt.subplots(figsize=(11,3.2)); s.plot(ax=ax,marker="o",color="#4C78A8"); ax.set_ylabel("receita (R$ mil)"); ax.set_title("Receita mensal (4 lojas) — nov/2026 parcial até dia 20"); plt.tight_layout(); plt.savefig("../out/fig/eda_mensal.png"); plt.show()
print(s.round(0).sort_values(ascending=False).head(5))"""),
("m","**Interpretação.** Há dois movimentos misturados: uma **tendência de alta** (a receita mensal sobe de ~R$ 200 mil no início de 2025 para ~R$ 320 mil em meados de 2026) e **picos sazonais** moderados (nov/2025, com R$ 306 mil, e maio/2025 ficam acima dos meses vizinhos). Nov/2026 aparece baixo só porque o mês está incompleto: termina no dia 20 e a Black Friday (dia 27) ainda não ocorreu. Por isso comparar “antes vs depois” sem controle engana: parte do crescimento é calendário e tendência, não gestão. É o motivo de usarmos uma loja de controle no Efeito Atria."),
("m","## 5. Correlações\nProcuramos relações entre variáveis de atendimento e resultado. Correlação não prova causa, mas indica onde investigar."),
("c","""c=conv.dropna(subset=["tempo_primeira_resposta_min","converteu_7d"]).copy(); c["log_resp"]=np.log1p(c.tempo_primeira_resposta_min)
cols=["log_resp","cliente_recorrente","valor_historico","msgs_cliente","enviou_midia","converteu_7d"]; cm=c[cols].corr().round(2); display(cm)
import matplotlib; fig,ax=plt.subplots(figsize=(5.5,4.2)); im=ax.imshow(cm,cmap="RdBu_r",vmin=-1,vmax=1); ax.set_xticks(range(len(cols)),cols,rotation=45,ha="right"); ax.set_yticks(range(len(cols)),cols)
for i in range(len(cols)):
    for j in range(len(cols)): ax.text(j,i,cm.iloc[i,j],ha="center",va="center",fontsize=8)
plt.colorbar(im); plt.tight_layout(); plt.savefig("../out/fig/eda_corr.png"); plt.show()"""),
("m","**Interpretação.** O tempo de resposta (em log) tem correlação **negativa**, porém fraca (−0,10), com virar pedido: quem espera mais compra menos. Cliente recorrente (+0,21) e valor histórico (+0,14) têm relação positiva, e as duas são correlacionadas entre si (0,55), o que importa na regressão. Correlação isolada é pequena porque a **intenção** da conversa (não incluída aqui) pesa mais; os modelos dos notebooks seguintes combinam os fatores."),
("m","## 6. Agrupamentos: quando o atendimento demora, o que acontece?"),
("c","""c["faixa"]=pd.cut(c.tempo_primeira_resposta_min,[0,15,60,240,1440,1e6],labels=["até 15 min","15–60 min","1–4 h","4–24 h","mais de 24 h"])
t=c.groupby("faixa",observed=True).converteu_7d.agg(["mean","size"]).round(3); display(t)
ax=t["mean"].mul(100).plot.bar(color="#E45756",figsize=(6.5,3)); ax.set_ylabel("conversas que viram pedido (%)"); ax.set_title("Conversão por tempo de primeira resposta"); plt.tight_layout(); plt.savefig("../out/fig/eda_conv_faixa.png"); plt.show()"""),
("m","**Interpretação.** A conversão cai de forma monotônica conforme a resposta demora (de 37% em até 15 min para 20% depois de 24 h). É um padrão acionável: a Atria deve destacar mensagens paradas há mais de 1 hora, que são as pendências de maior retorno."),
("m","## 7. Posts: o que gera conversa?"),
("c","""post["salv_comp"]=post.salvamentos+post.compartilhamentos
g=post.groupby("tipo").agg(posts=("post_id","size"),alcance_med=("alcance","median"),eng=("taxa_engajamento","median"),conversas=("conversas_48h_atribuidas","mean")).round(3); display(g)
print("corr(salvamentos+compart., conversas 48h) =",post[["salv_comp","conversas_48h_atribuidas"].copy()].corr().iloc[0,1].round(2),"| corr(curtidas, conversas) =",post[["curtidas","conversas_48h_atribuidas"]].corr().iloc[0,1].round(2))"""),
("m","**Interpretação.** Reels e carrosséis geram muito mais conversas do que foto. Salvamentos+compartilhamentos têm correlação 0,97 com as conversas em 48 h; curtidas também têm correlação alta (0,84), porque posts de mais alcance recebem mais de tudo. Para separar o que é sinal de intenção do que é só alcance, usamos regressão múltipla no notebook 02."),
("m","## 8. Antes e depois, sem se enganar\nUma comparação simples “antes vs depois” da Atria mistura efeito da ferramenta com sazonalidade e tendência. Comparamos as lojas que adotaram com a loja de controle (diferença em diferenças)."),
("c","""c2=conv.merge(loja[["loja_id","data_adocao_atria","grupo"]],on="loja_id"); c2["tri"]=c2.iniciada_em.dt.to_period("Q")
r=c2.groupby(["tri","grupo"]).tempo_primeira_resposta_min.median().unstack().round(0); display(r)
ax=r.plot(marker="o",figsize=(8,3.2),color=["#999","#4C78A8"]); ax.set_ylabel("mediana da 1ª resposta (min)"); ax.set_title("Tempo de resposta: lojas com Atria vs controle"); plt.tight_layout(); plt.savefig("../out/fig/eda_did.png"); plt.show()"""),
("m","**Interpretação.** De 2025T1 a 2025T3 as lojas que vão adotar ficam na faixa de ~230–250 min, perto do controle (~210–250). A partir de 2025T4 (primeiras adoções) a linha das lojas com Atria desce para ~105–140 min em 2026, enquanto o controle fica em ~260–275. Atenção: a linha “tratada” junta lojas com datas de adoção diferentes (cada uma entra no efeito em seu momento) e o controle também oscila com o acaso; o detalhamento por loja, com data de adoção, está no painel."),
("m","## Resumo dos achados\n1. Margem varia muito por canal (~33–34% nos marketplaces clássicos, 43% no TikTok Shop e 48% na loja própria); receita sozinha engana.\n2. Tendência de alta e sazonalidade moderada: comparações simples antes/depois são perigosas.\n3. Resposta rápida está associada a mais conversão.\n4. Reels e carrosséis geram muito mais conversas que fotos; salvamentos/compartilhamentos acompanham de perto as conversas.\n5. Lojas com Atria respondem mais rápido que o controle, depois da adoção.")],"01_eda.ipynb")
nb([("m","""# 02 · Regressão — o que explica o quê?
Duas perguntas, duas regressões:
1. **Tempo de resposta → probabilidade de a conversa virar pedido** (logística, efeito de cada fator).
2. **Engajamento do post → conversas abertas em 48 h** (linear, com linha de tendência e análise de resíduos)."""),
("c",SETUP+"\nimport statsmodels.api as sm, statsmodels.formula.api as smf"),
("m","## A. Engajamento do post → conversas em 48 h\nVariável explicativa: salvamentos + compartilhamentos do post. Resposta: quantas conversas foram abertas nas 48 h seguintes."),
("c","""post["salv_comp"]=post.salvamentos+post.compartilhamentos
fig,ax=plt.subplots(figsize=(6.5,4)); ax.scatter(post.salv_comp,post.conversas_48h_atribuidas,s=8,alpha=.35,color="#4C78A8")
m=smf.ols("conversas_48h_atribuidas ~ salv_comp",post).fit(); xs=np.linspace(0,post.salv_comp.quantile(.99),100); ax.plot(xs,m.params.Intercept+m.params.salv_comp*xs,c="#E45756",lw=2,label=f"tendência: +{m.params.salv_comp:.3f} conversa por salv./compart.")
ax.set_xlabel("salvamentos + compartilhamentos"); ax.set_ylabel("conversas em 48 h"); ax.legend(); ax.set_title("Dispersão e linha de tendência"); plt.tight_layout(); plt.savefig("../out/fig/reg_dispersao.png"); plt.show()
print(m.summary().tables[1]); print("R² =",round(m.rsquared,3))"""),
("m","**Leitura.** Cada salvamento ou compartilhamento extra está associado a ~0,06 conversa a mais (intervalo estreito: 0,059–0,060), e a reta explica cerca de 94% da variação (R² = 0,94). É um R² alto porque, na simulação, esse é o principal caminho do post até a conversa; em dados reais seria esperado menor. Agora olhamos os resíduos para ver se a reta é um bom resumo."),
("c","""res=m.resid; fit=m.fittedvalues
fig,ax=plt.subplots(1,3,figsize=(13,3.6))
ax[0].scatter(fit,res,s=8,alpha=.35,color="#72B7B2"); ax[0].axhline(0,c="k"); ax[0].set_title("Resíduos vs ajustado"); ax[0].set_xlabel("ajustado"); ax[0].set_ylabel("resíduo")
ax[1].hist(res,bins=40,color="#4C78A8"); ax[1].set_title("Distribuição dos resíduos")
sm.qqplot(res,line="s",ax=ax[2],markersize=2); ax[2].set_title("QQ-plot"); plt.tight_layout(); plt.savefig("../out/fig/reg_residuos.png"); plt.show()
print("assimetria dos resíduos:",round(res.skew(),2))"""),
("m","**Leitura dos resíduos.** O gráfico à esquerda tem formato de leque: o erro cresce junto com o valor ajustado (heterocedasticidade). O histograma é concentrado em zero com caudas pesadas, e o QQ-plot em S confirma que os resíduos não são normais (assimetria 0,69). A variável resposta é uma contagem, e contagens têm variância que cresce com a média; posts virais criam as caudas. Isso não derruba a direção do efeito, mas os erros-padrão da reta linear não são confiáveis. Por isso, na regressão múltipla a seguir usamos erros-padrão robustos."),
("c","""from statsmodels.stats.outliers_influence import variance_inflation_factor as vif
mm=smf.ols("conversas_48h_atribuidas ~ salv_comp + curtidas + comentarios",post).fit(cov_type="HC3"); print(mm.summary().tables[1]); print("R² =",round(mm.rsquared,3))
X=post[["salv_comp","curtidas","comentarios"]].assign(const=1.0); print("\\nVIF (colinearidade):",{c:round(vif(X.values,i),1) for i,c in enumerate(["salv_comp","curtidas","comentarios"])})"""),
("m","""**Leitura.** Usamos erros-padrão robustos (HC3) justamente por causa do leque nos resíduos. Com os três sinais no mesmo modelo, **só salvamentos+compartilhamentos mantêm efeito claro** (~0,057 conversa por unidade); curtidas e comentários perdem significância. Não é que curtida seja “inútil”: o VIF alto (>10) para curtidas e comentários mostra que eles andam juntos (correlação 0,96) e juntos acompanham o alcance do post, então o modelo não consegue separar o efeito de um do outro. Para o lojista, a leitura prática é: **salvamento e compartilhamento são o sinal mais confiável de conversa futura**; curtida sozinha diz pouco."""),
("m","## B. Tempo de resposta → conversão (logística)\nCada conversa vira pedido ou não. Controlamos por intenção, canal, cliente recorrente e origem em post."),
("c","""c=conv.dropna(subset=["tempo_primeira_resposta_min","converteu_7d"]).copy(); c["converteu_7d"]=c.converteu_7d.astype(int); c["log_resp"]=np.log1p(c.tempo_primeira_resposta_min/5)
c["origem_post"]=c.origem_post_id.notna().astype(int); c["noite"]=c.hora.between(17,22).astype(int)
ml=smf.logit("converteu_7d ~ log_resp + C(intencao) + C(canal) + cliente_recorrente + origem_post + noite + np.log1p(valor_historico)",c).fit(disp=0)
print(ml.summary().tables[1]); print("pseudo-R² (McFadden):",round(ml.prsquared,3))
print("\\nOdds ratio de dobrar o tempo de resposta (+ln2 em log_resp ≈):", np.exp(ml.params["log_resp"]*np.log(2)).round(3))"""),
("c","""g=np.linspace(1,1440,200); base=c.iloc[:1].copy(); base=pd.concat([base]*len(g),ignore_index=True); base["tempo_primeira_resposta_min"]=g; base["log_resp"]=np.log1p(g/5)
for k,v in {"intencao":"compra","canal":"whatsapp","cliente_recorrente":0,"origem_post":0,"noite":1,"valor_historico":0}.items(): base[k]=v
pr=ml.predict(base); fig,ax=plt.subplots(figsize=(6.5,3.6)); ax.plot(g/60,pr*100,color="#E45756",lw=2); ax.set_xlabel("horas até a 1ª resposta"); ax.set_ylabel("chance de virar pedido (%)"); ax.set_title("Cliente novo, WhatsApp, intenção de compra"); plt.tight_layout(); plt.savefig("../out/fig/reg_logit_curva.png"); plt.show()
print(pr.iloc[[0,20,70,199]].round(3).values)"""),
("m","**Leitura.** O coeficiente de `log_resp` é negativo e muito significativo: dobrar o tempo de resposta multiplica as chances de virar pedido por ~0,82, mantidos os demais fatores. A curva mostra que a queda é mais forte logo nas primeiras horas (cerca de 71% no primeiro minuto, 49% em ~2,5 h, 34% em 24 h, para cliente novo no WhatsApp com intenção de compra), o que sugere uma regra simples: **responder em até 1 hora**. Origem em post (+0,51) e horário noturno (+0,24) ajudam; a intenção é o fator dominante (compra vs reclamação). O coeficiente de cliente recorrente deixa de ser significativo (p = 0,82) porque o valor histórico já carrega essa informação (as duas variáveis se correlacionam em 0,55)."),
("m","## Limitações\n- Dados simulados: os coeficientes refletem as relações que plantamos, com ruído; o objetivo é demonstrar o método e a leitura, não estimar o mundo real.\n- Regressão mostra associação; a causalidade do tempo de resposta é sustentada aqui pelo desenho com loja de controle (EDA, seção 8).")],"02_regressao.ipynb")
nb([("m","""# 03 · Classificadores
**Modelo 1 — “esta conversa vira pedido em 7 dias?”** (regressão logística vs. árvore de decisão/boosting).
**Modelo 2 — “este pedido vai virar reclamação ou cancelamento?”** (classe desbalanceada → precisão/recall e curva PR).

Divisão **temporal** (treino: até jun/2026; teste: depois) para simular uso real, sem vazamento do futuro."""),
("c",SETUP+"""
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import make_pipeline
from sklearn.metrics import (confusion_matrix,ConfusionMatrixDisplay,accuracy_score,precision_score,recall_score,f1_score,roc_curve,roc_auc_score,precision_recall_curve,average_precision_score)
CORTE=pd.Timestamp("2026-07-01")"""),
("m","## Modelo 1 · conversa → pedido em 7 dias\nVariáveis conhecidas **no momento da primeira resposta**: intenção, canal, hora, dia da semana, recorrência, valor histórico, origem em post, mensagens do cliente e **tempo de resposta**."),
("c","""c=conv.dropna(subset=["converteu_7d"]).copy(); c["y"]=c.converteu_7d.astype(int); c["origem_post"]=c.origem_post_id.notna().astype(int)
c["resp"]=c.tempo_primeira_resposta_min.fillna(14400); c["log_resp"]=np.log1p(c.resp)
num=["log_resp","hora","dia_semana","cliente_recorrente","valor_historico","msgs_cliente","enviou_midia","origem_post"]; cat=["intencao","canal"]
tr=c[c.iniciada_em<CORTE]; te=c[c.iniciada_em>=CORTE]; print("treino",len(tr),"teste",len(te),"| taxa de conversão treino/teste:",tr.y.mean().round(3),te.y.mean().round(3))
pre=ColumnTransformer([("n",StandardScaler(),num),("c",OneHotEncoder(handle_unknown="ignore"),cat)])
base=make_pipeline(ColumnTransformer([("c",OneHotEncoder(handle_unknown="ignore"),cat)]),LogisticRegression(max_iter=500))
lr=make_pipeline(pre,LogisticRegression(max_iter=1000)); gb=make_pipeline(pre,HistGradientBoostingClassifier(max_depth=4,learning_rate=.06,max_iter=200,random_state=0))
res={}
for nome,m in {"Só intenção+canal (baseline)":base,"Regressão logística":lr,"Gradient boosting":gb}.items():
    m.fit(tr[num+cat],tr.y); p=m.predict_proba(te[num+cat])[:,1]; res[nome]=(m,p)
tab=pd.DataFrame({n:{"AUC":roc_auc_score(te.y,p),"AP (precisão média)":average_precision_score(te.y,p)} for n,(m,p) in res.items()}).T.round(3); display(tab)"""),
("m","**Leitura.** O baseline (só intenção e canal) já tem AUC 0,80, porque a intenção é muito informativa. Com tempo de resposta, recorrência e origem do post a AUC sobe para 0,855 e a precisão média de 0,51 para 0,66. O boosting (0,860) praticamente empata com a logística (0,855): sugere relações quase lineares no log-odds, então escolhemos a logística, mais simples e interpretável."),
("c","""m,p=res["Regressão logística"]
# limiar escolhido no treino por F1
ptr=m.predict_proba(tr[num+cat])[:,1]; ths=np.linspace(.05,.8,76); f=[f1_score(tr.y,ptr>=t) for t in ths]; t=ths[int(np.argmax(f))]; yhat=(p>=t).astype(int)
print("limiar (F1 no treino):",round(t,2))
met=dict(acuracia=accuracy_score(te.y,yhat),precisao=precision_score(te.y,yhat),recall=recall_score(te.y,yhat),f1=f1_score(te.y,yhat)); print({k:round(v,3) for k,v in met.items()})
fig,ax=plt.subplots(1,3,figsize=(14,4)); ConfusionMatrixDisplay(confusion_matrix(te.y,yhat),display_labels=["não vira","vira"]).plot(ax=ax[0],colorbar=False,cmap="Blues"); ax[0].set_title("Matriz de confusão (teste)")
for n,(mm,pp) in res.items():
    fpr,tpr,_=roc_curve(te.y,pp); ax[1].plot(fpr,tpr,label=f"{n} (AUC {roc_auc_score(te.y,pp):.2f})")
    pr,rc,_=precision_recall_curve(te.y,pp); ax[2].plot(rc,pr,label=f"AP {average_precision_score(te.y,pp):.2f}")
ax[1].plot([0,1],[0,1],"k:"); ax[1].set_title("Curva ROC"); ax[1].set_xlabel("falsos positivos"); ax[1].set_ylabel("verdadeiros positivos"); ax[1].legend(fontsize=7)
ax[2].axhline(te.y.mean(),c="k",ls=":"); ax[2].set_title("Precisão × Recall"); ax[2].set_xlabel("recall"); ax[2].set_ylabel("precisão"); ax[2].legend(fontsize=7); plt.tight_layout(); plt.savefig("../out/fig/clf1.png"); plt.show()
print(confusion_matrix(te.y,yhat))"""),
("m","""**Como ler.**
- **Acurácia** é a fração de acertos totais; com ~28% de conversões, “chutar não vira” já acerta ~72%. O modelo chega a 76%, pouco acima, então acurácia sozinha engana.
- **Precisão**: das conversas que o modelo marca como “vão virar”, quantas viram. **Recall**: das que viraram, quantas o modelo achou. **F1** equilibra os dois.
- No teste: precisão 0,55 (de cada 2 marcadas, ~1 vira) e recall 0,81 (acha 4 de cada 5 que viram), com limiar baixo escolhido no treino.
- **Uso na Atria:** priorizar a fila de mensagens abertas. Aqui, errar um falso positivo custa pouco (responder primeiro a uma conversa que não ia comprar); perder um verdadeiro positivo custa uma venda. Por isso o recall pesa mais e o limiar pode ser baixado.
- A curva ROC e a PR mostram o desempenho em todos os limiares, não só no escolhido."""),
("c","""coefs=pd.Series(m[-1].coef_[0],index=num+list(m[0].named_transformers_["c"].get_feature_names_out(cat))).sort_values(); display(coefs.round(2).to_frame("coeficiente (padronizado)"))
fig,ax=plt.subplots(figsize=(6.5,4)); coefs.plot.barh(ax=ax,color="#4C78A8"); ax.set_title("O que mais pesa na previsão"); plt.tight_layout(); plt.savefig("../out/fig/clf1_coef.png"); plt.show()"""),
("m","## Modelo 2 · pedido → reclamação ou cancelamento\nClasse minoritária (~7%). Variáveis conhecidas **no despacho**: categoria, canal, desconto, valor, itens, atraso no envio, evento comercial, cliente omnichannel."),
("c","""ped2=ped.merge(pd.read_csv(D+"dim_cliente.csv")[["cliente_id","omnichannel"]],on="cliente_id",how="left")
rc=pd.read_csv(D+"fato_reclamacao.csv"); ped2["problema"]=((ped2.status=="CANCELADO")|ped2.pedido_id.isin(rc.pedido_id)).astype(int)
ped2["espera_envio_h"]=(pd.to_datetime(ped2.data_envio)-ped2.data_pedido).dt.total_seconds()/3600
# cancelados não têm envio: o atraso conhecido no momento do cancelamento vira janela de observação -> excluir (vazamento)
d=ped2[(ped2.status!="CANCELADO")].copy(); d["y"]=d.pedido_id.isin(rc.pedido_id).astype(int); d=d[d.data_pedido<pd.Timestamp("2026-10-20")]
d["espera_envio_h"]=d.espera_envio_h.fillna(d.espera_envio_h.median()); d["omni"]=d.omnichannel.astype(int); d["tem_evento"]=(d.evento.notna()).astype(int)
num2=["valor_itens","desconto_pct","qtd_itens","espera_envio_h","atrasado","omni","tem_evento"]; cat2=["categoria_principal","canal"]
tr=d[d.data_pedido<CORTE]; te=d[d.data_pedido>=CORTE]; print("taxa de reclamação treino/teste:",tr.y.mean().round(3),te.y.mean().round(3))
pre2=ColumnTransformer([("n",StandardScaler(),num2),("c",OneHotEncoder(handle_unknown="ignore"),cat2)])
mod=make_pipeline(pre2,LogisticRegression(max_iter=1000,class_weight="balanced")); mod.fit(tr[num2+cat2],tr.y); p2=mod.predict_proba(te[num2+cat2])[:,1]
gb2=make_pipeline(pre2,HistGradientBoostingClassifier(max_depth=3,learning_rate=.05,max_iter=150,random_state=0)); gb2.fit(tr[num2+cat2],tr.y); pg=gb2.predict_proba(te[num2+cat2])[:,1]
print("AUC logística",round(roc_auc_score(te.y,p2),3),"| AUC boosting",round(roc_auc_score(te.y,pg),3),"| AP logística",round(average_precision_score(te.y,p2),3),"| AP boosting",round(average_precision_score(te.y,pg),3),"| acaso (AP)",round(te.y.mean(),3))"""),
("c","""# limiar que captura ~60% das reclamações (recall alvo), escolhido no treino
ptr=mod.predict_proba(tr[num2+cat2])[:,1]; thr=np.quantile(ptr[tr.y==1],.40); yh=(p2>=thr).astype(int)
met2=dict(acuracia=accuracy_score(te.y,yh),precisao=precision_score(te.y,yh),recall=recall_score(te.y,yh),f1=f1_score(te.y,yh)); print({k:round(v,3) for k,v in met2.items()})
fig,ax=plt.subplots(1,3,figsize=(14,4)); ConfusionMatrixDisplay(confusion_matrix(te.y,yh),display_labels=["ok","reclamação"]).plot(ax=ax[0],colorbar=False,cmap="Oranges"); ax[0].set_title("Matriz de confusão (teste)")
for n,pp in {"logística":p2,"boosting":pg}.items():
    fpr,tpr,_=roc_curve(te.y,pp); ax[1].plot(fpr,tpr,label=f"{n} AUC {roc_auc_score(te.y,pp):.2f}"); pr,rcl,_=precision_recall_curve(te.y,pp); ax[2].plot(rcl,pr,label=f"{n} AP {average_precision_score(te.y,pp):.2f}")
ax[1].plot([0,1],[0,1],"k:"); ax[1].set_title("Curva ROC"); ax[1].legend(); ax[2].axhline(te.y.mean(),c="k",ls=":",label="acaso"); ax[2].set_title("Precisão × Recall (classe rara)"); ax[2].set_xlabel("recall"); ax[2].set_ylabel("precisão"); ax[2].legend(); plt.tight_layout(); plt.savefig("../out/fig/clf2.png"); plt.show(); print(confusion_matrix(te.y,yh))"""),
("c","""co=pd.Series(mod[-1].coef_[0],index=num2+list(mod[0].named_transformers_["c"].get_feature_names_out(cat2))).sort_values(); display(co.round(2).to_frame("coeficiente (padronizado)")); print("reclamação (%) com envio no prazo vs atrasado:",(te.groupby("atrasado").y.mean()*100).round(1).to_dict())"""),
("m","""**Leitura.** Com classe rara, a **precisão média (AP)** é a medida certa: compare com a linha do acaso (a taxa de reclamação, 5,6%). O modelo fica acima do acaso (AP ≈ 0,08; AUC ≈ 0,60), porém **é um modelo fraco**: com o limiar escolhido, a precisão é 0,07 (de cada 100 pedidos sinalizados, ~7 viram reclamação, contra ~5,6 ao acaso) e o recall 0,53. Reclamação depende muito de fatores que a Atria não observa (qualidade real da peça, expectativa do cliente). Conclusão honesta: **não usar para decidir nada automaticamente**; no máximo como apoio para conferir pedidos atrasados e de categorias de maior devolução. Entre os pedidos do teste, a taxa de reclamação é 8,2% quando o envio atrasou contra 5,3% no prazo; o atraso é o fator de processo que o lojista controla. O restante do poder de separação vem da categoria (tabela de coeficientes acima).

## Limitações e honestidade
- Dados simulados: a relação “atraso → reclamação” foi plantada, então o modelo a reencontra; em dados reais o resultado seria um teste, não uma garantia.
- Descartamos um terceiro modelo (“prever a prioridade do alerta”) porque a prioridade é calculada por regra a partir das mesmas variáveis: o modelo só reaprenderia a regra (circularidade).""")],"03_classificadores.ipynb")
