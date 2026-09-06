# Certame — Especificação técnica v1.0

> Angular 22 · Spring Boot 4.1 (Spring Framework 7, Java 25 LTS) · PostgreSQL 18
> Referência: `docs/PRD.md` v1.3 · 29/08/2026 · Tech Spec v1.3 (flashcards, decks configuráveis e agendamento FSRS, §3–§10)

## 1. Visão da arquitetura

SPA Angular servida como estático, consumindo uma API REST Spring Boot 4.1 stateless autenticada por JWT, sobre um único PostgreSQL. Sem microsserviços na v1: monólito modular por domínio, com fronteiras de pacote rígidas o bastante para extração futura.

```
Angular 22 (SPA)  ──HTTPS/JSON + JWT──▶  Spring Boot 4.1 (REST)  ──JDBC──▶  PostgreSQL 18
   standalone                              Web · Security 7 · JPA           Flyway · GIN · MVs
   zoneless · signals                      Validation · MapStruct
```

Serviços auxiliares: e-mail transacional (SMTP/SES), armazenamento de objetos para anexos de enunciado (S3-compatível) e Redis opcional a partir da F3 para cache de agregados.

## 2. Stack e convenções

| Camada | Tecnologia | Convenções |
| --- | --- | --- |
| Frontend | Angular 22, TypeScript 5.9, Angular Router, Reactive Forms (Signal Forms onde estável) | Componentes standalone, detecção de mudanças *zoneless*, estado por signals em serviços `providedIn: 'root'`. |
| Estilo | CSS do design system Nocturne (`styles.css`) + SCSS por componente | Nenhum valor cru: cores, tipos, espaços e raios sempre por `var(--*)`. Sem biblioteca de UI de terceiros. |
| Backend | Java 25 (LTS), Spring Boot 4.1 / Spring Framework 7, Spring Security 7, Spring Data JPA, Bean Validation, MapStruct | Pacote por domínio; controllers só orquestram; regra em `service`; DTOs em `record`. |
| Banco | PostgreSQL 18, Flyway | `snake_case`, PKs `uuid` (`gen_random_uuid()`), `timestamptz` em UTC, migrações versionadas e imutáveis. |
| Build / CI | Maven, Node 20+, Docker, GitHub Actions | lint → testes → build → imagem → deploy. Nenhum merge sem pipeline verde. |

**Política de versões:** Angular e Spring Boot lançam a cada seis meses com janela curta (18 meses / 12 meses OSS). O projeto nasce na estável corrente e mantém uma tarefa de atualização por semestre. Não adotamos release candidate em produção.

## 3. Frontend Angular

**O layout aprovado é congelado.** A migração do protótipo para Angular é uma tradução estrutural — mesma marcação, mesmas classes, mesmos tokens Nocturne. Nenhum ajuste visual (espaçamento, cor, tamanho, sombra, radius) sem aprovação de design registrada.

```
src/app/
  core/          auth (guards, interceptor, token store), api (http clients), models
  shared/        ui/ (botão, tag, campo, tabela, diálogo, toast), pipes, directives
  layout/        shell com sidebar por perfil, topbar, área de conteúdo
  features/
    auth/        login, cadastro, esqueci-senha, definir-senha
    dashboard/   início do estudante
    study/       sessão de questões: foco | contexto | prova + correção
    cadernos/    lista e sessão por caderno
    simulados/   lista, execução, resultado
    stats/       desempenho (evolução, disciplinas, pontos fracos)
    flashcards/  lista, decks/ (grade + dialogo de configuracao), sessao-revisao/, flashcard-dialog/
    admin/       questoes/, materias/, simulados/, editais/
    super/       usuarios/ (lista + formulário)
```

| Rota | Perfis | Observações |
| --- | --- | --- |
| `/login`, `/cadastro`, `/esqueci-senha`, `/definir-senha/:token` | público | Fora do shell; abas Entrar/Criar conta na mesma tela. |
| `/inicio`, `/estudar`, `/cadernos`, `/simulados`, `/desempenho` | autenticados | `authGuard`; modo de estudo em query param (`?modo=foco`). |
| `/flashcards`, `/flashcards/:id`, `/flashcards/decks`, `/flashcards/estudar` | autenticados | `authGuard`; a revisão aceita `?deck=` e `?disciplina=`. A criação de card e a configuração de deck **não** têm rota própria — são diálogos (RF-28, RF-36). |
| `/admin/questoes`, `/admin/questoes/:id`, `/admin/materias` | professor+ | `roleGuard(['PROFESSOR','ADMIN','SUPER_ADMIN'])`; publicar exige ADMIN. |
| `/admin/editais`, `/admin/editais/novo`, `/admin/editais/:id` | admin+ | `perfilGuard(['ADMIN','SUPER_ADMIN'])`; item de menu só renderizado para esses perfis (RF-22). |
| `/admin/simulados`, `/admin/simulados/:id` | admin+ | Formulário em página cheia com composição por disciplina. |
| `/super/usuarios`, `/super/usuarios/:id` | super-admin | Item de navegação só renderizado para SUPER_ADMIN. |

