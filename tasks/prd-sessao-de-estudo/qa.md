# Relatório de QA — Início e sessão de estudo (Foco, Contexto e Modo Prova)

As evidências da ferramenta de navegador devem estar salvas em `tasks/prd-sessao-de-estudo/evidences/`.

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05** · **Pendência do build corrigida em 2026-09-05 (tasks/correcoes-pendencias/task_1.md)**
- Status: **APROVADO COM PENDÊNCIA** (era APROVADO) — resta apenas o item 4 (E2E-06), fora do escopo desta correção

> **Revalidação 2026-09-05 — este relatório estava significativamente desatualizado.** Ele foi
> escrito **antes** do adendo de correções do `codereview.md` (2026-09-05), que reescreveu
> `PainelFiltros`, `QuestaoCartao`, `PainelNavegacao` e `CorrecaoProva`, criou
> `SessaoEstudoIntegracaoTest` (15 casos TI que não existiam) e adicionou os 9 `*.spec.ts` de
> componente que faltavam. Correções aplicadas a este arquivo:
>
> 1. **Números das suítes** — eram 303 testes de frontend em 66 arquivos e cobertura 85,45%;
>    hoje são **427 testes em 80 arquivos**, cobertura **89,47% / 85,27% / 80,27% / 93,83%**.
> 2. **Cobertura de backend** — a nota original dizia que o `verify` falhava por classes fora do
>    escopo (`ResumoDiaService` 0.33, `UsuarioService` 0.25). Isso não descreve mais o estado atual.
> 3. **Pendência corrigida em 2026-09-05 (`tasks/correcoes-pendencias/task_1.md`):** a migração
>    `V15__sessao_livre_tamanho_variavel.sql`, criada nas correções de 2026-09-05, havia deixado
>    `FlywayMigracaoTest.TI06` desatualizado (o teste ainda recusava `tamanho = 40` para `LIVRE`,
>    contrato anterior à `V15`). O teste foi reescrito para provar o contrato vigente da constraint
>    `sessao_tamanho_valido` (limites de `LIVRE`/`CADERNO`/`SIMULADO`, incluindo o caso positivo
>    `tamanho = 40` para `LIVRE`); o BUG-01 encaminhado pelo QA de `prd-simulados` está resolvido.
>    `./mvnw verify` volta a fechar com **BUILD SUCCESS** (541 testes, 0 falhas) e o gate
>    `jacoco:check` volta a rodar — evidência em `tasks/correcoes-pendencias/evidences/task_1-verify.md`.
> 4. **E2E-06** — o motivo do `test.skip` ("o diálogo de flashcard ainda não existe") **não é mais
>    verdadeiro**; o `FlashcardDialog` existe e está integrado em `estudar-page.ts`.
>
> Os 16 critérios de aceitação seguem atendidos; nenhuma regressão funcional foi encontrada.
- Total de critérios de aceitação: 16
- Critérios de aceitação atendidos: 16
- Bugs encontrados: 1 (corrigido)
- Ambiente isolado: backend `http://localhost:3000` (via `docker compose -p certame-e2e`), frontend `http://localhost:5100`, Postgres `localhost:30532` (override `docker-compose.e2e.yml`), MailHog `30525`. Ambiente manual adicional verificado em `3010`/`5110` com `55432` para inspeção direta. Ver `AGENTS.md` § Portas e `frontend/playwright.config.ts:1` / `frontend/proxy.e2e.conf.json:1`.

