# Tarefa 7.0: Frontend de cadernos e `shared/ui/estado-vazio`

## Visão geral

A tela `CADERNOS` em tradução 1:1 do protótipo aprovado na tarefa 1.0: grade de cards com ícone, nome, descrição, barra de progresso e meta, mais o diálogo de criar/renomear, o seletor de questões e o bloco de estado vazio reutilizável — que nasce aqui e é reaproveitado pela tela de desempenho na tarefa 8.0.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone, detecção *zoneless*, estado por signals em serviço `providedIn: 'root'`, `input()`/`output()` como funções, `computed` para derivar ordenação e estado vazio, guardas funcionais, `LOCALE_ID` pt-BR para números e datas.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 1 (inegociável):** tradução 1:1 do protótipo — mesma marcação, mesmos tokens Nocturne, sempre `var(--*)` de `styles.css`. Nenhum valor cru de cor, fonte, espaçamento, raio ou sombra; nenhuma biblioteca de UI de terceiros. Esta tarefa **não começa** antes da aprovação visual da tarefa 1.0.
- **Regra 2:** a interface esconde renomear, excluir e adicionar questões quando `editavel: false`, mas quem decide é o servidor — a interface nunca é a autorização.
- **Regra 3:** textos, rótulos e mensagens em pt-BR; datas e números em formato brasileiro.
- **`code-standards.md` §4:** componentes standalone; HTTP só em `core/api` — nenhum componente injeta `HttpClient`; template e estilo em arquivos separados; 409 e 422 mapeados campo a campo.
- **`tests.md` §6:** HTTP sempre por `HttpTestingController`, nunca `fetch` real; `http.verify()` ao final.
</rules>

<requirements>
- **RF-13/CA-01** — cada card exibe nome, descrição, progresso e data da última atualização.
- **CA-02, CA-03** — os dois cadernos automáticos aparecem primeiro na grade, sempre, mesmo vazios.
- **CA-04** — criar caderno manual com nome e descrição, adicionar questões pelo seletor, e ver o caderno na grade com progresso zerado e a barra vazia (não escondida). O 409 `NOME_CADERNO_EM_USO` marca o `FormControl` `nome`, sem *toast* genérico.
- **CA-11** — `vazio()` é verdadeiro quando todos os cadernos têm zero questões, inclusive com os dois automáticos presentes; a página exibe a orientação de como começar em vez da grade.
- Abrir um card dispara `POST /estudo/sessoes` com `origem: "CADERNO"` — o fluxo completo é fechado na tarefa 9.0.
- Ícones fixos definidos na tarefa 1.0: `ph-warning-circle`, `ph-star` e `ph-notebook`.
- O seletor de questões consome `GET /questoes` (F2); sem ele CA-04 não fecha (ver *Riscos conhecidos*).
</requirements>

## Subtarefas

- [x] 7.1 `core/api/caderno-api.ts` — cliente HTTP das sete rotas de `/cadernos`, com o mapeamento de 409/422 para erro de campo (TU-43).
- [x] 7.2 `core/api/modelos-desempenho.ts` — tipagem dos contratos desta especificação (compartilhada com a tarefa 8.0).
- [x] 7.3 `shared/ui/estado-vazio` — título, orientação e ação, reutilizável.
- [x] 7.4 `features/cadernos/cadernos-store.ts` — signals da lista, ordenação com automáticos primeiro, `vazio` computado e as ações criar/renomear/excluir/adicionar/estudar (TU-42).
- [x] 7.5 `features/cadernos/cadernos-page` — grade de cards traduzida do protótipo, com barra de progresso e meta.
- [x] 7.6 `features/cadernos/caderno-dialog` — criar e renomear caderno manual.
- [x] 7.7 `features/cadernos/questoes-picker-dialog` — seleção de questões sobre `GET /questoes`, reusando o `confirmacao-dialog` de `prd-banco-de-questoes` onde couber.
- [x] 7.8 Registrar `/cadernos` em `app.routes.ts` sob `authGuard`.
- [x] 7.9 Testes TU-42 e TU-43.

## Detalhes de implementação

Ver `techspec.md`, §*Arquitetura do sistema* → *Frontend* (linhas de `caderno-api`, `cadernos-page`, `cadernos-store`, `caderno-dialog`, `questoes-picker-dialog`, `estado-vazio`, `app.routes.ts`), §*Principais interfaces* (bloco `CadernosStore`), §*Modelos de dados* (`CadernoResumo` e a nota do caderno recém-criado; `ErroApi`), §*Endpoints da API* (a nota sobre `CadernosStore.vazio` no exemplo de usuário novo) e §*Riscos conhecidos* (ícone do card e dependência de `GET /questoes`). Ordem de construção: passo 7.

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-04
- CA-11

## Testes da tarefa

### Testes de unidade

- [x] TU-42 — `CadernosStore` marca `vazio` quando todos os cadernos têm zero questões
- [x] TU-43 — `caderno-api` mapeia 409 de nome duplicado para o campo `nome`

## Arquivos relevantes

- `frontend/src/app/core/api/caderno-api.ts`, `modelos-desempenho.ts`
- `frontend/src/app/shared/ui/estado-vazio/`
- `frontend/src/app/features/cadernos/cadernos-page/`, `cadernos-store.ts`, `caderno-dialog/`, `questoes-picker-dialog/`
- `frontend/src/app/app.routes.ts` (modificado)
- `frontend/src/styles.css` — tokens Nocturne (somente leitura)
- `protótipo/Certame.dc.html` — fonte da marcação
