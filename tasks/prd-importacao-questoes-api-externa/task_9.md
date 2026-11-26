# Tarefa 9.0: Frontend Angular, E2E e fechamento de cobertura

## Visão geral

A tradução 1:1 do protótipo aprovado na tarefa 8.0 para Angular, e o fechamento da funcionalidade.

Cinco telas administrativas novas, o cartão de texto-base no estudo, o bloco de procedência na ficha da questão e os filtros de pendência na listagem. A peça com mais comportamento é a `ImportacoesStore`: o `POST` responde 202 e a tela passa a **consultar o progresso a cada 2 s enquanto a situação for `EXECUTANDO`**, mostrando "Importando 18 de 50"; ao chegar em `CONCLUIDO`, `PARCIAL` ou `FALHOU`, a consulta para e o relatório substitui o progresso. Parar a consulta é comportamento testado, não detalhe — é o que impede a tela de martelar o servidor indefinidamente.

O fechamento vem junto: sete cenários Playwright com a QAPI substituída por um duplo no perfil de teste — E2E não fala com fornecedor real nem gasta crédito —, e o piso de 80% aplicado nos dois módulos por `./mvnw verify` e `npm run test:coverage`.

Dois cenários E2E valem por provas de compromisso, não por cobertura: **E2E-07** exercita estudo, caderno, simulado e desempenho com o fornecedor completamente fora do ar, e **E2E-05** prova que a questão importada com texto-base é indistinguível de uma nativa para o estudante.

<skills>
### Conformidade com skills

- `angular-developer` — standalone, zoneless, signals, `input()`/`output()` como funções, `@if`/`@for`, HTTP concentrado em `core/api`, ARIA nos estados de progresso e nos sinalizadores de pendência, template e estilo em arquivos separados, estado compartilhado em serviço `providedIn: 'root'`.
- `impeccable` — os estados vazio, carregando, parcial e de erro com mensagem própria em pt-BR, motivo do descarte em texto e confirmação explícita antes de aplicar divergência em questão publicada, conforme desenhado na tarefa 8.0. O mesmo desvio registrado ali (sem pré-visualização e sem triagem em massa) vale aqui.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras que incidem aqui:

- **Regra 1** — tradução 1:1 do protótipo aprovado na tarefa 8.0. Nenhuma marcação nova nasce aqui; tokens `var(--*)` de `styles.css`, sem biblioteca de UI de terceiros.
- **Regra 2** — a interface esconde as ações de importação para quem não é administrador, **e** o servidor já recusa por `@PreAuthorize` (tarefa 7.0). A guarda de rota é conveniência, não segurança.
- **Regra 3 / RNF-09** — toda a interface e todas as mensagens em português do Brasil.
- **Frontend** (`code-standards.md` §4) — HTTP só em `core/api`; Prettier com `printWidth: 100` e `npm run format:check` retornando zero arquivos fora do padrão antes do merge.
- **Testes** (`tests.md`) — Vitest com `HttpTestingController`, nunca `fetch` real; `vi.useFakeTimers()` para a consulta de progresso, nunca espera fixa; cobertura ≥ 80% em statements, branches, functions e lines; pirâmide ~70/20/10 respeitada — o que couber em unidade não sobe para E2E.
</rules>

<requirements>
- **RF-70** — rota `admin/importacoes` sob `perfilGuard([ADMIN, SUPER_ADMIN])`, com o formulário do pedido, a lista de lotes e o relatório.
- **RF-71** — o item importado leva à tela de edição de questão **que já existe**; nenhum editor paralelo.
- **RF-72** — sinalizadores por questão importada e os mesmos como filtro na listagem.
- **RF-73** — fila de pendências de mapeamento com contagem e a ação de mapear, oferecendo **apenas nós existentes** da árvore.
- **RF-64** — comparação lado a lado e as três decisões, com confirmação explícita para questão publicada.
- **RF-57 / CA-22** — texto-base antes do enunciado na sessão, no simulado e nas telas de correção e resultado. Sem `textoBase`, **nada é renderizado** e o comportamento é idêntico ao de hoje.
- **RF-54 / CA-28** — fonte, identificador externo, data de importação e link externo visíveis na ficha administrativa.
- **CA-24** — nenhuma tela de estudante passa a depender de serviço externo.
- **RNF-06** — cobertura mínima de 80% no código novo, nos dois módulos.
</requirements>

## Subtarefas

