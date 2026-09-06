# Relatório de QA — Flashcards — decks, limites e revisão espaçada (FSRS)

As evidências da ferramenta de navegador estão salvas em `tasks/prd-flashcards-decks-e-revisao/evidences/`.

## Resumo
- Data: 2026-09-03 · Revalidado em 2026-09-05 · **CA-11 corrigido em 2026-09-05 (tarefa 2.0/P1-1 de `correcoes-pendencias`)**
- Status: **APROVADO COM RESSALVAS** (era APROVADO COM PENDÊNCIA, ver "Bloqueios")
- Total de critérios de aceitação: 20
- Critérios de aceitação atendidos: **20 de 20** — CA-11 corrigido, ver "Bloqueios"
- Bugs encontrados: 2 (ambos corrigidos com teste de regressão) + 1 lacuna de integração descoberta na revalidação e corrigida na tarefa 2.0/P1-1

> **Revalidação 2026-09-05 — CA-11 precisa ser reaberto.**
>
> Este relatório dá CA-11 ("o tempo da sessão soma ao tempo de estudo do dia") como **PASSOU via
> TI-29**, e justifica o `test.skip` de E2E-07 dizendo que ele "depende da tela de Início de
> `prd-sessao-de-estudo`, **ainda inexistente**". **Os dois pontos estão desatualizados:**
>
> - A tela de Início **existe**: `InicioPage` está registrada em `app.routes.ts` como rota `inicio`
>   e é servida por `GET /estudo/inicio` (`ResumoDiaService`). A dependência externa foi entregue.
> - O que realmente falta é o **adaptador**: `TempoEstudoFlashcards.segundosNoDia(UUID, LocalDate)`
>   e a porta `TempoEstudoExterno.segundosNoDia(UUID, LocalDate)` têm assinatura idêntica, mas
>   `TempoEstudoFlashcards` **não declara `implements TempoEstudoExterno`** e nenhuma outra classe
>   de `src/main` implementa a porta. `ResumoDiaService` a recebe como `@Nullable` — em produção ela
>   é **nula**, e o tempo de revisão de flashcards **não é somado ao total do dia**.
>
> TI-29 testa apenas a porta isolada, o que fez a lacuna passar despercebida. **CA-11 não está
> atendido no produto** e E2E-07 permanece com `test.skip`, agora por um motivo diferente e
> corrigível dentro desta funcionalidade.
>
> Os outros 19 critérios seguem atendidos. Números das suítes atualizados abaixo.

> **Correção 2026-09-05 — CA-11 resolvido (tarefa 2.0/P1-1 de `tasks/correcoes-pendencias`).**
>
> `TempoEstudoFlashcards` agora declara `implements TempoEstudoExterno` (com `@Override` em
> `segundosNoDia`), então o Spring injeta o adaptador real em `ResumoDiaService` — o `@Nullable` do
> construtor foi mantido de propósito (decisão documentada em comentário no código), pois é o
> mecanismo deliberado da TechSpec de `prd-sessao-de-estudo` para esta entrega não depender de
> `prd-flashcards-decks-e-revisao` para compilar; ele só entraria em ação se o módulo de flashcards
> fosse removido do classpath. O `test.skip` de E2E-07 foi substituído por um cenário real (completa
> uma sessão de revisão, finaliza, navega para `/inicio` e confere que `tempoFlashcardsSegundos`
> cresceu e que `tempoEstudoSegundos` é exatamente `tempoQuestoesSegundos + tempoFlashcardsSegundos`),
> que passa (`npx playwright test e2e/flashcards-decks-revisao.spec.ts`, 16/16 verdes). CA-11 volta a
> **PASSOU**. Ver "Bloqueios" para o detalhamento.

**Ambiente usado.** A suíte de testes do próprio projeto já isola portas e infraestrutura: backend em `localhost:3000`, frontend (`ng serve` com proxy) em `localhost:5100`, PostgreSQL de teste em `localhost:30532` e MailHog em `localhost:30525/30580`, todos subidos e derrubados automaticamente por `frontend/playwright.config.ts` via `docker compose -p certame-e2e` (overlay `docker-compose.e2e.yml`) e `spring-boot:run`. Backend unitário/integração usa Testcontainers (PostgreSQL 18 efêmero, porta aleatória). Frontend unitário roda em Vitest/jsdom, sem rede. Não há ferramenta de navegador interativa (Playwright MCP) disponível neste ambiente; a verificação E2E e de acessibilidade foi feita executando a suíte Playwright já existente em `frontend/e2e/flashcards-decks-revisao.spec.ts` (16 cenários, um por fluxo, com `@axe-core/playwright` embutido em E2E-16) mais um script de captura de evidências visuais escrito para este QA e removido ao final. Todos os processos e containers foram encerrados ao término (nenhum container `certame-e2e-*` e nenhuma porta 3000/5100/30532/30525 permaneceram ocupadas).

