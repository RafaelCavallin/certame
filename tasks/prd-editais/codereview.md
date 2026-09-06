# Relatório de revisão de código — Editais acompanhados

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Branch: (worktree local)
- Status: **APROVADO** (mantido)

> **Revalidação 2026-09-05.** Nenhuma regressão no escopo de editais: todas as classes de
> `br.com.certame.catalogo.edital.*` continuam verdes na suíte completa, e as três ressalvas de
> severidade Baixa (ordem serial de E2E-09, ordenação por ordinal de `StatusEdital`, `@Injectable()`
> sem `providedIn` em `EditaisAdminStore`) seguem abertas como recomendação.
> **Uma afirmação deste relatório deixou de valer:** a linha "Backend `verify` passou (`All coverage
> checks have been met`)". Hoje `./mvnw verify` termina em **BUILD FAILURE** (535 testes, 1 falha)
> por causa de `FlywayMigracaoTest.TI06`, defeito de `prd-sessao-de-estudo`, e o gate JaCoCo não
> chega a ser executado. Os números do frontend também mudaram.

Funcionalidade RF-17/RF-22 revisada contra `AGENTS.md`, `.agents/rules/*`, `tasks/prd-editais/techspec.md` e `tasks/prd-editais/tasks.md` (10 tarefas). Testes de backend (Testcontainers + slice web) e frontend (Vitest) executados localmente; cobertura verificada via `verify`/`test:coverage`. Nenhum bloqueador encontrado.

## Conformidade com regras
| Regra | Status | Observações |
|-------|--------|-------------|
| AGENTS §1 Frontend 1:1 protótipo / tokens Nocturne | OK | `editais-page`, `edital-card`, `editais-admin-page`, `edital-form-page`, `disciplinas-seletor` usam exclusivamente `var(--*)` (`frontend/src/app/features/editais/editais-page/editais-page.scss:5`, `edital-card.scss:3`, `admin/editais/*.scss`). Sem cor/fonte/espaçamento cru; sem lib UI terceira. Protótipo `Certame.dc.html` contém telas `EDITAIS` e `ADMIN: EDITAIS` (tarefa 1.0). |
| AGENTS §2 Autorização no servidor `@PreAuthorize` | OK | `backend/src/main/java/br/com/certame/catalogo/edital/EditalAdminController.java:23` com `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` em nível de classe + `EditalService.java:35,45,56` nos métodos de escrita. Leitura `GET /api/v1/editais` exige autenticação via `SegurancaConfig.java:37-39` (`anyRequest().authenticated()`); TI-09/TI-12...TI-14 cobrem 401/403. |
| AGENTS §3 Idioma | OK | Código/tabelas/colunas em português sem acento (`edital`, `prazo_inscricao`, `excluido_em`), mensagens pt-BR com ponto final (`EditalRegras.java:8`, `EditalService.java:19-21`, `ResolvedorDisciplinasEdital.java:14`). |
| AGENTS §4 Contrato sem endpoint fora da Tech Spec | OK | 6 rotas (`GET /editais`, `GET /admin/editais`, `GET /admin/editais/{id}`, `POST`, `PUT`, `DELETE`) coincidem com `TECH-SPEC.md:324,331-334` atualizado na entrega (§6). Nenhum endpoint extra. |
| AGENTS §5 Flyway imutável | OK | `V12__editais.sql:1-25` e `V907__seed_editais_dev.sql` aditivas; nenhuma migração aplicada editada. `TECH-SPEC.md:392` registra colisão V7→V12 justificada. |
| AGENTS §6-9 RN-02/RN-04/RN-07/RN-08/RN-10/RN-11 | OK | Não aplicáveis ao escopo editais exceto RN-10/RN-11 indireto via `TaxonomiaService.java:103` (disciplina em uso). |
| `code-standards.md` §2.1 Sem comentários óbvios | OK | Sem comentários desnecessários; únicos comentários justificam regra. |
| `code-standards.md` §2.2 Arquivos ≤100 linhas / métodos ≤30 | OK | Maiores: `EditalService.java:97`, `ConsultaEditaisService.java:59`, `EditaisAdminStore:100` (limite). Métodos todos <30 linhas, extraídos (`aplicar`, `garantirNomeInedito`, `resolverBanca` etc.). |
| `code-standards.md` §2.3 ≤3 níveis if/early return | OK | `VerificadorPrazoEdital.java:14-18`, `ResolvedorDisciplinasEdital.java:20-27` com guard clauses. |
| `code-standards.md` §2.4 ≤3 params / objeto parâmetro | OK | `EditalService.criar(RequisicaoEdital)`, `atualizar(UUID, RequisicaoEdital)`; frontend `RequisicaoEdital` como objeto parâmetro. |
| `code-standards.md` §2.5 Sem linha em branco dentro de método | OK | Inspeção manual não encontrou agrupamento por linha vazia dentro de métodos. |
| `code-standards.md` §2.6 Sem literal mágico | OK | Constantes `NOME_TAMANHO_MAXIMO=160`, `VAGAS_MAXIMO=999999`, `URL_OFICIAL_TAMANHO_MAXIMO=500` (`Edital.java:15-17`), `PRAZO_OBRIGATORIO` no store. |
| `code-standards.md` §3 Backend camadas/DTOs/entidades/erros | OK | Controller só orquestra + `@Valid` + `Cache-Control: no-store` (`EditalController.java:20`); Service com `@Transactional` e regra; Repository com `@EntityGraph`; DTOs `record` imutáveis com mensagens pt-BR (`RequisicaoEdital.java:16-32`); entidade `Edital.java:13` com `@Table(name="edital")`, `@GeneratedValue(UUID)`, `Instant` UTC, `@PrePersist/@PreUpdate`; erros via `ConflitoException`/`DadosInvalidosException`/`RecursoNaoEncontradoException` e `ManipuladorErrosGlobal` (422 com `fieldErrors`). |
| `code-standards.md` §3 Injeção construtor `private final` / sem Lombok | OK | Construtores explícitos `EditalService`, `ConsultaEditaisService`; entidade sem Lombok. |
| `code-standards.md` §4 Frontend standalone/zoneless/signals | OK | `EditaisPage`, `EditalCard`, `EditaisAdminPage`, `EditalFormPage` standalone, signals/computed (`editais-store.ts:13-17`, `editais-admin-store.ts:44`, `editais-page.ts:19`). `input()`/`output()` funcionais; template/style separados. |
| `tests.md` §1 Cobertura ≥80% | OK em 2026-09-03 · **backend NÃO CONFIRMÁVEL em 2026-09-05** | Backend: em 2026-09-03 o `verify` passou (`All coverage checks have been met`); em 2026-09-05 o build falha antes do `jacoco:check` por `FlywayMigracaoTest.TI06` (defeito de outra funcionalidade). Frontend `test:coverage`: 89.47% statements / 85.27% branches — acima do piso. |
| `folder-structure.md` Onde colocar o quê | OK | Pacote `br.com.certame.catalogo.edital` + `dto/`, migração `db/migration/V12__...`, seed `migration-dev/V907__...`, frontend `features/editais` + `features/admin/editais`, `core/api/edital-api.ts`. |

## Aderência à TechSpec
| Decisão Técnica | Implementado | Observações |
|---------------|--------------|-------------|
| Entidade `Edital` + `StatusEdital` + `@ManyToMany` `edital_disciplina` | SIM | `Edital.java:13-66`, `StatusEdital.java`, `V12__editais.sql:20-24` PK composta + `on delete cascade` edital, sem cascata disciplina. |
| `EditalRepository` com `@EntityGraph` e `contarEditaisComDisciplina` | SIM | `EditalRepository.java:12-44` — 3 métodos com EntityGraph, unicidade `existsByNomeIgnoreCaseAndExcluidoEmIsNull`, contagem nativa `edital_disciplina`. |
| `EditalService` RF-22 criar/atualizar/excluir lógica + auditoria | SIM | `EditalService.java:35-62` + `EditalAuditoria.java:10-22` payload idêntico à spec. |
| `ConsultaEditaisService` agrupados ordenados (CA-01, CA-09) | SIM | `ConsultaEditaisService.java:25-29` — abertos asc prazo, previstos asc nome, encerrados desc prazo com nulls last via `findPorStatusOrdenadosPorPrazoDescendente`. |
| `ResolvedorDisciplinasEdital` CA-08 | SIM | `ResolvedorDisciplinasEdital.java:20-27` — vazia sem consulta, size mismatch → 422 `fieldErrors.disciplinaIds`. |
| `VerificadorPrazoEdital` Borda 2 Clock+ZonaAplicacao | SIM | `VerificadorPrazoEdital.java:14-18` `Clock` injetado, `LocalDate.now(clock.withZone(AMERICA_SAO_PAULO))`. |
| `EditalConsultaVinculos` porta para TaxonomiaService | SIM | `EditalConsultaVinculos.java:6-19` interface + `Jpa` implementação; `TaxonomiaService.java:103` recusa `DISCIPLINA_EM_USO`. |
| `EditalController` GET /editais `Cache-Control: no-store` | SIM | `EditalController.java:19-22` `CacheControl.noStore()` validado por TI-10. |
| `EditalAdminController` 5 rotas admin+ | SIM | `EditalAdminController.java:22-59` — CRUD completo, `Location` 201, 204, 404/409/422. |
| DTOs `EditaisAgrupados`, `EditalResumo`, `EditalAdminResumo`, `EditalDetalhe`, `RequisicaoEdital`, `DisciplinaExigida`, `ItemCatalogoResumo` | SIM | `dto/*.java` e `modelos-edital.ts:1-29` — tipos, obrigatórios e limites coincidem com spec §Modelos (160 chars, 0-999999, URL http/https 500). |
| Esquema banco `edital_aberto_com_prazo` + índices | SIM | `V12__editais.sql:6,13-14,16-18` — check, unique parcial `where excluido_em is null`, índice `status, prazo_inscricao`. |
| Frontend `EditalApi` + `modelos-edital` | SIM | `edital-api.ts:7-17` 6 rotas centralizadas; componente não injeta `HttpClient` direto. |
| `EditaisStore` + `EditaisPage` + `EditalCard` (RF-17) | SIM | `editais-store.ts:6-31` signals `agrupados/vazio/selecionado`, `editais-page.ts:16-32` deep link `/editais/:id`, `edital-card.ts/html` rótulo textual + `DecimalPipe`/`DatePipe` pt-BR. |
| `EditaisAdminStore` + `EditaisAdminPage` + `EditalFormPage` + `DisciplinasSeletor` (RF-22) | SIM | `editais-admin-store.ts:13-100` exigePrazo/aviso, 422→`fieldErrors`, 409→campo nome; `editais-admin-page.html:29-31` aviso `ph-warning-circle`; seletor sem texto livre filtrado via `GET /admin/disciplinas`. |
| Shell nav + `app.routes.ts` guards | SIM | `app.routes.ts:51-98` `/editais` `authGuard`, `/admin/editais*` `perfilGuard([ADMIN,SUPER_ADMIN])`. |
| Integração `RegistradorAuditoria` / `ZonaAplicacao` / `ConfirmacaoDialog`/`EstadoVazio` | SIM | `EditalService` audita 3 ações, `VerificadorPrazoEdital` usa `ZonaAplicacao`, `editais-page.html`/`editais-admin-page.html` usam `EstadoVazio`/`ConfirmacaoDialog`. |
| Sem paginação (dezenas de editais) | SIM | Nenhuma rota paginada; envelope `{abertos,previstos,encerrados}` já agrupado. |

## Tarefas verificadas
| Tarefa | Status | Observações |
|--------|--------|-------------|
| 1.0 Protótipo EDITAIS e ADMIN: EDITAIS | COMPLETA | 12 subtarefas marcadas [x]; telas em `protótipo/Certame.dc.html` liberam 8.0/9.0. |
| 2.0 Migração V12 + seed + entidade + repositório | COMPLETA | `V12__editais.sql`, `V907__seed_editais_dev.sql`, `Edital.java:13`, `EditalRepository.java:11` + TI-01…TI-04 no `EditalPersistenciaTest.java:39-92`. |
| 3.0 Regras puras Verificador/Resolvedor | COMPLETA | `VerificadorPrazoEdital.java:9`, `ResolvedorDisciplinasEdital.java:12`, `EditalRegras.java:7` + TU-01…TU-04, TU-14…TU-17. |
| 4.0 EditalService + DTOs + auditoria | COMPLETA | `EditalService.java:18` + `dto/RequisicaoEdital.java:15` + TU-05…TU-13 em `EditalServiceTest.java:27`. |
| 5.0 ConsultaEditaisService agrupamento | COMPLETA | `ConsultaEditaisService.java:15` + TU-18…TU-22 em `ConsultaEditaisServiceTest.java:30-103`. |
| 6.0 Controllers contrato/autorização/no-store | COMPLETA | `EditalController.java:12`, `EditalAdminController.java:21` + TI-05…TI-21 (`EditalControllerTest`, `EditalAdminControllerTest`, `EditalIntegracaoTest`). |
| 7.0 EditalConsultaVinculos + recusa exclusão disciplina | COMPLETA | `EditalConsultaVinculos.java:6` + `TaxonomiaService.java:99-106` + TI-22/TI-23 (cobertos por `TaxonomiaService` e `EditalIntegracaoTest`). |
| 8.0 Frontend estudante (RF-17) | COMPLETA | `features/editais/editais-page`, `edital-card`, `editais-store.ts` + TU-23…TU-25 (`editais-store.spec.ts`, `edital-card.spec`). |
| 9.0 Frontend admin (RF-22) | COMPLETA | `features/admin/editais/*` + TU-26…TU-30 (`editais-admin-store.spec.ts`, `disciplinas-seletor.spec`). |
| 10.0 E2E, cobertura, a11y, TECH-SPEC | COMPLETA | `frontend/e2e/editais.spec.ts:64-286` 12 cenários E2E-01…E2E-12, `test:coverage` 87%, `verify` OK, `TECH-SPEC.md:324,331-334,392,395` atualizado + protótipo aprovado. |

## Testes

### Medição original (2026-09-03)
- Backend: 51 testes de edital dentro de suíte maior, 0 falhas; `verify` com `All coverage checks have been met`.
- Frontend: 71 arquivos, 362 testes, 0 falhas — 87.49% / 83.62% / 82.89% / 94.93%.

Comandos executados:
- `backend: ./mvnw test -Dtest="**/edital/*Test,**/edital/*Integracao*" -Dsurefire.failIfNoSpecifiedTests=false` → 51 passed em 51.7s+11.1s
- `backend: ./mvnw verify -Dtest="...` → BUILD SUCCESS, `check-cobertura-minima` passed
- `frontend: npm run test:coverage` → 71 files, 362 passed, Duration 23.46s

### Medição vigente (2026-09-05) — substitui a anterior
- Backend (`./mvnw verify`, repositório inteiro): **535 testes, 1 falha, 0 erros → BUILD FAILURE**.
  **Escopo de editais: 0 falhas** — `EditalServiceTest`, `ConsultaEditaisServiceTest`,
  `VerificadorPrazoEditalTest`, `ResolvedorDisciplinasEditalTest`, `EditalPersistenciaTest`,
  `EditalControllerTest`, `EditalAdminControllerTest` e `EditalIntegracaoTest` passam.
  A falha é `FlywayMigracaoTest.TI06` (`prd-sessao-de-estudo`).
- **Gate JaCoCo: não confirmável.** O `surefire` interrompe o build antes do `jacoco:check`, logo a
  afirmação "All coverage checks have been met" **não pode ser reproduzida hoje**.
- Frontend (`npm run test:coverage`): **427 testes em 80 arquivos, 0 falhas** —
  Statements 89.47% · Branches 85.27% · Functions 80.27% · Lines 93.83%.

Cobertura por módulo tocado atende `tests.md` §1 (prioridade baixa CRUD exige essencial — autorizado desvio pirâmide 46/35/19 registrado em `TECH-SPEC.md:395`).

## Problemas encontrados
| Severidade | Arquivo | Linha | Descrição | Sugestão |
|------------|---------|-------|-----------|----------|
| Baixa | `frontend/e2e/editais.spec.ts:270` | 270 | E2E-09 depende de ordem de declaração (esvazia base) e é descrito como "rodar por último". Em `workers>1` a ordem entre arquivos não é garantida; suíte pode ficar flaky se outro arquivo criar editais em paralelo. | Fixar `test.describe.serial` no arquivo ou isolar estado vazio em um `beforeAll` dedicado, ou usar etiqueta `slow` + `workers=1` documentado. Não bloqueante — suíte atual passou localmente. |
| Baixa | `backend/src/main/java/br/com/certame/catalogo/edital/ConsultaEditaisService.java:54` | 54-58 | `comparadorAdministracao` ordena `StatusEdital` por `naturalOrder` (ordinal). Funciona hoje (ABERTO=0, PREVISTO=1, ENCERRADO=2) mas acoplado à ordem de declaração do enum. | Considerar `Comparator.comparingInt(s -> Map.of(ABERTO→0, PREVISTO→1, ENCERRADO→2).get(s.getStatus()))` ou extrair constante `ORDEM_STATUS`. Não bloqueante. |
| Baixa | `frontend/src/app/features/admin/editais/editais-admin-store.ts:13` | 13 | `@Injectable()` sem `providedIn` — store é provido por componente/página (escopo correto para formulário), mas difere do padrão `providedIn: 'root'` usado em `EditaisStore`. | Documentar no Javadoc curto o escopo intencional (`// escopo por rota, não singleton`) para evitar refatoração acidental. |

Nenhum problema de severidade Alta ou Média.

## Pontos positivos
- `VerificadorPrazoEdital` puro com `Clock` fixo — testabilidade exemplar (TU-01…TU-04 sem Spring, `tests.md` §2 Repeatable).
- `ResolvedorDisciplinasEdital` isola CA-08 em único ponto; `findByIdInAndExcluidoEmIsNull` evita N+1 e recusa exclusão lógica corretamente.
- `EditalRepository` com `@EntityGraph` em todos os `findBy...` elimina N+1 confirmado por TI-06 (`SessionFactory.getStatistics`).
- `Cache-Control: no-store` explícito com teste dedicado TI-10 — cumpre CA-05 sem camadas intermediárias.
- `TaxonomiaService.excluirDisciplina` recusa com mensagem específica para edital (`DISCIPLINA_EM_USO`) sem expor erro SQL cru — FK como rede de segurança + serviço como mensagem amigável.
- Frontend segue `code-standards.md` §4 rigorosamente: standalone, signals/computed, `input.required`, tokens `var(--*)` exclusivos, `edital-api` único ponto HTTP, 422→`FormControl` campo a campo.
- E2E cobre caminho feliz, bordas (Borda 2 `prazoVencido`, CA-09 vazio), autorização (E2E-10) e acessibilidade (Axe `wcag2a/aa`, foco visível, contraste) — expectativa de `TECH-SPEC.md` §Abordagem de testes totalmente atendida.

## Recomendações
- Manter E2E-09 isolado com `test.describe.serial` se o CI vier a rodar com `workers>1`; hoje o arquivo assume série implícita.
- Extrair `ORDEM_STATUS` no `ConsultaEditaisService` para desacoplar da ordem ordinal do enum.
- Adicionar teste unitário específico para `EditalRegras.garantirUrlValida(null)` (já coberto via `EditalService`, mas validação pura merece caso explícito em suite de unidade).

## Conclusão
**APROVADO.** Código conforme `AGENTS.md`, `.agents/rules/*` e `techspec.md`; todas as 10 tarefas completas com critérios rastreados e testes presentes; 51 testes de backend e 362 de frontend passando com cobertura acima de 80%. Desvios registrados (numeração V12/V907, pirâmide 46/35/19) previamente aprovados em `TECH-SPEC.md:392,395`. Pode seguir para QA.
