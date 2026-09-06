# Resumo das tarefas de implementação de Simulados

> **PRD:** `prd.md` · **TechSpec:** `techspec.md` · **Fase:** F4 — Simulados (RF-14, RF-21, RN-01, RN-04)
> **Dependência bloqueante parcial:** `prd-sessao-de-estudo` (F3) precisa estar implementada antes das tarefas 5 e 7 — é de lá que vêm `sessao_estudo`, `sessao_questao`, `resposta`, o Modo Prova e a correção. As tarefas 1 a 4 e 6 (parcial) não dependem de F3.
> **Dependência de dados:** acervo publicado suficiente por disciplina (`prd-banco-de-questoes`, já implementado) — sem ele nenhum simulado publica.

## Tarefas

- [x] 1.0 Migração `V9__simulado.sql` + entidades e repositórios
- [x] 2.0 Regras puras: `JanelaSimulado`, `ValidadorPublicacaoSimulado`, `MontadorProvaSimulado`
- [x] 3.0 `SimuladoAdminService` e `PublicacaoSimuladoService` (CRUD, composição, publicar/despublicar, auditoria)
- [x] 4.0 `SimuladoAdminController` (endpoints administrativos e autorização)
- [x] 5.0 `SimuladoCatalogoService`, `TentativaSimuladoService`, `ExpiradorTentativaSimulado` e `SimuladoController`
- [x] 6.0 Frontend administrativo (lista, formulário, composição por disciplina)
- [x] 7.0 Frontend do estudante (lista em três grupos, destaque, resultado)
- [x] 8.0 E2E e ajuste fino de acessibilidade
