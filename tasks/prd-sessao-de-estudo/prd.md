# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Início e sessão de estudo (Foco, Contexto e Modo Prova) · **Fase:** F2 — Estudo
> Derivado de `PRD.md` (Certame v1.3). Cobre RF-06…RF-12 e RN-01…RN-04.

## Visão geral

Esta é a funcionalidade central do Certame e a razão de o produto existir. Candidatos a concursos estudam por repetição de questões, mas as ferramentas disponíveis os afogam em opções — filtros, estatísticas, comentários e propaganda competem com a única tarefa que importa: ler o enunciado e decidir. O resultado é sessão curta, dispersão alta e nenhuma leitura confiável de evolução.

A resposta do Certame é uma tela de resolução deliberadamente silenciosa. O modo **Foco** é o padrão de toda sessão e mostra apenas enunciado e alternativas; o modo **Contexto** revela metadados, estatísticas e comentário quando o candidato pede; o **Modo Prova** simula a prova real, sem feedback até a finalização. Em torno disso ficam o Início, que resume o dia e devolve o candidato ao ponto em que parou, o cronômetro contínuo e as marcações que alimentam os cadernos automáticos.

## Objetivos

- **Sessão mais longa e contínua:** mediana de ≥ 25 questões por sessão (métrica-chave do PRD-mãe).
- **Foco preservado:** o modo Foco é o estado inicial de 100% das sessões; nenhum elemento além de enunciado, alternativas e cronômetro compete pela atenção.
- **Fluidez:** próxima questão disponível em até 300 ms (p95) — a espera nunca deve interromper o ritmo de resolução.
- **Continuidade:** ≥ 60% das sessões iniciadas pelo Início usam "continuar de onde parou" ou terminam sem abandono no meio da série (premissa de meta, a calibrar após o lançamento).
- **Registro fiel:** 100% das respostas gravadas com questão, alternativa marcada, acerto, tempo gasto e contexto de origem.

## Histórias de usuário

- **US1** — Como estudante, quero abrir o Início e ver quantas questões resolvi hoje, meu percentual de acerto e meu tempo de estudo, para saber onde estou antes de continuar.
- **US2** — Como estudante, quero continuar de onde parei com um clique, para não gastar decisão escolhendo o que estudar.
- **US3** — Como estudante, quero resolver questões vendo só o enunciado e as alternativas, para não me dispersar.
- **US4** — Como estudante, quero, quando quiser, abrir o contexto da questão — disciplina, banca, ano, estatísticas e comentário — para entender por que errei.
- **US5** — Como estudante, quero fazer uma série completa sem gabarito, como numa prova real, e só depois ver a correção.
- **US6** — Como estudante, quero ver ao fim do Modo Prova quantas acertei, quantas errei, quantas deixei em branco e o tempo total, com revisão questão a questão.
- **US7** — Como estudante, quero marcar uma questão como favorita ou como erro recorrente, para revê-la depois nos meus cadernos.
- **US8** — Como estudante, quero navegar pelo número da questão dentro da série, para voltar a uma que quero reler.
- **US9** — Como estudante, quero um cronômetro visível durante a sessão, para me habituar ao ritmo da prova.
- **Caso de borda 1** — Fecho o navegador no meio de um Modo Prova sem finalizar: a tentativa é descartada e nada entra no meu histórico.
- **Caso de borda 2** — Troco de modo no meio da série: continuo na mesma questão, com a alternativa que já havia marcado.

## Principais funcionalidades

### 1. Início do estudante (RF-06)

Painel de entrada, curto e factual: o que já foi feito hoje e um único caminho para continuar.

- **RF-06** — Início com resumo do dia: questões resolvidas, percentual de acerto, tempo de estudo e ação "continuar de onde parou". Os números refletem a atividade real do usuário no período exibido, incluindo o tempo das sessões de flashcards.

### 2. Resolução de questões (RF-07)

- **RF-07** — Resolução de questões de múltipla escolha (A–E) e certo/errado, com uma alternativa selecionável por vez. Fora do Modo Prova, responder revela gabarito, comentário e estatísticas da questão.
- **RN-01** — Somente questões com status *Publicada* entram nas sessões.
- **RN-03** — As estatísticas exibidas excluem as respostas do próprio autor da questão.

