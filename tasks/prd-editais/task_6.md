# Tarefa 6.0: `EditalController` e `EditalAdminController`: contrato, autorização e `no-store`

## Visão geral

Expor as seis rotas da funcionalidade: a leitura do estudante em `/api/v1/editais`, autenticada e com `Cache-Control: no-store`, e as cinco rotas administrativas em `/api/v1/admin/editais`, restritas a administrador ou superior. São dois controllers, não um, pelo limite de 100 linhas por arquivo do `code-standards.md` §2.

É aqui que a maior parte da regra desta funcionalidade se prova: a autorização por perfil (CA-10), a propagação imediata sem cache (CA-05), a migração de agrupamento (CA-06), o sumiço após exclusão (CA-07) e a auditoria das três ações administrativas.

<skills>
### Conformidade com skills

- `spring-boot-testing` — fatia mais estreita por cenário: `@WebMvcTest` com `MockMvcTester` nos cenários de contrato e autorização (15 dos casos), `@SpringBootTest` apenas em TI-11, TI-15, TI-16 e TI-21, que atravessam escrita e leitura. Testcontainers com PostgreSQL 18 real (`@ServiceConnection`), sem H2; contexto Spring compartilhado entre as classes de integração.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 2** — `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` nas cinco rotas administrativas, mesmo com a interface já escondendo a ação. TI-12, TI-13 e TI-14 provam o 403 no servidor.
- **Regra 4** — `GET /editais` é acréscimo à §6 da `TECH-SPEC.md` e o perfil de `/admin/editais` é corrigido de "professor+" para "admin+". O documento é atualizado na mesma entrega — tarefa 10.0.
- `code-standards.md` §3 — controllers só orquestram e montam a `ResponseEntity`; regra, `@Transactional` e `@PreAuthorize` ficam no serviço. Sem `try/catch` em controller; sem entidade no controller. O envelope de erro é o `ErroApi` `application/problem+json` já existente — `ManipuladorErrosGlobal` **não muda**.
- `tests.md` §3 — nomes no padrão `TI15_mudarStatusMigraEditalDeAgrupamento`; dados por *builders* no pacote de teste, com nome isolado por `UUID.randomUUID()` para não colidir no índice único.
- **Restrição do PRD** — nenhuma camada de cache pode atrasar a visão do estudante: sem `@Cacheable`, sem `ETag`, sem CDN no caminho.
</rules>

<requirements>
- **RF-17** — `GET /api/v1/editais`, autenticado: 200 com `EditaisAgrupados` sempre, inclusive sem editais; 401 sem token; `Cache-Control: no-store`.
- **RF-22** — `GET /api/v1/admin/editais` (lista com `prazoVencido`), `GET /admin/editais/{id}`, `POST /admin/editais` (201 com `Location`), `PUT /admin/editais/{id}` (200), `DELETE /admin/editais/{id}` (204).
- Códigos de erro: `403 ACESSO_NEGADO`, `404 EDITAL_NAO_ENCONTRADO`, `409 NOME_EM_USO`, `422 DADOS_INVALIDOS` com `fieldErrors`.
- Nenhuma rota paginada — o envelope `{content, page, size, totalElements}` da §6 não se aplica a listagens que não crescem sem limite.
- Perfis `ESTUDANTE` e `PROFESSOR` recebem 403 em todas as rotas administrativas, incluindo a leitura.
</requirements>

## Subtarefas

- [x] 6.1 Criar `EditalController` com `GET /api/v1/editais`, devolvendo a `ResponseEntity` com `Cache-Control: no-store`.
- [x] 6.2 Criar `EditalAdminController` com as cinco rotas de `/api/v1/admin/editais`.
- [x] 6.3 Aplicar `@Valid` no corpo de `POST` e `PUT` e devolver `Location` no 201.
- [x] 6.4 Conferir o mapeamento dos cinco códigos de erro pelo `ManipuladorErrosGlobal` existente, sem acrescentar tipo novo.
- [x] 6.5 Escrever TI-05…TI-10 (leitura do estudante) com `@WebMvcTest`, incluindo o cabeçalho `no-store` e o 401 sem token.
- [x] 6.6 Escrever TI-12, TI-13, TI-14, TI-17, TI-18, TI-19 e TI-20 (autorização, validação e contrato administrativo) com `@WebMvcTest`.
- [x] 6.7 Escrever TI-11, TI-15, TI-16 e TI-21 com `@SpringBootTest`, atravessando escrita e leitura.
- [x] 6.8 Confirmar em TI-06 a ausência de N+1 pelo contador de consultas.

## Detalhes de implementação

Ver `techspec.md` §*Endpoints da API* na íntegra (as seis rotas, com tabelas de resposta e exemplos), §*Pontos de integração* (`SegurancaConfig`, `ManipuladorErrosGlobal`) e §*Sequenciamento do desenvolvimento*, passo 6.

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-07
- CA-09
- CA-10
- Caso de borda 2 do PRD

## Testes da tarefa

### Testes de integração

- [x] TI-05 — `GET /editais` com editais nos três status
- [x] TI-06 — `GET /editais` carrega disciplinas, banca e órgão numa consulta
- [x] TI-07 — `GET /editais` devolve vagas, prazo e banca no payload
- [x] TI-08 — `GET /editais` em base sem editais
- [x] TI-09 — `GET /editais` sem token
- [x] TI-10 — `GET /editais` responde `Cache-Control: no-store`
- [x] TI-11 — `POST /admin/editais` como `ADMIN`
- [x] TI-12 — `POST /admin/editais` como `ESTUDANTE`
- [x] TI-13 — `PUT /admin/editais/{id}` como `PROFESSOR`
- [x] TI-14 — `DELETE /admin/editais/{id}` como `ESTUDANTE`
- [x] TI-15 — `PUT` mudando previsto para aberto e novo `GET /editais`
- [x] TI-16 — `DELETE` seguido de `GET /editais`
- [x] TI-17 — `POST` com `disciplinaIds` contendo id inexistente
- [x] TI-18 — `POST` com nome duplicado ignorando maiúsculas
- [x] TI-19 — `GET /admin/editais` com um aberto de prazo vencido
- [x] TI-20 — `GET /admin/editais/{id}` de edital excluído
- [x] TI-21 — Auditoria das três ações administrativas

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/catalogo/edital/EditalController.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/edital/EditalAdminController.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/edital/EditalService.java` (tarefa 4.0)
- `backend/src/main/java/br/com/certame/catalogo/edital/ConsultaEditaisService.java` (tarefa 5.0)
- `backend/src/main/java/br/com/certame/config/SegurancaConfig.java` (dependência)
- `backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java`, `ErroApi.java` (dependências, não modificados)
