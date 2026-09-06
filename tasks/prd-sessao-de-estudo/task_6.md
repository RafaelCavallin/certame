# Tarefa 6.0: Ajustes no protótipo Nocturne (pré-requisito do frontend)

## Visão geral

Corrige as nove divergências entre o protótipo aprovado (`Certame.dc.html`) e o PRD, listadas em "Riscos conhecidos" da TechSpec. É um pré-requisito bloqueante da Tarefa 7.0: pela regra 1 do `AGENTS.md`, o frontend é tradução 1:1 do protótipo aprovado, e nenhum componente Angular novo pode ser escrito antes de o protótipo refletir o que o PRD exige.

<skills>
### Conformidade com skills

- `impeccable` — aplicável apenas na revisão visual das peças alteradas/adicionadas, mantendo os tokens do design system Nocturne; nenhuma cor, fonte, espaçamento, raio ou sombra inventados.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e `folder-structure.md` lidos.

- Regra 1 (`AGENTS.md`) — o frontend é tradução 1:1 do protótipo aprovado; mesma marcação, mesmos tokens `var(--*)` de `styles.css`; sem biblioteca de UI de terceiros; sem elementos que não estejam no protótipo.
- `folder-structure.md` §4 — `protótipo/Certame.dc.html` é a fonte da verdade visual; nada é editado para acomodar o código, é o código que se ajusta ao protótipo.
</rules>

<requirements>
- RF-06, RF-08, RF-09, RF-11
- CA-03, CA-12
</requirements>

## Subtarefas

- [x] 6.1 Adicionar o campo de quantidade da série (10/30/50) ao painel de filtros.
- [x] 6.2 Substituir o botão de marcação genérico por dois controles distintos: favorita e erro recorrente.
- [x] 6.3 Levar a navegação numerada por questão também para o modo Foco (hoje só existe no Contexto), compacta no Foco e como painel lateral no Contexto.
- [x] 6.4 Remover do modo Foco as tags de disciplina, banca, ano e dificuldade (CA-03 proíbe metadados fora de Contexto).
- [x] 6.5 Criar o estado de série vazia (sem questões elegíveis para os filtros aplicados).
- [x] 6.6 No Início: remover "Sequência" e "Ranking" (sem requisito correspondente) e adicionar o card de tempo de estudo (RF-06).
- [x] 6.7 Ajustar o cartão "Retomar" para não trazer um simulado (fora deste PRD); manter só a retomada de sessão de estudo.
- [x] 6.8 Remover "Minhas anotações" do painel de gabarito (sem RF correspondente).
- [x] 6.9 Trocar o gesto "Modo prova" no meio da série por "Iniciar prova", que abre uma sessão nova (o Modo Prova não aceita troca a partir de uma sessão em andamento).

## Detalhes de implementação

Ver TechSpec, tabela em "Riscos conhecidos" (linha "O protótipo diverge do PRD em nove pontos") para a descrição de cada divergência e a mitigação correspondente.

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-03
- CA-12

## Testes da tarefa

Sem testes automatizados nesta tarefa — é ajuste de protótipo estático (`.dc.html`), validado por revisão visual e aprovação de design antes da Tarefa 7.0.

## Arquivos relevantes

- `protótipo/Certame.dc.html`
