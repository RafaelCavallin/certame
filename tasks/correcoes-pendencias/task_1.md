# Tarefa 1.0 (P0-1): Destravar o build — alinhar `FlywayMigracaoTest` ao contrato vigente da `V15`

## Visão geral

`FlywayMigracaoTest.TI06_tamanhoDeSessaoForaDoDominioEhRecusado` afirma que
`sessao_estudo.tamanho = 40` deve ser recusado para sessão de origem `LIVRE`. A migração
`V15__sessao_livre_tamanho_variavel.sql` — criada deliberadamente por `prd-sessao-de-estudo` para
corrigir um defeito real de produção (sessão com acervo menor que o tier pedido quebrava ao salvar)
— passou a aceitar `tamanho between 1 and 50` para `LIVRE`. **O teste ficou desatualizado, a
migração está correta.**

É a única falha do backend (`535 testes, 1 falha`), mas o `surefire` interrompe o build antes do
`jacoco:check` rodar. Enquanto ela existir, **nenhuma** funcionalidade do projeto consegue comprovar
o piso de 80% de cobertura exigido por `.agents/rules/tests.md`. Esta é a tarefa que destrava a
verificação de todo o resto — deve ser a primeira a ser executada.

<rules>
### Conformidade com o AGENTS.md e as rules

- **Regra 5 (AGENTS.md)** — migração de banco só por Flyway, versionada e imutável; nunca editar uma
  migração já aplicada. `V15` não é tocada nesta tarefa: o ajuste é inteiramente no teste.
- `.agents/rules/tests.md` §1 — cobertura mínima de 80% linhas/branches é inegociável; esta tarefa
  existe para que o `jacoco:check` volte a rodar e o piso volte a ser verificável.
- `.agents/rules/tests.md` §3 (AAA/Given-When-Then) — os casos novos devem seguir o mesmo formato do
  teste atual.
</rules>

<requirements>
- Constraint `sessao_tamanho_valido` (introduzida por `V15__sessao_livre_tamanho_variavel.sql`):
  `(origem = 'SIMULADO' and tamanho between 10 and 200) or (origem in ('CADERNO','LIVRE') and tamanho between 1 and 50)`.
- O teste deve provar o contrato vigente, não o anterior a `V15`.
</requirements>

## Subtarefas

- [x] 1.1 Reescrever `FlywayMigracaoTest.TI06_tamanhoDeSessaoForaDoDominioEhRecusado`
  (`backend/src/test/java/br/com/certame/FlywayMigracaoTest.java:81-84`) para provar o contrato
  vigente da constraint `sessao_tamanho_valido`, não o anterior a `V15`:
  - recusar `tamanho = 0` e `tamanho = 51` para origem `LIVRE`;
  - recusar `tamanho = 0` e `tamanho = 51` para origem `CADERNO`;
  - recusar `tamanho = 5` e `tamanho = 201` para origem `SIMULADO`;
  - **aceitar** `tamanho = 40` para origem `LIVRE` (é exatamente o caso que `V15` corrigiu — a
    ausência desse caso positivo é o que permitiu o teste antigo ficar errado sem ninguém notar).

  Implementado como sete testes de um comportamento cada (`TI06`–`TI12`, conforme regra "um
  comportamento por teste" de `.agents/rules/tests.md` §3), em vez de um único método com múltiplas
  asserções.
- [x] 1.2 Verificar se `prepararResposta()` (linha 86-98) precisa de um parâmetro `origem` explícito
  na inserção de `sessao_estudo` — hoje ela insere sem a coluna e depende do `default`. Tornar a
  origem explícita em cada subcaso para que o teste não dependa de um default que pode mudar.

  `prepararResposta()` ganhou uma sobrecarga `prepararResposta(String origem, int tamanho, String
  origemRef)`; o método sem parâmetros (usado por TI02–TI05) delega para ela com `("LIVRE", 10,
  null)`, tornando o valor explícito em vez de depender do `default` da coluna.
- [x] 1.3 Rodar `cd backend && ./mvnw verify` e confirmar **BUILD SUCCESS** com o `jacoco:check`
  executando (não apenas o `surefire`).
- [x] 1.4 Registrar o percentual de cobertura real obtido pelo `jacoco:check` nesta tarefa (evidência
  em `evidences/`). Se algum módulo ficar abaixo de 80%, **não corrigir aqui** — encaminhar para a
  tarefa 6.0 (P2-3), que já lista as lacunas conhecidas.

  Evidência em `tasks/correcoes-pendencias/evidences/task_1-verify.md`: 541 testes, 0 falhas,
  `jacoco:check` aprovado (regra `CLASS`/`*Service`/`LINE` ≥ 80%, todas as classes passaram);
  cobertura agregada do bundle 89,17% linhas / 71,85% branches. Branches abaixo do piso de 80% do
  projeto ficam para a tarefa 6.0, conforme já catalogado em `tasks/RELATORIO-PENDENCIAS.md` §4.
- [x] 1.5 Atualizar `tasks/prd-sessao-de-estudo/qa.md` removendo a referência ao BUG-01 (encaminhado
  por QA de `prd-simulados`) como pendência, e atualizar os números de `./mvnw verify` (535 testes,
  0 falhas) nos `qa.md`/`codereview.md` que ainda citam os números antigos (509/515 testes).

  `qa.md` e `codereview.md` de `prd-sessao-de-estudo` atualizados: pendência do `FlywayMigracaoTest`
  marcada como corrigida (BUG-01 resolvido), números de `./mvnw verify` atualizados para o valor real
  pós-correção (541 testes, 0 falhas — 535 anteriores menos o `TI06` antigo mais os 7 casos novos).
  Nenhuma menção literal a "509"/"515" testes foi encontrada em nenhum dos dois arquivos desta
  funcionalidade (são referências de outros `prd-*` fora do escopo desta tarefa).

## Detalhes de implementação

O caso `tamanho = 40` para `LIVRE` é o cerne do defeito de produção que `V15` corrigiu (sessão com
acervo menor que o tier pedido quebrava ao salvar) — por isso ele precisa aparecer como caso
**positivo** no teste, não só os limites negativos.

## Critérios de aceitação relacionados

- Nenhum CA específico — é correção de teste, não de comportamento de produto. A regra de negócio
  por trás (tamanho de sessão livre variável) já está coberta pelo PRD/TechSpec de
  `prd-sessao-de-estudo`.

## Testes da tarefa

- `FlywayMigracaoTest` (classe alterada) — todos os casos devem passar.
- `cd backend && ./mvnw verify` — build completo, incluindo `jacoco:check`.

## Arquivos relevantes

- `backend/src/test/java/br/com/certame/FlywayMigracaoTest.java` (modificado)
- `tasks/prd-sessao-de-estudo/qa.md` (atualizado)
