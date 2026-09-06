# Tarefa 6.0: Controllers e contrato HTTP (RF-28…RF-32, RN-07)

## Visão geral

`FlashcardController` e `DeckController` expõem as dez rotas de `/api/v1/flashcards`. É aqui que a privacidade sai do serviço e vira contrato observável: **nenhuma rota aceita um identificador de usuário** como parâmetro de caminho, de consulta ou de corpo — o dono é sempre o `subject` do token.

RN-07 não é uma autorização a verificar, é uma superfície a não criar: **não existe rota administrativa de flashcard em perfil nenhum**, e TI-26 prova isso mandando um super-admin ler, editar e excluir o card de um estudante e recebendo `404` nas três tentativas, idêntico ao de um estranho. TI-16 vai além e exige que o corpo do `404` de card de terceiro seja **byte a byte igual** ao do id inexistente: sem `Location`, sem cabeçalho próprio e sem diferença de tempo perceptível.

Três rotas não constam da §6 da `TECH-SPEC.md` (`GET /flashcards/{id}`, `POST /flashcards/{id}/restaurar` e `POST /flashcards/decks/{id}/mover`) — pela regra 4 do `AGENTS.md`, o documento é atualizado na mesma alteração, o que acontece na tarefa 10.0.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `@WebMvcTest` (slice web) para status, formato do JSON e autorização por perfil; `@SpringBootTest @AutoConfigureMockMvc @Testcontainers` com PostgreSQL 18 real (`@ServiceConnection`) para o fluxo ponta a ponta, a busca textual e a contagem de consultas. Sem H2.
- `postgresql-table-design` — TI-27 conta as consultas da listagem paginada para provar a ausência de N+1 sobre deck, disciplina, assunto e questão de origem.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras relevantes: regra 2 (toda autorização validada no servidor), regra 4 (nenhum endpoint fora do contrato da Tech Spec §6 sem atualizar o documento — as três rotas novas são registradas na tarefa 10.0), regra 7 (privacidade por construção; `404`, nunca `403`, para card de terceiro) e regra 3 (mensagens em pt-BR com ponto final; `code`, `message` e `fieldErrors` do envelope permanecem em inglês, exceção deliberada de `code-standards.md` §1). `code-standards.md` §3: controller só orquestra, entidade JPA nunca cruza o controller, `ManipuladorErrosGlobal` e `ErroApi` são reutilizados **sem alteração**. `tests.md` §4: `@PreAuthorize` só é provado de verdade no slice web.

**Dependência de F2:** TI-06, TI-09, TI-10 e TI-21 precisam de `sessao_estudo`, que na suíte vem de `db/migration-test/V991__sessao_estudo_minima.sql` (tarefa 2.0).
</rules>

<requirements>
- As dez rotas de `/api/v1/flashcards`, todas autenticadas, exatamente como na TechSpec: `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/arquivar`, `POST /{id}/restaurar`, `GET /decks`, `POST /decks`, `POST /decks/{id}/mover`.
- `POST /flashcards`: `201` com `RespostaFlashcardCriado` e cabeçalho `Location` apontando para `/api/v1/flashcards/{id}`.
- `DELETE`, `arquivar` e `restaurar`: `204` sem corpo.
- `POST /flashcards/decks`: `201` com `DeckResumo`; nome repetido responde `409` `NOME_EM_USO`, garantido pelo índice único (que também fecha a corrida entre requisições simultâneas).
- `POST /flashcards/decks/{id}/mover`: `200` com `{ "movidos": n }`; deck inexistente ou de terceiro responde `404`.
- Os quatro códigos de erro do contrato: `DADOS_INVALIDOS` (`422`), `RECURSO_NAO_ENCONTRADO` (`404`), `NOME_EM_USO` (`409`) e `ACESSO_NEGADO` (`403`, **nunca** para card de terceiro).
- O `404` de card de terceiro é indistinguível do de card inexistente: mesmo código, mesma mensagem, mesmo corpo.
- Nenhuma rota aceita identificador de usuário; o `usuarioId` vem do token.
- Nenhuma rota administrativa de flashcard, em nenhum perfil.
- Requisição sem token responde `401` em **todas** as rotas, antes de qualquer consulta.
- `ManipuladorErrosGlobal` e `ErroApi` reutilizados sem alteração.
</requirements>

