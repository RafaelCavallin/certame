# Tarefa 4.0: Início do estudante e expiração de sessões

## Visão geral

Implementa o resumo do dia exibido no Início (RF-06) — questões resolvidas, percentual de acerto, tempo de estudo somando questões e flashcards, e a sessão retomável — e o job que expira sessões abandonadas (leitura após 7 dias, Modo Prova após 24 h), garantindo que CA-10 valha também para sessões de leitura esquecidas, não só para o Modo Prova.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `Clock` fixo em todos os testes de tempo; `@Scheduled` testado por chamada direta ao método, não por espera real.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `code-standards.md` e `tests.md` lidos.

- RF-06 (`AGENTS.md`/PRD) — os números do Início refletem a atividade real do usuário, incluindo o tempo de flashcards.
- `tests.md` §2 R — `Clock` injetado em `ResumoDiaService` e `ExpiradorSessoes`; nenhum teste depende do relógio real.
- Fuso horário: `comum/ZonaAplicacao` (`America/Sao_Paulo`) é usado para o corte do dia — um dia civil calculado em UTC viraria às 21h em Brasília.
- `TempoEstudoExterno` é uma porta (`code-standards.md` §3, integração por contrato): a ausência de implementação (F6b ainda não existe) não pode lançar `NullPointerException` nem bloquear esta entrega.
</rules>

<requirements>
- RF-06, RF-12
- RN-04 (expiração do Modo Prova)
</requirements>

## Subtarefas

- [x] 4.1 Criar `comum/ZonaAplicacao` (`America/Sao_Paulo`).
- [x] 4.2 Implementar `estudo/inicio/TempoEstudoExterno` (porta `segundosNoDia(usuarioId, dia)`), sem implementação nesta entrega — retorno 0 quando ausente.
- [x] 4.3 Implementar `estudo/inicio/ResumoDiaService.doDia` (RF-06, CA-01): resolvidas, percentual de acerto, tempo do dia (questões + `TempoEstudoExterno`), sessão retomável, uso de `ZonaAplicacao` para o corte do dia.
- [x] 4.4 Implementar `estudo/ExpiradorSessoes` (`@Scheduled` horário): marca `expirada_em` em sessões `FOCO`/`CONTEXTO` sem atividade há mais de 7 dias e `PROVA` sem atividade há mais de 24 h; nunca apaga linha.

## Detalhes de implementação

Ver TechSpec, "Principais interfaces" (`ResumoDiaService`, `TempoEstudoExterno`) e o modelo `ResumoInicio`/`SessaoRetomavel` em "Modelos de dados", incluindo os exemplos de dia sem atividade. Ver "Pontos de integração" para o contrato exato de `TempoEstudoFlashcards` (F6b) que implementará `TempoEstudoExterno` futuramente.

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01
- CA-10
- CA-13

## Testes da tarefa

### Testes de unidade

- [x] TU-34 — `ResumoDiaService` soma resolvidas, percentual e tempo do dia
- [x] TU-35 — `ResumoDiaService` conta a sessão em andamento pelo `ultima_atividade_em`
- [x] TU-36 — `ResumoDiaService` soma o tempo de flashcards pela porta
- [x] TU-37 — `ResumoDiaService` sem a implementação da porta
- [x] TU-38 — `ResumoDiaService` em dia sem atividade
- [x] TU-39 — `ResumoDiaService` usa `America/Sao_Paulo` para o corte do dia
- [x] TU-40 — `ExpiradorSessoes` expira Prova após 24 h e leitura após 7 dias

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/comum/ZonaAplicacao.java`
- `backend/src/main/java/br/com/certame/estudo/inicio/ResumoDiaService.java`
- `backend/src/main/java/br/com/certame/estudo/inicio/TempoEstudoExterno.java`
- `backend/src/main/java/br/com/certame/estudo/ExpiradorSessoes.java`
- `backend/src/test/java/br/com/certame/estudo/inicio/ResumoDiaServiceTest.java`
- `backend/src/test/java/br/com/certame/estudo/ExpiradorSessoesTest.java`
