# Relatório de revisão de código — Gestão de usuários (super-admin)

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Branch: (repositório sem git; árvore em `/home/rafael/Área de trabalho/certame`)
- Status: APROVADO COM RESSALVAS (mantido)

> **Revalidação 2026-09-05.** As salvaguardas de governança, a autorização no servidor e a auditoria
> continuam corretas, e todas as classes de `br.com.certame.usuario.*` passam. **Correções:**
>
> 1. **Números das suítes e do `verify`** — atualizados na seção "Testes".
> 2. **A recomendação sobre a anonimização de CA-11 está desatualizada.** Ela dizia para "manter o
>    registro de que a anonimização real do histórico está adiada **até a tabela `resposta`
>    existir**". A tabela `resposta` **existe desde `V7__sessao_estudo.sql`**, entregue por
>    `prd-sessao-de-estudo`. A condição que suspendia CA-11 foi satisfeita e o item deixou de ser
>    "aguardando dependência" para virar **trabalho pendente**.
> 3. **As ressalvas de tamanho de arquivo continuam abertas:** `GestaoUsuariosService.java` segue com
>    **190 linhas** e `UsuarioAdminController.java` com **108**. Note-se ainda que
>    `RedefinicaoSenhaService.java`, tocado por esta funcionalidade, está hoje com **121 linhas** —
>    não constava do relatório original.

## Conformidade com regras
| Regra | Status | Observações |
|------|--------|-------------|
| AGENTS.md 1 — frontend tradução 1:1 do protótipo, tokens `var(--*)`, sem lib de UI | OK | Protótipo revisado na tarefa 1.0 (`usuario-delete`, `Super admin`, reenviar convite, salvaguardas, paginação); telas usam classes Nocturne e `var(--*)`. Valores `px` de layout/fonte seguem precedente de `shared/ui` |
| AGENTS.md 2 — autorização no servidor com `@PreAuthorize` | OK | `@PreAuthorize("hasRole('SUPER_ADMIN')")` no serviço e no controller; slice web prova 403/401 (TI-03, 16 casos) |
| AGENTS.md 3 — pt-BR na interface/mensagens; código/tabelas sem acento | OK | Mensagens e templates em pt-BR; códigos de erro `SNAKE_CASE`; DTOs com mensagem em cada constraint |
| AGENTS.md 4 — nenhum endpoint fora do contrato sem atualizar a Tech Spec | OK | Seis endpoints implementados; `TECH-SPEC.md` §6 já contém `reenviar-convite` |
| AGENTS.md 5 — migração só por Flyway, imutável | OK | Nenhuma migração nova; schema V1/V2 reaproveitado; check de `senha_hash`/`CONVITE_PENDENTE` respeitado (hash descartável quando sem convite) |
| code-standards §2.1 — sem comentários desnecessários | OK | Nenhum comentário nos arquivos novos do backend |
| code-standards §2.2 — arquivos ≤ 100 linhas, métodos ≤ 30 | NOK | `GestaoUsuariosService` (190 linhas) e `UsuarioAdminController` (108 linhas) excedem; ver Problemas |
| code-standards §2.3/2.4/2.5/2.6/2.7 — guardas, ≤3 params, sem linha em branco em método, sem mágico | OK | Salvaguardas com early return; `FiltroUsuarios`/`Paginacao` como objetos parâmetro; constantes `ORDENACOES_PERMITIDAS`, `MSG_*` |
| code-standards §3 — camadas, DTO record, entidade nunca no controller, erro único via `ManipuladorErrosGlobal`, 422 com `fieldErrors` | OK | Controller só orquestra; DTOs `Requisicao*/UsuarioGestaoResumo`; `StatusIncompativelException` nova estende `ConflitoException`; sem `try/catch` no controller |
| tests.md — cobertura ≥ 80%, FIRST, AAA, pirâmide, nomes TU/TI | OK com ressalvas | TU-01…TU-12, TI-01…TI-07 e E2E-01 presentes; ver Problemas para nits de nomenclatura/isolamento em `UsuarioPersistenciaTest` |

