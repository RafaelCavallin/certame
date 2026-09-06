# Especificação técnica

> **Funcionalidade:** Cadernos e desempenho · **PRD:** `tasks/prd-cadernos-e-desempenho/prd.md` · **Fase:** F3 — Progresso
> Deriva de `TECH-SPEC.md` v1.0 (§3–§6 e §8, recortados para RF-13, RF-15, RF-16, RN-02 e RN-04).
> Depende de `tasks/prd-acesso-e-conta/techspec.md` (`SegurancaConfig`, `ManipuladorErrosGlobal`, `ErroApi`, `Usuario`), de `tasks/prd-banco-de-questoes/techspec.md` (`questao`, `disciplina`, `assunto`, `PaginaResposta<T>`, `confirmacao-dialog`) e de `prd-sessao-de-estudo` (F2), que funda `sessao_estudo`, `resposta` e `marcacao`.

## Resumo

Esta funcionalidade não gera dado novo: ela lê. `resposta` é imutável (RN-02) e é a única fonte de verdade; cadernos e desempenho são duas leituras diferentes do mesmo histórico. O desenho segue essa assimetria — nenhum caminho desta especificação escreve em `resposta`, `sessao_estudo` ou `marcacao`.

Os **cadernos** dividem-se em dois mecanismos deliberadamente distintos. Os manuais são linhas em `caderno` e `caderno_questao`, com CRUD do próprio usuário. Os automáticos ("Erros recorrentes", "Favoritas") **não existem como linha**: são projeções de `marcacao`, devolvidas na mesma lista com ids sintéticos `auto:ERRO_RECORRENTE` e `auto:FAVORITA`. Isso mantém a marcação como fonte única — CA-02 e CA-03 passam a ser propriedade do modelo, e o caso de borda 3 (acertar uma questão marcada não desmarca nada) é verdadeiro por construção, porque não há sincronizador algum que pudesse desfazer a marcação. O **progresso** de qualquer caderno é cobertura, não nota: questões distintas do caderno já respondidas ao menos uma vez, sobre o total atual do caderno. Como é derivado, CA-05 ("o progresso é atualizado ao final da sessão") não exige escrita nenhuma no fim da sessão.

O **desempenho** é servido por três tabelas de agregado — `desempenho_semanal`, `desempenho_disciplina` e `desempenho_assunto` — atualizadas por *upsert* do próprio usuário quando a sessão é finalizada, e reconciliadas por um job `@Scheduled` que reconstrói as linhas marcadas como pendentes. É o ponto em que esta especificação se afasta da §8 da `TECH-SPEC.md`, que previa *materialized views*: o `REFRESH` de uma MV varre a base inteira para atualizar um usuário, e o PRD exige que a curva reflita o estudo que acabou de acontecer. A decisão está registrada em *Principais decisões*.

Três regras do PRD viram objetos puros, sem Spring e sem banco, porque são o que realmente pode dar errado: `CalendarioSemanas` (gera as 8 semanas e devolve **lacuna**, não zero, onde não houve atividade — CA-07), `SeletorPontosFracos` (mínimo de 20 respostas e acerto abaixo da média do próprio usuário — CA-09, CA-10) e `ProgressoCaderno`. RN-04 é respeitado na origem: os agregados só enxergam respostas de sessões finalizadas, e nenhuma tentativa de Modo Prova abandonada entra em qualquer métrica (CA-12).

No frontend são duas telas em tradução 1:1 do protótipo — `CADERNOS` e `DESEMPENHO`, ambas já desenhadas, inclusive o SVG da curva semanal, que é reproduzido à mão sobre tokens Nocturne, sem biblioteca de gráficos. O protótipo tem sete lacunas (diálogo de novo caderno, menu do card, seletor de questões, os dois estados vazios, a representação da lacuna na série e o volume por disciplina), listadas em *Riscos conhecidos* e fechadas no protótipo antes de qualquer linha de Angular.

## Arquitetura do sistema

### Visão dos componentes

```
Angular 22 (SPA)                            Spring Boot 4.1                       PostgreSQL 18
────────────────                            ───────────────                       ─────────────
features/cadernos/                                                                ┌── caderno            (novo)
  CadernosPage      ────▶  GET/POST/PUT/DELETE /api/v1/cadernos                   ├── caderno_questao    (novo)
  CadernoDialog     ────▶  POST   /api/v1/cadernos/{id}/questoes                  ├── desempenho_semanal    (novo)
  QuestoesPicker    ────▶  GET    /api/v1/questoes            (F2)                ├── desempenho_disciplina (novo)
                    ────▶  POST   /api/v1/estudo/sessoes      (F2, origem CADERNO)├── desempenho_assunto    (novo)
features/stats/                                                                   ├── desempenho_pendencia  (novo)
  DesempenhoPage    ────▶  GET    /api/v1/desempenho/resumo                       │
  EvolucaoChart     ────▶  GET    /api/v1/desempenho/evolucao                     ├── resposta      (F2, só leitura)
  DisciplinasCard   ────▶  GET    /api/v1/desempenho/disciplinas                  ├── sessao_estudo (F2, só leitura)
  PontosFracosTable ────▶  GET    /api/v1/desempenho/pontos-fracos                └── marcacao      (F2, só leitura)
```

**Backend — pacote `br.com.certame`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `estudo/caderno/Caderno` | novo | Entidade do caderno manual: `usuario_id`, `nome`, `descricao`, `excluido_em`, `atualizado_em`. Sem coluna `tipo` — automático não é linha (ver *Principais decisões*). |
| `estudo/caderno/CadernoQuestao` | novo | Vínculo N:N com `questao`, PK composta `(caderno_id, questao_id)` e `adicionada_em`. |
| `estudo/caderno/CadernoRepository`, `CadernoQuestaoRepository` | novo | CRUD escopado por `usuario_id`, contagem de questões e progresso agregados em uma consulta só (nunca N+1). |
| `estudo/caderno/TipoCadernoAutomatico` | novo | Enum `ERRO_RECORRENTE`, `FAVORITA` — espelha `marcacao.tipo` (RF-11). |
| `estudo/caderno/ReferenciaCaderno` | novo | Regra pura que interpreta o id do contrato: `auto:FAVORITA` vira automático, UUID vira manual, qualquer outra coisa é recusada. Usada pela listagem e pela abertura de sessão (CA-05). |
| `estudo/caderno/ProgressoCaderno` | novo | Regra pura: `(respondidas, total) -> percentual`. Zero questões é 0%, nunca divisão por zero (CA-04). |
| `estudo/caderno/CadernoService` | novo | RF-13: criar, renomear, descrever, excluir logicamente e adicionar/remover questões do caderno manual; recusa qualquer escrita em caderno automático. |
| `estudo/caderno/CadernoAutomaticoService` | novo | Projeta `marcacao` nos dois cadernos sintéticos: nome, descrição fixa, contagem, data da última marcação (CA-02, CA-03). |
| `estudo/caderno/ConsultaCadernosService` | novo | CA-01: monta a lista única — automáticos primeiro, manuais por `atualizado_em desc` — com progresso calculado em lote. |
| `estudo/caderno/SerieDoCadernoService` | novo | CA-05: resolve uma `ReferenciaCaderno` na lista ordenada de `questao_id` que a sessão de estudo vai usar. É a porta que `prd-sessao-de-estudo` chama ao abrir sessão com `origem = CADERNO`. |
| `estudo/caderno/CadernoController` | novo | Rotas de `/api/v1/cadernos`; só orquestra e mapeia DTO. |
| `estudo/caderno/dto/` | novo | `CadernoResumo`, `RequisicaoCaderno`, `RequisicaoQuestoesCaderno`, `QuestaoCadernoResumo`. |
| `estudo/resposta/RespostaAgregadoAdapter` | novo | Implementa a porta `FonteRespostas` sobre o `RespostaRepository` de F2. Evita que `desempenho` toque no repositório de outro domínio (regra do `code-standards.md` §3). |
| `desempenho/FonteRespostas` | novo | Porta de leitura: totais por semana, por disciplina e por assunto de **um** usuário, contando apenas sessões finalizadas (RN-04). |
| `desempenho/DesempenhoSemanal`, `DesempenhoDisciplina`, `DesempenhoAssunto` | novo | Entidades dos três agregados; PK composta por `usuario_id` mais a chave da dimensão. |
| `desempenho/DesempenhoPendencia` | novo | Fila de reconciliação: uma linha por usuário com agregado possivelmente defasado. |
| `desempenho/AtualizadorAgregados` | novo | *Upsert* dos três agregados de um usuário a partir da `FonteRespostas`; idempotente — rodar duas vezes dá o mesmo resultado. |
| `desempenho/OuvinteSessaoFinalizada` | novo | `@TransactionalEventListener(AFTER_COMMIT)` de `SessaoFinalizada`: chama o atualizador e, em falha, apenas registra a pendência — nunca derruba a finalização da sessão. |
| `desempenho/ReconciliadorAgregados` | novo | `@Scheduled` de 15 min: consome `desempenho_pendencia` e reconstrói os agregados. É a rede de segurança da §8 da `TECH-SPEC.md`. |
| `desempenho/CalendarioSemanas` | novo | Regra pura, com `Clock` injetado: gera as 8 chaves de semana e casa com os agregados, devolvendo **lacuna** onde não houve atividade (CA-07). |
| `desempenho/EvolucaoSemanalService` | novo | RF-15: série de 8 semanas, média do período, melhor semana e volume total. |
| `desempenho/DisciplinaDesempenhoService` | novo | RF-16: volume e percentual por disciplina, ordenados por volume (CA-08). |
| `desempenho/SeletorPontosFracos` | novo | Regra pura de RF-16: `≥ 20 respostas` **e** acerto abaixo da média do próprio usuário; ordena por impacto (CA-09, CA-10). |
| `desempenho/PontosFracosService` | novo | Aplica o seletor sobre `desempenho_assunto` e resolve os nomes de assunto e disciplina. |
| `desempenho/ResumoDesempenhoService` | novo | Os quatro indicadores do topo da tela e o sinalizador `possuiHistorico`, que decide o estado vazio (CA-11). |
| `desempenho/DesempenhoController` | novo | Rotas de `/api/v1/desempenho`; todas escopadas ao usuário do token. |
| `desempenho/dto/` | novo | `ResumoDesempenho`, `EvolucaoSemanal`, `SemanaEvolucao`, `DesempenhoDisciplinaResumo`, `PontosFracos`, `PontoFraco`. |
| `comum/evento/SessaoFinalizada` | novo | `record SessaoFinalizada(UUID usuarioId, UUID sessaoId, Instant finalizadaEm)`. Transversal porque o publicador (`estudo`) e o assinante (`desempenho`) não devem se conhecer. |
| `comum/ZonaAplicacao` | novo | Constante `America/Sao_Paulo`, usada para fechar a semana e o dia — ver *Principais decisões*. |
| `comum/excecao/*` | modificado | Reusa `RecursoNaoEncontradoException` e `ConflitoException` de `prd-banco-de-questoes`; acrescenta `CadernoAutomaticoImutavelException`. |

**Frontend — `frontend/src/app`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `core/api/caderno-api.ts` | novo | Cliente HTTP de `/cadernos` e `/cadernos/{id}/questoes`. |
| `core/api/desempenho-api.ts` | novo | Cliente dos quatro painéis de `/desempenho`. |
| `core/api/modelos-desempenho.ts` | novo | Tipagem dos contratos desta especificação. |
| `features/cadernos/cadernos-page` | novo | Grade de cards com ícone, nome, descrição, barra de progresso e meta (CA-01). |
| `features/cadernos/cadernos-store.ts` | novo | Signals da lista, ordenação (automáticos primeiro) e estado vazio. |
| `features/cadernos/caderno-dialog` | novo | Criar e renomear caderno manual (CA-04); mapeia 409 para erro no campo `nome`. |
| `features/cadernos/questoes-picker-dialog` | novo | Seleção de questões para o caderno, sobre `GET /questoes` (F2). |
| `features/stats/desempenho-page` | novo | Orquestra os quatro painéis; decide o estado vazio uma vez por `possuiHistorico` (CA-11). |
| `features/stats/evolucao-chart` | novo | SVG da curva (linha + área) e das barras de volume, traduzido do protótipo; quebra a linha em segmentos nas lacunas (CA-07) e expõe a tabela textual equivalente. |
| `features/stats/disciplinas-card` | novo | Barras por disciplina com volume e percentual (CA-08). |
| `features/stats/pontos-fracos-table` | novo | Tabela com assunto, disciplina, volume, percentual e a ação "Treinar" (CA-09). |
| `features/stats/desempenho-store.ts` | novo | Signals dos quatro painéis, `computed` dos caminhos SVG e da série textual. |
| `shared/ui/estado-vazio` | novo | Bloco de estado vazio com título, orientação e ação — reutilizável (CA-11). |
| `app.routes.ts` | modificado | `/cadernos` e `/desempenho` sob `authGuard`. |
| `protótipo/Certame.dc.html` | modificado | As sete lacunas listadas em *Riscos conhecidos*. |

