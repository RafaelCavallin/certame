# Especificação técnica

> **Funcionalidade:** importação de questões de concursos a partir de APIs externas · **PRD:** `tasks/prd-importacao-questoes-api-externa/prd.md`
> **Fases cobertas:** F1 (PoC, CA-01 a CA-08) e F3/F4 (MVP backend e interface, CA-09 a CA-28). F0, F2 e F5 são trabalho de negócio e curadoria, não de código.
> **Deriva de** `TECH-SPEC.md` v1.0 (§4 fronteira por domínio, §5 modelo de dados, §6 contrato da API) e depende de `tasks/prd-banco-de-questoes/techspec.md`, que funda `questao/`, `catalogo/`, `comum/auditoria` e a *check constraint* `questao_publicada_completa`.
> **Contrato do fornecedor:** validado em 2026-09-07 contra `https://qapi.otunac.com/#documentacao`. Fecha as questões Q-3 a Q-8 do PRD; Q-6 permanece aberta e é tratada por premissa explícita (ver *Pontos de integração*).

## Resumo

Esta funcionalidade acrescenta um segundo caminho de entrada para o acervo, sem tocar na régua editorial. Um pacote novo, `br.com.certame.importacao`, busca questões num fornecedor externo, traduz o payload para uma representação neutra, reprova o que o Certame não sabe exibir, resolve a taxonomia contra a árvore curada, descarta duplicatas e grava a questão aprovada como questão nativa em `EM_REVISAO` ou `RASCUNHO`. A partir daí ela é indistinguível de uma questão digitada à mão: entra na busca, na sessão de estudo, no simulado, no desempenho e no flashcard, e só chega ao estudante por publicação humana (RN-12).

O eixo do desenho é uma decisão de dependência: **o domínio nunca vê o fornecedor**. `QuestaoExterna` é a única forma que triagem, mapeamento, deduplicação e persistência enxergam; os DTOs da QAPI ficam confinados em `importacao/qapi/dto/` e são verificados por teste de arquitetura (ADR-2). Somar um fornecedor é escrever um adaptador que implementa `FornecedorQuestoes` e ligá-lo por configuração: `RegistroFornecedores` recebe todas as implementações do Spring e resolve pela `Fonte`, sem `switch` nem condicional por fornecedor em lugar nenhum. O enum `Fonte` e os `check` da migração já nascem com os quatro valores previstos pelo PRD, para que o segundo fornecedor não custe migração. O custo exato dessa troca está tabelado em *O custo real de trocar de fornecedor* e é provado por TU-40, que roda o pipeline inteiro com um fornecedor fictício.

O segundo eixo é a **reutilização deliberada da validação existente**. A importação não reimplementa regra editorial: monta um `RequisicaoQuestao` e chama `MontadorQuestao`, o mesmo que atende o formulário administrativo, e a publicação continua passando por `ValidadorPublicacao` e pela *check constraint* do banco. Duas réguas divergentes seriam a primeira fonte de bug do produto, e o PRD é explícito quanto a isso.

O terceiro eixo é a **deduplicação sem deriva**. O hash do enunciado normalizado (ADR-6) é uma **coluna gerada** em `questao`, calculada por uma função SQL imutável. Existe uma única implementação da normalização, no banco, usada tanto para gravar quanto para consultar — o Java nunca normaliza enunciado para deduplicar. Isso mata por construção a classe de bug em que a normalização do código e a do backfill divergem, e faz CA-17 (colisão contra questão digitada à mão) valer para todo o acervo pré-existente, sem migração de dados.

O lote roda **fora do request**: `POST /admin/importacoes` responde 202 com o identificador do lote, um executor `@Async` de uma thread processa candidata a candidata em transações independentes, e a tela consulta o progresso. Isso atende RNF-04, sustenta o estado "Importando 18 de 50" de RF-70 e impede que uma falha no meio desfaça o que já entrou (RF-62, CA-18).

O PoC de F1 **não é código descartável paralelo**: é o mesmo pipeline com um sinalizador `somenteSimulacao`, que grava o lote e seus itens — que são o relatório — e não grava questão, texto-base nem mapeamento (CA-05). Duas triagens diferentes entre PoC e MVP invalidariam o PoC como previsão do MVP, que é justamente para o que ele existe.

## Arquitetura do sistema

### Visão dos componentes

```
Angular 22 (SPA)                          Spring Boot 4.1                              PostgreSQL 18
────────────────                          ───────────────                              ─────────────
features/admin/importacoes/                                                            ┌── questao        (+ procedencia,
  ImportacoesPage    ──▶ GET/POST /api/v1/admin/importacoes                            │                   texto_base_id,
  ImportacaoDialog   ──▶ GET      /api/v1/admin/importacoes/fornecedores               │                   hash gerado)
  RelatorioPage      ──▶ GET      /api/v1/admin/importacoes/{id}                       ├── texto_base
                     ──▶ GET      /api/v1/admin/importacoes/{id}/itens                 ├── mapeamento_taxonomia
  MapeamentosPage    ──▶ CRUD     /api/v1/admin/mapeamentos                            ├── lote_importacao
                     ──▶ GET      /api/v1/admin/mapeamentos/pendencias                 ├── item_importacao
  DivergenciasPage   ──▶ GET/POST /api/v1/admin/divergencias                           └── divergencia_externa
features/study/
  TextoBaseCartao    ──▶ (sem rota; vem dentro de RespostaSessao)
features/admin/questoes/
  QuestaoFormPage    ──▶ GET      /api/v1/admin/questoes/{id}   (+ bloco procedencia)

                          br.com.certame.importacao                      ┌─────────────────┐
                          ─────────────────────────                      │  QAPI           │
  ImportacaoController ─▶ ImportacaoService ─▶ ExecutorLoteImportacao ──▶ │  api externa    │
                                                      │                  └─────────────────┘
                                                      ▼                    ▲  RestClient
                          TriagemCompatibilidade ─▶ ResolvedorTaxonomia ─▶ │  Q-Api-Key
                          DetectorDuplicidade    ─▶ ResolvedorTextoBase ───┘  timeout/recuo
                                                      │
                                                      ▼
                          questao/QuestaoImportadaService ─▶ MontadorQuestao (reuso)
```

**Backend — pacote novo `br.com.certame.importacao`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `importacao/FornecedorQuestoes` | novo | Porta do fornecedor: `buscar(FiltroFornecedor, int pagina)` e `buscarTexto(String codigo)`. É o único ponto por onde conteúdo externo entra (ADR-2). |
| `importacao/RegistroFornecedores` | novo | Recebe `List<FornecedorQuestoes>` por injeção e resolve `Fonte → implementação ativa`. É o que faz um adaptador novo entrar em operação só por existir como *bean*: nenhum `switch`, nenhum `if` por fornecedor em lugar nenhum do código. |
| `importacao/FiltragemLocalCandidatas` | novo | RF-39: aplica localmente os filtros que `filtrosSuportados()` disser que o fornecedor não faz, e devolve a lista dos que foram aplicados assim, para o relatório (`filtragemLocal`). Genérica sobre `QuestaoExterna` — não conhece fornecedor. |
| `importacao/QuestaoExterna` | novo | Representação neutra (RF-40). Único formato que triagem, mapeamento, dedup e persistência enxergam. |
| `importacao/TextoExterno`, `PaginaExterna`, `FiltroFornecedor` | novo | *Records* neutros de apoio à porta. |
| `importacao/ResultadoCandidata` | novo | Enum dos dez resultados de RF-66; é a chave do relatório e do `check` da tabela `item_importacao`. |
| `importacao/CatalogoFornecedor` | novo | Vocabulário do fornecedor (matérias, assuntos, bancas, órgãos) para o formulário do pedido e para a curadoria de mapeamento. Cache em memória com TTL, porque cada consulta custa crédito. |
| `importacao/qapi/PropriedadesQapi` | novo | `@ConfigurationProperties("certame.importacao.qapi")` + `@Validated`, no padrão de `PropriedadesSeguranca`: URL base, chave, tempos limite, teto de chamadas por lote e situação. |
| `importacao/qapi/ClienteQapi` | novo | `RestClient` com o header `Q-Api-Key`, tempo limite de conexão e leitura, e tradução dos códigos HTTP em `FalhaFornecedorException` classificada (ADR-5). |
| `importacao/qapi/FornecedorQapi` | novo | Implementa `FornecedorQuestoes`; pagina, respeita o teto de chamadas e devolve `QuestaoExterna`. |
| `importacao/qapi/TradutorQapi` | novo | DTO da QAPI → `QuestaoExterna`. Único lugar que conhece `opcaoA`–`opcaoE`, `materia` e `_id`. |
| `importacao/qapi/dto/` | novo | `QuestaoQapi`, `PaginaQuestoesQapi`, `TextoQapi`, `ErroQapi`. **Não saem deste pacote** (verificado por TU-23). |
| `importacao/qapi/LimitadorChamadasFornecedor` | novo | Conta chamadas por lote contra o teto configurado; estourado, encerra o lote como `PARCIAL` com causa. Protege o saldo de créditos. |
| `importacao/qapi/NovaTentativaComRecuo` | novo | Nova tentativa limitada, com recuo progressivo, **apenas** em falha transitória (rede, 5xx). Nunca em 401, 402 e 403. |
| `importacao/triagem/TriagemCompatibilidade` | novo | Orquestra a triagem e devolve `ResultadoTriagem` (resultado + motivo + pendências). |
| `importacao/triagem/DetectorTipo` | novo | RF-42: infere `ME`/`CE`; ambíguo → `TIPO_NAO_SUPORTADO`. Obrigatório, porque a QAPI não informa o tipo. |
| `importacao/triagem/ValidadorEstrutura` | novo | RF-43: 2 a 5 alternativas, ordens contíguas sem lacuna, gabarito correspondente. Não compacta letras (CA-15). |
| `importacao/triagem/ValidadorLimites` | novo | RF-44: enunciado 10–20 000, comentário até 20 000, ano 1990–2100. |
| `importacao/triagem/DetectorRecursoNaoSuportado` | novo | RF-45: marcação HTML, LaTeX, imagem, anexo, tabela e caractere de controle em qualquer campo → `RECURSO_NAO_SUPORTADO`. |
| `importacao/triagem/AvaliadorComentario` | novo | RF-46: comentário vazio de conteúdo gera **pendência**, não descarte. |
| `importacao/taxonomia/MapeamentoTaxonomia` | novo | Entidade do dicionário de RF-47. |
| `importacao/taxonomia/NormalizadorValorExterno` | novo | Sem acento, sem caixa, espaço colapsado — a chave do dicionário. |
| `importacao/taxonomia/ResolvedorTaxonomia` | novo | RF-48: mapeamento explícito → nome normalizado → sigla (banca e órgão) → não resolvido. **Nunca cria nó** (CA-13). |
| `importacao/taxonomia/MapeamentoTaxonomiaService` | novo | RF-50: listar pendências com contagem, criar, corrigir e remover; nunca reclassifica retroativamente. |
| `importacao/dedup/DetectorDuplicidade` | novo | RF-59 e RF-60: identidade externa, hash de enunciado contra todo o acervo e contra o lote em formação. |
| `importacao/dedup/HashConteudoExterno` | novo | RF-63: SHA-256 sobre a forma canônica do conteúdo externo, para detectar divergência. |
| `importacao/textobase/TextoBase` | novo | Entidade de RF-55, com `fonte` e identificador externo próprios. |
| `importacao/textobase/ResolvedorTextoBase` | novo | RF-56 e RF-58: reaproveita o texto local, busca no fornecedor só o que falta, submete o conteúdo à triagem de RF-45. |
| `importacao/lote/LoteImportacao`, `ItemImportacao` | novo | Cabeçalho do pedido e uma linha por candidata — relatório, memória de descarte e trilha de auditoria ao mesmo tempo. |
| `importacao/lote/ImportacaoService` | novo | Abre o lote, valida o pedido e a habilitação (RN-21), recusa lote concorrente e delega ao executor. |
| `importacao/lote/ExecutorLoteImportacao` | novo | `@Async("executorImportacao")`. Percorre as candidatas; cada uma em transação própria (`REQUIRES_NEW`), erro em uma não desfaz as anteriores (RF-62). |
| `importacao/lote/ProcessadorCandidata` | novo | O pipeline de uma candidata, do DTO ao `ItemImportacao`. Cada candidata termina com exatamente um resultado (RF-67, O-7). |
| `importacao/lote/RelatorioImportacaoService` | novo | RF-66 e RF-68: relatório do lote, distribuição por resultado e histórico filtrável. |
| `importacao/divergencia/DetectorDivergencia` | novo | RF-63: compara o hash externo do que foi importado com o do que a fonte devolve agora. |
| `importacao/divergencia/DivergenciaService` | novo | RF-64 e RF-65: atualiza automaticamente só o que não é `PUBLICADA` e não tem resposta; o resto vira pendência humana. |
| `importacao/MetricasImportacao` | novo | RNF-07, no padrão de `MetricasPublicacao` e `EstudoObservabilidade`. |
| `config/ExecutorImportacaoConfig` | novo | Pool de uma thread envolvido em `DelegatingSecurityContextAsyncTaskExecutor` — sem isso o `@PreAuthorize` e o ator da auditoria se perdem na thread assíncrona. |

**Backend — domínios existentes, alterados**

