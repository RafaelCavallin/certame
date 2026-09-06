# Certame — PRD v1.0

> Plataforma de estudo para concursos públicos. Documento de requisitos de produto.
> Versão 1.4 · 29/08/2026 · Status: aprovado para implementação.
> Alteração v1.1: inclusão de flashcards (RF-28…RF-34, RN-07…RN-09, fase F6).
> Alteração v1.2: gestão de decks e limites de agendamento por deck (RF-35, RF-36, RN-10, RN-11).
> Alteração v1.3: o algoritmo de repetição espaçada passa a ser o **FSRS** (RN-08 reescrita; RF-36 troca o tratamento de lapso pela retenção desejada).
> Alteração v1.4: este documento passa a ser o **PRD-mãe** — contexto, métricas, perfis, regras de negócio, requisitos não funcionais e faseamento. Os requisitos detalhados foram divididos em nove PRDs derivados, listados na §10.

## 1. Contexto e problema

Candidatos a concursos estudam por repetição de questões, mas as ferramentas disponíveis os afogam em opções: filtros, estatísticas, comentários e propaganda competem com a única tarefa que importa — ler o enunciado e decidir. O resultado é sessão curta, dispersão alta e nenhuma leitura confiável de evolução.

O Certame resolve isso com um banco de questões curado e uma interface de resolução deliberadamente silenciosa (modo **Foco**), com contexto e simulação disponíveis quando o candidato pede. Do lado da operação, professores e administradores precisam cadastrar, revisar e publicar questões e simulados em escala, sem planilhas paralelas.

## 2. Objetivos e métricas

| Objetivo | Métrica | Meta (6 meses pós-lançamento) |
| --- | --- | --- |
| Sessão de estudo mais longa e contínua | Mediana de questões por sessão | ≥ 25 |
| Retenção do candidato | Usuários ativos semanais / cadastrados | ≥ 45% |
| Evolução mensurável | % de usuários com 8 semanas de série | ≥ 30% |
| Operação editorial autossuficiente | Questões publicadas por professor/semana | ≥ 40 |
| Qualidade do acervo | Questões publicadas com gabarito e comentário revisados | 100% |
| Retenção ativa por revisão espaçada | Usuários com ≥ 20 flashcards revisados por semana | ≥ 25% |

## 3. Perfis de acesso

Quatro perfis, hierárquicos em capacidade. O perfil determina a navegação exibida e as operações permitidas.

| Capacidade | Estudante | Professor | Administrador | Super-admin |
| --- | --- | --- | --- | --- |
| Resolver questões, cadernos, simulados, desempenho | ✓ | ✓ | ✓ | ✓ |
| Criar, gerenciar e revisar os próprios flashcards | ✓ | ✓ | ✓ | ✓ |
| Cadastrar e editar questões, matérias e assuntos | — | ✓ | ✓ | ✓ |
| Publicar / despublicar questão | — | — | ✓ | ✓ |
| Criar e agendar simulados, gerenciar editais | — | — | ✓ | ✓ |
| CRUD de usuários, troca de perfil, redefinir senha de terceiros | — | — | — | ✓ |

## 4. Escopo da v1

**Dentro:** autenticação e cadastro · recuperação de senha · início/dashboard · resolução de questões (Foco, Contexto, Modo Prova) · cadernos · **flashcards: criação dentro da sessão de estudo, gerenciamento, decks com limites por deck e sessão de revisão espaçada** · simulados do estudante · desempenho · CRUD de questões, matérias/assuntos, simulados e editais · CRUD de usuários (super-admin).

**Fora (v2+):** flashcards compartilhados entre usuários e decks públicos · flashcards com imagem/áudio · geração automática de flashcard por IA · pagamentos e planos · correção de discursivas · videoaulas · app nativo · fórum/comentários de usuários · importação em massa de questões · notificações push · relatórios exportáveis em PDF.

## 5. Requisitos funcionais

### 5.1 Acesso e conta

