# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Banco de questões e taxonomia (administração de conteúdo) · **Fase:** F1 — Base
> Derivado de `PRD.md` (Certame v1.3). Cobre RF-18, RF-19, RF-20, RN-01 e RN-03. Acrescenta **RF-37** (cadastro de bancas e órgãos), requisito novo sem correspondência na v1.3 do `PRD.md` — também registrado lá, na tabela de requisitos e no mapa de rastreabilidade da funcionalidade 2.

## Visão geral

Todo o valor do Certame para o estudante vem do acervo: questões curadas, com gabarito e comentário revisados, classificadas por disciplina, assunto, banca, órgão, ano e dificuldade. Esta funcionalidade é a ferramenta de trabalho de professores e administradores — a esteira editorial que cadastra, revisa e publica esse acervo em escala, sem planilhas paralelas nem importações manuais no banco.

O fluxo é deliberadamente simples: o professor cadastra e edita questões e mantém a árvore de matérias e assuntos; o administrador decide o que vai ao ar e mantém o catálogo de bancas e órgãos. O status da questão (Rascunho → Em revisão → Publicada) é o único portão entre o trabalho editorial e o que o estudante enxerga, e nenhuma questão chega a uma sessão de estudo ou simulado sem passar por ele.

## Objetivos

- **Operação editorial autossuficiente:** ≥ 40 questões publicadas por professor por semana (métrica do PRD-mãe).
- **Qualidade do acervo:** 100% das questões publicadas com gabarito e comentário revisados — garantido por validação de publicação, não por disciplina do time.
- **Acervo mínimo de lançamento:** 5.000 questões publicadas antes do lançamento.
- **Produtividade da tela:** listas administrativas respondem em até 800 ms (p95) com 100 mil questões na base.
- **Taxonomia consistente:** 0 assuntos órfãos ou duplicados na árvore de matérias, e nenhuma questão publicada sem disciplina.

## Histórias de usuário

- **US1** — Como professor, quero listar as questões com busca e filtros, para encontrar rapidamente o que preciso editar ou revisar.
- **US2** — Como professor, quero cadastrar uma questão em um formulário de página cheia, para escrever enunciado, alternativas, gabarito e comentário sem sair da tela.
- **US3** — Como professor, quero salvar uma questão incompleta como rascunho, para continuar depois sem perder o trabalho.
- **US4** — Como administrador, quero publicar e despublicar uma questão, para controlar o que entra nas sessões de estudo e nos simulados.
- **US5** — Como professor, quero manter a árvore de matérias e assuntos com a contagem de questões, para saber onde o acervo está raso.
- **US6** — Como administrador, quero que uma questão só possa ser publicada com gabarito, comentário e alternativas suficientes, para que o estudante nunca veja conteúdo pela metade.
- **US7** — Como administrador, quero cadastrar, editar e excluir bancas e órgãos, para manter atualizado o catálogo usado no formulário de questões.
- **Caso de borda 1** — Tento excluir um assunto que já tem questões vinculadas: o sistema oferece arquivar, não excluir.
- **Caso de borda 3** — Tento excluir uma banca ou órgão que já tem questões vinculadas: a exclusão é recusada.
- **Caso de borda 2** — Uma questão publicada precisa de correção: ao despublicá-la, ela sai imediatamente das novas sessões, mas o histórico de respostas já registradas é preservado.

## Principais funcionalidades

### 1. Listagem de questões (RF-18)

Tabela de trabalho do time editorial, otimizada para varredura rápida e triagem por status.

- **RF-18** — Listagem de questões com código, enunciado resumido, disciplina, banca/ano, tipo e status, com busca por texto e filtros. A tabela é paginada e os status são Rascunho, Em revisão e Publicada.

### 2. Formulário de questão em página cheia (RF-19)

Uma questão é um objeto longo; o cadastro ocupa a página inteira em vez de um modal, para caber enunciado e alternativas sem rolagem aninhada.

- **RF-19** — Formulário com tipo (múltipla escolha A–E ou certo/errado), disciplina, assunto, banca, ano, órgão, dificuldade, enunciado, alternativas, gabarito e comentário. A publicação exige gabarito, comentário e ao menos duas alternativas no caso de múltipla escolha.

### 3. Ciclo de vida e publicação

O status é a fronteira entre a redação e o acervo visível. Publicar e despublicar é ação de administrador; cadastrar e editar é ação de professor.

- **RN-01** — Somente questões com status *Publicada* entram em sessões de estudo e simulados.
- **RN-03** — O percentual de acerto exibido para uma questão é recalculado a partir de todas as respostas válidas, excluindo as respostas do próprio autor da questão.

### 4. Árvore de matérias e assuntos (RF-20)

Taxonomia de duas camadas (disciplina → assunto) usada por questões, flashcards, desempenho e composição de simulados.

- **RF-20** — Árvore de matérias e assuntos com contagem de questões, permitindo criar, renomear e remover nós. Assunto com questões vinculadas não pode ser excluído — apenas arquivado.

### 5. Cadastro de bancas e órgãos (RF-37)

> Requisito acrescentado nesta especificação, sem correspondência no `PRD.md` (Certame v1.3) original — decisão registrada nesta conversa: bancas e órgãos deixam de ser catálogo somente semeado e passam a ter tela própria de cadastro, mantida pelo administrador (diferente da árvore de matérias, mantida pelo professor).

Catálogo plano — sem hierarquia — usado pelos combos Banca e Órgão do formulário de questão. Diferente da taxonomia, é o administrador quem cadastra e mantém, não o professor.

- **RF-37** — Tela de cadastro de bancas e de órgãos, com criar, editar (nome e sigla) e excluir. A exclusão é recusada quando há questões vinculadas.

## Critérios de aceitação

