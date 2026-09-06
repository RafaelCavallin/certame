# Relatório de QA — Banco de questões e taxonomia

As evidências da ferramenta de navegador devem estar salvas em `tasks/prd-banco-de-questoes/evidences/`.

## Resumo
- Data: 2026-09-02 · **Revalidado em 2026-09-05**
- Status: APROVADO (mantido)
- Total de critérios de aceitação: 14
- Critérios de aceitação atendidos: 14
- Bugs encontrados: 0

> **Revalidação 2026-09-05.** Os 14 critérios de aceitação continuam atendidos e nenhum bug de
> produto foi encontrado. Os números das suítes automatizadas mudaram e foram atualizados abaixo.
> Esta revalidação **não reexecutou a suíte Playwright** nem subiu o ambiente E2E — a verificação
> foi feita por `./mvnw verify` e `npm run test:coverage`. Registre-se também que a linha de
> cobertura de backend deste relatório citava `./mvnw verify -DskipTests`, comando que **pula os
> testes e, portanto, não exercita o gate JaCoCo** — a afirmação foi corrigida.

> **Revalidação 2026-09-05 (tarefa 6.0, correção de pendências P2-3).** Fechadas as duas lacunas de
> cobertura desta funcionalidade registradas em `tasks/RELATORIO-PENDENCIAS.md`:
> - **Autorização no slice web.** Os testes de serviço de `questao`/`catalogo` são unitários com
>   mocks e não passavam pelo proxy do `@PreAuthorize` (só o E2E provava o 403 real). Novos
>   `QuestaoAdminControllerTest` (4/4) e `CatalogoControllerTest` (8/8) sobem o `PublicacaoService` e
>   o `CatalogoService` **reais** (não mockados) sob `@EnableMethodSecurity`, provando 403 por perfil
>   insuficiente em publicar/despublicar questão e no CRUD de bancas/órgãos.
> - **Páginas administrativas sem teste de unidade.** `materias-page.spec.ts` (17 testes),
>   `catalogo-page.spec.ts` (17 testes) e `questoes-page.spec.ts` (11 testes) cobrem orquestração de
>   diálogos (abrir/fechar/confirmar), tratamento de erro por `code` (`ASSUNTO_EM_USO`,
>   `QUESTAO_EM_USO`, nome duplicado) e troca de aba.
> - `cd backend && ./mvnw verify` roda limpo nesta data: **571/571 testes, `jacoco:check` "All
>   coverage checks have been met."** — a falha de `FlywayMigracaoTest.TI06` citada acima foi
>   corrigida pela tarefa 1.0 de `tasks/correcoes-pendencias/`; o gate de 80% do backend está
>   confirmado.
> - Frontend: **473 testes, 0 falhas** — Statements 89,45% · Branches 84,88% · Functions 80,18% ·
>   Lines 93,93%.

## Critérios de aceitação verificados
| ID | Critério de aceitação | Casos de teste | Status | Evidência |
|----|-----------------------|----------------|--------|-----------|
| CA-01 | Listagem inicial com colunas e desempenho até 800 ms (p95). | TI-06, E2E-01 | PASSOU | `evidences/lista-desktop.png`, `evidences/lista-mobile.png` |
| CA-02 | Busca e filtros combinados recalculam a lista e paginação. | TU-20, TU-21, TU-22, TI-03, TI-04, E2E-02, E2E-03 | PASSOU | E2E-02 a E2E-04 |
| CA-03 | Múltipla escolha com 2 a 5 alternativas e um gabarito. | TU-10, TU-11, TI-09, E2E-05 | PASSOU | `evidences/formulario-desktop.png` |
| CA-04 | Certo/Errado substitui alternativas e permite gabarito binário. | TU-06, TU-07, TU-12, TI-10, E2E-06 | PASSOU | E2E-06 |
| CA-05 | Rascunho mínimo salvo sem gabarito ou comentário. | TU-08, TU-09, TI-08, E2E-07 | PASSOU | E2E-07 |
| CA-06 | Publicação incompleta é recusada com todas as pendências, incluindo disciplina. | TU-01 a TU-05, ValidadorPublicacaoTest, ManipuladorErrosGlobalTest, TI-12, TI-19, E2E-08 | PASSOU | `ValidadorPublicacaoTest.retornaDisciplinaComoPendenciaQuandoAusente`; `ManipuladorErrosGlobalTest.trataViolacaoDeIntegridadeComEnvelopeSeguro`; E2E-08 |
| CA-07 | Apenas questões publicadas entram em novas sessões. | TU-15, TI-14, TI-15, E2E-09 | PASSOU | E2E-09 |
| CA-08 | Despublicação remove de novas sessões e preserva histórico. | TU-17, TU-18, TI-16, E2E-11 | PASSOU | E2E-11 |
| CA-09 | Respostas do autor são excluídas da estatística. | TU-28, TU-29, TI-27 | PASSOU | Não aplicável à interface |
| CA-10 | Árvore exibe contagens por disciplina e assunto. | TI-20, TI-21, E2E-12 | PASSOU | `evidences/materias-desktop.png`, `evidences/materias-mobile.png` |
| CA-11 | Assunto vinculado não é excluído e oferece arquivamento. | TU-23 a TU-25, TI-22, TI-23, E2E-13 | PASSOU | E2E-13 |
| CA-12 | Renomear assunto preserva vínculos e atualiza a exibição. | TU-26, TI-24, E2E-14 | PASSOU | E2E-14 |
| CA-13 | Administrador cadastra banca/órgão e os itens aparecem nos combos. | TU-36, TU-38, TI-29, TI-26, E2E-16 | PASSOU | `evidences/catalogo-desktop.png`, `evidences/catalogo-mobile.png` |
| CA-14 | Exclusão de banca/órgão vinculado é recusada. | TU-37, TU-39, TI-31, TI-33, E2E-16 | PASSOU | E2E-16 |

