# Tarefa 2.0: Regras puras — `JanelaSimulado`, `ValidadorPublicacaoSimulado`, `MontadorProvaSimulado`

## Visão geral

Implementa o núcleo de regra de negócio do domínio, isolado de Spring e de banco: derivar o estado do simulado a partir da janela (`JanelaSimulado`), validar se um simulado pode ser publicado (`ValidadorPublicacaoSimulado`) e sortear a série de questões respeitando a composição por disciplina (`MontadorProvaSimulado`). São objetos puros, testados em milissegundos, e não dependem de nenhuma outra tarefa além do esquema da Tarefa 1.0.

<skills>
### Conformidade com skills

- `spring-boot-testing` — testes de unidade sem contexto Spring, dependências mockadas à mão, AssertJ nas asserções.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Leitura de `AGENTS.md` e das rules confirmada. Aplicam-se: arquivo até 100 linhas, método até 30, até 3 parâmetros, até 3 níveis de condicional com cláusulas guarda, sem literal mágico (`DURACAO_MINIMA_SEGUNDOS`, `QUESTOES_MAXIMO`, `LIMIAR_MONTAGEM_LENTA` como constantes nomeadas), sem comentário decorativo. `JanelaSimulado` recebe `Clock` injetado (nunca `Instant.now()` direto) para ser testável e repetível (`tests.md` §2 R-Repeatable). `ValidadorPublicacaoSimulado.validar` devolve o mapa de pendências por campo, nunca lança exceção diretamente — quem decide o fluxo é o serviço da Tarefa 3.0. Mensagens de pendência em português com acento.
</rules>

<requirements>
- RN-01 (só questões publicadas compõem o simulado)
- RF-21 (publicar exige janela definida e quantidade compatível com a composição)
</requirements>

## Subtarefas

- [x] 2.1 Implementar `JanelaSimulado.estado(StatusSimulado, Instant abertura, Instant encerramento)` e `aceitaNovaTentativa(EstadoSimulado)`, com `Clock` injetado.
- [x] 2.2 Implementar `ValidadorPublicacaoSimulado.validar(Simulado, List<SimuladoComposicao>, Map<UUID,Long> acervo)`, cobrindo janela ausente, soma da composição divergente do número de questões, e acervo insuficiente por disciplina (mensagem nomeando a disciplina).
- [x] 2.3 Implementar `MontadorProvaSimulado.montar(UUID simuladoId, List<SimuladoComposicao>)`, sorteando apenas questões `PUBLICADA` na quantidade exata por disciplina.
- [x] 2.4 Escrever os testes de unidade TU-03 a TU-09.

## Detalhes de implementação

Ver TechSpec, seção "Design de implementação" → "Principais interfaces" (assinaturas de `JanelaSimulado`, `ValidadorPublicacaoSimulado`, `MontadorProvaSimulado`) e "Considerações técnicas" → "Estado do simulado derivado, não armazenado" e "Sorteio de 200 questões em acervo grande" (uso do índice `questao_filtro_idx` e do log `simulado.montagem_lenta`, limiar de 2 s).

## Critérios de aceitação relacionados

- CA-04
- CA-10
- CA-11

## Testes da tarefa

### Testes de unidade

- [x] TU-03 — `TU03_montaSerieApenasComQuestoesPublicadas`
- [x] TU-04 — `TU04_respeitaQuantidadePorDisciplinaNaComposicao`
- [x] TU-05 — `TU05_recusaPublicacaoSemJanela`
- [x] TU-06 — `TU06_recusaPublicacaoComComposicaoQueNaoFecha`
- [x] TU-07 — `TU07_recusaPublicacaoComAcervoInsuficiente`
- [x] TU-08 — `TU08_derivaAgendadoDisponivelEEncerradoDaJanela`
- [x] TU-09 — `TU09_recusaInicioForaDaJanela`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/simulado/JanelaSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/ValidadorPublicacaoSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/MontadorProvaSimulado.java`
- `backend/src/test/java/br/com/certame/simulado/JanelaSimuladoTest.java`
- `backend/src/test/java/br/com/certame/simulado/ValidadorPublicacaoSimuladoTest.java`
- `backend/src/test/java/br/com/certame/simulado/MontadorProvaSimuladoTest.java`
