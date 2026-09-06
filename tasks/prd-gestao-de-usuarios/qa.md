# Relatório de QA — Gestão de usuários (super-admin)

As evidências da ferramenta de navegador estão salvas em `tasks/prd-gestao-de-usuarios/evidences/`.

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Status: **APROVADO**

> **Revalidação 2026-09-05.** Os 14 critérios de aceitação seguem atendidos, a correção do BUG-01
> (`<h1>` acessível) continua no repositório e todas as classes de teste de
> `br.com.certame.usuario.*` passam. **Duas correções:**
>
> 1. **Números das suítes e do `verify`** — atualizados abaixo. Em 2026-09-05, o `verify`
>    completo terminou com 545 testes, 0 falhas, 0 erros e `jacoco:check` aprovado.
> 2. **CA-11 foi concluído.** O relatório registrava que "a anonimização real do
>    histórico foi adiada porque a tabela `resposta` ainda não existe — será tratada quando
>    `prd-sessao-de-estudo` for implementado". **`prd-sessao-de-estudo` foi implementado e a tabela
>    `resposta` existe** (`V7__sessao_estudo.sql`). O risco deixou de ser "aceito por dependência" e
>    foi encerrado. A decisão de produto foi registrada na TechSpec e a rotina de anonimização após
>    sete dias foi implementada e coberta por integração.
- Total de critérios de aceitação: 14 (CA-01 a CA-14)
- Critérios de aceitação atendidos: 14/14
- Bugs encontrados: 1 (1 corrigido)

> Desde o QA anterior (REPROVADO, mesma data), as tasks 5.0 (integração backend), 6.0 (frontend) e 7.0 (E2E) foram concluídas. Esta execução reexecuta o QA do zero: 42 testes de backend (unidade + slice + integração com Testcontainers), 37 testes de unidade de frontend, 1 E2E Playwright, verificação manual dos 14 critérios de aceitação com navegador, acessibilidade (axe-core, navegação por teclado) e responsividade (mobile/tablet/desktop). Um bug de acessibilidade foi encontrado e corrigido durante o QA (ausência de `<h1>` na tela de usuários).

