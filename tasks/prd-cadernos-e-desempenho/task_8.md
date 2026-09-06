# Tarefa 8.0: Frontend de desempenho — store, página, gráfico SVG e painéis

## Visão geral

A tela `DESEMPENHO`, com os quatro painéis e o gráfico de evolução semanal desenhado em SVG próprio, traduzido do protótipo sobre tokens Nocturne — sem biblioteca de gráficos. É a parte mais delicada da funcionalidade: a linha e a área precisam **quebrar em segmentos** nas lacunas, e em nenhum ponto do caminho um `pctAcerto: null` pode virar zero. A página decide o estado vazio uma única vez, por `possuiHistorico`, e nesse caso nem dispara as outras três requisições.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone, *zoneless*, signals, `input()`/`output()`, `computed` para os caminhos SVG e para a série textual, `LOCALE_ID` pt-BR.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 1 (inegociável):** o SVG vem do protótipo — grid, barras de volume, área, linha, pontos e rótulos —, com a representação da lacuna aprovada na tarefa 1.0. Trocá-lo por biblioteca de gráficos violaria a regra e soma peso ao *bundle*. Só `var(--*)` do Nocturne.
- **Regra 3:** rótulos e textos em pt-BR; percentuais, volumes e datas em formato brasileiro (`3.482`, `68%`, `24/08`).
- **`code-standards.md` §2:** `DesempenhoPage` é dividida em quatro componentes filhos justamente para respeitar o limite de 100 linhas por arquivo.
- **`code-standards.md` §4:** HTTP só em `core/api`; nenhum componente injeta `HttpClient`; estado em `signal`/`computed`; store `providedIn: 'root'`.
- **PRD, acessibilidade:** o gráfico não pode depender só de cor — eixos rotulados, foco visível, valores acessíveis por teclado e alternativa textual equivalente (TU-40).
</rules>

<requirements>
- **RF-15/CA-06** — série de 8 semanas com linha, área e barras de volume; média do período, melhor semana e volume total no rodapé do card.
- **CA-07** — `EvolucaoChart` quebra `linePath` e `areaPath` em segmentos a cada `null` e **não** desenha círculo nessas posições; a barra de volume continua sendo desenhada com altura zero, porque volume zero é um fato, não uma ausência. **Nenhum `?? 0` no caminho** (TU-39). Um único ponto não forma linha: desenha o círculo e o rótulo, sem `path`.
- **CA-08** — barras por disciplina com volume e percentual.
- **CA-09/CA-10** — tabela de pontos fracos com assunto, disciplina, volume, percentual e a ação "Treinar"; quando `itens` vem vazio com `pctMediaUsuario` preenchido, a tabela mostra a explicação ("nenhum assunto atingiu 20 respostas ainda") usando o `minimoRespostas` do envelope, e não um vazio mudo nem o número 20 codificado no frontend.
- **CA-11** — com `possuiHistorico: false`, a página renderiza o `estado-vazio` da tarefa 7.0 e não dispara `/evolucao`, `/disciplinas` nem `/pontos-fracos`.
- Série textual equivalente ao gráfico: oito linhas com semana, volume e percentual, com a lacuna aparecendo como "sem atividade".
</requirements>

## Subtarefas

- [x] 8.1 `core/api/desempenho-api.ts` — cliente dos quatro painéis de `/desempenho`.
- [x] 8.2 `features/stats/desempenho-store.ts` — signals dos quatro painéis, `possuiHistorico`, `segmentosLinha` e `serieTextual` (TU-38…TU-40).
- [x] 8.3 `features/stats/desempenho-page` — orquestra os painéis e decide o estado vazio uma vez (TU-41).
- [x] 8.4 `features/stats/evolucao-chart` — SVG traduzido do protótipo, com segmentos nas lacunas e a tabela textual equivalente.
- [x] 8.5 `features/stats/disciplinas-card` — barras com volume e percentual.
- [x] 8.6 `features/stats/pontos-fracos-table` — tabela com a ação "Treinar" e a explicação da lista vazia.
- [x] 8.7 Formatação pt-BR de percentuais, volumes e datas via `LOCALE_ID` (TU-44).
- [x] 8.8 Registrar `/desempenho` em `app.routes.ts` sob `authGuard`.
- [x] 8.9 Testes TU-38…TU-41 e TU-44.

## Detalhes de implementação

Ver `techspec.md`, §*Arquitetura do sistema* → *Frontend* (linhas de `desempenho-api`, `desempenho-page`, `evolucao-chart`, `disciplinas-card`, `pontos-fracos-table`, `desempenho-store`), §*Principais interfaces* (bloco `DesempenhoStore`), §*Modelos de dados* (`SemanaEvolucao` e a nota "Lacuna, nunca zero"; `PontosFracos` e a nota de amostra pequena), §*Endpoints da API* (a nota de que `/resumo` é a primeira chamada, e o exemplo de ponto único) e §*Principais decisões* (gráfico em SVG próprio, `minimoRespostas` no envelope, `possuiHistorico` decide uma vez). Ordem de construção: passo 8.

## Critérios de aceitação relacionados

- CA-06
- CA-07
- CA-08
- CA-09
- CA-10
- CA-11

## Testes da tarefa

### Testes de unidade

- [x] TU-38 — `DesempenhoStore` quebra a linha do gráfico em segmentos nas lacunas
- [x] TU-39 — `DesempenhoStore` não converte `pctAcerto: null` em zero
- [x] TU-40 — `DesempenhoStore` monta a tabela textual equivalente ao gráfico
- [x] TU-41 — `DesempenhoPage` renderiza o estado vazio com `possuiHistorico: false`
- [x] TU-44 — Formatação pt-BR de percentuais, volumes e datas

## Arquivos relevantes

- `frontend/src/app/core/api/desempenho-api.ts`
- `frontend/src/app/features/stats/desempenho-page/`, `desempenho-store.ts`, `evolucao-chart/`, `disciplinas-card/`, `pontos-fracos-table/`
- `frontend/src/app/shared/ui/estado-vazio/` (reuso da tarefa 7.0)
- `frontend/src/app/app.routes.ts` (modificado)
- `protótipo/Certame.dc.html` — SVG e representação da lacuna
