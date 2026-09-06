# Tarefa 1.0: Fundação do projeto (pacote, dependências e perfis)

## Visão geral

Prepara o scaffold backend para tudo o que vem depois: renomeia o pacote de `com.cavallin.certame` para `br.com.certame`, acrescenta as dependências Maven necessárias à autenticação, e configura os perfis `dev`/`staging`/`prod` com o datasource e o MailHog em Docker Compose para desenvolvimento local. Nenhuma regra de negócio nasce aqui.

<skills>
### Conformidade com skills

Nenhuma skill de implementação específica se aplica diretamente a esta tarefa (é configuração de projeto, não código de domínio). `313-frameworks-spring-db-migrations-flyway` é referenciada preventivamente para configurar `spring.flyway.locations` corretamente antes da tarefa 2.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: stack não negociável (Angular 22 / Spring Boot 4.1 / Java 25 / PostgreSQL 18 + Flyway / Maven) e "nenhum endpoint fora do contrato" (não se aplica ainda — nenhuma rota é criada nesta tarefa). Nenhum desvio.
</rules>

<requirements>
- Renomear o pacote base para `br.com.certame` (decisão registrada na TechSpec, seção *Principais decisões*).
- Acrescentar ao `pom.xml`: `spring-boot-starter-validation`, `spring-boot-starter-mail`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-testcontainers`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`.
- Configurar perfis `dev`, `staging` e `prod` e o datasource de cada um.
- Preparar o MailHog no Docker Compose de `dev` (TECH-SPEC.md §9).
</requirements>

## Subtarefas

- [x] 1.1 Renomear o pacote `com.cavallin.certame` para `br.com.certame` em `CertameApplication.java`, `ServletInitializer.java` e `CertameApplicationTests.java`, movendo os arquivos para a nova estrutura de diretórios.
- [x] 1.2 Adicionar as dependências Maven listadas em `pom.xml` (versões gerenciadas pelo BOM do Spring Boot 4.1.1).
- [x] 1.3 Criar `application-dev.properties`, `application-staging.properties` e `application-prod.properties` com o datasource de cada perfil.
- [x] 1.4 Configurar `spring.flyway.locations=classpath:db/migration,classpath:db/migration-dev` apenas no perfil `dev`.
- [x] 1.5 Adicionar o serviço MailHog ao Docker Compose de desenvolvimento.
- [x] 1.6 Validar que o projeto compila e o `CertameApplicationTests` (renomeado) continua passando.

## Detalhes de implementação

Ver TechSpec, seção *Sequenciamento do desenvolvimento → Ordem de construção*, item 1, e *Dependências técnicas* (PostgreSQL 18/Docker, segredo do JWT — este último provisionado na tarefa 3.0).

## Critérios de aceitação relacionados

Nenhum critério de aceitação do PRD é validado diretamente por esta tarefa — é fundação.

## Testes da tarefa

Nenhum caso de teste da TechSpec é mapeado para esta tarefa; verificação por build bem-sucedido e teste de contexto Spring (`CertameApplicationTests`).

## Arquivos relevantes

- `backend/pom.xml`
- `backend/src/main/java/br/com/certame/CertameApplication.java`
- `backend/src/main/java/br/com/certame/ServletInitializer.java`
- `backend/src/test/java/br/com/certame/CertameApplicationTests.java`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-dev.properties`
- `backend/src/main/resources/application-staging.properties`
- `backend/src/main/resources/application-prod.properties`
- `docker-compose.yml` (ou equivalente de `dev`)
