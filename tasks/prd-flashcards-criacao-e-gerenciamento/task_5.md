# Tarefa 5.0: Leitura, indicadores, porta da fila e decks (RF-31, RF-32)

## Visão geral

O lado de leitura da funcionalidade, mais os três verbos de deck que RF-32 e CA-16 exigem.

`ConsultaFlashcardsService` monta a listagem paginada com busca textual, filtros combinados e ordenação restrita a uma **lista branca** (`criadoEm`, `proximaRevisao`) — o `sort` do Spring Data aceita qualquer nome de propriedade e produziria ordenação sem índice ou vazamento de nome de coluna, então valor fora da lista responde `422` antes de chegar ao SQL. Os quatro indicadores do topo viajam **dentro** do envelope da listagem, em uma requisição só, e **ignoram os filtros**: eles respondem "quanto eu tenho", não "quanto o filtro achou" — é o que permite ao cliente distinguir acervo vazio de filtro sem correspondência.

`ConsultaFilaFlashcards` é a porta de leitura que F6b consome para montar a fila de revisão. Ela existe nesta entrega porque CA-11 e CA-12 são critérios daqui e precisam de algo observável que prove que arquivado e excluído ficam de fora.

`DeckService` entra com três verbos — listar, criar por nome e mover um lote. Renomear, descrever, excluir e configurar limites são de F6b, sobre a tabela que a tarefa 2.0 já deixou completa.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito puros sobre os serviços; a verificação de SQL real (coluna gerada, GIN, índice único) fica nos testes de integração das tarefas 2.0 e 6.0.
- `postgresql-table-design` — as consultas são desenhadas para os índices criados na tarefa 2.0: `flashcard_lista_idx` para a listagem padrão, `flashcard_fila_idx` para a porta da fila e `flashcard_busca_idx` para `websearch_to_tsquery`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras relevantes: regra 7 (toda consulta filtra por `usuario_id` do token — inclusive as contagens dos indicadores e a porta da fila), regra 2 (autorização no servidor) e regra 3 (identificadores sem acento; mensagens em pt-BR). `code-standards.md` §2.4: `FiltroFlashcards` é objeto parâmetro, e não quatro argumentos soltos. `tests.md` §5: repositório com query própria é testado onde a query importa; a regra de lista branca é unidade pura.

**Nota de escopo:** RN-10 e RN-11 (limites do deck e exclusão de deck) são de `prd-flashcards-decks-e-revisao`. Aqui o esquema já os sustenta, mas nenhum comportamento de limite é implementado.
</rules>

<requirements>
- `FiltroFlashcards` como objeto parâmetro com `termo`, `deckId`, `disciplinaId`, `estado` e `arquivados`.
- `ConsultaFlashcardsService.listar(UUID usuarioId, FiltroFlashcards, Pageable) -> PaginaFlashcards`, somente leitura, com os filtros combináveis entre si.
- Busca textual por `websearch_to_tsquery('portuguese', ...)` sobre a coluna `busca`; **termo em branco é tratado como ausente**.
- `deck` aceita um uuid ou o valor literal `sem-deck`, que filtra `deck_id is null`.
- `arquivados=true` lista os arquivados em vez dos ativos; cards `EXCLUIDO` **nunca aparecem** em nenhum dos dois modos.
- Ordenação por lista branca (`criadoEm`, `proximaRevisao`, com `asc`/`desc`); fora dela, `DadosInvalidosException` — a ordenação nunca chega ao SQL como texto livre.
- `size` padrão 20 e máximo 100; acima disso, `422`.
- `IndicadoresFlashcards` com `vencendoHoje`, `proximosSeteDias`, `emFixacao`, `total` e `decks`, sempre sobre cards `ATIVO` do usuário, **sem aplicar os filtros da tabela**, e sempre presente mesmo com `content` vazio.
- `ConsultaFlashcardsService.buscar(UUID id, UUID usuarioId) -> FlashcardResumo`, por `VerificadorDonoFlashcard`.
- `ConsultaFilaFlashcards.elegiveis(UUID usuarioId, LocalDate ate) -> List<Flashcard>`: só `status = 'ATIVO'` com `proxima_revisao <= ate`. Arquivado e excluído ficam de fora.
- Sem N+1: deck, disciplina, assunto e questão de origem vêm com a página, não uma consulta por linha.
- `DeckService.listar(UUID usuarioId) -> List<DeckResumo>`, ordenado por nome, com `totalCards` de cards `ATIVO`; lista vazia quando não houver.
- `DeckService.criar(UUID usuarioId, RequisicaoDeck) -> DeckResumo`: nome de 3 a 60 caracteres após recorte; repetido ignorando maiúsculas lança `ConflitoException` (`NOME_EM_USO`); limites nascem com os padrões de RN-11.
- `DeckService.mover(UUID deckId, UUID usuarioId, List<UUID> flashcardIds) -> int`: `update` que nomeia **apenas** `deck_id` e `atualizado_em`; ids que não sejam do usuário são ignorados em silêncio e `movidos` reflete o número real (CA-16, RN-07). Lista vazia ou acima de 200 itens: `422`.
- DTOs `PaginaFlashcards` (estendendo `PaginaResposta<T>` com o bloco de indicadores), `IndicadoresFlashcards`, `DeckResumo`, `RequisicaoDeck` e `RequisicaoMoverCards` como `record`.
- Toda asserção de data usa `Clock` fixo: `proxima_revisao` é `current_date` e um teste na virada do dia falharia sozinho.
</requirements>

