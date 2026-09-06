# Tarefa 5.0: Questão e alternativas (RF-19)

## Visão geral

Implementa o agregado central da funcionalidade: criação, leitura, edição e exclusão lógica da questão com suas alternativas, mais a regra pura de validação de publicação.

A ordem interna importa. O `ValidadorPublicacao` vem **primeiro**, porque é o coração de CA-06 e não depende de Spring, repositório nem transação: recebe uma `Questao` e devolve o mapa de pendências — todas de uma vez, nunca a primeira. Depois vêm `GeradorCodigoQuestao`, `QuestaoService` e o controller, que só orquestra e mapeia DTO.

Duas decisões moldam o serviço: as **letras das alternativas são atribuídas pelo servidor a partir da `ordem`** (o cliente envia texto e posição, `A`–`E` é derivado), e a **exclusão é lógica e bloqueada quando há respostas**, porque o acervo é referenciado por respostas, cadernos, simulados e flashcards.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `ValidadorPublicacao` testado como função pura, sem mock algum; `QuestaoService` com JUnit + Mockito e repositórios mockados; `@WebMvcTest` com `MockMvcTester` nos cenários de contrato; `Clock` injetado em vez de `Instant.now()`; AssertJ nas asserções.
- `postgresql-table-design` — a normalização de letras é respaldada pelo `unique (questao_id, letra)` e `(questao_id, ordem)` da tarefa 2.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: regra 2 (cadastrar, editar e excluir exigem `ROLE_PROFESSOR`+ por `@PreAuthorize` no serviço, validado no servidor mesmo com o botão escondido), regra 3 (`QuestaoService`, `ValidadorPublicacao`, colunas `enunciado`, `gabarito`, `dificuldade`; mensagens de validação em pt-BR), regra 4 (`DELETE /admin/questoes/{id}` é acréscimo ao contrato, já registrado na §6 pela tarefa 3.0), regra 6 (`resposta` é imutável — a exclusão de questão **nunca** a toca; ela é recusada quando há respostas) e "domínio + testes antes do controller". Nenhum desvio.
</rules>

<requirements>
- **RF-19** — Formulário com tipo (`ME` A–E ou `CE`), disciplina, assunto, banca, ano, órgão, dificuldade, enunciado, alternativas, gabarito e comentário.
- **`ValidadorPublicacao.validar(Questao) -> Map<String,String>`** — regra pura, sem dependência de Spring. Mapa vazio = publicável. Verifica, **numa só passada**: gabarito presente, comentário presente e não vazio, e ao menos duas alternativas quando o tipo é `ME`. Em `CE`, ausência de alternativas não é pendência, e o gabarito só aceita `CERTO` ou `ERRADO` (CA-04, CA-06).
- **`QuestaoService.criar`** — a questão **sempre nasce em `RASCUNHO`**, mesmo com todos os campos preenchidos (CA-05); exige apenas tipo e enunciado. O `autorId` vem do token, nunca do cliente — é esse vínculo que RN-03 usa depois.
- **`GeradorCodigoQuestao`** envolve a sequência `questao_codigo_seq`; formato `Q-#####` (`10000` → `Q-10000`).
- Criar a questão cria também a linha zerada em `questao_estatistica`.
- **Normalização das alternativas**: ordem 1..n vira `A`..`E`, mesmo que o cliente envie letras erradas ou ausentes. Mais de cinco alternativas → `DadosInvalidosException`, nada persistido (CA-03).
- Assunto que não pertence à disciplina selecionada → 422 com `fieldErrors.assuntoId` (CA-03).
- Referência de catálogo inexistente (`disciplinaId`, `assuntoId`, `bancaId`, `orgaoId`) → 404 `RECURSO_NAO_ENCONTRADO`.
- **`QuestaoService.atualizar`** edita o agregado inteiro **sem tocar no status nem em `publicadoEm`** (fluxo *Corrigir* do PRD: a questão publicada permanece publicada). Numa questão `PUBLICADA`, valem também as regras de publicação — não é possível esvaziar o gabarito de uma questão no ar.
- Trocar `tipo` de `ME` para `CE` **descarta as alternativas e limpa o gabarito**, que passa a exigir `CERTO`/`ERRADO`; a interface avisa antes de aplicar (CA-04). As alternativas são substituídas por completo a cada `PUT`.
- **`QuestaoService.excluir`** é lógica (marca `excluido_em`); lança `ConflitoException` `QUESTAO_EM_USO` (409) se já existem respostas registradas — `excluido_em` permanece nulo. Nenhuma exclusão física em cascata.
- Eventos de log `INFO` para criação, edição e exclusão, sempre com `id`, `codigo`, ator e ação — **nunca** com o enunciado ou o comentário completos.
</requirements>

