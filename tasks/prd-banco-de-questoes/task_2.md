# Tarefa 2.0: Migração e persistência (Flyway, entidades e repositórios)

## Visão geral

Cria o modelo que sustenta tudo o mais: a migração `V2__banco_questoes.sql` com as oito tabelas desta funcionalidade (`disciplina`, `assunto`, `banca`, `orgao`, `questao`, `alternativa`, `questao_estatistica`, `auditoria`), a semente de catálogo `V3` e a semente de `dev` `V901`, mais as entidades JPA e os repositórios correspondentes.

Vem antes de qualquer regra de negócio porque o modelo é o que decide o resto: o `check` `questao_publicada_completa` é a rede de segurança de RN-01 e CA-06 no banco, a coluna gerada `busca` (`tsvector`) com índice GIN é o que torna RF-18 viável, e `arquivado_em` é o que sustenta CA-11.

<skills>
### Conformidade com skills

- `313-frameworks-spring-db-migrations-flyway` — SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração de `V1`; `validate-on-migrate` ligado; disciplina forward-only; `spring.flyway.locations` separando produção, `dev` e teste.
- `postgresql-table-design` — PKs `uuid` com `gen_random_uuid()`, `timestamptz` em UTC, `check` para enums textuais, índices únicos parciais compatíveis com exclusão lógica, coluna `tsvector` gerada e `stored` com índice GIN, índice composto alinhado aos filtros da listagem e `check` de invariante de negócio.
- `spring-boot-testing` — `@DataJpaTest` com Testcontainers e PostgreSQL 18 real (`@ServiceConnection`); sem H2 e sem banco em memória, porque `tsvector`, GIN, índice único parcial e o `check` só existem no Postgres.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: regra 5 (migração só por Flyway, versionada e imutável — complementos de catálogo entram sempre como versão nova, nunca editando a `V3` já aplicada) e regra 3 (nomes de tabela e coluna em português sem acento, `snake_case`). Regra 6 respeitada por omissão: nada nesta migração toca em `resposta`. Nenhum desvio.

**Bloqueio conhecido:** `V1__base_acesso.sql` de `prd-acesso-e-conta` precisa existir antes, porque `questao.autor_id` e `auditoria.ator_id` referenciam `usuario(id)`.
</rules>

<requirements>
- **`V2__banco_questoes.sql`** com o esquema exato da TechSpec: `disciplina`, `assunto`, `banca`, `orgao`, a sequência `questao_codigo_seq`, `questao`, `alternativa`, `questao_estatistica` e `auditoria`.
- `codigo` da questão gerado por sequência no formato `Q-#####` (`default 'Q-' || lpad(nextval(...)::text, 5, '0')`).
- Coluna `busca` como `tsvector generated always as (to_tsvector('portuguese', coalesce(enunciado,''))) stored`, com índice GIN.
- Índices `questao_codigo_idx` (`text_pattern_ops`), `questao_filtro_idx` sobre `(status, disciplina_id, banca_id, atualizado_em desc)` e `questao_assunto_idx`, todos com `where excluido_em is null`.
- `check questao_publicada_completa`: uma questão `PUBLICADA` exige `disciplina_id`, `gabarito` e `comentario` não vazio.
- Índices únicos parciais de nome: `disciplina (lower(nome)) where excluido_em is null` e `assunto (disciplina_id, lower(nome)) where excluido_em is null`.
- `alternativa` única por `(questao_id, letra)` e por `(questao_id, ordem)`, apagada em cascata com a questão.
- **`V3__seed_catalogo.sql`** com as bancas recorrentes (Cebraspe, FGV, FCC, Vunesp, Cesgranrio, IBFC, Quadrix, Instituto AOCP) e os órgãos recorrentes — dado de catálogo, entra em todos os ambientes.
- **`db/migration-dev/V901__seed_banco_questoes_dev.sql`** com disciplinas, assuntos e questões de exemplo, restrito ao perfil `dev`.
- Entidades JPA: `Disciplina`, `Assunto`, `Banca`, `Orgao`, `Questao`, `Alternativa`, `QuestaoEstatistica`, `Auditoria`; enums `TipoQuestao` (`ME`/`CE`), `StatusQuestao` (`RASCUNHO`/`EM_REVISAO`/`PUBLICADA`) e `Dificuldade` (`FACIL`/`MEDIA`/`DIFICIL`).
- `Questao` → `Alternativa` como `@OneToMany` com `orphanRemoval`, carregada por `@EntityGraph` (nunca N+1).
- Repositórios: `DisciplinaRepository`, `AssuntoRepository`, `BancaRepository`, `OrgaoRepository`, `QuestaoRepository`, `AlternativaRepository`, `QuestaoEstatisticaRepository`, `AuditoriaRepository`.
- A consulta agregada da árvore (contagem derivada por `group by`, uma só consulta) vive no repositório, não no serviço.
</requirements>