## Subtarefas

- [x] 5.1 Criar `FiltroFlashcards` e os DTOs `PaginaFlashcards`, `IndicadoresFlashcards`, `DeckResumo`, `RequisicaoDeck` e `RequisicaoMoverCards`.
- [x] 5.2 Implementar `ConsultaFlashcardsService.listar` com busca, filtros combinados, `arquivados` e a lista branca de ordenação.
- [x] 5.3 Implementar o cálculo dos cinco indicadores globais, independente dos filtros da tabela.
- [x] 5.4 Implementar `ConsultaFlashcardsService.buscar` e `ConsultaFilaFlashcards.elegiveis`.
- [x] 5.5 Implementar `DeckService` com listar, criar por nome e mover lote.
- [x] 5.6 Escrever TU-25…TU-27.
- [x] 5.7 Escrever TU-28…TU-30.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`ConsultaFlashcardsService`, `ConsultaFilaFlashcards`, `DeckService`), *Modelos de dados* (`IndicadoresFlashcards`, `PaginaFlashcards`, `DeckResumo`, `RequisicaoDeck`, `RequisicaoMoverCards`), *Endpoints da API* (`GET /flashcards` com a tabela de parâmetros e os dois exemplos de lista vazia; `GET`/`POST /flashcards/decks`; `POST /flashcards/decks/{id}/mover`) e *Principais decisões* (indicadores dentro da listagem e alheios ao filtro; ordenação por lista branca; busca por `tsvector`; "Sem deck" como `deck_id null`; deck em três verbos, não no CRUD inteiro).

## Critérios de aceitação relacionados

- CA-09
- CA-10
- CA-11
- CA-12
- CA-15
- CA-16

## Testes da tarefa

### Testes de unidade

- [x] TU-25 — `ConsultaFlashcardsService` calcula os quatro indicadores
- [x] TU-26 — `ConsultaFlashcardsService` com indicadores sob filtro
- [x] TU-27 — `ConsultaFlashcardsService` com `sort` fora da lista branca
- [x] TU-28 — `DeckService.criar` com nome repetido ignorando maiúsculas
- [x] TU-29 — `DeckService.criar` com nome de 2 caracteres
- [x] TU-30 — `DeckService.mover` com ids de outro usuário na lista

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/flashcard/ConsultaFlashcardsService.java`
- `backend/src/main/java/br/com/certame/flashcard/ConsultaFilaFlashcards.java`
- `backend/src/main/java/br/com/certame/flashcard/FiltroFlashcards.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/PaginaFlashcards.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/IndicadoresFlashcards.java`
- `backend/src/main/java/br/com/certame/flashcard/deck/DeckService.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/DeckResumo.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/RequisicaoDeck.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/RequisicaoMoverCards.java`
- `backend/src/test/java/br/com/certame/flashcard/ConsultaFlashcardsServiceTest.java`
- `backend/src/test/java/br/com/certame/flashcard/deck/DeckServiceTest.java`