**Estado.** `AuthStore` (usuário, perfil, token), `StudySessionStore` (série, índice, respostas, cronômetro, modo) e `ToastStore`, todos com signals e `computed`. O cronômetro é um sinal atualizado por `interval`, pausado em `document.hidden`. O Modo Prova mantém respostas em memória e envia o lote na finalização; rascunho espelhado em `sessionStorage`.

**Flashcards no fluxo da questão.** `FlashcardDialogComponent` (`shared/ui/dialog`) é aberto pelo `DialogService` a partir da tela de estudo — sem navegação, sem recriar `StudySessionStore`, sem pausar o cronômetro. O diálogo recebe `{questaoId, disciplinaId, assuntoId, sessaoId}` da questão corrente, faz `POST /flashcards` e emite o toast; `FlashcardStore` mantém `criadosNaSessao` (contador no botão) e a fila de revisão. Rascunho do diálogo espelhado em `sessionStorage` sob `fc-draft:{questaoId}` e limpo no sucesso, de modo que uma recarga acidental não perca o texto. Falha de rede enfileira o card e reenvia no próximo `POST` bem-sucedido, sem bloquear a sessão.

**HTTP.** Interceptor anexa o access token, renova via refresh token em 401 uma única vez e redireciona ao login em falha; erros 422 são mapeados campo a campo para os `FormControl` correspondentes.

## 4. Backend Spring

```
br.com.certame
  config/        security, cors, jackson, openapi, flyway
  auth/          controller, service, jwt, refresh token, reset de senha
  usuario/       entity, repository, service, controller, dto
  catalogo/      disciplina, assunto, banca, orgao, edital
  questao/       questao, alternativa, publicação, busca
  estudo/        sessao, resposta, caderno
  simulado/      simulado, composicao, tentativa, resultado
  flashcard/     flashcard, deck, revisao, fsrs/ (pesos, agendador), fila de estudo
  desempenho/    agregados, consultas de relatório
  common/        exceções, ApiError, auditoria, paginação
```

Camadas: `Controller` (validação e mapeamento de DTO) → `Service` (`@Transactional`, regra, autorização de recurso) → `Repository` (Spring Data + JPQL/native quando necessário). Erros em `application/problem+json` com `code`, `message` e `fieldErrors`, por um `@RestControllerAdvice` único.

