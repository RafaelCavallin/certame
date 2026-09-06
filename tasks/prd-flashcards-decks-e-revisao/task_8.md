# Tarefa 8.0: Frontend — cliente HTTP, modelos e tela de Decks

## Visão geral

Traz os contratos desta especificação para o cliente e traduz 1:1 a tela de Decks aprovada (com as lacunas fechadas na tarefa 1.0): um card por deck com nome, descrição, contadores, resumo dos limites e a ação "Estudar este deck", mais o diálogo de criação/edição com os quatro limites.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone, detecção *zoneless*, estado por signals em `DecksStore` (`providedIn: 'root'`), `input()`/`output()` como funções, `LOCALE_ID` pt-BR.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/tests.md` e `.agents/rules/folder-structure.md`.

- **Regra 1 (AGENTS.md)** — tradução 1:1 do protótipo já ajustado na tarefa 1.0; só tokens `var(--*)` de `styles.css`; nenhuma biblioteca de UI de terceiros.
- `folder-structure.md` §3 — telas em `features/flashcards/`, cliente HTTP em `core/api/`, sem sufixo `.component`.
- Nota de `folder-structure.md` §3.2 — a configuração de deck (RF-36) não tem rota própria: é um diálogo aberto pelo `DialogService`, sobre `shared/ui/dialog` de F6a.
</rules>

<requirements>
- RF-35, RF-36.
- CA-12, CA-13, CA-14, CA-15, CA-16, CA-17, CA-18.
</requirements>

## Subtarefas

- [x] 8.1 `core/api/revisao-api.ts`: cliente das quatro rotas de sessão e resumo.
- [x] 8.2 `core/api/modelos-revisao.ts`: tipagem dos contratos (`CardRevisao`, `MemoriaCard`, `PrevisaoNota`, `RespostaSessaoRevisao`, `RespostaRevisao`, `ResumoSessao`, `FaixaVencimento`, `ResumoFlashcards`).
- [x] 8.3 Estender `core/api/flashcard-api.ts` com `atualizarDeck`, `excluirDeck` e o tipo `DeckDetalhe` ampliado; `listarDecks` passa a devolver `DeckDetalhe[]`.
- [x] 8.4 `features/flashcards/decks-store.ts`: signals `decks`, `carregando`, `salvando`, `erroDeCampo`; métodos `carregar()`, `salvar(deck, id?)` (refletindo os limites ajustados pelo servidor, CA-16) e `excluir(id)`.
- [x] 8.5 `features/flashcards/decks-page/`: a grade de decks em tradução 1:1 do protótipo (CA-12).
- [x] 8.6 `features/flashcards/deck-card/`: contadores, resumo dos limites, aviso de fila acumulada (CA-18) e a ação "Estudar este deck".
- [x] 8.7 `features/flashcards/deck-dialog/`: nome, descrição e os quatro limites, sobre `shared/ui/dialog` de F6a, sem rota própria; mapeamento de `409` para o campo `nome` (CA-13).

## Detalhes de implementação

Ver `techspec.md`, seção "Arquitetura do sistema" (tabela "Frontend — `frontend/src/app`") e "Modelos de dados" (`DeckDetalhe`, `LimitesDeck`, `AvisoDeck`, `RequisicaoDeck`, com o exemplo do ajuste silencioso e observável de CA-16).

## Critérios de aceitação relacionados

- CA-12, CA-13, CA-14, CA-15, CA-16, CA-17, CA-18

## Testes da tarefa

### Testes de unidade

- [x] TU-65 — `DecksStore.salvar` mapeia 409 para o campo nome
- [x] TU-66 — `DecksStore.salvar` reflete os limites ajustados pelo servidor

## Arquivos relevantes

- `core/api/revisao-api.ts`
- `core/api/modelos-revisao.ts`
- `core/api/flashcard-api.ts` (modificado)
- `features/flashcards/decks-store.ts`
- `features/flashcards/decks-page/`
- `features/flashcards/deck-card/`
- `features/flashcards/deck-dialog/`
