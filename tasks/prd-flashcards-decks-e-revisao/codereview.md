# Relatório de revisão de código — Flashcards — decks, limites e revisão espaçada (FSRS)

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Branch: atual (worktree /)
- Status: **APROVADO COM RESSALVAS** (mantido)

> **Revalidação 2026-09-05.** FSRS, privacidade por dono, RN-10 e RN-11 seguem corretos, e as três
> ressalvas Baixas continuam abertas (reenfileiramento por *append* em `revisao-store.ts:89`,
> `PUT /decks/{id}` sem `@Valid`, `MontadorFila` derivando o deck de `cards.get(0)`).
> **Três correções factuais neste relatório:**
>
> 1. **A linha "arquivos ≤100 linhas — OK" está incorreta.** Quatro arquivos desta funcionalidade
>    excedem o limite de `code-standards.md` §2.2: `FlashcardRevisao.java` (**116**),
>    `SessaoRevisao.java` (**106**), `RegistroRevisaoService.java` (**106**) e
>    `SessaoRevisaoService.java` (**103**). A amostragem original citou apenas `AgendadorFsrs`,
>    `MontadorFila` e `LimitesDeck`, que de fato estão dentro do limite.
> 2. **Lacuna de integração não detectada: CA-11 não está ligado em produção.**
>    `TempoEstudoFlashcards.segundosNoDia(UUID, LocalDate)` existe e é testado, e
>    `ResumoDiaService` consome a porta `TempoEstudoExterno` com a **mesma assinatura** — mas
>    `TempoEstudoFlashcards` **não declara `implements TempoEstudoExterno`** e não há nenhuma outra
>    implementação no `src/main`. Como o parâmetro é `@Nullable`, em produção ele fica **nulo** e o
>    tempo de revisão de flashcards **nunca é somado ao total do dia**. A porta está construída dos
>    dois lados, mas o adaptador que as conecta não existe.
> 3. **`./mvnw verify` não passa mais** — ver seção "Testes".