## Testes E2E executados
| ID | Fluxo | Resultado | Observações |
|----|-------|-----------|-------------|
| E2E-01 a E2E-16 | Fluxos administrativos da funcionalidade | PASSOU | `npx playwright test e2e/banco-questoes.spec.ts`: 16 passed. |

## Ambiente
- Backend QA: `http://localhost:3000`, processo Spring Boot iniciado nesta execução.
- Frontend QA: `http://localhost:5100`, processo Angular iniciado nesta execução.
- PostgreSQL E2E: `localhost:30532`, container `certame-e2e-postgres-1`, volume descartável.
- MailHog E2E: SMTP `localhost:30525`, UI `localhost:30580`, container `certame-e2e-mailhog-1`.
- O ambiente foi isolado do Compose de desenvolvimento (`55432`, `55525`, `58025`).
- Após o relatório, os processos e containers acima foram encerrados e as portas liberadas.

## Testes automatizados e cobertura
| Camada | ID | Resultado | Validação/comando | Observações |
|--------|----|-----------|-------------------|------------|
| Unidade/Integração | TU/TI associados | PASSOU | `./mvnw test -Dtest='*Questao*Test,*Taxonomia*Test,*Catalogo*Test,*Publicacao*Test,*BancoQuestoes*Test'` | 2026-09-02: 87 testes, 0 falhas. **2026-09-05: as classes de `questao`/`catalogo` continuam todas verdes na suíte completa.** |
| Frontend | TU frontend | PASSOU | `npm test` | 2026-09-02: 173 testes, 0 falhas. **2026-09-05: 427 testes em 80 arquivos, 0 falhas.** |

| Integração | Autorização slice web (`questao`/`catalogo`) | **PASSOU** | `QuestaoAdminControllerTest` 4/4 + `CatalogoControllerTest` 8/8 | **Nova na tarefa 6.0.** 403 por perfil insuficiente com `PublicacaoService`/`CatalogoService` reais sob `@EnableMethodSecurity` |
| Frontend | `materias-page`, `catalogo-page`, `questoes-page` | **PASSOU** | `materias-page.spec.ts` 17/17, `catalogo-page.spec.ts` 17/17, `questoes-page.spec.ts` 11/11 | **Novos na tarefa 6.0.** Diálogos, erro por `code` e troca de aba |

- **Cobertura frontend:** 2026-09-02 — 86,5% statements, 81,64% branches, 82,14% functions, 95,03% lines. 2026-09-05 (revisão) — 89,47% / 85,27% / 80,27% / 93,83%. **2026-09-05 (tarefa 6.0) — 89,45% / 84,88% / 80,18% / 93,93% (473 testes)**, acima do piso de 80%.
- **Cobertura backend:** a rodada de 2026-09-02 registrou `./mvnw verify -DskipTests`, que **pula os testes e não exercita o gate JaCoCo** — a afirmação original era inválida. Na revalidação de 2026-09-05, o `./mvnw verify` completo rodava 535 testes e falhava em 1 (`FlywayMigracaoTest.TI06`, de `prd-sessao-de-estudo`), interrompendo o build antes do `jacoco:check`. **2026-09-05 (tarefa 6.0) — corrigido: `./mvnw verify` roda 571/571 testes e `jacoco:check` reporta "All coverage checks have been met."**

## Acessibilidade
- [x] Navegação por teclado (Tab, Enter, Esc), verificado em E2E-15.
- [x] Elementos interativos com rótulos descritivos, verificado em E2E-15.
- [x] Imagens com texto alternativo apropriado, verificado por axe em E2E-15.
- [x] Contraste de cores adequado, verificado por axe em E2E-15.
- [x] Formulários com rótulos associados aos campos, verificado em E2E-15.
- [x] Mensagens de erro claras e acessíveis, verificado em E2E-08 e axe.
- [x] Fontes com tamanho apropriado, verificado visualmente nos breakpoints capturados.

## Bugs encontrados e corrigidos
| ID | Descrição | Severidade | Status | Correção | Teste de regressão | Evidência |
|----|-----------|-----------|--------|----------|--------------------|-----------|
| P1-3 | Publicação sem disciplina podia vazar 500 e não aparecia entre as pendências. | Alta | RESOLVIDO | `ValidadorPublicacao` valida disciplina; handler genérico de integridade devolve 422 seguro. | `ValidadorPublicacaoTest.retornaDisciplinaComoPendenciaQuandoAusente`; `ManipuladorErrosGlobalTest.trataViolacaoDeIntegridadeComEnvelopeSeguro` | — |

## Conclusão
APROVADO. Os 14 critérios de aceitação foram verificados e atendidos. A suíte Playwright específica passou integralmente; os testes backend e frontend, incluindo cobertura, também passaram. As evidências finais cobrem as quatro telas em desktop e mobile, além do estado vazio.
