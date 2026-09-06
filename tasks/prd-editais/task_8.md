# Tarefa 8.0: Frontend do estudante: consulta de editais agrupados (RF-17)

## Visão geral

Traduzir 1:1 a tela `EDITAIS` aprovada na tarefa 1.0: `edital-api`, os modelos do contrato, o `EditaisStore` de signals, a `EditaisPage` com as três seções por status e o `EditalCard` com o rótulo textual, banca, órgão, vagas, prazo e as disciplinas como `.tag`.

A página faz **uma** requisição: o payload da lista já é o detalhe, então abrir um edital por *deep link* `/editais/:id` não dispara segunda chamada. O estado vazio é um `computed` sobre os três comprimentos (`vazio()`), para que a decisão de CA-09 não se espalhe pelo template.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone, detecção *zoneless*, estado por signals em serviço `providedIn: 'root'`, `input()`/`output()` como funções, `computed` para o estado vazio e para o rótulo de status, guarda funcional `authGuard`, `LOCALE_ID` pt-BR para datas e números.
- `impeccable` — **não aplicável**: o layout vem congelado da tarefa 1.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 1** — tradução 1:1 do protótipo aprovado, mesma marcação e mesmos tokens Nocturne. Só `var(--*)` de `styles.css`; nenhuma biblioteca de UI de terceiros. **Bloqueada pela aprovação visual da tarefa 1.0.**
- **Regra 3** — interface em português do Brasil; datas pelo `DatePipe` e números pelo `DecimalPipe` com `LOCALE_ID` pt-BR (CA-03).
- `code-standards.md` §4 — HTTP apenas em `core/api`: nenhum componente injeta `HttpClient`. Template e estilo em arquivos separados; membros de template `protected readonly`.
- `code-standards.md` §2 — `EditaisPage` delega o card a `EditalCard`, para manter os arquivos dentro do limite.
- `tests.md` §3 — Vitest com nomes descritivos em pt-BR: `it('exibe Vagas a definir quando o edital não tem vagas')`; `vi.setSystemTime` onde a data importar.
</rules>

<requirements>
- **RF-17** — três seções por status, com rótulo textual além da cor, e as disciplinas exigidas visíveis.
- Campo nulo é renderizado como "Vagas a definir" e "Data a definir" — nunca `null` nem traço mudo.
- `vagas` em formato brasileiro (`1500` → `1.500`) e `prazoInscricao` em `dd/MM/yyyy` (`2026-08-30` → `30/08/2026`).
- Estado vazio explicativo quando os três grupos vierem vazios, reusando `shared/ui/estado-vazio` (se não existir na entrega, nasce aqui).
- Rotas `/editais` e `/editais/:id` sob `authGuard`; item "Editais" (`ph ph-scroll`) na seção Estudo do `shell.html`.
- Nenhuma lógica de agrupamento ou ordenação no cliente: os grupos vêm prontos do servidor.
</requirements>

## Subtarefas

- [x] 8.1 Criar `core/api/modelos-edital.ts` com a tipagem de `EditaisAgrupados`, `EditalResumo`, `EditalDetalhe`, `EditalAdminResumo`, `RequisicaoEdital`, `DisciplinaExigida` e `ItemCatalogoResumo`.
- [x] 8.2 Criar `core/api/edital-api.ts` com as seis rotas — único ponto que fala com a API de editais.
- [x] 8.3 Criar `features/editais/editais-store.ts` com `carregar()`, `agrupados`, `carregando`, `vazio` e `selecionar(id)`.
- [x] 8.4 Criar `features/editais/editais-page/` com as três seções, título e contagem por seção.
- [x] 8.5 Criar `features/editais/edital-card/` com rótulo textual de status, banca, órgão, vagas, prazo e as disciplinas como `.tag`.
- [x] 8.6 Renderizar o estado vazio por `shared/ui/estado-vazio` quando `vazio()` for verdadeiro (criando o componente com a mesma assinatura se F3 ainda não o tiver entregue).
- [x] 8.7 Configurar `LOCALE_ID` pt-BR e aplicar `DatePipe`/`DecimalPipe` no card.
- [x] 8.8 Registrar `/editais` e `/editais/:id` em `app.routes.ts` sob `authGuard` e acrescentar o item "Editais" ao `shell.html`.
- [x] 8.9 Escrever TU-23, TU-24 e TU-25 com Vitest.

## Detalhes de implementação

Ver `techspec.md` §*Arquitetura do sistema* → *Visão dos componentes*, tabela do frontend, §*Design de implementação* → *Principais interfaces* (`EditaisStore`), §*Modelos de dados* (`EditaisAgrupados`, `EditalResumo`, incluindo a nota sobre edital previsto com campos nulos) e §*Sequenciamento do desenvolvimento*, passo 8.

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-09

## Testes da tarefa

### Testes de unidade

- [x] TU-23 — `EditaisStore` expõe `vazio()` quando os três grupos vêm vazios (Vitest)
- [x] TU-24 — `EditalCard` formata vagas e prazo em pt-BR (Vitest)
- [x] TU-25 — `EditalCard` com vagas e prazo nulos (Vitest)

## Arquivos relevantes

- `frontend/src/app/core/api/edital-api.ts` (novo)
- `frontend/src/app/core/api/modelos-edital.ts` (novo)
- `frontend/src/app/features/editais/editais-store.ts` (novo)
- `frontend/src/app/features/editais/editais-page/` (novo)
- `frontend/src/app/features/editais/edital-card/` (novo)
- `frontend/src/app/app.routes.ts` (**modificado**)
- `frontend/src/app/layout/shell/shell.html` (**modificado**)
- `frontend/src/app/shared/ui/estado-vazio/` (reutilizado de F3, ou criado aqui)
- `frontend/src/styles.css` · `protótipo/Certame.dc.html` (fonte da tradução 1:1)
