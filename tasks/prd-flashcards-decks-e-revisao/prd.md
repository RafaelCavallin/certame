# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Flashcards — decks, limites e revisão espaçada (FSRS) · **Fase:** F6 — Flashcards
> Derivado de `PRD.md` (Certame v1.3). Cobre RF-33…RF-36, RN-08, RN-10 e RN-11.

## Visão geral

Criar flashcards é fácil; revê-los na hora certa é o que produz memória de longo prazo. Esta funcionalidade é o motor de retenção do Certame: a tela **Estudar flashcards**, que mostra a frente, revela o verso e pede uma classificação (Errei, Difícil, Bom, Fácil), e o agendamento por **FSRS** (Free Spaced Repetition Scheduler, versão 5), que decide quando cada card volta a aparecer.

O FSRS mantém dois valores de memória por card — estabilidade, em dias, e dificuldade, de 1 a 10 — e calcula o intervalo como o tempo até a probabilidade de lembrança cair à **retenção desejada do deck**. É aí que entram os decks: cada deck é uma unidade de organização *e* de política de agendamento, com seus próprios limites de novos por dia, revisões por dia, intervalo máximo e retenção alvo. A fila diária é montada deck a deck, respeitando esses limites — o que impede o acúmulo que faz o estudante desistir da revisão espaçada.

## Objetivos

- **Retenção ativa:** ≥ 25% dos usuários com pelo menos 20 flashcards revisados por semana (métrica do PRD-mãe).
- **Sessão de revisão fluida:** a classificação de um card e a exibição do próximo ocorrem sem espera perceptível, com atalhos de teclado 1–4 cobrindo 100% das classificações.
- **Agendamento previsível:** 100% dos intervalos calculados respeitam a retenção alvo e o intervalo máximo do deck.
- **Fila sob controle:** ≤ 10% dos usuários com fila vencida acima do dobro do limite diário do deck após 8 semanas de uso, apoiado por padrões conservadores e aviso na tela de decks.
- **Transparência do algoritmo:** cada classificação mostra, antes da escolha, o intervalo que seria agendado — o estudante nunca classifica às cegas.
- **Nenhuma perda de conteúdo:** 0 flashcards apagados por operações de organização de decks.

## Histórias de usuário

- **US1** — Como estudante, quero uma sessão só de flashcards que me mostre a frente, revele o verso e me deixe classificar minha lembrança, para consolidar o que estudei.
- **US2** — Como estudante, quero ver o intervalo que cada classificação agendaria antes de escolher, para entender o efeito da minha resposta.
- **US3** — Como estudante, quero classificar com as teclas 1 a 4 e revelar o verso com a barra de espaço, para revisar rápido sem tirar as mãos do teclado.
- **US4** — Como estudante, quero ver estabilidade, dificuldade e retenção atual do card no verso, para entender por que ele volta com essa frequência.
- **US5** — Como estudante, quero um resumo ao fim da sessão com cards revisados, distribuição por classificação, tempo total e o que vence nos próximos dias, para saber o tamanho da revisão de amanhã.
- **US6** — Como estudante, quero uma tela de decks com total de cards, vencendo hoje e novos, para escolher o que estudar.
- **US7** — Como estudante, quero criar, renomear, descrever e excluir decks, para organizar meu acervo por matéria ou por concurso.
- **US8** — Como estudante, quero configurar novos por dia, revisões por dia, intervalo máximo e retenção desejada por deck, para ajustar o ritmo à minha rotina.
- **US9** — Como estudante, quero estudar um deck específico, para focar em uma matéria.
- **Caso de borda 1** — Excluo um deck com 200 cards: os cards não são apagados, vão para "Sem deck".
- **Caso de borda 2** — Configuro 500 revisões por dia: o valor é ajustado ao limite máximo permitido ao salvar.
- **Caso de borda 3** — Minha fila vencida passa do dobro do limite diário: a tela de decks me avisa.
- **Caso de borda 4** — Altero a retenção desejada de um deck: os próximos intervalos mudam, mas o histórico de revisões e os valores de memória já calculados permanecem.
- **Caso de borda 5** — Não há cards vencendo hoje: a sessão informa que a fila está vazia, em vez de antecipar revisões.

## Principais funcionalidades

### 1. Sessão de revisão de flashcards (RF-33)

Tela dedicada, no mesmo espírito silencioso do modo Foco: um card por vez, verso sob demanda, quatro classificações.

- **RF-33** — Tela **Estudar flashcards**: sessão só de cards — mostra a frente, revela o verso por clique ou barra de espaço, e o usuário classifica em Errei / Difícil / Bom / Fácil. Cada classificação exibe o intervalo que o FSRS agendaria, e o verso mostra estabilidade, dificuldade e retenção atual do card. A fila respeita deck/disciplina escolhidos, limite diário e agendamento (RN-08), com atalhos de teclado 1–4.

### 2. Resumo da sessão (RF-34)

