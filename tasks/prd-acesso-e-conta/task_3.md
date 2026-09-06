# Tarefa 3.0: Fundação transversal (erros, segurança e CORS)

## Visão geral

Estabelece o contrato compartilhado por toda a aplicação: o tratamento global de erro em `application/problem+json`, a hierarquia de exceções de negócio, a configuração de segurança stateless (BCrypt força 12, `SecurityFilterChain`, `JwtEncoder`/`JwtDecoder`, conversor do claim `perfil` para `ROLE_*`) e o CORS com `allowCredentials`. Nada disso é específico de Acesso e conta — é a base de todas as funcionalidades seguintes.

<skills>
### Conformidade com skills

- `spring-boot-testing` — a fatia de teste adequada para o `@RestControllerAdvice` e a configuração de segurança é `@WebMvcTest`; o teste de rota protegida (TI-16) fica na tarefa 4.0, quando houver rota real para exercitar.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/`. Regras relevantes: regra 2 (toda autorização validada no servidor com `@PreAuthorize`; os quatro perfis `ROLE_ESTUDANTE`, `ROLE_PROFESSOR`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN` são mapeados aqui) e regra 3 (mensagens de erro em português do Brasil). Nenhum desvio.
</rules>

<requirements>
- `ManipuladorErrosGlobal` como `@RestControllerAdvice` único, produzindo `application/problem+json` com `code`, `message` e `fieldErrors`.
- Hierarquia `ErroNegocioException` com `CredencialInvalidaException`, `EmailEmUsoException`, `TokenInvalidoException`, `SessaoExpiradaException` e `MuitasTentativasException`, mapeadas para os códigos e status da TechSpec.
- `SegurancaConfig`: `SecurityFilterChain` stateless, `PasswordEncoder` BCrypt força 12, `JwtEncoder`/`JwtDecoder` HS256 com `NimbusJwtEncoder`, `JwtAuthenticationConverter` traduzindo o claim `perfil` em `ROLE_*`, rotas `/api/v1/auth/**` públicas.
- `CorsConfig` com origem do SPA por configuração e `allowCredentials=true`.
- `PropriedadesSeguranca` como `@ConfigurationProperties("certame.seguranca")`, com validação que falha no start se o segredo do JWT tiver menos de 32 bytes.
- `CertameApplication` com `@EnableAsync` e `@EnableScheduling`.
</requirements>

## Subtarefas

- [x] 3.1 Criar `comum/ErroApi.java` (record do envelope) e `comum/excecao/` com `ErroNegocioException` e os cinco subtipos.
- [x] 3.2 Criar `comum/ManipuladorErrosGlobal.java` mapeando cada exceção para o código e status da tabela da TechSpec, incluindo `DADOS_INVALIDOS` (422) a partir do Bean Validation com `fieldErrors` preenchido.
- [x] 3.3 Criar `config/PropriedadesSeguranca.java` com segredo do JWT, TTLs, atributos do cookie e limite de tentativas, com validação de tamanho mínimo do segredo.
- [x] 3.4 Criar `config/SegurancaConfig.java` com o `SecurityFilterChain` stateless, `PasswordEncoder` BCrypt 12, `JwtEncoder`/`JwtDecoder` e o `JwtAuthenticationConverter` de perfis.
- [x] 3.5 Criar `config/CorsConfig.java` com origem configurável e `allowCredentials=true`.
- [x] 3.6 Anotar `CertameApplication` com `@EnableAsync` e `@EnableScheduling`.
- [x] 3.7 Provisionar o segredo do JWT por variável de ambiente, com valor de `dev` explicitamente marcado como não produtivo.

## Detalhes de implementação

Ver TechSpec, seções *Visão dos componentes → Backend* (tabela de componentes `config/*` e `comum/*`), *Modelos de dados → `ErroApi`* (tabela de códigos e status) e *Sequenciamento do desenvolvimento → Ordem de construção*, item 3.

## Critérios de aceitação relacionados

- CA-03

## Testes da tarefa

Os casos de teste que exercitam esta fundação dependem de rotas reais e estão mapeados nas tarefas 4.0 a 6.0 — em especial TI-16 (rota protegida, 401/403 no servidor), TI-02 e TI-03 (formato `problem+json`).

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/comum/ErroApi.java`
- `backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java`
- `backend/src/main/java/br/com/certame/comum/excecao/`
- `backend/src/main/java/br/com/certame/config/SegurancaConfig.java`
- `backend/src/main/java/br/com/certame/config/CorsConfig.java`
- `backend/src/main/java/br/com/certame/config/PropriedadesSeguranca.java`
- `backend/src/main/java/br/com/certame/CertameApplication.java`
