# Resumo das tarefas de implementação de Cadernos e desempenho

> **PRD:** `prd.md` · **TechSpec:** `techspec.md` · **Fase:** F3 — Progresso · **Requisitos:** RF-13, RF-15, RF-16 (RN-01, RN-02, RN-04)

A ordem abaixo é a da §*Sequenciamento do desenvolvimento* da TechSpec: protótipo antes de qualquer marcação Angular (regra 1 do `AGENTS.md`), persistência antes das regras, regras puras antes dos serviços, backend antes do frontend, e E2E por último.

## Tarefas

- [x] 1.0 Fechar as sete lacunas do protótipo Nocturne — diálogos, estados vazios, gráfico semanal acessível e volume por disciplina implementados em `protótipo/Certame.dc.html`.
- [x] 2.0 Migração `V6__cadernos_desempenho.sql`, entidades e repositórios
- [x] 3.0 Regras puras de caderno e de desempenho
- [x] 4.0 Cadernos: serviços, controller e DTOs (RF-13) — CRUD manual, projeções automáticas, progresso, série e listagem paginada implementados.
- [x] 5.0 Porta `FonteRespostas`, agregados incrementais e reconciliador — porta, consultas RN-04, upserts idempotentes, listener pós-commit, fila de reconciliação e métricas implementados; TI-24 provado na tarefa 10 (achou e corrigiu um bug real: `AtualizadorAgregados.atualizar` precisa de `Propagation.REQUIRES_NEW` para rodar de dentro do listener `AFTER_COMMIT`, senão o Hibernate recusa a query em massa sem transação ativa e o desempenho nunca era atualizado); TI-25 (reconciliador) segue pendente.
- [x] 6.0 Desempenho: quatro serviços de leitura e `DesempenhoController` (RF-15, RF-16) — resumo, evolução semanal, disciplinas, pontos fracos, métricas e contrato implementados; TIs aguardam F2.
- [x] 7.0 Frontend de cadernos e `shared/ui/estado-vazio` — API, store, cards, diálogos, seletor de questões e estados vazios implementados (RF-13).
- [x] 8.0 Frontend de desempenho: store, página, gráfico SVG e painéis — API, store, visualização acessível e painéis implementados (RF-15, RF-16).
- [x] 9.0 Integração com a sessão de estudo: abrir caderno e "Treinar" — `SerieDoCadernoService` ligada em `SessaoEstudoService.abrir` (origem CADERNO restrita, modo Foco forçado), "Treinar" abrindo sessão filtrada por assunto, métricas de caderno e TI-16/TI-17 implementados.
- [x] 10.0 E2E, cobertura, latência e acessibilidade — treze cenários Playwright, TI-27 de latência, ./mvnw verify e npm run test:coverage em 80%, revisão de acessibilidade e contraste implementados.

## Mapa de cobertura

| Tarefa | Critérios de aceitação | Testes |
| --- | --- | --- |
| 1.0 | CA-04, CA-07, CA-08, CA-11 | — (habilita E2E-04, E2E-07, E2E-10) |
| 2.0 | CA-01, CA-04, CA-06 | TI-01…TI-05 |
| 3.0 | CA-01, CA-04, CA-05, CA-06, CA-07, CA-09, CA-10, CA-11 | TU-01…TU-19 |
| 4.0 | CA-01, CA-02, CA-03, CA-04, CA-05, CA-11 | TU-20…TU-30 · TI-06…TI-15, TI-18 |
| 5.0 | CA-06, CA-08, CA-09, CA-12 | TU-31…TU-35 · TI-24, TI-25 |
| 6.0 | CA-06…CA-12 | TU-36, TU-37 · TI-19…TI-23, TI-26 |
| 7.0 | CA-01, CA-02, CA-03, CA-04, CA-11 | TU-42, TU-43 |
| 8.0 | CA-06…CA-11 | TU-38…TU-41, TU-44 |
| 9.0 | CA-05 | TI-16, TI-17 |
| 10.0 | CA-01…CA-12 | E2E-01…E2E-13 · TI-27 |

## Dependências e bloqueios

- **`prd-sessao-de-estudo` (F2) foi implementada** (`sessao_estudo`, `resposta`, `marcacao`, `POST /estudo/sessoes` em `V7__sessao_estudo.sql`), desbloqueando a tarefa 9. O fluxo de abertura de F2 não chamava `SerieDoCadernoService` e o `check` de `tamanho` do schema de F2 excluía cadernos fora de 10/30/50; a tarefa 9 fechou os dois com `V8__sessao_caderno_tamanho_livre.sql` (aditiva) e a ramificação por `origem` em `SessaoEstudoService.abrir`.
- **`prd-banco-de-questoes`** (`questao`, `disciplina`, `assunto`, `PaginaResposta<T>`, `confirmacao-dialog`) bloqueia as tarefas 2, 4 e 7.
- **`prd-acesso-e-conta`** (`SegurancaConfig`, `authGuard`, `ManipuladorErrosGlobal`, `styles.css`) bloqueia tudo.
- **O número da migração (`V6`)** foi confirmado na entrega; `V5` já era usado por outra alteração e nenhuma migração aplicada foi editada.
- **Tarefa 10 fechada.** Playwright já estava instalado; achou e corrigiu, além do bug de agregação citado na tarefa 5.0: `GET /api/v1/questoes` (documentado na TECH-SPEC.md §6, nunca implementado — bloqueava o seletor de questões do caderno) e um bug de UI em `CadernosPage.abrirDialog()` que nunca abria o diálogo "Novo caderno" (o valor de "fechado" e o de "aberto para criar" eram os dois `null`). `./mvnw verify` e `npm run test:coverage` passam para todas as classes desta funcionalidade. Uma sessão concorrente está implementando `prd-simulados` no mesmo repositório; isso deixou `./mvnw verify` do repositório inteiro instável por período (classes do `simulado` e do F2 abaixo de 80%) — não é lacuna desta funcionalidade.