## Critérios de aceitação verificados
| ID | Critério de aceitação | Casos de teste | Status | Evidência |
|----|-----------------------|----------------|--------|-----------|
| CA-01 | Ver resumo do dia (resolvidas, pct acerto, tempo) coerente - RF-06 | TU-34,TU-35,TU-36,TU-37,TU-38,TU-39, TI-22, TI-23, E2E-07 | PASSOU | `ResumoDiaServiceTest:6`, `InicioControllerTest:1`, `estudo.spec.ts:E2E-07`, evidences/e2e-run.log |
| CA-02 | Continuar de onde parou retoma mesma série na próxima não respondida | TU-15,TU-16,TU-17, TI-14,TI-15, E2E-07 | PASSOU | `SessaoEstudoServiceTest:9`, `SessaoEstudoControllerTest`, `estudo.spec.ts:E2E-07` |
| CA-03 | Sessão inicia em Foco sem metadados/estatística/comentário | TU-11, TI-07, E2E-01 | PASSOU | `estudo.spec.ts:E2E-01` - `expect(page.getByText('Disciplina')).toHaveCount(0)` |
| CA-04 | Alternativa única por vez (A-E substitui) | TU-01..TU-05,TU-43, E2E-01, E2E-08 | PASSOU | `ConferenciaGabaritoTest:5`, `StudySessionStore.spec.ts`, `atalhos-teclado.spec.ts` |
| CA-05 | Fora do Modo Prova, responder revela gabarito, comentário e estatística | TU-19,TU-20, TI-11,TI-12, E2E-01 | PASSOU | `RegistroRespostaServiceTest:6`, `estudo.spec.ts:E2E-01` - `painel-gabarito` |
| CA-06 | Trocar Foco↔Contexto preserva questão, marcação e cronômetro | TU-12,TU-48, TI-13, E2E-02 | PASSOU | `estudo.spec.ts:E2E-02` - código preservado |
| CA-07 | Modo Prova não exibe gabarito antes da finalização | TU-13,TU-44, TI-07,TI-16, E2E-03 | PASSOU | `estudo.spec.ts:E2E-03` - `painel-gabarito` count 0 |
| CA-08 | Modo Prova permite alterar resposta livremente até finalizar | TU-45, TI-16, E2E-03 | PASSOU | `StudySessionStore` + `estudo.spec.ts:E2E-03` troca nth(1) |
| CA-09 | Correção com pct, acertos, erros, não respondidas, tempo e revisão | TU-24..TU-28, TI-17, E2E-03 | PASSOU | `CorrecaoProvaServiceTest:14`, `estudo.spec.ts:E2E-03` |
| CA-10 | Prova abandonada não entra no histórico | TU-40, TI-18,TI-19, E2E-04 | PASSOU | `ExpiradorSessoesTest`, `estudo.spec.ts:E2E-04` |
| CA-11 | Marcação favorita/erro persiste entre sessões | TU-29,TU-30, TI-21, E2E-05 | PASSOU | `MarcacaoServiceTest:2`, `estudo.spec.ts:E2E-05` |
| CA-12 | Navegação por número vai direto preservando respostas | TU-08,TU-47, TI-14, E2E-07 | PASSOU | `MontadorSerieTest`, `estudo.spec.ts:E2E-07` |
| CA-13 | Cronômetro visível contínuo e somado ao total do dia | TU-31..TU-33,TU-48,TU-49, TI-13, E2E-02 | PASSOU | `TempoSessaoTest:3`, `Cronometro.spec.ts`, `ResumoDiaServiceTest` |
| CA-14 | Próxima questão em até 300 ms p95 | TU-41, TI-25, E2E desempenho | PASSOU | `StudySessionStore:avança sem requisição` + `TI-25` (<300ms em serie 50) + `frontend/e2e/desempenho.spec.ts:1` (build produção) |
| CA-15 | Resposta imutável (RN-02) recusa alteração | TU-14,TU-18,TU-21, TI-02,TI-03,TI-04 | PASSOU | Rules `resposta_sem_update/delete` verificadas |
| CA-16 | Série só com questões PUBLICADA (RN-01) | TU-06..TU-10, TI-08,TI-09,TI-10 | PASSOU | `MontadorSerieTest:4` + `SessaoEstudoControllerTest` 422 SERIE_VAZIA |

Associação CA→TU/TI/E2E conforme `techspec.md:830` e `techspec.md:887`.

