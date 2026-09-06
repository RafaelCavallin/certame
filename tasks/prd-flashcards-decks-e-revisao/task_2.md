# Tarefa 2.0: Migração e persistência

## Visão geral

Cria a base de dados e as entidades que sustentam o agendamento por FSRS com memória de curto prazo: a tabela `flashcard_sessao`, as três colunas novas em `flashcard` (`proxima_revisao_em`, `passo_aprendizado`, com o `check` de coerência com a coluna `date` já existente) e as três colunas novas em `flashcard_revisao` (`sessao_id`, `intervalo_minutos`, `segundos_decorridos`). É o pré-requisito de todo o resto: o agendador de curto prazo pressupõe `proxima_revisao_em`.

<skills>
### Conformidade com skills

- `313-frameworks-spring-db-migrations-flyway` — migração versionada em `db/migration`, no padrão `V<n>__<assunto>.sql`, aditiva e imutável; seed em `db/migration-dev/V9xx__*.sql`.
- `postgresql-table-design` — PK `uuid` com `gen_random_uuid()`, `timestamptz` para instantes, `check` para os três enums/invariantes, índice parcial `where status = 'ATIVO'`, índice de cobertura com `include` para a consulta de consumo diário, expressão de fuso imutável no `check`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/tests.md` e `.agents/rules/folder-structure.md`.

- **Regra 5 (AGENTS.md)** — migração só por Flyway, versionada e imutável; nunca editar `V8` (F6a) nem a semente `V906`. O número final de `V9` e da série `V900+` é confirmado na entrega, conforme a nota da techspec.
- **Regra 3** — nomes de tabela e coluna em português sem acento, `snake_case`.
- `code-standards.md` — entidades sem Lombok, construtor protegido, *setters* só no que muda (`agendarPara(Instant)` como único ponto que escreve as duas colunas de agenda).
</rules>

<requirements>
- RN-08 — suporte de esquema ao agendamento FSRS-5 com memória de curto prazo.
- Restrição técnica do PRD — log imutável de revisões (nota, dias decorridos, estabilidade e dificuldade antes/depois, intervalo, tempo).
</requirements>

## Subtarefas

- [x] 2.1 Criar `backend/src/main/resources/db/migration/V14__flashcards_revisao.sql` (número real confirmado na entrega: `V9`-`V13` já estavam ocupados por F2/F4/F5/F6a, então esta migração assume `V14`, a próxima livre): tabela `flashcard_sessao` com os `check`s de escopo e de contagem; colunas novas em `flashcard` (`proxima_revisao_em`, `passo_aprendizado`, novo `default` em `proxima_revisao`) com o `update` de preenchimento seguido do `check` `flashcard_agenda_coerente`; colunas novas em `flashcard_revisao` (`sessao_id` anulável, `intervalo_minutos`, `segundos_decorridos`); índices `flashcard_sessao_idx`, `flashcard_sessao_dia_idx`, `flashcard_fila_hora_idx`, `flashcard_revisao_sessao_idx`, `flashcard_revisao_dia_idx` (com `include`). Divergência necessária do SQL da techspec: como a semente `V908__seed_flashcards_dev.sql` (real F6a, imutável) já grava `flashcard.proxima_revisao` com datas variadas e roda **depois** desta migração, um `default` estático para `proxima_revisao_em` não bastava — a coluna nasce anulável e um gatilho `flashcard_preenche_agenda` (`before insert`) preenche `proxima_revisao_em` a partir de `proxima_revisao` só quando o autor da linha não informa a coluna nova, antes do `not null` ser fixado; `agendarPara(Instant)` continua sendo o único ponto de escrita para inserções/atualizações novas.
- [x] 2.2 Criar a semente `backend/src/main/resources/db/migration-dev/V909__seed_revisao_dev.sql` (número real: `V900`-`V908` já ocupados; próxima livre da série `V900+`): duas sessões finalizadas (ontem e hoje) com revisões ligadas por `sessao_id`, e um deck ("Revisao Acumulada", limite 5) com 12 cards vencidos — mais que o dobro do limite diário (para CA-18 ficar visível sem massa manual).
- [x] 2.3 Criar a entidade `flashcard/revisao/SessaoRevisao.java` e `SessaoRevisaoRepository.java`.
- [x] 2.4 Criar a entidade `flashcard/revisao/FlashcardRevisao.java` (sem *setter* algum além do construtor) e `FlashcardRevisaoRepository.java`.
- [x] 2.5 Estender `flashcard/Flashcard.java` com `proximaRevisaoEm`, `passoAprendizado` e o método `agendarPara(Instant)`, único ponto que grava as duas colunas de agenda. `FlashcardService.criar` foi ajustado para chamar `agendarPara(Instant.now(clock))` no lugar do antigo `setProximaRevisao(LocalDate.now(clock))` (removido), eliminando a divergência de fuso que o `check` agora barra.

## Detalhes de implementação

Ver `techspec.md`, seção "Esquema de banco — `V9__flashcards_revisao.sql`" (com o SQL completo e as notas sobre `flashcard_agenda_coerente`, a zona no SQL, `sessao_id` anulável e o índice com `include`).

## Critérios de aceitação relacionados

- CA-07, CA-08, CA-09 (dependem da coluna `proxima_revisao_em`)

## Testes da tarefa

### Testes de integração

- [x] TI-01 — Migração preenche `proxima_revisao_em` coerente com `proxima_revisao`
- [x] TI-02 — `check` `flashcard_agenda_coerente` recusa divergência
- [x] TI-03 — `check` de escopo de `flashcard_sessao`
- [x] TI-04 — Exclusão lógica de card preserva o log de revisões

## Arquivos relevantes

- `backend/src/main/resources/db/migration/V14__flashcards_revisao.sql` (número real; techspec previa `V9`)
- `backend/src/main/resources/db/migration-dev/V909__seed_revisao_dev.sql` (número real; techspec previa `V907`)
- `flashcard/revisao/SessaoRevisao.java`
- `flashcard/revisao/SessaoRevisaoRepository.java`
- `flashcard/revisao/FlashcardRevisao.java`
- `flashcard/revisao/FlashcardRevisaoRepository.java`
- `flashcard/Flashcard.java` (modificado)
- `flashcard/FlashcardService.java` (modificado: `criar` passa a usar `agendarPara`)
- `flashcard/FlashcardControllerIntegracaoTest.java` (modificado: helper `cartaoAtivo` passou a gravar `proxima_revisao_em` coerente)
- `flashcard/revisao/FlashcardRevisaoEsquemaIntegracaoTest.java` (novo: TI-01 a TI-04)
