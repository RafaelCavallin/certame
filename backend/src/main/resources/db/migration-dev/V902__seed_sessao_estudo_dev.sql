insert into disciplina (nome)
select 'Direito Constitucional'
where not exists (select 1 from disciplina where nome = 'Direito Constitucional');

insert into assunto (disciplina_id, nome)
select d.id, 'Controle de constitucionalidade'
from disciplina d
where d.nome = 'Direito Constitucional'
  and not exists (select 1 from assunto a where a.disciplina_id = d.id and a.nome = 'Controle de constitucionalidade');

insert into assunto (disciplina_id, nome)
select d.id, 'Direitos fundamentais'
from disciplina d
where d.nome = 'Direito Constitucional'
  and not exists (select 1 from assunto a where a.disciplina_id = d.id and a.nome = 'Direitos fundamentais');

insert into questao (tipo, enunciado, disciplina_id, assunto_id, ano, dificuldade, gabarito, comentario, status, autor_id, publicado_em)
select case when n % 5 = 0 then 'CE' else 'ME' end,
       'Questao de desenvolvimento da sessao de estudo ' || n,
       d.id, a.id, 2026, 'MEDIA', case when n % 5 = 0 then 'CERTO' else 'A' end,
       'Comentario da questao de desenvolvimento ' || n, 'PUBLICADA', u.id, now()
from generate_series(1, 60) n
cross join (select id from disciplina where nome = 'Direito Constitucional' limit 1) d
cross join (select id from assunto where nome = 'Controle de constitucionalidade' limit 1) a
cross join (select id from usuario where email = 'professor@dev.certame.local' limit 1) u
where not exists (select 1 from questao q where q.enunciado = 'Questao de desenvolvimento da sessao de estudo ' || n);

insert into questao (tipo, enunciado, disciplina_id, assunto_id, status, autor_id)
select 'ME', 'Questao nao publicada da sessao ' || n, d.id, a.id, case when n = 1 then 'RASCUNHO' else 'EM_REVISAO' end, u.id
from generate_series(1, 3) n
cross join (select id from disciplina where nome = 'Direito Constitucional' limit 1) d
cross join (select id from assunto where nome = 'Direitos fundamentais' limit 1) a
cross join (select id from usuario where email = 'professor@dev.certame.local' limit 1) u
where not exists (select 1 from questao q where q.enunciado = 'Questao nao publicada da sessao ' || n);

insert into sessao_estudo (usuario_id, modo, origem, tamanho, iniciada_em, ultima_atividade_em)
select u.id, 'FOCO', 'LIVRE', 40, now() - interval '30 minutes', now() - interval '5 minutes'
from usuario u
where u.email = 'estudante@dev.certame.local'
  and not exists (select 1 from sessao_estudo s where s.usuario_id = u.id and s.tamanho = 40 and s.finalizada_em is null);

insert into sessao_questao (sessao_id, ordem, questao_id)
select s.id, row_number() over (order by q.id), q.id
from sessao_estudo s
join questao q on q.status = 'PUBLICADA' and q.excluido_em is null
where s.usuario_id = (select id from usuario where email = 'estudante@dev.certame.local')
  and s.finalizada_em is null and s.tamanho = 40
  and not exists (select 1 from sessao_questao sq where sq.sessao_id = s.id)
limit 40;

insert into resposta (sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms, respondida_em)
select sq.sessao_id, s.usuario_id, sq.questao_id,
       case when q.tipo = 'CE' then 'CERTO' else 'A' end,
       case when q.tipo = 'CE' then q.gabarito = 'CERTO' else q.gabarito = 'A' end,
       30000, now() - interval '30 minutes' + (sq.ordem * interval '1 minute')
from sessao_questao sq
join sessao_estudo s on s.id = sq.sessao_id
join questao q on q.id = sq.questao_id
where s.usuario_id = (select id from usuario where email = 'estudante@dev.certame.local')
  and s.finalizada_em is null and s.tamanho = 40
  and sq.ordem <= 14
  and not exists (select 1 from resposta r where r.sessao_id = sq.sessao_id and r.questao_id = sq.questao_id);
