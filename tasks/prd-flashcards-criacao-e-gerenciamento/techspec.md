# Especificação técnica

> **Funcionalidade:** Flashcards — criação no fluxo da questão e gerenciamento · **PRD:** `tasks/prd-flashcards-criacao-e-gerenciamento/prd.md` · **Fase:** F6 — Flashcards
> Deriva de `TECH-SPEC.md` v1.0 (§3–§6, recortadas para RF-28…RF-32, RN-07 e RN-09).
> Depende de `tasks/prd-acesso-e-conta/techspec.md` (segurança, `authGuard`, shell, `styles.css`) e de `tasks/prd-banco-de-questoes/techspec.md`, que funda `disciplina`, `assunto`, `questao`, `PaginaResposta<T>`, os três tipos de exceção e o `confirmacao-dialog` reutilizados aqui. Integra-se a `tasks/prd-sessao-de-estudo/prd.md` (ainda sem TechSpec) pelo contrato registrado em *Pontos de integração*.
> Entrega a metade de criação e gerenciamento; `tasks/prd-flashcards-decks-e-revisao/prd.md` entrega a tela de decks, os limites e o agendamento FSRS sobre o mesmo esquema.

## Resumo

A funcionalidade tem uma restrição que define todo o desenho: **criar um flashcard não pode custar a sessão de estudo**. Isso elimina rota, elimina recarga e elimina qualquer estado global recriado — o modal é um diálogo aberto por um `DialogService` sobre a tela da questão, e o `StudySessionStore` nem sabe que ele existe. A garantia não é de disciplina de código: ela é verificável, porque a única coisa que o diálogo recebe é um objeto de contexto imutável (`ContextoQuestaoFlashcard`) e a única coisa que ele devolve é um evento de card criado. Não há injeção do store da sessão, não há `router.navigate`, não há `location`. CA-02 vira uma propriedade estrutural, não um cuidado.

No servidor, a regra que manda é RN-07: **flashcard é privado do dono, sem exceção de perfil**. Isso é implementado por construção, não por verificação espalhada. Não existe rota administrativa de flashcard em lugar nenhum do contrato; toda consulta parte do `usuarioId` do token, nunca de um parâmetro de caminho; e o único ponto de entrada por id é `VerificadorDonoFlashcard`, que devolve `404` — nunca `403` — para card de terceiro, porque `403` confirmaria a existência do card (CA-15). Um super-admin autenticado recebe exatamente a mesma resposta de um estranho.

O card guarda desde já **todo o estado do FSRS** (estabilidade, dificuldade, intervalo, repetições, lapsos, estado e próxima revisão), com os padrões da §5 da `TECH-SPEC.md`, e a migração desta entrega cria também `flashcard_revisao`. Nada aqui escreve nessas colunas além da criação, e nada aqui calcula agendamento: `prd-flashcards-decks-e-revisao` acrescenta comportamento sobre um esquema pronto, sem nova migração. A razão é CA-12 — a exclusão lógica precisa preservar um histórico que só existe se a tabela existir — e a consequência é que `ConsultaFilaFlashcards`, a porta de leitura que F6b consome para montar a fila, já nasce aqui coberta por teste, tornando CA-11 verificável nesta entrega.

O deck aparece na medida exata do que RF-32 e CA-16 exigem: a tabela completa (já com as colunas de limites e seus padrões, para que F6b não precise alterá-la), a listagem, a criação por nome e a movimentação de um lote de cards. Renomear, descrever, excluir, configurar limites e a tela de decks são de F6b. A busca textual usa uma coluna `tsvector` gerada sobre frente e verso com índice GIN, no mesmo padrão de `questao.busca`, e os quatro indicadores do topo da tela saem de contagens sobre `flashcard` — nenhuma revisão é necessária para calculá-los, então eles viajam embutidos no payload da listagem, em uma requisição só.

## Arquitetura do sistema

### Visão dos componentes

```
Angular 22 (SPA)                              Spring Boot 4.1                        PostgreSQL 18
────────────────                              ───────────────                        ─────────────
features/study/ (F2)                                                                 ┌── flashcard
  QuestaoAcoes  ──abre──▶ DialogService                                              ├── deck
                          │                                                          ├── flashcard_revisao
features/flashcards/      │                                                          │   (criada aqui,
  FlashcardDialog    ◀────┘   ────▶ POST   /api/v1/flashcards                        │    escrita por F6b)
  FlashcardFormStore          ────▶ PUT    /api/v1/flashcards/{id}                   │
  RascunhoFlashcard  (sessionStorage)                                                └─ referenciadas
                                                                                        (V1, V2, F2)
  FlashcardsPage              ────▶ GET    /api/v1/flashcards?q&deck&…               ┌── usuario
  FlashcardsFiltros           ────▶ DELETE /api/v1/flashcards/{id}                   ├── disciplina
  FlashcardsIndicadores       ────▶ POST   /api/v1/flashcards/{id}/arquivar          ├── assunto
  FlashcardsStore             ────▶ POST   /api/v1/flashcards/{id}/restaurar         ├── questao
  MoverParaDeck               ────▶ POST   /api/v1/flashcards/decks/{id}/mover       └── sessao_estudo
  DeckSeletor                 ────▶ GET    /api/v1/flashcards/decks
                              ────▶ POST   /api/v1/flashcards/decks
shared/ui/dialog · shared/ui/toast
```

**Backend — pacote `br.com.certame.flashcard`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `flashcard/Flashcard` | novo | Entidade raiz: dono, deck, frente, verso, disciplina, assunto, questão e sessão de origem, tags, `status` e o bloco de estado FSRS com os padrões da §5. Estados por `status`, não por booleano. |
| `flashcard/StatusFlashcard` | novo | Enum `ATIVO`, `ARQUIVADO`, `EXCLUIDO`, persistido como texto com `check` equivalente na migração. |
| `flashcard/EstadoFlashcard` | novo | Enum `NOVO`, `APRENDENDO`, `REVISAO`, `REAPRENDENDO`. Escrito por F6b; aqui só nasce em `NOVO` e é lido no filtro de estado (CA-10). |
| `flashcard/FlashcardRepository` | novo | Spring Data. **Toda** assinatura recebe `usuarioId` — não existe método que consulte card por id isolado (RN-07). Busca textual por `@Query` nativa sobre a coluna `busca`. |
| `flashcard/VerificadorDonoFlashcard` | novo | RN-07/CA-15: `exigirProprio(id, usuarioId)` devolve o card ou lança `RecursoNaoEncontradoException`. Ponto único de entrada por id — nenhum serviço chama `findById` diretamente. |
| `flashcard/FlashcardService` | novo | RF-29/RF-32: criar, editar, arquivar, restaurar e excluir logicamente. `@Transactional`, `@PreAuthorize("isAuthenticated()")` e a normalização de frente, verso e tags. |
| `flashcard/ResolvedorContextoFlashcard` | novo | RN-09/CA-03: valida `questaoOrigemId` e `sessaoOrigemId`, herda `disciplinaId` e `assuntoId` da questão quando não informados e recusa origem que não seja do usuário. Isola a única regra de integridade referencial da criação. |
| `flashcard/NormalizadorTags` | novo | Regra pura: recorta, minúsculiza, remove duplicatas e vazios, aplica o limite de 10 tags. Sem Spring, sem repositório. |
| `flashcard/FiltroFlashcards` | novo | Objeto parâmetro (`code-standards.md` §2.4) com `termo`, `deckId`, `disciplinaId` e `estado`. |
| `flashcard/ConsultaFlashcardsService` | novo | RF-31: listagem paginada com busca e filtros combinados, ordenação restrita a `criadoEm` e `proximaRevisao`, e os quatro indicadores globais do topo. Somente leitura. |
| `flashcard/ConsultaFilaFlashcards` | novo | Porta de leitura consumida por F6b: `elegiveis(usuarioId, ate)` devolve os cards `ATIVO` com `proxima_revisao <= ate`. Existe aqui porque CA-11 e CA-12 são critérios desta entrega e precisam de algo observável para provar que arquivado e excluído ficam de fora. |
| `flashcard/FlashcardController` | novo | Rotas de `/api/v1/flashcards`, autenticadas. Só orquestra; o `usuarioId` vem do token, nunca do corpo ou do caminho. |
| `flashcard/deck/Deck` | novo | Entidade do deck com nome, descrição e as quatro colunas de limite já com os padrões de RN-11. Nome único por usuário, sem diferenciar maiúsculas. |
| `flashcard/deck/DeckRepository` | novo | Listagem por usuário com a contagem de cards ativos; verificação de nome em uso. |
| `flashcard/deck/DeckService` | novo | RF-32: listar, criar por nome e mover um lote de cards (CA-16). Renomear, descrever, excluir e configurar limites são de F6b. |
| `flashcard/deck/DeckController` | novo | `GET` e `POST /api/v1/flashcards/decks` e `POST /api/v1/flashcards/decks/{id}/mover`. |
| `flashcard/dto/` | novo | `RequisicaoFlashcard`, `FlashcardResumo`, `PaginaFlashcards`, `IndicadoresFlashcards`, `RespostaFlashcardCriado`, `OrigemResumo`, `ReferenciaResumo`, `RequisicaoDeck`, `DeckResumo`, `RequisicaoMoverCards`. |
| `comum/excecao/RecursoNaoEncontradoException`, `DadosInvalidosException`, `ConflitoException` | reutilizados | Criados em `prd-banco-de-questoes`; cobrem os cinco códigos desta funcionalidade. `ManipuladorErrosGlobal` **não muda**. |

**Frontend — `frontend/src/app`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `core/api/flashcard-api.ts` | novo | Cliente HTTP das dez rotas; único ponto que fala com a API de flashcards. |
| `core/api/modelos-flashcard.ts` | novo | Tipagem dos contratos desta especificação. |
| `shared/ui/dialog/` | novo | `DialogService` + contêiner `app-dialog`: `aria-modal`, armadilha de foco, `Esc`, foco inicial configurável e devolução do foco ao elemento indicado no fechamento (CA-05). Reutilizado pelo diálogo de deck de F6b. |
| `shared/ui/toast/` | novo | `ToastStore` (signals) + `app-toast`: mensagem não bloqueante com `role="status"`, prevista no `folder-structure.md` §3.1 e exigida por CA-05. |
| `features/flashcards/flashcard-dialog/` | novo | RF-29/RF-30: o modal, em tradução 1:1 do protótipo. Não injeta `StudySessionStore` nem `Router` — recebe `ContextoQuestaoFlashcard` e emite `criado`. |
| `features/flashcards/flashcard-form-store.ts` | novo | Estado do modal: validação inline de frente e verso, as três ações, a confirmação de descarte (CA-07) e o contador da sessão (CA-08). |
| `features/flashcards/rascunho-flashcard.ts` | novo | Funções puras de rascunho em `sessionStorage` sob `fc-draft:{questaoId}`, gravadas a cada digitação e limpas no sucesso e no descarte confirmado. |
| `features/flashcards/flashcards-page/` | novo | RF-31: a tela `/flashcards` — indicadores, filtros, tabela paginada, ações de linha e estado vazio. |
| `features/flashcards/flashcards-filtros/` | novo | Busca com atraso de digitação e os três seletores (deck, disciplina, estado), refletidos em *query params* para recarga e compartilhamento. |
| `features/flashcards/flashcards-indicadores/` | novo | A faixa de quatro cartões do topo, alimentada pelo bloco `indicadores` da listagem. |
| `features/flashcards/flashcards-store.ts` | novo | Signals da lista: `pagina`, `filtro`, `indicadores`, `carregando`, `vazio`, `selecionados`. |
| `features/flashcards/deck-seletor/` | novo | Seletor de deck com a opção "Sem deck" e a ação "+ Novo deck", que cria por nome sem sair do modal. |
| `features/flashcards/mover-para-deck/` | novo | CA-16: ação em lote sobre as linhas selecionadas. |
| `features/study/questao-acoes/` | **modificado (F2)** | Recebe o botão "Novo flashcard" com o contador. É o único ponto de contato com a sessão e está especificado em *Pontos de integração* para que F2 o incorpore. |
| `shared/ui/confirmacao-dialog/` | reutilizado | Criado em `prd-banco-de-questoes`; usado na exclusão de card. Se ainda não existir na entrega, nasce aqui sobre o `shared/ui/dialog`. |
| `layout/shell/shell.html` | **modificado** | Item "Flashcards" (`ph ph-lightning`) na seção Estudo, com o contador de cards vencendo hoje. |
| `app.routes.ts` | **modificado** | `/flashcards` e `/flashcards/:id` sob `authGuard`. A criação **não** ganha rota (RF-28). |
| `protótipo/Certame.dc.html` | **modificado** | Três lacunas pontuais nas telas já aprovadas — detalhadas em *Riscos conhecidos*. |

