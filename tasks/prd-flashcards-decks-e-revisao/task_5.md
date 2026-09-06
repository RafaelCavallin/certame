# Tarefa 5.0: Montagem da fila diária

## Visão geral

Implementa RN-10: a fila diária é montada deck a deck, descontando o consumo já registrado no dia (agrupado por card distinto, não por classificação), aplicando `novos_dia` e `revisoes_dia` por deck e ordenando por vencimento. Depende do `RelogioEstudo` e do `LimitesDeck` da tarefa 4.0, e consome a porta de leitura `ConsultaFilaFlashcards` já criada por F6a.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito, com repositórios mockados à mão e acervos montados no próprio teste; `Clock` fixo para determinismo (`tests.md` §2, Repeatable). O `AgendadorFsrs` não é mockado em lugar algum do projeto — aqui nem é consumido diretamente.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md`, `.agents/rules/code-standards.md` e `.agents/rules/tests.md`.

- **Regra 9 (AGENTS.md)** — a fila de revisão é montada respeitando os limites do deck (RN-10). É o núcleo desta tarefa.
- **Regra 7** — a consulta de consumo diário e a fila permanecem por `usuario_id`; nenhum caminho expõe card de outro usuário.
- `tests.md` §2 — todo teste com data usa `Clock` fixo injetado em `RelogioEstudo`.
</rules>

<requirements>
- RN-10 — limite diário por deck, contado a partir do histórico, por card distinto.
- CA-06, CA-15, CA-19, CA-20, US9.
</requirements>

## Subtarefas

- [x] 5.1 `flashcard/revisao/ConsumoDiario.java`: `record` com `novos` e `revisoes` por deck, mais a consulta agregada `porDeck(usuarioId, inicioDoDia)` sobre `flashcard_revisao`.
- [x] 5.2 `flashcard/revisao/EscopoFila.java`: enum `TODOS`, `DECK`, `SEM_DECK`.
- [x] 5.3 `flashcard/revisao/MontadorFila.java`: `montar(usuarioId, EscopoFila, RelogioEstudo)`, aplicando limites por deck (inclusive os limites padrão de "Sem deck"), descontando o consumo diário e ordenando por vencimento crescente (empate por `criado_em`).
- [x] 5.4 Estender `flashcard/FlashcardRepository.java` com a consulta da fila por escopo, consumindo `ConsultaFilaFlashcards.elegiveis(usuarioId, ate)` de F6a sem alterar sua assinatura.

## Detalhes de implementação

Ver `techspec.md`, seções "Principais interfaces" (bloco `MontadorFila`/`ConsumoDiarioRepository`) e "Considerações técnicas" (linhas "Limite diário contado a partir do histórico, por card distinto" e "Card novo identificado por `estabilidade_ant = 0`, sem coluna nova").

## Critérios de aceitação relacionados

- CA-06, CA-19, CA-20

## Testes da tarefa

### Testes de unidade

- [x] TU-31 — `MontadorFila` respeita novos e revisões por dia de cada deck
- [x] TU-32 — `MontadorFila` desconta o consumo já registrado no dia
- [x] TU-33 — `MontadorFila` conta card distinto, não classificação
- [x] TU-34 — `MontadorFila` aplica limites deck a deck, sem misturar
- [x] TU-35 — `MontadorFila` usa os limites padrão para cards sem deck
- [x] TU-36 — `MontadorFila` ordena por vencimento crescente
- [x] TU-37 — `MontadorFila` não antecipa cards de vencimento futuro
- [x] TU-38 — `MontadorFila` ignora cards arquivados e excluídos
- [x] TU-39 — `MontadorFila` restringe por escopo de deck e por disciplina

## Arquivos relevantes

- `flashcard/revisao/ConsumoDiario.java`
- `flashcard/revisao/EscopoFila.java`
- `flashcard/revisao/MontadorFila.java`
- `flashcard/FlashcardRepository.java` (modificado)
