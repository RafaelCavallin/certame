# Tarefa 2.0: Migração `V7__editais.sql`, semente de `dev`, entidade e repositório

## Visão geral

Fundar a persistência do agregado: a tabela `edital`, o vínculo N:N `edital_disciplina`, a entidade JPA `Edital`, o enum `StatusEdital` e o `EditalRepository`. É o passo 2 da ordem de construção — antes de qualquer regra, porque o modelo sustenta o resto.

O que a tabela carrega não é decoração: o índice único parcial de nome, o `check edital_aberto_com_prazo` e a PK composta do vínculo são a rede de segurança de CA-04, do caso de borda 2 e de CA-08, e por isso são testados diretamente contra PostgreSQL.

<skills>
### Conformidade com skills

- `313-frameworks-spring-db-migrations-flyway` — SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração; `validate-on-migrate` ligado e `baseline-on-migrate` desligado, como já configurado; disciplina *forward-only*; semente de `dev` isolada por `spring.flyway.locations`.
- `postgresql-table-design` — PK `uuid` com `gen_random_uuid()` e PK composta natural no N:N; `timestamptz` para instantes e `date` para o prazo (dia civil, não instante); `check` para o enum textual, a faixa de vagas, o esquema da URL e a invariante de negócio; índice único parcial compatível com exclusão lógica; índice de filtro alinhado à consulta de agrupamento; `on delete cascade` a partir de `edital` e ausente a partir de `disciplina`.
- `spring-boot-testing` — `@DataJpaTest` com Testcontainers (`@ServiceConnection`) para as *constraints*; sem H2.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 5** — migração só por Flyway, versionada e imutável. **O número `V7` é conferido na entrega** contra o que já existe em `db/migration`: `prd-simulados` é da mesma fase F4 e F2/F3 ainda não foram aplicadas. O mesmo vale para a série `V900+` da semente. Nenhuma migração aplicada é editada; a migração é puramente aditiva.
- **Regra 3** — tabelas e colunas em português sem acento, `snake_case`: `edital`, `edital_disciplina`, `prazo_inscricao`, `url_oficial`, `excluido_em`, `criado_em`, `atualizado_em`.
- `code-standards.md` §3 — entidade sem Lombok: construtor, getters e setters escritos à mão, com setters só no que muda depois de criado; `criado_em`/`atualizado_em` por `@PrePersist`/`@PreUpdate`. Hibernate em `ddl-auto=validate` — o esquema vem só das migrações.
- `code-standards.md` §2 — limites de nome (160), vagas (999999) e URL (500) como constantes nomeadas, nunca literais espalhados.
</rules>

<requirements>
- **RF-22** — persistir status, vagas, banca, prazo e disciplinas do edital.
- Exclusão lógica por `excluido_em`; nenhum `delete` físico.
- Nome único entre não excluídos, ignorando maiúsculas (índice único parcial).
- `check edital_aberto_com_prazo`: `status <> 'ABERTO' or prazo_inscricao is not null`.
- `EditalRepository` com `@EntityGraph` de banca, órgão e disciplinas — uma consulta, sem N+1.
- Semente de `dev` com quatro editais: um por status mais um aberto com prazo vencido, para exercitar a borda 2.
</requirements>

## Subtarefas

- [x] 2.1 Conferir a próxima versão livre em `backend/src/main/resources/db/migration` e em `db/migration-dev` antes de nomear os arquivos.
- [x] 2.2 Escrever `V12__editais.sql` (versão `V7` já ocupada por `V7__sessao_estudo.sql`) com as tabelas `edital` e `edital_disciplina`, os três `check`, o índice único parcial `edital_nome_uk` e os índices `edital_status_idx` e `edital_disciplina_disciplina_idx`.
- [x] 2.3 Escrever `V907__seed_editais_dev.sql` (série `V905` já ocupada) com os quatro editais de exemplo, dependendo das disciplinas e bancas semeadas por `V901`/`V3`.
- [x] 2.4 Criar o enum `StatusEdital` (`ABERTO`, `PREVISTO`, `ENCERRADO`), persistido como texto.
- [x] 2.5 Criar a entidade `Edital` com o `@ManyToMany` para `Disciplina`, as FKs de banca e órgão e os *callbacks* de auditoria temporal.
- [x] 2.6 Criar o `EditalRepository`: consultas por status com `@EntityGraph`, unicidade de nome entre não excluídos e verificação de uso de disciplina.
- [x] 2.7 Escrever TI-01…TI-04 com `@DataJpaTest` e Testcontainers.

## Detalhes de implementação

Ver `techspec.md` §*Modelos de dados* → *Esquema de banco — `V7__editais.sql`* (DDL completo e a justificativa de `edital_aberto_com_prazo`), §*Arquitetura do sistema* → *Visão dos componentes* (linhas de `Edital`, `StatusEdital` e `EditalRepository`) e §*Sequenciamento do desenvolvimento*, passo 2.

## Critérios de aceitação relacionados

- CA-04
- CA-07
- CA-08
- Caso de borda 2 do PRD

## Testes da tarefa

### Testes de integração

- [x] TI-01 — Migração em base limpa (`@DataJpaTest`)
- [x] TI-02 — `check edital_aberto_com_prazo` (`@DataJpaTest`)
- [x] TI-03 — Índice único parcial de nome (`@DataJpaTest`)
- [x] TI-04 — PK composta de `edital_disciplina` (`@DataJpaTest`)

## Arquivos relevantes

- `backend/src/main/resources/db/migration/V12__editais.sql` (novo — `V7` já ocupada por `prd-simulados`)
- `backend/src/main/resources/db/migration-dev/V907__seed_editais_dev.sql` (novo — `V905` já ocupada)
- `backend/src/main/java/br/com/certame/catalogo/edital/Edital.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/edital/StatusEdital.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/edital/EditalRepository.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/Disciplina.java`, `Banca.java`, `Orgao.java` (dependências de `prd-banco-de-questoes`)
- `backend/src/main/resources/db/migration/V2__banco_questoes.sql` (dependência)