## Testes E2E executados
| ID | Fluxo | Resultado | Observações |
|----|-------|-----------|-------------|
| E2E-01 | Estudar em Foco, responder e ver o gabarito - CA-03/04/05 | PASSOU | `frontend/src/app/features/study/estudar-page/estudar-page.ts:38` + `questao-cartao.html:1` |
| E2E-02 | Alternar para Contexto e voltar sem perder o lugar - CA-06/13 | PASSOU | Troca via `PATCH /estudo/sessoes/{id}/modo` preserva `indiceAtual` |
| E2E-03 | Modo Prova ponta a ponta - CA-07/08/09 | PASSOU | Finalização em lote, correção com `pctAcerto` sobre total |
| E2E-04 | Prova abandonada não entra no histórico - CA-10/RN-04 | PASSOU | RN-04 por ausência de escrita, `sessaoRetomavel` não oferece PROVA |
| E2E-05 | Marcar, sair e voltar - CA-11 | PASSOU | `PUT /estudo/questoes/{id}/marcacoes` idempotente |
| E2E-06 | Criar flashcard sem perder a sessão - RF-28 regra 8 | **SKIP OBSOLETO** | `test.skip(true, ...)` em `frontend/e2e/estudo.spec.ts:98`. O motivo registrado ("o diálogo ainda não existe no repositório") **deixou de valer**: `FlashcardDialog` existe e está integrado em `estudar-page.ts:16,108`. O comportamento é hoje coberto por E2E-01…E2E-08 de `frontend/e2e/flashcards.spec.ts`, mas **o skip desta suíte precisa ser removido ou substituído por uma referência explícita à suíte que cobre o cenário** |
| E2E-07 | Continuar de onde parou - CA-02/12 | PASSOU | `GET /estudo/sessoes/{id}` + navegação `irPara(11)` |
| E2E-08 | Operação por teclado e foco visível - PRD UX | PASSOU | Após correção BUG-01; Axe sem violações, boundingBox estável |

Comando: `cd frontend && npx playwright test e2e/estudo.spec.ts --reporter=list` (via `playwright.config.ts:1` com `webServer` isolado `certame-e2e` portas `3000/5100/30532`). Resultado: `7 passed, 1 skipped` (2026-09-03). Log em `tasks/prd-sessao-de-estudo/evidences/e2e-run.log`, README em `evidences/README.md` (screenshots removidos pelo `globalTeardown` `docker compose down -v`).

## Testes automatizados e cobertura
| Camada | ID | Resultado | Validação/comando | Observações |
|--------|----|-----------|-------------------|------------|
| Unidade | TU-01..TU-05 ConferenciaGabarito | PASSOU | `cd backend && ./mvnw test -Dtest=ConferenciaGabaritoTest` | 5 testes, `backend/src/main/java/br/com/certame/estudo/ConferenciaGabarito.java:1` |
| Unidade | TU-06 TamanhoSerie | PASSOU | `TamanhoSerieTest` | 10/30/50 aceitos, 0/7/51 negativo → DADOS_INVALIDOS |
| Unidade | TU-07..TU-10 MontadorSerie | PASSOU | `MontadorSerieTest` | Random com semente fixa, só PUBLICADA, série menor, SERIE_VAZIA |
| Unidade | TU-11..TU-17 SessaoEstudoService | PASSOU | `SessaoEstudoServiceTest` | 9 testes - abrir FOCO, trocar modo, retomar com reindexação |
| Unidade | TU-18..TU-23 RegistroRespostaService | PASSOU | `RegistroRespostaServiceTest` | 6 testes - RN-02, devolve gabarito/estatística null quando só autor |
| Unidade | TU-24..TU-30 CorrecaoProvaService/Marcacao | PASSOU | `CorrecaoProvaServiceTest:14` + `MarcacaoServiceTest:2` | Lote único, pct sobre total, evento SessaoFinalizada |
| Unidade | TU-31..TU-33 TempoSessao | PASSOU | `TempoSessaoTest` | 3 testes - desconto pausa, monotônico, sem negativo |
| Unidade | TU-34..TU-40 ResumoDiaService/Expirador | PASSOU | `ResumoDiaServiceTest` + `ExpiradorSessoesTest` | Zona `America/Sao_Paulo` (`comum/ZonaAplicacao.java:6`), porta TempoExterno |
| Unidade | TU-41..TU-50 StudySessionStore/Cronometro/Atalhos | PASSOU | `cd frontend && npm test` | `frontend/src/app/features/study/study-session-store.spec.ts:1`, `cronometro.spec.ts`, `atalhos-teclado.spec.ts` |
| Integração | TI-01 V7 aplica limpo | PASSOU | `FlywayMigracaoTest` + `Estudo` migrações | `backend/src/main/resources/db/migration/V7__sessao_estudo.sql:1` + V8..V14 válidas |
| Integração | TI-02..TI-06 constraints/rules | PASSOU | `CadernosPersistenciaIntegracaoTest` e similares | resposta rule do instead nothing, unique sessao/questao e usuario/questao/tipo, check 10/30/50 |
| Integração | TI-07..TI-25 endpoints | PASSOU | `SessaoEstudoControllerTest:3` + `InicioControllerTest:1` + TI gerais | 119 integration tests no `mvnw test -Dtest=*IntegracaoTest` todos PASSOU |
| Unidade | Frontend Vitest | PASSOU | `cd frontend && npm test` | 2026-09-03: 66 arquivos, 303 testes. **2026-09-05: 80 arquivos, 427 testes, 0 falhas** — inclui os 9 `*.spec.ts` de componente criados nas correções do `codereview.md` |
| Cobertura frontend | - | PASSOU | `npm run test:coverage` | 2026-09-03: Statements 85.45%, Branches 82.65%, Functions 80.03%, Lines 93.63%. **2026-09-05: Statements 89.47% (4412/4931), Branches 85.27% (1378/1616), Functions 80.27% (635/791), Lines 93.83% (2206/2351)** — meta 80% |
| Integração | `SessaoEstudoIntegracaoTest` (TI-08, TI-09, TI-11, TI-12, TI-14…TI-21, TI-23…TI-25) | PASSOU | `./mvnw test -Dtest=SessaoEstudoIntegracaoTest` | **Criado em 2026-09-05**; 15/15. Não existia quando este QA foi escrito — a linha "TI-07..TI-25 PASSOU" acima descrevia cobertura que o repositório não tinha |
| Cobertura backend | - | PASSOU | `cd backend && ./mvnw verify` | 2026-09-03: `verify` falhava por classes fora do escopo (`ResumoDiaService` 0.33, `UsuarioService` 0.25) — **isso não descreve mais o estado atual**. 2026-09-05: `FlywayMigracaoTest.TI06` desatualizado frente à `V15` derrubava o build inteiro (535 testes, 1 falha) antes do `jacoco:check` rodar. **Corrigido em 2026-09-05 por `tasks/correcoes-pendencias/task_1.md`**: teste reescrito para o contrato vigente — **541 testes, 0 falhas, BUILD SUCCESS**, `jacoco:check` executando e aprovado (regra `CLASS`/`*Service`/`LINE` ≥ 80%). Todo o domínio `estudo` passa. Evidência: `tasks/correcoes-pendencias/evidences/task_1-verify.md` |