**Segurança.** `SecurityFilterChain` stateless; access token JWT de 15 min e refresh token opaco de 14 dias, persistido e rotacionado a cada uso. Perfis `ROLE_ESTUDANTE`, `ROLE_PROFESSOR`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN` aplicados por `@PreAuthorize` no serviço — a interface esconde, o servidor decide. BCrypt força 12. Rate limit de 5 tentativas por e-mail em 15 min no login e no pedido de redefinição.

## 5. Modelo de dados

| Tabela | Conteúdo e relações |
| --- | --- |
| `usuario` | nome, email (único), senha_hash, perfil, status, criado_em, excluido_em. Status: `ATIVO`, `INATIVO`, `CONVITE_PENDENTE`. |
| `refresh_token`, `token_senha` | hash do token, usuario_id, expira_em, usado_em. Uso único. |
| `disciplina`, `assunto` | assunto pertence a disciplina; `arquivado_em` e contagem de questões derivada, não armazenada. |
| `banca`, `orgao` | itens do catálogo referenciados por questão e por edital. |
| `edital` | nome (único, exclusão lógica à parte), status (`ABERTO`/`PREVISTO`/`ENCERRADO`), vagas, banca_id, orgao_id, prazo_inscricao, url_oficial, `excluido_em`. `check` garante que todo edital `ABERTO` tem `prazo_inscricao` (RF-17, RF-22). |
| `edital_disciplina` | vínculo N:N entre `edital` e `disciplina` — as disciplinas exigidas de cada edital (CA-08). PK composta, sem coluna própria. |
| `questao` | codigo, tipo (`ME`/`CE`), enunciado, disciplina_id, assunto_id, banca_id, `orgao_id`, ano, dificuldade, gabarito, comentario, status, autor_id, `publicado_em`, `excluido_em` e coluna gerada `busca`. |
| `alternativa` | questao_id, letra, texto, ordem. Ausente em certo/errado. |
| `sessao_estudo` | usuario_id, modo (`FOCO`/`CONTEXTO`/`PROVA`), origem, iniciada_em, finalizada_em. |
| `resposta` | sessao_id, questao_id, usuario_id, marcada, correta, tempo_ms, respondida_em. Imutável. |
| `caderno`, `caderno_questao` | caderno do usuário, manual ou automático (regra em `tipo`). |
| `marcacao` | usuario_id, questao_id, tipo (`FAVORITA`, `ERRO_RECORRENTE`). Único por trio. |
| `deck` | usuario_id, nome, descricao, criado_em + configuração de agendamento: novos_dia, revisoes_dia, intervalo_max_dias, retencao_alvo (0,80–0,97). Nome único por usuário; "Sem deck" é virtual (`deck_id null`) e usa os padrões da coluna. |
| `flashcard` | usuario_id, deck_id, frente, verso, disciplina_id, assunto_id, questao_origem_id, sessao_origem_id, tags, status e estado FSRS (estabilidade, dificuldade, intervalo_dias, repeticoes, lapsos, estado, passo_aprendizado, ultima_revisao, proxima_revisao, proxima_revisao_em). Privado do dono (RN-07). `proxima_revisao_em` (timestamptz) é a agenda de precisão por minuto que sustenta os passos curtos do FSRS-5 (RN-08); `proxima_revisao` (date, Brasília) deriva dela e é o que a fila usa para filtrar "vence hoje" — o `check flashcard_agenda_coerente` garante que as duas nunca divergem. |
| `flashcard_revisao` | flashcard_id, usuario_id, sessao_id, nota (`ERREI`/`DIFICIL`/`BOM`/`FACIL`), dias_decorridos, estabilidade_anterior/nova, dificuldade_anterior/nova, intervalo_novo, intervalo_minutos, tempo_ms, segundos_decorridos, revisado_em. Imutável — é o log que permitirá otimizar os pesos do FSRS depois. |
| `flashcard_sessao` | usuario_id, escopo (`TODOS`/`DECK`/`SEM_DECK`), deck_id, disciplina_id, iniciada_em, finalizada_em e os contadores da sessão (revisados, errei, dificil, bom, facil, tempo_ms) usados no resumo (RF-34) e no tempo de estudo do dia. |
| `simulado`, `simulado_composicao`, `tentativa_simulado` | composição por disciplina/quantidade; tentativa com nota, tempo e status. |
| `auditoria` | ator_id, acao, entidade, entidade_id, payload jsonb, criado_em. |

```sql
create table edital (
  id              uuid primary key default gen_random_uuid(),
  nome            text not null,
  status          text not null check (status in ('ABERTO','PREVISTO','ENCERRADO')),
  vagas           integer check (vagas >= 0 and vagas <= 999999),
  banca_id        uuid references banca(id),
  orgao_id        uuid references orgao(id),
  prazo_inscricao date,
  url_oficial     text check (url_oficial is null or url_oficial ~ '^https?://'),
  excluido_em     timestamptz,
  criado_em       timestamptz not null default now(),
  atualizado_em   timestamptz not null default now(),
  constraint edital_aberto_com_prazo check (
    status <> 'ABERTO' or prazo_inscricao is not null
  )
);
create unique index edital_nome_uk on edital (lower(nome)) where excluido_em is null;
create index edital_status_idx on edital (status, prazo_inscricao) where excluido_em is null;

create table edital_disciplina (
  edital_id     uuid not null references edital(id) on delete cascade,
  disciplina_id uuid not null references disciplina(id),
  primary key (edital_id, disciplina_id)
);
create index edital_disciplina_disciplina_idx on edital_disciplina (disciplina_id);

create table questao (
  id            uuid primary key default gen_random_uuid(),
  codigo        text not null unique,
  tipo          text not null check (tipo in ('ME','CE')),
  enunciado     text not null,
  disciplina_id uuid not null references disciplina(id),
  assunto_id    uuid references assunto(id),
  banca_id      uuid references banca(id),
  ano           smallint,
  dificuldade   text check (dificuldade in ('FACIL','MEDIA','DIFICIL')),
  gabarito      text not null,
  comentario    text,
  status        text not null default 'RASCUNHO'
                check (status in ('RASCUNHO','EM_REVISAO','PUBLICADA')),
  autor_id      uuid references usuario(id),
  criado_em     timestamptz not null default now()
);
create index questao_busca_idx on questao
  using gin (to_tsvector('portuguese', enunciado));
create index questao_filtro_idx on questao (status, disciplina_id, assunto_id);

create table resposta (
  id            uuid primary key default gen_random_uuid(),
  sessao_id     uuid not null references sessao_estudo(id),
  usuario_id    uuid not null references usuario(id),
  questao_id    uuid not null references questao(id),
  marcada       text not null,
  correta       boolean not null,
  tempo_ms      integer not null,
  respondida_em timestamptz not null default now()
);
create index resposta_usuario_data_idx on resposta (usuario_id, respondida_em desc);

create table deck (
  id                 uuid primary key default gen_random_uuid(),
  usuario_id         uuid not null references usuario(id),
  nome               text not null,
  descricao          text,
  novos_dia          smallint not null default 10  check (novos_dia between 0 and 100),
  revisoes_dia       smallint not null default 60  check (revisoes_dia between 5 and 400),
  intervalo_max_dias smallint not null default 180 check (intervalo_max_dias between 1 and 3650),
  retencao_alvo      numeric(3,2) not null default 0.90
                     check (retencao_alvo between 0.80 and 0.97),
  criado_em          timestamptz not null default now()
);
create unique index deck_nome_uk on deck (usuario_id, lower(nome));

