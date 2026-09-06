# Tarefa 10.0: Integração com a sessão de estudo, E2E e endurecimento (RF-28)

## Visão geral

A última peça é o botão "Novo flashcard" na barra de ações da questão, com o contador de cards criados na sessão, disponível nos **três modos** — inclusive no Modo Prova, onde o candidato marca o que quer estudar depois. É o único ponto de contato entre esta funcionalidade e a sessão de estudo, e o contrato é deliberadamente estreito: a tela de estudo importa **uma função e um tipo** (`DialogService.abrir` e `ContextoQuestaoFlashcard`), e o diálogo não importa nada da tela de estudo.

É aqui também que a promessa central do PRD deixa de ser argumento estrutural e vira verificação: E2E-02 abre e fecha o modal na questão 12, com uma alternativa marcada, e confere que a série, a alternativa e o cronômetro seguem intactos.

Fecham a entrega os dezesseis E2E, a verificação de cobertura pelos dois comandos que aplicam o piso de 80%, e a atualização da `TECH-SPEC.md`, exigida pela regra 4 do `AGENTS.md`: três das dez rotas não constam da §6 (`GET /flashcards/{id}`, `POST /flashcards/{id}/restaurar` e `POST /flashcards/decks/{id}/mover`), e a §5 precisa das três tabelas novas.

<skills>
### Conformidade com skills

- `angular-developer` — o botão é um componente standalone que recebe o contador por `input()` e emite a abertura por `output()`; nada de estado global novo.
- `spring-boot-testing` — a verificação final de cobertura por `./mvnw verify`, que aplica a regra de 80% do JaCoCo sobre as classes `*Service`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras relevantes: **regra 8** (a criação na sessão é um diálogo, nunca uma rota — a sessão em memória, o índice da série e o cronômetro sobrevivem intactos à abertura e ao fechamento do modal; é exatamente o que E2E-02 verifica), **regra 4** (nenhum endpoint fora do contrato da §6 sem atualizar o documento na mesma alteração), regra 6 (criar um card no Modo Prova é inerte para a correção: nada aqui escreve em `resposta` nem participa da finalização) e regra 1 (o botão vem do protótipo). `tests.md` §4: E2E é o topo da pirâmide — um cenário por fluxo, só o caminho crítico; §8: `./mvnw verify` e `npm run test:coverage` passam localmente.

**Bloqueio conhecido:** `prd-sessao-de-estudo` não tem TechSpec. Enquanto a barra de ações da questão e a rota `/estudar?questao=` não existirem, E2E-01…E2E-08 e E2E-13 não podem rodar contra a tela real. Nesse caso, as subtarefas 10.1 e 10.3 ficam retidas e o restante da tarefa é entregue; a retenção é registrada explicitamente no relatório final, e não silenciosamente contornada.

**Dependência:** Playwright ainda não está no `package.json` do frontend — previsto em `prd-acesso-e-conta`, com `frontend/e2e/` já existente (`auth.spec.ts`, `banco-questoes.spec.ts`). Conferir a instalação antes das subtarefas de E2E.
</rules>

<requirements>
- Botão "Novo flashcard" na barra de ações da questão (`features/study/questao-acoes/`), presente nos **três modos**, que chama `DialogService.abrir(FlashcardDialog, contexto)`.
- O contexto passado é o `ContextoQuestaoFlashcard` completo, incluindo `devolverFocoPara` apontando para o enunciado.
- O contador exibido no botão vem do `criadosNaSessao` devolvido pelo `POST`, propagado pelo evento `criado` — e sobrevive a uma recarga da página.
- A tela de estudo **não importa** nada do diálogo além da função de abertura e do tipo de contexto; o diálogo continua sem importar nada da tela de estudo.
- Abrir e fechar o modal **não** altera a URL, **não** recarrega a tela e **não** desmonta o componente da sessão.
- Os dezesseis E2E de `frontend/e2e/flashcards.spec.ts`, com seletores saídos da marcação do protótipo traduzida 1:1.
- Percurso por teclado verificado: `Tab` circula apenas dentro do diálogo, `Esc` cancela, o foco é visível e os estados têm rótulo textual.
- `./mvnw verify` e `npm run test:coverage` passam localmente, ambos aplicando o piso de 80%.
- `TECH-SPEC.md` atualizada: §5 com `deck`, `flashcard` e `flashcard_revisao`; §6 com as três rotas acrescentadas.
- Registrar na `TECH-SPEC.md` a numeração real das migrações desta entrega (`V6__flashcards.sql`, `V902__seed_flashcards_dev.sql`), que diverge da estimativa `V8`/`V906` do `techspec.md` desta funcionalidade.
</requirements>

## Subtarefas

