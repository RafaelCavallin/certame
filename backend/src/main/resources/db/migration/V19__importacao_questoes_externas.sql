create table texto_base (
  id uuid primary key default gen_random_uuid(),
  fonte text not null check (fonte in ('INTERNA','QAPI','QUEST_API','API_DAS_QUESTOES')),
  identificador_externo text, titulo text, conteudo text not null, referencia text,
  criado_em timestamptz not null default now()
);
create unique index texto_base_externo_uk on texto_base (fonte, identificador_externo)
  where identificador_externo is not null;

alter table questao add column fonte text not null default 'INTERNA'
  check (fonte in ('INTERNA','QAPI','QUEST_API','API_DAS_QUESTOES'));
alter table questao add column identificador_externo text;
alter table questao add column url_externa text;
alter table questao add column importado_em timestamptz;
alter table questao add column verificado_em timestamptz;
alter table questao add column hash_externo text;
alter table questao add column lote_importacao_id uuid;
alter table questao add column texto_base_id uuid references texto_base(id);
create unique index questao_fonte_externa_uk on questao (fonte, identificador_externo)
  where identificador_externo is not null;

-- A normalizacao (minusculas, sem acento, sem pontuacao, espacos colapsados) tem uma unica
-- implementacao, em SQL, usada tanto pela coluna gerada quanto pela consulta de colisao (ADR-6).
create or replace function certame_enunciado_normalizado(entrada text) returns text
language sql immutable parallel safe as $$
  select btrim(regexp_replace(
           regexp_replace(lower(certame_unaccent(coalesce(entrada, ''))), '[^a-z0-9]+', ' ', 'g'),
           '\s+', ' ', 'g'))
$$;

create or replace function certame_hash_enunciado(entrada text) returns text
language sql immutable parallel safe as $$
  select encode(sha256(certame_enunciado_normalizado(entrada)::bytea), 'hex')
$$;

alter table questao add column hash_enunciado text
  generated always as (certame_hash_enunciado(enunciado)) stored;
create index questao_hash_enunciado_idx on questao (hash_enunciado);

create table mapeamento_taxonomia (
  id uuid primary key default gen_random_uuid(),
  fonte text not null check (fonte in ('QAPI','QUEST_API','API_DAS_QUESTOES')),
  tipo text not null check (tipo in ('DISCIPLINA','ASSUNTO','BANCA','ORGAO')),
  valor_externo text not null, valor_externo_original text not null, valor_externo_pai text,
  disciplina_id uuid references disciplina(id), assunto_id uuid references assunto(id),
  banca_id uuid references banca(id), orgao_id uuid references orgao(id),
  criado_por uuid not null references usuario(id),
  criado_em timestamptz not null default now(), atualizado_em timestamptz not null default now(),
  constraint mapeamento_alvo_unico check (
    (case when disciplina_id is not null then 1 else 0 end)
  + (case when assunto_id  is not null then 1 else 0 end)
  + (case when banca_id    is not null then 1 else 0 end)
  + (case when orgao_id    is not null then 1 else 0 end) = 1)
);
create unique index mapeamento_taxonomia_uk
  on mapeamento_taxonomia (fonte, tipo, valor_externo, coalesce(valor_externo_pai, ''));

create sequence lote_importacao_codigo_seq start with 1;
create table lote_importacao (
  id uuid primary key default gen_random_uuid(),
  codigo text not null unique,
  fonte text not null check (fonte in ('QAPI','QUEST_API','API_DAS_QUESTOES')),
  filtros jsonb not null,
  quantidade_pedida smallint not null check (quantidade_pedida between 1 and 50),
  status text not null check (status in ('EXECUTANDO','CONCLUIDO','PARCIAL','FALHOU')),
  motivo_interrupcao text, somente_simulacao boolean not null default false,
  total_recuperado integer not null default 0, total_processado integer not null default 0,
  total_importado integer not null default 0, chamadas_externas integer not null default 0,
  solicitante_id uuid not null references usuario(id),
  iniciado_em timestamptz not null default now(), finalizado_em timestamptz
);
create index lote_importacao_historico_idx on lote_importacao (fonte, status, iniciado_em desc);

create table item_importacao (
  id uuid primary key default gen_random_uuid(),
  lote_id uuid not null references lote_importacao(id) on delete cascade,
  ordem integer not null, identificador_externo text not null,
  resultado text not null check (resultado in ('IMPORTADA','IMPORTADA_COM_PENDENCIA','JA_EXISTENTE',
    'DUPLICADA_NO_LOTE','SEM_MAPEAMENTO','TIPO_NAO_SUPORTADO','RECURSO_NAO_SUPORTADO','INCOMPLETA',
    'ALTERACAO_EXTERNA','ERRO')),
  motivo text not null, pendencias jsonb not null default '[]'::jsonb,
  valor_externo_nao_mapeado text, tipo_taxonomia_nao_mapeada text,
  questao_id uuid references questao(id), criado_em timestamptz not null default now()
);
create index item_importacao_lote_idx on item_importacao (lote_id, ordem);
create index item_importacao_externo_idx on item_importacao (identificador_externo);
create index item_importacao_pendencia_idx on item_importacao (tipo_taxonomia_nao_mapeada, valor_externo_nao_mapeado)
  where valor_externo_nao_mapeado is not null;

create table divergencia_externa (
  id uuid primary key default gen_random_uuid(),
  questao_id uuid not null references questao(id),
  lote_id uuid references lote_importacao(id),
  hash_anterior text not null, hash_novo text not null, conteudo_externo jsonb not null,
  situacao text not null check (situacao in ('PENDENTE','APLICADA','IGNORADA')),
  prioridade text not null default 'NORMAL' check (prioridade in ('NORMAL','ALTA')),
  detectada_em timestamptz not null default now(),
  decidida_em timestamptz, decidido_por uuid references usuario(id)
);
create unique index divergencia_pendente_uk on divergencia_externa (questao_id) where situacao = 'PENDENTE';
