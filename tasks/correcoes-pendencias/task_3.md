# Tarefa 3.0 (P1-3): Publicação de questão sem disciplina — 500 vira 422, `disciplina` vira quarta pendência

## Visão geral

`ValidadorPublicacao` verifica gabarito, comentário e alternativas, mas **não verifica
`disciplinaId`**. O banco verifica, pelo `check questao_publicada_completa`. Se uma questão sem
disciplina satisfizer os outros três requisitos e for publicada, a violação estoura no PostgreSQL, e
`ManipuladorErrosGlobal` **não trata `DataIntegrityViolationException`** — o usuário recebe um 500
cru no lugar do 422 amigável do padrão `PUBLICACAO_INVALIDA`.

Decisão já tomada com o dono do produto: **`disciplina` passa a ser a quarta pendência listada por
`ValidadorPublicacao`**, alinhando o validador da aplicação ao `check` que já existe no banco. Isso
exige atualizar CA-06 do PRD de `prd-banco-de-questoes`, que hoje lista apenas gabarito, comentário e
alternativas. Independentemente disso, o manipulador global precisa da rede de segurança — nenhum
`check` de banco deve conseguir vazar como 500.

<rules>
### Conformidade com o AGENTS.md e as rules

- `.agents/rules/code-standards.md` §"Erros" — erros de negócio devem virar um envelope padronizado
  (`code` + `fieldErrors`), nunca um 500 cru.
- `AGENTS.md` regra 4 — nenhum endpoint fora do contrato da TechSpec §6 sem atualizar o documento na
  mesma alteração: como o formato de resposta de `PUBLICACAO_INVALIDA` não muda (só ganha mais uma
  chave possível no mapa de pendências), não é necessário mudar o contrato de rota, mas o CA-06 do
  PRD precisa refletir a nova pendência.
</rules>

<requirements>
- CA-06 (`prd-banco-de-questoes`, US6, RF-19), a ser atualizado: "Dada uma questão sem gabarito, sem
  comentário, com menos de duas alternativas (múltipla escolha) **ou sem disciplina**, quando tento
  publicá-la, então a publicação é recusada com a indicação de cada campo pendente."
- Constraint de banco `questao_publicada_completa` (já existente) é a fonte da verdade sobre o que é
  "questão completa" — o validador da aplicação deve cobrir o mesmo conjunto de casos que o banco,
  para que o `check` nunca seja o primeiro a rejeitar.
</requirements>

## Subtarefas

- [x] 3.1 Atualizar CA-06 em `tasks/prd-banco-de-questoes/prd.md` (linha ~75) para incluir
  `disciplinaId` ausente como quarto motivo de recusa. Atualizar a TechSpec correspondente se ela
  documentar a lista de pendências de publicação.
- [x] 3.2 Adicionar a verificação de `disciplinaId` em
  `backend/src/main/java/br/com/certame/questao/ValidadorPublicacao.java`, no mesmo padrão do mapa
  `LinkedHashMap<String, String>` já usado (`pendencias.put("disciplina", "...")`), com mensagem
  amigável equivalente às três já existentes.
- [x] 3.3 Adicionar em `backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java` um
  `@ExceptionHandler(DataIntegrityViolationException.class)` que devolve o envelope de erro padrão
  (mesmo formato dos três handlers já existentes) em vez de deixar a exceção não tratada virar 500.
  Esta rede de segurança vale para qualquer `check` de banco, não só o de publicação.
- [x] 3.4 Escrever teste de unidade em `ValidadorPublicacaoTest` para o caso "questão sem
  disciplina" (deve aparecer no mapa de pendências).
- [x] 3.5 Escrever teste para o novo handler de `ManipuladorErrosGlobal` (simular uma
  `DataIntegrityViolationException` e confirmar que o envelope de resposta é 422/409 com `code`, não
  500).
- [x] 3.6 Atualizar `tasks/prd-banco-de-questoes/qa.md`: mover P1-3 de pendência para resolvido, com
  evidência dos dois testes novos.

## Detalhes de implementação

O envelope de erro do handler de integridade não precisa expor a mensagem interna do PostgreSQL
(pode vazar nome de constraint/tabela) — usar uma mensagem genérica de negócio, seguindo o padrão dos
handlers já existentes em `ManipuladorErrosGlobal`.

## Critérios de aceitação relacionados

- CA-06 (`prd-banco-de-questoes`, atualizado nesta tarefa)

## Testes da tarefa

- `ValidadorPublicacaoTest` — novo caso "sem disciplina".
- Teste de `ManipuladorErrosGlobal` — novo handler de `DataIntegrityViolationException`.
- O E2E de banco de questões já existente cobre o caminho feliz; não precisa de novo cenário E2E a
  menos que o QA decida validar o 422 ponta a ponta.

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/questao/ValidadorPublicacao.java` (modificado)
- `backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java` (modificado)
- `backend/src/test/java/br/com/certame/questao/ValidadorPublicacaoTest.java` (modificado)
- `tasks/prd-banco-de-questoes/prd.md` (CA-06 atualizado)
- `tasks/prd-banco-de-questoes/qa.md` (atualizado)
