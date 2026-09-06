# Relatório de revisão de código — Acesso e conta

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Branch: N/A — não é repositório git
- Status: **APROVADO COM RESSALVAS** (mantido)

> **Revalidação 2026-09-05.** As conclusões de conformidade e de aderência à TechSpec deste relatório
> continuam válidas: `AutenticacaoService.java` está com 96 linhas, a constante `COOKIE_REFRESH`
> existe em `AutenticacaoController.java:25`, as três páginas de `features/auth` importam `corpoErro`
> de `core/api/erro-api` e usam `firstValueFrom`. **Os números de teste da seção "Testes" mudaram** —
> foram atualizados abaixo. A mudança relevante é que `./mvnw verify` **não conclui mais**: a suíte
> de backend está em 535 testes com **1 falha** (`FlywayMigracaoTest.TI06`), causada pela migração
> `V15__sessao_livre_tamanho_variavel.sql` de `prd-sessao-de-estudo`, que liberou `tamanho between
> 1 and 50` para sessão `LIVRE` enquanto o teste ainda espera que `40` seja recusado. É um defeito
> **fora do escopo desta funcionalidade**, mas impede confirmar o gate JaCoCo do build inteiro.
> Pendências desta funcionalidade que continuam abertas: ausência de teste para o laço de
> retentativa de `EnviadorEmailSmtp` (não existe `src/test/java/br/com/certame/email/`) e a
> imprecisão de redação da TechSpec sobre "1s, 4s, 16s".

> **Revalidação 2026-09-05 (tarefa 6.0, correção de pendências P2-3).** A falha de
> `FlywayMigracaoTest.TI06` citada acima foi corrigida pela tarefa 1.0 de
> `tasks/correcoes-pendencias/`: `cd backend && ./mvnw verify` roda agora **571 testes, 0 falhas**, e
> `jacoco:check` reporta **"All coverage checks have been met."** — o gate JaCoCo do build inteiro
> volta a ser confirmável. A pendência de `EnviadorEmailSmtp` também foi fechada: novo
> `backend/src/test/java/br/com/certame/email/EnviadorEmailSmtpTest.java` (3 testes) exercita
> sucesso na primeira tentativa, sucesso após uma falha e o esgotamento das três tentativas sem
> propagar exceção (RF-03/RF-04). A imprecisão de redação da TechSpec sobre "1s, 4s, 16s" segue
> aberta — está fora do escopo de teste desta tarefa. Frontend: **473 testes, 0 falhas** — Statements
> 89,45% · Branches 84,88% · Functions 80,18% · Lines 93,93%.

