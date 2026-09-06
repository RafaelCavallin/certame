# Especificação técnica

> **Funcionalidade:** Acesso e conta · **PRD:** `tasks/prd-acesso-e-conta/prd.md` · **Fase:** F1 — Base
> Deriva de `TECH-SPEC.md` v1.0 (§1–§4, §6 e §7 recortados para RF-01…RF-05).

## Resumo

Esta é a primeira funcionalidade implementada sobre dois scaffolds vazios, e por isso ela funda a espinha dorsal do backend: `SecurityFilterChain` stateless, emissão e validação de JWT, tratamento global de erro em `application/problem+json`, baseline Flyway e a porta de e-mail transacional. O desenho segue a `TECH-SPEC.md` sem inventar: access token JWT de 15 minutos no corpo da resposta, refresh token opaco de 14 dias em cookie `HttpOnly SameSite=Strict` rotacionado a cada uso, BCrypt força 12 e perfis aplicados por `@PreAuthorize` no serviço.

No frontend, a entrega é a camada de acesso do SPA — `AuthStore` por signals com o access token apenas em memória, interceptor com renovação única em 401, guardas de rota e restauração de sessão no bootstrap pelo cookie de refresh — mais as quatro telas em tradução 1:1 do protótipo `protótipo/Certame.dc.html`, sobre o `styles.css` do design system Nocturne (`protótipo/nocturne-7bd49ab8-.../styles.css`), que passa a ser o `frontend/src/styles.css` do projeto.

O protótipo já resolve o painel de acesso em duas colunas, o controle segmentado Entrar / Criar conta, o campo Nome exclusivo do cadastro, o link e o painel de "Esqueci minha senha" com seu estado de confirmação, e o "Sair" no rodapé da navegação. Faltam três peças, tratadas nesta especificação: a tela de definição de nova senha (RF-04), o bloco de erro inline nas abas Entrar e Criar conta (CA-02) e um papel de erro no Nocturne — hoje inexistente, com o protótipo usando hex crus contra a própria regra do sistema.

## Arquitetura do sistema

### Visão dos componentes

```
Angular 22 (SPA)                          Spring Boot 4.1                    PostgreSQL 18
────────────────                          ───────────────                    ─────────────
features/auth/  AcessoPage        POST /api/v1/auth/login        ┌── usuario
                EsqueciSenhaPage  POST /api/v1/auth/cadastro     ├── refresh_token
                DefinirSenhaPage  POST /api/v1/auth/senha/*      └── token_senha
core/auth/      AuthStore         POST /api/v1/auth/refresh
                AuthApi           POST /api/v1/auth/logout                   SMTP
                authInterceptor        │                                     ────
                authGuard              └── EnviadorEmail ──────────────▶ MailHog (dev)
```

**Backend — pacote `br.com.certame`** (renomeado a partir de `com.cavallin.certame`; ver *Principais decisões*)

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `config/SegurancaConfig` | novo | `SecurityFilterChain` stateless, `PasswordEncoder` BCrypt 12, `JwtEncoder`/`JwtDecoder`, `JwtAuthenticationConverter` (claim `perfil` → `ROLE_*`), rotas públicas de `/api/v1/auth/**`. |
| `config/CorsConfig` | novo | Origem do SPA por configuração, `allowCredentials=true` (necessário ao cookie de refresh). |
| `config/PropriedadesSeguranca` | novo | `@ConfigurationProperties("certame.seguranca")`: segredo do JWT, TTLs, atributos do cookie, limite de tentativas. |
| `usuario/Usuario`, `PerfilUsuario`, `StatusUsuario` | novo | Entidade JPA e enums (`ESTUDANTE`, `PROFESSOR`, `ADMIN`, `SUPER_ADMIN`; `ATIVO`, `INATIVO`, `CONVITE_PENDENTE`). |
| `usuario/UsuarioRepository` | novo | `findByEmailAndExcluidoEmIsNull`, `existsByEmail`. |
| `usuario/UsuarioService` | novo | Criação de estudante (RF-02), normalização de e-mail, troca de senha. Única porta de escrita em `usuario`. |
| `auth/AutenticacaoController` | novo | Seis rotas do contrato §6; só orquestra e mapeia DTO. |
| `auth/AutenticacaoService` | novo | Login (RF-01), cadastro com autenticação imediata (RF-02), logout (RF-05), checagem de status (RN-05, RN-06). |
| `auth/TokenService` | novo | Emissão e leitura do access token JWT (HS256, 15 min). |
| `auth/RefreshTokenService` | novo | Emissão, rotação, revogação individual e revogação em massa por usuário. |
| `auth/RedefinicaoSenhaService` | novo | Pedido (RF-03) e consumo do token (RF-04); invalida todos os refresh tokens do usuário. |
| `auth/LimitadorTentativas` | novo | Interface + implementação em memória: 5 tentativas por e-mail em 15 min (TECH-SPEC §4). |
| `auth/RefreshToken`, `auth/TokenSenha` | novo | Entidades dos tokens persistidos como hash SHA-256. |
| `email/EnviadorEmail` | novo | Porta de domínio: `enviarRedefinicaoSenha`, `enviarConvite`. |
| `email/EnviadorEmailSmtp` | novo | Adaptador sobre `JavaMailSender`, assíncrono (`@Async`), com modelo de mensagem em pt-BR. |
| `comum/ManipuladorErrosGlobal` | novo | `@RestControllerAdvice` único → `application/problem+json` com `code`, `message` e `fieldErrors`. |
| `comum/ErroNegocioException` e subtipos | novo | `CredencialInvalidaException`, `EmailEmUsoException`, `TokenInvalidoException`, `MuitasTentativasException`. |
| `CertameApplication` | modificado | `@EnableAsync`, `@EnableScheduling` (expurgo de tokens expirados). |

**Frontend — `frontend/src/app`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `core/auth/auth-store.ts` | novo | Signals `usuario`, `accessToken` (só memória), `carregando`, `erro`; `computed` `autenticado` e `perfil`. |
| `core/auth/auth-api.ts` | novo | Cliente HTTP das seis rotas, `withCredentials: true`. |
| `core/auth/auth-interceptor.ts` | novo | Anexa `Authorization`, renova uma única vez em 401, enfileira concorrentes, redireciona ao login na falha. |
| `core/auth/auth-guard.ts` | novo | `authGuard`, `visitanteGuard` e `perfilGuard(perfis)` (este último já preparado para as fases seguintes). |
| `core/auth/rota-inicial.ts` | novo | Mapeia perfil → rota de Início (RF-01, CA-03). |
| `core/api/erro-api.ts` | novo | Tipagem do envelope `problem+json` e mapeamento `fieldErrors` → `FormControl` (422). |
| `features/auth/acesso-page` | novo | Tela única com abas Entrar / Criar conta; a aba ativa vem da rota (`/login` ou `/cadastro`). |
| `features/auth/esqueci-senha-page` | novo | Pedido de redefinição e tela de confirmação incondicional. |
| `features/auth/definir-senha-page` | novo | Consome `:token`, valida a nova senha, trata token inválido ou expirado. |
| `app.routes.ts`, `app.config.ts` | modificado | Rotas públicas, `provideHttpClient(withInterceptors([...]))`, `provideAppInitializer` da restauração de sessão, `LOCALE_ID` pt-BR. |
| `index.html` | modificado | `lang="pt-BR"`; Inter e Phosphor servidos pelo projeto (ver *Principais decisões*). |
| `styles.css` | modificado | Recebe o `styles.css` do Nocturne na íntegra, acrescido do papel `--color-danger-*`. Nenhum valor existente é alterado. |

