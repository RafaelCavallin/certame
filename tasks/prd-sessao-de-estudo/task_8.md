# Tarefa 8.0: Testes E2E e acessibilidade

## Visão geral

Fecha a pirâmide de testes com os oito cenários ponta a ponta em Playwright, contra a API real subida em perfil de teste, cobrindo os fluxos completos do PRD: estudo em Foco, alternância de modo, Modo Prova ponta a ponta, abandono de prova, marcações, o diálogo de flashcard preservando a sessão, "continuar de onde parou" e operação completa por teclado com verificação de acessibilidade. Inclui a medição de CA-14 (300 ms p95) sobre a build de produção.

<skills>
### Conformidade com skills

- `angular-developer` — verificação de acessibilidade conforme `angular-aria.md`: `role="radiogroup"`, `aria-live="polite"`, foco visível, navegação completa por teclado.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e `tests.md` lidos.

- `tests.md` — pirâmide ~70/20/10; os E2E cobrem fluxo completo, não repetem o que já está coberto em unidade/integração.
- Regra 8 (`AGENTS.md`) — E2E-06 é a prova de que o diálogo de flashcard não destrói a sessão em memória.
- UI/UX do PRD — contraste mínimo 4,5:1, foco visível, sem salto de layout entre questões, operação completa por teclado.
</rules>

<requirements>
- RF-06, RF-07, RF-08, RF-09, RF-10, RF-11, RF-12
- Todos os CA do PRD (verificação ponta a ponta)
</requirements>

## Subtarefas

- [x] 8.1 Implementar E2E-01 a E2E-03 (Foco/gabarito, alternância de modo, Modo Prova ponta a ponta).
- [x] 8.2 Implementar E2E-04 e E2E-05 (prova abandonada não entra no histórico, marcar/sair/voltar).
- [x] 8.3 Implementar E2E-06 (criar flashcard sem perder a sessão). Bloqueado: o diálogo de flashcard (RF-28) é entregue por `prd-flashcards-criacao-e-gerenciamento`, ainda não implementado no repositório — cenário documentado como `test.skip` com o motivo, mesmo padrão do "não é bloqueador" já usado pela TechSpec para `TempoEstudoFlashcards` (F6b).
- [x] 8.4 Implementar E2E-07 (continuar de onde parou + navegação por número).
- [x] 8.5 Implementar E2E-08 (operação por teclado e foco visível, com `@axe-core/playwright` e comparação de *bounding box* entre questões consecutivas).
- [x] 8.6 Medir CA-14 (próxima questão em até 300 ms p95) sobre a build de produção.

## Detalhes de implementação

Ver TechSpec, seção "Testes E2E" para o resultado esperado de cada cenário e a nota final sobre `Playwright em frontend/e2e/*.spec.ts`, um cenário por fluxo, contra a API real.

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01 a CA-16 (verificação ponta a ponta dos fluxos principais)

## Testes da tarefa

### Testes E2E

- [x] E2E-01 — Estudar em Foco, responder e ver o gabarito
- [x] E2E-02 — Alternar para Contexto e voltar sem perder o lugar
- [x] E2E-03 — Modo Prova ponta a ponta
- [x] E2E-04 — Prova abandonada não entra no histórico
- [x] E2E-05 — Marcar, sair e voltar
- [ ] E2E-06 — Criar flashcard sem perder a sessão (bloqueado: diálogo ainda não existe, ver 8.3)
- [x] E2E-07 — Continuar de onde parou
- [x] E2E-08 — Operação por teclado e foco visível

CA-14 medido em `frontend/e2e/desempenho.spec.ts`, contra a build de produção (`npm run e2e:producao`).

## Arquivos relevantes

- `frontend/e2e/estudo.spec.ts`
- `frontend/e2e/desempenho.spec.ts` — CA-14 sobre a build de produção.
- `frontend/playwright.prod.config.ts` e `frontend/e2e/global-teardown.prod.ts` — config e teardown dedicados à suíte de produção.
- `frontend/proxy.e2e.prod.conf.json` — proxy do `ng serve --configuration production` para o backend na porta 3001.
- `frontend/playwright.config.ts` — ignora `desempenho.spec.ts` (roda só via `e2e:producao`).
- `frontend/package.json` — script `e2e:producao`.
