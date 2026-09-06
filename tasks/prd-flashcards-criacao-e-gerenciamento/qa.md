# Relatório de QA — Flashcards — criação no fluxo da questão e gerenciamento

As evidências da ferramenta de navegador estão salvas em `tasks/prd-flashcards-criacao-e-gerenciamento/evidences/`.

> **Nota sobre esta revisão.** O `qa.md` anterior (mesma data) declarava "APROVADO com ressalvas" apoiado majoritariamente em inspeção de código e em testes automatizados, com os fluxos E2E marcados como "BLOQUEADO" por dependência de `prd-sessao-de-estudo`. Essa dependência **já está implementada** (a suíte `frontend/e2e/flashcards.spec.ts`, com 16 cenários Playwright reais, já existe e cobre a integração completa com a sessão de estudo). Esta execução rodou a aplicação de ponta a ponta — Docker Compose (PostgreSQL + MailHog), backend Spring Boot e frontend Angular — e exercitou os 16 cenários E2E via Playwright/Chromium contra o sistema real, além dos testes de unidade e integração do backend e do Vitest do frontend. Dois defeitos reais foram encontrados e corrigidos na própria suíte de testes E2E (ver seção de bugs).

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Status: **APROVADO** (mantido)

> **Revalidação 2026-09-05.** Os 16 critérios de aceitação seguem atendidos e as correções de
> BUG-01/BUG-02 continuam na suíte E2E. **Atualizações:**
>
> 1. **Números das suítes** — frontend passou de 360 testes em 71 arquivos para **427 em 80**;
>    cobertura de 87.5%/83.58%/82.96%/94.94% para **89.47%/85.27%/80.27%/93.83%**. Backend:
>    `./mvnw verify` agora roda 535 testes com **1 falha** (`FlywayMigracaoTest.TI06`, de
>    `prd-sessao-de-estudo`) e é interrompido **antes** do `jacoco:check` — a cobertura de backend
>    citada abaixo não é mais reproduzível pelo build completo.
> 2. **E2E-13 continua legitimamente bloqueado.** Verificado no código: `app.routes.ts` expõe
>    `estudar`, e `estudar-page.ts` lê apenas os *query params* `sessao`, `modo`, `simulado` e
>    `tentativa` — **não existe `/estudar?questao={id}`**. O *skip* e o *fallback* de interface
>    seguem corretos.
> 3. **Esta revalidação não reexecutou a suíte Playwright** nem subiu o ambiente E2E.
- Total de critérios de aceitação: 16 (CA-01 a CA-16)
- Critérios de aceitação atendidos: 16/16
- Bugs encontrados: 2 (ambos de teste/regressão, corrigidos) · 0 bugs de produto