## Critérios de aceitação verificados
| ID | Critério de aceitação | Casos de teste | Status | Evidência |
|----|-----------------------|----------------|--------|-----------|
| CA-01 | Tela de usuários exibe nome, e-mail, perfil, status e criadoEm com paginação | TU-11b, TI-01, E2E-01 | PASSOU | `lista-desktop-com-dados.png`; `TI01_listaUsuariosComFiltroDePerfilEStatus` PASSOU |
| CA-02 | Filtro por perfil/status e ordenação por criadoEm | TU-11, TU-11b, TI-01 | PASSOU | `lista-estado-vazio-real.png` (filtro `CONVITE_PENDENTE` sem resultado); sort whitelist validado em `UsuarioAdminControllerTest` |
| CA-03 | Criar usuário com e-mail inédito aparece na lista sem reload | TU-01, TU-02, TI-02, E2E-01 | PASSOU | E2E-01 cria e valida a linha na tabela sem reload; `form-criar-vazio.png` |
| CA-04 | E-mail duplicado recusado com mensagem clara | TU-03, TI-07 | PASSOU | `form-criar-email-duplicado.png` — "Este e-mail já está em uso." |
| CA-05 | Convite marcado deixa status CONVITE_PENDENTE, envia link e exige definição de senha (RN-05) | TU-01, TU-10, TI-02 | PASSOU | `ca05-reenviar-convite-toast.png`; `TI02_criaUsuarioComConviteEFluxoCompletoDeAtivacao` PASSOU (token persiste e `POST /auth/senha/redefinir` ativa a conta) |
| CA-06 | Editar perfil professor→admin reflete na próxima requisição | TU-04, TI-04 | PASSOU | `TI04_editaUsuarioRevogandoRefreshTokens` PASSOU — refresh token revogado (401 `SESSAO_EXPIRADA`) |
| CA-07 | Super-admin não pode rebaixar/desativar a si mesmo | TU-05, TU-08, TI-07 | PASSOU | `ca07-auto-modificacao-bloqueada.png`, `ca07-erro-exclusao-proprio.png` — mensagem "Você não pode rebaixar ou desativar o próprio acesso." |
| CA-08 | Último super-admin ativo não pode ser rebaixado/desativado | TU-06, persistencia `deveContarSuperAdminsAtivos`, TI-07 | PASSOU | `TI07_envelopeDeErroEstavelNasSalvaguardas` PASSOU; salvaguarda de contagem intercepta antes mesmo da tentativa de auto-exclusão do único super-admin dev |
| CA-09 | Usuário INATIVO bloqueado no login com mensagem genérica (RN-06) | Regressão `regressao_usuarioEditadoParaInativoRecebeCredencialInvalidaGenericaNoLogin` | PASSOU | Editar usuário para INATIVO e tentar login retorna 401 `CREDENCIAL_INVALIDA` genérico |
| CA-10 | Exclusão exige confirmação explícita | TU-07, E2E-01 | PASSOU | `ca10-confirmacao-exclusao.png` — modal nomeado com o nome do usuário afetado |
| CA-11 | Histórico preservado após exclusão lógica, acesso encerrado e sem identificação pessoal | TU-07, persistencia `deveIgnorarUsuarioExcluidoAoBuscarPorId`, TI-05, TI-08, TI-09 | PASSOU | Após sete dias, usuário, conteúdos textuais próprios e payloads de auditoria recebem placeholders; `resposta` mantém 2 linhas e os agregados mantêm volume/acertos; antes do prazo nada é anonimizado |
| CA-12 | Redefinir senha envia link, notifica e não exibe senha | TU-09, TI-03 | PASSOU | `ca12-redefinir-senha-toast.png` — "Link de redefinição de senha enviado para professor@dev.certame.local." |
| CA-13 | Não super-admin recebe 403 no servidor mesmo com UI manipulada | TI-03 (16 casos) | PASSOU | `UsuarioAdminControllerTest` — 403 para ESTUDANTE/PROFESSOR/ADMIN em todos os 6 endpoints, 200/204 para SUPER_ADMIN |
| CA-14 | Auditoria com ator, ação, entidade e data em toda operação | TU-12, TI-06 | PASSOU | `TI06_auditoriaPersistidaComAtorEEntidade` PASSOU — linha em `auditoria` com ator, ação, entidade e payload |

## Testes E2E executados
| ID | Fluxo | Resultado | Observações |
|----|-------|-----------|-------------|
| E2E-01 | `E2E-01_criaEditaERemoveUsuarioComConfirmacao` — super-admin cria → aparece sem reload → edita perfil → confirma exclusão nomeada → toast | PASSOU | Ambiente isolado Playwright (`docker-compose.e2e.yml`, backend porta 3000, frontend porta 5100); 1.7min, sem flakiness em 2 execuções consecutivas (antes e depois da correção do BUG-01) |

