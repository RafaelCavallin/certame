# Estrutura de pastas — Certame

Mapa completo do repositório: o que **já existe** hoje e o que é a **estrutura alvo** definida na `TECH-SPEC.md` (§3 frontend, §4 backend). Use este documento antes de criar qualquer arquivo novo — pasta nova fora deste mapa exige atualizar a Tech Spec na mesma alteração.

Legenda: ✅ existe · 🎯 alvo (ainda não criado)

---

## 1. Raiz

```
certame/
├── AGENTS.md              instruções para agentes: stack, regras inegociáveis, como rodar
├── PRD.md                 requisitos numerados (RF-xx), regras de negócio (RN-xx), perfis, escopo
├── TECH-SPEC.md           arquitetura, estrutura, modelo de dados, contrato da API, sprints
├── folder-structure.md    este documento
├── APIs.txt               anotações soltas de endpoints
├── docker-compose.yml     PostgreSQL 18 (55432) + MailHog (55525 / UI 58025)
├── skills-lock.json       versões travadas das skills instaladas
├── backend/               API REST Spring Boot (Maven)
├── frontend/              SPA Angular (npm)
├── protótipo/             protótipo Nocturne aprovado — fonte da verdade visual
├── tasks/                 uma pasta por funcionalidade (prd-*/)
├── .agents/               regras e skills do projeto
├── .claude/               skills espelhadas para o Claude Code
└── .vscode/               settings do editor
```

> Os documentos de referência ficam na **raiz**, não em `docs/`.

---

## 2. `backend/` — API REST

Spring Boot 4.1.1 · Java 25 · Maven (`./mvnw`) · Spring Security 7 (JWT) · Spring Data JPA · Flyway · PostgreSQL 18 · Lombok · empacotamento **war** (Tomcat `provided`).
Testes: JUnit 5, test slices do Spring Boot, Testcontainers 2.0.5.

```
backend/
├── pom.xml                 groupId com.cavallin — mas o código vive em br.com.certame
├── mvnw · mvnw.cmd · .mvn/ Maven Wrapper
├── HELP.md
├── target/                 build (ignorado no versionamento)
└── src/
    ├── main/
    │   ├── java/br/com/certame/
    │   └── resources/
    └── test/java/br/com/certame/
```

### 2.1 `src/main/java/br/com/certame/` — pacotes de domínio

```
br/com/certame/
├── CertameApplication.java      ✅ entrada da aplicação
├── ServletInitializer.java      ✅ deploy em container (war)
├── config/                      ✅ SegurancaConfig, CorsConfig, PropriedadesSeguranca
├── auth/                        ✅ autenticação, JWT, refresh token, redefinição de senha
│   ├── AutenticacaoController.java · AutenticacaoService.java
│   ├── TokenService.java · ContextoRequisicao.java
│   ├── RefreshToken.java · RefreshTokenRepository.java · RefreshTokenService.java
│   ├── TokenSenha.java · TokenSenhaRepository.java · RedefinicaoSenhaService.java
│   ├── FinalidadeTokenSenha.java
│   ├── LimitadorTentativas.java · LimitadorTentativasMemoria.java
│   └── dto/                     RequisicaoLogin, RequisicaoCadastro, RequisicaoEsqueciSenha,
│                                RequisicaoRedefinirSenha, RespostaAutenticacao, UsuarioResumo
├── usuario/                     ✅ Usuario, PerfilUsuario, StatusUsuario,
│                                   UsuarioRepository, UsuarioService
├── email/                       ✅ EnviadorEmail (interface) + EnviadorEmailSmtp
├── comum/                       ✅ ErroApi, ManipuladorErrosGlobal
│   └── excecao/                 CredencialInvalidaException, EmailEmUsoException,
│                                ErroNegocioException, MuitasTentativasException,
│                                SessaoExpiradaException, TokenInvalidoException
├── catalogo/                    🎯 disciplina, assunto, banca, orgao, edital
├── questao/                     🎯 questao, alternativa, publicação, busca
├── estudo/                      🎯 sessao, resposta, caderno
├── simulado/                    🎯 simulado, composicao, tentativa, resultado
├── flashcard/                   🎯 flashcard, deck, revisao, fila de estudo
│   └── fsrs/                    🎯 pesos e agendador FSRS (RN-08)
└── desempenho/                  🎯 agregados e consultas de relatório
```

**Camadas** (obrigatórias, ver `.agents/rules/code-standards.md`):

```
Controller  →  Service                    →  Repository
validação      @Transactional, regra de       Spring Data + JPQL/native
e DTO          negócio, @PreAuthorize         quando necessário
```

Erros em `application/problem+json` (`code`, `message`, `fieldErrors`), por um único `@RestControllerAdvice`.

