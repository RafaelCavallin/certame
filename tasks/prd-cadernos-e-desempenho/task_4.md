# Tarefa 4.0: Cadernos — serviços, controller e DTOs (RF-13)

## Visão geral

O domínio de cadernos inteiro: CRUD do caderno manual, projeção dos dois automáticos a partir de `marcacao`, a lista unificada com progresso calculado em lote e a porta que a sessão de estudo usará para montar a série de questões. O ponto central do desenho é que **o automático não é linha**: "Erros recorrentes" e "Favoritas" são projeções de `marcacao` com ids sintéticos `auto:ERRO_RECORRENTE` e `auto:FAVORITA`, o que faz de CA-02, CA-03 e do caso de borda 3 propriedades do modelo, não de um sincronizador.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit e Mockito puros nos serviços; `@WebMvcTest` com `MockMvcTester` para o contrato HTTP e a autorização do controller; `@SpringBootTest` só nos fluxos completos.
- `postgresql-table-design` — `insert … on conflict do nothing` na adição de questões; consultas agregadas de progresso apoiadas nos índices da tarefa 2.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 2 (inegociável):** toda rota exige autenticação e é escopada ao usuário do token; **nenhuma aceita `usuarioId` como parâmetro**. Caderno de terceiro responde **404, nunca 403** — mesma política de RN-07.
- **Regra 4:** `GET /cadernos/{id}/questoes` e `DELETE /cadernos/{id}/questoes/{questaoId}` são acréscimos ao contrato; a §6 da `TECH-SPEC.md` é atualizada **na mesma entrega**.
- **Regra 6 / RN-02:** nenhum caminho desta tarefa escreve em `resposta`, `sessao_estudo` ou `marcacao` — apenas leitura.
- **`code-standards.md` §3:** controller só orquestra e mapeia DTO; regra e `@Transactional` no serviço; entidade nunca sai no controller; DTOs são `record` com mensagem de validação em pt-BR; um domínio não acessa o repositório de outro — a validação de publicação da questão passa pelo serviço de `questao`.
- **`tests.md` §1:** cadernos são criticidade média; caminho feliz e bordas cobertos, com atenção especial às três operações recusadas em caderno automático.
</rules>

<requirements>
- **RF-13** — criar, renomear/redescrever, excluir logicamente e adicionar/remover questões do caderno manual; lista com nome, descrição, progresso e data da última atualização.
- Os dois automáticos **sempre** vêm na resposta, mesmo vazios, com `atualizadoEm: null` e `editavel: false`.
- Ordem da lista: `ERRO_RECORRENTE`, `FAVORITA`, depois os manuais por `atualizadoEm desc`.
- Progresso é **cobertura**: questões distintas do caderno já respondidas ao menos uma vez sobre o total atual — a questão conta uma vez por mais que seja respondida.
- `nome` de 3 a 80 caracteres, único por usuário sem diferenciar maiúsculas → `409 NOME_CADERNO_EM_USO`, também em `fieldErrors.nome`.
- `descricao` opcional, até 240 caracteres.
- Renomear, excluir ou alterar questões de `auto:*` → `409 CADERNO_AUTOMATICO_IMUTAVEL`.
- **RN-01** — questão não publicada no lote → `422 QUESTAO_NAO_PUBLICADA`, com o **lote inteiro recusado**; ids repetidos ou já presentes são não operação, sem erro.
- Progresso calculado em consultas agregadas em lote, nunca uma por caderno.
</requirements>

## Subtarefas