**Fluxo de dados.** O login valida credencial e status, emite o access token no corpo e o refresh token no cookie. O SPA guarda o access token apenas em memória; ao recarregar a página, o `provideAppInitializer` chama `POST /auth/refresh` uma vez e restaura sessão e perfil a partir do cookie. Cada uso do refresh o rotaciona: o antigo é marcado como usado e um novo é emitido. O consumo do token de redefinição troca a senha e revoga todos os refresh tokens do usuário.

## Design de implementação

### Principais interfaces

```
AutenticacaoService
  autenticar(RequisicaoLogin, ContextoRequisicao) -> ResultadoAutenticacao
  cadastrarEstudante(RequisicaoCadastro, ContextoRequisicao) -> ResultadoAutenticacao
  renovar(String refreshTokenBruto, ContextoRequisicao) -> ResultadoAutenticacao
  encerrar(String refreshTokenBruto) -> void

RedefinicaoSenhaService
  solicitar(String email) -> void                  // sempre silencioso (RF-03)
  redefinir(String tokenBruto, String novaSenha) -> void

TokenService
  emitirAccessToken(Usuario) -> AccessToken        // valor + instante de expiracao

RefreshTokenService
  emitir(Usuario, ContextoRequisicao) -> String    // devolve o token bruto; persiste o hash
  consumirRotacionando(String tokenBruto) -> Usuario
  revogarTodosDoUsuario(UUID usuarioId) -> void

LimitadorTentativas
  registrarFalha(String chave) -> void
  verificar(String chave) -> void                  // lanca MuitasTentativasException

EnviadorEmail
  enviarRedefinicaoSenha(Usuario, String linkAbsoluto) -> void
```

```
AuthStore (signals)
  entrar(credenciais) -> Promise<void>
  cadastrar(dados) -> Promise<void>
  restaurarSessao() -> Promise<void>               // chamada no bootstrap
  renovar() -> Promise<string>                     // usada pelo interceptor
  sair() -> Promise<void>
```

### Modelos de dados

Todos os contratos JSON usam `camelCase`; as colunas usam `snake_case`. Campos ausentes são serializados como `null` — nunca omitidos.

#### `RequisicaoLogin` — corpo de `POST /auth/login`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `email` | `string` | sim | E-mail válido, normalizado para minúsculas no servidor. Máx. 255. |
| `senha` | `string` | sim | Texto puro sob TLS. Mín. 8, máx. 72 (limite do BCrypt). |

```text
{
  "email": "rafael.cavallin@example.com",
  "senha": "senhaSegura123"
}
```

#### `RequisicaoCadastro` — corpo de `POST /auth/cadastro`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `nome` | `string` | sim | Mín. 3, máx. 120; ao menos duas palavras não são exigidas na v1. |
| `email` | `string` | sim | E-mail válido e único entre os usuários não excluídos. |
| `senha` | `string` | sim | Mín. 8, máx. 72. |

```text
{
  "nome": "Rafael Cavallin",
  "email": "rafael.cavallin@example.com",
  "senha": "senhaSegura123"
}
```

#### `RespostaAutenticacao` — resposta de login, cadastro e refresh

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `accessToken` | `string` | sim | JWT compacto HS256, validade de 15 min. |
| `expiraEm` | `string` | sim | Instante de expiração do access token em ISO-8601 UTC. |
| `usuario` | `UsuarioResumo` | sim | Identidade e perfil, para o SPA resolver a navegação. |

> O refresh token **não** aparece no corpo: viaja no cookie `certame_rt` (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`, `Max-Age` de 14 dias).

```text
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2ZDFm...",
  "expiraEm": "2026-08-29T18:45:00Z",
  "usuario": {
    "id": "6d1f2c9e-6f2a-4a1c-9a3e-1c6b0f5a77d1",
    "nome": "Rafael Cavallin",
    "email": "rafael.cavallin@example.com",
    "perfil": "ESTUDANTE"
  }
}
```

#### `UsuarioResumo` — identidade devolvida ao SPA

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Identificador do usuário. |
| `nome` | `string` | sim | Nome de exibição. |
| `email` | `string` | sim | E-mail normalizado. |
| `perfil` | `string` | sim | `ESTUDANTE`, `PROFESSOR`, `ADMIN` ou `SUPER_ADMIN`. |

```text
{
  "id": "6d1f2c9e-6f2a-4a1c-9a3e-1c6b0f5a77d1",
  "nome": "Rafael Cavallin",
  "email": "rafael.cavallin@example.com",
  "perfil": "ESTUDANTE"
}
```

#### `RequisicaoEsqueciSenha` — corpo de `POST /auth/senha/esqueci`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `email` | `string` | sim | E-mail sintaticamente válido; a existência nunca é revelada (RF-03). |

```text
{ "email": "rafael.cavallin@example.com" }
```

#### `RequisicaoRedefinirSenha` — corpo de `POST /auth/senha/redefinir`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `token` | `string` | sim | Token bruto de 32 bytes em base64url, recebido por e-mail. |
| `senha` | `string` | sim | Nova senha, mín. 8, máx. 72. |

```text
{
  "token": "cJ7t1vQm0Z2xN8pR4sL6yH9bK3fW5aD1eG0oT2uI7c",
  "senha": "novaSenhaSegura123"
}
```

#### `ErroApi` — envelope de erro (`application/problem+json`)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `code` | `string` | sim | Código estável, em maiúsculas, consumido pelo SPA. |
| `message` | `string` | sim | Mensagem em português do Brasil, exibível ao usuário. |
| `fieldErrors` | `object` | não | Mapa campo → mensagem; `null` quando o erro não é de validação. |

| Código | HTTP | Significado |
| --- | --- | --- |
| `DADOS_INVALIDOS` | `422` | Falha de Bean Validation; `fieldErrors` preenchido. |
| `CREDENCIAL_INVALIDA` | `401` | Senha errada, usuário inexistente, inativo (RN-06) ou com convite pendente (RN-05) — resposta idêntica nos quatro casos. |
| `EMAIL_EM_USO` | `409` | Cadastro com e-mail já existente (RF-02). |
| `TOKEN_INVALIDO` | `400` | Token de redefinição inexistente, expirado (60 min) ou já usado (RF-04). |
| `SESSAO_EXPIRADA` | `401` | Refresh token ausente, expirado, já usado ou revogado. |
| `MUITAS_TENTATIVAS` | `429` | Acima de 5 tentativas por e-mail em 15 min; cabeçalho `Retry-After` em segundos. |

```text
{
  "code": "CREDENCIAL_INVALIDA",
  "message": "E-mail ou senha inválidos.",
  "fieldErrors": null
}
```

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Verifique os campos destacados.",
  "fieldErrors": {
    "senha": "A senha deve ter ao menos 8 caracteres.",
    "email": "Informe um e-mail válido."
  }
}
```

