# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Cadernos e desempenho · **Fase:** F3 — Progresso
> Derivado de `PRD.md` (Certame v1.3). Cobre RF-13, RF-15 e RF-16.

## Visão geral

Resolver questões sem enxergar evolução é o que faz o candidato desistir. Esta funcionalidade transforma o histórico de respostas — registrado de forma imutável durante as sessões de estudo — em duas coisas acionáveis: **cadernos**, que organizam o que precisa ser revisto, e **desempenho**, que mostra se o estudo está funcionando.

Os cadernos vêm em duas formas: automáticos, montados pelo próprio sistema a partir das marcações do estudante ("Erros recorrentes", "Favoritas"), e manuais, criados por ele. Abrir um caderno é iniciar uma sessão restrita àquelas questões. O desempenho responde a três perguntas, nessa ordem: estou melhorando ao longo das semanas? em quais disciplinas? e qual assunto específico está me segurando?

## Objetivos

- **Evolução mensurável e visível:** ≥ 30% dos usuários com 8 semanas de série de estudo (métrica do PRD-mãe).
- **Revisão dirigida:** ≥ 40% dos estudantes com ao menos um caderno em uso ativo por semana (premissa de meta, a calibrar após o lançamento).
- **Leitura honesta do progresso:** semanas sem atividade nunca são apresentadas como zero — aparecem como lacuna, para não distorcer a curva.
- **Diagnóstico confiável:** um assunto só é apontado como ponto fraco com ao menos 20 respostas, evitando conclusões sobre amostra pequena.
- **Desempenho de tela:** os painéis de evolução, disciplinas e pontos fracos carregam sem espera perceptível, servidos por agregados e não por varredura do histórico.

## Histórias de usuário

- **US1** — Como estudante, quero ver meus cadernos com progresso e descrição, para retomar uma revisão exatamente de onde ela parou.
- **US2** — Como estudante, quero que as questões que marquei como erro recorrente ou favorita formem cadernos automáticos, para não precisar organizá-las manualmente.
- **US3** — Como estudante, quero criar cadernos manuais, para agrupar questões do meu jeito.
- **US4** — Como estudante, quero abrir um caderno e cair direto numa sessão restrita àquelas questões, para revisar sem montar filtro.
- **US5** — Como estudante, quero ver a evolução do meu percentual de acerto por semana junto do volume de questões, para saber se estou melhorando ou só resolvendo mais.
- **US6** — Como estudante, quero ver meu desempenho por disciplina, para distribuir melhor meu tempo de estudo.
- **US7** — Como estudante, quero uma lista dos meus pontos fracos por assunto, com volume e percentual de acerto, para atacar primeiro o que mais me prejudica.
- **Caso de borda 1** — Passo duas semanas sem estudar: o gráfico mostra a interrupção como lacuna, não como queda para zero.
- **Caso de borda 2** — Sou usuário novo, sem histórico: as telas mostram estado vazio explicativo, não gráficos zerados.
- **Caso de borda 3** — Resolvo corretamente uma questão marcada como erro recorrente: a marcação continua sob meu controle, não é removida sozinha.

## Principais funcionalidades

### 1. Cadernos (RF-13)

Unidade de organização do estudo dirigido. Automáticos e manuais convivem na mesma lista, com progresso visível.

- **RF-13** — Cadernos com progresso, descrição e data de atualização, automáticos ("Erros recorrentes", "Favoritas") e manuais. Abrir um caderno inicia uma sessão de estudo restrita às suas questões.
- Os cadernos automáticos são alimentados pelas marcações feitas durante as sessões (`prd-sessao-de-estudo`, RF-11).

### 2. Evolução semanal (RF-15)

O painel que responde "estou melhorando?" — percentual de acerto por semana em linha com área, sobre o volume semanal de questões.

- **RF-15** — Desempenho com evolução do percentual de acerto por semana (linha + área) e volume semanal, em série de até 8 semanas. Períodos sem atividade aparecem como lacuna, nunca como zero.

### 3. Desempenho por disciplina e pontos fracos (RF-16)

- **RF-16** — Desempenho por disciplina e lista de pontos fracos, com assunto, disciplina, volume de respostas e percentual de acerto. Ponto fraco é o assunto com ao menos 20 respostas e acerto abaixo da média do próprio usuário.

## Critérios de aceitação

