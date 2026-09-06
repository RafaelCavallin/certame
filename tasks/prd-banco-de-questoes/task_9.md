# Tarefa 9.0: Frontend administrativo (RF-18, RF-19, RF-20, RF-37)

## Visão geral

As quatro telas em **tradução 1:1 do protótipo aprovado na tarefa 1.0**: lista de questões, formulário de página cheia, árvore de matérias e o cadastro de bancas/órgãos, mais os clientes HTTP, o store de filtros, o store do formulário, o diálogo de confirmação reutilizável e as cinco rotas sob `perfilGuard`.

Duas decisões estruturam o estado. Os **filtros vivem nos *query params***, não num store em memória — é a URL que atende a exigência do PRD de "voltar da edição devolve a lista no mesmo ponto", e ela sobrevive a recarga, *deep link* e ao botão voltar do navegador. E o formulário usa **Reactive Forms, não Signal Forms**: o bloco dinâmico de alternativas com `FormArray` não é o lugar de assumir esse risco na F1.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone, detecção zoneless, estado por signals, guardas e interceptores funcionais, Reactive Forms com `FormArray` para as alternativas e sincronização de filtros por `queryParams` do Router.
- `impeccable` — **não aplicável**: o layout é congelado pela regra 1 do `AGENTS.md`; esta tarefa traduz o protótipo, não redesenha.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: regra 1 (tradução 1:1 do protótipo, mesma marcação, tokens Nocturne por `var(--*)` de `styles.css`, **sem valor cru** e sem biblioteca de UI de terceiros — nenhuma marcação nova nasce aqui; o que faltava entrou na tarefa 1.0), regra 2 (a interface esconde as ações que o perfil não pode executar, mas a autorização real é a do servidor — E2E-10 prova) e regra 3 (interface e mensagens de validação em português do Brasil). Nenhum desvio.

**Bloqueios:** a aprovação visual da tarefa 1.0 e o `styles.css` do Nocturne entregue por `prd-acesso-e-conta` (passo 8a).
</rules>

<requirements>
- **`core/api/questao-api.ts`** — cliente de `/admin/questoes` (listar, obter, criar, atualizar, excluir) e das três transições de status.
- **`core/api/catalogo-api.ts`** — cliente de disciplinas, assuntos, e de bancas/órgãos (leitura para os combos, e CRUD para a tela `catalogo-page`).
- **`core/api/modelos-questao.ts`** — tipagem dos contratos desta especificação, em `camelCase`.
- **`QuestoesFiltros`** (signals: `termo`, `disciplinaId`, `bancaId`, `status`, `pagina`, `tamanho`) com `aplicar()` e `limpar()`. Alterar um filtro **reescreve a URL e zera a página**; voltar do formulário **reconstrói termo, filtros e página a partir da URL**.
- **`questoes-page`** (RF-18) — tabela paginada com código, enunciado resumido, disciplina, banca/ano, tipo e status; busca por texto; filtros de disciplina, banca e status; ações de editar, publicar, despublicar e excluir. Estado vazio com a ação de limpar filtros e "Próxima" desabilitada quando `totalElements` é zero.
- **`QuestaoFormStore`** — `carregar(id | null)`, `salvarRascunho()`, `enviarParaRevisao()`, `publicar()`, `despublicar()` e o signal `pendenciasPublicacao`.
- **`questao-form-page`** (RF-19) — formulário de página cheia com `FormArray` de alternativas limitado a cinco (o botão "Adicionar alternativa" desabilita em `E`); tipo `CE` **substitui o bloco de alternativas** pela escolha Certo/Errado, avisando antes de aplicar a troca (CA-04); o catálogo é carregado uma vez (disciplinas, assuntos da disciplina escolhida, bancas e órgãos) e o agregado inteiro vai num único `POST`/`PUT`.
- Ao receber **422 `PUBLICACAO_INVALIDA`**, cada pendência marca seu `FormControl`, os demais campos ficam limpos e preenchidos, e a tela **rola até o primeiro** — nada do que foi digitado se perde (CA-06).
- Ao receber **409 `QUESTAO_EM_USO`** na exclusão, a tela oferece despublicar como alternativa.
- **`materias-page`** (RF-20) — árvore de disciplinas e assuntos com contagem, com criar, renomear, arquivar e excluir. No **409 `ASSUNTO_EM_USO`**, o diálogo troca o botão "Excluir" por "Arquivar" (CA-11).
- **`catalogo-page`** (RF-37) — listagem de bancas e órgãos com criar, editar e excluir, **visível e operável apenas para o perfil administrador**. No **409 `BANCA_EM_USO`/`ORGAO_EM_USO`**, a exclusão é recusada com mensagem explícita — sem oferta de arquivar, diferente do assunto (CA-13, CA-14).
- **`shared/ui/confirmacao-dialog`** — confirmação explícita para toda ação destrutiva (excluir, despublicar, arquivar), reutilizável pelas fases seguintes.
- **`app.routes.ts`** — `/admin/questoes`, `/admin/questoes/nova`, `/admin/questoes/:id`, `/admin/materias` e `/admin/catalogo`, sob `perfilGuard`.
- Formulário totalmente operável por teclado, com foco visível e validação inline por campo; status com rótulo textual, nunca apenas cor.
</requirements>

