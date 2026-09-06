# Tarefa 3.0: Regras de domínio puras e contexto de origem (RN-07, RN-09)

## Visão geral

Duas regras que não dependem de Spring e uma que isola a única integridade referencial da criação.

`VerificadorDonoFlashcard` é o componente mais crítico do documento: é o **ponto único de entrada por id** de toda a funcionalidade. Nenhum serviço chama `findById` diretamente; todos passam por `exigirProprio(id, usuarioId)`, que devolve o card ou lança `RecursoNaoEncontradoException`. Card de terceiro responde `404`, **nunca `403`** — `403` confirmaria a existência do card e derrubaria CA-15. O filtro por `usuario_id` vive dentro do `where`, não em uma checagem posterior, para que a resposta seja indistinguível também no tempo.

`NormalizadorTags` é regra pura: recorta, minúsculiza, remove duplicatas e vazios, e recusa acima de 10 tags em vez de truncar em silêncio.

`ResolvedorContextoFlashcard` valida `questaoOrigemId` e `sessaoOrigemId`, herda `disciplinaId` e `assuntoId` da questão quando há origem, e recusa origem que não seja do usuário — sempre com a mesma mensagem da origem inexistente, pelo mesmo motivo de CA-15. Como `sessao_estudo` ainda não tem serviço, ele consulta a tabela por uma porta `SessaoDoUsuario` de uma linha, que F2 substitui pelo serviço dele na entrega própria.

<skills>
### Conformidade com skills

- `spring-boot-testing` — a fatia mais estreita por cenário: JUnit + Mockito puros, sem contexto Spring. As três classes são objetos comuns com dependências injetadas por construtor; nenhuma precisa de `@SpringBootTest`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras relevantes: regra 7 (flashcard é privado do dono; toda consulta filtra por `usuario_id` do token e responde 404 para card de terceiro — é literalmente o que `VerificadorDonoFlashcard` implementa), regra 2 (autorização no servidor) e regra 3 (mensagens ao usuário em pt-BR com ponto final; identificadores sem acento). `code-standards.md` §2 (arquivo ≤ 100 linhas, método ≤ 30) é a razão de as três regras serem classes separadas em vez de métodos privados do serviço. `tests.md` §1: a autorização por dono é **prioridade alta** — caminho feliz e todas as bordas.

**Bloqueio conhecido:** a porta `SessaoDoUsuario` é provisória. Quando `prd-sessao-de-estudo` entregar o serviço de sessão, ela é substituída sem mudar o contrato de `ResolvedorContextoFlashcard`.
</rules>

<requirements>
- `VerificadorDonoFlashcard.exigirProprio(UUID id, UUID usuarioId) -> Flashcard`: **uma única consulta**, já filtrada por `usuarioId` e por `status <> 'EXCLUIDO'`. Card inexistente, excluído e de outro usuário produzem a **mesma** exceção com a **mesma** mensagem.
- A exceção é `RecursoNaoEncontradoException` (`RECURSO_NAO_ENCONTRADO`), reutilizada de `prd-banco-de-questoes`. Nunca `AcessoNegado`, em nenhum caminho.
- Nenhum outro componente da funcionalidade pode chamar `FlashcardRepository.findById`; a regra vale também para os serviços das tarefas 4.0 e 5.0.
- `NormalizadorTags.normalizar(List<String>) -> List<String>`: recorta espaços, minúsculiza, remove duplicatas e vazios preservando a ordem de entrada; nulo vira lista vazia; nunca devolve `null`.
- Acima de 10 tags, ou tag acima de 30 caracteres, lança `DadosInvalidosException` com `fieldErrors.tags` — nada é truncado em silêncio.
- `ResolvedorContextoFlashcard.resolver(UUID usuarioId, RequisicaoFlashcard) -> ContextoOrigem`: sem origem, devolve contexto vazio **sem consultar** `questao` nem `sessao_estudo`.
- Com `questaoOrigemId`, a questão precisa existir e estar publicada; `disciplinaId` e `assuntoId` são herdados dela e os valores do corpo são **ignorados** (CA-03).
- `sessaoOrigemId` só é aceito junto de `questaoOrigemId` e precisa ser sessão do próprio usuário; sessão de terceiro e sessão inexistente devolvem a mesma mensagem em `fieldErrors.sessaoOrigemId`.
- Porta `SessaoDoUsuario` com um único método, isolando a dependência de F2 em uma interface.
- Sem `Instant.now()` embutido: todo instante vem de `Clock` injetado (`tests.md` §2, *Repeatable*).
</requirements>

## Subtarefas

- [x] 3.1 Criar `VerificadorDonoFlashcard` com `exigirProprio`, sobre a consulta filtrada por dono do `FlashcardRepository`.
- [x] 3.2 Criar `NormalizadorTags` como regra pura, sem Spring e sem repositório.
- [x] 3.3 Criar a porta `SessaoDoUsuario` e a implementação provisória sobre `sessao_estudo`.
- [x] 3.4 Criar `ResolvedorContextoFlashcard` com a validação das duas origens e a herança de taxonomia.
- [x] 3.5 Escrever TU-01…TU-06.
- [x] 3.6 Escrever TU-21…TU-24.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (assinaturas de `VerificadorDonoFlashcard`, `NormalizadorTags` e `ResolvedorContextoFlashcard`), *Modelos de dados — `RequisicaoFlashcard`* (regras de cada campo) e *Principais decisões* (`404`, nunca `403`, para card de terceiro; não existe rota administrativa de flashcard). O `check flashcard_sessao_exige_questao` da tarefa 2.0 é a segunda barreira da mesma regra que TU-23 verifica na camada de domínio.

## Critérios de aceitação relacionados

- CA-03
- CA-12
- CA-13
- CA-14
- CA-15

## Testes da tarefa

### Testes de unidade

- [x] TU-01 — `VerificadorDonoFlashcard` com card do próprio usuário
- [x] TU-02 — `VerificadorDonoFlashcard` com card de outro usuário
- [x] TU-03 — `VerificadorDonoFlashcard` com card já excluído
- [x] TU-04 — `NormalizadorTags` com maiúsculas, espaços e duplicatas
- [x] TU-05 — `NormalizadorTags` acima de 10 tags
- [x] TU-06 — `NormalizadorTags` com nulo, vazios e só espaços
- [x] TU-21 — `ResolvedorContextoFlashcard` com questão não publicada
- [x] TU-22 — `ResolvedorContextoFlashcard` com sessão de outro usuário
- [x] TU-23 — `ResolvedorContextoFlashcard` com sessão sem questão
- [x] TU-24 — `ResolvedorContextoFlashcard` sem nenhuma origem

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/flashcard/VerificadorDonoFlashcard.java`
- `backend/src/main/java/br/com/certame/flashcard/NormalizadorTags.java`
- `backend/src/main/java/br/com/certame/flashcard/ResolvedorContextoFlashcard.java`
- `backend/src/main/java/br/com/certame/flashcard/SessaoDoUsuario.java`
- `backend/src/test/java/br/com/certame/flashcard/VerificadorDonoFlashcardTest.java`
- `backend/src/test/java/br/com/certame/flashcard/NormalizadorTagsTest.java`
- `backend/src/test/java/br/com/certame/flashcard/ResolvedorContextoFlashcardTest.java`