## Testes automatizados e cobertura
| Camada | ID | Resultado | Validação/comando | Observações |
|--------|----|-----------|-------------------|------------|
| Unidade | TU-01…TU-12 | PASSOU (13 testes) | `./mvnw test -Dtest=GestaoUsuariosServiceTest` | Todos os casos da TechSpec |
| Persistência | `UsuarioPersistenciaTest` | PASSOU (7 testes) | `./mvnw test -Dtest=UsuarioPersistenciaTest` | Testcontainers postgres:18 |
| Controller slice | TI-03/TI-07 | PASSOU (16 testes) | `./mvnw test -Dtest=UsuarioAdminControllerTest` | 403 x3 perfis x6 endpoints + 200/204 SUPER_ADMIN + 409 salvaguardas |
| Integração | TI-01, TI-02, TI-04, TI-05, TI-06 + regressão CA-09 | PASSOU (6 testes) | `./mvnw test -Dtest=UsuarioAdminIntegracaoTest` | Testcontainers postgres:18, e-mails com UUID, `@Async` desabilitado |
| Integração | TI-08, TI-09 | PASSOU (2 testes) | `./mvnw test -Dtest=AnonimizadorHistoricoUsuarioIntegracaoTest` | Testcontainers postgres:18; valida prazo, placeholders, preservação de `resposta` e agregados |
| Frontend unidade | `usuario-formulario-store`, `usuarios-lista-store`, `usuarios-page`, `usuario-formulario` | PASSOU (37 testes) | `npx ng test --include='src/app/features/super/**/*.spec.ts'` | Inclui novo teste de regressão `exibe um título de nível 1 acessível na página` |
| E2E | E2E-01 | PASSOU | `npm run e2e -- gestao-usuarios.spec.ts` | — |

- **Backend — suíte completa.** 2026-09-03: `./mvnw verify` executado (98 classes, todo o projeto) — 0 falhas, 0 erros; `jacoco:check` passou. Cobertura do pacote `usuario/`: `GestaoUsuariosService` 100% instruções/linhas/métodos, 88% branches; `UsuarioAdminController` 80% instruções, 86% linhas, 100% métodos; `Usuario` 97% instruções.
  **2026-09-05: `./mvnw verify` → 545 testes, 0 falhas, 0 erros (BUILD SUCCESS).** `jacoco:check` passou após a inclusão dos testes TI-08/TI-09; o fixture de `PublicacaoServiceTest` também foi alinhado à regra vigente de disciplina obrigatória.
- **Frontend — suíte completa.** 2026-09-03: 71 arquivos, 360 testes, 0 falhas; Statements 87,5%, Branches 83,58%, Functions 82,96%, Lines 94,94%. **2026-09-05: 80 arquivos, 427 testes, 0 falhas; Statements 89,47%, Branches 85,27%, Functions 80,27%, Lines 93,83%** — todos acima da meta de 80%.

## Acessibilidade
Verificado com axe-core (`@axe-core/playwright`) nas telas `/super/usuarios` e `/super/usuarios/novo`, e navegação manual por teclado.

- [x] Navegação por teclado (Tab, Enter, Esc) — `teclado-foco-visivel.png` mostra foco visível ao tabular pelo menu; tabela, filtros, formulário e modal de confirmação `usuario-delete` são alcançáveis e operáveis pelo teclado
- [x] Elementos interativos com rótulos descritivos — botões "Reenviar convite" (só `CONVITE_PENDENTE`), "Redefinir senha" (só `ATIVO`), paginação e ordenação com `title`/rótulo
- [x] Imagens com texto alternativo (`alt`) apropriado — avatares com iniciais seguem padrão do shell (decorativos, sem `alt` necessário; texto textual ao lado)
- [x] Contraste de cores adequado — tokens Nocturne `var(--*)`; axe-core não reportou violação de `color-contrast`
- [x] Formulários com rótulos associados aos campos — `Nome completo`, `E-mail` com `<label>`/`aria-label`; axe-core não reportou `label` ausente
- [x] Mensagens de erro claras e acessíveis — 422 com "Preencha este campo.", 409 com "Este e-mail já está em uso." e mensagens de salvaguarda, todas em pt-BR e associadas ao campo/contexto
- [x] Fontes com tamanho apropriado — `styles.css` Nocturne, sem violação de zoom/texto reportada

**axe-core — resultado final:** 0 violações em `/super/usuarios` (33 regras verificadas) e 0 violações em `/super/usuarios/novo` (30 regras verificadas). Evidências em `evidences/axe-lista.json` e `evidences/axe-form-criar.json`.