| Componente | Tipo | Alteração |
| --- | --- | --- |
| `questao/Questao` | modificado | Campos de procedência (RF-53), `textoBase` e a coluna gerada `hashEnunciado` (somente leitura, como `busca`). |
| `questao/Fonte` | novo | Enum `INTERNA`, `QAPI`, `QUEST_API`, `API_DAS_QUESTOES` — **os quatro valores previstos pelo PRD já nascem declarados**, no enum e nos `check` da migração, para que o segundo fornecedor não exija migração nem alteração aqui. Fica em `questao/` porque `questao.fonte` é coluna dessa tabela; `importacao/` a consome. |
| `questao/ProcedenciaQuestao` | novo | *Record* com fonte, identificador externo, URL, datas e hashes — o objeto parâmetro que evita seis argumentos no criador. |
| `questao/QuestaoImportadaService` | novo | Porta de escrita que `importacao/` usa: `criar(...)` e `atualizarConteudo(...)`. Existe para que a regra "um domínio não acessa o repositório de outro" (`code-standards.md` §3) continue valendo. Reutiliza `MontadorQuestao`. |
| `questao/QuestaoRepository` | modificado | Consultas de identidade externa, de colisão por hash e de contagem de pendências. |
| `questao/dto/QuestaoDetalhe` | modificado | Ganha `procedencia` e `textoBase` (RF-54, CA-28). |
| `questao/dto/FiltroQuestao` | modificado | Ganha `fonte` e `pendencia` (RF-72). |
| `questao/MontadorQuestao` | **inalterado** | Reutilizado como está. É o ponto do desenho em que RF-43 e RN-13 se sustentam sem duplicação. |
| `questao/ValidadorPublicacao` | **inalterado** | Continua sendo o único juiz da publicação, para importada e para manual (RN-13, CA-11). |
| `estudo/dto/QuestaoDaSerie` | modificado | Ganha `textoBase` (RF-57, CA-22). Como `simulado/` reusa `RespostaSessao`, o Modo Prova é atendido pela mesma alteração. |
| `estudo/dto/ItemCorrecao` | modificado | Ganha `textoBase`, para a tela de correção e a de resultado. |
| `estudo/MontadorRespostaSessao` | modificado | Preenche `textoBase` quando a questão tem um; `null` quando não tem. |
| `comum/excecao/` | modificado | `FornecedorIndisponivelException`, `ImportacaoDesabilitadaException`, `MapeamentoEmUsoException`. |

**Frontend — `frontend/src/app`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `core/api/importacao-api.ts` | novo | Cliente de importações, mapeamentos e divergências. |
| `core/api/modelos-importacao.ts` | novo | Tipagem dos contratos desta especificação. |
| `features/admin/importacoes/importacoes-page` | novo | RF-70: lista de lotes com situação, contagem e acesso ao relatório. |
| `features/admin/importacoes/importacao-dialog` | novo | Formulário do pedido, preenchido com o vocabulário do fornecedor. |
| `features/admin/importacoes/relatorio-page` | novo | RF-66 e RF-71: distribuição por resultado e lista de candidatas, com link para `admin/questoes/:id`. |
| `features/admin/importacoes/importacoes-store.ts` | novo | Signals do lote em andamento e a consulta de progresso enquanto `EXECUTANDO`. |
| `features/admin/importacoes/mapeamentos-page` | novo | RF-73: fila de pendências com contagem, e o CRUD de mapeamento. |
| `features/admin/importacoes/divergencias-page` | novo | RF-64: comparação lado a lado e as três decisões. |
| `features/study/texto-base-cartao` | novo | RF-57: texto de apoio antes do enunciado, mesma renderização de texto puro. |
| `features/study/questao-cartao` | modificado | Passa a exibir o texto-base quando houver. |
| `features/study/correcao-prova` | modificado | Idem, na correção e no resultado. |
| `features/admin/questoes/questao-form-page` | modificado | Bloco de procedência com link externo (CA-28). |
| `features/admin/questoes/questoes-filtros.ts` | modificado | Filtros de fonte e de pendência (RF-72). |
| `app.routes.ts` · `layout/shell` | modificado | Rotas `admin/importacoes*` sob `perfilGuard([ADMIN, SUPER_ADMIN])` e o item de menu. |
| `protótipo/Certame.dc.html` | modificado | Telas `ADMIN: IMPORTAÇÕES`, `ADMIN: MAPEAMENTOS` e `ADMIN: DIVERGÊNCIAS`, e o bloco de texto-base no cartão de questão. Entram **antes** do Angular, pela regra 1 do `AGENTS.md`. |

**Fluxo de dados.** O administrador abre o diálogo do pedido, que carrega o vocabulário do fornecedor (matérias, assuntos, bancas, órgãos) já em cache. O `POST` responde 202 com o identificador do lote, e a tela passa a consultar o progresso a cada 2 s enquanto a situação for `EXECUTANDO`. No servidor, o executor busca `ceil(quantidade ÷ 10)` páginas — o teto de `size` da QAPI é 10 — e processa cada candidata pelo pipeline abaixo, em transação própria. Ao fim, o lote fecha como `CONCLUIDO`, `PARCIAL` ou `FALHOU`, e a tela troca o progresso pelo relatório.

**Pipeline de uma candidata** — a ordem não é arbitrária: cada estágio é mais caro que o anterior, e o único estágio que gasta crédito é o penúltimo.

```
1. Tradução           DTO da QAPI → QuestaoExterna                    (sem custo)
2. Triagem            tipo → estrutura → limites → recurso → comentário
3. Identidade externa (fonte, identificador) já no acervo?            → JA_EXISTENTE | ALTERACAO_EXTERNA
4. Taxonomia          disciplina obrigatória                          → SEM_MAPEAMENTO
5. Duplicidade        hash do enunciado, acervo + lote em formação    → JA_EXISTENTE | DUPLICADA_NO_LOTE
6. Texto-base         local primeiro; chamada externa só se faltar    → INCOMPLETA   (custa 1 crédito)
7. Persistência       Questao + Alternativas + procedência + item     → IMPORTADA | IMPORTADA_COM_PENDENCIA
```

O PRD já antecipava que a triagem vem antes do mapeamento. A documentação da QAPI acrescenta um motivo econômico para o texto-base ficar no estágio 6: buscar o texto de uma candidata que seria descartada por duplicidade é crédito jogado fora, e o texto já gravado localmente dispensa a chamada (RF-58).

## Design de implementação

### Principais interfaces

```
FornecedorQuestoes                                   // porta; uma implementacao por fornecedor
  fonte() -> Fonte
  ativo() -> boolean                                 // desligado por configuracao nao aparece no registro
  filtrosSuportados() -> Set<CampoFiltro>            // o que ele nao filtra vira filtragem local (RF-39)
  tamanhoPagina() -> int                             // 10 na QAPI; outro fornecedor, outro teto
  buscar(FiltroFornecedor, int pagina) -> PaginaExterna
  buscarTexto(String codigo) -> Optional<TextoExterno>
  catalogo(TipoTaxonomia, String pai) -> List<String>

RegistroFornecedores                                 // Spring injeta List<FornecedorQuestoes>
  resolver(Fonte) -> FornecedorQuestoes              // lanca FornecedorIndisponivelException se inativo
  ativos() -> List<FornecedorQuestoes>               // alimenta GET /admin/importacoes/fornecedores

FiltragemLocalCandidatas                             // RF-39; nao conhece fornecedor
  aplicar(List<QuestaoExterna>, FiltroFornecedor, Set<CampoFiltro> suportados)
      -> ResultadoFiltragem                          // candidatas restantes + campos filtrados localmente
```

```
ImportacaoService
  solicitar(RequisicaoImportacao, UUID solicitanteId) -> LoteAceito   // 202; admin+
  relatorio(UUID loteId) -> RelatorioImportacao
  historico(FiltroImportacao, Paginacao) -> PaginaResposta<LoteResumo>
  itens(UUID loteId, ResultadoCandidata, Paginacao) -> PaginaResposta<ItemImportacaoResumo>

ExecutorLoteImportacao
  executar(UUID loteId) -> void                      // @Async; sem transacao envolvente

ProcessadorCandidata
  processar(QuestaoExterna, ContextoLote) -> ItemImportacao   // @Transactional(REQUIRES_NEW)
```

```
TriagemCompatibilidade
  avaliar(QuestaoExterna) -> ResultadoTriagem        // resultado + motivo pt-BR + pendencias

DetectorTipo
  detectar(QuestaoExterna) -> Optional<TipoQuestao>  // RF-42; vazio = ambiguo

DetectorRecursoNaoSuportado
  primeiraOcorrencia(String... campos) -> Optional<RecursoNaoSuportado>   // RF-45

AvaliadorComentario
  avaliar(String comentario, String enunciado, String gabarito) -> boolean  // RF-46
```

```
ResolvedorTaxonomia
  resolver(Fonte, QuestaoExterna) -> TaxonomiaResolvida     // nunca cria no (CA-13)

DetectorDuplicidade
  avaliar(QuestaoExterna, ContextoLote) -> Optional<ResultadoCandidata>   // RF-59, RF-60

ResolvedorTextoBase
  resolver(Fonte, String codigo) -> Optional<TextoBase>     // RF-56, RF-58

DivergenciaService
  registrar(Questao, QuestaoExterna, UUID loteId) -> Divergencia   // RF-63
  aplicar(UUID id) / ignorar(UUID id) -> void                      // RF-64; admin+
```

```
QuestaoImportadaService                              // porta de escrita em questao/
  criar(RequisicaoQuestao, ProcedenciaQuestao, UUID autorId, TextoBase) -> Questao
  atualizarConteudo(UUID id, RequisicaoQuestao, String hashExterno) -> Questao
  // ambos reutilizam MontadorQuestao; nenhum publica (RN-12)
```

```
ImportacoesStore (signals)                           // frontend
  solicitar(RequisicaoImportacao) -> Promise<void>
  loteAtual: Signal<RelatorioImportacao | null>
  emAndamento: Signal<boolean>                       // consulta a cada 2s enquanto EXECUTANDO
  progresso: Signal<{processado: number; total: number} | null>

MapeamentosStore (signals)
  pendencias: Signal<PendenciaMapeamento[]>
  mapear(RequisicaoMapeamento) -> Promise<void>
```

### Modelos de dados

Contratos JSON em `camelCase`, colunas em `snake_case`. Campo ausente na origem é serializado como `null`, nunca omitido. Datas em ISO-8601 UTC. **A chave de acesso do fornecedor não aparece em nenhum contrato desta seção, em nenhuma forma** (RN-19, CA-26).

#### `QuestaoExterna` — representação neutra (RF-40)

Não é contrato HTTP: é o tipo interno que isola o domínio do fornecedor. Está documentado aqui porque é o centro de ADR-2.

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `fonte` | `Fonte` | sim | Procedência; `QAPI` no primeiro adaptador. |
| `identificadorExterno` | `String` | sim | `_id` do fornecedor. Com `fonte`, é a identidade externa (RN-14). |
| `enunciado` | `String` | sim | Texto puro. |
| `alternativas` | `List<AlternativaExterna>` | não | Vazia em `CE`. Cada item tem `letra`, `texto` e `ordem`. |
| `gabarito` | `String` | sim | Bruto, como veio; a normalização é da triagem. |
| `comentario` | `String` | não | Ausente ou reprovado gera pendência, não descarte (RF-46). |
| `disciplinaExterna` | `String` | não | `materia` na QAPI. Sem mapeamento, descarta (RF-49). |
| `assuntoExterno` | `String` | não | Escopado pela matéria na QAPI. |
| `bancaExterna` | `String` | não | — |
| `orgaoExterno` | `String` | não | — |
| `ano` | `Short` | não | — |
| `cargoExterno` | `String` | não | Recebido e **descartado**: o Certame não modela cargo (fora de escopo do PRD). |
| `referenciaTextoBase` | `String` | não | Código do texto (`QTXT566467`). Vazio quando não há. |
| `urlExterna` | `String` | não | A QAPI não expõe URL pública por questão; fica `null`. |

```text
{
  "fonte": "QAPI",
  "identificadorExterno": "69a8224af6d9445ec04dad9c",
  "enunciado": "Em sistemas operacionais, a métrica que mede a quantidade de processos concluídos por unidade de tempo é denominada",
  "alternativas": [
    { "letra": "A", "texto": "throughput;", "ordem": 1 },
    { "letra": "B", "texto": "tempo de espera;", "ordem": 2 },
    { "letra": "C", "texto": "tempo de turnaround;", "ordem": 3 },
    { "letra": "D", "texto": "tempo de processador;", "ordem": 4 },
    { "letra": "E", "texto": "utilização do processador.", "ordem": 5 }
  ],
  "gabarito": "A",
  "comentario": "Throughput é o número de processos concluídos por unidade de tempo.",
  "disciplinaExterna": "Conhecimento Específico",
  "assuntoExterno": "Arquitetura de Computadores",
  "bancaExterna": "FGV",
  "orgaoExterno": "TJ/SE",
  "ano": 2023,
  "cargoExterno": "Analista Judiciário - Análise de Sistemas",
  "referenciaTextoBase": "QTXT566467",
  "urlExterna": null
}
```

#### `RequisicaoImportacao` — corpo de `POST /admin/importacoes`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `fonte` | `Fonte` | sim | Fornecedor ativo. `INTERNA` é recusado. |
| `quantidade` | `Short` | sim | 1 a 50 (RN-22). Vira `ceil(quantidade ÷ 10)` páginas na QAPI. |
| `materia` | `String` | não | Valor **externo**, do vocabulário do fornecedor. |
| `assunto` | `String` | não | Idem; exige `materia` na QAPI. |
| `banca` | `String` | não | Idem. |
| `orgao` | `String` | não | Idem. |
| `cargo` | `String` | não | Idem. |
| `ano` | `Short` | não | 1990 a 2100. |
| `somenteSimulacao` | `boolean` | não | Padrão `false`. `true` roda o PoC: classifica tudo e não grava questão, texto-base nem mapeamento (CA-05). Recusado quando `certame.importacao.simulacao-permitida` é falso. |

```text
{
  "fonte": "QAPI",
  "quantidade": 50,
  "materia": "Direito Constitucional",
  "assunto": null,
  "banca": "FGV",
  "orgao": null,
  "cargo": null,
  "ano": 2024,
  "somenteSimulacao": false
}
```