**Fluxo de dados.** A tela de cadernos faz uma requisição e recebe a lista já unificada e já com progresso: automáticos projetados de `marcacao`, manuais lidos de `caderno`, o progresso de todos calculado em duas consultas agregadas — nunca uma por caderno. Abrir um card navega para `POST /estudo/sessoes` com `origem: "CADERNO"` e o id do caderno; a sessão pede a `SerieDoCadernoService` a lista de questões e segue o fluxo normal de F2. Ao finalizar, `estudo` publica `SessaoFinalizada`; `desempenho` recalcula os três agregados daquele usuário depois do commit. A tela de desempenho lê primeiro `/desempenho/resumo`: se `possuiHistorico` for `false`, ela renderiza o estado vazio e **não** dispara as outras três requisições.

## Design de implementação

### Principais interfaces

```
ConsultaCadernosService
  listar(UUID usuarioId) -> List<CadernoResumo>        // automaticos + manuais, com progresso (CA-01)

CadernoService
  criar(UUID usuarioId, RequisicaoCaderno) -> CadernoResumo          // CA-04
  atualizar(UUID usuarioId, UUID id, RequisicaoCaderno) -> CadernoResumo
  excluir(UUID usuarioId, UUID id) -> void                            // logica
  adicionarQuestoes(UUID usuarioId, UUID id, List<UUID>) -> CadernoResumo
  removerQuestao(UUID usuarioId, UUID id, UUID questaoId) -> void

CadernoAutomaticoService
  projetar(UUID usuarioId) -> List<CadernoResumo>      // le marcacao (CA-02, CA-03)

SerieDoCadernoService
  questoes(UUID usuarioId, ReferenciaCaderno) -> List<UUID>   // serie da sessao (CA-05)

ReferenciaCaderno                                       // regra pura
  de(String id) -> ReferenciaCaderno                    // "auto:FAVORITA" | UUID
  automatico() -> boolean

ProgressoCaderno                                        // regra pura
  percentual(int respondidas, int total) -> int         // total 0 => 0 (CA-04)
```

```
FonteRespostas                                          // porta implementada por estudo/
  porSemana(UUID usuarioId)     -> List<TotalPorChave<LocalDate>>
  porDisciplina(UUID usuarioId) -> List<TotalPorDisciplina>
  porAssunto(UUID usuarioId)    -> List<TotalPorAssunto>   // so sessoes finalizadas (RN-04)

AtualizadorAgregados
  atualizar(UUID usuarioId) -> void                     // upsert idempotente dos tres agregados

CalendarioSemanas                                       // regra pura, Clock injetado
  ultimasOitoSemanas() -> List<LocalDate>
  casar(List<LocalDate>, Map<LocalDate,TotalSemana>) -> List<SemanaEvolucao>   // lacuna, nao zero (CA-07)

EvolucaoSemanalService
  consultar(UUID usuarioId) -> EvolucaoSemanal          // RF-15

DisciplinaDesempenhoService
  consultar(UUID usuarioId) -> List<DesempenhoDisciplinaResumo>   // CA-08

SeletorPontosFracos                                     // regra pura
  selecionar(List<TotalPorAssunto> assuntos, int pctMedioUsuario) -> List<PontoFraco>

ResumoDesempenhoService
  consultar(UUID usuarioId) -> ResumoDesempenho         // KPIs + possuiHistorico (CA-11)
```

```
CadernosStore (signals)
  cadernos: Signal<CadernoResumo[]>                     // automaticos primeiro
  vazio: Signal<boolean>                                // nenhum caderno com questao (CA-11)
  criar(novo) / renomear(id, dados) / excluir(id) -> Promise<void>
  adicionarQuestoes(id, questaoIds) -> Promise<void>
  estudar(id) -> Promise<void>                          // POST /estudo/sessoes origem CADERNO

DesempenhoStore (signals)
  resumo, evolucao, disciplinas, pontosFracos: Signal<... | null>
  possuiHistorico: Signal<boolean>
  segmentosLinha: Signal<Ponto[][]>                     // a linha quebra nas lacunas (CA-07)
  serieTextual: Signal<LinhaTabela[]>                   // alternativa acessivel ao grafico
  carregar() -> Promise<void>
```

### Modelos de dados

Todos os contratos JSON usam `camelCase`; as colunas usam `snake_case`. Campos ausentes são serializados como `null` — nunca omitidos. Datas e semanas em ISO-8601; percentuais são inteiros de 0 a 100, e `null` significa **ausência de dado**, jamais zero.

#### `CadernoResumo` — item da lista de cadernos (RF-13)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` | sim | UUID do caderno manual, ou `auto:ERRO_RECORRENTE` / `auto:FAVORITA` para os automáticos. |
| `tipo` | `string` | sim | `AUTOMATICO` ou `MANUAL`. |
| `nome` | `string` | sim | Nome do caderno; fixo nos automáticos. |
| `descricao` | `string \| null` | sim | Descrição do usuário; texto fixo nos automáticos. |
| `totalQuestoes` | `integer` | sim | Total de questões hoje no caderno. |
| `questoesRespondidas` | `integer` | sim | Questões distintas do caderno já respondidas ao menos uma vez. |
| `progressoPercentual` | `integer` | sim | `questoesRespondidas / totalQuestoes` arredondado; `0` quando o caderno está vazio. |
| `atualizadoEm` | `string \| null` | sim | Última alteração do caderno manual, ou data da marcação mais recente no automático; `null` no automático vazio. |
| `editavel` | `boolean` | sim | `false` nos automáticos — a interface esconde renomear, excluir e adicionar questões. |

```text
{
  "id": "auto:ERRO_RECORRENTE",
  "tipo": "AUTOMATICO",
  "nome": "Erros recorrentes",
  "descricao": "Questões que você marcou como erro recorrente durante as sessões.",
  "totalQuestoes": 42,
  "questoesRespondidas": 18,
  "progressoPercentual": 43,
  "atualizadoEm": "2026-08-28T19:14:02Z",
  "editavel": false
}
```

> **Caderno recém-criado (CA-04):** nasce com `totalQuestoes: 0`, `questoesRespondidas: 0` e `progressoPercentual: 0`. A barra do card fica vazia, não escondida.

```text
{
  "id": "0f0b8b34-9a2e-4c3d-8a1f-7c9c2f4d5e6a",
  "tipo": "MANUAL",
  "nome": "Revisão de Direito Administrativo",
  "descricao": "Licitações e contratos para a segunda fase.",
  "totalQuestoes": 0,
  "questoesRespondidas": 0,
  "progressoPercentual": 0,
  "atualizadoEm": "2026-08-30T12:00:00Z",
  "editavel": true
}
```

#### `RequisicaoCaderno` — corpo de `POST /cadernos` e `PUT /cadernos/{id}`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `nome` | `string` | sim | 3 a 80 caracteres; único por usuário, sem diferenciar maiúsculas. |
| `descricao` | `string \| null` | não | Até 240 caracteres. |

```text
{
  "nome": "Revisão de Direito Administrativo",
  "descricao": "Licitações e contratos para a segunda fase."
}
```

#### `RequisicaoQuestoesCaderno` — corpo de `POST /cadernos/{id}/questoes`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `questaoIds` | `string[]` | sim | 1 a 200 UUIDs de questões publicadas. Ids repetidos ou já presentes são ignorados sem erro. |

```text
{
  "questaoIds": [
    "3f7a9c21-2b48-4f9e-9d31-6a1c8e2b7d40",
    "b1c4e5f6-7a89-4b0c-8d1e-2f3a4b5c6d7e"
  ]
}
```

#### `QuestaoCadernoResumo` — linha da listagem de questões do caderno

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `questaoId` | `string` | sim | UUID da questão. |
| `codigo` | `string` | sim | Código editorial (`Q-#####`). |
| `enunciadoResumo` | `string` | sim | Primeiros 160 caracteres do enunciado. |
| `disciplina` | `string` | sim | Nome da disciplina. |
| `assunto` | `string \| null` | sim | Nome do assunto, quando houver. |
| `respondida` | `boolean` | sim | Se o usuário já respondeu essa questão ao menos uma vez. |

```text
{
  "questaoId": "3f7a9c21-2b48-4f9e-9d31-6a1c8e2b7d40",
  "codigo": "Q-10482",
  "enunciadoResumo": "Acerca do regime jurídico das licitações públicas, julgue o item…",
  "disciplina": "Direito Administrativo",
  "assunto": "Licitações",
  "respondida": true
}
```

#### `ResumoDesempenho` — indicadores do topo da tela (RF-15/RF-16)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `totalRespostas` | `integer` | sim | Todas as respostas válidas do usuário (RN-04). |
| `acertos` | `integer` | sim | Respostas corretas. |
| `erros` | `integer` | sim | `totalRespostas - acertos`. |
| `pctAproveitamento` | `integer \| null` | sim | Percentual geral; `null` quando não há histórico. |
| `possuiHistorico` | `boolean` | sim | `false` dispara o estado vazio da tela inteira (CA-11). |

```text
{
  "totalRespostas": 3482,
  "acertos": 2368,
  "erros": 1114,
  "pctAproveitamento": 68,
  "possuiHistorico": true
}
```

> **Usuário sem histórico (CA-11):** todos os contadores em zero, `pctAproveitamento: null` e `possuiHistorico: false`. A tela não renderiza gráfico nem tabela — mostra a orientação de como começar.

```text
{
  "totalRespostas": 0,
  "acertos": 0,
  "erros": 0,
  "pctAproveitamento": null,
  "possuiHistorico": false
}
```

#### `EvolucaoSemanal` — série de até 8 semanas (RF-15)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `semanas` | `SemanaEvolucao[]` | sim | Sempre 8 itens, do mais antigo ao mais recente, incluindo as lacunas. |
| `pctMediaPeriodo` | `integer \| null` | sim | Média ponderada pelo volume, **ignorando as lacunas**; `null` se não houve atividade alguma. |
| `melhorSemana` | `SemanaEvolucao \| null` | sim | Semana de maior percentual entre as com atividade. |
| `volumeTotal` | `integer` | sim | Soma do volume das 8 semanas. |

#### `SemanaEvolucao` — ponto da série

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `semana` | `string` | sim | Segunda-feira da semana, em `AAAA-MM-DD` (fuso `America/Sao_Paulo`). |
| `rotulo` | `string` | sim | Rótulo curto do eixo X, em formato brasileiro (`06/07`). |
| `volume` | `integer` | sim | Respostas válidas na semana; `0` na lacuna. |
| `pctAcerto` | `integer \| null` | sim | **`null` é lacuna** — a linha do gráfico é interrompida, e nenhum ponto é desenhado (CA-07). |

```text
{
  "semanas": [
    { "semana": "2026-07-06", "rotulo": "06/07", "volume": 142, "pctAcerto": 61 },
    { "semana": "2026-07-13", "rotulo": "13/07", "volume": 168, "pctAcerto": 64 },
    { "semana": "2026-07-20", "rotulo": "20/07", "volume": 0,   "pctAcerto": null },
    { "semana": "2026-07-27", "rotulo": "27/07", "volume": 0,   "pctAcerto": null },
    { "semana": "2026-08-03", "rotulo": "03/08", "volume": 121, "pctAcerto": 66 },
    { "semana": "2026-08-10", "rotulo": "10/08", "volume": 190, "pctAcerto": 69 },
    { "semana": "2026-08-17", "rotulo": "17/08", "volume": 204, "pctAcerto": 71 },
    { "semana": "2026-08-24", "rotulo": "24/08", "volume": 176, "pctAcerto": 74 }
  ],
  "pctMediaPeriodo": 68,
  "melhorSemana": { "semana": "2026-08-24", "rotulo": "24/08", "volume": 176, "pctAcerto": 74 },
  "volumeTotal": 1001
}
```

> **Lacuna, nunca zero (CA-07).** O servidor devolve as 8 posições para que o eixo X não encolha, mas quem não estudou tem `pctAcerto: null`. O cliente **não** pode aplicar `?? 0`: `EvolucaoChart` quebra `linePath` e `areaPath` em segmentos a cada `null` e não desenha círculo nessas posições. A barra de volume continua sendo desenhada com altura zero, porque volume zero é um fato, não uma ausência.

#### `DesempenhoDisciplinaResumo` — barra por disciplina (RF-16, CA-08)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `disciplinaId` | `string` | sim | UUID da disciplina. |
| `disciplina` | `string` | sim | Nome da disciplina. |
| `volume` | `integer` | sim | Respostas válidas do usuário na disciplina. |
| `pctAcerto` | `integer` | sim | Percentual de acerto na disciplina. |

