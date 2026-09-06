# Tarefa 7.0: Frontend do estudante (lista em três grupos, destaque, resultado)

## Visão geral

Traduz 1:1 o artboard `simulados` do protótipo Nocturne: a lista do estudante com os três agrupamentos (disponíveis, agendados, concluídos), o card de destaque do próximo simulado, e a tela de resultado com nota, contagens, tempo total e revisão questão a questão. O botão Iniciar abre o Modo Prova já existente em `features/study`, sem nenhuma tela de resolução própria. Consome os endpoints da Tarefa 5.0.

<skills>
### Conformidade com skills

- `angular-developer` — standalone, zoneless, `signal`/`computed`, `input()`/`output()` como funções, `templateUrl`/`styleUrl` separados, cliente HTTP concentrado em `core/api`, membros de template como `protected readonly`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Leitura de `AGENTS.md` e das rules confirmada. Regra 1 (tradução 1:1 do protótipo, tokens `var(--*)`, sem biblioteca de UI de terceiros) — desvio aprovado: card "Próximo simulado" sem botão de inscrição. Estados (disponível, agendado, concluído) sinalizados por rótulo textual, não apenas por cor (UX do PRD, acessibilidade). Datas, horários e notas em formato brasileiro via `DatePipe` com locale `pt-BR` — conversão de UTC só na exibição. Cronômetro do Modo Prova (reuso de `features/study`) sempre visível, com aviso claro ao aproximar-se do fim da duração; interface silenciosa: sem gabarito, sem estatísticas, sem elementos que induzam resposta. Rota `/simulados` e `/simulados/:id/resultado` para autenticados, sem guard de perfil administrativo.
</rules>

<requirements>
- RF-14 (lista de simulados disponíveis, agendados e concluídos, com nota)
</requirements>

## Subtarefas

- [x] 7.1 Criar `features/simulados/simulados-page/` — os três grupos, estado vazio quando as listas vierem `[]`.
- [x] 7.2 Criar `features/simulados/proximo-simulado-card/` — destaque do próximo agendado, contagem regressiva até a abertura.
- [x] 7.3 Criar `features/simulados/simulado-card/` — item de lista reutilizado nos três grupos, com rótulo textual do estado.
- [x] 7.4 Ligar o botão Iniciar/Retomar a `POST /simulados/{id}/tentativas` e navegar para o Modo Prova (`features/study`, rota `/estudar?sessao={id}`), incluindo o aviso de `tipo: "TREINO"` antes de começar quando aplicável (CA-06).
- [x] 7.5 Criar `features/simulados/simulado-resultado-page/` — nota, acertos, erros, não respondidas, tempo total, revisão questão a questão via `RespostaCorrecao`, com aviso quando `tipo = TREINO` de que a nota registrada (`notaRegistrada`) não mudou.
- [x] 7.6 Adicionar as rotas `/simulados` e `/simulados/:id/resultado` em `app.routes.ts` e o item "Simulados" na navegação do estudante em `layout/shell/shell.html`.
- [x] 7.7 Validar o fluxo de integração por testes de componentes, cliente HTTP existente, build e rotas; o teste manual ponta a ponta fica reservado à Tarefa 8.0, que executa os E2E com os backends ativos.

## Detalhes de implementação

Ver TechSpec, seção "Visão dos componentes" (mapa `features/simulados/`), "Modelos de dados" (`RespostaSimulados`, `SimuladoResumo`, `SimuladoConcluido`, `RespostaTentativa`, `RespostaResultado`), "Endpoints da API" (os quatro endpoints do estudante) e "Experiência do usuário" do PRD (fluxos "Fazer simulado" e "Revisar").

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-05
- CA-06
- CA-07

## Testes da tarefa

Nenhum caso de teste numerado da TechSpec é atribuído especificamente ao frontend do estudante (a TechSpec concentra TU/TI no backend e reserva E2E-01/E2E-02/E2E-03 para o fluxo completo na Tarefa 8.0). Ainda assim, todo código novo precisa de teste (`tests.md` regra 1): escrever `*.spec.ts` (Vitest) para `simulados-page`, `proximo-simulado-card`, `simulado-card` e `simulado-resultado-page`, cobrindo os três agrupamentos, o estado vazio, o aviso de treino e a revisão questão a questão. **Concluído:** testes adicionados e `npm test -- --watch=false` passou.

## Arquivos relevantes

- `frontend/src/app/features/simulados/simulados-page/`
- `frontend/src/app/features/simulados/proximo-simulado-card/`
- `frontend/src/app/features/simulados/simulado-card/`
- `frontend/src/app/features/simulados/simulado-resultado-page/`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/layout/shell/shell.html`
