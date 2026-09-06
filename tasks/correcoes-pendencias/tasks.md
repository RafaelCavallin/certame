# Resumo das tarefas de correção das pendências (RELATORIO-PENDENCIAS.md, 2026-09-05)

Cada tarefa desta pasta corrige uma ou mais pendências de `tasks/RELATORIO-PENDENCIAS.md`, na ordem
de prioridade registrada na seção 6 daquele documento. Não representam uma funcionalidade nova: são
correções sobre funcionalidades já aprovadas, cujos `codereview.md`/`qa.md` originais permanecem
como registro histórico e devem ser atualizados conforme cada correção é concluída.

## Ordem de execução

```
1.0 (bloqueador)
 ├── 2.0 ──┐
 ├── 3.0   ├── 5.0 ── 6.0 ── 7.0 ── 8.0 ── 9.0
 └── 4.0 ──┘
```

## Tarefas

- [x] 1.0 P0-1 — Destravar o build: alinhar `FlywayMigracaoTest` ao contrato vigente da `V15`
- [x] 2.0 P1-1 — CA-11 de flashcards: ligar `TempoEstudoFlashcards` à porta `TempoEstudoExterno`
- [x] 3.0 P1-3 — Publicação de questão: 500 vira 422 e `disciplina` vira quarta pendência
- [x] 4.0 P1-2 — CA-11 de gestão de usuários: anonimização do histórico após 7 dias, com placeholders e preservação dos agregados
- [x] 5.0 P2-1/P2-2 — Sanear os `test.skip` obsoletos e executar o E2E de editais
- [x] 6.0 P2-3 — Fechar as seis lacunas de cobertura registradas na seção 4 do relatório
- [x] 7.0 P3-1 — Prettier no frontend inteiro (191 arquivos) + gate no processo
- [x] 8.0 P3-2 — Fazer valer o limite de 100 linhas e acabar com a linha comprimida
- [x] 9.0 P3-3 — Desvios pontuais de padrão (10 itens confirmados no código atual)

## Dono por funcionalidade afetada

| Tarefa | Funcionalidade(s) dona(s) |
|---|---|
| 1.0 | `prd-sessao-de-estudo` |
| 2.0 | `prd-flashcards-decks-e-revisao` |
| 3.0 | `prd-banco-de-questoes` |
| 4.0 | `prd-gestao-de-usuarios` |
| 5.0 | `prd-sessao-de-estudo`, `prd-flashcards-criacao-e-gerenciamento`, `prd-editais` |
| 6.0 | `prd-cadernos-e-desempenho`, `prd-banco-de-questoes`, `prd-acesso-e-conta`, `prd-flashcards-decks-e-revisao` |
| 7.0 | transversal (frontend) |
| 8.0 | `prd-flashcards-criacao-e-gerenciamento`, `prd-gestao-de-usuarios`, `prd-banco-de-questoes`, `prd-simulados`, `prd-acesso-e-conta`, `prd-flashcards-decks-e-revisao`, `prd-sessao-de-estudo`, `prd-cadernos-e-desempenho` |
| 9.0 | `prd-simulados`, `prd-flashcards-criacao-e-gerenciamento`, `prd-flashcards-decks-e-revisao`, `prd-cadernos-e-desempenho`, `prd-sessao-de-estudo` |