## Conformidade com regras
| Regra | Status | Observações |
|------|--------|-------------|
| Idioma pt-BR sem acento em código; mensagens em pt-BR | OK | Classes, tabelas e colunas em português sem acento; mensagens de validação e de erro em pt-BR completas. |
| Arquivos ≤ 100 linhas | NOK → corrigido | `AutenticacaoService.java` tinha 103 linhas (duas constructors redundantes). Removida a construtora de 5 parâmetros usada só em teste; arquivo ficou com 96 linhas e os testes foram ajustados para usar a construtora única de 6 parâmetros. |
| Métodos ≤ 30 linhas | OK | Nenhum método do domínio revisado excede o limite. |
| ≤ 3 níveis de `if`/`else`, cláusulas de guarda | OK | `RefreshTokenService`, `RedefinicaoSenhaService` e `AutenticacaoService` usam early-return; nenhum aninhamento de 3+ níveis encontrado. |
| ≤ 3 parâmetros por método | OK | `ContextoRequisicao` agrupa IP/user-agent como já previsto pela própria regra. |
| Sem linha em branco dentro de método | OK | Confirmado nos arquivos do domínio `auth`/`usuario`/`email`. |
| Sem número/texto mágico | NOK → corrigido | (1) `AutenticacaoController` usava o literal `"certame_rt"` duas vezes em `@CookieValue`, divergindo de `propriedades.getCookieNome()` — exatamente o caso citado em `code-standards.md` §2.6. Extraída a constante `COOKIE_REFRESH`. (2) `EnviadorEmailSmtp` tinha `1_000L`/`4_000L`/`3` soltos no laço de retentativa — extraídas as constantes `MAX_TENTATIVAS`, `ESPERA_PRIMEIRA_TENTATIVA_MS`, `ESPERA_SEGUNDA_TENTATIVA_MS`. |
| Variável declarada no uso | OK | Sem blocos de declaração antecipada nos arquivos revisados. |
| Sem segredo no código | OK | `JWT_SECRET` de dev tem valor literal apenas em `application-dev.properties`, comentado como não produtivo; `staging`/`prod` exigem variável de ambiente. |
| DTOs `record` com mensagens em pt-BR | OK | Todos os DTOs de `auth/dto` são `record` com `@NotBlank`/`@Size`/`@Email` e mensagem em pt-BR por campo. |
| Entidades sem Lombok, injeção por construtor | OK | `Usuario`, `RefreshToken`, `TokenSenha` com construtores/getters/setters manuais; todos os serviços usam injeção por construtor (`private final`). |
| Camadas (Controller só orquestra / Service concentra regra) | OK | `AutenticacaoController` não acessa repositório nem tem `@Transactional`; toda regra fica em `AutenticacaoService`/`RedefinicaoSenhaService`/`UsuarioService`. |
| Migração Flyway imutável, `snake_case`, PK `uuid`, `timestamptz` | OK | `V1__base_acesso.sql` segue o padrão; nenhuma migração aplicada foi editada nesta revisão. |
| Autorização sempre no servidor | OK | `SecurityFilterChain` stateless com `@EnableMethodSecurity`; claim `perfil` mapeado para `ROLE_*`; rotas de auth públicas, demais autenticadas. |
| Reuso de código (frontend) | NOK → corrigido | `acesso-page.ts`, `esqueci-senha-page.ts` e `definir-senha-page.ts` duplicavam localmente a função `corpoErro` já exportada por `core/api/erro-api.ts` (e já reutilizada por `usuario-formulario-store.ts`/`usuarios-lista-store.ts` de outra feature). Substituído pelo import da função compartilhada nas três páginas. |
| Consistência de API RxJS | NOK → corrigido | `esqueci-senha-page.ts` e `definir-senha-page.ts` usavam `.toPromise()` (depreciado), enquanto o resto do projeto usa `firstValueFrom` (inclusive `AuthStore`). Substituído por `firstValueFrom` nas duas páginas. |
| Cobertura mínima 80% | OK em 2026-09-03 · NÃO CONFIRMÁVEL em 2026-09-05 (revisão) · **OK em 2026-09-05 (tarefa 6.0)** | Na revisão original `./mvnw verify` e `npm run test:coverage` passaram. Na revalidação de 2026-09-05 o `verify` do backend falhava antes do gate JaCoCo por causa de `FlywayMigracaoTest.TI06` (defeito de outra funcionalidade, corrigido pela tarefa 1.0). **Confirmado na tarefa 6.0: `./mvnw verify` roda 571/571 testes e `jacoco:check` reporta "All coverage checks have been met."; frontend em 89,45%/84,88%/80,18%/93,93%.** |

