# Tarefa 4.0: Controller e DTOs (UsuarioAdminController)

## Visão geral

`UsuarioAdminController` expõe os seis endpoints de `/api/v1/super/usuarios` definidos na TechSpec, apenas orquestrando chamadas ao `GestaoUsuariosService` (tarefa 3.0) — validação de entrada via Bean Validation nos DTOs, sem regra de negócio no controller. Cobre a prova de autorização por perfil na fatia web (`@WebMvcTest`) e a estabilidade do envelope de erro nas salvaguardas.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `@ControllerTest`/`@WebMvcTest` para provar `@PreAuthorize` e o mapeamento de exceções para `ErroApi`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lido `AGENTS.md` e as rules em `.agents/rules/` (`code-standards.md`, `tests.md`, `folder-structure.md`).

- **Regra 4 (AGENTS.md)** — nenhum endpoint fora do contrato de `TECH-SPEC.md` §6; a rota `reenviar-convite` já foi adicionada ao §6 pela TechSpec desta funcionalidade — confirmar que o documento reflete exatamente os seis endpoints implementados.
- **Regra 2 (AGENTS.md)** — o controller não reimplementa autorização de domínio; apenas o `@PreAuthorize` do serviço (tarefa 3.0) é a fonte de verdade, mas a fatia web precisa provar que qualquer perfil ≠ SUPER_ADMIN recebe 403 e requisição sem token recebe 401 (CA-13).
- Camada Controller: validação e DTO, sem regra de negócio (`.agents/rules/folder-structure.md` §2.1).
- Erros em `application/problem+json` (`code`, `message`, `fieldErrors`) por `ManipuladorErrosGlobal` (regra existente, reuso obrigatório — não criar um segundo `@RestControllerAdvice`).
</rules>

<requirements>
- Techspec, "Endpoints da API" — os seis endpoints (`GET`, `POST`, `PATCH`, `DELETE /{id}`, `POST /{id}/redefinir-senha`, `POST /{id}/reenviar-convite`) com os status HTTP e corpos de resposta descritos.
- CA-13 — qualquer perfil ≠ SUPER_ADMIN recusado no servidor.
</requirements>

## Subtarefas

- [x] 4.1 Implementar `GET /api/v1/super/usuarios` com `page`/`size`/`sort`/`perfil`/`status`, delegando a `listar` e retornando `PaginaResposta<UsuarioGestaoResumo>`.
- [x] 4.2 Implementar `POST /api/v1/super/usuarios` com `RequisicaoCriarUsuario` validado, retornando 201.
- [x] 4.3 Implementar `PATCH /api/v1/super/usuarios/{id}` com `RequisicaoEditarUsuario` validado, retornando 200 ou os erros 404/422/409 mapeados.
- [x] 4.4 Implementar `DELETE /api/v1/super/usuarios/{id}` retornando 204 ou 404/409.
- [x] 4.5 Implementar `POST /api/v1/super/usuarios/{id}/redefinir-senha` e `POST /api/v1/super/usuarios/{id}/reenviar-convite`, retornando 204 ou 404/409 (`STATUS_INCOMPATIVEL`).
- [x] 4.6 Garantir `@PreAuthorize` efetivo na fatia web e mapeamento estável de `ConflitoException`/`EmailEmUsoException`/`StatusIncompativelException` para os códigos `ErroApi` correspondentes.

## Detalhes de implementação

Ver `techspec.md`, seção "Endpoints da API" (contrato completo, exemplos de requisição/resposta) e tabela de `ErroApi` em "Modelos de dados".

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01
- CA-02
- CA-03
- CA-04
- CA-07
- CA-08
- CA-13

## Testes da tarefa

### Testes de integração

- [x] TI-03 — `TI03_recusaOperacaoSemPapelSuperAdminNoSliceWeb` (Controller)
- [x] TI-07 — `TI07_envelopeDeErroEstavelNasSalvaguardas` (Integracao)

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/usuario/UsuarioAdminController.java`
- `backend/src/main/java/br/com/certame/usuario/dto/CriarUsuarioRequest.java` (referência, criado na tarefa 3.0)
- `backend/src/main/java/br/com/certame/usuario/dto/EditarUsuarioRequest.java` (referência, criado na tarefa 3.0)
- `backend/src/main/java/br/com/certame/comum/ErroApi.java`
- `backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java`
- `backend/src/test/java/br/com/certame/usuario/UsuarioAdminControllerTest.java`
- `TECH-SPEC.md` (§6, confirmar contrato atualizado)