- [x] 4.1 DTOs `record` em `estudo/caderno/dto/`: `CadernoResumo`, `RequisicaoCaderno`, `RequisicaoQuestoesCaderno`, `QuestaoCadernoResumo`, com Bean Validation e mensagens em pt-BR.
- [x] 4.2 `CadernoService`: criar, atualizar, excluir logicamente, adicionar e remover questões; recusa toda escrita em caderno automático (TU-20…TU-24).
- [x] 4.3 `CadernoAutomaticoService`: projeta os dois cadernos sintéticos a partir de `marcacao`, com nome e descrição fixos, contagem e data da marcação mais recente (TU-25…TU-28).
- [x] 4.4 `ConsultaCadernosService`: lista unificada e ordenada, com progresso em lote (TU-29).
- [x] 4.5 `SerieDoCadernoService`: resolve uma `ReferenciaCaderno` na lista ordenada e estável de `questao_id` (TU-30).
- [x] 4.6 Listagem paginada de questões do caderno (`PaginaResposta<QuestaoCadernoResumo>`), aceitando UUID e `auto:*`.
- [x] 4.7 `CadernoController` com as sete rotas de `/api/v1/cadernos`, `Location` no `201` e status corretos.
- [x] 4.8 Registrar `NOME_CADERNO_EM_USO`, `CADERNO_AUTOMATICO_IMUTAVEL` e `QUESTAO_NAO_PUBLICADA` no `ManipuladorErrosGlobal`.
- [x] 4.9 Atualizar a §6 da `TECH-SPEC.md` com as duas rotas acrescentadas.
- [x] 4.10 Testes TU-20…TU-30 e TI-06…TI-15, TI-18.

## Detalhes de implementação

Ver `techspec.md`, §*Arquitetura do sistema* → *Backend* (linhas de `CadernoService`, `CadernoAutomaticoService`, `ConsultaCadernosService`, `SerieDoCadernoService`, `CadernoController`), §*Principais interfaces*, §*Modelos de dados* (`CadernoResumo`, `RequisicaoCaderno`, `RequisicaoQuestoesCaderno`, `QuestaoCadernoResumo`, `ErroApi`), §*Endpoints da API* (as sete rotas de `/cadernos`, com exemplos de sucesso e de erro) e §*Principais decisões* (automático como projeção, progresso como cobertura, 404 para terceiro, `on conflict do nothing`). Ordem de construção: passo 4.

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-04
- CA-05
- CA-11

## Testes da tarefa

### Testes de unidade

- [ ] TU-20 — `CadernoService.criar` recusa nome duplicado do mesmo usuário
- [ ] TU-21 — `CadernoService.criar` aceita nome já usado por outro usuário
- [ ] TU-22 — `CadernoService` recusa renomear, excluir e alterar questões de caderno automático
- [ ] TU-23 — `CadernoService.adicionarQuestoes` recusa o lote com questão não publicada
- [ ] TU-24 — `CadernoService.adicionarQuestoes` ignora questão já presente
- [ ] TU-25 — `CadernoAutomaticoService` projeta "Erros recorrentes" a partir das marcações
- [ ] TU-26 — `CadernoAutomaticoService` projeta "Favoritas"
- [ ] TU-27 — `CadernoAutomaticoService` sem marcação alguma
- [ ] TU-28 — Responder corretamente questão marcada não altera a projeção
- [ ] TU-29 — `ConsultaCadernosService` ordena automáticos antes dos manuais
- [ ] TU-30 — `SerieDoCadernoService` devolve as 40 questões do caderno em ordem estável

### Testes de integração

- [ ] TI-06 — `GET /cadernos` com automáticos e manuais
- [ ] TI-07 — `GET /cadernos` de usuário novo
- [ ] TI-08 — Questão marcada como erro recorrente aparece no caderno automático
- [ ] TI-09 — Questão favoritada aparece em "Favoritas"
- [ ] TI-10 — Responder corretamente questão marcada não a remove do caderno
- [ ] TI-11 — Criar caderno manual e adicionar questões
- [ ] TI-12 — `PUT`/`DELETE`/`POST questoes` em `auto:*`
- [ ] TI-13 — Caderno de outro usuário
- [ ] TI-14 — Rotas sem token
- [ ] TI-15 — Adicionar questão não publicada
- [ ] TI-18 — Responder duas vezes a mesma questão do caderno

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/estudo/caderno/CadernoService.java`, `CadernoAutomaticoService.java`, `ConsultaCadernosService.java`, `SerieDoCadernoService.java`, `CadernoController.java`
- `backend/src/main/java/br/com/certame/estudo/caderno/dto/` — os quatro DTOs
- `backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java` (modificado)
- `TECH-SPEC.md` §6 (modificado)
- `backend/src/test/java/br/com/certame/estudo/caderno/` — `*Test` e `*IntegracaoTest`
