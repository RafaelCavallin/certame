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
