# Tarefa 10.0: E2E, cobertura, acessibilidade e atualização da `TECH-SPEC.md`

## Visão geral

Fechar a entrega: os doze cenários E2E com Playwright contra o SPA e a API reais, a verificação da cobertura mínima de 80% nas duas pontas, a revisão de acessibilidade e contraste, e a atualização da `TECH-SPEC.md` — §6 com as duas rotas de leitura e a correção do perfil administrativo, §5 com as duas tabelas novas.

A atualização do documento **não é follow-up**: a regra 4 do `AGENTS.md` exige que entre na mesma alteração que introduz os endpoints.

<skills>
### Conformidade com skills

- `spring-boot-testing` — verificação da regra de cobertura JaCoCo no `verify`.
- `angular-developer` — `npm run test:coverage` (v8, falha abaixo de 80%).
- `impeccable` — **não aplicável**: a acessibilidade aqui é verificação do que a tarefa 1.0 desenhou, não redesenho.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 4** — §6 da `TECH-SPEC.md` recebe `GET /api/v1/editais` (acréscimo) e a correção do perfil de `/admin/editais` de "professor+" para "admin+", como o PRD exige; §5 recebe `edital` e `edital_disciplina`.
- `tests.md` §1 — cobertura mínima de **80%**, que prevalece sobre o texto de 70% das TechSpecs de F1.
- `tests.md` §4, **desvio registrado** — a proporção 46% / 35% / 19% foge do alvo 70/20/10 porque a funcionalidade é essencialmente um CRUD: autorização por perfil, unicidade de nome, FK da taxonomia e ausência de cache só se provam acima da unidade, e inventar testes de unidade para atingir a proporção seria o "teste decorativo" que a §1 proíbe. O peso de *execução* fica na base: dos 23 testes de integração, 4 são `@DataJpaTest`, 15 `@WebMvcTest` e apenas 4 `@SpringBootTest`.
- **PRD, Experiência do usuário** — navegação completa por teclado, foco visível e contraste ≥ 4,5:1, verificados por E2E-12 nas duas telas.
- **Regra 2** — E2E-10 prova que o bloqueio não é só de interface: a chamada direta à API devolve 403, verificada na mesma suíte.
</rules>

<requirements>
- Playwright configurado no `package.json` (ainda ausente; previsto em `prd-acesso-e-conta`) e executando contra o SPA e a API reais, com PostgreSQL em contêiner e a semente de `dev`.
- Seletores extraídos da marcação do protótipo traduzida 1:1: `.card`, `.tag`, `.table`, `.field`, `.input`, `.btn-primary`, `.dialog`.
- `./mvnw verify` e `npm run test:coverage` passando com ≥ 80%.
- `TECH-SPEC.md` §5 e §6 atualizadas na mesma entrega.
</requirements>

## Subtarefas

- [x] 10.1 Configurar Playwright no frontend, com a base de `dev` semeada pela `V905`.
- [x] 10.2 Escrever E2E-01…E2E-03 e E2E-09 sobre a tela do estudante.
- [x] 10.3 Escrever E2E-04…E2E-08 e E2E-11 sobre a tela administrativa.
- [x] 10.4 Escrever E2E-10, verificando na mesma suíte o menu escondido, o redirecionamento da rota e o 403 da chamada direta à API.
- [x] 10.5 Escrever E2E-12: percurso por teclado com foco visível e contraste ≥ 4,5:1 dos rótulos de status nas duas telas.
- [x] 10.6 Rodar `./mvnw verify` e `npm run test:coverage` e fechar as lacunas até ≥ 80% nas duas pontas.
- [x] 10.7 Atualizar a §6 da `TECH-SPEC.md`: acrescentar `GET /api/v1/editais` e corrigir o perfil de `/admin/editais` para admin+.
- [x] 10.8 Atualizar a §5 da `TECH-SPEC.md` com as tabelas `edital` e `edital_disciplina`.
- [x] 10.9 Registrar na `TECH-SPEC.md` o desvio de proporção da pirâmide de testes e a versão final da migração aplicada.

## Detalhes de implementação

Ver `techspec.md` §*Abordagem de testes* → *Testes E2E* (os doze cenários e a nota de execução), §*Conformidade com o AGENTS.md e as rules* (linhas de cobertura, `tests.md` §4 e regra 4) e §*Sequenciamento do desenvolvimento*, passos 9 e 10.

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
- Casos de borda 1, 2 e 3 do PRD

## Testes da tarefa

### Testes E2E

- [x] E2E-01 — Estudante abre Editais pelo menu
- [x] E2E-02 — Abrir um edital e ver as disciplinas exigidas
- [x] E2E-03 — Conferir banca, vagas e prazo de um edital aberto
- [x] E2E-04 — Administrador cadastra um edital completo
- [x] E2E-05 — Administrador corrige um dado e o estudante recarrega
- [x] E2E-06 — Administrador muda um edital de previsto para aberto
- [x] E2E-07 — Administrador exclui um edital com confirmação
- [x] E2E-08 — Selecionar disciplinas no formulário
- [x] E2E-09 — Base sem nenhum edital
- [x] E2E-10 — Estudante tenta alcançar a administração
- [x] E2E-11 — Lista administrativa com prazo vencido
- [x] E2E-12 — Percurso por teclado e contraste dos rótulos de status

## Arquivos relevantes

- `frontend/e2e/editais.spec.ts` (novo) — os doze cenários E2E.
- `frontend/playwright.config.ts`, `frontend/package.json` (Playwright já configurado por `prd-acesso-e-conta`; nenhuma mudança necessária).
- `frontend/src/app/layout/shell/shell.html` (**modificado** — `tabindex="0"` na região rolável `<section>` do conteúdo, corrigindo a violação `scrollable-region-focusable` que o axe-core acusou em E2E-12; correção global de acessibilidade, não específica de editais).
- `TECH-SPEC.md` (**modificado** — §3 e §6 com a rota/perfil `admin+` de `/admin/editais`, §5 com as tabelas `edital` e `edital_disciplina`, §11 com o desvio de pirâmide de testes e a versão final da migração e da semente).
- `backend/pom.xml` (regra JaCoCo já configurada no `verify`).
- `backend/src/main/resources/db/migration/V12__editais.sql` e `db/migration-dev/V907__seed_editais_dev.sql` (versões finais aplicadas; a TechSpec da tarefa 2.0 previa `V7`/`V905`, registrado como decisão em `TECH-SPEC.md` §11).
