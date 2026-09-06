# Especificação técnica

> **Funcionalidade:** Gestão de usuários (super-admin) · **PRD:** `tasks/prd-gestao-de-usuarios/prd.md` (RF-23…RF-27, RN-05, RN-06, CA-01…CA-14)

## Resumo

O backend concentra toda a gestão em um serviço novo, `GestaoUsuariosService`, dentro do pacote `usuario/`, com autorização `@PreAuthorize("hasRole('SUPER_ADMIN')")` no serviço e auditoria em cada operação via `RegistradorAuditoria`. A listagem usa Specification do Spring Data com filtros por perfil/status e ordenação limitada a `criadoEm`; criação, edição e exclusão executam as salvaguardas de auto-modificação e "último super-admin" antes de persistir. Convites e redefinições administrativas reutilizam o fluxo de token de uso único de `RedefinicaoSenhaService` (`token_senha` + `FinalidadeTokenSenha.CONVITE/REDEFINICAO`), apenas produzindo o token sem passar pelo limitador anônimo.

O frontend adiciona `features/super/usuarios/` com tabela paginada/filtrada e `features/super/usuario-formulario/` como rota própria para criar/editar, guardados por `perfilGuard([PerfilUsuario.SUPER_ADMIN])`. Como o protótipo aprovado carece da opção Super admin, da confirmação de exclusão de usuário, das explicações de salvaguarda e da paginação da tabela, a primeira etapa do sequenciamento é a revisão de `protótipo/Certame.dc.html`; só então a interface é traduzida.

## Arquitetura do sistema

### Visão dos componentes

```text
┌────────────────────────────────────────────────────────────────┐
│ SPA Angular (features/super)                                    │
│  UsuariosPage → UsuarioAdminApi → /api/v1/super/usuarios        │
│  UsuarioFormulario ─┘                                          │
└──────────────────────┬─────────────────────────────────────────┘
                       │ JWT com claim perfil=SUPER_ADMIN
┌──────────────────────┴─────────────────────────────────────────┐
│ Spring Boot — pacote usuario/                                  │
│  UsuarioAdminController (orquestra)                            │
│  GestaoUsuariosService  @PreAuthorize("hasRole('SUPER_ADMIN')")│
│    ├─ UsuarioRepository (listagem, unicidade, salvaguardas)    │
│    ├─ RedefinicaoSenhaService (emitirConvite/Redefinicao)      │
│    ├─ RefreshTokenService (revogação na exclusão)              │
│    └─ RegistradorAuditoria (CRIAR/EDITAR/EXCLUIR/...)          │
└────────────────────────────────────────────────────────────────┘
```

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `usuario/GestaoUsuariosService` | novo | Regras de RF-23…RF-27, salvaguardas, autorização e auditoria |
| `usuario/UsuarioAdminController` | novo | REST de `/api/v1/super/usuarios`; só orquestra |
| `usuario/UsuarioRepository` | modificado | Listagem filtrada/paginada, contador de super-admin, busca ativa por id |
| `usuario/Usuario` | modificado | Método de domínio `editar` e `excluir` (setters hoje só cobrem senha/status/exclusão) |
| `usuario/UsuarioGestaoResumo` (dto) | novo | Projeção de listagem/edição com status e criação |
| `usuario/CriarUsuarioRequest` / `EditarUsuarioRequest` (dto) | novo | Entradas validadas de criar/editar |
| `auth/RedefinicaoSenhaService` | modificado | Métodos `emitirConvite`/`emitirRedefinicao` sem limitador anônimo |
| `auth/TokenSenhaRepository` | modificado | `invalidarPendentes` passa a receber a finalidade |
| `features/super/usuarios/UsuariosPage` | novo | Tabela, filtros, paginação, ações e confirmações |
| `features/super/usuario-formulario/` | novo | Criação/edição com convite e mapeamento de erros 422 |
| `core/api/usuario-admin-api.ts` | novo | Cliente HTTP tipado da área super-admin |

Fluxo de dados: a UI envia DTO validado → controller chama o serviço com o `UUID` do ator do `Authentication` → o serviço aplica regra, salva e registra `auditoria` na mesma transação → tokens de convite/redefinição são agendados `@Async` após o commit, mantendo o padrão de `RedefinicaoSenhaService`.

