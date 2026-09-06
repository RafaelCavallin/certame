# Tarefa 7.0: Frontend — telas de Início e sessão de estudo

## Visão geral

Traduz 1:1 o protótipo corrigido (Tarefa 6.0) em componentes Angular standalone/zoneless: o cliente HTTP dos sete endpoints, o `StudySessionStore` que guarda série, índice, modo, respostas e marcações, o `Cronometro` que nunca reinicia por troca de modo ou questão, os componentes da tela de estudo com seus três modos e a correção do Modo Prova, o Início do estudante, e a integração no shell e nas rotas. Depende das Tarefas 5.0 (contrato da API) e 6.0 (protótipo corrigido).

<skills>
### Conformidade com skills

- `angular-developer` — standalone, *zoneless*, `signal`/`computed` para o estado da série (não `httpResource`/`resource`, a série é carregada uma vez e vive no store), `input()`/`output()` como funções, roteamento com `authGuard`, `role="radiogroup"` nas alternativas, `aria-live="polite"` no painel de gabarito, foco visível e navegação completa por teclado.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `code-standards.md` e `folder-structure.md` lidos.

- Regra 1 (`AGENTS.md`) — tradução 1:1 do protótipo já corrigido na Tarefa 6.0; só tokens `var(--*)` de `styles.css`; sem biblioteca de UI de terceiros.
- Regra 8 (`AGENTS.md`) — a criação de flashcard na sessão é diálogo, nunca rota; `StudySessionStore` e `Cronometro` são `providedIn: 'root'` e sobrevivem intactos à abertura/fechamento do modal.
- `code-standards.md` §4 — componentes standalone, estado em `signal`/`computed`, template e estilo em arquivos separados, HTTP concentrado em `core/api`, 422 mapeado campo a campo.
- `folder-structure.md` §3 — `features/study/`, `features/dashboard/`, clientes em `core/api/`, sem sufixo `.component` nos nomes de arquivo.
</rules>

<requirements>
- RF-06, RF-07, RF-08, RF-09, RF-10, RF-11, RF-12
- Restrição técnica: estado da sessão sobrevive ao diálogo de flashcard (RF-28)
</requirements>

## Subtarefas

- [x] 7.1 Implementar `core/api/estudo-api.ts` e `core/api/modelos-estudo.ts` (cliente HTTP dos 7 endpoints e os tipos do contrato).
- [x] 7.2 Implementar `features/study/study-session-store.ts` (`StudySessionStore`, `providedIn: 'root'`): série, índice, modo, respostas, marcações, gabaritos recebidos por questão.
- [x] 7.3 Implementar `features/study/cronometro.ts` (`Cronometro`): segundos por `interval`, pausa em `document.hidden` e no botão da topbar, acumula `pausadoMs` monotônico, nunca reinicia.
- [x] 7.4 Implementar `features/study/atalhos-teclado.ts` (diretiva pura: `A`–`E`, `C`/`E`, `Enter`, `←`/`→`, `F`).
- [x] 7.5 Implementar `features/study/estudar-page/`, `painel-filtros/`, `seletor-modo/`, `questao-cartao/`, `barra-acao/`, `gabarito-painel/`, `painel-navegacao/`, `correcao-prova/` — a tela de resolução completa e a correção do Modo Prova.
- [x] 7.6 Modificar `features/dashboard/inicio-page` (deixa de ser stub): cards do dia, ação "continuar de onde parou".
- [x] 7.7 Modificar `layout/shell` (item "Resolver questões" na navegação, cronômetro na topbar quando a rota é `/estudar`) e `app.routes.ts` (rota `/estudar` sob `authGuard`, com `?modo=` e `?sessao=`).

## Detalhes de implementação

Ver TechSpec, "Frontend — `frontend/src/app`" para a responsabilidade de cada componente e "Fluxo de dados" para a sequência exata Início → abrir/retomar → responder → finalizar. Ver "Modelos de dados" para os contratos de `RespostaSessao`, `QuestaoDaSerie`, `RespostaRegistrada`, `RespostaCorrecao` e `ResumoInicio` que o `StudySessionStore` consome.

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01
- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-07
- CA-08
- CA-09
- CA-11
- CA-12
- CA-13
- CA-14

## Testes da tarefa

### Testes de unidade

- [x] TU-41 — `StudySessionStore` avança o índice sem nova requisição
- [x] TU-42 — `StudySessionStore` mantém a alternativa marcada ao trocar de modo
- [x] TU-43 — `StudySessionStore` substitui a alternativa marcada, sem acumular
- [x] TU-44 — `StudySessionStore` não expõe gabarito no Modo Prova
- [x] TU-45 — `StudySessionStore` permite trocar resposta de questão já respondida na prova
- [x] TU-46 — `StudySessionStore` bloqueia nova resposta fora da prova
- [x] TU-47 — `StudySessionStore` vai direto para a questão 12 preservando respostas
- [x] TU-48 — `Cronometro` não reinicia na troca de modo nem de questão
- [x] TU-49 — `Cronometro` acumula pausa em `document.hidden` e retoma na volta
- [x] TU-50 — `atalhos-teclado` mapeia letras, Enter e setas

## Arquivos relevantes

- `frontend/src/app/core/api/estudo-api.ts`
- `frontend/src/app/core/api/modelos-estudo.ts`
- `frontend/src/app/features/study/study-session-store.ts`
- `frontend/src/app/features/study/cronometro.ts`
- `frontend/src/app/features/study/atalhos-teclado.ts`
- `frontend/src/app/features/study/estudar-page/`
- `frontend/src/app/features/study/painel-filtros/`
- `frontend/src/app/features/study/seletor-modo/`
- `frontend/src/app/features/study/questao-cartao/`
- `frontend/src/app/features/study/barra-acao/`
- `frontend/src/app/features/study/gabarito-painel/`
- `frontend/src/app/features/study/painel-navegacao/`
- `frontend/src/app/features/study/correcao-prova/`
- `frontend/src/app/features/dashboard/inicio-page.ts` (modificado, ganha `.html`/`.scss`/`.spec.ts`)
- `frontend/src/app/layout/shell/shell.ts` (modificado)
- `frontend/src/app/layout/shell/shell.html` (modificado)
- `frontend/src/app/app.routes.ts` (modificado)
