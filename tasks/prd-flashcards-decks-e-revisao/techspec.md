# Especificação técnica

> **Funcionalidade:** Flashcards — decks, limites e revisão espaçada (FSRS) · **PRD:** `tasks/prd-flashcards-decks-e-revisao/prd.md` · **Fase:** F6 — Flashcards
> Deriva de `TECH-SPEC.md` v1.3 (§3–§9, recortadas para RF-33…RF-36, RN-07, RN-08, RN-10 e RN-11).
> Depende de `tasks/prd-flashcards-criacao-e-gerenciamento/techspec.md` (F6a), que funda `deck`, `flashcard`, `flashcard_revisao`, `VerificadorDonoFlashcard`, `ConsultaFilaFlashcards`, `shared/ui/dialog`, `shared/ui/toast` e `core/api/flashcard-api.ts`; de `tasks/prd-acesso-e-conta/techspec.md` (segurança, `authGuard`, shell, `styles.css`) e de `tasks/prd-banco-de-questoes/techspec.md` (`disciplina`, `assunto`, os três tipos de exceção e o `confirmacao-dialog`).
> Entrega a metade de agendamento e organização: a sessão de revisão, o resumo, a tela de decks e a configuração de limites por deck.

## Resumo

O que define este desenho é uma pergunta única: **quem calcula o intervalo?** CA-03 exige que cada botão de classificação mostre, *antes* da escolha, o intervalo que seria agendado — e CA-07 exige que o intervalo agendado seja o do FSRS. Se a tela calculasse a previsão e o servidor calculasse o agendamento, seriam duas implementações do mesmo algoritmo, e a promessa do PRD ("o estudante nunca classifica às cegas") viraria uma coincidência a ser mantida à mão a cada mudança de peso. Aqui existe **um só agendador**, em `flashcard/fsrs`, e as quatro previsões viajam prontas dentro de cada card da fila. O cliente não conhece os pesos, não conhece a curva e não faz aritmética de agendamento: ele exibe rótulos que o servidor já formatou em dias e minutos. CA-03 deixa de ser um cuidado e passa a ser uma propriedade estrutural — a previsão e o agendamento saem literalmente da mesma chamada de método.

O agendador é o **FSRS-5 completo**, com memória de curto prazo: além das fórmulas de estabilidade e dificuldade de longo prazo, implementa `S · e^(w17·(g−3+w18))` para revisões do mesmo dia e os passos de aprendizado e reaprendizado. Isso tem uma consequência de esquema que não é opcional: `flashcard.proxima_revisao` é `date`, e um card que volta em dez minutos não cabe em um dia civil. A migração desta entrega acrescenta `proxima_revisao_em timestamptz` e `passo_aprendizado`, mantendo `proxima_revisao` sincronizado por um `check` de coerência — a coluna antiga continua servindo à listagem, aos filtros e aos indicadores de F6a sem uma linha alterada, e a nova sustenta a fila intradiária. Toda a noção de "hoje" — fila do dia, limite diário, vencimento — passa por um único `RelogioEstudo` sobre a zona `America/Sao_Paulo`, porque um limite diário calculado em UTC viraria em Brasília às 21h.

A segunda decisão estrutural é **onde o limite diário é contado**. RN-10 fala em fila *diária*, não em fila *por sessão*: quem revisa 30 cards, fecha a aba e volta não ganha mais 60. Por isso a montagem desconta o consumo do dia lido de `flashcard_revisao`, agrupado por deck e por card distinto — um card que passou por três passos de aprendizado consome uma vaga, não três. É o que faz CA-06 valer no mundo real, e é também o que dá sentido ao aviso de CA-18: a fila vencida só cresce acima do dobro do limite quando o estudante realmente parou de revisar.

A sessão ganha tabela própria, `flashcard_sessao`, e uma porta de leitura `TempoEstudoFlashcards` que o Início consome para somar o tempo ao total do dia (CA-11/RF-06). Não reusamos `sessao_estudo` porque aquela tabela pertence a `prd-sessao-de-estudo`, que ainda não tem TechSpec — depender do enum `modo` dela transformaria uma entrega pronta em uma entrega bloqueada. O deck, enfim, completa o CRUD que F6a deixou pela metade: renomear, descrever, configurar os quatro limites e excluir. Excluir é a operação mais delicada do documento e a única que o PRD trata como risco de perda de conteúdo — resolvida na mesma transação, com `on delete set null` como segunda barreira e um teste que conta os cards antes e depois.

## Arquitetura do sistema

### Visão dos componentes

```
Angular 22 (SPA)                                Spring Boot 4.1                          PostgreSQL 18
────────────────                                ───────────────                          ─────────────
features/flashcards/                                                                     ┌── flashcard_sessao   (nova)
  DecksPage                 ────▶ GET    /api/v1/flashcards/decks         (enriquecido)  │
  DeckCard                  ────▶ PUT    /api/v1/flashcards/decks/{id}    (novo)         ├── flashcard          (+3 colunas)
  DeckDialog                ────▶ DELETE /api/v1/flashcards/decks/{id}    (novo)         ├── flashcard_revisao  (+3 colunas)
  DecksStore                                                                             ├── deck               (F6a)
                                                                                         └── usuario, disciplina (F1/F3)
  SessaoRevisaoPage         ────▶ POST   /api/v1/flashcards/sessoes               (novo)
  CardRevisao               ────▶ POST   /api/v1/flashcards/sessoes/{id}/revisoes (novo)
  NotasRevisao              ────▶ POST   /api/v1/flashcards/sessoes/{id}/finalizar(novo)
  ResumoRevisao             ────▶ GET    /api/v1/flashcards/resumo                (novo)
  FilaVazia
  RevisaoStore  ·  AtalhosRevisao  ·  CronometroSessao

shared/ui/dialog (F6a) · shared/ui/toast (F6a) · shared/ui/confirmacao-dialog (F3)
shared/pipes/intervalo-pipe (novo)
```

**Backend — pacote `br.com.certame.flashcard`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `flashcard/fsrs/PesosFsrs` | novo | Os 19 pesos padrão publicados do FSRS-5, `DECAY = −0.5`, `FACTOR = 19/81` e os limites de saturação de estabilidade. Constantes `static final` — parâmetros do sistema versionados junto ao código (RN-08). Nenhum outro arquivo repete um número do algoritmo. |
| `flashcard/fsrs/CurvaEsquecimento` | novo | Duas funções puras: `retrievability(S, dias)` e `intervaloParaRetencao(S, retencaoAlvo)`. É o par que traduz "o intervalo é o tempo até a retenção cair ao alvo do deck" (RN-08) em aritmética. |
| `flashcard/fsrs/CalculoDificuldade` | novo | `inicial(nota)` e `proxima(dificuldadeAtual, nota)`, com reversão à média em `D0(FACIL)` e saturação em [1, 10]. |
| `flashcard/fsrs/CalculoEstabilidade` | novo | `inicial(nota)`, `aposAcerto(...)`, `aposLapso(...)` e `curtoPrazo(S, nota)`. Separado da dificuldade porque são quatro fórmulas independentes e o arquivo cabe em 100 linhas (`code-standards.md` §2.2). |
| `flashcard/fsrs/PassosAprendizado` | novo | Os passos de aprendizado (1 min, 10 min) e de reaprendizado (10 min) e a máquina de transição entre `NOVO`, `APRENDENDO`, `REVISAO` e `REAPRENDENDO`. É o que a escolha por FSRS-5 completo acrescenta ao agendamento. |
| `flashcard/fsrs/AgendadorFsrs` | novo | **O único ponto que agenda.** `proximo(estado, nota, contexto)` devolve o agendamento e `previsoes(estado, contexto)` devolve os quatro intervalos — implementada chamando `proximo` uma vez por nota, para que previsão e agendamento não possam divergir (CA-03). Sem Spring, sem repositório, sem relógio próprio. |
| `flashcard/fsrs/EstadoMemoria`, `ContextoAgendamento`, `Agendamento`, `PrevisaoIntervalos` | novos | `record`s de entrada e saída do agendador. `ContextoAgendamento(retencaoAlvo, intervaloMaxDias, diasDecorridos, agora)` é o objeto parâmetro que mantém o agendador em três argumentos (`code-standards.md` §2.4). |
| `flashcard/NotaRevisao` | novo | Enum `ERREI(1)`, `DIFICIL(2)`, `BOM(3)`, `FACIL(4)`. O ordinal do FSRS é campo explícito, não `ordinal()`. |
| `flashcard/RelogioEstudo` | novo | `hoje()`, `inicioDoDia()` e `instante()` sobre `Clock` injetado e a zona `certame.estudo.zona`. Ponto único da noção de dia civil — é o que torna todo teste de fila `Repeatable` (`tests.md` §2). |
| `flashcard/revisao/SessaoRevisao` | novo | Entidade da sessão: dono, escopo (deck ou disciplina), início, fim, contagem por nota e tempo total. |
| `flashcard/revisao/FlashcardRevisao` | novo | Entidade do log imutável. A tabela nasceu em F6a; a entidade nasce aqui, sem *setter* algum além do construtor. |
| `flashcard/revisao/MontadorFila` | novo | RN-10: monta a fila deck a deck, desconta o consumo do dia, aplica `novos_dia` e `revisoes_dia` por deck e ordena por vencimento. A regra mais crítica do documento depois do FSRS. |
| `flashcard/revisao/ConsumoDiario` | novo | `record` com `novos` e `revisoes` por deck, mais a consulta agregada que o produz a partir de `flashcard_revisao`. |
| `flashcard/revisao/SessaoRevisaoService` | novo | RF-33/RF-34: abrir a sessão com a fila montada e finalizá-la com o resumo. `@Transactional`, `@PreAuthorize("isAuthenticated()")`. |
| `flashcard/revisao/RegistroRevisaoService` | novo | RN-08: aplica o agendador, escreve o bloco de memória do card, grava a linha imutável de log e devolve o resultado com as previsões atualizadas quando o card volta à fila do dia. |
| `flashcard/revisao/ResumoRevisaoService` | novo | RF-34 e `GET /flashcards/resumo`: distribuição por nota, tempo total e faixas de vencimento futuro. Somente leitura. |
| `flashcard/revisao/TempoEstudoFlashcards` | novo | Porta de leitura para RF-06: `segundosNoDia(usuarioId, dia)` sobre sessões finalizadas. É o contrato que o Início e o painel de desempenho consomem (CA-11). |
| `flashcard/revisao/RevisaoController` | novo | `/api/v1/flashcards/sessoes` e `/api/v1/flashcards/resumo`. Só orquestra; o dono vem do token (RN-07). |
| `flashcard/deck/LimitesDeck` | novo | `record` dos quatro limites com `normalizar()` — o ajuste ao intervalo permitido de CA-16 — e a constante `PADRAO` de "Sem deck" (RN-11). Regra pura, sem Spring. |
| `flashcard/deck/PainelDecksService` | novo | RF-35: total, vencendo hoje, novos, resumo dos limites e o aviso de fila acumulada (CA-18), inclusive a linha virtual "Sem deck". |
| `flashcard/deck/Deck` | **modificado (F6a)** | Ganha `setNome`, `setDescricao` e `aplicarLimites(LimitesDeck)`. Nenhuma outra coluna passa a ser mutável. |
| `flashcard/deck/DeckService` | **modificado (F6a)** | Ganha `atualizar` (renomear, descrever, configurar) e `excluir` (RN-11, na mesma transação). `listar` passa a delegar ao `PainelDecksService`. |
| `flashcard/deck/DeckController` | **modificado (F6a)** | Ganha `PUT` e `DELETE /api/v1/flashcards/decks/{id}`. |
| `flashcard/FlashcardRepository` | **modificado (F6a)** | Ganha a consulta da fila por escopo e as contagens do painel. Toda assinatura continua recebendo `usuarioId` (RN-07). |
| `comum/excecao/*` | reutilizados | `RecursoNaoEncontradoException`, `DadosInvalidosException` e `ConflitoException` cobrem os cinco códigos desta entrega. `ManipuladorErrosGlobal` **não muda**. |

**Frontend — `frontend/src/app`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `core/api/revisao-api.ts` | novo | Cliente das quatro rotas de sessão e resumo. |
| `core/api/modelos-revisao.ts` | novo | Tipagem dos contratos desta especificação. |
| `core/api/flashcard-api.ts` | **modificado (F6a)** | Ganha `atualizarDeck` e `excluirDeck`; `listarDecks` passa a devolver `DeckDetalhe`. |
| `shared/pipes/intervalo-pipe.ts` | novo | Formata o rótulo de intervalo em pt-BR quando o cliente precisa recompor um (o servidor já manda pronto no caminho principal). |
| `features/flashcards/decks-page/` | novo | RF-35: a grade de decks, em tradução 1:1 do protótipo. |
| `features/flashcards/deck-card/` | novo | Um card de deck: contadores, resumo dos limites, aviso e "Estudar este deck". |
| `features/flashcards/deck-dialog/` | novo | RF-36: nome, descrição e os quatro limites, sobre o `shared/ui/dialog` de F6a. Sem rota (regra do `AGENTS.md`). |
| `features/flashcards/decks-store.ts` | novo | Signals da tela de decks: lista, carregando, salvando, erro de campo. |
| `features/flashcards/sessao-revisao/` | novo | RF-33: a página `/flashcards/estudar`, orquestrando card, notas, resumo e estado vazio. |
| `features/flashcards/sessao-revisao/card-revisao/` | novo | Frente, verso sob demanda e a linha de memória do card (CA-04). |
| `features/flashcards/sessao-revisao/notas-revisao/` | novo | Os quatro botões com rótulo, intervalo previsto e atalho (CA-03, CA-05). |
| `features/flashcards/sessao-revisao/resumo-revisao/` | novo | RF-34: quatro cartões, próximos vencimentos e as duas ações. |
| `features/flashcards/sessao-revisao/fila-vazia/` | novo | CA-19: o estado vazio, sem oferecer antecipação. |
| `features/flashcards/revisao-store.ts` | novo | Fila em memória, índice, revelado, contagem por nota e reenfileiramento dos passos curtos. |
| `features/flashcards/atalhos-revisao.ts` | novo | Espaço revela; 1–4 classificam. Ignora eventos vindos de campo de texto e teclas de classificação com o verso oculto. |
| `features/flashcards/cronometro-sessao.ts` | novo | Tempo da sessão por signal, pausado em `document.hidden` — o número que CA-11 soma ao dia. |
| `layout/shell/shell.html` | **modificado (F6a)** | O contador de "vencendo hoje" do item Flashcards passa a ser alimentado por `GET /flashcards/resumo`. |
| `app.routes.ts` | **modificado (F6a)** | `/flashcards/decks` e `/flashcards/estudar` sob `authGuard`; a revisão aceita `?deck=` e `?disciplina=`. |
| `protótipo/Certame.dc.html` | **modificado** | Três lacunas pontuais — detalhadas em *Riscos conhecidos*. |

