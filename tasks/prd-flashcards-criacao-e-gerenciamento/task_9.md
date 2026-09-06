# Tarefa 9.0: Tela de gerenciamento `/flashcards` (RF-31, RF-32)

## Visão geral

A segunda metade da funcionalidade: o menu **Flashcards** e a tela onde o estudante organiza o acervo. Aqui ele não está concentrado numa questão, está em modo de organização — e a densidade de informação ajuda.

A tela cabe em **uma requisição**: a listagem devolve a página filtrada e os indicadores globais juntos. Os indicadores ignoram os filtros de propósito, e é isso que permite distinguir os dois estados vazios: `total` igual a zero leva ao estado de boas-vindas ("você ainda não tem cards"); `total` maior que zero com `content` vazio leva a "Nenhum flashcard com esses filtros". A edição abre o mesmo diálogo da tarefa 8.0 com os dados já carregados da linha, sem segunda leitura — frente e verso viajam completos justamente por isso.

Filtros e busca são refletidos em *query params*, para que a tela sobreviva a uma recarga e possa ser compartilhada. Arquivar, restaurar, excluir e mover recarregam apenas a página corrente.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone e *zoneless*; estado por signals em `FlashcardsStore`; `input()`/`output()` como funções; roteamento com `authGuard`; `DatePipe` com `LOCALE_ID` `pt-BR` para a próxima revisão; acessibilidade por ARIA na tabela e nas ações de linha.
- `impeccable` — **não aplicável.** A tela "Meus flashcards" já existe no protótipo e é traduzida 1:1, com as lacunas fechadas na tarefa 1.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras relevantes: regra 1 (tradução 1:1 do protótipo, só tokens `var(--*)` de `styles.css`, sem biblioteca de terceiros), regra 2 (a interface esconder a ação não substitui a autorização do servidor — a privacidade real é a da tarefa 6.0) e regra 3 (interface e datas em pt-BR). `code-standards.md` §4: HTTP concentrado em `core/api` (o `flashcard-api` da tarefa 8.0 é reutilizado, não duplicado), estado em signals, template e estilo em arquivos separados. `tests.md` §2 (*Repeatable*): a busca com atraso de digitação usa temporizador falso, sem espera real.

**Reuso:** `shared/ui/confirmacao-dialog/` já existe (`prd-banco-de-questoes`) e é usado na exclusão de card, sem reescrita.

**Bloqueio conhecido:** depende da aprovação das lacunas do protótipo (tarefa 1.0), em especial a multisseleção com "Mover para deck".
</rules>

<requirements>
- Rotas `/flashcards` e `/flashcards/:id` em `app.routes.ts`, sob `authGuard`. A **criação não ganha rota** (RF-28, regra 8).
- Item "Flashcards" (`ph ph-lightning`) na seção Estudo do `layout/shell/shell.html`, com o contador de cards vencendo hoje.
- `FlashcardsPage` com a faixa de indicadores, os filtros, a tabela paginada, as ações de linha e os dois estados vazios distintos.
- Cada linha exibe frente resumida, deck, disciplina/assunto, estado e próxima revisão; o corte de 96 caracteres do verso é do cliente.
- Estados exibidos por **rótulo textual** ("Novo", "Aprendendo", "Em revisão", "Reaprendendo"), não só por cor.
- `FlashcardsFiltros` com busca com atraso de digitação e os três seletores (deck, disciplina, estado), refletidos em *query params* para recarga e compartilhamento.
- Ordenação por criação e por próxima revisão, nas duas direções.
- `FlashcardsIndicadores` alimentado pelo bloco `indicadores` da própria listagem — **sem segunda requisição**.
- `FlashcardsStore` com `pagina`, `filtro`, `indicadores`, `carregando`, `vazio` e `selecionados`, e os métodos `carregar`, `filtrar`, `arquivar`, `excluir` e `moverSelecionados`.
- Uma requisição por mudança de filtro, com todos os parâmetros juntos.
- Ações de linha: editar (abre o diálogo da tarefa 8.0 com os dados da linha, sem nova leitura), arquivar, restaurar, excluir e "ver questão de origem" quando houver origem.
- Exclusão com `confirmacao-dialog` antes de disparar o `DELETE`.
- `MoverParaDeck` envia exatamente os ids marcados e fica desabilitado com seleção vazia.
- Criação avulsa a partir da tela, abrindo o diálogo sem contexto de questão.
- Arquivar, restaurar, excluir e mover recarregam apenas a página corrente.
</requirements>

## Subtarefas

