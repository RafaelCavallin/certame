create table caderno (
  id uuid primary key default gen_random_uuid(),
  usuario_id uuid not null references usuario(id),
  nome text not null,
  descricao text,
  excluido_em timestamptz,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),
  constraint caderno_nome_ck check (length(btrim(nome)) between 3 and 80)
);
create unique index caderno_nome_uk on caderno (usuario_id, lower(nome)) where excluido_em is null;
create index caderno_usuario_idx on caderno (usuario_id, atualizado_em desc) where excluido_em is null;

create table caderno_questao (
  caderno_id uuid not null references caderno(id) on delete cascade,
  questao_id uuid not null references questao(id),
  adicionada_em timestamptz not null default now(),
  primary key (caderno_id, questao_id)
);
create index caderno_questao_questao_idx on caderno_questao (questao_id);

create table desempenho_semanal (
  usuario_id uuid not null references usuario(id) on delete cascade,
  semana date not null,
  volume integer not null default 0 check (volume >= 0),
  acertos integer not null default 0 check (acertos >= 0),
  pct_acerto smallint check (pct_acerto between 0 and 100),
  atualizado_em timestamptz not null default now(),
  primary key (usuario_id, semana),
  constraint desempenho_semanal_acertos_ck check (acertos <= volume)
);

create table desempenho_disciplina (
  usuario_id uuid not null references usuario(id) on delete cascade,
  disciplina_id uuid not null references disciplina(id),
  volume integer not null default 0 check (volume >= 0),
  acertos integer not null default 0 check (acertos >= 0),
  pct_acerto smallint check (pct_acerto between 0 and 100),
  atualizado_em timestamptz not null default now(),
  primary key (usuario_id, disciplina_id),
  constraint desempenho_disciplina_acertos_ck check (acertos <= volume)
);
create index desempenho_disciplina_volume_idx on desempenho_disciplina (usuario_id, volume desc);

create table desempenho_assunto (
  usuario_id uuid not null references usuario(id) on delete cascade,
  assunto_id uuid not null references assunto(id),
  disciplina_id uuid not null references disciplina(id),
  volume integer not null default 0 check (volume >= 0),
  acertos integer not null default 0 check (acertos >= 0),
  pct_acerto smallint check (pct_acerto between 0 and 100),
  atualizado_em timestamptz not null default now(),
  primary key (usuario_id, assunto_id),
  constraint desempenho_assunto_acertos_ck check (acertos <= volume)
);
create index desempenho_assunto_fraco_idx on desempenho_assunto (usuario_id, volume desc)
  where volume >= 20;

create table desempenho_pendencia (
  usuario_id uuid primary key references usuario(id) on delete cascade,
  marcado_em timestamptz not null default now()
);

do $$
begin
  if to_regclass('resposta') is not null then
    create index if not exists resposta_usuario_questao_idx on resposta (usuario_id, questao_id);
  end if;
end $$;