> A Tech Spec §4 chama esse pacote de `common/`; no código ele é **`comum/`** (idioma português). Vale o que está no código.

### 2.2 `src/main/resources/`

```
resources/
├── application.properties              comum (JPA em validate, Flyway on, mail)
├── application-dev.properties          perfil dev — precisa ser informado ao subir
├── application-staging.properties
├── application-prod.properties
├── db/
│   ├── migration/                      migrações versionadas e IMUTÁVEIS
│   │   └── V1__base_acesso.sql
│   └── migration-dev/                  seeds só do perfil dev
│       └── V900__seed_acesso_dev.sql
├── static/
└── templates/
```

**Convenção de migração:** `V<n>__<assunto_snake_case>.sql`. Números `V900+` são reservados para seeds de dev. Nunca edite uma migração já aplicada — crie a próxima versão.

### 2.3 `src/test/java/br/com/certame/` — espelha o pacote de produção

```
test/java/br/com/certame/
├── CertameApplicationTests.java        contexto sobe
├── FlywayMigracaoTest.java             migrações aplicam limpo
├── auth/
│   ├── *ServiceTest.java               unidade (JUnit + Mockito)
│   ├── *ControllerTest.java            slice web (@WebMvcTest)
│   └── *IntegracaoTest.java            integração (Testcontainers, exige Docker)
└── usuario/                            UsuarioServiceTest, UsuarioPersistenciaTest
```

**Sufixos:** `*Test` para unidade/slice, `*IntegracaoTest` para integração, `*PersistenciaTest` para camada de dados. Cobertura mínima **80%** (JaCoCo no `verify`).

---

## 3. `frontend/` — SPA Angular

Angular 22 standalone/zoneless/signals · TypeScript 6.0 · Angular CLI 22 + `@angular/build` · Vitest (jsdom) · Prettier. **Sem biblioteca de UI de terceiros.**

```
frontend/
├── angular.json · package.json · package-lock.json
├── tsconfig.json · tsconfig.app.json · tsconfig.spec.json
├── .prettierrc · .editorconfig · .vscode/
├── public/favicon.ico
├── dist/ · coverage/ · .angular/     gerados (ignorados)
└── src/
    ├── index.html
    ├── main.ts                        bootstrap
    ├── styles.css                     design system Nocturne (tokens --*)
    └── app/
        ├── app.ts · app.html · app.scss · app.spec.ts
        ├── app.config.ts              providers da aplicação
        └── app.routes.ts              rotas raiz
```

### 3.1 Estrutura alvo de `src/app/` (Tech Spec §3)

```
src/app/
├── core/                    🎯 infraestrutura de aplicação (sem UI)
│   ├── auth/                   guards (authGuard, roleGuard), interceptor, token store
│   ├── api/                    clientes HTTP por domínio
│   └── models/                 interfaces e tipos do contrato da API
├── shared/                  🎯 reutilizável entre features
│   ├── ui/                     botão, tag, campo, tabela, diálogo, toast
│   ├── pipes/
│   └── directives/
├── layout/                  🎯 shell: sidebar por perfil, topbar, área de conteúdo
└── features/                🎯 uma pasta por área funcional
    ├── auth/                   login, cadastro, esqueci-senha, definir-senha
    ├── dashboard/              início do estudante
    ├── study/                  sessão de questões: foco | contexto | prova + correção
    ├── cadernos/               lista e sessão por caderno
    ├── simulados/              lista, execução, resultado
    ├── stats/                  desempenho: evolução, disciplinas, pontos fracos
    ├── flashcards/             lista, decks/, sessao-revisao/, flashcard-dialog/
    ├── admin/                  questoes/, materias/, simulados/, editais/
    └── super/                  usuarios/ (lista + formulário)
```

**Arquivos por componente** (padrão Angular CLI, sem sufixo `.component`): `nome.ts`, `nome.html`, `nome.scss`, `nome.spec.ts`.

**Estilos:** só tokens `var(--*)` de `styles.css`. SCSS por componente para layout local; nenhuma cor, fonte, espaçamento, raio ou sombra inventados.

**Estado (signals):** `AuthStore`, `StudySessionStore`, `FlashcardStore`, `ToastStore` — em `core/` ou na feature dona, conforme o escopo.

### 3.2 Rotas × pastas

| Rota | Feature | Perfis |
| --- | --- | --- |
| `/login`, `/cadastro`, `/esqueci-senha`, `/definir-senha/:token` | `features/auth/` | público (fora do shell) |
| `/inicio` | `features/dashboard/` | autenticados |
| `/estudar` | `features/study/` | autenticados |
| `/cadernos` | `features/cadernos/` | autenticados |
| `/simulados` | `features/simulados/` | autenticados |
| `/desempenho` | `features/stats/` | autenticados |
| `/flashcards`, `/flashcards/:id`, `/flashcards/decks`, `/flashcards/estudar` | `features/flashcards/` | autenticados |
| `/admin/questoes`, `/admin/materias`, `/admin/editais` | `features/admin/` | professor+ |
| `/admin/simulados` | `features/admin/simulados/` | admin+ |
| `/super/usuarios` | `features/super/` | super-admin |

