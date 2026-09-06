create table sessao_estudo (
  id uuid primary key default gen_random_uuid(),
  usuario_id uuid not null references usuario(id),
  modo text not null check (modo in ('FOCO','CONTEXTO','PROVA')),
  origem text not null default 'LIVRE' check (origem in ('LIVRE','CADERNO','SIMULADO')),
  origem_ref text,
  tamanho smallint not null default 10 check (tamanho in (10,30,50)),
  pausado_ms integer not null default 0 check (pausado_ms >= 0),
  iniciada_em timestamptz not null default now(),
  ultima_atividade_em timestamptz not null default now(),
  finalizada_em timestamptz,
  expirada_em timestamptz,
  constraint sessao_encerramento_unico check (finalizada_em is null or expirada_em is null),
  constraint sessao_caderno_com_ref check (origem <> 'CADERNO' or origem_ref is not null)
);
create index sessao_estudo_retomavel_idx on sessao_estudo (usuario_id, ultima_atividade_em desc)
  where finalizada_em is null and expirada_em is null;
create index sessao_estudo_dia_idx on sessao_estudo (usuario_id, iniciada_em desc);

create table sessao_questao (
  sessao_id uuid not null references sessao_estudo(id) on delete cascade,
  ordem smallint not null check (ordem between 1 and 50),
  questao_id uuid not null references questao(id),
  primary key (sessao_id, ordem),
  unique (sessao_id, questao_id)
);

create table resposta (
  id uuid primary key default gen_random_uuid(),
  sessao_id uuid not null references sessao_estudo(id),
  usuario_id uuid not null references usuario(id),
  questao_id uuid not null references questao(id),
  marcada text not null,
  correta boolean not null,
  tempo_ms integer not null check (tempo_ms >= 0),
  respondida_em timestamptz not null default now(),
  unique (sessao_id, questao_id)
);
create index resposta_usuario_data_idx on resposta (usuario_id, respondida_em desc);
create index resposta_questao_idx on resposta (questao_id);
create rule resposta_sem_update as on update to resposta do instead nothing;
create rule resposta_sem_delete as on delete to resposta do instead nothing;

create table marcacao (
  id uuid primary key default gen_random_uuid(),
  usuario_id uuid not null references usuario(id),
  questao_id uuid not null references questao(id),
  tipo text not null check (tipo in ('FAVORITA','ERRO_RECORRENTE')),
  criado_em timestamptz not null default now(),
  unique (usuario_id, questao_id, tipo)
);
create index marcacao_usuario_idx on marcacao (usuario_id, tipo, criado_em desc);

alter table questao_estatistica add column tempo_medio_ms integer;