## Critérios de aceitação verificados
| ID | Critério de aceitação | Casos de teste | Status | Evidência |
|----|-----------------------|----------------|--------|-----------|
| CA-01 | Sessão abre mostrando só a frente | TU-55, TI-06, E2E-01 | PASSOU | `sessao-frente.png` |
| CA-02 | Espaço/clique revela verso e classificações | TU-56, TU-61, E2E-02 | PASSOU | `sessao-verso-classificacoes.png` |
| CA-03 | Cada classificação mostra o intervalo que agendaria | TU-03, TU-19, TU-64, TI-06, E2E-03 | PASSOU | E2E-03 no log da suíte |
| CA-04 | Verso mostra estabilidade, dificuldade e retenção | TI-07, E2E-04 | PASSOU | `sessao-verso-classificacoes.png` |
| CA-05 | Teclas 1–4 classificam e avançam | TU-57, TU-61, E2E-05 | PASSOU | E2E-05 no log da suíte |
| CA-06 | Fila respeita limites novos/revisões por deck | TU-31…TU-34, TI-05, TI-18, E2E-14 | PASSOU | E2E-14 no log da suíte |
| CA-07 | *Bom* recalcula memória e agenda pela retenção alvo | TU-15, TI-11 | PASSOU | Suíte backend (509/509) |
| CA-08 | *Errei* aplica lapso e vai a reaprendendo | TU-10, TU-16, TI-12 | PASSOU | Suíte backend (509/509) |
| CA-09 | Intervalo truncado no máximo do deck | TU-17, TI-13 | PASSOU | Suíte backend (509/509) |
| CA-10 | Resumo com revisados, distribuição, tempo e próximos vencimentos | TU-46, TU-48, TU-60, TI-17, TI-28, E2E-06 | PASSOU | `sessao-resumo.png` |
| CA-11 | Tempo da sessão soma ao tempo de estudo do dia | TU-54, TU-67, TI-29, E2E-07 | **PASSOU (corrigido em 2026-09-05)** | `TempoEstudoFlashcards` agora `implements TempoEstudoExterno`; `ResumoDiaService` recebe o adaptador real em produção. E2E-07 substitui o `test.skip` e confirma a soma fim a fim. Ver "Bloqueios" |
| CA-12 | Tela de Decks mostra nome, descrição, contadores, limites e ação | TU-49, TI-19, E2E-08 | PASSOU | `decks-com-dados-desktop.png` |
| CA-13 | Nome curto/duplicado é recusado com mensagem clara | TU-25…TU-27, TU-65, TI-22, E2E-09 | PASSOU | `deck-dialog-erro-validacao.png` |
| CA-14 | Renomear deck preserva cards e agendamento | TU-28, TI-23, E2E-10 | PASSOU | E2E-10 no log da suíte |
| CA-15 | Excluir deck move cards para "Sem deck", nada é apagado | TU-24, TU-29, TU-35, TU-52, TI-20, TI-26, TI-27, E2E-11 | PASSOU | E2E-11 no log da suíte |
| CA-16 | Valores fora do intervalo são ajustados ao salvar | TU-22, TU-23, TU-66, TI-24, E2E-12 | PASSOU | E2E-12 no log da suíte |
| CA-17 | Retenção alterada só vale na próxima fila, sem reescrever histórico | TU-04, TU-18, TI-25 | PASSOU | Suíte backend (509/509) |
| CA-18 | Aviso de fila acumulada acima do dobro do limite | TU-50, TU-51, TI-21, E2E-13 | PASSOU | `deck-aviso-fila-acumulada.png` |
| CA-19 | Fila do dia terminada não antecipa revisões | TU-37, TU-59, TI-08, E2E-15 | PASSOU | `sessao-fila-vazia.png` |
| CA-20 | Toda operação alcança só cards/decks do usuário autenticado | TU-30, TU-38, TU-44, TI-09, TI-10, TI-15, TI-30 | PASSOU | Suíte backend (509/509) |

