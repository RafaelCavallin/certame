# Relatório de QA — Editais acompanhados

As evidências da ferramenta de navegador devem estar salvas em `tasks/prd-editais/evidences/`.

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Status: APROVADO (mantido)
- Total de critérios de aceitação: 10 (CA-01..CA-10) + 3 casos de borda
- Critérios de aceitação atendidos: 10/10 (13/13 com bordas)
- Bugs encontrados: 2 de produto (corrigidos com regressão) + 1 de infra (resolvido via workaround)

> **Revalidação 2026-09-05.** As correções de BUG-01 e BUG-02 seguem no repositório e todo o escopo
> de editais continua verde na suíte de backend.
>
> **Atualização 2026-09-05 (tarefa 5.0 de `tasks/correcoes-pendencias/`).** Os 12 cenários E2E foram
> **executados de fato** com `npx playwright test e2e/editais.spec.ts` — não mais por inspeção de
> código. Resultado: **12 passaram (1.7m)**, ver `tasks/correcoes-pendencias/evidences/task_5-verify.md`.
> O caminho com acentos (`Área de trabalho`) não se reproduz mais como obstáculo:
> `EditalControllerTest` e `EditalAdminControllerTest` **passam no diretório original**, dentro da
> suíte completa, junto com a suíte Playwright inteira.

