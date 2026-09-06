# Tarefa 5.0 (P2-1/P2-2): Sanear os `test.skip` obsoletos e executar o E2E de editais

## Visão geral

Dois cenários E2E estão desligados por motivos que já não existem:

| Cenário | Motivo registrado no `test.skip` | Situação real |
|---|---|---|
| `e2e/estudo.spec.ts:98` — **E2E-06** (criar flashcard sem perder a sessão, RF-28) | "o diálogo de flashcard ainda não existe no repositório" | **Falso.** `FlashcardDialog` existe e está integrado em `estudar-page.ts`. O comportamento é coberto por `e2e/flashcards.spec.ts`, mas esta suíte segue mentindo sobre o motivo |
| `e2e/flashcards-decks-revisao.spec.ts:193` — **E2E-07** (tempo da sessão no total do dia) | "depende da tela de Início, ainda inexistente" | **Falso.** A tela existe; o que falta é o adaptador — resolvido pela tarefa 2.0 |

Um terceiro skip, `e2e/flashcards.spec.ts:250` (**E2E-13**, abrir a questão de origem), **continua
legítimo**: `estudar-page` lê apenas os *query params* `sessao`, `modo`, `simulado` e `tentativa` — a
rota `/estudar?questao={id}` de fato não existe. **Não mexer nele.**

Separadamente, o `qa.md` de `prd-editais` marca os 12 cenários como **PASSOU**, mas a nota da própria
seção admite que a suíte Playwright nunca foi executada e que a verificação foi feita "por
correspondência 1:1" entre o arquivo de teste e o protótipo — inspeção de código não é execução. O
motivo alegado para não executar (caminho com acentos, `Área de trabalho`, quebrando testes de
*slice*) **não se reproduz mais**: `EditalControllerTest` e `EditalAdminControllerTest` passam no
diretório original.

**Um `skip` cujo motivo não é mais verdadeiro é pior que um teste ausente: ele dá a impressão de
cobertura consciente.**

<rules>
### Conformidade com o AGENTS.md e as rules

- `.agents/rules/tests.md` §8 (checklist antes do PR) — nenhum teste pode ficar skipado com
  justificativa que não corresponde ao estado real do repositório.
</rules>

## Subtarefas

- [x] 5.1 Remover o `test.skip` de `e2e/estudo.spec.ts:98` (E2E-06). Duas opções válidas, escolher
  conforme o que fizer mais sentido para a suíte: (a) escrever o cenário real nesta suíte (criar
  flashcard durante a sessão de estudo sem perder o progresso, RF-28), ou (b) remover o placeholder e
  deixar um comentário apontando explicitamente que o comportamento é coberto por
  `e2e/flashcards.spec.ts`, citando o teste específico. **Feito (opção b)**: placeholder removido,
  comentário aponta `e2e/flashcards.spec.ts` "E2E-02 abre e fecha o modal preservando a questão, a
  alternativa marcada e o cronômetro".
- [x] 5.2 Confirmar que o `test.skip` de `e2e/flashcards-decks-revisao.spec.ts:193` (E2E-07) foi
  removido pela tarefa 2.0 — esta subtarefa é só a verificação de que não sobrou resíduo. **Feito**:
  confirmado, sem resíduo.
- [x] 5.3 **Não alterar** `e2e/flashcards.spec.ts:250` (E2E-13) — confirmar de novo, como parte desta
  tarefa, que a rota `/estudar?questao={id}` continua inexistente antes de fechar o item; se alguém
  tiver implementado a rota nesse meio-tempo, aí sim o skip também fica obsoleto. **Feito**:
  `estudar-page.ts` só lê `sessao`, `modo`, `simulado`, `tentativa`; rota `questao` não existe; skip
  mantido intacto.
- [x] 5.4 Executar `npx playwright test e2e/editais.spec.ts` pela primeira vez de fato (não por
  inspeção) e registrar o resultado real dos 12 cenários com evidência (prints ou relatório do
  Playwright). **Feito**: 12 passed (1.7m); evidência em
  `tasks/correcoes-pendencias/evidences/task_5-verify.md`.
- [x] 5.5 Atualizar `tasks/prd-editais/qa.md`: substituir os 12 "NÃO EXECUTADO" pelo resultado real
  medido em 5.4, removendo a nota de verificação por correspondência 1:1. **Feito**.
- [x] 5.6 Rodar `grep -rn "test.skip" frontend/e2e/` e confirmar que resta exatamente 1 ocorrência
  (E2E-13, legítima). **Feito**: exatamente 1 ocorrência (`flashcards.spec.ts:250`).

## Detalhes de implementação

Esta tarefa depende da 2.0 apenas para a subtarefa 5.2 (verificação); as subtarefas 5.1, 5.3, 5.4,
5.5 e 5.6 podem começar em paralelo com a 2.0.

## Critérios de aceitação relacionados

- RF-28 (criação de flashcard na sessão de estudo) — reforçado por E2E-06.
- Os 12 cenários E2E de `prd-editais` (ver `tasks/prd-editais/qa.md` para a lista).

## Testes da tarefa

- `e2e/estudo.spec.ts` (E2E-06, reescrito ou removido com justificativa correta).
- `e2e/editais.spec.ts` (12 cenários, executados pela primeira vez).

## Arquivos relevantes

- `frontend/e2e/estudo.spec.ts` (modificado)
- `frontend/e2e/flashcards-decks-revisao.spec.ts` (verificado, sem alteração própria)
- `frontend/e2e/flashcards.spec.ts` (verificado, sem alteração)
- `tasks/prd-editais/qa.md` (atualizado com resultado real)
