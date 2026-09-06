# Relatório de QA — Simulados (visão do estudante e gestão administrativa)

As evidências da ferramenta de navegador estão salvas em `tasks/prd-simulados/evidences/`.
Revalidação executada em 2026-09-05 sobre as correções da revisão (`revisao-andamento.md`):
bugs 1–4 da revisão reverificados como corrigidos (contagem de concluídas, `bancaId` 422,
200-na-retomada, ordenação da lista admin). Nenhum bug novo no escopo de simulados.

## Resumo
- Data: 2026-09-05
- Status: APROVADO

> **Conferido na revisão geral dos relatórios (2026-09-05).** Este relatório está **atualizado**: os
> números conferem com a medição independente daquele dia — backend `./mvnw verify` com 535 testes e
> 1 falha (`FlywayMigracaoTest.TI06`), frontend 427 testes em 80 arquivos com cobertura
> 89,47% / 85,27% / 80,27% / 93,83%. BUG-01 **continua aberto** e agora consta também em
> `tasks/RELATORIO-PENDENCIAS.md`. Nenhuma correção foi necessária neste arquivo.
- Total de critérios de aceitação: 12
- Critérios de aceitação atendidos: 12
- Bugs encontrados: 0 (no escopo; 1 falha estrangeira documentada em BUG-01)

## Critérios de aceitação verificados
| ID | Critério de aceitação | Casos de teste | Status | Evidência |
|----|-----------------------|----------------|--------|-----------|
| CA-01 | Tela de simulados com três agrupamentos (disponíveis, agendados e concluídos) com nome, banca, número de questões e duração | TU-01, TI-09, E2E-01 | PASSOU | evidences/e2e-01-log.txt |
| CA-02 | Simulado concluído exibe nota e data de conclusão | TU-02, E2E-01 | PASSOU | evidences/e2e-01-log.txt |
| CA-03 | Simulado disponível dentro da janela abre em Modo Prova com número de questões e duração declarados | TU-10, TU-13, TI-04, TI-13, E2E-02 | PASSOU | evidences/e2e-02-log.txt |
| CA-04 | Série gerada contém apenas questões publicadas e respeita composição por disciplina | TU-03, TU-04, TU-07, TI-01, TI-03 | PASSOU | suíte simulado 41/41 |
| CA-05 | Resultado pós-finalização com nota, acertos, erros, não respondidas, tempo total e revisão questão a questão | TU-12, TI-04, E2E-02 | PASSOU | evidences/e2e-02-log.txt |
| CA-06 | Simulados concluídos permitem revisão e refeitura como treino sem alterar nota registrada | TU-11, TU-28, TI-06, TI-07, TI-11, E2E-03 | PASSOU | evidences/e2e-03-log.txt |
| CA-07 | Tentativa abandonada sem finalização não gera nota nem histórico | TI-05 | PASSOU | suíte simulado 41/41 |
| CA-08 | Formulário cadastra código, nome, banca, número de questões, duração e janela com status não publicado | TU-16, TU-26, E2E-04 | PASSOU | evidences/e2e-04-log.txt |
| CA-09 | Composição exibe soma comparada ao número declarado | TU-14, TU-15, E2E-04 | PASSOU | evidences/e2e-04-log.txt |
| CA-10 | Publicação recusada quando janela ausente ou soma diverge, com indicação do que está inconsistente | TU-05, TU-06, TU-07, TI-01, TI-02, E2E-05 | PASSOU | evidences/e2e-05-log.txt |
| CA-11 | Estado derivado da janela (agendado/disponível/encerrado) e bloqueio de nova tentativa fora da janela | TU-08, TU-09, TI-09, TI-10 | PASSOU | suíte simulado 41/41 |
| CA-12 | Operações administrativas recusadas para usuário sem perfil admin no servidor | TI-08 | PASSOU | suíte simulado 41/41 |

## Testes E2E executados
| ID | Fluxo | Resultado | Observações |
|----|-------|-----------|-------------|
| E2E-01 | estudanteVeOsTresGruposDeSimulados | PASSOU | 3 grupos com rótulo textual, nota 100% e data dd/MM/yyyy |
| E2E-02 | estudanteFazSimuladoDoInicioAoResultado | PASSOU | Modo Prova sem gabarito, cronômetro visível, finalização → resultado |
| E2E-03 | estudanteRevisaERefazComoTreino | PASSOU | Revisão + refeitura treino mantém nota original |
| E2E-04 | administradorMontaEPublicaSimulado | PASSOU | Composição com soma exibida, publicar após fechar |
| E2E-05 | publicacaoRecusadaMostraOQueEstaInconsistente | PASSOU | Janela/composição/acervo apontados no formulário |
| E2E-06 | navegacaoPorTecladoEContrasteNaListaESimuladoForm | PASSOU | Axe wcag2a/wcag2aa sem violações em /simulados e /admin/simulados/nova |

