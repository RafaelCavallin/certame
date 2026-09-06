# Tarefa 10.0: E2E e endurecimento

## Visão geral

Fecha a funcionalidade: a suíte Playwright contra o SPA e a API reais, com PostgreSQL e MailHog em contêiner; a verificação da cobertura mínima de 70% em `service`; a revisão de acessibilidade e de contraste nas quatro telas; e a auditoria de que nenhuma mensagem, corpo, cabeçalho ou latência revela a existência de uma conta.

<skills>
### Conformidade com skills

- `angular-developer` — E2E com Playwright e testes de acessibilidade.
- `spring-boot-testing` — verificação da meta de cobertura em `service`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/`. Regras relevantes: "Testes: JUnit 5 + Testcontainers no backend, Vitest + Playwright no frontend. Cobertura mínima 70% em `service`". **Desvio registrado na TechSpec:** o `AGENTS.md` fala em "JUnit 5", enquanto o BOM do Spring Boot 4.1.1 traz JUnit Jupiter 6.0.3 — a API usada é a mesma, e prevalece a versão gerenciada pelo BOM.
</rules>

<requirements>
- Executar os dez casos E2E com Playwright contra SPA e API reais, com PostgreSQL e MailHog em contêiner.
- Os seletores saem da marcação do protótipo traduzida 1:1 (`.field`, `.input`, `.btn-primary`, o controle segmentado das abas).
- E2E-06 e E2E-07 dependem do painel de nova senha do protótipo, já implementado na tarefa 9.0.
- E2E-10 cobre a navegação por teclado e a checagem automatizada de acessibilidade e contraste (mínimo 4,5:1 em texto corrido) sobre as quatro telas.
- Verificar cobertura mínima de 70% em `service`.
- Auditar que login, cadastro e "esqueci minha senha" não funcionam como oráculo de existência de conta — corpo, cabeçalhos e latência idênticos onde a TechSpec exige.
- Confirmar que nenhum log, em nenhum nível, contém e-mail completo, senha, token bruto, hash ou o link de redefinição.
</requirements>

## Subtarefas

- [x] 10.1 Configurar o ambiente E2E (Playwright, PostgreSQL e MailHog em contêiner, semente de `dev`).
- [x] 10.2 Escrever E2E-01 a E2E-04 (entrar, erro de senha, criar conta, e-mail duplicado).
- [x] 10.3 Escrever E2E-05 a E2E-07 (esqueci senha, redefinir pelo link do MailHog, reabrir link já usado).
- [x] 10.4 Escrever E2E-08 e E2E-09 (sair e voltar pelo histórico; recarregar autenticado).
- [x] 10.5 Escrever E2E-10 (percurso completo por teclado e checagem de acessibilidade e contraste nas quatro telas).
- [x] 10.6 Medir e reportar a cobertura em `service`, corrigindo lacunas até atingir 70%.
- [x] 10.7 Auditar as respostas de login e de "esqueci minha senha" quanto a corpo, cabeçalhos e latência idênticos.
- [x] 10.8 Auditar os logs quanto a dados sensíveis e confirmar que as métricas de observabilidade estão emitindo.
- [x] 10.9 Validar `java -jar` sobre o war reempacotado, conforme o risco registrado na TechSpec.

## Detalhes de implementação

Ver TechSpec, seções *Abordagem de testes → Testes E2E* (tabela completa e nota de execução), *Sequenciamento do desenvolvimento → Ordem de construção*, item 10, *Monitoramento e observabilidade* (métricas e eventos de log) e *Riscos conhecidos* (packaging `war` com Tomcat `provided`).

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-07
- CA-08
- CA-09
- CA-10
- CA-11
- CA-12

## Testes da tarefa

### Testes E2E

- [x] E2E-01 — Entrar como estudante
- [x] E2E-02 — Erro de senha preserva o e-mail
- [x] E2E-03 — Criar conta pela aba Criar conta
- [x] E2E-04 — Criar conta com e-mail duplicado
- [x] E2E-05 — Esqueci minha senha com e-mail inexistente
- [x] E2E-06 — Redefinir senha pelo link do MailHog
- [x] E2E-07 — Reabrir link já usado
- [x] E2E-08 — Sair pelo rodapé e voltar pelo histórico
- [x] E2E-09 — Recarregar a página autenticado
- [x] E2E-10 — Percurso completo por teclado nas quatro telas

## Arquivos relevantes

- `frontend/e2e/` (especificações Playwright)
- `frontend/playwright.config.ts`
- `docker-compose.yml` (PostgreSQL e MailHog para E2E)
- `backend/pom.xml` (configuração de cobertura)
