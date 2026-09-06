# Tarefa 4.0: Serviço de escrita do flashcard (RF-29, RF-32)

## Visão geral

`FlashcardService` reúne os cinco verbos de escrita: criar, editar, arquivar, restaurar e excluir logicamente. Toda entrada por id passa por `VerificadorDonoFlashcard` (tarefa 3.0) — o serviço nunca chama `findById`.

Três invariantes definem o comportamento. A **criação** persiste com `status` `ATIVO`, `estado` `NOVO`, o bloco FSRS zerado e `proximaRevisao` na data do `Clock` injetado — nada aqui calcula agendamento. A **edição nunca reagenda**: corrigir uma vírgula não pode apagar o que o FSRS aprendeu, então o `PUT` não escreve em nenhuma coluna do bloco de agendamento, e `questaoOrigemId`/`sessaoOrigemId` são imutáveis (enviá-los diferentes não altera nada e não é erro). A **exclusão é lógica**: `status` vai a `EXCLUIDO`, `excluido_em` recebe o instante, e nenhum `delete` é emitido — as linhas de `flashcard_revisao` continuam onde estão (CA-12).

O contador `criadosNaSessao` vem de uma contagem por `sessaoOrigemId` no banco, não de um número acumulado em memória: é isso que faz recarregar a página no meio da sessão não perder nem duplicar CA-08. Na criação avulsa ele é `null`, não zero.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito puros sobre o serviço, com repositórios e colaboradores mockados; nenhum contexto Spring. `Clock.fixed` para toda asserção de data.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras relevantes: regra 2 (`@PreAuthorize` no servidor, mesmo com a interface já restringindo), regra 7 (privacidade do dono por construção — toda operação parte do `usuarioId` do token e passa pelo verificador; o algoritmo FSRS **não é tocado**: nada aqui calcula agendamento) e regra 3 (mensagens de erro em pt-BR com ponto final). `code-standards.md` §2.4 (objeto parâmetro em vez de listas longas de argumentos), §3 (DTOs em `record`, entidade JPA nunca cruza o controller, injeção por construtor com campos `private final`, `@Transactional` no serviço). `tests.md` §1: a preservação do histórico na exclusão é **prioridade alta**.
</rules>

<requirements>
- `FlashcardService` com `@PreAuthorize("isAuthenticated()")` e `@Transactional` nos verbos de escrita; `Clock` injetado.
- `criar(UUID usuarioId, RequisicaoFlashcard) -> RespostaFlashcardCriado`: persiste com `status` `ATIVO`, `estado` `NOVO`, bloco FSRS nos padrões e `proximaRevisao` na data do `Clock`.
- A taxonomia da criação vem de `ResolvedorContextoFlashcard`: com origem, `disciplinaId` e `assuntoId` são os da questão e os do corpo são ignorados (CA-03).
- Frente e verso: recorte de espaços **antes** da medida de tamanho; mínimo de 3, máximo de 1000 (frente) e 4000 (verso). Falha lança `DadosInvalidosException` com `fieldErrors` campo a campo e **nada é persistido** (CA-04).
- Tags normalizadas por `NormalizadorTags` antes de persistir.
- `criadosNaSessao` é a contagem de cards com aquela `sessaoOrigemId` no banco; `null` na criação avulsa.
- `atualizar(UUID id, UUID usuarioId, RequisicaoFlashcard) -> FlashcardResumo`: altera frente, verso, deck e tags sempre; disciplina e assunto **só em card avulso**. Não escreve em `estabilidade`, `dificuldade`, `intervalo_dias`, `repeticoes`, `lapsos`, `estado`, `ultima_revisao` nem `proxima_revisao`.
- `questaoOrigemId` e `sessaoOrigemId` são imutáveis no `PUT`: valores diferentes são ignorados, sem erro.
- `arquivar(UUID id, UUID usuarioId)`: `status` `ARQUIVADO` e `arquivado_em` com o `Clock`. Arquivar um já arquivado lança `ConflitoException`, sem efeito colateral.
- `restaurar(UUID id, UUID usuarioId)`: volta a `ATIVO` e limpa `arquivado_em`. Restaurar um ativo lança `ConflitoException`.
- `excluir(UUID id, UUID usuarioId)`: `status` `EXCLUIDO` e `excluido_em` preenchido. **Nenhum `delete`** em `flashcard` nem em `flashcard_revisao`.
- Card de outro usuário em qualquer verbo: `RecursoNaoEncontradoException` e o repositório de escrita **não é chamado**.
- DTOs `RequisicaoFlashcard`, `FlashcardResumo`, `RespostaFlashcardCriado`, `OrigemResumo` e `ReferenciaResumo` como `record`, com `origem` nula inteira em card avulso — nunca um objeto de campos nulos.
</requirements>

## Subtarefas

- [x] 4.1 Criar os DTOs `RequisicaoFlashcard`, `FlashcardResumo`, `RespostaFlashcardCriado`, `OrigemResumo` e `ReferenciaResumo` em `flashcard/dto/`.
- [x] 4.2 Implementar `FlashcardService.criar`, com o resolvedor de origem, a normalização de tags e a validação de frente e verso.
- [x] 4.3 Implementar a contagem `criadosNaSessao` por `sessaoOrigemId` no repositório.
- [x] 4.4 Implementar `FlashcardService.atualizar`, preservando o bloco de agendamento e a origem.
- [x] 4.5 Implementar `arquivar`, `restaurar` e `excluir`, com as transições e o conflito de transição sem efeito.
- [x] 4.6 Escrever TU-07…TU-20.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (assinaturas de `FlashcardService`), *Modelos de dados* (`RequisicaoFlashcard`, `FlashcardResumo`, `RespostaFlashcardCriado`, `OrigemResumo`), *Endpoints da API* (`POST`, `PUT`, `DELETE`, `arquivar` e `restaurar`, com as notas sobre a edição que não reagenda e sobre a exclusão que preserva o histórico) e *Principais decisões* (o contador da sessão vem do servidor; a edição nunca reagenda; `status` como estado único).

## Critérios de aceitação relacionados

- CA-03
- CA-04
- CA-05
- CA-08
- CA-11
- CA-12
- CA-13
- CA-14
- CA-15

## Testes da tarefa

### Testes de unidade

- [x] TU-07 — `FlashcardService.criar` a partir de uma questão
- [x] TU-08 — `FlashcardService.criar` herdando a taxonomia da origem
- [x] TU-09 — `FlashcardService.criar` com frente de 2 caracteres
- [x] TU-10 — `FlashcardService.criar` com verso só de espaços
- [x] TU-11 — `FlashcardService.criar` avulso
- [x] TU-12 — `FlashcardService.criar` devolve o total da sessão
- [x] TU-13 — `FlashcardService.criar` avulso não conta sessão
- [x] TU-14 — `FlashcardService.atualizar` frente, verso, deck e tags
- [x] TU-15 — `FlashcardService.atualizar` tentando trocar a origem
- [x] TU-16 — `FlashcardService.arquivar` card ativo
- [x] TU-17 — `FlashcardService.arquivar` card já arquivado
- [x] TU-18 — `FlashcardService.restaurar` card arquivado
- [x] TU-19 — `FlashcardService.excluir` card com histórico
- [x] TU-20 — `FlashcardService.excluir` card de outro usuário

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/flashcard/FlashcardService.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/RequisicaoFlashcard.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/FlashcardResumo.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/RespostaFlashcardCriado.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/OrigemResumo.java`
- `backend/src/main/java/br/com/certame/flashcard/dto/ReferenciaResumo.java`
- `backend/src/test/java/br/com/certame/flashcard/FlashcardServiceTest.java`