## Design de implementação

### Principais interfaces

```text
GestaoUsuariosService
  listar(FiltroUsuarios filtro, Paginacao paginacao) -> PaginaResposta<UsuarioGestaoResumo>
  criar(RequisicaoCriarUsuario requisicao) -> UsuarioGestaoResumo
  editar(UUID usuarioId, RequisicaoEditarUsuario requisicao, UUID atorId) -> UsuarioGestaoResumo
  excluir(UUID usuarioId, UUID atorId)
  redefinirSenha(UUID usuarioId)
  reenviarConvite(UUID usuarioId)
```

```text
RedefinicaoSenhaService (métodos novos, sem limitador)
  emitirRedefinicao(Usuario usuario)      // exige ATIVO; invalida REDEFINICAO pendentes
  emitirConvite(Usuario usuario)          // exige CONVITE_PENDENTE; invalida CONVITE pendentes
```

```text
UsuarioRepository (acréscimos)
  Page<Usuario> findAll(Specification<Usuario> spec, Pageable pageable)  // via JpaSpecificationExecutor
  long countByPerfilAndStatusAndExcluidoEmIsNull(PerfilUsuario perfil, StatusUsuario status)
  Optional<Usuario> findByIdAndExcluidoEmIsNull(UUID id)

TokenSenhaRepository
  invalidarPendentes(UUID usuarioId, FinalidadeTokenSenha finalidade, Instant agora)
```

```text
Usuario (métodos de domínio novos)
  editar(String nome, String email, PerfilUsuario perfil, StatusUsuario status)
  excluir()   // preenche excluidoEm
```

Salvaguardas no serviço: ao editar, (1) se `usuarioId == atorId` e a alteração rebaixa o perfil ou desativa o status, lança `ConflitoException("AUTO_MODIFICACAO", ...)` (CA-07); (2) se o alvo é o último super-admin ativo e a alteração o rebaixa/desativa, lança `ConflitoException("ULTIMO_SUPER_ADMIN", ...)` (CA-08); a contagem usa `countByPerfilAndStatusAndExcluidoEmIsNull(SUPER_ADMIN, ATIVO)`. As mesmas salvaguardas valem na exclusão. Perfis editáveis incluem `SUPER_ADMIN` (decisão esclarecida com o usuário), com as salvaguardas acima como defesa.

### Modelos de dados

#### `FiltroUsuarios` — filtros da listagem (serviço)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `perfil` | `PerfilUsuario` | não | Filtra igualdade; `null` sem filtro |
| `status` | `StatusUsuario` | não | Filtra igualdade; `null` sem filtro |

#### `UsuarioGestaoResumo` — projeção da listagem/editar

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `UUID` | sim | Identificador |
| `nome` | `String` | sim | Nome completo |
| `email` | `String` | sim | E-mail normalizado (minúsculo) |
| `perfil` | `PerfilUsuario` | sim | ESTUDANTE/PROFESSOR/ADMIN/SUPER_ADMIN |
| `status` | `StatusUsuario` | sim | ATIVO/INATIVO/CONVITE_PENDENTE |
| `criadoEm` | `Instant` | sim | Data de criação, UTC |

```text
{
  "id": "3f4b2c…",
  "nome": "Marina Duarte",
  "email": "marina.duarte@certame.com",
  "perfil": "PROFESSOR",
  "status": "CONVITE_PENDENTE",
  "criadoEm": "2026-08-01T12:30:00Z"
}
```

#### `RequisicaoCriarUsuario` — criação com convite opcional

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `nome` | `String` | sim | `@NotBlank @Size(min=3, max=120)` |
| `email` | `String` | sim | `@NotBlank @Email @Size(max=255)`, normalizado em minúsculo |
| `perfil` | `PerfilUsuario` | sim | `@NotNull` |
| `status` | `StatusUsuario` | sim | `@NotNull`; quando `enviarConvite=true`, é forçado para `CONVITE_PENDENTE` |
| `enviarConvite` | `boolean` | não | padrão `true` |

