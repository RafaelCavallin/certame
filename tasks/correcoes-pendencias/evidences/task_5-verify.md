# Evidência — Tarefa 5.0 (P2-1/P2-2)

## 5.1 — `e2e/estudo.spec.ts:98` (E2E-06)

Opção (b) escolhida: o placeholder foi removido e substituído por um comentário apontando o teste
que já cobre o cenário real (RF-28 — criar flashcard na sessão sem perder o progresso):

> E2E-06 (RF-28) é coberto por `e2e/flashcards.spec.ts`, teste "E2E-02 abre e fecha o modal
> preservando a questão, a alternativa marcada e o cronômetro".

Conferido que esse teste (linhas 55-73 de `flashcards.spec.ts`) de fato abre o `FlashcardDialog`
durante uma sessão de estudo ativa e verifica código da questão, alternativa marcada e cronômetro
intactos após fechar o modal — o mesmo comportamento que E2E-06 pretendia cobrir.

## 5.2 — `e2e/flashcards-decks-revisao.spec.ts:193` (E2E-07)

```text
$ grep -n "test.skip" frontend/e2e/flashcards-decks-revisao.spec.ts
(sem resultado)
```

Confirmado: a tarefa 2.0 já removeu o `test.skip`; `E2E-07` (linha 210) é hoje um teste ativo.

## 5.3 — `e2e/flashcards.spec.ts:250` (E2E-13) — não alterar

Confirmado que `estudar-page.ts` continua lendo apenas os query params `sessao`, `modo`, `simulado`
e `tentativa`:

```text
$ grep -n "queryParamMap.get" frontend/src/app/features/study/estudar-page/estudar-page.ts
61:    const sessao = this.rota.snapshot.queryParamMap.get('sessao');
66:        modo: (this.rota.snapshot.queryParamMap.get('modo') as ModoSessao) || 'FOCO',
178:    const simuladoId = this.rota.snapshot.queryParamMap.get('simulado');
179:    const tentativaId = this.rota.snapshot.queryParamMap.get('tentativa');
```

A rota `/estudar?questao={id}` continua inexistente. O skip de E2E-13 segue legítimo — nenhuma
alteração feita nele.

## 5.4 — Execução real de `e2e/editais.spec.ts`

Ambiente isolado subido pelo próprio `webServer` do `playwright.config.ts` (compose
`certame-e2e`, backend em `:3000`, frontend em `:5100`, Postgres em `:30532`, MailHog em `:30525`).
Node 18 do ambiente padrão não atende ao mínimo do Playwright (>=20); execução feita com
`nvm use 22`.

```text
$ npx playwright test e2e/editais.spec.ts --reporter=list

Running 12 tests using 1 worker

  ✓  1  E2E-01 estudante abre Editais pelo menu e vê os três agrupamentos por status (1.9s)
  ✓  2  E2E-02 abrir um edital pela URL exibe as disciplinas exigidas sem nova requisição à API (2.7s)
  ✓  3  E2E-03 confere banca, vagas e prazo formatados em pt-BR no edital aberto (2.2s)
  ✓  4  E2E-04 administrador cadastra um edital completo e ele já aparece para o estudante (4.3s)
  ✓  5  E2E-05 administrador corrige um dado e o estudante vê o valor novo ao recarregar (5.1s)
  ✓  6  E2E-06 administrador muda edital de previsto para aberto e ele migra de agrupamento na tela do estudante (4.6s)
  ✓  7  E2E-07 administrador exclui um edital com confirmação e ele some da lista do estudante (3.3s)
  ✓  8  E2E-08 seleciona disciplinas a partir da árvore de matérias, sem aceitar texto livre (2.4s)
  ✓  9  E2E-10 estudante não alcança a administração de editais nem pela interface nem pela API (2.4s)
  ✓ 10  E2E-11 lista administrativa sinaliza o edital aberto com prazo vencido (1.7s)
  ✓ 11  E2E-12 percorre as duas telas por teclado com foco visível e rótulos de status com contraste adequado (4.4s)
  ✓ 12  E2E-09 base sem nenhum edital exibe o estado vazio explicativo ao estudante (3.1s)

  12 passed (1.7m)
```

Relatório HTML gerado em `frontend/playwright-report/index.html` (não versionado — artefato local
de execução, como os demais relatórios de `target/`/`coverage/`).

Confirmado também: o caminho com acentos (`Área de trabalho`) não quebra a suíte —
`EditalControllerTest`/`EditalAdminControllerTest` e agora todo o `editais.spec.ts` rodam
normalmente nesse diretório.

## 5.5 — `tasks/prd-editais/qa.md` atualizado

Os 12 "NÃO EXECUTADO" foram substituídos pelo resultado real de 5.4 (12 passaram), e a nota de
verificação por correspondência 1:1 foi removida.

## 5.6 — Resíduo de `test.skip`

```text
$ grep -rn "test.skip" frontend/e2e/
frontend/e2e/flashcards.spec.ts:250:  test.skip(
```

Exatamente 1 ocorrência, a de E2E-13 (legítima, ver 5.3).
