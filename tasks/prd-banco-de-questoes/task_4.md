# Tarefa 4.0: Taxonomia e catálogo administrativo (RF-20, RF-37)

## Visão geral

Implementa duas estruturas distintas que o formulário de questão consome. A **árvore de matérias** (RF-20) — disciplina → assunto, duas camadas, usada também por flashcards, desempenho e composição de simulados — é mantida pelo **professor**. O **catálogo de bancas e órgãos** (RF-37) é uma lista plana, sem hierarquia, mantida pelo **administrador**; até a primeira versão desta TechSpec ele era só semente sem tela própria — a mudança de escopo está registrada em *Principais decisões*.

Vem antes das questões porque o formulário depende de disciplina, assunto, banca e órgão existirem. Três propriedades definem o desenho: a contagem de questões da árvore é **derivada** por uma consulta agregada, nunca armazenada (CA-10); excluir um nó ou item com acervo vinculado é **recusado** — assunto oferece arquivar (CA-11), banca/órgão só bloqueia, sem arquivamento (CA-14); e renomear é sempre um `update` de `nome` — como o vínculo é por `id`, CA-12 é consequência do modelo.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito puros no `TaxonomiaService` e no `CatalogoService` (sem contexto Spring, repositórios mockados); `@WebMvcTest` com `MockMvcTester` nos cenários de contrato do `CatalogoController`; `@DataJpaTest` com Testcontainers em TI-21; AssertJ nas asserções.
- `postgresql-table-design` — a unicidade de nome entre irmãos (assunto) e entre itens (banca/órgão) é garantida pelo índice único, não só pelo serviço: é ele que fecha a corrida entre requisições simultâneas (TI-25); a exclusão de banca/órgão em uso é reforçada pela FK sem `on delete` em `questao`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: regra 2 (as rotas de disciplina/assunto exigem `ROLE_PROFESSOR`+; as rotas de escrita de banca/órgão exigem `ROLE_ADMIN`+, no serviço — TI-30 prova o 403 para professor), regra 3 (`TaxonomiaService`, `CatalogoService`, `CatalogoController`, mensagens em pt-BR) e regra 4 (`POST /admin/assuntos/{id}/arquivar` e `GET`/`POST`/`PUT`/`DELETE` de `/admin/bancas` e `/admin/orgaos` são acréscimos ao contrato, já registrados na §6 pela tarefa 3.0). Nenhum desvio.
</rules>

<requirements>
- **RF-20** — Árvore de matérias e assuntos com contagem de questões, permitindo criar, renomear e remover nós.
- **`TaxonomiaService`** com `arvore()`, `criarDisciplina`, `renomearDisciplina`, `criarAssunto`, `renomearAssunto`, `excluirAssunto`, `arquivarAssunto` (e os equivalentes de disciplina). Exige perfil professor+.
- `arvore()` monta a árvore inteira numa **única consulta agregada** — sem N+1 — trazendo `totalQuestoes` e `totalPublicadas` por disciplina e por assunto (CA-10).
- A contagem ignora questões excluídas logicamente e conta na disciplina as questões sem assunto (TI-21).
- **`excluirAssunto`** lança `ConflitoException` `ASSUNTO_EM_USO` **com a contagem de questões na mensagem** quando há ao menos um vínculo (CA-11); sem vínculos, marca `excluido_em`. `DELETE /admin/disciplinas/{id}` segue a mesma lógica com `DISCIPLINA_EM_USO`.
- **`arquivarAssunto`** marca `arquivado_em`; o assunto some da árvore padrão e dos combos do formulário, mas as questões vinculadas permanecem intactas e visíveis.
- `GET /admin/disciplinas?incluirArquivados=true` traz também os nós arquivados.
- Renomear altera **apenas** `nome`; nenhum `update` em `questao` (CA-12). Trocar a disciplina de um assunto **não é suportado**.
- Nome de assunto duplicado entre irmãos → 409 `NOME_EM_USO`; nome fora de 2–120 caracteres → 422 `DADOS_INVALIDOS`.
- **RF-37** — Tela de cadastro de bancas e órgãos, com criar, editar e excluir; exclusão recusada quando há questões vinculadas.
- **`CatalogoService`** com `listarBancas`, `listarOrgaos`, `criarBanca`/`renomearBanca`, `criarOrgao`/`renomearOrgao`, `excluirBanca`/`excluirOrgao`. Leitura exige professor+; escrita exige **administrador+** (CA-13, CA-14).
- **`excluirBanca`/`excluirOrgao`** checam vínculo em `questao` antes de excluir e lançam `ConflitoException` `BANCA_EM_USO`/`ORGAO_EM_USO` **com a contagem na mensagem** quando há vínculo. Sem vínculo, **exclusão física** (banca/órgão não têm `excluido_em` nem `arquivado_em` — diferente da taxonomia).
- Nome de banca/órgão duplicado → 409 `NOME_EM_USO`; nome fora de 2–120 caracteres → 422 `DADOS_INVALIDOS`.
- `GET /admin/bancas` e `/admin/orgaos` devolvem `ItemCatalogo[]` ordenado por nome, sem paginação, com cache de segundo nível invalidado a cada escrita.
- Toda operação de escrita da taxonomia e do catálogo registra auditoria pelo `RegistradorAuditoria`.
- Métrica `certame.taxonomia.exclusao.recusada` incrementada a cada CA-11 acionado.
</requirements>

