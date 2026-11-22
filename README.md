# Atria

Plataforma de **governança de dados** para donos de pequenas e médias lojas de moda. A ideia é ser a primeira tela que o lojista abre no dia: um painel único reúne as pendências de todas as plataformas (Mercado Livre, Shopee, Shein, Nuvemshop, TikTok Shop, WhatsApp Business e Instagram) e gera alertas objetivos, com contexto e prioridade:

> "Você tem 1 cliente sem resposta no Instagram, 1 reclamação não atendida no WhatsApp e 2 pedidos não enviados no Mercado Livre."

## Módulos

| Módulo | O que faz |
| --- | --- |
| **Hub de Conexões** | Onboarding no primeiro acesso, autorização simulada no estilo OAuth (escopos, Autorizar/Cancelar), conectar e desconectar, detalhe da integração e acessos da equipe. |
| **Mapeamento** | Onde estão os dados dos clientes: clientes por plataforma, dados por tipo, tabela filtrável e indicadores de problema (CPF em canal de conversa, cliente em 4+ plataformas, cadastro duplicado, acesso de ex-funcionário). |
| **Central de Alertas** | Varredura idempotente das pendências, acessos e prazos; prioridade por regras (ALTA/MÉDIA/BAIXA); detalhe com origem, data/hora e clientes afetados; mudança de status; "Revogar acesso" em um clique. |
| **Clientes** (Central de clientes) | Busca unificada por nome, CPF, e-mail ou telefone; perfil com plataformas, histórico, gastos totais e solicitações; registro e acompanhamento de solicitações LGPD. |
| **Exclusão LGPD** | Prévia do que será apagado e do que será preservado, confirmação, anonimização, remoção dos vínculos, protocolo `ATR-XXXXXXXX`, evidência com hash SHA-256 e comprovante em PDF. |
| **Riscos e Prazos** | Solicitações em aberto com indicador de prazo (verde > 7 dias, amarelo 3–7, laranja até 2, vermelho vencido), métricas, status de exposição ANPD, riscos de marketplace e histórico de ocorrências. |
| **Relatórios** | Indicadores consolidados de conformidade, solicitações atendidas, riscos resolvidos, histórico de ações (auditoria) e exportação em PDF: ESG (pilar G), ANPD e Diferencial Comercial. |
| **Painel** | Como foi ontem e o período (7, 30, 60, 120 dias, 12 meses ou todo o histórico), o que precisa de atenção hoje, canais, atendimento, Instagram, clientes, reputação e o efeito da Atria na própria loja (antes e depois). Usa dados simulados de `src/main/resources/analise`; a pasta `analise/` tem os notebooks e os scripts que os geram. |

A Atria **não altera nada nas plataformas de origem**: ela identifica, conecta as pontas e apaga apenas o que ela própria armazena.

## Como rodar

Pré-requisitos: Java 17+ e Maven 3.9+.

```bash
mvn spring-boot:run      # sobe em http://localhost:8080 (ou na porta da variável PORT)
mvn test                 # testes unitários, de controller e de integração
```

Ao subir, o seed carrega a loja fictícia **Flor de Lis Moda** (moda feminina, Recife/PE) com 30 clientes, 4 plataformas conectadas, solicitações em vários estados, pendências de canal e uma ex-funcionária com acesso ainda ativo. Todas as datas são relativas ao dia em que a aplicação sobe, então os prazos fazem sentido em qualquer dia.

Configuração em `src/main/resources/application.properties`:

| Propriedade | Padrão | Uso |
| --- | --- | --- |
| `atria.lojista-atual-id` | `1` | Lojista atendido (não há login nesta etapa) |
| `atria.fuso-horario` | `America/Fortaleza` | Fuso do `Clock` usado em todas as datas |
| `atria.prazo-lgpd-dias` | `15` | Prazo de atendimento de uma solicitação |

### Roteiro sugerido para a apresentação

1. **Conexões** (abra numa aba anônima para ver o onboarding) → conecte a **Shopee** pelo modal de autorização.
2. **Alertas** → *Executar varredura*: aparece o pedido parado na Shopee. Abra o alerta de **acesso não revogado** e clique em *Revogar acesso*.
3. **Home** → leia a frase-resumo, clique num dia marcado no calendário e baixe um relatório.
4. **Clientes** → busque "Roberto" → *Deletar todos os dados* → confira a prévia, confirme, veja a animação por plataforma e baixe o comprovante.
5. **Riscos** e **Relatórios** → veja os indicadores atualizados, a exclusão no histórico e a ação registrada na auditoria.

## Estrutura de pastas