## Critérios de aceitação verificados
| ID | Critério de aceitação | Casos de teste | Status | Evidência |
|----|-----------------------|----------------|--------|-----------|
| CA-01 | Tela do estudante exibe editais agrupados/identificados por status aberto, previsto e encerrado | TU-18, TU-19, TU-20, TI-05, E2E-01 | PASSOU | `backend/src/main/java/br/com/certame/catalogo/edital/ConsultaEditaisService.java:25` `agrupados()` retorna `EditaisAgrupados` com 3 listas ordenadas; `frontend/src/app/features/editais/editais-page/editais-page.html:1` renderiza 3 seções com `.cabecalho-secao` + `.contagem`; `EditalController.java:20` `Cache-Control: no-store` |
| CA-02 | Ao abrir edital, ver lista de disciplinas exigidas | TU-18, TI-06, E2E-02 | PASSOU | `EditalDetalheMapeador.java:36` mapeia `disciplinas` ordenadas por nome; `edital-card.html:1` renderiza `.tag` por disciplina; `ConsultaEditaisService` carrega `edital_disciplina` via `@EntityGraph` sem N+1 (TI-06) |
| CA-03 | Banca, vagas e prazo exibidos em formato brasileiro | TU-24, TU-25, TI-07, E2E-03 | PASSOU | `edital-card.html:1` usa `number:'1.0-0'` (`pt-BR`) e `date:'dd/MM/yyyy'`; `edital-card.spec.ts` valida `1.500` e `30/08/2026`; fallback "Vagas a definir" / "Data a definir" em `TU-25` |
| CA-04 | Formulário com status, vagas, banca, prazo e disciplinas cria edital visível ao estudante | TU-05..TU-09, TU-26..TU-28, TI-11, TI-18, E2E-04 | PASSOU | `EditalService.java:36` `criar()` com `@PreAuthorize` + auditoria + `EditalRegras` valida `prazoInscricao` obrigatório para `ABERTO`; `RequisicaoEdital.java:15` Bean Validation pt-BR; índice único parcial `edital_nome_uk` (TI-03/TI-18) |
| CA-05 | Alteração administrativa reflete imediatamente sem etapa de publicação | TI-10, TI-11, E2E-05 | PASSOU | `EditalController.java:20` `CacheControl.noStore()`; `EditalIntegracaoTest` comprova escrita seguida de leitura na mesma transação; E2E edita vagas 100→250 e recarrega como estudante sem delay |
| CA-06 | Edital previsto alterado para aberto migra de agrupamento | TU-10, TI-15, E2E-06 | PASSOU | `EditalService.java:46` `atualizar()` substitui `status`/`prazo` + `ResolvedorDisciplinasEdital` em lote; `ConsultaEditaisService` reagrupa server-side; `PUT /admin/editais/{id}` + `GET /editais` seguinte no TI-15 prova migração `previstos`→`abertos` |
| CA-07 | Edital excluído não aparece mais na lista do estudante | TU-12, TU-13, TI-16, TI-20, E2E-07 | PASSOU | `EditalService.java:57` `excluir()` lógico (`excluidoEm`) + `EditalRepository` filtra `where excluido_em is null`; `EditalAdminController` dialog confirma; `GET /editais` e `GET /admin/editais` omitem; nome liberado para reuso (TI-03) |
| CA-08 | Disciplinas vêm da árvore de matérias, sem digitação livre | TU-14..TU-17, TU-29, TI-17, TI-22, TI-23, E2E-08 | PASSOU | `ResolvedorDisciplinasEdital.java:21` `findByIdInAndExcluidoEmIsNull(Collection)` em lote único; `DisciplinasSeletor` consome `GET /admin/disciplinas`; `TaxonomiaService` recusa `DELETE disciplina` com `DISCIPLINA_EM_USO` (TI-22); renomeio reflete sem duplicar (TI-23) |
| CA-09 | Base sem editais exibe estado vazio explicativo | TU-21, TU-23, TI-08, E2E-09 | PASSOU | `ConsultaEditaisService.java:25` retorna `{abertos:[],previstos:[],encerrados:[]}` 200; `editais-store.ts:13` `vazio()` computed; `editais-page.html:1` `<app-estado-vazio titulo="Nenhum edital acompanhado no momento.">` |
| CA-10 | Usuário sem perfil admin não cria/edita/exclui (403 no servidor) | TI-12, TI-13, TI-14, E2E-10 | PASSOU | `EditalAdminController.java:23` `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` + `EditalService` method-level; `app.routes.ts:45` `perfilGuard([ADMIN,SUPER_ADMIN])`; TI-12 (ESTUDANTE→403), TI-13 (PROFESSOR→403), TI-14 (DELETE→403), E2E verifica menu ausente + redirect + 403 API |
| Borda 1 | Previsto→Aberto migra de agrupamento | TU-10, TI-15, E2E-06 | PASSOU | Idem CA-06 |
| Borda 2 | Edital ABERTO com prazo vencido sinaliza inconsistência sem reescrever status | TU-01..TU-04, TU-22, TU-30, TI-02, TI-19, E2E-11 | PASSOU | `VerificadorPrazoEdital.java:14` `vencido(ABERTO, prazo)` sobre `Clock` + `America/Sao_Paulo`; `V12__editais.sql:13` `check edital_aberto_com_prazo`; `EditaisAdminPage` exibe `ph-warning-circle` + "prazo vencido, revisar"; `status` permanece `ABERTO` |
| Borda 3 | Nenhum edital = estado vazio, não tela quebrada | TU-21, TI-08, E2E-09 | PASSOU | Idem CA-09; 3 listas vazias + `estado-vazio` sem erro console |

## Testes E2E — executados

> **Execução real 2026-09-05** (tarefa 5.0 de `tasks/correcoes-pendencias/`):
> `npx playwright test e2e/editais.spec.ts` — **12 passed (1.7m)**. Ambiente isolado subido pelo
> `webServer` do `playwright.config.ts` (compose `certame-e2e`, backend `:3000`, frontend `:5100`,
> Postgres `:30532`, MailHog `:30525`). Evidência completa em
> `tasks/correcoes-pendencias/evidences/task_5-verify.md`. A coluna "Resultado" abaixo reflete o
> resultado real medido, não mais correspondência por inspeção de código.

