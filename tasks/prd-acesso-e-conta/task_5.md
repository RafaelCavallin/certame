# Tarefa 5.0: Cadastro de estudante (RF-02)

## Visão geral

Implementa o autosserviço de cadastro: `UsuarioService` como única porta de escrita em `usuario` e a rota `POST /auth/cadastro`, que cria a conta com perfil `ESTUDANTE` e status `ATIVO` e já devolve a sessão aberta. Depende da tarefa 4.0 por reaproveitar a emissão de sessão.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito no `UsuarioService`, `@WebMvcTest` com `MockMvcTester` na rota, `@SpringBootTest` + Testcontainers no fluxo de duplicidade; `PasswordEncoder` BCrypt real com força reduzida nos testes.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/`. Regras relevantes: regra 3 (`UsuarioService`, mensagens em pt-BR), regra 4 (a rota é exatamente a da §6) e "domínio + testes antes do controller". O cadastro é restrito ao perfil estudante — professor, administrador e super-admin pertencem a `prd-gestao-de-usuarios` (RF-24). Nenhum desvio.
</rules>

<requirements>
- **RF-02** — Cadastro com nome, e-mail e senha; a conta nasce com perfil `ESTUDANTE` e status `ATIVO`, e o usuário já entra autenticado (201 com `RespostaAutenticacao`).
- E-mail duplicado devolve 409 `EMAIL_EM_USO` acompanhado de `fieldErrors.email`, para que o SPA marque o campo sem limpar os demais.
- A unicidade é garantida pelo índice parcial `usuario_email_uk`; a violação de constraint é traduzida para o mesmo 409, cobrindo a corrida entre requisições simultâneas.
- E-mail normalizado para minúsculas no servidor, tanto no cadastro quanto no login.
- Senha gravada em BCrypt força 12; validação de 8 a 72 caracteres e nome de 3 a 120.
- O limitador de tentativas também protege a rota de cadastro (mitigação do risco de enumeração de contas).
</requirements>

## Subtarefas

- [x] 5.1 Criar o DTO `RequisicaoCadastro` em `auth/dto/` com Bean Validation conforme a tabela da TechSpec.
- [x] 5.2 Implementar `UsuarioService` com a criação de estudante, a normalização de e-mail e a troca de senha (esta última consumida pela tarefa 6.0), com TU-05 a TU-07.
- [x] 5.3 Implementar `AutenticacaoService.cadastrarEstudante`, reaproveitando a emissão de sessão da tarefa 4.0.
- [x] 5.4 Acrescentar `POST /auth/cadastro` ao `AutenticacaoController`, devolvendo 201 e o cookie de refresh.
- [x] 5.5 Traduzir a violação do índice `usuario_email_uk` para `EmailEmUsoException` com `fieldErrors` preenchido.
- [x] 5.6 Aplicar o limitador de tentativas à rota de cadastro.
- [x] 5.7 Escrever os testes de integração TI-04 e TI-05.
- [x] 5.8 Registrar a métrica `certame.auth.cadastro`.

## Detalhes de implementação

Ver TechSpec, seções *Modelos de dados → `RequisicaoCadastro`*, *Endpoints da API → `POST /api/v1/auth/cadastro`* (incluindo a nota sobre `fieldErrors` e a corrida de unicidade), *Riscos conhecidos* (enumeração de contas pelo cadastro) e *Sequenciamento do desenvolvimento*, item 5.

## Critérios de aceitação relacionados

- CA-04
- CA-05

## Testes da tarefa

### Testes de unidade

- [x] TU-05 — `cadastrarEstudante` com e-mail inédito
- [x] TU-06 — `cadastrarEstudante` com e-mail existente
- [x] TU-07 — Normalização de e-mail no cadastro e no login

### Testes de integração

- [x] TI-04 — `POST /auth/cadastro` com e-mail inédito
- [x] TI-05 — `POST /auth/cadastro` com e-mail duplicado

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/usuario/UsuarioService.java`
- `backend/src/main/java/br/com/certame/auth/AutenticacaoService.java`
- `backend/src/main/java/br/com/certame/auth/AutenticacaoController.java`
- `backend/src/main/java/br/com/certame/auth/dto/RequisicaoCadastro.java`