| ID | Requisito | Critério de aceite |
| --- | --- | --- |
| RF-01 | Login por e-mail e senha, com abas Entrar / Criar conta na mesma tela. | Credencial válida abre o Início do perfil correspondente; inválida exibe erro inline sem limpar o e-mail. |
| RF-02 | Cadastro de estudante com nome, e-mail e senha. | Conta criada com perfil `estudante` e status ativo; e-mail duplicado é recusado com mensagem. |
| RF-03 | Fluxo "Esqueci minha senha": informar e-mail e receber link de redefinição. | Tela de confirmação exibida sempre, mesmo para e-mail inexistente (não vaza cadastro); e-mail malformado é barrado. |
| RF-04 | Definição de nova senha por link com token de uso único. | Token expira em 60 min ou após o uso; senha mínima de 8 caracteres. |
| RF-05 | Encerrar sessão a partir do rodapé da navegação. | Sessão invalidada e retorno à tela de login. |

### 5.2 Início e resolução de questões

| ID | Requisito | Critério de aceite |
| --- | --- | --- |
| RF-06 | Início com resumo do dia: questões resolvidas, % de acerto, tempo de estudo e continuar de onde parou. | Números refletem a atividade real do usuário no período exibido. |
| RF-07 | Resolução de questões de múltipla escolha (A–E) e certo/errado. | Uma alternativa selecionável por vez; responder revela gabarito, comentário e estatísticas. |
| RF-08 | Três modos: **Foco** (padrão, só enunciado e alternativas), **Contexto** (metadados, estatísticas, comentário) e **Modo Prova**. | Troca de modo preserva a questão atual; Foco é o modo inicial de toda sessão. |
| RF-09 | Modo Prova: responde a série completa sem gabarito, com finalização explícita. | Nenhum feedback antes de finalizar; navegação livre entre questões respondidas. |
| RF-10 | Correção do Modo Prova com % de acerto, acertos, erros, não respondidas, tempo total e revisão questão a questão. | Ações "refazer" e "voltar ao início"; resultado gravado no histórico. |
| RF-11 | Marcar questão (favorita / erro recorrente) e navegar por número dentro da série. | Marcação persiste entre sessões e alimenta os cadernos automáticos. |
| RF-12 | Cronômetro de sessão visível, contínuo enquanto a sessão está ativa. | Tempo somado ao total de estudo do dia. |
| RF-28 | Botão **"Novo flashcard"** na barra de ações da questão, disponível nos três modos, abre modal de criação sobre a questão. | O modal não navega nem recarrega a tela: alternativa marcada, índice da série e cronômetro permanecem intactos ao abrir e ao fechar. |
| RF-29 | Modal de criação com frente, verso, deck, disciplina, assunto e tags. Disciplina, assunto e questão de origem vêm pré-preenchidos a partir da questão em tela. | Frente e verso obrigatórios (mín. 3 caracteres); salvar fecha o modal, exibe toast de confirmação e devolve o foco ao enunciado. |
| RF-30 | Ações do modal: **Salvar e fechar**, **Salvar e criar outro** (mantém o modal aberto e limpo) e **Cancelar**. | Cancelar com campos preenchidos pede confirmação; `Esc` equivale a cancelar; contador de flashcards criados na sessão fica visível no botão. |

### 5.3 Cadernos, simulados e desempenho

| ID | Requisito | Critério de aceite |
| --- | --- | --- |
| RF-13 | Cadernos com progresso, descrição e atualização; automáticos ("Erros recorrentes", "Favoritas") e manuais. | Abrir um caderno inicia sessão restrita às suas questões. |
| RF-14 | Lista de simulados disponíveis, agendados e concluídos, com nota. | Simulado concluído não pode ser refeito para efeito de ranking; permite revisão. |
| RF-15 | Desempenho: evolução do % de acerto por semana (linha + área) e volume semanal. | Série de até 8 semanas; períodos sem atividade aparecem como lacuna, não como zero. |
| RF-16 | Desempenho por disciplina e lista de pontos fracos (assunto, disciplina, volume, % de acerto). | Ponto fraco = assunto com ≥ 20 respostas e acerto abaixo da média do usuário. |
| RF-17 | Editais acompanhados com status (aberto, previsto, encerrado) e disciplinas exigidas. | Estudante vê os editais publicados pela administração. |

### 5.4 Flashcards

