"""
Gerador do mundo simulado da Atria (trilha de Análise e Visualização de Dados).

Cria 24 meses de operação de 4 lojas de moda (nomes fictícios), com 7 plataformas
(as mesmas do catálogo da Atria) e efeitos plantados COM RUÍDO, para que a análise
exploratória encontre padrões de verdade em vez de dados aleatórios:

  1. Sazonalidade (Dia das Mães, Namorados, Black Friday, Natal, datas 9.9/10.10/11.11/12.12)
  2. Economia por canal (taxas, frete, devoluções) -> margem líquida muito diferente da receita
  3. Tempo de primeira resposta -> chance de a conversa virar pedido (efeito logístico)
  4. Engajamento do post (salvamentos/compartilhamentos) -> conversas abertas em 48 h
  5. Atraso no envio -> cancelamento e reclamação; reputação semanal -> demanda da semana seguinte
  6. Cliente omnichannel (~20%) compra mais e volta mais
  7. "Efeito Atria": adoção escalonada (3 lojas em datas diferentes) + 1 loja de controle que nunca adota.
     Depois da adoção, em rampa de 8 semanas: resposta mais rápida, menos mensagem sem resposta,
     envio mais rápido e reclamação resolvida mais cedo.

Tudo é reprodutível: mesma SEED, mesmos dados.
Parâmetros de taxas e frete são hipóteses de simulação, não valores oficiais das plataformas.
"""
import math
import sys
from collections import defaultdict
from datetime import date, datetime, timedelta
from pathlib import Path

import numpy as np
import pandas as pd

SEED = 20261121
START = date(2024, 12, 1)
END = date(2026, 11, 20)          # último dia com dados ("ontem" do painel)
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "data" / "core"
OUT.mkdir(parents=True, exist_ok=True)
rng = np.random.default_rng(SEED)

# ----------------------------------------------------------------------------- cadastros
PLATAFORMAS = [  # espelha o catálogo do seed da Atria
    (1, "whatsapp", "WhatsApp", "MENSAGERIA"),
    (2, "instagram", "Instagram", "REDE_SOCIAL"),
    (3, "nuvemshop", "Nuvemshop", "LOJA_PROPRIA"),
    (4, "mercado-livre", "Mercado Livre", "MARKETPLACE"),
    (5, "shopee", "Shopee", "MARKETPLACE"),
    (6, "shein", "Shein", "MARKETPLACE"),
    (7, "tiktok-shop", "TikTok Shop", "MARKETPLACE"),
]
PID = {s: i for i, s, _, _ in PLATAFORMAS}
COMERCIAIS = ["nuvemshop", "mercado-livre", "shopee", "shein", "tiktok-shop"]
MARKETPLACES = ["mercado-livre", "shopee", "shein", "tiktok-shop"]

LOJAS = [  # id, nome, cidade, uf, adoção da Atria
    (1, "Flor de Lis Moda", "Recife", "PE", date(2025, 12, 1)),
    (2, "Casa Aurora Jeans", "Fortaleza", "CE", date(2025, 9, 1)),
    (3, "Mar & Linho", "Salvador", "BA", date(2026, 3, 1)),
    (4, "Studio Bela Rosa", "Natal", "RN", None),  # controle: nunca adota
]
# pedidos/dia por loja e plataforma (demanda orgânica; as conversas de WhatsApp/Instagram somam pedidos na Nuvemshop)
DEMANDA = {
    1: {"mercado-livre": 2.0, "shopee": 1.5, "nuvemshop": 1.1, "shein": 0.8, "tiktok-shop": 0.9},
    2: {"mercado-livre": 2.8, "shopee": 2.6, "shein": 1.8, "nuvemshop": 0.9},
    3: {"nuvemshop": 1.4, "tiktok-shop": 2.0, "shopee": 1.8},
    4: {"mercado-livre": 2.0, "shopee": 1.6, "nuvemshop": 1.0},
}
CONVERSAS_DIA = {1: 12, 2: 16, 3: 11, 4: 10}
SEGUIDORES0 = {1: 2800, 2: 5200, 3: 3900, 4: 3100}
CONECTADAS_DESDE = {1: date(2024, 12, 1), 2: date(2024, 12, 1), 3: date(2024, 12, 1), 4: date(2024, 12, 1)}

