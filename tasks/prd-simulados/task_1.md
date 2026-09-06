# Tarefa 1.0: Migração `V9__simulado.sql` + entidades e repositórios

## Visão geral

Cria o esquema de banco do domínio `simulado`: as quatro tabelas novas (`simulado`, `simulado_composicao`, `simulado_questao`, `tentativa_simulado`), o gatilho que congela `tentativa_simulado` finalizada, e o ajuste aditivo em `sessao_estudo`/`sessao_questao` para acomodar provas de até 200 questões. Cria as entidades JPA, enums e repositórios Spring Data correspondentes, sem nenhuma regra de negócio ainda.

<skills>
### Conformidade com skills

- `313-frameworks-spring-db-migrations-flyway` — migração versionada e imutável em `db/migration`, seed de `dev` isolado na série `V900+`, `alter table` em `sessao_estudo` com `drop constraint if exists` para ser idempotente entre a ordem possível de F3 e F4.
- `postgresql-table-design` — PK `uuid` com `gen_random_uuid()`, `timestamptz` com `default now()`, enum como `text` + `check`, índice único parcial para exclusão lógica, chave composta natural em `simulado_composicao` e `simulado_questao`, `on delete cascade` só nas filhas cujo ciclo de vida é o do pai.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Leitura de `AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/folder-structure.md` e `.agents/rules/tests.md` confirmada. Aplicam-se a esta tarefa: regra 5 (migração só por Flyway, versionada e imutável, nunca editar migração aplicada), regra 3 (tabelas/colunas em português sem acento, `snake_case`); entidade JPA sem Lombok, construtor protegido + construtor público, setters só no que muda, `criado_em`/`atualizado_em` por `@PrePersist`/`@PreUpdate`, exclusão lógica via `excluido_em`; enum persistido como texto com `@Enumerated(EnumType.STRING)` espelhando o `check` da migração; Hibernate em `ddl-auto=validate` exige entidade e migração coerentes desde a subida. Pacote `br.com.certame.simulado/`, sem acesso a repositório de outro domínio.

Se `prd-sessao-de-estudo` (F3) ainda não tiver sido implementada nesta base de código, o `alter table sessao_estudo`/`sessao_questao` desta migração deve ser omitido — a V7 nasce diretamente com o `check` amplo. Confirme o estado de F3 antes de escrever o bloco de `alter`.
</rules>

<requirements>
- RF-21 (dados que sustentam o CRUD administrativo)
- RN-01 (só questões publicadas compõem a série — sustentado pela FK de `simulado_questao` para `questao`)
- RN-04 (respostas só contam após finalização — sustentado pela imutabilidade de `tentativa_simulado` finalizada)
</requirements>

## Subtarefas

- [x] 1.1 Criar `backend/src/main/resources/db/migration/V9__simulado.sql` com as tabelas `simulado`, `simulado_composicao`, `simulado_questao`, `tentativa_simulado`, os índices (`simulado_codigo_idx`, `simulado_janela_idx`, `tentativa_valida_unica_idx`, `tentativa_aberta_unica_idx`, `tentativa_historico_idx`), o gatilho `tentativa_simulado_congelada` e o bloco de `alter` em `sessao_estudo`/`sessao_questao` (condicional ao estado de F3), conforme o esquema definido na TechSpec.
- [x] 1.2 Criar `backend/src/main/resources/db/migration-dev/V904__seed_simulados_dev.sql` com massa mínima de dev (ao menos um simulado publicado e um em rascunho, cobrindo disciplinas com acervo publicado).
- [x] 1.3 Criar as entidades JPA em `br.com.certame.simulado`: `Simulado`, `SimuladoComposicao` (+ `SimuladoComposicaoId`), `SimuladoQuestao` (+ `SimuladoQuestaoId`), `TentativaSimulado`.
- [x] 1.4 Criar os enums `StatusSimulado`, `EstadoSimulado`, `TipoTentativa`, `StatusTentativa`.
- [x] 1.5 Criar os repositórios `SimuladoRepository`, `SimuladoComposicaoRepository`, `SimuladoQuestaoRepository`, `TentativaSimuladoRepository`.
- [x] 1.6 Subir a aplicação com o perfil `dev` e confirmar que o Hibernate valida o esquema sem divergência.

## Detalhes de implementação

Ver TechSpec, seção "Esquema de banco — `V9__simulado.sql`" (com o SQL completo e os blockquotes explicando o gatilho e o índice de tentativa aberta) e "Arquivos relevantes e dependentes" (lista exata dos arquivos novos do domínio `simulado`).

## Critérios de aceitação relacionados

- CA-04
- CA-06
- CA-07
- CA-11

## Testes da tarefa

Nenhum caso de teste específico de unidade/integração/E2E da TechSpec é atribuído a esta tarefa — ela é fundação de esquema. Validar via `FlywayMigracaoTest` (suíte já existente, cobre a aplicação limpa das migrações) e a subida da aplicação em `ddl-auto=validate`.

### Validações realizadas

- [x] `./mvnw test -Dtest=FlywayMigracaoTest`
- [x] Aplicação com perfil `dev` em `3000`, Flyway até `V904` e Hibernate em `ddl-auto=validate`.
- [ ] `./mvnw clean verify` bloqueado por outro processo Maven concorrente usando `backend/target`.

## Arquivos relevantes

- `backend/src/main/resources/db/migration/V9__simulado.sql`
- `backend/src/main/resources/db/migration-dev/V904__seed_simulados_dev.sql`
- `backend/src/main/java/br/com/certame/simulado/Simulado.java`
- `backend/src/main/java/br/com/certame/simulado/SimuladoComposicao.java`
- `backend/src/main/java/br/com/certame/simulado/SimuladoComposicaoId.java`
- `backend/src/main/java/br/com/certame/simulado/SimuladoQuestao.java`
- `backend/src/main/java/br/com/certame/simulado/SimuladoQuestaoId.java`
- `backend/src/main/java/br/com/certame/simulado/TentativaSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/StatusSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/EstadoSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/TipoTentativa.java`
- `backend/src/main/java/br/com/certame/simulado/StatusTentativa.java`
- `backend/src/main/java/br/com/certame/simulado/SimuladoRepository.java`
- `backend/src/main/java/br/com/certame/simulado/SimuladoComposicaoRepository.java`
- `backend/src/main/java/br/com/certame/simulado/SimuladoQuestaoRepository.java`
- `backend/src/main/java/br/com/certame/simulado/TentativaSimuladoRepository.java`
- `backend/src/main/resources/db/migration/V7__sessao_estudo.sql` (só se F3 ainda não implementada)