| ID | Requisito | Critério de aceite |
| --- | --- | --- |
| RF-31 | Menu **Flashcards** na navegação do estudante, com lista dos cards do próprio usuário: frente resumida, deck, disciplina/assunto, estado (novo, aprendendo, revisão) e próxima revisão. | Tabela paginada com busca por texto e filtros por deck, disciplina e estado; ordenação por criação e por próxima revisão. |
| RF-32 | Editar, arquivar e excluir flashcard; organizar cards em decks manuais; criar flashcard avulso fora da sessão de estudo. | Card criado a partir de uma questão mantém link para ela ("ver questão de origem"); exclusão é lógica e preserva o histórico de revisões. |
| RF-33 | Tela **Estudar flashcards**: sessão só de cards — mostra a frente, revela o verso por clique ou barra de espaço e o usuário classifica em Errei / Difícil / Bom / Fácil. Cada classificação exibe o intervalo que o FSRS agendaria, e o verso mostra estabilidade, dificuldade e retenção atual do card. | Fila respeita deck/disciplina escolhidos, limite diário e agendamento (RN-08); atalhos de teclado 1–4 para as classificações. |
| RF-34 | Resumo ao fim da sessão de flashcards: cards revisados, distribuição por classificação, tempo total e quantidade vencendo nos próximos dias. | Resultado gravado no histórico; tempo somado ao total de estudo do dia (RF-06). |
| RF-35 | Tela **Decks**, acessível de Flashcards: um card por deck com nome, descrição, total de cards, vencendo hoje, novos, resumo dos limites e ação "Estudar este deck". Criar, renomear, descrever e excluir deck. | Nome de deck único por usuário (mín. 3 caracteres); renomear move todos os cards junto; excluir não apaga cards — eles vão para "Sem deck" (RN-11). |
| RF-36 | Configuração de agendamento **por deck**: novos por dia (0–100), revisões por dia (5–400), intervalo máximo em dias (1–3650) e **retenção desejada** (80%, 85%, 90% padrão ou 95%), que é o alvo do FSRS ao calcular cada intervalo. | Valores fora do intervalo são ajustados ao limite ao salvar; a configuração passa a valer na próxima montagem de fila, sem alterar o histórico de revisões nem os valores de memória já calculados. |

### 5.5 Administração de conteúdo

| ID | Requisito | Critério de aceite |
| --- | --- | --- |
| RF-18 | Listagem de questões com código, enunciado resumido, disciplina, banca/ano, tipo e status; busca e filtros. | Tabela paginada; status Rascunho / Em revisão / Publicada. |
| RF-19 | Formulário de questão em página cheia: tipo, disciplina, assunto, banca, ano, órgão, dificuldade, enunciado, alternativas, gabarito e comentário. | Publicação exige gabarito, comentário e ao menos duas alternativas (múltipla escolha). |
| RF-20 | Árvore de matérias e assuntos com contagem de questões; criar, renomear e remover. | Assunto com questões vinculadas não pode ser excluído — apenas arquivado. |
| RF-21 | CRUD de simulados: código, nome, banca, nº de questões, duração, janela, status e composição por disciplina. | Publicar exige janela e quantidade compatível com a composição. |
| RF-37 | Cadastro de bancas e órgãos (catálogo plano, sem hierarquia) pelo administrador: criar, editar e excluir. *(Acrescentado após a v1.3; ver `tasks/prd-banco-de-questoes/prd.md`.)* | Exclusão recusada quando há questões vinculadas. |
| RF-22 | CRUD de editais com status, vagas, banca, prazo e disciplinas. | Alteração reflete imediatamente na visão do estudante. |

### 5.6 Gestão de usuários (super-admin)

| ID | Requisito | Critério de aceite |
| --- | --- | --- |
| RF-23 | Tabela de usuários com nome, e-mail, perfil, status e data de criação. | Ordenação por criação, filtro por perfil e status. |
| RF-24 | Criar usuário com nome, e-mail, perfil, status e envio opcional de convite. | Validação de nome e e-mail; e-mail único; usuário aparece na lista sem recarregar a página. |
| RF-25 | Editar usuário, alterar perfil e status (ativo, inativo, convite pendente). | Super-admin não pode remover o próprio acesso nem rebaixar o último super-admin. |
| RF-26 | Excluir usuário com confirmação. | Exclusão lógica; histórico de respostas preservado de forma anonimizada. |
| RF-27 | Ação "Redefinir senha" envia link ao usuário e confirma por notificação na tela. | Nenhuma senha é exibida ou definida manualmente pelo administrador. |

## 6. Regras de negócio