**Fluxo de dados.** Na sessão, a barra de ações da questão abre o diálogo com `{questaoId, codigo, disciplinaId, disciplinaNome, assuntoId, assuntoNome, sessaoId}`; o `FlashcardFormStore` faz `POST /flashcards`, recebe o card criado **e** o total de cards daquela sessão, emite o toast, devolve o foco ao enunciado e some. Nada da sessão é remontado: o cronômetro é um sinal em `StudySessionStore`, que não é injetado nem tocado. Fora da sessão, `/flashcards` faz **uma** requisição que traz a página filtrada e os indicadores globais; editar abre o mesmo diálogo com os dados já carregados da linha, sem segunda leitura; arquivar, restaurar, excluir e mover recarregam apenas a página corrente.

## Design de implementação

### Principais interfaces

```
VerificadorDonoFlashcard
  exigirProprio(UUID id, UUID usuarioId) -> Flashcard    // 404 para terceiro (CA-15)

FlashcardService
  criar(UUID usuarioId, RequisicaoFlashcard) -> RespostaFlashcardCriado   // RN-09
  atualizar(UUID id, UUID usuarioId, RequisicaoFlashcard) -> FlashcardResumo
  arquivar(UUID id, UUID usuarioId) -> void              // CA-11
  restaurar(UUID id, UUID usuarioId) -> void
  excluir(UUID id, UUID usuarioId) -> void               // logica; preserva revisoes (CA-12)

ResolvedorContextoFlashcard
  resolver(UUID usuarioId, RequisicaoFlashcard) -> ContextoOrigem  // 422 em origem invalida

NormalizadorTags
  normalizar(List<String> brutas) -> List<String>        // minusculas, sem duplicata, ate 10

ConsultaFlashcardsService
  listar(UUID usuarioId, FiltroFlashcards, Pageable) -> PaginaFlashcards
  buscar(UUID id, UUID usuarioId) -> FlashcardResumo

ConsultaFilaFlashcards
  elegiveis(UUID usuarioId, LocalDate ate) -> List<Flashcard>   // porta consumida por F6b

DeckService
  listar(UUID usuarioId) -> List<DeckResumo>
  criar(UUID usuarioId, RequisicaoDeck) -> DeckResumo    // 409 em nome repetido
  mover(UUID deckId, UUID usuarioId, List<UUID> flashcardIds) -> int   // CA-16
```

```
FlashcardFormStore (signals)
  abrir(contexto: ContextoQuestaoFlashcard | null) -> void
  salvarEFechar() -> Promise<void>
  salvarECriarOutro() -> Promise<void>          // limpa frente/verso, mantem contexto (CA-06)
  cancelar() -> void                            // pede confirmacao se houver texto (CA-07)
  erroDeCampo: Signal<Record<'frente'|'verso', string | null>>
  sujo: Signal<boolean>
  criadosNaSessao: Signal<number>               // contador do botao (CA-08)

FlashcardsStore (signals)
  carregar(pagina: number) -> Promise<void>
  filtrar(filtro: FiltroFlashcards) -> void     // busca com atraso de digitacao
  arquivar(id: string) -> Promise<void>
  excluir(id: string) -> Promise<void>
  moverSelecionados(deckId: string) -> Promise<void>
  vazio: Signal<boolean>                        // pagina vazia com filtro aplicado
```

### Modelos de dados

Contratos JSON entre backend e UI, prontos para exibição. Instantes trafegam em ISO-8601 UTC (`2026-08-30T14:02:11Z`) e `proximaRevisao` trafega como dia civil (`yyyy-MM-dd`), porque o agendamento é por dia, não por instante; a formatação em pt-BR é do cliente, pelo `DatePipe` com `LOCALE_ID` `pt-BR`. Campo ausente na origem é normalizado para `null`, nunca omitido nem convertido em string vazia. Listas nunca voltam `null`.

#### `FlashcardResumo` — um card na listagem e no detalhe (RF-31)

Frente e verso viajam **completos**: um card é curto por construção e o mesmo objeto alimenta a linha da tabela e a edição, sem segunda requisição. O corte de 96 caracteres do verso na tabela é do cliente.

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Identificador do card. |
| `frente` | `string` | sim | Texto integral da frente. |
| `verso` | `string` | sim | Texto integral do verso. |
| `deck` | `ReferenciaResumo` | não | `null` quando o card está em "Sem deck". |
| `disciplina` | `ReferenciaResumo` | não | `null` em card avulso sem disciplina informada. |
| `assunto` | `ReferenciaResumo` | não | `null` quando a questão de origem não tinha assunto. |
| `tags` | `string[]` | sim | Já normalizadas; lista vazia quando não houver. |
| `estado` | `string` | sim | `NOVO`, `APRENDENDO`, `REVISAO` ou `REAPRENDENDO`. O rótulo em pt-BR é do cliente. |
| `status` | `string` | sim | `ATIVO` ou `ARQUIVADO`. `EXCLUIDO` nunca aparece em resposta. |
| `proximaRevisao` | `string` (date) | sim | Dia da próxima revisão; para card novo é o dia da criação. |
| `origem` | `OrigemResumo` | não | `null` em card avulso (CA-14). |
| `criadoEm` | `string` | sim | Instante de criação, base da ordenação padrão. |

```text
{
  "id": "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f",
  "frente": "Efeitos da decisão de mérito em ADI",
  "verso": "Eficácia erga omnes e efeito vinculante (art. 102, §2º, CF).",
  "deck": { "id": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d", "nome": "Constitucional" },
  "disciplina": { "id": "6a1c0f2e-9d3b-4a10-8f22-71c5b0e4d901", "nome": "Direito Constitucional" },
  "assunto": { "id": "b2d4e6f8-1a3c-4b5d-8e7f-0a9c1b2d3e4f", "nome": "Controle de constitucionalidade" },
  "tags": ["adi", "stf"],
  "estado": "NOVO",
  "status": "ATIVO",
  "proximaRevisao": "2026-08-30",
  "origem": {
    "questaoId": "1b0f7f0a-5c2f-4f0e-9a51-2a4d3e7c9b10",
    "codigo": "Q-88409",
    "sessaoId": "7c8d9e0f-1a2b-4c3d-8e5f-6a7b8c9d0e1f"
  },
  "criadoEm": "2026-08-30T14:02:11Z"
}
```

> **Variante — card avulso (CA-14):** criado fora da sessão, sem questão nem sessão de origem. `origem` volta `null` inteiro, nunca um objeto com campos nulos, e a linha da tabela omite o link "Criado na questão".

```text
{
  "origem": null,
  "deck": { "id": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d", "nome": "Constitucional" },
  "disciplina": { "id": "6a1c0f2e-9d3b-4a10-8f22-71c5b0e4d901", "nome": "Direito Constitucional" }
}
```

#### `ReferenciaResumo` e `OrigemResumo` — referências ao catálogo e à origem

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Identificador do deck, da disciplina ou do assunto. |
| `nome` | `string` | sim | Nome corrente; renomear a disciplina reflete na próxima leitura, sem migrar card. |

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `questaoId` | `string` (uuid) | sim | Questão que originou o card; alimenta "ver questão de origem" (CA-13). |
| `codigo` | `string` | sim | Código `Q-#####` exibido no link da tabela. |
| `sessaoId` | `string` (uuid) | não | Sessão de estudo em que o card nasceu (RN-09). `null` quando o card veio de uma questão fora de sessão. |

#### `IndicadoresFlashcards` — a faixa de quatro cartões (RF-31)

Contagens **globais** do usuário, sobre cards `ATIVO`: elas não seguem os filtros da tabela, exatamente como no protótipo, porque respondem "quanto eu tenho" e não "quanto o filtro achou".

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `vencendoHoje` | `number` | sim | `proxima_revisao <= hoje`. |
| `proximosSeteDias` | `number` | sim | `proxima_revisao <= hoje + 7`, incluindo os vencidos. |
| `emFixacao` | `number` | sim | Estado `NOVO`, `APRENDENDO` ou `REAPRENDENDO`. |
| `total` | `number` | sim | Todos os cards ativos. |
| `decks` | `number` | sim | Decks distintos com ao menos um card ativo; alimenta o texto "em N decks". |

#### `PaginaFlashcards` — envelope da listagem

Estende o `PaginaResposta<T>` da §6 da `TECH-SPEC.md` com o bloco de indicadores, para que a tela inteira caiba em uma requisição.

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `content` | `FlashcardResumo[]` | sim | Itens da página; lista vazia quando o filtro não acha nada (CA-10). |
| `page` | `number` | sim | Índice base zero. |
| `size` | `number` | sim | Tamanho solicitado; padrão 20, máximo 100. |
| `totalElements` | `number` | sim | Total exato de cards que atendem ao filtro. |
| `indicadores` | `IndicadoresFlashcards` | sim | Sempre presente, mesmo com `content` vazio. |

```text
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "indicadores": { "vencendoHoje": 12, "proximosSeteDias": 34, "emFixacao": 9, "total": 128, "decks": 4 }
}
```

#### `RequisicaoFlashcard` — corpo de `POST /flashcards` e `PUT /flashcards/{id}`

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `frente` | `string` | sim | Após recorte de espaços: mínimo de 3, máximo de 1000 caracteres (CA-04). |
| `verso` | `string` | sim | Mesmas regras da frente, máximo de 4000 caracteres. |
| `deckId` | `string` (uuid) | não | Deck do próprio usuário; `null` significa "Sem deck". |
| `disciplinaId` | `string` (uuid) | não | Disciplina da árvore (RF-20). Ignorado quando há `questaoOrigemId` — a origem manda. |
| `assuntoId` | `string` (uuid) | não | Assunto pertencente à disciplina informada. |
| `tags` | `string[]` | não | Até 10 tags de até 30 caracteres; normalizadas no servidor. Ausente equivale a lista vazia. |
| `questaoOrigemId` | `string` (uuid) | não | Questão publicada; herda disciplina e assunto (CA-03). Imutável depois de criado — o `PUT` ignora alteração deste campo. |
| `sessaoOrigemId` | `string` (uuid) | não | Sessão do próprio usuário (RN-09). Só aceito junto de `questaoOrigemId`. |

```text
{
  "frente": "Efeitos da decisão de mérito em ADI",
  "verso": "Eficácia erga omnes e efeito vinculante (art. 102, §2º, CF).",
  "deckId": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d",
  "disciplinaId": null,
  "assuntoId": null,
  "tags": ["adi", "stf"],
  "questaoOrigemId": "1b0f7f0a-5c2f-4f0e-9a51-2a4d3e7c9b10",
  "sessaoOrigemId": "7c8d9e0f-1a2b-4c3d-8e5f-6a7b8c9d0e1f"
}
```

#### `RespostaFlashcardCriado` — corpo de `201` na criação

O contador da sessão vem do servidor, e não de um número guardado na tela: recarregar a página no meio da sessão, ou criar um card em outra aba, não perde nem duplica a contagem de CA-08.

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `flashcard` | `FlashcardResumo` | sim | O card recém-criado, já agendado para hoje. |
| `criadosNaSessao` | `number` | não | Total de cards com esta `sessaoOrigemId`. `null` na criação avulsa. |

```text
{ "flashcard": { "id": "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f" }, "criadosNaSessao": 3 }
```

