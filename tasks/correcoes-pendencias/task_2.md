# Tarefa 2.0 (P1-1): CA-11 de flashcards — ligar `TempoEstudoFlashcards` à porta `TempoEstudoExterno`

## Visão geral

`RF-06`/`CA-11` exige que o tempo das sessões de revisão de flashcards some ao tempo de estudo
exibido no Início. Existem as duas pontas: `br.com.certame.estudo.inicio.TempoEstudoExterno` (porta
consumida por `ResumoDiaService`) e `br.com.certame.flashcard.revisao.TempoEstudoFlashcards` (que
calcula exatamente esse valor, com a **mesma assinatura de método** `segundosNoDia(UUID, LocalDate)`
retornando `int`). **Falta o vínculo entre elas:** a classe de flashcards não declara implementar a
porta, e nenhuma outra classe de `src/main` a implementa. Como `ResumoDiaService` recebe a
dependência como `@Nullable`, em produção ela é injetada como `null` e o ramo simplesmente não roda
(`ResumoDiaService.doDia`: `tempoExterno == null ? 0 : …`).

`TI-29` testa a porta isolada e passa. O E2E que provaria a integração (`E2E-07` em
`e2e/flashcards-decks-revisao.spec.ts:193`) está com `test.skip` sob a justificativa de que "a tela
de Início de `prd-sessao-de-estudo` ainda não existe" — **essa tela existe** (`InicioPage`, rota
`inicio`, `GET /estudo/inicio`). A justificativa do skip ficou obsoleta e escondeu a lacuna real.

<rules>
### Conformidade com o AGENTS.md e as rules

- `.agents/rules/code-standards.md` §3 "Injeção e nulidade" — dependência de produção não deveria
  precisar de `@Nullable` quando existe implementação real; usar isso para decidir se o `@Nullable`
  em `ResumoDiaService` continua justificado.
- `.agents/rules/tests.md` — um `test.skip` cujo motivo não é mais verdadeiro é pior que teste
  ausente (seção 8, checklist antes do PR): esta tarefa remove o skip, não só o código.
</rules>

<requirements>
- RF-06 / CA-11 de `prd-flashcards-decks-e-revisao`: "Dada uma sessão de flashcards concluída, quando
  abro o Início, então o tempo dessa sessão está somado ao tempo de estudo do dia (RF-06)."
</requirements>

## Subtarefas

- [x] 2.1 Fazer `flashcard/revisao/TempoEstudoFlashcards` declarar
  `implements br.com.certame.estudo.inicio.TempoEstudoExterno` e anotar `segundosNoDia` com
  `@Override`. A assinatura já é idêntica — é só o vínculo de tipo que falta.

  Feito: `TempoEstudoFlashcards implements TempoEstudoExterno`, `segundosNoDia` com `@Override`.
- [x] 2.2 Reavaliar `estudo/inicio/ResumoDiaService`: com um único implementador real de
  `TempoEstudoExterno` em `src/main`, decidir se o parâmetro do construtor continua `@Nullable` (por
  exemplo, para não quebrar testes de slice que sobem o contexto sem o módulo de flashcard) ou se
  passa a ser obrigatório. Documentar a decisão em comentário/TechSpec se `@Nullable` for mantido.

  Decisão: `@Nullable` mantido. É o mecanismo deliberado da TechSpec de `prd-sessao-de-estudo`
  ("Pontos de integração") para esta entrega não depender de `prd-flashcards-decks-e-revisao` para
  compilar — não uma lacuna acidental. Nenhum teste de slice do projeto sobe o contexto Spring sem o
  módulo de flashcard (todos os `@SpringBootTest` escaneiam `br.com.certame` inteiro a partir de
  `CertameApplication`), então a decisão não é motivada por risco de quebrar testes, e sim por manter
  o desacoplamento de compilação já registrado. Decisão documentada em comentário no construtor de
  `ResumoDiaService`.
- [x] 2.3 Remover o `test.skip` de `e2e/flashcards-decks-revisao.spec.ts:193` (E2E-07) e escrever o
  cenário real: completar uma sessão de revisão de flashcards, finalizar, navegar para `/inicio` e
  verificar que o tempo da sessão está somado ao tempo total de estudo do dia exibido.

  Feito: E2E-07 completa uma sessão de um card, finaliza, navega para `/inicio` via `page.route`
  (para ler o corpo da resposta com segurança) e confere que `tempoFlashcardsSegundos` cresceu e que
  `tempoEstudoSegundos === tempoQuestoesSegundos + tempoFlashcardsSegundos` — igualdade estrutural em
  vez de diff simples, porque a semente de dev mantém uma sessão de questões em aberto cujo tempo
  avança em tempo real.
- [x] 2.4 Rodar `npx playwright test e2e/flashcards-decks-revisao.spec.ts` e confirmar que passa.

  `npx playwright test e2e/flashcards-decks-revisao.spec.ts`: 16 passed (E2E-07 incluído, sem skip).
- [x] 2.5 Atualizar `tasks/prd-flashcards-decks-e-revisao/qa.md`: reverter CA-11 de "não atendido"
  para "atendido", e remover o rebaixamento do veredito da funcionalidade (de "aprovado com
  pendência" para "aprovado com ressalvas", conforme o restante do domínio).

  `qa.md` atualizado: CA-11 e E2E-07 voltam a "PASSOU", seção "Bloqueios" ganhou o registro da
  resolução, e o veredito final passou de "APROVADO COM PENDÊNCIA" para "APROVADO COM RESSALVAS"
  (20/20 critérios), preservando o histórico da revalidação anterior.

## Detalhes de implementação

Não é necessário criar uma classe de adaptador nova: `TempoEstudoFlashcards` já é um `@Service` do
Spring com a assinatura correta, então declarar a interface é suficiente para o Spring injetá-la
onde `TempoEstudoExterno` é requisitado — é exatamente o padrão de porta/adaptador que o restante do
código já usa (ver como outras portas de `estudo/inicio` são implementadas, se houver mais de uma).

## Critérios de aceitação relacionados

- CA-11 (`prd-flashcards-decks-e-revisao`, US5, RF-34)

## Testes da tarefa

- [x] `TI-29` (já existe, deve continuar passando) — `RevisaoControllerIntegracaoTest`: 17/17.
- [x] `E2E-07` em `e2e/flashcards-decks-revisao.spec.ts` (novo, substitui o skip) — passa; suíte
  completa 16/16.

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/flashcard/revisao/TempoEstudoFlashcards.java` (modificado)
- `backend/src/main/java/br/com/certame/estudo/inicio/ResumoDiaService.java` (revisado)
- `frontend/e2e/flashcards-decks-revisao.spec.ts` (modificado)
- `tasks/prd-flashcards-decks-e-revisao/qa.md` (atualizado)