TAXAS = {  # percentual, fixo, fixo só abaixo de R$ 79?  (hipóteses de simulação)
    "mercado-livre": (0.14, 6.0, True),
    "shopee": (0.14, 4.0, False),
    "shein": (0.16, 0.0, False),
    "tiktok-shop": (0.06, 2.0, False),
    "nuvemshop": (0.045, 0.40, False),  # gateway de pagamento
}
CATEGORIAS = {  # nome: (preço mediano, desvio log, prob. de devolução base, peso)
    "Vestidos": (139, 0.35, 0.075, 1.5), "Blusas": (79, 0.30, 0.045, 1.8), "Calças": (149, 0.30, 0.080, 1.3),
    "Saias": (99, 0.30, 0.050, 1.0), "Conjuntos": (169, 0.30, 0.065, 0.9), "Moda praia": (119, 0.30, 0.040, 0.7),
    "Acessórios": (39, 0.40, 0.020, 1.4), "Jaquetas": (219, 0.30, 0.070, 0.5),
}
INTENCOES = ["compra", "duvida_produto", "pos_venda", "reclamacao", "troca_devolucao"]
FRASES = {
    "compra": ["Oi, quero comprar esse modelo, tem no M?", "Boa tarde! Ainda tem o vestido do reels?", "Quero fechar o pedido, como pago?"],
    "duvida_produto": ["Qual o tecido dessa peça?", "Essa calça veste bem em quem tem quadril largo?", "Tem outras cores além da que está na foto?"],
    "pos_venda": ["Meu pedido já foi enviado?", "Qual o código de rastreio?", "Quando chega na minha cidade?"],
    "reclamacao": ["A peça chegou diferente da foto.", "Ainda não recebi e o prazo já passou.", "Veio com defeito na costura."],
    "troca_devolucao": ["Quero trocar por um tamanho maior.", "Como faço a devolução?", "Posso trocar por outra cor?"],
}

# ----------------------------------------------------------------------------- calendário e sazonalidade
def black_friday(ano):
    d = date(ano, 11, 30)
    while d.weekday() != 4:
        d -= timedelta(days=1)
    return d


def dia_das_maes(ano):
    d = date(ano, 5, 8)
    while d.weekday() != 6:
        d += timedelta(days=1)
    return d


MES = {1: .92, 2: .86, 3: .95, 4: 1.0, 5: 1.12, 6: 1.05, 7: .92, 8: 1.0, 9: .95, 10: 1.05, 11: 1.12, 12: 1.15}
DOW = {0: 1.05, 1: 1.05, 2: 1.0, 3: 1.0, 4: .95, 5: .85, 6: 1.0}
DATAS_COMBO = [(9, 9), (10, 10), (11, 11), (12, 12)]


def evento_do_dia(d):
    """Nome do evento comercial do dia (ou '')."""
    if abs((d - black_friday(d.year)).days) <= 3 and d.month == 11:
        return "Black Friday"
    if 0 <= (dia_das_maes(d.year) - d).days <= 9:
        return "Dia das Mães"
    if d.month == 6 and 3 <= d.day <= 12:
        return "Dia dos Namorados"
    if d.month == 12 and d.day <= 22:
        return "Natal"
    if (d.month, d.day) in DATAS_COMBO:
        return f"{d.day}.{d.month}"
    return ""


def sazonal(d):
    m = MES[d.month]
    bf = black_friday(d.year)
    f = m
    if abs((d - bf).days) <= 3 and d.month == 11:
        f *= 1 + 1.6 * math.exp(-((d - bf).days / 1.8) ** 2)
    dm = (dia_das_maes(d.year) - d).days
    if 0 <= dm <= 9:
        f *= 1 + 0.55 * (1 - dm / 10)
    if d.month == 12 and d.day <= 22:
        f *= 1.18
    if d.month == 12 and d.day >= 24 or (d.month == 1 and d.day <= 3):
        f *= 0.7
    return f