#### Esquema de banco — `V1__base_acesso.sql`

Migração versionada e imutável em `backend/src/main/resources/db/migration`, seguindo a §5 da `TECH-SPEC.md`.

| Tabela | Colunas principais | Observações |
| --- | --- | --- |
| `usuario` | `id`, `nome`, `email`, `senha_hash`, `perfil`, `status`, `criado_em`, `atualizado_em`, `excluido_em` | `senha_hash` é nulo enquanto o status é `CONVITE_PENDENTE` (RN-05). Unicidade de e-mail é parcial, ignorando excluídos. |
| `refresh_token` | `id`, `usuario_id`, `token_hash`, `expira_em`, `usado_em`, `revogado_em`, `criado_em`, `ip`, `user_agent` | Apenas o hash SHA-256 é persistido; rotação marca `usado_em`. |
| `token_senha` | `id`, `usuario_id`, `token_hash`, `finalidade`, `expira_em`, `usado_em`, `criado_em` | `finalidade` já contempla `CONVITE`, reaproveitado por RF-24 e RF-27 na F5. |

```sql
create table usuario (
  id            uuid primary key default gen_random_uuid(),
  nome          text not null,
  email         text not null,
  senha_hash    text,
  perfil        text not null
                check (perfil in ('ESTUDANTE','PROFESSOR','ADMIN','SUPER_ADMIN')),
  status        text not null default 'ATIVO'
                check (status in ('ATIVO','INATIVO','CONVITE_PENDENTE')),
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),
  excluido_em   timestamptz
);
create unique index usuario_email_uk on usuario (email) where excluido_em is null;

create table refresh_token (
  id          uuid primary key default gen_random_uuid(),
  usuario_id  uuid not null references usuario(id),
  token_hash  text not null unique,
  expira_em   timestamptz not null,
  usado_em    timestamptz,
  revogado_em timestamptz,
  criado_em   timestamptz not null default now(),
  ip          inet,
  user_agent  text
);
create index refresh_token_ativo_idx on refresh_token (usuario_id)
  where usado_em is null and revogado_em is null;

create table token_senha (
  id         uuid primary key default gen_random_uuid(),
  usuario_id uuid not null references usuario(id),
  token_hash text not null unique,
  finalidade text not null check (finalidade in ('REDEFINICAO','CONVITE')),
  expira_em  timestamptz not null,
  usado_em   timestamptz,
  criado_em  timestamptz not null default now()
);
create index token_senha_usuario_idx on token_senha (usuario_id) where usado_em is null;
```

> **Semente de desenvolvimento:** `db/migration-dev/V900__seed_acesso_dev.sql` cria um usuário por perfil (senha conhecida) e entra apenas no perfil `dev`, por `spring.flyway.locations=classpath:db/migration,classpath:db/migration-dev`. Nunca é carregada em `staging` ou `prod`.

### Endpoints da API

Base `/api/v1`, conforme §6 da `TECH-SPEC.md`. Nenhuma rota nova foi criada além do contrato aprovado.

#### Visão geral

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Autentica por e-mail e senha (RF-01). |
| `POST` | `/api/v1/auth/cadastro` | Cria estudante ativo e já autentica (RF-02). |
| `POST` | `/api/v1/auth/senha/esqueci` | Solicita link de redefinição; sempre 204 (RF-03). |
| `POST` | `/api/v1/auth/senha/redefinir` | Consome o token de uso único e grava a nova senha (RF-04). |
| `POST` | `/api/v1/auth/refresh` | Rotaciona o refresh token e devolve novo access token. |
| `POST` | `/api/v1/auth/logout` | Revoga o refresh token e limpa o cookie (RF-05). |

---

#### `POST /api/v1/auth/login`

Valida credencial e status da conta e abre a sessão.

**Corpo** — `RequisicaoLogin`

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `email` | `string` | — | `@Email`, `@NotBlank`, máx. 255; normalizado para minúsculas. |
| `senha` | `string` | — | `@NotBlank`, 8 a 72 caracteres. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RespostaAutenticacao` | Credencial correta e status `ATIVO`. |
| `401` | `ErroApi` (`CREDENCIAL_INVALIDA`) | Senha errada, e-mail inexistente, status `INATIVO` (RN-06) ou `CONVITE_PENDENTE` (RN-05). |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | E-mail malformado ou campo ausente. |
| `429` | `ErroApi` (`MUITAS_TENTATIVAS`) | Sexta tentativa do mesmo e-mail em 15 min. |

**Exemplo — sucesso**

```http
POST /api/v1/auth/login
```

```text
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "expiraEm": "2026-08-29T18:45:00Z", "usuario": { "id": "6d1f...", "nome": "Rafael Cavallin", "email": "rafael.cavallin@example.com", "perfil": "ESTUDANTE" } }
```

**Exemplo — credencial recusada (as quatro causas respondem igual)**

```http
POST /api/v1/auth/login
```

```text
{ "code": "CREDENCIAL_INVALIDA", "message": "E-mail ou senha inválidos.", "fieldErrors": null }
```

> O servidor executa o `BCrypt.matches` contra um hash-isca quando o e-mail não existe, para que o tempo de resposta não distinga conta existente de inexistente. O SPA mantém o e-mail digitado e devolve o foco ao campo de senha (RF-01, CA-02).

---

#### `POST /api/v1/auth/cadastro`

Cria a conta de estudante e devolve a sessão já aberta (CA-04).

**Corpo** — `RequisicaoCadastro`

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `nome` | `string` | — | `@NotBlank`, 3 a 120 caracteres. |
| `email` | `string` | — | `@Email`, único entre usuários não excluídos. |
| `senha` | `string` | — | `@NotBlank`, 8 a 72 caracteres. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `RespostaAutenticacao` | Conta criada com perfil `ESTUDANTE` e status `ATIVO`. |
| `409` | `ErroApi` (`EMAIL_EM_USO`) | E-mail já cadastrado (CA-05). |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Falha de validação de qualquer campo. |

**Exemplo — e-mail duplicado**

```http
POST /api/v1/auth/cadastro
```

```text
{ "code": "EMAIL_EM_USO", "message": "Este e-mail já está em uso.", "fieldErrors": { "email": "Este e-mail já está em uso." } }
```

> O código vem acompanhado de `fieldErrors` para que o SPA marque o campo sem limpar os demais (CA-05). A unicidade é garantida pelo índice `usuario_email_uk`; a violação de constraint é traduzida para o mesmo 409, evitando corrida entre duas requisições simultâneas.

---

#### `POST /api/v1/auth/senha/esqueci`

Dispara o link de redefinição sem revelar se a conta existe.

**Corpo** — `RequisicaoEsqueciSenha`

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `email` | `string` | — | `@Email`, `@NotBlank`. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `204` | vazio | Sempre que o e-mail for sintaticamente válido — exista ou não a conta (CA-06). |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | E-mail malformado (CA-07). |
| `429` | `ErroApi` (`MUITAS_TENTATIVAS`) | Acima de 5 pedidos do mesmo e-mail em 15 min. |

**Exemplo — sucesso (com e sem conta correspondente)**

```http
POST /api/v1/auth/senha/esqueci
```

```text

