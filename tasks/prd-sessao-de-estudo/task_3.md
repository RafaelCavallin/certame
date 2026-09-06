# Tarefa 3.0: Serviços de sessão (abrir, retomar, trocar modo, responder, finalizar, marcar)

## Visão geral

Implementa os serviços transacionais que orquestram o ciclo de vida da sessão de estudo: abertura e retomada, troca de modo Foco↔Contexto, registro de resposta única fora do Modo Prova, finalização com correção em lote no Modo Prova, e as marcações de favorita/erro recorrente. É onde moram as regras centrais do PRD — CA-02, CA-05, CA-06, CA-07, CA-09 e CA-11 — construídas sobre as regras puras da Tarefa 2.0 e as entidades da Tarefa 1.0.

<skills>
### Conformidade com skills

- `spring-boot-testing` — serviços testados com dependências mockadas à mão (Mockito), sem contexto Spring; `@MockitoBean` reservado para os slices de controller da Tarefa 5.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `code-standards.md` e `tests.md` lidos.

- Regra 2 (`AGENTS.md`) — `@PreAuthorize("isAuthenticated()")` nos serviços; toda consulta leva `usuarioId` do token e responde `RecursoNaoEncontradoException` (404), nunca 403, para recurso de terceiro.
- Regra 6 (`AGENTS.md`) — Modo Prova só grava respostas na finalização, numa transação (RN-04); CA-10 é verdadeiro por ausência de escrita, não por rotina de limpeza.
- `code-standards.md` §3 — regra de negócio e `@Transactional` no serviço; controller (Tarefa 5.0) só orquestra; erros por `ErroNegocioException`.
- Um único ponto de escrita em `resposta` por modo (decisão da TechSpec): fora da prova, `RegistroRespostaService`; na prova, `CorrecaoProvaService`. `RequisicaoFinalizacao` com lote fora da prova responde 422.
</rules>

<requirements>
- RF-07, RF-08, RF-09, RF-10, RF-11
- RN-01, RN-02, RN-03, RN-04
</requirements>

## Subtarefas

- [x] 3.1 Implementar `SessaoEstudoService.abrir` (RF-08, CA-03, CA-16): sorteia a série via `MontadorSerie`, persiste em `sessao_questao`, sessão nasce em `FOCO` salvo `PROVA` explícito.
- [x] 3.2 Implementar `SessaoEstudoService.retomar`/`retomavel` (CA-02): primeira questão sem resposta, descarte e reindexação de questões despublicadas na janela de 7 dias, 404 para sessão de outro usuário.
- [x] 3.3 Implementar `SessaoEstudoService.trocarModo` (CA-06): `PATCH` que só troca a coluna `modo`; recusa qualquer troca envolvendo `PROVA` (`ModoNaoAlteravelException`, 409).
- [x] 3.4 Implementar `RegistroRespostaService.registrar` (RF-07, RN-02, CA-05): grava a resposta única, recusa segunda resposta (`RespostaDuplicadaException`) e resposta em sessão `PROVA` ou encerrada; devolve gabarito, comentário e estatística (recálculo síncrono via `EstatisticaQuestaoService`, `null` quando só o autor respondeu).
- [x] 3.5 Implementar `CorrecaoProvaService.corrigir` (RF-09, RF-10, RN-04): grava o lote inteiro numa transação, monta a correção (percentual sobre o total, acertos, erros, não respondidas, revisão item a item com `marcada`/`correta` nulos nas não respondidas), publica `SessaoFinalizada` após o commit.
- [x] 3.6 Implementar `MarcacaoService.aplicar`/`marcadasDa` (RF-11, CA-11): liga/desliga favorita e erro recorrente de forma idempotente.
- [x] 3.7 Implementar as exceções de negócio `SessaoEncerradaException`, `ModoNaoAlteravelException`, `RespostaDuplicadaException` (o `ManipuladorErrosGlobal` não muda).

## Detalhes de implementação

Ver TechSpec, "Principais interfaces" para as assinaturas de `SessaoEstudoService`, `RegistroRespostaService`, `CorrecaoProvaService` e `MarcacaoService`; "Pontos de integração" para o recálculo de estatística síncrono vs. assíncrono e o evento `SessaoFinalizada`; e as notas sobre "Um único ponto de escrita em `resposta` por modo" e "Modo Prova é escolhido na abertura e não aceita troca" em "Principais decisões".

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-02
- CA-03
- CA-05
- CA-06
- CA-07
- CA-08
- CA-09
- CA-10
- CA-11
- CA-15

## Testes da tarefa

### Testes de unidade

- [x] TU-11 — `SessaoEstudoService` abre toda sessão em Foco quando o modo é omitido
- [x] TU-12 — `SessaoEstudoService` troca de Foco para Contexto preservando índice e respostas
- [x] TU-13 — `SessaoEstudoService` recusa a troca de modo envolvendo Prova
- [x] TU-14 — `SessaoEstudoService` recusa escrita em sessão finalizada
- [x] TU-15 — `SessaoEstudoService` retoma na primeira questão sem resposta
- [x] TU-16 — `SessaoEstudoService` retoma descartando questão despublicada
- [x] TU-17 — `SessaoEstudoService` não devolve sessão retomável de outro usuário
- [x] TU-18 — `RegistroRespostaService` grava questão, marcada, acerto, tempo e origem
- [x] TU-19 — `RegistroRespostaService` devolve gabarito, comentário e estatística
- [x] TU-20 — `RegistroRespostaService` devolve estatística nula quando só o autor respondeu
- [x] TU-21 — `RegistroRespostaService` recusa a segunda resposta da mesma questão
- [x] TU-22 — `RegistroRespostaService` recusa resposta em sessão de Modo Prova
- [x] TU-23 — `RegistroRespostaService` recusa questão fora da série
- [x] TU-24 — `CorrecaoProvaService` conta acertos, erros e não respondidas
- [x] TU-25 — `CorrecaoProvaService` grava o lote inteiro numa única chamada
- [x] TU-26 — `CorrecaoProvaService` monta a revisão na ordem da série com item em branco
- [x] TU-27 — `CorrecaoProvaService` de prova sem nenhuma resposta
- [x] TU-28 — `CorrecaoProvaService` publica `SessaoFinalizada` uma única vez
- [x] TU-29 — `MarcacaoService` liga favorita e erro recorrente de forma idempotente
- [x] TU-30 — `MarcacaoService` desliga só a marcação pedida

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/estudo/SessaoEstudoService.java`
- `backend/src/main/java/br/com/certame/estudo/RegistroRespostaService.java`
- `backend/src/main/java/br/com/certame/estudo/CorrecaoProvaService.java`
- `backend/src/main/java/br/com/certame/estudo/MarcacaoService.java`
- `backend/src/main/java/br/com/certame/comum/excecao/SessaoEncerradaException.java`
- `backend/src/main/java/br/com/certame/comum/excecao/ModoNaoAlteravelException.java`
- `backend/src/main/java/br/com/certame/comum/excecao/RespostaDuplicadaException.java`
- `backend/src/main/java/br/com/certame/comum/evento/SessaoFinalizada.java`
- `backend/src/test/java/br/com/certame/estudo/SessaoEstudoServiceTest.java`
- `backend/src/test/java/br/com/certame/estudo/RegistroRespostaServiceTest.java`
- `backend/src/test/java/br/com/certame/estudo/CorrecaoProvaServiceTest.java`
- `backend/src/test/java/br/com/certame/estudo/MarcacaoServiceTest.java`