## Subtarefas

- [x] 2.1 Escrever `V2__banco_questoes.sql` com as oito tabelas, a sequência, os índices e os `check`, conforme o SQL da TechSpec.
- [x] 2.2 Escrever `V3__seed_catalogo.sql` com bancas e órgãos — confirmar a lista definitiva com o usuário antes de aplicar.
- [x] 2.3 Escrever `db/migration-dev/V901__seed_banco_questoes_dev.sql` com disciplinas, assuntos e questões de exemplo.
- [x] 2.4 Criar as entidades de `catalogo/`: `Disciplina`, `Assunto`, `Banca`, `Orgao`, com `arquivado_em` e `excluido_em` em disciplina e assunto.
- [x] 2.5 Criar as entidades de `questao/`: `Questao`, `Alternativa`, `QuestaoEstatistica`, e os enums `TipoQuestao`, `StatusQuestao`, `Dificuldade`.
- [x] 2.6 Criar `comum/auditoria/Auditoria` com `payload` `jsonb`.
- [x] 2.7 Criar os repositórios, incluindo a consulta agregada de contagem da árvore e as consultas de verificação de uso (assunto e disciplina em uso, questão com respostas).
- [x] 2.8 Escrever TI-01 e TI-19.

## Detalhes de implementação

Ver TechSpec, seções *Esquema de banco — `V2__banco_questoes.sql`* (SQL completo, notas sobre o `check` e sobre as sementes), *Visão dos componentes* (tabela de componentes do backend) e *Principais decisões* (exclusão lógica em toda parte, `tsvector` gerado e materializado, letras atribuídas pelo servidor, `arquivado_em` em vez de coluna `status`, contagem derivada nunca armazenada).

## Critérios de aceitação relacionados

- CA-01
- CA-06
- CA-10
- CA-11

## Testes da tarefa

### Testes de integração

- [x] TI-01 — Migração `V2` e `V3` em base limpa
- [x] TI-19 — `check` `questao_publicada_completa` (`@DataJpaTest`)

## Arquivos relevantes

- `backend/src/main/resources/db/migration/V2__banco_questoes.sql`
- `backend/src/main/resources/db/migration/V3__seed_catalogo.sql`
- `backend/src/main/resources/db/migration-dev/V901__seed_banco_questoes_dev.sql`
- `backend/src/main/java/br/com/certame/catalogo/Disciplina.java`
- `backend/src/main/java/br/com/certame/catalogo/Assunto.java`
- `backend/src/main/java/br/com/certame/catalogo/Banca.java`
- `backend/src/main/java/br/com/certame/catalogo/Orgao.java`
- `backend/src/main/java/br/com/certame/catalogo/DisciplinaRepository.java`
- `backend/src/main/java/br/com/certame/catalogo/AssuntoRepository.java`
- `backend/src/main/java/br/com/certame/catalogo/BancaRepository.java`
- `backend/src/main/java/br/com/certame/catalogo/OrgaoRepository.java`
- `backend/src/main/java/br/com/certame/questao/Questao.java`
- `backend/src/main/java/br/com/certame/questao/Alternativa.java`
- `backend/src/main/java/br/com/certame/questao/QuestaoEstatistica.java`
- `backend/src/main/java/br/com/certame/questao/TipoQuestao.java`
- `backend/src/main/java/br/com/certame/questao/StatusQuestao.java`
- `backend/src/main/java/br/com/certame/questao/Dificuldade.java`
- `backend/src/main/java/br/com/certame/questao/QuestaoRepository.java`
- `backend/src/main/java/br/com/certame/questao/AlternativaRepository.java`
- `backend/src/main/java/br/com/certame/questao/QuestaoEstatisticaRepository.java`
- `backend/src/main/java/br/com/certame/comum/auditoria/Auditoria.java`
- `backend/src/main/java/br/com/certame/comum/auditoria/AuditoriaRepository.java`
