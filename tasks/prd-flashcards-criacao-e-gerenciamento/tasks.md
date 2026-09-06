# Resumo das tarefas de implementação de Flashcards — criação no fluxo da questão e gerenciamento

> **PRD:** `prd.md` · **TechSpec:** `techspec.md` · **Fase:** F6 — Flashcards (RF-28…RF-32, RN-07, RN-09)
> **Dependências implementadas:** `prd-acesso-e-conta` (`SegurancaConfig`, `authGuard`, shell, `styles.css`, interceptor) e `prd-banco-de-questoes` (`disciplina`, `assunto`, `questao`, `PaginaResposta<T>`, as três exceções e o `confirmacao-dialog`).
> **Numeração confirmada nesta entrega:** `db/migration` está em `V5`, logo a migração desta funcionalidade é **`V6__flashcards.sql`** (e não `V8`, como estimado na TechSpec); a semente de `dev` é **`V902__seed_flashcards_dev.sql`** (existem apenas `V900` e `V901`). Nenhuma migração aplicada é editada.
> **Dependência não implementada:** `prd-sessao-de-estudo` não tem TechSpec e a tabela `sessao_estudo` não existe. Até F2 chegar, ela vem de `db/migration-test/V991__sessao_estudo_minima.sql`, no mesmo padrão do `V990__resposta_minima.sql` já usado por `prd-cadernos-e-desempenho`. A integração real com a barra de ações da questão está isolada na tarefa 10.0.

## Tarefas

- [x] 1.0 Lacunas do protótipo (`Certame.dc.html`) — multisseleção com "Mover para deck", "+ Novo deck" no seletor e modo avulso
- [x] 2.0 Migração e persistência (Flyway, entidades, enums e repositórios)
- [x] 3.0 Regras de domínio puras e contexto de origem (RN-07, RN-09) — regras de propriedade, tags e origens implementadas com TU-01…TU-06 e TU-21…TU-24.
- [x] 4.0 Serviço de escrita do flashcard (RF-29, RF-32)
- [x] 5.0 Leitura, indicadores, porta da fila e decks (RF-31, RF-32)
- [x] 6.0 Controllers e contrato HTTP (RF-28…RF-32, RN-07)
- [x] 7.0 Infraestrutura de UI: `shared/ui/dialog` e `shared/ui/toast`
- [x] 8.0 Modal de criação de flashcard (RF-29, RF-30)
- [x] 9.0 Tela de gerenciamento `/flashcards` (RF-31, RF-32)
- [x] 10.0 Integração com a sessão de estudo, E2E e endurecimento (RF-28)