- **RF-34** — Resumo ao fim da sessão com cards revisados, distribuição por classificação, tempo total e quantidade vencendo nos próximos dias. O resultado é gravado no histórico e o tempo é somado ao total de estudo do dia (RF-06).

### 3. Tela de decks (RF-35)

Deck é organização e política ao mesmo tempo: a tela mostra o estado de cada um e leva direto ao estudo.

- **RF-35** — Tela **Decks**, acessível de Flashcards: um card por deck com nome, descrição, total de cards, vencendo hoje, novos, resumo dos limites e ação "Estudar este deck". Permite criar, renomear, descrever e excluir deck. O nome é único por usuário (mínimo de 3 caracteres); renomear move todos os cards junto; excluir não apaga cards — eles vão para "Sem deck" (RN-11).

### 4. Configuração de agendamento por deck (RF-36)

- **RF-36** — Configuração por deck: novos por dia (0–100), revisões por dia (5–400), intervalo máximo em dias (1–3650) e **retenção desejada** (80%, 85%, 90% padrão ou 95%), que é o alvo do FSRS ao calcular cada intervalo. Valores fora do intervalo são ajustados ao limite ao salvar; a configuração passa a valer na próxima montagem de fila, sem alterar o histórico de revisões nem os valores de memória já calculados.

### 5. Agendamento por FSRS (RN-08)

- **RN-08** — O agendamento usa o **FSRS versão 5**. Cada card carrega estabilidade (em dias) e dificuldade (1 a 10), atualizados a cada revisão a partir da nota (Errei, Difícil, Bom, Fácil) e da retrievability no momento da revisão. O intervalo é o tempo em que a retrievability cai até a retenção desejada do deck, limitado pelo intervalo máximo do deck. *Errei* reduz a estabilidade pela fórmula de lapso e coloca o card em *reaprendendo*; não existe fator de facilidade nem progressão fixa de dias. Os pesos do FSRS são parâmetros do sistema, versionados junto ao código.

### 6. Montagem da fila e integridade dos decks (RN-10, RN-11)

- **RN-10** — A fila diária é montada deck a deck e respeita os limites do deck: no máximo *novos por dia* cards em estado novo e *revisões por dia* cards já agendados, por deck. O intervalo calculado nunca excede o intervalo máximo do deck.
- **RN-11** — Excluir um deck é operação de organização, não de conteúdo: os cards são preservados e passam para "Sem deck", que usa os limites padrão (10 novos/dia, 60 revisões/dia, máximo de 180 dias).

## Critérios de aceitação

- **CA-01** (US1, RF-33) — Dada uma fila com cards vencidos, quando inicio a sessão, então vejo apenas a frente do primeiro card, sem o verso e sem as classificações.
- **CA-02** (US1/US3, RF-33) — Dada a frente exibida, quando clico ou pressiono a barra de espaço, então o verso é revelado junto das quatro classificações.
- **CA-03** (US2, RF-33) — Dado o verso revelado, quando olho as classificações, então cada uma exibe o intervalo que o FSRS agendaria caso eu a escolha.
- **CA-04** (US4, RF-33) — Dado o verso revelado, quando o observo, então vejo estabilidade, dificuldade e retenção atual do card.
- **CA-05** (US3, RF-33) — Dadas as teclas 1, 2, 3 e 4, quando as pressiono com o verso revelado, então correspondem respectivamente a Errei, Difícil, Bom e Fácil, e o próximo card é exibido.
- **CA-06** (US9, RF-33, RN-10) — Dado um deck com limite de 10 novos e 60 revisões por dia, quando monto a fila do dia desse deck, então ela contém no máximo 10 cards novos e 60 cards já agendados.
- **CA-07** (RN-08) — Dada uma revisão classificada como Bom, quando ela é registrada, então estabilidade e dificuldade do card são recalculados pelo FSRS e a próxima revisão é agendada para o momento em que a retrievability atinge a retenção alvo do deck.
- **CA-08** (RN-08) — Dada uma revisão classificada como Errei, quando ela é registrada, então a estabilidade é reduzida pela fórmula de lapso e o card passa ao estado *reaprendendo*.
- **CA-09** (RN-08, RN-10) — Dado um intervalo calculado maior que o intervalo máximo do deck, quando o agendamento é gravado, então o intervalo é truncado ao máximo do deck.
- **CA-10** (US5, RF-34) — Dada uma sessão finalizada, quando vejo o resumo, então ele traz cards revisados, distribuição por classificação, tempo total e quantidade vencendo nos próximos dias.
- **CA-11** (US5, RF-34) — Dada uma sessão de flashcards concluída, quando abro o Início, então o tempo dessa sessão está somado ao tempo de estudo do dia (RF-06).
- **CA-12** (US6, RF-35) — Dada a tela de Decks, quando a abro, então cada deck exibe nome, descrição, total de cards, vencendo hoje, novos, resumo dos limites e a ação "Estudar este deck".
- **CA-13** (US7, RF-35) — Dado um nome de deck com menos de 3 caracteres ou já usado por mim, quando tento salvar, então a operação é recusada com mensagem clara.
- **CA-14** (US7, RF-35) — Dado um deck renomeado, quando confirmo, então todos os seus cards permanecem vinculados a ele, sem alteração de agendamento.
- **CA-15** (Borda 1, RN-11) — Dado um deck com cards, quando o excluo, então nenhum card é apagado: todos passam para "Sem deck", que usa os limites padrão de 10 novos/dia, 60 revisões/dia e máximo de 180 dias.
- **CA-16** (Borda 2, RF-36) — Dado o valor 500 em revisões por dia, quando salvo, então ele é ajustado ao limite máximo permitido (400), e o mesmo vale para os demais campos fora do intervalo.
- **CA-17** (US8, RF-36) — Dada uma retenção desejada alterada de 90% para 95%, quando salvo, então a nova configuração vale na próxima montagem de fila, sem alterar o histórico de revisões nem os valores de memória já calculados.
- **CA-18** (Borda 3) — Dada uma fila vencida maior que o dobro do limite diário do deck, quando abro a tela de Decks, então vejo o aviso correspondente naquele deck.
- **CA-19** (Borda 5, RF-33) — Dado que não há cards vencidos nem novos disponíveis dentro dos limites, quando abro a sessão, então vejo um estado vazio informando que a fila do dia terminou, sem antecipar revisões futuras.
- **CA-20** (RN-07) — Dada qualquer operação de fila, revisão ou deck, quando ela é executada, então alcança exclusivamente os cards e decks do usuário autenticado.

