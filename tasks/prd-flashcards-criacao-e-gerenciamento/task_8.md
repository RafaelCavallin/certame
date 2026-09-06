# Tarefa 8.0: Modal de criação de flashcard (RF-29, RF-30)

## Visão geral

O modal é a promessa central do PRD: criar um flashcard não pode custar a sessão de estudo. O desenho que garante isso é negativo — o que o `FlashcardDialog` **não** faz. Ele **não** injeta `StudySessionStore`, **não** injeta `Router`, **não** lê a rota e **não** conhece o cronômetro. A única coisa que recebe é um `ContextoQuestaoFlashcard` imutável; a única coisa que devolve é um evento de card criado. É isso que faz CA-02 ser uma propriedade estrutural, verificável por leitura dos imports, e não um cuidado a manter.

O `FlashcardFormStore` cuida das três ações de RF-30. **Salvar e criar outro** limpa frente e verso e preserva deck, disciplina, assunto e tags, com o foco de volta na frente. **Cancelar** e `Esc` são o mesmo caminho: com texto digitado, pedem confirmação; com campos vazios, fecham direto — confirmar o descarte de nada seria ruído. O contador do botão assume o `criadosNaSessao` **devolvido pelo servidor**, nunca um incremento local, para que recarregar a página ou abrir outra aba não perca nem duplique a contagem.

O rascunho em `sessionStorage` sob `fc-draft:{questaoId}` protege o texto contra uma recarga acidental. Não há fila de reenvio offline — decisão registrada na TechSpec.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone e *zoneless*; estado em signals; `input()`/`output()` como funções; **Reactive Forms**, não Signal Forms, mantendo a decisão de `prd-acesso-e-conta`, `prd-banco-de-questoes` e `prd-editais`; HTTP concentrado em `core/api`; template e estilo em arquivos separados.
- `impeccable` — **não aplicável.** O modal já existe no protótipo aprovado e é traduzido 1:1.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras relevantes: **regra 8** (a criação na sessão é um diálogo, nunca uma rota — nenhuma rota de criação em `app.routes.ts`, nenhuma injeção do store da sessão, nenhum `router.navigate`), regra 1 (tradução 1:1 do protótipo, tokens `var(--*)`, sem biblioteca de terceiros) e regra 3 (interface em pt-BR, com as datas formatadas por `DatePipe` com `LOCALE_ID` `pt-BR`). `code-standards.md` §4: HTTP só em `core/api`, estado em signals, componentes standalone. `tests.md` §2 (*Repeatable*): a busca e os temporizadores usam `vi.useFakeTimers()`.

**Bloqueio conhecido (resolvido nesta execução):** dependia da aprovação das lacunas do protótipo (tarefa 1.0). O "+ Novo deck" no seletor e o modo avulso com disciplina e assunto editáveis foram compostos no `Certame.dc.html` com componentes e tokens já existentes (ver task_1.md, subtarefas 1.2/1.3) e aprovados para esta execução antes de iniciar 8.0.
</rules>

<requirements>
- `core/api/flashcard-api.ts` como **único** ponto que fala com a API de flashcards, cobrindo as dez rotas; `core/api/modelos-flashcard.ts` com a tipagem dos contratos.
- `FlashcardDialog` recebe um `ContextoQuestaoFlashcard` imutável (`questaoId`, `codigo`, `disciplinaId`/`disciplinaNome`, `assuntoId`/`assuntoNome`, `sessaoId`, `devolverFocoPara`) e emite `criado`.
- **Nenhum import de `StudySessionStore`, de `Router` ou de `location`** no diálogo nem no store do formulário.
- Com origem: disciplina e assunto exibidos em **modo somente leitura**, com a tarja "Vinculado à questão em tela" e o código da questão. Sem origem (modo avulso): seletores editáveis e sem tarja.
- Validação inline: frente e verso com mínimo de 3 caracteres após recorte; abaixo disso, **nenhuma requisição é feita** e o erro aparece no campo correspondente.
- `422` do servidor mapeado campo a campo por `fieldErrors`; o texto digitado permanece e o diálogo **não fecha**.
- **Salvar e fechar:** cria, fecha o diálogo, publica o toast e devolve o foco ao elemento de `devolverFocoPara`.
- **Salvar e criar outro:** cria, mantém o diálogo aberto, limpa frente e verso, preserva deck, disciplina, assunto e tags, e devolve o foco à frente.
- **Cancelar** e `Esc`: com texto digitado, ativam o estado de confirmação e **não** fecham nem descartam antes dela; com campos vazios, fecham direto.
- `criadosNaSessao` assume o valor devolvido pelo `POST`, propagado no evento `criado`; nunca um incremento local.
- `rascunho-flashcard.ts` como funções puras sobre `sessionStorage`, sob `fc-draft:{questaoId}`: grava a cada digitação, restaura ao reabrir e limpa no salvamento e no descarte confirmado.
- `DeckSeletor` com a opção "Sem deck" e a ação "+ Novo deck", que cria por nome **sem sair do modal**.
- Até 10 tags; o servidor é a autoridade sobre a normalização.
</requirements>