def mult_plataforma_dia(plat, d):
    if plat in ("shopee", "shein", "tiktok-shop") and (d.month, d.day) in DATAS_COMBO:
        return 1.9
    return 1.0


def tendencia(d):
    return 1.0 + 0.25 * (d - START).days / (END - START).days


def fator_atria(loja, d):
    """0 antes da adoção, sobe linear até 1 em 56 dias."""
    ad = LOJAS[loja - 1][4]
    if ad is None or d < ad:
        return 0.0
    return min(1.0, ((d - ad).days + 1) / 56)


def desconto_promocao(plat, d, ev):
    if plat == "nuvemshop":
        return float(rng.choice([0, 0, 0, 0.05, 0.10, 0.15], p=[.55, .1, .1, .1, .1, .05]))
    base = {"": 0.04, "Black Friday": 0.20, "Natal": 0.10, "Dia das Mães": 0.10, "Dia dos Namorados": 0.08}.get(ev, 0.15)
    return float(max(0, rng.normal(base, 0.04)))


def hora_pedido():
    h = rng.choice([9, 11, 12, 13, 15, 18, 20, 21, 22], p=[.06, .09, .12, .09, .09, .12, .16, .17, .10])
    return int(h), int(rng.integers(0, 60)), int(rng.integers(0, 60))


def proximo_9h(dt):
    base = dt.replace(hour=9, minute=0, second=0, microsecond=0)
    if dt.hour >= 9:
        base += timedelta(days=1)
    while base.weekday() == 6:
        base += timedelta(days=1)
    return base


# ----------------------------------------------------------------------------- catálogo
def criar_catalogo():
    linhas = []
    pid = 1
    for loja, *_ in LOJAS:
        for cat, (mediana, dp, _, peso) in CATEGORIAS.items():
            n = int(round(peso * 9))
            for k in range(n):
                preco = float(np.clip(rng.lognormal(math.log(mediana), dp), 25, 399))
                preco = round(preco / 10) * 10 - 0.10
                custo = round(preco * rng.uniform(0.30, 0.48), 2)
                linhas.append((pid, loja, f"L{loja}-{cat[:3].upper()}-{k + 1:03d}", f"{cat} modelo {k + 1}", cat, round(preco, 2), custo))
                pid += 1
    prod = pd.DataFrame(linhas, columns=["produto_id", "loja_id", "sku", "nome", "categoria", "preco_base", "custo"])
    # popularidade com cauda longa
    prod["popularidade"] = rng.pareto(1.6, len(prod)) + 0.3
    return prod


PROD = criar_catalogo()
PROD_LOJA = {l: PROD[PROD.loja_id == l].reset_index(drop=True) for l, *_ in LOJAS}


def peso_sazonal_categoria(cat, mes):
    if cat == "Moda praia":
        return 1.8 if mes in (11, 12, 1, 2, 3) else 0.5
    if cat == "Jaquetas":
        return 1.8 if mes in (6, 7, 8) else 0.4
    if cat == "Vestidos":
        return 1.3 if mes in (5, 12, 11) else 1.0
    return 1.0


