# Tarefa 1.0: Fechar as sete lacunas do protótipo Nocturne

## Visão geral

O protótipo aprovado já traz as telas `CADERNOS` e `DESEMPENHO`, inclusive o SVG completo da curva semanal, mas sete peças exigidas pelo PRD não existem nele. Como o frontend é tradução 1:1 do protótipo (regra 1 do `AGENTS.md`), nenhuma dessas peças pode nascer direto no Angular: elas entram primeiro em `protótipo/Certame.dc.html`, com componentes e tokens Nocturne já existentes. Esta tarefa é design, não código de aplicação, e libera as tarefas 7.0 e 8.0.

<skills>
### Conformidade com skills

- `impeccable` — **não aplicável** como redesenho: a TechSpec (§*Conformidade com skills*) registra que o layout está congelado pela regra 1 do `AGENTS.md`. As sete lacunas são acréscimos com componentes Nocturne existentes, e a acessibilidade exigida pelo PRD é coberta por TU-40 e E2E-13.
- Nenhuma skill de implementação de backend ou frontend se aplica a esta tarefa.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/folder-structure.md` e `.agents/rules/tests.md` lidos na íntegra.

- **Regra 1 (inegociável):** o protótipo é a fonte da verdade visual. Nada de cor, fonte, espaçamento, raio ou sombra inventados — só `var(--*)` do design system Nocturne. Nenhuma biblioteca de UI de terceiros, nem para gráfico.
- **Regra 3:** todos os rótulos, títulos e textos de orientação em português do Brasil, com números, percentuais e datas em formato brasileiro.
- O arquivo alterado é `protótipo/Certame.dc.html`, conforme `folder-structure.md`; nenhum arquivo de `frontend/` é tocado nesta tarefa.
- Sem desvios.
</rules>

<requirements>
- **RF-13** — o card de caderno precisa de diálogo de criação, menu de renomear/excluir e seletor de questões.
- **RF-15** — a curva semanal precisa representar a lacuna (a linha do protótipo hoje é contínua).
- **RF-16** — o painel de disciplinas precisa exibir o volume, além do percentual.
- **PRD, UI/UX e acessibilidade** — estados vazios explicativos em vez de painéis zerados; gráfico legível sem depender só de cor, com eixos rotulados e alternativa textual equivalente; contraste mínimo de 4,5:1; foco visível.
- **TechSpec, *Riscos conhecidos*** — o card do protótipo usa `c.icon`/`c.iconBg` sem lugar para escolher: automáticos recebem ícone fixo (`ph-warning-circle`, `ph-star`) e manuais um ícone único (`ph-notebook`) com o papel neutro do Nocturne. Seletor de ícone é v2.
</requirements>

## Subtarefas

- [x] 1.1 Diálogo "Novo caderno" (nome e descrição) sobre o componente de diálogo do Nocturne, com estado de erro no campo `nome` para `NOME_CADERNO_EM_USO` (CA-04).
- [x] 1.2 Menu do card de caderno a partir do ícone `dots-three` existente, com as ações renomear e excluir, ausentes no card automático (`editavel: false`).
- [x] 1.3 Seletor de questões do caderno, sobre a listagem de questões já desenhada, com seleção múltipla e ação de adicionar (CA-04).
- [x] 1.4 Estado vazio da tela de cadernos: título, orientação de como começar e ação (CA-11).
- [x] 1.5 Estado vazio da tela de desempenho, no lugar dos quatro painéis (CA-11).
- [x] 1.6 Representação da lacuna na curva semanal: linha e área interrompidas, sem ponto na posição, com a barra de volume mantida em altura zero e o eixo preservando as 8 semanas (CA-07).
- [x] 1.7 Volume de respostas na barra por disciplina, ao lado do percentual (CA-08).
- [x] 1.8 Definir os ícones fixos dos cadernos (`ph-warning-circle`, `ph-star`, `ph-notebook`) e a alternativa textual equivalente ao gráfico (tabela ou resumo) exigida pelo PRD.
- [x] 1.9 Conferir contraste ≥ 4,5:1 no texto corrido e foco visível em todos os elementos novos; submeter à aprovação visual antes de liberar as tarefas 7.0 e 8.0.

## Detalhes de implementação

Ver `techspec.md`, §*Sequenciamento do desenvolvimento* (passo 1 da ordem de construção), §*Riscos conhecidos* (a linha das sete lacunas e a do ícone do card) e §*Arquitetura do sistema* → *Frontend*, linha `protótipo/Certame.dc.html`. A semântica da lacuna está em §*Modelos de dados* → `SemanaEvolucao`.

## Critérios de aceitação relacionados

- CA-04
- CA-07
- CA-08
- CA-11

## Testes da tarefa

Tarefa de design, sem teste automatizado próprio. Ela é pré-requisito de **E2E-04**, **E2E-07**, **E2E-10** e **E2E-13**, cujos seletores saem desta marcação; esses casos são executados na tarefa 10.0.

## Arquivos relevantes

- `protótipo/Certame.dc.html` — as sete lacunas.
- `protótipo/_ds/` — tokens e componentes Nocturne existentes, fonte dos valores.
- `frontend/src/styles.css` — referência dos `var(--*)` que a tradução usará nas tarefas 7.0 e 8.0.
