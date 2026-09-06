# Tarefa 9.0: Frontend — sessão de revisão de flashcards

## Visão geral

A tela mais exercitada da funcionalidade: `/flashcards/estudar`, orquestrando card, notas, resumo e estado vazio, operável inteiramente por teclado (espaço revela, 1–4 classificam). Depende do `IntervaloPipe` e do padrão de store já usado na tarefa 8.0. Fecha com as rotas sob `authGuard` e o contador do item Flashcards no shell alimentado por `GET /flashcards/resumo`.

<skills>
### Conformidade com skills

- `angular-developer` — signals para fila em memória, índice, revelado e contagem por nota (`RevisaoStore`); `computed` para progresso, restantes e estado vazio; atalhos de teclado com foco visível.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/tests.md` e `.agents/rules/folder-structure.md`.

- **Regra 1 (AGENTS.md)** — tradução 1:1 do protótipo ajustado na tarefa 1.0, incluindo o rótulo de intervalo em minutos.
- PRD, seção "UI/UX e acessibilidade" — sessão operável inteiramente por teclado, foco visível, atalhos indicados na própria tela, contraste mínimo 4,5:1.
- `folder-structure.md` §3.2 — `/flashcards/estudar` aceita `?deck=` e `?disciplina=`.
</rules>

<requirements>
- RF-33, RF-34, RN-08 (consumo, não cálculo — o cliente exibe o que o servidor já formatou).
- CA-01 a CA-11, CA-19.
</requirements>

## Subtarefas

- [x] 9.1 `shared/pipes/intervalo-pipe.ts`: formata o rótulo de intervalo em pt-BR quando o cliente precisa recompor um.
- [x] 9.2 `features/flashcards/revisao-store.ts`: `abrir(escopo)`, `revelar()` (CA-02), `classificar(nota)` (CA-05), `finalizar()` (CA-10); signals `cardAtual`, `revelado` (falso a cada card novo — CA-01), `restantes`, `progressoPct`, `filaVazia` (CA-19), `resumo`; reenfileiramento do card com as previsões novas quando o servidor devolve `reenfileirar: true`.
- [x] 9.3 `features/flashcards/atalhos-revisao.ts`: espaço revela, 1–4 classificam, ignorando eventos de campo de texto e teclas de classificação com o verso oculto.
- [x] 9.4 `features/flashcards/cronometro-sessao.ts`: tempo da sessão por signal, pausado em `document.hidden`.
- [x] 9.5 `features/flashcards/sessao-revisao/`: página orquestradora; `card-revisao/` (frente, verso sob demanda, linha de memória — CA-04); `notas-revisao/` (quatro botões com rótulo, intervalo previsto e atalho — CA-03, CA-05); `resumo-revisao/` (RF-34: cards revisados, distribuição, tempo total, próximos vencimentos — CA-10); `fila-vazia/` (CA-19, sem ação de antecipar).
- [x] 9.6 Estender `app.routes.ts` com `/flashcards/decks` e `/flashcards/estudar` sob `authGuard`.
- [x] 9.7 Atualizar `layout/shell/shell.html`: o contador de "vencendo hoje" do item Flashcards passa a ser alimentado por `GET /flashcards/resumo`.

## Detalhes de implementação

Ver `techspec.md`, seção "Principais interfaces" (bloco `RevisaoStore`), "Fluxo de dados" e "Modelos de dados" (`CardRevisao`, `MemoriaCard`, `PrevisaoNota`, exemplos de primeiro contato e reenfileiramento).

## Critérios de aceitação relacionados

- CA-01, CA-02, CA-03, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-10, CA-11, CA-19

## Testes da tarefa

### Testes de unidade

- [x] TU-55 — `RevisaoStore` inicia cada card sem o verso revelado
- [x] TU-56 — `RevisaoStore.revelar` expõe verso e classificações
- [x] TU-57 — `RevisaoStore.classificar` avança para o próximo card
- [x] TU-58 — `RevisaoStore` reenfileira o card quando o servidor manda
- [x] TU-59 — `RevisaoStore` sinaliza fila vazia sem oferecer antecipação
- [x] TU-60 — `RevisaoStore.finalizar` expõe o resumo devolvido
- [x] TU-61 — `AtalhosRevisao` mapeia espaço e as teclas 1 a 4
- [x] TU-62 — `AtalhosRevisao` ignora 1–4 com o verso oculto
- [x] TU-63 — `AtalhosRevisao` ignora teclas vindas de campo de texto
- [x] TU-64 — `IntervaloPipe` formata minutos, dias e "Amanhã"
- [x] TU-67 — `CronometroSessao` acumula e pausa em `document.hidden`

## Arquivos relevantes

- `shared/pipes/intervalo-pipe.ts`
- `features/flashcards/revisao-store.ts`
- `features/flashcards/atalhos-revisao.ts`
- `features/flashcards/cronometro-sessao.ts`
- `features/flashcards/sessao-revisao/`
- `features/flashcards/sessao-revisao/card-revisao/`
- `features/flashcards/sessao-revisao/notas-revisao/`
- `features/flashcards/sessao-revisao/resumo-revisao/`
- `features/flashcards/sessao-revisao/fila-vazia/`
- `app.routes.ts` (modificado)
- `layout/shell/shell.html` (modificado)
