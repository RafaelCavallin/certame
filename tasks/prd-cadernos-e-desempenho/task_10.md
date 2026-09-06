# Tarefa 10.0: E2E, cobertura, latência e acessibilidade

## Visão geral

O endurecimento da funcionalidade: os treze cenários E2E ponta a ponta com Playwright contra o SPA e a API reais, a verificação do piso de 80% de cobertura nos dois lados, a medição de latência dos quatro painéis com 50 mil respostas e a revisão de acessibilidade e contraste. É a tarefa que prova os doze critérios de aceitação no produto montado, e não em pedaços.

<skills>
### Conformidade com skills

- `angular-developer` — E2E com Playwright, seletores estáveis sobre a marcação traduzida do protótipo.
- `spring-boot-testing` — cenário de latência sobre base populada, com verificação do plano de consulta.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **`tests.md` §4:** E2E é o topo da pirâmide — um cenário por fluxo, nada que caiba abaixo sobe para cá.
- **`tests.md` §2 (Repeatable):** nada de espera fixa; espera por condição. Dados isolados por execução.
- **`tests.md` §7:** `./mvnw verify` aplica 80% de linhas por classe `*Service` no JaCoCo; `npm run test:coverage` aplica 80% em statements, branches, functions e lines. Ambos precisam passar localmente.
- **Regra 1:** os seletores saem da marcação do protótipo traduzida 1:1 (`.card`, `.card-title`, `.card-meta`, `.table`, `.dialog`, `.btn-primary`, `.tag`) — nenhum atributo de teste inventado que não exista no protótipo.
- **Bloqueio conhecido:** Playwright está ausente do `package.json` e é previsto em `prd-acesso-e-conta`; E2E-04, E2E-07 e E2E-10 dependem das lacunas fechadas na tarefa 1.0; toda a tarefa depende de `prd-sessao-de-estudo` (F2) implementado.
</rules>

<requirements>
- Execução com PostgreSQL em contêiner e a semente do perfil `dev`.
- **CA-01…CA-12** provados ponta a ponta pelos treze cenários.
- **TI-27** — p95 abaixo de 400 ms por painel com 50 mil respostas e 8 semanas, servido pelos agregados; o plano confirma uso dos índices e **não varre** `resposta`.
- **Acessibilidade (PRD, E2E-13)** — percurso completo por teclado com foco visível, alternativa textual equivalente ao gráfico para leitor de tela e contraste ≥ 4,5:1 em texto corrido.
- Cobertura ≥ 80% nos dois lados, com o fluxo crítico coberto em caminho feliz **e** bordas.
</requirements>

## Subtarefas

- [x] 10.1 Instalar e configurar o Playwright no `frontend/`, com o Compose de E2E.
- [x] 10.2 E2E-01…E2E-05 — os cinco cenários de cadernos.
- [x] 10.3 E2E-06…E2E-09 — os quatro cenários de desempenho.
- [x] 10.4 E2E-10 e E2E-11 — usuário novo e Modo Prova abandonado.
- [x] 10.5 E2E-12 — "Treinar" a partir de um ponto fraco.
- [x] 10.6 E2E-13 — revisão de acessibilidade das duas telas.
- [x] 10.7 TI-27 — cenário de latência com 50 mil respostas e verificação do plano de consulta.
- [x] 10.8 Rodar `./mvnw verify` e `npm run test:coverage` e fechar as lacunas de cobertura abaixo de 80%.
- [x] 10.9 Conferir que a §6 e a §8 da `TECH-SPEC.md` já refletem as rotas acrescentadas e a troca das *materialized views* pelos agregados incrementais.

## Detalhes de implementação

Ver `techspec.md`, §*Abordagem de testes* (as três tabelas de casos e a nota sobre execução com Playwright e seletores do protótipo), §*Sequenciamento do desenvolvimento* (passo 10 e a tabela de *Dependências técnicas*) e §*Monitoramento e observabilidade* (os sinais que sustentam a medição de latência e de divergência). Ordem de construção: passo 10.

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

### Testes de integração

- [x] TI-27 — Latência dos quatro painéis com 50 mil respostas e 8 semanas

### Testes E2E

- [x] E2E-01 — Abrir a tela de cadernos
- [x] E2E-02 — Marcar erro recorrente na sessão e reencontrar a questão
- [x] E2E-03 — Favoritar uma questão e reencontrá-la
- [x] E2E-04 — Criar caderno manual com nome e descrição e adicionar questões
- [x] E2E-05 — Abrir um caderno de 40 questões e estudar
- [x] E2E-06 — Abrir o desempenho com histórico
- [x] E2E-07 — Semana sem atividade
- [x] E2E-08 — Desempenho por disciplina
- [x] E2E-09 — Pontos fracos com o corte de 20 respostas
- [x] E2E-10 — Usuário novo abre cadernos e desempenho
- [x] E2E-11 — Modo Prova abandonado não altera o desempenho
- [x] E2E-12 — "Treinar" a partir de um ponto fraco
- [x] E2E-13 — Acessibilidade das duas telas

## Arquivos relevantes

- `frontend/e2e/*.spec.ts` — os treze cenários
- `frontend/package.json`, `playwright.config.ts` (modificado/criado)
- `docker-compose.e2e.yml`
- `backend/src/test/java/br/com/certame/desempenho/` — `*IntegracaoTest` de TI-27
- `backend/target/site/jacoco/`, `frontend/coverage/` — relatórios de cobertura
- `TECH-SPEC.md` §6 e §8 (conferência final)
