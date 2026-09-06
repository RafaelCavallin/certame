# Tarefa 5.0: `SimuladoCatalogoService`, `TentativaSimuladoService`, `ExpiradorTentativaSimulado` e `SimuladoController`

## Visão geral

Implementa a ponta do estudante: a lista de simulados em três grupos com o card de destaque, o início/retomada/finalização da tentativa reaproveitando o Modo Prova de `prd-sessao-de-estudo`, a expiração automática de tentativas abandonadas, e os quatro endpoints públicos correspondentes. É o passo que depende de F3 (`SessaoEstudoService`) estar disponível — confirme o estado dessa dependência antes de iniciar.

<skills>
### Conformidade com skills

- `spring-boot-testing` — test slices, `*IntegracaoTest` com Testcontainers, AssertJ, dados isolados por execução.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Leitura de `AGENTS.md` e das rules confirmada. Regra 6 (RN-04: respostas só contam após finalização; `TentativaSimuladoService` não grava nada em `resposta` nem fala com `sessao_questao` diretamente — só via `SessaoEstudoService`). Regra 2 (autorização — aqui é apenas autenticação, mas a tentativa é sempre filtrada por `usuario_id`, respondendo 404 para tentativa de outro usuário, nunca 403, para não revelar tentativa alheia). Um domínio não acessa o repositório de outro: conversa com `estudo/` só por `SessaoEstudoService`. `ExpiradorTentativaSimulado` usa `@Scheduled`, sem estado estático compartilhado entre execuções.
</rules>

<requirements>
- RF-14 (lista de simulados disponíveis, agendados e concluídos, com nota)
- RN-01 (só questões publicadas — já garantido na série congelada da Tarefa 3.0)
- RN-04 (respostas só contam após finalização; abandono descarta a tentativa)
</requirements>

## Subtarefas

- [x] 5.1 Implementar `SimuladoCatalogoService.listarDoEstudante`, agrupando em `proximo`/`disponiveis`/`agendados`/`concluidos` via `JanelaSimulado`, com a nota vindo da tentativa `VALIDA`.
- [x] 5.2 Implementar `TentativaSimuladoService.iniciar`, abrindo `SessaoEstudoService` com `modo = PROVA`, `origem = SIMULADO`, `origemRef = simuladoId` e a série congelada; retomar idempotente quando já há tentativa `EM_ANDAMENTO`; decidir `VALIDA`/`TREINO` apenas na finalização.
- [x] 5.3 Implementar `TentativaSimuladoService.finalizar`, delegando a `SessaoEstudoService.finalizar`, recebendo `RespostaCorrecao`, gravando nota/acertos/erros/não respondidas/tempo total.
- [x] 5.4 Implementar `TentativaSimuladoService.resultado`, para revisão de tentativa finalizada (404 para tentativa `ABANDONADA`, de outro usuário, ou ainda em andamento).
- [x] 5.5 Implementar `ExpiradorTentativaSimulado` (`@Scheduled`), marcando `ABANDONADA` a tentativa cuja sessão expirou.
- [x] 5.6 Implementar `SimuladoController` com os quatro endpoints do estudante.
- [x] 5.7 Atualizar `TECH-SPEC.md` §6 com os quatro endpoints do estudante restantes (regra 4 do `AGENTS.md`).
- [x] 5.8 Escrever os testes de unidade TU-01, TU-02, TU-10 a TU-13 e de integração TI-03 a TI-07, TI-09, TI-10.

## Detalhes de implementação

Ver TechSpec, seção "Design de implementação" → `SimuladoCatalogoService`, `TentativaSimuladoService`, "Fluxo de dados — aplicação", e os blocos de endpoint `GET /simulados`, `POST /simulados/{id}/tentativas`, `POST .../finalizar`, `GET .../tentativas/{tentativaId}`. Ver também "Pontos de integração" quanto ao contrato exigido de `SessaoEstudoService` (sobrecarga com série explícita, dependência de F3) e o Caso de borda 3 (janela encerrar durante a prova não invalida a finalização).

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-05
- CA-06
- CA-07
- CA-11

## Testes da tarefa

### Testes de unidade

- [x] TU-01 — `TU01_agrupaSimuladosEmDisponiveisAgendadosEConcluidos`
- [x] TU-02 — `TU02_exibeNotaEDataNaListaDeConcluidos`
- [x] TU-10 — `TU10_abreSessaoEmModoProvaComDuracaoDeclarada`
- [x] TU-11 — `TU11_marcaSegundaTentativaComoTreino`
- [x] TU-12 — `TU12_calculaNotaSobreOTotalDaProva`
- [x] TU-13 — `TU13_retomaTentativaEmAndamentoSemReiniciarCronometro`

### Testes de integração

- [x] TI-03 — `TI03_doisEstudantesRecebemAMesmaProva`
- [x] TI-04 — `TI04_aplicaSimuladoDoInicioAoResultado`
- [x] TI-05 — `TI05_naoRegistraNotaDeTentativaAbandonada`
- [x] TI-06 — `TI06_refeituraNaoAlteraNotaRegistrada`
- [x] TI-07 — `TI07_congelaTentativaFinalizada`
- [x] TI-09 — `TI09_derivaEstadoDaJanelaNaListaDoEstudante`
- [x] TI-10 — `TI10_encerramentoDaJanelaBloqueiaNovaEntradaMasNaoAFinalizacao`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/simulado/SimuladoCatalogoService.java`
- `backend/src/main/java/br/com/certame/simulado/TentativaSimuladoService.java`
- `backend/src/main/java/br/com/certame/simulado/ExpiradorTentativaSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/SimuladoController.java`
- `backend/src/main/java/br/com/certame/simulado/dto/SimuladoResumo.java`
- `backend/src/main/java/br/com/certame/simulado/dto/SimuladoConcluido.java`
- `backend/src/main/java/br/com/certame/simulado/dto/RespostaSimulados.java`
- `backend/src/main/java/br/com/certame/simulado/dto/RespostaTentativa.java`
- `backend/src/main/java/br/com/certame/simulado/dto/RespostaResultado.java`
- `backend/src/main/java/br/com/certame/estudo/SessaoEstudoService.java` (sobrecarga com série explícita, se ainda pendente de F3)
- `backend/src/test/java/br/com/certame/simulado/SimuladoCatalogoServiceTest.java`
- `backend/src/test/java/br/com/certame/simulado/TentativaSimuladoServiceTest.java`
- `backend/src/test/java/br/com/certame/simulado/TentativaSimuladoIntegracaoTest.java`
- `TECH-SPEC.md` (§6 — endpoints do estudante)