create table flashcard (
  id                uuid primary key default gen_random_uuid(),
  usuario_id        uuid not null references usuario(id),
  deck_id           uuid references deck(id) on delete set null,
  frente            text not null check (length(btrim(frente)) >= 3),
  verso             text not null check (length(btrim(verso)) >= 3),
  disciplina_id     uuid references disciplina(id),
  assunto_id        uuid references assunto(id),
  questao_origem_id uuid references questao(id),
  sessao_origem_id  uuid references sessao_estudo(id),
  tags              text[] not null default '{}',
  status            text not null default 'ATIVO'
                    check (status in ('ATIVO','ARQUIVADO','EXCLUIDO')),
  estabilidade      real not null default 0,
  dificuldade       real not null default 0 check (dificuldade between 0 and 10),
  intervalo_dias    smallint not null default 0,
  repeticoes        smallint not null default 0,
  lapsos            smallint not null default 0,
  estado            text not null default 'NOVO'
                    check (estado in ('NOVO','APRENDENDO','REVISAO','REAPRENDENDO')),
  passo_aprendizado smallint not null default 0 check (passo_aprendizado between 0 and 10),
  ultima_revisao    timestamptz,
  proxima_revisao   date not null default (now() at time zone 'America/Sao_Paulo')::date,
  proxima_revisao_em timestamptz not null,
  arquivado_em      timestamptz,
  excluido_em       timestamptz,
  criado_em         timestamptz not null default now(),
  atualizado_em     timestamptz not null default now(),
  busca             tsvector generated always as
                    (to_tsvector('portuguese', coalesce(frente, '') || ' ' || coalesce(verso, ''))) stored,
  constraint flashcard_sessao_exige_questao check (
    sessao_origem_id is null or questao_origem_id is not null),
  constraint flashcard_excluido_coerente check (
    (status = 'EXCLUIDO') = (excluido_em is not null)),
  constraint flashcard_arquivado_coerente check (
    (status = 'ARQUIVADO') = (arquivado_em is not null)),
  constraint flashcard_agenda_coerente check (
    proxima_revisao = (proxima_revisao_em at time zone 'America/Sao_Paulo')::date)
);
create index flashcard_lista_idx  on flashcard (usuario_id, criado_em desc)
  where status <> 'EXCLUIDO';
create index flashcard_fila_idx   on flashcard (usuario_id, proxima_revisao)
  where status = 'ATIVO';
create index flashcard_fila_hora_idx on flashcard (usuario_id, proxima_revisao_em)
  where status = 'ATIVO';
create index flashcard_busca_idx  on flashcard using gin (busca);
create index flashcard_deck_idx   on flashcard (deck_id) where status <> 'EXCLUIDO';
create index flashcard_origem_idx on flashcard (sessao_origem_id) where sessao_origem_id is not null;

create table flashcard_sessao (
  id            uuid primary key default gen_random_uuid(),
  usuario_id    uuid not null references usuario(id),
  escopo        text not null check (escopo in ('TODOS','DECK','SEM_DECK')),
  deck_id       uuid references deck(id) on delete set null,
  disciplina_id uuid references disciplina(id),
  iniciada_em   timestamptz not null default now(),
  finalizada_em timestamptz,
  revisados     smallint not null default 0,
  errei         smallint not null default 0,
  dificil       smallint not null default 0,
  bom           smallint not null default 0,
  facil         smallint not null default 0,
  tempo_ms      integer  not null default 0,
  constraint flashcard_sessao_escopo_coerente check (
    (escopo = 'DECK') = (deck_id is not null)),
  constraint flashcard_sessao_contagem check (
    revisados <= errei + dificil + bom + facil)
);
create index flashcard_sessao_idx on flashcard_sessao (usuario_id, iniciada_em desc);
create index flashcard_sessao_dia_idx on flashcard_sessao (usuario_id, finalizada_em)
  where finalizada_em is not null;

create table flashcard_revisao (
  id                  uuid primary key default gen_random_uuid(),
  flashcard_id        uuid not null references flashcard(id),
  usuario_id          uuid not null references usuario(id),
  sessao_id           uuid references flashcard_sessao(id),
  nota                text not null check (nota in ('ERREI','DIFICIL','BOM','FACIL')),
  dias_decorridos     smallint not null,
  estabilidade_ant    real not null,
  estabilidade_nova   real not null,
  dificuldade_ant     real not null,
  dificuldade_nova    real not null,
  intervalo_novo      smallint not null,
  intervalo_minutos   integer not null default 0,
  tempo_ms            integer not null,
  segundos_decorridos integer not null default 0,
  revisado_em         timestamptz not null default now()
);
create index flashcard_revisao_idx on flashcard_revisao (usuario_id, revisado_em desc);
create index flashcard_revisao_card_idx on flashcard_revisao (flashcard_id, revisado_em desc);
create index flashcard_revisao_sessao_idx on flashcard_revisao (sessao_id)
  where sessao_id is not null;
create index flashcard_revisao_dia_idx on flashcard_revisao (usuario_id, revisado_em)
  include (flashcard_id, estabilidade_ant);