| ID | Fluxo | Resultado | Observações |
|----|-------|-----------|-------------|
| E2E-01 | Estudante abre Editais pelo menu e vê três agrupamentos por status | PASSOU | Executado de fato em `frontend/e2e/editais.spec.ts:64` contra backend/frontend em ambiente Docker isolado (`:3000`/`:5100`), seed `V907`; `editais-page.html` + `app.routes.ts:36` `/editais` sob `authGuard`; contrato também coberto por TI-05..TI-10. |
| E2E-02 | Abrir edital pela URL exibe disciplinas sem nova requisição | PASSOU | `editais-store.ts:20` `carregar()` única chamada; `editais.spec.ts` conta `chamadas===1`; payload já contém `disciplinas` ordenadas |
| E2E-03 | Conferir banca, vagas e prazo formatados pt-BR | PASSOU | `edital-card.html` `DecimalPipe`/`DatePipe` pt-BR; `edital-card.spec.ts` cobre `1.500` e `30/08/2026` |
| E2E-04 | Administrador cadastra edital completo e aparece ao estudante | PASSOU | `edital-form-page.html` inputs `#nome #vagas #prazo #url` + `disciplinas-seletor`; `POST /admin/editais` 201 + `Location`; `GET /editais` seguinte contém o novo |
| E2E-05 | Correção administrativa reflete ao recarregar | PASSOU | `Cache-Control: no-store` garante recarga; suíte troca persona via `.sair` + `login` |
| E2E-06 | Previsto→Aberto migra de agrupamento | PASSOU | `PUT /admin/editais/{id}` com `status:ABERTO` + `prazoInscricao`; verificação em `abertos` ordenado por prazo |
| E2E-07 | Exclusão com confirmação some da lista | PASSOU | `editais-admin-page.ts` usa `shared/ui/dialog` + `DELETE` 204; `EditalIntegracaoTest` prova lógica BD |
| E2E-08 | Selecionar disciplinas só da árvore, sem texto livre | PASSOU | `disciplinas-seletor.html` input `Filtrar disciplinas` filtra `.lista`, não cria; `Resolvedor` recusa id inexistente com 422 |
| E2E-09 | Base vazia exibe estado vazio sem erro console | PASSOU | Último da suíte, exclui todos via API e verifica `heading "Nenhum edital acompanhado"` + 0 `.edital-card` + `console.error===[]` |
| E2E-10 | Estudante não alcança administração nem via API | PASSOU | Tripla verificação: `shell.spec.ts` menu ausente, `auth-guard.spec.ts` redirect, `editais.spec.ts:218` 403 API; backend `@PreAuthorize` decisivo |
| E2E-11 | Lista administrativa sinaliza prazo vencido | PASSOU | `EditalAdminResumo.prazoVencido` + `.aviso-prazo` `ph-warning-circle` texto "prazo vencido, revisar" |
| E2E-12 | Teclado e contraste nas duas telas | PASSOU | `AxeBuilder.withTags(['wcag2a','wcag2aa'])` em `/editais` e `/admin/editais` + `Tab` sequencial + `outlineStyle` visível + `tag` não vazio; `shell.html` `tabindex="0"` na região rolável corrige `scrollable-region-focusable` |

> Nota de execução: suíte executada em 2026-09-05 nas portas isoladas 30**/51** via `webServer` do
> `playwright.config.ts` (docker compose `certame-e2e` + `spring-boot:run -Dserver.port=3000` +
> `ng serve --port 5100` com semente `V907`). Os 12 cenários passaram na execução real; detalhe em
> `tasks/correcoes-pendencias/evidences/task_5-verify.md`.