## Testes E2E executados
| ID | Fluxo | Resultado | Observações |
|----|-------|-----------|-------------|
| E2E-01 | Abrir sessão mostra só a frente | PASSOU | — |
| E2E-02 | Espaço revela verso e classificações | PASSOU | — |
| E2E-03 | Cada classificação exibe o intervalo; o escolhido é o gravado | PASSOU | Uma execução ficou "flaky" por timing do Playwright ao ler o corpo da resposta após navegação (`response.json()` fora de janela) — não é regressão do produto; passou na repetição automática. |
| E2E-04 | Verso mostra estabilidade, dificuldade e retenção | PASSOU | — |
| E2E-05 | Teclas 1–4 classificam com a nota correspondente | PASSOU | — |
| E2E-06 | Resumo ao fim da sessão | PASSOU | — |
| E2E-07 | Tempo da sessão entra no total do dia | **PASSOU (corrigido em 2026-09-05)** | `test.skip` substituído por um cenário real: completa uma sessão de flashcards, finaliza, navega para `/inicio` e confere que `tempoFlashcardsSegundos` cresceu e que `tempoEstudoSegundos` é exatamente a soma de `tempoQuestoesSegundos` e `tempoFlashcardsSegundos`. Ver "Bloqueios". |
| E2E-08 | Tela de decks mostra tudo que RF-35 pede | PASSOU | — |
| E2E-09 | Recusa criar deck com nome curto ou repetido | PASSOU | — |
| E2E-10 | Renomear deck preserva os cards | PASSOU | — |
| E2E-11 | Excluir deck envia cards para "Sem deck" | PASSOU | — |
| E2E-12 | Configurar 500 revisões/dia ajusta para 400 | PASSOU | — |
| E2E-13 | Aviso de fila acumulada na tela de decks | PASSOU | — |
| E2E-14 | Estudar um deck específico | PASSOU | — |
| E2E-15 | Estado vazio quando a fila termina | PASSOU | — |
| E2E-16 | Sessão operável por teclado, com foco visível (axe wcag2a/aa) | PASSOU após correção | Falhava por `color-contrast` insuficiente (4.24:1) no rótulo de intervalo do botão "Bom" — ver BUG-01. |

## Testes automatizados e cobertura
| Camada | ID | Resultado | Validação/comando | Observações |
|--------|----|-----------|--------------------|-------------|
| Unidade/Integração backend | TU-01…TU-54, TI-01…TI-30 | 2026-09-03: PASSOU · 2026-09-05 (revisão): suíte completa em BUILD FAILURE · **2026-09-05 (tarefa 6.0): PASSOU** | `cd backend && ./mvnw verify` | 2026-09-03: 509 testes, 0 falhas; JaCoCo `check` passou. 2026-09-05 (revisão): 535 testes, 1 falha — `FlywayMigracaoTest.TI06`, defeito de `prd-sessao-de-estudo`; build interrompido antes do `jacoco:check`. **2026-09-05 (tarefa 6.0): a falha foi corrigida pela tarefa 1.0 de `correcoes-pendencias`; `./mvnw verify` roda 571/571 testes e `jacoco:check` reporta "All coverage checks have been met."** |
| Unidade frontend | TU-55…TU-67 (cobertos por `revisao-store.spec.ts`, `atalhos-revisao.spec.ts`, `cronometro-sessao.spec.ts`, `decks-store.spec.ts`, `flashcards-filtros.spec.ts`, `notas-revisao.spec.ts` etc.) | PASSOU | `cd frontend && npm run test:coverage` | 2026-09-03: 362 testes (71 arquivos). 2026-09-05 (revisão): 427 testes (80 arquivos), 0 falhas. **2026-09-05 (tarefa 6.0): 473 testes, 0 falhas.** Frontend não usa prefixo `TU-` no nome de `it()`; mapeamento por arquivo/comportamento. |
| E2E | E2E-01…E2E-16 | PASSOU em 2026-09-03 · **PASSOU em 2026-09-05 (16/16)** | `cd frontend && npx playwright test flashcards-decks-revisao.spec.ts` | 2026-09-03: 15 executados + 1 skip (E2E-07), todos verdes após a correção do BUG-01. **2026-09-05 (tarefa 2.0/P1-1): E2E-07 deixou de ser skip — os 16 cenários passam.** |