- **CA-01** (US1, RF-13) — Dada a tela de cadernos, quando a abro, então cada caderno exibe nome, descrição, progresso e data da última atualização.
- **CA-02** (US2, RF-13) — Dada uma questão marcada como erro recorrente durante uma sessão, quando abro os cadernos, então ela consta no caderno automático "Erros recorrentes".
- **CA-03** (US2, RF-13) — Dada uma questão favoritada, quando abro os cadernos, então ela consta no caderno automático "Favoritas".
- **CA-04** (US3, RF-13) — Dado um caderno manual novo com nome e descrição, quando o crio e adiciono questões, então ele aparece na lista com progresso zerado.
- **CA-05** (US4, RF-13) — Dado um caderno com 40 questões, quando o abro, então inicia uma sessão de estudo restrita a essas 40 questões, no modo Foco, e o progresso do caderno é atualizado ao final.
- **CA-06** (US5, RF-15) — Dado um histórico com atividade em várias semanas, quando abro o desempenho, então vejo a série de até 8 semanas com percentual de acerto (linha + área) e o volume semanal correspondente.
- **CA-07** (Borda 1, RF-15) — Dada uma semana sem nenhuma resposta registrada, quando o gráfico é renderizado, então essa semana aparece como lacuna na série, e não como ponto de valor zero.
- **CA-08** (US6, RF-16) — Dado meu histórico, quando abro o desempenho por disciplina, então cada disciplina exibe volume de respostas e percentual de acerto.
- **CA-09** (US7, RF-16) — Dado um assunto com 25 respostas e acerto abaixo da minha média geral, quando abro os pontos fracos, então ele aparece na lista com assunto, disciplina, volume e percentual.
- **CA-10** (US7, RF-16) — Dado um assunto com 19 respostas e acerto baixíssimo, quando abro os pontos fracos, então ele não é listado, por não atingir o mínimo de 20 respostas.
- **CA-11** (Borda 2) — Dado um usuário sem histórico, quando abre desempenho ou cadernos, então vê estados vazios com orientação sobre como começar, sem gráficos ou métricas fictícias.
- **CA-12** (RF-15, RF-16) — Dadas respostas de tentativas de Modo Prova abandonadas sem finalização, quando os painéis são calculados, então essas respostas não entram em nenhuma métrica (RN-04).

## Experiência do usuário

**Perfil e necessidade.** O estudante consulta estas telas entre sessões, em momentos de decisão: o que revisar hoje e onde investir tempo. Elas precisam ser rápidas de ler e levar de volta ao estudo em um clique.

**Fluxos principais.**
1. *Revisar erros* — Cadernos → "Erros recorrentes" → sessão restrita → resolver.
2. *Organizar* — Cadernos → novo caderno → adicionar questões → estudar.
3. *Avaliar evolução* — Desempenho → curva semanal → desempenho por disciplina → pontos fracos.
4. *Agir sobre o ponto fraco* — Pontos fracos → escolher assunto → iniciar sessão daquele assunto.

**UI/UX e acessibilidade.**
- Tradução 1:1 do protótipo aprovado sobre o design system Nocturne — as cores dos gráficos vêm dos tokens existentes, sem paleta nova.
- Gráficos legíveis sem depender só de cor: eixos rotulados, valores acessíveis por teclado e alternativa textual equivalente (tabela ou resumo) para leitores de tela.
- Contraste mínimo de 4,5:1 em texto corrido; foco visível em toda a navegação por teclado.
- Números e datas em formato brasileiro; textos em português do Brasil.
- Estados vazios explicativos em vez de painéis zerados.

## Restrições técnicas de alto nível

- **Stack não negociável:** Angular 22 no frontend; Spring Boot 4.1 / Java 25 e PostgreSQL 18 no backend.
- **Fonte de verdade:** o histórico imutável de respostas (RN-02); os painéis derivam dele e nunca o alteram.
- **Custo de estatísticas:** agregados materializados por job, conforme decisão do PRD-mãe — cálculo em tempo real sobre o histórico completo não é aceitável.
- **Escopo de dados:** todas as consultas são restritas ao usuário autenticado, validadas no servidor.
- **Privacidade (LGPD):** métricas de desempenho são dados pessoais; exportação e exclusão a pedido do titular.
- **Idioma:** português do Brasil, incluindo formatação de datas e percentuais.

## Fora do escopo

- Exportação de relatórios em PDF ou planilha (v2+).
- Comparação de desempenho com outros usuários, ranking geral e metas gamificadas.
- Cadernos compartilhados entre usuários.
- Recomendação automática do que estudar a seguir por IA.
- Desempenho de flashcards e retenção por revisão espaçada — em `prd-flashcards-decks-e-revisao` (RF-34).
- Resultados e ranking de simulados — em `prd-simulados` (RF-14).
- Notificações ou lembretes de estudo (push fora da v1).