> **Por que os filtros são valores externos e não UUIDs internos.** A QAPI filtra por nome (`materia=Direito Constitucional`), não por identificador. Traduzir o UUID interno para o nome externo exigiria um mapeamento inverso que pode não existir ainda — e é exatamente antes da primeira importação que ele não existe. O diálogo do pedido é preenchido com o vocabulário do fornecedor, lido de `GET /admin/importacoes/fornecedores/{fonte}/catalogo`, o que também alimenta a curadoria de F2.

#### `LoteAceito` — resposta 202 de `POST /admin/importacoes`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `UUID` | sim | Identificador do lote. |
| `codigo` | `String` | sim | `IMP-00001`, emitido por sequência, no padrão de `Q-00000` e `SM-000`. |
| `status` | `StatusLote` | sim | Sempre `EXECUTANDO` nesta resposta. |

```text
{ "id": "0a3f1c4e-8b21-4d77-9a0e-5c2b8f61d4a3", "codigo": "IMP-00001", "status": "EXECUTANDO" }
```

#### `RelatorioImportacao` — resposta de `GET /admin/importacoes/{id}` (RF-66)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `UUID` | sim | — |
| `codigo` | `String` | sim | — |
| `fonte` | `Fonte` | sim | — |
| `filtros` | `object` | sim | Os filtros pedidos, como enviados. |
| `filtragemLocal` | `String[]` | sim | Filtros pedidos que o fornecedor não suporta e foram aplicados localmente (RF-39). Vazio quando todos foram aplicados na origem. |
| `solicitante` | `String` | sim | Nome de quem disparou. |
| `status` | `StatusLote` | sim | `EXECUTANDO`, `CONCLUIDO`, `PARCIAL` ou `FALHOU`. |
| `motivoInterrupcao` | `String` | não | Preenchido em `PARCIAL` e `FALHOU`, em português. |
| `somenteSimulacao` | `boolean` | sim | `true` no PoC. |
| `iniciadoEm` | `Instant` | sim | — |
| `finalizadoEm` | `Instant` | não | `null` enquanto `EXECUTANDO`. |
| `totalRecuperado` | `int` | sim | Candidatas obtidas do fornecedor. |
| `totalProcessado` | `int` | sim | Já classificadas. Sustenta "Importando 18 de 50". |
| `totalImportado` | `int` | sim | `IMPORTADA` + `IMPORTADA_COM_PENDENCIA`. |
| `chamadasExternas` | `int` | sim | Requisições gastas no lote — é o custo em créditos. |
| `distribuicao` | `object` | sim | Contagem por `ResultadoCandidata`; todos os dez resultados presentes, com zero quando não houve. |
| `pendenciasMapeamento` | `PendenciaMapeamento[]` | sim | Valores externos sem correspondência e quantas candidatas cada um afetou (CA-07). |

```text
{
  "id": "0a3f1c4e-8b21-4d77-9a0e-5c2b8f61d4a3",
  "codigo": "IMP-00001",
  "fonte": "QAPI",
  "filtros": { "materia": "Direito Constitucional", "banca": "FGV", "ano": 2024 },
  "filtragemLocal": [],
  "solicitante": "Rafael Cavallin",
  "status": "CONCLUIDO",
  "motivoInterrupcao": null,
  "somenteSimulacao": false,
  "iniciadoEm": "2026-09-07T13:04:11Z",
  "finalizadoEm": "2026-09-07T13:04:48Z",
  "totalRecuperado": 50,
  "totalProcessado": 50,
  "totalImportado": 42,
  "chamadasExternas": 12,
  "distribuicao": {
    "IMPORTADA": 38, "IMPORTADA_COM_PENDENCIA": 4, "JA_EXISTENTE": 3,
    "DUPLICADA_NO_LOTE": 0, "SEM_MAPEAMENTO": 2, "TIPO_NAO_SUPORTADO": 0,
    "RECURSO_NAO_SUPORTADO": 1, "INCOMPLETA": 2, "ALTERACAO_EXTERNA": 0, "ERRO": 0
  },
  "pendenciasMapeamento": [
    { "tipo": "DISCIPLINA", "valorExterno": "Conhecimentos de Informática", "candidatasAfetadas": 2 }
  ]
}
```

> **Lote parcial.** Quando o fornecedor interrompe, o lote fecha em `PARCIAL`, o que já foi processado permanece gravado e `motivoInterrupcao` diz a causa em português (RF-41, CA-18). `totalRecuperado` reflete o que chegou, não o que foi pedido.

```text
{
  "status": "PARCIAL",
  "motivoInterrupcao": "O fornecedor não respondeu a tempo. O lote foi interrompido em 34 de 50.",
  "totalRecuperado": 34,
  "totalProcessado": 34,
  "finalizadoEm": "2026-09-07T13:05:02Z"
}
```

#### `ItemImportacaoResumo` — linha de `GET /admin/importacoes/{id}/itens` (RF-67)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `ordem` | `int` | sim | Posição no lote. |
| `identificadorExterno` | `String` | sim | — |
| `resultado` | `ResultadoCandidata` | sim | Exatamente um por candidata (O-7). |
| `motivo` | `String` | sim | Legível, em português, sempre preenchido. |
| `pendencias` | `String[]` | sim | `SEM_COMENTARIO`, `SEM_ASSUNTO`, `SEM_BANCA`, `SEM_ORGAO`. Vazio quando não há. |
| `questaoId` | `UUID` | não | Preenchido só quando a candidata virou questão; é o link para `admin/questoes/:id` (RF-71). |
| `codigoQuestao` | `String` | não | `Q-00000` da questão criada. |

```text
{
  "content": [
    { "ordem": 1, "identificadorExterno": "69a8224af6d9445ec04dad9c", "resultado": "IMPORTADA",
      "motivo": "Questão importada e enviada para revisão.", "pendencias": [],
      "questaoId": "b71e2f90-...", "codigoQuestao": "Q-10437" },
    { "ordem": 2, "identificadorExterno": "69a8224af6d9445ec04dada1", "resultado": "IMPORTADA_COM_PENDENCIA",
      "motivo": "Importada como rascunho: o comentário do fornecedor não tem conteúdo suficiente.",
      "pendencias": ["SEM_COMENTARIO", "SEM_ORGAO"], "questaoId": "c02a...", "codigoQuestao": "Q-10438" },
    { "ordem": 3, "identificadorExterno": "69a8224af6d9445ec04dada7", "resultado": "RECURSO_NAO_SUPORTADO",
      "motivo": "O enunciado contém marcação HTML, que o Certame não exibe.",
      "pendencias": [], "questaoId": null, "codigoQuestao": null }
  ],
  "page": 0, "size": 20, "totalElements": 50
}
```

#### `ProcedenciaQuestao` — bloco novo em `QuestaoDetalhe` (RF-53, RF-54, CA-28)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `fonte` | `Fonte` | sim | `INTERNA` em questão cadastrada à mão. |
| `identificadorExterno` | `String` | não | `null` quando `INTERNA`. |
| `urlExterna` | `String` | não | Link para a origem, quando o fornecedor expõe. |
| `importadoEm` | `Instant` | não | — |
| `verificadoEm` | `Instant` | não | Última vez que uma importação reencontrou a questão. |
| `loteCodigo` | `String` | não | `IMP-00001` do lote que a trouxe. |
| `divergenciaPendente` | `boolean` | sim | `true` quando há divergência aguardando decisão. |

```text
{
  "fonte": "QAPI",
  "identificadorExterno": "69a8224af6d9445ec04dad9c",
  "urlExterna": null,
  "importadoEm": "2026-09-07T13:04:31Z",
  "verificadoEm": "2026-09-07T13:04:31Z",
  "loteCodigo": "IMP-00001",
  "divergenciaPendente": false
}
```

> **Questão cadastrada à mão.** Todas as questões que já existem no acervo recebem `fonte: "INTERNA"` pelo `default` da migração, sem nenhuma outra alteração — a ficha administrativa mostra o bloco reduzido.

```text
{ "fonte": "INTERNA", "identificadorExterno": null, "urlExterna": null,
  "importadoEm": null, "verificadoEm": null, "loteCodigo": null, "divergenciaPendente": false }
```

#### `TextoBaseResumo` — texto de apoio (RF-55, RF-57)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `UUID` | sim | — |
| `titulo` | `String` | não | `titulo` da QAPI. |
| `conteudo` | `String` | sim | Texto puro, já aprovado pela triagem de RF-45. |
| `referencia` | `String` | não | O campo `fonte` da QAPI (`"Machado de Assis"`). Chamado de `referencia` no Certame para não colidir com `fonte`, que aqui significa procedência. |

```text
{
  "id": "6f0b1d22-...",
  "titulo": "A reinvenção da vírgula",
  "conteudo": "No começo de 1902, Machado de Assis...",
  "referencia": "Machado de Assis"
}
```

> **Questão sem texto-base.** O campo `textoBase` vem `null` em `QuestaoDaSerie`, `ItemCorrecao` e `QuestaoDetalhe`, e a interface não renderiza o bloco. É o caso de toda questão hoje no acervo — o comportamento atual não muda em nada (RI-10).

```text
{ "textoBase": null }
```

#### `PendenciaMapeamento` e `RequisicaoMapeamento` (RF-47, RF-50, RF-73)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `tipo` | `TipoTaxonomia` | sim | `DISCIPLINA`, `ASSUNTO`, `BANCA` ou `ORGAO`. |
| `valorExterno` | `String` | sim | Como veio do fornecedor, sem normalizar — é o que o administrador lê. |
| `valorExternoPai` | `String` | não | Obrigatório em `ASSUNTO`: a matéria que dá sentido ao assunto na QAPI. |
| `candidatasAfetadas` | `int` | sim | Só em `PendenciaMapeamento`. Quantas candidatas o valor já custou. |
| `alvoId` | `UUID` | sim | Só em `RequisicaoMapeamento`. Entidade interna correspondente. |

```text
{ "fonte": "QAPI", "tipo": "DISCIPLINA", "valorExterno": "Conhecimentos de Informática",
  "valorExternoPai": null, "alvoId": "3d9a7c11-..." }
```

#### `DivergenciaExternaDetalhe` (RF-63, RF-64)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `UUID` | sim | — |
| `questaoId` / `codigoQuestao` | `UUID` / `String` | sim | Questão local afetada. |
| `situacao` | `String` | sim | `PENDENTE`, `APLICADA` ou `IGNORADA`. |
| `prioridade` | `String` | sim | `ALTA` quando a questão foi anulada na origem. |
| `bloqueadaPorResposta` | `boolean` | sim | `true` quando há resposta de estudante — a atualização automática é proibida (RN-17). |
| `local` / `externo` | `object` | sim | Enunciado, alternativas, gabarito e classificação de cada lado, para a comparação lado a lado. |

```text
{
  "id": "9c4d...", "questaoId": "b71e2f90-...", "codigoQuestao": "Q-10437",
  "situacao": "PENDENTE", "prioridade": "NORMAL", "bloqueadaPorResposta": true,
  "local":   { "gabarito": "A", "enunciado": "Em sistemas operacionais, a métrica..." },
  "externo": { "gabarito": "C", "enunciado": "Em sistemas operacionais, a métrica..." }
}
```

#### `ErroApi` — envelope de erro (`application/problem+json`)

Reaproveita `comum/ErroApi` e o `ManipuladorErrosGlobal` existentes. Códigos novos desta funcionalidade:

| Código | HTTP | Significado |
| --- | --- | --- |
| `DADOS_INVALIDOS` | `422` | Quantidade fora de 1–50, ano fora da faixa, fonte inexistente. Com `fieldErrors`. |
| `IMPORTACAO_DESABILITADA` | `409` | `certame.importacao.habilitada` é falso — gates G-01 a G-04 pendentes (RN-21). |
| `IMPORTACAO_EM_ANDAMENTO` | `409` | Já existe lote `EXECUTANDO` para a fonte. Protege o saldo de créditos. |
| `SIMULACAO_NAO_PERMITIDA` | `409` | `somenteSimulacao` pedido num ambiente onde o PoC não roda. |
| `FORNECEDOR_INDISPONIVEL` | `502` | Falha de comunicação ao validar o pedido, antes de abrir o lote. |
| `MAPEAMENTO_EM_USO` | `409` | Remoção de mapeamento recusada. |
| `TRANSICAO_INVALIDA` | `409` | Decisão sobre divergência já decidida. |
| `RECURSO_NAO_ENCONTRADO` | `404` | Lote, mapeamento ou divergência inexistente. |

```text
{
  "code": "IMPORTACAO_DESABILITADA",
  "message": "A importação está desabilitada neste ambiente.",
  "fieldErrors": null
}
```

> **Nunca vaza credencial.** A falha do fornecedor é traduzida antes de sair: o `message` descreve a causa em português e **não** repassa o corpo da resposta externa nem a chave (RN-19, CA-26). O `{"error": "..."}` da QAPI fica no log de nível `warn`, sem o header de autenticação.

#### Mapeamento QAPI → `QuestaoExterna`

| Origem (QAPI `/api/questoes`) | Destino (`QuestaoExterna`) | Observação |
| --- | --- | --- |
| `_id` | `identificadorExterno` | Com `fonte`, forma a identidade externa (RN-14). |
| `enunciado` | `enunciado` | — |
| `opcaoA` … `opcaoE` | `alternativas[]` | Ordem 1 a 5 pela posição. **Opção vazia no meio descarta a candidata** (CA-15) — compactar deslocaria as letras e invalidaria o gabarito. |
| `gabarito` | `gabarito` | Letra em `ME`; em `CE`, normalizado pela triagem. |
| `comentario` | `comentario` | Vazio ou raso vira pendência (RF-46). |
| `materia` | `disciplinaExterna` | Sem mapeamento, descarta (RF-49). |
| `assunto` | `assuntoExterno` | Escopado por `materia`. |
| `banca` | `bancaExterna` | — |
| `orgao` | `orgaoExterno` | — |
| `ano` | `ano` | — |
| `cargo` | `cargoExterno` | Recebido e descartado: cargo não é modelado. |
| `texto` | `referenciaTextoBase` | **String vazia quando não há texto-base**, conforme a documentação — tratada como ausência. |
| — | `dificuldade` | **A QAPI não informa dificuldade.** Toda questão importada entra com `null` (ver *Riscos conhecidos*). |
| — | `tipo` | **A QAPI não informa o tipo.** Sempre inferido por RF-42. |
| — | `urlExterna` | A QAPI não expõe URL pública por questão. |