## Subtarefas

- [x] 4.1 Criar os DTOs `DisciplinaArvore`, `AssuntoArvore`, `RequisicaoNoTaxonomia`, `ItemCatalogo` e `RequisicaoItemCatalogo` em `catalogo/dto/`, com Bean Validation (nome de 2 a 120 caracteres).
- [x] 4.2 Implementar `TaxonomiaService.arvore()` sobre a consulta agregada do repositório, com a contagem derivada por disciplina e por assunto.
- [x] 4.3 Implementar criação e renomeação de disciplina e assunto, com a checagem de nome duplicado entre irmãos.
- [x] 4.4 Implementar `excluirAssunto` e `excluirDisciplina` com a recusa por vínculo, e `arquivarAssunto`/`desarquivar`.
- [x] 4.5 Implementar `CatalogoService` com criação, renomeação e exclusão de banca e órgão, com a checagem de vínculo antes da exclusão física.
- [x] 4.6 Registrar auditoria em todas as escritas (taxonomia e catálogo) e a métrica `certame.taxonomia.exclusao.recusada`.
- [x] 4.7 Implementar `CatalogoController` com as rotas de `/admin/disciplinas`, `/admin/assuntos` (professor+) e `/admin/bancas`, `/admin/orgaos` (leitura professor+, escrita admin+ por `@PreAuthorize`).
- [x] 4.8 Escrever os testes de unidade TU-23 a TU-27 e TU-36 a TU-40.
- [ ] 4.9 Escrever os testes de integração TI-20 a TI-26 e TI-29 a TI-34.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`TaxonomiaService`, `CatalogoService`), *Modelos de dados* (`DisciplinaArvore`, `AssuntoArvore`, `RequisicaoNoTaxonomia`, `ItemCatalogo`, `RequisicaoItemCatalogo`), *Endpoints da API* (`GET /admin/disciplinas`, `POST`/`PUT` de disciplinas e assuntos, `DELETE /admin/assuntos/{id}` e `POST /admin/assuntos/{id}/arquivar`, `GET`/`POST`/`PUT`/`DELETE /admin/bancas` e `/admin/orgaos`) e *Principais decisões* (contagem derivada nunca armazenada, `arquivado_em` em vez de coluna `status`, banca e órgão como catálogo plano semeado e mantido pelo administrador — decisão revisada, CA-12 como consequência do modelo).

## Critérios de aceitação relacionados

- CA-03
- CA-10
- CA-11
- CA-12
- CA-13
- CA-14

## Testes da tarefa

### Testes de unidade

- [x] TU-23 — `TaxonomiaService.excluirAssunto` com questões vinculadas
- [x] TU-24 — `TaxonomiaService.excluirAssunto` sem vínculos
- [x] TU-25 — `TaxonomiaService.arquivarAssunto`
- [x] TU-26 — `TaxonomiaService.renomearAssunto`
- [x] TU-27 — `TaxonomiaService` com nome duplicado entre irmãos
- [x] TU-36 — `CatalogoService.criarBanca` com nome e sigla válidos
- [x] TU-37 — `CatalogoService.excluirBanca` com questões vinculadas
- [x] TU-38 — `CatalogoService.criarOrgao` com nome e sigla válidos
- [x] TU-39 — `CatalogoService.excluirOrgao` com questões vinculadas
- [x] TU-40 — `CatalogoService` com nome de banca ou órgão duplicado

### Testes de integração

- [ ] TI-20 — `GET /admin/disciplinas` com árvore povoada
- [ ] TI-21 — Contagem ignora questões excluídas e conta as sem assunto na disciplina
- [ ] TI-22 — `DELETE /admin/assuntos/{id}` com questões vinculadas
- [ ] TI-23 — `POST /admin/assuntos/{id}/arquivar` seguido de `GET /admin/disciplinas`
- [ ] TI-24 — `PUT /admin/assuntos/{id}` renomeando
- [ ] TI-25 — `POST /admin/disciplinas` com nome duplicado (ignorando maiúsculas)
- [ ] TI-26 — `GET /admin/bancas` e `/admin/orgaos`
- [ ] TI-29 — `POST /admin/bancas` como `ADMIN`
- [ ] TI-30 — `POST /admin/bancas` como `PROFESSOR`
- [ ] TI-31 — `DELETE /admin/bancas/{id}` com questões vinculadas
- [ ] TI-32 — `PUT /admin/orgaos/{id}` renomeando
- [ ] TI-33 — `DELETE /admin/orgaos/{id}` com questões vinculadas
- [ ] TI-34 — `DELETE /admin/bancas/{id}` sem vínculos

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/catalogo/TaxonomiaService.java`
- `backend/src/main/java/br/com/certame/catalogo/CatalogoService.java`
- `backend/src/main/java/br/com/certame/catalogo/CatalogoController.java`
- `backend/src/main/java/br/com/certame/catalogo/dto/`
- `backend/src/main/java/br/com/certame/catalogo/DisciplinaRepository.java` (consulta agregada)
- `backend/src/main/java/br/com/certame/catalogo/AssuntoRepository.java`
- `backend/src/main/java/br/com/certame/catalogo/BancaRepository.java`
- `backend/src/main/java/br/com/certame/catalogo/OrgaoRepository.java`