```

> O envio do e-mail é assíncrono (`@Async`) e ocorre depois do commit, de modo que a latência percebida seja idêntica nos dois casos (CA-06). Quando a conta não existe, ou está `INATIVO`, nenhum token é gerado e nada é enviado. Um pedido novo invalida os tokens de redefinição pendentes do mesmo usuário.

---

#### `POST /api/v1/auth/senha/redefinir`

Consome o token de uso único e grava a nova senha.

**Corpo** — `RequisicaoRedefinirSenha`

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `token` | `string` | — | `@NotBlank`; comparado por hash SHA-256 contra `token_senha`. |
| `senha` | `string` | — | `@NotBlank`, 8 a 72 caracteres. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `204` | vazio | Senha trocada, token marcado como usado, refresh tokens do usuário revogados (CA-08). |
| `400` | `ErroApi` (`TOKEN_INVALIDO`) | Token inexistente, expirado ou já consumido (CA-09). |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Senha abaixo do mínimo. |

**Exemplo — token expirado ou já usado**

```http
POST /api/v1/auth/senha/redefinir
```

```text
{ "code": "TOKEN_INVALIDO", "message": "Este link expirou ou já foi utilizado. Solicite um novo.", "fieldErrors": null }
```

> Quando a `finalidade` é `CONVITE`, o mesmo endpoint ativa a conta: o status passa de `CONVITE_PENDENTE` para `ATIVO` (RN-05). É o gancho já previsto para RF-24 na F5.

---

#### `POST /api/v1/auth/refresh`

Rotaciona o refresh token do cookie e emite um novo access token. É a rota usada tanto pelo interceptor em 401 quanto pela restauração de sessão no bootstrap do SPA.

**Corpo** — vazio. O token vem no cookie `certame_rt`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RespostaAutenticacao` | Cookie válido e não consumido; novo cookie é emitido na mesma resposta. |
| `401` | `ErroApi` (`SESSAO_EXPIRADA`) | Cookie ausente, expirado, revogado ou já usado. |

> Reuso de um refresh já consumido é tratado como indício de roubo: **todos** os refresh tokens do usuário são revogados e a resposta é 401. O SPA distingue o 401 desta rota (leva ao login sem nova tentativa) do 401 das demais (dispara uma única renovação).

---

#### `POST /api/v1/auth/logout`

Encerra a sessão (RF-05).

**Corpo** — vazio.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `204` | vazio | Sempre; idempotente mesmo sem cookie válido. |

> A resposta apaga o cookie (`Max-Age=0`) e revoga o refresh token no banco. O access token remanescente expira sozinho em até 15 min — aceito pela decisão de sessão stateless da `TECH-SPEC.md` §4. O SPA limpa o `AuthStore` imediatamente, de modo que voltar pelo histórico do navegador não restaura acesso (CA-12).

## Pontos de integração

- **Servidor SMTP (e-mail transacional).** Único serviço externo da funcionalidade. Acessado pela porta `EnviadorEmail`, com adaptador `EnviadorEmailSmtp` sobre `spring-boot-starter-mail`. Em `dev` aponta para o MailHog do Docker Compose (`TECH-SPEC.md` §9); em `staging` e `prod`, para o provedor definido até o fim da F1 (SES ou SendGrid) — a troca é configuração, sem alterar serviço.
- **Autenticação da integração:** credenciais SMTP por variável de ambiente, nunca em `application.properties` versionado.
- **Tratamento de erro:** o envio é assíncrono e não participa da transação. Falha de SMTP é registrada em log com nível `ERROR` e métrica própria, **sem** alterar a resposta 204 de RF-03 — o usuário jamais descobre se houve ou não envio. Três tentativas com espera exponencial (1 s, 4 s, 16 s); esgotadas, o token permanece válido e o usuário pode pedir outro link.
- **Timeout:** 5 s de conexão e 10 s de leitura no `JavaMailSender`.

## Abordagem de testes

Meta de cobertura: **mínimo de 70% em `service`**, conforme `AGENTS.md`. Ferramentas: JUnit Jupiter 6.0.3, AssertJ 3.27.7, Mockito 5.23.0 e Testcontainers 2.0.5 (versões gerenciadas pelo BOM do Spring Boot 4.1.1); Vitest e Playwright no frontend.

### Testes de unidade

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `autenticar` com credencial correta e status ATIVO | CA-01 | Devolve `ResultadoAutenticacao` com perfil do usuário e refresh token emitido. |
| TU-02 | `autenticar` com senha incorreta | CA-02 | Lança `CredencialInvalidaException`; nenhuma sessão é criada. |
| TU-03 | `autenticar` com usuário `INATIVO` | CA-11 | Lança `CredencialInvalidaException` com a mesma mensagem de TU-02. |
| TU-04 | `autenticar` com usuário `CONVITE_PENDENTE` | CA-10 | Lança `CredencialInvalidaException`; nenhum token é emitido. |
| TU-05 | `cadastrarEstudante` com e-mail inédito | CA-04 | Cria usuário `ESTUDANTE`/`ATIVO`, senha em BCrypt, e devolve sessão aberta. |
| TU-06 | `cadastrarEstudante` com e-mail existente | CA-05 | Lança `EmailEmUsoException`; nenhum usuário é persistido. |
| TU-07 | Normalização de e-mail no cadastro e no login | CA-04, CA-05 | `Rafael@X.com` e `rafael@x.com` resolvem para o mesmo usuário. |
| TU-08 | `solicitar` redefinição para e-mail inexistente | CA-06 | Não lança, não gera token e não chama `EnviadorEmail`. |
| TU-09 | `solicitar` redefinição para e-mail existente | CA-06 | Gera token com TTL de 60 min, persiste apenas o hash e chama `EnviadorEmail` uma vez. |
| TU-10 | `redefinir` com token válido | CA-08 | Troca a senha, marca `usado_em` e revoga todos os refresh tokens do usuário. |
| TU-11 | `redefinir` com token expirado | CA-09 | Lança `TokenInvalidoException`; senha inalterada. |
| TU-12 | `redefinir` com token já consumido | CA-09 | Lança `TokenInvalidoException`; senha inalterada. |
| TU-13 | `redefinir` com token de finalidade `CONVITE` | CA-10 | Define a senha e move o status de `CONVITE_PENDENTE` para `ATIVO`. |
| TU-14 | `consumirRotacionando` com refresh válido | CA-01 | Marca o antigo como usado e emite um novo distinto. |
| TU-15 | `consumirRotacionando` com refresh já usado | CA-12 | Lança `SessaoExpiradaException` e revoga todos os tokens do usuário. |
| TU-16 | `TokenService` emite JWT com `sub`, `perfil` e expiração de 15 min | CA-03 | Claims corretos; `exp` a 900 s do `Clock` injetado. |
| TU-17 | `LimitadorTentativas` na sexta falha em 15 min | CA-02 | Lança `MuitasTentativasException` com `Retry-After`. |
| TU-18 | `AuthStore.entrar` em sucesso (Vitest) | CA-01, CA-03 | Popula `usuario` e `accessToken`; `autenticado` passa a `true`. |
| TU-19 | `AuthStore.entrar` em 401 (Vitest) | CA-02 | Sinal `erro` preenchido, `accessToken` permanece nulo, e-mail do formulário intacto. |
| TU-20 | `authInterceptor` renova uma única vez em 401 (Vitest) | CA-01 | Chama `refresh` uma vez, repete a requisição original e não entra em laço. |
| TU-21 | Mapeamento de `fieldErrors` 422 para `FormControl` (Vitest) | CA-05, CA-07 | Cada campo recebe o erro correspondente; campos válidos permanecem limpos. |
| TU-22 | `rotaInicialPorPerfil` para os quatro perfis | CA-03 | Devolve a rota de Início correspondente a cada perfil. |