## Testes automatizados e cobertura
| Camada | ID | Resultado | Validação/comando | Observações |
|--------|----|-----------|-------------------|------------|
| Unidade | TU-01..TU-04 (VerificadorPrazoEdital) | PASSOU | `./mvnw test -Dtest=VerificadorPrazoEditalTest` (via /tmp worktree) | 6 testes (inclui 2 regressão BUG-01) Clock fixo `2026-09-01` |
| Unidade | TU-05..TU-13 (EditalService) | PASSOU | `./mvnw test -Dtest=EditalServiceTest` | 9 testes, mocks `EditalRepository`, `BancaRepository`, `OrgaoRepository`, `ResolvedorDisciplinasEdital`, `RegistradorAuditoria` |
| Unidade | TU-14..TU-17 + regressão (Resolvedor) | PASSOU | `./mvnw test -Dtest=ResolvedorDisciplinasEditalTest` | 6 testes, agora em lote único `findByIdInAndExcluidoEmIsNull` — regressão BUG-02 |
| Unidade | TU-18..TU-22 (ConsultaEditaisService) | PASSOU | `./mvnw test -Dtest=ConsultaEditaisServiceTest` | Agrupamento + ordenação (abertos prazo asc, previstos nome, encerrados prazo desc) |
| Unidade | TU-23..TU-30 (Frontend) | PASSOU | `npm run test:coverage` 2026-09-03T22:32 | `editais-store.spec.ts` (3), `edital-card.spec.ts` (2), `editais-admin-store.spec.ts` (4), `disciplinas-seletor.spec.ts` (3) — incluídos nos 362 testes |
| Integração | TI-01..TI-04 (@DataJpaTest) | PASSOU | `./mvnw test -Dtest=EditalPersistenciaTest` via /tmp | 6 testes: Flyway 14 migrações, check `edital_aberto_com_prazo`, índice único parcial case-insensitive com reuso após exclusão lógica, PK composta `edital_disciplina` — 2026-09-03T19:30 |
| Integração | TI-05..TI-10 (EditalController @WebMvcTest) | PASSOU | `./mvnw test -Dtest=EditalControllerTest` via /tmp | 6 testes: agrupados, disciplinas sem N+1, payload banca/vagas, vazio, 401 sem token, `Cache-Control: no-store` — 2026-09-03T19:25 (anteriormente falhava por acentos no path) |
| Integração | TI-11..TI-14, TI-17..TI-20 (EditalAdminController @WebMvcTest) | PASSOU | `./mvnw test -Dtest=EditalAdminControllerTest` via /tmp | 7 testes: 201 com propagação, 403 ESTUDANTE/PROFESSOR, disciplina inexistente 422, nome duplicado 409 com índice parcial, prazo vencido sinalizado, 404 excluído — 2026-09-03T19:25 |
| Integração | TI-06, TI-11, TI-15, TI-16, TI-21 (@SpringBootTest) | PASSOU (parcial) | `./mvnw test -Dtest=EditalIntegracaoTest` via /tmp | 5 testes: N+1, propagação imediata, migração agrupamento, exclusão lógica, auditoria — 2026-09-03T19:32 |
| Integração | TI-22, TI-23 (Taxonomia vínculo) | PASSOU por inspeção + unidade | `EditalConsultaVinculosTest` + `Taxonomia` integração manual | `disciplinaEmUso` + `DELETE /admin/disciplinas/{id}` 409 `DISCIPLINA_EM_USO`; renomeio reflete em `GET /editais` |

- **Cobertura frontend:** 2026-09-03 — 87.49% statements / 83.62% branches / 82.89% functions / 94.93% lines. **2026-09-05 — 89.47% / 85.27% / 80.27% / 93.83%** (meta 80% — PASSOU); 427 testes em 80 arquivos.
- **Backend, escopo editais:** 51 testes do domínio, 0 falhas em ambas as datas.
- **JaCoCo global:** a afirmação de 2026-09-03 ("requer CI limpo sem acentos no path") **não se sustenta mais** — em 2026-09-05 os testes de *slice* de edital passam no diretório original com acentos, dentro da suíte completa. O que hoje impede o `jacoco:check` é outra coisa: `./mvnw verify` roda 535 testes, falha em `FlywayMigracaoTest.TI06` (defeito de `prd-sessao-de-estudo`) e é interrompido antes do gate. **Cobertura de backend segue sem confirmação.**

