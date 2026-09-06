# Tarefa 2.0: Migração e persistência (Flyway, entidades e repositórios)

## Visão geral

Cria o modelo de dados da funcionalidade: a migração `V1__base_acesso.sql` com as tabelas `usuario`, `refresh_token` e `token_senha`, as entidades JPA correspondentes, os enums de perfil e status, e os repositórios. É a base de todos os serviços das tarefas seguintes.

<skills>
### Conformidade com skills

- `313-frameworks-spring-db-migrations-flyway` — SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, `validate-on-migrate` ligado, `baseline-on-migrate` desligado (base nova), disciplina forward-only.
- `postgresql-table-design` — PKs `uuid` com `gen_random_uuid()`, `timestamptz` em UTC, `check` para enums textuais, índice único parcial para e-mail (compatível com exclusão lógica) e índices parciais nos tokens ativos.
- `spring-boot-testing` — `@DataJpaTest` com Testcontainers para os testes de repositório e de migração; sem H2.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/`. Regras relevantes: regra 5 (migração só por Flyway, versionada e imutável — nunca editar migração já aplicada; a semente de `dev` fica isolada em `db/migration-dev`) e regra 3 (nomes de tabela e coluna em português sem acento, `snake_case`). Nenhum desvio.
</rules>

<requirements>
- Criar `V1__base_acesso.sql` exatamente com o DDL da TechSpec (três tabelas, constraints `check`, índice único parcial `usuario_email_uk` e índices parciais de tokens ativos).
- `senha_hash` nulo enquanto o status for `CONVITE_PENDENTE` (RN-05).
- Criar as entidades `Usuario`, `RefreshToken` e `TokenSenha` e os enums `PerfilUsuario` e `StatusUsuario`.
- Criar `UsuarioRepository` com `findByEmailAndExcluidoEmIsNull` e `existsByEmail`, além de `RefreshTokenRepository` e `TokenSenhaRepository`.
- Criar a semente de desenvolvimento `V900__seed_acesso_dev.sql` em `db/migration-dev`, com um usuário por perfil, carregada apenas no perfil `dev`.
</requirements>

## Subtarefas

- [x] 2.1 Escrever `backend/src/main/resources/db/migration/V1__base_acesso.sql` com o DDL das três tabelas.
- [x] 2.2 Criar os enums `PerfilUsuario` (`ESTUDANTE`, `PROFESSOR`, `ADMIN`, `SUPER_ADMIN`) e `StatusUsuario` (`ATIVO`, `INATIVO`, `CONVITE_PENDENTE`).
- [x] 2.3 Criar a entidade `Usuario` mapeando todas as colunas, incluindo `excluido_em` para exclusão lógica.
- [x] 2.4 Criar as entidades `RefreshToken` e `TokenSenha` (apenas o hash SHA-256 é persistido; `finalidade` contempla `REDEFINICAO` e `CONVITE`).
- [x] 2.5 Criar `UsuarioRepository`, `RefreshTokenRepository` e `TokenSenhaRepository`.
- [x] 2.6 Criar `db/migration-dev/V900__seed_acesso_dev.sql` com um usuário por perfil e senha conhecida.
- [x] 2.7 Escrever os testes TI-14 e TI-15 com `@DataJpaTest` + Testcontainers sobre PostgreSQL 18 real.

## Detalhes de implementação

Ver TechSpec, seções *Modelos de dados → Esquema de banco — `V1__base_acesso.sql`* (DDL completo e observações por tabela) e *Sequenciamento do desenvolvimento → Ordem de construção*, item 2.

## Critérios de aceitação relacionados

- CA-05
- CA-10
- CA-11

## Testes da tarefa

### Testes de integração

- [x] TI-14 — Persistência e unicidade em `usuario` (`@DataJpaTest` + Testcontainers)
- [x] TI-15 — Migração Flyway em base limpa

## Arquivos relevantes

- `backend/src/main/resources/db/migration/V1__base_acesso.sql`
- `backend/src/main/resources/db/migration-dev/V900__seed_acesso_dev.sql`
- `backend/src/main/java/br/com/certame/usuario/Usuario.java`
- `backend/src/main/java/br/com/certame/usuario/PerfilUsuario.java`
- `backend/src/main/java/br/com/certame/usuario/StatusUsuario.java`
- `backend/src/main/java/br/com/certame/usuario/UsuarioRepository.java`
- `backend/src/main/java/br/com/certame/auth/RefreshToken.java`
- `backend/src/main/java/br/com/certame/auth/TokenSenha.java`
- `backend/src/main/java/br/com/certame/auth/RefreshTokenRepository.java`
- `backend/src/main/java/br/com/certame/auth/TokenSenhaRepository.java`
