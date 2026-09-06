# Tarefa 6.0: Frontend administrativo (lista, formulário, composição por disciplina)

## Visão geral

Traduz 1:1 os artboards `admin-simulados` e `admin-simulado-form` do protótipo Nocturne: a tabela administrativa com filtros e status derivado, e o formulário de dados gerais com a tabela de composição disciplina × quantidade, soma comparada ao número de questões, e as ações de publicar/despublicar. Consome os endpoints da Tarefa 4.0.

<skills>
### Conformidade com skills

- `angular-developer` — standalone, zoneless, `signal`/`computed`, `input()`/`output()` como funções, `templateUrl`/`styleUrl` separados, cliente HTTP concentrado em `core/api`, membros de template como `protected readonly`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Leitura de `AGENTS.md` e das rules confirmada. Regra 1 (tradução 1:1 do protótipo aprovado, mesma marcação, tokens `var(--*)` de `styles.css`, sem biblioteca de UI de terceiros) — aplicam-se os quatro desvios aprovados e registrados na TechSpec (botão/coluna "inscritos" saem em favor de `tentativasConcluidas`; composição vira tabela disciplina × quantidade; card "Próximo simulado" sem botão de inscrição; segmento de status vira indicador somente leitura). HTTP concentrado em `core/api/simulado-api.ts`, componente não injeta `HttpClient` direto. Resposta 422 mapeada campo a campo para os `FormControl`; demais erros viram toast. Rotas `/admin/simulados`, `/admin/simulados/nova`, `/admin/simulados/:id` com `perfilGuard([ADMIN, SUPER_ADMIN])`.
</rules>

<requirements>
- RF-21 (CRUD de simulados — interface administrativa)
</requirements>

## Subtarefas

- [x] 6.1 Criar `core/api/simulado-api.ts` e `core/models/modelos-simulado.ts` (contratos de todos os DTOs administrativos e do estudante), com `.spec.ts` cobrindo as chamadas via `HttpTestingController`.
- [x] 6.2 Criar `features/admin/simulados/simulados-page/` — tabela com filtros (`q`, `estado`, `bancaId`), paginação, coluna de status derivado e `composicaoFechada`.
- [x] 6.3 Criar `features/admin/simulados/simulado-form-page/` — dados gerais (nome, descrição, banca, número de questões, duração, janela), indicador de estado somente leitura, ações Publicar/Despublicar/Excluir com exibição de `PUBLICACAO_INVALIDA` campo a campo.
- [x] 6.4 Criar `features/admin/simulados/composicao-disciplinas/` — tabela disciplina × quantidade, soma comparada ao número de questões, `disponiveis` por disciplina, botão Publicar desabilitado enquanto `fechada` for `false`.
- [x] 6.5 Adicionar as rotas administrativas em `app.routes.ts` com `perfilGuard([ADMIN, SUPER_ADMIN])` e o item "Simulados" na navegação administrativa do `layout/shell/shell.html`.
- [x] 6.6 Testar manualmente no navegador o fluxo de montar e publicar um simulado (cadastro → composição → publicar bloqueado até fechar a soma → publicado), com o backend das Tarefas 3.0/4.0 rodando.

## Detalhes de implementação

Ver TechSpec, seção "Visão dos componentes" (mapa `features/admin/simulados/`), "Modelos de dados" (`RequisicaoSimulado`, `RequisicaoComposicao`, `ResumoComposicao`, `SimuladoResumoAdmin`, `SimuladoDetalhe`), "Endpoints da API" (contratos consumidos) e "Considerações técnicas" → "Desvios do protótipo, aprovados com o usuário".

## Critérios de aceitação relacionados

- CA-08
- CA-09
- CA-10

## Testes da tarefa

Nenhum caso de teste numerado da TechSpec é atribuído especificamente ao frontend administrativo (a TechSpec concentra TU/TI no backend e reserva E2E-04/E2E-05 para o fluxo completo na Tarefa 8.0). Ainda assim, todo código novo precisa de teste (`tests.md` regra 1): escrever `*.spec.ts` (Vitest) para `simulado-api.ts`, `simulados-page`, `simulado-form-page` e `composicao-disciplinas`, cobrindo carregamento, filtros, envio do formulário, exibição de `fieldErrors` e o estado do botão Publicar conforme `fechada`.

## Arquivos relevantes

- `frontend/src/app/core/api/simulado-api.ts` (+ `.spec.ts`)
- `frontend/src/app/core/models/modelos-simulado.ts`
- `frontend/src/app/features/admin/simulados/simulados-page/`
- `frontend/src/app/features/admin/simulados/simulado-form-page/`
- `frontend/src/app/features/admin/simulados/composicao-disciplinas/`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/layout/shell/shell.html`
