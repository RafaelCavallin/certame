# Tarefa 2.0: Regras puras do domínio de estudo

## Visão geral

Implementa as regras de negócio que não dependem de Spring nem de estado externo: conferência de gabarito, validação da quantidade de questões da série, cálculo de tempo de sessão descontando pausas, o objeto parâmetro do filtro e o sorteio da série. São classes pequenas, rápidas de testar e que definem o vocabulário usado pelos serviços da Tarefa 3.0.

<skills>
### Conformidade com skills

- `spring-boot-testing` — testes de unidade puros (JUnit + AssertJ), sem contexto Spring, `Random` e `Clock` injetados conforme `tests.md` §2 R.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `code-standards.md` e `tests.md` lidos.

- `code-standards.md` §2 — sem literal mágico (10/30/50, os prazos), até 3 parâmetros (`FiltroSerie` como objeto parâmetro), arquivos ≤ 100 linhas e métodos ≤ 30 — daí `MontadorSerie`, `ConferenciaGabarito`, `TempoSessao` e `TamanhoSerie` serem classes próprias.
- `tests.md` §2 R — `Random` injetado em `MontadorSerie`, `Clock` injetado em `TempoSessao`; nenhum teste depende do relógio real nem do sorteio da máquina.
- RN-01 (`AGENTS.md`) — somente questões `PUBLICADA` entram na série; a regra vive em `MontadorSerie`/`SerieRepository`.
</rules>

<requirements>
- RF-07, RF-08, RF-09, RF-12
- RN-01
</requirements>

## Subtarefas

- [x] 2.1 Implementar `ConferenciaGabarito` (compara `marcada` com o gabarito para `ME` e `CE`, normaliza caixa, valida domínio).
- [x] 2.2 Implementar `TamanhoSerie` (aceita 10/30/50, recusa o resto com `fieldErrors`).
- [x] 2.3 Implementar `TempoSessao` (`duracaoSegundos`, `acumular` monotônico, `Clock` injetado).
- [x] 2.4 Implementar `FiltroSerie` (objeto parâmetro: `disciplinaId`, `assuntoId`, `bancaId`, `ano`, `dificuldade`, `somenteIneditas`, `excluirAcertadas`).
- [x] 2.5 Implementar `MontadorSerie` sobre `SerieRepository` (RN-01/CA-16: publicadas, não excluídas, sem repetição, `Random` injetado, série menor quando o acervo não basta, `SerieVaziaException` quando zero).

## Detalhes de implementação

Ver TechSpec, tabela "Backend — pacote `br.com.certame.estudo`" (linhas de `TamanhoSerie`, `FiltroSerie`, `MontadorSerie`, `ConferenciaGabarito`, `TempoSessao`) e "Principais interfaces" para as assinaturas exatas.

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-04
- CA-13
- CA-16

## Testes da tarefa

### Testes de unidade

- [x] TU-01 — `ConferenciaGabarito` acerta múltipla escolha com a letra correta
- [x] TU-02 — `ConferenciaGabarito` erra múltipla escolha com letra diferente
- [x] TU-03 — `ConferenciaGabarito` normaliza a caixa da marcação
- [x] TU-04 — `ConferenciaGabarito` recusa marcação fora de A–E em múltipla escolha
- [x] TU-05 — `ConferenciaGabarito` aceita CERTO e ERRADO em certo/errado
- [x] TU-06 — `TamanhoSerie` aceita 10, 30 e 50 e recusa o resto
- [x] TU-07 — `MontadorSerie` monta série só com questões publicadas
- [x] TU-08 — `MontadorSerie` não repete questão dentro da série
- [x] TU-09 — `MontadorSerie` devolve série menor quando o acervo não basta
- [x] TU-10 — `MontadorSerie` sem nenhuma questão elegível
- [x] TU-31 — `TempoSessao` desconta as pausas da duração
- [x] TU-32 — `TempoSessao` ignora `pausadoMs` menor que o já gravado
- [x] TU-33 — `TempoSessao` nunca devolve duração negativa

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/estudo/ConferenciaGabarito.java`
- `backend/src/main/java/br/com/certame/estudo/TamanhoSerie.java`
- `backend/src/main/java/br/com/certame/estudo/TempoSessao.java`
- `backend/src/main/java/br/com/certame/estudo/FiltroSerie.java`
- `backend/src/main/java/br/com/certame/estudo/MontadorSerie.java`
- `backend/src/main/java/br/com/certame/comum/excecao/SerieVaziaException.java`
- `backend/src/test/java/br/com/certame/estudo/ConferenciaGabaritoTest.java`
- `backend/src/test/java/br/com/certame/estudo/TamanhoSerieTest.java`
- `backend/src/test/java/br/com/certame/estudo/TempoSessaoTest.java`
- `backend/src/test/java/br/com/certame/estudo/MontadorSerieTest.java`