# ----------------------------------------------------------------------------- estado por loja
class Clientes:
    def __init__(self, loja):
        self.loja = loja
        self.ids = []
        self.mask = []            # conjunto de plataformas comerciais em que compra
        self.omni = []
        self.prop = []
        self.gasto = []           # receita acumulada
        self.nped = []
        self.ult = []
        self.primeira = []
        self.cidade = []
        self.por_plat = defaultdict(list)

    def novo(self, plat, d, nome_plat_extra=True, cidade=None):
        cid = NEXT_CLIENTE[0]
        NEXT_CLIENTE[0] += 1
        extra = None
        outras = [p for p in DEMANDA[self.loja] if p != plat]
        if nome_plat_extra and outras and rng.random() < 0.20:
            extra = str(rng.choice(outras))
        plats = {plat} | ({extra} if extra else set())
        i = len(self.ids)
        self.ids.append(cid)
        self.mask.append(plats)
        self.omni.append(len(plats) > 1)
        self.prop.append(float(rng.gamma(1.0, 1.0)) * (1.6 if len(plats) > 1 else 1.0))
        self.gasto.append(0.0)
        self.nped.append(0)
        self.ult.append(d)
        self.primeira.append(d)
        self.cidade.append(cidade or CIDADES[self.loja][int(rng.choice(len(CIDADES[self.loja]), p=PESOS_CIDADE))])
        for p in plats:
            self.por_plat[p].append(i)
        return i

    def adicionar_plat(self, i, plat):
        if plat not in self.mask[i]:
            self.mask[i].add(plat)
            self.por_plat[plat].append(i)
            if len(self.mask[i]) > 1 and not self.omni[i]:
                self.omni[i] = True
                self.prop[i] *= 1.6

    def escolher(self, plat, d, p_novo=0.58):
        existentes = self.por_plat[plat]
        if not existentes or rng.random() < p_novo:
            return self.novo(plat, d)
        w = np.array([self.prop[i] * (1.0 if (d - self.ult[i]).days > 20 else 0.6) for i in existentes])
        return existentes[int(rng.choice(len(existentes), p=w / w.sum()))]

    def existente_qualquer(self, d):
        if not self.ids:
            return None
        w = np.array(self.prop)
        return int(rng.choice(len(self.ids), p=w / w.sum()))


CIDADES = {
    1: ["Recife", "Olinda", "Jaboatão", "Caruaru", "Paulista"],
    2: ["Fortaleza", "Caucaia", "Maracanaú", "Sobral", "Juazeiro do Norte"],
    3: ["Salvador", "Lauro de Freitas", "Feira de Santana", "Camaçari", "Vitória da Conquista"],
    4: ["Natal", "Parnamirim", "Mossoró", "São Gonçalo do Amarante", "Macaíba"],
}
PESOS_CIDADE = np.array([.55, .15, .12, .10, .08])
UF = {1: "PE", 2: "CE", 3: "BA", 4: "RN"}
NEXT_CLIENTE = [1]

# ----------------------------------------------------------------------------- listas de resultado
pedidos, itens, reclamacoes, carrinhos, conversas, posts = [], [], [], [], [], []
reputacao_semana = []
NEXT = defaultdict(lambda: 1)


def novo_id(chave):
    NEXT[chave] += 1
    return NEXT[chave] - 1


def sorteia_itens(loja, d, rec_omni):
    prod = PROD_LOJA[loja]
    pesos = prod.popularidade.values * np.array([peso_sazonal_categoria(c, d.month) for c in prod.categoria])
    n = int(rng.choice([1, 2, 3], p=[.64, .26, .10]))
    if rec_omni:
        n = min(3, n + (1 if rng.random() < 0.18 else 0))
    idx = rng.choice(len(prod), size=n, replace=False, p=pesos / pesos.sum())
    return prod.iloc[idx]


def criar_pedido(loja, plat, d, clientes, ci, conversa_id=None, momento=None):
    ev = evento_do_dia(d)
    h, mi, s = hora_pedido()
    ts = momento or datetime(d.year, d.month, d.day, h, mi, s)
    sel = sorteia_itens(loja, d, clientes.omni[ci])
    desc = desconto_promocao(plat, d, ev)
    pid = novo_id("pedido")
    receita_itens = custo = 0.0
    qtd_total = 0
    for _, p in sel.iterrows():
        q = 1 if rng.random() < 0.9 else 2
        preco = round(p.preco_base * (1 - desc), 2)
        itens.append((pid, int(p.produto_id), q, preco, p.custo))
        receita_itens += preco * q
        custo += p.custo * q
        qtd_total += q
    receita_itens = round(receita_itens, 2)
    pct, fixo, so_baixo = TAXAS[plat]
    taxa = receita_itens * pct + (fixo if (not so_baixo or receita_itens < 79) else 0)
    if plat == "nuvemshop":
        frete_cobrado = 0.0 if receita_itens >= 199 else float(rng.uniform(14, 28))
        frete_custo = frete_cobrado * float(rng.uniform(0.85, 1.05)) if frete_cobrado else float(rng.uniform(14, 24))
    else:
        frete_cobrado = 0.0
        frete_custo = float(rng.uniform(8, 19)) if receita_itens >= 79 else 0.0
        if plat == "shein":
            frete_custo = float(rng.uniform(5, 10))
    prometido_h = 48
    pedidos.append(dict(
        pedido_id=pid, loja_id=loja, plataforma=plat, cliente_idx=ci, data_pedido=ts, desconto_pct=round(desc, 3),
        valor_itens=receita_itens, frete_cobrado=round(frete_cobrado, 2), taxa_plataforma=round(taxa, 2),
        frete_custo=round(frete_custo, 2), custo_produtos=round(custo, 2), qtd_itens=qtd_total,
        prometido_envio_ate=ts + timedelta(hours=prometido_h), conversa_id=conversa_id,
        categoria_principal=sel.iloc[0].categoria, evento=ev, _omni=clientes.omni[ci],
    ))
    clientes.gasto[ci] += receita_itens
    clientes.nped[ci] += 1
    clientes.ult[ci] = d
    return pid


