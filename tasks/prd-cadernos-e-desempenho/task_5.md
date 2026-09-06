# Tarefa 5.0: Porta `FonteRespostas`, agregados incrementais e reconciliador

## Visão geral

O motor que mantém os três agregados de desempenho em dia. `desempenho` não conhece o repositório de `estudo`: lê por uma porta, `FonteRespostas`, implementada em `estudo/resposta/RespostaAgregadoAdapter`, e é nessa porta que RN-04 é filtrado na origem — as três consultas exigem `sessao_estudo.finalizada_em is not null`, de modo que nenhuma tentativa de Modo Prova abandonada entra em métrica alguma (CA-12). O `AtualizadorAgregados` faz *upsert* idempotente; o `OuvinteSessaoFinalizada` roda **depois do commit** para que um erro de estatística nunca custe a sessão do estudante; e o `ReconciliadorAgregados`, de 15 minutos, é a rede de segurança.

<skills>
### Conformidade com skills

- `spring-boot-testing` — a porta permite testar `AtualizadorAgregados` em unidade com um *fake* de três métodos, sem banco; `@SpringBootTest` reservado ao `@TransactionalEventListener` e ao job.
- `postgresql-table-design` — *upsert* por `insert … on conflict do update`, apoiado nas PKs compostas e nas invariantes da tarefa 2.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 6 / RN-02:** nada aqui escreve em `resposta`, `sessao_estudo` ou `marcacao`. **RN-04:** o filtro de sessão finalizada mora na porta, não no serviço nem na tela — assim uma consulta futura não pode esquecê-lo.
- **`code-standards.md` §3:** um domínio não acessa o repositório de outro. `FonteRespostas` é declarada em `desempenho/` e implementada em `estudo/resposta/`. O evento `SessaoFinalizada` fica em `comum/evento/` porque publicador e assinante não devem se conhecer.
- **`tests.md` §1:** CA-12 é tratado como criticidade **alta** — é a diferença entre uma métrica honesta e uma mentira.
- **`tests.md` §2 (Repeatable):** `Clock` injetado onde houver tempo; sem `Thread.sleep` para esperar o job.
- `@EnableScheduling` nasce aqui, em `config/`.
</rules>

<requirements>
- **RF-15, RF-16** — `desempenho_semanal`, `desempenho_disciplina` e `desempenho_assunto` atualizados por *upsert* do próprio usuário.
- `AtualizadorAgregados.atualizar(usuarioId)` é **idempotente**: rodar duas vezes produz exatamente o mesmo resultado.
- **CA-12 / RN-04** — a `FonteRespostas` só enxerga respostas de sessões finalizadas.
- Questão **sem assunto** conta em `desempenho_disciplina` e fica fora de `desempenho_assunto`.
- `OuvinteSessaoFinalizada` é `@TransactionalEventListener(phase = AFTER_COMMIT)`; em falha, registra `desempenho_pendencia` e um `WARN`, e **não** propaga a exceção.
- `ReconciliadorAgregados` é `@Scheduled` de 15 minutos, consome a fila em lotes e apaga a pendência ao reconstruir.
- Semana fechada na segunda-feira, no fuso de `comum/ZonaAplicacao`.
- Métricas de observabilidade: `certame.desempenho.agregado.atualizado` (tag `origem`), `certame.desempenho.agregado.divergencia`, `certame.desempenho.pendencia.fila`.
</requirements>

## Subtarefas

- [x] 5.1 Porta `desempenho/FonteRespostas` com `porSemana`, `porDisciplina` e `porAssunto`, mais os tipos `TotalPorChave`, `TotalPorDisciplina` e `TotalPorAssunto`.
- [x] 5.2 `estudo/resposta/RespostaAgregadoAdapter`: as três consultas agregadas sobre `RespostaRepository`, com o filtro de sessão finalizada e `date_trunc` no fuso da aplicação.
- [x] 5.3 `comum/evento/SessaoFinalizada` — `record SessaoFinalizada(UUID usuarioId, UUID sessaoId, Instant finalizadaEm)`.
- [x] 5.4 `desempenho/AtualizadorAgregados`: *upsert* idempotente dos três agregados (TU-31…TU-34).
- [x] 5.5 `desempenho/OuvinteSessaoFinalizada` com `AFTER_COMMIT` e registro de pendência em falha (TU-35).
- [x] 5.6 `desempenho/ReconciliadorAgregados` `@Scheduled`, com `@EnableScheduling` já existente em `CertameApplication`.
- [x] 5.7 Instrumentar os contadores, *timers* e *gauges* de agregado e de fila.
- [x] 5.8 Testes TU-31…TU-35; TI-24 e TI-25 dependem das tabelas definitivas de F2, ainda ausentes.

## Detalhes de implementação

Ver `techspec.md`, §*Arquitetura do sistema* → *Backend* (linhas de `FonteRespostas`, `RespostaAgregadoAdapter`, `AtualizadorAgregados`, `OuvinteSessaoFinalizada`, `ReconciliadorAgregados`, `SessaoFinalizada`), §*Principais interfaces* (bloco de `FonteRespostas` e `AtualizadorAgregados`), §*Pontos de integração* (as duas linhas de `prd-sessao-de-estudo` e a de `desempenho → estudo`), §*Principais decisões* (agregados incrementais no lugar de MVs, RN-04 filtrado na origem, porta em vez de repositório, `AFTER_COMMIT`, três agregados e não um) e §*Monitoramento e observabilidade*. Ordem de construção: passo 5.

## Critérios de aceitação relacionados

- CA-06
- CA-08
- CA-09
- CA-12

## Testes da tarefa

### Testes de unidade

- [x] TU-31 — `AtualizadorAgregados` faz upsert somando volume e acertos por semana
- [x] TU-32 — `AtualizadorAgregados` é idempotente
- [x] TU-33 — `AtualizadorAgregados` não conta respostas de sessão não finalizada
- [x] TU-34 — `AtualizadorAgregados` separa questão sem assunto
- [x] TU-35 — `OuvinteSessaoFinalizada` marca pendência quando o upsert falha

### Testes de integração

- [ ] TI-24 — Agregado consistente com `resposta` após finalização da sessão
- [ ] TI-25 — Reconciliador reconstrói agregado divergente e limpa a pendência

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/desempenho/FonteRespostas.java`, `AtualizadorAgregados.java`, `OuvinteSessaoFinalizada.java`, `ReconciliadorAgregados.java`
- `backend/src/main/java/br/com/certame/estudo/resposta/RespostaAgregadoAdapter.java`
- `backend/src/main/java/br/com/certame/comum/evento/SessaoFinalizada.java`
- `backend/src/main/java/br/com/certame/config/` (`@EnableScheduling`)
- `backend/src/test/java/br/com/certame/desempenho/` — `AtualizadorAgregadosTest`, `OuvinteSessaoFinalizadaTest`, `*IntegracaoTest` de TI-24/TI-25
