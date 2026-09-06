# Relatório de revisão de código — Simulados (visão do estudante e gestão administrativa)

Revisão executada em 2026-09-03/04, finalizada em 2026-09-05. Escopo: RF-14, RF-21, RN-01, RN-04
(CA-01 a CA-12). Detalhe do andamento em `revisao-andamento.md`; validação funcional em `qa.md`.

## Resumo
- Data: 2026-09-05
- Branch: N/A (repositório sem git nesta worktree)
- Status: APROVADO COM RESSALVAS

> **Conferido na revisão geral dos relatórios (2026-09-05).** Este relatório está **atualizado** e
> não precisou de correção. As três ressalvas Baixas foram reverificadas no código e **continuam
> abertas**: `SimuladoAdminService.java` com **125 linhas**; `@Autowired` em construtor secundário de
> `MontadorProvaSimulado.java:22`; `Clock` injetado e não utilizado em
> `ExpiradorTentativaSimulado.java`. A nota sobre a falha estrangeira em `FlywayMigracaoTest.TI06`
> também segue válida. Consolidado em `tasks/RELATORIO-PENDENCIAS.md`.

## Conformidade com regras
| Regra | Status | Observações |
|------|--------|-------------|
| AGENTS.md 1 — frontend 1:1 do protótipo, tokens `var(--*)`, sem UI de terceiros | OK | Componentes `features/simulados` e `features/admin/simulados` traduzem os artboards `simulados`, `admin-simulados` e `admin-simulado-form`; arquivos `.ts` de produção com ≤ 31 linhas |
| AGENTS.md 2 — autorização no servidor com `@PreAuthorize` | OK | `SimuladoAdminService` (6 métodos) e `PublicacaoSimuladoService` (publicar/despublicar) anotados com `hasAnyRole('ADMIN','SUPER_ADMIN')`; TI-08 cobre 403 para estudante |
| AGENTS.md 3 — pt-BR na interface, código/banco sem acento | OK | DTOs `Requisicao*`/`Resposta*`/`*Resumo`, mensagens com ponto final; migração `V9__simulado.sql` em `snake_case` |
| AGENTS.md 4 — nenhum endpoint fora do contrato sem atualizar a Tech Spec | OK | `TECH-SPEC.md` §6 atualizado na mesma alteração com as 7 rotas admin (linhas 331–337) |
| AGENTS.md 5 — migração só por Flyway, versionada e imutável | OK | `V9__simulado.sql` aditiva; `V10/V11` (sequência de código) posteriores preservadas |
| AGENTS.md 6 — `resposta` imutável, RN-04 sem feedback antes da finalização | OK | Reuso de `SessaoEstudoService`/`CorrecaoProvaService`; série sem gabarito (E2E-02) |
| code-standards §2.1 — sem comentários desnecessários | OK | Nenhum comentário óbvio no pacote `simulado` |
| code-standards §2.2 — arquivos ≤ 100 linhas, métodos ≤ 30 | NOK | `SimuladoAdminService.java` com 125 linhas (ver Problemas). Único arquivo de produção acima do limite no backend e no frontend do escopo |
| code-standards §2.3/2.4/2.5/2.7 — guardas, ≤ 3 parâmetros, sem linha em branco em método | OK | `resumo()` em linha única porém legível; sem violações relevantes |
| code-standards §3 — camadas, construtor sem `@Autowired` em produção, DTOs record, erros `ErroNegocioException` | OK com ressalva | `@Autowired` em `MontadorProvaSimulado.java:22` (ressalva Baixa, ver Problemas) |
| tests.md — FIRST/AAA, nomes TU-*/TI-*, cobertura ≥ 80% | OK | 41 testes do módulo verdes; gate JaCoCo `CLASS br.com.certame.*.*Service LINE ≥ 80%` atendido |
| folder-structure.md — pacote `br.com.certame.simulado`, `flashcard/fsrs` intocado | OK | Domínio isolado em `simulado/` (+`dto/`); sem invasão de outros domínios |