**Fluxo de dados.** A tela de decks faz **uma** requisição (`GET /flashcards/decks`) que traz, por deck, os contadores, os limites e o aviso; nada é calculado no cliente. "Estudar este deck" navega para `/flashcards/estudar?deck={id}`, que abre a sessão com `POST /flashcards/sessoes` e recebe a **fila inteira** — frente, verso, memória e as quatro previsões de cada card. A partir daí a sessão é local: revelar o verso e avançar não tocam a rede, o que satisfaz a restrição de desempenho do PRD ("com o card seguinte já disponível"). Cada classificação dispara um `POST .../revisoes` que grava o log e devolve o novo agendamento; a interface já avançou para o card seguinte quando a resposta chega. Se a resposta disser `reenfileirar`, o card volta à fila ordenado pelo novo vencimento, com as previsões recalculadas — é assim que os passos de aprendizado do FSRS-5 aparecem sem uma segunda requisição de fila. A finalização envia `POST .../finalizar` e recebe o resumo já consolidado no servidor, inclusive as faixas de vencimento futuro, que o cliente não teria como calcular sem baixar o acervo inteiro.

## Design de implementação

### Principais interfaces

```
AgendadorFsrs                                                   // puro; sem Spring, sem Clock
  proximo(EstadoMemoria, NotaRevisao, ContextoAgendamento) -> Agendamento
  previsoes(EstadoMemoria, ContextoAgendamento)            -> PrevisaoIntervalos  // CA-03

CurvaEsquecimento
  retrievability(double estabilidade, double dias)         -> double   // (1 + F·t/S)^DECAY
  intervaloParaRetencao(double estabilidade, double alvo)  -> double   // S/F · (alvo^(1/DECAY) − 1)

CalculoEstabilidade
  inicial(NotaRevisao)                                     -> double   // w[g−1]
  aposAcerto(double S, double D, double R, NotaRevisao)    -> double
  aposLapso(double S, double D, double R)                  -> double   // CA-08
  curtoPrazo(double S, NotaRevisao)                        -> double   // S · e^(w17·(g−3+w18))

CalculoDificuldade
  inicial(NotaRevisao)                                     -> double   // clamp(w4 − e^(w5·(g−1)) + 1)
  proxima(double dificuldade, NotaRevisao)                 -> double   // reversao a D0(FACIL)

PassosAprendizado
  proximoEstado(EstadoFlashcard, int passo, NotaRevisao)   -> Transicao  // estado + passo + atraso
```

```
MontadorFila
  montar(UUID usuarioId, EscopoFila, RelogioEstudo)        -> List<Flashcard>   // RN-10, CA-06

ConsumoDiarioRepository
  porDeck(UUID usuarioId, Instant inicioDoDia)             -> List<ConsumoDiario>

SessaoRevisaoService
  abrir(UUID usuarioId, RequisicaoSessaoRevisao)           -> RespostaSessaoRevisao   // CA-01, CA-19
  finalizar(UUID sessaoId, UUID usuarioId)                 -> ResumoSessao            // CA-10

RegistroRevisaoService
  registrar(UUID sessaoId, UUID usuarioId, RequisicaoRevisao) -> RespostaRevisao      // CA-07…CA-09

ResumoRevisaoService
  resumoDoUsuario(UUID usuarioId)                          -> ResumoFlashcards
  proximosVencimentos(UUID usuarioId)                      -> List<FaixaVencimento>

TempoEstudoFlashcards
  segundosNoDia(UUID usuarioId, LocalDate dia)             -> int                     // CA-11 / RF-06

PainelDecksService
  painel(UUID usuarioId)                                   -> List<DeckDetalhe>       // CA-12, CA-18

DeckService                                                 // metodos novos sobre F6a
  atualizar(UUID id, UUID usuarioId, RequisicaoDeck)       -> DeckDetalhe   // CA-13, CA-14, CA-16, CA-17
  excluir(UUID id, UUID usuarioId)                         -> int           // CA-15; devolve cards movidos

LimitesDeck
  normalizar()                                             -> LimitesDeck   // CA-16
  PADRAO                                                   -> LimitesDeck   // 10 / 60 / 180 / 0.90 (RN-11)
```

```
RevisaoStore (signals)
  abrir(escopo: EscopoFila) -> Promise<void>
  revelar() -> void                                  // CA-02
  classificar(nota: NotaRevisao) -> Promise<void>    // CA-05
  finalizar() -> Promise<void>                       // CA-10
  cardAtual: Signal<CardRevisao | null>
  revelado: Signal<boolean>                          // false a cada card novo (CA-01)
  restantes: Signal<number>
  progressoPct: Signal<number>
  filaVazia: Signal<boolean>                         // CA-19
  resumo: Signal<ResumoSessao | null>

DecksStore (signals)
  carregar() -> Promise<void>
  salvar(deck: RequisicaoDeck, id?: string) -> Promise<void>   // CA-13, CA-16
  excluir(id: string) -> Promise<void>                          // CA-15
  decks: Signal<DeckDetalhe[]>
  erroDeCampo: Signal<Record<'nome', string | null>>
```

### Modelos de dados

Todos os contratos são JSON `application/json`, com campos em `camelCase`. Campo ausente na origem é normalizado para `null`, nunca omitido — é a convenção já adotada por F6a. Rótulos textuais (`intervaloRotulo`, `memoriaRotulo`, `avisoMensagem`) são gerados no servidor em português do Brasil: o cliente exibe, não formata, para que a tela e o agendamento nunca discordem.

#### `CardRevisao` — um card da fila, com tudo que a sessão precisa (RF-33)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Identificador do flashcard. |
| `frente` | `string` | sim | Exibida ao entrar no card (CA-01). |
| `verso` | `string` | sim | Enviado junto, revelado sob demanda no cliente (CA-02) — nenhuma requisição ao revelar. |
| `deckId` | `string` (uuid) | não | `null` para card sem deck. |
| `deckNome` | `string` | sim | `"Sem deck"` quando `deckId` é `null`. |
| `disciplinaNome` | `string` | não | `null` quando o card não tem disciplina. |
| `assuntoNome` | `string` | não | `null` quando o card não tem assunto. |
| `estado` | `string` | sim | `NOVO`, `APRENDENDO`, `REVISAO` ou `REAPRENDENDO`. |
| `memoria` | `MemoriaCard` | sim | Estabilidade, dificuldade e retenção atual (CA-04). |
| `previsoes` | `PrevisaoNota[4]` | sim | Uma entrada por nota, na ordem `ERREI`, `DIFICIL`, `BOM`, `FACIL` (CA-03). |

#### `MemoriaCard` — o bloco de memória exibido no verso (CA-04)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `estabilidade` | `number` | sim | Em dias, duas casas. `0` em card nunca revisado. |
| `dificuldade` | `number` | sim | De 1 a 10, duas casas. `0` em card nunca revisado. |
| `retencaoAtual` | `number` | não | De 0 a 1. `null` em card nunca revisado — não há curva antes do primeiro contato. |
| `rotulo` | `string` | sim | Texto pronto para a tela, em português e sem jargão. |

#### `PrevisaoNota` — o que cada classificação agendaria (CA-03)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `nota` | `string` | sim | `ERREI`, `DIFICIL`, `BOM` ou `FACIL`. |
| `minutos` | `number` | sim | Intervalo em minutos — cobre tanto os passos curtos quanto os intervalos longos. |
| `rotulo` | `string` | sim | `"1 min"`, `"10 min"`, `"1 dia"`, `"9 dias"`. |

```text
{
  "id": "b7c1e2a0-4d3f-4a1b-9c8d-7e6f5a4b3c2d",
  "frente": "Efeitos da decisão de mérito em ADI",
  "verso": "Eficácia erga omnes e efeito vinculante aos demais órgãos do Judiciário e à Administração Pública (art. 102, §2º, CF).",
  "deckId": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d",
  "deckNome": "Constitucional — controle",
  "disciplinaNome": "Direito Constitucional",
  "assuntoNome": "Controle de constitucionalidade",
  "estado": "REVISAO",
  "memoria": {
    "estabilidade": 8.4,
    "dificuldade": 5.1,
    "retencaoAtual": 0.87,
    "rotulo": "Estabilidade 8 dias · dificuldade 5,1 de 10 · retenção hoje 87%"
  },
  "previsoes": [
    { "nota": "ERREI",   "minutos": 10,    "rotulo": "10 min" },
    { "nota": "DIFICIL", "minutos": 8640,  "rotulo": "6 dias" },
    { "nota": "BOM",     "minutos": 12960, "rotulo": "9 dias" },
    { "nota": "FACIL",   "minutos": 25920, "rotulo": "18 dias" }
  ]
}
```

> **Card em primeiro contato.** `estabilidade` e `dificuldade` valem `0`, `retencaoAtual` é `null` e o rótulo muda de texto. As previsões continuam presentes — elas vêm da estabilidade inicial `w[nota−1]`, que existe antes de qualquer revisão.

```text
{
  "estado": "NOVO",
  "memoria": { "estabilidade": 0, "dificuldade": 0, "retencaoAtual": null,
               "rotulo": "Primeiro contato com este card" },
  "previsoes": [
    { "nota": "ERREI",   "minutos": 1,    "rotulo": "1 min" },
    { "nota": "DIFICIL", "minutos": 10,   "rotulo": "10 min" },
    { "nota": "BOM",     "minutos": 10,   "rotulo": "10 min" },
    { "nota": "FACIL",   "minutos": 5760, "rotulo": "4 dias" }
  ]
}
```

#### `RequisicaoSessaoRevisao` — corpo de `POST /flashcards/sessoes`

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `escopo` | `string` | sim | `TODOS`, `DECK` ou `SEM_DECK`. Fora disso, `422`. |
| `deckId` | `string` (uuid) | condicional | Obrigatório quando `escopo` é `DECK`; ignorado nos demais. Deck de terceiro responde `404`. |
| `disciplinaId` | `string` (uuid) | não | Restringe a fila à disciplina, combinável com qualquer escopo (RF-33). |

```text
{ "escopo": "DECK", "deckId": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d", "disciplinaId": null }
```

#### `RespostaSessaoRevisao` — corpo de `201` na abertura

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Sessão criada; usada nas duas rotas seguintes. |
| `iniciadaEm` | `string` (ISO-8601) | sim | Instante em UTC. |
| `escopoRotulo` | `string` | sim | `"Todos os decks"`, o nome do deck ou `"Sem deck"`. |
| `total` | `number` | sim | Tamanho da fila montada — alimenta "Card 1 de N" e a barra de progresso. |
| `fila` | `CardRevisao[]` | sim | Ordenada por vencimento; vazia quando não há nada a revisar (CA-19). |

```text
{
  "id": "5f4e3d2c-1b0a-4998-8776-6554433221100",
  "iniciadaEm": "2026-08-30T13:02:11Z",
  "escopoRotulo": "Constitucional — controle",
  "total": 24,
  "fila": [ { "id": "b7c1e2a0-…", "frente": "…" } ]
}
```

> **Fila vazia não é erro.** Sem cards vencidos nem novos dentro dos limites, a resposta é `201` com `total: 0` e `fila: []`; a sessão é criada e pode ser finalizada com zero revisões. CA-19 exige exatamente isso — informar que a fila terminou, **sem** antecipar revisões futuras. Nenhum caminho desta especificação seleciona card com vencimento posterior a hoje.

```text
{ "id": "…", "iniciadaEm": "…", "escopoRotulo": "Todos os decks", "total": 0, "fila": [] }
```

#### `RequisicaoRevisao` — corpo de `POST /flashcards/sessoes/{id}/revisoes`

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `flashcardId` | `string` (uuid) | sim | Card do próprio usuário e com `status = ATIVO`; caso contrário `404`. |
| `nota` | `string` | sim | `ERREI`, `DIFICIL`, `BOM` ou `FACIL`. |
| `tempoMs` | `number` | sim | De 0 a 600000. Acima disso é truncado — tempo de card esquecido aberto não contamina o total do dia. |

```text
{ "flashcardId": "b7c1e2a0-4d3f-4a1b-9c8d-7e6f5a4b3c2d", "nota": "BOM", "tempoMs": 4200 }
```

#### `RespostaRevisao` — corpo de `200` no registro da revisão

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `flashcardId` | `string` (uuid) | sim | Card revisado. |
| `estado` | `string` | sim | Estado após a revisão (CA-08). |
| `estabilidade` | `number` | sim | Novo valor, duas casas. |
| `dificuldade` | `number` | sim | Novo valor, duas casas. |
| `intervaloMinutos` | `number` | sim | Intervalo agendado, já truncado pelo máximo do deck (CA-09). |
| `intervaloRotulo` | `string` | sim | O mesmo texto que a previsão da nota escolhida trazia (CA-03). |
| `proximaRevisaoEm` | `string` (ISO-8601) | sim | Instante do próximo vencimento, em UTC. |
| `reenfileirar` | `boolean` | sim | `true` quando o card volta ainda hoje, por passo de aprendizado ou reaprendizado. |
| `card` | `CardRevisao` | não | Preenchido apenas quando `reenfileirar` é `true`, com as previsões já recalculadas. `null` caso contrário. |

```text
{
  "flashcardId": "b7c1e2a0-4d3f-4a1b-9c8d-7e6f5a4b3c2d",
  "estado": "REVISAO", "estabilidade": 21.63, "dificuldade": 4.92,
  "intervaloMinutos": 12960, "intervaloRotulo": "9 dias",
  "proximaRevisaoEm": "2026-09-08T03:00:00Z",
  "reenfileirar": false, "card": null
}
```

> **Classificação *Errei* em card de revisão** (CA-08): o estado vai a `REAPRENDENDO`, a estabilidade cai pela fórmula de lapso e o card volta em dez minutos, dentro da mesma sessão.

```text
{
  "flashcardId": "b7c1e2a0-…", "estado": "REAPRENDENDO",
  "estabilidade": 2.71, "dificuldade": 6.14,
  "intervaloMinutos": 10, "intervaloRotulo": "10 min",
  "proximaRevisaoEm": "2026-08-30T13:12:11Z",
  "reenfileirar": true,
  "card": { "id": "b7c1e2a0-…", "estado": "REAPRENDENDO", "previsoes": [] }
}
```

