alter table usuario add column anonimizado_em timestamptz;

create index usuario_anonimizacao_idx on usuario (excluido_em)
  where excluido_em is not null and anonimizado_em is null;