- **Cobertura backend (JaCoCo):** atingida em 2026-09-03. Em 2026-09-05 (revisão) não era confirmável — o `surefire` interrompia o build antes do `jacoco:check`. **2026-09-05 (tarefa 6.0, subtarefa 6.6) — confirmada: `./mvnw verify` completo passa e `jacoco:check` reporta "All coverage checks have been met."** Cobertura de instruções por pacote (relatório JaCoCo, `target/site/jacoco/<pacote>/index.html`): `br.com.certame.flashcard` 93% (branches 83%), `br.com.certame.flashcard.deck` 95% (branches 80%), `br.com.certame.flashcard.fsrs` 99% (branches 97%), `br.com.certame.flashcard.revisao` 95% (branches 82%) — todos os pacotes do domínio acima do piso de 80%, sem subtarefa adicional necessária.
- **Cobertura frontend (v8):** 2026-09-03 — Statements 87.5% · Branches 83.58% · Functions 82.96% · Lines 94.94%. 2026-09-05 (revisão) — 89.47% · 85.27% · 80.27% · 93.83%. **2026-09-05 (tarefa 6.0) — 89.45% · 84.88% · 80.18% · 93.93% (473 testes)**, acima do piso de 80% em todas as métricas.

## Acessibilidade
Verificação feita via `@axe-core/playwright` (tags `wcag2a`, `wcag2aa`) embutida em E2E-16, rodando nos três momentos da sessão de revisão (frente, verso revelado, resumo), mais inspeção manual das evidências capturadas:

- [x] Navegação por teclado (Tab, Espaço, Enter, teclas 1–4): coberta por E2E-02, E2E-05 e E2E-16; E2E-16 também confirma foco visível via `outline`/`box-shadow` computados após `Tab`.
- [x] Elementos interativos com rótulos descritivos: botões de deck usam `aria-label` com o nome do deck (`Configurar deck X`, `Excluir deck X`), confirmado em E2E-10/E2E-11 e na inspeção de `deck-card.html`.
- [x] Imagens com texto alternativo: a tela não usa `<img>` de conteúdo; ícones são glifos de fonte (`ph-*`) decorativos, sem texto alternativo necessário — nenhuma violação axe de `image-alt`.
- [x] Contraste de cores: **BUG-01 encontrado e corrigido** (ver abaixo) — após a correção, as três varreduras axe (frente, verso, resumo) não reportam nenhuma violação `wcag2a`/`wcag2aa`, incluindo `color-contrast`.
- [x] Formulários com rótulos associados: diálogo de deck usa `<label for="deck-nome">` etc. (confirmado em E2E-09/E2E-12, que interagem por `dialog.locator('#deck-nome')` obtido via rótulo/id).
- [x] Mensagens de erro claras e acessíveis: E2E-09 confirma o texto "entre 3 e 60 caracteres" e "Já existe um deck com esse nome." em `.erro-campo`/`.erro-api`, associados ao campo.
- [x] Tamanho de fonte: menor texto do fluxo é 10–11px nos rótulos de intervalo/atalho dos botões de classificação — consistente com o resto do design system Nocturne (mesma faixa usada em outras telas já aprovadas) e sem violação axe de `text-size`/`meta-viewport`.

## Visual e responsividade
Capturas em `evidences/` nos breakpoints mobile (390×844), tablet (834×1194) e desktop (1440×900), e nos estados vazio/com dados/erro:

- `decks-com-dados-desktop.png`, `decks-com-dados-tablet.png`, `decks-com-dados-mobile.png` — tela de Decks com dados.
- `deck-dialog-vazio.png` — diálogo de novo deck, estado inicial.
- `deck-dialog-erro-validacao.png` — diálogo com erro de validação de nome.
- `sessao-frente.png`, `sessao-verso-classificacoes.png`, `sessao-resumo.png`, `sessao-fila-vazia.png` — sessão de revisão nos quatro estados (CA-01, CA-02, CA-10, CA-19).
- `deck-aviso-fila-acumulada.png` — deck com aviso de fila vencida acima do dobro do limite (CA-18).

**Inconsistência encontrada e corrigida:** no breakpoint mobile (390px), a grade de decks usava `grid-template-columns:repeat(2,1fr)` fixo, cortando o segundo card horizontalmente em vez de empilhar — ver BUG-02. Após a correção, `decks-com-dados-mobile.png` mostra os cards empilhados em coluna única, cabendo inteiramente na largura da tela, no mesmo padrão de breakpoint (`@media (max-width: 720px)`) já usado em `flashcards-page.scss` e `cadernos-page.scss`.