## Aderência à TechSpec
| Decisão Técnica | Implementado | Observações |
|-----------------|--------------|-------------|
| 10 endpoints (`GET /simulados`, 3 de tentativa, 7 grupos admin) | SIM | `SimuladoController` (4 rotas) e `SimuladoAdminController` (8 rotas) conferem com a tabela da TechSpec §6 |
| Série congelada na publicação, mesma prova para todos | SIM | `PublicacaoSimuladoService`: validar → montar → gravar `simulado_questao` (`ordem` 1..N) na mesma transação; TI-01/TI-03 |
| `VALIDA` decidida na finalização, refeitura nasce `TREINO` | SIM | Índice `tentativa_valida_unica_idx`; `notaRegistrada` no resultado de treino; TI-06/TI-07 |
| Estado derivado (`JanelaSimulado` + `Clock`), nunca armazenado | SIM | `AGENDADO`/`DISPONIVEL`/`ENCERRADO` de status + janela; TU-08, TI-09 |
| Gatilho em vez de `rule` para congelar tentativa finalizada | SIM | `tentativa_simulado_congelada` devolve `old` em silêncio; TI-07 |
| Reuso de F3 (`SessaoEstudoService`, `CorrecaoProvaService`), sem tabela/interface paralela | SIM | `origem = SIMULADO`, `modo = PROVA`; execução reaproveita telas de `features/study` |
| Retomada com 200, criação com 201 | SIM | Corrigido nesta revisão (`temEmAndamento()`); TI-13 |
| `tentativasConcluidas` conta `VALIDA` finalizadas | SIM | Corrigido nesta revisão (`countBySimuladoIdAndTipoAndStatus`); TU-27 |
| `bancaId` inexistente responde 422 | SIM | Corrigido nesta revisão (`DadosInvalidosException`); TU-26 |
| Lista admin com ordenação estável | SIM | Corrigido nesta revisão (`Sort.by("atualizadoEm").descending()`) |
| Auditoria de publicação/despublicação/janela | SIM | `RegistradorAuditoria` na transação do serviço; TI-12 |
| Duração/tempo em segundos, nota em percentual 0–100 | SIM | Contratos `RespostaTentativa`/`RespostaResultado`/`SimuladoResumo` |

## Tarefas verificadas
| Tarefa | Status | Observações |
|------|--------|-------------|
| 1.0 Migração `V9__simulado.sql` + entidades e repositórios | COMPLETA | Esquema + `V10/V11` de sequência; `validate` passa na subida |
| 2.0 Regras puras (`JanelaSimulado`, `ValidadorPublicacaoSimulado`, `MontadorProvaSimulado`) | COMPLETA | TU-03 a TU-09 verdes |
| 3.0 `SimuladoAdminService` e `PublicacaoSimuladoService` | COMPLETA | CRUD, composição, publicar/despublicar, auditoria; TU-14 a TU-16, TU-26, TU-27, TI-01, TI-02, TI-11, TI-12 |
| 4.0 `SimuladoAdminController` + autorização | COMPLETA | Subtarefas 4.1–4.5 marcadas; TI-08 (403 para estudante) |
| 5.0 `SimuladoCatalogoService`, `TentativaSimuladoService`, `ExpiradorTentativaSimulado`, `SimuladoController` | COMPLETA | TU-01, TU-02, TU-10, TU-11, TU-13, TU-28, TI-03 a TI-07, TI-09, TI-10, TI-13 |
| 6.0 Frontend administrativo | COMPLETA | Lista, formulário, `composicao-disciplinas`; cobertura de funções destravada (80.27%) |
| 7.0 Frontend do estudante | COMPLETA | Lista em 3 grupos, destaque, resultado; botão Iniciar liga ao Modo Prova |
| 8.0 E2E e acessibilidade | COMPLETA | E2E-01 a E2E-06 verdes; Axe wcag2a/wcag2aa sem violações |

