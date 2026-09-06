# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Flashcards — criação no fluxo da questão e gerenciamento · **Fase:** F6 — Flashcards
> Derivado de `PRD.md` (Certame v1.3). Cobre RF-28…RF-32, RN-07 e RN-09.

## Visão geral

O melhor momento para criar um flashcard é o instante imediatamente após errar uma questão — quando o candidato acabou de descobrir exatamente o que não sabe. Se criar o card custar sair da sessão, ele não será criado: a sessão de estudo é justamente o que o Certame protege com o modo Foco. Por isso a criação de flashcard nasce como um **diálogo sobre a questão**, nunca como uma rota: a alternativa marcada, o índice da série e o cronômetro seguem intactos ao abrir e ao fechar o modal.

A funcionalidade tem duas metades. A primeira é a criação dentro da sessão: um botão na barra de ações da questão, disponível nos três modos, abrindo um modal enxuto com disciplina, assunto e questão de origem já preenchidos. A segunda é o gerenciamento: um menu **Flashcards** com a lista dos cards do próprio usuário, busca, filtros, edição, arquivamento, exclusão lógica e criação avulsa fora da sessão. Todo card é privado do dono — nem o super-admin lê o conteúdo de cards de terceiros.

## Objetivos

- **Retenção ativa por revisão espaçada:** habilitar a métrica do PRD-mãe de ≥ 25% dos usuários com pelo menos 20 flashcards revisados por semana, fornecendo o acervo pessoal de cards.
- **Criação sem custo de foco:** 0 ocorrências de perda de estado da sessão (alternativa marcada, índice da série, cronômetro) ao abrir ou fechar o modal.
- **Captura no momento certo:** ≥ 60% dos flashcards criados na v1 nascem dentro de uma sessão de estudo, com questão de origem registrada (premissa de meta, a calibrar após o lançamento).
- **Fluxo contínuo:** criar um card e voltar ao enunciado em até 3 interações — abrir, preencher, salvar.
- **Privacidade absoluta:** 0 acessos de leitura ou escrita a cards de terceiros, por qualquer perfil.
- **Sessão preservada:** taxa de abandono de sessão após criação de flashcard igual ou inferior à das sessões sem criação (risco monitorado do PRD-mãe).

## Histórias de usuário

- **US1** — Como estudante, quero criar um flashcard direto da questão que acabei de resolver, para registrar o que não sabia sem perder o ritmo.
- **US2** — Como estudante, quero que disciplina, assunto e questão de origem venham preenchidos, para digitar apenas frente e verso.
- **US3** — Como estudante, quero "Salvar e criar outro", para extrair vários cards da mesma questão sem reabrir o modal.
- **US4** — Como estudante, quero fechar o modal com `Esc` ou Cancelar e voltar exatamente ao ponto em que estava na questão.
- **US5** — Como estudante, quero ver todos os meus flashcards em uma lista com frente resumida, deck, disciplina/assunto, estado e próxima revisão, para saber o que tenho e o que está por vir.
- **US6** — Como estudante, quero buscar por texto e filtrar por deck, disciplina e estado, para achar um card específico entre centenas.
- **US7** — Como estudante, quero editar, arquivar e excluir cards, para manter o acervo limpo sem perder meu histórico de revisões.
- **US8** — Como estudante, quero criar um flashcard avulso fora da sessão, para registrar algo que li em outro lugar.
- **US9** — Como estudante, quero abrir a questão que originou um card, para revisar o contexto completo.
- **Caso de borda 1** — Cancelo o modal com campos preenchidos: o sistema pede confirmação antes de descartar.
- **Caso de borda 2** — Tento salvar com frente ou verso com menos de 3 caracteres: a validação bloqueia e indica o campo.
- **Caso de borda 3** — Excluo um card já revisado: a exclusão é lógica e o histórico de revisões permanece.

## Principais funcionalidades

### 1. Botão "Novo flashcard" na sessão (RF-28)

Ponto de captura no fluxo de resolução, presente nos três modos — inclusive no Modo Prova, onde o candidato marca o que quer estudar depois.

- **RF-28** — Botão "Novo flashcard" na barra de ações da questão, disponível nos três modos, abrindo um modal de criação sobre a questão. O modal não navega nem recarrega a tela: alternativa marcada, índice da série e cronômetro permanecem intactos ao abrir e ao fechar.