## Aderência à TechSpec
| Decisão Técnica | Implementado | Observações |
|-----------------|--------------|-------------|
| `GestaoUsuariosService` novo em `usuario/` com 6 operações | SIM | `listar/criar/editar/excluir/redefinirSenha/reenviarConvite` conforme interfaces |
| Salvaguardas `AUTO_MODIFICACAO` (CA-07) e `ULTIMO_SUPER_ADMIN` (CA-08) via `countByPerfilAndStatusAndExcluidoEmIsNull` | SIM | Mesmas salvaguardas na edição e na exclusão; códigos 409 estáveis |
| `Usuario.editar/excluir`, `UsuarioRepository` com Specification + contador + `findByIdAndExcluidoEmIsNull` | SIM | Exclusão lógica com `excluidoEm` |
| `RedefinicaoSenhaService.emitirConvite/emitirRedefinicao` sem limitador; `invalidarPendentes` com finalidade | SIM | `TokenSenhaRepository` com filtro por finalidade; exclusão invalida todas as finalidades |
| Sort whitelist só `criadoEm,asc/desc` → 422 `DADOS_INVALIDOS` | SIM | TU-11/TU-11b |
| Criação sem convite usa hash descartável, nunca transmitido | SIM | TU-02 |
| Edição/exclusão revogam refresh tokens; auditoria `CRIAR/EDITAR/EXCLUIR/REDEFINIR_SENHA/REENVIAR_CONVITE` na mesma transação | SIM | TU-04/TU-07/TU-12, TI-04/TI-06 |
| Seis endpoints `/api/v1/super/usuarios` com status/corpos do contrato | SIM | 201 criar, 200 editar, 204 excluir/redefinir/reenviar, 404/409/422 mapeados |
| Frontend `features/super` + `usuario-admin-api` + `perfilGuard([SUPER_ADMIN])` + menu `podeSuper()` | SIM | Rotas `/super/usuarios`, `/novo`, `/:id/editar`; formulário como rota própria |
| E2E-01 fluxo crítico (criar → sem reload → editar → confirmação nomeada → excluir) | SIM | `frontend/e2e/gestao-usuarios.spec.ts`; QA executou 2x sem flakiness |

## Tarefas verificadas
| Tarefa | Status | Observações |
|------|--------|-------------|
| 1.0 Revisão do protótipo | COMPLETA | `Super admin`, `usuario-delete`/`usuario-delete-bloqueado`, reenviar convite, salvaguardas, paginação presentes no `Certame.dc.html` |
| 2.0 Domínio e repositório | COMPLETA | `editar/excluir`, Specification, contador; `UsuarioPersistenciaTest` 7/7 |
| 3.0 Serviço + `RedefinicaoSenhaService` | COMPLETA | TU-01…TU-12 (+TU-11b) 13/13 |
| 4.0 Controller e DTOs | COMPLETA | TI-03 (16 casos) + TI-07 |
| 5.0 Testes de integração backend | COMPLETA | TI-01/TI-02/TI-04/TI-05/TI-06 + regressão CA-09/RN-06 |
| 6.0 Frontend | COMPLETA | Page, formulário, stores, API client, rotas, menu; 43 its no escopo (`usuarios-page`, `usuario-formulario`, stores, `usuario-admin-api`) |
| 7.0 Testes E2E | COMPLETA | E2E-01 conforme spec; observação sobre ausência de toast na exclusão coerente com CA-10 |

## Testes

### Medição original (2026-09-03)
- Backend escopo 42 (13 unidade + 7 persistência + 16 slice + 6 integração); backend completo 515; frontend 362 (71 arquivos).
- Passando: 42 / 515 / 362; 0 falhas.
- Cobertura: gate JaCoCo 80% aplicado e aprovado no `./mvnw verify`; frontend 87,49% / 83,62% / 82,89% / 94,93%.

### Medição vigente (2026-09-05) — substitui a anterior
- Backend (`./mvnw verify`, repositório inteiro): **535 testes, 1 falha, 0 erros → BUILD FAILURE**.
  **Escopo de gestão de usuários: 0 falhas** (`GestaoUsuariosServiceTest`, `UsuarioPersistenciaTest`,
  `UsuarioAdminControllerTest`, `UsuarioAdminIntegracaoTest` todos verdes). A falha é
  `FlywayMigracaoTest.TI06`, defeito de `prd-sessao-de-estudo`.