#### Mapeamento QAPI `/api/textos/{codigo}` → `TextoBase`

| Origem | Destino | Observação |
| --- | --- | --- |
| `codigo` | `identificadorExterno` | Com `fonte`, garante a deduplicação de RF-58. |
| `titulo` | `titulo` | — |
| `texto` | `conteudo` | Passa pela triagem de RF-45 igual ao enunciado. |
| `fonte` | `referencia` | Renomeado para não colidir com `fonte` = procedência. |
| `createdAt` | — | Descartado. |

#### Parâmetros fixos na origem

| API | Parâmetros principais |
| --- | --- |
| **QAPI — questões** | `GET /api/questoes?page&size&banca&orgao&cargo&materia&assunto&ano`, header `Q-Api-Key`. `page` começa em **1**; `size` máximo **10**. Envelope `{page, size, pages, total, questoes[]}`. |
| **QAPI — texto-base** | `GET /api/textos/{codigo}`, header `Q-Api-Key`. |
| **QAPI — catálogos** | `GET /api/materias`, `/api/assuntos?materia=`, `/api/bancas`, `/api/orgaos`, `/api/cargos`. Envelope `{<plural>: string[], total, page, pages}`. |
| **Filtros não suportados** | Não há filtro de **dificuldade**. Se o pedido trouxer um, a filtragem é local e o relatório registra em `filtragemLocal` (RF-39). |
| **Custo** | Cada página consome 1 requisição de billing. Um lote de 50 gasta 5 páginas + 1 por texto-base ainda não gravado localmente. |

#### Esquema de banco — `V19__importacao_questoes_externas.sql`

A próxima versão livre é **V19** (as aplicadas vão até `V18__simulado_composicao_dificuldade.sql`). Tudo aditivo: nenhuma coluna existente é alterada ou removida, e nenhuma migração aplicada é editada (R-4, RNF-05).

O conjunto de valores de `fonte` já nasce com **todos os fornecedores previstos pelo PRD**, não só o primeiro. É o que permite acrescentar o segundo adaptador sem migração nenhuma — ver *O custo real de trocar de fornecedor*.

```sql
create table texto_base (
  id uuid primary key default gen_random_uuid(),
  fonte text not null check (fonte in ('INTERNA','QAPI','QUEST_API','API_DAS_QUESTOES')),
  identificador_externo text, titulo text, conteudo text not null, referencia text,
  criado_em timestamptz not null default now()
);
create unique index texto_base_externo_uk on texto_base (fonte, identificador_externo)
  where identificador_externo is not null;

alter table questao add column fonte text not null default 'INTERNA'
  check (fonte in ('INTERNA','QAPI','QUEST_API','API_DAS_QUESTOES'));
alter table questao add column identificador_externo text;
alter table questao add column url_externa text;
alter table questao add column importado_em timestamptz;
alter table questao add column verificado_em timestamptz;
alter table questao add column hash_externo text;
alter table questao add column lote_importacao_id uuid;
alter table questao add column texto_base_id uuid references texto_base(id);
create unique index questao_fonte_externa_uk on questao (fonte, identificador_externo)
  where identificador_externo is not null;
```

A normalização de ADR-6 é **uma única implementação, em SQL**, usada para gravar e para consultar. `certame_unaccent` já existe desde `V4`.

```sql
create or replace function certame_enunciado_normalizado(entrada text) returns text
language sql immutable parallel safe as $$
  select btrim(regexp_replace(
           regexp_replace(lower(certame_unaccent(coalesce(entrada, ''))), '[^a-z0-9]+', ' ', 'g'),
           '\s+', ' ', 'g'))
$$;

create or replace function certame_hash_enunciado(entrada text) returns text
language sql immutable parallel safe as $$
  select encode(sha256(certame_enunciado_normalizado(entrada)::bytea), 'hex')
$$;

alter table questao add column hash_enunciado text
  generated always as (certame_hash_enunciado(enunciado)) stored;
create index questao_hash_enunciado_idx on questao (hash_enunciado);
```

> **Por que coluna gerada e não cálculo em Java.** O hash passa a existir para **toda** questão do acervo, inclusive as digitadas à mão antes desta funcionalidade, sem backfill e sem risco de a normalização do Java divergir da do banco. É o que faz CA-17 valer contra o acervo pré-existente. O Java consulta com `certame_hash_enunciado(:enunciado)` — a mesma função — e nunca normaliza enunciado por conta própria.

```sql
create table mapeamento_taxonomia (
  id uuid primary key default gen_random_uuid(),
  fonte text not null check (fonte in ('QAPI','QUEST_API','API_DAS_QUESTOES')),
  tipo text not null check (tipo in ('DISCIPLINA','ASSUNTO','BANCA','ORGAO')),
  valor_externo text not null, valor_externo_original text not null, valor_externo_pai text,
  disciplina_id uuid references disciplina(id), assunto_id uuid references assunto(id),
  banca_id uuid references banca(id), orgao_id uuid references orgao(id),
  criado_por uuid not null references usuario(id),
  criado_em timestamptz not null default now(), atualizado_em timestamptz not null default now(),
  constraint mapeamento_alvo_unico check (
    (case when disciplina_id is not null then 1 else 0 end)
  + (case when assunto_id  is not null then 1 else 0 end)
  + (case when banca_id    is not null then 1 else 0 end)
  + (case when orgao_id    is not null then 1 else 0 end) = 1)
);
create unique index mapeamento_taxonomia_uk
  on mapeamento_taxonomia (fonte, tipo, valor_externo, coalesce(valor_externo_pai, ''));
```

> **`valor_externo_pai` não é enfeite.** Na QAPI, assunto só existe dentro de uma matéria (`GET /api/assuntos?materia=`): *Hibernate Cache* só significa alguma coisa sob *Tecnologia Java*. Sem o pai na chave, dois assuntos homônimos de matérias diferentes colidiriam num mapeamento só.

```sql
create sequence lote_importacao_codigo_seq start with 1;
create table lote_importacao (
  id uuid primary key default gen_random_uuid(),
  codigo text not null unique,
  fonte text not null check (fonte in ('QAPI','QUEST_API','API_DAS_QUESTOES')),
  filtros jsonb not null,
  quantidade_pedida smallint not null check (quantidade_pedida between 1 and 50),
  status text not null check (status in ('EXECUTANDO','CONCLUIDO','PARCIAL','FALHOU')),
  motivo_interrupcao text, somente_simulacao boolean not null default false,
  total_recuperado integer not null default 0, total_processado integer not null default 0,
  total_importado integer not null default 0, chamadas_externas integer not null default 0,
  solicitante_id uuid not null references usuario(id),
  iniciado_em timestamptz not null default now(), finalizado_em timestamptz
);
create index lote_importacao_historico_idx on lote_importacao (fonte, status, iniciado_em desc);

create table item_importacao (
  id uuid primary key default gen_random_uuid(),
  lote_id uuid not null references lote_importacao(id) on delete cascade,
  ordem integer not null, identificador_externo text not null,
  resultado text not null check (resultado in ('IMPORTADA','IMPORTADA_COM_PENDENCIA','JA_EXISTENTE',
    'DUPLICADA_NO_LOTE','SEM_MAPEAMENTO','TIPO_NAO_SUPORTADO','RECURSO_NAO_SUPORTADO','INCOMPLETA',
    'ALTERACAO_EXTERNA','ERRO')),
  motivo text not null, pendencias jsonb not null default '[]'::jsonb,
  valor_externo_nao_mapeado text, tipo_taxonomia_nao_mapeada text,
  questao_id uuid references questao(id), criado_em timestamptz not null default now()
);
create index item_importacao_lote_idx on item_importacao (lote_id, ordem);
create index item_importacao_externo_idx on item_importacao (identificador_externo);
create index item_importacao_pendencia_idx on item_importacao (tipo_taxonomia_nao_mapeada, valor_externo_nao_mapeado)
  where valor_externo_nao_mapeado is not null;

create table divergencia_externa (
  id uuid primary key default gen_random_uuid(),
  questao_id uuid not null references questao(id),
  lote_id uuid references lote_importacao(id),
  hash_anterior text not null, hash_novo text not null, conteudo_externo jsonb not null,
  situacao text not null check (situacao in ('PENDENTE','APLICADA','IGNORADA')),
  prioridade text not null default 'NORMAL' check (prioridade in ('NORMAL','ALTA')),
  detectada_em timestamptz not null default now(),
  decidida_em timestamptz, decidido_por uuid references usuario(id)
);
create unique index divergencia_pendente_uk on divergencia_externa (questao_id) where situacao = 'PENDENTE';
```

> **`conteudo_externo` fica aqui, não na auditoria.** RF-69 proíbe cópia de conteúdo bruto do fornecedor no *payload* de `auditoria`. A divergência é outra coisa: sem guardar o lado externo não há como mostrar a comparação nem aplicar a decisão depois. A auditoria registra identificador, hashes e decisão — nunca o texto.

Seeds de desenvolvimento em `db/migration-dev/V912__seed_importacao_dev.sql` (faixa `V9xx`), com um lote de exemplo, mapeamentos iniciais e um texto-base compartilhado por duas questões.

### Endpoints da API

Base `/api/v1`. **Todo endpoint desta seção exige `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")`** (RNF-10, CA-25) e será registrado na `TECH-SPEC.md` §6 na mesma alteração que o implementar (regra 4 do `AGENTS.md`, RNF-11, R-5).

#### Visão geral

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/admin/importacoes` | Solicita um lote. Responde 202. |
| `GET` | `/admin/importacoes` | Histórico paginado e filtrável (RF-68). |
| `GET` | `/admin/importacoes/{id}` | Relatório do lote e progresso (RF-66). |
| `GET` | `/admin/importacoes/{id}/itens` | Candidatas do lote, com resultado e motivo (RF-67). |
| `GET` | `/admin/importacoes/fornecedores` | Fornecedores configurados, situação e filtros suportados. |
| `GET` | `/admin/importacoes/fornecedores/{fonte}/catalogo` | Vocabulário do fornecedor para o formulário e a curadoria. |
| `GET` | `/admin/mapeamentos` | Dicionário de mapeamentos (RF-47). |
| `POST` · `PUT` · `DELETE` | `/admin/mapeamentos`, `/admin/mapeamentos/{id}` | Gestão dos mapeamentos (RF-50). |
| `GET` | `/admin/mapeamentos/pendencias` | Valores externos não mapeados com contagem (RF-73). |
| `GET` | `/admin/divergencias` | Divergências, filtráveis por situação. |
| `POST` | `/admin/divergencias/{id}/aplicar` · `/ignorar` | Decisão humana (RF-64). |

Endpoints existentes que **mudam de corpo** (nenhuma rota nova): `GET /admin/questoes` ganha os filtros `fonte` e `pendencia`; `GET /admin/questoes/{id}` ganha `procedencia` e `textoBase`; `POST /estudo/sessoes`, `GET /estudo/sessoes/{id}` e `POST /estudo/sessoes/{id}/finalizar` ganham `textoBase` dentro de `QuestaoDaSerie` e `ItemCorrecao` — o que cobre também `/simulados/{id}/tentativas*`, porque reusam os mesmos *records*.

---

#### `POST /api/v1/admin/importacoes`

Solicita um lote. Valida, abre o registro e devolve imediatamente; o processamento é assíncrono.

**Corpo** — `RequisicaoImportacao` (ver *Modelos de dados*)

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `fonte` | `Fonte` | — | Obrigatório, ativo, diferente de `INTERNA`. |
| `quantidade` | `Short` | — | Obrigatório, 1 a 50 (RN-22). |
| `materia`, `assunto`, `banca`, `orgao`, `cargo` | `String` | `null` | Valores do vocabulário do fornecedor. `assunto` exige `materia`. |
| `ano` | `Short` | `null` | 1990 a 2100. |
| `somenteSimulacao` | `boolean` | `false` | `true` só onde o PoC é permitido. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `202` | `LoteAceito` | Pedido aceito; lote em `EXECUTANDO`. |
| `409` | `ErroApi` | `IMPORTACAO_DESABILITADA`, `IMPORTACAO_EM_ANDAMENTO` ou `SIMULACAO_NAO_PERMITIDA`. |
| `422` | `ErroApi` | Quantidade, ano ou fonte inválidos, com `fieldErrors`. |
| `502` | `ErroApi` | `FORNECEDOR_INDISPONIVEL` na verificação inicial. |
| `403` | — | Estudante ou professor (CA-25). |

**Exemplo — sucesso**

```http
POST /api/v1/admin/importacoes
```

```text
{ "id": "0a3f1c4e-8b21-4d77-9a0e-5c2b8f61d4a3", "codigo": "IMP-00001", "status": "EXECUTANDO" }
```

**Exemplo — quantidade fora do limite**

```http
POST /api/v1/admin/importacoes
```

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Verifique os campos destacados.",
  "fieldErrors": { "quantidade": "Peça entre 1 e 50 questões por importação." }
}
```

> A tela desabilita o botão e mostra a mensagem no campo, sem toast, como já faz o formulário de questão.

**Exemplo — gates de licenciamento pendentes (RN-21)**

```http
POST /api/v1/admin/importacoes
```

```text
{ "code": "IMPORTACAO_DESABILITADA",
  "message": "A importação está desabilitada neste ambiente.", "fieldErrors": null }
```

---

#### `GET /api/v1/admin/importacoes/{id}`