```text
{
  "nome": "Marina Duarte",
  "email": "marina.duarte@certame.com",
  "perfil": "PROFESSOR",
  "status": "ATIVO",
  "enviarConvite": true
}
```

> **Variante — sem convite (premissa técnica):** a migração `V1` exige `senha_hash not null` para `ATIVO`/`INATIVO`. Quando `enviarConvite=false`, o serviço gera um hash aleatório descartável (mesmo padrão do hash-isca do login) e nunca o transmite. Isso preserva a regra "nenhuma senha definida por terceiros": a senha real só nasce no consumo do token. O frontend marca o convite por padrão; sem convite, o usuário só entra após "Redefinir senha".

#### `RequisicaoEditarUsuario` — edição de perfil/status

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `nome` | `String` | sim | Mesmas regras da criação |
| `email` | `String` | sim | Mesmas regras da criação; revalida unicidade |
| `perfil` | `PerfilUsuario` | sim | Alvo das salvaguardas |
| `status` | `StatusUsuario` | sim | Alvo das salvaguardas |

#### `ErroApi` — envelope de erro (reuso de `comum/ErroApi`)

| Código | HTTP | Significado |
| --- | --- | --- |
| `RECURSO_NAO_ENCONTRADO` | 404 | Usuário inexistente ou excluído |
| `DADOS_INVALIDOS` | 422 | Bean Validation ou `:sort` fora da lista permitida, com `fieldErrors` |
| `EMAIL_EM_USO` | 409 | E-mail já cadastrado em usuário não excluído (`fieldErrors.email`) |
| `AUTO_MODIFICACAO` | 409 | Super-admin tentando rebaixar/desativar/excluir a si mesmo |
| `ULTIMO_SUPER_ADMIN` | 409 | Tentativa contra o último super-admin ativo |
| `STATUS_INCOMPATIVEL` | 409 | Redefinir senha em usuário não ativo / reenviar convite em usuário não pendente |

```text
{
  "code": "ULTIMO_SUPER_ADMIN",
  "message": "O último super-admin ativo não pode ser rebaixado ou desativado.",
  "fieldErrors": null
}
```

#### Auditoria — registro por operação (CA-14)

| Operação | `acao` | `entidade` | `payload` |
| --- | --- | --- | --- |
| criar | `CRIAR` | `usuario` | `{perfil, status, enviarConvite}` |
| editar | `EDITAR` | `usuario` | `{antes: {nome,email,perfil,status}, depois: {...}}` |
| excluir | `EXCLUIR` | `usuario` | `{perfil, status}` |
| redefinir senha | `REDEFINIR_SENHA` | `usuario` | `null` |
| reenviar convite | `REENVIAR_CONVITE` | `usuario` | `null` |

Nenhuma migração de banco é necessária: `usuario` (V1) e `auditoria` (V2) já suportam tudo; a exclusão é lógica e não há FKs novas.

### Endpoints da API

