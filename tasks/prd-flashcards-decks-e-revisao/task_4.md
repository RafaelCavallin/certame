# Tarefa 4.0: Relógio de estudo, limites de deck e CRUD completo de deck

## Visão geral

Duas regras puras das quais a fila e o deck dependem — `RelogioEstudo` (a noção única de "hoje" sobre a zona `America/Sao_Paulo`) e `LimitesDeck` (os quatro limites com `normalizar()`) — seguidas do CRUD de deck que F6a deixou pela metade: renomear, descrever, configurar limites e excluir. Excluir é a operação de maior risco do documento (o PRD trata perda de conteúdo como falha), por isso entra cedo, com o mesmo rigor de teste do agendador.

<skills>
### Conformidade com skills

- `postgresql-table-design` — não se aplica nesta tarefa (sem alteração de esquema); a tarefa apenas consome as colunas criadas em 2.0.
- `spring-boot-testing` — JUnit + Mockito puros no `DeckService` e no `PainelDecksService`; sem Testcontainers (regra de negócio, não integração).
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md`, `.agents/rules/code-standards.md` e `.agents/rules/tests.md`.

- **Regra 9 (AGENTS.md)** — excluir um deck nunca apaga cards (RN-11); a exclusão e a realocação para "Sem deck" ocorrem na mesma transação, com `on delete set null` como segunda barreira.
- **Regra 7** — toda consulta filtra por `usuario_id` do token; deck de terceiro responde `404` (CA-20), nunca `403`.
- `code-standards.md` — `LimitesDeck` como `record` puro, sem Spring; cláusulas guarda em vez de aninhamento; `@Transactional` no `DeckService.excluir`.
</rules>

<requirements>
- RN-10 (limites), RN-11 (integridade da exclusão), RF-35, RF-36.
- CA-12, CA-13, CA-14, CA-15, CA-16, CA-17, CA-18, CA-20.
</requirements>

## Subtarefas

- [x] 4.1 `flashcard/RelogioEstudo.java`: `hoje()`, `inicioDoDia()`, `instante()` sobre `Clock` injetado e a propriedade `certame.estudo.zona`.
- [x] 4.2 `flashcard/deck/LimitesDeck.java`: `record` dos quatro limites, `normalizar()` (CA-16) e a constante `PADRAO` (10 novos/dia, 60 revisões/dia, máximo 180 dias, retenção 0,90 — RN-11).
- [x] 4.3 Estender `flashcard/deck/Deck.java` com `setNome`, `setDescricao` e `aplicarLimites(LimitesDeck)`.
- [x] 4.4 `DeckService.atualizar(id, usuarioId, RequisicaoDeck)`: renomear (recusa nome < 3 caracteres ou duplicado — CA-13), descrever, configurar limites com `normalizar()` (CA-16), retenção sem tocar histórico (CA-17); nenhuma interação com `FlashcardRepository` (CA-14).
- [x] 4.5 `DeckService.excluir(id, usuarioId)`: move os cards para "Sem deck" na mesma transação, devolve a contagem movida (CA-15, RN-11); recusa deck de terceiro (CA-20).
- [x] 4.6 `flashcard/deck/PainelDecksService.java`: total, vencendo hoje, novos, resumo dos limites por deck, incluindo a linha virtual "Sem deck" (CA-12) e o `AvisoDeck` de fila acumulada acima do dobro do limite (CA-18).
- [x] 4.7 DTOs `flashcard/deck/dto/DeckDetalhe.java` e `flashcard/deck/dto/AvisoDeck.java`; ampliar `flashcard/deck/dto/RequisicaoDeck.java` com `descricao` e os quatro limites.

## Detalhes de implementação

Ver `techspec.md`, seções "Principais interfaces" (blocos `LimitesDeck`, `DeckService`, `PainelDecksService`), "Modelos de dados" (`DeckDetalhe`, `LimitesDeck`, `AvisoDeck`, a linha virtual "Sem deck") e "Principais decisões" (linha "A linha virtual 'Sem deck' no painel").

## Critérios de aceitação relacionados

- CA-12, CA-13, CA-14, CA-15, CA-16, CA-17, CA-18, CA-20

## Testes da tarefa

### Testes de unidade

- [x] TU-22 — `LimitesDeck.normalizar` ajusta 500 revisões por dia para 400
- [x] TU-23 — `LimitesDeck.normalizar` ajusta cada campo fora do intervalo
- [x] TU-24 — `LimitesDeck.PADRAO` traz os limites de "Sem deck"
- [x] TU-25 — `DeckService.criar` recusa nome com menos de 3 caracteres
- [x] TU-26 — `DeckService.atualizar` recusa nome já usado por outro deck
- [x] TU-27 — `DeckService.atualizar` aceita o próprio nome sem alteração
- [x] TU-28 — `DeckService.atualizar` não escreve em nenhum flashcard
- [x] TU-29 — `DeckService.excluir` move os cards e não apaga nenhum
- [x] TU-30 — `DeckService.excluir` recusa deck de outro usuário
- [x] TU-49 — `PainelDecksService` calcula total, vencendo hoje e novos
- [x] TU-50 — `PainelDecksService` emite aviso acima do dobro do limite
- [x] TU-51 — `PainelDecksService` não emite aviso no dobro exato
- [x] TU-52 — `PainelDecksService` inclui "Sem deck" apenas quando há cards
- [x] TU-53 — `RelogioEstudo` resolve o dia na zona do estudo

## Arquivos relevantes

- `flashcard/RelogioEstudo.java`
- `flashcard/deck/LimitesDeck.java`
- `flashcard/deck/Deck.java` (modificado)
- `flashcard/deck/DeckService.java` (modificado)
- `flashcard/deck/PainelDecksService.java`
- `flashcard/deck/dto/DeckDetalhe.java`
- `flashcard/deck/dto/AvisoDeck.java`
- `flashcard/deck/dto/RequisicaoDeck.java` (modificado)