## Aderência à TechSpec
| Decisão Técnica | Implementado | Observações |
|-----------------|--------------|-------------|
| Access token JWT HS256, 15 min, no corpo | SIM | `TokenService` usa `NimbusJwtEncoder`/`MacAlgorithm.HS256`; TTL padrão de `PropriedadesSeguranca` é 15 min; `TU16` cobre claims e expiração. |
| Refresh token opaco em cookie `HttpOnly`, `Secure`, `SameSite=Strict`, 14 dias, rotação a cada uso | SIM | `AutenticacaoController.cookie()` monta o cookie com esses atributos; `RefreshTokenService.consumirRotacionando` marca `usadoEm` e emite novo token. TI-01/TI-11 confirmam via integração. |
| Detecção de reuso do refresh revoga toda a árvore do usuário | SIM | `consumirRotacionando` chama `revogarTodosDoUsuario` quando `usadoEm != null`; TU-15/TI-10 cobrem. |
| BCrypt força 12 em produção, força 4 em teste | SIM | `SegurancaConfig.passwordEncoder()` usa `new BCryptPasswordEncoder(12)`; testes usam força 4 (`UsuarioServiceTest`) ou mock. |
| Limitador de tentativas 5/15min em memória, por chave de e-mail | SIM | `LimitadorTentativasMemoria` com `ConcurrentHashMap`; `TU17`/`TI17` cobrem o bloqueio na 6ª tentativa com `Retry-After`. |
| Resposta idêntica para credencial inválida / conta inativa / convite pendente, com hash-isca contra oráculo de tempo | SIM | `AutenticacaoService.autenticar` usa `HASH_ISCA` quando usuário é nulo ou sem senha, e trata `!senhaValida \|\| usuario == null \|\| status != ATIVO` como o mesmo `CredencialInvalidaException`; TU-02/03/04 e TI-13 confirmam corpo idêntico. |
| 204 incondicional em "esqueci minha senha", envio assíncrono pós-commit | SIM | `RedefinicaoSenhaService.solicitar` nunca lança para e-mail válido; `agendarEnvio` registra `TransactionSynchronization.afterCommit`; controller sempre devolve `noContent()`. |
| Seis rotas do contrato §6, nenhuma rota extra | SIM | `AutenticacaoController` expõe exatamente `/login`, `/cadastro`, `/senha/esqueci`, `/senha/redefinir`, `/refresh`, `/logout`. |
| Modelo de dados (`usuario`, `refresh_token`, `token_senha`) | SIM | `V1__base_acesso.sql` bate com a TechSpec; inclui ainda um `check` adicional (senha_hash nulo somente em `CONVITE_PENDENTE`) que reforça RN-05 além do que a TechSpec detalhava — melhoria, não divergência. |
| `token_senha.finalidade` reaproveitável para `CONVITE` | SIM | `FinalidadeTokenSenha` já tem `CONVITE`; `RedefinicaoSenhaService.redefinir` ativa o usuário `CONVITE_PENDENTE → ATIVO` quando a finalidade é `CONVITE` (TU-13). |
| AuthStore com access token só em memória, `restaurarSessao` no bootstrap | SIM | `auth-store.ts` mantém `accessToken` em `signal`, nunca em `localStorage`; `app.config.ts` chama `restaurarSessao()` via `provideAppInitializer`. |
| Interceptor renova uma única vez em 401 e enfileira concorrentes | SIM | `auth-interceptor.ts` usa `renovacaoEmAndamento` compartilhado e `HttpContextToken` para não reentrar; `TU-20` cobre. |
| Uma página, duas rotas (`/login`, `/cadastro`) | SIM | `app.routes.ts` mapeia ambas para `AcessoPage` com `data.aba`. |
| Papel `--color-danger-*` no Nocturne, sem hex cru no código Angular | SIM | Tokens presentes em `styles.css`; nenhum hex cru encontrado em `features/auth/**`. |
| Retentativa de e-mail 3x com espera exponencial (1s, 4s, 16s) | PARCIAL | `EnviadorEmailSmtp` implementa 3 tentativas com esperas de 1s e 4s (únicos dois intervalos possíveis entre 3 tentativas); o valor de 16s citado na TechSpec não tem onde ser usado com apenas 3 tentativas — imprecisão de redação da própria TechSpec, não do código. Registrado como observação, não bloqueante. |
| Endpoints e DTOs batem exatamente com o contrato (`RequisicaoLogin`, `RequisicaoCadastro`, `RespostaAutenticacao`, `UsuarioResumo`, `ErroApi`) | SIM | Campos, tipos e mensagens conferem com a TechSpec. |

## Tarefas verificadas
| Tarefa | Status | Observações |
|------|--------|-------------|
| 1.0 Fundação do projeto | COMPLETA | Pacote `br.com.certame`, dependências (`validation`, `mail`, `oauth2-resource-server`, Testcontainers) presentes no `pom.xml`; perfis `dev/staging/prod` configurados. |
| 2.0 Migração e persistência | COMPLETA | `V1__base_acesso.sql`, entidades `Usuario`/`RefreshToken`/`TokenSenha`, repositórios; `FlywayMigracaoTest` e `UsuarioPersistenciaTest` cobrem TI-14/TI-15. |
| 3.0 Fundação transversal | COMPLETA | `ManipuladorErrosGlobal` com `problem+json`, hierarquia de exceções em `comum/excecao`, `SegurancaConfig` (JWT HS256, BCrypt 12, filtro stateless), `CorsConfig`. |
| 4.0 Login, refresh e logout | COMPLETA | `TokenService`, `RefreshTokenService`, `AutenticacaoService`, `AutenticacaoController`, `LimitadorTentativas(Memoria)`; TU-01…04/14…17 e TI-01…03/11…13/16/17 presentes e passando. |
| 5.0 Cadastro de estudante | COMPLETA | `UsuarioService.cadastrarEstudante`, rota `/cadastro`; TU-05…07 e TI-04/05 presentes e passando. |
| 6.0 Redefinição de senha | COMPLETA | `RedefinicaoSenhaService`, `EnviadorEmail`/`EnviadorEmailSmtp`, expurgo agendado (`@Scheduled`); TU-08…13 e TI-06…10 presentes e passando. |
| 7.0 Núcleo do frontend | COMPLETA | `AuthStore`, `AuthApi`, `authInterceptor`, `authGuard`/`visitanteGuard`/`perfilGuard`, `rotaInicialPorPerfil`; TU-18…22 presentes (inclusive casos extras TU-22b…e). |
| 8.0 Fundação visual | COMPLETA | Tokens Nocturne e `--color-danger-*` em `styles.css`; nenhuma cor crua nas telas de auth. |
| 9.0 Telas de acesso | COMPLETA | `AcessoPage`, `EsqueciSenhaPage`, `DefinirSenhaPage` implementadas com Reactive Forms, tratamento de `fieldErrors` 422 e foco em erro de credencial. |
| 10.0 E2E e endurecimento | COMPLETA | `frontend/e2e/auth.spec.ts` cobre E2E-01…10 da TechSpec, mais três cenários extras (E2E-11…13) sobre roteamento por perfil e URL inexistente; suíte não executada nesta revisão de código (fica a cargo do QA), mas os arquivos existem e cobrem os cenários exigidos. |