#### Visão geral

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/v1/super/usuarios` | Lista paginada com filtros e ordenação (RF-23) |
| `POST` | `/api/v1/super/usuarios` | Cria usuário com convite opcional (RF-24) |
| `PATCH` | `/api/v1/super/usuarios/{id}` | Edita nome/e-mail/perfil/status com salvaguardas (RF-25) |
| `DELETE` | `/api/v1/super/usuarios/{id}` | Exclusão lógica com salvaguardas (RF-26) |
| `POST` | `/api/v1/super/usuarios/{id}/redefinir-senha` | Token REDEFINICAO para usuário ATIVO (RF-27) |
| `POST` | `/api/v1/super/usuarios/{id}/reenviar-convite` | Token CONVITE para usuário CONVITE_PENDENTE (RF-24) |

---

#### `GET /api/v1/super/usuarios`

Lista usuários não excluídos com ordenação por criação e filtros opcionais por perfil e status.

**Parâmetros de consulta**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `page` | `int` | 0 | `@Min(0)` |
| `size` | `int` | 20 | `@Min(1) @Max(100)` |
| `sort` | `String` | `criadoEm,desc` | Somente `criadoEm,asc`/`criadoEm,desc`; outro valor → 422 |
| `perfil` | `PerfilUsuario` | — | opcional |
| `status` | `StatusUsuario` | — | opcional |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| 200 | `PaginaResposta<UsuarioGestaoResumo>` | Sucesso, mesmo com lista vazia |
| 422 | `ErroApi(DADOS_INVALIDOS)` | Sort/paginação inválidos |
| 403 | `ErroApi` | Qualquer perfil ≠ SUPER_ADMIN (CA-13) |

**Exemplo — sucesso**

```http
GET /api/v1/super/usuarios?page=0&size=20&sort=criadoEm,desc&perfil=PROFESSOR&status=ATIVO
```

```text
{
  "content": [ { "id": "...", "nome": "Marina Duarte", "email": "marina.duarte@certame.com",
                 "perfil": "PROFESSOR", "status": "ATIVO", "criadoEm": "2026-08-01T12:30:00Z" } ],
  "page": 0, "size": 20, "totalElements": 1
}
```

**Exemplo — lista vazia**

```http
GET /api/v1/super/usuarios?perfil=ESTUDANTE&status=CONVITE_PENDENTE
```

```text
{ "content": [], "page": 0, "size": 20, "totalElements": 0 }
```

> O frontend sustenta ordenação apenas por data de criação (CA-02): os cabeçalhos de coluna clicáveis alternam `asc/desc` no parâmetro `sort`.

---

#### `POST /api/v1/super/usuarios`

Cria o usuário; com `enviarConvite=true`, fixa `CONVITE_PENDENTE` e dispara o link de definição de senha.

**Corpo**

`RequisicaoCriarUsuario` (tabela na seção de modelos).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| 201 | `UsuarioGestaoResumo` | Criado com sucesso; o frontend insere na lista sem recarregar (CA-03) |
| 422 | `ErroApi(DADOS_INVALIDOS)` | Nome/e-mail/perfil/status inválidos |
| 409 | `ErroApi(EMAIL_EM_USO)` | E-mail já cadastrado (CA-04), `fieldErrors.email` |
| 403 | `ErroApi` | Perfil ≠ SUPER_ADMIN |

**Exemplo — sucesso (com convite)**

```http
POST /api/v1/super/usuarios
```

```text
{
  "nome": "Marina Duarte", "email": "marina.duarte@certame.com",
  "perfil": "PROFESSOR", "status": "ATIVO", "enviarConvite": true
}
```

```text
HTTP 201
{
  "id": "3f4b2c…", "nome": "Marina Duarte", "email": "marina.duarte@certame.com",
  "perfil": "PROFESSOR", "status": "CONVITE_PENDENTE", "criadoEm": "2026-08-30T18:00:00Z"
}
```

**Exemplo — e-mail duplicado**

```text
HTTP 409
{
  "code": "EMAIL_EM_USO",
  "message": "Este e-mail já está em uso.",
  "fieldErrors": { "email": "Este e-mail já está em uso." }
}
```

---

#### `PATCH /api/v1/super/usuarios/{id}`

Edita nome, e-mail, perfil e status, aplicando as salvaguardas de auto-modificação e último super-admin.

**Corpo**

`RequisicaoEditarUsuario`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| 200 | `UsuarioGestaoResumo` | Editado com sucesso |
| 404 | `ErroApi(RECURSO_NAO_ENCONTRADO)` | Id inexistente ou excluído |
| 422 | `ErroApi(DADOS_INVALIDOS)` | Campos inválidos |
| 409 | `ErroApi(EMAIL_EM_USO / AUTO_MODIFICACAO / ULTIMO_SUPER_ADMIN)` | Salvaguarda ou unicidade violadas |

**Exemplo — salvaguarda violada**

```text
HTTP 409
{
  "code": "AUTO_MODIFICACAO",
  "message": "Você não pode rebaixar ou desativar o próprio acesso.",
  "fieldErrors": null
}
```

> Qualquer alteração concluída revoga os refresh tokens do usuário editado; o access token remanescente expira em até 15 minutos (ver "Principais decisões").

---

#### `DELETE /api/v1/super/usuarios/{id}`

Exclusão lógica (`excluidoEm`), com revogação de refresh tokens e invalidação dos tokens de senha pendentes.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| 204 | — | Excluído com sucesso |
| 404 | `ErroApi(RECURSO_NAO_ENCONTRADO)` | Id inexistente ou já excluído |
| 409 | `ErroApi(AUTO_MODIFICACAO / ULTIMO_SUPER_ADMIN)` | Salvaguarda violada |

> A linha permanece na base para preservar `questao.autor_id`, `auditoria.ator_id` e os históricos. A
> anonimização é executada sete dias após a exclusão lógica por uma rotina agendada; os identificadores
> estrangeiros permanecem estáveis para que os agregados de desempenho continuem consultáveis.

---

#### `POST /api/v1/super/usuarios/{id}/redefinir-senha`

Envia link de redefinição ao e-mail do próprio usuário; nenhuma senha é gerada ou exibida (CA-12).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| 204 | — | Link enviado; frontend mostra toast "Link de redefinição de senha enviado para {email}." |
| 404 | `ErroApi(RECURSO_NAO_ENCONTRADO)` | Id inexistente ou excluído |
| 409 | `ErroApi(STATUS_INCOMPATIVEL)` | Usuário não está `ATIVO` |

---

#### `POST /api/v1/super/usuarios/{id}/reenviar-convite`

Reenvia o convite a usuário em `CONVITE_PENDENTE`, invalidando convites anteriores.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| 204 | — | Convite reenviado |
| 404 | `ErroApi(RECURSO_NAO_ENCONTRADO)` | Id inexistente ou excluído |
| 409 | `ErroApi(STATUS_INCOMPATIVEL)` | Usuário não está `CONVITE_PENDENTE` |

> O frontend mostra "Reenviar convite" apenas para `CONVITE_PENDENTE` e "Redefinir senha" apenas para `ATIVO`; o servidor valida novamente e responde 409 quando a ação é indevida.

---

## Pontos de integração

- **E-mail transacional:** convites e redefinições usam `EnviadorEmail.enviarConvite` / `enviarRedefinicaoSenha`; envio `@Async` após o commit, com retry interno de 3 tentativas. Falha final → contador `certame.auth.email.falha` e log ERROR sem vazar conteúdo — o fluxo HTTP não falha por e-mail indisponível.
- **Auditoria:** `RegistradorAuditoria` resolve o ator via `SecurityContextHolder` (sub do JWT) e serializa o payload; chamada na mesma transação da operação.
- **Não há dependências externas novas.**
- **Anonimização do histórico:** `AnonimizadorHistoricoUsuario` roda diariamente e anonimiza usuários
  com `excluido_em` há pelo menos sete dias e `anonimizado_em` nulo. O `usuario_id` não muda em
  `resposta`, `sessao_estudo`, `flashcard`, `flashcard_revisao`, `flashcard_sessao`, `marcacao`,
  `caderno`, `deck`, `tentativa_simulado` e agregados; apenas os dados textuais pessoais recebem
  placeholders. `resposta` nunca é atualizada ou removida, preservando RN-02.

## Abordagem de testes

Cobertura mínima **80%** (AGENTS.md / `.agents/rules/tests.md`). Autorização por perfil é prioridade **Alta**: os salvaguardas e o gancho de autorização têm caminho feliz **e** bordas cobertos. Backend: unidade sem Spring com mocks manuais; `@ControllerTest` prova `@PreAuthorize`; `*IntegracaoTest` com Testcontainers PostgreSQL 18. Frontend: Vitest com `HttpTestingController`; E2E com Playwright apenas no fluxo crítico.

### Testes de unidade

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `TU01_criaUsuarioComConviteForcandoStatusPendente` | CA-05 | Status salvo como CONVITE_PENDENTE e `emitirConvite` chamado |
| TU-02 | `TU02_criaUsuarioSemConviteGerandoHashDescartavel` | CA-03 | Usuário persistido com hash interno; nenhum token emitido |
| TU-03 | `TU03_recusaEmailDuplicadoInformandoOCampo` | CA-04 | `EmailEmUsoException` com `fieldErrors.email` |
| TU-04 | `TU04_editaPerfilPromovendoProfessorParaAdmin` | CA-06 | Entidade atualizada, auditoria `EDITAR` com antes/depois |
| TU-05 | `TU05_bloqueiaAutoRebaixamentoOuDesativacao` | CA-07 | `ConflitoException(AUTO_MODIFICACAO)` 409 |
| TU-06 | `TU06_bloqueiaUltimoSuperAdminNoRebaixarOuDesativar` | CA-08 | `ConflitoException(ULTIMO_SUPER_ADMIN)` 409 |
| TU-07 | `TU07_excluiUsuarioLogicamenteRevogandoTokens` | CA-10/CA-11 | `excluidoEm` preenchido; refresh revogado; tokens invalidados; auditoria `EXCLUIR` |
| TU-08 | `TU08_bloqueiaAutoexclusao` | Borda 1 | `AUTO_MODIFICACAO` 409 na exclusão de si mesmo |
| TU-09 | `TU09_redefineSenhaApenasEmUsuarioAtivo` | CA-12 | Emite REDEFINICAO; 409 `STATUS_INCOMPATIVEL` caso contrário |
| TU-10 | `TU10_reenviaConviteApenasEmPendenteInvalidandoAnteriores` | CA-05 | Emite CONVITE; 409 `STATUS_INCOMPATIVEL` caso contrário |
| TU-11 | `TU11_rejeitaSortForaDaListaPermitida` | CA-02 | `DadosInvalidosException` com `fieldErrors.sort` |
| TU-12 | `TU12_registraAuditoriaEmTodaOperacao` | CA-14 | `RegistradorAuditoria.registrar` com ator, ação, entidade e payload |

Estratégia: `GestaoUsuariosService` instanciado com mocks de `UsuarioRepository`, `RedefinicaoSenhaService`, `RefreshTokenService`, `RegistradorAuditoria`; `Clock.fixed` quando o tempo entra na regra. Salvaguardas testadas com stubs de contagem — nenhum mock de tipos da JDK.

### Testes de integração

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | `TI01_listaUsuariosComFiltroDePerfilEStatus` (Integracao) | CA-01/CA-02 | Envelope de paginação correto e itens filtrados |
| TI-02 | `TI02_criaUsuarioComConviteEFluxoCompletoDeAtivacao` | CA-05 | Token CONVITE persiste; e-mail agendado; `POST /auth/senha/redefinir` ativa a conta |
| TI-03 | `TI03_recusaOperacaoSemPapelSuperAdminNoSliceWeb` (Controller) | CA-13 | 403 para ESTUDANTE/PROFESSOR/ADMIN; 401 sem token |
| TI-04 | `TI04_editaUsuarioRevogandoRefreshTokens` (Integracao) | CA-06 | Alteração persistida e sessões revogadas |
| TI-05 | `TI05_excluindoUsuarioOracleEncerraAcesso` (Integracao) | CA-11 | GET/PATCH/DELETE pós-exclusão → 404; login recusado |
| TI-06 | `TI06_auditoriaPersistidaComAtorEEntidade` (Integracao) | CA-14 | Linha em `auditoria` com ação/entidade/payload coerentes |
| TI-07 | `TI07_envelopeDeErroEstavelNasSalvaguardas` (Integracao) | CA-07/CA-08 | `code` estável (`AUTO_MODIFICACAO`/`ULTIMO_SUPER_ADMIN`) |
| TI-08 | `TI08_anonimizaHistoricoAposSeteDiasPreservandoAgregados` (Integracao) | CA-11 | Usuário, conteúdos textuais próprios e payloads de auditoria recebem placeholders; respostas e agregados mantêm volume e acertos |
| TI-09 | `TI09_naoAnonimizaHistoricoAntesDoPrazo` (Integracao) | CA-11 | Usuário excluído há menos de sete dias mantém seus dados e a marca de anonimização fica nula |

Dados de teste isolados: e-mail com `UUID.randomUUID()`; para TI-02, uma consulta direta ao repositório de `token_senha` para capturar o token agendado (`@Async` desabilitado nos testes, como no fluxo de redefinição existente).

### Testes E2E

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | `E2E-01_criaEditaERemoveUsuarioComConfirmacao` | CA-01, CA-03, CA-10 | Playwright: super-admin cria usuário → aparece na lista sem reload → edita perfil → abre confirmação nomeada → exclui → toast e remoção |

> O fluxo de ativação pelo link (CA-05) é provado na camada de integração; o E2E cobre apenas o percurso crítico de tela, de acordo com a pirâmide (~10%).

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Revisão do protótipo** (`protótipo/Certame.dc.html`): adicionar opção Super admin ao filtro e ao segmented de perfil, confirmação de exclusão de usuário (`kind: 'usuario-delete'`), handler de "Reenviar convite", explicação textual das salvaguardas e paginação/ordenação da tabela. Pré-requisito da interface (regra 1 do AGENTS.md).
2. **Domínio e repositório** (`Usuario.editar/excluir`, `UsuarioRepository` com Specification + contador): base das regras e dos `*PersistenciaTest`.
3. **Serviço** (`GestaoUsuariosService` + ajustes em `RedefinicaoSenhaService`/`TokenSenhaRepository.invalidarPendentes`): unidades TU-01…TU-12.
4. **Controller e DTOs** (`UsuarioAdminController` + slice web TI-03/TI-07).
5. **Integração backend** (TI-01, TI-02, TI-04…TI-06).
6. **Frontend** (`core/api/usuario-admin-api`, `UsuariosPage`, `UsuarioFormulario`, rotas `/super/usuarios*` e gate `podeSuper()` no shell; remove o fallback de `rota-inicial` para SUPER_ADMIN).
7. **E2E** (E2E-01 com Playwright).

### Dependências técnicas

- Docker + Testcontainers para a suíte de integração.
- Infra de e-mail (`EnviadorEmail`) já existente; em dev, MailHog do `docker-compose`.
- Protótipo revisado antes de qualquer tela nova.

## Monitoramento e observabilidade

- A fonte primária de observabilidade da funcionalidade é a tabela `auditoria` (consulta direta no banco; painel fora do escopo do PRD).
- Falhas de envio de convite/redefinição → contador `certame.auth.email.falha` (reuso do padrão de `EnviadorEmailSmtp`).
- Logs operacionais: INFO no início de cada operação administrativa com `action` e `usuario_id`; WARN quando falha de e-mail; ERROR só em falha inesperada. Nunca logar e-mail (hash) ou conteúdo de mensagem.

## Considerações técnicas

### Principais decisões

- **Serviço novo em vez de estender `UsuarioService`:** mantém a porta de escrita atual (`cadastrarEstudante`, `trocarSenha`) estável e concentra o papel `SUPER_ADMIN` em uma classe única, alinhado ao padrão já usado em `CatalogoService`/`QuestaoService`.
- **Dois endpoints explícitos (redefinir vs. reenviar):** decisão esclarecida com o usuário; a validação do status no servidor gera `STATUS_INCOMPATIVEL` 409, e a UI apenas esconde o botão inaplicável.
- **Specification + whitelist de sort:** evita JPQL fixa e protege contra campo inválido em `Pageable`; a única ordenação exposta é `criadoEm`, atendendo CA-02 sem abrir o schema.
- **Alteração de perfil/status revoga refresh tokens:** a nova capacidade vale no próximo login; o access token remanescente vive até 15 minutos (TTL atual). Carregar o perfil do banco a cada requisição foi descartado pelo custo extra por request; o risco da janela de 15 min está em "Riscos conhecidos".
- **Anonimização após exclusão:** decisão do produto para CA-11: os dados pessoais são `usuario.nome`,
  `usuario.email` e os conteúdos textuais próprios de `caderno`, `deck` e `flashcard`, além de
  payloads de auditoria associados ao usuário. Todos recebem placeholders; referências UUID e
  métricas numéricas permanecem. A execução ocorre sete dias após `excluido_em`, sem alteração de
  `resposta` ou de seus agregados. A migração `V16__anonimizacao_historico_usuario.sql` adiciona
  `usuario.anonimizado_em` e um índice parcial para a rotina idempotente.
- **Criação sem convite usa hash descartável:** premissa técnica para satisfazer o check `senha_hash not null` de `V1` quando `ATIVO`/`INATIVO` é escolhido; documentada acima como variante de `RequisicaoCriarUsuario`.

### Riscos conhecidos

- **Janela de 15 minutos até a revogação completa:** o access token carrega `perfil` como claim; alteração/exclusão revoga refresh, mas access válido até expirar. Mitigação: revogar refresh tokens e encurtar o TTL do access (mudança futura fora do escopo).
- **Anonimização periódica:** existe uma janela de até 24 horas após o prazo de sete dias, porque a
  rotina roda diariamente. Falhas são reprocessadas na execução seguinte enquanto `anonimizado_em`
  permanecer nulo.
- **Ator da auditoria por `SecurityContextHolder`:** presume JWT com sub UUID — mesmo padrão já em uso em `RegistradorAuditoria`.
- **Refactor de `invalidarPendentes`:** alterar a assinatura para receber a finalidade impacta um método interno em produção; mitigação com testes existentes + TI-02.

### Conformidade com o AGENTS.md e as rules

Lidos: `AGENTS.md`, `.agents/rules/code-standards.md` e `.agents/rules/tests.md`. Restrições aplicadas:

- Autorização validada no servidor com `@PreAuthorize("hasRole('SUPER_ADMIN')")` no serviço (regra 2, CA-13), mesmo com a interface escondendo ações; no frontend, os gates (guards e menu) apenas escondem o item.
- Interface e mensagens em pt-BR; identificadores e códigos `SNAKE_CASE` sem acento; commits em pt-BR sem acento (regra 3).
- Endpoints limitados ao contrato da TECH-SPEC §6 — a linha de `reenviar-convite` foi adicionada à §6 nesta mesma alteração (regra 4).
- A migração `V16__anonimizacao_historico_usuario.sql` adiciona somente a marca e o índice necessários
  à anonimização idempotente (regra 5).
- O usuário em pt-BR mensagens, `fieldErrors` campo a campo em 422; `code` estável para o frontend.
- Cobertura mínima de 80% e prioridade Alta para autorização; testes nomeados `TU-_-`/`TI-_-`/`E2E-_-` com AAA/FIRST.
- Lacunas do protótipo são corrigidas no protótipo antes do frontend (regra 1).

### Conformidade com skills

Skills do projeto aplicáveis verificadas em `.agents/skills/`: `criar-techspec` (seguida na íntegra, template aplicado), `spring-boot-testing` (estratégia TU/TI/E2E derivada), `postgresql-table-design` reuso do schema sem migração nova, `angular-developer` convenções de rotas/signals no frontend. Nenhum desvio registrado; as skills `criar-tasks`, `executar-task`, `executar-review` e `executar-qa` serão reutilizadas nas fases seguintes.

### Arquivos relevantes e dependentes

- `PRD.md` — RF-23…RF-27, RN-05, RN-06, F5.
- `TECH-SPEC.md` (raiz) — §3 (rota `/super/usuarios`), §4 (pacote `usuario/`), §6 (contrato; linha `reenviar-convite` adicionada nesta alteração), §7/§9 (convite e auditoria).
- `.agents/rules/code-standards.md`, `.agents/rules/tests.md`.
- `tasks/prd-acesso-e-conta/techspec.md` — base do fluxo de token CONVITE/REDEFINICAO.
- `backend/src/main/java/br/com/certame/usuario/` (Usuario, UsuarioRepository, UsuarioService, dto).
- `backend/src/main/java/br/com/certame/auth/` (RedefinicaoSenhaService, RedefinicaoSenhaController, TokenSenhaRepository, RefreshTokenService).
- `backend/src/main/java/br/com/certame/comum/` (ErroApi, Paginacao, PaginaResposta, auditoria/, excecao/).
- `backend/src/main/java/br/com/certame/config/SegurancaConfig.java`.
- `frontend/src/app/core/auth/` (`perfilGuard`, `modelos`), `frontend/src/app/core/api/`, `frontend/src/app/shared/ui/confirmacao-dialog/`, `frontend/src/app/layout/shell/`, `frontend/src/app/app.routes.ts`.
- `protótipo/Certame.dc.html` (fonte visual; revisada no passo 1 do sequenciamento).