#### `ResumoSessao` — corpo de `200` na finalização (RF-34)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `revisados` | `number` | sim | Cards distintos revisados na sessão — um card que passou por três passos conta uma vez. |
| `classificacoes` | `number` | sim | Total de classificações registradas, incluindo os passos. |
| `distribuicao` | `object` | sim | `errei`, `dificil`, `bom`, `facil`. |
| `tempoMs` | `number` | sim | Soma dos `tempoMs` da sessão. |
| `tempoRotulo` | `string` | sim | `"8 min 32 s"`. |
| `proximosVencimentos` | `FaixaVencimento[]` | sim | Três faixas fixas, sempre presentes, com `quantidade: 0` quando vazias. |

#### `FaixaVencimento` — uma linha de "Próximos vencimentos"

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `faixa` | `string` | sim | `AMANHA`, `PROXIMOS_7_DIAS` ou `DEPOIS_DE_7_DIAS`. |
| `rotulo` | `string` | sim | `"Amanhã"`, `"Nos próximos 7 dias"`, `"Depois de 7 dias"`. |
| `quantidade` | `number` | sim | Cards `ATIVO` do usuário com vencimento na faixa. |

```text
{
  "revisados": 24, "classificacoes": 29,
  "distribuicao": { "errei": 3, "dificil": 5, "bom": 12, "facil": 4 },
  "tempoMs": 512000, "tempoRotulo": "8 min 32 s",
  "proximosVencimentos": [
    { "faixa": "AMANHA",           "rotulo": "Amanhã",              "quantidade": 6 },
    { "faixa": "PROXIMOS_7_DIAS",  "rotulo": "Nos próximos 7 dias", "quantidade": 18 },
    { "faixa": "DEPOIS_DE_7_DIAS", "rotulo": "Depois de 7 dias",    "quantidade": 41 }
  ]
}
```

#### `DeckDetalhe` — um deck na tela de decks (RF-35)

Substitui o `DeckResumo` de F6a **acrescentando** campos; `id`, `nome`, `descricao` e `totalCards` permanecem com o mesmo nome e o mesmo significado, de modo que o seletor de deck do diálogo de criação continua funcionando sem alteração.

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | não | `null` na linha virtual "Sem deck". |
| `nome` | `string` | sim | Único por usuário, sem diferenciar maiúsculas. |
| `descricao` | `string` | não | `null` quando não informada. |
| `totalCards` | `number` | sim | Cards `ATIVO` no deck. |
| `vencendoHoje` | `number` | sim | Cards `ATIVO` com vencimento até hoje. |
| `novos` | `number` | sim | Cards `ATIVO` em estado `NOVO`. |
| `limites` | `LimitesDeck` | sim | Os quatro valores em vigor. |
| `limitesRotulo` | `string` | sim | `"10 novos/dia · 60 revisões/dia · máx. 180 d"`. |
| `retencaoRotulo` | `string` | sim | `"FSRS · retenção alvo 90%"`. |
| `editavel` | `boolean` | sim | `false` na linha virtual "Sem deck" — ela não se renomeia nem se exclui. |
| `aviso` | `AvisoDeck` | não | `null` quando a fila está sob controle (CA-18). |

#### `LimitesDeck` — a política de agendamento do deck (RF-36)

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `novosDia` | `number` | sim | 0 a 100. Padrão 10. |
| `revisoesDia` | `number` | sim | 5 a 400. Padrão 60. |
| `intervaloMaxDias` | `number` | sim | 1 a 3650. Padrão 180. |
| `retencaoAlvo` | `number` | sim | Um de `0.80`, `0.85`, `0.90`, `0.95`. Padrão `0.90`. Valor fora do conjunto é aproximado ao mais próximo. |

#### `AvisoDeck` — fila acumulada (CA-18)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `tipo` | `string` | sim | `FILA_ACUMULADA`. |
| `mensagem` | `string` | sim | Informativa, nunca punitiva, e sempre apontando a ação. |
| `vencidos` | `number` | sim | Tamanho da fila vencida que disparou o aviso. |
| `limiteDiario` | `number` | sim | `revisoesDia` do deck, para a tela mostrar a comparação. |

```text
{
  "id": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d",
  "nome": "Constitucional — controle",
  "descricao": "ADI, ADC, ADPF e súmulas vinculantes",
  "totalCards": 214, "vencendoHoje": 137, "novos": 12,
  "limites": { "novosDia": 10, "revisoesDia": 60, "intervaloMaxDias": 180, "retencaoAlvo": 0.9 },
  "limitesRotulo": "10 novos/dia · 60 revisões/dia · máx. 180 d",
  "retencaoRotulo": "FSRS · retenção alvo 90%",
  "editavel": true,
  "aviso": {
    "tipo": "FILA_ACUMULADA",
    "mensagem": "137 cards vencidos, mais que o dobro do limite diário deste deck. Ajuste os limites para retomar no seu ritmo.",
    "vencidos": 137, "limiteDiario": 60
  }
}
```

> **A linha virtual "Sem deck"** aparece com `id: null` e `editavel: false` sempre que o usuário tiver ao menos um card sem deck, e usa os limites padrão de RN-11. Sem ela, os cards realocados por CA-15 ficariam invisíveis na tela de decks e só alcançáveis pela sessão de todos os decks.

```text
{
  "id": null, "nome": "Sem deck", "descricao": null,
  "totalCards": 200, "vencendoHoje": 4, "novos": 200,
  "limites": { "novosDia": 10, "revisoesDia": 60, "intervaloMaxDias": 180, "retencaoAlvo": 0.9 },
  "limitesRotulo": "10 novos/dia · 60 revisões/dia · máx. 180 d",
  "retencaoRotulo": "FSRS · retenção alvo 90%",
  "editavel": false, "aviso": null
}
```

#### `RequisicaoDeck` — corpo de `POST` e `PUT /flashcards/decks/{id}` (RF-35, RF-36)

Amplia o `RequisicaoDeck` de F6a, que tinha apenas `nome`. Todos os campos de limite são opcionais; ausentes preservam o valor atual na edição e assumem o padrão na criação.

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nome` | `string` | sim | De 3 a 60 caracteres após recorte (CA-13); duplicado responde `409`. |
| `descricao` | `string` | não | Até 200 caracteres; `null` ou vazio limpa. |
| `novosDia` | `number` | não | Ajustado a [0, 100] ao salvar (CA-16). |
| `revisoesDia` | `number` | não | Ajustado a [5, 400] ao salvar (CA-16). |
| `intervaloMaxDias` | `number` | não | Ajustado a [1, 3650] ao salvar (CA-16). |
| `retencaoAlvo` | `number` | não | Aproximado ao valor permitido mais próximo. |

```text
{
  "nome": "Constitucional — controle",
  "descricao": "ADI, ADC, ADPF e súmulas vinculantes",
  "novosDia": 10, "revisoesDia": 500, "intervaloMaxDias": 180, "retencaoAlvo": 0.95
}
```

> **O ajuste é silencioso e observável.** `revisoesDia: 500` é salvo como `400` e a resposta devolve o deck **com os valores já ajustados** — a tela reescreve o campo com o que foi realmente gravado, e o estudante vê o limite aplicado sem precisar recarregar (CA-16). Nada é recusado por estar fora do intervalo; recusar transformaria uma preferência em erro de formulário.

#### `ResumoFlashcards` — corpo de `GET /flashcards/resumo`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `vencendoHoje` | `number` | sim | Alimenta o contador do item Flashcards no shell e o Início. |
| `proximos7Dias` | `number` | sim | Cards vencendo de amanhã até o sétimo dia. |
| `novosDisponiveis` | `number` | sim | Cards `ATIVO` em estado `NOVO`. |
| `porDeck` | `ResumoDeck[]` | sim | Uma linha por deck com cards, incluindo "Sem deck". |

```text
{
  "vencendoHoje": 141, "proximos7Dias": 63, "novosDisponiveis": 212,
  "porDeck": [
    { "deckId": "3a2b1c0d-…", "nome": "Constitucional — controle", "vencendoHoje": 137, "novos": 12 },
    { "deckId": null,          "nome": "Sem deck",                  "vencendoHoje": 4,   "novos": 200 }
  ]
}
```

#### `ErroApi` — envelope de erro (`application/problem+json`)

Estrutura definida em `prd-acesso-e-conta` (`code`, `message`, `fieldErrors`). Todos os códigos usam tipos de exceção já existentes; nenhum tipo novo nasce aqui e o `ManipuladorErrosGlobal` não muda.

| Código | HTTP | Significado |
| --- | --- | --- |
| `DADOS_INVALIDOS` | `422` | Nome de deck com menos de 3 caracteres (CA-13), `escopo` desconhecido, `escopo = DECK` sem `deckId`, nota inválida, `tempoMs` negativo. `fieldErrors` preenchido campo a campo. |
| `RECURSO_NAO_ENCONTRADO` | `404` | Deck, sessão ou card inexistente, excluído **ou de outro usuário** (RN-07, CA-20). Mensagem indistinguível nos três casos. |
| `NOME_EM_USO` | `409` | Deck com nome repetido para o mesmo usuário, na criação ou na renomeação (CA-13). |
| `SESSAO_FINALIZADA` | `409` | Revisão registrada em sessão já encerrada, ou finalização repetida. |
| `ACESSO_NEGADO` | `403` | Requisição sem autenticação válida. **Nunca** usado para recurso de terceiro. |

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Não foi possível salvar o deck: verifique os campos destacados.",
  "fieldErrors": { "nome": "O nome do deck deve ter ao menos 3 caracteres." }
}
```

```text
{ "code": "NOME_EM_USO", "message": "Já existe um deck com esse nome.", "fieldErrors": { "nome": "Já existe um deck com esse nome." } }
```

```text
{ "code": "RECURSO_NAO_ENCONTRADO", "message": "Deck não encontrado.", "fieldErrors": null }
```

> **CA-20 depende de a mensagem ser indistinguível.** Deck inexistente e deck de outro usuário respondem o mesmo `404` com o mesmo texto, porque o filtro por `usuario_id` está dentro do `where` da consulta, não em uma checagem posterior — não há diferença de caminho nem de tempo entre os dois casos.

#### Parâmetros fixos do FSRS-5 (RN-08)

Os pesos são **parâmetros do sistema, versionados junto ao código**, em `PesosFsrs`. São os pesos padrão publicados do FSRS-5, os mesmos que a implementação de referência do protótipo aprovado utiliza. Trocá-los exige atualizar o PRD-mãe (regra 7 do `AGENTS.md`).

| Parâmetro | Valor |
| --- | --- |
| `W` (19 pesos) | `0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046, 1.54575, 0.1192, 1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621` |
| `DECAY` | `−0.5` |
| `FACTOR` | `19 / 81` |
| `ESTABILIDADE_MIN` / `ESTABILIDADE_MAX` | `0.01` / `36500` |
| `DIFICULDADE_MIN` / `DIFICULDADE_MAX` | `1` / `10` |
| `PASSOS_APRENDIZADO` | `1 min`, `10 min` |
| `PASSOS_REAPRENDIZADO` | `10 min` |
| `TEMPO_MAX_POR_CARD` | `600000 ms` |

#### Fórmulas do agendador (RN-08)

| Situação | Estabilidade | Dificuldade |
| --- | --- | --- |
| Primeiro contato (`S = 0`) | `S = w[g−1]` | `D = clamp(w4 − e^(w5·(g−1)) + 1)` |
| Revisão no mesmo dia (`t = 0`), notas 2–4 | `S = clamp(S · e^(w17·(g−3+w18)))` | reversão à média |
| Revisão no mesmo dia (`t = 0`), nota *Errei* | `S = min(S_lapso, S · e^(w17·(g−3+w18)))` | reversão à média |
| Revisão em dia posterior, notas 2–4 | `S = S · (1 + e^w8 · (11−D) · S^(−w9) · (e^(w10·(1−R)) − 1) · h · b)`, com `h = w15` se *Difícil* e `b = w16` se *Fácil* | reversão à média |
| Revisão em dia posterior, nota *Errei* | `S_lapso = min(S, w11 · D^(−w12) · ((S+1)^w13 − 1) · e^(w14·(1−R)))` | reversão à média |

Onde `R = (1 + FACTOR · t / S)^DECAY` é a retrievability no momento da revisão, `t` são os dias decorridos e a reversão à média é `D' = clamp(w7 · D0(FACIL) + (1 − w7) · clamp(D − w6 · (g−3) · (10−D) / 9))`. O intervalo de longo prazo é `I = round(S / FACTOR · (retencaoAlvo^(1/DECAY) − 1))`, truncado em `[1, intervaloMaxDias]` (CA-09).

#### Máquina de estados dos passos (FSRS-5 com memória de curto prazo)

Estabilidade e dificuldade são recalculadas em **toda** classificação, inclusive nos passos; o que a tabela governa é apenas quando o card volta.

| Estado atual | Nota | Novo estado | Próximo vencimento |
| --- | --- | --- | --- |
| `NOVO` / `APRENDENDO` | Errei | `APRENDENDO`, passo 0 | 1 min |
| `NOVO` / `APRENDENDO` | Difícil | `APRENDENDO`, mesmo passo | passo atual |
| `NOVO` / `APRENDENDO` | Bom | `APRENDENDO`, passo + 1; gradua a `REVISAO` no último | passo seguinte, ou intervalo do FSRS |
| `NOVO` / `APRENDENDO` | Fácil | `REVISAO` | intervalo do FSRS |
| `REVISAO` | Errei | `REAPRENDENDO`, passo 0 | 10 min |
| `REVISAO` | Difícil / Bom / Fácil | `REVISAO` | intervalo do FSRS |
| `REAPRENDENDO` | Errei | `REAPRENDENDO`, passo 0 | 10 min |
| `REAPRENDENDO` | Difícil | `REAPRENDENDO`, mesmo passo | passo atual |
| `REAPRENDENDO` | Bom / Fácil | `REVISAO` | intervalo do FSRS |

#### Esquema de banco — `V9__flashcards_revisao.sql`

Migração versionada e imutável em `backend/src/main/resources/db/migration`. `V1__base_acesso.sql` criou `usuario`; `V2__banco_questoes.sql` criou `disciplina`, `assunto` e `questao`; a migração de F6a (prevista como `V8`) criou `deck`, `flashcard` e `flashcard_revisao`. **O número final é confirmado na entrega**: F2, F4 e F5 ainda não aplicaram suas versões, e esta migração assume a próxima livre, sem jamais editar as anteriores.

| Tabela | Alteração | Observações |
| --- | --- | --- |
| `flashcard_sessao` | criada | Uma linha por sessão de revisão. As contagens são consolidadas na finalização, com `check` de coerência com o total. |
| `flashcard` | `+ proxima_revisao_em`, `+ passo_aprendizado`, novo `default` em `proxima_revisao` | A coluna `date` continua existindo e continua sendo a que a listagem, os filtros e os indicadores de F6a consultam; o `check` de coerência garante que as duas nunca divirjam. |
| `flashcard_revisao` | `+ sessao_id`, `+ intervalo_minutos`, `+ segundos_decorridos` | Completa o log imutável exigido pelo PRD, agora com a resolução de minutos que os passos de curto prazo introduzem. |

