# Instruções do projeto Certame

Plataforma de estudo para concursos públicos. Antes de escrever qualquer código, leia:

- `PRD.md` — requisitos numerados (RF-xx), regras de negócio (RN-xx), perfis e escopo.
- `TECH-SPEC.md` — arquitetura, estrutura de pastas, modelo de dados, contrato da API, plano de sprints.
- `.agents/rules/folder-structure.md` — mapa detalhado das pastas do backend, do frontend, do protótipo e de `tasks/`, com o que já existe, o que é alvo e onde colocar cada tipo de arquivo novo.
- `.agents/rules/code-standards.md` — padrões de código: idioma, camadas, DTOs, entidades, erros, migrações, testes, Angular.
- `.agents/rules/tests.md` — regras de testes: cobertura obrigatória, princípios FIRST, AAA/Given-When-Then e pirâmide de testes.

## Stack (não trocar sem aprovação)

Angular 22 (standalone, zoneless, signals) · Spring Boot 4.1 / Java 25 · PostgreSQL 18 + Flyway · Maven.

## Regras inegociáveis

1. **O frontend é uma tradução 1:1 do protótipo aprovado.** Mesma marcação, mesmos tokens do design system Nocturne. Não invente cores, fontes, espaçamentos, raios ou sombras; use sempre `var(--*)` de `styles.css`. Não introduza biblioteca de UI de terceiros.
2. **Toda autorização é validada no servidor** com `@PreAuthorize`, mesmo quando a interface já esconde a ação. Perfis: `ROLE_ESTUDANTE`, `ROLE_PROFESSOR`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`.
3. **Interface e mensagens em português do Brasil**; código, nomes de tabela/coluna e commits em português sem acento (`snake_case` no banco).
4. **Nenhum endpoint fora do contrato** da Tech Spec §6 sem atualizar o documento na mesma alteração.
5. **Migração de banco só por Flyway**, versionada e imutável. Nunca edite uma migração já aplicada.
6. `resposta` é imutável (RN-02). No Modo Prova, respostas só contam após finalização (RN-04).
7. **Flashcards são privados do dono** (RN-07): toda consulta filtra por `usuario_id` do token e responde 404 para card de terceiro. O agendamento segue o **FSRS** de RN-08 (estabilidade, dificuldade e retenção alvo por deck), implementado em `flashcard/fsrs` — não troque o algoritmo nem os pesos sem atualizar o PRD.
8. **A criação de flashcard na sessão de estudo é um diálogo, nunca uma rota** (RF-28): a sessão em memória, o índice da série e o cronômetro devem sobreviver intactos à abertura e ao fechamento do modal.
9. **A fila de revisão é montada respeitando os limites do deck** (RN-10) e excluir um deck nunca apaga cards (RN-11).

## Mapa do repositório

```
certame/
  PRD.md              requisitos numerados (RF-xx), regras de negócio (RN-xx), perfis e escopo
  TECH-SPEC.md        arquitetura, estrutura de pastas, modelo de dados, contrato da API, sprints
  docker-compose.yml  PostgreSQL 18 + MailHog para desenvolvimento local
  backend/            API REST Spring Boot (Maven)
  frontend/           SPA Angular (npm)
  protótipo/          protótipo Nocturne aprovado (`Certame.dc.html` + `_ds/`) — fonte da verdade visual
  tasks/              uma pasta por funcionalidade (`prd-*/` com prd.md, techspec.md, tasks.md)
  .agents/rules/      regras de leitura obrigatória (`code-standards.md`, `tests.md`)
  .agents/skills/     skills do projeto (criar-prd, executar-task, executar-review, executar-qa, ...)
```

Os documentos citados no topo estão na **raiz** do repositório, não em `docs/`.
O detalhamento completo — pacotes do backend, pastas do frontend, convenções de nome de migração e de teste, e uma tabela de "onde colocar o quê" — está em `agents/rules/folder-structure.md`; consulte-o antes de criar qualquer arquivo novo.

### backend/ — API REST

Spring Boot 4.1.1 · Java 25 · Maven (wrapper `./mvnw`) · Spring Security 7 (JWT) · Spring Data JPA · Flyway · PostgreSQL 18 · Lombok · empacotamento **war** (Tomcat `provided`).
Testes: JUnit 5, Spring Boot test slices, Testcontainers 2.0.5.

Pacote raiz `br.com.certame` (atenção: o `groupId` Maven é `com.cavallin`, mas o código vive em `br.com.certame`):

```
src/main/java/br/com/certame/
  CertameApplication.java · ServletInitializer.java
  config/     SegurancaConfig, CorsConfig, PropriedadesSeguranca
  auth/       AutenticacaoController/Service, TokenService, RefreshToken*, RedefinicaoSenha*, LimitadorTentativas, dto/
  usuario/    Usuario, PerfilUsuario, StatusUsuario, UsuarioRepository/Service
  email/      EnviadorEmail (interface) + EnviadorEmailSmtp
  comum/      ErroApi, ManipuladorErrosGlobal, excecao/