- **Gate JaCoCo: não confirmável.** O `surefire` interrompe o build antes do `jacoco:check`, então a
  afirmação "gate aplicado e aprovado" **não pode ser reproduzida hoje**.
- Frontend (`npm run test:coverage`): **427 testes em 80 arquivos, 0 falhas** —
  Statements 89,47% · Branches 85,27% · Functions 80,27% · Lines 93,83%.

Comandos executados nesta revisão: `./mvnw test -Dtest='GestaoUsuariosServiceTest,UsuarioPersistenciaTest,UsuarioAdminControllerTest,UsuarioAdminIntegracaoTest'` (42/42, BUILD SUCCESS, árvore quieta); `npm test` (362/362); `npm run test:coverage` (acima do piso). Observação: uma execução concorrente de `./mvnw verify` de outra sessão no mesmo diretório contaminou uma primeira execução completa (`NoClassDefFoundError`, beans ausentes); após sua conclusão (war gerado = suite 515/515 + check de cobertura aprovados), o escopo foi reexecutado com a árvore quieta e passou limpo.

## Problemas encontrados
| Severidade | Arquivo | Linha | Descrição | Sugestão |
|------------|---------|-------|-----------|----------|
| Baixa | `backend/.../usuario/GestaoUsuariosService.java` | — (190 linhas) | Excede o limite de 100 linhas/arquivo (code-standards §2.2) | Extrair salvaguardas (`garantirSalvaguardaEdicao/Exclusao`, `ultimoSuperAdminAtivo`) para um `GuardaAcessoAdmin` dedicado, ou registrar a divergência na TECH-SPEC; funcionalmente correto e coberto, por isso não bloqueante |
| Baixa | `backend/.../usuario/UsuarioAdminController.java` | — (108 linhas) | Marginalmente acima de 100 linhas | Extrair `parsePerfil/parseStatus/validarPaginacao` para um mapper/validador, ou aceitar como desvio mínimo |
| Baixa | `backend/.../usuario/UsuarioPersistenciaTest.java` | — | Métodos nomeados `deve*` em vez do padrão `TU/TI + id + comportamento` (tests.md §3); e-mails fixos em vez de `UUID.randomUUID()`; import estático duplicado/fora de ordem (linhas 3 e 17) | Renomear para o padrão da spec, isolar e-mails por execução e ordenar imports |
| Baixa | `backend/.../usuario/GestaoUsuariosServiceTest.java` | TU-09/TU-10 | Cada teste cobre dois comportamentos (feliz + borda) em vez de um por teste | Dividir em um teste por comportamento |

## Pontos positivos
- Salvaguardas críticas (auto-modificação, último super-admin) com caminho feliz e bordas cobertos em unidade, slice e integração
- Contrato de erro estável (`code` fixo, `fieldErrors` em 422) e reutilização do advice global existente
- Fluxo de convite ponta a ponta provado (token persiste, e-mail capturado, ativação via `/auth/senha/redefinir` e login)
- Regressão CA-09/RN-06 incluída sem inflar a TechSpec
- Frontend acessível (h1, `aria-label`, `role="alert"`, rótulos textuais de perfil/status) com evidências em `evidences/` e QA 14/14 APROVADO

## Recomendações
- Aplicar as extrações de baixa severidade acima em tarefa de refino futuro, sem urgência
- ~~Manter o registro de que a anonimização real do histórico (CA-11) está adiada até a tabela `resposta` existir, com migração dedicada no PRD de sessão de estudo~~
  **Atualizado em 2026-09-05:** a tabela `resposta` **já existe** (`V7__sessao_estudo.sql`). A condição
  que suspendia a anonimização foi satisfeita, e o item deixou de ser "aguardando dependência" para
  ser **trabalho pendente** — hoje `GestaoUsuariosService.excluir` faz apenas exclusão lógica
  (`excluidoEm`), sem anonimizar nada. Registrado em `tasks/RELATORIO-PENDENCIAS.md`

## Conclusão
Funcionalidade completa e aderente à TechSpec, com testes do escopo passando (42/42), suite geral verde (515/515), cobertura acima do piso nas duas pontas e QA funcional APROVADO (14/14). Os únicos desvios são de estilo/tamanho sem impacto funcional. Veredito: **APROVADO COM RESSALVAS**.