# ----------------------------------------------------------------------------- posts do Instagram
def agenda_posts():
    agenda = defaultdict(list)  # (loja, dia) -> lista de posts
    for loja, *_ in LOJAS:
        d = START
        while d <= END:
            n = int(rng.choice([4, 5, 6], p=[.3, .45, .25]))
            dias = rng.choice(7, size=n, replace=False)
            for k in dias:
                dd = d + timedelta(days=int(k))
                if dd <= END:
                    agenda[(loja, dd)].append(1)
            d += timedelta(days=7)
    return agenda


BASE_ALCANCE = {"reels": 0.75, "carrossel": 0.32, "foto": 0.20}
SALVA = {"reels": .010, "carrossel": .018, "foto": .006}
COMPARTILHA = {"reels": .012, "carrossel": .006, "foto": .003}
LIKE_MULT = {"reels": 1.0, "carrossel": 1.1, "foto": 0.9}
HORAS_POST = [8, 12, 18, 19, 20, 21]
HORA_MULT = {8: .9, 12: 1.0, 18: 1.05, 19: 1.15, 20: 1.2, 21: 1.1}
DOW_POST = {0: .95, 1: 1.05, 2: 1.10, 3: 1.05, 4: .95, 5: .85, 6: .9}


def publicar(loja, d, seguidores, extra_conv):
    tipo = str(rng.choice(["reels", "carrossel", "foto"], p=[.35, .33, .32]))
    hora = int(rng.choice(HORAS_POST))
    ts = datetime(d.year, d.month, d.day, hora, int(rng.integers(0, 60)))
    viral = 1.0
    if rng.random() < 0.03:
        viral = float(rng.uniform(3, 8))
    alcance = seguidores * BASE_ALCANCE[tipo] * HORA_MULT[hora] * DOW_POST[d.weekday()] * float(rng.lognormal(0, 0.55)) * viral
    alcance = max(60, int(alcance))
    likes = int(alcance * rng.uniform(.035, .06) * LIKE_MULT[tipo] * rng.lognormal(0, .25))
    comentarios = int(likes * rng.uniform(.02, .06))
    salvamentos = int(alcance * SALVA[tipo] * rng.lognormal(0, .4))
    compart = int(alcance * COMPARTILHA[tipo] * rng.lognormal(0, .4))
    visual = int(alcance * rng.uniform(1.1, 1.6))
    pid = novo_id("post")
    esperado = 0.05 * (salvamentos + 1.5 * compart + 0.15 * comentarios)
    n_msg = int(rng.poisson(max(0.0, esperado)))
    for _ in range(n_msg):  # conversas abertas ao longo das 48 h seguintes, decaindo
        atraso_h = min(47.9, float(rng.exponential(14)))
        t = ts + timedelta(hours=atraso_h)
        if t.date() <= END:
            extra_conv[(loja, t.date())].append((t, pid))
    posts.append(dict(post_id=pid, loja_id=loja, tipo=tipo, publicado_em=ts, hora=hora, dia_semana=d.weekday(),
                      seguidores_na_data=int(seguidores), alcance=alcance, visualizacoes=visual, curtidas=likes,
                      comentarios=comentarios, salvamentos=salvamentos, compartilhamentos=compart,
                      tempo_medio_s=round(float(rng.uniform(3, 14)), 1) if tipo == "reels" else None,
                      conversas_48h_atribuidas=n_msg))
    return int(alcance * 0.0)  # seguidores ganhos tratados na rotina semanal