src/main/resources/
  application.properties            comum (JPA em `validate`, Flyway on, mail)
  application-{dev,staging,prod}.properties
  db/migration/       migrações versionadas e imutáveis (V1__base_acesso.sql)
  db/migration-dev/   seeds só do perfil dev (V900__seed_acesso_dev.sql)
```

Domínios ainda a criar seguem a fronteira da Tech Spec §4: `catalogo/`, `questao/`, `estudo/`, `simulado/`, `flashcard/` (com `fsrs/`), `desempenho/`.

### frontend/ — SPA

Angular 22 standalone/zoneless/signals · TypeScript 6.0 · Angular CLI 22 + `@angular/build` · Vitest (jsdom) · Prettier. Estilos: `src/styles.css` (design system Nocturne) + SCSS por componente. **Sem biblioteca de UI de terceiros.**

Hoje existe apenas o bootstrap (`src/main.ts`, `src/app/app.{ts,html,scss,config.ts,routes.ts}`). A estrutura alvo é a da Tech Spec §3: `core/` (auth, api, models), `shared/` (ui, pipes, directives), `layout/`, `features/{auth,dashboard,study,cadernos,simulados,stats,flashcards,admin,super}`.

## Portas

| Serviço | Porta | Origem |
| --- | --- | --- |
| Frontend (`ng serve`) | http://localhost:4200 | Angular CLI |
| Backend (API) | http://localhost:8080 | padrão Spring Boot; base dos endpoints é `/api/v1` |
| PostgreSQL | localhost:**55432** → 5432 no container | `docker-compose.yml` (db `certame_dev`, user/senha `certame`) |
| MailHog SMTP | localhost:**55525** | captura e-mails do perfil dev |
| MailHog UI | http://localhost:58025 | ler os e-mails enviados |

CORS e links de e-mail apontam para `certame.cors.origem` / `certame.seguranca.frontend-url` = `http://localhost:4200` (sobrescreva com `SPA_ORIGIN`).

## Como rodar

**1. Infraestrutura** (na raiz, sempre primeiro):

```bash
docker compose up -d          # postgres + mailhog
docker compose down           # parar (o volume certame-postgres-data persiste)
```

**2. Backend** — o perfil **`dev` não é padrão, precisa ser informado**; sem ele não há `spring.datasource.url` e a aplicação não sobe:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway roda na subida (`db/migration` + `db/migration-dev`) e o Hibernate está em `ddl-auto=validate` — o esquema vem só das migrações.

**3. Frontend:**

```bash
cd frontend
npm ci        # instalar dependências (npm install ao adicionar pacote)
npm start     # ng serve em http://localhost:4200
```

## Testes

```bash
# backend — unidade + integração; Testcontainers exige Docker rodando
cd backend && ./mvnw test
./mvnw test -Dtest=AutenticacaoServiceTest        # uma classe

# frontend — Vitest via builder do Angular
cd frontend && npm test
npm run test:coverage                             # cobertura v8, falha abaixo de 80%

# cobertura do backend (JaCoCo): relatório em target/site/jacoco/, regra aplicada no verify
cd backend && ./mvnw verify
```

Cobertura mínima **80%** (ver `.agents/rules/tests.md`). Testes de integração usam Testcontainers com PostgreSQL próprio — não dependem do compose, mas dependem do daemon do Docker.

## Build

```bash
cd backend  && ./mvnw clean package                 # gera target/certame-0.0.1-SNAPSHOT.war
cd frontend && npm run build                        # produção (default) → frontend/dist/
cd frontend && npm run watch                        # build de desenvolvimento em watch
```

## Variáveis de ambiente

`dev` traz padrões para tudo; `staging`/`prod` exigem: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `JWT_SECRET`, `SPA_ORIGIN`. O segredo JWT fixo de `application-dev.properties` **nunca** vai para outro ambiente.

## Como trabalhar comigo

- Toda tarefa deve citar o(s) requisito(s) que atende: "implementar RF-19".
- Comece pela camada de domínio + testes; depois controller; depois frontend.
- Testes: JUnit + Mockito + Testcontainers no backend, Vitest + Playwright no frontend. Regras completas em `.agents/rules/tests.md` — todo código coberto, mínimo 80%, princípios FIRST, AAA e pirâmide de testes.
- Siga `.agents/rules/code-standards.md` e `.agents/rules/tests.md` em todo código novo; divergir dele exige registrar a decisão na `TECH-SPEC.md`.
- Se algo não estiver especificado nos documentos, pergunte em vez de inventar — e registre a decisão no documento correspondente.
