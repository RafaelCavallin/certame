# Tarefa 2.0: Migração `V6__cadernos_desempenho.sql`, entidades e repositórios

## Visão geral

Fundação de dados da funcionalidade: seis tabelas novas — `caderno`, `caderno_questao` e os quatro objetos de desempenho (`desempenho_semanal`, `desempenho_disciplina`, `desempenho_assunto`, `desempenho_pendencia`) — mais o índice aditivo `resposta_usuario_questao_idx`. Junto vêm as entidades JPA e os repositórios escopados por `usuario_id`. Vem antes de qualquer regra porque o modelo sustenta o resto, e é onde as invariantes que impedem agregado impossível (`acertos <= volume`) passam a ser garantidas pelo banco.

<skills>
### Conformidade com skills

- `313-frameworks-spring-db-migrations-flyway` — SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração; `validate-on-migrate` ligado; disciplina *forward-only*; `spring.flyway.locations` separando produção, `dev` e teste; migração aditiva e retrocompatível.
- `postgresql-table-design` — PKs `uuid` com `gen_random_uuid()` e PKs compostas naturais nos agregados; `timestamptz` para instantes e `date` para a chave de semana; `check` de invariante e de faixa; índice único parcial compatível com exclusão lógica; índice parcial alinhado à consulta de pontos fracos.
- `spring-boot-testing` — `@DataJpaTest` com Testcontainers para *constraints* e índices; contexto compartilhado para manter a suíte rápida.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 5 (inegociável):** migração só por Flyway, versionada e **imutável**. `V5` já é usado pela correção do código de questão; esta entrega usa a próxima versão livre, `V6`.
- **Regra 3:** tabelas e colunas em português sem acento, `snake_case` (`pct_acerto`, `atualizado_em`, `excluido_em`, `adicionada_em`).
- **`code-standards.md` §3:** entidades com construtores e getters escritos à mão, sem Lombok; `ddl-auto` permanece em `validate` — o esquema vem só da migração.
- **`tests.md` §4:** Testcontainers apenas em `*IntegracaoTest`; nada de subir contexto para o que cabe em unidade.
- A migração de teste `db/migration-test/V991` sustenta `sessao_estudo`, `resposta` e `marcacao` enquanto F2 não existe, e fica **fora do classpath de produção**.
</rules>

<requirements>
- **RF-13** — `caderno` sem coluna `tipo`: todo caderno em tabela é manual; automático é projeção de `marcacao`.
- **RF-15, RF-16** — três tabelas de agregado, não *materialized views* (decisão registrada na TechSpec).
- `caderno.nome` único por usuário entre os não excluídos, sem diferenciar maiúsculas.
- `caderno_questao` com PK composta `(caderno_id, questao_id)` e cascata a partir do caderno.
- `on delete cascade` a partir de `usuario` nos quatro objetos de desempenho (LGPD); `caderno` mantém FK sem cascata, porque a exclusão de usuário é lógica.
- `check (acertos <= volume)` e `check (pct_acerto between 0 and 100)` nos três agregados.
- `desempenho_assunto_fraco_idx` parcial em `volume >= 20`.
- `resposta_usuario_questao_idx` criado com `if not exists`, para que F2 possa trazê-lo na origem sem conflito.
</requirements>

## Subtarefas

- [x] 2.1 Escrever `V6__cadernos_desempenho.sql` com as seis tabelas, *constraints*, índices e o índice aditivo em `resposta`, exatamente como o DDL da TechSpec.
- [x] 2.2 Criar `db/migration-test/V991` com `sessao_estudo`, `resposta` e `marcacao` mínimos, e apontar `spring.flyway.locations` do perfil de teste para ela.
- [x] 2.3 Entidades `Caderno`, `CadernoQuestao` e `CadernoQuestaoId` em `estudo/caderno/`.
- [x] 2.4 Entidades `DesempenhoSemanal`, `DesempenhoDisciplina`, `DesempenhoAssunto` e `DesempenhoPendencia` em `desempenho/`, com PK composta.
- [x] 2.5 `CadernoRepository` e `CadernoQuestaoRepository`, escopados por `usuario_id`, com contagem de questões e de respondidas em consulta agregada — nunca N+1.
- [x] 2.6 Repositórios dos quatro objetos de desempenho, com `ORDER BY` explícito nas consultas de listagem.
- [x] 2.7 Confirmar que `ddl-auto=validate` aceita o esquema e que a numeração da migração não colide com a de F2.

## Detalhes de implementação

Ver `techspec.md`, §*Modelos de dados* → *Esquema de banco — `V6__cadernos_desempenho.sql`* (DDL completo e as três notas sobre invariante, índice parcial e índice aditivo) e §*Arquitetura do sistema* → *Backend* (linhas de `Caderno`, `CadernoQuestao`, os repositórios e as três entidades de agregado). O motivo de haver três agregados e não um está em §*Principais decisões*.

## Critérios de aceitação relacionados

- CA-01
- CA-04
- CA-06

## Testes da tarefa

### Testes de integração

- [x] TI-01 — Migração `V6` aplica sobre a base das versões anteriores
- [x] TI-02 — Índice único de nome por usuário
- [x] TI-03 — PK composta de `caderno_questao`
- [x] TI-04 — Excluir caderno não toca em `resposta` nem em `marcacao`
- [x] TI-05 — `check acertos <= volume` recusa agregado inconsistente

## Arquivos relevantes

- `backend/src/main/resources/db/migration/V6__cadernos_desempenho.sql`
- `backend/src/main/resources/db/migration-test/V991__*.sql`
- `backend/src/main/java/br/com/certame/estudo/caderno/Caderno.java`, `CadernoQuestao.java`, `CadernoQuestaoId.java`, `CadernoRepository.java`, `CadernoQuestaoRepository.java`
- `backend/src/main/java/br/com/certame/desempenho/DesempenhoSemanal.java`, `DesempenhoDisciplina.java`, `DesempenhoAssunto.java`, `DesempenhoPendencia.java` e respectivos repositórios
- `backend/src/main/resources/application*.properties` (`spring.flyway.locations`)
- `backend/src/test/java/br/com/certame/...` — `*IntegracaoTest` de TI-01…TI-05