Relatório do lote. É também a fonte do progresso enquanto o lote roda.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RelatorioImportacao` | Lote existente. |
| `404` | `ErroApi` | `RECURSO_NAO_ENCONTRADO`. |
| `403` | — | Perfil sem permissão. |

**Exemplo — em andamento**

```http
GET /api/v1/admin/importacoes/0a3f1c4e-8b21-4d77-9a0e-5c2b8f61d4a3
```

```text
{
  "codigo": "IMP-00001", "status": "EXECUTANDO",
  "totalRecuperado": 50, "totalProcessado": 18, "totalImportado": 15,
  "finalizadoEm": null,
  "distribuicao": { "IMPORTADA": 14, "IMPORTADA_COM_PENDENCIA": 1, "JA_EXISTENTE": 2,
                    "DUPLICADA_NO_LOTE": 0, "SEM_MAPEAMENTO": 1, "TIPO_NAO_SUPORTADO": 0,
                    "RECURSO_NAO_SUPORTADO": 0, "INCOMPLETA": 0, "ALTERACAO_EXTERNA": 0, "ERRO": 0 }
}
```

> A tela consulta a cada 2 s enquanto `status` for `EXECUTANDO` e mostra "Importando 18 de 50". Chegando a `CONCLUIDO`, `PARCIAL` ou `FALHOU`, a consulta para e o relatório substitui o progresso.

**Exemplo — concluído** — ver `RelatorioImportacao` em *Modelos de dados*.

---

#### `GET /api/v1/admin/importacoes/{id}/itens`

Uma linha por candidata avaliada (RF-67). Nenhuma candidata processada fica sem resultado (O-7).

**Parâmetros de consulta**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `resultado` | `ResultadoCandidata` | `null` | Filtra por um dos dez resultados. |
| `page` · `size` | `int` | `0` · `20` | `size` máximo 100. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `PaginaResposta<ItemImportacaoResumo>` | Sempre, inclusive vazio. |
| `404` | `ErroApi` | Lote inexistente. |

**Exemplo — sucesso** — ver `ItemImportacaoResumo` em *Modelos de dados*.

**Exemplo — nenhum item com o resultado pedido**

```http
GET /api/v1/admin/importacoes/0a3f1c4e-.../itens?resultado=ERRO
```

```text
{ "content": [], "page": 0, "size": 20, "totalElements": 0 }
```

> A tela mostra "Nenhuma candidata com este resultado", pelo `estado-vazio` que já existe em `shared/ui`.

---

#### `GET /api/v1/admin/importacoes/fornecedores/{fonte}/catalogo`

Vocabulário do fornecedor, para preencher o formulário do pedido e a tela de mapeamento sem adivinhação. Servido de cache em memória com TTL configurável, porque cada consulta ao fornecedor custa crédito.

**Parâmetros de consulta**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `tipo` | `TipoTaxonomia` | — | Obrigatório. |
| `pai` | `String` | `null` | Obrigatório quando `tipo=ASSUNTO`: a matéria. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `{ "valores": string[], "atualizadoEm": Instant, "doCache": boolean }` | — |
| `422` | `ErroApi` | `ASSUNTO` sem `pai`. |
| `502` | `ErroApi` | `FORNECEDOR_INDISPONIVEL` e cache vazio. |

**Exemplo — sucesso**

```http
GET /api/v1/admin/importacoes/fornecedores/QAPI/catalogo?tipo=DISCIPLINA
```

```text
{
  "valores": ["Direito Administrativo", "Direito Constitucional", "Direito Civil"],
  "atualizadoEm": "2026-09-07T12:40:00Z",
  "doCache": true
}
```

> **Degradação.** Fornecedor fora do ar com cache preenchido devolve 200 e `doCache: true` — não faz sentido bloquear a curadoria de mapeamento por indisponibilidade momentânea, já que ela não grava nada no fornecedor.

---

#### `GET /api/v1/admin/mapeamentos/pendencias`

Fila de RF-73: o que ainda não tem correspondência interna e quanto isso já custou.

**Parâmetros de consulta**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `fonte` | `Fonte` | — | Obrigatório. |
| `tipo` | `TipoTaxonomia` | `null` | — |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `PendenciaMapeamento[]` | Ordenado por `candidatasAfetadas` desc. |

**Exemplo — sucesso**

```http
GET /api/v1/admin/mapeamentos/pendencias?fonte=QAPI
```

```text
[
  { "tipo": "DISCIPLINA", "valorExterno": "Conhecimentos de Informática",
    "valorExternoPai": null, "candidatasAfetadas": 12 },
  { "tipo": "ASSUNTO", "valorExterno": "Hibernate Cache",
    "valorExternoPai": "Tecnologia Java", "candidatasAfetadas": 3 }
]
```

> Cada linha leva ao diálogo de mapeamento, que só oferece nós **existentes** da árvore — a interface não tem caminho para criar disciplina ou assunto a partir daqui (RN-15, CA-13).

---

#### `POST /api/v1/admin/mapeamentos`

**Corpo** — `RequisicaoMapeamento` (ver *Modelos de dados*)

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `MapeamentoDetalhe` | Criado; vale para as **próximas** importações. |
| `409` | `ErroApi` | Já existe mapeamento para `(fonte, tipo, valorExterno, valorExternoPai)`. |
| `422` | `ErroApi` | `alvoId` inexistente, arquivado, ou de tipo incompatível. |

> **Nunca reclassifica retroativamente** (RF-50). Corrigir o mapeamento não toca em questão já gravada — reclassificar é edição de questão, com auditoria própria.

---

#### `POST /api/v1/admin/divergencias/{id}/aplicar`

Aplica o conteúdo externo sobre a questão local. Ação humana, sempre.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `DivergenciaExternaDetalhe` | Aplicada; `situacao: "APLICADA"`. |
| `409` | `ErroApi` | `TRANSICAO_INVALIDA` — divergência já decidida. |
| `422` | `ErroApi` | O conteúdo externo não passa em `ValidadorPublicacao` e a questão está publicada. |

**Exemplo — recusa por régua editorial**

```http
POST /api/v1/admin/divergencias/9c4d.../aplicar
```

```text
{
  "code": "PUBLICACAO_INVALIDA",
  "message": "Verifique os campos destacados.",
  "fieldErrors": { "comentario": "O comentário do professor é obrigatório para publicar." }
}
```

> A tela pede confirmação explícita antes de aplicar sobre questão publicada, pelo `confirmacao-dialog` que já existe. Nenhum registro de `resposta` é tocado em nenhuma das três decisões — o banco já garante isso pelas regras `do instead nothing` (RN-17, CA-21).

---

## Pontos de integração

**Serviço externo.** QAPI, `https://qapi.otunac.com/api`, REST/JSON, autenticada por header `Q-Api-Key: qb_<40 caracteres>`. É a **primeira e única** chamada de saída do projeto — a varredura por `RestClient`, `RestTemplate`, `WebClient` e `java.net.http` no código atual não retorna nada.

**Configuração** (`@ConfigurationProperties("certame.importacao.qapi")` + `@Validated`, padrão de `PropriedadesSeguranca`):

| Propriedade | Padrão | Origem |
| --- | --- | --- |
| `url-base` | `https://qapi.otunac.com/api` | `QAPI_URL` |
| `chave` | — (obrigatória quando ativa) | `QAPI_KEY` |
| `tempo-limite-conexao` | `5s` | RNF-01 |
| `tempo-limite-leitura` | `15s` | RNF-01 |
| `tamanho-pagina` | `10` | Teto do fornecedor |
| `chamadas-maximas-por-lote` | `120` | Controle de custo |
| `tentativas-maximas` | `2` | RNF-02 |
| `recuo-inicial` | `500ms` | Recuo progressivo |
| `ttl-catalogo` | `1h` | Cache do vocabulário |
| `ativo` | `false` | `QAPI_ATIVO` |

E, fora do fornecedor: `certame.importacao.habilitada` (RN-21, falso em produção até G-01 a G-04) e `certame.importacao.simulacao-permitida` (PoC; verdadeiro em `dev`, falso em `prod`). A chave **nunca** vai ao frontend, a resposta de API, a tela ou o log (RN-19, CA-06, CA-26).

**Tratamento de erro** (RF-41). Cada código vira uma classificação, e nenhum derruba o lote inteiro:

| Situação | Classificação | Efeito |
| --- | --- | --- |
| Tempo esgotado, falha de rede, `500` | transitória | Nova tentativa com recuo, até `tentativas-maximas`. Persistindo, lote `PARCIAL`. |
| `401` chave ausente/inválida, `403` chave suspensa | permanente | Lote `FALHOU` na hora. Sem nova tentativa — repetir não conserta e gasta crédito. |
| `402` sem créditos | permanente | Lote `PARCIAL` com motivo "O plano do fornecedor está sem créditos."; alerta em métrica e log `error`. |
| `400` parâmetro inválido | permanente | Lote `FALHOU`; indica defeito nosso na montagem do filtro. |
| `404` no texto-base | por candidata | Candidata `INCOMPLETA` (RF-56, CA-23). O lote segue. |
| Corpo malformado | por candidata | Candidata `ERRO` com motivo. O lote segue. |

**Premissas explícitas onde a documentação não responde.** A documentação não descreve questões de certo/errado (Q-6 permanece aberta), nem o comportamento de `opcaoA`–`opcaoE` ausentes ou nulas. O desenho é defensivo e não presume: `DetectorTipo` classifica como `ME` quando há duas ou mais opções preenchidas; como `CE` quando não há nenhuma opção **e** o gabarito, normalizado, pertence a `{C, E, CERTO, ERRADO, V, F, VERDADEIRO, FALSO}`; qualquer outra combinação é `TIPO_NAO_SUPORTADO` com motivo. Nada é importado em silêncio num tipo que o Certame não sabe exibir (RF-42, RN-16). O PoC de F1 mede quantas candidatas caem em cada caso — é essa medição que fecha Q-6.

**O que a integração não faz.** Nenhum fluxo de estudo, caderno, simulado, desempenho ou flashcard chama a QAPI, em nenhum momento (RN-18, O-5, CA-24). A dependência é unidirecional e vive inteiramente dentro de `importacao/`, verificada por TI-18.

## Abordagem de testes

Cobertura mínima **80%** por módulo, conforme `.agents/rules/tests.md` — que prevalece sobre qualquer outra regra de cobertura — com JaCoCo aplicando o piso em `verify` sobre as classes `*Service`. Pirâmide alvo ~70/20/10. Todo dublê é da **fronteira**: `FornecedorQuestoes` é substituído por um *fake* em memória, e nenhum teste toca a rede.

**CA-01, CA-03 e CA-08 não são automatizáveis.** São resultados de campo do PoC — taxa de aproveitamento e reprovação do fornecedor —, medidos executando F1 contra a API real com chave real. Ficam registrados como verificação manual, com o relatório do lote `somenteSimulacao` como evidência.

