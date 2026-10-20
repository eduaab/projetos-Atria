# Tarefa: implementar o back-end e o front-end completos da Atria (S1 a S9)

Você vai evoluir o projeto **Atria** que está neste repositório até cobrir todas as funcionalidades de produto planejadas para as sprints S1 a S9 (trilhas BACK/FRONT e a parte de dados simulados em memória).

Leia este documento inteiro antes de escrever qualquer código.

---

## 1. O que é a Atria

A Atria é uma plataforma de **governança de dados** para donos de pequenas e médias lojas de moda. Ela integra:

- **marketplaces**: Mercado Livre, Shopee, Shein, Nuvemshop, TikTok Shop;
- **canais de conversa**: WhatsApp Business, Instagram.

A ideia é ser a primeira tela que o lojista abre no dia. Um painel único reúne as pendências de todas as plataformas e gera alertas objetivos, com contexto (quem gerou, quando e qual o risco). Exemplo de alerta:

> "Você tem 1 cliente sem resposta no Instagram, 1 reclamação não atendida no WhatsApp e 2 pedidos não enviados no Mercado Livre."

Módulos do produto:

1. **Hub de Conexões (onboarding):** o lojista autoriza a Atria a se conectar às plataformas. A partir daí, a Atria mapeia onde estão os dados dos clientes.
2. **Monitoramento contínuo e alertas:** a Atria detecta mudanças relevantes e gera alertas com nível de prioridade. Exemplos: cliente sem resposta, reclamação pendente, pedido não enviado, **acesso não revogado de funcionário desligado** (resolvível em um clique) e prazo de solicitação LGPD perto de vencer.
3. **Clientes (Central de clientes):** busca unificada de cliente por nome, CPF, e-mail ou telefone. Mostra em quais plataformas aquele cliente aparece e o histórico de interações. A Atria **não altera nada na origem**: só identifica e conecta as pontas.
4. **Exclusão LGPD:** quando o titular pede exclusão, a Atria apaga **apenas o que ela própria armazena** (nome, CPF, e-mail, telefone, CEP) e anonimiza o registro. O histórico da solicitação continua existindo, sem dado pessoal, como comprovante, com protocolo gerado automaticamente.
5. **Riscos e Prazos:** solicitações em aberto, prazos restantes, indicadores de risco, status de exposição ANPD e riscos de marketplace.
6. **Relatórios:** relatório ESG (pilar G, governança), relatório de conformidade ANPD e "Diferencial Comercial" para parceiros e marketplaces, exportados em **PDF**.

---

## 2. Estado atual do repositório

- Spring Boot 3.3.5, Java 17, Maven. Dependências: `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-test`.
- Back-end: um único `ApiController`, um único `AtriaService` com todos os dados mockados em memória (`ConcurrentHashMap`), e `record`s em `model/` e `dto/`.
- Front-end: `src/main/resources/static/` com `index.html`, `css/style.css`, `js/app.js` (um arquivo só, roteamento por hash: home, clientes, riscos, hub, esg) e `img/Logo.png`.
- Problemas conhecidos:
    - **XSS:** `app.js` monta HTML com `innerHTML` e dados sem escapar.
    - O model é exposto direto na API (CPF completo em listagens).
    - Toda a regra de negócio está concentrada em um único service.

**Use o código existente como base.** Reaproveite o visual, o CSS e os fluxos que já funcionam (purga com feedback, conexão simulada, calendário). Reorganize e complete; não descarte o que já está bom.

---

## 3. Escopo

### Dentro
Tudo o que está nas seções 6 e 7: back-end completo com API REST, front-end completo, dados fictícios em memória, exportação em PDF, testes e README.