### 3. Os três modos (RF-08)

O modo é uma propriedade da sessão, alternável a qualquer momento sem perder o lugar.

- **RF-08** — Três modos: **Foco** (padrão — só enunciado e alternativas), **Contexto** (metadados, estatísticas e comentário) e **Modo Prova**. A troca de modo preserva a questão atual, e Foco é o modo inicial de toda sessão.

### 4. Modo Prova e correção (RF-09, RF-10)

- **RF-09** — No Modo Prova o estudante responde a série completa sem gabarito, com finalização explícita. Nenhum feedback aparece antes de finalizar, e a navegação entre questões já respondidas é livre.
- **RF-10** — Correção do Modo Prova com percentual de acerto, acertos, erros, não respondidas, tempo total e revisão questão a questão, com as ações "refazer" e "voltar ao início". O resultado é gravado no histórico.
- **RN-04** — No Modo Prova, as respostas só são consideradas após a finalização; abandono sem finalizar descarta a tentativa.

### 5. Marcações e navegação na série (RF-11)

- **RF-11** — Marcar questão como favorita ou erro recorrente e navegar por número dentro da série. A marcação persiste entre sessões e alimenta os cadernos automáticos (`prd-cadernos-e-desempenho`, RF-13).

### 6. Cronômetro de sessão (RF-12)

- **RF-12** — Cronômetro de sessão visível e contínuo enquanto a sessão está ativa; o tempo é somado ao total de estudo do dia exibido no Início.

### 7. Registro imutável das respostas (RN-02)

- **RN-02** — Cada resposta é imutável e registra questão, alternativa marcada, acerto, tempo gasto e contexto (livre, caderno ou simulado).

## Critérios de aceitação

- **CA-01** (US1, RF-06) — Dado um estudante com atividade no dia, quando abre o Início, então vê questões resolvidas, percentual de acerto, tempo de estudo do dia e a ação de continuar, todos coerentes com o histórico real.
- **CA-02** (US2, RF-06) — Dada uma sessão anterior interrompida, quando aciono "continuar de onde parou", então retomo a mesma série na próxima questão não respondida.
- **CA-03** (US3, RF-08) — Dado o início de qualquer sessão, quando a primeira questão é exibida, então o modo ativo é Foco e a tela mostra apenas enunciado, alternativas, cronômetro e navegação — sem metadados, estatísticas ou comentário.
- **CA-04** (US3, RF-07) — Dada uma questão de múltipla escolha, quando seleciono uma alternativa e depois outra, então apenas a última permanece marcada.
- **CA-05** (US4, RF-07) — Dada uma questão respondida fora do Modo Prova, quando confirmo a resposta, então vejo gabarito, comentário e percentual de acerto da questão.
- **CA-06** (Borda 2, RF-08) — Dada a questão 7 de uma série com uma alternativa marcada, quando alterno de Foco para Contexto e volto, então continuo na questão 7 com a mesma alternativa marcada e o cronômetro sem interrupção.
- **CA-07** (US5, RF-09) — Dado o Modo Prova em andamento, quando respondo qualquer questão, então nenhum gabarito, comentário ou estatística é exibido antes da finalização.
- **CA-08** (US5, RF-09) — Dado o Modo Prova em andamento, quando navego para uma questão já respondida, então posso alterar a resposta livremente até finalizar.
- **CA-09** (US6, RF-10) — Dado um Modo Prova finalizado, quando vejo a correção, então ela apresenta percentual de acerto, acertos, erros, não respondidas e tempo total, com revisão questão a questão e as ações "refazer" e "voltar ao início".
- **CA-10** (Borda 1, RN-04) — Dado um Modo Prova abandonado sem finalização, quando consulto o histórico e o desempenho, então nenhuma resposta daquela tentativa aparece.
- **CA-11** (US7, RF-11) — Dada uma questão marcada como favorita ou erro recorrente, quando encerro a sessão e volto depois, então a marcação continua ativa e a questão aparece no caderno automático correspondente.
- **CA-12** (US8, RF-11) — Dada uma série de 30 questões, quando escolho o número 12 na navegação, então vou direto a essa questão preservando as respostas já dadas.
- **CA-13** (US9, RF-12) — Dada uma sessão ativa, quando o cronômetro corre, então ele permanece visível em todos os modos e seu tempo é somado ao total de estudo do dia.
- **CA-14** (RF-07) — Dada uma sessão em andamento, quando avanço para a próxima questão, então ela é exibida em até 300 ms (p95).
- **CA-15** (RN-02) — Dada uma resposta já registrada, quando qualquer operação tenta alterá-la, então a alteração é recusada — o registro guarda questão, alternativa marcada, acerto, tempo e contexto de origem.
- **CA-16** (RN-01) — Dada uma sessão iniciada por qualquer filtro, quando a série é montada, então nenhuma questão em Rascunho ou Em revisão é incluída.