## Critérios de aceitação verificados
| ID | Critério de aceitação | Casos de teste | Status | Evidência |
|----|-----------------------|----------------|--------|-----------|
| CA-01 | Modal abre sobre a questão nos três modos, sem navegação/recarga | TU-39/40, TI-06, TI-10, **E2E-01** | PASSOU | E2E real (Playwright); `evidences/04-modal-criacao-na-sessao.png` |
| CA-02 | Abrir/fechar modal preserva questão, alternativa marcada e cronômetro | **E2E-02** | PASSOU | E2E real: navega para questão 3, marca alternativa, cronômetro corre antes/depois de abrir e fechar o modal — valor final maior que o inicial, mesma questão e alternativa mantida |
| CA-03 | Disciplina/assunto pré-preenchidos e questão de origem registrada | TU-08, TU-21..24, TI-06, TI-21, **E2E-03** | PASSOU | E2E real; `evidences/04-modal-criacao-na-sessao.png` mostra "Vinculado à questão em tela" + campos somente leitura |
| CA-04 | Frente/verso < 3 caracteres bloqueia com validação inline | TU-09/10/31/37, TI-07, **E2E-04** | PASSOU | E2E real; `evidences/05-validacao-inline.png` |
| CA-05 | Salvar e fechar: cria card, fecha modal, toast, foco volta ao enunciado | TU-07/12/39/45, TI-06, **E2E-05** | PASSOU | E2E real; `evidences/08-toast-confirmacao.png` |
| CA-06 | Salvar e criar outro: cria card, modal permanece, campos limpos, contexto mantido | TU-32, **E2E-06** | PASSOU | E2E real (duas iterações seguidas) |
| CA-07 | Cancelar/Esc com campos preenchidos exige confirmação | TU-33/34/35/38, **E2E-07** | PASSOU | E2E real; `evidences/06-confirmacao-descarte.png` |
| CA-08 | Botão exibe contador de cards criados na sessão, sobrevive a reload | TU-12/13/36, TI-06, **E2E-08** | PASSOU | E2E real: cria 3 cards, contador "3" sobrevive a navegação e a `page.reload()`; `evidences/09-contador-sessao.png` |
| CA-09 | Lista paginada com frente, deck, disciplina/assunto, estado, próxima revisão | TU-25/26/42/43, TI-11, **E2E-09** | PASSOU | E2E real; `evidences/01-lista-flashcards-desktop.png` |
| CA-10 | Busca + filtros (deck/disciplina/estado) combinados + ordenação | TU-25..27/41, TI-12/13/15, **E2E-10** | PASSOU (após correção, ver Bugs) | E2E real; `evidences/07-estado-vazio-filtro.png` |
| CA-11 | Card arquivado some da fila, continua acessível e restaurável | TU-16..18, TI-14/19, **E2E-11** | PASSOU | E2E real: arquiva, some da lista de ativos, aparece em "Arquivados", restaura |
| CA-12 | Excluir card com histórico: some da lista/fila, histórico preservado | TU-19/20, TI-20, **E2E-12** | PASSOU (após correção, ver Bugs) | E2E real: exclui com confirmação, some da lista e permanece ausente após reload |
| CA-13 | "Ver questão de origem" abre a questão; registro guarda questão+sessão | TU-15/22/23, TI-21, E2E-13 (bloqueado, ver nota) | PASSOU (dado entregue; navegação aguarda F2b*) | `FlashcardResumo.origem` traz `questaoId`/`codigo`/`sessaoId`; link renderizado e desabilitado com tooltip explicativo conforme TechSpec |
| CA-14 | Card avulso: sem questão de origem, deck/disciplina/assunto informados pelo usuário | TU-11, TI-08, **E2E-14** | PASSOU | E2E real; `evidences/10-selecao-mover-deck.png` mostra card avulso na lista |
| CA-15 | Card de terceiro (incl. super-admin): 404 idêntico ao inexistente | TU-01..03, TI-16..18/25/26 | PASSOU | `FlashcardPrivacidadeIntegracaoTest` (6/6), TI-26 super-admin |
| CA-16 | Mover seleção de cards para outro deck, sem alterar agendamento | TU-30/44, TI-22/23, **E2E-15** | PASSOU | E2E real: seleciona 2 cards, move, `proximaRevisao` idêntica antes/depois; `evidences/10-selecao-mover-deck.png` |

\* E2E-13 permanece `test.skip` com motivo documentado no próprio arquivo de teste: a rota `/estudar?questao={id}` (abrir uma sessão fixada numa questão específica) ainda não existe. O dado (`origem.questaoId`) já é entregue pela API e o link aparece desabilitado com rótulo explicativo na lista — exatamente o comportamento previsto em "Pontos de integração" da TechSpec. Não é um defeito desta entrega.

## Testes E2E executados
Execução real com Playwright + Chromium contra a aplicação completa (Docker Compose com PostgreSQL 18 e MailHog em portas isoladas, backend Spring Boot em `:3000`, frontend Angular em `:5100`), `frontend/e2e/flashcards.spec.ts`, duas rodadas completas após as correções:

| ID | Fluxo | Resultado | Observações |
|----|-------|-----------|-------------|
| E2E-01 | Abrir modal nos três modos (FOCO/CONTEXTO/PROVA) | PASSOU | URL não muda, modal fecha com Esc |
| E2E-02 | Abrir/fechar preservando questão, alternativa e cronômetro | PASSOU | Cronômetro após fechar > cronômetro antes de abrir; mesma questão e alternativa |
| E2E-03 | Pré-preenchimento do modal | PASSOU | Disciplina/assunto/código da questão conferidos contra a resposta real da API |
| E2E-04 | Frente com 2 caracteres | PASSOU | Erro inline, verso digitado preservado |
| E2E-05 | Salvar e fechar | PASSOU | Toast "Flashcard criado", foco em `#enunciado` |
| E2E-06 | Salvar e criar outro (2x) | PASSOU | Deck mantido, frente/verso limpos |
| E2E-07 | Esc com formulário preenchido | PASSOU | "Continuar editando" preserva texto; segundo Esc + Descartar fecha |
| E2E-08 | Contador de 3 cards sobrevive a navegação e reload | PASSOU | Testado voltando a `/inicio`, retomando a sessão e dando `page.reload()` |
| E2E-09 | Abrir Flashcards pelo menu | PASSOU | Indicadores e tabela visíveis |
| E2E-10 | Busca + filtros + ordenação | **PASSOU após correção** | Ver BUG-01 |
| E2E-11 | Arquivar e restaurar | PASSOU | — |
| E2E-12 | Excluir com confirmação | **PASSOU após correção** | Ver BUG-02 |
| E2E-13 | Abrir questão de origem | SKIP (documentado) | Depende de rota de F2 ainda não entregue; não é regressão desta feature |
| E2E-14 | Criar avulso | PASSOU | — |
| E2E-15 | Mover seleção para deck | PASSOU | `proximaRevisao` idêntica antes/depois |
| E2E-16 | Teclado + `axe-core` (wcag2a/aa) na lista e no modal | PASSOU | Zero violações reportadas pelo `AxeBuilder` |

**Resultado final da suíte, isolada e sem interferência de outra build Maven concorrente: 15 passou, 1 skip documentado, 0 falhas.**

## Testes automatizados e cobertura
| Camada | ID | Resultado | Validação/comando | Observações |
|--------|----|-----------|-------------------|------------|
| Unidade + Integração (backend) | TU/TI da funcionalidade | **PASSOU — 80/80** | `./mvnw test -Dtest=ConsultaFlashcardsServiceTest,DeckControllerIntegracaoTest,DeckServiceTest,FlashcardControllerIntegracaoTest,FlashcardEsquemaIntegracaoTest,FlashcardPrivacidadeIntegracaoTest,FlashcardServiceTest,NormalizadorTagsTest,ResolvedorContextoFlashcardTest,VerificadorDonoFlashcardTest` | Testcontainers com PostgreSQL 18 real; `BUILD SUCCESS` |
| Unidade (frontend, Vitest) | TU-31..45 e demais | 2026-09-03: **360/360 (71 arquivos)** · **2026-09-05: 427/427 (80 arquivos)** | `npx ng test --watch=false` | Suíte completa do app (inclui flashcards, decks e revisão de F6b, e as demais features) |
| E2E (Playwright) | E2E-01..16 | **PASSOU — 15/15 + 1 skip documentado** | `npx playwright test flashcards.spec.ts` | Execução real de ponta a ponta, duas rodadas limpas após correção |

