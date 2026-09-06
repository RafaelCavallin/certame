-- Seed de sessoes e revisoes de flashcards para o estudante de exemplo (perfil dev).
-- Depende de V900 (usuario) e V908 (deck Constitucional/Administrativo, flashcard).

-- deck de exemplo com fila vencida acima do dobro do limite diario (CA-18)
insert into deck (usuario_id, nome, descricao, revisoes_dia)
select u.id, 'Revisao Acumulada', 'Deck de exemplo com fila vencida acima do limite diario', 5
from usuario u where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from deck d where d.usuario_id = u.id and lower(d.nome) = lower('Revisao Acumulada'));

insert into flashcard (usuario_id, deck_id, frente, verso, estado, proxima_revisao_em, proxima_revisao)
select u.id, d.id,
       'Card vencido de exemplo ' || g.n,
       'Card de exemplo para ilustrar fila vencida acima do limite do deck - card ' || g.n,
       'REVISAO',
       now() - interval '3 days',
       ((now() - interval '3 days') at time zone 'America/Sao_Paulo')::date
from usuario u
cross join generate_series(1, 12) g(n)
join deck d on d.usuario_id = u.id and lower(d.nome) = lower('Revisao Acumulada')
where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard f where f.usuario_id = u.id and f.frente = 'Card vencido de exemplo ' || g.n);

-- dois cards dedicados ao historico de sessoes finalizadas
-- proxima_revisao_em/proxima_revisao calculados na zona de Brasilia para nao violar flashcard_agenda_coerente
insert into flashcard (id, usuario_id, deck_id, frente, verso, estado, proxima_revisao_em, proxima_revisao)
select 'a0000000-0000-0000-0000-000000000021'::uuid, u.id, dc.id,
       'Card de sessao finalizada de ontem',
       'Card revisado numa sessao ja finalizada, de ontem.',
       'REVISAO',
       date_trunc('day', now() at time zone 'America/Sao_Paulo') at time zone 'America/Sao_Paulo',
       date_trunc('day', now() at time zone 'America/Sao_Paulo')::date
from usuario u join deck dc on dc.usuario_id = u.id and lower(dc.nome) = lower('Constitucional')
where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard f where f.id = 'a0000000-0000-0000-0000-000000000021'::uuid);

insert into flashcard (id, usuario_id, deck_id, frente, verso, estado, proxima_revisao_em, proxima_revisao)
select 'a0000000-0000-0000-0000-000000000022'::uuid, u.id, da.id,
       'Card de sessao finalizada de hoje',
       'Card revisado numa sessao ja finalizada, de hoje.',
       'REVISAO',
       date_trunc('day', now() at time zone 'America/Sao_Paulo') at time zone 'America/Sao_Paulo',
       date_trunc('day', now() at time zone 'America/Sao_Paulo')::date
from usuario u join deck da on da.usuario_id = u.id and lower(da.nome) = lower('Administrativo')
where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard f where f.id = 'a0000000-0000-0000-0000-000000000022'::uuid);

-- sessao finalizada de ontem, ligada a revisao por sessao_id
insert into flashcard_sessao (id, usuario_id, escopo, iniciada_em, finalizada_em, revisados, errei, dificil, bom, facil, tempo_ms)
select 'b0000000-0000-0000-0000-000000000001'::uuid, u.id, 'TODOS',
       now() - interval '1 day', now() - interval '1 day' + interval '10 minutes',
       1, 0, 0, 1, 0, 15000
from usuario u where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard_sessao s where s.id = 'b0000000-0000-0000-0000-000000000001'::uuid);

insert into flashcard_revisao (flashcard_id, usuario_id, sessao_id, nota, dias_decorridos, estabilidade_ant, estabilidade_nova, dificuldade_ant, dificuldade_nova, intervalo_novo, intervalo_minutos, tempo_ms, segundos_decorridos, revisado_em)
select f.id, f.usuario_id, 'b0000000-0000-0000-0000-000000000001'::uuid, 'BOM', 2, 1.5, 2.4, 5.2, 5.0, 4, 0, 15000, 15, now() - interval '1 day' + interval '9 minutes'
from flashcard f join usuario u on u.id = f.usuario_id
where u.email = 'estudante@dev.certame.local' and f.id = 'a0000000-0000-0000-0000-000000000021'::uuid
  and not exists (select 1 from flashcard_revisao r where r.sessao_id = 'b0000000-0000-0000-0000-000000000001'::uuid);

-- sessao finalizada de hoje, ligada a revisao por sessao_id
insert into flashcard_sessao (id, usuario_id, escopo, iniciada_em, finalizada_em, revisados, errei, dificil, bom, facil, tempo_ms)
select 'b0000000-0000-0000-0000-000000000002'::uuid, u.id, 'TODOS',
       now() - interval '30 minutes', now() - interval '20 minutes',
       1, 1, 0, 0, 0, 9000
from usuario u where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard_sessao s where s.id = 'b0000000-0000-0000-0000-000000000002'::uuid);

insert into flashcard_revisao (flashcard_id, usuario_id, sessao_id, nota, dias_decorridos, estabilidade_ant, estabilidade_nova, dificuldade_ant, dificuldade_nova, intervalo_novo, intervalo_minutos, tempo_ms, segundos_decorridos, revisado_em)
select f.id, f.usuario_id, 'b0000000-0000-0000-0000-000000000002'::uuid, 'ERREI', 1, 2.4, 1.1, 5.0, 6.3, 0, 10, 9000, 9, now() - interval '25 minutes'
from flashcard f join usuario u on u.id = f.usuario_id
where u.email = 'estudante@dev.certame.local' and f.id = 'a0000000-0000-0000-0000-000000000022'::uuid
  and not exists (select 1 from flashcard_revisao r where r.sessao_id = 'b0000000-0000-0000-0000-000000000002'::uuid);
