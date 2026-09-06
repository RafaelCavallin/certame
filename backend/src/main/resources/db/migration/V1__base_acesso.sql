create extension if not exists pgcrypto;

create table usuario (
  id            uuid primary key default gen_random_uuid(),
  nome          text not null,
  email         text not null,
  senha_hash    text,
  perfil        text not null
                check (perfil in ('ESTUDANTE','PROFESSOR','ADMIN','SUPER_ADMIN')),
  status        text not null default 'ATIVO'
                check (status in ('ATIVO','INATIVO','CONVITE_PENDENTE')),
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),
  excluido_em   timestamptz,
  check ((status = 'CONVITE_PENDENTE' and senha_hash is null)
         or (status <> 'CONVITE_PENDENTE' and senha_hash is not null))
);

create unique index usuario_email_uk on usuario (email) where excluido_em is null;

create table refresh_token (
  id          uuid primary key default gen_random_uuid(),
  usuario_id  uuid not null references usuario(id),
  token_hash  text not null unique,
  expira_em   timestamptz not null,
  usado_em    timestamptz,
  revogado_em timestamptz,
  criado_em   timestamptz not null default now(),
  ip          inet,
  user_agent  text
);

create index refresh_token_ativo_idx on refresh_token (usuario_id)
  where usado_em is null and revogado_em is null;

create table token_senha (
  id         uuid primary key default gen_random_uuid(),
  usuario_id uuid not null references usuario(id),
  token_hash text not null unique,
  finalidade text not null check (finalidade in ('REDEFINICAO','CONVITE')),
  expira_em  timestamptz not null,
  usado_em   timestamptz,
  criado_em  timestamptz not null default now()
);

create index token_senha_usuario_idx on token_senha (usuario_id) where usado_em is null;
