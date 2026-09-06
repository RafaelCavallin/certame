# Tarefa 3.0: `SimuladoAdminService` e `PublicacaoSimuladoService`

## Visão geral

Implementa o CRUD administrativo do simulado (criação em rascunho, edição, substituição de composição, exclusão lógica, listagem paginada com filtros) e a orquestração de publicar/despublicar, que usa as regras puras da Tarefa 2.0 para validar, montar e congelar a série dentro de uma única transação. Inclui a geração de código sequencial `SM-###` e o registro de auditoria de todas as ações administrativas.

<skills>
### Conformidade com skills

- `spring-boot-testing` — test slices em vez de `@SpringBootTest` onde couber, `*IntegracaoTest` com Testcontainers e `@DynamicPropertySource`, AssertJ, dados isolados por execução.
- `postgresql-table-design` — nenhuma alteração de esquema nesta tarefa; a transação de publicação escreve `simulado` e `simulado_questao` atomicamente.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Leitura de `AGENTS.md` e das rules confirmada. Aplicam-se: regra 2 (`@PreAuthorize` no serviço, mesmo que o controller da Tarefa 4.0 ainda não exista — a autorização mora aqui), regra 4 (o contrato de erro `ErroApi` com os códigos `DADOS_INVALIDOS`, `COMPOSICAO_INVALIDA`, `PUBLICACAO_INVALIDA`, `SIMULADO_NAO_ENCONTRADO`, `SIMULADO_EM_USO`, `CODIGO_EM_USO` — subclasses de `ErroNegocioException`, sem `try/catch` em controller), regra 5 (a série é congelada dentro da mesma transação da mudança de status: ou publica com N questões gravadas, ou nada é escrito — CA-10). `SimuladoAdminService` não acessa `QuestaoRepository` nem `DisciplinaRepository` fora do que a TechSpec autoriza; DTOs sempre `record` com mensagens em português. Auditoria participa da transação — falha ao serializar derruba a operação (comportamento desejado para ação administrativa, conforme "Pontos de integração" da TechSpec).
</rules>

<requirements>
- RF-21 (CRUD de simulados com código, nome, banca, número de questões, duração, janela, status e composição por disciplina; publicar exige janela definida e quantidade compatível com a composição)
</requirements>

## Subtarefas

- [x] 3.1 Implementar `GeradorCodigoSimulado` (código sequencial `SM-###`, no molde de `GeradorCodigoQuestao`), tratando colisão de concorrência (`CODIGO_EM_USO`).
- [x] 3.2 Implementar `SimuladoAdminService.criar/atualizar/excluir/listar`, com `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")`, criação sempre em `RASCUNHO`, exclusão lógica (`excluido_em`), e bloqueio de edição/exclusão quando há tentativa finalizada (`SIMULADO_EM_USO`).
- [x] 3.3 Implementar `SimuladoAdminService.substituirComposicao`, substituindo a composição inteira, validando disciplina única/existente/não arquivada e quantidade ≥ 1 (`COMPOSICAO_INVALIDA`), devolvendo `ResumoComposicao` com soma, `fechada` e `acervoSuficiente`.
- [x] 3.4 Implementar `PublicacaoSimuladoService.publicar`, orquestrando `ValidadorPublicacaoSimulado` → `MontadorProvaSimulado` → gravação de `simulado_questao` → mudança de status → auditoria (`SIMULADO_PUBLICADO`), tudo na mesma transação.
- [x] 3.5 Implementar `PublicacaoSimuladoService.despublicar`, voltando a `RASCUNHO`, apagando a série congelada e auditando (`SIMULADO_DESPUBLICADO`); recusar quando há tentativa finalizada.
- [x] 3.6 Registrar auditoria (`RegistradorAuditoria`) para `SIMULADO_CRIADO`, `SIMULADO_ATUALIZADO`, `SIMULADO_COMPOSICAO_ALTERADA`, `SIMULADO_PUBLICADO`, `SIMULADO_DESPUBLICADO`, `SIMULADO_EXCLUIDO`, com payload de antes/depois da janela.
- [x] 3.7 Escrever os testes de unidade TU-14 a TU-16 e de integração TI-01, TI-02, TI-11, TI-12.

## Detalhes de implementação

Ver TechSpec, seção "Design de implementação" → "Principais interfaces" (`SimuladoAdminService`, `PublicacaoSimuladoService`), "Fluxo de dados — publicação", tabela `ErroApi` (códigos e status HTTP), e "Monitoramento e observabilidade" (eventos `simulado.publicado`, `simulado.publicacao_recusada`, `simulado.acervo_insuficiente`).

## Critérios de aceitação relacionados

- CA-08
- CA-09
- CA-10
- CA-12

## Testes da tarefa

### Testes de unidade

- [x] TU-14 — `TU14_recusaComposicaoComDisciplinaRepetida`
- [x] TU-15 — `TU15_resumoDaComposicaoComparaSomaComNumeroDeQuestoes`
- [x] TU-16 — `TU16_criaSimuladoComStatusRascunho`

### Testes de integração

- [x] TI-01 — `TI01_publicaSimuladoECongelaSerie`
- [x] TI-02 — `TI02_naoEscreveNadaQuandoPublicacaoERecusada`
- [x] TI-11 — `TI11_impedeAlteracaoDeComposicaoComTentativaFinalizada`
- [x] TI-12 — `TI12_registraAuditoriaDePublicacaoEDeJanela`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/simulado/SimuladoAdminService.java`
- `backend/src/main/java/br/com/certame/simulado/PublicacaoSimuladoService.java`
- `backend/src/main/java/br/com/certame/simulado/GeradorCodigoSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/dto/RequisicaoSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/dto/RequisicaoComposicao.java`
- `backend/src/main/java/br/com/certame/simulado/dto/ItemComposicao.java`
- `backend/src/main/java/br/com/certame/simulado/dto/FiltroSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/dto/SimuladoDetalhe.java`
- `backend/src/main/java/br/com/certame/simulado/dto/SimuladoResumoAdmin.java`
- `backend/src/main/java/br/com/certame/simulado/dto/ResumoComposicao.java`
- `backend/src/main/java/br/com/certame/comum/excecao/` (exceções novas ou reuso de `ConflitoException`/`RecursoNaoEncontradoException`)
- `backend/src/test/java/br/com/certame/simulado/SimuladoAdminServiceTest.java`
- `backend/src/test/java/br/com/certame/simulado/PublicacaoSimuladoIntegracaoTest.java`
