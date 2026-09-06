# Resumo das tarefas de implementação de Editais acompanhados

> **PRD:** `prd.md` · **TechSpec:** `techspec.md` · **Fase:** F4 — Simulados · **Requisitos:** RF-17, RF-22

A ordem abaixo é a da §*Sequenciamento do desenvolvimento* da TechSpec: protótipo antes de qualquer marcação Angular (regra 1 do `AGENTS.md`), persistência antes das regras, regras puras antes dos serviços, backend antes do frontend, e E2E por último. O passo 8 da TechSpec (frontend) é dividido em duas tarefas — estudante e administração — porque são duas telas, dois stores e dois conjuntos de rotas.

## Tarefas

- [x] 1.0 Desenhar as telas `EDITAIS` e `ADMIN: EDITAIS` no protótipo Nocturne
- [x] 2.0 Migração `V12__editais.sql`, semente de `dev`, entidade e repositório
- [x] 3.0 Regras puras: `VerificadorPrazoEdital` e `ResolvedorDisciplinasEdital`
- [x] 4.0 `EditalService` e DTOs: criar, atualizar e excluir com auditoria (RF-22)
- [x] 5.0 `ConsultaEditaisService`: agrupamento por status e lista administrativa (RF-17)
- [x] 6.0 `EditalController` e `EditalAdminController`: contrato, autorização e `no-store`
- [x] 7.0 `EditalConsultaVinculos` e recusa de exclusão de disciplina vinculada
- [x] 8.0 Frontend do estudante: consulta de editais agrupados (RF-17)
- [x] 9.0 Frontend administrativo: lista, formulário e seletor de disciplinas (RF-22)
- [x] 10.0 E2E, cobertura, acessibilidade e atualização da `TECH-SPEC.md`

## Mapa de cobertura

| Tarefa | Critérios de aceitação | Testes |
| --- | --- | --- |
| 1.0 | CA-01, CA-02, CA-03, CA-07, CA-08, CA-09, Borda 2 | — (habilita E2E-01…E2E-12) |
| 2.0 | CA-04, CA-07, CA-08, Borda 2 | TI-01…TI-04 |
| 3.0 | CA-08, Borda 2 | TU-01…TU-04, TU-14…TU-17 |
| 4.0 | CA-04, CA-06, CA-07, CA-08 | TU-05…TU-13 |
| 5.0 | CA-01, CA-09, Borda 2 | TU-18…TU-22 |
| 6.0 | CA-01…CA-07, CA-09, CA-10 | TI-05…TI-21 |
| 7.0 | CA-08 | TI-22, TI-23 |
| 8.0 | CA-01, CA-02, CA-03, CA-09 | TU-23, TU-24, TU-25 |
| 9.0 | CA-04, CA-06, CA-07, CA-08, CA-10, Borda 2 | TU-26…TU-30 |
| 10.0 | CA-01…CA-10 | E2E-01…E2E-12 |

Os 10 critérios de aceitação, os 3 casos de borda e os 65 casos de teste da TechSpec (30 TU, 23 TI, 12 E2E) estão mapeados.

## Dependências e bloqueios

- **`prd-banco-de-questoes` bloqueia quase tudo.** `disciplina`, `banca`, `orgao` e `auditoria` (migração `V2`), `RegistradorAuditoria`, `ConflitoException`, `RecursoNaoEncontradoException`, `DadosInvalidosException` e `TaxonomiaService` precisam existir antes das tarefas 2, 4 e 7; `GET /admin/disciplinas`, `/admin/bancas`, `/admin/orgaos` e `shared/ui/confirmacao-dialog` bloqueiam a tarefa 9.
- **`prd-acesso-e-conta`** (`SegurancaConfig`, `authGuard`, `perfilGuard`, shell, `ManipuladorErrosGlobal`, `styles.css`) — implementado, sem bloqueio.
- **`prd-cadernos-e-desempenho`** (`shared/ui/estado-vazio`, `ZonaAplicacao`) não bloqueia: se não estiverem prontos, nascem aqui com a mesma assinatura (tarefas 3 e 8).
- **Aprovação visual das duas telas novas** (tarefa 1) bloqueia as tarefas 8 e 9. É a dependência mais provável de atrasar: são telas inéditas, não lacunas de telas existentes.
- **Docker/PostgreSQL 18** bloqueiam a suíte de integração (Testcontainers); o índice único parcial e os `check` não têm equivalente em banco em memória.
- **Playwright** ainda não está no `package.json`; bloqueia a tarefa 10.
- **O número da migração (`V7`, semente `V905`)** é conferido na entrega contra `db/migration` — `prd-simulados` é da mesma fase F4 e F2/F3 ainda não foram aplicadas. Nenhuma migração aplicada é editada.