## Testes
- Total de testes (módulo simulado): 41
- Passando: 41
- Falhando: 0
- Cobertura: backend gate JaCoCo ATENDIDO (`*Service` LINE ≥ 80%: Admin 100%, Catalogo 100%, Tentativa 81.1%, Publicacao 82.4%); frontend Statements 89.47%, Branches 85.27%, Functions 80.27%, Lines 93.83% (427 testes); E2E 6/6
- Nota: `./mvnw verify` completo soma 535 testes com 1 falha estrangeira (`FlywayMigracaoTest.TI06`, V15 do prd-sessao-de-estudo com teste desatualizado — ver `qa.md` BUG-01, fora deste escopo)

## Problemas encontrados
| Severidade | Arquivo | Linha | Descrição | Sugestão |
|------------|---------|-------|-----------|----------|
| Alta | `SimuladoAdminService.java` (`resumo()`) | — | `tentativasConcluidas` hardcoded `0` | Corrigido: `countBySimuladoIdAndTipoAndStatus(...)` (método novo em `TentativaSimuladoRepository`); regressão TU-27 |
| Alta | `SimuladoAdminService.java` (`preencher()`) | — | `bancaId` inexistente retornava 404, TechSpec exige 422 | Corrigido: `DadosInvalidosException(Map.of("bancaId", ...))`; regressão TU-26 |
| Média | `SimuladoController.java` (`iniciar()`) | 25 | Sempre 201; TechSpec exige 200 ao retomar | Corrigido: `temEmAndamento()` + 200 na retomada; regressão TI-13 |
| Média | `SimuladoAdminService.java` (`listar()`) | 74 | Ramo sem filtro sem ordenação (paginação instável) | Corrigido: `Sort.by("atualizadoEm").descending()` |
| Baixa | `SimuladoAdminService.java` | 1–125 | 125 linhas (> 100, §2.2): CRUD + composição + mapeamento `resumo()`/`detalhe()` no mesmo arquivo | Não refatorado (risco vs. benefício); follow-up: extrair mapeador `SimuladoMapeador` |
| Baixa | `MontadorProvaSimulado.java` | 22 | `@Autowired` em construtor secundário (code-standards §3 prescreve injeção por construtor sem `@Autowired` em produção) | Unificar em construtor único com `Random` via `@Bean`, mantendo o construtor com `Random` explícito para teste |
| Baixa | `ExpiradorTentativaSimulado.java` | 11–13 | `Clock` injetado e nunca usado; o filtro `expiradaEm != null` está correto no domínio (só preenchido após expiração) | Remover o parâmetro `Clock` ou documentar como extensão futura |
| Baixa | `TentativaSimulado.java` | 23, 27 | `Instant.now()` direto em `aoCriar()`/`finalizar()` (não testável via `Clock`, ao contrário de `JanelaSimulado`) | Receber `Clock`/instante por parâmetro nos pontos de escrita |

## Pontos positivos
- Regras puras (`JanelaSimulado`, `ValidadorPublicacaoSimulado`) isoladas e testadas com `Clock` fixo
- Série congelada transacional e índice único de tentativa válida: comparabilidade e imutabilidade garantidas no banco, não só no código
- Autorização no servidor em todos os métodos admin, com teste de 403 dedicado
- Frontend 1:1 do protótipo com arquivos pequenos (≤ 31 linhas) e sem biblioteca de UI
- Documentação atualizada junto (`TECH-SPEC.md` §6, `task_4.md`)

## Recomendações
- Follow-up não bloqueante: extrair `SimuladoMapeador` de `SimuladoAdminService` para voltar a ≤ 100 linhas (§2.2)
- Avaliar `@Bean Clock`/`Random` central para remover `Instant.now()`/`new Random()` espalhados no domínio
- Registrar no `prd-sessao-de-estudo` a atualização do `TI06` frente à `V15__sessao_livre_tamanho_variavel.sql`

## Conclusão
Quatro bugs reais corrigidos na causa raiz com testes de regressão (TU-26, TU-27, TU-28, TI-13),
tarefas 1.0–8.0 completas e verificadas, testes verdes (41/41 módulo, 427 frontend, 6/6 E2E) e
cobertura acima da meta. Resta uma violação não bloqueante de §2.2 (125 linhas) com follow-up
proposto. Veredito: **APROVADO COM RESSALVAS**.
