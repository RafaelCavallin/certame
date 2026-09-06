# Tarefa 7.0: Controllers e contrato da API

## Visão geral

Expõe as sete rotas desta entrega — quatro novas em `RevisaoController` e duas novas em `DeckController` (mais o `GET /flashcards/decks` enriquecido) — onde a privacidade sai do serviço e vira contrato observável. Nenhuma rota aceita identificador de usuário como parâmetro; o dono é sempre o *subject* do token.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `@WebMvcTest` com `MockMvcTester` para contrato, códigos de erro e privacidade; `@SpringBootTest` completo para os fluxos que atravessam escrita e leitura (abertura de sessão, registro de revisão, exclusão de deck).
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md`, `.agents/rules/code-standards.md` e `.agents/rules/tests.md`.

- **Regra 4 (AGENTS.md)** — nenhum endpoint fora do contrato da Tech Spec; as sete rotas já estão previstas na `techspec.md`, seção "Endpoints da API".
- **Regra 2** — toda autorização validada no servidor com `@PreAuthorize`, mesmo quando a interface já esconde a ação.
- **Regra 7** — recurso de terceiro responde sempre `404`, nunca `403`; a mensagem é indistinguível entre "não existe" e "é de outro usuário" (CA-20).
- `code-standards.md` — controller só orquestra, sem `try/catch` (erros sobem por `ErroNegocioException` até `ManipuladorErrosGlobal`, que não muda).
</rules>

<requirements>
- RF-33, RF-34, RF-35, RF-36, RN-07.
- CA-01, CA-03, CA-04, CA-06 a CA-20 (contrato observável de todos).
</requirements>

## Subtarefas

- [x] 7.1 `flashcard/revisao/RevisaoController.java`: `POST /api/v1/flashcards/sessoes`, `POST /api/v1/flashcards/sessoes/{id}/revisoes`, `POST /api/v1/flashcards/sessoes/{id}/finalizar`, `GET /api/v1/flashcards/resumo`.
- [x] 7.2 Estender `flashcard/deck/DeckController.java` com `PUT /api/v1/flashcards/decks/{id}` e `DELETE /api/v1/flashcards/decks/{id}` (devolvendo `{ "cardsMovidos": number }`).
- [x] 7.3 Confirmar que `GET /api/v1/flashcards/decks` (rota de F6a) passa a devolver `DeckDetalhe[]`, preservando `id`, `nome`, `descricao`, `totalCards` para os consumidores de F6a.
- [x] 7.4 Mapear os cinco códigos de erro (`DADOS_INVALIDOS`, `RECURSO_NAO_ENCONTRADO`, `NOME_EM_USO`, `SESSAO_FINALIZADA`, `ACESSO_NEGADO`) sobre as exceções já existentes, sem criar tipo novo.

## Detalhes de implementação

Ver `techspec.md`, seção "Endpoints da API" (visão geral e o detalhamento de cada rota, com exemplos de sucesso, borda e erro) e "Modelos de dados" (`ErroApi`).

## Critérios de aceitação relacionados

- CA-01, CA-03, CA-04, CA-06, CA-07, CA-08, CA-09, CA-10, CA-11, CA-12, CA-13, CA-14, CA-15, CA-16, CA-17, CA-18, CA-19, CA-20

## Testes da tarefa

### Testes de integração

- [x] TI-05 — `POST /sessoes` monta a fila respeitando os limites do deck
- [x] TI-06 — `POST /sessoes` devolve as quatro previsões por card
- [x] TI-07 — `POST /sessoes` devolve o bloco de memória do card
- [x] TI-08 — `POST /sessoes` com fila do dia terminada
- [x] TI-09 — `POST /sessoes` de um usuário não alcança cards de outro
- [x] TI-10 — `POST /sessoes` com `escopo = DECK` de deck de terceiro
- [x] TI-11 — `POST /revisoes` com *Bom* recalcula memória e agenda
- [x] TI-12 — `POST /revisoes` com *Errei* aplica lapso e reaprendizado
- [x] TI-13 — `POST /revisoes` trunca no intervalo máximo do deck
- [x] TI-14 — `POST /revisoes` grava a linha completa do log
- [x] TI-15 — `POST /revisoes` em card de outro usuário
- [x] TI-16 — `POST /revisoes` em sessão já finalizada
- [x] TI-17 — `POST /finalizar` devolve o resumo completo
- [x] TI-18 — Segunda sessão no mesmo dia respeita o consumo anterior
- [x] TI-19 — `GET /decks` devolve o painel completo
- [x] TI-20 — `GET /decks` inclui a linha virtual "Sem deck"
- [x] TI-21 — `GET /decks` emite o aviso de fila acumulada
- [x] TI-22 — `PUT /decks/{id}` recusa nome curto e nome duplicado
- [x] TI-23 — `PUT /decks/{id}` renomeia mantendo cards e agendamento
- [x] TI-24 — `PUT /decks/{id}` ajusta valores fora do intervalo
- [x] TI-25 — `PUT /decks/{id}` muda a retenção sem tocar o histórico
- [x] TI-26 — `DELETE /decks/{id}` move os cards e preserva todos
- [x] TI-27 — `DELETE /decks/{id}` é atômico
- [x] TI-28 — `GET /resumo` devolve vencendo hoje e próximos 7 dias
- [x] TI-29 — `TempoEstudoFlashcards` soma as sessões finalizadas do dia
- [x] TI-30 — Rotas sem autenticação

## Arquivos relevantes

- `flashcard/revisao/RevisaoController.java`
- `flashcard/deck/DeckController.java` (modificado)
