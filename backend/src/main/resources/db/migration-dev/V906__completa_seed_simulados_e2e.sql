insert into simulado (codigo, nome, descricao, banca_id, numero_questoes, duracao_segundos,
  janela_abertura, janela_encerramento, status, publicado_em, criado_por)
select 'SM-003', 'Simulado dev concluído', 'Histórico de desenvolvimento.', b.id, 10, 3600,
  now() - interval '30 days', now() - interval '1 day', 'PUBLICADO', now() - interval '30 days', u.id
from banca b cross join usuario u
where b.sigla = 'FCC' and u.email = 'admin@dev.certame.local'
  and not exists (select 1 from simulado where codigo = 'SM-003');

insert into simulado (codigo, nome, descricao, banca_id, numero_questoes, duracao_segundos,
  janela_abertura, janela_encerramento, status, publicado_em, criado_por)
select 'SM-004', 'Simulado dev agendado', 'Próxima prova de desenvolvimento.', b.id, 10, 3600,
  now() + interval '1 day', now() + interval '30 days', 'PUBLICADO', now(), u.id
from banca b cross join usuario u
where b.sigla = 'FCC' and u.email = 'admin@dev.certame.local'
  and not exists (select 1 from simulado where codigo = 'SM-004');

insert into simulado_composicao (simulado_id, disciplina_id, quantidade)
select s.id, d.id, 10
from simulado s cross join disciplina d
where s.codigo in ('SM-003', 'SM-004') and d.nome = 'Direito Constitucional'
  and not exists (select 1 from simulado_composicao c where c.simulado_id = s.id);

insert into simulado_questao (simulado_id, ordem, questao_id)
select s.id, row_number() over (partition by s.id order by q.id), q.id
from simulado s join questao q on q.status = 'PUBLICADA' and q.excluido_em is null
join disciplina d on d.id = q.disciplina_id
where s.codigo in ('SM-003', 'SM-004') and d.nome = 'Direito Constitucional'
  and not exists (select 1 from simulado_questao sq where sq.simulado_id = s.id)
limit 20;

insert into sessao_estudo (usuario_id, modo, origem, origem_ref, tamanho,
  iniciada_em, ultima_atividade_em, finalizada_em)
select u.id, 'PROVA', 'SIMULADO', s.id::text, 10,
  now() - interval '2 days', now() - interval '2 days', now() - interval '2 days'
from usuario u cross join simulado s
where u.email = 'estudante@dev.certame.local' and s.codigo = 'SM-003'
  and not exists (select 1 from sessao_estudo x where x.origem = 'SIMULADO' and x.origem_ref = s.id::text);

insert into sessao_questao (sessao_id, ordem, questao_id)
select se.id, sq.ordem, sq.questao_id
from sessao_estudo se join simulado s on s.id::text = se.origem_ref
join simulado_questao sq on sq.simulado_id = s.id
where s.codigo = 'SM-003'
  and not exists (select 1 from sessao_questao x where x.sessao_id = se.id);

insert into resposta (sessao_id, usuario_id, questao_id, marcada, correta, tempo_ms, respondida_em)
select se.id, se.usuario_id, sq.questao_id, q.gabarito, true, 1000, se.finalizada_em
from sessao_estudo se join simulado s on s.id::text = se.origem_ref
join sessao_questao sq on sq.sessao_id = se.id
join questao q on q.id = sq.questao_id
where s.codigo = 'SM-003'
  and not exists (select 1 from resposta r where r.sessao_id = se.id);

insert into tentativa_simulado (simulado_id, usuario_id, sessao_id, tipo, status,
  nota, acertos, erros, nao_respondidas, tempo_total_segundos, iniciada_em, finalizada_em)
select s.id, se.usuario_id, se.id, 'VALIDA', 'FINALIZADA', 100, 10, 0, 0, 10,
  se.iniciada_em, se.finalizada_em
from simulado s join sessao_estudo se on se.origem_ref = s.id::text
where s.codigo = 'SM-003'
  and not exists (select 1 from tentativa_simulado t where t.sessao_id = se.id);

select setval(
  'simulado_codigo_seq',
  greatest(coalesce((select max(substring(codigo from 4)::bigint) from simulado), 1), 1),
  true
);