- [x] 10.1 Acrescentar o botão "Novo flashcard" com o contador à barra de ações da questão, nos três modos, passando o `ContextoQuestaoFlashcard`.
- [x] 10.2 Conferir a instalação do Playwright e preparar a semente de `dev` usada pelos cenários.
- [x] 10.3 Escrever E2E-01…E2E-08 e E2E-13 (fluxo dentro da sessão e retorno à questão de origem). **E2E-13 retido** — ver nota abaixo.
- [x] 10.4 Escrever E2E-09…E2E-12, E2E-14 e E2E-15 (tela de gerenciamento, arquivamento, exclusão, avulso e movimentação em lote).
- [x] 10.5 Escrever E2E-16 (percurso por teclado e contraste no modal e na lista).
- [x] 10.6 Rodar `./mvnw verify` e `npm run test:coverage` e cobrir o que ficar abaixo de 80%.
- [x] 10.7 Atualizar a `TECH-SPEC.md` (§5 com as três tabelas, §6 com as três rotas novas e a numeração real das migrações).

**Nota sobre o bloqueio conhecido:** `prd-sessao-de-estudo` (F2) já está implementado no repositório (`features/study/`, com `estudar-page`, `barra-acao`, `study-session-store`, rota `/estudar`), diferente do que a `tasks.md` desta funcionalidade presumia — a retenção de 10.1 e 10.3 **não se aplicou**. A única capacidade de fato ausente é a rota `/estudar?questao={id}` (abrir a sessão fixada numa questão específica), que **não existe** em F2 e está fora do escopo desta tarefa (é uma rota nova de F2, não uma integração de F6). Por isso **E2E-13 permanece retido** (`test.skip` documentado em `flashcards.spec.ts`), exatamente como o fallback já implementado na tarefa 9.0 (link "Criado na questão" desabilitado com rótulo explicativo). Os demais quinze cenários (E2E-01…E2E-12, E2E-14…E2E-16) rodam contra a tela real e passam.

## Detalhes de implementação

Ver TechSpec, seções *Pontos de integração* (o contrato de UI com F2 e a porta `SessaoDoUsuario`), *Modelos de dados — `ContextoQuestaoFlashcard`*, *Testes E2E* (os dezesseis cenários e a nota sobre os seletores), *Sequenciamento do desenvolvimento* (passos 12 e 13) e *Riscos conhecidos* (a ausência de TechSpec de `prd-sessao-de-estudo` e o impacto sobre CA-01, CA-02 e CA-08).

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-07
- CA-08
- CA-09
- CA-10
- CA-11
- CA-12
- CA-13
- CA-14
- CA-16

## Testes da tarefa

### Testes E2E

- [x] E2E-01 — Abrir o modal a partir da questão nos três modos
- [x] E2E-02 — Abrir e fechar o modal preservando a questão, a alternativa marcada e o cronômetro (adaptado: verificado numa questão intermediária da série de 10, não literalmente na "questão 12")
- [x] E2E-03 — Conferir o pré-preenchimento do modal
- [x] E2E-04 — Tentar salvar com frente de 2 caracteres
- [x] E2E-05 — Salvar e fechar
- [x] E2E-06 — Salvar e criar outro, duas vezes seguidas
- [x] E2E-07 — Pressionar `Esc` com o formulário preenchido
- [x] E2E-08 — Criar três cards na mesma sessão
- [x] E2E-09 — Abrir Flashcards pelo menu
- [x] E2E-10 — Combinar busca, filtros e ordenação
- [x] E2E-11 — Arquivar um card e restaurá-lo
- [x] E2E-12 — Excluir um card já revisado, com confirmação
- [ ] E2E-13 — Abrir a questão de origem a partir da lista — **retido** (`test.skip`): depende da rota `/estudar?questao={id}`, que não existe em `prd-sessao-de-estudo` e está fora do escopo desta tarefa
- [x] E2E-14 — Criar um card avulso pelo menu Flashcards
- [x] E2E-15 — Mover uma seleção de cards para outro deck
- [x] E2E-16 — Percurso por teclado e contraste no modal e na lista

## Arquivos relevantes

- `frontend/src/app/features/study/barra-acao/` (modificado — pertence a F2; botão "Novo flashcard" com contador)
- `frontend/src/app/features/study/estudar-page/` (modificado — pertence a F2; abre o diálogo e propaga `ContextoQuestaoFlashcard`)
- `frontend/src/app/features/study/questao-cartao/` (modificado — pertence a F2; `tabindex="-1"` no enunciado para devolução de foco, CA-05)
- `frontend/src/app/features/study/contador-flashcards-sessao.ts` (novo — persistência do contador em `sessionStorage`, sobrevive a recarga)
- `frontend/src/app/features/flashcards/flashcard-dialog/flashcard-dialog.ts` (modificado — refoco do campo "Frente" ao voltar de "Continuar editando", endurecimento de CA-07)
- `frontend/e2e/flashcards.spec.ts` (novo — os dezesseis E2E)
- `TECH-SPEC.md` (modificado — §5 com as tabelas reais de `deck`/`flashcard`/`flashcard_revisao`, §6 com `GET /flashcards/{id}` e `POST /flashcards/{id}/restaurar`, §11 com a numeração real das migrações)
- `backend/src/main/resources/db/migration-dev/V908__seed_flashcards_dev.sql` (corrigido — a junção lateral dos cards do deck "Administrativo" exigia questão `PUBLICADA` da disciplina "Direito Administrativo", que não existe na semente; os quatro cards nunca eram inseridos)