#### `DeckResumo` e `RequisicaoDeck` — decks do usuário

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Identificador do deck. |
| `nome` | `string` | sim | Único por usuário, sem diferenciar maiúsculas. |
| `descricao` | `string` | não | `null` quando não informada; escrita só em F6b. |
| `totalCards` | `number` | sim | Cards `ATIVO` no deck. |

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nome` | `string` | sim | De 3 a 60 caracteres após recorte; duplicado responde `409`. |

```text
{ "id": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d", "nome": "Constitucional", "descricao": null, "totalCards": 42 }
```

#### `RequisicaoMoverCards` — corpo de `POST /flashcards/decks/{id}/mover` (CA-16)

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `flashcardIds` | `string[]` (uuid) | sim | De 1 a 200 ids. Ids que não sejam do usuário são ignorados silenciosamente — informar quais não existem revelaria cards de terceiros (RN-07). |

```text
{ "flashcardIds": ["9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f", "0e1d2c3b-4a59-4687-9a0b-1c2d3e4f5a6b"] }
```

#### `ContextoQuestaoFlashcard` — entrada do diálogo (contrato de UI com F2)

Objeto imutável que a tela de estudo entrega ao diálogo. É a **única** dependência entre as duas features no cliente: o diálogo não injeta `StudySessionStore`, não lê a rota e não conhece o cronômetro — é o que torna CA-02 estrutural.

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `questaoId` | `string` (uuid) | sim | Questão em tela. |
| `codigo` | `string` | sim | Código exibido na tarja "Vinculado à questão em tela". |
| `disciplinaId` / `disciplinaNome` | `string` | não | Pré-preenchimento exibido em campo somente leitura (CA-03). |
| `assuntoId` / `assuntoNome` | `string` | não | Idem; `null` quando a questão não tem assunto. |
| `sessaoId` | `string` (uuid) | não | Sessão corrente (RN-09); `null` fora de sessão. |
| `devolverFocoPara` | `HTMLElement` | sim | Elemento do enunciado que recebe o foco ao fechar (CA-05). |

#### `ErroApi` — envelope de erro (`application/problem+json`)

Estrutura definida em `prd-acesso-e-conta` (`code`, `message`, `fieldErrors`). Códigos usados por esta funcionalidade, todos com tipos de exceção já existentes:

| Código | HTTP | Significado |
| --- | --- | --- |
| `DADOS_INVALIDOS` | `422` | Falha de validação: frente ou verso com menos de 3 caracteres (CA-04), origem inexistente, deck de terceiro, assunto fora da disciplina. `fieldErrors` preenchido campo a campo. |
| `RECURSO_NAO_ENCONTRADO` | `404` | Card inexistente, já excluído **ou de outro usuário** (CA-15). A mensagem é a mesma nos três casos. |
| `NOME_EM_USO` | `409` | Deck com nome repetido para o mesmo usuário. |
| `ACESSO_NEGADO` | `403` | Requisição sem autenticação válida para a rota. Nunca usado para card de terceiro. |

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Não foi possível salvar o flashcard: verifique os campos destacados.",
  "fieldErrors": {
    "frente": "A frente deve ter ao menos 3 caracteres.",
    "verso": "O verso deve ter ao menos 3 caracteres."
  }
}
```

```text
{
  "code": "RECURSO_NAO_ENCONTRADO",
  "message": "Flashcard não encontrado.",
  "fieldErrors": null
}
```

> **CA-15 depende desta mensagem ser indistinguível.** Card inexistente e card de outro usuário respondem o mesmo `404` com o mesmo texto, sem `Location`, sem cabeçalho próprio e sem diferença de tempo perceptível — a consulta é a mesma em ambos os casos, porque o filtro por `usuario_id` está dentro do `where`, não em uma checagem posterior.

#### Esquema de banco — `V8__flashcards.sql`

Migração versionada e imutável em `backend/src/main/resources/db/migration`, seguindo a §5 da `TECH-SPEC.md`. `V1__base_acesso.sql` criou `usuario`; `V2__banco_questoes.sql` criou `disciplina`, `assunto` e `questao`; a migração de `prd-sessao-de-estudo` (prevista como `V4`) cria `sessao_estudo`. **O número final é confirmado na entrega**: F2, F4 e F5 ainda não aplicaram suas versões, e esta migração assume a próxima livre, sem jamais editar as anteriores.

| Tabela | Colunas principais | Observações |
| --- | --- | --- |
| `deck` | `id`, `usuario_id`, `nome`, `descricao`, `novos_dia`, `revisoes_dia`, `intervalo_max_dias`, `retencao_alvo`, `criado_em` | Nome único por usuário, sem diferenciar maiúsculas. As quatro colunas de limite nascem aqui com os padrões de RN-11 e só passam a ser editáveis em F6b — evita alterar a tabela duas vezes. |
| `flashcard` | conforme SQL abaixo | `busca` é coluna gerada `tsvector` sobre frente e verso; o bloco FSRS nasce com os padrões da §5 e só F6b o atualiza. |
| `flashcard_revisao` | `flashcard_id`, `usuario_id`, `nota`, valores de memória antes e depois, `tempo_ms`, `revisado_em` | Criada aqui, escrita por F6b. Imutável e **sem `on delete cascade`**: é o que faz CA-12 ser uma propriedade do esquema, não uma disciplina de código. |

```sql
create table deck (
  id                 uuid primary key default gen_random_uuid(),
  usuario_id         uuid not null references usuario(id),
  nome               text not null,
  descricao          text,
  novos_dia          smallint not null default 10  check (novos_dia between 0 and 100),
  revisoes_dia       smallint not null default 60  check (revisoes_dia between 5 and 400),
  intervalo_max_dias smallint not null default 180 check (intervalo_max_dias between 1 and 3650),
  retencao_alvo      numeric(3,2) not null default 0.90
                     check (retencao_alvo between 0.80 and 0.97),
  criado_em          timestamptz not null default now()
);
create unique index deck_nome_uk on deck (usuario_id, lower(nome));

create table flashcard (
  id                uuid primary key default gen_random_uuid(),
  usuario_id        uuid not null references usuario(id),
  deck_id           uuid references deck(id) on delete set null,
  frente            text not null check (length(btrim(frente)) >= 3),
  verso             text not null check (length(btrim(verso)) >= 3),
  disciplina_id     uuid references disciplina(id),
  assunto_id        uuid references assunto(id),
  questao_origem_id uuid references questao(id),
  sessao_origem_id  uuid references sessao_estudo(id),
  tags              text[] not null default '{}',
  status            text not null default 'ATIVO'
                    check (status in ('ATIVO','ARQUIVADO','EXCLUIDO')),
  estabilidade      real not null default 0,
  dificuldade       real not null default 0 check (dificuldade between 0 and 10),
  intervalo_dias    smallint not null default 0,
  repeticoes        smallint not null default 0,
  lapsos            smallint not null default 0,
  estado            text not null default 'NOVO'
                    check (estado in ('NOVO','APRENDENDO','REVISAO','REAPRENDENDO')),
  ultima_revisao    timestamptz,
  proxima_revisao   date not null default current_date,
  arquivado_em      timestamptz,
  excluido_em       timestamptz,
  criado_em         timestamptz not null default now(),
  atualizado_em     timestamptz not null default now(),
  busca             tsvector generated always as
                    (to_tsvector('portuguese', coalesce(frente, '') || ' ' || coalesce(verso, ''))) stored,
  constraint flashcard_sessao_exige_questao check (
    sessao_origem_id is null or questao_origem_id is not null),
  constraint flashcard_excluido_coerente check (
    (status = 'EXCLUIDO') = (excluido_em is not null)),
  constraint flashcard_arquivado_coerente check (
    (status = 'ARQUIVADO') = (arquivado_em is not null))
);
create index flashcard_lista_idx  on flashcard (usuario_id, criado_em desc)
  where status <> 'EXCLUIDO';
create index flashcard_fila_idx   on flashcard (usuario_id, proxima_revisao)
  where status = 'ATIVO';
create index flashcard_busca_idx  on flashcard using gin (busca);
create index flashcard_deck_idx   on flashcard (deck_id) where status <> 'EXCLUIDO';
create index flashcard_origem_idx on flashcard (sessao_origem_id) where sessao_origem_id is not null;

create table flashcard_revisao (
  id                uuid primary key default gen_random_uuid(),
  flashcard_id      uuid not null references flashcard(id),
  usuario_id        uuid not null references usuario(id),
  nota              text not null check (nota in ('ERREI','DIFICIL','BOM','FACIL')),
  dias_decorridos   smallint not null,
  estabilidade_ant  real not null,
  estabilidade_nova real not null,
  dificuldade_ant   real not null,
  dificuldade_nova  real not null,
  intervalo_novo    smallint not null,
  tempo_ms          integer not null,
  revisado_em       timestamptz not null default now()
);
create index flashcard_revisao_idx on flashcard_revisao (usuario_id, revisado_em desc);
create index flashcard_revisao_card_idx on flashcard_revisao (flashcard_id, revisado_em desc);
```

> **Os três `check` de coerência são o coração da integridade desta funcionalidade.** `flashcard_excluido_coerente` e `flashcard_arquivado_coerente` impedem que `status` e a marca temporal correspondente divirjam — a redundância existe porque a tela precisa de "quando", e o `check` é o que a torna segura. `flashcard_sessao_exige_questao` traduz RN-09: sessão de origem sem questão de origem não é um card criado na sessão, é um dado sem significado.

> **Ausência deliberada de `on delete cascade` em `flashcard_revisao`.** Nem a exclusão lógica desta entrega nem qualquer exclusão física futura podem levar o histórico junto (CA-12). Como `flashcard` nunca é apagado fisicamente, a FK sem cascata é a rede que garante isso mesmo se alguém escrever um `delete` no futuro: o banco recusa.

> **`deck_id ... on delete set null`** já sustenta RN-11 de F6b — excluir um deck move seus cards para "Sem deck" em vez de apagá-los. F6b faz isso explicitamente na transação; a cláusula é a segunda barreira.

> **Sementes.** `db/migration-dev/V906__seed_flashcards_dev.sql` cria dois decks e cerca de doze cards para o estudante de exemplo — alguns com questão de origem, um arquivado, um excluído logicamente e um com duas linhas em `flashcard_revisao`, para exercitar CA-11, CA-12 e CA-13 sem dado montado à mão. Entra apenas no perfil `dev`, pelo `spring.flyway.locations` já configurado em `prd-acesso-e-conta`, e depende das sementes `V901` (disciplinas, assuntos e questões). **O número da série `V900+` também é confirmado na entrega.**

### Endpoints da API

Base `/api/v1`. Todas as rotas exigem autenticação; **nenhuma** aceita um identificador de usuário como parâmetro — o dono é sempre o `subject` do token (RN-07). Não existe rota administrativa de flashcard em nenhum perfil.

#### Visão geral

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/flashcards` | Cria card, na sessão ou avulso (RF-28, RF-29, CA-14). |
| `GET` | `/api/v1/flashcards` | Lista paginada com busca, filtros, ordenação e indicadores (RF-31). |
| `GET` | `/api/v1/flashcards/{id}` | Um card do próprio usuário; sustenta o *deep link* `/flashcards/:id`. |
| `PUT` | `/api/v1/flashcards/{id}` | Edita frente, verso, deck, tags e — em card avulso — disciplina e assunto (RF-32). |
| `DELETE` | `/api/v1/flashcards/{id}` | Exclusão lógica preservando o histórico (CA-12). |
| `POST` | `/api/v1/flashcards/{id}/arquivar` | Retira o card da fila sem apagá-lo (CA-11). |
| `POST` | `/api/v1/flashcards/{id}/restaurar` | Devolve um card arquivado ao estado ativo (CA-11). |
| `GET` | `/api/v1/flashcards/decks` | Decks do usuário, para o seletor e o filtro. |
| `POST` | `/api/v1/flashcards/decks` | Cria deck por nome (RF-32). |
| `POST` | `/api/v1/flashcards/decks/{id}/mover` | Move um lote de cards para o deck (CA-16). |

---

#### `POST /api/v1/flashcards`

Cria um flashcard. É a rota do modal da sessão (RF-28/RF-29) e da criação avulsa (CA-14) — a diferença está apenas na presença de `questaoOrigemId`.

**Corpo**

`RequisicaoFlashcard`, documentado em *Modelos de dados*.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `RespostaFlashcardCriado` | Card criado; `Location` aponta para `/api/v1/flashcards/{id}`. |
| `422` | `ErroApi` | Frente ou verso com menos de 3 caracteres (CA-04); questão, sessão, deck, disciplina ou assunto inválidos. |
| `401` | — | Sem token válido. |

**Exemplo — criação dentro da sessão (CA-03, CA-05, CA-08)**

```http
POST /api/v1/flashcards
```

```text
{
  "flashcard": {
    "id": "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f",
    "frente": "Efeitos da decisão de mérito em ADI",
    "verso": "Eficácia erga omnes e efeito vinculante (art. 102, §2º, CF).",
    "deck": { "id": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d", "nome": "Constitucional" },
    "disciplina": { "id": "6a1c0f2e-9d3b-4a10-8f22-71c5b0e4d901", "nome": "Direito Constitucional" },
    "assunto": { "id": "b2d4e6f8-1a3c-4b5d-8e7f-0a9c1b2d3e4f", "nome": "Controle de constitucionalidade" },
    "tags": ["adi", "stf"],
    "estado": "NOVO",
    "status": "ATIVO",
    "proximaRevisao": "2026-08-30",
    "origem": {
      "questaoId": "1b0f7f0a-5c2f-4f0e-9a51-2a4d3e7c9b10",
      "codigo": "Q-88409",
      "sessaoId": "7c8d9e0f-1a2b-4c3d-8e5f-6a7b8c9d0e1f"
    },
    "criadoEm": "2026-08-30T14:02:11Z"
  },
  "criadosNaSessao": 3
}
```

> Disciplina e assunto do corpo são **ignorados** quando há `questaoOrigemId`: a origem é a fonte da verdade taxonômica (CA-03). O cliente exibe os dois campos em modo somente leitura justamente por isso.

**Exemplo — validação de tamanho (CA-04)**

```http
POST /api/v1/flashcards
```

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Não foi possível salvar o flashcard: verifique os campos destacados.",
  "fieldErrors": { "frente": "A frente deve ter ao menos 3 caracteres." }
}
```

> O cliente pinta o erro no campo correspondente e **não fecha o modal nem perde o texto digitado** — o mesmo mapeamento de `fieldErrors` já usado em `prd-acesso-e-conta`.

**Exemplo — origem inválida**

```http
POST /api/v1/flashcards
```

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Não foi possível salvar o flashcard: verifique os campos destacados.",
  "fieldErrors": { "sessaoOrigemId": "Sessão de estudo não encontrada." }
}
```

> Sessão de **outro usuário** e sessão inexistente devolvem a mesma mensagem, pelo mesmo motivo de CA-15.

---

#### `GET /api/v1/flashcards`

Lista paginada dos cards do próprio usuário, com busca textual, filtros combinados, ordenação e os indicadores globais (RF-31, CA-09, CA-10).

**Parâmetros de consulta**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `q` | `string` | — | Busca textual em português sobre frente e verso, por `websearch_to_tsquery` sobre a coluna `busca`. Termo em branco é tratado como ausente. |
| `deck` | `uuid` \| `sem-deck` | — | Filtra por deck; o valor literal `sem-deck` filtra `deck_id is null`. |
| `disciplina` | `uuid` | — | Filtra por disciplina. |
| `estado` | `string` | — | `NOVO`, `APRENDENDO`, `REVISAO` ou `REAPRENDENDO`. |
| `arquivados` | `boolean` | `false` | Quando `true`, lista os arquivados em vez dos ativos (CA-11). Cards excluídos nunca aparecem. |
| `page` | `number` | `0` | Base zero. |
| `size` | `number` | `20` | Máximo 100. |
| `sort` | `string` | `criadoEm,desc` | Lista branca: `criadoEm` e `proximaRevisao`, com `asc` ou `desc`. Valor fora dela responde `422`. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `PaginaFlashcards` | Sempre que a consulta for válida, inclusive sem resultados. |
| `422` | `ErroApi` | `sort` fora da lista branca, `estado` desconhecido ou `size` acima do máximo. |

**Exemplo — busca e filtros combinados (CA-10)**

```http
GET /api/v1/flashcards?q=controle%20concentrado&deck=3a2b1c0d-…&estado=NOVO&sort=proximaRevisao,asc
```

```text
{
  "content": [ { "id": "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f" } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "indicadores": { "vencendoHoje": 12, "proximosSeteDias": 34, "emFixacao": 9, "total": 128, "decks": 4 }
}
```

**Exemplo — nenhum card corresponde ao filtro**

```http
GET /api/v1/flashcards?q=xyz
```

```text
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "indicadores": { "vencendoHoje": 12, "proximosSeteDias": 34, "emFixacao": 9, "total": 128, "decks": 4 }
}
```

> Os indicadores continuam preenchidos com lista vazia: eles descrevem o acervo, não o filtro. É o que permite ao cliente distinguir "você não tem cards" (`total` zero → estado vazio de boas-vindas) de "o filtro não achou nada" (`total` maior que zero → estado vazio do protótipo, "Nenhum flashcard com esses filtros").

---

#### `GET /api/v1/flashcards/{id}`

Um card do próprio usuário. Existe para o *deep link* `/flashcards/:id` sobreviver a uma recarga e é onde a privacidade de leitura é provada (CA-15).

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `FlashcardResumo` | Card do usuário, ativo ou arquivado. |
| `404` | `ErroApi` | Card inexistente, excluído **ou de outro usuário** — resposta idêntica nos três casos. |

---

#### `PUT /api/v1/flashcards/{id}`

Edita o card (RF-32). Frente, verso, deck e tags são sempre editáveis; disciplina e assunto só em card avulso, porque em card com origem eles são derivados da questão. `questaoOrigemId` e `sessaoOrigemId` são imutáveis — enviá-los diferentes não altera nada e não é erro.

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `FlashcardResumo` | Card atualizado. |
| `422` | `ErroApi` | Mesmas validações do `POST`. |
| `404` | `ErroApi` | Card inexistente, excluído ou de outro usuário. |

> A edição **não toca** em nenhuma coluna do bloco FSRS nem em `proxima_revisao`: corrigir um erro de digitação não pode reagendar o card nem apagar o que já foi aprendido sobre ele.

---

#### `DELETE /api/v1/flashcards/{id}`

Exclusão lógica: `status` vai a `EXCLUIDO`, `excluido_em` recebe o instante, e nada mais acontece (CA-12).

| Status | Corpo | Quando |
| --- | --- | --- |
| `204` | — | Card excluído. |
| `404` | `ErroApi` | Card inexistente, já excluído ou de outro usuário. |

> As linhas de `flashcard_revisao` permanecem, e a porta `ConsultaFilaFlashcards` deixa de enxergar o card no mesmo instante. Não há rota de restauração de card excluído nesta v1 — arquivar é a operação reversível; excluir é definitiva do ponto de vista do usuário, e lógica apenas do ponto de vista do dado.

---

#### `POST /api/v1/flashcards/{id}/arquivar` e `POST /api/v1/flashcards/{id}/restaurar`

Alternam `status` entre `ATIVO` e `ARQUIVADO`, preenchendo ou limpando `arquivado_em` (CA-11). Sem corpo.

| Status | Corpo | Quando |
| --- | --- | --- |
| `204` | — | Transição aplicada. |
| `404` | `ErroApi` | Card inexistente, excluído ou de outro usuário. |
| `409` | `ErroApi` | Transição sem efeito (arquivar um arquivado, restaurar um ativo). |

> O card arquivado **continua na listagem** com `?arquivados=true` e some da porta da fila — a combinação é exatamente o enunciado de CA-11.

---

#### `GET /api/v1/flashcards/decks` e `POST /api/v1/flashcards/decks`

Listagem e criação por nome. O restante do CRUD de deck (renomear, descrever, excluir, limites) é de `prd-flashcards-decks-e-revisao` (RF-35/RF-36).

**Respostas de `GET`**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `DeckResumo[]` | Decks do usuário, ordenados por nome; lista vazia quando não houver. |

**Corpo e respostas de `POST`**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `DeckResumo` | Deck criado com os limites padrão de RN-11. |
| `409` | `ErroApi` | `NOME_EM_USO` — nome repetido para o mesmo usuário, ignorando maiúsculas. |
| `422` | `ErroApi` | Nome com menos de 3 ou mais de 60 caracteres. |

```http
POST /api/v1/flashcards/decks
```

```text
{ "id": "3a2b1c0d-9e8f-4a7b-8c6d-5e4f3a2b1c0d", "nome": "Constitucional", "descricao": null, "totalCards": 0 }
```

> "Sem deck" **não é uma linha** em `deck`: é `deck_id is null`, com os padrões das colunas de limite. Criar um deck chamado "Sem deck" é permitido e não conflita — o filtro usa o valor literal `sem-deck`, não o nome.

---

#### `POST /api/v1/flashcards/decks/{id}/mover`

Move um lote de cards para o deck (CA-16). Operação de organização: um `update` de `deck_id` e nada mais.

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `{ "movidos": 12 }` | Quantidade efetivamente movida — só os cards do usuário e não excluídos. |
| `404` | `ErroApi` | Deck inexistente ou de outro usuário. |
| `422` | `ErroApi` | `flashcardIds` vazio ou com mais de 200 itens. |

```http
POST /api/v1/flashcards/decks/3a2b1c0d-…/mover
```

```text
{ "movidos": 12 }
```

> **Nenhuma coluna de agendamento é tocada** — é a segunda metade literal de CA-16, garantida por um `update` que nomeia apenas `deck_id` e `atualizado_em`, e verificada por TI-22. Mover para "Sem deck" em lote não tem rota: o caso individual é atendido pelo `PUT` com `deckId: null`, e o caso em massa é consequência da exclusão de deck em F6b (RN-11).

---

## Pontos de integração

Nenhuma integração externa: sem rede a terceiros, sem timeout, sem retentativa e sem idempotência de integração a especificar. As integrações são internas, e a mais importante delas é de **interface**, não de servidor.

| Integração | Direção | Contrato |
| --- | --- | --- |
| `prd-sessao-de-estudo` → flashcards (UI) | A barra de ações da questão renderiza o botão "Novo flashcard" nos três modos e chama `DialogService.abrir(FlashcardDialog, contexto)`. | `ContextoQuestaoFlashcard`, documentado em *Modelos de dados*. **A tela de estudo não importa nada do diálogo além dessa função e desse tipo**, e o diálogo não importa nada da tela de estudo. O contador exibido no botão vem do `criadosNaSessao` devolvido pelo `POST`, propagado pelo evento `criado`. |
| flashcards → `estudo` (banco) | `sessao_origem_id` referencia `sessao_estudo(id)` (RN-09). | Chave estrangeira. A validação de que a sessão é do usuário é feita por serviço de `estudo`; enquanto F2 não existir, `ResolvedorContextoFlashcard` consulta a tabela por uma porta `SessaoDoUsuario` de uma linha, substituída pelo serviço de F2 na entrega dele. |
| flashcards → `questao` | Leitura de `codigo`, `disciplina_id` e `assunto_id` da questão de origem, e verificação de que ela está publicada (RN-01). | Por serviço de `questao`, nunca pelo repositório de outro domínio (`code-standards.md` §3). |
| flashcards → `catalogo` | Disciplina e assunto do card avulso vêm da árvore de matérias (RF-20). | Leitura por serviço; o cliente alimenta os seletores com `GET /admin/disciplinas`, já aberto a autenticados na F1. |
| flashcards → `estudo` (CA-13) | "Ver questão de origem" navega para a tela de estudo com a questão indicada. | Rota `/estudar?questao={id}`, de F2. Enquanto ela não existir, o link fica desabilitado com rótulo explicativo — o dado (`origem.questaoId`) já é entregue. |
| F6b → flashcards | `prd-flashcards-decks-e-revisao` monta a fila a partir de `ConsultaFilaFlashcards.elegiveis(usuarioId, ate)` e escreve em `flashcard_revisao` e no bloco FSRS de `flashcard`. | Porta de leitura definida aqui e coberta por TI-19 e TI-20. F6b **não cria migração de tabela**; acrescenta apenas comportamento. |

## Abordagem de testes

Meta de cobertura: **mínimo de 80%**, conforme `.agents/rules/tests.md` §1, que prevalece sobre o texto de 70% da `TECH-SPEC.md` §9. Ferramentas: JUnit Jupiter, AssertJ, Mockito e Testcontainers 2.0.5 (versões gerenciadas pelo BOM do Spring Boot 4.1.1); Vitest e Playwright no frontend. Proporção alvo da pirâmide: ~70% unidade, ~20% integração, ~10% E2E.

Pela tabela de criticidade de `tests.md` §1, o que é de **prioridade alta** aqui é a autorização por dono (RN-07/CA-15) — a única regra da funcionalidade cuja falha vaza dado pessoal de terceiro — e a preservação do histórico na exclusão (CA-12). Em seguida vem a preservação da sessão (CA-02), que é a promessa central do PRD e só é provável ponta a ponta. Edição, arquivamento e movimentação de deck são CRUD e recebem o essencial. Todo teste com data usa `Clock` fixo: `proxima_revisao` é `current_date` e um teste que rodasse na virada do dia falharia sozinho (`tests.md` §2, *Repeatable*).

### Testes de unidade

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `VerificadorDonoFlashcard` com card do próprio usuário | CA-15 | Devolve a entidade; uma única consulta, já filtrada por `usuarioId`. |
| TU-02 | `VerificadorDonoFlashcard` com card de outro usuário | CA-15 | Lança `RecursoNaoEncontradoException` com `RECURSO_NAO_ENCONTRADO`; nunca `ACESSO_NEGADO`. |
| TU-03 | `VerificadorDonoFlashcard` com card já excluído | CA-12, CA-15 | Mesma exceção e mesma mensagem do card inexistente. |
| TU-04 | `NormalizadorTags` com maiúsculas, espaços e duplicatas | RF-29 | `[" ADI ", "adi", "STF"]` vira `["adi", "stf"]`, na ordem de entrada. |
| TU-05 | `NormalizadorTags` acima de 10 tags | RF-29 | Lança `DadosInvalidosException` com `fieldErrors.tags`; nada é truncado silenciosamente. |
| TU-06 | `NormalizadorTags` com nulo, vazios e só espaços | RF-29 | Lista vazia, sem exceção; nunca `null`. |
| TU-07 | `FlashcardService.criar` a partir de uma questão | CA-05 | Persiste com `status` `ATIVO`, `estado` `NOVO`, `proximaRevisao` igual à data do `Clock` fixo e o bloco FSRS zerado. |
| TU-08 | `FlashcardService.criar` herdando a taxonomia da origem | CA-03 | `disciplinaId` e `assuntoId` vêm da questão, mesmo quando o corpo envia outros valores. |
| TU-09 | `FlashcardService.criar` com frente de 2 caracteres | CA-04 | `DadosInvalidosException` com `fieldErrors.frente`; nada persistido. |
| TU-10 | `FlashcardService.criar` com verso só de espaços | CA-04 | `fieldErrors.verso` — o recorte de espaços acontece antes da medida de tamanho. |
| TU-11 | `FlashcardService.criar` avulso | CA-14 | Sem questão nem sessão de origem; disciplina, assunto e deck vêm do corpo. |
| TU-12 | `FlashcardService.criar` devolve o total da sessão | CA-08 | `criadosNaSessao` é a contagem por `sessaoOrigemId`, não um contador incrementado em memória. |
| TU-13 | `FlashcardService.criar` avulso não conta sessão | CA-08 | `criadosNaSessao` é `null`, não zero. |
| TU-14 | `FlashcardService.atualizar` frente, verso, deck e tags | RF-32 | Os quatro campos mudam; `estabilidade`, `dificuldade`, `estado` e `proximaRevisao` permanecem idênticos. |
| TU-15 | `FlashcardService.atualizar` tentando trocar a origem | CA-13 | `questaoOrigemId` e `sessaoOrigemId` permanecem os originais, sem erro. |
| TU-16 | `FlashcardService.arquivar` card ativo | CA-11 | `status` `ARQUIVADO` e `arquivadoEm` preenchido com o `Clock` fixo. |
| TU-17 | `FlashcardService.arquivar` card já arquivado | CA-11 | `ConflitoException`; nenhum efeito colateral. |
| TU-18 | `FlashcardService.restaurar` card arquivado | CA-11 | `status` volta a `ATIVO` e `arquivadoEm` volta a `null`. |
| TU-19 | `FlashcardService.excluir` card com histórico | CA-12 | `status` `EXCLUIDO` e `excluidoEm` preenchido; nenhum `delete` é chamado no repositório de revisões. |
| TU-20 | `FlashcardService.excluir` card de outro usuário | CA-15 | `RecursoNaoEncontradoException`; o repositório de escrita não é chamado. |
| TU-21 | `ResolvedorContextoFlashcard` com questão não publicada | CA-03 | `DadosInvalidosException` com `fieldErrors.questaoOrigemId`. |
| TU-22 | `ResolvedorContextoFlashcard` com sessão de outro usuário | CA-13 | `fieldErrors.sessaoOrigemId`, com a mesma mensagem da sessão inexistente. |
| TU-23 | `ResolvedorContextoFlashcard` com sessão sem questão | CA-13 | Recusa: sessão de origem sem questão de origem é dado sem significado (RN-09). |
| TU-24 | `ResolvedorContextoFlashcard` sem nenhuma origem | CA-14 | Contexto vazio, sem consulta a `questao` nem a `sessao_estudo`. |
| TU-25 | `ConsultaFlashcardsService` calcula os quatro indicadores | CA-09 | Vencendo hoje, próximos 7 dias, em fixação e total conferem sobre um acervo montado com `Clock` fixo. |
| TU-26 | `ConsultaFlashcardsService` com indicadores sob filtro | CA-09, CA-10 | Os indicadores não mudam ao aplicar busca ou filtro; só `content` e `totalElements` mudam. |
| TU-27 | `ConsultaFlashcardsService` com `sort` fora da lista branca | CA-10 | `DadosInvalidosException`; a ordenação nunca chega ao SQL como texto livre. |
| TU-28 | `DeckService.criar` com nome repetido ignorando maiúsculas | RF-32 | `ConflitoException` `NOME_EM_USO`. |
| TU-29 | `DeckService.criar` com nome de 2 caracteres | RF-32 | `DadosInvalidosException` com `fieldErrors.nome`. |
| TU-30 | `DeckService.mover` com ids de outro usuário na lista | CA-15, CA-16 | Só os cards do usuário são movidos; `movidos` reflete o número real, sem revelar quais ids foram descartados. |
| TU-31 | `FlashcardFormStore` bloqueia o envio com frente curta (Vitest) | CA-04 | Nenhuma requisição é feita; o erro aparece no campo `frente`. |
| TU-32 | `FlashcardFormStore.salvarECriarOutro` (Vitest) | CA-06 | Frente e verso ficam vazios; deck, disciplina, assunto e tags permanecem; o diálogo continua aberto e o foco volta à frente. |
| TU-33 | `FlashcardFormStore.cancelar` com texto digitado (Vitest) | CA-07 | Estado de confirmação ativo; o diálogo **não** fecha nem descarta antes da confirmação. |
| TU-34 | `FlashcardFormStore` com `Esc` e campos preenchidos (Vitest) | CA-07 | Comportamento idêntico ao de Cancelar — a tecla não é um atalho de descarte. |
| TU-35 | `FlashcardFormStore.cancelar` com campos vazios (Vitest) | CA-07 | Fecha direto, sem confirmação — confirmar o descarte de nada seria ruído. |
| TU-36 | `FlashcardFormStore` atualiza o contador pela resposta (Vitest) | CA-08 | `criadosNaSessao` assume o valor devolvido pelo servidor, não um incremento local. |
| TU-37 | `FlashcardFormStore` mapeia 422 campo a campo (Vitest) | CA-04 | Cada `fieldError` marca seu campo; o texto digitado permanece e o diálogo segue aberto. |
| TU-38 | `rascunho-flashcard` grava, restaura e limpa (Vitest) | CA-07 | Grava sob `fc-draft:{questaoId}`, restaura ao reabrir e limpa no salvamento e no descarte confirmado. |
| TU-39 | `DialogService` devolve o foco ao fechar (Vitest) | CA-05 | O elemento de `devolverFocoPara` recebe o foco; o foco inicial fica na frente ao abrir. |
| TU-40 | `DialogService` confina o `Tab` no diálogo (Vitest) | Experiência do usuário | O percurso circula entre os controles do diálogo e não alcança a tela ao fundo. |
| TU-41 | `FlashcardsStore` combina busca e filtros (Vitest) | CA-10 | Uma requisição com todos os parâmetros; a digitação usa temporizador falso, sem espera real. |
| TU-42 | `FlashcardsStore` distingue acervo vazio de filtro vazio (Vitest) | CA-09 | `total` zero leva ao estado de boas-vindas; `total` maior que zero com `content` vazio leva a "Nenhum flashcard com esses filtros". |
| TU-43 | `FlashcardsPage` exibe estado por rótulo textual (Vitest) | CA-09 | "Novo", "Aprendendo", "Em revisão" aparecem como texto, não só como cor. |
| TU-44 | `MoverParaDeck` envia apenas os selecionados (Vitest) | CA-16 | O corpo contém exatamente os ids marcados; a ação fica desabilitada com seleção vazia. |
| TU-45 | `ToastStore` publica e expira a mensagem (Vitest) | CA-05 | A mensagem aparece com `role="status"` e some após o tempo previsto, sem bloquear a tela. |

Componentes cobertos: `VerificadorDonoFlashcard`, `NormalizadorTags`, `ResolvedorContextoFlashcard`, `FlashcardService`, `ConsultaFlashcardsService`, `DeckService`, `FlashcardFormStore`, `FlashcardsStore`, `DialogService`, `ToastStore`, `rascunho-flashcard`, `MoverParaDeck` e `FlashcardsPage`. Serviços puros com JUnit + Mockito, sem contexto Spring; mocks apenas na fronteira (`FlashcardRepository`, `DeckRepository`, os serviços de `questao` e `estudo`). `VerificadorDonoFlashcard` e `NormalizadorTags` existem separados justamente para serem provados em milissegundos — o primeiro é a regra mais crítica do documento. No frontend, HTTP sempre por `HttpTestingController`, nunca `fetch` real, e temporizadores falsos na busca com atraso de digitação.

### Testes de integração

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | Migração em base limpa (`@DataJpaTest`) | — | Flyway aplica sem erro; `deck`, `flashcard` e `flashcard_revisao`, os índices e os cinco `check` existem, e o Hibernate valida o esquema. |
| TI-02 | `check flashcard_excluido_coerente` (`@DataJpaTest`) | CA-12 | `update` que zera `excluido_em` mantendo `status` `EXCLUIDO` é recusado pelo banco. |
| TI-03 | `check flashcard_sessao_exige_questao` (`@DataJpaTest`) | CA-13 | Inserir sessão de origem sem questão de origem é recusado. |
| TI-04 | Índice único de deck por usuário (`@DataJpaTest`) | RF-32 | "constitucional" e "Constitucional" colidem para o mesmo usuário; o mesmo nome em outro usuário é aceito. |
| TI-05 | Coluna gerada `busca` e índice GIN (`@DataJpaTest`) | CA-10 | Buscar "decisões" encontra o card que escreve "decisão"; a coluna é preenchida sem código de aplicação. |
| TI-06 | `POST /flashcards` dentro de uma sessão | CA-01, CA-05 | 201 com `Location`; a linha guarda questão e sessão de origem (RN-09). |
| TI-07 | `POST /flashcards` com frente de 2 caracteres | CA-04 | 422 `DADOS_INVALIDOS` com `fieldErrors.frente`; nada persistido. |
| TI-08 | `POST /flashcards` avulso | CA-14 | 201 com `origem` nulo e a taxonomia informada pelo corpo. |
| TI-09 | `POST /flashcards` com sessão de outro usuário | CA-13, CA-15 | 422 com a mesma mensagem da sessão inexistente. |
| TI-10 | `POST /flashcards` com sessão em Modo Prova aberta | CA-01 | 201 — a criação é permitida nos três modos e não interfere na finalização da sessão. |
| TI-11 | `GET /flashcards` com acervo montado | CA-09 | 200 com página e `indicadores` conferindo com o acervo. |
| TI-12 | `GET /flashcards?q=` com termo em português | CA-10 | Encontra por radical e acentuação sobre frente e verso; termo em branco é ignorado. |
| TI-13 | `GET /flashcards` com filtros combinados e ordenação | CA-10 | Deck, disciplina, estado e busca aplicados juntos; `sort=proximaRevisao,asc` respeitado. |
| TI-14 | `GET /flashcards?arquivados=true` | CA-11 | Lista os arquivados; os ativos ficam de fora e os excluídos não aparecem em nenhum dos dois modos. |
| TI-15 | `GET /flashcards?sort=frente,asc` | CA-10 | 422 — a lista branca de ordenação é aplicada antes do SQL. |
| TI-16 | `GET /flashcards/{id}` de card de outro usuário | CA-15 | 404 com corpo byte a byte igual ao do id inexistente. |
| TI-17 | `PUT /flashcards/{id}` de card de outro usuário | CA-15 | 404; a linha do dono permanece intacta na base. |
| TI-18 | `DELETE /flashcards/{id}` de card de outro usuário | CA-15 | 404; nenhuma coluna alterada. |
| TI-19 | Arquivar e consultar a porta da fila | CA-11 | O card some de `ConsultaFilaFlashcards.elegiveis`, continua acessível por `?arquivados=true` e volta à fila depois de restaurado. |
| TI-20 | Excluir card com duas revisões registradas | CA-12 | 204; o card some da lista e da porta da fila; as duas linhas de `flashcard_revisao` continuam lá. |
| TI-21 | `GET /flashcards` traz a origem com o código da questão | CA-13 | `origem.codigo` no formato `Q-#####`, pronto para o link "Criado na questão". |
| TI-22 | `POST /flashcards/decks/{id}/mover` com 12 cards | CA-16 | `movidos` igual a 12; `deck_id` alterado e `estabilidade`, `dificuldade`, `estado`, `intervalo_dias` e `proxima_revisao` idênticos aos de antes. |
| TI-23 | `POST /flashcards/decks/{id}/mover` com id de terceiro na lista | CA-15, CA-16 | Os ids alheios são ignorados; `movidos` é menor que a lista enviada e nenhum card de terceiro muda. |
| TI-24 | `POST /flashcards/decks` com nome repetido | RF-32 | 409 `NOME_EM_USO`; o índice único fecha a corrida entre requisições simultâneas. |
| TI-25 | Todas as rotas sem token | CA-15 | 401 em todas; nenhuma responde 200 nem 404 antes de autenticar. |
| TI-26 | Super-admin lê, edita e exclui card de estudante | CA-15 | 404 nas três tentativas, idêntico ao de um estranho — não existe caminho administrativo para o conteúdo do card. |
| TI-27 | Listagem de 500 cards paginada (contador de consultas) | RF-31 | Uma consulta de página mais uma de contagem e uma de indicadores; sem N+1 em deck, disciplina, assunto e questão de origem. |

Componentes testados em conjunto: controller + segurança + serviço + repositório + Flyway sobre PostgreSQL 18 real via Testcontainers (`@ServiceConnection`), sem H2 — coluna gerada `tsvector`, índice GIN, `text[]`, índice único com `lower()` e os `check` de coerência só existem no Postgres. Fatia mais estreita por cenário: `@DataJpaTest` em TI-01…TI-05, `@WebMvcTest` com `MockMvcTester` nos cenários de contrato e privacidade, e `@SpringBootTest` apenas em TI-06, TI-19, TI-20, TI-22 e TI-26, que atravessam escrita e leitura. Dados criados por *builders* no pacote de teste, com e-mail e nome de deck isolados por `UUID.randomUUID()`. TI-26 é o teste que não pode faltar: é o único que prova que o perfil mais poderoso do sistema não tem acesso ao dado mais pessoal dele.

**Dependência de F2.** TI-03, TI-06, TI-09, TI-10 e TI-21 precisam de `sessao_estudo`, que pertence a `prd-sessao-de-estudo`. Enquanto aquela funcionalidade não estiver implementada, a tabela vem de `db/migration-test/V991__sessao_resposta_marcacao_minima.sql`, já previsto por `prd-cadernos-e-desempenho` e restrito ao *classpath* de teste — o mesmo padrão adotado por `prd-banco-de-questoes` com `V990`. Se aquela migração de teste ainda não existir na entrega, ela nasce aqui com o mesmo nome e conteúdo. Quando F2 criar a tabela definitiva, as migrações de teste são removidas na mesma entrega.

### Testes E2E

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | Abrir o modal a partir da questão nos três modos | CA-01 | O modal abre sobre a questão; a URL não muda e a tela não recarrega. |
| E2E-02 | Abrir e fechar o modal na questão 12 com alternativa marcada | CA-02 | Continua na questão 12, com a mesma alternativa marcada, e o cronômetro segue correndo — o valor após o fechamento é maior que o de antes, nunca reiniciado. |
| E2E-03 | Conferir o pré-preenchimento do modal | CA-03 | Disciplina e assunto da questão aparecem preenchidos e a tarja "Vinculado à questão em tela" exibe o código. |
| E2E-04 | Tentar salvar com frente de 2 caracteres | CA-04 | Erro inline no campo; o modal permanece aberto e o verso digitado continua lá. |
| E2E-05 | Salvar e fechar | CA-05 | O modal fecha, o toast de confirmação aparece e o foco volta ao enunciado da questão. |
| E2E-06 | Salvar e criar outro, duas vezes seguidas | CA-06 | Frente e verso limpos a cada salvamento, deck e taxonomia preservados, modal sempre aberto. |
| E2E-07 | Pressionar `Esc` com o formulário preenchido | CA-07 | Confirmação exigida; "Continuar editando" devolve o texto intacto e "Descartar" fecha sem criar card. |
| E2E-08 | Criar três cards na mesma sessão | CA-08 | O botão "Novo flashcard" exibe o contador 3; recarregar a página mantém o número. |
| E2E-09 | Abrir Flashcards pelo menu | CA-09 | Lista paginada com frente resumida, deck, disciplina/assunto, estado e próxima revisão, mais a faixa de indicadores. |
| E2E-10 | Combinar busca, filtros e ordenação | CA-10 | Só os cards correspondentes aparecem; ordenar por próxima revisão reordena a tabela; sem correspondência aparece o estado vazio de filtro. |
| E2E-11 | Arquivar um card e restaurá-lo | CA-11 | Sai da lista de ativos, aparece nos arquivados, volta ao ser restaurado. |
| E2E-12 | Excluir um card já revisado, com confirmação | CA-12 | Diálogo de confirmação; depois de confirmar, o card some da lista e não volta ao recarregar. |
| E2E-13 | Abrir a questão de origem a partir da lista | CA-13 | O link leva à questão correspondente na tela de estudo. |
| E2E-14 | Criar um card avulso pelo menu Flashcards | CA-14 | Salvo sem questão de origem, com deck, disciplina e assunto escolhidos por mim. |
| E2E-15 | Mover uma seleção de cards para outro deck | CA-16 | Todos passam ao deck escolhido e a coluna de próxima revisão de cada um permanece a mesma de antes. |
| E2E-16 | Percurso por teclado e contraste no modal e na lista | Experiência do usuário | `Tab` circula apenas dentro do diálogo, `Esc` cancela, o foco é visível, os estados têm rótulo textual além da cor e o contraste é de ao menos 4,5:1. |

Execução com Playwright contra o SPA e a API reais, com PostgreSQL em contêiner e a semente de `dev`. Os seletores saem da marcação do protótipo traduzida 1:1 (`.dialog`, `.dialog-backdrop`, `.dialog-actions`, `.field`, `.input`, `.table`, `.tag`, `.btn-primary`). E2E-01…E2E-08 e E2E-13 dependem da tela de estudo de F2 e ficam bloqueados até ela existir; E2E-02 é o teste mais importante da suíte, porque é o único lugar onde a promessa central do PRD — a sessão sobrevive intacta — é observável de verdade.

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Design: fechar as três lacunas do protótipo** — o protótipo já traz a tela "Meus flashcards", o modal de criação e o botão na barra da questão, mas falta (a) a multisseleção de linhas com a ação "Mover para deck" (CA-16), (b) a opção "+ Novo deck" no seletor do modal e (c) o modo avulso do modal, em que disciplina e assunto são seletores editáveis em vez de campos desabilitados (CA-14). Primeiro porque a regra 1 do `AGENTS.md` impede que qualquer marcação nasça direto no Angular.
2. **Migração e persistência** — `V8__flashcards.sql`, a semente de `dev`, as entidades `Flashcard` e `Deck`, os dois enums e os repositórios, com TI-01…TI-05. Antes de qualquer regra, porque o esquema sustenta as duas features de flashcards.
3. **Regras puras** — `VerificadorDonoFlashcard` e `NormalizadorTags`, com TU-01…TU-06. São as duas regras que não dependem de Spring, e a primeira é a mais crítica do documento.
4. **Contexto de origem** — `ResolvedorContextoFlashcard` e a porta `SessaoDoUsuario`, com TU-21…TU-24. Antes do serviço, porque a criação depende dele.
5. **Serviço de escrita** — `FlashcardService` com criar, editar, arquivar, restaurar e excluir, com TU-07…TU-20.
6. **Consulta e indicadores** — `ConsultaFlashcardsService`, `FiltroFlashcards` e `ConsultaFilaFlashcards`, com TU-25…TU-27.
7. **Decks** — `DeckService` e `DeckRepository` com listar, criar e mover, com TU-28…TU-30.
8. **Controllers** — `FlashcardController` e `DeckController`, com TI-06…TI-27. É aqui que a privacidade sai do serviço e vira contrato observável.
9. **Infraestrutura de UI** — `shared/ui/dialog` (armadilha de foco, `Esc`, devolução de foco) e `shared/ui/toast` com o `ToastStore`, com TU-39, TU-40 e TU-45. Antes do modal, porque ele é o primeiro cliente das duas.
10. **Modal de criação** — `flashcard-api`, `modelos-flashcard`, `FlashcardDialog`, `FlashcardFormStore`, `rascunho-flashcard` e `DeckSeletor`, com TU-31…TU-38. Em tradução 1:1 do protótipo aprovado no passo 1.
11. **Tela de gerenciamento** — `FlashcardsPage`, `FlashcardsFiltros`, `FlashcardsIndicadores`, `FlashcardsStore`, `MoverParaDeck`, o item de menu no shell e as rotas sob `authGuard`, com TU-41…TU-44.
12. **Integração com a sessão de estudo** — o botão na barra de ações e a passagem do `ContextoQuestaoFlashcard`, com E2E-01…E2E-08 e E2E-13. Depende de F2; até lá, o modal é exercitado pela criação avulsa do passo 11.
13. **E2E, endurecimento e documentos** — E2E-09…E2E-12 e E2E-14…E2E-16, verificação da cobertura por `./mvnw verify` e `npm run test:coverage`, e atualização da `TECH-SPEC.md`: §5 com as três tabelas e §6 com as rotas que esta entrega acrescenta ao contrato (`GET /flashcards/{id}`, `POST /flashcards/{id}/restaurar`, `GET` e `POST /flashcards/decks`). Regra 4 do `AGENTS.md`; entra na mesma entrega, não depois.

### Dependências técnicas

| Dependência | Situação | Impacto |
| --- | --- | --- |
| `prd-acesso-e-conta` (`SegurancaConfig`, `authGuard`, shell, `styles.css`, interceptor) | **Implementado** | Nenhum bloqueio. O shell recebe apenas o item de menu novo. |
| `prd-banco-de-questoes` — tabelas `disciplina`, `assunto` e `questao` | Especificado, ainda não implementado | **Bloqueia o passo 2.** Sem elas não há FK de taxonomia nem de questão de origem, e CA-03 e CA-13 perdem o sentido. |
| `prd-banco-de-questoes` — `RecursoNaoEncontradoException`, `DadosInvalidosException`, `ConflitoException`, `PaginaResposta<T>`, `confirmacao-dialog` | Especificado, ainda não implementado | Bloqueia os passos 5, 8 e 11. Nenhum deles é reimplementado aqui; se a F1 atrasar, nascem com a mesma assinatura e são unificados na entrega dela. |
| `prd-sessao-de-estudo` — tabela `sessao_estudo`, `StudySessionStore`, barra de ações da questão e rota `/estudar?questao=` | **Sem TechSpec ainda** — só PRD | **Bloqueia o passo 12 e nove dos dezesseis E2E**, entre eles CA-01, CA-02 e CA-08. Os passos 2–11 avançam sobre `db/migration-test/V991`; o contrato de UI está fixado em *Pontos de integração* para que a TechSpec de F2 o incorpore em vez de inventar outro. É a dependência mais séria da entrega. |
| `prd-flashcards-decks-e-revisao` (F6b) | Só PRD | Não bloqueia — a direção é a oposta: F6b depende do esquema e da porta `ConsultaFilaFlashcards` entregues aqui. |
| Numeração de migração (`V8`, `V906`) | Em aberto — F2, F4 e F5 ainda não aplicaram as suas | Não bloqueia: a versão livre é conferida no passo 2 e nenhuma migração aplicada é editada. |
| PostgreSQL 18 e Docker | Necessários para Testcontainers e para o Compose de `dev` | Bloqueiam a suíte de integração: coluna gerada `tsvector`, GIN, `text[]` e os `check` não têm equivalente em banco em memória. |
| Playwright | Ausente do `package.json`; previsto em `prd-acesso-e-conta` | Bloqueia o passo 13. |
| Aprovação visual das três lacunas do protótipo | A obter no passo 1 | Bloqueia os passos 10 e 11. São ajustes pequenos em telas já aprovadas, não telas novas. |

## Monitoramento e observabilidade

Sobre Actuator + Micrometer/Prometheus e logs JSON com `traceId`, conforme a §9 da `TECH-SPEC.md`.

| Sinal | Tipo | Uso |
| --- | --- | --- |
| `certame.flashcard.criado` | contador, tag `origem` (`sessao`, `avulso`) | Mede diretamente o objetivo de ≥ 60% dos cards nascendo dentro de uma sessão. É a métrica que decide se a aposta central do PRD se confirmou. |
| `certame.flashcard.dialogo.duracao` | *timer* (p95), tag `acao` (`salvar_fechar`, `salvar_novo`, `cancelar`) | Traduz o objetivo "criar um card e voltar ao enunciado em até 3 interações": o que importa não é a contagem de cliques, é quanto tempo o foco fica fora da questão. Medido no cliente e enviado junto do `POST`. |
| `certame.flashcard.descartado` | contador | Quantas vezes o descarte foi confirmado. Valor alto indica modal aberto por engano ou campos demais. |
| `certame.flashcard.acesso.negado` | contador, tag `operacao` (`ler`, `editar`, `excluir`, `mover`) | Objetivo de **zero** acessos a cards de terceiros (RN-07). Qualquer valor diferente de zero é investigação imediata, não ajuste de meta. |
| `certame.flashcard.total` | *gauge*, tag `estado` | Tamanho e composição do acervo; alimenta a leitura do objetivo de retenção junto de F6b. |
| `certame.flashcard.busca` | *timer* (p95) | Desempenho da busca textual paginada, restrição explícita do PRD. Alerta quando o p95 sair do previsto para o índice GIN. |
| `/actuator/health` com indicador de banco | *health check* | Prontidão do contêiner; já existente. |

**Eventos de log.** `INFO` para criação, edição, arquivamento, restauração e exclusão de card, sempre com `flashcardId`, `usuarioId` e ação. `WARN` para escrita recusada por validação e para toda tentativa de acesso a card de terceiro, com o id tentado e o usuário do token. `ERROR` apenas para falha de infraestrutura.

> **Nenhum log, métrica, tag ou mensagem de erro carrega `frente`, `verso` ou `tags`.** O conteúdo do flashcard é dado pessoal do titular (LGPD) e o PRD o trata como privado até do super-admin; despejá-lo em um agregador de logs contornaria RN-07 por fora da aplicação. Ids e contagens bastam para diagnosticar. Pelo mesmo motivo, `certame.flashcard.total` não recebe tag de usuário — a cardinalidade seria proibitiva e o dado, desnecessário.

## Considerações técnicas

### Principais decisões

- **O diálogo não conhece a sessão, e a sessão não conhece o diálogo.** O acoplamento entre as duas features é um único tipo de dados (`ContextoQuestaoFlashcard`) e um evento de saída. `FlashcardDialog` não injeta `StudySessionStore`, não injeta `Router` e não lê `location`. Isso transforma CA-02 de "cuidado a manter" em propriedade estrutural: não existe caminho de código pelo qual abrir o modal possa reiniciar o cronômetro ou remontar a série, porque o modal não tem referência a nenhum dos dois. A alternativa — passar o store inteiro para o diálogo — pareceria conveniente e criaria exatamente a classe de bug que o PRD proíbe.
- **`404`, nunca `403`, para card de terceiro** (CA-15), com filtro por `usuario_id` dentro do `where` e não em uma checagem posterior. A resposta é idêntica à de um card inexistente, byte a byte, e o custo de tempo é o mesmo, porque a consulta é literalmente a mesma. Um `403` confirmaria a existência do card; uma checagem posterior abriria uma diferença de tempo mensurável.
- **Não existe rota administrativa de flashcard.** RN-07 não é uma autorização a verificar, é uma superfície a não criar: nenhum `@PreAuthorize` de super-admin em lugar nenhum, e TI-26 existe para falhar no dia em que alguém acrescentar "só para suporte".
- **O contador da sessão vem do servidor** (`criadosNaSessao` na resposta do `POST`), não de um número acumulado na tela. Recarregar a página no meio de uma sessão, ou criar cards em duas abas, mantém CA-08 correto — e o servidor já tem o índice por `sessao_origem_id` para responder isso em uma contagem.
- **Frente e verso viajam completos na listagem, e não existe segunda requisição para editar.** Um card é curto por construção (limites de 1000 e 4000 caracteres) e a mesma linha alimenta a tabela e o modal de edição. É a mesma decisão de `prd-editais` — o payload da lista já é o detalhe — pelo mesmo motivo: não há caminho pelo qual lista e detalhe divirjam.
- **Os indicadores viajam dentro da listagem e ignoram os filtros.** É o que o protótipo desenha e é o que faz sentido: eles respondem "quanto eu tenho", não "quanto o filtro achou". Como efeito colateral, o cliente consegue distinguir acervo vazio de filtro vazio sem uma segunda chamada, o que separa dois estados vazios com mensagens diferentes.
- **`status` como estado único, com marcas temporais amarradas por `check`.** O card tem três estados (`ATIVO`, `ARQUIVADO`, `EXCLUIDO`) e um `excluido_em` booleanizado não conseguiria expressar o intermediário. As colunas `arquivado_em` e `excluido_em` existem porque a tela precisa do "quando", e os dois `check` de coerência são o que torna essa redundância segura — não há como `status` e a marca divergirem. É um desvio consciente do `...AndExcluidoEmIsNull` do `code-standards.md` §3, registrado aqui como a rule exige.
- **A migração desta entrega cria também `flashcard_revisao` e o bloco FSRS** (decisão do usuário). CA-12 exige preservar um histórico que precisa de tabela para existir, e as colunas de estado FSRS já estão na §5 da `TECH-SPEC.md` como parte de `flashcard`. Criar tudo de uma vez evita alterar duas tabelas em F6b e permite que `ConsultaFilaFlashcards` — a porta que F6b consome — nasça aqui coberta por teste. O preço é uma tabela que esta funcionalidade só lê; o ganho é que CA-11 e CA-12 deixam de ser promessas.
- **Deck entra em três verbos, não no CRUD inteiro** (decisão do usuário): tabela completa, listar, criar por nome e mover lote. É o mínimo que faz RF-32 e CA-16 funcionarem sem invadir RF-35/RF-36. As colunas de limite nascem com os padrões de RN-11 para que F6b acrescente tela e regra, não migração.
- **"Sem deck" é `deck_id null`, não uma linha reservada.** Uma linha "Sem deck" por usuário exigiria criá-la no cadastro, protegê-la contra renomeação e contra exclusão, e trataria a ausência de escolha como uma escolha. O `null` já é a ausência, e os padrões de coluna dão a ele a mesma política de agendamento.
- **Busca por coluna `tsvector` gerada, com índice GIN**, no mesmo padrão de `questao.busca` da F1. A alternativa `ILIKE '%termo%'` não usa índice e degrada exatamente onde o PRD pede desempenho — listas paginadas de centenas de cards. A limitação de busca parcial está em *Riscos conhecidos*.
- **Ordenação por lista branca** (`criadoEm`, `proximaRevisao`), recusada com `422` fora dela. O `sort` do Spring Data aceita qualquer nome de propriedade e produziria ordenações sem índice, além de expor a forma interna da entidade.
- **Rascunho em `sessionStorage`, sem fila de reenvio offline** (decisão do usuário). O rascunho por questão é barato e protege o texto contra uma recarga acidental; a fila de reenvio traria ordenação, duplicidade e expiração de sessão para resolver um caso raro. Falha de rede vira erro inline com o texto preservado, e a decisão fica registrada para reavaliação depois do lançamento.
- **A criação é permitida nos três modos, inclusive no Modo Prova** (RF-28). Nada no fluxo revela gabarito ou comentário: o conteúdo do card é escrito pelo próprio candidato, e a criação não toca em `resposta` nem na finalização da sessão (RN-04).
- **A edição nunca reagenda o card.** Corrigir uma vírgula não pode apagar o que o FSRS aprendeu sobre a memória do usuário — `PUT` não escreve em nenhuma coluna do bloco de agendamento, e TU-14 falha se alguém mudar isso.
- **Reactive Forms, não Signal Forms**, mantendo a decisão de `prd-acesso-e-conta`, `prd-banco-de-questoes` e `prd-editais`. Revisar quando a §2 da `TECH-SPEC.md` for reavaliada, não em uma funcionalidade de F6.

### Riscos conhecidos

| Risco | Mitigação |
| --- | --- |
| **`prd-sessao-de-estudo` não tem TechSpec**, e esta funcionalidade depende da barra de ações da questão, de `sessao_estudo` e da rota `/estudar?questao=`. Sem F2, a promessa central do PRD (CA-01, CA-02, CA-08) não é entregável. | O contrato de UI (`ContextoQuestaoFlashcard`) e o de banco estão fixados em *Pontos de integração* para que a TechSpec de F2 os incorpore. Os passos 2–11 avançam sobre `db/migration-test/V991` e a criação avulsa exercita o mesmo modal. É a dependência bloqueadora da entrega e está registrada como tal. |
| A busca por `websearch_to_tsquery` casa **lexemas inteiros**: digitar "constitu" não encontra "constitucional", ao contrário do `includes` do protótipo. | Aceito para a v1 e documentado no rótulo do campo ("Buscar na frente ou no verso do card…"). Se o uso mostrar que a busca por prefixo é esperada, a evolução é acrescentar `:*` ao último termo do `tsquery` — mudança de uma linha, sem alterar o índice. |
| A tela do protótipo é orientada a acervos de dezenas de cards; com milhares, a tabela paginada de 20 linhas com quatro contagens globais por requisição pode pesar. | Os três índices parciais cobrem exatamente as consultas da tela, e TI-27 mede o número de consultas. Se os indicadores virarem o gargalo, eles saem para um endpoint próprio com cache curto — o formato de `PaginaFlashcards` já os isola em um bloco. |
| `tags text[]` é um tipo específico do PostgreSQL; o mapeamento por Hibernate e a filtragem por tag são menos triviais que uma tabela de vínculo. | Nesta entrega tags são apenas gravadas e exibidas — **não** há filtro por tag em nenhum critério de aceitação. Se o filtro entrar em v2, a alternativa é um índice GIN sobre o array, sem mudar o tipo. |
| O bloco FSRS nasce aqui e só é escrito por F6b: existe uma janela em que todo card fica em `NOVO` com `proxima_revisao` no dia da criação, e o indicador "vencendo hoje" cresce sem que exista sessão de revisão. | É o comportamento correto e transitório — cards novos estão de fato vencidos até serem revisados. A tela de decks e a sessão de revisão de F6b fecham a experiência; até lá, os indicadores medem acervo, não dívida. |
| Conteúdo de flashcard é dado pessoal do titular (LGPD) e RN-07 impede que qualquer perfil o leia. Um pedido de exportação ou de exclusão não pode ser atendido por uma tela administrativa, porque essa tela não existe. | Exclusão de conta marca os cards como excluídos junto do usuário, em `prd-gestao-de-usuarios`. Exportação a pedido é procedimento operacional com autenticação do próprio titular, não rota administrativa — a decisão de não criar uma porta de leitura para o suporte é deliberada e está registrada acima. |
| Três lacunas no protótipo (multisseleção com "Mover para deck", "+ Novo deck" no seletor e o modo avulso com disciplina e assunto editáveis) precisam de aprovação de design antes do frontend. | São ajustes pequenos em telas já aprovadas, com componentes Nocturne existentes, e estão no passo 1 da ordem de construção. O risco é de calendário, não de escopo. |
| O `shared/ui/dialog` nasce aqui e será a base do diálogo de deck de F6b e, possivelmente, do `confirmacao-dialog` da F1. Uma API mal desenhada agora se paga três vezes. | A superfície é deliberadamente pequena: abrir com um componente e um contexto, fechar com um resultado, foco inicial e elemento de devolução de foco. Armadilha de foco e `Esc` ficam no contêiner, não no consumidor, e TU-39 e TU-40 fixam o comportamento. |

### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `.agents/rules/code-standards.md` e `.agents/rules/tests.md` foram lidos na íntegra, assim como `PRD.md`, `TECH-SPEC.md` e `folder-structure.md`.

| Regra | Como esta especificação a observa |
| --- | --- |
| 1 — Frontend é tradução 1:1 do protótipo, só tokens Nocturne, sem biblioteca de UI | A tela, o modal e o botão da questão já existem em `Certame.dc.html` e são traduzidos com a mesma marcação e as mesmas classes (`.dialog`, `.table`, `.field`, `.tag`). As três lacunas entram **no protótipo** no passo 1, nunca direto no Angular. O diálogo é construído sobre `shared/ui/dialog`, sem biblioteca de terceiros. |
| 2 — Autorização sempre no servidor | `@PreAuthorize("isAuthenticated()")` nos serviços e, acima disso, o filtro por dono dentro de toda consulta. A interface não é a defesa: TI-16…TI-18 e TI-26 provam pela API. |
| 3 — Interface em pt-BR; identificadores sem acento | Mensagens de erro em português com ponto final; classes, tabelas e colunas em português sem acento (`flashcard`, `proxima_revisao`, `VerificadorDonoFlashcard`). Campos do envelope de erro seguem em inglês, pela exceção da própria rule. |
| 4 — Nenhum endpoint fora do contrato da §6 sem atualizar o documento | Sete das dez rotas já constam da §6. As três acrescentadas (`GET /flashcards/{id}`, `POST /flashcards/{id}/restaurar`, `GET`/`POST /flashcards/decks` como subconjunto do CRUD previsto) entram na `TECH-SPEC.md` no passo 13, na mesma entrega. |
| 5 — Migração só por Flyway, versionada e imutável | `V8__flashcards.sql` em `db/migration`, aditiva; semente de `dev` isolada em `db/migration-dev`; migração de teste em `db/migration-test`, fora do *classpath* de produção. Nenhuma migração aplicada é editada; os números são conferidos no passo 2. |
| 6 — `resposta` imutável; Modo Prova conta só na finalização | Esta funcionalidade não escreve em `resposta` nem participa da finalização. Criar um card no Modo Prova é inerte para RN-04, e TI-10 registra isso. |
| 7 — Flashcards privados do dono; FSRS em `flashcard/fsrs` | RN-07 implementado por construção, como descrito acima. O algoritmo **não é tocado**: nada aqui calcula agendamento, e o pacote `flashcard/fsrs` é criado por F6b. |
| 8 — Criação na sessão é diálogo, nunca rota | Nenhuma rota de criação em `app.routes.ts`; o diálogo é aberto pelo `DialogService` e não recebe referência à sessão. CA-02 é verificado por E2E-02. |
| 9 — Fila respeita limites do deck; excluir deck não apaga cards | Fora do escopo desta entrega (RN-10/RN-11 são de F6b), mas o esquema já sustenta as duas: as colunas de limite nascem com os padrões, e `deck_id ... on delete set null` é a segunda barreira contra perda de card. |
| `code-standards.md` §2 | Arquivos ≤ 100 linhas e métodos ≤ 30 orientaram a divisão em `VerificadorDonoFlashcard`, `NormalizadorTags`, `ResolvedorContextoFlashcard`, `FlashcardService`, `ConsultaFlashcardsService` e `DeckService`, em vez de um serviço único; `FiltroFlashcards` existe para não passar quatro parâmetros de filtro; a tela é dividida em página, filtros, indicadores e diálogo. Sem comentário supérfluo, sem literal mágico (limites de tamanho, `size` máximo e teto de tags como constantes nomeadas). |
| `code-standards.md` §3 | DTOs em `record` com mensagens de validação em português; entidade JPA nunca cruza o controller; injeção por construtor com campos `private final`; sem Lombok em entidade e serviço; enum como texto com `check` na migração; um domínio não acessa o repositório de outro — `questao` e `estudo` são consultados por serviço e por porta. |
| `code-standards.md` §4 | Componentes standalone, zoneless, estado em signals, `input()`/`output()` como funções, template e estilo em arquivos separados, HTTP concentrado em `core/api`, `fieldErrors` mapeados campo a campo e demais erros em toast. |
| `tests.md` | Piso de **80%** (não os 70% da `TECH-SPEC.md` §9), esforço concentrado no que é crítico (privacidade e preservação de histórico), nomes `TU`/`TI` + id + comportamento em português, AAA visível, AssertJ no backend, `Clock` fixo em tudo que depende de data, temporizadores falsos na busca com atraso, e a pirâmide respeitada — 45 testes de unidade, 27 de integração e 16 E2E. |

### Conformidade com skills

| Skill | Aplicável | Como é observada |
| --- | --- | --- |
| `313-frameworks-spring-db-migrations-flyway` | sim | SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração; `validate-on-migrate` ligado e `baseline-on-migrate` desligado, como já configurado em `application.properties`; disciplina *forward-only*; `spring.flyway.locations` separa produção, `dev` e teste; a migração é puramente aditiva e não altera tabela existente. |
| `postgresql-table-design` | sim | PK `uuid` com `gen_random_uuid()`; `timestamptz` em UTC para instantes e `date` para `proxima_revisao`, que é um dia civil; coluna gerada `tsvector` com índice GIN para a busca em português; índices parciais alinhados exatamente às três consultas da tela (lista, fila e busca); `check` para os enums textuais, para o tamanho mínimo de frente e verso e para as três invariantes de coerência; índice único com `lower(nome)` por usuário; `on delete set null` deliberado em `deck_id` e ausência deliberada de cascata em `flashcard_revisao`. |
| `spring-boot-testing` | sim | Fatia mais estreita por cenário: JUnit + Mockito puros nas regras e serviços, `@DataJpaTest` com Testcontainers nas *constraints* e na coluna gerada, `@WebMvcTest` com `MockMvcTester` no contrato e na privacidade, `@SpringBootTest` só nos cinco fluxos que atravessam escrita e leitura; asserções em AssertJ; contexto compartilhado. **Desvio registrado:** o `AGENTS.md` fala em "JUnit 5", enquanto o BOM do Spring Boot 4.1.1 traz JUnit Jupiter 6 — a API é a mesma e prevalece a versão gerenciada pelo BOM. |
| `angular-developer` | sim | Componentes standalone, detecção *zoneless*, estado por signals em serviços `providedIn: 'root'` (`ToastStore`, `FlashcardsStore`), `input()`/`output()` como funções, `computed` para os estados vazios e para os rótulos, guarda funcional `authGuard`, `LOCALE_ID` pt-BR para as datas de próxima revisão e acessibilidade de diálogo (`aria-modal`, armadilha de foco, devolução do foco). |
| `impeccable` | não | O layout é congelado pela regra 1 do `AGENTS.md`. As três lacunas são compostas com componentes Nocturne existentes e o padrão visual das telas irmãs, não redesenho; a acessibilidade exigida pelo PRD é coberta por TU-39, TU-40 e E2E-16. |
| `find-skills`, `criar-*`, `executar-*` | não | São skills de processo, não de implementação. |

### Arquivos relevantes e dependentes

**Criados** — backend:
`flashcard/Flashcard.java` · `flashcard/StatusFlashcard.java` · `flashcard/EstadoFlashcard.java` · `flashcard/FlashcardRepository.java` · `flashcard/VerificadorDonoFlashcard.java` · `flashcard/NormalizadorTags.java` · `flashcard/ResolvedorContextoFlashcard.java` · `flashcard/SessaoDoUsuario.java` · `flashcard/FiltroFlashcards.java` · `flashcard/FlashcardService.java` · `flashcard/ConsultaFlashcardsService.java` · `flashcard/ConsultaFilaFlashcards.java` · `flashcard/FlashcardController.java` · `flashcard/dto/RequisicaoFlashcard.java` · `flashcard/dto/FlashcardResumo.java` · `flashcard/dto/PaginaFlashcards.java` · `flashcard/dto/IndicadoresFlashcards.java` · `flashcard/dto/RespostaFlashcardCriado.java` · `flashcard/dto/OrigemResumo.java` · `flashcard/dto/ReferenciaResumo.java` · `flashcard/deck/Deck.java` · `flashcard/deck/DeckRepository.java` · `flashcard/deck/DeckService.java` · `flashcard/deck/DeckController.java` · `flashcard/deck/dto/RequisicaoDeck.java` · `flashcard/deck/dto/DeckResumo.java` · `flashcard/deck/dto/RequisicaoMoverCards.java` · `backend/src/main/resources/db/migration/V8__flashcards.sql` · `backend/src/main/resources/db/migration-dev/V906__seed_flashcards_dev.sql`

**Criados** — frontend:
`core/api/flashcard-api.ts` · `core/api/modelos-flashcard.ts` · `shared/ui/dialog/` · `shared/ui/toast/` · `features/flashcards/flashcard-dialog/` · `features/flashcards/flashcard-form-store.ts` · `features/flashcards/rascunho-flashcard.ts` · `features/flashcards/flashcards-page/` · `features/flashcards/flashcards-filtros/` · `features/flashcards/flashcards-indicadores/` · `features/flashcards/flashcards-store.ts` · `features/flashcards/deck-seletor/` · `features/flashcards/mover-para-deck/`

**Modificados:**
`frontend/src/app/layout/shell/shell.html` (item "Flashcards") · `frontend/src/app/app.routes.ts` (`/flashcards`, `/flashcards/:id`) · `protótipo/Certame.dc.html` (multisseleção com "Mover para deck", "+ Novo deck" no seletor, modo avulso do modal) · `TECH-SPEC.md` §5 e §6 · `features/study/questao-acoes/` (botão "Novo flashcard", entregue por F2)

**Reutilizados:** `config/SegurancaConfig.java` · `usuario/Usuario.java` · `comum/ManipuladorErrosGlobal.java` · `comum/ErroApi.java` · `comum/excecao/RecursoNaoEncontradoException.java` · `comum/excecao/DadosInvalidosException.java` · `comum/excecao/ConflitoException.java` · `catalogo/Disciplina.java` · `catalogo/Assunto.java` · `questao/Questao.java` · `backend/src/main/resources/db/migration/V1__base_acesso.sql` · `backend/src/main/resources/db/migration/V2__banco_questoes.sql` · `backend/src/test/resources/db/migration-test/V991__sessao_resposta_marcacao_minima.sql` · `frontend/src/app/core/auth/auth-guard.ts` · `frontend/src/app/core/auth/auth-interceptor.ts` · `frontend/src/app/core/api/erro-api.ts` · `frontend/src/app/shared/ui/confirmacao-dialog/` · `frontend/src/styles.css`

**Dependentes:** `prd-flashcards-decks-e-revisao` (RF-33…RF-36) monta a fila pela porta `ConsultaFilaFlashcards`, escreve em `flashcard_revisao` e no bloco FSRS de `flashcard`, e completa o CRUD de `deck` sem nova migração; `prd-sessao-de-estudo` hospeda o botão da barra de ações e a rota `/estudar?questao=` de CA-13; `prd-cadernos-e-desempenho` soma o tempo das sessões de flashcards ao total do dia (RF-06), a partir de F6b; `prd-gestao-de-usuarios` marca os cards do usuário como excluídos ao excluir a conta.
