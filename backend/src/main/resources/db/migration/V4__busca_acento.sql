create extension if not exists unaccent;
create or replace function certame_unaccent(text) returns text
language sql immutable parallel safe
as $$ select unaccent('public.unaccent', $1) $$;

drop index if exists questao_busca_idx;
alter table questao drop column busca;
alter table questao add column busca tsvector generated always as
  (to_tsvector('portuguese', certame_unaccent(coalesce(enunciado, '')))) stored;
create index questao_busca_idx on questao using gin (busca);
