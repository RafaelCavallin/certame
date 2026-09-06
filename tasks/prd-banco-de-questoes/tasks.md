# Resumo das tarefas de implementação de Banco de questões e taxonomia

> **PRD:** `prd.md` · **TechSpec:** `techspec.md` · **Fase:** F1 — Base (RF-18, RF-19, RF-20, RF-37, RN-01, RN-03)
> **Dependência bloqueante:** `prd-acesso-e-conta` implementado (`usuario`, `SegurancaConfig`, `perfilGuard`, `ManipuladorErrosGlobal`). Sem ela não há FK de `autor_id` nem como provar autorização por perfil.
> **RF-37** (cadastro de bancas e órgãos pelo administrador) foi acrescentado depois da primeira versão desta especificação — decisão registrada em conversa, também refletida no `PRD.md`-mãe.

## Tarefas

- [x] 1.0 Lacunas do protótipo e nova tela `ADMIN: CATÁLOGO` (`Certame.dc.html`) — implementado RF-18, RF-19, RF-20 e RF-37 no protótipo.
- [x] 2.0 Migração e persistência (Flyway, entidades e repositórios)
- [x] 3.0 Fundação transversal (paginação, auditoria, exceções e contrato)
- [x] 4.0 Taxonomia e catálogo administrativo (RF-20, RF-37)
- [x] 5.0 Questão e alternativas (RF-19)
- [x] 6.0 Ciclo de vida e publicação (RN-01)
- [x] 7.0 Busca e listagem administrativa (RF-18)
- [x] 8.0 Estatística de acerto por questão (RN-03) — migração de teste, agregação sem respostas do autor, serviço e testes TU-28/TU-29/TI-27 implementados.
- [x] 9.0 Frontend administrativo (RF-18, RF-19, RF-20, RF-37) — APIs, filtros por URL, listagem, formulário, taxonomia, catálogo, diálogo de confirmação e rotas implementados.
- [x] 10.0 E2E e endurecimento