## Subtarefas

- [x] 9.1 Criar `core/api/modelos-questao.ts` com a tipagem dos contratos (`QuestaoResumo`, `QuestaoDetalhe`, `RequisicaoQuestao`, `AlternativaDto`, `DisciplinaArvore`, `AssuntoArvore`, `ItemCatalogo`, `PaginaResposta<T>`, `ErroApi`).
- [x] 9.2 Criar `core/api/questao-api.ts` e `core/api/catalogo-api.ts`.
- [x] 9.3 Implementar `questoes-filtros.ts` com os signals espelhados em `queryParams`, com TU-31 e TU-32.
- [x] 9.4 Implementar `questoes-page` em tradução 1:1 do protótipo, com tabela, busca, filtros, paginação, estado vazio e as ações da linha.
- [x] 9.5 Implementar `shared/ui/confirmacao-dialog`, com a variante "excluir → arquivar".
- [x] 9.6 Implementar `questao-form-store.ts` com o carregamento, o salvamento e as transições, com TU-33, TU-34 e TU-35.
- [x] 9.7 Implementar `questao-form-page` em tradução 1:1 do protótipo, com o `FormArray` de alternativas, a troca `ME`/`CE` e o bloco de erro inline de publicação.
- [x] 9.8 Implementar `materias-page` com a árvore, as contagens e o diálogo de edição/arquivamento.
- [x] 9.9 Implementar `catalogo-page` com a listagem, o formulário de banca/órgão e a exclusão com confirmação, restrita ao perfil administrador.
- [x] 9.10 Acrescentar as cinco rotas ao `app.routes.ts` sob `perfilGuard`.
- [x] 9.11 Esconder as ações de publicar/despublicar para o professor, e a rota `/admin/catalogo` inteira para quem não é administrador — lembrando que a autorização real é a do servidor (regra 2).

## Detalhes de implementação

Ver TechSpec, seções *Visão dos componentes* (tabela de componentes do frontend e o *Fluxo de dados*), *Principais interfaces* (`QuestoesFiltros`, `QuestaoFormStore` e `CatalogoService`), *Modelos de dados* (todos os contratos, incluindo `RequisicaoItemCatalogo`), *Endpoints da API* (as dezesseis rotas consumidas) e *Principais decisões* (Reactive Forms em vez de Signal Forms, filtros nos *query params* em vez de store em memória, duplicar questão fora do escopo, banca/órgão como catálogo plano mantido pelo administrador).

## Critérios de aceitação relacionados

- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-10
- CA-11
- CA-13
- CA-14

## Testes da tarefa

### Testes de unidade

- [x] TU-31 — `QuestoesFiltros` espelha filtros nos query params (Vitest)
- [x] TU-32 — `QuestoesFiltros` restaura estado ao voltar do formulário (Vitest)
- [x] TU-33 — `QuestaoFormStore` troca de tipo (Vitest)
- [x] TU-34 — `QuestaoFormStore` limita alternativas a cinco (Vitest)
- [x] TU-35 — `QuestaoFormStore` mapeia `PUBLICACAO_INVALIDA` (Vitest)

## Arquivos relevantes

- `frontend/src/app/core/api/questao-api.ts`
- `frontend/src/app/core/api/catalogo-api.ts`
- `frontend/src/app/core/api/modelos-questao.ts`
- `frontend/src/app/features/admin/questoes/questoes-page/`
- `frontend/src/app/features/admin/questoes/questoes-filtros.ts`
- `frontend/src/app/features/admin/questoes/questao-form-page/`
- `frontend/src/app/features/admin/questoes/questao-form-store.ts`
- `frontend/src/app/features/admin/materias/materias-page/`
- `frontend/src/app/features/admin/catalogo/catalogo-page/`
- `frontend/src/app/shared/ui/confirmacao-dialog/`
- `frontend/src/app/app.routes.ts` (modificado)
