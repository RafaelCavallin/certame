# Tarefa 9.0: Frontend administrativo: lista, formulário e seletor de disciplinas (RF-22)

## Visão geral

Traduzir 1:1 a tela `ADMIN: EDITAIS` aprovada na tarefa 1.0: a tabela de trabalho com o aviso de prazo vencido e a exclusão com confirmação, o formulário de página cheia com os campos estruturados e o `DisciplinasSeletor` de multisseleção alimentado por `GET /admin/disciplinas`.

O seletor é o coração de CA-08 na interface: **não há entrada de texto livre**; digitar filtra a lista, nunca cria item. E o formulário mapeia os erros do servidor onde eles pertencem — 422 campo a campo, 409 no campo `nome` —, não em um toast genérico.

<skills>
### Conformidade com skills

- `angular-developer` — standalone, *zoneless*, signals em store `providedIn: 'root'`, `input()`/`output()`, `computed` para `exigePrazo`, guarda funcional `perfilGuard`, `LOCALE_ID` pt-BR.
- `impeccable` — **não aplicável**: o layout vem congelado da tarefa 1.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 1** — tradução 1:1 do protótipo aprovado. O seletor de disciplinas é construído com `.input`, `.tag` e `.card` — nenhuma dependência de UI é adicionada. **Bloqueada pela aprovação visual da tarefa 1.0.**
- **Regra 2** — o `perfilGuard` esconde e bloqueia a rota, mas **a autorização de verdade é a do servidor** (tarefa 6.0); a interface nunca é a única barreira.
- **Regra 3** — rótulos, mensagens e validações em português do Brasil; datas e números em formato brasileiro.
- `code-standards.md` §4 — HTTP só em `core/api`; nenhum componente injeta `HttpClient`; 422 e 409 mapeados campo a campo (TU-27, TU-28).
- `code-standards.md` §2 — o formulário delega a seleção ao `DisciplinasSeletor` para manter os arquivos dentro do limite.
- **PRD** — ação destrutiva pede confirmação: a exclusão passa pelo `shared/ui/confirmacao-dialog` de F1.
</rules>

<requirements>
- **RF-22** — cadastrar e editar edital com status, vagas, banca, prazo e disciplinas; qualquer alteração reflete imediatamente na visão do estudante, sem etapa de publicação.
- Tabela administrativa com a linha de prazo vencido marcada por `ph-warning-circle` e o texto "prazo vencido, revisar"; o status continua "Aberto" até correção manual.
- Formulário: nome, status, vagas, banca, órgão, prazo, URL oficial e disciplinas. `exigePrazo()` verdadeiro quando o status é `ABERTO`, bloqueando o envio com o campo vazio.
- Combos de banca e órgão alimentados por `GET /admin/bancas` e `/admin/orgaos`; disciplinas por `GET /admin/disciplinas` — carregados em paralelo com o edital.
- `DisciplinasSeletor` só emite ids vindos da árvore; nenhum campo aceita texto digitado como disciplina.
- Exclusão precedida de diálogo de confirmação.
- Rotas `/admin/editais`, `/admin/editais/novo` e `/admin/editais/:id` sob `perfilGuard([ADMIN, SUPER_ADMIN])`.
</requirements>

## Subtarefas

- [x] 9.1 Criar `features/admin/editais/editais-admin-store.ts` com os signals da lista e do formulário e o mapeamento de 422 campo a campo e de 409 para `nome`.
- [x] 9.2 Criar `features/admin/editais/editais-admin-page/` com a tabela e o aviso textual de prazo vencido.
- [x] 9.3 Integrar a exclusão ao `shared/ui/confirmacao-dialog` de F1.
- [x] 9.4 Criar `features/admin/editais/edital-form-page/` com os oito campos estruturados e `exigePrazo()` condicionando o envio.
- [x] 9.5 Criar `features/admin/editais/disciplinas-seletor/` com multisseleção filtrável, sem entrada de texto livre.
- [x] 9.6 Carregar em paralelo o edital, a árvore de disciplinas e os catálogos de banca e órgão.
- [x] 9.7 Registrar as três rotas administrativas em `app.routes.ts` sob `perfilGuard`.
- [x] 9.8 Escrever TU-26…TU-30 com Vitest.

## Detalhes de implementação

Ver `techspec.md` §*Arquitetura do sistema* → *Visão dos componentes*, tabela do frontend e o *Fluxo de dados*, §*Design de implementação* → *Principais interfaces* (`EditalFormStore`), §*Modelos de dados* (`EditalAdminResumo`, `RequisicaoEdital`, `ErroApi`), §*Endpoints da API* (as cinco rotas administrativas) e §*Sequenciamento do desenvolvimento*, passo 8.

## Critérios de aceitação relacionados

- CA-04
- CA-05
- CA-06
- CA-07
- CA-08
- CA-10
- Caso de borda 2 do PRD

## Testes da tarefa

### Testes de unidade

- [x] TU-26 — `EditalFormStore` exige prazo ao escolher status aberto (Vitest)
- [x] TU-27 — `EditalFormStore` mapeia 422 campo a campo (Vitest)
- [x] TU-28 — `EditalFormStore` mapeia 409 para o campo nome (Vitest)
- [x] TU-29 — `DisciplinasSeletor` só emite ids vindos da árvore (Vitest)
- [x] TU-30 — `EditaisAdminStore` destaca a linha com prazo vencido (Vitest)

## Arquivos relevantes

- `frontend/src/app/features/admin/editais/editais-admin-store.ts` (novo)
- `frontend/src/app/features/admin/editais/editais-admin-page/` (novo)
- `frontend/src/app/features/admin/editais/edital-form-page/` (novo)
- `frontend/src/app/features/admin/editais/disciplinas-seletor/` (novo)
- `frontend/src/app/core/api/edital-api.ts` · `modelos-edital.ts` (tarefa 8.0)
- `frontend/src/app/app.routes.ts` (**modificado**)
- `frontend/src/app/layout/shell/shell.ts` · `shell.html` (**modificado** — item "Editais" na navegação administrativa)
- `frontend/src/app/core/auth/auth-guard.ts` (`perfilGuard`, dependência)
- `frontend/src/app/shared/ui/confirmacao-dialog/` (dependência de F1)
- `protótipo/Certame.dc.html` (fonte da tradução 1:1)