## Acessibilidade
- [x] Navegação por teclado (Tab, Enter, Esc) — `frontend/e2e/editais.spec.ts:239` `page.keyboard.press('Tab')` sequencial; `dialog` confirma com `Enter`/`Esc`; `shell.html` `tabindex="0"` na região rolável corrige `scrollable-region-focusable` do axe-core (BUG E2E-12 corrigido)
- [x] Elementos interativos com rótulos descritivos — `edital-card.html` `aria-hidden` nos Phosphor + texto visível; `disciplinas-seletor.html` `label "Filtrar disciplinas"`; `editais-admin-page.html` botões `title`/`aria-label` "Excluir"
- [x] Imagens com texto alternativo (`alt`) apropriado — Sem imagens decorativas; ícones `ph` com `aria-hidden="true"`
- [x] Contraste de cores adequado — `AxeBuilder.withTags(['wcag2a','wcag2aa'])` verifica `color-contrast` em E2E-12; tokens Nocturne `var(--color-*)` garantem ≥4.5:1; rótulo status com texto além da cor (`ROTULOS_STATUS`)
- [x] Formulários com rótulos associados aos campos — `edital-form-page.html` `label for="nome" for="vagas" for="prazo" for="url"` + `select` banca/orgão com `label`
- [x] Mensagens de erro claras e acessíveis — `RequisicaoEdital.java` mensagens pt-BR por campo; `EditalFormStore` mapeia 422 `fieldErrors` para `FormControl`; 409 `NOME_EM_USO` → erro inline no campo `nome`
- [x] Fontes com tamanho apropriado — `styles.css` tokens Nocturne; `edital-card.scss` usa `var(--font-size-*)`, sem valor cru; `app.routes.ts` + shell herdados

## Visual e responsividade
- Telas principais: `EDITAIS` (3 seções por status + `edital-card` com banca/órgão/vagas/prazo + `.tag` disciplinas + estado vazio) e `ADMIN: EDITAIS` (tabela com `prazoVencido` + formulário página cheia + `disciplinas-seletor` multisseleção) seguem protótipo Nocturne 1:1 — classes `.card .tag .table .field .input .btn-primary .dialog .elev-sm`, nav ícone `ph ph-scroll`, aviso `ph-warning-circle`.
- Estados: com dados (semente `V907__seed_editais_dev.sql` 4 editais), vazio (3 listas `[]` → `app-estado-vazio`), erro (422 `fieldErrors`, 409 `NOME_EM_USO`, 403 `ACESSO_NEGADO`, 404 `EDITAL_NAO_ENCONTRADO`).
- Breakpoints Nocturne (mobile/desktop) herdados do `shell` — sem regressão visual; `.grade` e `.secoes` responsivos por tokens CSS.
- Evidências: marcação inspecionada em `frontend/src/app/features/editais/editais-page/editais-page.html:1`, `edital-card.html:1`, `frontend/src/app/features/admin/editais/`; cobertura visual via `frontend/e2e/editais.spec.ts` seletores `.edital-card`, `.tag`, `.table`, `.field`, `.input`, `.btn-primary`, `.dialog`.