## Experiência do usuário

**Perfil e necessidade.** Todos os perfis revisam os próprios cards. A revisão é uma rotina diária curta e repetitiva — cada segundo e cada clique a mais por card se multiplicam por dezenas. A tela de decks, ao contrário, é de configuração ocasional e pede clareza sobre consequências.

**Fluxos principais.**
1. *Revisar tudo* — Flashcards → Estudar → frente → espaço → classificar (1–4) → próximo → resumo.
2. *Revisar um deck* — Flashcards → Decks → "Estudar este deck" → sessão restrita ao deck.
3. *Organizar* — Flashcards → Decks → criar, renomear, descrever ou excluir deck.
4. *Ajustar ritmo* — Decks → deck → configuração de agendamento → salvar (vale na próxima fila).

**UI/UX e acessibilidade.**
- Tradução 1:1 do protótipo aprovado sobre o design system Nocturne; nenhuma cor ou componente novo para os estados do card.
- Sessão operável inteiramente por teclado: espaço revela o verso, 1–4 classificam, com foco visível e os atalhos indicados na própria tela.
- O intervalo previsto por classificação é exibido como texto legível ("1 dia", "9 dias"), em português e sem jargão de algoritmo; estabilidade, dificuldade e retenção aparecem rotuladas e explicadas.
- Avisos de fila acumulada são informativos, não punitivos, e sempre acompanhados da ação de ajustar limites.
- Ações destrutivas em decks pedem confirmação, deixando explícito que os cards serão preservados.
- Contraste mínimo de 4,5:1; textos, números e datas em português do Brasil.

## Restrições técnicas de alto nível

- **Stack não negociável:** Angular 22 (standalone, zoneless, signals) no frontend; Spring Boot 4.1 / Java 25, PostgreSQL 18 e Flyway no backend.
- **Algoritmo obrigatório:** FSRS versão 5, com os pesos padrão publicados, versionados junto ao código. Trocar o algoritmo ou os pesos exige atualizar o PRD-mãe.
- **Log imutável de revisões:** cada revisão registra nota, dias decorridos, estabilidade e dificuldade antes e depois, intervalo resultante e tempo gasto — é esse log que permitirá otimizar os pesos por usuário na v2.
- **Estabilidade do agendamento:** alterações de configuração de deck nunca reescrevem retroativamente o histórico nem os valores de memória já calculados.
- **Privacidade (RN-07):** cards e decks são privados do dono; toda consulta filtra pelo usuário do token, inclusive para super-admin.
- **Integridade (RN-11):** a exclusão de deck e a realocação dos cards para "Sem deck" ocorrem na mesma transação.
- **Desempenho:** a montagem da fila diária e o registro de cada revisão precisam responder sem espera perceptível, com o card seguinte já disponível.
- **Idioma:** português do Brasil.

## Fora do escopo

- Criação e gerenciamento de flashcards (modal na sessão, lista, edição, arquivamento) — em `prd-flashcards-criacao-e-gerenciamento` (RF-28…RF-32).
- Otimização dos pesos do FSRS a partir do histórico do usuário (v2, quando houver volume de revisões suficiente).
- Decks públicos, compartilhamento e assinatura de decks de terceiros (v2+).
- Modo de revisão antecipada ("estudar à frente") e revisão filtrada por sessão personalizada.
- Cards com imagem, áudio ou vídeo; cloze e oclusão de imagem.
- Importação e exportação de decks (Anki, CSV).
- Notificações e lembretes de revisão diária (push fora da v1).
- Sincronização offline da fila.