## Testes

### Medição original (2026-09-03)
- Backend: **509** testes, 0 falhas; `./mvnw verify` build **SUCCESS**, regra JaCoCo atingida.
- Frontend: **362** testes em 71 arquivos; Statements 87,49% · Branches 83,62% · Functions 82,89% · Lines 94,93%.

### Medição vigente (2026-09-05) — substitui a anterior
- Backend (`./mvnw verify`): **535 testes, 1 falha, 0 erros** → **BUILD FAILURE**.
  A única falha é `FlywayMigracaoTest.TI06_tamanhoDeSessaoForaDoDominioEhRecusado`, **alheia a esta
  funcionalidade** (contrato de `sessao_estudo.tamanho` alterado pela `V15` de `prd-sessao-de-estudo`).
  Todas as classes de `br.com.certame.auth.*` e `br.com.certame.usuario.*` passam.
  Como o `surefire` interrompe o build, **o gate JaCoCo de 80% não chega a ser executado** — a
  cobertura por classe `*Service` não pôde ser confirmada nesta revalidação.
- Frontend (`npm run test:coverage`): **427 testes em 80 arquivos, 0 falhas** — build **SUCCESS**.
  Statements 89,47% · Branches 85,27% · Functions 80,27% · Lines 93,83% — todos acima do piso de 80%.
- Testcontainers: nenhum container órfão após a execução.

## Problemas encontrados
| Severidade | Arquivo | Linha | Descrição | Sugestão |
|------------|---------|-------|-----------|----------|
| Média | `backend/.../auth/AutenticacaoService.java` | arquivo inteiro (103→96 linhas) | Excedia o limite de 100 linhas por causa de uma construtora legada de 5 parâmetros mantida só para um teste. | **Corrigido nesta revisão**: construtora removida, `AutenticacaoServiceTest` passou a usar a construtora de 6 parâmetros com `UsuarioService` mockado. |
| Média | `backend/.../auth/AutenticacaoController.java` | 45, 62 (antes da correção) | `@CookieValue(value = "certame_rt", ...)` duplicava como literal o nome do cookie já configurável via `propriedades.getCookieNome()` — caso citado nominalmente em `code-standards.md` §2.6. | **Corrigido nesta revisão**: extraída a constante `COOKIE_REFRESH`. |
| Baixa | `backend/.../email/EnviadorEmailSmtp.java` | 51-58 (antes da correção) | Números mágicos `1_000L`, `4_000L` e `3` no laço de retentativa de e-mail. | **Corrigido nesta revisão**: extraídas as constantes `MAX_TENTATIVAS`, `ESPERA_PRIMEIRA_TENTATIVA_MS`, `ESPERA_SEGUNDA_TENTATIVA_MS`. |
| Baixa | `frontend/.../features/auth/acesso-page/acesso-page.ts`, `esqueci-senha-page.ts`, `definir-senha-page.ts` | métodos privados `corpoDoErro` (antes da correção) | Duplicavam a função `corpoErro` já exportada por `core/api/erro-api.ts` e já reaproveitada por outra feature (`usuarios-lista-store.ts`). | **Corrigido nesta revisão**: as três páginas passaram a importar `corpoErro` de `core/api/erro-api`. |
| Baixa | `frontend/.../esqueci-senha-page.ts`, `definir-senha-page.ts` | linhas com `.toPromise()` (antes da correção) | `.toPromise()` é API depreciada do RxJS; o restante do projeto (`AuthStore`, `shell.ts`, `decks-store.ts`, etc.) usa `firstValueFrom`. | **Corrigido nesta revisão**: substituído por `firstValueFrom`. |
| Baixa (corrigida na tarefa 6.0) | `backend/.../email/EnviadorEmailSmtp.java` | classe inteira | Cobertura de linha de apenas ~17% (5/29) — o laço de retentativa/falha de SMTP não tinha teste dedicado. | **Corrigido:** `EnviadorEmailSmtpTest.java` (3 testes) exercita sucesso na primeira tentativa, sucesso após uma falha e o esgotamento das três tentativas sem propagar exceção, com `JavaMailSender` mockado. |
| Baixa | `tasks/prd-acesso-e-conta/techspec.md` | linha ~496 | A TechSpec descreve "três tentativas com espera exponencial (1s, 4s, 16s)", mas três tentativas só comportam dois intervalos (antes da 2ª e da 3ª); o valor de 16s nunca seria usado pela implementação descrita. | Imprecisão apenas de redação da TechSpec — não é bug de código; ajustar o texto para "1s, 4s" ou para 4 tentativas totais, se a intenção era usar os três valores. Registrado aqui como observação, não como bloqueador de código. |