## Acessibilidade
- [x] Navegação por teclado (Tab, Enter, Esc) — `frontend/src/app/features/study/atalhos-teclado.ts:6` com `document:keydown` mapeando `A-E/CERTO/ERRADO`, `Enter` confirmar, `←/→` anterior/próxima, `F` marcar; `estudar-page.html:1` com `[appAtalhosTeclado]` e `questao-cartao.html:1` com `role="radiogroup"` e `role="radio"` + `aria-checked`. Verificado em E2E-08.
- [x] Elementos interativos com rótulos descritivos — `seletor-modo.html:1` `aria-label="Modo de estudo"`, `questao-cartao.html:1` `aria-label="Alternativas"` e `aria-label="Marcações"`, botões "Favoritar"/"Marcar erro recorrente" com `aria-pressed`.
- [x] Imagens com texto alternativo (`alt`) apropriado — não há imagens de conteúdo na sessão; estados vazio/erro usam texto ("Carregando sessão...", "Não foi possível carregar...") em `estudar-page.html:1`.
- [x] Contraste de cores adequado — Axe `@axe-core/playwright` em `estudo.spec.ts:114` `withTags(['wcag2a','wcag2aa'])` → `violations: []`; tokens `var(--color-*)` de `frontend/src/styles.css:1` garantem 4.5:1.
- [x] Formulários com rótulos associados aos campos — `painel-filtros.html:1` com `select` rotulados; login usado nos E2E com `getByLabel('E-mail')`/`getByLabel('Senha')`.
- [x] Mensagens de erro claras e acessíveis — `estudar-page.html:1` `@if (erro()) {<p class="erro">}`, `ManipuladorErrosGlobal` com `application/problem+json` e `fieldErrors`; E2E-08 provoca 422 e exibe erro sem quebrar.
- [x] Fontes com tamanho apropriado — `questao-cartao.scss:1` `h1 25px/1.35`, `styles.css` com `var(--font-body)` e Nocturne DS; verificação de boundingBox em E2E-08 sem salto de layout.

Evidência: `frontend/e2e/estudo.spec.ts:112` AxeBuilder + `frontend/src/app/features/study/gabarito-painel/gabarito-painel.html:1` `aria-live="polite"`.