| ID | Regra |
| --- | --- |
| RN-01 | Somente questões com status *Publicada* entram em sessões de estudo e simulados. |
| RN-02 | Cada resposta é imutável: registra questão, alternativa marcada, acerto, tempo gasto e contexto (livre, caderno ou simulado). |
| RN-03 | O % de acerto de uma questão é recalculado a partir de todas as respostas válidas, excluindo respostas do próprio autor. |
| RN-04 | No Modo Prova, respostas só são consideradas após a finalização; abandono sem finalizar descarta a tentativa. |
| RN-05 | Convite pendente permite login apenas depois da definição de senha pelo link enviado. |
| RN-06 | Usuário inativo é bloqueado no login com mensagem genérica de credencial inválida. |
| RN-07 | Flashcard é privado do usuário que o criou: nenhum outro perfil, incluindo super-admin, lê ou edita o conteúdo de cards de terceiros. |
| RN-08 | Agendamento por **FSRS** (Free Spaced Repetition Scheduler, versão 5). Cada card carrega dois valores de memória — **estabilidade** (em dias) e **dificuldade** (1 a 10) — atualizados a cada revisão a partir da nota (Errei, Difícil, Bom, Fácil) e da retrievability no momento da revisão. O intervalo é o tempo em que a retrievability cai até a **retenção desejada do deck** (RF-36), limitado pelo intervalo máximo do deck. *Errei* reduz a estabilidade pela fórmula de lapso e coloca o card em *reaprendendo*; não existe mais fator de facilidade nem progressão fixa de dias. Os pesos do FSRS são parâmetros do sistema, versionados junto ao código. |
| RN-09 | Flashcard criado durante uma sessão de estudo registra a questão e a sessão de origem, para rastrear o que a resolução de questões gerou de revisão. |
| RN-10 | A fila diária é montada deck a deck e respeita os limites do deck (RF-36): no máximo *novos por dia* cards em estado novo e *revisões por dia* cards já agendados, por deck. O intervalo calculado pelo FSRS nunca excede o intervalo máximo do deck. |
| RN-11 | Excluir um deck é operação de organização, não de conteúdo: os cards são preservados e passam para "Sem deck", que usa os limites padrão (10 novos/dia, 60 revisões/dia, máximo de 180 dias). |

## 7. Requisitos não funcionais

| Categoria | Requisito |
| --- | --- |
| Desempenho | Próxima questão em até 300 ms (p95); listas administrativas em até 800 ms com 100 mil questões. |
| Disponibilidade | 99,5% mensal; manutenção fora do pico (19h–23h BRT). |
| Segurança | Senhas em BCrypt; autorização por perfil no servidor (nunca só na interface); auditoria de ações administrativas. |
| Privacidade | LGPD: base legal registrada, exportação e exclusão de dados a pedido do titular. |
| Acessibilidade | Navegação completa por teclado, foco visível, contraste mínimo 4,5:1 em texto corrido. |
| **Identidade visual** | Interface implementada **exatamente** como o protótipo aprovado, sobre o design system Nocturne — sem novas cores, fontes ou espaçamentos. |
| Idioma | Português do Brasil em toda a interface, incluindo erros e datas. |

## 8. Faseamento

| Fase | Entrega | Requisitos |
| --- | --- | --- |
| F1 — Base | Autenticação, perfis, navegação, CRUD de matérias e questões | RF-01…05, RF-18…20 |
| F2 — Estudo | Sessão nos três modos, correção, marcações | RF-06…12 |
| F3 — Progresso | Cadernos, desempenho, pontos fracos | RF-13, RF-15, RF-16 |
| F4 — Simulados | Simulados do estudante, CRUD de simulados e editais | RF-14, RF-17, RF-21, RF-22 |
| F5 — Governança | Gestão de usuários, auditoria, redefinição administrativa | RF-23…27 |
| F6 — Flashcards | Criação no fluxo da questão, gerenciamento, decks com limites por deck e sessão de revisão espaçada | RF-28…36 |

## 9. Riscos e questões abertas