### Fora (NÃO fazer agora)
- **Banco de dados real.** Não adicione JPA, Hibernate, H2, PostgreSQL, Flyway nem nenhum driver. A persistência fica **em memória**, atrás de interfaces de repositório (ver seção 4), para ser trocada por Postgres + JPA em outra etapa.
- **A trilha de Análise e Visualização de Dados** (vendas, lucro, métricas de marketplace e Instagram, notebooks, dashboards de BI, classificador de machine learning).
- Autenticação e login de usuários. A aplicação roda para **um lojista fixo**, configurado em `application.properties` (`atria.lojista-atual-id=1`), mas o modelo já tem o conceito de lojista.
- Integração real com APIs externas. Toda conexão e autorização é **simulada**.

---

## 4. Arquitetura obrigatória: MVC em camadas, limpa e fácil de entender

### 4.1 Back-end

Estrutura de pacotes (`br.com.atria`):

```
br.com.atria
├── AtriaApplication.java
├── config/          → configuração (Clock, propriedades da Atria, CORS se necessário)
├── controller/      → um @RestController por módulo; só recebe, valida e delega
├── service/         → regras de negócio, um service por módulo
│   └── regra/       → regras isoladas e testáveis (cálculo de prazo, classificação de prioridade, anonimização)
├── repository/      → INTERFACES de repositório, uma por entidade
│   └── memoria/     → implementações em memória (@Repository)
├── model/           → entidades de domínio (classes) + enums em model/enums
├── dto/
│   ├── request/     → records de entrada, com Bean Validation
│   └── response/    → records de saída (nunca expor o model direto)
├── mapper/          → conversão model ↔ DTO (classes simples, sem biblioteca)
├── exception/       → exceções de negócio + @RestControllerAdvice global
├── relatorio/       → geração de PDF (relatórios e comprovante de exclusão)
└── seed/            → carga dos dados fictícios na inicialização
```

Regras:

1. **Controller fino:** sem regra de negócio, sem acesso direto a repositório. Ele recebe a requisição, chama o service e devolve o DTO.
2. **Service** concentra a regra. Regras complexas ou reutilizáveis ficam em classes próprias em `service/regra/` (ex.: `CalculadoraPrazo`, `ClassificadorPrioridade`, `AnonimizadorTitular`).
3. **Repository como interface**, com nomes no estilo Spring Data (`findById`, `findAll`, `save`, `deleteById`, `findByLojistaId`…). Assim, a troca futura por `JpaRepository` é quase mecânica. A implementação em memória usa `ConcurrentHashMap` + `AtomicLong` para gerar IDs.
4. **Entidades do model** como classes (não records), com `Long id`, construtor, getters e só os setters/métodos de mudança necessários (ex.: `titular.anonimizar(instante)`, `alerta.resolver(instante)`). Prepare os campos para virar JPA depois, mas **sem** anotações JPA agora.
5. **DTOs são `record`s.** Em listagens, CPF sempre mascarado (`***.***.789-**`). O CPF completo só aparece no detalhe do titular.
6. **Injeção por construtor**, sem `@Autowired` em campo. Sem Lombok.
7. **Datas:** use `java.time` com um `Clock` injetado (bean em `config/`), para que prazos e testes sejam determinísticos. Fuso padrão `America/Fortaleza`.
8. **Erros:** um `@RestControllerAdvice` devolvendo sempre o mesmo formato: `{ "timestamp", "status", "erro", "mensagem", "caminho" }`. Use exceções próprias (`RecursoNaoEncontradoException` → 404, `RegraNegocioException` → 422, validação → 400).
9. **Nomes em português**, coerentes com o domínio (titular, solicitação, conexão, alerta, evidência). Métodos curtos, sem comentários óbvios; JavaDoc só onde a regra não é evidente.
10. **Única dependência nova permitida:** `com.github.librepdf:openpdf` para os PDFs.

### 4.2 Modelo de domínio

Alinhado ao esquema de banco já planejado pelo grupo, para facilitar a migração:

| Entidade | Campos principais |
| --- | --- |
| `Lojista` | id, nomeLoja, slug, segmento, uf, cidade, dataAdesaoAtria |
| `Usuario` | id, lojistaId, nome, email, papel (DONO, ATENDENTE, ESTOQUISTA), dataAdmissao, dataDesligamento (nula = ativo) |
| `Plataforma` | id, slug, nome, tipo (MARKETPLACE, LOJA_PROPRIA, MENSAGERIA, REDE_SOCIAL), escopos de autorização simulados |
| `Conexao` | id, lojistaId, plataformaId, status (ATIVA, PENDENTE, REVOGADA, ERRO), conectadaEm, desconectadaEm, ultimaSincronizacao |
| `Acesso` | id, usuarioId, conexaoId, concedidoEm, revogadoEm |
| `Titular` | id, lojistaId, nome, cpf, email, telefone, cep, criadoEm, anonimizadoEm |
| `IdentidadePlataforma` (Clientes) | id, titularId, plataformaId, idExterno (@, telefone, id de comprador), metodoVinculo (CPF, EMAIL, TELEFONE, MANUAL), vinculadoEm |
| `DadoIdentificado` | id, titularId, plataformaId, tipoDado (NOME, CPF, EMAIL, TELEFONE, ENDERECO), encontradoEm |
| `Interacao` | id, titularId, plataformaId, tipo (COMPRA_FINALIZADA, CARRINHO_ABANDONADO, MENSAGEM, RECLAMACAO, SOLICITACAO_LGPD), data, valor (opcional) |
| `PendenciaCanal` | id, lojistaId, plataformaId, titularId, tipo (MENSAGEM_SEM_RESPOSTA, RECLAMACAO_PENDENTE, PEDIDO_NAO_ENVIADO), desde, resolvida. São os "cenários fictícios" que alimentam os alertas. |
| `SolicitacaoLgpd` | id, titularId, tipo (EXCLUSAO, ACESSO, CORRECAO, PORTABILIDADE), canalOrigem (WHATSAPP, INSTAGRAM, EMAIL, MARKETPLACE…), abertaEm, prazo, concluidaEm, status (PENDENTE, EM_ANDAMENTO, CONCLUIDA, ATRASADA), protocolo |
| `Evidencia` | id, solicitacaoId, geradaEm, cpfMascarado, plataformas, hashComprovante (SHA-256), protocolo |
| `Alerta` | id, lojistaId, plataformaId (opcional), tipo, prioridade (ALTA, MEDIA, BAIXA), status (ABERTO, EM_ANALISE, RESOLVIDO), titulo, descricao, clientesAfetados, origem (referência à pendência, acesso ou solicitação), criadoEm, resolvidoEm |
| `RegistroAuditoria` | id, lojistaId, acao, descricao, entidade, entidadeId, ocorridoEm. É o "histórico de ações" dos relatórios. |

"Gastos totais" do titular = soma dos `valor` das interações de compra.

### 4.3 Regras de negócio

- **Prazo LGPD:** `prazo = abertaEm + 15 dias`. Uma solicitação vira `ATRASADA` quando passa do prazo sem ser concluída. Dias restantes são calculados com o `Clock`.
- **Classificação de prioridade** (`ClassificadorPrioridade`, por regras, atrás de uma interface, porque um modelo de ML vai substituí-lo no futuro):
    - **ALTA:** reclamação pendente há mais de 24 h; prazo LGPD com até 2 dias ou vencido; acesso não revogado de funcionário desligado.
    - **MÉDIA:** cliente sem resposta há mais de 4 h; pedido não enviado há mais de 1 dia; prazo LGPD entre 3 e 7 dias.
    - **BAIXA:** o restante.