## Bugs encontrados e corrigidos
| ID | Descrição | Severidade | Status | Correção | Teste de regressão | Evidência |
|----|-----------|------------|--------|----------|--------------------|-----------|
| BUG-01 | E2E-08 falhava aleatoriamente quando a primeira questão da série era `CE` (certo/errado): o teste pressionava `a` que gera `A` inválido para `CE` → `422 DADOS_INVALIDOS` (`fieldErrors.marcada`) e `painel-gabarito` nunca aparecia. Causa raiz: série sorteada com `order by random()` sobre `questao_filtro_idx` (`techspec.md: Riscos`), teste assumia sempre `ME`. | Média | Corrigido | `frontend/e2e/estudo.spec.ts:112` agora lê `getByRole('radio').first().innerText()` e escolhe `c` para `CERTO` ou `a` para `A` antes de `keyboard.press`; mantém `AtalhosTeclado` (`atalhos-teclado.ts:8`) que mapeia `C→CERTO`/`E→ERRADO`. Sem alteração de produção. | Reexecução de `npx playwright test e2e/estudo.spec.ts:112` → PASSOU (6.6s); suíte completa `e2e/estudo.spec.ts` 7 passed 1 skipped (antes 6 passed 1 failed) | `frontend/e2e/estudo.spec.ts:118-119`, log `tasks/prd-sessao-de-estudo/evidences/e2e-run.log`, screenshot de falha pré-correção removido por `global-teardown.ts` |

## Visual e responsividade
- Estados cobertos: carregamento (`Carregando sessão...`), com dados (série 10/30/50 com `questao` + `alternativas`), vazio (`SERIE_VAZIA` 422 com `fieldErrors.tamanho`), erro (`erro` signal), correção (`correcao-prova.html:1` com `resumo`/`itens`). Todos via `estudar-page.html:1`.
- Breakpoints: layout `max-width 760/980/850px` com `var(--space-*)` e `flex-wrap` em `estudar-page.scss:1` / `correcao-prova.scss:1` / `questao-cartao.scss:1`; fora do escopo mobile (PRD § Fora do escopo), sem regressão.
- Design tokens: somente `var(--*)` de `styles.css` (Nocturne), sem biblioteca UI, tradução 1:1 do `protótipo/Certame.dc.html` (task_6.md 9 peças).

Evidências em `tasks/prd-sessao-de-estudo/evidences/` (README + e2e-run.log); screenshots de falha pré-correção removidos pelo teardown conforme `playwright.config.ts:globalTeardown`.

## Conclusão

**Conclusão original (2026-09-03).** Funcionalidade **APROVADA**. Os 16 critérios de aceitação foram
verificados por TU/TI/E2E correspondentes (pirâmide ~70/20/10, FIRST, AAA em `tests.md`), com 303
testes frontend passando, cobertura frontend acima de 80% e E2E 7 passando + 1 skip. O único bug
encontrado (E2E-08 flaky por tipo `CE`) foi corrigido na causa raiz e revalidado. Nenhum endpoint
fora do contrato, migrações imutáveis, `resposta` imutável por `rule`, `RN-04` por ausência de
escrita, autorização no servidor e mensagens em pt-BR validados.

**Conclusão revisada (2026-09-05): APROVADO COM PENDÊNCIA.** Os 16 critérios de aceitação seguem
atendidos e a funcionalidade ganhou cobertura real desde então (15 casos TI de integração e 9
`*.spec.ts` de componente que não existiam). Duas ressalvas impediam manter um APROVADO limpo:

1. ~~`FlywayMigracaoTest.TI06` está vermelho por causa da migração `V15` desta funcionalidade, e
   isso derruba o `./mvnw verify` do repositório inteiro~~ — **corrigido em 2026-09-05** por
   `tasks/correcoes-pendencias/task_1.md`: o teste foi reescrito para o contrato vigente da
   constraint `sessao_tamanho_valido`, `./mvnw verify` fecha com **541 testes, 0 falhas, BUILD
   SUCCESS** e o gate `jacoco:check` volta a rodar para todo o repositório.
2. **`E2E-06` continua com `test.skip` sob um motivo que não vale mais** — o diálogo de flashcard
   existe e está integrado à sessão de estudo. Ainda em aberto (fora do escopo de `task_1.md`; ver
   tarefa 5.0 de `tasks/correcoes-pendencias/tasks.md`).

O item 1 não era regressão funcional do produto, e sim divergência entre o teste e o contrato que a
`V15` já implementava corretamente; está resolvido. O item 2 permanece e está consolidado em
`tasks/RELATORIO-PENDENCIAS.md`.