| Risco / questão | Mitigação ou decisão pendente |
| --- | --- |
| Acervo inicial insuficiente | 5.000 questões publicadas antes do lançamento; antecipar importação em massa se necessário. |
| Custo de estatísticas em tempo real | Agregados materializados por job (Tech Spec §8). |
| Entrega de e-mails transacionais | Provedor a definir (SES ou SendGrid) até o fim da F1. |
| Uso predominante em celular | Layout responsivo ainda não especificado; roadmap imediato pós-v1. |
| Monetização | Sem planos pagos na v1. |
| Modal de flashcard tirar o foco da questão | Modal enxuto (frente, verso, deck), campos de classificação pré-preenchidos e "Salvar e criar outro"; medir abandono de sessão após criação. |
| Calibração dos pesos do FSRS | v1 usa os pesos padrão publicados; otimização por histórico do usuário fica para a v2, quando houver volume de revisões suficiente. |
| Limites por deck mal ajustados geram acúmulo de fila | Padrões conservadores (10 novos/dia, 60 revisões/dia) e aviso na tela de decks quando a fila vencida passa do dobro do limite diário. |

## 10. PRDs derivados

Este documento continua sendo a fonte única de contexto, métricas, perfis, regras de negócio (§6), requisitos não funcionais (§7) e faseamento (§8). Os requisitos foram divididos em nove PRDs, cada um com histórias de usuário, critérios de aceitação e recortes de escopo próprios, em `tasks/prd-<slug>/prd.md`. É a partir deles que se produzem a TechSpec e as tarefas de implementação.

| # | PRD | Caminho | Requisitos | Regras | Fase |
| --- | --- | --- | --- | --- | --- |
| 1 | Acesso e conta | `tasks/prd-acesso-e-conta/prd.md` | RF-01…RF-05 | RN-05, RN-06 | F1 |
| 2 | Banco de questões e taxonomia | `tasks/prd-banco-de-questoes/prd.md` | RF-18, RF-19, RF-20, RF-37 | RN-01, RN-03 | F1 |
| 3 | Início e sessão de estudo | `tasks/prd-sessao-de-estudo/prd.md` | RF-06…RF-12 | RN-01…RN-04 | F2 |
| 4 | Cadernos e desempenho | `tasks/prd-cadernos-e-desempenho/prd.md` | RF-13, RF-15, RF-16 | RN-02, RN-04 | F3 |
| 5 | Simulados | `tasks/prd-simulados/prd.md` | RF-14, RF-21 | RN-01, RN-04 | F4 |
| 6 | Editais | `tasks/prd-editais/prd.md` | RF-17, RF-22 | — | F4 |
| 7 | Gestão de usuários | `tasks/prd-gestao-de-usuarios/prd.md` | RF-23…RF-27 | RN-05, RN-06 | F5 |
| 8 | Flashcards — criação e gerenciamento | `tasks/prd-flashcards-criacao-e-gerenciamento/prd.md` | RF-28…RF-32 | RN-07, RN-09 | F6 |
| 9 | Flashcards — decks e revisão espaçada | `tasks/prd-flashcards-decks-e-revisao/prd.md` | RF-33…RF-36 | RN-07, RN-08, RN-10, RN-11 | F6 |

### Mapa requisito → PRD

| Requisito | PRD derivado |
| --- | --- |
| RF-01…RF-05 | 1 · Acesso e conta |
| RF-06…RF-12 | 3 · Início e sessão de estudo |
| RF-13, RF-15, RF-16 | 4 · Cadernos e desempenho |
| RF-14 | 5 · Simulados |
| RF-17 | 6 · Editais |
| RF-18…RF-20, RF-37 | 2 · Banco de questões e taxonomia |
| RF-21 | 5 · Simulados |
| RF-22 | 6 · Editais |
| RF-23…RF-27 | 7 · Gestão de usuários |
| RF-28…RF-32 | 8 · Flashcards — criação e gerenciamento |
| RF-33…RF-36 | 9 · Flashcards — decks e revisão espaçada |

### Dependências entre os PRDs

- **1 → todos:** identidade e perfil são pré-requisito de qualquer tela.
- **2 → 3, 5:** sem acervo publicado não há sessão de estudo nem simulado (RN-01).
- **3 → 4, 5, 8:** o histórico imutável de respostas (RN-02) alimenta cadernos e desempenho; o Modo Prova é reaproveitado pelo simulado; a barra de ações da questão hospeda a criação de flashcard.
- **2 → 6, 8:** a árvore de matérias e assuntos é referenciada por editais e flashcards.
- **8 → 9:** sem cards não há fila de revisão.
- **9 → 3:** o tempo da sessão de flashcards entra no resumo do dia (RF-06).
