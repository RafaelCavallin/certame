create table disciplina (
  id uuid primary key default gen_random_uuid(), nome text not null,
  arquivado_em timestamptz, excluido_em timestamptz,
  criado_em timestamptz not null default now(), atualizado_em timestamptz not null default now()
);
create unique index disciplina_nome_uk on disciplina (lower(nome)) where excluido_em is null;

create table assunto (
  id uuid primary key default gen_random_uuid(), disciplina_id uuid not null references disciplina(id), nome text not null,
  arquivado_em timestamptz, excluido_em timestamptz,
  criado_em timestamptz not null default now(), atualizado_em timestamptz not null default now()
);
create unique index assunto_nome_uk on assunto (disciplina_id, lower(nome)) where excluido_em is null;

create table banca (
  id uuid primary key default gen_random_uuid(), nome text not null, sigla text,
  criado_em timestamptz not null default now()
);
create unique index banca_nome_uk on banca (lower(nome));

create table orgao (
  id uuid primary key default gen_random_uuid(), nome text not null, sigla text,
  criado_em timestamptz not null default now()
);
create unique index orgao_nome_uk on orgao (lower(nome));

create sequence questao_codigo_seq start with 10000;
create table questao (
  id uuid primary key default gen_random_uuid(),
  codigo text not null unique default 'Q-' || lpad(nextval('questao_codigo_seq')::text, 5, '0'),
  tipo text not null check (tipo in ('ME','CE')), enunciado text not null,
  disciplina_id uuid references disciplina(id), assunto_id uuid references assunto(id),
  banca_id uuid references banca(id), orgao_id uuid references orgao(id),
  ano smallint check (ano between 1990 and 2100), dificuldade text check (dificuldade in ('FACIL','MEDIA','DIFICIL')),
  gabarito text, comentario text,
  status text not null default 'RASCUNHO' check (status in ('RASCUNHO','EM_REVISAO','PUBLICADA')),
  autor_id uuid not null references usuario(id), publicado_em timestamptz, excluido_em timestamptz,
  criado_em timestamptz not null default now(), atualizado_em timestamptz not null default now(),
  busca tsvector generated always as (to_tsvector('portuguese', coalesce(enunciado, ''))) stored,
  constraint questao_publicada_completa check (status <> 'PUBLICADA' or
    (disciplina_id is not null and gabarito is not null and comentario is not null and length(btrim(comentario)) > 0))
);
create index questao_busca_idx on questao using gin (busca);
create index questao_codigo_idx on questao (codigo text_pattern_ops) where excluido_em is null;
create index questao_filtro_idx on questao (status, disciplina_id, banca_id, atualizado_em desc) where excluido_em is null;
create index questao_assunto_idx on questao (assunto_id) where excluido_em is null;

create table alternativa (
  id uuid primary key default gen_random_uuid(), questao_id uuid not null references questao(id) on delete cascade,
  letra text not null check (letra in ('A','B','C','D','E')), texto text not null,
  ordem smallint not null check (ordem between 1 and 5), unique (questao_id, letra), unique (questao_id, ordem)
);
create table questao_estatistica (
  questao_id uuid primary key references questao(id) on delete cascade,
  total_respostas integer not null default 0, total_acertos integer not null default 0,
  pct_acerto smallint check (pct_acerto between 0 and 100), atualizado_em timestamptz not null default now()
);
create table auditoria (
  id uuid primary key default gen_random_uuid(), ator_id uuid references usuario(id), acao text not null,
  entidade text not null, entidade_id uuid, payload jsonb, criado_em timestamptz not null default now()
);
create index auditoria_entidade_idx on auditoria (entidade, entidade_id, criado_em desc);