- **Geração de alertas** (`AlertaService.executarVarredura()`): percorre as pendências de canal, os acessos e as solicitações, e cria um alerta por ocorrência. É **idempotente**: não duplica um alerta que já existe para a mesma origem. Roda na inicialização e via endpoint (botão "Executar varredura" na Central de Alertas). Também gera a frase-resumo da Home no formato do exemplo da seção 1, agrupando por tipo e plataforma, com plural correto.
- **Acesso não revogado:** ação "Revogar acesso" em um clique. Ela preenche `revogadoEm`, resolve o alerta e registra auditoria.
- **Exclusão (purga) do titular:**
    1. A prévia mostra exatamente o que será apagado (os campos pessoais que a Atria guarda) e o que será preservado (protocolo, datas, plataformas, CPF mascarado).
    2. Exige confirmação.
    3. Anula nome, CPF, e-mail, telefone e CEP, e preenche `anonimizadoEm`.
    4. Remove `IdentidadePlataforma` e `DadoIdentificado` do titular.
    5. Conclui a solicitação de exclusão vinculada (ou cria uma, se não houver).
    6. Gera a `Evidencia` com hash e protocolo `ATR-XXXXXXXX`.
    7. Registra auditoria.
    8. Retorna o protocolo.

    Um titular anonimizado continua aparecendo no histórico como "Titular anonimizado #id", mas não sai mais na busca por nome, CPF ou e-mail. Tentar excluir de novo → 422.
- **Mapeamento de dados (identificação simulada):** ao conectar uma plataforma, os `DadoIdentificado` daquela plataforma passam a contar. Ao desconectar, deixam de contar no resumo (sem apagar). **Indicadores de problema:**
    - CPF encontrado em canal de conversa (WhatsApp/Instagram);
    - titular com dados em 4 ou mais plataformas;
    - cadastro possivelmente duplicado (mesmo e-mail ou telefone em titulares diferentes);
    - acesso ativo de ex-funcionário.
- **Auditoria:** registre conectar, desconectar, varredura, mudança de status de alerta, revogação de acesso, registro e conclusão de solicitação, exclusão e geração de relatório.

### 4.4 Front-end: HTML/CSS/JS puro, organizado em camadas

Continue **sem framework e sem build**, servido pelo Spring em `static/`, usando **ES Modules** (`<script type="module">`):

```
static/
├── index.html            → só o esqueleto: sidebar + <main id="app"> + contêiner de modais/toasts
├── css/
│   ├── base.css          → variáveis (cores, raios, sombras, fontes), reset, tipografia
│   ├── layout.css        → sidebar, grid, painéis, responsivo
│   └── components.css    → cards, botões, badges, modal, tabela, toast, skeleton/loading
├── img/
└── js/
    ├── main.js           → inicialização e registro das rotas
    ├── core/             → api.js (fetch + tratamento de erro), router.js (hash), dom.js (escapeHtml, html tag), format.js (data, moeda, CPF), toast.js, state.js
    ├── services/         → uma função por endpoint, agrupadas por módulo (titularService.js, alertaService.js…)
    ├── components/       → funções puras que recebem dados e devolvem HTML: alertCard, clienteRow, statCard, statusBadge, platformRow, modal, calendar, emptyState, loading
    └── pages/            → uma página por tela (home, clientes, alertas, riscos, conexoes, relatorios), cada uma com mount() e os próprios eventos
```

Divisão de responsabilidades, no espírito MVC:

- `services/` é o acesso ao modelo (API);
- `pages/` é o controlador da tela;
- `components/` é a visão.

Regras:

- **Todo** dado dinâmico passa por `escapeHtml` antes de ir para o HTML.
- Toda tela tem estado de carregando, estado vazio e estado de erro, com toast.
- Nada de `onclick` inline. Use delegação de eventos.
- Rotas por hash com parâmetro: `#/clientes/12`, `#/alertas/5`.

### 4.5 Identidade visual (Figma)

Siga o protótipo do Figma. Se existir a pasta `docs/figma/` com as imagens das telas, **abra e olhe as imagens antes de mexer no front**. Em resumo:

- **Sidebar lilás** à esquerda com o logo "atria" no topo e os itens: Home, Clientes, Alertas, Riscos, Conexões, Relatórios. Ícone + texto; o item ativo tem fundo lilás mais escuro e sombra. Use ícones SVG inline simples (estilo outline), não emoji.
- **Área principal** com fundo cinza-claro, painéis cinza com cantos bem arredondados e sombra suave, e títulos em roxo escuro.
- **Cards de alerta/solicitação:** pílula laranja-terracota com:
    - círculo "!" à esquerda, fora do card;
    - avatar circular vazado;
    - nome e e-mail;
    - divisor vertical;
    - tipo da solicitação e "Prazo: N dias";
    - botão "Visualizar" com contorno branco.