Componentes cobertos: `AutenticacaoService`, `RedefinicaoSenhaService`, `RefreshTokenService`, `TokenService`, `LimitadorTentativas`, `UsuarioService`, `AuthStore`, `authInterceptor` e o mapeador de erro. Serviços puros com JUnit + Mockito, sem contexto Spring. Mocks apenas para o que é externo — `EnviadorEmail` e o relógio (`Clock` injetado, nunca `Instant.now()` direto). O `PasswordEncoder` usa BCrypt real com força reduzida nos testes, para não penalizar o tempo da suíte.

### Testes de integração

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | `POST /auth/login` com credencial válida (`@WebMvcTest` + `MockMvcTester`) | CA-01 | 200, corpo `RespostaAutenticacao` e cookie `certame_rt` `HttpOnly`/`Secure`/`SameSite=Strict`. |
| TI-02 | `POST /auth/login` com senha errada | CA-02 | 401 com `code: CREDENCIAL_INVALIDA` em `application/problem+json`. |
| TI-03 | `POST /auth/login` com e-mail malformado | CA-02 | 422 com `fieldErrors.email` preenchido. |
| TI-04 | `POST /auth/cadastro` com e-mail inédito | CA-04 | 201, usuário persistido como `ESTUDANTE`/`ATIVO` e sessão aberta. |
| TI-05 | `POST /auth/cadastro` com e-mail duplicado | CA-05 | 409 `EMAIL_EM_USO`; contagem de usuários inalterada. |
| TI-06 | `POST /auth/senha/esqueci` para e-mail existente e inexistente | CA-06 | 204 nos dois casos, com corpo e cabeçalhos idênticos. |
| TI-07 | `POST /auth/senha/esqueci` com e-mail malformado | CA-07 | 422 e nenhuma interação com `EnviadorEmail`. |
| TI-08 | Ciclo completo de redefinição | CA-08 | Pedido gera token, consumo devolve 204, senha nova autentica e a antiga não. |
| TI-09 | Reuso do token de redefinição | CA-09 | Segunda chamada devolve 400 `TOKEN_INVALIDO`. |
| TI-10 | Refresh tokens revogados após redefinição | CA-08 | O cookie anterior passa a devolver 401 `SESSAO_EXPIRADA`. |
| TI-11 | `POST /auth/refresh` rotacionando o cookie | CA-01 | 200, novo cookie diferente do anterior; o anterior deixa de funcionar. |
| TI-12 | `POST /auth/logout` seguido de `refresh` | CA-12 | 204 no logout, cookie limpo, `refresh` posterior devolve 401. |
| TI-13 | Login de `INATIVO` e de `CONVITE_PENDENTE` (`@DataJpaTest` + `@SpringBootTest`) | CA-10, CA-11 | Ambos devolvem 401 com corpo byte a byte idêntico ao de senha errada. |
| TI-14 | Persistência e unicidade em `usuario` (`@DataJpaTest` + Testcontainers) | CA-05 | Índice parcial `usuario_email_uk` rejeita duplicado e aceita reuso após exclusão lógica. |
| TI-15 | Migração Flyway em base limpa | — | `V1__base_acesso.sql` aplica sem erro e cria as três tabelas com as constraints declaradas. |
| TI-16 | Rota protegida sem token e com token de perfil insuficiente | CA-03 | 401 sem token; 403 com perfil insuficiente, decidido no servidor. |
| TI-17 | Sexta tentativa de login do mesmo e-mail | CA-02 | 429 `MUITAS_TENTATIVAS` com `Retry-After`. |

Componentes testados em conjunto: controller + segurança + serviço + repositório + Flyway sobre PostgreSQL 18 real via Testcontainers (`@ServiceConnection`), sem H2 e sem banco em memória. Dados de teste criados por builders no próprio pacote de teste; o `EnviadorEmail` é substituído por um duplo de teste que acumula as mensagens, permitindo asserção sobre destinatário e link sem SMTP real. O contexto Spring é compartilhado entre as classes de integração para manter a suíte rápida.

### Testes E2E

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | Entrar como estudante | CA-01, CA-03 | Da tela de acesso ao Início do estudante, com a navegação do perfil. |
| E2E-02 | Erro de senha preserva o e-mail | CA-02 | Erro inline abaixo da senha; campo de e-mail continua preenchido e o foco vai para a senha. |
| E2E-03 | Criar conta pela aba Criar conta | CA-04 | Conta criada e usuário já autenticado no Início. |
| E2E-04 | Criar conta com e-mail duplicado | CA-05 | Mensagem no campo de e-mail; nome e senha permanecem preenchidos. |
| E2E-05 | Esqueci minha senha com e-mail inexistente | CA-06 | Mesma tela de confirmação exibida para e-mail existente. |
| E2E-06 | Redefinir senha pelo link do MailHog | CA-08 | Link do e-mail abre a tela, nova senha é aceita e permite entrar. |
| E2E-07 | Reabrir link já usado | CA-09 | Aviso de link inválido com a ação de solicitar novo. |
| E2E-08 | Sair pelo rodapé e voltar pelo histórico | CA-12 | Retorno ao login; o botão "voltar" do navegador não restaura a sessão. |
| E2E-09 | Recarregar a página autenticado | CA-01 | Sessão restaurada pelo cookie de refresh, sem passar pelo login. |
| E2E-10 | Percurso completo por teclado nas quatro telas | Experiência do usuário | `Tab` e `Enter` operam tudo, com foco visível em cada controle. |