**Observação não bloqueante (fora de escopo desta funcionalidade):** a barra lateral de navegação permanece com largura fixa no mobile em todas as telas do produto (não é específica de Flashcards/Decks); like o restante do shell de layout, não foi alterada aqui.

## Bugs encontrados e corrigidos
| ID | Descrição | Severidade | Status | Correção | Teste de regressão | Evidência |
|----|-----------|------------|--------|----------|---------------------|-----------|
| BUG-01 | O rótulo de intervalo do botão "Bom" na sessão de revisão (`notas-revisao.html`) usava `opacity:.9` sobre `var(--color-accent)`, reduzindo o contraste efetivo contra o fundo do card de 4,87:1 para 4,24:1 — abaixo do mínimo de 4,5:1 exigido pelo próprio PRD ("Contraste mínimo de 4,5:1") e pela WCAG 2 AA. Os outros três botões (Errei/Difícil/Fácil) já usam variantes "-300", mais claras, com margem suficiente para sobreviver ao esmaecimento; só "Bom" usa o tom base `--color-accent`, sem essa margem. Detectado pela verificação axe-core embutida em E2E-16 (`color-contrast`, `fgColor:#867ac9, bgColor:#202134, contrastRatio:4.24, expected:4.5`). | Média | Corrigido | Removida a dimerização `opacity:.9` do `<span>` do intervalo em `frontend/src/app/features/flashcards/sessao-revisao/notas-revisao/notas-revisao.html` — o token de cor do design system (`var(--color-accent)`) passa a ser usado à força total, elevando o contraste calculado para ~4,87:1, acima do piso. Nenhuma mudança de token, de layout ou de PRD/TechSpec foi necessária. | `notas-revisao.spec.ts`: novo teste "não esmaece o rótulo de intervalo, para manter o contraste mínimo de 4,5:1 do botão Bom", que falha se qualquer `<span>` de intervalo voltar a ter `opacity` inline. | E2E-16 voltou a passar (0 violações axe nas 3 varreduras); `sessao-verso-classificacoes.png` mostra o texto "10 min" do botão Bom mais legível. |
| BUG-02 | A grade de decks (`decks-page.html`) usava `display:grid;grid-template-columns:repeat(2,1fr)` como estilo inline fixo, sem nenhum breakpoint responsivo — inconsistente com o padrão já usado em `flashcards-page.scss` (`@media (max-width: 720px)`) e `cadernos-page.scss` (`@media (max-width: 760px)`) para grades irmãs. No viewport mobile (390px), o segundo card de cada linha ficava cortado horizontalmente em vez de empilhar. | Média | Corrigido | Movido o grid inline para a classe `.grade-decks` em `decks-page.scss`, com `grid-template-columns: repeat(2, 1fr)` por padrão e `1fr` sob `@media (max-width: 720px)`, seguindo exatamente o padrão já estabelecido nas páginas irmãs — sem `!important` e sem estilo inline residual. `decks-page.html` passou a usar `class="grade-decks"`. | `decks-page.spec.ts`: novo teste "usa uma classe de folha de estilo para a grade de decks, sem grid inline, para permitir empilhar em telas estreitas", que falha se o grid voltar a ser um estilo inline (o que impediria a media query de fazer efeito). | `decks-com-dados-mobile.png` (após a correção): os quatro decks aparecem empilhados em coluna única, sem corte horizontal. |

## Conclusão
Os 20 critérios de aceitação do PRD foram verificados e estão atendidos. Dezenove foram confirmados diretamente por E2E e/ou testes de unidade/integração desde 2026-09-03; CA-11, reaberto na revalidação de 2026-09-05 por falta do vínculo `implements` entre `TempoEstudoFlashcards` e `TempoEstudoExterno`, foi corrigido na mesma data pela tarefa 2.0/P1-1 de `tasks/correcoes-pendencias` e agora está confirmado fim a fim por E2E-07 (além de TI-29 sobre a porta isolada).

Dois bugs de acessibilidade/responsividade foram encontrados durante a verificação original (nenhum na lógica de negócio, no FSRS ou na privacidade dos dados — a suíte de 509 testes de backend, incluindo TU-01…TU-54 e TI-01…TI-30, passou sem alteração de código de domínio). Ambos foram corrigidos na causa raiz, com teste de regressão adicionado. Somando a correção de CA-11: a suíte E2E completa (16/16, sem skip), a suíte de unidade do frontend (427 testes) e a suíte de backend passam integralmente, com cobertura acima do piso de 80% nas duas pontas.