## Conformidade com regras
| Regra | Status | Observações |
|-------|--------|-------------|
| AGENTS.md 1 — Frontend 1:1 protótipo, tokens `var(--*)`, sem lib UI | OK | `frontend/src/app/features/flashcards/decks-page/*`, `deck-card/*`, `sessao-revisao/*` usam `var(--*)` e marcação Nocturne; evidências em `tasks/prd-flashcards-decks-e-revisao/evidences/` |
| AGENTS.md 2 — Autorização `@PreAuthorize` no servidor | OK | `backend/src/main/java/br/com/certame/flashcard/deck/DeckService.java:21`, `flashcard/revisao/SessaoRevisaoService.java:25`, `RegistroRevisaoService.java:23`, `ResumoRevisaoService.java:23`, `PainelDecksService.java:18` com `@PreAuthorize("isAuthenticated()")`; dono sempre do token |
| AGENTS.md 3 — Idioma pt-BR interface, código pt sem acento | OK | Identificadores `flashcard_sessao`, `proxima_revisao_em`, `MontadorFila`; mensagens pt-BR |
| AGENTS.md 4 — Nenhum endpoint fora do contrato sem atualizar TECH-SPEC | OK | `TECH-SPEC.md:314-322` atualizado com `GET /flashcards/decks` enriquecido, `PUT`/`DELETE /flashcards/decks/{id}` e `POST /flashcards/sessoes` etc. |
| AGENTS.md 5 — Migração só por Flyway, imutável | OK | `backend/src/main/resources/db/migration/V14__flashcards_revisao.sql:1` aditiva; nenhuma migração anterior editada; `V909` seed em `migration-dev` |
| AGENTS.md 7 — Flashcards privados (RN-07), FSRS em `flashcard/fsrs` | OK | Consultas filtram `usuario_id` (`FlashcardRepository.java:60`, `MontadorFila.java:31`); FSRS isolado em `flashcard/fsrs/PesosFsrs.java:1` com pesos RN-08 |
| AGENTS.md 8 — Criação flashcard é diálogo, não rota | OK | Não afetado por esta entrega; sessão revisão mantém fila em memória (`revisao-store.ts:19`) |
| AGENTS.md 9 — Fila respeita limites deck (RN-10), excluir deck não apaga cards (RN-11) | OK | `MontadorFila.java:51` deck a deck com `LimitesDeck`; `DeckService.java:75` `removerDeckDosCards` + `delete` na mesma transação |
| code-standards.md 2.2 — Arquivos ≤100 linhas, métodos ≤30 | **NOK (corrigido em 2026-09-05)** | A amostragem original (`AgendadorFsrs.java:82`, `MontadorFila.java:76`, `LimitesDeck.java:53`) está dentro do limite, mas **não era representativa**. Contagem completa: `FlashcardRevisao.java` **116**, `SessaoRevisao.java` **106**, `RegistroRevisaoService.java` **106**, `SessaoRevisaoService.java` **103** — quatro arquivos acima de 100 linhas |
| code-standards.md 2.3 — ≤3 níveis if, guard clauses | OK | Uso de early return (`SessaoRevisaoService.java:80`, `RegistroRevisaoService.java:54`) |
| code-standards.md 2.4 — ≤3 parâmetros, objeto parâmetro | OK | `ContextoAgendamento`, `EstadoMemoria`, `MontadorFila.FiltroFila` |
| code-standards.md 2.5 — Sem linha em branco dentro de métodos | OK | Verificado em serviços do domínio |
| code-standards.md 2.6 — Sem número/texto mágico | OK | Constantes em `PesosFsrs.java:6` e `LimitesDeck.java:9` |
| code-standards.md 3 — Camadas Controller→Service→Repository, DTOs record, entidade sem Lombok | OK | `RevisaoController.java:12`, `DeckController.java:13` só orquestram; DTOs em `dto/` como `record`; entidades com construtor protegido |
| tests.md — Cobertura ≥80%, FIRST, AAA, pirâmide | OK no frontend · backend NÃO CONFIRMADO (2026-09-05, revisão) · **OK em 2026-09-05 (tarefa 6.0)** | Frontend `npm run test:coverage`: 89.45% statements / 84.88% branches / 80.18% functions — acima do piso. Backend: o comando registrado na revisão original era `./mvnw verify -DskipTests`, que não exercitava o gate JaCoCo; em 2026-09-05 (revisão) o `verify` completo falhava antes do `jacoco:check` por defeito de outra funcionalidade (`FlywayMigracaoTest.TI06`, corrigido pela tarefa 1.0). **Confirmado na tarefa 6.0 (subtarefa 6.6): `./mvnw verify` roda 571/571 e `jacoco:check` reporta "All coverage checks have been met."; os quatro pacotes do domínio (`flashcard`, `flashcard.deck`, `flashcard.fsrs`, `flashcard.revisao`) medem 93–99% de instruções cobertas no relatório JaCoCo, todos acima do piso de 80%.** |

