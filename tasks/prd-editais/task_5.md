# Tarefa 5.0: `ConsultaEditaisService`: agrupamento por status e lista administrativa (RF-17)

## Visão geral

Implementar a leitura. `ConsultaEditaisService.agrupados()` monta `EditaisAgrupados` com os três grupos **já ordenados no servidor** — abertos pelo prazo mais próximo, previstos por nome, encerrados pelo prazo mais recente primeiro (sem prazo ao fim). É essa decisão que torna CA-01 e CA-06 propriedades observáveis do contrato, sem uma linha de lógica no Angular, e faz CA-09 cair sozinho: três listas vazias **são** o estado vazio, não um caso especial.

O serviço entrega também `listarParaAdministracao()`, com o `prazoVencido` calculado pelo `VerificadorPrazoEdital` (borda 2), e `buscarParaEdicao(id)` para o formulário. É o passo 5 da ordem de construção.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito puros, sem contexto Spring; `Clock.fixed` em TU-22; AssertJ nas asserções.
- `postgresql-table-design` — o `edital_status_idx` da tarefa 2.0 está alinhado exatamente à consulta de agrupamento; o `@EntityGraph` do repositório evita o N+1.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 2** — a leitura do estudante exige apenas autenticação, sem `@PreAuthorize` de perfil; a leitura administrativa é restrita a administrador ou superior, aplicada no controller da tarefa 6.0.
- `code-standards.md` §2 — a separação leitura/escrita entre este serviço e o `EditalService` é o que mantém ambos dentro do limite de 100 linhas.
- `code-standards.md` §3 — somente leitura (`@Transactional(readOnly = true)`), injeção por construtor, entidade nunca exposta: a saída é sempre DTO.
- `tests.md` §2, *Repeatable* — `ORDER BY` explícito nas três consultas de agrupamento; nenhuma ordenação implícita do banco.
- **PRD, "Zero informação obsoleta visível"** — nada aqui reescreve status: `prazoVencido` é sinal, não correção.
</rules>

<requirements>
- **RF-17** — editais acompanhados com status e disciplinas exigidas.
- `agrupados() -> EditaisAgrupados` com `abertos`, `previstos` e `encerrados` **sempre presentes**, nunca `null`, e vazios quando não houver edital (CA-09).
- Ordenação: abertos pelo prazo mais próximo; previstos por nome; encerrados pelo prazo mais recente primeiro, com os sem prazo ao fim.
- Nenhum edital aparece em dois grupos; editais com `excluido_em` preenchido não aparecem em nenhum.
- `listarParaAdministracao() -> List<EditalAdminResumo>` com `totalDisciplinas` (contagem, não a lista), `prazoVencido` e `atualizadoEm`. Ordenação fixa: `ABERTO`, `PREVISTO`, `ENCERRADO`; dentro de cada status, pelo prazo mais próximo e depois por nome.
- `buscarParaEdicao(UUID) -> EditalDetalhe`; id inexistente ou excluído → `RecursoNaoEncontradoException`.
- As disciplinas de cada edital vêm ordenadas por nome, carregadas em uma consulta com `@EntityGraph`.
- Nenhuma rota é paginada — o universo é de dezenas de editais.
</requirements>

## Subtarefas

- [x] 5.1 Implementar `agrupados()` distribuindo os editais não excluídos nos três grupos, com `ORDER BY` explícito em cada consulta.
- [x] 5.2 Garantir que os três grupos sejam listas vazias, nunca `null`, na base sem editais.
- [x] 5.3 Mapear `Edital` para `EditalResumo`, com `banca`, `orgao` e `disciplinas` como referências e campo ausente normalizado para `null`.
- [x] 5.4 Implementar `listarParaAdministracao()` com `totalDisciplinas`, `atualizadoEm` e `prazoVencido` vindo do `VerificadorPrazoEdital`.
- [x] 5.5 Implementar `buscarParaEdicao(id)` devolvendo `EditalDetalhe` e recusando id excluído.
- [x] 5.6 Confirmar a ausência de N+1 com o `@EntityGraph` do `EditalRepository`.
- [x] 5.7 Escrever TU-18…TU-22.

## Detalhes de implementação

Ver `techspec.md` §*Resumo* (por que o agrupamento é do servidor), §*Design de implementação* → *Principais interfaces* (`ConsultaEditaisService`), §*Modelos de dados* (`EditaisAgrupados`, `EditalResumo`, `EditalAdminResumo`) e §*Sequenciamento do desenvolvimento*, passo 5.

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-09
- Caso de borda 2 do PRD

## Testes da tarefa

### Testes de unidade

- [x] TU-18 — `ConsultaEditaisService.agrupados` distribui os três status
- [x] TU-19 — `ConsultaEditaisService` ordena os abertos pelo prazo mais próximo
- [x] TU-20 — `ConsultaEditaisService` ordena previstos por nome e encerrados pelo prazo mais recente
- [x] TU-21 — `ConsultaEditaisService.agrupados` sem nenhum edital
- [x] TU-22 — `ConsultaEditaisService.listarParaAdministracao` marca o prazo vencido

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/catalogo/edital/ConsultaEditaisService.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/edital/dto/EditaisAgrupados.java`, `EditalResumo.java`, `EditalAdminResumo.java`, `EditalDetalhe.java` (tarefa 4.0)
- `backend/src/main/java/br/com/certame/catalogo/edital/EditalRepository.java` (tarefa 2.0)
- `backend/src/main/java/br/com/certame/catalogo/edital/VerificadorPrazoEdital.java` (tarefa 3.0)