- **Cards de status** (Status de Exposição ANPD, Riscos de Marketplace): verdes, com check grande quando não há pendência. Quando há pendência, use a cor de alerta e mostre a contagem.
- **Home:**
    - Quadro de avisos (alertas mais prioritários + a frase-resumo);
    - Central de clientes (busca);
    - Gerar Relatórios (ESG, ANPD, Diferencial Comercial, com botão "Baixar");
    - calendário mensal com os dias de prazo marcados.
- **Clientes – Perfil do Cliente:**
    - cabeçalho em pílula roxo-acinzentada com avatar, nome, telefone, e-mail, CPF, CEP e Gastos Totais;
    - painel "Dados Sensíveis do Cliente – Foram detectados dados nas seguintes plataformas" (linhas brancas com ícone, nome da plataforma e ícone de link externo);
    - painel "Histórico de Interação com o Cliente" (cards brancos com descrição e data; a solicitação de remoção fica destacada em laranja);
    - botão laranja largo "Deletar todos os dados".
- **Riscos e Prazos:** busca + "Filtrar por", lista de cards de solicitação e, abaixo, os dois cards de status.
- **Hub de Conexões:** "Plataformas conectadas ao seu comércio", com linhas brancas por plataforma. Conectada mostra ícone de link externo; não conectada mostra ícone de entrar/conectar.
- Extraia as cores atuais de `style.css` para variáveis CSS em `base.css` e use **somente as variáveis** daí em diante.
- O layout deve funcionar em telas de notebook e não quebrar em telas estreitas (sidebar recolhe para ícones).

---

## 5. Dados fictícios (seed)

Em `seed/`, um `CommandLineRunner` popula os repositórios com dados **coerentes e plausíveis**, com datas relativas ao `Clock`:

- 1 lojista (ex.: "Flor de Lis Moda", moda feminina, PE).
- 4 usuários, sendo **1 desligado com acesso ainda ativo** (para gerar alerta).
- As 7 plataformas. Conectadas inicialmente: WhatsApp, Instagram, Nuvemshop e Mercado Livre; as demais ficam desconectadas.
- ~30 titulares com nomes e e-mails fictícios (`@exemplo.com`) e CPFs fictícios em formato válido. Inclua os nomes do protótipo: Roberto Lucas Almeida e Darci Henrique Ayres.
- Identidades na Central de clientes: ~20% dos titulares em 3 ou mais plataformas e 2 casos de possível duplicidade.
- Interações variadas (compras com valor, carrinho abandonado, reclamações) dos últimos 12 meses.
- 6 a 8 solicitações LGPD cobrindo: vencida, vence amanhã, vence em 15 dias, concluída com evidência, e vários canais de origem.
- 8 a 12 pendências de canal (sem resposta no Instagram, reclamação no WhatsApp, pedidos não enviados no Mercado Livre…).
- Algumas entradas de auditoria antigas, para o histórico não começar vazio.

---

## 6. API REST

Prefixo `/api`. JSON em português. Filtros por query string. Ajuste nomes se encontrar algo mais consistente, mas mantenha o padrão REST (substantivos, verbos HTTP corretos, 201 na criação, 204 sem corpo).