```

## 6. API REST

Base `/api/v1`. Listagens paginadas com `?page&size&sort` e envelope `{content, page, size, totalElements}`. Documentação por OpenAPI/springdoc.

| Verbo | Rota | Perfil | Descrição |
| --- | --- | --- | --- |
| POST | `/auth/login` | público | Retorna access + refresh token e o perfil. |
| POST | `/auth/cadastro` | público | Cria estudante ativo. |
| POST | `/auth/senha/esqueci` | público | Sempre 204, independente da existência do e-mail. |
| POST | `/auth/senha/redefinir` | público | Consome token de uso único e grava nova senha. |
| POST | `/auth/refresh`, `/auth/logout` | autenticado | Rotaciona ou revoga o refresh token. |
| GET | `/questoes?disciplina&assunto&banca&ano&tipo&q` | autenticado | Somente publicadas para estudante; todas para professor+. |
| POST | `/estudo/sessoes` | autenticado | Abre sessão (modo, filtros ou caderno) e devolve a série. |
| GET | `/estudo/inicio` | autenticado | Resumo do dia e sessão retomável. |
| GET | `/estudo/sessoes/{id}` | autenticado | Retoma sessão do próprio usuário com série e respostas. |
| PATCH | `/estudo/sessoes/{id}/modo` | autenticado | Alterna Foco e Contexto na sessão. |
| POST | `/estudo/sessoes/{id}/respostas` | autenticado | Registra resposta; devolve gabarito e estatísticas fora do Modo Prova. |
| POST | `/estudo/sessoes/{id}/finalizar` | autenticado | Fecha a sessão e retorna a correção. |
| PUT | `/estudo/questoes/{questaoId}/marcacoes` | autenticado | Atualiza favorita e erro recorrente. |
| GET | `/simulados` | autenticado | Lista simulados disponíveis, agendados e concluídos do estudante. |
| POST | `/simulados/{id}/tentativas` | autenticado | Inicia ou retoma uma tentativa em Modo Prova. |
| POST | `/simulados/{id}/tentativas/{tentativaId}/finalizar` | autenticado | Finaliza, corrige e registra a tentativa. |
| GET | `/simulados/{id}/tentativas/{tentativaId}` | autenticado | Retorna o resultado e a revisão da tentativa finalizada. |
| GET | `/cadernos` | autenticado | Lista automáticos e manuais do próprio usuário. |
| POST/PUT/DELETE | `/cadernos`, `/cadernos/{id}` | autenticado | Cria, altera e exclui logicamente caderno manual. |
| GET/POST | `/cadernos/{id}/questoes` | autenticado | Lista paginada ou adiciona questões publicadas ao caderno. |
| DELETE | `/cadernos/{id}/questoes/{questaoId}` | autenticado | Remove questão de caderno manual. |
| POST | `/flashcards` | autenticado | Cria card; aceita `questaoOrigemId` e `sessaoOrigemId` (criação dentro da sessão, RF-28). Responde 201 com o card já agendado. |
| GET | `/flashcards?q&deck&disciplina&estado&page&size&sort` | autenticado | Somente cards do próprio usuário (RN-07). |
| GET | `/flashcards/{id}` | autenticado | Busca um card do próprio usuário; 404 (não 403), byte a byte igual ao de um id inexistente, para card de outro usuário. |
| PUT/DELETE | `/flashcards/{id}` | autenticado | Edita ou exclui logicamente; 404 (não 403) para card de outro usuário. |
| POST | `/flashcards/{id}/arquivar` | autenticado | Retira o card da fila sem apagá-lo. |
| POST | `/flashcards/{id}/restaurar` | autenticado | Devolve um card arquivado à fila (CA-11). |
| GET | `/flashcards/decks` | autenticado | Painel de decks do próprio usuário (RF-35): para cada deck, `{id, nome, descricao, totalCards, vencendoHoje, novos, limites: {novosDia, revisoesDia, intervaloMaxDias, retencaoAlvo}, limitesRotulo, retencaoRotulo, editavel, aviso}`. Inclui a linha virtual "Sem deck" (`id: null`, `editavel: false`, limites padrão) quando há cards sem deck. `aviso` vem preenchido (`{tipo: "FILA_ACUMULADA", mensagem, vencidos, limiteDiario}`) quando a fila vencida do deck passa do dobro do limite diário de revisões (CA-18); os demais decks recebem `aviso: null`. |
| POST | `/flashcards/decks` | autenticado | Cria deck com nome, descrição e limites (RF-36). Nome duplicado → 409 `NOME_EM_USO`. |
| PUT | `/flashcards/decks/{id}` | autenticado | Renomeia e ajusta limites do deck do próprio usuário; cards e agendamento não são tocados. Nome duplicado → 409 `NOME_EM_USO`; limites fora do intervalo são ajustados ao valor mais próximo permitido (RN-10), não recusados. |
| DELETE | `/flashcards/decks/{id}` | autenticado | Remove o deck e move seus cards para `deck_id null` na mesma transação (RN-11); nenhum card é apagado. Responde `{cardsMovidos}`. |
| POST | `/flashcards/decks/{id}/mover` | autenticado | Move um lote de cards (`{flashcardIds}`) para o deck. |
| POST | `/flashcards/sessoes` | autenticado | Abre sessão de revisão (deck/disciplina, limite diário) e devolve a fila do dia. |
| POST | `/flashcards/sessoes/{id}/revisoes` | autenticado | Registra `{flashcardId, nota, tempoMs}`, aplica o FSRS (RN-08) e devolve estabilidade, dificuldade e `proximaRevisao`. |
| POST | `/flashcards/sessoes/{id}/finalizar` | autenticado | Fecha a sessão e retorna o resumo (RF-34). |
| GET | `/flashcards/resumo` | autenticado | Vencendo hoje / próximos 7 dias, por deck — alimenta o Início e o menu. |
| GET | `/desempenho/resumo`, `/desempenho/evolucao`, `/desempenho/disciplinas`, `/desempenho/pontos-fracos` | autenticado | Resumo, séries semanais e agregados do usuário autenticado. |
| GET | `/editais` | autenticado | Editais agrupados por status (`abertos`, `previstos`, `encerrados`), com as disciplinas exigidas de cada um (RF-17). Resposta com `Cache-Control: no-store`. |
| GET/POST/PUT | `/admin/questoes`, `/admin/questoes/{id}` | professor+ | CRUD editorial com alternativas aninhadas. |
| POST | `/admin/questoes/{id}/enviar-revisao` | professor+ | Envia questão para revisão. |
| POST | `/admin/questoes/{id}/publicar` | admin+ | Valida gabarito, comentário e alternativas. |
| POST | `/admin/questoes/{id}/despublicar` | admin+ | Retira questão publicada do acervo. |
| DELETE | `/admin/questoes/{id}` | professor+ | Exclui questão logicamente. |
| CRUD | `/admin/disciplinas`, `/admin/assuntos`, `/admin/simulados` | professor+ / admin+ | Catálogo e simulados. |
| GET | `/admin/simulados?q&estado&bancaId&page&size` | admin+ | Lista paginada de simulados com `estado` derivado (`RASCUNHO`/`AGENDADO`/`DISPONIVEL`/`ENCERRADO`); `size` máximo 100. (F4 simulados) |
| POST | `/admin/simulados` | admin+ | Cria simulado em `RASCUNHO` com código `SM-###`; 201 com `Location`. (F4) |
| GET/PUT | `/admin/simulados/{id}` | admin+ | Detalhe e edição dos dados gerais; 409 `SIMULADO_EM_USO` com tentativa finalizada. (F4) |
| DELETE | `/admin/simulados/{id}` | admin+ | Exclusão lógica; 409 `SIMULADO_EM_USO` com tentativa finalizada. (F4) |
| PUT | `/admin/simulados/{id}/composicao` | admin+ | Substitui a composição e devolve o resumo (`somaComposicao`, `fechada`, `acervoSuficiente`). (F4) |
| POST | `/admin/simulados/{id}/publicar` | admin+ | Valida, congela a série em `simulado_questao` e publica; 422 `PUBLICACAO_INVALIDA`. (F4) |
| POST | `/admin/simulados/{id}/despublicar` | admin+ | Volta a `RASCUNHO` e apaga a série; 409 `SIMULADO_EM_USO` com tentativa finalizada. (F4) |
| GET | `/admin/editais` | admin+ | Lista administrativa, com `prazoVencido` e contagem de disciplinas por edital (RF-22). |
| GET | `/admin/editais/{id}` | admin+ | Edital completo para o formulário. |
| POST/PUT | `/admin/editais`, `/admin/editais/{id}` | admin+ | Cria ou atualiza o edital (nome, status, vagas, banca, órgão, prazo, URL e disciplinas exigidas). Passa a constar na visão do estudante sem etapa de publicação. |
| DELETE | `/admin/editais/{id}` | admin+ | Exclusão lógica; some da visão do estudante e da lista administrativa. |
| POST | `/admin/assuntos/{id}/arquivar` | professor+ | Arquiva assunto sem remover questões vinculadas. |
| GET | `/admin/bancas` | professor+ | Lista bancas para o formulário de questão. |
| GET | `/admin/orgaos` | professor+ | Lista órgãos para o formulário de questão. |
| CRUD | `/super/usuarios`, `/super/usuarios/{id}` | super-admin | Listar, criar (com convite), editar, excluir logicamente. |
| POST | `/super/usuarios/{id}/redefinir-senha` | super-admin | Dispara link; nenhuma senha é retornada. |
| POST | `/super/usuarios/{id}/reenviar-convite` | super-admin | Reenvia o convite a usuário em `CONVITE_PENDENTE`. |