# ----------------------------------------------------------------------------- conversas
def minutos_ate_9h(dt):
    return (proximo_9h(dt) - dt).total_seconds() / 60


def tempo_resposta(ts, atria_f):
    med = 95 * (1 - 0.55 * atria_f)
    base = med * float(np.exp(rng.normal(0, 1.0)))
    h = ts.hour + ts.minute / 60
    if ts.weekday() == 6:
        espera = minutos_ate_9h(ts) * float(rng.uniform(0.8, 1.0)) + base * 0.5 if rng.random() < 0.65 else base * 1.5
    elif 9 <= h < 19:
        espera = base
    elif rng.random() < 0.25:
        espera = base * 1.3
    else:
        espera = minutos_ate_9h(ts) + base * 0.4
    return float(min(espera, 14400))


EFEITO_INTENCAO = {"compra": 1.5, "duvida_produto": 0.3, "pos_venda": -2.5, "reclamacao": -3.5, "troca_devolucao": -3.0}


def prob_conversao(intencao, canal, recorrente, origem_post, valor_hist, hora, resp_min):
    z = -1.1 + EFEITO_INTENCAO[intencao] + (0.3 if canal == "whatsapp" else -0.1)
    z += 0.9 * recorrente + 0.55 * origem_post + 0.35 * min(valor_hist / 300, 2.0)
    z += 0.2 if 17 <= hora <= 22 else 0.0
    z += -0.28 * math.log1p(resp_min / 5)
    return 1 / (1 + math.exp(-z))


def gerar_conversa(loja, ts, origem_post, clientes, atria_f, futuras):
    cid = novo_id("conversa")
    canal = "instagram" if (origem_post is not None and rng.random() < 0.75) else str(rng.choice(["whatsapp", "instagram"], p=[.6, .4]))
    p_int = [.45, .35, .10, .04, .06] if origem_post is not None else [.34, .26, .18, .10, .12]
    intencao = str(rng.choice(INTENCOES, p=p_int))
    recorrente = 0
    ci = None
    valor_hist = 0.0
    if rng.random() < 0.35 and clientes.ids:
        ci = clientes.existente_qualquer(ts.date())
        recorrente = 1
        valor_hist = clientes.gasto[ci]
    n_cli = 1 + int(rng.geometric(0.45)) - 1
    midia = int(rng.random() < 0.28)
    sem_resposta = rng.random() < 0.06 * (1 - 0.6 * atria_f)
    resp = None if sem_resposta else tempo_resposta(ts, atria_f)
    if sem_resposta:
        p = 0.0
    else:
        p = prob_conversao(intencao, canal, recorrente, int(origem_post is not None), valor_hist, ts.hour, resp)
    converteu = rng.random() < p
    pedido_id = None
    if converteu:
        resp_ts = ts + timedelta(minutes=resp)
        delay_h = float(min(rng.exponential(26), 168))
        t_ped = resp_ts + timedelta(hours=delay_h)
        if t_ped.date() <= END:
            futuras[(loja, t_ped.date())].append((t_ped, cid, ci))
        else:
            converteu = None  # fora da janela de observação
    conversas.append(dict(
        conversa_id=cid, loja_id=loja, canal=canal, iniciada_em=ts, hora=ts.hour, dia_semana=ts.weekday(),
        intencao=intencao, primeira_mensagem=str(rng.choice(FRASES[intencao])), cliente_recorrente=recorrente,
        valor_historico=round(valor_hist, 2), origem_post_id=origem_post, msgs_cliente=n_cli + 1, enviou_midia=midia,
        sem_resposta=int(sem_resposta), tempo_primeira_resposta_min=None if resp is None else round(resp, 1),
        _converteu=converteu, _prob_real=p, cliente_idx=ci,
    ))
    return cid


