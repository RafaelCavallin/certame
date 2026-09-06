# Tarefa 2.0: Migração e persistência (Flyway, entidades, enums e repositórios)

## Visão geral

Cria o esquema que sustenta a funcionalidade inteira: `deck`, `flashcard` e `flashcard_revisao`, com os índices, a coluna gerada `busca` e os três `check` de coerência. Vem antes de qualquer regra porque é o banco que torna dois critérios desta entrega propriedades do esquema, e não disciplina de código: `flashcard_revisao` **sem `on delete cascade`** é o que garante CA-12, e `flashcard_sessao_exige_questao` é RN-09 escrito em SQL.

A tabela `flashcard` nasce com **todo o bloco FSRS** (estabilidade, dificuldade, intervalo, repetições, lapsos, estado, próxima revisão) nos padrões da §5 da `TECH-SPEC.md`, e `deck` nasce com as quatro colunas de limite de RN-11. Nada nesta entrega escreve nessas colunas além da criação — elas existem aqui para que `prd-flashcards-decks-e-revisao` acrescente comportamento sobre um esquema pronto, sem uma segunda alteração de tabela.

**Numeração confirmada contra o repositório:** `db/migration` tem `V1`…`V5` aplicadas, então esta migração é **`V6__flashcards.sql`**, não `V8` como a TechSpec estimou; `db/migration-dev` tem `V900` e `V901`, então a semente é **`V902__seed_flashcards_dev.sql`**. Nenhuma migração aplicada é editada.

<skills>
### Conformidade com skills

- `313-frameworks-spring-db-migrations-flyway` — SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração real do repositório; migração aditiva e forward-only; `validate-on-migrate` ligado; `spring.flyway.locations` separando produção, `dev` e teste.
- `postgresql-table-design` — PK `uuid` com `gen_random_uuid()`; `timestamptz` em UTC para instantes e `date` para `proxima_revisao`, que é um dia civil; enums textuais com `check`; coluna gerada `tsvector` `stored` com índice GIN; índices parciais alinhados aos filtros reais (`where status <> 'EXCLUIDO'`, `where status = 'ATIVO'`); `check` de invariante de negócio amarrando `status` às marcas temporais.
- `spring-boot-testing` — `@DataJpaTest` com Testcontainers e PostgreSQL 18 real (`@ServiceConnection`); sem H2, porque `tsvector`, GIN, `text[]`, índice único sobre `lower(nome)` e os `check` só existem no Postgres.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra. Regras relevantes: regra 5 (migração só por Flyway, versionada e imutável — a versão livre é conferida antes de escrever e nenhuma migração já aplicada é tocada), regra 3 (tabelas e colunas em português sem acento, `snake_case`: `flashcard`, `proxima_revisao`, `excluido_em`), regra 7 (o bloco FSRS nasce aqui mas **não é calculado** aqui — nenhum peso, nenhum algoritmo) e regra 9 (as colunas de limite do deck nascem com os padrões de RN-11, mesmo sem uso nesta entrega). Regra 6 respeitada por omissão: nada aqui toca em `resposta`. `code-standards.md` §3: entidades JPA com injeção por construtor nos serviços que as usam, sem Lombok em entidade nova além do já praticado no repositório.

**Bloqueio conhecido:** `sessao_estudo` não existe — `prd-sessao-de-estudo` não tem TechSpec. A FK `sessao_origem_id` referencia uma tabela que, na suíte de teste, vem de `db/migration-test/V991__sessao_estudo_minima.sql`, no mesmo padrão de `V990__resposta_minima.sql`. Em `dev` e em produção a migração real de F2 precisa vir antes de `V6`; a ordem é conferida na entrega.
</rules>