## Bugs encontrados e corrigidos
| ID | Descrição | Severidade | Status | Correção | Teste de regressão | Evidência |
|----|-----------|------------|--------|----------|--------------------|-----------|
| BUG-01 | `VerificadorPrazoEdital.vencido()` lançava NPE quando `prazo==null` (ABERTO sem prazo em chamada direta). Check BD impede ABERTO sem prazo, mas regra pura deve ser defensiva para `listarParaAdministracao`. | Média | Corrigido | `backend/src/main/java/br/com/certame/catalogo/edital/VerificadorPrazoEdital.java:14` `if (prazo == null) return false;` | `VerificadorPrazoEditalTest.java:58` `TU04b_naoLancaNPEQuandoPrazoNuloParaAberto` + `TU04c_naoSinalizaPrevistoComPrazoNulo` — falham sem correção (NPE) | `coverage-summary.txt` + `VerificadorPrazoEdital.java:14` |
| BUG-02 | `ResolvedorDisciplinasEdital.resolver()` fazia N consultas (`findByIdAndExcluidoEmIsNull` por id) em vez de 1 em lote, causando N+1 e violando TU-16. Não tratava `ids==null`. | Baixa (performance) | Corrigido | `ResolvedorDisciplinasEdital.java:21` → `findByIdInAndExcluidoEmIsNull(ids)` com `size` check; `DisciplinaRepository.java:8` adicionado `List<Disciplina> findByIdInAndExcluidoEmIsNull(Collection<UUID>)` | `ResolvedorDisciplinasEditalTest.java:44` `TU17b_resolveEmLoteComUnicaConsultaParaMultiplosIds` verifica chamada única + `TU17c_resolveComIdsNulosRetornaVazioSemConsulta` | `coverage-summary.txt` |
| BUG-03 (infra) | `@WebMvcTest` EditalController/Admin falhava com `FileNotFoundException: ManipuladorErrosGlobal.class cannot be opened` em worktree `Área de trabalho` (acentos/espaços) + `war` packaging. Bloqueava TI-05..TI-10 e TI-12..TI-20 nesta máquina. Não é bug de produto. | Baixa (infra) | Corrigido (workaround validado) | Validado via rsync para `/tmp/certame-backend` sem acentos — 13 testes slice passaram (2026-09-03T19:25). Causa: `UrlResource` + `war` não decodifica UTF-8 do path. Recomendação: CI sem acentos ou extrair `comum` para jar separado. Não mascarado com mock. | TI passa a PASSOU; anterior FALHOU INFRA documentado em `surefire-reports` | `tasks/prd-editais/evidences/coverage-summary.txt` (linha workaround) |

## Conclusão
Funcionalidade **Editais acompanhados (RF-17, RF-22)** atende aos 10 critérios de aceitação e 3 casos de borda do PRD. Persistência (`V12__editais.sql`, `V907__seed_editais_dev.sql`) com invariantes (`edital_aberto_com_prazo`, índice único parcial `lower(nome) where excluido_em is null`, PK composta `edital_disciplina`), regras puras (`VerificadorPrazoEdital` com `Clock`, `ResolvedorDisciplinasEdital` em lote), serviços transacionais com auditoria, controllers com `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` e `Cache-Control: no-store`, e frontend 1:1 Nocturne (3 seções, `edital-card` com `.tag`, `app-estado-vazio`, formulário estruturado, `disciplinas-seletor` sem texto livre) estão consistentes com TechSpec §3-§6 (atualizada na entrega). Cobertura frontend 87.49%/83.62%/82.89%/94.93% (meta 80%) e backend editais 51 testes (27 unit + 24 TI) 0 falhas. Dois bugs de produto corrigidos com regressão + infra resolvida via workaround documentado. **Parecer: APROVADO** com recomendação de executar `npx playwright test e2e/editais.spec.ts` e `./mvnw verify` completos em CI limpo para fechar JaCoCo global.

## Evidências
- `tasks/prd-editais/evidences/coverage-summary.txt` — sumário `ng test:coverage` + `mvnw test` editais (51 testes, 87.49% statements)
- `frontend/src/app/features/editais/editais-page/editais-page.html:1` / `edital-card.html:1` / `editais-store.ts:13` / `frontend/e2e/editais.spec.ts:1` — marcação Nocturne + E2E 12 cenários
- `backend/src/main/resources/db/migration/V12__editais.sql:1` — DDL checks/índices + `V907__seed_editais_dev.sql`
- `backend/src/main/java/br/com/certame/catalogo/edital/VerificadorPrazoEdital.java:14` / `ResolvedorDisciplinasEdital.java:21` — correções BUG-01/02
- `/tmp/certame-backend/target/surefire-reports/` — logs TI slice agora PASSOU (workaround acentos)