## Subtarefas

- [x] 8.1 Criar `core/api/modelos-flashcard.ts` e `core/api/flashcard-api.ts` com as dez rotas.
- [x] 8.2 Criar `FlashcardDialog` em tradução 1:1 do protótipo, nos modos com origem e avulso.
- [x] 8.3 Criar `FlashcardFormStore` com a validação inline, as três ações e a confirmação de descarte.
- [x] 8.4 Implementar o contador da sessão a partir do `criadosNaSessao` da resposta e o evento `criado`.
- [x] 8.5 Criar `rascunho-flashcard.ts` com gravação, restauração e limpeza em `sessionStorage`.
- [x] 8.6 Criar `DeckSeletor` com "Sem deck" e "+ Novo deck".
- [x] 8.7 Escrever TU-31…TU-38.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`FlashcardFormStore`), *Modelos de dados* (`ContextoQuestaoFlashcard`, `RequisicaoFlashcard`, `RespostaFlashcardCriado`), *Visão dos componentes* (linhas de `features/flashcards/`), *Pontos de integração* (o contrato de UI com F2) e *Principais decisões* (o diálogo não conhece a sessão; o contador vem do servidor; rascunho em `sessionStorage` sem fila de reenvio; Reactive Forms, não Signal Forms; a criação é permitida nos três modos, inclusive no Modo Prova).

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-07
- CA-08
- CA-14

## Testes da tarefa

### Testes de unidade

- [x] TU-31 — `FlashcardFormStore` bloqueia o envio com frente curta (Vitest)
- [x] TU-32 — `FlashcardFormStore.salvarECriarOutro` (Vitest)
- [x] TU-33 — `FlashcardFormStore.cancelar` com texto digitado (Vitest)
- [x] TU-34 — `FlashcardFormStore` com `Esc` e campos preenchidos (Vitest)
- [x] TU-35 — `FlashcardFormStore.cancelar` com campos vazios (Vitest)
- [x] TU-36 — `FlashcardFormStore` atualiza o contador pela resposta (Vitest)
- [x] TU-37 — `FlashcardFormStore` mapeia 422 campo a campo (Vitest)
- [x] TU-38 — `rascunho-flashcard` grava, restaura e limpa (Vitest)

## Arquivos relevantes

- `frontend/src/app/core/api/flashcard-api.ts`
- `frontend/src/app/core/api/modelos-flashcard.ts`
- `frontend/src/app/features/flashcards/flashcard-dialog/flashcard-dialog.ts`
- `frontend/src/app/features/flashcards/flashcard-dialog/flashcard-dialog.html`
- `frontend/src/app/features/flashcards/flashcard-dialog/flashcard-dialog.scss`
- `frontend/src/app/features/flashcards/flashcard-form-store.ts`
- `frontend/src/app/features/flashcards/flashcard-form-store.spec.ts`
- `frontend/src/app/features/flashcards/rascunho-flashcard.ts`
- `frontend/src/app/features/flashcards/rascunho-flashcard.spec.ts`
- `frontend/src/app/features/flashcards/deck-seletor/deck-seletor.ts`