### 2. Modal de criação com contexto pré-preenchido (RF-29)

Enxuto por decisão de produto: quanto menos campos, menor o custo de foco.

- **RF-29** — Modal com frente, verso, deck, disciplina, assunto e tags. Disciplina, assunto e questão de origem vêm pré-preenchidos a partir da questão em tela. Frente e verso são obrigatórios (mínimo de 3 caracteres); salvar fecha o modal, exibe toast de confirmação e devolve o foco ao enunciado.

### 3. Ações do modal (RF-30)

- **RF-30** — Ações **Salvar e fechar**, **Salvar e criar outro** (mantém o modal aberto e limpo) e **Cancelar**. Cancelar com campos preenchidos pede confirmação; `Esc` equivale a cancelar; o contador de flashcards criados na sessão fica visível no botão.

### 4. Lista de flashcards (RF-31)

- **RF-31** — Menu **Flashcards** na navegação do estudante, com a lista dos cards do próprio usuário: frente resumida, deck, disciplina/assunto, estado (novo, aprendendo, revisão) e próxima revisão. Tabela paginada com busca por texto e filtros por deck, disciplina e estado; ordenação por criação e por próxima revisão.

### 5. Edição, organização e exclusão (RF-32)

- **RF-32** — Editar, arquivar e excluir flashcard; organizar cards em decks manuais; criar flashcard avulso fora da sessão de estudo. Card criado a partir de uma questão mantém o link para ela ("ver questão de origem"); a exclusão é lógica e preserva o histórico de revisões.

### 6. Privacidade e rastreabilidade de origem (RN-07, RN-09)

- **RN-07** — O flashcard é privado do usuário que o criou: nenhum outro perfil, incluindo o super-admin, lê ou edita o conteúdo de cards de terceiros.
- **RN-09** — Flashcard criado durante uma sessão de estudo registra a questão e a sessão de origem, para rastrear o que a resolução de questões gerou de revisão.

## Critérios de aceitação

- **CA-01** (US1, RF-28) — Dada uma questão em qualquer um dos três modos, quando aciono "Novo flashcard", então o modal abre sobre a questão, sem navegação nem recarregamento da tela.
- **CA-02** (US4, RF-28) — Dada a questão 12 da série, com uma alternativa marcada e o cronômetro em andamento, quando abro e fecho o modal, então continuo na questão 12, com a mesma alternativa marcada e o cronômetro sem interrupção nem reinício.
- **CA-03** (US2, RF-29) — Dado o modal aberto a partir de uma questão, quando ele é exibido, então disciplina e assunto vêm preenchidos com os da questão e a questão de origem está registrada.
- **CA-04** (Borda 2, RF-29) — Dada uma frente ou um verso com menos de 3 caracteres, quando tento salvar, então o salvamento é bloqueado com validação inline no campo correspondente.
- **CA-05** (US1, RF-29) — Dado frente e verso válidos, quando aciono "Salvar e fechar", então o card é criado, o modal fecha, um toast de confirmação aparece e o foco volta ao enunciado da questão.
- **CA-06** (US3, RF-30) — Dado frente e verso válidos, quando aciono "Salvar e criar outro", então o card é criado, o modal permanece aberto com os campos de frente e verso limpos e o contexto (deck, disciplina, assunto) preservado.
- **CA-07** (Borda 1, RF-30) — Dado o modal com campos preenchidos, quando aciono Cancelar ou pressiono `Esc`, então uma confirmação é exigida antes de descartar o conteúdo.
- **CA-08** (RF-30) — Dada uma sessão em que criei 3 flashcards, quando olho o botão "Novo flashcard", então ele exibe o contador de cards criados na sessão.
- **CA-09** (US5, RF-31) — Dada a tela de Flashcards, quando a abro, então vejo a lista paginada dos meus cards com frente resumida, deck, disciplina/assunto, estado e próxima revisão.
- **CA-10** (US6, RF-31) — Dados busca por texto e filtros de deck, disciplina e estado combinados, quando os aplico, então a lista exibe apenas os cards correspondentes e permite ordenar por criação e por próxima revisão.
- **CA-11** (US7, RF-32) — Dado um card arquivado, quando a fila de revisão é montada, então ele não aparece, e continua acessível e restaurável pela lista.
- **CA-12** (Borda 3, RF-32) — Dado um card com histórico de revisões, quando o excluo, então ele deixa de aparecer na lista e na fila, e o histórico de revisões é preservado.
- **CA-13** (US9, RF-32, RN-09) — Dado um card criado a partir de uma questão, quando aciono "ver questão de origem", então a questão correspondente é aberta, e o registro guarda a questão e a sessão de origem.
- **CA-14** (US8, RF-32) — Dado o menu Flashcards, quando crio um card avulso, então ele é salvo sem questão de origem e com deck, disciplina e assunto informados por mim.
- **CA-15** (RN-07) — Dado um card de outro usuário, quando qualquer perfil — incluindo super-admin — tenta lê-lo ou editá-lo, então a requisição é recusada no servidor, sem revelar a existência do card.
- **CA-16** (US7, RF-32) — Dado um conjunto de cards selecionados, quando os movo para outro deck manual, então todos passam ao deck escolhido, sem alterar seus dados de agendamento.

