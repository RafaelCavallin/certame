# Tarefa 5.0: Testes de integração backend

## Visão geral

Suíte `*IntegracaoTest` com Testcontainers PostgreSQL 18, provando ponta a ponta a listagem paginada/filtrada, o fluxo completo de convite até a ativação, a edição com revogação de sessões, a exclusão lógica encerrando o acesso e a persistência de auditoria. Fecha a cobertura de integração antes de iniciar o frontend (tarefa 6.0).

<skills>
### Conformidade com skills

- `spring-boot-testing` — Testcontainers, dados isolados por `UUID.randomUUID()`, `@Async` desabilitado nos testes para capturar o token agendado.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lido `AGENTS.md` e as rules em `.agents/rules/` (`code-standards.md`, `tests.md`, `folder-structure.md`).

- Sufixo `*IntegracaoTest`, exige Docker rodando (`.agents/rules/folder-structure.md` §2.3).
- Cobertura mínima 80% (JaCoCo no `verify`); prioridade **Alta** para autorização e salvaguardas com caminho feliz e bordas.
- Dados de teste isolados (e-mail com `UUID.randomUUID()`), sem interferência entre testes (princípio FIRST — Independent).
- Não introduzir migração nova nem alterar `V1`/`V2` — a suíte usa o schema existente.
</rules>

<requirements>
- CA-01, CA-02 — envelope de paginação e filtros corretos ponta a ponta.
- CA-05 — token de convite persistido, e-mail agendado, ativação via `POST /auth/senha/redefinir`.
- CA-06 — alteração de perfil persistida e sessões (refresh tokens) revogadas.
- CA-09 (regressão, RN-06 já implementado em `prd-acesso-e-conta`) — usuário inativo recebe mensagem genérica de credencial inválida no login; validar que a alteração de status feita por esta funcionalidade não quebra esse comportamento existente.
- CA-11 — após exclusão, `GET`/`PATCH`/`DELETE` retornam 404 e login é recusado.
- CA-14 — linha em `auditoria` coerente com ator/ação/entidade/payload.
</requirements>

## Subtarefas

- [x] 5.1 Escrever `TI01_listaUsuariosComFiltroDePerfilEStatus`, cobrindo paginação, filtro por perfil/status e ordenação por `criadoEm`.
- [x] 5.2 Escrever `TI02_criaUsuarioComConviteEFluxoCompletoDeAtivacao`, validando o token `CONVITE` persistido e a ativação via `POST /auth/senha/redefinir`.
- [x] 5.3 Escrever `TI04_editaUsuarioRevogandoRefreshTokens`, validando persistência da alteração e revogação de sessões.
- [x] 5.4 Escrever `TI05_excluindoUsuarioOracleEncerraAcesso`, validando 404 pós-exclusão em `GET`/`PATCH`/`DELETE` e login recusado.
- [x] 5.5 Escrever `TI06_auditoriaPersistidaComAtorEEntidade`, validando a linha de `auditoria` para as cinco operações.
- [x] 5.6 Validar, como regressão, que um usuário editado para `INATIVO` por esta funcionalidade recebe a mensagem genérica de credencial inválida no login existente (CA-09/RN-06), sem introduzir um novo caso de teste na TechSpec.

## Detalhes de implementação

Ver `techspec.md`, seção "Testes de integração" (tabela TI-01…TI-07) e "Pontos de integração" (envio `@Async` de e-mail, `RegistradorAuditoria`).

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01
- CA-02
- CA-05
- CA-06
- CA-09
- CA-11
- CA-14

## Testes da tarefa

### Testes de integração

- [x] TI-01 — `TI01_listaUsuariosComFiltroDePerfilEStatus`
- [x] TI-02 — `TI02_criaUsuarioComConviteEFluxoCompletoDeAtivacao`
- [x] TI-04 — `TI04_editaUsuarioRevogandoRefreshTokens`
- [x] TI-05 — `TI05_excluindoUsuarioOracleEncerraAcesso`
- [x] TI-06 — `TI06_auditoriaPersistidaComAtorEEntidade`

## Arquivos relevantes

- `backend/src/test/java/br/com/certame/usuario/UsuarioAdminIntegracaoTest.java`
- `backend/src/main/java/br/com/certame/auth/RefreshTokenService.java` (referência)
- `backend/src/main/java/br/com/certame/comum/auditoria/` (referência, `RegistradorAuditoria`)
- `backend/src/main/java/br/com/certame/auth/AutenticacaoService.java` (referência, regressão CA-09)