### Testes de unidade

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `detectaMultiplaEscolhaComDuasOuMaisOpcoes` | CA-09 | `ME` inferido, mesmo sem o fornecedor informar o tipo. |
| TU-02 | `detectaCertoErradoSemOpcoesComGabaritoEquivalente` | CA-09 | `CE`; gabarito normalizado para `CERTO`/`ERRADO`. |
| TU-03 | `recusaCombinacaoAmbiguaComoTipoNaoSuportado` | CA-14 | `TIPO_NAO_SUPORTADO` com motivo legível. |
| TU-04 | `descartaAlternativasComLacunaSemCompactarLetras` | CA-15 | A, B, D, E com C vazia → descarte; nenhuma letra realinhada. |
| TU-05 | `descartaGabaritoSemAlternativaCorrespondente` | CA-14 | `INCOMPLETA`. |
| TU-06 | `recusaEnunciadoForaDosLimitesDoModelo` | CA-14 | Abaixo de 10 e acima de 20 000 caracteres → `INCOMPLETA`. |
| TU-07 | `descartaMarcacaoHtmlEmQualquerCampo` | CA-14 | `RECURSO_NAO_SUPORTADO`, testado em enunciado, alternativa, comentário e texto-base. |
| TU-08 | `descartaDelimitadorLatex` | CA-14 | Idem. |
| TU-09 | `descartaReferenciaImagemOuAnexo` | CA-14 | Idem. |
| TU-10 | `descartaTabelaECaractereDeControle` | CA-14 | Idem. |
| TU-11 | `sinalizaComentarioAusenteSemDescartarCandidata` | CA-11 | Pendência `SEM_COMENTARIO`; a candidata segue no pipeline. |
| TU-12 | `reprovaComentarioApenasComALetraDoGabarito` | CA-11 | Pendência, não descarte. |
| TU-13 | `reprovaComentarioQueRepeteOEnunciado` | CA-11 | Pendência. |
| TU-14 | `resolveTaxonomiaPeloMapeamentoExplicitoAntesDoNome` | CA-12 | Mapeamento vence correspondência por nome. |
| TU-15 | `resolveBancaPorSiglaQuandoNaoHaMapeamento` | CA-12 | Sigla normalizada resolve; assunto não usa sigla. |
| TU-16 | `naoCriaNoDeTaxonomiaQuandoValorExternoEDesconhecido` | CA-13 | Nenhuma escrita em disciplina, assunto, banca ou órgão. |
| TU-17 | `descartaCandidataQuandoDisciplinaNaoTemMapeamento` | CA-12 | `SEM_MAPEAMENTO`; valor externo registrado no item. |
| TU-18 | `importaSemAssuntoBancaOuOrgaoComPendenciaSinalizada` | CA-10 | Candidata entra; pendências registradas. |
| TU-19 | `traduzPayloadDaQapiParaQuestaoExterna` | CA-09 | `opcaoA`–`opcaoE` viram alternativas 1–5; `texto` vazio vira ausência. |
| TU-20 | `confinaDtosDoFornecedorAoPacoteDoAdaptador` | — | Teste de arquitetura: nenhuma classe fora de `importacao.qapi` referencia `importacao.qapi.dto`. |
| TU-21 | `hashExternoMudaQuandoGabaritoMuda` | CA-19 | Hashes diferentes ⇒ divergência detectável. |
| TU-22 | `hashExternoEEstavelParaOMesmoConteudo` | CA-04 | Duas traduções do mesmo payload → mesmo hash. |
| TU-23 | `interrompeLoteAoAtingirTetoDeChamadas` | CA-18 | Lote `PARCIAL` com motivo; nada perdido. |
| TU-24 | `repeteApenasFalhaTransitoriaComRecuoProgressivo` | CA-18 | 500 repete; 401, 402 e 403 não. |
| TU-25 | `classificaSemCreditosComoLoteParcial` | CA-18 | `402` → `PARCIAL`, motivo em português. |
| TU-26 | `gravaCandidataAprovadaEmRevisao` | CA-10 | `EM_REVISAO`, código próprio, autor = solicitante, procedência preenchida. |
| TU-27 | `gravaCandidataSemComentarioEmRascunho` | CA-11 | `RASCUNHO` com pendência. |
| TU-28 | `descartaRepeticaoDentroDoProprioLote` | CA-16 | `DUPLICADA_NO_LOTE`. |
| TU-29 | `erroEmUmaCandidataNaoDesfazAsAnteriores` | CA-18 | Anteriores permanecem; item `ERRO`; laço continua. |
| TU-30 | `mantemQuestaoPublicadaRespondidaIntactaNaDivergencia` | CA-19 | Nenhuma escrita na questão; divergência `PENDENTE`. |
| TU-31 | `atualizaQuestaoEmRevisaoSemRespostaAutomaticamente` | CA-20 | Conteúdo atualizado; auditoria registrada. |
| TU-32 | `reaproveitaTextoBaseJaGravadoSemChamarFornecedor` | CA-22 | Zero chamadas ao *fake*; mesmo `texto_base_id`. |
| TU-33 | `descartaCandidataQuandoTextoBaseEIndisponivel` | CA-23 | `INCOMPLETA`. |
| TU-34 | `classificaTodaCandidataProcessadaComExatamenteUmResultado` | CA-02 | Nenhuma candidata sem resultado ou com dois. |
| TU-35 | `naoPersisteNadaEmModoSimulacao` | CA-05 | Nenhuma escrita em questão, texto-base ou mapeamento. |
| TU-36 | `omiteChaveDeAcessoDeTodaMensagemDeErro` | CA-26 | A chave não aparece em nenhuma mensagem nem em `toString`. |
| TU-37 | `importacoesStoreConsultaProgressoEnquantoExecutando` (Vitest) | CA-09 | Consulta enquanto `EXECUTANDO`; para ao concluir. |
| TU-38 | `textoBaseCartaoOmiteBlocoQuandoNaoHaTexto` (Vitest) | CA-22 | Sem `textoBase`, nada é renderizado. |
| TU-39 | `resolveFornecedorPelaFonteSemConhecerImplementacao` | — (ADR-2) | `RegistroFornecedores` com dois *fakes* resolve cada `Fonte`; fornecedor inativo não é resolvido. |
| TU-40 | `processaLoteDeFornecedorFicticioSemAlterarNadaDoNucleo` | — (ADR-2) | Um `FornecedorQuestoes` inventado no teste percorre o pipeline inteiro e grava questão. É o teste que **prova** o custo de troca declarado em *O custo real de trocar de fornecedor*. |
| TU-41 | `aplicaLocalmenteFiltroQueOFornecedorNaoSuporta` | — (RF-39) | Fornecedor sem `DIFICULDADE` em `filtrosSuportados()`: candidatas fora do filtro caem e o campo aparece em `filtragemLocal`. |

Alvo dos testes de unidade: `DetectorTipo`, `ValidadorEstrutura`, `ValidadorLimites`, `DetectorRecursoNaoSuportado`, `AvaliadorComentario`, `ResolvedorTaxonomia`, `DetectorDuplicidade`, `HashConteudoExterno`, `TradutorQapi`, `NovaTentativaComRecuo`, `LimitadorChamadasFornecedor`, `ProcessadorCandidata` e `DivergenciaService` — objetos puros, sem contexto Spring, com o fornecedor substituído por *fake*. `Clock` fixo em tudo que grava data (RNF de repetibilidade).

### Testes de integração

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | `aplicaMigracaoV19PreservandoQuestoesExistentesComoInternas` | CA-28 | Todas as questões anteriores com `fonte = INTERNA`; nenhuma outra coluna alterada. |
| TI-02 | `geraHashDeEnunciadoParaTodoOAcervoPreExistente` | CA-17 | Coluna gerada preenchida para questão criada antes da migração. |
| TI-03 | `recusaSegundaGravacaoDoMesmoParFonteEIdentificador` | CA-16 | Violação de unicidade no banco, não só na aplicação. |
| TI-04 | `aceitaPedidoDeImportacaoERespondeDuzentosEDois` | CA-09 | 202 com código do lote; lote em `EXECUTANDO`. |
| TI-05 | `recusaTodoEndpointDeImportacaoParaEstudanteEProfessor` | CA-25 | 403 em todas as rotas desta especificação. |
| TI-06 | `nuncaDevolveAChaveDoFornecedorEmNenhumEndpoint` | CA-26 | Varredura no corpo de todas as respostas. |
| TI-07 | `processaLoteCompletoEProduzRelatorioComDistribuicao` | CA-09, CA-02 | Soma da distribuição = `totalProcessado`. |
| TI-08 | `naoDuplicaNadaAoReexecutarOMesmoPedido` | CA-16 | Nenhuma questão nova; todas `JA_EXISTENTE`. |
| TI-09 | `descartaCandidataIdenticaAQuestaoCadastradaAMao` | CA-17 | `JA_EXISTENTE` por colisão de hash. |
| TI-10 | `registraDisciplinaNaoMapeadaNaFilaDePendencias` | CA-12 | `SEM_MAPEAMENTO`; valor aparece em `/mapeamentos/pendencias` com contagem. |
| TI-11 | `mantemCatalogoIntactoDepoisDeQualquerImportacao` | CA-13 | Contagem de disciplina, assunto, banca e órgão inalterada. |
| TI-12 | `fechaLoteComoParcialPreservandoOQueJaEntrou` | CA-18 | Falha no meio: anteriores gravadas, motivo preenchido, reexecução não duplica. |
| TI-13 | `recusaPublicacaoDeImportadaSemComentarioComOValidadorAtual` | CA-11 | 422 com a mensagem de `ValidadorPublicacao`, idêntica à da questão manual. |
| TI-14 | `criaDivergenciaPendenteSemAlterarQuestaoPublicadaRespondida` | CA-19 | Conteúdo local intacto; divergência `PENDENTE`. |
| TI-15 | `atualizaAutomaticamenteQuestaoEmRevisaoSemRespostaERegistraAuditoria` | CA-20 | Conteúdo novo; linha em `auditoria`. |
| TI-16 | `naoAlteraNemRemoveNenhumaRespostaEmQualquerDecisao` | CA-21 | Contagem e conteúdo de `resposta` idênticos nas três decisões. |
| TI-17 | `gravaUmaVezOTextoBaseCompartilhadoPorVariasQuestoes` | CA-22 | Uma linha em `texto_base`; N questões apontando para ela. |
| TI-18 | `naoChamaFornecedorEmNenhumFluxoDeEstudo` | CA-24 | `FornecedorQuestoes` que falha se invocado; sessão, caderno, simulado e desempenho funcionam. |
| TI-19 | `entregaQuestaoImportadaPublicadaComoQualquerOutraNaSerie` | CA-27 | Entra na série, aceita resposta, devolve gabarito e comentário, conta no desempenho, gera flashcard. |
| TI-20 | `exibeProcedenciaNaFichaAdministrativaDaQuestaoImportada` | CA-28 | Fonte, identificador, data e link presentes em `QuestaoDetalhe`. |
| TI-21 | `naoEscreveAChaveDoFornecedorEmNenhumaLinhaDeLog` | CA-06 | *Appender* de captura; varredura por `qb_`. |
| TI-22 | `naoGravaQuestaoTextoBaseNemMapeamentoEmModoSimulacao` | CA-05 | Só `lote_importacao` e `item_importacao` mudam. |
| TI-23 | `classificaIdenticamenteEmDuasExecucoesDoMesmoPedido` | CA-04 | Relatórios comparados item a item. |
| TI-24 | `listaValoresExternosNaoMapeadosComContagemNoRelatorio` | CA-07 | `pendenciasMapeamento` preenchido. |
| TI-25 | `recusaImportacaoQuandoDesabilitadaNoAmbiente` | — (RN-21) | 409 `IMPORTACAO_DESABILITADA`. |
| TI-26 | `recusaSegundoLoteConcorrenteParaAMesmaFonte` | — | 409 `IMPORTACAO_EM_ANDAMENTO`. |

Testados em conjunto: migração Flyway sobre `PostgreSQLContainer<>("postgres:18")` com `@DynamicPropertySource`, o pipeline inteiro com `FornecedorQuestoes` *fake*, o contrato HTTP e a autorização por perfil. Massa de teste isolada por execução (`UUID.randomUUID()` em código de questão e identificador externo), nunca compartilhada entre testes. TI-18 e TI-21 são os testes que provam O-5 e RN-19 — os dois compromissos que o PRD trata como inegociáveis.

### Testes E2E

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | `administrador dispara importação e lê o relatório` | CA-09, CA-02 | Diálogo → progresso → relatório com a distribuição por resultado. |
| E2E-02 | `administrador abre questão importada na tela de edição existente` | CA-10 | Link do item leva a `admin/questoes/:id`; não há editor paralelo (RF-71). |
| E2E-03 | `professor completa o comentário e o administrador publica` | CA-11 | Filtro "sem comentário" → resolução → enviar para revisão → publicar. |
| E2E-04 | `administrador mapeia valor externo e reexecuta a importação` | CA-12 | Candidatas antes em `SEM_MAPEAMENTO` entram na segunda execução. |
| E2E-05 | `estudante resolve questão importada com texto-base` | CA-22, CA-27 | Texto antes do enunciado, na sessão e no simulado; indistinguível de questão nativa. |
| E2E-06 | `administrador decide divergência de questão publicada` | CA-19 | Comparação lado a lado, confirmação explícita, histórico intacto. |
| E2E-07 | `estudo funciona normalmente com o fornecedor fora do ar` | CA-24 | Sessão, caderno, simulado e desempenho sem erro; nenhuma chamada externa. |

Executados com Playwright em `frontend/e2e/importacao-questoes.spec.ts`, no padrão dos onze arquivos que já existem. A QAPI é substituída por um duplo no perfil de teste — E2E não fala com fornecedor real, nem gasta crédito.

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Migração V19 e entidades.** `texto_base`, procedência em `questao`, a função de hash e a coluna gerada, `mapeamento_taxonomia`, `lote_importacao`, `item_importacao`, `divergencia_externa`. Vem primeiro porque o `ddl-auto=validate` faz tudo o mais depender dela, e porque TI-01 a TI-03 travam o modelo antes de existir código em cima.
2. **Triagem, isolada.** `DetectorTipo`, `ValidadorEstrutura`, `ValidadorLimites`, `DetectorRecursoNaoSuportado`, `AvaliadorComentario` — objetos puros, sem Spring, sem fornecedor. É a parte de maior risco de produto (RI-3, RI-4) e a que o PoC mede; existir cedo permite validar o fornecedor antes de investir no resto.
3. **Porta, registro e adaptador QAPI.** `FornecedorQuestoes`, `RegistroFornecedores`, `FiltragemLocalCandidatas`, `QuestaoExterna`, e então `ClienteQapi`, `TradutorQapi`, limitador e recuo. A porta e o registro vêm **antes** do adaptador de propósito: escrever o registro com dois *fakes* (TU-39, TU-40) antes de existir uma implementação real é o que impede o desenho de encaixar na QAPI por acidente. Só depois disso a primeira chamada de saída do projeto passa a existir.
4. **PoC (F1).** Pipeline com `somenteSimulacao`, relatório e lista de pendências. **Portão de decisão:** CA-03 e CA-08 reprovam o fornecedor antes de qualquer investimento no MVP, e é aqui que Q-6 se fecha.
5. **Taxonomia e deduplicação.** `ResolvedorTaxonomia`, `MapeamentoTaxonomiaService`, `DetectorDuplicidade`. Depende de 1 e alimenta a curadoria de F2.
6. **Persistência e lote.** `QuestaoImportadaService` em `questao/` reusando `MontadorQuestao`, `ProcessadorCandidata`, `ExecutorLoteImportacao`, `ImportacaoService` e o executor assíncrono.
7. **Texto-base.** `ResolvedorTextoBase` e a propagação de `textoBase` por `QuestaoDaSerie` e `ItemCorrecao` — uma alteração que atende sessão de estudo e simulado de uma vez.
8. **Divergência.** `DetectorDivergencia` e `DivergenciaService`; depende de 6 existir para ter o que reencontrar.
9. **Controllers e contrato.** Endpoints, `@PreAuthorize`, e a atualização da `TECH-SPEC.md` §6 **na mesma alteração** (regra 4 do `AGENTS.md`).
10. **Protótipo.** As três telas novas e o bloco de texto-base em `Certame.dc.html`, com componentes Nocturne existentes. Antecede o Angular pela regra 1 do `AGENTS.md`.
11. **Frontend.** Cliente HTTP, modelos, telas de importação, mapeamento e divergência, texto-base no cartão e na correção, procedência na ficha, filtros de pendência.
12. **E2E e fechamento de cobertura.**

Domínio e testes antes do controller, controller antes do frontend — a ordem que o `AGENTS.md` exige.

### Dependências técnicas