## Experiência do usuário

**Perfil e necessidade.** Todos os perfis criam e gerenciam os próprios flashcards. O estudante em sessão está concentrado — o modal é uma interrupção que precisa durar segundos. Na tela de gerenciamento ele está em modo de organização, e aí a densidade de informação ajuda.

**Fluxos principais.**
1. *Criar na sessão* — questão → "Novo flashcard" → frente e verso → Salvar e fechar → volta ao enunciado.
2. *Extrair vários* — questão → "Novo flashcard" → Salvar e criar outro (repete) → Cancelar → volta ao enunciado.
3. *Gerenciar* — Flashcards → busca e filtros → editar, arquivar, excluir ou mover de deck.
4. *Criar avulso* — Flashcards → novo card → preencher tudo → salvar.
5. *Voltar à origem* — Flashcards → card → "ver questão de origem".

**UI/UX e acessibilidade.**
- Tradução 1:1 do protótipo aprovado sobre o design system Nocturne; o modal usa os componentes de diálogo já existentes, sem biblioteca de terceiros.
- O modal captura o foco ao abrir (foco inicial no campo Frente) e o devolve ao enunciado ao fechar; `Esc` cancela; `Tab` circula apenas dentro do diálogo.
- Confirmação obrigatória antes de descartar conteúdo digitado; toast de confirmação após salvar, sem bloquear a tela.
- Estados do card (novo, aprendendo, revisão) por rótulo textual além da cor; contraste mínimo de 4,5:1.
- Português do Brasil em toda a interface, incluindo datas de próxima revisão.

## Restrições técnicas de alto nível

- **Stack não negociável:** Angular 22 (standalone, zoneless, signals) no frontend; Spring Boot 4.1 / Java 25, PostgreSQL 18 e Flyway no backend.
- **Diálogo, nunca rota:** a criação na sessão não pode alterar a URL nem desmontar o componente da sessão — a sessão em memória, o índice da série e o cronômetro sobrevivem intactos.
- **Privacidade no servidor (RN-07):** toda consulta filtra pelo usuário do token e responde como inexistente (404) para card de terceiro — nunca 403, que confirmaria a existência.
- **Exclusão lógica:** cards excluídos preservam o histórico de revisões, que é o log usado para calibrar o agendamento no futuro.
- **Integração com a taxonomia:** disciplina e assunto vêm da árvore de matérias (`prd-banco-de-questoes`, RF-20); a questão de origem referencia o acervo.
- **Busca textual em português** sobre frente e verso, com desempenho compatível com listas paginadas.
- **Privacidade (LGPD):** conteúdo de flashcard é dado pessoal do titular; exportação e exclusão a pedido.
- **Idioma:** português do Brasil.

## Fora do escopo

- Decks, limites de agendamento por deck e sessão de revisão espaçada — em `prd-flashcards-decks-e-revisao` (RF-33…RF-36).
- Flashcards compartilhados entre usuários e decks públicos (v2+).
- Flashcards com imagem, áudio ou vídeo (v2+).
- Geração automática de flashcard por IA a partir da questão (v2+).
- Importação e exportação de decks (formato Anki ou CSV).
- Edição colaborativa e sugestão de cards por professores.
- Notificações ou lembretes de revisão (push fora da v1).