- [ ] 9.1 Criar `core/api/importacao-api.ts` e `core/api/modelos-importacao.ts` com a tipagem dos contratos da tarefa 7.0.
- [ ] 9.2 Acrescentar `textoBase` a `core/api/modelos-estudo.ts` e `procedencia`/`textoBase` a `core/api/modelos-questao.ts`.
- [ ] 9.3 Implementar `importacoes-store.ts` com signals, incluindo a consulta a cada 2 s **apenas** enquanto `EXECUTANDO` e a parada ao concluir.
- [ ] 9.4 Implementar `importacoes-page` e `importacao-dialog`, preenchendo os campos com o vocabulário do fornecedor vindo do catálogo.
- [ ] 9.5 Implementar `relatorio-page` com a distribuição pelos dez resultados, a lista de candidatas com motivo em texto e o link para `admin/questoes/:id`.
- [ ] 9.6 Implementar `mapeamentos-page` e `mapeamento-dialog` (fila de pendências e CRUD), sem nenhum caminho para criar nó de taxonomia.
- [ ] 9.7 Implementar `divergencias-page`, com comparação lado a lado e o `confirmacao-dialog` existente antes de aplicar sobre questão publicada.
- [ ] 9.8 Implementar `features/study/texto-base-cartao` e usá-lo em `questao-cartao` e `correcao-prova`, omitindo o bloco quando não há texto.
- [ ] 9.9 Acrescentar o bloco de procedência a `questao-form-page` e os filtros de fonte e pendência a `questoes-filtros.ts` e `questoes-page`.
- [ ] 9.10 Registrar as rotas `admin/importacoes*` em `app.routes.ts` sob `perfilGuard` e o item de menu no `layout/shell`.
- [ ] 9.11 Escrever os testes Vitest TU-37 e TU-38 e cobrir stores, cliente de API e lógica de componente até o piso de 80%.
- [ ] 9.12 Escrever `frontend/e2e/importacao-questoes.spec.ts` com E2E-01 a E2E-07, com a QAPI substituída por um duplo no perfil de teste.
- [ ] 9.13 Rodar `./mvnw verify` e `npm run test:coverage`, fechar as lacunas de cobertura do código novo dos dois módulos e conferir `npm run format:check`.

## Detalhes de implementação

Ver TechSpec, seções *Visão dos componentes* → tabela **Frontend** e **Fluxo de dados** (o ciclo 202 → progresso → relatório), *Design de implementação* → **Principais interfaces** (`ImportacoesStore`, `MapeamentosStore`), *Modelos de dados* → **`RelatorioImportacao`**, **`ItemImportacaoResumo`**, **`ProcedenciaQuestao`**, **`TextoBaseResumo`** e **`PendenciaMapeamento`**, *Endpoints da API* (as notas de estado vazio e de degradação do catálogo), e *Abordagem de testes* → **Testes E2E**.

A marcação aprovada na tarefa 8.0 é a fonte da verdade visual; os seletores dos E2E saem dela.

## Critérios de aceitação relacionados

- CA-09
- CA-10
- CA-11
- CA-12
- CA-19
- CA-22
- CA-24
- CA-27
- CA-28

## Testes da tarefa

### Testes de unidade

- [ ] TU-37 — `importacoesStoreConsultaProgressoEnquantoExecutando` (Vitest)
- [ ] TU-38 — `textoBaseCartaoOmiteBlocoQuandoNaoHaTexto` (Vitest)

### Testes E2E

- [ ] E2E-01 — `administrador dispara importação e lê o relatório`
- [ ] E2E-02 — `administrador abre questão importada na tela de edição existente`
- [ ] E2E-03 — `professor completa o comentário e o administrador publica`
- [ ] E2E-04 — `administrador mapeia valor externo e reexecuta a importação`
- [ ] E2E-05 — `estudante resolve questão importada com texto-base`
- [ ] E2E-06 — `administrador decide divergência de questão publicada`
- [ ] E2E-07 — `estudo funciona normalmente com o fornecedor fora do ar`

## Arquivos relevantes

- `frontend/src/app/core/api/{importacao-api.ts,modelos-importacao.ts}`
- `frontend/src/app/core/api/{modelos-estudo.ts,modelos-questao.ts}` (modificados)
- `frontend/src/app/features/admin/importacoes/{importacoes-store.ts,importacoes-page/,importacao-dialog/,relatorio-page/,mapeamentos-page/,mapeamento-dialog/,divergencias-page/}`
- `frontend/src/app/features/study/texto-base-cartao/`
- `frontend/src/app/features/study/questao-cartao/{questao-cartao.html,questao-cartao.ts}` · `features/study/correcao-prova/correcao-prova.html` (modificados)
- `frontend/src/app/features/admin/questoes/{questao-form-page/,questoes-filtros.ts,questoes-page/}` (modificados)
- `frontend/src/app/app.routes.ts` · `frontend/src/app/layout/shell/` (modificados)
- `frontend/e2e/importacao-questoes.spec.ts`
