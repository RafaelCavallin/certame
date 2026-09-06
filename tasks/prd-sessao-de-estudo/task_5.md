# Tarefa 5.0: Controllers, contrato da API e eventos

## Visão geral

Expõe os sete endpoints REST do domínio `estudo` sobre os serviços das Tarefas 3.0 e 4.0, atualiza a `TECH-SPEC.md` §6 com os quatro endpoints novos na mesma alteração (regra 4 do `AGENTS.md`), e fecha a suíte de testes de integração ponta a ponta contra banco real via Testcontainers. É o único ponto em que o domínio vira contrato HTTP — controllers só orquestram, toda regra já está nos serviços.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `@WebMvcTest` para status/formato/autenticação dos controllers, `@SpringBootTest @AutoConfigureMockMvc @Testcontainers` sobre `PostgreSQLContainer<>("postgres:18")` para os fluxos ponta a ponta, `@DynamicPropertySource`, dados isolados por execução (`tests.md` §2 I).
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `code-standards.md` e `tests.md` lidos.

- Regra 2 (`AGENTS.md`) — todos os endpoints exigem autenticação; nenhum exige perfil específico (a tela é a mesma para os 4 perfis); o servidor decide mesmo quando a interface já esconde a ação (ex.: `MODO_NAO_ALTERAVEL`).
- Regra 4 (`AGENTS.md`) — os 4 endpoints novos entram na `TECH-SPEC.md` §6 na mesma alteração desta tarefa.
- `code-standards.md` §3 — controller só orquestra; DTOs em `record` com Bean Validation e mensagem em pt-BR; erros por `@RestControllerAdvice` único, que não muda.
- Desempenho (PRD, restrições técnicas) — CA-14 exige a série pré-carregada, sem N+1, medida por TI-25.
</rules>

<requirements>
- RF-06, RF-07, RF-08, RF-09, RF-10, RF-11
- RN-01, RN-02, RN-03, RN-04
</requirements>

## Subtarefas

- [x] 5.1 Implementar `estudo/dto/` (14 records: `RequisicaoSessao`, `RespostaSessao`, `QuestaoDaSerie`, `AlternativaDaSerie`, `RequisicaoResposta`, `RespostaRegistrada`, `RequisicaoFinalizacao`, `RespostaCorrecao`, `ItemCorrecao`, `RequisicaoModo`, `RequisicaoMarcacoes`, `MarcacoesQuestao`, `ResumoInicio`, `SessaoRetomavel`) com Bean Validation e mensagens em pt-BR.
- [x] 5.2 Implementar `estudo/SessaoEstudoController` (`POST /estudo/sessoes`, `GET /estudo/sessoes/{id}`, `PATCH /estudo/sessoes/{id}/modo`, `POST /estudo/sessoes/{id}/respostas`, `POST /estudo/sessoes/{id}/finalizar`, `PUT /estudo/questoes/{id}/marcacoes`).
- [x] 5.3 Implementar `estudo/inicio/InicioController` (`GET /estudo/inicio`).
- [x] 5.4 Atualizar `TECH-SPEC.md` §6 com os quatro endpoints novos (`GET /estudo/inicio`, `GET /estudo/sessoes/{id}`, `PATCH .../modo`, `PUT /estudo/questoes/{id}/marcacoes`).
- [x] 5.5 Adicionar os instrumentos de observabilidade (`Timer certame.estudo.abertura/resposta/finalizacao`, `Counter certame.estudo.serie_vazia/sessao_expirada`, `Gauge certame.estudo.sessoes_ativas`) e os logs estruturados descritos na TechSpec.

## Detalhes de implementação

Ver TechSpec, seção "Endpoints da API" (contrato completo, exemplos de sucesso e erro por endpoint) e "Monitoramento e observabilidade" para os instrumentos e logs.

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01
- CA-02
- CA-03
- CA-05
- CA-06
- CA-07
- CA-09
- CA-10
- CA-11
- CA-14
- CA-16

## Testes da tarefa

### Testes de integração

- [x] TI-07 — Abertura de sessão devolve 201 com a série completa e sem gabarito
- [x] TI-08 — Abertura de sessão nunca inclui Rascunho nem Em revisão (`SessaoEstudoIntegracaoTest`)
- [x] TI-09 — Abertura com filtro sem correspondência responde 422 `SERIE_VAZIA` (`SessaoEstudoIntegracaoTest`)
- [x] TI-10 — Abertura com `tamanho: 7` responde 422 com `fieldErrors.tamanho`
- [x] TI-11 — Resposta fora do Modo Prova devolve gabarito, comentário e estatística (`SessaoEstudoIntegracaoTest`)
- [x] TI-12 — Estatística devolvida exclui as respostas do autor da questão (`SessaoEstudoIntegracaoTest`)
- [x] TI-13 — Troca de modo preserva questão, marcação e cronômetro
- [x] TI-14 — Retomada devolve a mesma série na primeira questão sem resposta (`SessaoEstudoIntegracaoTest`)
- [x] TI-15 — Retomada de sessão de outro usuário responde 404 (`SessaoEstudoIntegracaoTest`)
- [x] TI-16 — Modo Prova não devolve gabarito em nenhuma etapa antes de finalizar (`SessaoEstudoIntegracaoTest`)
- [x] TI-17 — Finalização do Modo Prova grava o lote e devolve a correção (`SessaoEstudoIntegracaoTest`)
- [x] TI-18 — Prova abandonada não deixa resposta alguma (`SessaoEstudoIntegracaoTest`)
- [x] TI-19 — Sessão expirada some da retomada e não conta como finalizada (`SessaoEstudoIntegracaoTest`)
- [x] TI-20 — Finalização publica `SessaoFinalizada` depois do commit (`SessaoEstudoIntegracaoTest`)
- [x] TI-21 — Marcação persiste entre sessões (`SessaoEstudoIntegracaoTest`)
- [x] TI-22 — `GET /estudo/inicio` reflete o histórico real do dia
- [x] TI-23 — Tempo do dia soma questões e flashcards (`SessaoEstudoIntegracaoTest`)
- [x] TI-24 — Endpoints exigem autenticação (`SessaoEstudoIntegracaoTest`)
- [x] TI-25 — Série de 50 é servida em uma requisição dentro do orçamento (`SessaoEstudoIntegracaoTest`)

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/estudo/dto/` (14 records)
- `backend/src/main/java/br/com/certame/estudo/SessaoEstudoController.java`
- `backend/src/main/java/br/com/certame/estudo/inicio/InicioController.java`
- `TECH-SPEC.md` (§6, modificado)
- `backend/src/test/java/br/com/certame/estudo/SessaoEstudoControllerTest.java`
- `backend/src/test/java/br/com/certame/estudo/SessaoEstudoIntegracaoTest.java`
- `backend/src/test/java/br/com/certame/estudo/inicio/InicioControllerTest.java`
- `backend/src/test/java/br/com/certame/estudo/inicio/InicioIntegracaoTest.java`
