# Tarefa 8.0: Estatística de acerto por questão (RN-03)

## Visão geral

RN-03 é a única regra desta funcionalidade **sem superfície de interface**: `questao_estatistica` e a consulta que exclui as respostas do próprio autor nascem aqui, junto do requisito, mas quem as exibe é `prd-sessao-de-estudo` (RF-08). Adiar o esquema faria a regra ser reinventada por outra funcionalidade — decisão registrada na TechSpec.

Esta tarefa também cria a migração de teste que destrava TI-15, TI-16 e TI-27, que dependem de `sessao_estudo` e `resposta` — tabelas que ainda não existem porque pertencem à F2.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito puros no `EstatisticaQuestaoService`, com o repositório de resposta mockado; `@SpringBootTest` com Testcontainers em TI-27, sobre a migração de teste.
- `313-frameworks-spring-db-migrations-flyway` — a migração de teste vive em `db/migration-test/`, fora do `classpath` de produção, servida pelo `spring.flyway.locations` do perfil de teste; nenhum efeito em `dev`, `staging` ou `prod`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: regra 5 (a migração de teste é versionada e isolada — `V990`, restrita ao `classpath` de teste, e será **removida** quando `prd-sessao-de-estudo` criar as tabelas definitivas), regra 6 (`resposta` é imutável — este serviço apenas **lê**) e regra 3 (`EstatisticaQuestaoService`, colunas `total_respostas`, `total_acertos`, `pct_acerto`). Nenhum desvio.

**Dívida registrada:** quando `prd-sessao-de-estudo` criar `sessao_estudo` e `resposta` de verdade, `V990__resposta_minima.sql` é removida na mesma entrega e os testes passam a usar as tabelas reais — item explícito da tarefa de F2.
</rules>

<requirements>
- **RN-03** — O percentual de acerto exibido para uma questão é recalculado a partir de **todas as respostas válidas, excluindo as respostas do próprio autor da questão**.
- **`EstatisticaQuestaoService.recalcular(UUID questaoId) -> EstatisticaQuestao`** — as respostas cujo `usuario_id` é igual ao `autor_id` da questão ficam fora **do numerador e do denominador** (CA-09).
- Sem respostas de terceiros: `totalRespostas` zero e `pctAcerto` `null`, **sem divisão por zero**.
- O resultado é persistido em `questao_estatistica` (`total_respostas`, `total_acertos`, `pct_acerto`, `atualizado_em`).
- **`db/migration-test/V990__resposta_minima.sql`** — versão mínima de `sessao_estudo` e `resposta`, restrita ao `classpath` de teste, suficiente para TI-15, TI-16 e TI-27.
- O serviço **nunca escreve** em `resposta`.
</requirements>

## Subtarefas

- [x] 8.1 Escrever `backend/src/test/resources/db/migration-test/V990__resposta_minima.sql` com o mínimo de `sessao_estudo` e `resposta`, e configurar o `spring.flyway.locations` do perfil de teste para incluí-la.
- [x] 8.2 Criar o DTO `EstatisticaQuestao` conforme a tabela da TechSpec.
- [x] 8.3 Implementar a consulta de agregação que exclui as respostas cujo autor é o autor da questão.
- [x] 8.4 Implementar `EstatisticaQuestaoService.recalcular`, persistindo em `questao_estatistica` com o `Clock` injetado, com TU-28 e TU-29.
- [x] 8.5 Escrever o teste de integração TI-27.
- [x] 8.6 Registrar em `techspec.md` (ou no item de F2 correspondente) a remoção da `V990` quando as tabelas reais existirem.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`EstatisticaQuestaoService`), *Modelos de dados* (`EstatisticaQuestao` e a tabela `questao_estatistica` do esquema), *Abordagem de testes* (a nota sobre TI-15, TI-16 e TI-27 dependerem de `prd-sessao-de-estudo`), *Principais decisões* (RN-03 especificado e testado, sem interface) e *Riscos conhecidos* (a migração de teste e sua remoção).

## Critérios de aceitação relacionados

- CA-09

## Testes da tarefa

### Testes de unidade

- [x] TU-28 — `EstatisticaQuestaoService.recalcular` excluindo o autor
- [x] TU-29 — `EstatisticaQuestaoService.recalcular` sem respostas de terceiros

### Testes de integração

- [x] TI-27 — Recalcular estatística com respostas do autor e de terceiros

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/questao/EstatisticaQuestaoService.java`
- `backend/src/main/java/br/com/certame/questao/QuestaoEstatisticaRepository.java`
- `backend/src/main/java/br/com/certame/questao/dto/EstatisticaQuestao.java`
- `backend/src/test/resources/db/migration-test/V990__resposta_minima.sql`