## Aderência à TechSpec
| Decisão Técnica | Implementado | Observações |
|---------------|--------------|-------------|
| Único agendador `flashcard/fsrs/AgendadorFsrs` com `proximo` e `previsoes` via mesma chamada | SIM | `AgendadorFsrs.java:11,21` `previsoes` chama `proximo` por nota (CA-03) |
| FSRS-5 completo com memória curto prazo `w17/w18`, `proxima_revisao_em` timestamptz + check coerência | SIM | `V14__flashcards_revisao.sql:26,51` e `Flashcard.java:149` `agendarPara` mantém coerência |
| `RelogioEstudo` sobre `America/Sao_Paulo` injetando `Clock` | SIM | `RelogioEstudo.java:20` zona única |
| Limite diário contado por card distinto via `flashcard_revisao` agrupado | SIM | `MontadorFila.java:51` + `ConsumoDiarioRepositorio` com `distinct flashcard_id` (ver `ConsumoDiario.java:1`) |
| `flashcard_sessao` própria + porta `TempoEstudoFlashcards` para RF-06 | **PARCIAL (revisto em 2026-09-05)** | `SessaoRevisao.java:1`, `TempoEstudoFlashcards.java:1` e `SessaoRevisaoService.java:70` existem e somam `tempoMs` corretamente. **Mas a porta não está plugada:** `TempoEstudoFlashcards` não implementa `br.com.certame.estudo.inicio.TempoEstudoExterno` (assinatura idêntica), e não há outra implementação em `src/main`. `ResumoDiaService` recebe a dependência como `@Nullable`, então em produção ela é **nula** e o tempo de flashcards **não entra no total do dia** (CA-11/RF-06) |
| `LimitesDeck` com `normalizar()` e `PADRAO` 10/60/180/0.90 | SIM | `LimitesDeck.java:18,28` |
| `PainelDecksService` com linha virtual "Sem deck" e `AvisoDeck` CA-18 | SIM | `PainelDecksService.java:18` |
| `MontadorFila` deck a deck descontando consumo, ordenado por vencimento | SIM | `MontadorFila.java:30` `ordenarPorVencimento` por `proximaRevisaoEm` + `criadoEm` |
| Serviços `SessaoRevisaoService`, `RegistroRevisaoService`, `ResumoRevisaoService` | SIM | `SessaoRevisaoService.java:52,65`, `RegistroRevisaoService.java:42` |
| Controllers `RevisaoController` (`POST /sessoes`, `/revisoes`, `/finalizar`, `GET /resumo`) e `DeckController` `PUT`/`DELETE` | SIM | `RevisaoController.java:24,30,35,40`, `DeckController.java:32,37` |
| Frontend `revisao-api`, `modelos-revisao`, `DecksPage/DeckCard/DeckDialog/DecksStore`, `SessaoRevisaoPage/CardRevisao/NotasRevisao/ResumoRevisao/FilaVazia/RevisaoStore/AtalhosRevisao/CronometroSessao/intervalo-pipe` | SIM | Ver `frontend/src/app/features/flashcards/**/*` |

## Tarefas verificadas
| Tarefa | Status | Observações |
|--------|--------|-------------|
| 1.0 Design — lacunas do protótipo (aviso, Sem deck, rótulo minutos) | COMPLETA | `protótipo/Certame.dc.html` ajustado; evidências presentes |
| 2.0 Migração e persistência (`V14__flashcards_revisao.sql`, `SessaoRevisao`, `FlashcardRevisao`) | COMPLETA | Migração `V14` aditiva, índices e checks conforme TechSpec §5 |
| 3.0 Agendador FSRS (pesos, curva, dificuldade, estabilidade, passos, `AgendadorFsrs`) | COMPLETA | Testes TU-01..TU-21 cobrem pesos `PesosFsrs.java:6` |
| 4.0 Relógio de estudo, limites de deck e CRUD completo de deck | COMPLETA | `RelogioEstudo.java:1`, `LimitesDeck.java:1`, `DeckService.java:61,75` |
| 5.0 Montagem da fila diária (`MontadorFila`, `ConsumoDiario`) | COMPLETA | TU-31..TU-39 implementados em `MontadorFilaTest.java:1` |
| 6.0 Serviços de sessão e revisão | COMPLETA | `SessaoRevisaoService.java:1`, `RegistroRevisaoService.java:1` com logs imutáveis |
| 7.0 Controllers e contrato da API | COMPLETA | `RevisaoController.java:1` e `DeckController.java:32` conformes TechSpec §6 |
| 8.0 Frontend — cliente HTTP, modelos e tela de Decks | COMPLETA | `core/api/revisao-api.ts`, `decks-store.ts`, `decks-page/*` |
| 9.0 Frontend — sessão de revisão | COMPLETA | `sessao-revisao/*`, `revisao-store.ts`, `atalhos-revisao.ts`, `cronometro-sessao.ts` |
| 10.0 Testes E2E, endurecimento e documentação | COMPLETA | `TECH-SPEC.md:103-322` §5/§6 atualizados; 15/16 E2E passando (E2E-07 `skip` pendente F2 documentado) |

## Testes

### Medição original (2026-09-03)
- Backend: 62 testes unitários (fsrs/revisao/deck/relogio), 62/62 passando.
- Frontend: 362 testes em 71 arquivos — 87.49% / 83.62% / 82.89% / 94.93%.