# ----------------------------------------------------------------------------- simulação principal
def semana_inicio(d):
    return d - timedelta(days=d.weekday())


def main():
    cl = {l: Clientes(l) for l, *_ in LOJAS}
    seguidores = dict(SEGUIDORES0)
    agenda = agenda_posts()
    extra_conv = defaultdict(list)
    futuras = defaultdict(list)
    tier_mult = defaultdict(lambda: 1.0)           # (loja, plat) -> multiplicador vigente
    janela = defaultdict(list)                       # (loja, plat) -> últimas 4 semanas [(n, atraso, cancel, reclam)]
    pend_pedidos = []                                # pedidos para sortear desfecho no fim do loop

    d = START
    semana_atual = semana_inicio(d)
    acum_semana = defaultdict(lambda: [0, 0, 0, 0])  # (loja, plat) -> n, atrasos, cancel, reclam
    while d <= END:
        if semana_inicio(d) != semana_atual:   # fecha a semana: atualiza reputação / multiplicador
            for (loja, plat), v in acum_semana.items():
                n, a, c, r = v
                janela[(loja, plat)].append(v[:])
                janela[(loja, plat)] = janela[(loja, plat)][-4:]
                N = sum(x[0] for x in janela[(loja, plat)])
                if N >= 12:
                    la = sum(x[1] for x in janela[(loja, plat)]) / N
                    ca = sum(x[2] for x in janela[(loja, plat)]) / N
                    ra = sum(x[3] for x in janela[(loja, plat)]) / N
                    score = 1 - (0.8 * la + 1.5 * ca + 1.2 * ra)
                    nivel = "verde" if score >= 0.86 else "amarelo" if score >= 0.80 else "laranja" if score >= 0.72 else "vermelho"
                    tier_mult[(loja, plat)] = {"verde": 1.05, "amarelo": 1.0, "laranja": 0.92, "vermelho": 0.82}[nivel]
                    if plat in MARKETPLACES:
                        reputacao_semana.append(dict(
                            loja_id=loja, plataforma=plat, semana=semana_atual, pedidos_4s=N, taxa_atraso=round(la, 4),
                            taxa_cancelamento=round(ca, 4), taxa_reclamacao=round(ra, 4), score=round(score, 4), nivel=nivel,
                            pontos_penalidade=int(round(100 * (1.5 * la + 3 * ca + 2 * ra)))))
            acum_semana = defaultdict(lambda: [0, 0, 0, 0])
            for loja, *_ in LOJAS:                           # seguidores crescem
                seguidores[loja] *= 1.007
            semana_atual = semana_inicio(d)

        for loja, *_ in LOJAS:
            af = fator_atria(loja, d)
            saz = sazonal(d) * DOW[d.weekday()] * tendencia(d)
            # ---- posts do dia
            for _ in agenda.get((loja, d), []):
                publicar(loja, d, seguidores[loja], extra_conv)
            # ---- conversas do dia (base + atribuídas a posts)
            lam = CONVERSAS_DIA[loja] * (0.5 + 0.5 * saz) * tendencia(d) / 1.12
            n_base = int(rng.poisson(lam))
            chegadas = []
            for _ in range(n_base):
                h = float(rng.choice([8, 10, 12, 14, 16, 18, 20, 21, 22, 23], p=[.05, .10, .12, .10, .09, .12, .16, .13, .08, .05]))
                chegadas.append((datetime(d.year, d.month, d.day, int(h), int(rng.integers(0, 60)), int(rng.integers(0, 60))), None))
            for t, post in extra_conv.pop((loja, d), []):
                chegadas.append((t, post))
            for t, post in chegadas:
                gerar_conversa(loja, t, post, cl[loja], af, futuras)
            # ---- pedidos do dia: orgânicos + vindos de conversas
            carga_dia = 0
            novos = []
            for plat, base in DEMANDA[loja].items():
                lam = base * saz * mult_plataforma_dia(plat, d) * tier_mult[(loja, plat)]
                for _ in range(int(rng.poisson(lam))):
                    ci = cl[loja].escolher(plat, d)
                    novos.append((plat, ci, None, None))
            for t, cv, ci in futuras.pop((loja, d), []):
                if ci is None:
                    ci = cl[loja].novo("nuvemshop", d)
                else:
                    cl[loja].adicionar_plat(ci, "nuvemshop")
                novos.append(("nuvemshop", ci, cv, t))
            media_dia = sum(DEMANDA[loja].values()) * 1.5
            carga = len(novos) / (media_dia * 1.6)
            for plat, ci, cv, t in novos:
                pid = criar_pedido(loja, plat, d, cl[loja], ci, cv, t)
                pend_pedidos.append((pid, af, carga))
            # carrinhos abandonados (lojas com checkout próprio)
            for plat in ("nuvemshop", "tiktok-shop"):
                if plat in DEMANDA[loja]:
                    n_c = int(rng.poisson(DEMANDA[loja][plat] * saz * 1.6))
                    for _ in range(n_c):
                        carrinhos.append(dict(loja_id=loja, plataforma=plat, data=d, valor=round(float(rng.lognormal(math.log(130), 0.5)), 2),
                                              recuperado=int(rng.random() < 0.09)))
        # desfechos dos pedidos do dia são sorteados já aqui para alimentar a reputação da semana
        for pid, af, carga in pend_pedidos:
            ped = pedidos[pid - 1]
            sortear_desfecho(ped, af, carga)
            k = (ped["loja_id"], ped["plataforma"])
            acum_semana[k][0] += 1
            acum_semana[k][1] += int(ped["atrasado"])
            acum_semana[k][2] += int(ped["cancelado"])
            acum_semana[k][3] += int(ped["_recl_flag"])
        pend_pedidos = []
        d += timedelta(days=1)
    return cl, pedidos