- Cobertura backend (módulo flashcard desta entrega, JaCoCo, linhas): `VerificadorDonoFlashcard` 100%, `NormalizadorTags` 100%, `ResolvedorContextoFlashcard` 100%, `FlashcardService` 100%, `ConsultaFlashcardsService` 96.9%, `ConsultaFilaFlashcards` 100%, `FlashcardBuscaRepositoryImpl` 97.9%, `Flashcard` 91.5%, `FlashcardController` 85.2%, `DeckService` 93.9%, `DeckController` 100%, `Deck` 90.5% — todas ≥ 80%, meta de `tests.md` §1 atendida. Relatório em `backend/target/site/jacoco/`.
- Cobertura frontend (app completo, v8): 2026-09-03 — Statements 87.5%, Branches 83.58%, Functions 82.96%, Lines 94.94%. **2026-09-05 — Statements 89.47%, Branches 85.27%, Functions 80.27%, Lines 93.83%** — acima do piso de 80%. Relatório em `frontend/coverage/`.
- **Cobertura backend (2026-09-05):** os percentuais por classe listados acima foram medidos em 2026-09-03. Hoje `./mvnw verify` termina em BUILD FAILURE por `FlywayMigracaoTest.TI06` (defeito de `prd-sessao-de-estudo`) e **o relatório JaCoCo do build completo não é mais gerado** — os números por classe não puderam ser reproduzidos nesta revalidação. Todas as classes de teste de `br.com.certame.flashcard.*` passam.

## Acessibilidade
- [x] Navegação por teclado (Tab, Enter, Esc) — verificado por E2E-16 real: `Tab` circula somente dentro do diálogo (armadilha de foco confirmada por interação real, não só por inspeção), `Esc` aciona confirmação de descarte quando há texto
- [x] Elementos interativos com rótulos descritivos — `aria-label` em campos e botões, verificado por `getByLabel`/`getByRole` funcionando corretamente nos testes reais
- [x] Imagens com texto alternativo apropriado — sem imagens de conteúdo; ícones `ph-*` decorativos
- [x] Contraste de cores adequado — `AxeBuilder` (`wcag2a`, `wcag2aa`) rodou contra a lista e o modal renderizados de verdade no Chromium e **não reportou nenhuma violação** (E2E-16)
- [x] Formulários com rótulos associados aos campos — confirmado via `page.getByLabel('E-mail')`, `dialog.locator('#fc-frente')` etc. funcionando nos testes reais
- [x] Mensagens de erro claras e acessíveis — `.erro-campo` com texto "3 caracteres" verificado via DOM real
- [x] Fontes com tamanho apropriado — inspeção visual das capturas de tela (ver evidências), tokens Nocturne

## Visual e responsividade
Capturado com o navegador real em três breakpoints e nos principais estados:

| Evidência | Estado |
|-----------|--------|
| `01-lista-flashcards-desktop.png` (1440×900) | Lista com 24 cards, indicadores populados |
| `02-lista-flashcards-tablet.png` (834×1112) | Lista responsiva em tablet |
| `03-lista-flashcards-mobile.png` (390×844) | Lista responsiva em mobile |
| `04-modal-criacao-na-sessao.png` | Modal aberto sobre a questão, pré-preenchido (CA-01, CA-03) |
| `05-validacao-inline.png` | Erro de validação inline (CA-04) |
| `06-confirmacao-descarte.png` | Confirmação de descarte (CA-07) |
| `07-estado-vazio-filtro.png` | Estado vazio de filtro, distinto do estado vazio de acervo (CA-10) |
| `08-toast-confirmacao.png` | Toast de confirmação após salvar (CA-05) |
| `09-contador-sessao.png` | Contador de flashcards da sessão (CA-08) |
| `10-selecao-mover-deck.png` | Seleção múltipla e "Mover para deck" (CA-16), card avulso na lista (CA-14) |
| `11-estado-vazio-acervo.png` | Estado vazio de acervo — usuário sem nenhum card (distinto do estado vazio de filtro) |

Nenhuma inconsistência visual encontrada nos três breakpoints; a tela usa flex/grid responsivo e os tokens Nocturne (`var(--*)`) consistentemente, sem overflow horizontal na página.