```text
[
  { "disciplinaId": "8c1d…", "disciplina": "Direito Constitucional", "volume": 812, "pctAcerto": 74 },
  { "disciplinaId": "2a9f…", "disciplina": "Direito Administrativo", "volume": 640, "pctAcerto": 66 },
  { "disciplinaId": "5b3e…", "disciplina": "Português",              "volume": 415, "pctAcerto": 81 }
]
```

> Ordenado por `volume desc`. Uma disciplina só aparece quando o usuário respondeu ao menos uma questão dela — o painel nunca mostra disciplina com volume zero. Lista vazia quando não há histórico (a tela já terá caído no estado vazio por `possuiHistorico`).

#### `PontosFracos` — envelope da lista (RF-16, CA-09/CA-10)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `pctMediaUsuario` | `integer \| null` | sim | Média geral do próprio usuário — é o limiar de comparação. |
| `minimoRespostas` | `integer` | sim | Sempre `20`; exposto para a interface explicar por que um assunto não aparece. |
| `itens` | `PontoFraco[]` | sim | Assuntos que passaram nos dois critérios, ordenados por impacto. |

#### `PontoFraco` — linha da tabela

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `assuntoId` | `string` | sim | UUID do assunto — é o que a ação "Treinar" leva para a sessão. |
| `assunto` | `string` | sim | Nome do assunto. |
| `disciplinaId` | `string` | sim | UUID da disciplina. |
| `disciplina` | `string` | sim | Nome da disciplina. |
| `volume` | `integer` | sim | Respostas válidas no assunto; sempre `≥ 20`. |
| `pctAcerto` | `integer` | sim | Percentual de acerto no assunto; sempre menor que `pctMediaUsuario`. |

```text
{
  "pctMediaUsuario": 68,
  "minimoRespostas": 20,
  "itens": [
    { "assuntoId": "d41c…", "assunto": "Controle de constitucionalidade",
      "disciplinaId": "8c1d…", "disciplina": "Direito Constitucional",
      "volume": 64, "pctAcerto": 41 },
    { "assuntoId": "77ab…", "assunto": "Licitações",
      "disciplinaId": "2a9f…", "disciplina": "Direito Administrativo",
      "volume": 25, "pctAcerto": 52 }
  ]
}
```

> **Ordenação por impacto, não por percentual.** O critério é `volume × (pctMediaUsuario - pctAcerto)`: um assunto com 41% em 64 respostas prejudica mais do que um com 38% em 20. É o que o PRD pede em US7 — "atacar primeiro o que mais me prejudica".

> **Amostra pequena (CA-10):** um assunto com 19 respostas e 12% de acerto **não** entra na lista e não aparece em lugar nenhum do payload. O `minimoRespostas` no envelope é o que permite à tela explicar a ausência sem inventar o número.

#### `ErroApi` — envelope de erro (`application/problem+json`)

| Código | HTTP | Significado |
| --- | --- | --- |
| `DADOS_INVALIDOS` | `422` | Falha de Bean Validation; `fieldErrors` campo a campo. |
| `NOME_CADERNO_EM_USO` | `409` | Já existe caderno com esse nome para o usuário; devolvido também em `fieldErrors.nome`. |
| `CADERNO_AUTOMATICO_IMUTAVEL` | `409` | Tentativa de renomear, excluir ou alterar questões de `auto:*`. |
| `CADERNO_NAO_ENCONTRADO` | `404` | Caderno inexistente, excluído **ou de outro usuário** — nunca 403, para não revelar existência. |
| `QUESTAO_NAO_PUBLICADA` | `422` | Ao menos um id enviado não é de questão publicada (RN-01). |
| `NAO_AUTENTICADO` | `401` | Token ausente, expirado ou inválido. |

```text
{
  "code": "NOME_CADERNO_EM_USO",
  "message": "Você já tem um caderno com esse nome.",
  "fieldErrors": { "nome": "Você já tem um caderno com esse nome." }
}
```

#### Esquema de banco — `V6__cadernos_desempenho.sql`

Migração versionada e imutável em `backend/src/main/resources/db/migration`. `V1__base_acesso.sql` já criou `usuario`; `V2__banco_questoes.sql` criou `questao`, `disciplina` e `assunto`; a migração de `prd-sessao-de-estudo` (prevista como `V4`) cria `sessao_estudo`, `resposta` e `marcacao`. **O número final é confirmado na entrega**: se F2 consumir mais de uma versão, esta migração assume a próxima livre, sem jamais editar as anteriores.

| Tabela | Colunas principais | Observações |
| --- | --- | --- |
| `caderno` | `id`, `usuario_id`, `nome`, `descricao`, `excluido_em`, `criado_em`, `atualizado_em` | Nome único por usuário entre os não excluídos, sem diferenciar maiúsculas. Sem coluna `tipo`: todo caderno em tabela é manual. |
| `caderno_questao` | `caderno_id`, `questao_id`, `adicionada_em` | PK composta — a mesma questão não entra duas vezes no mesmo caderno. Cascata a partir do caderno. |
| `desempenho_semanal` | `usuario_id`, `semana`, `volume`, `acertos`, `pct_acerto`, `atualizado_em` | PK `(usuario_id, semana)`; `semana` é a segunda-feira em `America/Sao_Paulo`. Só existem linhas de semanas com atividade — **a ausência de linha é a lacuna** de CA-07. |
| `desempenho_disciplina` | `usuario_id`, `disciplina_id`, `volume`, `acertos`, `pct_acerto`, `atualizado_em` | Alimenta CA-08 e, somada, a média geral do usuário e os KPIs do topo. |
| `desempenho_assunto` | `usuario_id`, `assunto_id`, `disciplina_id`, `volume`, `acertos`, `pct_acerto`, `atualizado_em` | Base de CA-09/CA-10. Só questões com assunto entram; questão sem assunto conta apenas em `desempenho_disciplina`. |
| `desempenho_pendencia` | `usuario_id` (PK), `marcado_em` | Fila do reconciliador. Linha presente = agregado possivelmente defasado. |

```sql
create table caderno (
  id            uuid primary key default gen_random_uuid(),
  usuario_id    uuid not null references usuario(id),
  nome          text not null,
  descricao     text,
  excluido_em   timestamptz,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),
  constraint caderno_nome_ck check (length(btrim(nome)) between 3 and 80)
);
create unique index caderno_nome_uk on caderno (usuario_id, lower(nome)) where excluido_em is null;
create index caderno_usuario_idx on caderno (usuario_id, atualizado_em desc) where excluido_em is null;

create table caderno_questao (
  caderno_id    uuid not null references caderno(id) on delete cascade,
  questao_id    uuid not null references questao(id),
  adicionada_em timestamptz not null default now(),
  primary key (caderno_id, questao_id)
);
create index caderno_questao_questao_idx on caderno_questao (questao_id);

create table desempenho_semanal (
  usuario_id    uuid not null references usuario(id) on delete cascade,
  semana        date not null,
  volume        integer not null default 0 check (volume >= 0),
  acertos       integer not null default 0 check (acertos >= 0),
  pct_acerto    smallint check (pct_acerto between 0 and 100),
  atualizado_em timestamptz not null default now(),
  primary key (usuario_id, semana),
  constraint desempenho_semanal_acertos_ck check (acertos <= volume)
);

create table desempenho_disciplina (
  usuario_id    uuid not null references usuario(id) on delete cascade,
  disciplina_id uuid not null references disciplina(id),
  volume        integer not null default 0 check (volume >= 0),
  acertos       integer not null default 0 check (acertos >= 0),
  pct_acerto    smallint check (pct_acerto between 0 and 100),
  atualizado_em timestamptz not null default now(),
  primary key (usuario_id, disciplina_id),
  constraint desempenho_disciplina_acertos_ck check (acertos <= volume)
);
create index desempenho_disciplina_volume_idx on desempenho_disciplina (usuario_id, volume desc);

create table desempenho_assunto (
  usuario_id    uuid not null references usuario(id) on delete cascade,
  assunto_id    uuid not null references assunto(id),
  disciplina_id uuid not null references disciplina(id),
  volume        integer not null default 0 check (volume >= 0),
  acertos       integer not null default 0 check (acertos >= 0),
  pct_acerto    smallint check (pct_acerto between 0 and 100),
  atualizado_em timestamptz not null default now(),
  primary key (usuario_id, assunto_id),
  constraint desempenho_assunto_acertos_ck check (acertos <= volume)
);
create index desempenho_assunto_fraco_idx on desempenho_assunto (usuario_id, volume desc)
  where volume >= 20;

create table desempenho_pendencia (
  usuario_id uuid primary key references usuario(id) on delete cascade,
  marcado_em timestamptz not null default now()
);

create index if not exists resposta_usuario_questao_idx on resposta (usuario_id, questao_id);
```

> **`acertos <= volume`** é a invariante que impede um agregado impossível de sobreviver a um bug de *upsert*. É barato no banco e transforma uma classe inteira de defeitos silenciosos em erro imediato — o mesmo raciocínio do `questao_publicada_completa` de `prd-banco-de-questoes`.

> **`desempenho_assunto_fraco_idx` é parcial em `volume >= 20`**: o índice cobre exatamente a população de CA-09 e ignora a cauda longa de assuntos com meia dúzia de respostas, que nunca serão consultados.

> **`resposta_usuario_questao_idx` é um acréscimo a uma tabela de `prd-sessao-de-estudo`.** É aditivo e idempotente (`if not exists`), e sustenta o cálculo de progresso do caderno, que cruza `caderno_questao` com `resposta`. Fica registrado como dependência para que F2 possa criá-lo na origem e esta linha vire inócua.

> **Cascata em `on delete cascade` a partir de `usuario`** nos quatro agregados: métricas de desempenho são dado pessoal (LGPD), e a exclusão do titular tem que levar os derivados junto. `caderno` mantém FK sem cascata porque a exclusão de usuário é lógica (`excluido_em`) — os cadernos seguem a mesma política do restante do domínio.

### Endpoints da API

Base `/api/v1`. Todas as rotas exigem autenticação e são **escopadas ao usuário do token** — nenhuma aceita `usuarioId` como parâmetro. Onze rotas: as de `/cadernos` e as três de `/desempenho` já constam da §6 da `TECH-SPEC.md` de forma agregada; `GET /desempenho/resumo`, `GET /cadernos/{id}/questoes` e `DELETE /cadernos/{id}/questoes/{questaoId}` são **acréscimos ao contrato** e a §6 é atualizada na mesma entrega, conforme a regra 4 do `AGENTS.md`.

#### Visão geral