```text
src/main/java/br/com/atria
├── AtriaApplication.java
├── config/        Clock (fuso America/Fortaleza) e AtriaProperties (application.properties)
├── controller/    Um @RestController por módulo. Só recebe, valida (@Valid) e delega ao service
├── service/       Regras de negócio, um service por módulo (Conexao, Mapeamento, Titular, Exclusao,
│   │              Solicitacao, Alerta, Acesso, Risco, Relatorio, Evidencia, Auditoria, Dashboard)
│   └── regra/     Regras isoladas e testáveis: CalculadoraPrazo, ClassificadorPrioridade (interface)
│                  + ClassificadorPrioridadePorRegras, AnonimizadorTitular, GeradorFraseResumo,
│                  GeradorProtocolo, GeradorHashComprovante, MascaradorCpf, CalculadoraIndiceConformidade
├── repository/    INTERFACES de repositório, uma por entidade, com nomes no estilo Spring Data
│   └── memoria/   Implementações em memória (ConcurrentHashMap + AtomicLong) sobre RepositorioMemoria
├── model/         Entidades de domínio (classes com id, construtor, getters e métodos de mudança)
│   └── enums/     Enums do domínio, cada um com a descrição em português
├── dto/
│   ├── request/   Records de entrada com Bean Validation
│   └── response/  Records de saída. O model nunca é exposto; CPF mascarado em toda listagem
├── mapper/        Conversão model → DTO, em classes simples
├── exception/     Exceções de negócio + @RestControllerAdvice com formato único de erro
├── relatorio/     PDFs com OpenPDF: DocumentoPdf (identidade visual), relatórios e comprovante
└── seed/          CommandLineRunner que carrega os dados fictícios e roda a primeira varredura

src/main/resources/static            Front-end em HTML/CSS/JS puro, sem framework e sem build (ES Modules)
├── index.html     Só o esqueleto: sidebar, <main id="app">, contêineres de modal e toast
├── css/           base.css (variáveis e reset), layout.css (sidebar, grades, responsivo), components.css
└── js/
    ├── main.js    Monta o menu e registra as rotas
    ├── core/      api.js (fetch + erros), router.js (hash com parâmetros), dom.js (escapeHtml, tag html),
    │              format.js (datas, moeda, prazos), toast.js, state.js (localStorage)
    ├── services/  Uma função por endpoint, por módulo (acesso ao "model" do front)
    ├── components/Funções puras que recebem dados e devolvem HTML seguro (a "view")
    └── pages/     Uma página por tela com mount() e os próprios eventos (o "controller" da tela)
```

### Decisões que valem saber

- **Segurança contra XSS:** todo HTML é montado com a tag `` html`...` ``, que escapa cada valor interpolado. `render()` recusa string crua. Não há `onclick` inline; os eventos usam delegação.
- **Erros da API** sempre no formato `{ "timestamp", "status", "erro", "mensagem", "caminho" }`: 404 para recurso inexistente, 422 para regra de negócio violada e 400 para validação ou parâmetro inválido.
- **Privacidade na auditoria e nos alertas:** os textos guardados não levam o nome do titular, só ids e protocolos. Assim, nada pessoal sobra depois da anonimização.
- **Varredura idempotente:** cada alerta guarda a origem (`tipoOrigem` + `origemId`). Uma origem que já tem alerta não ganha outro; só a prioridade dos abertos é recalculada.
- **Coerência entre módulos:** resolver um alerta de pendência marca a pendência como resolvida; alertas de acesso e de prazo só se resolvem revogando o acesso ou concluindo a solicitação; desconectar uma plataforma tira os dados dela do mapeamento sem apagá-los.

## Endpoints

Prefixo `/api`, JSON em português.

| Método | Rota | Uso |
| --- | --- | --- |
| GET | `/api/health` | Status da aplicação |
| GET | `/api/dashboard` | Contadores, frase-resumo, alertas prioritários, prazos (calendário), status ANPD e marketplace |
| GET | `/api/plataformas` | Catálogo de plataformas e escopos |
| GET | `/api/conexoes` | Conexões do lojista com status e datas |
| GET | `/api/conexoes/{slug}` | Detalhe da integração: status, datas, nº de clientes, dados por tipo, acessos |
| POST | `/api/conexoes/{slug}` | Conectar (após a autorização simulada) → 201 |
| DELETE | `/api/conexoes/{slug}` | Desconectar → 204 |
| GET | `/api/mapeamento/resumo` | Clientes por plataforma, dados por tipo, indicadores de problema |
| GET | `/api/mapeamento/dados?plataforma=&tipo=` | Tabela de dados identificados |
| GET | `/api/titulares?busca=` | Busca por nome, CPF, e-mail ou telefone (CPF mascarado) |
| GET | `/api/titulares/{id}` | Perfil completo: dados, plataformas, histórico, gastos totais, solicitações |
| GET | `/api/titulares/{id}/exclusao/previa` | O que será apagado e o que será preservado |
| POST | `/api/titulares/{id}/exclusao` | Executa a exclusão (`{"confirmacao": true}`) → 201 com protocolo |
| GET | `/api/solicitacoes?status=&tipo=&canal=` | Solicitações com dias restantes e faixa de prazo |
| POST | `/api/solicitacoes` | Registra uma solicitação → 201 |
| PATCH | `/api/solicitacoes/{id}/status` | Altera o status (concluir gera comprovante) |
| GET | `/api/alertas?prioridade=&status=&plataforma=` | Central de Alertas |
| GET | `/api/alertas/{id}` | Detalhe com origem, data/hora e clientes afetados |
| PATCH | `/api/alertas/{id}/status` | Altera o status |
| POST | `/api/alertas/varredura` | Executa o monitoramento simulado |
| POST | `/api/acessos/{id}/revogacao` | Revoga o acesso em um clique |
| GET | `/api/riscos/indicadores` | Métricas do painel de riscos |
| GET | `/api/riscos/ocorrencias` | Alertas novos/resolvidos por semana e ocorrências recentes |
| GET | `/api/relatorios/indicadores` | Indicadores consolidados de conformidade (12 meses) |
| GET | `/api/relatorios/{tipo}` | PDF (`esg`, `anpd`, `comercial`) com `Content-Disposition: attachment` |
| GET | `/api/evidencias` | Histórico de solicitações atendidas |
| GET | `/api/evidencias/{protocolo}/comprovante` | PDF do comprovante |
| GET | `/api/auditoria?limite=` | Histórico de ações |