## Visual e responsividade
| Estado/breakpoint | Evidência | Observação |
|---|---|---|
| Desktop (1440px), lista com dados | `lista-desktop-com-dados.png` | Tabela completa, título `Usuários` visível |
| Desktop, lista vazia (filtro aplicado) | `lista-estado-vazio-real.png` | "Nenhum usuário encontrado" + orientação |
| Tablet (768px) | `lista-tablet.png` | Sidebar recolhida ao ícone, tabela legível |
| Mobile (375px) | `lista-mobile.png` | Tabela em `.table-wrap` com `overflow-x:auto` (rola horizontalmente dentro do próprio contêiner, não a página — conforme padrão do projeto) |
| Formulário — vazio | `form-criar-vazio.png` | — |
| Formulário — erros de validação | `form-criar-erros-validacao.png` | "Preencha este campo." nos campos obrigatórios |
| Formulário — erro de negócio (e-mail duplicado) | `form-criar-email-duplicado.png` | — |
| Salvaguarda — auto-modificação | `ca07-auto-modificacao-bloqueada.png`, `ca07-erro-exclusao-proprio.png` | — |
| Confirmação de exclusão nomeada | `ca10-confirmacao-exclusao.png` | — |
| Toast — redefinir senha | `ca12-redefinir-senha-toast.png` | — |
| Toast — reenviar convite | `ca05-reenviar-convite-toast.png` | — |

## Bugs encontrados e corrigidos
| ID | Descrição | Severidade | Status | Correção | Teste de regressão | Evidência |
|----|-----------|------------|--------|----------|--------------------|-----------|
| BUG-01 | `UsuariosPage` (`/super/usuarios`) não tinha nenhum `<h1>` — a página violava a regra de acessibilidade `page-has-heading-one` (axe-core, impacto moderado) e quebrava o padrão do projeto (todas as demais páginas de feature têm `<header><span class="eyebrow">…</span><h1>…</h1><p class="text-muted">…</p></header>`, ex. `simulados-page.html`) | Média | Corrigido | Adicionado `<header>` com `eyebrow` "Administração", `<h1>Usuários</h1>` e subtítulo em `usuarios-page.html:1-6`; regra de espaçamento em `usuarios-page.scss` espelhando `simulados-page.scss` | `usuarios-page.spec.ts` — `exibe um título de nível 1 acessível na página` (falha sem a correção); revalidado com axe-core (0 violações) e `lista-desktop-com-dados.png` atualizado | `evidences/axe-lista.json`, `evidences/lista-desktop-com-dados.png` |

## Conclusão
QA **APROVADO**. Os 14 critérios de aceitação foram verificados; o CA-11 agora inclui a anonimização posterior de dados pessoais sem alterar o histórico numérico nem os agregados.

Um bug de acessibilidade (ausência de `<h1>` na listagem) foi encontrado durante a verificação com axe-core, corrigido na causa raiz (adição do cabeçalho semântico seguindo o padrão já usado em outras páginas do projeto) e coberto por teste de regressão; a suíte completa foi reexecutada após a correção sem novas quebras.

**Riscos conhecidos (atualizado em 2026-09-05):**

- **Anonimização do histórico (CA-11) — concluída.** O registro
  original dizia que a anonimização "foi adiada porque a tabela `resposta` ainda não existe — será
  tratada quando `prd-sessao-de-estudo` for implementado". **`prd-sessao-de-estudo` foi
  implementado e a tabela `resposta` existe** desde `V7__sessao_estudo.sql`. Hoje
  `GestaoUsuariosService.excluir` faz a exclusão lógica (`excluidoEm`) e o componente agendado
  `AnonimizadorHistoricoUsuario` substitui os dados pessoais após sete dias. A rotina mantém os
  identificadores das respostas e reconstrói os agregados sem perda numérica.
- **Janela de até 15 minutos** entre a edição/exclusão de um usuário e a expiração do *access token*
  remanescente: decisão de *trade-off* já registrada, **inalterada**.