## Subtarefas

- [x] 5.1 Criar os DTOs `QuestaoDetalhe`, `RequisicaoQuestao` e `AlternativaDto` em `questao/dto/`, com Bean Validation conforme as tabelas da TechSpec.
- [x] 5.2 Implementar `ValidadorPublicacao` como regra pura, com TU-01 a TU-07.
- [x] 5.3 Implementar `GeradorCodigoQuestao` sobre a sequência, com TU-30.
- [x] 5.4 Implementar `QuestaoService.criar`, com autor do token, código gerado, status `RASCUNHO` e a linha zerada em `questao_estatistica`, com TU-08 e TU-09.
- [x] 5.5 Implementar a normalização e a validação das alternativas (letras pela ordem, máximo de cinco), com TU-10 e TU-11.
- [x] 5.6 Implementar a resolução das referências de catálogo, incluindo a checagem de assunto pertencente à disciplina, com TU-13.
- [x] 5.7 Implementar `QuestaoService.atualizar`, preservando status e `publicadoEm`, com a troca `ME` → `CE`, com TU-12.
- [x] 5.8 Implementar `QuestaoService.buscarPorId` (com `@EntityGraph` nas alternativas) e `QuestaoService.excluir` (lógica, recusada com respostas), com TU-14.
- [x] 5.9 Implementar `QuestaoAdminController` com `GET /{id}`, `POST`, `PUT /{id}` e `DELETE /{id}`, sob `@PreAuthorize` de professor+, devolvendo `Location` no 201.
- [x] 5.10 Registrar auditoria na criação, edição e exclusão, e os eventos de log sem enunciado nem comentário.
- [x] 5.11 Escrever os testes de integração TI-08 a TI-11, TI-17 e TI-18.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`QuestaoService`, `ValidadorPublicacao`), *Modelos de dados* (`QuestaoDetalhe`, `RequisicaoQuestao`, `AlternativaDto`), *Endpoints da API* (`POST`, `PUT` e `DELETE /admin/questoes`, com os exemplos de rascunho mínimo e de assunto de outra disciplina) e *Principais decisões* (`ValidadorPublicacao` como regra pura, letras atribuídas pelo servidor a partir da `ordem`, exclusão lógica bloqueada quando há respostas, duplicar questão fora do escopo).

## Critérios de aceitação relacionados

- CA-03
- CA-04
- CA-05
- CA-06

## Testes da tarefa

### Testes de unidade

- [ ] TU-01 — `ValidadorPublicacao` com questão `ME` completa
- [ ] TU-02 — `ValidadorPublicacao` sem gabarito
- [ ] TU-03 — `ValidadorPublicacao` sem comentário
- [ ] TU-04 — `ValidadorPublicacao` com uma única alternativa em `ME`
- [ ] TU-05 — `ValidadorPublicacao` com as três faltas simultâneas
- [ ] TU-06 — `ValidadorPublicacao` em questão `CE` sem alternativas
- [ ] TU-07 — `ValidadorPublicacao` em `CE` com gabarito `B`
- [ ] TU-08 — `QuestaoService.criar` só com tipo e enunciado
- [ ] TU-09 — `QuestaoService.criar` atribui autor do token e código gerado
- [ ] TU-10 — `QuestaoService` normaliza letras das alternativas pela ordem
- [ ] TU-11 — `QuestaoService.criar` com seis alternativas
- [ ] TU-12 — `QuestaoService` troca `ME` → `CE`
- [ ] TU-13 — `QuestaoService.criar` com assunto de outra disciplina
- [ ] TU-14 — `QuestaoService.excluir` com respostas registradas
- [ ] TU-30 — `GeradorCodigoQuestao` formata a sequência

### Testes de integração

- [ ] TI-08 — `POST /admin/questoes` com rascunho mínimo
- [ ] TI-09 — `POST /admin/questoes` com cinco alternativas e gabarito
- [ ] TI-10 — `POST /admin/questoes` tipo `CE`
- [ ] TI-11 — `PUT /admin/questoes/{id}` em questão publicada
- [ ] TI-17 — `DELETE /admin/questoes/{id}` com respostas
- [ ] TI-18 — `DELETE /admin/questoes/{id}` sem respostas

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/questao/ValidadorPublicacao.java`
- `backend/src/main/java/br/com/certame/questao/GeradorCodigoQuestao.java`
- `backend/src/main/java/br/com/certame/questao/QuestaoService.java`
- `backend/src/main/java/br/com/certame/questao/QuestaoAdminController.java`
- `backend/src/main/java/br/com/certame/questao/dto/`
