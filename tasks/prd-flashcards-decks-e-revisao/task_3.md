# Tarefa 3.0: Agendador FSRS

## Visão geral

Implementa o único ponto que calcula agendamento no sistema — `flashcard/fsrs` — com o FSRS-5 completo (fórmulas de estabilidade e dificuldade de longo prazo, mais a fórmula de mesmo dia e a máquina de estados de aprendizado/reaprendizado). `AgendadorFsrs.previsoes()` chama `proximo()` uma vez por nota, para que a previsão exibida em cada botão de classificação (CA-03) e o agendamento efetivamente gravado (CA-07) nunca possam divergir. É a peça mais crítica e mais isolada da funcionalidade: sem Spring, sem repositório, sem relógio próprio — e o risco mais sério do documento, porque um erro de sinal ou de índice de peso não quebra nada visível, só agenda mal por meses.

<skills>
### Conformidade com skills

- `spring-boot-testing` — mesmo sendo classes puras (sem Spring), a estratégia de teste segue o padrão do projeto: JUnit Jupiter + AssertJ, sem *mock* algum aqui (o agendador é o que está sob teste), fatia mais estreita possível.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md`, `.agents/rules/code-standards.md` e `.agents/rules/tests.md`.

- **Regra 7 (AGENTS.md)** — o agendamento segue o FSRS de RN-08, implementado em `flashcard/fsrs`; não trocar o algoritmo nem os pesos sem atualizar o PRD.
- `code-standards.md` — arquivos até 100 linhas e métodos até 30 (motivo direto de o agendador ser cinco arquivos e não um); até 3 parâmetros, com `ContextoAgendamento` como objeto parâmetro; nenhum número mágico fora de `PesosFsrs`; `record`s para entrada e saída.
- `tests.md` §2 — FIRST: teste 100% determinístico, sem I/O, sem relógio real; é exatamente o tipo de defeito "invisível" que a regra pede para cobrir com casos de referência (TU-21).
</rules>

<requirements>
- RN-08 — agendamento por FSRS versão 5, pesos padrão publicados versionados junto ao código.
- CA-03, CA-07, CA-08, CA-09, CA-17.
</requirements>

## Subtarefas

- [x] 3.1 `flashcard/fsrs/PesosFsrs.java`: os 19 pesos, `DECAY`, `FACTOR`, limites de saturação de estabilidade e dificuldade, passos de aprendizado/reaprendizado, `TEMPO_MAX_POR_CARD` — constantes `static final`.
- [x] 3.2 `flashcard/fsrs/CurvaEsquecimento.java`: `retrievability(S, dias)` e `intervaloParaRetencao(S, retencaoAlvo)`.
- [x] 3.3 `flashcard/fsrs/CalculoDificuldade.java`: `inicial(nota)` e `proxima(dificuldadeAtual, nota)`, com reversão à média e saturação em [1, 10].
- [x] 3.4 `flashcard/fsrs/CalculoEstabilidade.java`: `inicial(nota)`, `aposAcerto(...)`, `aposLapso(...)`, `curtoPrazo(S, nota)`.
- [x] 3.5 `flashcard/fsrs/PassosAprendizado.java`: máquina de transição entre `NOVO`, `APRENDENDO`, `REVISAO`, `REAPRENDENDO`.
- [x] 3.6 `flashcard/fsrs/AgendadorFsrs.java` com `proximo(EstadoMemoria, NotaRevisao, ContextoAgendamento)` e `previsoes(EstadoMemoria, ContextoAgendamento)`, e os `record`s `EstadoMemoria`, `ContextoAgendamento`, `Agendamento`, `PrevisaoIntervalos`.
- [x] 3.7 `flashcard/NotaRevisao.java`: enum `ERREI(1)`, `DIFICIL(2)`, `BOM(3)`, `FACIL(4)` com ordinal explícito.
- [x] 3.8 Escrever a tabela de casos de referência do FSRS-5 (TU-21), derivada da implementação de referência do protótipo aprovado e dos pesos publicados.

## Detalhes de implementação

Ver `techspec.md`, seções "Principais interfaces" (bloco `AgendadorFsrs`/`CurvaEsquecimento`/`CalculoEstabilidade`/`CalculoDificuldade`/`PassosAprendizado`), "Fórmulas do agendador (RN-08)", "Máquina de estados dos passos" e "Parâmetros fixos do FSRS-5 (RN-08)".

## Critérios de aceitação relacionados

- CA-03, CA-07, CA-08, CA-09, CA-17

## Testes da tarefa

### Testes de unidade

- [x] TU-01 — `CurvaEsquecimento` devolve retenção 1 com zero dias decorridos
- [x] TU-02 — `CurvaEsquecimento` decresce monotonicamente com os dias
- [x] TU-03 — `CurvaEsquecimento` fecha o ciclo intervalo → retenção
- [x] TU-04 — `CurvaEsquecimento` encurta o intervalo quando a retenção alvo sobe
- [x] TU-05 — `CalculoDificuldade.inicial` cobre as quatro notas dentro da faixa
- [x] TU-06 — `CalculoDificuldade.proxima` aplica reversão à média em D0(Fácil)
- [x] TU-07 — `CalculoDificuldade` satura em 1 e em 10
- [x] TU-08 — `CalculoEstabilidade.inicial` usa o peso da nota
- [x] TU-09 — `CalculoEstabilidade.aposAcerto` cresce mais com retenção baixa
- [x] TU-10 — `CalculoEstabilidade.aposLapso` nunca supera a estabilidade anterior
- [x] TU-11 — `CalculoEstabilidade.curtoPrazo` aplica a fórmula de mesmo dia
- [x] TU-12 — `PassosAprendizado` leva card novo com *Bom* ao segundo passo
- [x] TU-13 — `PassosAprendizado` gradua com *Bom* no último passo
- [x] TU-14 — `PassosAprendizado` gradua imediatamente com *Fácil*
- [x] TU-15 — `AgendadorFsrs` recalcula memória e agenda com *Bom*
- [x] TU-16 — `AgendadorFsrs` com *Errei* em card de revisão
- [x] TU-17 — `AgendadorFsrs` trunca o intervalo no máximo do deck
- [x] TU-18 — `AgendadorFsrs` respeita retenções alvo diferentes no mesmo card
- [x] TU-19 — `AgendadorFsrs.previsoes` coincide com `proximo` nas quatro notas
- [x] TU-20 — `AgendadorFsrs` não muta o `EstadoMemoria` recebido
- [x] TU-21 — `AgendadorFsrs` reproduz a tabela de casos de referência do FSRS-5

## Arquivos relevantes

- `flashcard/fsrs/PesosFsrs.java`
- `flashcard/fsrs/CurvaEsquecimento.java`
- `flashcard/fsrs/CalculoDificuldade.java`
- `flashcard/fsrs/CalculoEstabilidade.java`
- `flashcard/fsrs/PassosAprendizado.java`
- `flashcard/fsrs/AgendadorFsrs.java`
- `flashcard/fsrs/EstadoMemoria.java`
- `flashcard/fsrs/ContextoAgendamento.java`
- `flashcard/fsrs/Agendamento.java`
- `flashcard/fsrs/PrevisaoIntervalos.java`
- `flashcard/NotaRevisao.java`