Execução com Playwright contra o SPA e a API reais, com PostgreSQL e MailHog em contêiner. Os seletores saem da marcação do protótipo (`.field`, `.input`, `.btn-primary`, o controle segmentado das abas), traduzida 1:1 — E2E-06 e E2E-07 dependem do painel de nova senha acrescentado ao protótipo (passo 8a). A verificação de contraste e a navegação por teclado (E2E-10) usam a mesma suíte, com checagem automatizada de acessibilidade sobre as quatro telas.

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Fundação do projeto** — renomear o pacote para `br.com.certame`, acrescentar ao `pom.xml` `spring-boot-starter-validation`, `spring-boot-starter-mail`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-testcontainers`, `org.testcontainers:postgresql` e `org.testcontainers:junit-jupiter`; configurar perfis `dev`/`staging`/`prod` e o datasource. Primeiro porque tudo o mais compila em cima disso.
2. **Migração e persistência** — `V1__base_acesso.sql`, entidades `Usuario`, `RefreshToken` e `TokenSenha`, repositórios e TI-14/TI-15. Antes de qualquer regra, porque o modelo é a base de todos os serviços.
3. **Fundação transversal** — `ManipuladorErrosGlobal` com `problem+json`, hierarquia de exceções, `SegurancaConfig` (BCrypt 12, `SecurityFilterChain`, `JwtEncoder`/`JwtDecoder`, conversor de perfis) e `CorsConfig`. É contrato compartilhado com todas as features seguintes.
4. **Login, refresh e logout** (RF-01, RF-05) — `TokenService`, `RefreshTokenService`, `AutenticacaoService`, `AutenticacaoController` e o `LimitadorTentativas`, com TU-01…04, TU-14…17 e TI-01…03, TI-11…13, TI-16, TI-17.
5. **Cadastro** (RF-02) — `UsuarioService` e a rota de cadastro, com TU-05…07 e TI-04, TI-05. Depende do passo 4 por reaproveitar a emissão de sessão.
6. **Redefinição de senha** (RF-03, RF-04) — porta `EnviadorEmail`, adaptador SMTP, `RedefinicaoSenhaService` e as duas rotas, com TU-08…13 e TI-06…10. Depende do MailHog no Compose.
7. **Núcleo do frontend** — `AuthStore`, `AuthApi`, interceptor, guardas, rotas públicas, restauração de sessão no bootstrap, `LOCALE_ID` pt-BR e `lang="pt-BR"`, com TU-18…22. Já testável contra a API sem as telas finais.
8. **Design: fundação visual** — (8a) copiar o `styles.css` do Nocturne, já com o papel `--color-danger-*`, para `frontend/src/styles.css`, e vendorizar Inter e Phosphor. (8b) **Concluído:** o protótipo foi estendido com o painel `loginTab: 'definir'` (RF-04, nos estados formulário, concluído e link inválido), com o bloco de erro nas abas Entrar e Criar conta (CA-02) e com o papel `--color-danger-*` registrado em `protótipo/nocturne-.../styles.css` e no `readme.md` do sistema. (8c) aprovação visual do usuário — sem ela o passo 9 não começa.
9. **Telas de acesso** — as quatro páginas em tradução 1:1 do protótipo estendido, sem inventar marcação, cor, espaçamento ou raio. O bloco "Entrar como (demonstração)" do protótipo é artifício de demonstração e **não** é implementado.
10. **E2E e endurecimento** — E2E-01…10, verificação de cobertura em `service`, revisão de acessibilidade e de contraste, checagem de que nenhuma mensagem revela existência de conta.

### Dependências técnicas

| Dependência | Situação | Impacto |
| --- | --- | --- |
| Protótipo `Certame.dc.html` + `styles.css` do Nocturne | **Presentes** em `protótipo/` | Nenhum bloqueio: o painel de acesso, o controle de abas e o painel de esqueci-senha já estão desenhados. |
| Extensão do protótipo (painel de nova senha e bloco de erro) | **Feita** | Pendente apenas a aprovação visual do usuário, que libera o passo 9. |
| Inter e Phosphor servidos pelo projeto | A vendorizar | O `styles.css` do Nocturne importa o Inter do Google Fonts e o protótipo carrega o Phosphor do unpkg; em produção ambos passam a ser servidos pelo projeto, sem alterar nenhum valor de token. |
| Provedor de e-mail transacional (SES ou SendGrid) | Pendente até o fim da F1 (`PRD.md` §9) | Não bloqueia: `dev` usa MailHog e a porta `EnviadorEmail` isola a escolha. |
| PostgreSQL 18 e Docker | Necessários para Testcontainers e para o Compose de `dev` | Bloqueiam a suíte de integração. |
| Segredo do JWT (`certame.seguranca.jwt.segredo`, mín. 32 bytes) | A provisionar por ambiente | Bloqueia `staging`/`prod`; em `dev` há valor padrão explicitamente marcado como não produtivo. |
| Playwright | Ausente do `package.json` | Bloqueia o passo 9. |
| Origem do SPA para o CORS | A definir por ambiente | Cookie de refresh exige origem exata e `allowCredentials`. |

## Monitoramento e observabilidade

Sobre Actuator + Micrometer/Prometheus e logs JSON com `traceId`, conforme §9 da `TECH-SPEC.md`.

| Sinal | Tipo | Uso |
| --- | --- | --- |
| `certame.auth.login` | contador, tags `resultado` (`sucesso`, `credencial_invalida`, `bloqueado`) | Detecta pico de falhas e ataque de força bruta. |
| `certame.auth.cadastro` | contador, tag `resultado` | Alimenta a métrica de novos cadastros do PRD. |
| `certame.auth.senha.pedido` e `certame.auth.senha.redefinida` | contadores | A razão entre os dois é a taxa de conclusão de RF-03/RF-04 (meta ≥ 80%). |
| `certame.auth.email.falha` | contador | Falha de SMTP; alerta operacional, invisível ao usuário. |
| `certame.auth.refresh.reuso` | contador | Reuso de refresh já consumido — indício de roubo de token; alerta imediato. |
| `/actuator/health` com indicador de banco | health check | Prontidão do contêiner. |

**Eventos de log.** `INFO` para login bem-sucedido, cadastro, logout e redefinição concluída — sempre com o `id` do usuário, **nunca** com e-mail completo, senha, token bruto ou hash. `WARN` para credencial recusada, token de redefinição inválido e bloqueio por tentativas. `ERROR` para falha de SMTP e reuso de refresh token. O link de redefinição nunca é registrado em log em nenhum nível.

## Considerações técnicas

### Principais decisões

- **Access token em memória e refresh em cookie `HttpOnly`.** Herdado da §11 da `TECH-SPEC.md` — menor superfície de XSS que `localStorage`. O custo é depender de CORS com `allowCredentials` e de uma chamada de restauração no bootstrap; aceito.
- **JWT HS256 com `NimbusJwtEncoder` do Spring Security 7**, em vez de biblioteca de terceiros como jjwt. Sem dependência nova além do starter `oauth2-resource-server`, já gerenciado pelo BOM. A troca para RS256 é configuração, caso surja um segundo consumidor da API.
- **Rotação de refresh com detecção de reuso.** Cada uso invalida o anterior; reuso revoga toda a árvore do usuário. É a proteção padrão para token opaco de longa duração, e custa apenas uma coluna `usado_em`.
- **Resposta idêntica para senha errada, conta inexistente, inativa e com convite pendente** (RN-05, RN-06, CA-11), inclusive no tempo de resposta, por meio de comparação BCrypt contra hash-isca. Sem isso, o login vira um oráculo de existência de cadastro.
- **204 incondicional em "esqueci minha senha" com envio assíncrono após o commit** (CA-06). O envio síncrono vazaria a existência da conta pela latência.
- **Migração do pacote para `br.com.certame`** — decisão do usuário nesta especificação. O scaffold nasceu em `com.cavallin.certame`; com dois arquivos Java a renomeação é trivial agora e cara depois. A §4 da `TECH-SPEC.md` passa a valer literalmente.
- **Porta `EnviadorEmail` com adaptador SMTP** em vez de SDK do provedor. Destrava a F1 sem antecipar a decisão pendente do PRD; MailHog em `dev` permite E2E reais.
- **Reactive Forms, não Signal Forms.** A §2 da `TECH-SPEC.md` admite Signal Forms "onde estável"; na v22 ainda são experimentais, e a camada de acesso não é o lugar de assumir esse risco. Revisar na F3.
- **Limitador de tentativas em memória por instância.** Cobre o requisito da §4 com custo zero de infraestrutura; a interface `LimitadorTentativas` permite trocar por Redis quando ele entrar na F3. Registrado como limitação conhecida.
- **`token_senha.finalidade` já contempla `CONVITE`.** Reaproveitar a mesma tabela e o mesmo endpoint em RF-24 e RF-27 evita duplicar fluxo de token na F5.
- **Papel `--color-danger-*` acrescentado ao Nocturne**, derivado de `#e0666e` e `#e0787f` — os valores que o próprio protótipo já usa no bloco de erro do painel Esqueci. Não é cor nova: é a formalização de uma cor existente como token, para que a validação inline exigida por CA-02, CA-05 e CA-07 (e por todos os formulários das fases seguintes) não espalhe hex cru pelo código.
- **As lacunas de design entram pelo protótipo, não pelo código.** A tela de RF-04 (com os estados formulário, concluído e link inválido) e o bloco de erro das abas Entrar e Criar conta foram acrescentados ao `Certame.dc.html` com componentes Nocturne existentes, para serem aprovados visualmente antes de virarem Angular — mantendo o protótipo como fonte única, conforme a regra 1 do `AGENTS.md`.
- **Inter e Phosphor passam a ser servidos pelo projeto.** O `styles.css` do Nocturne importa o Inter por `@import` do Google Fonts e o protótipo carrega o Phosphor do unpkg; ambos viram assets locais, o que remove terceiros do caminho crítico de renderização sem alterar um único token. Não é biblioteca de UI: são a fonte e o conjunto de ícones que o design system já prescreve.
- **O seletor "Entrar como (demonstração)" não é implementado.** É artifício do protótipo para navegar entre perfis; em produção o perfil vem do JWT.
- **Uma página, duas rotas.** `/login` e `/cadastro` renderizam o mesmo `AcessoPage` com a aba pré-selecionada, conciliando RF-01 (abas na mesma tela) com as rotas declaradas na §3 da `TECH-SPEC.md`.