| Método | Rota | Uso |
| --- | --- | --- |
| GET | `/api/health` | status |
| GET | `/api/dashboard` | contadores, frase-resumo, alertas prioritários, prazos do mês (calendário), status ANPD e marketplace |
| GET | `/api/plataformas` | catálogo de plataformas e escopos |
| GET | `/api/conexoes` | conexões do lojista com status e datas |
| GET | `/api/conexoes/{slug}` | detalhe da integração: status, conectada em, última sincronização, nº de clientes, dados identificados por tipo |
| POST | `/api/conexoes/{slug}` | conectar (após a autorização simulada) |
| DELETE | `/api/conexoes/{slug}` | desconectar |
| GET | `/api/mapeamento/resumo` | clientes por plataforma, dados por tipo, indicadores de problema |
| GET | `/api/mapeamento/dados?plataforma=&tipo=` | tabela de dados identificados |
| GET | `/api/titulares?busca=` | busca por nome, CPF, e-mail ou telefone (CPF mascarado) |
| GET | `/api/titulares/{id}` | perfil completo: dados, plataformas, histórico, gastos totais, solicitações |
| GET | `/api/titulares/{id}/exclusao/previa` | o que será apagado e o que será preservado |
| POST | `/api/titulares/{id}/exclusao` | executa a exclusão (body com `confirmacao: true`) e devolve o protocolo |
| GET | `/api/solicitacoes?status=&tipo=&canal=` | lista com dias restantes |
| POST | `/api/solicitacoes` | registra uma nova solicitação |
| PATCH | `/api/solicitacoes/{id}/status` | altera o status |
| GET | `/api/alertas?prioridade=&status=&plataforma=` | Central de Alertas |
| GET | `/api/alertas/{id}` | detalhe com origem, data/hora e clientes afetados |
| PATCH | `/api/alertas/{id}/status` | altera o status |
| POST | `/api/alertas/varredura` | executa o monitoramento simulado |
| POST | `/api/acessos/{id}/revogacao` | revoga o acesso em um clique |
| GET | `/api/riscos/indicadores` | métricas do painel de riscos |
| GET | `/api/riscos/ocorrencias` | histórico de ocorrências (alertas resolvidos e abertos ao longo do tempo) |
| GET | `/api/relatorios/indicadores` | indicadores consolidados de conformidade |
| GET | `/api/relatorios/{tipo}` | PDF (`esg`, `anpd`, `comercial`) com `Content-Disposition: attachment` |
| GET | `/api/evidencias` | histórico de solicitações atendidas |
| GET | `/api/evidencias/{protocolo}/comprovante` | PDF do comprovante da exclusão |
| GET | `/api/auditoria` | histórico de ações |

---

## 7. Telas e cards do Trello

Implemente na ordem das sprints. Cada item abaixo é um card do quadro do grupo; alguns cards estavam cortados no print, então complete com o que fizer sentido para o módulo.

**S1 – Base:** estrutura visual do dashboard; estrutura do front e do back; navegação entre telas; layout base; componentes reutilizáveis; servidor e configuração; rotas dos módulos; comunicação front ↔ back.

**S2 – Conexões / Onboarding:**
- tela de onboarding: no primeiro acesso, sem nenhuma conexão salva no `localStorage`, mostre um passo a passo convidando a conectar plataformas; depois disso o Hub abre direto;
- cards de serviços disponíveis;
- botão de conectar e desconectar;
- **interface de autorização simulada:** modal estilo OAuth "A Atria quer acessar: ler pedidos, ler mensagens…" com os escopos da plataforma, botões Autorizar e Cancelar, e um carregamento curto;
- status de cada plataforma;
- endpoints de listar, conectar e desconectar;
- atualização de status;
- registro da data de conexão.

**S3 – Mapeamento:**
- tela de visão geral dos dados encontrados;
- cards com a quantidade de clientes por plataforma;
- tabela de dados identificados, com filtro;
- indicadores de possíveis problemas;
- detalhe de cada integração;
- endpoints de dados mapeados e de clientes cadastrados;
- lógica simulada de identificação.

**S4 – Clientes:**
- tela de Clientes;
- busca por nome e CPF (e também e-mail e telefone);
- tela de resultados;
- dados pessoais armazenados;
- lista de solicitações de exclusão;
- status e canal de origem das solicitações;
- endpoints de busca, de detalhe e de listar solicitações;
- filtros por status e canal.

**S5 – Exclusão:**
- botão de exclusão;
- **modal de confirmação** que mostra quais dados internos serão excluídos e quais serão preservados;
- feedback visual por plataforma (reaproveite a animação atual);
- histórico de solicitações;
- protocolo e comprovante: tela + download do PDF;
- endpoint de exclusão simulada;
- regras de exclusão e anonimização;
- atualização do status da solicitação;
- preservação dos registros que demonstram o atendimento.