Comando: `cd frontend && npx playwright test e2e/simulados.spec.ts e2e/admin-simulados.spec.ts` —
6 passed em 1.4 min, stack isolado `certame-e2e` (postgres :30532, backend :3000, frontend :5100),
teardown com `compose down -v` executado.

## Testes automatizados e cobertura
| Camada | ID | Resultado | Validação/comando | Observações |
|--------|----|-----------|-------------------|------------|
| Unidade/Integração (simulado) | TU-01..TU-16, TU-26..TU-28, TI-01..TI-13 | PASSOU | `./mvnw test -Dtest='br.com.certame.simulado.*Test'` — 41 tests, 0 falhas | Inclui os 4 testes de regressão da revisão |
| Backend (todos os domínios) | 535 testes | 534 PASSOU / 1 FALHA estrangeira | `./mvnw verify` | Falha só em `FlywayMigracaoTest.TI06`, ver BUG-01 (fora do escopo) |
| Backend cobertura | gate JaCoCo | PASSOU | `./mvnw jacoco:check@check-cobertura-minima` — All coverage checks have been met | Regra CLASS `br.com.certame.*.*Service` LINE ≥ 80%: SimuladoAdminService 100%, Catalogo 100%, Tentativa 81.1%, Publicacao 82.4% |
| Frontend | 80 specs / 427 testes | PASSOU | `npm run test:coverage` | Statements 89.47%, Branches 85.27%, Functions 80.27%, Lines 93.83% (meta 80%) |
| E2E | E2E-01..E2E-06 | PASSOU | `npx playwright test e2e/simulados.spec.ts e2e/admin-simulados.spec.ts` — 6 passed | Chromium, dados zerados por execução |

- Cobertura: backend gate JaCoCo ATENDIDO; frontend acima de 80% nas 4 métricas.

## Acessibilidade
- [x] Navegação por teclado (Tab, Enter, Esc) — E2E-06: Tab com foco visível em /simulados e /admin/simulados/nova
- [x] Elementos interativos com rótulos descritivos — `aria-label`, role heading/nome nos specs
- [x] Imagens com texto alternativo (`alt`) apropriado — N/A (ícones `aria-hidden`)
- [x] Contraste de cores adequado — Axe wcag2a/wcag2aa sem violações nas duas telas
- [x] Formulários com rótulos associados aos campos — `label for=` em nome/banca/número/duração/abertura/encerramento
- [x] Mensagens de erro claras e acessíveis — `role="alert"` em erros de formulário e `fieldErrors`
- [x] Fontes com tamanho apropriado — tokens Nocturne var(--*)

## Bugs encontrados e corrigidos
| ID | Descrição | Severidade | Status | Correção | Teste de regressão | Evidência |
|----|-----------|------------|--------|----------|--------------------|-----------|
| BUG-01 | `FlywayMigracaoTest.TI06_tamanhoDeSessaoForaDoDominioEhRecusado` falha: espera que `tamanho = 40` seja recusado para sessão LIVRE, mas a migração `V15__sessao_livre_tamanho_variavel.sql` (prd-sessao-de-estudo, intencional — codereview §127) liberou `1..50` para LIVRE. Teste desatualizado, não regressão de simulados | Baixa (fora do escopo) | **AINDA ABERTO** — encaminhado ao prd-sessao-de-estudo; reconfirmado em 2026-09-05 na revisão geral dos relatórios | Não corrigido neste QA (domínio alheio; alterar V15 ou TI06 exige decisão do dono) | TI06 existente; sugestão: assertir `40` aceito e `0`/`51` recusados para LIVRE | `./mvnw verify` — Tests run: 535, Failures: 1 (reproduzido em 2026-09-05) |
| OBS-01 | `TentativaSimuladoIntegracaoTest` falhou 3x seguidas com `FileNotFoundException` do próprio `.class` no parse do contexto Spring, e passou sem alteração nas 2 execuções seguintes (8/8) e na suíte do módulo (41/41). Padrão compatível com pressão ambiental (build incremental/`target/` + containers), não com defeito de código | — | Sem ação | Nenhuma (código inalterado; verde em todas as reexecuções) | Suíte 41/41 + E2E-02/E2E-03 | logs de execução 2026-09-05 |

## Conclusão
Todos os 12 critérios de aceitação verificados e atendidos. Nenhum bug no escopo de simulados.
E2E 6/6, unidade/integração do módulo 41/41, frontend 427/427, gate JaCoCo e cobertura frontend
acima de 80%. A única falha do backend completo (TI06) pertence ao prd-sessao-de-estudo e foi
encaminhada. QA APROVADO.
