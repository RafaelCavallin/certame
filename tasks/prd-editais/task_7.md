# Tarefa 7.0: `EditalConsultaVinculos` e recusa de exclusão de disciplina vinculada

## Visão geral

Fechar o vínculo de volta com a taxonomia. Sem esta tarefa, a integridade de CA-08 dependeria só da FK e devolveria erro cru ao administrador que tentasse excluir uma disciplina usada por um edital.

`EditalConsultaVinculos` é a porta de leitura (`disciplinaEmUso(UUID) -> boolean`) consumida pelo `TaxonomiaService` — existe justamente para que um domínio não alcance o repositório de outro (`code-standards.md` §3). `TaxonomiaService.excluirDisciplina` passa a consultá-la e a recusar com `DISCIPLINA_EM_USO`.

É o passo 7 da ordem de construção: depois das tarefas 2.0 e 4.0, porque precisa da tabela e do serviço prontos, e **antes do frontend**, porque muda uma mensagem que a tela de matérias já exibe.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `@SpringBootTest` ou `@WebMvcTest` conforme a fatia já usada pelos testes de `TaxonomiaService` em `prd-banco-de-questoes`; Testcontainers com PostgreSQL real, porque o vínculo e a FK só existem lá.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- `code-standards.md` §3 — **um domínio não acessa o repositório de outro**: `TaxonomiaService` consulta `EditalConsultaVinculos`, nunca `EditalRepository`. É a única razão de a porta existir.
- **Regra 4** — nenhum endpoint novo aqui; `DELETE /admin/disciplinas/{id}` já existe e apenas ganha um caso de recusa.
- **Nenhuma exceção nova** — `ConflitoException` com o código `DISCIPLINA_EM_USO` já é o padrão usado pela checagem de questões vinculadas.
- **Alteração contida em código entregue na F1** — um método a mais na checagem que já existe, com o mesmo código de erro e o mesmo formato de mensagem. A tela de matérias **não muda**; TI-22 e TI-23 são escritos junto da alteração.
</rules>

<requirements>
- **CA-08** — a consistência taxonômica vale nos dois sentidos: o edital só aponta para disciplinas da árvore, e a árvore não perde uma disciplina que um edital referencia.
- `EditalConsultaVinculos.disciplinaEmUso(UUID disciplinaId) -> boolean` — verdadeiro quando existe edital **não excluído** vinculado à disciplina.
- `DELETE /admin/disciplinas/{id}` com edital vinculado → 409 `DISCIPLINA_EM_USO`; a disciplina continua na árvore e o edital permanece íntegro.
- Renomear uma disciplina vinculada continua permitido: `PUT /admin/disciplinas/{id}` devolve 200 e `GET /editais` passa a exibir o novo nome, com `disciplina_id` inalterado — a FK é o vínculo, o nome não é copiado.
</requirements>

## Subtarefas

- [x] 7.1 Criar a interface `EditalConsultaVinculos` e sua implementação sobre o `EditalRepository`, filtrando editais não excluídos.
- [x] 7.2 Injetar a porta em `TaxonomiaService` por construtor.
- [x] 7.3 Acrescentar a checagem em `excluirDisciplina`, recusando com `ConflitoException` `DISCIPLINA_EM_USO` na mesma mensagem em pt-BR já usada pela checagem de questões.
- [x] 7.4 Confirmar que a renomeação de disciplina continua permitida e reflete em `GET /editais` sem migração de dados.
- [x] 7.5 Escrever TI-22 e TI-23.

## Detalhes de implementação

Ver `techspec.md` §*Arquitetura do sistema* → *Visão dos componentes* (linhas de `EditalConsultaVinculos` e do `TaxonomiaService` modificado), §*Pontos de integração* (linha `TaxonomiaService.excluirDisciplina`, "alteração de mão dupla"), §*Modelos de dados* → `ErroApi` (código `DISCIPLINA_EM_USO`) e §*Riscos conhecidos*, última linha.

## Critérios de aceitação relacionados

- CA-08

## Testes da tarefa

### Testes de integração

- [x] TI-22 — `DELETE /admin/disciplinas/{id}` com edital vinculado
- [x] TI-23 — `PUT /admin/disciplinas/{id}` renomeando disciplina vinculada

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/catalogo/edital/EditalConsultaVinculos.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/TaxonomiaService.java` (**modificado**)
- `backend/src/main/java/br/com/certame/catalogo/edital/EditalRepository.java` (tarefa 2.0)
- `backend/src/main/java/br/com/certame/comum/excecao/ConflitoException.java` (dependência)
