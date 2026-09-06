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
  ultima_revisao    timestamptz,
  proxima_revisao   date not null default current_date,
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
    (status = 'ARQUIVADO') = (arquivado_em is not null))
);
create index flashcard_lista_idx  on flashcard (usuario_id, criado_em desc)
  where status <> 'EXCLUIDO';
create index flashcard_fila_idx   on flashcard (usuario_id, proxima_revisao)
  where status = 'ATIVO';
create index flashcard_busca_idx  on flashcard using gin (busca);
create index flashcard_deck_idx   on flashcard (deck_id) where status <> 'EXCLUIDO';
create index flashcard_origem_idx on flashcard (sessao_origem_id) where sessao_origem_id is not null;

create table flashcard_revisao (
  id                uuid primary key default gen_random_uuid(),
  flashcard_id      uuid not null references flashcard(id),
  usuario_id        uuid not null references usuario(id),
  nota              text not null check (nota in ('ERREI','DIFICIL','BOM','FACIL')),
  dias_decorridos   smallint not null,
  estabilidade_ant  real not null,
  estabilidade_nova real not null,
  dificuldade_ant   real not null,
  dificuldade_nova  real not null,
  intervalo_novo    smallint not null,
  tempo_ms          integer not null,
  revisado_em       timestamptz not null default now()
);
create index flashcard_revisao_idx on flashcard_revisao (usuario_id, revisado_em desc);
create index flashcard_revisao_card_idx on flashcard_revisao (flashcard_id, revisado_em desc);
