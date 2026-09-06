create table flashcard_sessao (
  id            uuid primary key default gen_random_uuid(),
  usuario_id    uuid not null references usuario(id),
  escopo        text not null check (escopo in ('TODOS','DECK','SEM_DECK')),
  deck_id       uuid references deck(id) on delete set null,
  disciplina_id uuid references disciplina(id),
  iniciada_em   timestamptz not null default now(),
  finalizada_em timestamptz,
  revisados     smallint not null default 0,
  errei         smallint not null default 0,
  dificil       smallint not null default 0,
  bom           smallint not null default 0,
  facil         smallint not null default 0,
  tempo_ms      integer  not null default 0,
  constraint flashcard_sessao_escopo_coerente check (
    (escopo = 'DECK') = (deck_id is not null)),
  constraint flashcard_sessao_contagem check (
    revisados <= errei + dificil + bom + facil)
);
create index flashcard_sessao_idx on flashcard_sessao (usuario_id, iniciada_em desc);
create index flashcard_sessao_dia_idx on flashcard_sessao (usuario_id, finalizada_em)
  where finalizada_em is not null;

alter table flashcard
  alter column proxima_revisao set default (now() at time zone 'America/Sao_Paulo')::date,
  add column proxima_revisao_em timestamptz,
  add column passo_aprendizado smallint not null default 0
      check (passo_aprendizado between 0 and 10);

update flashcard
   set proxima_revisao_em = proxima_revisao::timestamp at time zone 'America/Sao_Paulo'
 where proxima_revisao_em is null;

alter table flashcard alter column proxima_revisao_em set not null;

-- V906/seed de flashcards ja aplicada so grava proxima_revisao (sem saber da coluna nova); o gatilho
-- preenche proxima_revisao_em a partir dela quando o autor da linha nao informa a coluna nova, sem
-- jamais editar aquela migracao. Escrita nova (Flashcard.agendarPara) sempre informa as duas colunas,
-- entao o gatilho nao interfere.
create or replace function flashcard_preencher_proxima_revisao_em() returns trigger as $$
begin
  if new.proxima_revisao_em is null then
    new.proxima_revisao_em := new.proxima_revisao::timestamp at time zone 'America/Sao_Paulo';
  end if;
  return new;
end;
$$ language plpgsql;
create trigger flashcard_preenche_agenda before insert on flashcard
  for each row execute function flashcard_preencher_proxima_revisao_em();

alter table flashcard add constraint flashcard_agenda_coerente check (
  proxima_revisao = (proxima_revisao_em at time zone 'America/Sao_Paulo')::date);

create index flashcard_fila_hora_idx on flashcard (usuario_id, proxima_revisao_em)
  where status = 'ATIVO';

alter table flashcard_revisao
  add column sessao_id           uuid references flashcard_sessao(id),
  add column intervalo_minutos   integer not null default 0,
  add column segundos_decorridos integer not null default 0;

create index flashcard_revisao_sessao_idx on flashcard_revisao (sessao_id)
  where sessao_id is not null;
create index flashcard_revisao_dia_idx on flashcard_revisao (usuario_id, revisado_em)
  include (flashcard_id, estabilidade_ant);