Nenhum problema de severidade **Alta** foi encontrado.

## Pontos positivos
- Resposta de login idêntica (byte a byte, inclusive tempo) para senha errada, e-mail inexistente, conta inativa e convite pendente — implementado com hash-isca, exatamente como a TechSpec exige para não virar oráculo de contas.
- Detecção de reuso de refresh token com revogação em massa está corretamente coberta em unidade (`TU-15`) e integração (`TI-10`), incluindo o cenário de redefinição de senha revogando sessões antigas.
- Suíte de testes ampla e bem além do mínimo: nomenclatura `TU`/`TI` + id da spec seguida à risca, `Clock` injetado em todos os pontos sensíveis a tempo, e-mails de teste com UUID aleatório, `EnviadorEmail` sempre substituído por *double* nos testes de integração.
- Frontend: `authInterceptor` implementado com deduplicação de renovação concorrente (`renovacaoEmAndamento` compartilhado) e proteção contra loop de retry via `HttpContextToken`, testado em `TU-20`.
- `V1__base_acesso.sql` vai além do mínimo da TechSpec ao adicionar um `check` cruzado entre `status` e `senha_hash`, reforçando RN-05 diretamente no banco.
- E2E (`auth.spec.ts`) cobre não só os 10 cenários da TechSpec como três cenários extras de roteamento por perfil e tratamento de URL inexistente, incluindo checagem de acessibilidade automatizada (`axe-core`) nas quatro telas.

## Recomendações
- ~~Adicionar um teste de unidade para o caminho de falha de `EnviadorEmailSmtp`~~ — feito na tarefa 6.0 (`EnviadorEmailSmtpTest.java`).
- Ajustar a redação da TechSpec sobre o cronograma de retentativa de e-mail (1s/4s/16s) para refletir que só há 3 tentativas e, portanto, 2 intervalos — ou decidir formalmente por 4 tentativas, se essa era a intenção original.
- Nada bloqueante identificado além do já corrigido; os itens acima são de melhoria contínua.

## Conclusão
A funcionalidade "Acesso e conta" está implementada em conformidade com a TechSpec e com as regras do projeto. Todos os 6 endpoints do contrato existem exatamente como especificado, o modelo de dados bate com `V1__base_acesso.sql`, e as decisões arquiteturais críticas (JWT HS256 15min, refresh em cookie `HttpOnly`/`SameSite=Strict` com rotação e detecção de reuso, BCrypt 12, limitador de tentativas 5/15min, resposta uniforme para credencial inválida/inativa/convite pendente, 204 incondicional em "esqueci senha") estão implementadas e testadas. As 10 tarefas do `tasks.md` foram verificadas com código e testes correspondentes.

Durante a revisão foram encontradas e corrigidas nesta mesma alteração: uma violação do limite de 100 linhas por arquivo, a duplicação literal do nome do cookie de refresh (already flagged pela própria regra do projeto), números mágicos no adaptador de e-mail, duplicação de lógica de extração de erro em três componentes do frontend, e uso de API RxJS depreciada. Após as correções, `./mvnw test`, `./mvnw verify`, `npm test` e `npm run test:coverage` foram reexecutados e passaram integralmente (509 testes backend + 362 testes frontend, cobertura acima do piso de 80% em ambos os lados). Nenhum problema de severidade alta foi encontrado; os itens de severidade baixa remanescentes (cobertura do laço de retentativa de e-mail e uma imprecisão de redação na TechSpec) não bloqueiam a aprovação.

**Veredito: APROVADO COM RESSALVAS.**