## 7. Fluxos críticos

| Fluxo | Sequência |
| --- | --- |
| Login | Credenciais → valida e checa status → access (15 min) + refresh (14 dias, cookie `HttpOnly SameSite=Strict`) → front resolve a navegação pelo perfil. |
| Redefinição de senha | Pedido → token de 32 bytes guardado como hash, TTL 60 min → e-mail → POST consome o token e invalida todos os refresh tokens do usuário. |
| Sessão de estudo | Abre sessão com filtros → série de IDs materializada no servidor → respostas uma a uma → finalização calcula o resumo. |
| Modo Prova | Respostas no cliente; `POST /respostas` em lote na finalização, em transação; abandono expira em 24 h. |
| Criação de flashcard na sessão | Botão na barra da questão → diálogo com disciplina/assunto/questão pré-preenchidos → `POST /flashcards` → toast + contador incrementado → foco devolvido ao enunciado. Nada na sessão de questões é remontado; o cronômetro não pausa. |
| Revisão de flashcards | `POST /flashcards/sessoes` monta a fila deck a deck (`proxima_revisao <= current_date`, ordenada por vencimento, cortada em `novos_dia` e `revisoes_dia` de cada deck — RN-10) → frente → verso → nota → o FSRS recalcula estabilidade e dificuldade, deriva o intervalo pela retenção alvo do deck e grava `flashcard_revisao` → finalização devolve o resumo. |
| Convite de usuário | Criação com `CONVITE_PENDENTE` → e-mail de definição de senha → primeira senha ativa a conta. |

