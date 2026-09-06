# Tarefa 4.0: `SimuladoAdminController` (endpoints administrativos e autorização)

## Visão geral

Expõe os sete endpoints administrativos sobre `SimuladoAdminService` e `PublicacaoSimuladoService`: listar, criar, detalhar, editar, excluir, substituir composição, publicar e despublicar. O controller só orquestra — valida o corpo com `@Valid` e monta a `ResponseEntity`; a autorização já está no serviço (Tarefa 3.0) e é provada de verdade aqui, no slice web, contra um token de perfil insuficiente.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `*IntegracaoTest` com Testcontainers e `@DynamicPropertySource` para provar 403 de ponta a ponta (autorização só é provada de verdade no slice web/integração, não em unidade).
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Leitura de `AGENTS.md` e das rules confirmada. Regra 2 (autorização sempre no servidor, mesmo que a interface já esconda a ação) e regra 4 (nenhum endpoint fora do contrato — os dez endpoints desta funcionalidade, sete administrativos nesta tarefa, entram na `TECH-SPEC.md` §6 na mesma alteração). Controller sem regra de negócio, sem acesso a repositório, sem `@Transactional`. Erros seguem `application/problem+json` via `ManipuladorErrosGlobal`, sem `try/catch` novo.
</rules>

<requirements>
- RF-21 (CRUD de simulados — parte da interface HTTP)
- RN-01 (indiretamente, via publicação)
</requirements>

## Subtarefas

- [x] 4.1 Implementar `GET /api/v1/admin/simulados` com filtros `q`, `estado`, `bancaId`, `page`, `size` (`size` máximo 100, `estado` fora da lista responde 422).
- [x] 4.2 Implementar `POST /api/v1/admin/simulados` (201 com `Location`), `GET` e `PUT /api/v1/admin/simulados/{id}`, `DELETE /api/v1/admin/simulados/{id}`.
- [x] 4.3 Implementar `PUT /api/v1/admin/simulados/{id}/composicao`.
- [x] 4.4 Implementar `POST /api/v1/admin/simulados/{id}/publicar` e `POST /api/v1/admin/simulados/{id}/despublicar`.
- [x] 4.5 Atualizar `TECH-SPEC.md` §6 com os sete endpoints administrativos (regra 4 do `AGENTS.md`).
- [x] 4.6 Escrever o teste de integração TI-08 (perfil de estudante recusado em todas as operações administrativas).

## Detalhes de implementação

Ver TechSpec, seção "Endpoints da API" → tabela "Visão geral" e os blocos detalhados de cada endpoint administrativo (parâmetros, respostas por status, exemplos).

## Critérios de aceitação relacionados

- CA-08
- CA-09
- CA-10
- CA-12

## Testes da tarefa

### Testes de integração

- [x] TI-08 — `TI08_recusaOperacoesAdministrativasParaEstudante`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/simulado/SimuladoAdminController.java`
- `backend/src/test/java/br/com/certame/simulado/PublicacaoSimuladoIntegracaoTest.java` (`TI08_recusaOperacoesAdministrativasParaEstudante`)
- `TECH-SPEC.md` (§6 — sete endpoints administrativos)
