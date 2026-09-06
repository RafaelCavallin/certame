# Tarefa 8.0: E2E e ajuste fino de acessibilidade

## Visão geral

Fecha a funcionalidade com os seis cenários E2E ponta a ponta contra front e back reais, usando a base semeada por `V904__seed_simulados_dev.sql` (Tarefa 1.0), e o ajuste fino de acessibilidade que os testes E2E expõem: navegação por teclado completa, foco visível e contraste mínimo de 4,5:1 na lista e no formulário.

<skills>
### Conformidade com skills

- `angular-developer` — nenhuma mudança estrutural nova; ajustes de acessibilidade seguem os mesmos princípios de standalone/zoneless já aplicados nas Tarefas 6.0 e 7.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Leitura de `AGENTS.md` e das rules confirmada. `tests.md` §4 (pirâmide de testes: E2E é a camada mais estreita, um cenário por fluxo crítico, Playwright). UX do PRD: estados sinalizados por rótulo textual além da cor, navegação por teclado completa, foco visível, contraste ≥ 4,5:1, datas/horários/notas em formato brasileiro. Esta tarefa depende das Tarefas 4.0, 5.0, 6.0 e 7.0 estarem concluídas.
</rules>

<requirements>
- RF-14, RF-21, RN-01, RN-04 (validação ponta a ponta de todos os requisitos desta funcionalidade)
</requirements>

## Subtarefas

- [x] 8.1 Confirmar que `V904__seed_simulados_dev.sql` (Tarefa 1.0) cobre os cenários necessários: um simulado disponível, um agendado e um concluído com nota, além de acervo publicado suficiente para permitir montar e publicar um novo simulado no E2E-04.
- [x] 8.2 Escrever `frontend/e2e/simulados.spec.ts` com E2E-01, E2E-02, E2E-03.
- [x] 8.3 Escrever `frontend/e2e/admin-simulados.spec.ts` com E2E-04, E2E-05.
- [x] 8.4 Escrever E2E-06 (navegação por teclado e contraste na lista e no formulário) e corrigir qualquer ajuste fino de acessibilidade que o teste revelar nas Tarefas 6.0/7.0.
- [x] 8.5 Rodar a suíte completa (`./mvnw verify`, `npm run test:coverage`, `npx playwright test`) e confirmar cobertura ≥ 80% nos módulos tocados.

## Detalhes de implementação

Ver TechSpec, seção "Abordagem de testes" → "Testes E2E" (os seis casos, com `docker-compose.e2e.yml` e o ponto de partida do seed).

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-05
- CA-06
- CA-08
- CA-09
- CA-10

## Testes da tarefa

### Testes E2E

- [x] E2E-01 — `E2E01_estudanteVeOsTresGruposDeSimulados`
- [x] E2E-02 — `E2E02_estudanteFazSimuladoDoInicioAoResultado`
- [x] E2E-03 — `E2E03_estudanteRevisaERefazComoTreino`
- [x] E2E-04 — `E2E04_administradorMontaEPublicaSimulado`
- [x] E2E-05 — `E2E05_publicacaoRecusadaMostraOQueEstaInconsistente`
- [x] E2E-06 — `E2E06_navegacaoPorTecladoEContrasteNaListaESimuladoForm`

## Arquivos relevantes

- `frontend/e2e/simulados.spec.ts`
- `frontend/e2e/admin-simulados.spec.ts`
- `backend/src/main/resources/db/migration-dev/V904__seed_simulados_dev.sql`