<requirements>
- **`V6__flashcards.sql`** com o esquema exato da TechSpec: `deck`, `flashcard` e `flashcard_revisao`, com todos os `check`, índices e a coluna gerada.
- `deck` com `nome` único por usuário ignorando maiúsculas (`create unique index deck_nome_uk on deck (usuario_id, lower(nome))`) e as quatro colunas de limite com os padrões e faixas de RN-11 (`novos_dia`, `revisoes_dia`, `intervalo_max_dias`, `retencao_alvo`).
- `flashcard` com `deck_id ... on delete set null`, `tags text[] not null default '{}'`, o bloco FSRS com os padrões da §5 e `busca` como `tsvector generated always as (to_tsvector('portuguese', coalesce(frente,'') || ' ' || coalesce(verso,''))) stored`.
- Os três `check` de coerência: `flashcard_sessao_exige_questao`, `flashcard_excluido_coerente` e `flashcard_arquivado_coerente`, além dos `check` de tamanho mínimo de `frente` e `verso`.
- Os cinco índices: `flashcard_lista_idx`, `flashcard_fila_idx`, `flashcard_busca_idx` (GIN), `flashcard_deck_idx` e `flashcard_origem_idx`.
- `flashcard_revisao` **sem `on delete cascade`** na FK para `flashcard`, com os dois índices previstos. Criada aqui, escrita só por F6b.
- **`db/migration-dev/V902__seed_flashcards_dev.sql`**: dois decks e cerca de doze cards do estudante de exemplo — alguns com questão de origem, um arquivado, um excluído logicamente e um com duas linhas em `flashcard_revisao`. Depende de `V901`.
- **`db/migration-test/V991__sessao_estudo_minima.sql`**: a tabela `sessao_estudo` mínima para as FKs e os testes de integração, fora do *classpath* de produção.
- Entidades JPA `Flashcard` e `Deck` e os enums `StatusFlashcard` (`ATIVO`, `ARQUIVADO`, `EXCLUIDO`) e `EstadoFlashcard` (`NOVO`, `APRENDENDO`, `REVISAO`, `REAPRENDENDO`), persistidos como texto.
- `FlashcardRepository` em que **toda** assinatura recebe `usuarioId` — não existe método que consulte card por id isolado (RN-07). Busca textual por `@Query` nativa com `websearch_to_tsquery` sobre a coluna `busca`.
- `DeckRepository` com a listagem por usuário trazendo a contagem de cards ativos em uma consulta, e a verificação de nome em uso ignorando maiúsculas.
- O Hibernate permanece em `ddl-auto=validate`: o esquema vem só da migração.
</requirements>

## Subtarefas

- [x] 2.1 Conferir a versão livre em `db/migration` e em `db/migration-dev` antes de escrever qualquer arquivo.
- [x] 2.2 Escrever `V13__flashcards.sql` (V6 no planejamento original, V13 na numeração real do repositório) com as três tabelas, os `check`, os índices e a coluna gerada, conforme o SQL da TechSpec.
- [x] 2.3 Escrever `db/migration-test/V991__sessao_estudo_minima.sql` com a tabela `sessao_estudo` mínima e ajustar `spring.flyway.locations` do perfil de teste.
- [x] 2.4 Escrever `db/migration-dev/V908__seed_flashcards_dev.sql` (V902 no planejamento, V908 na numeração real) com os dois decks e os doze cards, incluindo o arquivado, o excluído e o com histórico.
- [x] 2.5 Criar as entidades `flashcard/Flashcard` e `flashcard/deck/Deck` e os enums `StatusFlashcard` e `EstadoFlashcard`.
- [x] 2.6 Criar `FlashcardRepository` (toda assinatura com `usuarioId`, busca nativa por `websearch_to_tsquery`) e `DeckRepository` (listagem com contagem, nome em uso).
- [x] 2.7 Escrever TI-01…TI-05.

## Detalhes de implementação

Ver TechSpec, seções *Esquema de banco — `V8__flashcards.sql`* (SQL completo, as notas sobre os três `check`, a ausência deliberada de `on delete cascade`, o `on delete set null` do deck e as sementes), *Visão dos componentes* (tabela de componentes do backend) e *Principais decisões* (`status` como estado único com marcas temporais amarradas por `check`; o bloco FSRS e `flashcard_revisao` nascendo nesta entrega; busca por `tsvector` gerado em vez de `ILIKE`; "Sem deck" como `deck_id is null`, não uma linha reservada).

## Critérios de aceitação relacionados

- CA-11
- CA-12
- CA-13
- CA-15

## Testes da tarefa

### Testes de integração

- [x] TI-01 — Migração em base limpa (`@DataJpaTest`)
- [x] TI-02 — `check flashcard_excluido_coerente` (`@DataJpaTest`)
- [x] TI-03 — `check flashcard_sessao_exige_questao` (`@DataJpaTest`)
- [x] TI-04 — Índice único de deck por usuário (`@DataJpaTest`)
- [x] TI-05 — Coluna gerada `busca` e índice GIN (`@DataJpaTest`)

## Arquivos relevantes

- `backend/src/main/resources/db/migration/V6__flashcards.sql`
- `backend/src/main/resources/db/migration-dev/V902__seed_flashcards_dev.sql`
- `backend/src/test/resources/db/migration-test/V991__sessao_estudo_minima.sql`
- `backend/src/main/java/br/com/certame/flashcard/Flashcard.java`
- `backend/src/main/java/br/com/certame/flashcard/StatusFlashcard.java`
- `backend/src/main/java/br/com/certame/flashcard/EstadoFlashcard.java`
- `backend/src/main/java/br/com/certame/flashcard/FlashcardRepository.java`
- `backend/src/main/java/br/com/certame/flashcard/deck/Deck.java`
- `backend/src/main/java/br/com/certame/flashcard/deck/DeckRepository.java`
- `backend/src/test/java/br/com/certame/flashcard/FlashcardEsquemaIntegracaoTest.java`
