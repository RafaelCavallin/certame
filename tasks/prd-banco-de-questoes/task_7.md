# Tarefa 7.0: Busca e listagem administrativa (RF-18)

## Visão geral

A tabela de trabalho do time editorial: lista paginada com busca por texto e filtros combinados, sob um orçamento de **800 ms (p95) com 100 mil questões na base**.

Fica por último no backend porque só faz sentido medi-la sobre o acervo produzido pelos passos anteriores (TI-06). O `BuscaQuestaoService` combina duas estratégias numa consulta só: termo em formato de código vira **busca por prefixo de `codigo`** — porque `Q-88409` como `plainto_tsquery` não acha nada, e o fluxo *Corrigir* do PRD começa exatamente por essa busca — e qualquer outro termo vira `plainto_tsquery('portuguese', …)` sobre a coluna gerada `busca`.

A paginação é **offset com contagem exata**, não keyset: CA-02 exige paginação recalculada e o protótipo mostra o total explícito, coisas que keyset não dá.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito puros no `BuscaQuestaoService` (a composição de predicados e a heurística de código são testáveis sem banco); `@WebMvcTest` com `MockMvcTester` nos cenários de contrato; Testcontainers com PostgreSQL 18 real nos cenários que exercitam `tsvector` e plano de consulta (TI-05, TI-06) — testá-los em outro motor não provaria nada.
- `postgresql-table-design` — o orçamento é sustentado por `questao_filtro_idx` e pelo índice GIN criados na tarefa 2.0; TI-06 confirma o uso pelo `explain`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: regra 2 (a rota exige `ROLE_PROFESSOR`+; TI-07 prova o 403 para `ESTUDANTE` decidido no servidor), regra 3 (`BuscaQuestaoService`, mensagens de validação em pt-BR) e regra 4 (`GET /admin/questoes` já consta da §6). Nenhum desvio.
</rules>

<requirements>
- **RF-18** — Listagem com código, enunciado resumido, disciplina, banca/ano, tipo e status, com busca por texto e filtros, paginada.
- **`BuscaQuestaoService.buscar(FiltroQuestao, Paginacao) -> PaginaResposta<QuestaoResumo>`**.
- **Heurística de termo**: `q` que case com `^[Qq]-?\d+$` vira busca por prefixo de `codigo`; qualquer outro vira `plainto_tsquery('portuguese', …)` sobre `busca`. Máximo de 200 caracteres.
- Filtros combináveis por `and`: `disciplina`, `assunto`, `banca`, `ano`, `tipo` e `status` (CA-02).
- Paginação: `page` base zero (≥ 0), `size` de 1 a 100 com padrão 20. Envelope `{content, page, size, totalElements}` com **contagem exata**, recalculada a cada consulta.
- Ordenação por **lista branca**: `atualizadoEm,desc` (padrão), `codigo`, `criadoEm`. Campo fora da lista → `DadosInvalidosException` — nenhuma coluna arbitrária chega ao SQL.
- Parâmetro inválido (`size` fora da faixa, `status`/`tipo` desconhecido, `sort` inválido) → 422 `DADOS_INVALIDOS` com `fieldErrors`.
- Sem correspondência → 200 com `content` vazio e `totalElements` zero; a tela exibe o estado vazio e desabilita "Próxima".
- Perfil `ESTUDANTE` → 403 `ACESSO_NEGADO`.
- A busca textual casa acento e flexão (`licitações` × `licitacao`) por `to_tsvector('portuguese')`.
- **Orçamento de desempenho** (CA-01): primeira página abaixo de 800 ms (p95) com 100 mil questões; o `explain` confirma o uso de `questao_filtro_idx`.
- A busca cobre **apenas o enunciado**, não o comentário nem as alternativas — limitação aceita na v1 e registrada em *Riscos conhecidos*.
- Métrica `certame.questao.busca` (*timer* p95/p99, tags `comFiltro` e `comTermo`), que verifica o orçamento em produção e não só em teste.
</requirements>

## Subtarefas

- [x] 7.1 Criar os DTOs `QuestaoResumo` e `FiltroQuestao` em `questao/dto/`, com Bean Validation nos parâmetros de consulta.
- [x] 7.2 Implementar a heurística de termo (código por prefixo × busca textual), com TU-20.
- [x] 7.3 Implementar a composição de predicados dos filtros por `and`, com TU-21.
- [x] 7.4 Implementar a lista branca de ordenação, com TU-22.
- [x] 7.5 Implementar a consulta paginada com contagem exata, devolvendo `PaginaResposta<QuestaoResumo>`.
- [x] 7.6 Acrescentar `GET /admin/questoes` ao `QuestaoAdminController`, sob `@PreAuthorize` de professor+.
- [x] 7.7 Registrar a métrica `certame.questao.busca` com as tags `comFiltro` e `comTermo`.
- [x] 7.8 Escrever os testes de integração TI-02 a TI-07, incluindo a carga em massa de TI-06 por `INSERT ... SELECT generate_series`, gerada uma vez por classe e marcada como lenta.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`BuscaQuestaoService`), *Modelos de dados* (`QuestaoResumo`, `PaginaResposta<T>`), *Endpoints da API* (`GET /api/v1/admin/questoes`, com a tabela de parâmetros de consulta e os três exemplos: filtros combinados, nenhuma correspondência e parâmetro inválido), *Principais decisões* (paginação por offset com contagem exata, `tsvector` gerado e materializado, termo em formato de código vira prefixo) e *Riscos conhecidos* (o orçamento de 800 ms e os escapes já mapeados; a busca cobrir só o enunciado).

## Critérios de aceitação relacionados

- CA-01
- CA-02

## Testes da tarefa

### Testes de unidade

- [x] TU-20 — `BuscaQuestaoService` reconhece termo em formato de código
- [x] TU-21 — `BuscaQuestaoService` combina termo e três filtros
- [x] TU-22 — `BuscaQuestaoService` rejeita `sort` fora da lista branca

### Testes de integração

- [ ] TI-02 — `GET /admin/questoes` sem filtros (`@WebMvcTest` + `MockMvcTester`)
- [ ] TI-03 — `GET /admin/questoes` com termo, disciplina, banca e status
- [ ] TI-04 — `GET /admin/questoes?q=Q-88409`
- [ ] TI-05 — Busca textual com acento e flexão (`licitações` × `licitacao`)
- [ ] TI-06 — `GET /admin/questoes` com 100 mil questões semeadas
- [ ] TI-07 — `GET /admin/questoes` como `ESTUDANTE`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/questao/BuscaQuestaoService.java`
- `backend/src/main/java/br/com/certame/questao/QuestaoRepository.java` (consulta paginada com `tsvector`)
- `backend/src/main/java/br/com/certame/questao/QuestaoAdminController.java` (modificado)
- `backend/src/main/java/br/com/certame/questao/dto/`
