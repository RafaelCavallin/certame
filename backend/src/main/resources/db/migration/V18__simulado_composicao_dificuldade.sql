alter table simulado_composicao
  drop constraint simulado_composicao_pkey,
  add column id uuid not null default gen_random_uuid(),
  add column dificuldade text check (dificuldade in ('FACIL','MEDIA','DIFICIL'));
alter table simulado_composicao add primary key (id);
create unique index simulado_composicao_disc_dificuldade_idx
  on simulado_composicao (simulado_id, disciplina_id, coalesce(dificuldade, ''));
