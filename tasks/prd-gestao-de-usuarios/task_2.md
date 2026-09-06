# Tarefa 2.0: Domínio e repositório (Usuario, UsuarioRepository, TokenSenhaRepository)

## Visão geral

Base de dados e domínio para as regras de gestão de usuários: métodos de domínio `editar`/`excluir` em `Usuario` (hoje os setters só cobrem senha/status/exclusão), listagem filtrada/paginada e contador de super-admin ativo em `UsuarioRepository`, e a alteração de assinatura de `TokenSenhaRepository.invalidarPendentes` para receber a finalidade (`FinalidadeTokenSenha`). Não há migração de banco nova — `usuario` (V1) e `auditoria` (V2) já suportam tudo.

<skills>
### Conformidade com skills

- `postgresql-table-design` — reuso do schema existente sem migração nova; validar que a Specification e os índices/consultas propostos são compatíveis com o schema atual de `usuario`.
- `spring-boot-testing` — estratégia de teste de persistência (`*PersistenciaTest`) com Testcontainers ou slice de dados, conforme convenção do projeto.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lido `AGENTS.md` e as rules em `.agents/rules/` (`code-standards.md`, `tests.md`, `folder-structure.md`).

- **Regra 5 (AGENTS.md)** — nenhuma migração nova é necessária; não editar migrações já aplicadas. Confirmar que `V1__base_acesso.sql` já cobre os campos usados.
- **Regra 3 (AGENTS.md)** — nomes de classe/método em português, sem acento onde aplicável ao banco (`snake_case` já em vigor no schema).
- Camadas obrigatórias (`.agents/rules/code-standards.md` via `folder-structure.md` §2.1): Repository não contém regra de negócio — as salvaguardas ficam no serviço (tarefa 3.0).
- Sufixo de teste `*PersistenciaTest` para a camada de dados (`.agents/rules/folder-structure.md` §2.3).
</rules>

<requirements>
- RF-25 — suporte de domínio para editar perfil/status de um usuário existente.
- RF-26 — suporte de domínio para exclusão lógica (`excluidoEm`).
- Techspec, "Design de implementação" — `UsuarioRepository` deve expor `findAll(Specification, Pageable)`, `countByPerfilAndStatusAndExcluidoEmIsNull` e `findByIdAndExcluidoEmIsNull`; `TokenSenhaRepository.invalidarPendentes` deve receber `FinalidadeTokenSenha`.
</requirements>

## Subtarefas

- [x] 2.1 Adicionar `Usuario.editar(nome, email, perfil, status)` e `Usuario.excluir()` (preenche `excluidoEm`), preservando as invariantes existentes da entidade.
- [x] 2.2 Fazer `UsuarioRepository` estender `JpaSpecificationExecutor<Usuario>` e adicionar `countByPerfilAndStatusAndExcluidoEmIsNull` e `findByIdAndExcluidoEmIsNull`.
- [x] 2.3 Criar a `Specification<Usuario>` de filtro por perfil/status e exclusão de registros com `excluidoEm` preenchido, usada pela listagem.
- [x] 2.4 Alterar `TokenSenhaRepository.invalidarPendentes` para receber `FinalidadeTokenSenha` e ajustar os chamadores existentes (fluxo de `prd-acesso-e-conta`).

## Detalhes de implementação

Ver `techspec.md`, seções "Visão dos componentes", "Principais interfaces" (bloco `UsuarioRepository`/`Usuario`) e "Riscos conhecidos" (refactor de `invalidarPendentes` impacta um método interno em produção).

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01
- CA-02
- CA-06
- CA-08
- CA-11

## Testes da tarefa

- [x] Não há casos de teste unitário/integração dedicados a esta camada na TechSpec (os TU/TI cobrem o serviço e o controller nas tarefas 3.0/5.0); cobrir `Usuario.editar/excluir` e as novas consultas com `*PersistenciaTest`, mantendo a cobertura mínima de 80% e os princípios FIRST/AAA de `.agents/rules/tests.md`. Implementado em `UsuarioPersistenciaTest` (7/7 passando, Testcontainers PostgreSQL 18).

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/usuario/Usuario.java`
- `backend/src/main/java/br/com/certame/usuario/UsuarioRepository.java`
- `backend/src/main/java/br/com/certame/usuario/PerfilUsuario.java` (referência)
- `backend/src/main/java/br/com/certame/usuario/StatusUsuario.java` (referência)
- `backend/src/main/java/br/com/certame/auth/TokenSenhaRepository.java`
- `backend/src/main/java/br/com/certame/auth/FinalidadeTokenSenha.java` (referência)
- `backend/src/test/java/br/com/certame/usuario/UsuarioPersistenciaTest.java`