**S6 – Central de Alertas:**
- tela de alertas;
- cards por nível de risco, com cor por prioridade;
- plataforma e origem do alerta;
- data, horário e quantidade de clientes afetados;
- tela de detalhes;
- ação para alterar o status do alerta;
- ação "Revogar acesso" quando o alerta for de acesso;
- endpoint de listagem;
- geração de alertas a partir dos cenários fictícios;
- classificação de prioridade.

**S7 – Riscos e Prazos:**
- painel de riscos e prazos;
- quadro de solicitações em aberto;
- indicador visual de prazo restante: verde acima de 7 dias, amarelo de 3 a 7, laranja até 2, vermelho vencido;
- cards de métricas;
- filtros por risco e status;
- histórico de ocorrências;
- endpoint de indicadores;
- cálculo de prazos;
- classificação de prioridades.

**S8 – Relatórios:**
- página de relatórios;
- indicadores de conformidade;
- solicitações atendidas e riscos resolvidos;
- histórico de ações (auditoria);
- modelo de relatório para parceiros e marketplaces;
- botão de exportação;
- endpoint de geração de PDF;
- consolidação dos indicadores.

O conteúdo dos PDFs deve ser feito a partir dos dados reais em memória. Cada PDF tem: cabeçalho com a marca Atria, período, indicadores, tabelas e data de geração.

**S9 – Refinamento:**
- revisar a navegação;
- padronizar componentes e identidade visual;
- estados de carregamento e erro;
- corrigir inconsistências;
- testar a experiência;
- fluxo visual da apresentação;
- testar todos os endpoints;
- validar a comunicação entre os módulos;
- corrigir regras simuladas;
- testar busca e exclusão.

---

## 8. Testes

- **Testes unitários** das regras: `CalculadoraPrazo`, `ClassificadorPrioridade`, anonimização/exclusão (incluindo exclusão repetida → erro), idempotência da varredura e frase-resumo dos alertas. Use um `Clock` fixo.
- **Testes de controller** com `@WebMvcTest` + MockMvc nos fluxos principais: busca de clientes, exclusão, conectar/desconectar, alterar status de alerta, download de PDF (status 200 e `application/pdf`), e formato do erro 404/422.
- `mvn test` deve terminar **verde** ao fim de cada fase.

---

## 9. Como trabalhar

1. **Antes de codar:** leia todo o repositório (`pom.xml`, Java, `index.html`, `app.js`, `style.css`, README) e as imagens em `docs/figma/`, se existirem. Depois me mostre um **plano curto**: estrutura de pastas final, lista de classes por camada e ordem de implementação. Siga sem esperar aprovação, a não ser que encontre um conflito real com este documento.
2. Implemente **fase por fase (S1 → S9)**. Ao fim de cada fase:
    - rode `mvn -q test` (ou `./mvnw` se existir) e corrija o que quebrar;
    - suba a aplicação, confira com `curl` os endpoints novos e verifique que o front carrega sem erros no console;
    - se o projeto for um repositório git, faça **um commit local por fase** com mensagem clara (ex.: `feat(s4): Clientes com busca e perfil do titular`). **Não faça push.**
3. Remova o `AtriaService` monolítico e o `ApiController` antigos quando os novos módulos os substituírem. Não deixe código morto.
4. Não invente dependências além da OpenPDF. Não adicione nada de banco de dados.
5. **Ao final:**
    - atualize o `README.md` com: visão do produto, como rodar, estrutura de pastas explicada (o que cada camada faz), tabela de endpoints e uma seção "Como migrar para PostgreSQL", com o passo a passo de trocar os repositórios em memória por JPA;
    - me entregue um resumo em **tabela card do Trello → status (feito / parcial / não feito) → onde está no código**.

Qualidade esperada: código que um colega do grupo entenda abrindo qualquer pasta. Nomes claros, classes pequenas, uma responsabilidade por classe, sem duplicação e sem "mágica".
