# Tarefa 1.0: Migração e persistência da sessão de estudo

## Visão geral

Cria o esquema de banco definitivo da sessão de estudo (`sessao_estudo`, `sessao_questao`, `resposta`, `marcacao`) por migração Flyway aditiva, a semente de dados de `dev`, as entidades JPA e os enums correspondentes, e os repositórios de persistência. É a base de tudo: a imutabilidade de `resposta` (RN-02) é uma propriedade do esquema (rules de `update`/`delete`), não da aplicação, e os passos seguintes dependem das tabelas e entidades existirem. Inclui também a renomeação do `RespostaRepository` (`JdbcTemplate`, RN-03) para `RespostaEstatisticaRepository`, liberando o nome para o novo repositório JPA de domínio, e a remoção das migrações de teste provisórias `V990`/`V991`.

<skills>
### Conformidade com skills

- `postgresql-table-design` — PKs `uuid` com `gen_random_uuid()`, `timestamptz` em UTC, enums como `text` + `check`, chave natural composta em `sessao_questao`, índices parciais para o caminho quente, unicidade como restrição de banco (`resposta`, `marcacao`) e `rules` para a imutabilidade de RN-02.
- `313-frameworks-spring-db-migrations-flyway` — SQL versionado em `db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração real (`V7`), migração aditiva (*expand*), `validate-on-migrate` já ligado.
- `spring-boot-testing` — `@DataJpaTest`/Testcontainers para TI-01…TI-06, AssertJ em toda asserção.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `code-standards.md`, `tests.md` e `folder-structure.md` lidos.

- Regra 5 (`AGENTS.md`) — migração só por Flyway, versionada e imutável; `V7` é o próximo número livre em `db/migration`, `V902` em `db/migration-dev`; nenhuma migração aplicada é editada.
- Regra 6 (`AGENTS.md`) — `resposta` imutável: entidade sem *setter* e sem `@PreUpdate`, mais a `rule` de banco `do instead nothing` em `update`/`delete` (não `raise`, pela razão registrada na TechSpec).
- `code-standards.md` §3 — entidades sem Lombok, construtor protegido, `@Enumerated(EnumType.STRING)`, `@EmbeddedId` em `SessaoQuestaoId`.
- `folder-structure.md` §2.1/§2.2 — pacote `br/com/certame/estudo/`, migração em `db/migration/`, seed em `db/migration-dev/`.
</rules>

<requirements>
- RF-06 (suporte), RF-07, RF-08, RF-09, RF-10, RF-11, RF-12
- RN-01, RN-02, RN-03 (repositório renomeado), RN-04
</requirements>

## Subtarefas

- [x] 1.1 Criar `V7__sessao_estudo.sql` com as tabelas `sessao_estudo`, `sessao_questao`, `resposta` (+ rules de imutabilidade), `marcacao` e a coluna `tempo_medio_ms` em `questao_estatistica`, conforme o esquema da TechSpec.
- [x] 1.2 Criar `V902__seed_sessao_estudo_dev.sql` com a massa de teste descrita na TechSpec (autor, estudante, disciplina com dois assuntos, ≥ 60 questões publicadas + 3 não publicadas, sessão interrompida na questão 14 de 40).
- [x] 1.3 Criar as entidades `SessaoEstudo`, `SessaoQuestao`/`SessaoQuestaoId`, `Resposta`, `Marcacao` e os enums `ModoSessao`, `OrigemSessao`, `TipoMarcacao`.
- [x] 1.4 Criar os repositórios `SessaoEstudoRepository`, `SessaoQuestaoRepository`, `RespostaRepository` (JPA, novo), `MarcacaoRepository` e `SerieRepository` (consulta nativa do sorteio).
- [x] 1.5 Renomear o `RespostaRepository` (`JdbcTemplate`) existente para `RespostaEstatisticaRepository`, ajustando os consumidores (`EstatisticaQuestaoService` e `RespostaAgregadoAdapter`).
- [x] 1.6 Adicionar `tempo_medio_ms` em `QuestaoEstatistica`/`EstatisticaQuestao`.
- [x] 1.7 Remover `db/migration-test/V990__resposta_minima.sql` e `V991__sessao_resposta_marcacao_minima.sql` e confirmar que as suítes de F1/F3 seguem passando contra o esquema real.

## Detalhes de implementação

Ver TechSpec, seção "Esquema de banco — `V7__sessao_estudo.sql`" para o DDL completo e as notas sobre `rule` em vez de `trigger` e o `unique (sessao_id, questao_id)`. Ver "Arquivos relevantes e dependentes" para a lista de entidades e repositórios novos.

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-15
- CA-16

## Testes da tarefa

### Testes de integração

- [x] TI-01 — `V7__sessao_estudo.sql` aplica limpo sobre V1…V6
- [x] TI-02 — `resposta` recusa `update`
- [x] TI-03 — `resposta` recusa `delete`
- [x] TI-04 — `unique (sessao_id, questao_id)` impede resposta duplicada
- [x] TI-05 — `unique (usuario_id, questao_id, tipo)` impede marcação duplicada
- [x] TI-06 — `check` recusa tamanho fora de 10/30/50

## Arquivos relevantes

- `backend/src/main/resources/db/migration/V7__sessao_estudo.sql`
- `backend/src/main/resources/db/migration-dev/V902__seed_sessao_estudo_dev.sql`
- `backend/src/main/java/br/com/certame/estudo/SessaoEstudo.java`
- `backend/src/main/java/br/com/certame/estudo/SessaoQuestao.java`
- `backend/src/main/java/br/com/certame/estudo/SessaoQuestaoId.java`
- `backend/src/main/java/br/com/certame/estudo/Resposta.java`
- `backend/src/main/java/br/com/certame/estudo/Marcacao.java`
- `backend/src/main/java/br/com/certame/estudo/TipoMarcacao.java`
- `backend/src/main/java/br/com/certame/estudo/ModoSessao.java`
- `backend/src/main/java/br/com/certame/estudo/OrigemSessao.java`
- `backend/src/main/java/br/com/certame/estudo/SerieRepository.java`
- `backend/src/main/java/br/com/certame/estudo/SessaoEstudoRepository.java`
- `backend/src/main/java/br/com/certame/estudo/SessaoQuestaoRepository.java`
- `backend/src/main/java/br/com/certame/estudo/RespostaRepository.java`
- `backend/src/main/java/br/com/certame/estudo/MarcacaoRepository.java`
- `backend/src/main/java/br/com/certame/estudo/RespostaEstatisticaRepository.java` (renomeado)
- `backend/src/main/java/br/com/certame/questao/EstatisticaQuestaoService.java` (modificado)
- `backend/src/main/java/br/com/certame/questao/QuestaoEstatistica.java` (modificado)
- `backend/src/main/java/br/com/certame/questao/dto/EstatisticaQuestao.java` (modificado)
- `backend/src/test/resources/db/migration-test/V990__resposta_minima.sql` (removido)
- `backend/src/test/resources/db/migration-test/V991__sessao_resposta_marcacao_minima.sql` (removido, se existir)