def proximo_dia_util_9h(dt):
    while dt.weekday() == 6:
        dt = (dt + timedelta(days=1)).replace(hour=9, minute=0, second=0)
    return dt


def sortear_desfecho(ped, af, carga):
    ts = ped["data_pedido"]
    plat = ped["plataforma"]
    mediana = 18 * (1 - 0.30 * af)
    atraso_h = mediana * float(np.exp(rng.normal(0, 0.75))) * (1 + 1.4 * max(0.0, carga - 0.9))
    envio = proximo_dia_util_9h(ts + timedelta(hours=atraso_h)) if (ts + timedelta(hours=atraso_h)).weekday() == 6 else ts + timedelta(hours=atraso_h)
    espera = (envio - ts).total_seconds() / 3600
    atrasado = espera > 48
    p_cancel = {"shopee": .035, "shein": .045, "tiktok-shop": .04}.get(plat, .025) + (0.04 if atrasado else 0.0)
    cancelado = rng.random() < p_cancel
    base_dev = CATEGORIAS[ped["categoria_principal"]][2]
    p_recl = base_dev * (1 + 1.2 * atrasado) * (0.85 if plat == "nuvemshop" else 1.0) * (0.9 if ped["_omni"] else 1.0)
    recl = (not cancelado) and rng.random() < p_recl
    entrega = envio + timedelta(days=float(rng.uniform(2, 7) if plat != "shein" else rng.uniform(4, 9)))
    ped.update(dict(atrasado=atrasado, cancelado=cancelado, _recl_flag=recl, _envio=envio, _entrega=entrega, _espera_envio=espera,
                    _cancel_ts=ts + timedelta(hours=min(espera, float(rng.uniform(3, 60)))) if cancelado else None, _af=af))


if __name__ == "__main__":
    cl, peds = main()
    print("pedidos:", len(peds), " conversas:", len(conversas), " posts:", len(posts))
    import pickle
    with open(OUT.parent / "_estado.pkl", "wb") as f:
        pickle.dump(dict(pedidos=pedidos, itens=itens, carrinhos=carrinhos, conversas=conversas, posts=posts,
                         reputacao=reputacao_semana, clientes={l: vars(c) for l, c in cl.items()}), f)
