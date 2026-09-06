create table simulado (
  id uuid primary key default gen_random_uuid(), codigo text not null, nome text not null,
  descricao text, banca_id uuid references banca(id), numero_questoes smallint not null
    check (numero_questoes between 10 and 200), duracao_segundos integer not null
    check (duracao_segundos between 600 and 21600), janela_abertura timestamptz,
  janela_encerramento timestamptz, status text not null default 'RASCUNHO'
    check (status in ('RASCUNHO','PUBLICADO','ENCERRADO')), publicado_em timestamptz,
  criado_por uuid references usuario(id), criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now(), excluido_em timestamptz,
  constraint simulado_janela_coerente check (janela_abertura is null or janela_encerramento is null
    or janela_encerramento > janela_abertura),
  constraint simulado_publicado_com_janela check (status <> 'PUBLICADO'
    or (janela_abertura is not null and janela_encerramento is not null))
);
create unique index simulado_codigo_idx on simulado (codigo) where excluido_em is null;
create index simulado_janela_idx on simulado (status, janela_abertura) where excluido_em is null;

create table simulado_composicao (
  simulado_id uuid not null references simulado(id) on delete cascade,
  disciplina_id uuid not null references disciplina(id), quantidade smallint not null check (quantidade >= 1),
  primary key (simulado_id, disciplina_id)
);
create index simulado_composicao_disciplina_idx on simulado_composicao (disciplina_id);

create table simulado_questao (
  simulado_id uuid not null references simulado(id) on delete cascade,
  ordem smallint not null check (ordem >= 1), questao_id uuid not null references questao(id),
  primary key (simulado_id, ordem), unique (simulado_id, questao_id)
);
create index simulado_questao_questao_idx on simulado_questao (questao_id);

create table tentativa_simulado (
  id uuid primary key default gen_random_uuid(), simulado_id uuid not null references simulado(id),
  usuario_id uuid not null references usuario(id), sessao_id uuid not null unique references sessao_estudo(id),
  tipo text not null default 'VALIDA' check (tipo in ('VALIDA','TREINO')),
  status text not null default 'EM_ANDAMENTO' check (status in ('EM_ANDAMENTO','FINALIZADA','ABANDONADA')),
  nota smallint check (nota between 0 and 100), acertos smallint, erros smallint,
  nao_respondidas smallint, tempo_total_segundos integer check (tempo_total_segundos >= 0),
  iniciada_em timestamptz not null default now(), finalizada_em timestamptz,
  constraint tentativa_finalizada_com_nota check (status <> 'FINALIZADA'
    or (nota is not null and finalizada_em is not null))
);
create unique index tentativa_valida_unica_idx on tentativa_simulado (simulado_id, usuario_id)
  where tipo = 'VALIDA' and status = 'FINALIZADA';
create unique index tentativa_aberta_unica_idx on tentativa_simulado (simulado_id, usuario_id)
  where status = 'EM_ANDAMENTO';
create index tentativa_historico_idx on tentativa_simulado (usuario_id, finalizada_em desc)
  where status = 'FINALIZADA';

create or replace function tentativa_simulado_imutavel() returns trigger as $$
begin
  if old.status = 'FINALIZADA' then return old; end if;
  return new;
end;
$$ language plpgsql;
create trigger tentativa_simulado_congelada before update on tentativa_simulado
  for each row execute function tentativa_simulado_imutavel();

alter table sessao_estudo drop constraint if exists sessao_estudo_tamanho_check;
alter table sessao_estudo add constraint sessao_tamanho_valido
  check ((origem = 'SIMULADO' and tamanho between 10 and 200) or
    (origem = 'CADERNO' and tamanho between 1 and 50) or
    (origem not in ('SIMULADO','CADERNO') and tamanho in (10,30,50)));
alter table sessao_estudo add constraint sessao_simulado_com_ref
  check (origem <> 'SIMULADO' or origem_ref is not null);
alter table sessao_questao drop constraint if exists sessao_questao_ordem_check;
alter table sessao_questao add constraint sessao_questao_ordem_valida check (ordem between 1 and 200);