## Experiência do usuário

**Perfil e necessidade.** O estudante em sessão está em estado de concentração; qualquer elemento novo na tela custa foco. Todos os perfis (estudante, professor, administrador e super-admin) usam a mesma tela de resolução.

**Fluxos principais.**
1. *Estudo livre* — Início → continuar / nova sessão → série em Foco → responder → próxima.
2. *Entender o erro* — questão respondida → alternar para Contexto → ler comentário → voltar a Foco.
3. *Simular prova* — nova sessão → Modo Prova → responder a série → Finalizar → correção → refazer ou voltar ao início.
4. *Marcar para depois* — durante a questão → favoritar ou marcar erro recorrente → o caderno automático recebe a questão.

**UI/UX e acessibilidade.**
- Tradução 1:1 do protótipo aprovado sobre o design system Nocturne; sem biblioteca de UI de terceiros e sem elementos que não estejam no protótipo.
- O modo Foco é o padrão absoluto: nada além de enunciado, alternativas, cronômetro e navegação da série.
- Operação completa por teclado — seleção de alternativa, confirmação, avanço, retorno e troca de modo — com foco visível e contraste mínimo de 4,5:1.
- Transições de questão sem salto de layout; o cronômetro nunca reinicia por troca de modo ou de questão.
- Português do Brasil em toda a interface, incluindo mensagens e formatos de data e tempo.

## Restrições técnicas de alto nível

- **Stack não negociável:** Angular 22 (standalone, zoneless, signals) no frontend; Spring Boot 4.1 / Java 25 e PostgreSQL 18 no backend.
- **Desempenho:** próxima questão em até 300 ms (p95), o que implica série pré-carregada e estatísticas servidas por agregados, não por contagem em tempo real.
- **Estado da sessão:** a sessão em memória (série, índice atual e cronômetro) precisa sobreviver a interações da própria tela, incluindo a abertura do modal de criação de flashcard (`prd-flashcards-criacao-e-gerenciamento`, RF-28).
- **Autorização no servidor:** o conteúdo da sessão é sempre filtrado no servidor por status de publicação e pelo usuário autenticado.
- **Imutabilidade:** o registro de respostas é append-only (RN-02) e é a fonte de verdade para desempenho, cadernos e estatísticas de questão.
- **Privacidade (LGPD):** histórico de respostas é dado pessoal; exportação e exclusão a pedido do titular, com anonimização quando o usuário é excluído.
- **Idioma:** português do Brasil.

## Fora do escopo

- Cadernos, desempenho semanal e pontos fracos — em `prd-cadernos-e-desempenho` (RF-13, RF-15, RF-16).
- Simulados agendados e seu ranking — em `prd-simulados` (RF-14, RF-21).
- Criação e revisão de flashcards — em `prd-flashcards-criacao-e-gerenciamento` e `prd-flashcards-decks-e-revisao`.
- Comentários, dúvidas ou fórum entre usuários na questão.
- Questões discursivas, videoaulas e resolução em vídeo.
- Layout responsivo para celular (roadmap imediato pós-v1, ainda não especificado).
- Modo offline e sincronização posterior.