## Testes

- **Regras** (`service/regra`): prazo LGPD, classificação de prioridade, anonimização (incluindo exclusão repetida), frase-resumo e máscara de CPF, todos com `Clock` fixo (`RelogioFixo`).
- **Services** (`service`): varredura idempotente, prioridades, revogação de acesso, exclusão completa e busca da Central de clientes, montados sem Spring sobre repositórios em memória (`CenarioServicos`).
- **Controllers** (`@WebMvcTest`): busca, exclusão (201/400/422), conectar/desconectar, status de alerta, download de PDF e formato dos erros 404/422.
- **Integração** (`AtriaApplicationIntegracaoTest`): sobe a aplicação com o seed e chama todos os endpoints de consulta e os três PDFs.

## Como migrar para PostgreSQL

A persistência está atrás de interfaces com nomes do Spring Data, então a troca é quase mecânica:

1. **Dependências** no `pom.xml`: `spring-boot-starter-data-jpa`, `org.postgresql:postgresql` e, para versionar o esquema, `org.flywaydb:flyway-core` + `flyway-database-postgresql`.
2. **Configuração** em `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/atria
   spring.datasource.username=atria
   spring.datasource.password=${DB_PASSWORD}
   spring.jpa.hibernate.ddl-auto=validate
   spring.jpa.open-in-view=false
   ```
3. **Entidades:** em cada classe de `model/`, adicione `@Entity`/`@Table`, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` no `id`, `@Enumerated(EnumType.STRING)` nos enums e um construtor `protected` sem argumentos (exigência do JPA). As listas simples (`Plataforma.escopos`, `Evidencia.plataformas`) viram `@ElementCollection`. As referências por id (`titularId`, `plataformaId`…) podem continuar como `Long`, que é o caminho mais simples, ou virar `@ManyToOne(fetch = LAZY)` aos poucos. Remova `Identificavel.setId` das chamadas manuais, pois o banco gera o id.
4. **Repositórios:** troque `extends CrudRepositorio<X>` por `extends JpaRepository<X, Long>` em cada interface de `repository/`. Os métodos derivados (`findByLojistaId`, `findByTitularIdIn`, `existsByTipoOrigemAndOrigemId`, `findByLojistaIdOrderByOcorridoEmDesc`, `findByLojistaIdAndResolvidaFalse`…) já seguem a convenção e passam a ser gerados pelo Spring Data. `findByConexaoIdIn(Iterable<Long>)` funciona igual; os `deleteByTitularId` precisam de `@Transactional`.
5. **Apague** `repository/memoria/` e `CrudRepositorio`.
6. **Transações:** anote os métodos de escrita dos services com `@Transactional` (principalmente `ExclusaoService.executar`, `SolicitacaoService.concluir`, `AcessoService.revogar` e `AlertaService.executarVarredura`) para que anonimização, evidência e auditoria sejam gravadas juntas ou não sejam gravadas.
7. **Esquema:** crie `src/main/resources/db/migration/V1__esquema_inicial.sql` com as tabelas do modelo (mesmos nomes de campo, em snake_case) e índices em `lojista_id`, `titular_id`, `plataforma_id`, `(tipo_origem, origem_id)` e `protocolo` (único).
8. **Seed:** mantenha `CargaDadosFicticios` só em desenvolvimento (`@Profile("dev")`) ou converta-o em migração `V2__dados_ficticios.sql`. O teste de integração pode usar Testcontainers com PostgreSQL.

Os services, controllers, DTOs, mappers e o front-end não mudam.

## Fora do escopo desta etapa

Banco de dados real, autenticação, integração real com as APIs das plataformas e a trilha de Análise e Visualização de Dados (vendas, métricas, BI e classificador de ML; o `ClassificadorPrioridade` já é uma interface pensada para receber esse modelo).
