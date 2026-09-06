# Tarefa 4.0: `EditalService` e DTOs: criar, atualizar e excluir com auditoria (RF-22)

## Visão geral

Implementar a escrita administrativa do agregado: criar, atualizar e excluir logicamente um edital, com a invariante do edital aberto, a unicidade de nome, a resolução das referências (banca, órgão, disciplinas) e o registro de auditoria. `EditalService` concentra `@Transactional`, o `@PreAuthorize` de administrador (CA-10) e a chamada ao `RegistradorAuditoria`.

Nesta tarefa nascem também os sete `record` de `dto/`, porque o serviço já entrega `EditalDetalhe` e consome `RequisicaoEdital`. Não há rascunho nem etapa de publicação: quem edita, publica (CA-05).

<skills>
### Conformidade com skills

- `spring-boot-testing` — serviço testado com JUnit + Mockito puros, sem contexto Spring; mocks apenas na fronteira (`EditalRepository`, `RegistradorAuditoria`, `ResolvedorDisciplinasEdital`); AssertJ nas asserções.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 2** — autorização validada no servidor com `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")`, mesmo com o menu escondido. O PRD exige administrador ou superior, **não professor**.
- **Regra 3** — DTOs `record` com mensagens de Bean Validation em pt-BR.
- `code-standards.md` §2 — a separação entre `EditalService` (escrita) e `ConsultaEditaisService` (leitura, tarefa 5.0) é o que torna o limite de 100 linhas por arquivo alcançável. Máximo de um id mais um objeto de parâmetro (`RequisicaoEdital`); limites de nome, vagas e URL como constantes nomeadas.
- `code-standards.md` §3 — injeção por construtor, sem Lombok, entidade fora do controller, erro por exceção de negócio. **Nenhuma exceção nova**: `ConflitoException`, `RecursoNaoEncontradoException` e `DadosInvalidosException` de `prd-banco-de-questoes` cobrem os três casos e o `ManipuladorErrosGlobal` não muda.
- **Restrição do PRD** — criar, editar e excluir edital são ações administrativas auditadas.
- `tests.md` §3 — AAA/Given-When-Then e nomes no padrão `TU06_recusaEditalAbertoSemPrazo`.
</rules>

<requirements>
- **RF-22** — CRUD de editais com status, vagas, banca, prazo e disciplinas.
- `criar(RequisicaoEdital) -> EditalDetalhe`; `atualizar(UUID, RequisicaoEdital) -> EditalDetalhe`; `excluir(UUID) -> void` (lógico, por `excluidoEm`).
- Invariante: `status` `ABERTO` exige `prazoInscricao` — recusa com `DadosInvalidosException` e `fieldErrors.prazoInscricao`.
- Nome duplicado entre não excluídos, ignorando maiúsculas → `ConflitoException` `NOME_EM_USO`.
- `bancaId`, `orgaoId` ou `disciplinaIds` inexistentes → `DadosInvalidosException` no campo correspondente.
- `urlOficial` sem esquema `http`/`https` → `DadosInvalidosException` em `urlOficial`.
- No `atualizar`, `disciplinaIds` **substitui** o conjunto anterior; enviar lista menor desvincula as ausentes, sem apagar `disciplina`.
- Id inexistente ou já excluído → `RecursoNaoEncontradoException` (`EDITAL_NAO_ENCONTRADO`).
</requirements>

## Subtarefas

- [x] 4.1 Criar os `record` de `dto/`: `EditaisAgrupados`, `EditalResumo`, `EditalAdminResumo`, `EditalDetalhe`, `RequisicaoEdital`, `DisciplinaExigida` e `ItemCatalogoResumo`.
- [x] 4.2 Anotar `RequisicaoEdital` com Bean Validation em pt-BR: `@NotBlank` e 3–160 no nome, `@NotNull` no status, `@Min(0)`/`@Max(999999)` em vagas, `@URL` e 500 em `urlOficial`.
- [x] 4.3 Implementar `criar`, com a invariante do edital aberto, a unicidade de nome e a resolução de banca, órgão e disciplinas.
- [x] 4.4 Implementar `atualizar`, substituindo o conjunto de disciplinas e preservando os campos não enviados como estão no corpo.
- [x] 4.5 Implementar `excluir` por exclusão lógica, mantendo os vínculos em `edital_disciplina` intactos.
- [x] 4.6 Aplicar `@PreAuthorize` de administrador ou superior nas três operações e `@Transactional` no ponto único do serviço.
- [x] 4.7 Registrar as três ações no `RegistradorAuditoria`, com ator, ação, entidade e o `payload` do delta.
- [x] 4.8 Escrever TU-05…TU-13.

## Detalhes de implementação

Ver `techspec.md` §*Design de implementação* → *Principais interfaces* (`EditalService`), §*Modelos de dados* (`RequisicaoEdital`, `EditalDetalhe`, `ErroApi` com os cinco códigos) e §*Sequenciamento do desenvolvimento*, passo 4.

## Critérios de aceitação relacionados

- CA-04
- CA-06
- CA-07
- CA-08

## Testes da tarefa

### Testes de unidade

- [x] TU-05 — `EditalService.criar` com todos os campos preenchidos
- [x] TU-06 — `EditalService.criar` com status aberto e sem prazo
- [x] TU-07 — `EditalService.criar` com nome já usado
- [x] TU-08 — `EditalService.criar` com `bancaId` inexistente
- [x] TU-09 — `EditalService.criar` com URL sem esquema `http`
- [x] TU-10 — `EditalService.atualizar` mudando status previsto para aberto
- [x] TU-11 — `EditalService.atualizar` trocando o conjunto de disciplinas
- [x] TU-12 — `EditalService.excluir` de edital existente
- [x] TU-13 — `EditalService.excluir` de id inexistente

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/catalogo/edital/EditalService.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/edital/dto/` — os sete `record` (novos)
- `backend/src/main/java/br/com/certame/catalogo/edital/ResolvedorDisciplinasEdital.java` (tarefa 3.0)
- `backend/src/main/java/br/com/certame/catalogo/edital/EditalRepository.java` (tarefa 2.0)
- `backend/src/main/java/br/com/certame/comum/auditoria/RegistradorAuditoria.java` (dependência)
- `backend/src/main/java/br/com/certame/comum/excecao/` — `ConflitoException`, `RecursoNaoEncontradoException`, `DadosInvalidosException` (dependências)