> Criação de flashcard (RF-28) e configuração de deck (RF-36) **não têm rota** — são diálogos abertos pelo `DialogService`, para preservar a sessão em memória e o cronômetro.

---

## 4. `protótipo/` — fonte da verdade visual

```
protótipo/
├── Certame.dc.html                        protótipo aprovado (todas as telas)
├── support.js
├── _ds/nocturne-<uuid>/                   design system (link)
└── nocturne-<uuid>/
    ├── styles.css                         tokens Nocturne
    ├── _ds_bundle.js · _ds_manifest.json
    ├── _adherence.oxlintrc.json
    └── readme.md
```

O frontend é **tradução 1:1** deste protótipo: mesma marcação, mesmas classes, mesmos tokens. Nada aqui é editado para acomodar o código — o código é que se ajusta.

---

## 5. `tasks/` — uma pasta por funcionalidade

```
tasks/
└── prd-<nome-da-funcionalidade>/
    ├── prd.md          requisitos da funcionalidade
    ├── techspec.md     especificação técnica derivada do PRD
    ├── tasks.md        índice das tarefas com estado de conclusão
    └── task_<n>.md     detalhe de cada tarefa
```

Funcionalidades atuais:

| Pasta | Estado |
| --- | --- |
| `prd-acesso-e-conta/` | PRD + TechSpec + 10 tarefas |
| `prd-banco-de-questoes/` | PRD + TechSpec + 10 tarefas |
| `prd-cadernos-e-desempenho/` | só PRD |
| `prd-editais/` | só PRD |
| `prd-flashcards-criacao-e-gerenciamento/` | só PRD |
| `prd-flashcards-decks-e-revisao/` | só PRD |
| `prd-gestao-de-usuarios/` | só PRD |
| `prd-sessao-de-estudo/` | só PRD |
| `prd-simulados/` | só PRD |

Fluxo: `criar-prd` → `criar-techspec` → `criar-tasks` → `executar-task` → `executar-review` → `executar-qa`.

---

## 6. `.agents/` e `.claude/` — regras e skills

```
.agents/
├── rules/                 leitura obrigatória antes de escrever código
│   ├── code-standards.md     idioma, camadas, DTOs, entidades, erros, migrações, Angular
│   └── tests.md              cobertura 80%, FIRST, AAA/Given-When-Then, pirâmide
└── skills/                skills do projeto (cada uma com SKILL.md + references/)
    ├── criar-prd/ · criar-techspec/ · criar-tasks/
    ├── executar-task/ · executar-review/ · executar-qa/
    ├── angular-developer/ · spring-boot-testing/
    ├── postgresql-table-design/ · 313-frameworks-spring-db-migrations-flyway/
    ├── impeccable/ · find-skills/
    └── ...

.claude/skills/            espelho das mesmas skills para o Claude Code
```

---

## 7. Onde colocar o quê

| O que você vai criar | Onde |
| --- | --- |
| Nova regra de negócio do backend | `br/com/certame/<dominio>/<Dominio>Service.java` |
| Novo endpoint | `br/com/certame/<dominio>/<Dominio>Controller.java` + atualizar Tech Spec §6 |
| DTO de entrada/saída | `br/com/certame/<dominio>/dto/` |
| Nova exceção de negócio | `br/com/certame/comum/excecao/` |
| Mudança de esquema | nova migração em `db/migration/V<n>__*.sql` (nunca editar as existentes) |
| Massa de teste local | `db/migration-dev/V9xx__*.sql` |
| Teste de serviço | `src/test/java/br/com/certame/<dominio>/<Classe>Test.java` |
| Teste ponta a ponta da API | `src/test/java/br/com/certame/<dominio>/<Assunto>IntegracaoTest.java` |
| Tela nova | `frontend/src/app/features/<area>/` + rota em `app.routes.ts` |
| Componente reutilizável | `frontend/src/app/shared/ui/` |
| Cliente HTTP / tipo da API | `frontend/src/app/core/api/` · `core/models/` |
| Guard ou interceptor | `frontend/src/app/core/auth/` |
| Documentação de funcionalidade | `tasks/prd-<nome>/` |

**Nunca crie:** pasta `docs/`, biblioteca de UI de terceiros no frontend, pacote fora de `br.com.certame` no backend, ou endpoint fora do contrato da Tech Spec §6.