### Riscos conhecidos

| Risco | Mitigação |
| --- | --- |
| ~~O protótipo não cobria RF-04 nem o erro inline de CA-02.~~ **Resolvido no passo 8b:** ambas as peças foram acrescentadas ao `Certame.dc.html` com componentes Nocturne existentes. | Resta a aprovação visual. O protótipo segue sendo a fonte única — nenhuma marcação nasce direto no Angular. |
| ~~O Nocturne não tinha papel de erro.~~ **Resolvido:** O protótipo usa `#e0666e` e `#e0787f` crus, contra a regra do próprio `readme.md` de nunca fixar hex — e formulários com validação inline aparecem em quase toda tela do produto. | O papel `--color-danger-*` foi registrado no `styles.css` do Nocturne e no `readme.md` a partir desses mesmos valores: nenhuma cor nova é inventada, e nenhuma tela futura precisa repetir hex cru. Ajuste de design system, registrado aqui e válido para as demais fases. |
| **Contradição entre documentos sobre limite de tentativas.** A §4 da `TECH-SPEC.md` exige 5 tentativas por e-mail em 15 min; o "Fora do escopo" do PRD derivado excluía bloqueio por tentativas. | Prevalece a `TECH-SPEC.md`, por ser controle de segurança de documento aprovado. O PRD foi corrigido na mesma entrega: ficam fora captcha e bloqueio de conta; o limite de taxa no servidor entra. |
| Limitador em memória não cobre múltiplas instâncias. | Aceito na v1 (instância única). Interface pronta para Redis na F3; métrica `certame.auth.login{resultado=bloqueado}` monitora a eficácia. |
| Cookie `SameSite=Strict` quebra se SPA e API ficarem em domínios distintos. | Servir ambos sob o mesmo domínio (SPA na raiz, API em `/api`). Se for inevitável separar, revisar para `SameSite=None; Secure` e reavaliar CSRF — decisão registrada antes do deploy. |
| Segredo do JWT fraco ou versionado por engano. | `@ConfigurationProperties` com validação de tamanho mínimo (32 bytes) que falha no start; valor de `dev` explicitamente marcado como não produtivo; nenhum segredo em arquivo versionado. |
| Packaging `war` com Tomcat `provided`. | O `spring-boot-maven-plugin` reempacota como war executável; validar `java -jar` na esteira antes do primeiro deploy. |
| Enumeração de contas pelo cadastro. | RF-02 exige informar e-mail duplicado, o que é inerentemente um oráculo. Mitigado pelo limitador de tentativas na rota de cadastro; risco aceito por decisão de produto. |

### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. **Não existe o diretório `.agents/rules/` neste projeto** — não há rules adicionais a observar; se ele for criado, esta especificação deve ser reavaliada.