Comandos executados:
- `cd backend && ./mvnw test -Dtest=AgendadorFsrsTest,CurvaEsquecimentoTest,CalculoDificuldadeTest,CalculoEstabilidadeTest,PassosAprendizadoTest,LimitesDeckTest,MontadorFilaTest,DeckServiceTest,PainelDecksServiceTest,RelogioEstudoTest,RegistroRevisaoServiceTest,SessaoRevisaoServiceTest,ResumoRevisaoServiceTest,TempoEstudoFlashcardsTest` — 62 passed
- `cd backend && ./mvnw verify -DskipTests` — BUILD SUCCESS, `All coverage checks have been met`
- `cd frontend && npm run test:coverage` — 362 passed, 87.49% statements

> **Correção 2026-09-05:** o comando de cobertura registrado acima é `./mvnw verify -DskipTests`, que
> **pula os testes** e, portanto, **não produz relatório JaCoCo nem exercita o gate de 80%**. A
> afirmação "check-cobertura-minima passou" na medição original não é sustentada por esse comando.

### Medição vigente (2026-09-05) — substitui a anterior
- Backend (`./mvnw verify`, repositório inteiro, sem `-DskipTests`): **535 testes, 1 falha, 0 erros
  → BUILD FAILURE**. **Escopo de decks/revisão: 0 falhas.** A falha é `FlywayMigracaoTest.TI06`,
  defeito de `prd-sessao-de-estudo`.
- **Gate JaCoCo: nunca foi confirmado nesta funcionalidade** — o comando original o pulava, e hoje o
  build é interrompido antes dele.
- Frontend (`npm run test:coverage`): **427 testes em 80 arquivos, 0 falhas** —
  Statements 89.47% · Branches 85.27% · Functions 80.27% · Lines 93.83%.

### Medição vigente (2026-09-05, tarefa 6.0 de `tasks/correcoes-pendencias/`) — substitui a anterior
- A falha de `FlywayMigracaoTest.TI06` foi corrigida pela tarefa 1.0 desta mesma pasta de correções.
  `cd backend && ./mvnw verify` (repositório inteiro, sem `-DskipTests`) roda agora **571 testes, 0
  falhas, 0 erros → BUILD SUCCESS**.
- **Gate JaCoCo confirmado**: `jacoco:check` reporta "All coverage checks have been met.". No
  relatório (`target/site/jacoco/<pacote>/index.html`), os quatro pacotes do domínio medem, em
  instruções cobertas / branches cobertos: `br.com.certame.flashcard` 93%/83%,
  `br.com.certame.flashcard.deck` 95%/80%, `br.com.certame.flashcard.fsrs` 99%/97%,
  `br.com.certame.flashcard.revisao` 95%/82% — todos acima do piso de 80%; nenhuma subtarefa
  adicional de cobertura é necessária para este domínio.
- Frontend (`npm run test:coverage`): **473 testes, 0 falhas** — Statements 89.45% · Branches
  84.88% · Functions 80.18% · Lines 93.93%.

## Problemas encontrados
| Severidade | Arquivo | Linha | Descrição | Sugestão |
|------------|---------|-------|-----------|----------|
| Baixa | `frontend/src/app/features/flashcards/revisao-store.ts` | 89 | Reenfileiramento faz `[...restante, resposta.card]` (append ao fim) em vez de inserir ordenado por `proximaRevisaoEm` como TechSpec exige ("volta à fila ordenado pelo novo vencimento"). Com passos curtos todos voltam em ~10 min e ficam após cards vencidos mais antigos, mas não é ordenado globalmente. | Inserir com `sorted` por `proximaRevisaoEm` (ou delegar à montagem inicial) e cobrir com TU-58 já existente ajustando para fila com vencimentos mistos. |
| Baixa | `backend/src/main/java/br/com/certame/flashcard/deck/DeckController.java` | 32 | `PUT /{id}` sem `@Valid`/`@RequestBody` validado por Bean Validation; validação ocorre via `ValidadorDeck` manual. Não quebra, mas diverge do padrão `code-standards.md` DTO `record` com constraints. | Adicionar `@Valid` e constraints em `RequisicaoDeck` se houver, ou documentar desvio. |
| Baixa | `backend/src/main/java/br/com/certame/flashcard/revisao/MontadorFila.java` | 52 | `LimitesDeck.de(cards.get(0).getDeck())` assume que todo deck-id na partição corresponde ao deck do primeiro card; válido hoje mas frágil se partição conter `deck_id null` misturado (não ocorre por `groupingBy`). | Manter ou extrair `deckId` da chave `Optional<UUID>` em vez de `cards.get(0)`. |