- **CA-01** (US1, RF-18) — Dada uma base com 100 mil questões, quando abro a listagem, então a primeira página é exibida em até 800 ms (p95) com código, enunciado resumido, disciplina, banca/ano, tipo e status.
- **CA-02** (US1, RF-18) — Dado um termo de busca e filtros de disciplina, banca e status combinados, quando aplico, então a lista mostra apenas os itens correspondentes e a paginação é recalculada.
- **CA-03** (US2, RF-19) — Dado o tipo múltipla escolha, quando preencho o formulário, então posso registrar de duas a cinco alternativas (A–E) e marcar exatamente uma como gabarito.
- **CA-04** (US2, RF-19) — Dado o tipo certo/errado, quando escolho esse tipo, então o bloco de alternativas é substituído pela escolha do gabarito entre Certo e Errado.
- **CA-05** (US3, RF-19) — Dada uma questão com apenas enunciado preenchido, quando salvo, então ela é gravada como Rascunho, sem exigir gabarito ou comentário.
- **CA-06** (US6, RF-19) — Dada uma questão sem gabarito, sem comentário, com menos de duas alternativas (múltipla escolha) ou sem disciplina, quando tento publicá-la, então a publicação é recusada com a indicação de cada campo pendente.
- **CA-07** (US4, RN-01) — Dada uma questão em Rascunho ou Em revisão, quando um estudante inicia qualquer sessão de estudo ou simulado, então essa questão nunca é sorteada.
- **CA-08** (Borda 2, US4) — Dada uma questão publicada que é despublicada, quando novas sessões são montadas, então ela deixa de aparecer, e as respostas já registradas continuam íntegras no histórico e nas estatísticas.
- **CA-09** (RN-03) — Dadas respostas do próprio autor da questão, quando o percentual de acerto da questão é calculado, então essas respostas são excluídas do cálculo.
- **CA-10** (US5, RF-20) — Dada a árvore de matérias, quando a abro, então cada disciplina e cada assunto exibe a contagem de questões vinculadas.
- **CA-11** (Borda 1, RF-20) — Dado um assunto com ao menos uma questão vinculada, quando tento excluí-lo, então a exclusão é recusada e me é oferecido arquivar o assunto.
- **CA-12** (US5, RF-20) — Dado um assunto renomeado, quando confirmo, então todas as questões vinculadas passam a exibir o novo nome, sem alteração de vínculo.
- **CA-13** (US7, RF-37) — Dado um administrador autenticado, quando cadastra uma banca ou um órgão com nome (e sigla opcional), então o item passa a aparecer nos combos do formulário de questão.
- **CA-14** (Borda 3, US7, RF-37) — Dada uma banca ou um órgão com ao menos uma questão vinculada, quando o administrador tenta excluí-lo, então a exclusão é recusada.

## Experiência do usuário

**Perfis e necessidades.** O professor passa horas por semana nestas telas: precisa de densidade de informação, atalhos e o mínimo de cliques por questão. O administrador entra para revisar e publicar em lote de decisões, não para redigir.

**Fluxos principais.**
1. *Cadastrar* — Questões → Nova questão → formulário de página cheia → salvar como Rascunho ou enviar para revisão.
2. *Revisar e publicar* — Questões → filtro "Em revisão" → abrir → conferir gabarito e comentário → Publicar.
3. *Corrigir* — Questões → busca por código → editar → salvar (a questão publicada permanece publicada).
4. *Organizar taxonomia* — Matérias → árvore → criar, renomear, arquivar.
5. *Manter catálogo* — Catálogo → bancas e órgãos → criar, editar, excluir (administrador).

**UI/UX e acessibilidade.**
- Tradução 1:1 do protótipo aprovado sobre o design system Nocturne; nenhuma biblioteca de UI de terceiros.
- Listagem paginada com filtros persistentes durante a navegação de ida e volta ao formulário — voltar da edição devolve a lista no mesmo ponto.
- Estados de status com rótulo textual, nunca apenas por cor (contraste mínimo 4,5:1).
- Formulário totalmente operável por teclado, com foco visível e validação inline por campo; mensagens em português do Brasil.
- Ações destrutivas (excluir, despublicar, arquivar) sempre com confirmação explícita.

## Restrições técnicas de alto nível

- **Stack não negociável:** Angular 22 no frontend; Spring Boot 4.1 / Java 25, PostgreSQL 18 e Flyway no backend.
- **Autorização no servidor:** cadastrar e editar questão exige perfil professor ou superior; publicar, despublicar e manter o catálogo de bancas e órgãos (RF-37) exige administrador ou superior — validado no servidor mesmo quando a interface esconde o botão.
- **Auditoria:** ações administrativas sobre questões e taxonomia são registradas (ator, ação, entidade, data).
- **Desempenho e escala:** listas administrativas em até 800 ms (p95) com 100 mil questões; busca textual em português.
- **Integridade:** o acervo é referenciado por respostas, cadernos, simulados e flashcards — exclusões precisam ser lógicas ou bloqueadas, nunca em cascata destrutiva.
- **Idioma:** português do Brasil em toda a interface, incluindo mensagens de validação.

## Fora do escopo

- Importação em massa de questões por planilha ou arquivo (v2+; mitigação prevista apenas se o acervo inicial ficar aquém).
- Geração ou revisão automática de questões e comentários por IA.
- Fluxo de aprovação com múltiplos revisores, comentários internos entre revisores e histórico de versões do enunciado.
- Questões discursivas e sua correção.
- Anexos de imagem, áudio ou vídeo no enunciado.
- Relatórios editoriais exportáveis em PDF.
- Estatísticas de desempenho por questão exibidas ao estudante — pertencem a `prd-sessao-de-estudo` (RF-08) e `prd-cadernos-e-desempenho`.