## 8. Desempenho e agregados

As telas de desempenho não consultam `resposta` diretamente. Agregados incrementais por usuário/semana, por usuário/disciplina e por usuário/assunto são atualizados na finalização da sessão e reconciliados por job (`@Scheduled`, 15 min). Estatísticas por questão vivem em `questao_estatistica`, atualizada incrementalmente.

Os agregados `desempenho_semanal`, `desempenho_disciplina` e `desempenho_assunto` são atualizados por *upsert* na finalização de uma sessão e reconstruídos pelo reconciliador quando houver pendência. Cada linha é escopada por `usuario_id`; as consultas dos painéis leem somente essas tabelas e nunca percorrem `resposta`.

Outras medidas: paginação por keyset nas listas administrativas, `@EntityGraph` contra N+1 em questão→alternativas, cache de segundo nível só no catálogo, HikariCP em 20 conexões por instância.

## 9. Qualidade, observabilidade e entrega

| Frente | Definição |
| --- | --- |
| Testes backend | JUnit 5 + Mockito no serviço; `@SpringBootTest` com Testcontainers (Postgres real) em repositórios e controllers. Cobertura mínima 70% em `service`. |
| Testes frontend | Vitest para serviços e stores; Playwright para login, sessão de estudo, correção do Modo Prova, criação de flashcard sem perder a sessão, revisão espaçada e CRUD de usuários. O agendador FSRS tem testes unitários próprios contra casos de referência. |
| Observabilidade | Actuator + Micrometer/Prometheus, logs JSON com `traceId`, alertas de 5xx e p95 por rota. |
| Auditoria | Toda ação administrativa grava em `auditoria`. |
| Ambientes | `dev` por Docker Compose (api + web + postgres + mailhog), `staging` e `prod` em containers, Postgres gerenciado com backup diário e PITR. |
| Deploy | Imagem única do backend; frontend em CDN. Flyway no start; migrações retrocompatíveis (expand/contract). |

## 10. Plano de implementação

| Sprint | Backend | Frontend |
| --- | --- | --- |
| 1–2 | Esqueleto, Flyway, usuário, auth JWT, perfis, seed | Shell com sidebar por perfil, login/cadastro, guards, interceptor |
| 3–4 | Catálogo, questões e alternativas, publicação, busca | Admin de questões (lista + formulário), árvore de matérias |
| 5–6 | Sessões, respostas, correção, marcações | Sessão nos três modos e tela de correção |
| 7 | Cadernos e agregados de desempenho | Cadernos, desempenho (gráficos e pontos fracos) |
| 8 | Simulados, tentativas, editais | Simulados do estudante e CRUD administrativo |
| 9 | CRUD de usuários, convites, redefinição, auditoria | Gestão de usuários, toasts, esqueci minha senha |
| 10 | Flashcards: deck com limites, card, FSRS (`flashcard/fsrs` + testes), fila e revisões | Diálogo de criação na sessão, menu Flashcards, tela de decks, revisão e resumo |
| 11 | Endurecimento, carga, observabilidade | Acessibilidade, revisão visual contra o protótipo, E2E |

## 11. Decisões e pendências

