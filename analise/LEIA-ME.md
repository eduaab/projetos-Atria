# Trilha de Análise e Visualização de Dados — Atria

Dados 100% simulados (4 lojas de moda, 7 plataformas, 01/12/2024 a 20/11/2026), reprodutíveis: mesma semente, mesmos dados.
Requisitos: Python 3.11+ com pandas, numpy, scikit-learn, statsmodels, matplotlib, nbformat, nbconvert, ipykernel.

Para refazer tudo do zero (a partir desta pasta):
    python gen/gen_world.py        # simula o mundo (efeitos plantados com ruído) -> data/_estado.pkl
    python gen/gen_tables.py       # tabelas do modelo -> data/core/*.csv
    python gen/export_raw.py       # suja os dados de propósito -> data/raw/*.csv (staging)
    python gen/prep.py             # limpa o staging -> data/clean/*.csv, docs/log_preparacao.csv, docs/checagens_qualidade.json
    python gen/make_nbs.py         # recria os notebooks; depois: cd notebooks && jupyter nbconvert --to notebook --execute --inplace *.ipynb
    python gen/export_painel.py <pasta>   # JSON do painel (copie para ../src/main/resources/analise)

O que tem aqui: `data/` (staging bruto, limpos e modelo core, em CSV), `docs/` (esquema PostgreSQL validado, log de limpeza, checagens de qualidade),
`notebooks/` (EDA, regressão, classificadores, com saídas) e `out/notebooks_html/` (os mesmos em HTML para ler sem Jupyter), `out/fig/` (figuras).

## O que está neste repositório
`gen/` (geração dos dados simulados e exportação para o Painel), `docs/` (esquema PostgreSQL do modelo, log de limpeza e checagens de qualidade) e `notebooks/` (EDA, regressão e classificadores, com saídas).
As pastas `data/` e `out/` não são versionadas: os scripts as recriam com a mesma semente.

O modelo de conversa exportado por `gen/export_painel.py` (`modelo_conversa.json`) é calculado dentro da Atria, em Java, e o teste `ModeloConversaTest` o confere contra o Python.