```sql
create table flashcard_sessao (
  id            uuid primary key default gen_random_uuid(),
  usuario_id    uuid not null references usuario(id),
  escopo        text not null check (escopo in ('TODOS','DECK','SEM_DECK')),
  deck_id       uuid references deck(id) on delete set null,
  disciplina_id uuid references disciplina(id),
  iniciada_em   timestamptz not null default now(),
  finalizada_em timestamptz,
  revisados     smallint not null default 0,
  errei         smallint not null default 0,
  dificil       smallint not null default 0,
  bom           smallint not null default 0,
  facil         smallint not null default 0,
  tempo_ms      integer  not null default 0,
  constraint flashcard_sessao_escopo_coerente check (
    (escopo = 'DECK') = (deck_id is not null)),
  constraint flashcard_sessao_contagem check (
    revisados <= errei + dificil + bom + facil)
);
create index flashcard_sessao_idx on flashcard_sessao (usuario_id, iniciada_em desc);
create index flashcard_sessao_dia_idx on flashcard_sessao (usuario_id, finalizada_em)
  where finalizada_em is not null;

alter table flashcard
  alter column proxima_revisao set default (now() at time zone 'America/Sao_Paulo')::date,
  add column proxima_revisao_em timestamptz not null
      default date_trunc('day', now() at time zone 'America/Sao_Paulo')
              at time zone 'America/Sao_Paulo',
  add column passo_aprendizado smallint not null default 0
      check (passo_aprendizado between 0 and 10);

update flashcard
   set proxima_revisao_em = proxima_revisao::timestamp at time zone 'America/Sao_Paulo';

alter table flashcard add constraint flashcard_agenda_coerente check (
  proxima_revisao = (proxima_revisao_em at time zone 'America/Sao_Paulo')::date);

create index flashcard_fila_hora_idx on flashcard (usuario_id, proxima_revisao_em)
  where status = 'ATIVO';

alter table flashcard_revisao
  add column sessao_id           uuid references flashcard_sessao(id),
  add column intervalo_minutos   integer not null default 0,
  add column segundos_decorridos integer not null default 0;

create index flashcard_revisao_sessao_idx on flashcard_revisao (sessao_id)
  where sessao_id is not null;
create index flashcard_revisao_dia_idx on flashcard_revisao (usuario_id, revisado_em)
  include (flashcard_id, estabilidade_ant);
```

> **`flashcard_agenda_coerente` é o coração da compatibilidade com F6a.** A coluna `date` foi criada por uma migração imutável e é consultada pela listagem, pelos filtros de estado e pelos quatro indicadores da tela "Meus flashcards". Em vez de reescrever aquelas consultas, o esquema garante que `proxima_revisao` seja sempre o dia civil brasileiro de `proxima_revisao_em`. Quem escreve as duas colunas é um único método da entidade — `agendarPara(Instant)` —, e o `check` é a rede que pega qualquer caminho futuro que esqueça uma delas. A expressão é imutável (`timezone(text, timestamptz)` e `timestamp::date` são imutáveis no PostgreSQL), então é aceita em um `check`.

> **Por que a zona aparece no SQL.** O `default` de `proxima_revisao` foi ajustado junto porque `current_date` depende do `TimeZone` da conexão: com o banco em UTC, um card criado às 22h de Brasília nasceria com `proxima_revisao` de amanhã e `proxima_revisao_em` de hoje, violando o `check`. Fixar a zona nas duas expressões elimina a dependência de configuração de sessão. No Java, a mesma zona vem de `certame.estudo.zona` e é o único lugar que decide o que é "hoje".

> **`sessao_id` é anulável de propósito.** F6a garante que `flashcard_revisao` está vazia quando esta migração roda, mas a semente `V906__seed_flashcards_dev.sql` do perfil `dev` grava duas linhas — e ela é uma migração já aplicada, que não pode ser editada. Coluna anulável mantém a semente válida; nenhuma revisão criada pela API nasce sem sessão, e a consulta do resumo filtra por `sessao_id = ?`, de modo que a linha semeada simplesmente não aparece em resumo algum.

> **`flashcard_revisao_dia_idx` com `include`.** A consulta de consumo diário (RN-10) lê `flashcard_id` e `estabilidade_ant` de todas as revisões do usuário no dia. Com as duas colunas no `include`, a varredura é *index-only* e não toca o *heap* — importante porque essa consulta roda a cada abertura de sessão e cresce com o histórico, que nunca é apagado.

> **Semente de `dev`.** `db/migration-dev/V907__seed_revisao_dev.sql` cria duas sessões finalizadas para o estudante de exemplo — uma de ontem e uma de hoje — com as revisões correspondentes ligadas por `sessao_id`, além de deixar um deck com fila vencida acima do dobro do limite, para que CA-18 seja visível na tela sem dado montado à mão. Entra apenas no perfil `dev` e depende de `V906`. **O número da série `V900+` é confirmado na entrega.**

### Endpoints da API

Base `/api/v1`. Todas as rotas exigem autenticação; **nenhuma** aceita identificador de usuário como parâmetro — o dono é sempre o *subject* do token (RN-07, CA-20). Não existe rota administrativa de deck, de card ou de revisão em nenhum perfil, nem para super-admin.

#### Visão geral

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/flashcards/sessoes` | Abre a sessão de revisão e devolve a fila do dia (RF-33, RN-10). |
| `POST` | `/api/v1/flashcards/sessoes/{id}/revisoes` | Registra a classificação, aplica o FSRS e devolve o agendamento (RN-08). |
| `POST` | `/api/v1/flashcards/sessoes/{id}/finalizar` | Fecha a sessão e devolve o resumo (RF-34). |
| `GET` | `/api/v1/flashcards/resumo` | Vencendo hoje, próximos 7 dias e novos, por deck. |
| `GET` | `/api/v1/flashcards/decks` | **Enriquecido:** contadores, limites e aviso por deck (RF-35). |
| `PUT` | `/api/v1/flashcards/decks/{id}` | Renomeia, descreve e configura os limites (RF-35, RF-36). |
| `DELETE` | `/api/v1/flashcards/decks/{id}` | Exclui o deck movendo os cards para "Sem deck" (RN-11). |

---

#### `POST /api/v1/flashcards/sessoes`

Abre uma sessão de revisão e monta a fila do dia respeitando os limites de cada deck (RN-10).

**Corpo**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `escopo` | `string` | — | `TODOS`, `DECK` ou `SEM_DECK`. |
| `deckId` | `string` (uuid) | `null` | Obrigatório com `escopo = DECK`; deck de terceiro responde `404`. |
| `disciplinaId` | `string` (uuid) | `null` | Restringe a fila à disciplina. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `RespostaSessaoRevisao` | Sessão criada, com ou sem cards na fila. |
| `422` | `ErroApi` | `escopo` desconhecido ou `DECK` sem `deckId`. |
| `404` | `ErroApi` | Deck ou disciplina inexistente, ou deck de outro usuário. |
| `403` | `ErroApi` | Sem autenticação válida. |

**Exemplo — sucesso**

```http
POST /api/v1/flashcards/sessoes
```

Corpo e resposta conforme `RequisicaoSessaoRevisao` e `RespostaSessaoRevisao` em *Modelos de dados*.

**Exemplo — fila do dia terminada (CA-19)**

```http
POST /api/v1/flashcards/sessoes
```

```text
{ "id": "…", "iniciadaEm": "2026-08-30T13:02:11Z", "escopoRotulo": "Todos os decks", "total": 0, "fila": [] }
```

> O cliente distingue "fila vazia" de "erro" apenas por `total`. A tela mostra o estado vazio do protótipo — "Nada vencido por hoje" — e não oferece nenhuma ação de antecipar revisões, porque nenhuma existe no contrato.

**Exemplo — escopo inválido**

```http
POST /api/v1/flashcards/sessoes
```

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Não foi possível abrir a sessão: verifique os campos destacados.",
  "fieldErrors": { "deckId": "Informe o deck para estudar um deck específico." }
}
```

---

#### `POST /api/v1/flashcards/sessoes/{id}/revisoes`

Registra uma classificação, recalcula estabilidade e dificuldade pelo FSRS, agenda a próxima revisão e grava a linha imutável do log.

