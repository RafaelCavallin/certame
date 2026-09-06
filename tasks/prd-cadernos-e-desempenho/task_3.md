# Tarefa 3.0: Regras puras de caderno e de desempenho

## Visão geral

Quatro objetos sem Spring, sem banco e sem I/O — `ProgressoCaderno`, `ReferenciaCaderno`, `CalendarioSemanas` e `SeletorPontosFracos` — que concentram o que realmente pode dar errado nesta funcionalidade: a lacuna que não pode virar zero (CA-07), o corte de 20 respostas (CA-09/CA-10) e a interpretação do id sintético `auto:*` (CA-05). São escritos antes dos serviços porque não dependem de nada, rodam em milissegundos e travam a semântica antes que qualquer serviço a interprete de outro jeito. Dezenove testes de unidade, o maior bloco da funcionalidade.

<skills>
### Conformidade com skills

- `spring-boot-testing` — a fatia mais estreita possível: JUnit e Mockito puros, **sem contexto Spring**, com asserções em AssertJ.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **`tests.md` §2 (Fast):** nenhum destes testes sobe contexto, toca banco ou dorme. **(Repeatable):** `CalendarioSemanas` recebe `Clock` injetado e os testes usam `Clock.fixed`.
- **`tests.md` §3:** AAA/Given-When-Then visível; nomes `TU01_geraOitoSegundasFeiras`, `TU02_devolveLacunaNaSemanaSemAtividade`.
- **`code-standards.md` §2:** arquivos ≤ 100 linhas, métodos ≤ 30, ≤ 3 parâmetros, ≤ 3 níveis de condicional, sem comentário, sem literal mágico — o mínimo de 20 respostas é constante nomeada, não número solto.
- **Regra 3:** nomes de classe e método em português sem acento.
- **`code-standards.md` §3:** erro de domínio por exceção da hierarquia de `ErroNegocioException`; `ReferenciaCaderno` lança `RecursoNaoEncontradoException`, nunca `IllegalArgumentException` cru.
</rules>

<requirements>
- **RF-13** — `ProgressoCaderno.percentual(respondidas, total)`: total zero devolve 0%, sem divisão por zero.
- **RF-13/CA-05** — `ReferenciaCaderno.de(String)` interpreta `auto:ERRO_RECORRENTE`, `auto:FAVORITA` e UUID; qualquer outra coisa é recusada.
- **RF-15/CA-07** — `CalendarioSemanas` gera 8 segundas-feiras terminando na semana corrente, no fuso `America/Sao_Paulo` (`comum/ZonaAplicacao`), e casa os agregados devolvendo **lacuna** (`volume: 0`, `pctAcerto: null`) onde não há linha.
- **RF-16/CA-09/CA-10** — `SeletorPontosFracos`: `volume >= 20` **e** `pctAcerto` abaixo da média do próprio usuário; ordenação por impacto `volume × (pctMediaUsuario - pctAcerto)`, não por percentual.
- Média do usuário nula não pode produzir `NullPointerException` nem divisão por zero.
</requirements>

## Subtarefas

- [x] 3.1 `comum/ZonaAplicacao` com a constante `America/Sao_Paulo`.
- [x] 3.2 `estudo/caderno/ProgressoCaderno` — regra pura de cobertura (TU-15…TU-17).
- [x] 3.3 `estudo/caderno/TipoCadernoAutomatico` (`ERRO_RECORRENTE`, `FAVORITA`) e `estudo/caderno/ReferenciaCaderno` (TU-18, TU-19).
- [x] 3.4 `desempenho/CalendarioSemanas` com `Clock` injetado: geração das 8 chaves e casamento com os agregados (TU-01…TU-05).
- [x] 3.5 `desempenho/SeletorPontosFracos` com o mínimo de 20 como constante nomeada e a ordenação por impacto (TU-09…TU-14).
- [x] 3.6 `comum/excecao/CadernoAutomaticoImutavelException`, reusando `RecursoNaoEncontradoException` e `ConflitoException` de `prd-banco-de-questoes`.
- [x] 3.7 Testes de unidade TU-01…TU-19, cobrindo caminho feliz, bordas (virada de ano, limite inclusivo de 20, caderno vazio) e falhas (id malformado, média nula).

## Detalhes de implementação

Ver `techspec.md`, §*Design de implementação* → *Principais interfaces* (assinaturas de `ReferenciaCaderno`, `ProgressoCaderno`, `CalendarioSemanas` e `SeletorPontosFracos`), §*Modelos de dados* → `SemanaEvolucao` (a nota "Lacuna, nunca zero") e `PontoFraco` (a nota sobre ordenação por impacto e sobre amostra pequena), e §*Principais decisões* (fuso da semana, lacuna como `null`, ordenação por impacto). A ordem de construção correspondente é o passo 3.

Nota: `TU-06`, `TU-07` e `TU-08` exercitam `EvolucaoSemanalService` e ficam na tarefa 6.0, embora dependam de `CalendarioSemanas`.

## Critérios de aceitação relacionados

- CA-01
- CA-04
- CA-05
- CA-06
- CA-07
- CA-09
- CA-10
- CA-11

## Testes da tarefa

### Testes de unidade

- [x] TU-01 — `CalendarioSemanas` gera 8 segundas-feiras terminando na semana corrente
- [x] TU-02 — `CalendarioSemanas` casa agregados e devolve lacuna onde não há linha
- [x] TU-03 — `CalendarioSemanas` com atividade só na semana corrente
- [x] TU-04 — `CalendarioSemanas` na virada de ano
- [x] TU-05 — `CalendarioSemanas` usa `America/Sao_Paulo`, não UTC
- [x] TU-09 — `SeletorPontosFracos` inclui assunto com 25 respostas e acerto abaixo da média
- [x] TU-10 — `SeletorPontosFracos` exclui assunto com 19 respostas e acerto baixíssimo
- [x] TU-11 — `SeletorPontosFracos` inclui assunto com exatamente 20 respostas
- [x] TU-12 — `SeletorPontosFracos` exclui assunto acima da média do usuário
- [x] TU-13 — `SeletorPontosFracos` ordena por impacto, não por percentual
- [x] TU-14 — `SeletorPontosFracos` com média de usuário nula
- [x] TU-15 — `ProgressoCaderno` com 18 de 40 respondidas
- [x] TU-16 — `ProgressoCaderno` de caderno vazio
- [x] TU-17 — `ProgressoCaderno` com todas respondidas
- [x] TU-18 — `ReferenciaCaderno` interpreta `auto:FAVORITA`, `auto:ERRO_RECORRENTE` e UUID
- [x] TU-19 — `ReferenciaCaderno` recusa id malformado e `auto:` desconhecido

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/comum/ZonaAplicacao.java`
- `backend/src/main/java/br/com/certame/comum/excecao/CadernoAutomaticoImutavelException.java`
- `backend/src/main/java/br/com/certame/estudo/caderno/ProgressoCaderno.java`, `ReferenciaCaderno.java`, `TipoCadernoAutomatico.java`
- `backend/src/main/java/br/com/certame/desempenho/CalendarioSemanas.java`, `SeletorPontosFracos.java`
- `backend/src/test/java/br/com/certame/desempenho/CalendarioSemanasTest.java`, `SeletorPontosFracosTest.java`
- `backend/src/test/java/br/com/certame/estudo/caderno/ProgressoCadernoTest.java`, `ReferenciaCadernoTest.java`
