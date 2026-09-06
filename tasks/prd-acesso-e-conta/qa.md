# Relatório de QA — Acesso e conta

As evidências da ferramenta de navegador estão em `tasks/prd-acesso-e-conta/evidences/`.

## Resumo
- Data: 2026-09-02 · **Revalidado em 2026-09-05**
- Status: APROVADO (mantido)
- Total de critérios de aceitação: 12
- Critérios de aceitação atendidos: 12
- Bugs encontrados: 2 (expectativas E2E desatualizadas, corrigidos)

> **Revalidação 2026-09-05.** Os 12 critérios de aceitação continuam atendidos e as correções de
> BUG-01/BUG-02 seguem no repositório. **Três informações deste relatório ficaram desatualizadas e
> foram corrigidas abaixo:** (1) os números das suítes automatizadas; (2) o motivo do bloqueio do
> `./mvnw verify` — o defeito de `TaxonomiaArvorePersistenciaTest`/`AssuntoArvore` **não existe
> mais** (a classe passa: 3/3), e hoje o `verify` é interrompido por outra falha,
> `FlywayMigracaoTest.TI06`; (3) as portas de execução registradas na seção "Ambiente" descrevem a
> rodada de 2026-09-02 e não foram reexecutadas — esta revalidação foi feita apenas pelas suítes
> automatizadas (`./mvnw verify` e `npm run test:coverage`), sem subir o ambiente E2E.

## Ambiente
- Backend: `http://localhost:3000`, Spring Boot perfil `dev`
- Frontend: `http://localhost:5100`, Angular dev server
- PostgreSQL E2E: `localhost:30532`, volume descartável `certame-e2e-postgres-data`
- MailHog SMTP/API/UI: `localhost:30525` / `localhost:30580`
- Navegador: Playwright Chromium 1.62.1
- Os serviços E2E foram encerrados pelo teardown; `docker compose ps` ficou vazio e as portas foram liberadas.

## Checklist de critérios de aceitação
| ID | Critério de aceitação | Casos de teste | Status | Evidência |
|----|-----------------------|----------------|--------|-----------|
| CA-01 | Login de estudante ativo redireciona ao Início em até 2 s. | TU-01, TU-18, E2E-01, E2E-09 | PASSOU | `login-desktop.png` |
| CA-02 | Senha incorreta mostra erro inline, preserva e-mail e foca senha. | TU-02, TU-19, E2E-02 | PASSOU | `login-erro-credencial.png` |
| CA-03 | Perfil determina navegação sem ações superiores. | TU-16, TU-22, E2E-10, E2E-11 | PASSOU | `cadastro-desktop.png` |
| CA-04 | Cadastro cria estudante ativo e abre sessão. | TU-05, TU-07, TI-04, E2E-03 | PASSOU | `cadastro-desktop.png` |
| CA-05 | E-mail duplicado é recusado sem limpar os campos. | TU-06, TU-21, TI-05, E2E-04 | PASSOU | `cadastro-duplicado.png` |
| CA-06 | Pedido de redefinição confirma de forma indistinguível. | TU-08, TU-09, TI-06, E2E-05 | PASSOU | `esqueci-confirmacao.png` |
| CA-07 | E-mail malformado é barrado inline sem envio. | TU-21, TI-07, E2E-10 | PASSOU | `esqueci-confirmacao.png` |
| CA-08 | Token válido troca senha, invalida sessões e retorna ao login. | TU-10, TI-08, TI-10, E2E-06 | PASSOU | `esqueci-confirmacao.png` |
| CA-09 | Token expirado/usado mostra link inválido e novo pedido. | TU-11, TU-12, TI-09, E2E-07 | PASSOU | `definir-link-invalido.png` |
| CA-10 | Convite pendente não entra antes de definir senha. | TU-04, TU-13, TI-13, E2E-11 | PASSOU | `login-desktop.png` |
| CA-11 | Usuário inativo recebe a mesma mensagem de senha errada. | TU-03, TI-13, E2E-02 | PASSOU | `login-desktop.png` |
| CA-12 | Sair revoga sessão e histórico não restaura acesso. | TU-15, TI-12, E2E-08 | PASSOU | `login-desktop.png` |

## Testes E2E executados
| ID | Fluxo | Resultado | Observações |
|----|-------|-----------|-------------|
| E2E-01 | Entrar como estudante | PASSOU | Início e navegação de estudo carregados. |
| E2E-02 | Erro de senha preserva o e-mail | PASSOU | Erro inline e foco confirmados. |
| E2E-03 | Criar conta | PASSOU | Sessão aberta no Início. |
| E2E-04 | Cadastro duplicado | PASSOU | Campos preservados. |
| E2E-05 | Recuperação para e-mail inexistente | PASSOU | Confirmação incondicional. |
| E2E-06 | Redefinição pelo MailHog | PASSOU | Token consumido e senha alterada. |
| E2E-07 | Reabrir token usado | PASSOU | Estado de link inválido. |
| E2E-08 | Sair e voltar pelo histórico | PASSOU | Sessão não restaurada. |
| E2E-09 | Recarregar autenticado | PASSOU | Refresh restaurou a sessão. |
| E2E-10 | Teclado, foco, axe e contraste | PASSOU | Quatro telas auditadas sem violações axe. |