**Corpo**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `flashcardId` | `string` (uuid) | — | Card `ATIVO` do próprio usuário. |
| `nota` | `string` | — | `ERREI`, `DIFICIL`, `BOM`, `FACIL`. |
| `tempoMs` | `number` | — | 0 a 600000; acima disso é truncado ao máximo. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RespostaRevisao` | Revisão registrada e card reagendado. |
| `422` | `ErroApi` | Nota desconhecida ou `tempoMs` negativo. |
| `404` | `ErroApi` | Sessão ou card inexistente, arquivado, excluído ou de outro usuário. |
| `409` | `ErroApi` | Sessão já finalizada. |

**Exemplo — sucesso e reenfileiramento** — ver os dois blocos de `RespostaRevisao` em *Modelos de dados*.

**Exemplo — sessão já encerrada**

```http
POST /api/v1/flashcards/sessoes/5f4e3d2c-1b0a-4998-8776-6554433221100/revisoes
```

```text
{ "code": "SESSAO_FINALIZADA", "message": "Esta sessão de revisão já foi encerrada.", "fieldErrors": null }
```

> **Idempotência.** A rota não é idempotente por natureza — cada chamada é uma revisão distinta no log imutável, que é exatamente o que o PRD pede para permitir a otimização de pesos na v2. O cliente não repete a chamada em falha de rede: ele mostra o erro e mantém o card na fila, porque reenviar às cegas duplicaria uma linha de histórico e enviesaria a estabilidade do card.

---

#### `POST /api/v1/flashcards/sessoes/{id}/finalizar`

Fecha a sessão, consolida as contagens e o tempo e devolve o resumo de RF-34.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `ResumoSessao` | Sessão fechada. |
| `404` | `ErroApi` | Sessão inexistente ou de outro usuário. |
| `409` | `ErroApi` | Sessão já finalizada. |

**Exemplo — sucesso** — ver `ResumoSessao` em *Modelos de dados*.

**Exemplo — sessão sem nenhuma revisão**

```http
POST /api/v1/flashcards/sessoes/5f4e3d2c-…/finalizar
```

```text
{
  "revisados": 0, "classificacoes": 0,
  "distribuicao": { "errei": 0, "dificil": 0, "bom": 0, "facil": 0 },
  "tempoMs": 0, "tempoRotulo": "0 s",
  "proximosVencimentos": [
    { "faixa": "AMANHA", "rotulo": "Amanhã", "quantidade": 6 },
    { "faixa": "PROXIMOS_7_DIAS", "rotulo": "Nos próximos 7 dias", "quantidade": 18 },
    { "faixa": "DEPOIS_DE_7_DIAS", "rotulo": "Depois de 7 dias", "quantidade": 41 }
  ]
}
```

> Uma sessão com zero revisões contribui com `0` para o tempo de estudo do dia, e não com o tempo que a tela ficou aberta. O tempo somado a RF-06 é a soma dos `tempoMs` das classificações, não a diferença entre início e fim — abrir a fila e sair para o almoço não vira tempo de estudo.

---

#### `GET /api/v1/flashcards/resumo`

Contadores de vencimento do usuário, para o Início, o contador do menu e a tela de decks.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `ResumoFlashcards` | Sempre, mesmo sem nenhum card (todos os contadores em `0` e `porDeck: []`). |
| `403` | `ErroApi` | Sem autenticação válida. |

**Exemplo — sucesso** — ver `ResumoFlashcards` em *Modelos de dados*.

**Exemplo — usuário sem flashcards**

```http
GET /api/v1/flashcards/resumo
```

```text
{ "vencendoHoje": 0, "proximos7Dias": 0, "novosDisponiveis": 0, "porDeck": [] }
```

---

#### `GET /api/v1/flashcards/decks`

Rota criada em F6a; esta entrega **enriquece o corpo da resposta** sem alterar rota, verbo, parâmetros ou os quatro campos originais.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `DeckDetalhe[]` | Decks do usuário, ordenados por nome, mais a linha virtual "Sem deck" quando houver cards sem deck. |
| `403` | `ErroApi` | Sem autenticação válida. |

**Exemplo — sucesso** — ver `DeckDetalhe` e a variante "Sem deck" em *Modelos de dados*.

**Exemplo — usuário sem nenhum deck e sem cards**

```http
GET /api/v1/flashcards/decks
```

```text
[]
```

> **Compatibilidade com F6a.** O seletor de deck do diálogo de criação e o filtro da tela "Meus flashcards" leem `id`, `nome`, `descricao` e `totalCards`; os campos novos são ignorados por eles sem qualquer alteração de código. A linha virtual "Sem deck" chega com `id: null`, que aqueles componentes já tratam como a opção "Sem deck" que sempre existiu na lista.

---

#### `PUT /api/v1/flashcards/decks/{id}`

Renomeia, descreve e configura os limites de agendamento do deck.

**Corpo** — `RequisicaoDeck`, conforme *Modelos de dados*.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `DeckDetalhe` | Deck atualizado, com os limites **já ajustados** (CA-16). |
| `422` | `ErroApi` | Nome com menos de 3 caracteres ou acima de 60 (CA-13). |
| `409` | `ErroApi` | Nome já usado por outro deck do mesmo usuário (CA-13). |
| `404` | `ErroApi` | Deck inexistente ou de outro usuário. |

**Exemplo — ajuste de limite fora do intervalo (CA-16)**

```http
PUT /api/v1/flashcards/decks/3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d
```

```text
{ "nome": "Constitucional — controle", "revisoesDia": 500 }
```

```text
{
  "id": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d",
  "nome": "Constitucional — controle",
  "limites": { "novosDia": 10, "revisoesDia": 400, "intervaloMaxDias": 180, "retencaoAlvo": 0.9 },
  "limitesRotulo": "10 novos/dia · 400 revisões/dia · máx. 180 d"
}
```

**Exemplo — nome já usado (CA-13)**

```http
PUT /api/v1/flashcards/decks/3a2b1c0d-…
```

```text
{ "code": "NOME_EM_USO", "message": "Já existe um deck com esse nome.", "fieldErrors": { "nome": "Já existe um deck com esse nome." } }
```

> **Nada em `flashcard` nem em `flashcard_revisao` é tocado por esta rota** (CA-14, CA-17). Renomear não escreve nos cards porque o vínculo é por `deck_id`, não por nome; mudar a retenção não reescreve estabilidade, dificuldade nem intervalo já calculado — a nova política só é lida na próxima montagem de fila e no próximo cálculo de agendamento. A transação desta rota escreve exatamente uma linha, na tabela `deck`.

---

#### `DELETE /api/v1/flashcards/decks/{id}`

Exclui o deck e move seus cards para "Sem deck" na mesma transação (RN-11, CA-15).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `{ "cardsMovidos": number }` | Deck excluído; nenhum card apagado. |
| `404` | `ErroApi` | Deck inexistente ou de outro usuário. |

**Exemplo — sucesso**

```http
DELETE /api/v1/flashcards/decks/3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d
```

```text
{ "cardsMovidos": 200 }
```

> **A contagem devolvida não é enfeite.** É o número que a tela mostra na confirmação ("200 cards foram para Sem deck") e é o mesmo número que o teste de CA-15 compara com a contagem de cards antes da exclusão. Devolver zero quando havia cards é uma falha detectável, e não um silêncio.

---

## Pontos de integração

Nenhuma integração externa: sem rede a terceiros, sem timeout, sem retentativa e sem idempotência de integração a especificar. As integrações são internas.

| Integração | Direção | Contrato |
| --- | --- | --- |
| F6a → F6b (banco) | Esta entrega escreve no bloco FSRS de `flashcard` e em `flashcard_revisao`, tabelas criadas por F6a. | Alteração puramente aditiva: três colunas em `flashcard` e três em `flashcard_revisao`. Nenhuma consulta de F6a muda, garantido pelo `check` `flashcard_agenda_coerente` e coberto por TI-01 e TI-02. |
| F6a → F6b (fila) | `ConsultaFilaFlashcards.elegiveis(usuarioId, ate)` é a porta de leitura que F6a criou para esta entrega. | `MontadorFila` a consome e acrescenta o escopo, os limites por deck e o desconto do consumo diário. A porta continua com a mesma assinatura; os testes TI-19 e TI-20 de F6a permanecem válidos. |
| F6b → F6a (UI) | `GET /flashcards/decks` passa a devolver `DeckDetalhe`. | Superconjunto de `DeckResumo`; o `DeckSeletor` e o filtro de F6a não mudam. Registrado em *Endpoints*. |
| F6b → `prd-sessao-de-estudo` / Início (RF-06) | O tempo das sessões de flashcards entra no resumo do dia (CA-11). | Porta `TempoEstudoFlashcards.segundosNoDia(usuarioId, dia)`, implementada aqui e coberta por TI-27. **Enquanto a tela de Início de F2 não existir, a porta fica pronta e sem consumidor**; a TechSpec de F2 a soma ao tempo de `sessao_estudo` em vez de recalcular. Nenhuma tabela de F2 é lida ou escrita aqui. |
| F6b → `catalogo` | O filtro por disciplina da sessão valida a disciplina informada. | Por serviço de `catalogo`, nunca pelo repositório de outro domínio (`code-standards.md` §3). |
| F6b → `prd-gestao-de-usuarios` | Exclusão de conta. | Nenhuma alteração: `flashcard_sessao` e `flashcard_revisao` referenciam `usuario(id)` sem cascata, e a exclusão de conta é lógica. |

## Abordagem de testes

Meta de cobertura: **mínimo de 80%**, conforme `.agents/rules/tests.md` §1, que prevalece sobre o texto de 70% da `TECH-SPEC.md` §9. Ferramentas: JUnit Jupiter, AssertJ, Mockito e Testcontainers 2.0.5 (versões gerenciadas pelo BOM do Spring Boot 4.1.1); Vitest e Playwright no frontend. Proporção alvo da pirâmide: ~70% unidade, ~20% integração, ~10% E2E.

Pela tabela de criticidade de `tests.md` §1, esta funcionalidade concentra **dois** dos itens de prioridade alta do projeto: `flashcard/fsrs` e a fila com os limites do deck (RN-10/RN-11). O agendador recebe caminho feliz, todas as bordas e uma tabela de casos de referência, porque um erro de sinal em uma fórmula não quebra nada visivelmente — só agenda mal, por meses, sem sintoma. A exclusão de deck (CA-15) recebe o mesmo rigor por ser a única operação do documento capaz de perder conteúdo. Renomear e descrever deck são CRUD e recebem o essencial.

Todo teste com data usa `Clock` fixo injetado em `RelogioEstudo`: a fila, os limites diários e o vencimento são funções do dia civil, e um teste que rodasse na virada do dia falharia sozinho (`tests.md` §2, *Repeatable*). Nenhum teste de unidade toca banco, rede ou relógio do sistema.

### Testes de unidade

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `CurvaEsquecimento` devolve retenção 1 com zero dias decorridos | RN-08 | `retrievability(S, 0) == 1.0` para qualquer `S > 0`. |
| TU-02 | `CurvaEsquecimento` decresce monotonicamente com os dias | RN-08 | `R(S, 1) > R(S, 7) > R(S, 30)`, todos em (0, 1). |
| TU-03 | `CurvaEsquecimento` fecha o ciclo intervalo → retenção | CA-07 | `retrievability(S, intervaloParaRetencao(S, 0.9))` volta a `0.9` com tolerância de 1e-6. |
| TU-04 | `CurvaEsquecimento` encurta o intervalo quando a retenção alvo sobe | CA-17 | `intervaloParaRetencao(S, 0.95) < intervaloParaRetencao(S, 0.90) < intervaloParaRetencao(S, 0.80)`. |
| TU-05 | `CalculoDificuldade.inicial` cobre as quatro notas dentro da faixa | RN-08 | Valores em [1, 10], decrescentes de *Errei* para *Fácil*. |
| TU-06 | `CalculoDificuldade.proxima` aplica reversão à média em D0(Fácil) | RN-08 | *Bom* em card com D alta reduz D; *Errei* aumenta; nenhuma passa da faixa. |
| TU-07 | `CalculoDificuldade` satura em 1 e em 10 | RN-08 | Dez *Fácil* seguidos param em 1; dez *Errei* param em 10. |
| TU-08 | `CalculoEstabilidade.inicial` usa o peso da nota | RN-08 | `inicial(BOM) == W[2]`; as quatro notas devolvem `W[g−1]`. |
| TU-09 | `CalculoEstabilidade.aposAcerto` cresce mais com retenção baixa | RN-08 | Mesmo `S` e `D`, `R = 0.7` produz estabilidade maior que `R = 0.95`. |
| TU-10 | `CalculoEstabilidade.aposLapso` nunca supera a estabilidade anterior | CA-08 | Para vinte combinações de `S`, `D` e `R`, `aposLapso <= S`. |
| TU-11 | `CalculoEstabilidade.curtoPrazo` aplica a fórmula de mesmo dia | RN-08 | *Fácil* aumenta `S`, *Errei* reduz, resultado saturado em [0.01, 36500]. |
| TU-12 | `PassosAprendizado` leva card novo com *Bom* ao segundo passo | RN-08 | `APRENDENDO`, passo 1, atraso de 10 minutos. |
| TU-13 | `PassosAprendizado` gradua com *Bom* no último passo | RN-08 | Estado `REVISAO` e atraso resolvido pelo intervalo do FSRS, não pelos passos. |
| TU-14 | `PassosAprendizado` gradua imediatamente com *Fácil* | RN-08 | Card em `APRENDENDO` passo 0 vai a `REVISAO` sem percorrer o passo restante. |
| TU-15 | `AgendadorFsrs` recalcula memória e agenda com *Bom* | CA-07 | Estabilidade e dificuldade novas e intervalo igual ao tempo em que a retenção atinge o alvo do deck. |
| TU-16 | `AgendadorFsrs` com *Errei* em card de revisão | CA-08 | Estabilidade reduzida pela fórmula de lapso e estado `REAPRENDENDO`. |
| TU-17 | `AgendadorFsrs` trunca o intervalo no máximo do deck | CA-09 | Card com estabilidade alta e `intervaloMaxDias = 30` agenda exatamente 30 dias. |
| TU-18 | `AgendadorFsrs` respeita retenções alvo diferentes no mesmo card | CA-17 | Mesmo `EstadoMemoria` com alvo 0.90 e 0.95 produz intervalos diferentes e memória idêntica. |
| TU-19 | `AgendadorFsrs.previsoes` coincide com `proximo` nas quatro notas | CA-03 | Para cada nota, `previsoes()[nota].minutos == proximo(nota).intervaloMinutos`. |
| TU-20 | `AgendadorFsrs` não muta o `EstadoMemoria` recebido | RN-08 | O *record* de entrada permanece idêntico após vinte chamadas — o agendador é puro. |
| TU-21 | `AgendadorFsrs` reproduz a tabela de casos de referência do FSRS-5 | RN-08 | Sequência conhecida de notas produz a série esperada de estabilidade, dificuldade e intervalo, com tolerância de 1e-4. |
| TU-22 | `LimitesDeck.normalizar` ajusta 500 revisões por dia para 400 | CA-16 | `revisoesDia == 400`; os demais campos preservados. |
| TU-23 | `LimitesDeck.normalizar` ajusta cada campo fora do intervalo | CA-16 | `novosDia = 300 → 100`, `intervaloMaxDias = 0 → 1`, `retencaoAlvo = 0.99 → 0.95`. |
| TU-24 | `LimitesDeck.PADRAO` traz os limites de "Sem deck" | CA-15 | 10 novos/dia, 60 revisões/dia, máximo de 180 dias, retenção 0,90. |
| TU-25 | `DeckService.criar` recusa nome com menos de 3 caracteres | CA-13 | Lança `DadosInvalidosException` com `fieldErrors.nome`; nada persistido. |
| TU-26 | `DeckService.atualizar` recusa nome já usado por outro deck | CA-13 | Lança `ConflitoException` `NOME_EM_USO`; o nome anterior permanece. |
| TU-27 | `DeckService.atualizar` aceita o próprio nome sem alteração | CA-13 | Salvar sem trocar o nome não dispara conflito consigo mesmo. |
| TU-28 | `DeckService.atualizar` não escreve em nenhum flashcard | CA-14, CA-17 | Nenhuma interação com `FlashcardRepository`, verificada por `verifyNoInteractions`. |
| TU-29 | `DeckService.excluir` move os cards e não apaga nenhum | CA-15 | Todos os cards recebem `deck_id null`; nenhuma chamada de exclusão de card; devolve a contagem movida. |
| TU-30 | `DeckService.excluir` recusa deck de outro usuário | CA-20 | Lança `RecursoNaoEncontradoException`; nenhum card tocado. |
| TU-31 | `MontadorFila` respeita novos e revisões por dia de cada deck | CA-06 | Deck com 10/60 e acervo de 40 novos e 200 vencidos devolve 10 novos e 60 revisões. |
| TU-32 | `MontadorFila` desconta o consumo já registrado no dia | CA-06 | Com 30 revisões feitas hoje no deck, a fila traz no máximo 30. |
| TU-33 | `MontadorFila` conta card distinto, não classificação | CA-06 | Card revisado três vezes hoje por passos de aprendizado consome uma vaga. |
| TU-34 | `MontadorFila` aplica limites deck a deck, sem misturar | CA-06 | Dois decks com limites diferentes recebem cortes independentes. |
| TU-35 | `MontadorFila` usa os limites padrão para cards sem deck | CA-15 | Cards com `deck_id null` são cortados em 10 novos e 60 revisões. |
| TU-36 | `MontadorFila` ordena por vencimento crescente | RF-33 | O card mais atrasado vem primeiro; empate resolvido por `criado_em`. |
| TU-37 | `MontadorFila` não antecipa cards de vencimento futuro | CA-19 | Acervo só com vencimento de amanhã devolve fila vazia. |
| TU-38 | `MontadorFila` ignora cards arquivados e excluídos | RN-07 | Apenas `status = ATIVO` entra na fila. |
| TU-39 | `MontadorFila` restringe por escopo de deck e por disciplina | US9 | `escopo = DECK` traz só aquele deck; `SEM_DECK` só os sem deck; a disciplina combina com ambos. |
| TU-40 | `RegistroRevisaoService` grava o log completo da revisão | Restrição do PRD | Uma linha com nota, dias decorridos, estabilidade e dificuldade antes e depois, intervalo e tempo. |
| TU-41 | `RegistroRevisaoService` atualiza a memória e as duas colunas de agenda | CA-07 | `proxima_revisao_em` e `proxima_revisao` gravados juntos e coerentes. |
| TU-42 | `RegistroRevisaoService` marca reenfileiramento em passo intradiário | RN-08 | *Errei* em card de revisão devolve `reenfileirar: true` e o card com previsões novas. |
| TU-43 | `RegistroRevisaoService` trunca `tempoMs` acima do máximo | RF-34 | 900000 ms é gravado como 600000. |
| TU-44 | `RegistroRevisaoService` recusa card de outro usuário | CA-20 | Lança `RecursoNaoEncontradoException`; nenhuma linha de log criada. |
| TU-45 | `RegistroRevisaoService` recusa revisão em sessão finalizada | RF-34 | Lança `ConflitoException` `SESSAO_FINALIZADA`. |
| TU-46 | `SessaoRevisaoService.finalizar` consolida contagens e tempo | CA-10 | Distribuição por nota, cards distintos revisados e soma dos tempos. |
| TU-47 | `SessaoRevisaoService.finalizar` recusa segunda finalização | RF-34 | Lança `ConflitoException`; as contagens da primeira permanecem. |
| TU-48 | `ResumoRevisaoService` monta as três faixas de vencimento | CA-10 | Faixas sempre presentes, com `0` quando vazias e sem sobreposição de contagem. |
| TU-49 | `PainelDecksService` calcula total, vencendo hoje e novos | CA-12 | Contagens conferem com o acervo semeado; arquivados e excluídos fora. |
| TU-50 | `PainelDecksService` emite aviso acima do dobro do limite | CA-18 | 137 vencidos com limite 60 produzem `AvisoDeck` com `vencidos` e `limiteDiario`. |
| TU-51 | `PainelDecksService` não emite aviso no dobro exato | CA-18 | 120 vencidos com limite 60 devolvem `aviso: null` — o critério é "acima do dobro". |
| TU-52 | `PainelDecksService` inclui "Sem deck" apenas quando há cards | CA-15 | Linha virtual com `id: null` e `editavel: false`; ausente quando não há card sem deck. |
| TU-53 | `RelogioEstudo` resolve o dia na zona do estudo | RN-10 | Às 23h de Brasília, `hoje()` é o dia local, não o dia UTC. |
| TU-54 | `TempoEstudoFlashcards` soma só sessões finalizadas do dia | CA-11 | Sessão aberta e sessão de ontem ficam de fora. |
| TU-55 | `RevisaoStore` inicia cada card sem o verso revelado | CA-01 | `revelado()` é `false` ao abrir e a cada avanço de card. |
| TU-56 | `RevisaoStore.revelar` expõe verso e classificações | CA-02 | `revelado()` vira `true`; `cardAtual()` inalterado. |
| TU-57 | `RevisaoStore.classificar` avança para o próximo card | CA-05 | Índice incrementa, `revelado()` volta a `false`, contagem da nota sobe. |
| TU-58 | `RevisaoStore` reenfileira o card quando o servidor manda | RN-08 | Card volta à fila na posição ordenada por vencimento, com as previsões novas. |
| TU-59 | `RevisaoStore` sinaliza fila vazia sem oferecer antecipação | CA-19 | `filaVazia()` é `true` e não há ação de estudar à frente. |
| TU-60 | `RevisaoStore.finalizar` expõe o resumo devolvido | CA-10 | `resumo()` traz revisados, distribuição, tempo e próximos vencimentos. |
| TU-61 | `AtalhosRevisao` mapeia espaço e as teclas 1 a 4 | CA-02, CA-05 | Espaço revela; 1–4 classificam em Errei, Difícil, Bom e Fácil, nessa ordem. |
| TU-62 | `AtalhosRevisao` ignora 1–4 com o verso oculto | CA-01 | Nenhuma classificação é registrada antes de revelar. |
| TU-63 | `AtalhosRevisao` ignora teclas vindas de campo de texto | UX | Evento com alvo `input`, `textarea` ou `select` não dispara atalho. |
| TU-64 | `IntervaloPipe` formata minutos, dias e "Amanhã" | CA-03 | `1 → "1 min"`, `1440 → "1 dia"`, `12960 → "9 dias"`. |
| TU-65 | `DecksStore.salvar` mapeia 409 para o campo nome | CA-13 | `erroDeCampo().nome` preenchido; nenhum toast genérico. |
| TU-66 | `DecksStore.salvar` reflete os limites ajustados pelo servidor | CA-16 | Enviado 500, o formulário passa a exibir 400 sem recarregar a tela. |
| TU-67 | `CronometroSessao` acumula e pausa em `document.hidden` | CA-11 | Com temporizador falso, o tempo não avança enquanto a aba está oculta. |

**Estratégia.** O alvo principal é `flashcard/fsrs`, testado como objeto puro: sem Spring, sem banco, sem relógio real. TU-21 é o teste que sustenta RN-08 — uma tabela de sequências conhecidas de notas com os valores esperados de estabilidade, dificuldade e intervalo, escrita a partir da implementação de referência do protótipo aprovado e conferida contra os pesos publicados. É o teste que fica vermelho se alguém trocar um sinal ou um índice de peso, exatamente o defeito que `tests.md` §2 chama de invisível. `MontadorFila` é o segundo alvo, com repositórios mockados à mão e acervos montados no próprio teste. Mocks só na fronteira: repositórios e `Clock`; o agendador nunca é mockado, porque é ele que está sob teste.

### Testes de integração

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | Migração preenche `proxima_revisao_em` coerente com `proxima_revisao` | RN-08 | Cards existentes recebem meia-noite de Brasília do mesmo dia civil. |
| TI-02 | `check` `flashcard_agenda_coerente` recusa divergência | RN-08 | `update` que muda só uma das colunas falha com violação de *constraint*. |
| TI-03 | `check` de escopo de `flashcard_sessao` | RF-33 | `escopo = DECK` sem `deck_id` é recusado pelo banco. |
| TI-04 | Exclusão lógica de card preserva o log de revisões | Restrição do PRD | Após excluir o card, as linhas de `flashcard_revisao` continuam íntegras. |
| TI-05 | `POST /sessoes` monta a fila respeitando os limites do deck | CA-06 | Deck 10/60 com acervo grande devolve exatamente 10 novos e 60 revisões. |
| TI-06 | `POST /sessoes` devolve as quatro previsões por card | CA-03 | Cada card traz `previsoes` com as quatro notas e rótulos em português. |
| TI-07 | `POST /sessoes` devolve o bloco de memória do card | CA-04 | Estabilidade, dificuldade e retenção atual presentes; `null` em card novo. |
| TI-08 | `POST /sessoes` com fila do dia terminada | CA-19 | `201` com `total: 0` e `fila: []`; nenhum card de vencimento futuro. |
| TI-09 | `POST /sessoes` de um usuário não alcança cards de outro | CA-20 | Fila do usuário A não contém nenhum card do usuário B, mesmo com o mesmo nome de deck. |
| TI-10 | `POST /sessoes` com `escopo = DECK` de deck de terceiro | CA-20 | `404` com a mesma mensagem de deck inexistente. |
| TI-11 | `POST /revisoes` com *Bom* recalcula memória e agenda | CA-07 | Estabilidade e dificuldade novas em `flashcard`, `proxima_revisao_em` no ponto da retenção alvo. |
| TI-12 | `POST /revisoes` com *Errei* aplica lapso e reaprendizado | CA-08 | Estabilidade menor que a anterior, `estado = REAPRENDENDO`, `reenfileirar: true`. |
| TI-13 | `POST /revisoes` trunca no intervalo máximo do deck | CA-09 | Deck com máximo de 30 dias grava exatamente 30 dias. |
| TI-14 | `POST /revisoes` grava a linha completa do log | Restrição do PRD | Uma linha com `sessao_id`, nota, dias decorridos, memória antes e depois, intervalo e tempo. |
| TI-15 | `POST /revisoes` em card de outro usuário | CA-20 | `404`; nenhuma linha criada e nenhuma coluna do card alterada. |
| TI-16 | `POST /revisoes` em sessão já finalizada | RF-34 | `409` `SESSAO_FINALIZADA`. |
| TI-17 | `POST /finalizar` devolve o resumo completo | CA-10 | Revisados, distribuição, tempo total e as três faixas de vencimento. |
| TI-18 | Segunda sessão no mesmo dia respeita o consumo anterior | CA-06 | Após 60 revisões, a nova sessão do mesmo deck devolve fila vazia de revisões. |
| TI-19 | `GET /decks` devolve o painel completo | CA-12 | Nome, descrição, total, vencendo hoje, novos, limites e ação por deck. |
| TI-20 | `GET /decks` inclui a linha virtual "Sem deck" | CA-15 | `id: null`, `editavel: false` e os limites padrão. |
| TI-21 | `GET /decks` emite o aviso de fila acumulada | CA-18 | Deck com fila acima do dobro do limite traz `aviso` preenchido; os demais, `null`. |
| TI-22 | `PUT /decks/{id}` recusa nome curto e nome duplicado | CA-13 | `422` com `fieldErrors.nome` e `409` `NOME_EM_USO`. |
| TI-23 | `PUT /decks/{id}` renomeia mantendo cards e agendamento | CA-14 | Todos os cards seguem no deck; `proxima_revisao_em` e a memória inalteradas. |
| TI-24 | `PUT /decks/{id}` ajusta valores fora do intervalo | CA-16 | 500 revisões viram 400 no banco e na resposta. |
| TI-25 | `PUT /decks/{id}` muda a retenção sem tocar o histórico | CA-17 | `flashcard_revisao` e o bloco de memória intactos; a próxima fila usa o novo alvo. |
| TI-26 | `DELETE /decks/{id}` move os cards e preserva todos | CA-15 | Contagem de cards do usuário idêntica antes e depois; todos com `deck_id null`. |
| TI-27 | `DELETE /decks/{id}` é atômico | RN-11 | Falha forçada na movimentação deixa o deck e os cards exatamente como estavam. |
| TI-28 | `GET /resumo` devolve vencendo hoje e próximos 7 dias | RF-34 | Contagens conferem com o acervo, incluindo a linha de "Sem deck". |
| TI-29 | `TempoEstudoFlashcards` soma as sessões finalizadas do dia | CA-11 | Duas sessões finalizadas somam; a aberta e a de ontem, não. |
| TI-30 | Rotas sem autenticação | CA-20 | As sete rotas respondem `401`/`403`, nenhuma expõe corpo de domínio. |

**Componentes testados em conjunto.** `@DataJpaTest` com Testcontainers cobre TI-01…TI-04 — os `check`, a migração e a ausência de cascata não têm equivalente em banco em memória. `@WebMvcTest` com `MockMvcTester` cobre o contrato, os códigos de erro e a privacidade (TI-09, TI-10, TI-15, TI-30). `@SpringBootTest` completo fica para os fluxos que atravessam escrita e leitura e nos quais o valor está na travessia: TI-05, TI-11…TI-13, TI-18, TI-23, TI-25…TI-27 e TI-29. Dados isolados por execução, com e-mail e nome de deck derivados de `UUID.randomUUID()`; `Clock` fixo injetado por `@TestConfiguration` para que a fila e o consumo diário sejam determinísticos.

### Testes E2E

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | Abrir a sessão mostra só a frente | CA-01 | Verso e classificações ausentes do DOM até revelar. |
| E2E-02 | Barra de espaço revela o verso e as classificações | CA-02 | Verso visível e os quatro botões presentes; o clique no card faz o mesmo. |
| E2E-03 | Cada classificação exibe o intervalo que agendaria | CA-03 | Os quatro botões trazem rótulo de intervalo, e o intervalo do botão escolhido é o gravado. |
| E2E-04 | O verso mostra estabilidade, dificuldade e retenção | CA-04 | A linha de memória aparece rotulada, em português e sem jargão. |
| E2E-05 | Teclas 1 a 4 classificam e avançam | CA-05 | Cada tecla registra a nota correspondente e mostra o card seguinte. |
| E2E-06 | Resumo ao fim da sessão | CA-10 | Cards revisados, distribuição, tempo total e próximos vencimentos. |
| E2E-07 | O tempo da sessão entra no total do dia | CA-11 | Após finalizar, o Início mostra o tempo de estudo acrescido. |
| E2E-08 | A tela de decks mostra tudo que RF-35 pede | CA-12 | Nome, descrição, total, vencendo hoje, novos, limites e "Estudar este deck". |
| E2E-09 | Criar deck com nome curto ou repetido | CA-13 | Mensagem clara no campo nome, diálogo permanece aberto. |
| E2E-10 | Renomear deck preserva os cards | CA-14 | Contagem do deck inalterada e nenhum card muda de vencimento. |
| E2E-11 | Excluir deck envia os cards para "Sem deck" | CA-15 | Confirmação explícita, nenhum card apagado, a linha "Sem deck" ganha os cards. |
| E2E-12 | Configurar 500 revisões por dia | CA-16 | O campo passa a exibir 400 após salvar. |
| E2E-13 | Aviso de fila acumulada na tela de decks | CA-18 | Deck com fila acima do dobro do limite exibe o aviso com a ação de ajustar limites. |
| E2E-14 | Estudar um deck específico | US9, CA-06 | A sessão só traz cards daquele deck, dentro dos limites dele. |
| E2E-15 | Fila do dia terminada | CA-19 | Estado vazio "Nada vencido por hoje", sem ação de antecipar revisões. |
| E2E-16 | Sessão inteira operável por teclado, com foco visível | UX do PRD | Do início ao resumo sem mouse; foco visível em todos os alvos e contraste de 4,5:1. |

**Como testar.** Playwright sobre o backend real com o perfil `dev` e as sementes `V906`/`V907`, que já trazem decks com fila vencida, cards novos e cards em revisão — o acervo necessário para CA-06, CA-18 e CA-19 sem montagem manual. Um cenário por fluxo, conforme `tests.md` §4. E2E-07 é o único que cruza funcionalidades: depende da tela de Início de `prd-sessao-de-estudo`; enquanto ela não existir, o critério é verificado por TI-29 sobre a porta `TempoEstudoFlashcards`, e o E2E fica marcado como pendente da entrega de F2.

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Design: fechar as três lacunas do protótipo** — o protótipo já traz a tela de decks, o diálogo de configuração e a sessão de revisão inteira, mas falta (a) o aviso de fila acumulada no card de deck (CA-18), (b) a linha virtual "Sem deck" na grade e (c) o rótulo de intervalo em minutos nos botões de classificação, que a escolha por FSRS-5 com memória de curto prazo introduz. Primeiro porque a regra 1 do `AGENTS.md` impede que qualquer marcação nasça direto no Angular.
2. **Migração e persistência** — `V9__flashcards_revisao.sql`, a semente `V907` de `dev`, a entidade `SessaoRevisao`, a entidade `FlashcardRevisao` e as três colunas novas em `Flashcard`, com TI-01…TI-04. Antes de qualquer regra, porque a coluna `proxima_revisao_em` é pressuposto do agendador de curto prazo.
3. **O agendador FSRS** — `PesosFsrs`, `CurvaEsquecimento`, `CalculoDificuldade`, `CalculoEstabilidade`, `PassosAprendizado` e `AgendadorFsrs`, com TU-01…TU-21. É a peça mais crítica e a mais isolada: nenhuma dependência de Spring, de banco ou de relógio, e a tabela de casos de referência de TU-21 nasce junto. Sem ela pronta e verde, nada adiante faz sentido.
4. **Relógio e limites** — `RelogioEstudo` e `LimitesDeck`, com TU-22…TU-24 e TU-53. Duas regras puras das quais a fila e o deck dependem.
5. **Deck completo** — `Deck` com os *setters* novos, `DeckService.atualizar` e `excluir`, `PainelDecksService`, com TU-25…TU-30 e TU-49…TU-52. A exclusão é a operação de maior risco do documento e é resolvida cedo.
6. **Montagem da fila** — `MontadorFila`, `ConsumoDiario` e a consulta agregada, com TU-31…TU-39. Depende do relógio, dos limites e da porta `ConsultaFilaFlashcards` de F6a.
7. **Sessão e revisão** — `SessaoRevisaoService`, `RegistroRevisaoService`, `ResumoRevisaoService` e `TempoEstudoFlashcards`, com TU-40…TU-48 e TU-54. É onde o agendador encontra a persistência.
8. **Controllers** — `RevisaoController` e os métodos novos de `DeckController`, com TI-05…TI-30. É aqui que a privacidade sai do serviço e vira contrato observável.
9. **Cliente HTTP e modelos** — `revisao-api.ts`, `modelos-revisao.ts` e a ampliação de `flashcard-api.ts`. Sem tela ainda; os contratos primeiro.
10. **Tela de decks** — `DecksPage`, `DeckCard`, `DeckDialog` e `DecksStore`, com TU-65 e TU-66, em tradução 1:1 do protótipo aprovado no passo 1.
11. **Sessão de revisão** — `SessaoRevisaoPage`, `CardRevisao`, `NotasRevisao`, `ResumoRevisao`, `FilaVazia`, `RevisaoStore`, `AtalhosRevisao`, `CronometroSessao` e `IntervaloPipe`, com TU-55…TU-64 e TU-67. A tela mais exercitada da funcionalidade vem depois da tela mais rara, porque depende do `IntervaloPipe` e do store que o passo 10 não usa.
12. **Rotas, shell e resumo** — `/flashcards/decks` e `/flashcards/estudar` sob `authGuard`, o contador do item Flashcards alimentado por `GET /flashcards/resumo`.
13. **E2E, endurecimento e documentos** — E2E-01…E2E-16, verificação da cobertura por `./mvnw verify` e `npm run test:coverage`, e atualização da `TECH-SPEC.md`: §5 com `flashcard_sessao` e as seis colunas novas, e §6 com `PUT` e `DELETE /flashcards/decks/{id}` e o corpo enriquecido de `GET /flashcards/decks`. Regra 4 do `AGENTS.md`; entra na mesma entrega, não depois.

### Dependências técnicas

| Dependência | Situação | Impacto |
| --- | --- | --- |
| `prd-flashcards-criacao-e-gerenciamento` (F6a) — `deck`, `flashcard`, `flashcard_revisao`, `VerificadorDonoFlashcard`, `ConsultaFilaFlashcards`, `shared/ui/dialog`, `shared/ui/toast`, `flashcard-api.ts` | Especificado, ainda não implementado | **Bloqueia tudo a partir do passo 2.** Esta entrega não cria nenhuma dessas peças; ela as estende. É a dependência mais forte do documento e a mais previsível, porque a TechSpec de F6a já fixa cada assinatura. |
| `prd-acesso-e-conta` (`SegurancaConfig`, `authGuard`, shell, `styles.css`, interceptor) | **Implementado** | Nenhum bloqueio. O shell recebe apenas o contador do item já existente. |
| `prd-banco-de-questoes` — `disciplina`, `RecursoNaoEncontradoException`, `DadosInvalidosException`, `ConflitoException`, `confirmacao-dialog` | Especificado, ainda não implementado | Bloqueia o filtro por disciplina da fila e os passos 5, 8 e 10. Nada é reimplementado aqui. |
| `prd-sessao-de-estudo` — tela de Início e o total de tempo de estudo do dia (RF-06) | **Sem TechSpec ainda** — só PRD | Bloqueia apenas E2E-07. A porta `TempoEstudoFlashcards` é entregue e coberta por TI-29; quem a consome é F2. Nenhum outro critério depende de F2 — diferença relevante em relação a F6a, que dependia da sessão de questões para a promessa central. |
| Numeração de migração (`V9`, `V907`) | Em aberto — F2, F4, F5 e F6a ainda não aplicaram as suas | Não bloqueia: a versão livre é conferida no passo 2 e nenhuma migração aplicada é editada. |
| PostgreSQL 18 e Docker | Necessários para Testcontainers e para o Compose de `dev` | Bloqueiam a suíte de integração: os `check` com expressão de fuso, o índice com `include` e os índices parciais não têm equivalente em banco em memória. |
| Playwright | Ausente do `package.json`; previsto em `prd-acesso-e-conta` | Bloqueia o passo 13. |
| Tabela de casos de referência do FSRS-5 | A produzir no passo 3 | Bloqueia TU-21, que é o teste que sustenta RN-08. Deriva da implementação de referência do protótipo e dos pesos publicados; não depende de serviço externo. |
| Aprovação visual das três lacunas do protótipo | A obter no passo 1 | Bloqueia os passos 10 e 11. São ajustes pequenos em telas já aprovadas, não telas novas. |

## Monitoramento e observabilidade

Sobre Actuator + Micrometer/Prometheus e logs JSON com `traceId`, conforme a §9 da `TECH-SPEC.md`.

| Sinal | Tipo | Uso |
| --- | --- | --- |
| `certame.flashcard.revisao` | contador, tags `nota` e `estado` | A métrica central do PRD: mede o objetivo de ≥ 25% dos usuários com 20 cards revisados por semana e mostra a distribuição real das notas. Uma distribuição muito deslocada para *Bom* indica retenção alvo alta demais. |
| `certame.flashcard.sessao.duracao` | *timer* (p95), tag `escopo` | Quanto dura uma sessão de revisão de verdade. Sustenta a leitura de "rotina diária curta" e alimenta o dimensionamento dos limites padrão. |
| `certame.flashcard.revisao.latencia` | *timer* (p95) | Tempo do `POST .../revisoes`. O objetivo "sem espera perceptível" é sobre esta métrica: se o p95 subir, a classificação começa a competir com a exibição do próximo card. |
| `certame.flashcard.fila.montagem` | *timer* (p95), tag `escopo` | Custo da montagem da fila, que cresce com o histórico por causa da consulta de consumo diário. É o alerta antecipado de degradação do índice `flashcard_revisao_dia_idx`. |
| `certame.flashcard.fila.tamanho` | *summary* | Distribuição do tamanho da fila entregue. Cauda longa persistente é o sinal agregado do que CA-18 mostra individualmente. |
| `certame.flashcard.fila.acumulada` | *gauge* | Quantos decks estão acima do dobro do limite diário. É a leitura direta do objetivo "≤ 10% dos usuários com fila vencida acima do dobro do limite após 8 semanas". |
| `certame.flashcard.deck.excluido` | contador, tag `com_cards` | Quantas exclusões de deck ocorrem e quantas delas moviam cards. Cruzado com o gauge de cards totais, é a verificação contínua do objetivo "0 flashcards apagados por operações de organização". |
| `certame.flashcard.acesso.negado` | contador, tag `operacao` (`fila`, `revisar`, `deck`) | Objetivo de **zero** acessos a recursos de terceiros (RN-07, CA-20). Qualquer valor diferente de zero é investigação imediata, não ajuste de meta. |
| `/actuator/health` com indicador de banco | *health check* | Prontidão do contêiner; já existente. |

**Eventos de log.** `INFO` para abertura e finalização de sessão (com `sessaoId`, `usuarioId`, escopo, total da fila e cards revisados) e para criação, atualização e exclusão de deck (com `deckId` e a contagem de cards movidos). `WARN` para revisão recusada por validação, para toda tentativa de acesso a deck, sessão ou card de terceiro — com o id tentado e o usuário do token — e para fila montada acima do dobro do limite. `ERROR` apenas para falha de infraestrutura.

> **Nenhum log, métrica, tag ou mensagem de erro carrega `frente`, `verso`, `tags` ou nome de deck.** O conteúdo do flashcard é dado pessoal do titular (LGPD) e o PRD o trata como privado até do super-admin; despejá-lo em um agregador de logs contornaria RN-07 por fora da aplicação. Ids, notas e contagens bastam para diagnosticar. Pelo mesmo motivo, nenhuma métrica recebe tag de usuário ou de deck — a cardinalidade seria proibitiva e o dado, desnecessário.

## Considerações técnicas

### Principais decisões

| Decisão | Justificativa e alternativas descartadas |
| --- | --- |
| **Um único agendador, no servidor; as previsões viajam com a fila** | CA-03 e CA-07 falam do mesmo cálculo visto de dois ângulos. Implementar o FSRS também em TypeScript daria a previsão sem viagem de rede, mas criaria duas verdades sobre a mesma fórmula, mantidas em sincronia à mão a cada mudança de peso — e a divergência seria silenciosa, porque a tela continuaria mostrando *um* número plausível. Com `previsoes()` chamando `proximo()` uma vez por nota, previsão e agendamento não podem divergir nem por construção nem por descuido. Custo: cada card da fila carrega quatro previsões, cerca de 200 bytes. |
| **A fila inteira é entregue na abertura** | A restrição de desempenho do PRD é explícita: "o card seguinte já disponível". Buscar card a card colocaria uma viagem de rede entre a classificação e a exibição do próximo, exatamente onde o PRD diz que não pode haver espera. Uma fila de 60 cards com frente e verso fica em torno de 30 KB. Descartada a paginação da fila: economizaria tráfego onde ele não é problema e gastaria latência onde ela é. |
| **FSRS-5 completo, com memória de curto prazo** | Decisão do produto sobre a alternativa fiel ao protótipo (que ignorava `w17`/`w18` e agendava tudo em dias). Traz o algoritmo publicado inteiro — *Errei* devolve o card em dez minutos, dentro da sessão, em vez de amanhã, que é o comportamento que faz o reaprendizado funcionar. Custos assumidos: a coluna de agenda precisa de resolução de minutos, os passos precisam de máquina de estados, os rótulos precisam falar de minutos e o card precisa voltar à fila sem nova requisição. Todos os quatro estão especificados. |
| **`proxima_revisao_em` ao lado de `proxima_revisao`, com `check` de coerência** | A coluna `date` foi criada por uma migração imutável de F6a e é lida pela listagem, pelos filtros e pelos indicadores. Substituí-la exigiria reescrever consultas de outra entrega; abandoná-la deixaria duas verdades sobre o vencimento. Manter as duas com um `check` imutável entre elas custa uma coluna e ganha compatibilidade verificada pelo banco. Descartada a coluna gerada: não se converte uma coluna existente em gerada sem recriá-la. |
| **Zona de estudo fixa em `America/Sao_Paulo`, resolvida por um único `RelogioEstudo`** | Todo o vocabulário da funcionalidade é diário: fila do dia, novos por dia, revisões por dia, vencendo hoje. Sem uma zona explícita, "hoje" seria o do fuso do servidor, e o limite diário do estudante viraria às 21h de Brasília. Um ponto único evita que metade do código pergunte ao banco e a outra metade ao `Clock`. Risco assumido e registrado: usuários fora do horário de Brasília têm a virada do dia no horário de Brasília. |
| **Limite diário contado a partir do histórico, por card distinto** | RN-10 fala em fila *diária*. Contar por sessão permitiria dobrar o limite reabrindo a tela, e o PRD escolheu os limites justamente como defesa contra o acúmulo que faz o estudante desistir. Contar classificações em vez de cards distintos puniria quem erra: um card que passa por três passos de aprendizado consumiria três vagas. A consulta agrupa por card antes de contar. |
| **Card novo identificado por `estabilidade_ant = 0`, sem coluna nova** | Distinguir "novos do dia" de "revisões do dia" exige saber se o card era novo no momento da revisão. A estabilidade anterior zerada é exatamente isso, já está gravada no log e não exige coluna nem *backfill*. Descartado um `boolean era_novo`: seria dado derivado, com risco de divergir do resto da linha. |
| **`flashcard_sessao` própria, e não um modo novo em `sessao_estudo`** | Reusar `sessao_estudo` faria CA-11 virar propriedade estrutural — o agregado de RF-06 somaria sem alteração. Mas aquela tabela pertence a `prd-sessao-de-estudo`, que ainda não tem TechSpec: depender do enum `modo` dela transformaria uma entrega inteira em entrega bloqueada, e uma sessão de flashcards não tem `resposta`, `origem` nem série de questões. A porta `TempoEstudoFlashcards` entrega o mesmo resultado com acoplamento de uma linha. |
| **Revisão registrada card a card, e não em lote na finalização** | O log de revisões é o ativo que o PRD guarda para otimizar os pesos na v2; perdê-lo por uma aba fechada seria perder o único dado irreprodutível da funcionalidade. O lote é o padrão do Modo Prova (RN-04) porque lá a resposta não pode contar antes da correção — aqui não existe essa restrição. A latência não aparece na tela porque o próximo card já está em memória. |
| **A linha virtual "Sem deck" no painel, com `id: null`** | RN-11 manda os cards órfãos para "Sem deck" e diz que ele tem limites próprios; sem uma linha na tela, esses cards ficariam invisíveis logo depois da operação que os moveu para lá — e CA-15 deixaria de ser observável pelo estudante. Uma tabela `deck` com uma linha "Sem deck" por usuário foi descartada: criaria dado por usuário para representar uma ausência e obrigaria a migrar todo mundo. |
| **Membership da fila não é validada no servidor** | O servidor valida dono, `status` e sessão aberta, mas não que o card estivesse na fila daquela sessão — validar exigiria materializar a fila, e a fila é derivada. A consequência é que uma chamada direta à API pode revisar um card fora da fila. RN-10 governa a *montagem* da fila, e é isso que a interface oferece; nenhum dado de terceiro é exposto e o log continua verdadeiro. Registrado aqui para que a decisão não seja redescoberta como defeito. |
| **`GET /flashcards/decks` enriquecido em vez de rota nova** | Uma rota `/decks/painel` seria endpoint fora do contrato da §6 da `TECH-SPEC.md`, que prevê o CRUD de `/flashcards/decks`. O corpo ampliado é superconjunto do de F6a, então nenhum consumidor existente muda. Custo: o seletor de deck do diálogo de criação passa a receber contadores que ignora — agregação sobre uma tabela pequena por usuário. |
| **Rótulos formatados no servidor** | `intervaloRotulo`, `limitesRotulo`, `retencaoRotulo` e `memoriaRotulo` chegam prontos. Formatar no cliente exigiria replicar a regra de arredondamento do intervalo — e o arredondamento é parte do agendamento, não da apresentação: "9 dias" precisa ser o mesmo 9 que o banco gravou. Os rótulos puramente visuais continuam no template, como manda o `code-standards.md` §2.6. |

### Riscos conhecidos

| Risco | Mitigação |
| --- | --- |
| **Erro silencioso em uma fórmula do FSRS.** Trocar um índice de peso ou um sinal não quebra nada visível: o sistema continua agendando, só que errado, e o sintoma aparece meses depois como retenção ruim. É o risco mais sério do documento. | TU-21 fixa uma tabela de casos de referência — sequências conhecidas de notas com estabilidade, dificuldade e intervalo esperados — derivada da implementação de referência do protótipo e dos pesos publicados. TU-03 fecha o ciclo intervalo→retenção, TU-10 impede que o lapso aumente a estabilidade e TU-19 amarra previsão e agendamento. Os pesos vivem em um único arquivo, e alterá-los exige atualizar o PRD-mãe (regra 7 do `AGENTS.md`). |
| **A migração toca uma tabela com dados.** `alter table flashcard` roda sobre o acervo existente, e o `check` de coerência é adicionado depois do `update`. Se a conversão de fuso estiver errada, a migração falha e a aplicação não sobe. | A ordem é deliberada: adicionar coluna com `default`, preencher com a conversão explícita, só então criar o `check`. TI-01 verifica o preenchimento e TI-02 verifica que o `check` recusa divergência. A conversão é `proxima_revisao::timestamp at time zone 'America/Sao_Paulo'`, que não depende do `TimeZone` da conexão. |
| **Zona de estudo fixa.** Um estudante no Acre vira o dia três horas antes do que esperaria. | Aceito para a v1 e registrado. A zona é uma propriedade (`certame.estudo.zona`) e um único ponto de leitura (`RelogioEstudo`), de modo que torná-la preferência do usuário na v2 é alterar a origem do valor, não espalhar `ZoneId` pelo código. |
| **Custo da consulta de consumo diário cresce com o histórico**, que nunca é apagado, e ela roda a cada abertura de sessão. | Índice `flashcard_revisao_dia_idx` com `include (flashcard_id, estabilidade_ant)` torna a varredura *index-only*, e o filtro é sempre `usuario_id` mais o dia. `certame.flashcard.fila.montagem` alerta se o p95 sair do previsto. Se um dia não bastar, o caminho é uma tabela de consumo por usuário/deck/dia, não um índice maior. |
| **Reenfileiramento pode prender o estudante em um card.** Errar repetidamente devolve o card a cada dez minutos, e a sessão pode não terminar nunca. | O comportamento é o do FSRS e é o que produz o reaprendizado, mas a fila mostra os cards vencidos antes dos reenfileirados e o resumo é alcançável a qualquer momento pela finalização — a sessão nunca fica refém do último card. A distribuição de notas em `certame.flashcard.revisao` mostra se isso vira padrão. |
| **A escolha por FSRS-5 completo diverge do protótipo aprovado** em um ponto visível: os botões passam a exibir "1 min" e "10 min" além de dias. | É a lacuna (c) do passo 1 do sequenciamento, resolvida com aprovação visual antes de qualquer marcação. Nenhuma cor, componente ou token novo — só o texto do rótulo, que o servidor já entrega pronto. |
| **`sessao_id` anulável em `flashcard_revisao`** deixa a porta aberta para revisão sem sessão. | Nenhum caminho da API cria revisão sem sessão, e o resumo filtra por `sessao_id = ?`. A anulabilidade existe só porque a semente `V906` de `dev` é uma migração já aplicada e imutável. TI-14 verifica que toda revisão criada pela API nasce com sessão. |
| **F6a inteira é pré-requisito** e ainda não foi implementada. | Diferente de F6a em relação a F2, aqui a dependência é sobre uma TechSpec fechada, com cada assinatura já fixada — `ConsultaFilaFlashcards`, `VerificadorDonoFlashcard`, `DeckService`, `shared/ui/dialog`. O risco é de cronograma, não de contrato. Os passos 3 e 4 (agendador, relógio e limites) não dependem de F6a e podem começar em paralelo. |

### Conformidade com o AGENTS.md e as rules

Foram lidos integralmente o `AGENTS.md` e as duas rules de `.agents/rules/` — `code-standards.md` e `tests.md`.

- **Regra 1 — o frontend é tradução 1:1 do protótipo.** A tela de decks, o diálogo de configuração e a sessão de revisão inteira já existem no protótipo aprovado e são traduzidas com a mesma marcação e os mesmos tokens `var(--*)` de `styles.css`. As três lacunas (aviso de fila acumulada, linha "Sem deck" e rótulo em minutos) são fechadas no protótipo antes do Angular, com componentes Nocturne existentes. Nenhuma biblioteca de UI de terceiros.
- **Regra 2 — autorização no servidor.** Todas as sete rotas exigem `@PreAuthorize("isAuthenticated()")` no serviço, e o dono vem sempre do *subject* do token. Não existe rota administrativa de deck, sessão ou revisão em nenhum perfil.
- **Regra 3 — idioma.** Identificadores, tabelas e colunas em português sem acento (`flashcard_sessao`, `proxima_revisao_em`, `passo_aprendizado`, `MontadorFila`, `RelogioEstudo`); mensagens ao usuário em pt-BR com acentuação e ponto final; `code`, `message` e `fieldErrors` permanecem em inglês, como já é convenção.
- **Regra 4 — nenhum endpoint fora do contrato.** As sete rotas estão previstas na §6 da `TECH-SPEC.md`. O passo 13 atualiza §5 e §6 na mesma entrega, com `flashcard_sessao`, as seis colunas novas e o detalhamento de `PUT`/`DELETE /flashcards/decks/{id}`.
- **Regra 5 — migração só por Flyway.** `V9__flashcards_revisao.sql` é aditiva e imutável; nenhuma migração já aplicada é editada, incluindo `V8` e a semente `V906`. É por isso que `sessao_id` é anulável.
- **Regra 7 — flashcards privados e FSRS em `flashcard/fsrs`.** Toda consulta filtra por `usuario_id` dentro do `where`; recurso de terceiro responde `404`, nunca `403`. O agendador vive em `flashcard/fsrs`, os pesos são os padrão publicados do FSRS-5 e estão versionados junto ao código.
- **Regra 9 — fila com os limites do deck e exclusão que não apaga cards.** RN-10 é implementada em `MontadorFila`, com desconto do consumo diário; RN-11 é implementada em `DeckService.excluir`, na mesma transação, com `on delete set null` como segunda barreira.
- **`code-standards.md`.** Pacote por domínio sob `br.com.certame.flashcard`; DTOs `record` em `dto/` com Bean Validation e mensagem em português por *constraint*; entidades sem Lombok, com construtor protegido e *setters* só no que muda; injeção por construtor com campos `private final`; erros por subclasse de `ErroNegocioException`, sem `try/catch` em controller; arquivos até 100 linhas e métodos até 30 — é o motivo direto de o agendador ser cinco arquivos e não um; até 3 parâmetros, com `ContextoAgendamento`, `EscopoFila` e `LimitesDeck` como objetos parâmetro; cláusulas guarda em vez de aninhamento; nenhum número mágico fora de `PesosFsrs` e `LimitesDeck`; nenhum segredo no código.
- **`tests.md`.** Cobertura mínima de 80% por módulo, que prevalece sobre os 70% da `TECH-SPEC.md` §9; prioridade alta para `flashcard/fsrs` e para a fila e os limites do deck, ambos com caminho feliz e todas as bordas; FIRST observado — unidade sem I/O, `Clock` fixo em tudo que é diário, dados isolados por `UUID.randomUUID()`, nenhum estado estático compartilhado; AAA/Given-When-Then visível; nomes no padrão `TU`/`TI` + id + comportamento em português sem acento; pirâmide de ~70/20/10 respeitada — o agendador não sobe para integração, e a privacidade não desce para unidade.

### Conformidade com skills

| Skill | Aplicável | Como é observada |
| --- | --- | --- |
| `313-frameworks-spring-db-migrations-flyway` | sim | SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração; `validate-on-migrate` ligado e `baseline-on-migrate` desligado, como já configurado; disciplina *forward-only*; `spring.flyway.locations` separa produção, `dev` e teste. A migração é aditiva: cria uma tabela e acrescenta colunas, sem remover nem redefinir nada existente, e o `update` de preenchimento roda antes de o `check` ser criado. |
| `postgresql-table-design` | sim | PK `uuid` com `gen_random_uuid()`; `timestamptz` para instantes e `date` preservado para o dia civil; `check` para os enums textuais e para as três invariantes (escopo da sessão, coerência das contagens, coerência da agenda); índice parcial `where status = 'ATIVO'` alinhado exatamente à consulta da fila; índice de cobertura com `include` para tornar *index-only* a consulta de consumo diário; expressão de fuso imutável no `check`; `on delete set null` deliberado em `deck_id` e ausência deliberada de cascata em `flashcard_revisao`. |
| `spring-boot-testing` | sim | Fatia mais estreita por cenário: JUnit + Mockito puros no agendador, no montador de fila e nos serviços; `@DataJpaTest` com Testcontainers nas *constraints* e na migração; `@WebMvcTest` com `MockMvcTester` no contrato e na privacidade; `@SpringBootTest` só nos treze fluxos que atravessam escrita e leitura; asserções em AssertJ; contexto compartilhado; `Clock` fixo por `@TestConfiguration`. **Desvio registrado:** o `AGENTS.md` fala em "JUnit 5", enquanto o BOM do Spring Boot 4.1.1 traz JUnit Jupiter 6 — a API é a mesma e prevalece a versão gerenciada pelo BOM. |
| `angular-developer` | sim | Componentes standalone, detecção *zoneless*, estado por signals em serviços `providedIn: 'root'` (`RevisaoStore`, `DecksStore`), `input()`/`output()` como funções, `computed` para progresso, restantes e estado vazio, guarda funcional `authGuard`, `LOCALE_ID` pt-BR, acessibilidade de diálogo herdada do `shared/ui/dialog` de F6a e atalhos de teclado com foco visível e alvos anunciados. |
| `impeccable` | não | O layout é congelado pela regra 1 do `AGENTS.md`. As três lacunas são compostas com componentes Nocturne existentes e o padrão visual das telas irmãs, não redesenho; a acessibilidade exigida pelo PRD é coberta por TU-61…TU-63 e E2E-16. |
| `find-skills`, `criar-*`, `executar-*` | não | São skills de processo, não de implementação. |

### Arquivos relevantes e dependentes

**Criados** — backend:
`flashcard/fsrs/PesosFsrs.java` · `flashcard/fsrs/CurvaEsquecimento.java` · `flashcard/fsrs/CalculoDificuldade.java` · `flashcard/fsrs/CalculoEstabilidade.java` · `flashcard/fsrs/PassosAprendizado.java` · `flashcard/fsrs/AgendadorFsrs.java` · `flashcard/fsrs/EstadoMemoria.java` · `flashcard/fsrs/ContextoAgendamento.java` · `flashcard/fsrs/Agendamento.java` · `flashcard/fsrs/PrevisaoIntervalos.java` · `flashcard/NotaRevisao.java` · `flashcard/RelogioEstudo.java` · `flashcard/revisao/SessaoRevisao.java` · `flashcard/revisao/SessaoRevisaoRepository.java` · `flashcard/revisao/FlashcardRevisao.java` · `flashcard/revisao/FlashcardRevisaoRepository.java` · `flashcard/revisao/EscopoFila.java` · `flashcard/revisao/MontadorFila.java` · `flashcard/revisao/ConsumoDiario.java` · `flashcard/revisao/SessaoRevisaoService.java` · `flashcard/revisao/RegistroRevisaoService.java` · `flashcard/revisao/ResumoRevisaoService.java` · `flashcard/revisao/TempoEstudoFlashcards.java` · `flashcard/revisao/RevisaoController.java` · `flashcard/revisao/dto/RequisicaoSessaoRevisao.java` · `flashcard/revisao/dto/RespostaSessaoRevisao.java` · `flashcard/revisao/dto/CardRevisao.java` · `flashcard/revisao/dto/MemoriaCard.java` · `flashcard/revisao/dto/PrevisaoNota.java` · `flashcard/revisao/dto/RequisicaoRevisao.java` · `flashcard/revisao/dto/RespostaRevisao.java` · `flashcard/revisao/dto/ResumoSessao.java` · `flashcard/revisao/dto/FaixaVencimento.java` · `flashcard/revisao/dto/ResumoFlashcards.java` · `flashcard/deck/LimitesDeck.java` · `flashcard/deck/PainelDecksService.java` · `flashcard/deck/dto/DeckDetalhe.java` · `flashcard/deck/dto/AvisoDeck.java` · `backend/src/main/resources/db/migration/V9__flashcards_revisao.sql` · `backend/src/main/resources/db/migration-dev/V907__seed_revisao_dev.sql`

**Criados** — frontend:
`core/api/revisao-api.ts` · `core/api/modelos-revisao.ts` · `shared/pipes/intervalo-pipe.ts` · `features/flashcards/decks-page/` · `features/flashcards/deck-card/` · `features/flashcards/deck-dialog/` · `features/flashcards/decks-store.ts` · `features/flashcards/sessao-revisao/` · `features/flashcards/sessao-revisao/card-revisao/` · `features/flashcards/sessao-revisao/notas-revisao/` · `features/flashcards/sessao-revisao/resumo-revisao/` · `features/flashcards/sessao-revisao/fila-vazia/` · `features/flashcards/revisao-store.ts` · `features/flashcards/atalhos-revisao.ts` · `features/flashcards/cronometro-sessao.ts`

**Modificados:**
`flashcard/Flashcard.java` (colunas `proximaRevisaoEm`, `passoAprendizado` e o método `agendarPara`) · `flashcard/FlashcardRepository.java` (consulta da fila por escopo e contagens do painel) · `flashcard/deck/Deck.java` (`setNome`, `setDescricao`, `aplicarLimites`) · `flashcard/deck/DeckService.java` (`atualizar`, `excluir`) · `flashcard/deck/DeckController.java` (`PUT`, `DELETE`) · `flashcard/deck/dto/RequisicaoDeck.java` (descrição e os quatro limites) · `frontend/src/app/core/api/flashcard-api.ts` (`atualizarDeck`, `excluirDeck`, `DeckDetalhe`) · `frontend/src/app/app.routes.ts` (`/flashcards/decks`, `/flashcards/estudar`) · `frontend/src/app/layout/shell/shell.html` (contador de vencendo hoje) · `protótipo/Certame.dc.html` (aviso de fila acumulada, linha "Sem deck", rótulo de intervalo em minutos) · `TECH-SPEC.md` §5 e §6

**Reutilizados:** `flashcard/ConsultaFilaFlashcards.java` · `flashcard/VerificadorDonoFlashcard.java` · `flashcard/StatusFlashcard.java` · `flashcard/EstadoFlashcard.java` · `flashcard/deck/DeckRepository.java` · `config/SegurancaConfig.java` · `usuario/Usuario.java` · `comum/ManipuladorErrosGlobal.java` · `comum/ErroApi.java` · `comum/excecao/RecursoNaoEncontradoException.java` · `comum/excecao/DadosInvalidosException.java` · `comum/excecao/ConflitoException.java` · `catalogo/Disciplina.java` · `backend/src/main/resources/db/migration/V8__flashcards.sql` · `backend/src/main/resources/db/migration-dev/V906__seed_flashcards_dev.sql` · `frontend/src/app/core/auth/auth-guard.ts` · `frontend/src/app/core/auth/auth-interceptor.ts` · `frontend/src/app/shared/ui/dialog/` · `frontend/src/app/shared/ui/toast/` · `frontend/src/app/shared/ui/confirmacao-dialog/` · `frontend/src/styles.css`

**Dependentes:** `prd-sessao-de-estudo` consome `TempoEstudoFlashcards.segundosNoDia` para somar o tempo das sessões de flashcards ao resumo do dia (RF-06, CA-11) e hospeda o E2E-07; `prd-cadernos-e-desempenho` pode agregar `flashcard_revisao` nas estatísticas de retenção da v2; `prd-gestao-de-usuarios` não muda — `flashcard_sessao` e `flashcard_revisao` referenciam `usuario(id)` sem cascata e a exclusão de conta é lógica. Nenhuma funcionalidade da v1 depende de contrato criado aqui além dessa porta.