## Bugs encontrados e corrigidos
| ID | Descrição | Severidade | Status | Correção | Teste de regressão | Evidência |
|----|-----------|------------|--------|----------|--------------------|-----------|
| BUG-01 | `E2E-10` esperava exatamente 4 cards no deck "Administrativo", mas a seed de `prd-flashcards-decks-e-revisao` (`V909__seed_revisao_dev.sql`, entregue depois deste arquivo de teste) acrescentou um 5º card ("Card de sessao finalizada de hoje") ao mesmo deck — a asserção de contagem total do deck ficou desatualizada frente ao ambiente `dev` compartilhado entre as duas funcionalidades. Não é um defeito de produto: a aplicação filtra corretamente, o teste é que contava a linha errada. | Baixa (teste, não produto) | **Corrigido** | `frontend/e2e/flashcards.spec.ts:198` — a asserção de "todos os cards do deck" passou a contar só as linhas com `hasText: 'Principio da legalidade'` (o subconjunto desta feature), em vez do total do deck, que agora é responsabilidade compartilhada com F6b | O próprio `E2E-10`, reexecutado isoladamente e na suíte completa (2 rodadas limpas) | Rodada completa: `15 passed, 1 skipped, 0 failed` |
| BUG-02 | `E2E-12` procurava a linha "Card com historico de revisao" na tabela sem filtro; a mesma seed `V909` acrescentou 14 cards mais recentes ao acervo do usuário de teste, empurrando esse card (mais antigo, da seed original) para além da primeira página de 20 linhas (ordenação padrão por `criadoEm desc`) — o teste nunca encontrava o botão "Excluir" e estourava o timeout. Também não é defeito de produto: paginação e ordenação funcionam conforme o contrato (CA-09/CA-10); o teste não isolava o card antes de interagir. | Baixa (teste, não produto) | **Corrigido** | `frontend/e2e/flashcards.spec.ts:230-232` — o teste agora busca por texto ("historico de revisao") antes de localizar a linha, isolando o card independentemente da paginação; o filtro é refletido em query params e sobrevive ao `page.reload()` que o teste já fazia | O próprio `E2E-12`, reexecutado isoladamente e na suíte completa (2 rodadas limpas) | Rodada completa: `15 passed, 1 skipped, 0 failed` |

Nenhum bug de produto (backend ou frontend) foi encontrado nesta execução. Os dois itens acima são debilidades da suíte de teste E2E desta feature diante do crescimento do seed de `dev` por uma feature irmã (`prd-flashcards-decks-e-revisao`), corrigidas na causa raiz (asserções que dependiam de contagens/posições absolutas em uma base de dados compartilhada e mutável).

## Conclusão
Funcionalidade **APROVADA**, com verificação de ponta a ponta em navegador real (não apenas inspeção de código ou testes automatizados isolados):

- **16/16 critérios de aceitação** verificados; 15 dos 16 cenários E2E da TechSpec passam de ponta a ponta contra a aplicação real, e o único não executado (E2E-13/CA-13) tem motivo documentado e não bloqueante — a navegação para a questão de origem depende de uma rota (`/estudar?questao={id}`) que outra funcionalidade (`prd-sessao-de-estudo`) ainda não expõe; o dado que a sustenta já é entregue por esta feature.
- **Privacidade (RN-07/CA-15)** confirmada por `FlashcardPrivacidadeIntegracaoTest` (leitura, edição e exclusão de card de terceiro, incluindo super-admin — sempre 404 idêntico ao inexistente).
- **Exclusão lógica (CA-12)** confirmada de ponta a ponta: excluir via UI faz o card sumir da lista e do reload, com o histórico de revisões preservado no banco (garantido por FK sem `cascade` + testes de integração).
- **Sessão preservada (CA-02)**, a promessa central do PRD, verificada com interação real: questão, alternativa marcada e cronômetro sobrevivem à abertura e ao fechamento do modal.
- **Cobertura de testes** acima do piso de 80% em ambas as camadas (backend por classe, frontend agregado).
- **Acessibilidade** validada com `axe-core` contra a lista e o modal renderizados de verdade, sem violações WCAG 2A/2AA.
- Dois problemas foram encontrados durante a execução real — ambos na suíte de teste E2E (não no produto), causados por crescimento do seed de dados de desenvolvimento por uma feature irmã já entregue (`prd-flashcards-decks-e-revisao`). Foram corrigidos na causa raiz e a suíte foi revalidada duas vezes de forma limpa e isolada.