- [x] 9.1 Criar `FlashcardsStore` com os signals de página, filtro, indicadores, carregamento, vazio e seleção.
- [x] 9.2 Criar `FlashcardsPage` com a tabela paginada, as ações de linha e os dois estados vazios.
- [x] 9.3 Criar `FlashcardsFiltros` com busca com atraso, os três seletores e a ordenação, refletidos em *query params*.
- [x] 9.4 Criar `FlashcardsIndicadores` sobre o bloco da própria listagem.
- [x] 9.5 Criar `MoverParaDeck` com a ação em lote sobre as linhas selecionadas.
- [x] 9.6 Ligar edição, arquivamento, restauração, exclusão com confirmação, criação avulsa e "ver questão de origem".
- [x] 9.7 Acrescentar o item "Flashcards" ao shell e as rotas `/flashcards` e `/flashcards/:id` sob `authGuard`.
- [x] 9.8 Escrever TU-41…TU-44.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`FlashcardsStore`), *Visão dos componentes* (linhas de `features/flashcards/flashcards-page/`, `flashcards-filtros/`, `flashcards-indicadores/`, `mover-para-deck/`, `layout/shell/shell.html` e `app.routes.ts`), *Endpoints da API — `GET /flashcards`* (a nota sobre distinguir acervo vazio de filtro vazio pelos indicadores) e *Principais decisões* (indicadores dentro da listagem e alheios ao filtro; frente e verso completos na listagem, sem segunda requisição para editar).

## Critérios de aceitação relacionados

- CA-09
- CA-10
- CA-11
- CA-12
- CA-13
- CA-14
- CA-16

## Testes da tarefa

### Testes de unidade

- [x] TU-41 — `FlashcardsStore` combina busca e filtros (Vitest)
- [x] TU-42 — `FlashcardsStore` distingue acervo vazio de filtro vazio (Vitest)
- [x] TU-43 — `FlashcardsPage` exibe estado por rótulo textual (Vitest)
- [x] TU-44 — `MoverParaDeck` envia apenas os selecionados (Vitest)

## Arquivos relevantes

- `frontend/src/app/features/flashcards/flashcards-page/flashcards-page.ts`
- `frontend/src/app/features/flashcards/flashcards-page/flashcards-page.html`
- `frontend/src/app/features/flashcards/flashcards-page/flashcards-page.scss`
- `frontend/src/app/features/flashcards/flashcards-page/flashcards-page.spec.ts`
- `frontend/src/app/features/flashcards/flashcards-filtros.ts` (sem template próprio — classe de estado/roteamento nos moldes de `QuestoesFiltros`; a marcação vive em `flashcards-page.html`)
- `frontend/src/app/features/flashcards/flashcards-filtros.spec.ts`
- `frontend/src/app/features/flashcards/flashcards-indicadores.ts` (idem, sem template próprio)
- `frontend/src/app/features/flashcards/flashcards-store.ts`
- `frontend/src/app/features/flashcards/flashcards-store.spec.ts`
- `frontend/src/app/features/flashcards/mover-para-deck/mover-para-deck.ts`
- `frontend/src/app/features/flashcards/mover-para-deck/mover-para-deck.spec.ts`
- `frontend/src/app/features/flashcards/flashcard-dialog/flashcard-dialog.ts` (modificado — modo edição reaproveitando o diálogo da tarefa 8.0)
- `frontend/src/app/features/flashcards/flashcard-dialog/flashcard-dialog.html` (modificado)
- `frontend/src/app/features/flashcards/flashcard-dialog/flashcard-dialog.spec.ts` (modificado)
- `frontend/src/app/features/flashcards/flashcard-form-store.ts` (modificado — `abrirEdicao`, `modoEdicao`, envio via `atualizar`)
- `frontend/src/app/features/flashcards/flashcard-form-store.spec.ts` (modificado)
- `frontend/src/app/layout/shell/shell.ts` (modificado — contador de cards vencendo hoje)
- `frontend/src/app/layout/shell/shell.html` (modificado)
- `frontend/src/app/layout/shell/shell.scss` (modificado)
- `frontend/src/app/layout/shell/shell.spec.ts` (modificado)
- `frontend/src/app/app.routes.ts` (modificado)
- `protótipo/Certame.dc.html` (modificado — lacuna 1.1 composta nesta execução: multisseleção, barra "Mover para deck", alternância Ativos/Arquivados, ordenação e o segundo estado vazio, ver `task_1.md`)
- `backend/src/main/java/br/com/certame/catalogo/TaxonomiaService.java` (modificado — bug real encontrado em QA manual: `GET /admin/disciplinas` respondia 403 para `ESTUDANTE`, quebrando o filtro de disciplina desta tarefa e o modo avulso do diálogo da tarefa 8.0; `arvore()` passou a exigir só `isAuthenticated()`, mutações continuam restritas a PROFESSOR/ADMIN/SUPER_ADMIN)
- `backend/src/test/java/br/com/certame/catalogo/DisciplinaEditalIntegracaoTest.java` (modificado — TI24 prova a leitura por ESTUDANTE)