- **Bloqueador de negócio (RN-21):** G-01 a G-04 confirmados por escrito antes de habilitar em produção. Sem eles, `certame.importacao.habilitada` fica falso e a funcionalidade só roda fora de produção. O PoC, por não persistir, pode acontecer antes — desde que o contrato de uso permita a consulta.
- **Chave da QAPI e saldo de créditos** (`QAPI_KEY`), em variável de ambiente. Sem saldo, todo lote termina em `402`.
- **Taxonomia povoada** (P-3): sem disciplinas internas, toda candidata cai em `SEM_MAPEAMENTO`. É o que F2 resolve, com a fila de pendências do PoC como insumo.
- **Docker** para os testes de integração com Testcontainers, como no resto do projeto.
- **Nenhuma dependência Maven nova.** `RestClient` já vem em `spring-boot-starter-webmvc`; `@EnableAsync` e `@EnableScheduling` já estão em `CertameApplication`; `sha256()` é nativo do PostgreSQL 18 e `certame_unaccent` existe desde `V4`.

## Monitoramento e observabilidade

Micrometer, no padrão já estabelecido por `EstudoObservabilidade` e `MetricasPublicacao`, com `management.endpoints.web.exposure.include=health,metrics` já configurado.

| Métrica | Tipo | Tags | Para quê |
| --- | --- | --- | --- |
| `certame.importacao.candidatas.recuperadas` | contador | `fonte` | Numerador do aproveitamento (O-1). |
| `certame.importacao.candidatas.importadas` | contador | `fonte`, `comPendencia` | Mede a pendência editorial, o custo humano real. |
| `certame.importacao.candidatas.descartadas` | contador | `fonte`, `motivo` | Uma série por resultado; é o que reprova o fornecedor (CA-08). |
| `certame.importacao.lote.duracao` | temporizador | `fonte`, `status` | RNF-03: 50 candidatas em menos de 60 s. |
| `certame.importacao.fornecedor.falhas` | contador | `fonte`, `classificacao` | Separa transitória de permanente; `sem_credito` merece alerta. |
| `certame.importacao.fornecedor.chamadas` | contador | `fonte`, `endpoint` | **É o custo em reais.** Cada chamada é 1 crédito. |
| `certame.importacao.divergencias.detectadas` | contador | `fonte`, `situacaoLocal` | Acompanha a métrica de divergências pendentes há mais de 30 dias. |
| `certame.importacao.mapeamentos.pendentes` | *gauge* | `fonte`, `tipo` | Cobertura de mapeamento (meta ≥ 90%). |

**Log** estruturado, em português, com o **código do lote em toda linha**, para reconstruir uma execução inteira (RNF-08).

| Evento | Nível | Conteúdo |
| --- | --- | --- |
| Início e fim do lote | `info` | Código, fonte, filtros, solicitante, totais, chamadas gastas. |
| Candidata descartada | `debug` | Código do lote, identificador externo, resultado, motivo. |
| Valor externo sem mapeamento | `info` | Tipo e valor — é o insumo da curadoria. |
| Falha transitória com nova tentativa | `warn` | Classificação e tentativa; **sem** header de autenticação. |
| Sem créditos, chave inválida ou suspensa | `error` | Classificação e ação tomada. |
| Divergência detectada | `warn` | Código da questão, situação local, se está bloqueada por resposta. |

Nenhuma linha carrega a chave de acesso nem despeja o payload bruto do fornecedor (RNF-08, RN-19) — TI-21 verifica isso varrendo o log capturado por `qb_`.

**Auditoria** na tabela `auditoria` que já existe, por `RegistradorAuditoria` (RF-69): `IMPORTACAO_INICIADA`, `QUESTAO_IMPORTADA`, `MAPEAMENTO_CRIADO`, `MAPEAMENTO_CORRIGIDO`, `DIVERGENCIA_APLICADA`, `DIVERGENCIA_IGNORADA`. O *payload* guarda identificador externo, hashes e decisão — **nunca** cópia do conteúdo bruto.

## Considerações técnicas

### Principais decisões

**ADR-1 — Procedência como colunas em `questao`, não tabela satélite.** Toda questão tem exatamente uma procedência, o modelo atual já concentra atributos ali, e a deduplicação precisa de índice sobre `(fonte, identificador_externo)` e sobre o hash na tabela mais consultada do sistema. Uma satélite 1:1 acrescentaria *join* em toda leitura de questão para benefício nenhum. Custo aceito: oito colunas a mais numa tabela larga, sete delas nulas em toda questão interna.

**ADR-2 — `QuestaoExterna` como representação neutra, DTOs confinados ao adaptador.** É a decisão que impede o domínio de acoplar à QAPI. Descartadas: (a) deixar o DTO da QAPI circular — menos código agora, acoplamento depois; (b) mapear DTO direto para `Questao` — impossível, porque `Questao` exige autor, código e disciplina resolvida, e triagem e deduplicação precisam decidir *antes* de existir questão. O confinamento é verificado por teste (TU-20), não por disciplina.

#### O custo real de trocar de fornecedor

Isolar o domínio só vale se o custo de somar o segundo fornecedor for verificável. Ele é este:

| O que fazer | Onde | Quanto |
| --- | --- | --- |
| Escrever o adaptador | `importacao/<fornecedor>/`: `Propriedades*`, `Cliente*`, `Fornecedor*`, `Tradutor*`, `dto/` | ~6 classes, nenhuma acima de 100 linhas |
| Declarar a configuração | `application-*.properties` | URL, chave, tempos limite, tetos |
| Ativar | `certame.importacao.<fornecedor>.ativo=true` | uma linha |

E este é o que **não** é preciso fazer, que é o ponto:

- **Nenhuma migração.** `Fonte` já declara `INTERNA`, `QAPI`, `QUEST_API` e `API_DAS_QUESTOES`, no enum e nos quatro `check` da `V19`. Um valor previsto pelo PRD que entrasse só depois custaria uma migração em quatro tabelas — inclusive `questao`, a mais acessada do sistema.
- **Nenhuma alteração em `questao/`, `catalogo/`, `estudo/`, `simulado/`, `desempenho/` ou `flashcard/`.**
- **Nenhuma alteração na triagem, no mapeamento de taxonomia, na deduplicação, no texto-base, no lote ou na divergência.** Todos operam sobre `QuestaoExterna` e não têm como saber de onde ela veio.
- **Nenhum `switch` nem `if` por fornecedor.** `RegistroFornecedores` recebe `List<FornecedorQuestoes>` do Spring e resolve por `fonte()`; o adaptador novo entra em operação por existir como *bean*.
- **Nenhum endpoint novo, nenhuma alteração de contrato.** `fonte` já é parâmetro de `POST /admin/importacoes` e do catálogo.
- **Nenhuma alteração no frontend.** A lista de fornecedores vem de `GET /admin/importacoes/fornecedores`, e os filtros de cada um vêm de `filtrosSuportados()`.

As diferenças previsíveis entre fornecedores estão na própria porta, e não em condicional espalhada: `tamanhoPagina()` absorve o teto de 10 da QAPI sem que ele vire constante do núcleo; `filtrosSuportados()` deixa `FiltragemLocalCandidatas` cobrir localmente o que a origem não filtra (RF-39) — é assim que a ausência de filtro por dificuldade na QAPI deixa de ser um caso especial e vira um dado. TU-39 e TU-40 provam isso com um fornecedor fictício, sem tocar em nada da QAPI.

O que **continua** custando por fornecedor, e não dá para abstrair: a curadoria dos mapeamentos de taxonomia (RF-47), que é trabalho humano sobre o vocabulário de cada origem, e a decisão de negócio dos gates de licenciamento (G-01 a G-07), que são por contrato.

**ADR-3 — Texto-base como entidade própria.** Descartadas: (a) concatenar ao enunciado — duplicaria texto longo em cada questão, inflaria o `tsvector` de busca e impediria exibir o texto uma vez para uma série; (b) rejeitar questões com texto-base no MVP — seguro, mas descartaria boa parte de Português, Interpretação e Direito. Custo aceito: `QuestaoDaSerie` e `ItemCorrecao` mudam, e com eles três telas. Mitigado por campo opcional e comportamento idêntico ao atual quando ausente.

**ADR-4 — Autor da questão importada é quem disparou a importação.** Satisfaz `autor_id not null` sem inventar usuário de sistema. A pergunta de um ator para sincronização agendada (Q-10) **não é decidida aqui**: sincronização agendada está fora desta entrega, e criar um usuário de sistema agora mudaria a semântica de autoria em todo o produto por uma necessidade que ainda não existe.

**ADR-5 — `RestClient` e resiliência escrita à mão.** `RestClient` já vem em `spring-web`, sem dependência nova, e mantém a stack fixa do `AGENTS.md`. Tempo limite por configuração, nova tentativa limitada com recuo progressivo apenas em falha transitória, e um limitador de chamadas por lote. Descartado Resilience4j: traria retry, *rate limiter* e disjuntor declarativos, mas é dependência nova para um caso de uso de um fornecedor e um lote por vez — e a cultura do projeto já resolve isso à mão em `LimitadorTentativas`. Se um segundo e um terceiro fornecedor entrarem, a decisão merece revisão.

> **Correção de rumo trazida pela documentação real.** RNF-02 fala em "limite de chamadas por minuto respeitando o contratado". A QAPI **não tem** limite por minuto: cobra por requisição, em créditos pré-pagos, e responde `402` quando o saldo acaba. O limitador que importa, portanto, não é temporal e sim **de custo por lote** — `chamadas-maximas-por-lote` —, e o `402` é tratado como interrupção legítima que fecha o lote em `PARCIAL`. A intenção de RNF-02 (não estourar o contratado, terminar como parcial e informar a causa) é atendida; o mecanismo é outro.

**ADR-6 — Deduplicação por hash calculado no banco.** A normalização (minúsculas, sem acento, sem pontuação, espaços colapsados) tem **uma única implementação**, a função SQL `certame_hash_enunciado`, usada pela coluna gerada e pela consulta de colisão. Descartado calcular em Java: exigiria backfill do acervo existente e criaria a possibilidade permanente de a normalização do código divergir da do backfill — falso negativo duplica acervo, falso positivo descarta questão legítima. Com a coluna gerada, o hash existe para toda questão desde a migração, e CA-17 passa a valer contra o acervo pré-existente sem escrever uma linha de migração de dados.

**Execução assíncrona com consulta de progresso.** `POST` responde 202; um pool de **uma** thread processa. A thread única não é economia de recurso: é controle de custo e de concorrência — dois lotes simultâneos gastariam crédito em paralelo e disputariam a mesma detecção de duplicidade. Um segundo pedido para a mesma fonte enquanto há lote `EXECUTANDO` recebe 409. O executor é envolvido em `DelegatingSecurityContextAsyncTaskExecutor`; sem isso, o `SecurityContext` não atravessa a fronteira da thread, `@PreAuthorize` recusaria as chamadas internas e `RegistradorAuditoria` gravaria ator nulo.

**Transação por candidata.** `ProcessadorCandidata` roda em `REQUIRES_NEW`; o executor não abre transação envolvente. É o que faz RF-62 e CA-18 valerem, e o que evita a transação longa que RNF-04 proíbe.

**Uma página a mais não é buscada.** Um lote de 50 pede exatamente `ceil(50 ÷ 10) = 5` páginas. Se 20 candidatas forem descartadas, o lote entrega 30 — resultado válido por RN-22, desde que cada descarte tenha motivo. Descartado buscar páginas extras para "completar" o pedido: gastaria crédito de forma imprevisível e tornaria o custo de um lote não determinístico. A ideia original de `tasks/nova-funcao.md` — buscar uma substituta para cada questão já existente — foi conscientemente abandonada pelo PRD em RN-22, e esta especificação segue o PRD.

**O formulário do pedido usa o vocabulário do fornecedor.** A QAPI filtra por nome, não por UUID, e os catálogos (`/api/materias`, `/api/bancas`, …) dão a lista exata. Traduzir o UUID interno para nome externo exigiria um mapeamento inverso que, antes da primeira importação, não existe. O catálogo é cacheado com TTL porque cada leitura custa crédito.

### Riscos conhecidos

| # | Risco | Mitigação |
| --- | --- | --- |
| RT-1 | **A QAPI não informa dificuldade.** Toda questão importada nasce com `dificuldade` nula — e `MontadorProvaSimulado` monta a série por `buscarIdsPublicadasPorDisciplinaEDificuldade`. Composição de simulado que exige dificuldade **nunca encontra questão importada**, sem erro visível: o acervo simplesmente parece menor. | Sinalizador "sem dificuldade" na listagem administrativa, ao lado das demais pendências, e a triagem editorial preenchendo o campo. Vale medir no PoC quanto do acervo isso representa antes de decidir se merece tratamento próprio. |
| RT-2 | **A QAPI não informa o tipo.** Toda classificação `ME`/`CE` é inferida, e a documentação não descreve questão de certo/errado (Q-6 em aberto). Inferência errada gravaria questão com gabarito inválido. | RF-42 recusa o ambíguo em vez de adivinhar; o PoC mede a distribuição real e fecha Q-6 antes do MVP. |
| RT-3 | **Qualidade da taxonomia da origem.** `GET /api/orgaos` devolve `["CESPE", "CESGRANRIO"]` — que são **bancas**, não órgãos. O catálogo de órgãos do fornecedor parece contaminado. | Órgão não é obrigatório para publicar: sem mapeamento, entra como pendência e não descarta (RF-49). A curadoria de F2 decide caso a caso. |
| RT-4 | **`size` máximo 10 e latência.** 50 candidatas custam 5 páginas mais uma chamada por texto-base novo. Em série, com fornecedor lento, RNF-03 (60 s) fica apertado. | Texto-base é buscado no estágio 6, só para candidatas que sobreviveram, e nunca para texto já gravado (RF-58). O tempo do lote é medido por `certame.importacao.lote.duracao`. |
| RT-5 | **Custo por crédito.** Um erro de laço ou uma reexecução distraída queima saldo real. | Teto de chamadas por lote, um lote por vez por fonte, contador `chamadasExternas` no relatório e métrica de chamadas por endpoint. |
| RT-6 | **Coluna gerada depende de função imutável.** `certame_hash_enunciado` chama `certame_unaccent`, declarada `immutable` em `V4` embora `unaccent` seja tecnicamente `stable`. Mudar o dicionário de `unaccent` invalidaria os hashes existentes. | O projeto já depende disso: a coluna gerada `busca` usa a mesma função desde `V4`. Precedente estabelecido, não risco novo — mas registrado. |
| RT-7 | **Texto-base toca telas de estudo estáveis** (RI-10 do PRD). | Campo opcional; comportamento idêntico ao atual quando ausente (TU-38), e E2E-05 e E2E-07 cobrindo os fluxos afetados. |
| RT-8 | **Primeira integração de saída do projeto.** Não há padrão anterior a seguir. | ADR-5 registra a decisão; o pacote `importacao/qapi` é o modelo para as próximas. |
| RT-9 | **Contexto de segurança na thread assíncrona.** Sem propagação, `@PreAuthorize` recusa e a auditoria perde o ator — falha silenciosa e difícil de diagnosticar. | `DelegatingSecurityContextAsyncTaskExecutor` em `ExecutorImportacaoConfig`, com TI-15 verificando que a auditoria registra o ator correto. |
| RT-10 | **Licenciamento negado** (RI-2 do PRD). | `certame.importacao.habilitada` falso por padrão em produção; TI-25 prova que o endpoint recusa. Nenhum direito é presumido. |

### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/folder-structure.md` e `.agents/rules/tests.md` foram lidos na íntegra. O que eles impõem a esta especificação:

- **Stack inalterada** (Angular 22 · Spring Boot 4.1 / Java 25 · PostgreSQL 18 + Flyway · Maven) e **nenhuma dependência Maven ou npm nova** — ADR-5 escolheu `RestClient` justamente por isso.
- **Regra 1 — o frontend é tradução 1:1 do protótipo.** O `Certame.dc.html` **não tem** tela de importações. As três telas novas e o bloco de texto-base entram no protótipo antes do Angular, com componentes Nocturne existentes, como foi feito em `prd-banco-de-questoes`. Nenhuma cor, fonte, espaçamento, raio ou sombra inventados; nenhuma biblioteca de UI de terceiros (RNF-09).
- **Regra 2 — autorização no servidor.** `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` em todo endpoint desta especificação, mesmo com a interface escondendo a ação (RNF-10, CA-25, TI-05).
- **Regra 3 — idioma.** Código, pacotes, tabelas e colunas em português sem acento, `snake_case` no banco; mensagens ao usuário em pt-BR com acentuação, frase completa e ponto final. Os campos do envelope de erro (`code`, `message`, `fieldErrors`) permanecem em inglês, como já é.
- **Regra 4 — nenhum endpoint fora do contrato.** As rotas de *Endpoints da API* entram na `TECH-SPEC.md` §6 na mesma alteração que as implementa (RNF-11, R-5).
- **Regra 5 — migração só por Flyway.** `V19__importacao_questoes_externas.sql`, aditiva; nenhuma migração aplicada é editada; `ddl-auto` permanece em `validate` (RNF-05, R-4). Seeds de dev em `V912`, na faixa `V9xx`.
- **Regra 6 — `resposta` é imutável.** Nenhuma decisão sobre divergência toca em `resposta`; o banco já garante por `do instead nothing` (RN-17, CA-21, TI-16).
- **Fronteira por domínio** (§4 da `TECH-SPEC.md`): pacote novo `importacao/`, um subpacote por fornecedor; `importacao/` conversa com `questao/` **por serviço**, nunca por repositório (`code-standards.md` §3) — daí `QuestaoImportadaService`.
- **Camadas:** controller só orquestra e mapeia DTO, sem regra e sem `@Transactional`; serviço concentra regra, transação e autorização; repositório é Spring Data. Erros por `ErroNegocioException` e o `@RestControllerAdvice` único que já existe — nenhum `try/catch` em controller, nenhum *advice* novo.
- **DTOs** são `record` imutável em `<dominio>/dto/`, com mensagem em português em cada *constraint*; `Requisicao*` para entrada, `Resposta*` para saída, `*Resumo` para projeção. Entidade JPA nunca cruza a fronteira do controller.
- **Entidades** sem Lombok, com construtor protegido para o JPA, `@GeneratedValue(strategy = UUID)`, datas em `Instant`, `@Enumerated(EnumType.STRING)` com `check` equivalente na migração, e setter só no que realmente muda. `hashEnunciado` é mapeado como somente leitura, no padrão de `busca`.
- **Limites de forma:** arquivos até 100 linhas, métodos até 30, no máximo 3 parâmetros e 3 níveis de condicional, sem comentário desnecessário, sem linha em branco dentro de método, sem literal mágico. A decomposição fina da tabela de componentes — cinco classes só de triagem, em vez de um validador único — existe para caber nesses limites por **responsabilidade real**, não por corte arbitrário de linhas.
- **Segredo nunca no código:** a chave da QAPI sai por variável de ambiente, lida por `@ConfigurationProperties`, e não entra em commit, teste ou arquivo versionado (§2.8, RN-19).
- **Testes** (`tests.md`, que prevalece sobre `code-standards.md` §3 em cobertura): mínimo 80%, FIRST, AAA/Given-When-Then, nome com o id do caso da spec, AssertJ no backend, `Clock` fixo, dados isolados por execução, Testcontainers só em `*IntegracaoTest`, pirâmide ~70/20/10. Nenhum teste toca a rede: `FornecedorQuestoes` é a fronteira e é sempre substituída.
- **Frontend:** standalone, zoneless, signals, `input()`/`output()` como funções, template e estilo em arquivos separados, estado compartilhado em serviço `providedIn: 'root'`, HTTP concentrado em `core/api`. Prettier com `printWidth: 100` e `npm run format:check` retornando zero arquivos fora do padrão antes do merge.

**Divergência registrada:** RNF-02 fala em limite de chamadas **por minuto**; a QAPI não tem esse limite, e o controle equivalente é de custo por lote com tratamento de `402`. Justificativa em *Principais decisões* (ADR-5).

### Conformidade com skills

Skills do projeto (`.agents/skills/`) aplicáveis a esta especificação:

| Skill | Onde se aplica | Aderência |
| --- | --- | --- |
| `postgresql-table-design` | `V19`: tipos, índices, restrições e recursos avançados | Segue a orientação: `uuid` com `gen_random_uuid()`, `timestamptz`, enum como `text` + `check`, índice único **parcial** para a identidade externa (`where identificador_externo is not null`), índice parcial para a fila de pendências, `jsonb` para filtros e pendências, coluna gerada `stored` para o hash, `check` de alvo único no mapeamento e `on delete cascade` só onde o filho não tem vida própria (`item_importacao`). |
| `313-frameworks-spring-db-migrations-flyway` | Numeração, imutabilidade e configuração do Flyway | `V19__importacao_questoes_externas.sql` em `db/migration/`, imutável; seeds em `db/migration-dev/V912`, carregados só no perfil `dev` por `spring.flyway.locations`; `validate-on-migrate` e `baseline-on-migrate=false` inalterados; `ddl-auto=validate` mantido, com TI-01 provando que entidade e migração não divergem. |
| `spring-boot-testing` | Escolha da técnica por camada | Unidade sem contexto Spring para triagem, tradução e resiliência; `@WebMvcTest` para autorização e formato; `@SpringBootTest @AutoConfigureMockMvc @Testcontainers` só para migração, SQL e fluxo ponta a ponta. Nada sobe ao nível de cima do que o de baixo já prova. |
| `angular-developer` | Telas novas e alterações no cartão de questão | Standalone, zoneless, signals, `input()`/`output()`, `@if`/`@for`, HTTP em `core/api`, ARIA nos estados de progresso e nos sinalizadores de pendência. |
| `impeccable` | Tela de triagem e sinalizadores | Estados vazio, carregando, parcial e de erro com mensagem própria em pt-BR; motivo do descarte em **texto**, não só cor ou ícone; sinalizadores com rótulo acessível a leitor de tela; tabela com rolagem horizontal própria em telas menores; confirmação explícita antes de aplicar divergência em questão publicada. |

**Desvio registrado:** `impeccable` pediria uma pré-visualização antes de gravar e triagem em massa. Ambas estão explicitamente **fora do escopo** do PRD ("Interface administrativa sofisticada, com pré-visualização antes de gravar e triagem em massa") e ficam para as fases seguintes. O MVP entrega interface suficiente para revisar o que entrou, e nada além disso (RF-70).

### Arquivos relevantes e dependentes

**Backend — novos**

`backend/src/main/resources/db/migration/V19__importacao_questoes_externas.sql` · `db/migration-dev/V912__seed_importacao_dev.sql`
`br/com/certame/importacao/`: `FornecedorQuestoes`, `RegistroFornecedores`, `FiltragemLocalCandidatas`, `ResultadoFiltragem`, `QuestaoExterna`, `AlternativaExterna`, `TextoExterno`, `PaginaExterna`, `FiltroFornecedor`, `CampoFiltro`, `ResultadoCandidata`, `CatalogoFornecedor`, `MetricasImportacao`
`importacao/qapi/`: `PropriedadesQapi`, `ClienteQapi`, `FornecedorQapi`, `TradutorQapi`, `LimitadorChamadasFornecedor`, `NovaTentativaComRecuo`, `ClassificadorFalhaFornecedor`, `dto/{QuestaoQapi,PaginaQuestoesQapi,TextoQapi,ErroQapi}`
`importacao/triagem/`: `TriagemCompatibilidade`, `DetectorTipo`, `ValidadorEstrutura`, `ValidadorLimites`, `DetectorRecursoNaoSuportado`, `AvaliadorComentario`, `ResultadoTriagem`
`importacao/taxonomia/`: `MapeamentoTaxonomia`, `TipoTaxonomia`, `MapeamentoTaxonomiaRepository`, `NormalizadorValorExterno`, `ResolvedorTaxonomia`, `TaxonomiaResolvida`, `MapeamentoTaxonomiaService`, `MapeamentoController`
`importacao/dedup/`: `DetectorDuplicidade`, `HashConteudoExterno`
`importacao/textobase/`: `TextoBase`, `TextoBaseRepository`, `ResolvedorTextoBase`
`importacao/lote/`: `LoteImportacao`, `ItemImportacao`, `StatusLote`, `LoteImportacaoRepository`, `ItemImportacaoRepository`, `GeradorCodigoLote`, `ImportacaoService`, `ExecutorLoteImportacao`, `ProcessadorCandidata`, `ContextoLote`, `RelatorioImportacaoService`, `ImportacaoController`
`importacao/divergencia/`: `DivergenciaExterna`, `DivergenciaExternaRepository`, `DetectorDivergencia`, `DivergenciaService`, `DivergenciaController`
`importacao/dto/`: `RequisicaoImportacao`, `LoteAceito`, `RelatorioImportacao`, `LoteResumo`, `ItemImportacaoResumo`, `PendenciaMapeamento`, `RequisicaoMapeamento`, `MapeamentoDetalhe`, `DivergenciaExternaDetalhe`, `FiltroImportacao`, `TextoBaseResumo`
`questao/`: `Fonte`, `ProcedenciaQuestao`, `QuestaoImportadaService`
`config/ExecutorImportacaoConfig`
`comum/excecao/`: `FornecedorIndisponivelException`, `ImportacaoDesabilitadaException`, `MapeamentoEmUsoException`

**Backend — modificados**

`questao/Questao` (procedência, texto-base, hash gerado) · `questao/QuestaoRepository` (identidade externa, colisão por hash, contagem de pendências) · `questao/dto/QuestaoDetalhe` · `questao/dto/FiltroQuestao` · `questao/QuestaoBuscaRepositoryImpl` (filtros de fonte e pendência) · `estudo/dto/QuestaoDaSerie` · `estudo/dto/ItemCorrecao` · `estudo/MontadorRespostaSessao` · `estudo/CorrecaoProvaService` · `src/main/resources/application.properties` e `application-{dev,staging,prod}.properties`

**Backend — inalterados, e é decisão de projeto que assim seja**

`questao/MontadorQuestao` · `questao/ValidadorPublicacao` · `questao/PublicacaoService` · `questao/GeradorCodigoQuestao` · `estudo/MontadorSerie` · `estudo/SerieRepository` · `simulado/MontadorProvaSimulado` — a importação **reutiliza** a régua editorial e a montagem de série existentes em vez de duplicá-las (RN-13). Toda alteração nesses arquivos deve ser tratada como sinal de que o desenho escorregou.

**Frontend — novos**

`core/api/importacao-api.ts` · `core/api/modelos-importacao.ts`
`features/admin/importacoes/`: `importacoes-store.ts`, `importacoes-page/`, `importacao-dialog/`, `relatorio-page/`, `mapeamentos-page/`, `mapeamento-dialog/`, `divergencias-page/`
`features/study/texto-base-cartao/`
`e2e/importacao-questoes.spec.ts`

**Frontend — modificados**

`app.routes.ts` · `layout/shell/` · `core/api/modelos-estudo.ts` (`textoBase` em `QuestaoDaSerie` e `ItemCorrecao`) · `core/api/modelos-questao.ts` (`procedencia`, `textoBase`) · `features/study/questao-cartao/{questao-cartao.html,questao-cartao.ts}` · `features/study/correcao-prova/correcao-prova.html` · `features/admin/questoes/questao-form-page/` · `features/admin/questoes/questoes-filtros.ts` · `features/admin/questoes/questoes-page/`

**Protótipo**

`protótipo/Certame.dc.html` — telas `ADMIN: IMPORTAÇÕES`, `ADMIN: MAPEAMENTOS`, `ADMIN: DIVERGÊNCIAS` e o bloco de texto-base no cartão de questão.

**Documentos**

`TECH-SPEC.md` §6 (contrato da API) e §5 (modelo de dados), atualizados na mesma alteração que implementar os endpoints · `tasks/prd-importacao-questoes-api-externa/prd.md` (Q-3 a Q-8 respondidas por esta especificação; Q-1, Q-2, Q-9 e Q-10 seguem abertas com o negócio).
