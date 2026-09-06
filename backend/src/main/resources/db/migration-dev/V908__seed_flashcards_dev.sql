-- Seed de flashcards para o estudante de exemplo (perfil dev).
-- Depende de V900 (usuario), V901/V902 (disciplina, assunto, questao, sessao_estudo).

-- decks do estudante
insert into deck (usuario_id, nome, descricao)
select u.id, 'Constitucional', 'Decks de Direito Constitucional'
from usuario u where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from deck d where d.usuario_id = u.id and lower(d.nome) = lower('Constitucional'));

insert into deck (usuario_id, nome, descricao)
select u.id, 'Administrativo', 'Decks de Direito Administrativo'
from usuario u where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from deck d where d.usuario_id = u.id and lower(d.nome) = lower('Administrativo'));

-- helper ids: disciplina/assunto/questao/sessao
-- flashcards: 12 cards do estudante, variando origem, status e fila

-- 1..4: com questao de origem, deck Constitucional
insert into flashcard (usuario_id, deck_id, frente, verso, disciplina_id, assunto_id, questao_origem_id, sessao_origem_id, tags, estado, proxima_revisao)
select u.id, dc.id,
       'Efeitos da decisao de merito em ADI ' || g.n,
       'Eficacia erga omnes e efeito vinculante (art. 102, par. 2, CF) - card ' || g.n,
       q.disciplina_id, q.assunto_id, q.id, s.id,
       array['adi','stf'], 'NOVO', current_date
from usuario u
cross join generate_series(1, 4) g(n)
join deck dc on dc.usuario_id = u.id and lower(dc.nome) = lower('Constitucional')
join lateral (select id, disciplina_id, assunto_id from questao where status = 'PUBLICADA' order by criado_em limit 1) q on true
join lateral (select id from sessao_estudo where usuario_id = u.id order by iniciada_em desc limit 1) s on true
where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard f where f.usuario_id = u.id and f.frente = 'Efeitos da decisao de merito em ADI ' || g.n);

-- 5..8: deck Administrativo, sem sessao (questao fora de sessao)
insert into flashcard (usuario_id, deck_id, frente, verso, disciplina_id, assunto_id, questao_origem_id, tags, estado, proxima_revisao)
select u.id, da.id,
       'Principio da legalidade na Administracao ' || g.n,
       'Vinculacao estrita a lei; excecoes previstas em lei. Card ' || g.n,
       q.disciplina_id, q.assunto_id, q.id,
       array['legalidade','admin'], 'REVISAO', current_date - (g.n % 3)
from usuario u
cross join generate_series(5, 8) g(n)
join deck da on da.usuario_id = u.id and lower(da.nome) = lower('Administrativo')
join lateral (select id, disciplina_id, assunto_id from questao where disciplina_id = (select id from disciplina where nome = 'Direito Administrativo' limit 1) order by criado_em limit 1) q on true
where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard f where f.usuario_id = u.id and f.frente = 'Principio da legalidade na Administracao ' || g.n);

-- 9: avulso sem deck, sem origem
insert into flashcard (usuario_id, frente, verso, disciplina_id, assunto_id, tags, estado, proxima_revisao)
select u.id,
       'O que e controle concentrado?',
       'Controle abstrato exercido pelo STF via ADI, ADC, ADPF.',
       d.id, a.id, array['avulso','controle'], 'NOVO', current_date
from usuario u, disciplina d, assunto a
where u.email = 'estudante@dev.certame.local'
  and d.nome = 'Direito Constitucional' and a.nome = 'Controle de constitucionalidade'
  and not exists (select 1 from flashcard f where f.usuario_id = u.id and f.frente = 'O que e controle concentrado?');

-- 10: arquivado (CA-11)
insert into flashcard (usuario_id, deck_id, frente, verso, status, arquivado_em, estado, proxima_revisao)
select u.id, dc.id,
       'Card arquivado de exemplo',
       'Este card esta arquivado e nao aparece na fila.',
       'ARQUIVADO', now(), 'REVISAO', current_date
from usuario u join deck dc on dc.usuario_id = u.id and lower(dc.nome) = lower('Constitucional')
where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard f where f.usuario_id = u.id and f.frente = 'Card arquivado de exemplo');

-- 11: excluido logicamente (CA-12)
insert into flashcard (usuario_id, deck_id, frente, verso, status, excluido_em, estado, proxima_revisao)
select u.id, da.id,
       'Card excluido de exemplo',
       'Este card foi excluido logicamente.',
       'EXCLUIDO', now(), 'NOVO', current_date
from usuario u join deck da on da.usuario_id = u.id and lower(da.nome) = lower('Administrativo')
where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard f where f.usuario_id = u.id and f.frente = 'Card excluido de exemplo');

-- 12: card com historico para CA-12 (sera usado para flashcard_revisao)
insert into flashcard (id, usuario_id, deck_id, frente, verso, tags, estado, proxima_revisao)
select 'a0000000-0000-0000-0000-000000000012'::uuid, u.id, dc.id,
       'Card com historico de revisao',
       'Card que possui duas revisoes registradas.',
       array['historico'], 'REVISAO', current_date - 1
from usuario u join deck dc on dc.usuario_id = u.id and lower(dc.nome) = lower('Constitucional')
where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from flashcard f where f.id = 'a0000000-0000-0000-0000-000000000012'::uuid);

insert into flashcard_revisao (flashcard_id, usuario_id, nota, dias_decorridos, estabilidade_ant, estabilidade_nova, dificuldade_ant, dificuldade_nova, intervalo_novo, tempo_ms, revisado_em)
select f.id, f.usuario_id, 'BOM', 3, 1.2, 2.1, 5.0, 4.8, 5, 12000, now() - interval '2 days'
from flashcard f join usuario u on u.id = f.usuario_id
where u.email = 'estudante@dev.certame.local' and f.frente = 'Card com historico de revisao'
  and not exists (select 1 from flashcard_revisao r where r.flashcard_id = f.id and r.nota = 'BOM' and r.dias_decorridos = 3);

insert into flashcard_revisao (flashcard_id, usuario_id, nota, dias_decorridos, estabilidade_ant, estabilidade_nova, dificuldade_ant, dificuldade_nova, intervalo_novo, tempo_ms, revisado_em)
select f.id, f.usuario_id, 'FACIL', 5, 2.1, 3.5, 4.8, 4.5, 12, 8000, now() - interval '1 day'
from flashcard f join usuario u on u.id = f.usuario_id
where u.email = 'estudante@dev.certame.local' and f.frente = 'Card com historico de revisao'
  and not exists (select 1 from flashcard_revisao r where r.flashcard_id = f.id and r.nota = 'FACIL' and r.dias_decorridos = 5);