## Subtarefas

- [x] 6.1 Criar `FlashcardController` com as sete rotas de card, delegando a `FlashcardService` e `ConsultaFlashcardsService`.
- [x] 6.2 Criar `DeckController` com as três rotas de deck.
- [x] 6.3 Ligar a validação de `sort`, `size`, `estado` e `deck=sem-deck` na camada web, devolvendo `422` com `fieldErrors`.
- [x] 6.4 Escrever TI-06…TI-10 (criação nos três modos, validação, avulso, sessão de terceiro).
- [x] 6.5 Escrever TI-11…TI-15 (listagem, busca em português, filtros combinados, arquivados, `sort` recusado).
- [x] 6.6 Escrever TI-16…TI-20 (privacidade de leitura, edição e exclusão; porta da fila; histórico preservado).
- [x] 6.7 Escrever TI-21…TI-27 (origem com código, mover lote, nome repetido, rotas sem token, super-admin, contagem de consultas).

## Detalhes de implementação

Ver TechSpec, seção *Endpoints da API* na íntegra (visão geral, corpo, respostas e exemplos de cada rota, com as notas sobre `Location`, sobre disciplina/assunto ignorados quando há origem, sobre indicadores presentes com lista vazia e sobre a ausência de rota de restauração de card excluído) e *Modelos de dados — `ErroApi`* (a nota de que CA-15 depende de a mensagem ser indistinguível). *Principais decisões* registra por que não existe rota administrativa.

## Critérios de aceitação relacionados

- CA-01
- CA-04
- CA-05
- CA-09
- CA-10
- CA-11
- CA-12
- CA-13
- CA-14
- CA-15
- CA-16

## Testes da tarefa

### Testes de integração

- [x] TI-06 — `POST /flashcards` dentro de uma sessão
- [x] TI-07 — `POST /flashcards` com frente de 2 caracteres
- [x] TI-08 — `POST /flashcards` avulso
- [x] TI-09 — `POST /flashcards` com sessão de outro usuário
- [x] TI-10 — `POST /flashcards` com sessão em Modo Prova aberta
- [x] TI-11 — `GET /flashcards` com acervo montado
- [x] TI-12 — `GET /flashcards?q=` com termo em português
- [x] TI-13 — `GET /flashcards` com filtros combinados e ordenação
- [x] TI-14 — `GET /flashcards?arquivados=true`
- [x] TI-15 — `GET /flashcards?sort=frente,asc`
- [x] TI-16 — `GET /flashcards/{id}` de card de outro usuário
- [x] TI-17 — `PUT /flashcards/{id}` de card de outro usuário
- [x] TI-18 — `DELETE /flashcards/{id}` de card de outro usuário
- [x] TI-19 — Arquivar e consultar a porta da fila
- [x] TI-20 — Excluir card com duas revisões registradas
- [x] TI-21 — `GET /flashcards` traz a origem com o código da questão
- [x] TI-22 — `POST /flashcards/decks/{id}/mover` com 12 cards
- [x] TI-23 — `POST /flashcards/decks/{id}/mover` com id de terceiro na lista
- [x] TI-24 — `POST /flashcards/decks` com nome repetido
- [x] TI-25 — Todas as rotas sem token
- [x] TI-26 — Super-admin lê, edita e exclui card de estudante
- [x] TI-27 — Listagem de 500 cards paginada (contador de consultas)

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/flashcard/FlashcardController.java`
- `backend/src/main/java/br/com/certame/flashcard/deck/DeckController.java`
- `backend/src/test/java/br/com/certame/flashcard/FlashcardControllerIntegracaoTest.java`
- `backend/src/test/java/br/com/certame/flashcard/FlashcardPrivacidadeIntegracaoTest.java`
- `backend/src/test/java/br/com/certame/flashcard/deck/DeckControllerIntegracaoTest.java`