## Testes automatizados e cobertura
| Camada | ID | Resultado | Validação/comando | Observações |
|--------|----|-----------|-------------------|------------|
| Frontend | TU-18 a TU-22 e testes de features | PASSOU | `npm test` | 2026-09-02: 40 arquivos, 173 testes. **2026-09-05: 80 arquivos, 427 testes, 0 falhas.** |
| Frontend | Cobertura | PASSOU | `npm run test:coverage` | 2026-09-02: Statements 86,5%; branches 81,64%; functions 82,14%; lines 95,03%. 2026-09-05 (revisão): Statements 89,47%; branches 85,27%; functions 80,27%; lines 93,83%. **2026-09-05 (tarefa 6.0): Statements 89,45%; branches 84,88%; functions 80,18%; lines 93,93% (473 testes).** |
| Backend | TU-01 a TU-17 e TI-04 a TI-10 | PASSOU | `./mvnw test -Dtest='AutenticacaoCadastroControllerTest,RedefinicaoSenhaControllerTest,AutenticacaoCadastroIntegracaoTest,RedefinicaoSenhaIntegracaoTest,AutenticacaoServiceTest,AutenticacaoCadastroServiceTest,RedefinicaoSenhaServiceTest,RefreshTokenServiceTest,TokenServiceTest,LimitadorTentativasTest,UsuarioServiceTest'` | 30 testes sem falhas; serviços de auth acima de 80% nas linhas JaCoCo. |
| Backend | Build WAR | PASSOU | `./mvnw package -DskipTests` | WAR executável reempacotado com sucesso. |
| Backend | Caminho de falha de `EnviadorEmailSmtp` | **PASSOU** | `EnviadorEmailSmtpTest` — 3/3 | **Novo na tarefa 6.0.** Sucesso na 1ª tentativa, sucesso após 1 falha e esgotamento das 3 tentativas sem propagar exceção (RF-03/RF-04) |

**Situação do `./mvnw verify` completo.**

- **2026-09-02 (rodada original):** bloqueado por defeito preexistente fora desta funcionalidade —
  `TaxonomiaArvorePersistenciaTest` não encontrava `br.com.certame.catalogo.dto.AssuntoArvore`
  durante o *discovery*.
- **2026-09-05 (revalidação):** aquele defeito **foi resolvido** (`TaxonomiaArvorePersistenciaTest`
  passa 3/3). O `verify` continuava não concluindo, agora por **outra** falha, também fora desta
  funcionalidade: `FlywayMigracaoTest.TI06_tamanhoDeSessaoForaDoDominioEhRecusado`, desatualizado
  frente à migração `V15__sessao_livre_tamanho_variavel.sql` de `prd-sessao-de-estudo`.
  Total: **535 testes, 1 falha, 0 erros**.
- **2026-09-05 (tarefa 6.0):** a falha de `FlywayMigracaoTest.TI06` foi corrigida pela tarefa 1.0 de
  `tasks/correcoes-pendencias/`. `./mvnw verify` agora conclui: **571 testes, 0 falhas**, e
  `jacoco:check` reporta "All coverage checks have been met."

Em todas as datas, a suíte direcionada de acesso e conta passou integralmente.

## Acessibilidade
- Navegação por teclado com `Tab`, `Enter` e foco visível: PASSOU nas quatro telas.
- Elementos interativos com rótulos descritivos: PASSOU.
- Imagens: não há imagens informativas sem `alt`; ícones são decorativos: PASSOU.
- Contraste mínimo de 4,5:1: PASSOU pela auditoria axe.
- Labels associados aos campos: PASSOU.
- Mensagens de erro inline e acessíveis: PASSOU.
- Tamanho de fonte e responsividade desktop/mobile: PASSOU; ver `cadastro-mobile.png`.

## Bugs encontrados e corrigidos
| ID | Descrição | Severidade | Status | Correção | Teste de regressão | Evidência |
|----|-----------|------------|--------|----------|--------------------|-----------|
| BUG-01 | E2E-01 esperava `Estudar`, mas o protótipo e o shell usam `Resolver questões`. | Baixa | Corrigido | Atualizada a expectativa para o texto aprovado no protótipo. | E2E-01 reexecutado com sucesso. | `login-desktop.png` |
| BUG-02 | E2E-11 esperava `/admin/questoes` para ADMIN, embora a rota inicial configurada prefira `/admin/simulados`. | Média | Corrigido | Atualizada a expectativa para a primeira rota administrativa real e autorizada. | E2E-11 reexecutado com sucesso. | `cadastro-desktop.png` |

## Conclusão
Os 12 critérios de aceitação foram verificados e atendidos. Os dois defeitos encontrados eram expectativas de teste divergentes da especificação vigente; foram corrigidos e revalidados. QA **APROVADO**.