| Método | Rota | Perfil | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/v1/cadernos` | autenticado | Lista unificada — automáticos e manuais, com progresso (CA-01). |
| `POST` | `/api/v1/cadernos` | autenticado | Cria caderno manual (CA-04). |
| `PUT` | `/api/v1/cadernos/{id}` | autenticado | Renomeia e redescreve caderno manual. |
| `DELETE` | `/api/v1/cadernos/{id}` | autenticado | Exclusão lógica do caderno manual. |
| `GET` | `/api/v1/cadernos/{id}/questoes` | autenticado | Questões do caderno, paginadas. |
| `POST` | `/api/v1/cadernos/{id}/questoes` | autenticado | Adiciona questões publicadas ao caderno (CA-04). |
| `DELETE` | `/api/v1/cadernos/{id}/questoes/{questaoId}` | autenticado | Remove uma questão do caderno. |
| `GET` | `/api/v1/desempenho/resumo` | autenticado | KPIs do topo e `possuiHistorico` (CA-11). |
| `GET` | `/api/v1/desempenho/evolucao` | autenticado | Série de 8 semanas com lacunas (CA-06, CA-07). |
| `GET` | `/api/v1/desempenho/disciplinas` | autenticado | Volume e percentual por disciplina (CA-08). |
| `GET` | `/api/v1/desempenho/pontos-fracos` | autenticado | Assuntos com ≥ 20 respostas abaixo da média (CA-09, CA-10). |

---

#### `GET /api/v1/cadernos`

Lista os cadernos do usuário autenticado: os dois automáticos primeiro, depois os manuais por `atualizado_em desc`.

**Parâmetros de consulta** — nenhum. A lista não é paginada: o protótipo mostra uma grade, e o volume é da ordem de dezenas.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `CadernoResumo[]` | Sempre que há autenticação válida — inclusive para usuário novo. |
| `401` | `ErroApi` | Token ausente ou inválido. |

**Exemplo — sucesso**

```http
GET /api/v1/cadernos
```

```text
[
  { "id": "auto:ERRO_RECORRENTE", "tipo": "AUTOMATICO", "nome": "Erros recorrentes",
    "descricao": "Questões que você marcou como erro recorrente durante as sessões.",
    "totalQuestoes": 42, "questoesRespondidas": 18, "progressoPercentual": 43,
    "atualizadoEm": "2026-08-28T19:14:02Z", "editavel": false },
  { "id": "auto:FAVORITA", "tipo": "AUTOMATICO", "nome": "Favoritas",
    "descricao": "Questões que você favoritou para rever depois.",
    "totalQuestoes": 17, "questoesRespondidas": 17, "progressoPercentual": 100,
    "atualizadoEm": "2026-08-27T08:02:11Z", "editavel": false },
  { "id": "0f0b8b34-9a2e-4c3d-8a1f-7c9c2f4d5e6a", "tipo": "MANUAL",
    "nome": "Revisão de Direito Administrativo",
    "descricao": "Licitações e contratos para a segunda fase.",
    "totalQuestoes": 40, "questoesRespondidas": 18, "progressoPercentual": 45,
    "atualizadoEm": "2026-08-29T21:40:00Z", "editavel": true }
]
```

**Exemplo — usuário novo, sem marcação e sem caderno manual (CA-11)**

```http
GET /api/v1/cadernos
```

```text
[
  { "id": "auto:ERRO_RECORRENTE", "tipo": "AUTOMATICO", "nome": "Erros recorrentes",
    "descricao": "Questões que você marcou como erro recorrente durante as sessões.",
    "totalQuestoes": 0, "questoesRespondidas": 0, "progressoPercentual": 0,
    "atualizadoEm": null, "editavel": false },
  { "id": "auto:FAVORITA", "tipo": "AUTOMATICO", "nome": "Favoritas",
    "descricao": "Questões que você favoritou para rever depois.",
    "totalQuestoes": 0, "questoesRespondidas": 0, "progressoPercentual": 0,
    "atualizadoEm": null, "editavel": false }
]
```

> Os automáticos **sempre** vêm na resposta, mesmo vazios: são a explicação de para que serve a tela. Quando todos os cadernos têm `totalQuestoes: 0`, `CadernosStore.vazio` fica verdadeiro e a página exibe a orientação de como começar em vez de uma grade de cards zerados.

---

#### `POST /api/v1/cadernos`

Cria um caderno manual vazio.

**Corpo** — `RequisicaoCaderno`.

| Campo | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `nome` | `string` | — | Obrigatório, 3–80 caracteres, único por usuário sem diferenciar maiúsculas. |
| `descricao` | `string` | `null` | Opcional, até 240 caracteres. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `CadernoResumo` | Criado; `Location` aponta para o recurso. |
| `409` | `ErroApi` | `NOME_CADERNO_EM_USO`. |
| `422` | `ErroApi` | `DADOS_INVALIDOS` com `fieldErrors`. |

**Exemplo — sucesso (CA-04)**

```http
POST /api/v1/cadernos
```

```text
{
  "id": "0f0b8b34-9a2e-4c3d-8a1f-7c9c2f4d5e6a",
  "tipo": "MANUAL",
  "nome": "Revisão de Direito Administrativo",
  "descricao": "Licitações e contratos para a segunda fase.",
  "totalQuestoes": 0,
  "questoesRespondidas": 0,
  "progressoPercentual": 0,
  "atualizadoEm": "2026-08-30T12:00:00Z",
  "editavel": true
}
```

**Exemplo — nome já usado**

```http
POST /api/v1/cadernos
```

```text
{
  "code": "NOME_CADERNO_EM_USO",
  "message": "Você já tem um caderno com esse nome.",
  "fieldErrors": { "nome": "Você já tem um caderno com esse nome." }
}
```

> O código vem **também** em `fieldErrors` para que o diálogo marque o campo `nome` sem precisar traduzir o `code` — mesmo tratamento que `prd-acesso-e-conta` dá a `EMAIL_EM_USO`.

---

#### `PUT /api/v1/cadernos/{id}` e `DELETE /api/v1/cadernos/{id}`

Renomeia/redescreve e exclui logicamente um caderno manual.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `CadernoResumo` | `PUT` bem-sucedido. |
| `204` | — | `DELETE` bem-sucedido. |
| `404` | `ErroApi` | `CADERNO_NAO_ENCONTRADO` — inexistente, já excluído ou **de outro usuário**. |
| `409` | `ErroApi` | `CADERNO_AUTOMATICO_IMUTAVEL` quando `{id}` é `auto:*`. |

**Exemplo — tentativa de renomear um automático**

```http
PUT /api/v1/cadernos/auto:FAVORITA
```

```text
{
  "code": "CADERNO_AUTOMATICO_IMUTAVEL",
  "message": "Os cadernos automáticos são montados a partir das suas marcações e não podem ser alterados.",
  "fieldErrors": null
}
```

> **404, nunca 403, para caderno de terceiro.** A interface já esconde o que não é do usuário; o servidor decide, e responder 403 revelaria que aquele id existe. Mesma política de RN-07 nos flashcards.

---

#### `GET /api/v1/cadernos/{id}/questoes`

Lista as questões do caderno. Aceita tanto UUID quanto `auto:*`.

**Parâmetros de consulta**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `page` | `integer` | `0` | ≥ 0. |
| `size` | `integer` | `20` | 1–100. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `PaginaResposta<QuestaoCadernoResumo>` | Caderno do usuário. |
| `404` | `ErroApi` | `CADERNO_NAO_ENCONTRADO`. |

**Exemplo — sucesso**

```http
GET /api/v1/cadernos/0f0b8b34-9a2e-4c3d-8a1f-7c9c2f4d5e6a/questoes?page=0&size=20
```

```text
{
  "content": [
    { "questaoId": "3f7a9c21-2b48-4f9e-9d31-6a1c8e2b7d40", "codigo": "Q-10482",
      "enunciadoResumo": "Acerca do regime jurídico das licitações públicas, julgue o item…",
      "disciplina": "Direito Administrativo", "assunto": "Licitações", "respondida": true }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 40
}
```

**Exemplo — caderno automático vazio**

```http
GET /api/v1/cadernos/auto:FAVORITA/questoes
```

```text
{ "content": [], "page": 0, "size": 20, "totalElements": 0 }
```

---

#### `POST /api/v1/cadernos/{id}/questoes` e `DELETE /api/v1/cadernos/{id}/questoes/{questaoId}`

Adiciona um lote de questões e remove uma questão do caderno manual.

**Corpo do `POST`** — `RequisicaoQuestoesCaderno`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `CadernoResumo` | `POST` aplicado; devolve o caderno com o total já atualizado. |
| `204` | — | `DELETE` aplicado. |
| `404` | `ErroApi` | `CADERNO_NAO_ENCONTRADO`. |
| `409` | `ErroApi` | `CADERNO_AUTOMATICO_IMUTAVEL` — a composição do automático vem da marcação, feita na sessão. |
| `422` | `ErroApi` | `QUESTAO_NAO_PUBLICADA` ou `DADOS_INVALIDOS`. |

**Exemplo — questão não publicada (RN-01)**

```http
POST /api/v1/cadernos/0f0b8b34-9a2e-4c3d-8a1f-7c9c2f4d5e6a/questoes
```

```text
{
  "code": "QUESTAO_NAO_PUBLICADA",
  "message": "Só é possível adicionar questões publicadas ao caderno.",
  "fieldErrors": { "questaoIds": "1 das 12 questões enviadas não está publicada." }
}
```

> **Ids repetidos ou já presentes não são erro.** O serviço faz `insert … on conflict do nothing`: adicionar de novo uma questão que já está no caderno é uma não operação, e o usuário vê o mesmo total. Erro só para questão inexistente ou não publicada — aí o lote inteiro é recusado, para não deixar o caderno num estado que o usuário não pediu.

---

#### `GET /api/v1/desempenho/resumo`

Indicadores do topo da tela e o sinalizador que decide o estado vazio.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `ResumoDesempenho` | Sempre. Ver os dois exemplos em *Modelos de dados*. |
| `401` | `ErroApi` | Token ausente ou inválido. |

> **É a primeira chamada da tela.** Com `possuiHistorico: false`, a página renderiza o estado vazio e **não** dispara `/evolucao`, `/disciplinas` nem `/pontos-fracos` — CA-11 vira uma decisão só, e o usuário novo custa uma requisição em vez de quatro.

---

#### `GET /api/v1/desempenho/evolucao`

Série semanal do percentual de acerto e do volume (RF-15).

**Parâmetros de consulta** — nenhum na v1. A janela é fixa em 8 semanas, conforme RF-15; um parâmetro de período está registrado em *Riscos conhecidos* como evolução possível.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `EvolucaoSemanal` | Sempre 8 posições. Ver exemplo em *Modelos de dados*. |
| `401` | `ErroApi` | Token ausente ou inválido. |

**Exemplo — usuário que só estudou nesta semana (CA-07, CA-11)**

```http
GET /api/v1/desempenho/evolucao
```

```text
{
  "semanas": [
    { "semana": "2026-07-06", "rotulo": "06/07", "volume": 0, "pctAcerto": null },
    { "semana": "2026-07-13", "rotulo": "13/07", "volume": 0, "pctAcerto": null },
    { "semana": "2026-07-20", "rotulo": "20/07", "volume": 0, "pctAcerto": null },
    { "semana": "2026-07-27", "rotulo": "27/07", "volume": 0, "pctAcerto": null },
    { "semana": "2026-08-03", "rotulo": "03/08", "volume": 0, "pctAcerto": null },
    { "semana": "2026-08-10", "rotulo": "10/08", "volume": 0, "pctAcerto": null },
    { "semana": "2026-08-17", "rotulo": "17/08", "volume": 0, "pctAcerto": null },
    { "semana": "2026-08-24", "rotulo": "24/08", "volume": 32, "pctAcerto": 59 }
  ],
  "pctMediaPeriodo": 59,
  "melhorSemana": { "semana": "2026-08-24", "rotulo": "24/08", "volume": 32, "pctAcerto": 59 },
  "volumeTotal": 32
}
```

> Um único ponto não forma linha: `EvolucaoChart` desenha o círculo e o rótulo, sem `path`. O eixo continua com as 8 posições, para que a semana seguinte não faça o gráfico "pular".

---

#### `GET /api/v1/desempenho/disciplinas`

Volume e percentual de acerto por disciplina (RF-16, CA-08).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `DesempenhoDisciplinaResumo[]` | Ordenado por volume desc. Ver exemplo em *Modelos de dados*. |
| `401` | `ErroApi` | Token ausente ou inválido. |

**Exemplo — sem histórico**

```http
GET /api/v1/desempenho/disciplinas
```

```text
[]
```

---

#### `GET /api/v1/desempenho/pontos-fracos`

Assuntos com ao menos 20 respostas e acerto abaixo da média do próprio usuário (RF-16).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `PontosFracos` | Ver exemplo em *Modelos de dados*. |
| `401` | `ErroApi` | Token ausente ou inválido. |

**Exemplo — nenhum assunto atinge o mínimo de respostas (CA-10)**

```http
GET /api/v1/desempenho/pontos-fracos
```

```text
{
  "pctMediaUsuario": 71,
  "minimoRespostas": 20,
  "itens": []
}
```

> Lista vazia com `pctMediaUsuario` preenchido é um estado legítimo e diferente do estado vazio da tela: o usuário tem histórico, mas nenhum assunto acumulou amostra suficiente. A tabela mostra a explicação ("nenhum assunto atingiu 20 respostas ainda"), não um vazio mudo.

---

## Pontos de integração

Nenhuma integração externa. A funcionalidade é inteiramente interna — PostgreSQL e as fundações de `prd-acesso-e-conta` e `prd-banco-de-questoes`. Não há chamada de rede a terceiros, e portanto não há timeout, retentativa ou idempotência de integração a especificar.

As integrações relevantes são **internas e todas em uma direção**:

| Integração | Direção | Contrato |
| --- | --- | --- |
| `prd-sessao-de-estudo` → cadernos | F2 chama `SerieDoCadernoService.questoes(usuarioId, referencia)` ao abrir sessão com `origem = CADERNO` (CA-05). | Lista ordenada de `questao_id`; lança `CADERNO_NAO_ENCONTRADO` se a referência não for do usuário. |
| `prd-sessao-de-estudo` → desempenho | F2 publica `comum/evento/SessaoFinalizada` ao fechar a sessão. | `record SessaoFinalizada(UUID usuarioId, UUID sessaoId, Instant finalizadaEm)`. O assinante é `@TransactionalEventListener(phase = AFTER_COMMIT)`: falha do agregado **não** desfaz nem atrasa a finalização; ela só marca `desempenho_pendencia`. |
| desempenho → `estudo` | Leitura de `resposta` pela porta `FonteRespostas`, implementada por `estudo/resposta/RespostaAgregadoAdapter`. | Três consultas agregadas por usuário, filtrando `sessao_estudo.finalizada_em is not null` (RN-04, CA-12). |
| cadernos → `questao` | Validação de publicação ao adicionar questões (RN-01) e leitura de `codigo`, `enunciado`, `disciplina`, `assunto` na listagem. | Por serviço de `questao`, nunca pelo repositório de outro domínio. |
| cadernos → `marcacao` | Projeção dos automáticos. `marcacao` é do mesmo domínio `estudo`, então o acesso é direto pelo repositório. | Somente leitura — nada nesta funcionalidade cria, altera ou apaga marcação (caso de borda 3). |

## Abordagem de testes

Meta de cobertura: **mínimo de 80%**, conforme `.agents/rules/tests.md` §1, que prevalece sobre os 70% citados na §9 da `TECH-SPEC.md` e no `AGENTS.md`. No backend a regra é aplicada pelo JaCoCo em `./mvnw verify` (80% de linhas por classe `*Service`); no frontend, por `npm run test:coverage` (80% em statements, branches, functions e lines). Ferramentas: JUnit Jupiter, AssertJ, Mockito e Testcontainers 2.0.5 no backend; Vitest e Playwright no frontend. Pirâmide alvo ~70/20/10.

Pela tabela de criticidade da rule, esta funcionalidade é de **prioridade média** — com duas exceções tratadas como alta: a imutabilidade de `resposta` (RN-02), provada aqui por omissão verificada, e o descarte de tentativas abandonadas (RN-04, CA-12), que é a diferença entre uma métrica honesta e uma mentira.

### Testes de unidade

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `CalendarioSemanas` gera 8 segundas-feiras terminando na semana corrente | CA-06 | Com `Clock` fixo em 2026-08-30 (domingo), a última chave é 2026-08-24 e a primeira, 2026-07-06. |
| TU-02 | `CalendarioSemanas` casa agregados e devolve lacuna onde não há linha | CA-07 | As posições sem agregado vêm com `volume: 0` e `pctAcerto: null` — nunca `0`. |
| TU-03 | `CalendarioSemanas` com atividade só na semana corrente | CA-07, CA-11 | Sete lacunas e um ponto; o tamanho da lista continua 8. |
| TU-04 | `CalendarioSemanas` na virada de ano | CA-06 | A janela atravessa 31/12 sem repetir nem pular semana; as chaves são segundas-feiras consecutivas. |
| TU-05 | `CalendarioSemanas` usa `America/Sao_Paulo`, não UTC | CA-06 | Resposta de domingo às 23h de Brasília cai na semana que termina naquele domingo, não na seguinte. |
| TU-06 | `EvolucaoSemanalService` calcula a média do período ignorando lacunas | CA-06, CA-07 | Média ponderada por volume só das semanas com atividade; duas lacunas não puxam a média para baixo. |
| TU-07 | `EvolucaoSemanalService` devolve melhor semana e volume total | CA-06 | Melhor semana é a de maior `pctAcerto` entre as com atividade; `volumeTotal` é a soma das oito. |
| TU-08 | `EvolucaoSemanalService` sem nenhuma atividade | CA-11 | Oito lacunas, `pctMediaPeriodo: null`, `melhorSemana: null`, `volumeTotal: 0`, sem divisão por zero. |
| TU-09 | `SeletorPontosFracos` inclui assunto com 25 respostas e acerto abaixo da média | CA-09 | Item presente com volume 25 e o percentual do assunto. |
| TU-10 | `SeletorPontosFracos` exclui assunto com 19 respostas e acerto baixíssimo | CA-10 | Lista vazia — o mínimo de 20 vence o percentual. |
| TU-11 | `SeletorPontosFracos` inclui assunto com exatamente 20 respostas | CA-10 | O limite é inclusivo: 20 entra, 19 não. |
| TU-12 | `SeletorPontosFracos` exclui assunto acima da média do usuário | CA-09 | Assunto com 80 respostas e 90% de acerto não é ponto fraco. |
| TU-13 | `SeletorPontosFracos` ordena por impacto, não por percentual | CA-09 | 41% em 64 respostas vem antes de 38% em 20. |
| TU-14 | `SeletorPontosFracos` com média de usuário nula | CA-11 | Lista vazia, sem `NullPointerException` nem divisão por zero. |
| TU-15 | `ProgressoCaderno` com 18 de 40 respondidas | CA-01 | 45%. |
| TU-16 | `ProgressoCaderno` de caderno vazio | CA-04 | 0%, sem divisão por zero. |
| TU-17 | `ProgressoCaderno` com todas respondidas | CA-01 | 100%. |
| TU-18 | `ReferenciaCaderno` interpreta `auto:FAVORITA`, `auto:ERRO_RECORRENTE` e UUID | CA-05 | Automático nos dois primeiros, manual no terceiro. |
| TU-19 | `ReferenciaCaderno` recusa id malformado e `auto:` desconhecido | CA-05 | Lança `RecursoNaoEncontradoException`, não `IllegalArgumentException` cru. |
| TU-20 | `CadernoService.criar` recusa nome duplicado do mesmo usuário | CA-04 | `ConflitoException` com `code` `NOME_CADERNO_EM_USO` e `fieldErrors.nome`. |
| TU-21 | `CadernoService.criar` aceita nome já usado por outro usuário | CA-04 | Persiste normalmente — a unicidade é por usuário. |
| TU-22 | `CadernoService` recusa renomear, excluir e alterar questões de caderno automático | CA-02, CA-03 | `CadernoAutomaticoImutavelException` nas três operações; nada é persistido. |
| TU-23 | `CadernoService.adicionarQuestoes` recusa o lote com questão não publicada | CA-04 | `QUESTAO_NAO_PUBLICADA`; nenhuma questão do lote é adicionada. |
| TU-24 | `CadernoService.adicionarQuestoes` ignora questão já presente | CA-04 | Total não muda e nenhuma exceção é lançada. |
| TU-25 | `CadernoAutomaticoService` projeta "Erros recorrentes" a partir das marcações | CA-02 | Item com id `auto:ERRO_RECORRENTE`, contagem igual às marcações e `editavel: false`. |
| TU-26 | `CadernoAutomaticoService` projeta "Favoritas" | CA-03 | Item com id `auto:FAVORITA` e a contagem das marcações de favorito. |
| TU-27 | `CadernoAutomaticoService` sem marcação alguma | CA-11 | Dois itens com `totalQuestoes: 0` e `atualizadoEm: null` — presentes, não omitidos. |
| TU-28 | Responder corretamente questão marcada não altera a projeção | Borda 3 | O item continua no caderno automático: nenhum caminho do serviço escreve em `marcacao`. |
| TU-29 | `ConsultaCadernosService` ordena automáticos antes dos manuais | CA-01 | Ordem: `ERRO_RECORRENTE`, `FAVORITA`, manuais por `atualizadoEm desc`. |
| TU-30 | `SerieDoCadernoService` devolve as 40 questões do caderno em ordem estável | CA-05 | Mesma ordem em duas chamadas seguidas; nenhuma questão de fora do caderno. |
| TU-31 | `AtualizadorAgregados` faz upsert somando volume e acertos por semana | CA-06 | Linha de `desempenho_semanal` com os totais da `FonteRespostas` e `pct_acerto` arredondado. |
| TU-32 | `AtualizadorAgregados` é idempotente | CA-06 | Duas execuções seguidas produzem exatamente o mesmo agregado. |
| TU-33 | `AtualizadorAgregados` não conta respostas de sessão não finalizada | CA-12 | A `FonteRespostas` é consultada com o filtro de finalização; a resposta abandonada não entra em nenhum dos três agregados. |
| TU-34 | `AtualizadorAgregados` separa questão sem assunto | CA-08, CA-09 | Conta em `desempenho_disciplina` e fica fora de `desempenho_assunto`. |
| TU-35 | `OuvinteSessaoFinalizada` marca pendência quando o upsert falha | CA-06 | `desempenho_pendencia` recebe a linha e a exceção não sobe — a finalização da sessão não é afetada. |
| TU-36 | `ResumoDesempenhoService` sem histórico | CA-11 | Contadores zerados, `pctAproveitamento: null`, `possuiHistorico: false`. |
| TU-37 | `ResumoDesempenhoService` deriva erros e aproveitamento dos agregados | CA-08 | `erros = total - acertos`; percentual arredondado. |
| TU-38 | `DesempenhoStore` quebra a linha do gráfico em segmentos nas lacunas | CA-07 | Série com lacuna no meio gera dois segmentos; nenhum ponto é desenhado na lacuna. |
| TU-39 | `DesempenhoStore` não converte `pctAcerto: null` em zero | CA-07 | Nenhum `?? 0` no caminho: o valor permanece nulo até a renderização. |
| TU-40 | `DesempenhoStore` monta a tabela textual equivalente ao gráfico | Acessibilidade (PRD) | Oito linhas com semana, volume e percentual; a lacuna aparece como "sem atividade". |
| TU-41 | `DesempenhoPage` renderiza o estado vazio com `possuiHistorico: false` | CA-11 | Não dispara as outras três requisições e mostra a orientação. |
| TU-42 | `CadernosStore` marca `vazio` quando todos os cadernos têm zero questões | CA-11 | `vazio()` verdadeiro mesmo com os dois automáticos presentes. |
| TU-43 | `caderno-api` mapeia 409 de nome duplicado para o campo `nome` | CA-04 | O `FormControl` `nome` recebe o erro; nenhum toast genérico é emitido. |
| TU-44 | Formatação pt-BR de percentuais, volumes e datas | UX (PRD) | `3.482`, `68%`, `24/08` — vírgula decimal e ponto de milhar do `pt-BR`. |

Componentes testados: as quatro regras puras (`CalendarioSemanas`, `SeletorPontosFracos`, `ProgressoCaderno`, `ReferenciaCaderno`) sem Spring e sem banco, com `Clock.fixed` onde há tempo; os serviços com repositórios e a porta `FonteRespostas` mockados à mão; os stores e clientes do frontend com Vitest e `HttpTestingController`. Mock apenas nas fronteiras — nunca no objeto sob teste. Nenhum destes testes sobe contexto Spring nem Testcontainers.

### Testes de integração

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | Migração `V6` aplica sobre a base das versões anteriores | — | Flyway sobe do zero; as seis tabelas e os índices existem, e o Hibernate valida o esquema (`ddl-auto=validate`). |
| TI-02 | Índice único de nome por usuário | CA-04 | Nome repetido no mesmo usuário viola; após exclusão lógica, o mesmo nome é aceito de novo. |
| TI-03 | PK composta de `caderno_questao` | CA-04 | A mesma questão não entra duas vezes; `on conflict do nothing` não lança. |
| TI-04 | Excluir caderno não toca em `resposta` nem em `marcacao` | RN-02 | `caderno_questao` some em cascata; a contagem de `resposta` do usuário permanece idêntica. |
| TI-05 | `check acertos <= volume` recusa agregado inconsistente | CA-06 | O `insert` falha; nenhuma linha impossível é persistida. |
| TI-06 | `GET /cadernos` com automáticos e manuais | CA-01 | Cada item traz nome, descrição, progresso e data da última atualização; automáticos primeiro. |
| TI-07 | `GET /cadernos` de usuário novo | CA-11 | Exatamente os dois automáticos, zerados, com `atualizadoEm: null`. |
| TI-08 | Questão marcada como erro recorrente aparece no caderno automático | CA-02 | Após inserir a marcação, `auto:ERRO_RECORRENTE` contém a questão e a contagem sobe. |
| TI-09 | Questão favoritada aparece em "Favoritas" | CA-03 | `auto:FAVORITA` contém a questão. |
| TI-10 | Responder corretamente questão marcada não a remove do caderno | Borda 3 | Após a resposta correta, a questão continua em `auto:ERRO_RECORRENTE` e a marcação segue em `marcacao`. |
| TI-11 | Criar caderno manual e adicionar questões | CA-04 | `POST` devolve 201 com progresso 0; após adicionar 40 questões, `totalQuestoes: 40` e `progressoPercentual: 0`. |
| TI-12 | `PUT`/`DELETE`/`POST questoes` em `auto:*` | CA-02, CA-03 | 409 `CADERNO_AUTOMATICO_IMUTAVEL` nas três rotas. |
| TI-13 | Caderno de outro usuário | Escopo de dados (PRD) | `GET`, `PUT`, `DELETE` e `POST /questoes` respondem 404, nunca 403 nem 200. |
| TI-14 | Rotas sem token | Segurança | 401 em todas as onze rotas. |
| TI-15 | Adicionar questão não publicada | RN-01 | 422 `QUESTAO_NAO_PUBLICADA`; o caderno continua com o total anterior. |
| TI-16 | Abrir sessão a partir de um caderno de 40 questões | CA-05 | A série tem exatamente as 40 questões do caderno, no modo Foco, e nenhuma questão de fora. |
| TI-17 | Progresso do caderno após responder 18 das 40 | CA-05 | `GET /cadernos` devolve `questoesRespondidas: 18` e `progressoPercentual: 45` sem escrita adicional. |
| TI-18 | Responder duas vezes a mesma questão do caderno | CA-01 | O progresso conta a questão uma vez só. |
| TI-19 | `GET /desempenho/evolucao` com atividade em várias semanas | CA-06 | Oito posições com percentual e volume coerentes com `resposta`. |
| TI-20 | `GET /desempenho/evolucao` com duas semanas sem atividade | CA-07 | As duas posições vêm com `pctAcerto: null` e `volume: 0`; as demais, preenchidas. |
| TI-21 | `GET /desempenho/disciplinas` | CA-08 | Cada disciplina com volume e percentual, ordenadas por volume desc. |
| TI-22 | `GET /desempenho/pontos-fracos` aplica os dois critérios | CA-09, CA-10 | Assunto com 25 respostas abaixo da média aparece; o de 19 respostas não, mesmo com acerto pior. |
| TI-23 | Respostas de Modo Prova abandonado não entram em métrica alguma | CA-12 | Após criar sessão de prova sem finalizar, resumo, evolução, disciplinas e pontos fracos ficam idênticos ao estado anterior. |
| TI-24 | Agregado consistente com `resposta` após finalização da sessão | CA-06 | O `@TransactionalEventListener` roda depois do commit e os três agregados batem com a contagem direta em `resposta`. |
| TI-25 | Reconciliador reconstrói agregado divergente e limpa a pendência | CA-06 | Após corromper o agregado à mão e marcar a pendência, o job restaura os valores corretos e apaga a linha de `desempenho_pendencia`. |
| TI-26 | Desempenho de um usuário nunca inclui resposta de outro | Escopo de dados (PRD) | Com dois usuários ativos na mesma base, cada resposta bate apenas com o próprio histórico. |
| TI-27 | Latência dos quatro painéis com 50 mil respostas e 8 semanas | Objetivos do PRD | p95 abaixo de 400 ms por painel, servido pelos agregados; o plano confirma uso dos índices e não varre `resposta`. |

Componentes testados em conjunto: controller + segurança + serviço + repositório + Flyway sobre PostgreSQL 18 real via Testcontainers (`@ServiceConnection`), sem H2 — `on conflict`, índice parcial, `check` de invariante e `date_trunc` com fuso só existem no Postgres. Fatia mais estreita por cenário: `@DataJpaTest` em TI-01…05 e TI-18; `@WebMvcTest` com `MockMvcTester` nos cenários de contrato e autorização (TI-12…15); `@SpringBootTest` só nos fluxos completos (TI-16, TI-17, TI-23…26). Dados por *builders* no pacote de teste, com e-mail e nome de caderno isolados por `UUID.randomUUID()`; TI-27 usa carga em massa por `insert … select generate_series`, montada uma vez por classe, e é a única marcada como lenta. O contexto Spring é compartilhado entre as classes de integração.

**Dependência de F2.** TI-04, TI-08…TI-10 e TI-16…TI-27 precisam de `sessao_estudo`, `resposta` e `marcacao`, que pertencem a `prd-sessao-de-estudo`. Enquanto aquela funcionalidade não estiver implementada, essas tabelas vêm de `db/migration-test/V991__sessao_resposta_marcacao_minima.sql`, restrita ao *classpath* de teste — o mesmo padrão já adotado por `prd-banco-de-questoes` com `V990`. Quando F2 criar as tabelas definitivas, as duas migrações de teste são removidas na mesma entrega e os testes passam a usar as reais.

### Testes E2E

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | Abrir a tela de cadernos | CA-01 | Cada card exibe nome, descrição, barra de progresso e data da última atualização, em formato brasileiro. |
| E2E-02 | Marcar erro recorrente na sessão e reencontrar a questão | CA-02 | Após marcar e voltar aos cadernos, a questão consta em "Erros recorrentes" e a contagem subiu. |
| E2E-03 | Favoritar uma questão e reencontrá-la | CA-03 | A questão consta em "Favoritas". |
| E2E-04 | Criar caderno manual com nome e descrição e adicionar questões | CA-04 | O caderno aparece na grade com progresso zerado e o total de questões adicionadas. |
| E2E-05 | Abrir um caderno de 40 questões e estudar | CA-05 | Inicia sessão restrita às 40 questões, no modo Foco; ao final, o progresso do caderno reflete o que foi respondido. |
| E2E-06 | Abrir o desempenho com histórico | CA-06 | Curva de 8 semanas com linha, área e barras de volume; média do período, melhor semana e total no rodapé do card. |
| E2E-07 | Semana sem atividade | CA-07 | A curva é interrompida naquela posição — sem ponto e sem queda a zero; o eixo mantém as 8 semanas. |
| E2E-08 | Desempenho por disciplina | CA-08 | Cada disciplina exibe volume e percentual de acerto. |
| E2E-09 | Pontos fracos com o corte de 20 respostas | CA-09, CA-10 | O assunto com 25 respostas abaixo da média aparece com assunto, disciplina, volume e percentual; o de 19 respostas não é listado. |
| E2E-10 | Usuário novo abre cadernos e desempenho | CA-11 | As duas telas mostram estado vazio com orientação; nenhum gráfico, nenhuma métrica fictícia, nenhum card zerado. |
| E2E-11 | Modo Prova abandonado não altera o desempenho | CA-12 | Após abandonar a tentativa e reabrir o desempenho, todos os números permanecem iguais aos de antes. |
| E2E-12 | "Treinar" a partir de um ponto fraco | Fluxo 4 (PRD) | A ação abre uma sessão de estudo restrita àquele assunto. |
| E2E-13 | Acessibilidade das duas telas | UI/UX (PRD) | Percurso completo por teclado com foco visível; o gráfico expõe alternativa textual equivalente para leitor de tela; contraste ≥ 4,5:1 em texto corrido; eixos rotulados. |

Execução com Playwright contra o SPA e a API reais, com PostgreSQL em contêiner e a semente de `dev`. Os seletores saem da marcação do protótipo traduzida 1:1 (`.card`, `.card-title`, `.card-meta`, `.table`, `.dialog`, `.btn-primary`, `.tag`). E2E-04, E2E-07 e E2E-10 dependem das lacunas de protótipo listadas em *Riscos conhecidos*, fechadas no passo 1 da ordem de construção. A verificação automatizada de acessibilidade e contraste (E2E-13) roda sobre as duas telas e sobre os três diálogos.

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Design: fechar as sete lacunas do protótipo** — diálogo "Novo caderno", menu do card (renomear/excluir), seletor de questões, os dois estados vazios, a representação da lacuna na série semanal e o volume por disciplina, tudo em `Certame.dc.html` com componentes Nocturne existentes. Primeiro porque a regra 1 do `AGENTS.md` impede que qualquer marcação nasça direto no Angular, e a aprovação visual libera os passos 7 e 8.
2. **Migração e persistência** — `V6__cadernos_desempenho.sql`, `db/migration-test/V991`, as seis entidades e os repositórios, com TI-01…TI-05. Antes de qualquer regra, porque o modelo sustenta o resto.
3. **Regras puras** — `ProgressoCaderno`, `ReferenciaCaderno`, `CalendarioSemanas` e `SeletorPontosFracos`, com TU-01…TU-19. São o coração de CA-07, CA-09 e CA-10 e não dependem de nada: escritas primeiro, custam milissegundos para rodar e travam a semântica antes que qualquer serviço a interprete de outro jeito.
4. **Cadernos** (RF-13) — `CadernoService`, `CadernoAutomaticoService`, `ConsultaCadernosService`, `SerieDoCadernoService`, `CadernoController` e os DTOs, com TU-20…TU-30 e TI-06…TI-15, TI-18.
5. **Porta e agregados** — `FonteRespostas`, `RespostaAgregadoAdapter`, `AtualizadorAgregados`, `comum/evento/SessaoFinalizada`, `OuvinteSessaoFinalizada` e `ReconciliadorAgregados`, com TU-31…TU-35 e TI-24, TI-25. Depois dos cadernos porque é aqui que a dependência de F2 fica mais forte.
6. **Desempenho** (RF-15, RF-16) — `EvolucaoSemanalService`, `DisciplinaDesempenhoService`, `PontosFracosService`, `ResumoDesempenhoService` e `DesempenhoController`, com TU-36, TU-37 e TI-19…TI-23, TI-26.
7. **Frontend de cadernos** — `caderno-api`, `CadernosStore`, `CadernosPage`, `CadernoDialog`, `QuestoesPickerDialog` e `shared/ui/estado-vazio`, em tradução 1:1 do protótipo aprovado no passo 1, com TU-42, TU-43.
8. **Frontend de desempenho** — `desempenho-api`, `DesempenhoStore`, `DesempenhoPage`, `EvolucaoChart`, `DisciplinasCard` e `PontosFracosTable`, com TU-38…TU-41, TU-44. Depois dos cadernos porque o gráfico é a parte mais delicada e se beneficia do `estado-vazio` já pronto.
9. **Integração com a sessão de estudo** — ligar a abertura de sessão por caderno e a ação "Treinar", com TI-16, TI-17.
10. **E2E e endurecimento** — E2E-01…E2E-13, verificação da cobertura de 80% nos dois lados, medição de latência (TI-27) e revisão de acessibilidade e contraste.

### Dependências técnicas

| Dependência | Situação | Impacto |
| --- | --- | --- |
| `prd-sessao-de-estudo` implementado (`sessao_estudo`, `resposta`, `marcacao`, `POST /estudo/sessoes`) | **Sem TechSpec ainda** — só PRD | **Bloqueia a entrega final.** Sem `marcacao` não há caderno automático; sem `resposta` não há desempenho; sem a abertura de sessão não há CA-05. Os passos 2–6 avançam sobre `db/migration-test/V991`, mas nada é dado por concluído antes de F2. |
| `prd-banco-de-questoes` implementado (`questao`, `disciplina`, `assunto`, `PaginaResposta<T>`, `confirmacao-dialog`) | Especificado, ainda não implementado | Bloqueia os passos 2 (FKs), 4 (validação de publicação) e 7 (reuso do diálogo de confirmação). |
| `prd-acesso-e-conta` implementado (`SegurancaConfig`, `authGuard`, `ManipuladorErrosGlobal`, `Usuario`, `styles.css`) | Especificado, ainda não implementado | Bloqueia tudo: sem a cadeia de segurança não há escopo por usuário, e sem `styles.css` não há tradução do protótipo. |
| Protótipo `Certame.dc.html` com `CADERNOS` e `DESEMPENHO` | **Presente** — inclusive o SVG da curva | Restam as sete lacunas do passo 1 e a aprovação visual, que libera os passos 7 e 8. |
| Número da migração (`V6`) | Confirmado nesta entrega; `V5` já é usado por outra alteração | A numeração não colide com migrações existentes. Nenhuma migração aplicada foi editada. |
| PostgreSQL 18 e Docker | Necessários para Testcontainers e para o Compose de `dev` | Bloqueiam a suíte de integração; `on conflict`, índice parcial e `date_trunc` com fuso não têm equivalente em banco em memória. |
| Playwright | Ausente do `package.json`; previsto em `prd-acesso-e-conta` | Bloqueia o passo 10. |
| `@Scheduled` habilitado (`@EnableScheduling`) | Ausente — nasce aqui | Bloqueia o reconciliador (passo 5); é uma anotação em `config/`. |

## Monitoramento e observabilidade

Sobre Actuator + Micrometer/Prometheus e logs JSON com `traceId`, conforme a §9 da `TECH-SPEC.md`.

| Sinal | Tipo | Uso |
| --- | --- | --- |
| `certame.caderno.criado` | contador | Adoção de cadernos manuais; alimenta a meta do PRD de ≥ 40% dos estudantes com caderno em uso ativo por semana. |
| `certame.caderno.sessao.iniciada` | contador, tag `tipo` (`AUTOMATICO`, `MANUAL`) | Mede *uso ativo*, não só criação — é o número que responde se a revisão dirigida está funcionando. |
| `certame.caderno.progresso` | *gauge* distribuído | Distribuição do percentual de conclusão; muitos cadernos parados em 0% indicam que o seletor de questões não está entregando o que o PRD promete. |
| `certame.desempenho.consulta` | *timer* (p95, p99), tag `painel` (`resumo`, `evolucao`, `disciplinas`, `pontos-fracos`) | Verifica em produção o objetivo de "carregar sem espera perceptível". |
| `certame.desempenho.agregado.atualizado` | *timer*, tag `origem` (`sessao`, `reconciliacao`) | Custo real do *upsert* e proporção entre atualização imediata e reconciliação. |
| `certame.desempenho.agregado.divergencia` | contador | **Alarme.** Quantas vezes a reconciliação encontrou o agregado diferente do histórico: acima de zero de forma sustentada, o caminho incremental tem defeito e a leitura está mentindo. |
| `certame.desempenho.pendencia.fila` | *gauge* | Tamanho de `desempenho_pendencia`; fila que cresce significa reconciliador parado. |
| `certame.desempenho.semanas.lacuna` | contador | Frequência de semanas sem atividade — é métrica de produto (abandono), não só técnica. |
| `/actuator/health` com indicador de banco | *health check* | Prontidão do contêiner; já existente. |

**Eventos de log.** `INFO` para criação, renomeação e exclusão de caderno e para cada execução do reconciliador, sempre com `usuarioId`, ação e contagens — nunca com enunciado de questão. `WARN` para falha do *upsert* na finalização da sessão (a pendência foi registrada e a tela pode mostrar dado defasado por até 15 minutos) e para divergência encontrada na reconciliação, com os valores antigo e novo. `ERROR` apenas para falha de infraestrutura. Nada de log por requisição de painel: são leituras de rotina, e o *timer* já as cobre.

## Considerações técnicas

### Principais decisões

- **Tabelas de agregado incrementais em vez de *materialized views*** (decisão do usuário). A §8 da `TECH-SPEC.md` previa MVs com `REFRESH` a cada 15 minutos, mas o `REFRESH` de uma MV varre a base inteira para atualizar um usuário, e o PRD é explícito: os painéis precisam refletir o estudo que acabou de acontecer. Três tabelas comuns com *upsert* por usuário na finalização da sessão custam O(1) por sessão, são testáveis em unidade e permitem `check` de invariante. O job `@Scheduled` de 15 minutos continua existindo — mudou de papel: não é mais o mecanismo de atualização, é a rede de segurança que reconcilia o que o caminho incremental errou. **A §8 é atualizada na mesma entrega.**
- **Cadernos automáticos são projeções de `marcacao`, não linhas em `caderno`** (decisão do usuário). Materializá-los exigiria um sincronizador entre `marcacao` e `caderno_questao`, e todo sincronizador tem um estado em que as duas pontas discordam. Sem ele, CA-02 e CA-03 são consequência do modelo, e o caso de borda 3 — acertar uma questão marcada não a remove do caderno — é verdadeiro porque **não existe código algum** que pudesse removê-la. O custo é um id sintético (`auto:*`) no contrato, tratado por uma regra pura de dezoito linhas.
- **Progresso é cobertura, não aproveitamento** (decisão do usuário). `questoesRespondidas / totalQuestoes`, com a questão contada uma vez por mais que seja respondida. O caderno responde "o que ainda preciso revisar", e o percentual de acerto já tem uma tela inteira dedicada a ele. Como é derivado, CA-05 ("o progresso é atualizado ao final") não custa escrita nenhuma: a próxima leitura já traz o número novo.
- **Nenhuma escrita em `resposta`, `sessao_estudo` ou `marcacao`.** RN-02 é respeitado por construção: esta funcionalidade só lê. É também o que mantém honesta a promessa do PRD de que os painéis derivam do histórico e nunca o alteram.
- **RN-04 é filtrado na origem, na porta `FonteRespostas`.** As três consultas exigem `sessao_estudo.finalizada_em is not null`. Filtrar mais adiante — no serviço ou na tela — deixaria a porta aberta para que uma consulta futura esquecesse do filtro e contaminasse a métrica com tentativas abandonadas (CA-12).
- **`desempenho` fala com `estudo` por uma porta, não pelo repositório.** O `code-standards.md` §3 proíbe um domínio de acessar o repositório de outro. `FonteRespostas` é declarada em `desempenho` e implementada em `estudo/resposta`; o serviço passa a ser testável em unidade com um *fake* de três métodos, sem banco.
- **`@TransactionalEventListener(AFTER_COMMIT)`, não chamada direta.** Se o *upsert* do agregado participasse da transação da finalização da sessão, um erro de estatística faria o estudante perder a sessão inteira. Depois do commit, a falha vira uma linha em `desempenho_pendencia` e um `WARN`; o pior caso é um painel defasado por 15 minutos.
- **Semana fecha na segunda-feira, no fuso `America/Sao_Paulo`.** Agregar em UTC jogaria o estudo de domingo à noite para a semana seguinte — o gráfico discordaria da memória do usuário. A constante fica em `comum/ZonaAplicacao`, e não em cada consulta.
- **Lacuna é `null`, não ausência de posição.** O servidor devolve sempre 8 itens para que o eixo X não encolha, mas quem não estudou vem com `pctAcerto: null`. Se a API omitisse a semana, cada cliente teria que reconstruir o calendário; se enviasse `0`, CA-07 estaria quebrado no contrato antes de chegar ao gráfico. TU-39 existe justamente para impedir o `?? 0` que alguém acabaria escrevendo.
- **Três agregados, não um.** `desempenho_disciplina` existe separado de `desempenho_assunto` porque `questao.assunto_id` é anulável: uma questão sem assunto tem que contar na disciplina (CA-08) e ficar fora dos pontos fracos (CA-09). Um agregado único com `assunto_id` nulo exigiria unicidade sobre coluna anulável — armadilha clássica em PostgreSQL.
- **A média do usuário sai de `desempenho_disciplina`, sem tabela própria.** `sum(acertos)/sum(volume)` sobre poucas dezenas de linhas é mais barato e menos sujeito a divergir do que uma quarta tabela para manter em sincronia. Os KPIs do topo vêm da mesma soma.
- **Pontos fracos ordenados por impacto (`volume × déficit`), não por percentual.** US7 pede "atacar primeiro o que mais me prejudica": 41% em 64 respostas custa mais pontos de prova do que 38% em 20. O critério fica no `SeletorPontosFracos`, testado por TU-13.
- **`minimoRespostas` viaja no envelope.** Sem ele, a tela teria que repetir o número 20 no código do frontend — uma regra de negócio duplicada em dois lugares e certa de divergir. Com ele, a explicação de por que um assunto não aparece é sempre a mesma do servidor.
- **`possuiHistorico` decide o estado vazio uma vez.** Deixar cada painel descobrir sozinho que está vazio produziria quatro estados vazios diferentes na mesma tela. Com o sinalizador no resumo, o usuário novo custa uma requisição, não quatro (CA-11).
- **Gráfico em SVG próprio, sem biblioteca.** O protótipo já traz o SVG completo — grid, barras de volume, área, linha, pontos e rótulos — sobre tokens Nocturne. Traduzi-lo é tradução 1:1; trocá-lo por uma biblioteca violaria a regra 1 do `AGENTS.md`, somaria peso ao *bundle* e ainda exigiria reconstruir a aparência com a API dela.
- **404 para caderno de terceiro, nunca 403.** Mesma política de RN-07: responder 403 confirmaria que aquele id existe.
- **Ids repetidos ao adicionar questões não são erro; questão não publicada é.** `on conflict do nothing` para o primeiro caso — reenviar um lote não pode punir o usuário. Para o segundo, o lote inteiro é recusado, para que ele não fique com metade das questões sem saber quais.
- **Sem parâmetro de período na v1.** RF-15 diz "série de até 8 semanas"; um seletor de intervalo é interface que o protótipo não tem e requisito que o PRD não pede. A rota nasce sem parâmetro, e acrescentá-lo depois é aditivo.
- **`resposta_usuario_questao_idx` criado aqui, com `if not exists`.** O progresso do caderno cruza `caderno_questao` com `resposta`, e sem esse índice a consulta degrada com o histórico. Criá-lo de forma idempotente permite que F2 o traga na origem sem conflito de migração.

### Riscos conhecidos

| Risco | Mitigação |
| --- | --- |
| **`prd-sessao-de-estudo` ainda não tem TechSpec**, e esta funcionalidade depende de três tabelas e de dois pontos de integração dela (`SerieDoCadernoService`, `SessaoFinalizada`). | Os dois contratos estão especificados aqui em *Pontos de integração*, com assinatura e semântica, para que a TechSpec de F2 os incorpore em vez de inventar outros. Enquanto isso, `db/migration-test/V991` sustenta os testes. É a dependência mais séria da entrega e está registrada como bloqueadora. |
| **O protótipo não cobre sete peças:** diálogo "Novo caderno" (CA-04 — só existe o botão), menu do card para renomear/excluir (só existe o ícone `dots-three`), seletor de questões para o caderno (CA-04 — inexistente), estado vazio de cadernos e de desempenho (CA-11), a representação da lacuna na curva (CA-07 — a linha do protótipo é contínua) e o volume por disciplina (CA-08 — o bloco mostra só o percentual). | Todas entram primeiro no `Certame.dc.html`, com componentes Nocturne existentes (`.dialog`, `.card`, `.input`, `.table`, `.tag`, `--color-danger` já registrado por `prd-acesso-e-conta`), no passo 1 da ordem de construção. Nenhuma marcação nasce direto no Angular. |
| **O card de caderno do protótipo tem ícone e cor por caderno (`c.icon`, `c.iconBg`), mas não há onde escolhê-los.** | O contrato não carrega ícone. Os automáticos recebem ícone fixo (`ph-warning-circle` e `ph-star`) e os manuais um ícone único (`ph-notebook`) com o papel neutro do Nocturne. Um seletor de ícone é funcionalidade de v2, não requisito de RF-13; o desvio fica registrado no protótipo no passo 1. |
| **O agregado incremental pode divergir do histórico** por um defeito no *upsert* ou por uma falha entre o commit da sessão e o evento. | Três camadas: `check acertos <= volume` no banco, `desempenho_pendencia` mais o reconciliador de 15 minutos, e a métrica `certame.desempenho.agregado.divergencia` como alarme. TI-25 prova a recuperação a partir de um agregado corrompido de propósito. |
| **A reconciliação recalcula o usuário inteiro**, e um usuário com histórico muito grande pode tornar o job lento. | O job processa a fila em lotes e a fila só recebe quem falhou — no caminho feliz ela fica vazia. `certame.desempenho.pendencia.fila` monitora; se crescer, o escape é reconciliar só as semanas afetadas, mudança contida no `AtualizadorAgregados`. |
| **Fuso fixo `America/Sao_Paulo`** para fechar a semana: um usuário fora desse fuso vê a semana do Brasil. | Aceito na v1 — o produto é para concursos brasileiros e o PRD determina formatos brasileiros. Trocar por fuso do usuário é acrescentar uma coluna em `usuario` e um parâmetro em `CalendarioSemanas`, que já recebe a zona como dependência justamente por isso. |
| **Id sintético `auto:*` no mesmo campo que um UUID** pode ser tratado como UUID por um cliente futuro. | O campo é `string` no contrato, nunca `uuid`, e `ReferenciaCaderno` é o único ponto que o interpreta, com TU-18 e TU-19 cobrindo formato válido e inválido. |
| **O seletor de questões do caderno depende de `GET /questoes`**, entregue por F2 para o perfil estudante. | Se a rota não estiver pronta no passo 7, o caderno manual nasce vazio e é preenchido pela ação "adicionar ao caderno" da sessão de estudo — que é o fluxo mais natural de qualquer forma. CA-04 exige o seletor, então o passo 7 não fecha sem ele. |
| **A meta de ≥ 40% de estudantes com caderno em uso ativo é premissa a calibrar**, como o próprio PRD admite. | Instrumentada por `certame.caderno.sessao.iniciada` e `certame.caderno.progresso`, que medem uso e não apenas criação — é o dado que permitirá calibrar a meta depois do lançamento. |
| **Exclusão do titular (LGPD)** precisa levar os quatro agregados junto; a exportação de dados pessoais não está nesta entrega. | `on delete cascade` a partir de `usuario` nos quatro agregados resolve a exclusão física. A exportação a pedido do titular é requisito transversal do PRD-mãe e fica para o endurecimento da F5, junto com a de `resposta` — registrado aqui para não se perder. |

### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. `.agents/rules/code-standards.md` e `.agents/rules/tests.md` lidos na íntegra — são as duas únicas rules do diretório. **Nota:** as TechSpecs de `prd-acesso-e-conta` e `prd-banco-de-questoes` afirmam que `.agents/rules/` não existia; ele existe agora, e a diferença material é a **cobertura mínima, que passa de 70% para 80%** — adotada nesta especificação e recomendada para reavaliação daquelas duas.

| Regra | Como esta especificação atende |
| --- | --- |
| 1 — Frontend é tradução 1:1 do protótipo, tokens Nocturne, sem UI de terceiros | As duas telas saem de `Certame.dc.html` (`CADERNOS`, `DESEMPENHO`) e do `styles.css` do Nocturne, sem valor cru. O gráfico é o SVG do próprio protótipo, não uma biblioteca. As sete lacunas entram primeiro no protótipo (passo 1), não no código. Nenhuma dependência de UI é adicionada. |
| 2 — Autorização validada no servidor com `@PreAuthorize` | Todas as onze rotas exigem autenticação e são escopadas ao usuário do token; nenhuma aceita `usuarioId` como parâmetro. TI-13, TI-14 e TI-26 provam 401, 404 para recurso de terceiro e isolamento entre usuários no servidor, não na interface. |
| 3 — Interface em pt-BR; código, tabelas e colunas em português sem acento | Classes `CadernoService`, `SeletorPontosFracos`, `CalendarioSemanas`; colunas `pct_acerto`, `atualizado_em`, `excluido_em`, `adicionada_em`; mensagens de erro e rótulos em pt-BR; números, percentuais e datas em formato brasileiro. |
| 4 — Nenhum endpoint fora do contrato da §6 sem atualizar o documento | As rotas de `/cadernos` e as três de `/desempenho` já constam da §6. `GET /desempenho/resumo`, `GET /cadernos/{id}/questoes` e `DELETE /cadernos/{id}/questoes/{questaoId}` são acréscimos e exigem **atualização da §6 na mesma entrega**. A §8 também é atualizada (agregados incrementais no lugar das *materialized views*). Ambas registradas em *Arquivos relevantes*. |
| 5 — Migração só por Flyway, versionada e imutável | `V6__cadernos_desempenho.sql` em `db/migration`, aditiva; migração de teste em `db/migration-test/V991`, fora do *classpath* de produção. Nenhuma migração aplicada é editada. |
| 6 — `resposta` imutável e Modo Prova | Respeitado nas duas pontas: nenhum caminho desta funcionalidade escreve em `resposta` (TI-04), e a porta `FonteRespostas` só enxerga sessões finalizadas (TU-33, TI-23, CA-12). |
| 7, 8, 9 — Flashcards, diálogo na sessão e fila de revisão | Não se aplicam. O desempenho de flashcards é explicitamente de `prd-flashcards-decks-e-revisao` (RF-34), fora do escopo deste PRD. |
| Toda tarefa cita o requisito | Cada componente, endpoint e caso de teste referencia RF-13, RF-15, RF-16, RN-01, RN-02, RN-04 ou os CA-01…CA-12 do PRD. |
| Domínio + testes antes de controller e frontend | A ordem de construção segue exatamente isso: regras puras (3) → serviços (4, 5, 6) → controllers → frontend (7, 8). |
| Cobertura mínima de 80% (`tests.md` §1, prevalece) | Declarada na abordagem de testes e verificada no passo 10, por `./mvnw verify` e `npm run test:coverage`. |
| `tests.md` §2 — FIRST | *Fast*: as quatro regras puras e todos os serviços rodam sem Spring e sem banco; Testcontainers só em `*IntegracaoTest`. *Independent*: dados por *builders*, nomes isolados por `UUID.randomUUID()`, sem estado estático. *Repeatable*: `Clock.fixed` em todo cálculo de semana (TU-01…TU-05), `ORDER BY` explícito nas consultas, `vi.setSystemTime` no frontend. *Self-validating*: AssertJ e `expect` com asserção sobre valor e efeito — TU-02 falha se a lacuna virar zero. |
| `tests.md` §3 — AAA/Given-When-Then e nomes | Três blocos visíveis em todo teste; nomes `TU02_devolveLacunaNaSemanaSemAtividade`, `TI20_evolucaoMarcaSemanasSemAtividadeComoLacuna`, `it('quebra a linha do gráfico nas lacunas')`. |
| `tests.md` §4 — pirâmide | 44 testes de unidade, 27 de integração e 13 E2E: ~52% / ~32% / ~16% dos casos nomeados, com o peso de execução concentrado na base. Nada subiu de camada sem necessidade — autorização e SQL só se provam acima da unidade. |
| `code-standards.md` §2 — arquivos ≤ 100 linhas, métodos ≤ 30, ≤ 3 parâmetros, ≤ 3 níveis de condicional, sem comentário, sem linha em branco no método, sem literal mágico | A decomposição em regras puras mais serviços curtos é o que torna o limite alcançável; `DesempenhoPage` é dividida em quatro componentes filhos por esse motivo. Todo serviço recebe no máximo `usuarioId` mais um objeto de parâmetro. O mínimo de 20 respostas e a janela de 8 semanas são constantes nomeadas em `SeletorPontosFracos` e `CalendarioSemanas`, nunca literais espalhados. |
| `code-standards.md` §3 — camadas, injeção por construtor, sem Lombok, DTO `record`, entidade fora do controller, erro por `ErroNegocioException` | Controllers só orquestram; a regra e o `@Transactional` ficam no serviço; entidades escrevem construtores e getters à mão. Todos os DTOs são `record` com mensagem de validação em pt-BR. Os quatro códigos de erro novos passam pelo `ManipuladorErrosGlobal` existente. |
| `code-standards.md` §4 — Angular standalone, zoneless, signals, `input()`/`output()`, template e estilo separados, HTTP só em `core/api` | Todos os componentes são standalone com estado em `signal`/`computed`; os stores são `providedIn: 'root'`; nenhum componente injeta `HttpClient`; 422 e 409 são mapeados campo a campo (TU-43). |
| Perguntar em vez de inventar, e registrar a decisão | Quatro decisões abertas foram levadas ao usuário — estratégia de agregados, natureza dos cadernos automáticos, definição de progresso e tratamento da dependência de F2 — e estão registradas em *Principais decisões*. |

### Conformidade com skills

| Skill | Aplicável | Como é observada |
| --- | --- | --- |
| `313-frameworks-spring-db-migrations-flyway` | sim | SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração; `validate-on-migrate` ligado; disciplina *forward-only* — nenhuma migração aplicada é reescrita; `spring.flyway.locations` separa produção, `dev` e teste; a migração é aditiva e retrocompatível (*expand/contract*), inclusive o índice `if not exists` sobre `resposta`. |
| `postgresql-table-design` | sim | PKs `uuid` com `gen_random_uuid()` e PKs compostas naturais nos agregados; `timestamptz` em UTC e `date` para a chave de semana; `check` de invariante (`acertos <= volume`) e de faixa (`pct_acerto between 0 and 100`); índice único parcial compatível com exclusão lógica; índice parcial `where volume >= 20` alinhado exatamente à consulta de pontos fracos; `on delete cascade` deliberado a partir de `usuario` nos agregados e ausente onde a exclusão é lógica; `on conflict do nothing` no vínculo N:N. |
| `spring-boot-testing` | sim | Fatia mais estreita por cenário: JUnit + Mockito puros nas regras e serviços, `@DataJpaTest` com Testcontainers nas *constraints* e índices, `@WebMvcTest` com `MockMvcTester` no contrato e na autorização, `@SpringBootTest` só nos fluxos completos e no `@TransactionalEventListener`; asserções em AssertJ; contexto compartilhado para manter a suíte rápida. **Desvio registrado:** o `AGENTS.md` fala em "JUnit 5", enquanto o BOM do Spring Boot 4.1.1 traz JUnit Jupiter 6 — a API é a mesma e prevalece a versão gerenciada pelo BOM. |
| `angular-developer` | sim | Componentes standalone, detecção *zoneless*, estado por signals em serviços `providedIn: 'root'`, `input()`/`output()` como funções, `computed` para os caminhos SVG e para a série textual, guardas funcionais e `LOCALE_ID` pt-BR para números e datas. |
| `impeccable` | não | O layout é congelado pela regra 1 do `AGENTS.md`. As sete lacunas são acréscimos com componentes Nocturne existentes, não redesenho; a acessibilidade exigida pelo PRD é coberta por TU-40 e E2E-13. |
| `find-skills`, `criar-*`, `executar-*` | não | São skills de processo, não de implementação. |

### Arquivos relevantes e dependentes

**Criados — backend** (`backend/src/main/java/br/com/certame/`)

`estudo/caderno/Caderno.java` · `estudo/caderno/CadernoQuestao.java` · `estudo/caderno/CadernoQuestaoId.java` · `estudo/caderno/TipoCadernoAutomatico.java` · `estudo/caderno/ReferenciaCaderno.java` · `estudo/caderno/ProgressoCaderno.java` · `estudo/caderno/CadernoRepository.java` · `estudo/caderno/CadernoQuestaoRepository.java` · `estudo/caderno/CadernoService.java` · `estudo/caderno/CadernoAutomaticoService.java` · `estudo/caderno/ConsultaCadernosService.java` · `estudo/caderno/SerieDoCadernoService.java` · `estudo/caderno/CadernoController.java` · `estudo/caderno/dto/CadernoResumo.java` · `estudo/caderno/dto/RequisicaoCaderno.java` · `estudo/caderno/dto/RequisicaoQuestoesCaderno.java` · `estudo/caderno/dto/QuestaoCadernoResumo.java` · `estudo/resposta/RespostaAgregadoAdapter.java` · `desempenho/FonteRespostas.java` · `desempenho/DesempenhoSemanal.java` · `desempenho/DesempenhoDisciplina.java` · `desempenho/DesempenhoAssunto.java` · `desempenho/DesempenhoPendencia.java` · `desempenho/DesempenhoSemanalRepository.java` · `desempenho/DesempenhoDisciplinaRepository.java` · `desempenho/DesempenhoAssuntoRepository.java` · `desempenho/DesempenhoPendenciaRepository.java` · `desempenho/AtualizadorAgregados.java` · `desempenho/OuvinteSessaoFinalizada.java` · `desempenho/ReconciliadorAgregados.java` · `desempenho/CalendarioSemanas.java` · `desempenho/EvolucaoSemanalService.java` · `desempenho/DisciplinaDesempenhoService.java` · `desempenho/SeletorPontosFracos.java` · `desempenho/PontosFracosService.java` · `desempenho/ResumoDesempenhoService.java` · `desempenho/DesempenhoController.java` · `desempenho/dto/` · `comum/evento/SessaoFinalizada.java` · `comum/ZonaAplicacao.java` · `comum/excecao/CadernoAutomaticoImutavelException.java` · `backend/src/main/resources/db/migration/V5__cadernos_desempenho.sql` · `backend/src/main/resources/db/migration-dev/V902__seed_cadernos_desempenho_dev.sql` · `backend/src/test/resources/db/migration-test/V991__sessao_resposta_marcacao_minima.sql`

**Criados — frontend** (`frontend/src/app/`)

`core/api/caderno-api.ts` · `core/api/desempenho-api.ts` · `core/api/modelos-desempenho.ts` · `features/cadernos/cadernos-page/` · `features/cadernos/cadernos-store.ts` · `features/cadernos/caderno-dialog/` · `features/cadernos/questoes-picker-dialog/` · `features/stats/desempenho-page/` · `features/stats/desempenho-store.ts` · `features/stats/evolucao-chart/` · `features/stats/disciplinas-card/` · `features/stats/pontos-fracos-table/` · `shared/ui/estado-vazio/`

**Modificados**

`backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java` (códigos `CADERNO_AUTOMATICO_IMUTAVEL`, `QUESTAO_NAO_PUBLICADA`, `NOME_CADERNO_EM_USO`) · `backend/src/main/java/br/com/certame/config/` (`@EnableScheduling` para o reconciliador) · `frontend/src/app/app.routes.ts` (`/cadernos` e `/desempenho` sob `authGuard`) · `protótipo/Certame.dc.html` (as sete lacunas: diálogo de novo caderno, menu do card, seletor de questões, dois estados vazios, lacuna na curva e volume por disciplina) · **`TECH-SPEC.md` §6** (as três rotas acrescentadas ao contrato, conforme a regra 4 do `AGENTS.md`) · **`TECH-SPEC.md` §8** (agregados incrementais e reconciliação no lugar das *materialized views*) · **`TECH-SPEC.md` §5** (as seis tabelas novas; `caderno` sem coluna `tipo`)

**Dependentes** — `prd-sessao-de-estudo` (F2) implementa os dois pontos de integração especificados aqui: chama `SerieDoCadernoService` ao abrir sessão com `origem = CADERNO` (CA-05) e publica `comum/evento/SessaoFinalizada` na finalização; também deve criar `resposta_usuario_questao_idx` na origem, tornando a linha desta migração inócua. `prd-simulados` (RF-14, RF-21) reaproveita `SessaoFinalizada` e os agregados para o resultado do simulado. `prd-flashcards-decks-e-revisao` (RF-34) traz o desempenho de flashcards, explicitamente fora deste escopo, e deve seguir o mesmo padrão de agregado incremental em vez de criar um segundo mecanismo. `prd-gestao-de-usuarios` reaproveita `shared/ui/estado-vazio`.