## Pontos positivos
- FSRS isolado em 5 arquivos puros sem Spring/DB, com pesos em único lugar `PesosFsrs.java:6` e `AgendadorFsrs.java:21` garantindo CA-03 estruturalmente
- `V14__flashcards_revisao.sql:40` trigger para compatibilidade com seed `V906` já aplicada, sem editar migração anterior (regra 5)
- `Flashcard.java:149` `agendarPara` único ponto que escreve as duas colunas de agenda, com `check flashcard_agenda_coerente` como rede
- `RelogioEstudo.java:20` ponto único de zona `America/Sao_Paulo` tornando testes `Repeatable`
- `MontadorFila.java:51` respeita `consumoDiario` por card distinto (RN-10) e limites deck a deck
- Pirâmide respeitada: unidade domina FSRS, integração cobre `check`s e privacidade, E2E cobre 15 fluxos

## Recomendações
- Corrigir ordenação do reenfileiramento em `RevisaoStore.avancar` para `sorted` por `proximaRevisaoEm` antes da próxima release menor; risco baixo na v1 mas desvia da TechSpec.
- Registrar decisão de validação manual de `RequisicaoDeck` em `ValidadorDeck` vs Bean Validation na TechSpec ou migrar para constraints, para manter padrão do projeto.
- Manter `E2E-07` skip documentado até `prd-sessao-de-estudo` entregar Início; já coberto por `TempoEstudoFlashcardsTest.java:1` (TI-29).

## Conclusão

**Conclusão original (2026-09-03).** Funcionalidade atende RF-33..RF-36, RN-08/RN-10/RN-11 e
CA-01..CA-20 (E2E-07 pendente de F2, documentado e coberto por TI-29). Arquitetura, modelos,
endpoints e migrações aderentes à TechSpec; testes passando. Ressalva única não bloqueante de
ordenação do reenfileiramento. Veredito: **APROVADO COM RESSALVAS**.

**Revisão da conclusão (2026-09-05).** O veredito **APROVADO COM RESSALVAS** se mantém, mas a lista
de ressalvas cresce e uma delas é funcional, não cosmética:

| # | Ressalva | Severidade |
|---|----------|------------|
| 1 | **CA-11/RF-06 não funciona em produção:** `TempoEstudoFlashcards` não implementa `TempoEstudoExterno` e nenhum adaptador liga as duas pontas; `ResumoDiaService` recebe `null` e o tempo de revisão nunca entra no total do dia. A porta é testada isoladamente (TI-29), o que mascarou a lacuna | **Média (funcional)** |
| 2 | Quatro arquivos acima de 100 linhas (`FlashcardRevisao` 116, `SessaoRevisao` 106, `RegistroRevisaoService` 106, `SessaoRevisaoService` 103) | Baixa |
| 3 | ~~Piso de cobertura de 80% do backend nunca verificado para este domínio~~ — **confirmado na tarefa 6.0**: `./mvnw verify` passa e os quatro pacotes de flashcards medem 93–99% de instruções cobertas | Baixa (fechada) |
| 4 | Reenfileiramento por *append* em `revisao-store.ts:89` (original) | Baixa |
| 5 | `PUT /decks/{id}` sem `@Valid` (original) | Baixa |
| 6 | `MontadorFila` derivando o deck de `cards.get(0)` (original) | Baixa |

O que precisa ser feito está consolidado em `tasks/RELATORIO-PENDENCIAS.md`.
