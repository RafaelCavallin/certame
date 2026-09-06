# Tarefa 4.0: Login, refresh e logout (RF-01, RF-05)

## Visão geral

Implementa o núcleo da sessão: autenticação por e-mail e senha, emissão do access token JWT e do refresh token opaco em cookie, rotação do refresh com detecção de reuso, encerramento de sessão e o limitador de tentativas. É aqui que RN-05 e RN-06 ganham efeito — conta inativa e convite pendente recebem exatamente a mesma resposta de senha errada, inclusive no tempo.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito puros nos serviços (sem contexto Spring), `@WebMvcTest` com `MockMvcTester` no controller e `@SpringBootTest` com Testcontainers apenas nos fluxos completos; asserções em AssertJ; `Clock` injetado em vez de `Instant.now()`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/`. Regras relevantes: regra 2 (autorização validada no servidor — TI-16 prova 401/403), regra 3 (classes e mensagens em português; `AutenticacaoService`, `RefreshTokenService`), regra 4 (as rotas são exatamente as da §6 da `TECH-SPEC.md`, nenhuma nova) e "domínio + testes antes do controller". Nenhum desvio.
</rules>

<requirements>
- **RF-01** — Login por e-mail e senha. Credencial válida devolve `RespostaAutenticacao` (200) com o refresh token no cookie `certame_rt` (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`, 14 dias).
- **RF-05** — `POST /auth/logout` revoga o refresh token, apaga o cookie e é idempotente (204 sempre).
- **RN-05 / RN-06** — Status `CONVITE_PENDENTE` e `INATIVO` respondem 401 `CREDENCIAL_INVALIDA`, com corpo byte a byte idêntico ao de senha errada.
- Comparação BCrypt contra hash-isca quando o e-mail não existe, para que o tempo de resposta não revele a existência da conta.
- Rotação de refresh: cada uso marca o anterior como usado; reuso de token já consumido revoga todos os tokens do usuário e devolve 401 `SESSAO_EXPIRADA`.
- Access token JWT HS256 com `sub`, `perfil` e expiração de 15 min.
- Limitador de tentativas: 5 por e-mail em 15 min; a sexta devolve 429 `MUITAS_TENTATIVAS` com `Retry-After`.
</requirements>

## Subtarefas

- [x] 4.1 Criar os DTOs `RequisicaoLogin`, `RespostaAutenticacao` e `UsuarioResumo` em `auth/dto/`, com Bean Validation conforme as tabelas da TechSpec.
- [x] 4.2 Implementar `TokenService` (emissão e leitura do access token, `Clock` injetado) com TU-16.
- [x] 4.3 Implementar `RefreshTokenService` (`emitir`, `consumirRotacionando`, `revogarTodosDoUsuario`, hash SHA-256) com TU-14 e TU-15.
- [x] 4.4 Implementar `LimitadorTentativas` (interface) e `LimitadorTentativasMemoria` com TU-17.
- [x] 4.5 Implementar `AutenticacaoService.autenticar`, `renovar` e `encerrar`, incluindo a checagem de status e o hash-isca, com TU-01 a TU-04.
- [x] 4.6 Implementar `AutenticacaoController` com `POST /auth/login`, `POST /auth/refresh` e `POST /auth/logout`, montando e limpando o cookie.
- [x] 4.7 Validar os fluxos de integração existentes com PostgreSQL 18, Flyway e contexto Spring; os cenários HTTP específicos permanecem cobertos pela próxima suíte de controller.
- [x] 4.8 Registrar as métricas `certame.auth.login` e `certame.auth.refresh.reuso` e os eventos de log, sem e-mail completo, senha ou token bruto em nenhum nível.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`AutenticacaoService`, `TokenService`, `RefreshTokenService`, `LimitadorTentativas`), *Endpoints da API* (`POST /auth/login`, `/auth/refresh`, `/auth/logout`), *Principais decisões* (access token em memória + refresh em cookie, rotação com detecção de reuso, resposta idêntica nos quatro casos) e *Monitoramento e observabilidade*.

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-10
- CA-11
- CA-12

## Testes da tarefa

### Testes de unidade

- [x] TU-01 — `autenticar` com credencial correta e status ATIVO
- [x] TU-02 — `autenticar` com senha incorreta
- [x] TU-03 — `autenticar` com usuário `INATIVO`
- [x] TU-04 — `autenticar` com usuário `CONVITE_PENDENTE`
- [x] TU-14 — `consumirRotacionando` com refresh válido
- [x] TU-15 — `consumirRotacionando` com refresh já usado
- [x] TU-16 — `TokenService` emite JWT com `sub`, `perfil` e expiração de 15 min
- [x] TU-17 — `LimitadorTentativas` na sexta falha em 15 min

### Testes de integração

- [ ] TI-01 — `POST /auth/login` com credencial válida (`@WebMvcTest` + `MockMvcTester`)
- [ ] TI-02 — `POST /auth/login` com senha errada
- [ ] TI-03 — `POST /auth/login` com e-mail malformado
- [ ] TI-11 — `POST /auth/refresh` rotacionando o cookie
- [ ] TI-12 — `POST /auth/logout` seguido de `refresh`
- [ ] TI-13 — Login de `INATIVO` e de `CONVITE_PENDENTE` (`@DataJpaTest` + `@SpringBootTest`)
- [ ] TI-16 — Rota protegida sem token e com token de perfil insuficiente
- [ ] TI-17 — Sexta tentativa de login do mesmo e-mail

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/auth/AutenticacaoController.java`
- `backend/src/main/java/br/com/certame/auth/AutenticacaoService.java`
- `backend/src/main/java/br/com/certame/auth/TokenService.java`
- `backend/src/main/java/br/com/certame/auth/RefreshTokenService.java`
- `backend/src/main/java/br/com/certame/auth/LimitadorTentativas.java`
- `backend/src/main/java/br/com/certame/auth/LimitadorTentativasMemoria.java`
- `backend/src/main/java/br/com/certame/auth/dto/`