| Item | Situação |
| --- | --- |
| Spring Boot 4.1 + Java 25 em vez da linha 3.x | Decidido: a 3.5 encerrou o suporte OSS em junho de 2026. |
| Monólito modular em vez de microsserviços | Decidido: o volume da v1 não justifica operação distribuída. |
| Editais (`prd-editais`): migração aplicada como `V12__editais.sql`, semente como `V907__seed_editais_dev.sql` | Os números `V7`/`V905` previstos na TechSpec da funcionalidade colidiram com migrações de `prd-sessao-de-estudo` e `prd-simulados`, aplicadas antes desta na mesma fase F4. A versão livre foi conferida contra `db/migration` e `db/migration-dev` na entrega; nenhuma migração já aplicada foi editada. |
| Flashcards (`prd-flashcards-criacao-e-gerenciamento`): migração aplicada como `V13__flashcards.sql`, semente como `V908__seed_flashcards_dev.sql` | Os números `V8`/`V906` estimados na TechSpec da funcionalidade colidiram com as migrações de `prd-simulados` e `prd-editais`, aplicadas antes desta na mesma fase F6. A versão livre foi conferida contra `db/migration` e `db/migration-dev` na entrega (tarefa 10.0); nenhuma migração já aplicada foi editada. |
| Flashcards — decks, limites e revisão espaçada (`prd-flashcards-decks-e-revisao`): migração aplicada como `V14__flashcards_revisao.sql`, semente como `V909__seed_revisao_dev.sql` | Os números `V9`/`V906`/`V907` estimados na TechSpec da funcionalidade colidiram com `V13__flashcards.sql` e as sementes `V906__completa_seed_simulados_e2e.sql`/`V907__seed_editais_dev.sql`, já aplicadas na mesma fase F6. A versão livre foi conferida contra `db/migration` e `db/migration-dev` na entrega (tarefa 10.0); nenhuma migração já aplicada foi editada. |
| Editais: pirâmide de testes em ~46% unidade / ~35% integração / ~19% E2E, fora do alvo 70/20/10 de `tests.md` §4 | Aceito como desvio registrado: a funcionalidade é essencialmente um CRUD — autorização por perfil, unicidade de nome, FK da taxonomia e ausência de cache só se provam acima da unidade, e inventar teste de unidade só para atingir a proporção seria o "teste decorativo" que `tests.md` §1 proíbe. Detalhamento em `tasks/prd-editais/techspec.md` § *Conformidade com o AGENTS.md e as rules*. |
| Refresh token em cookie `HttpOnly` | Decidido: menor superfície de XSS que armazenamento local. |
| Agregados materializados em vez de cálculo ao vivo | Decidido: leitura domina e tolera 15 min de atraso. |
| Provedor de e-mail transacional | Pendente até o fim da F1 (SES ou SendGrid). |
| Busca textual `tsvector` vs. motor externo | Começa nativo; reavaliar acima de 200 mil questões. |
| Layout responsivo mobile | Pendente de especificação de design; não bloqueia a v1. |
| **FSRS em vez de SM-2** | Decidido: agendamento por estabilidade/dificuldade com retenção alvo configurável (RN-08). Substitui a SM-2 antes da implementação — nenhuma migração de dados de produção envolvida. |
| Implementação própria do FSRS, sem biblioteca | Decidido: o cálculo são duas fórmulas fechadas + os pesos padrão; ficam em `flashcard/fsrs` com testes de referência sobre casos conhecidos. |
| Pesos padrão, sem otimização por usuário na v1 | Decidido: `flashcard_revisao` guarda o log completo (dias decorridos, estabilidade e dificuldade antes/depois) para permitir a otimização na v2. |
| Limites de agendamento na tabela `deck`, não em tabela de configuração separada | Decidido: a configuração é por deck e sempre lida junto dele; uma tabela à parte só somaria join. |
| Excluir deck preserva os cards | Decidido (RN-11): `on delete set null` em `flashcard.deck_id`, movimento na mesma transação. |
| Criação de flashcard como diálogo, não como rota | Decidido: rota própria destruiria a sessão de estudo em memória e o cronômetro (RF-28). |
| Flashcards privados, sem decks públicos | Decidido para a v1 (RN-07); compartilhamento fica para a v2. |
| Validação de `frente`/`verso` em `RequisicaoFlashcard` feita manualmente em `FlashcardService`, não por Bean Validation | Decidido: a regra exige aparar espaços (`trim`) antes de medir o tamanho, para que `"   "` ou um texto só com espaços de sobra não conte como preenchido nem estoure o limite por espaços à margem; `@Size` do Bean Validation mede a string como veio, sem normalizar. Um `ConstraintValidator` customizado resolveria, mas pagaria a complexidade de mais uma classe para uma regra usada em um único DTO — mantido manual até que outro DTO precise da mesma normalização. |
| Validação de `nome`/`descricao` em `RequisicaoDeck` (`flashcard/deck/ValidadorDeck`) mantida manual, com `@Valid` presente no controller só para o formato da rota | Decidido pelo mesmo motivo do item de `RequisicaoFlashcard` acima: o `trim` precisa vir antes da medida de tamanho, e migrar para `@Size` mudaria o `code`/`message` do erro já coberto por teste (`DadosInvalidosException` vira `MethodArgumentNotValidException` no `ManipuladorErrosGlobal`). Os limites numéricos (`novosDia`, `revisoesDia`, `intervaloMaxDias`, `retencaoAlvo`) não usam Bean Validation porque `LimitesDeck.normalizar()` faz *clamp* para a faixa válida em vez de rejeitar — não é uma regra de rejeição, é normalização de negócio. |