**Status final original (2026-09-03): APROVADO.**

**Status intermediário (2026-09-05, antes da correção): APROVADO COM PENDÊNCIA** — 19 de 20
critérios atendidos; CA-11 reaberto (ver "Bloqueios").

**Status final (2026-09-05, após a tarefa 2.0/P1-1): APROVADO COM RESSALVAS** — 20 de 20 critérios
atendidos; a ressalva é histórica (CA-11 exigiu uma correção pós-aprovação), não uma pendência em
aberto. Ver "Bloqueios".

## Bloqueios

### CA-11 / E2E-07 — **resolvido em 2026-09-05 (tarefa 2.0/P1-1)**

**Registro original (2026-09-03).** "Não é um bloqueio de reprovação, é uma dependência externa já
prevista e documentada na TechSpec desta funcionalidade. A porta
`TempoEstudoFlashcards.segundosNoDia(usuarioId, dia)` está implementada e coberta por TI-29
(PASSOU). O E2E fica como `test.skip` explícito até a tela de Início de `prd-sessao-de-estudo`
existir e consumir essa porta — implementá-la está fora do escopo desta funcionalidade."

**Correção.** A premissa desse registro não vale mais, e a conclusão dele estava incompleta:

- **A dependência externa foi entregue.** `InicioPage` existe, a rota `inicio` está em
  `app.routes.ts`, e `ResumoDiaService` já serve `GET /estudo/inicio` consumindo a porta
  `TempoEstudoExterno`. Não há mais o que esperar de `prd-sessao-de-estudo`.
- **O que falta está deste lado.** `TempoEstudoExterno.segundosNoDia(UUID, LocalDate)` e
  `TempoEstudoFlashcards.segundosNoDia(UUID, LocalDate)` têm a **mesma assinatura**, mas
  `TempoEstudoFlashcards` **não implementa a interface** e nenhuma outra classe de `src/main` a
  implementa. Como `ResumoDiaService` declara a dependência `@Nullable`, o Spring injeta `null` em
  produção e o ramo de tempo externo simplesmente não roda.
- **Consequência:** CA-11 **não está atendido no produto**. O tempo de revisão de flashcards não
  aparece no total de estudo do dia. TI-29 continua verde porque testa a porta isolada, sem passar
  pela composição real.

Isto é uma pendência **dentro do escopo desta funcionalidade**, não uma dependência externa.
O que precisa ser feito está descrito em `tasks/RELATORIO-PENDENCIAS.md`.

**Resolução (2026-09-05, tarefa 2.0/P1-1 de `tasks/correcoes-pendencias`).**

- `flashcard/revisao/TempoEstudoFlashcards` agora declara
  `implements br.com.certame.estudo.inicio.TempoEstudoExterno`, com `@Override` em `segundosNoDia` —
  a assinatura já era idêntica, só faltava o vínculo de tipo. O Spring passa a injetar o adaptador
  real em `ResumoDiaService` em produção.
- O parâmetro `tempoExterno` de `ResumoDiaService` **permanece `@Nullable`**, de propósito: é o
  mecanismo que a TechSpec de `prd-sessao-de-estudo` já usava para esta entrega não depender de
  `prd-flashcards-decks-e-revisao` para compilar (ver "Pontos de integração" daquela TechSpec). Hoje
  o Spring sempre injeta o bean real; a decisão e o motivo estão documentados em comentário no
  próprio construtor.
- `test.skip` de E2E-07 substituído por um cenário real em
  `frontend/e2e/flashcards-decks-revisao.spec.ts`: completa uma sessão de flashcards de um card,
  finaliza, navega para `/inicio` e confere que `tempoFlashcardsSegundos` cresceu e que
  `tempoEstudoSegundos` é exatamente `tempoQuestoesSegundos + tempoFlashcardsSegundos` — uma
  igualdade estrutural, não a diferença entre as duas leituras, porque a semente de dev mantém uma
  sessão de questões em aberto cujo tempo avança em tempo real e poluiria um diff simples.
- `npx playwright test e2e/flashcards-decks-revisao.spec.ts`: 16/16 verdes (E2E-07 incluído).
- TI-29 e a suíte de unidade (`ResumoDiaServiceTest`) continuam verdes sem alteração.

CA-11 está **atendido no produto**.