| Regra | Como esta especificação atende |
| --- | --- |
| 1 — Frontend é tradução 1:1 do protótipo, tokens Nocturne, sem UI de terceiros | As telas saem de `protótipo/Certame.dc.html` e do `styles.css` do Nocturne, sem valor cru. As duas lacunas (RF-04 e erro inline) entram primeiro no protótipo, não no código. A única adição ao design system é o papel `--color-danger-*`, derivado dos hex que o protótipo já usava — decisão registrada e aprovada. Nenhuma biblioteca de UI é adicionada; o `package.json` só ganha Playwright, de teste. |
| 2 — Autorização validada no servidor com `@PreAuthorize` | `SegurancaConfig` mapeia o claim `perfil` para `ROLE_*`; TI-16 prova 401/403 no servidor. As rotas desta feature são públicas por natureza, e a fundação fica pronta para as demais. |
| 3 — Interface em pt-BR; código, tabelas e colunas em português sem acento | Classes `Usuario`, `AutenticacaoService`, `RedefinicaoSenhaService`; colunas `senha_hash`, `expira_em`, `usado_em`; mensagens de erro em pt-BR; `LOCALE_ID` e `lang="pt-BR"`. |
| 4 — Nenhum endpoint fora do contrato da §6 sem atualizar o documento | As seis rotas são exatamente as da §6. Nenhuma rota nova foi criada — a restauração de sessão reusa `POST /auth/refresh` em vez de um `GET /auth/eu`. |
| 5 — Migração só por Flyway, versionada e imutável | `V1__base_acesso.sql` em `db/migration`; semente de `dev` isolada em `db/migration-dev` por `spring.flyway.locations`. |
| 6 — `resposta` imutável e Modo Prova | Não se aplica a esta funcionalidade. |
| 7, 8, 9 — Flashcards, diálogo na sessão e fila de revisão | Não se aplicam a esta funcionalidade. |
| Toda tarefa cita o requisito | Cada componente, endpoint e teste desta especificação referencia RF-01…RF-05, RN-05, RN-06 ou os CA-01…CA-12 do PRD. |
| Domínio + testes antes de controller e frontend | A ordem de construção segue exatamente isso (passos 2 → 4 → 7 → 8). |
| Cobertura mínima de 70% em `service` | Declarada na abordagem de testes e verificada no passo 9. |
| Perguntar em vez de inventar, e registrar a decisão | Seis decisões abertas foram levadas ao usuário (protótipo, pacote, e-mail, escopo, tela de RF-04 e papel de erro) e estão registradas em *Principais decisões*. |

### Conformidade com skills

| Skill | Aplicável | Como é observada |
| --- | --- | --- |
| `313-frameworks-spring-db-migrations-flyway` | sim | SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`; `validate-on-migrate` ligado, `baseline-on-migrate` desligado (base nova); disciplina forward-only — nenhuma migração aplicada é reescrita; `spring.flyway.locations` separa a semente de `dev`. |
| `spring-boot-testing` | sim | Fatia mais estreita por cenário: JUnit + Mockito puros no serviço, `@DataJpaTest` com Testcontainers no repositório, `@WebMvcTest` com `MockMvcTester` no controller e `@SpringBootTest` só nos fluxos completos; asserções em AssertJ; contexto compartilhado para manter a suíte rápida. **Desvio registrado:** o `AGENTS.md` fala em "JUnit 5", enquanto o BOM do Spring Boot 4.1.1 traz JUnit Jupiter 6.0.3 — a API usada é a mesma, e prevalece a versão gerenciada pelo BOM. |
| `postgresql-table-design` | sim | PKs `uuid` com `gen_random_uuid()`, `timestamptz` em UTC, `check` para enums textuais, índice único parcial para e-mail (compatível com exclusão lógica) e índices parciais nos tokens ativos. |
| `angular-developer` | sim | Componentes standalone, detecção zoneless, estado por signals em serviços `providedIn: 'root'`, interceptor funcional, guardas funcionais, `provideAppInitializer` e Reactive Forms. |
| `impeccable` | não | O layout é congelado pela regra 1 do `AGENTS.md`: não há espaço para redesenho. Os aspectos de acessibilidade exigidos pelo PRD são cobertos em E2E-10. |
| `find-skills`, `criar-*`, `executar-*` | não | São skills de processo, não de implementação. |

### Arquivos relevantes e dependentes

**Criados — backend** (`backend/src/main/java/br/com/certame/`)

`config/SegurancaConfig.java` · `config/CorsConfig.java` · `config/PropriedadesSeguranca.java` · `usuario/Usuario.java` · `usuario/PerfilUsuario.java` · `usuario/StatusUsuario.java` · `usuario/UsuarioRepository.java` · `usuario/UsuarioService.java` · `auth/AutenticacaoController.java` · `auth/AutenticacaoService.java` · `auth/TokenService.java` · `auth/RefreshTokenService.java` · `auth/RedefinicaoSenhaService.java` · `auth/LimitadorTentativas.java` · `auth/LimitadorTentativasMemoria.java` · `auth/RefreshToken.java` · `auth/TokenSenha.java` · `auth/RefreshTokenRepository.java` · `auth/TokenSenhaRepository.java` · `auth/dto/` (records de requisição e resposta) · `email/EnviadorEmail.java` · `email/EnviadorEmailSmtp.java` · `comum/ManipuladorErrosGlobal.java` · `comum/ErroApi.java` · `comum/excecao/` · `backend/src/main/resources/db/migration/V1__base_acesso.sql` · `backend/src/main/resources/db/migration-dev/V900__seed_acesso_dev.sql` · `backend/src/main/resources/application-dev.properties`

**Criados — frontend** (`frontend/src/app/`)

`core/auth/auth-store.ts` · `core/auth/auth-api.ts` · `core/auth/auth-interceptor.ts` · `core/auth/auth-guard.ts` · `core/auth/rota-inicial.ts` · `core/auth/modelos.ts` · `core/api/erro-api.ts` · `features/auth/acesso-page/` · `features/auth/esqueci-senha-page/` · `features/auth/definir-senha-page/`

**Modificados**

`backend/pom.xml` (starters de validação, mail, oauth2-resource-server e Testcontainers) · `backend/src/main/java/.../CertameApplication.java` e `ServletInitializer.java` (renomeação de pacote) · `backend/src/main/resources/application.properties` · `frontend/src/app/app.config.ts` · `frontend/src/app/app.routes.ts` · `frontend/src/index.html` · `frontend/src/styles.css` (design system Nocturne + `--color-danger-*`) · `frontend/package.json` (Playwright) · `protótipo/Certame.dc.html` (painel de nova senha e bloco de erro) · `protótipo/nocturne-7bd49ab8-.../readme.md` (registro do papel de erro) · `tasks/prd-acesso-e-conta/prd.md` ("Fora do escopo": captcha e bloqueio de conta ficam fora, limite de taxa no servidor entra)

**Dependentes** — toda funcionalidade posterior consome a fundação criada aqui: `prd-banco-de-questoes` e `prd-gestao-de-usuarios` dependem de `SegurancaConfig`, do `ManipuladorErrosGlobal` e da entidade `Usuario`; `prd-gestao-de-usuarios` (RF-24, RF-27) reaproveita `TokenSenha`, `RedefinicaoSenhaService` e `EnviadorEmail`; `prd-sessao-de-estudo` e as demais telas dependem do `AuthStore`, do interceptor e das guardas.
