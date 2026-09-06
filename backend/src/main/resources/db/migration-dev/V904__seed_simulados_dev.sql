insert into simulado (codigo, nome, descricao, banca_id, numero_questoes, duracao_segundos,
  janela_abertura, janela_encerramento, status, publicado_em, criado_por)
select 'SM-001', 'Simulado dev publicado', 'Massa de desenvolvimento.', b.id, 10, 3600,
  now() - interval '1 day', now() + interval '30 days', 'PUBLICADO', now(), u.id
from banca b cross join usuario u
where b.sigla = 'FCC' and u.email = 'admin@dev.certame.local'
  and not exists (select 1 from simulado where codigo = 'SM-001');
insert into simulado (codigo, nome, numero_questoes, duracao_segundos, criado_por)
select 'SM-002', 'Simulado dev em rascunho', 10, 3600, u.id
from usuario u where u.email = 'admin@dev.certame.local'
  and not exists (select 1 from simulado where codigo = 'SM-002');
insert into simulado_composicao (simulado_id, disciplina_id, quantidade)
select s.id, d.id, 10 from simulado s cross join disciplina d
where s.codigo = 'SM-001' and d.nome = 'Direito Constitucional'
  and not exists (select 1 from simulado_composicao c where c.simulado_id = s.id);
insert into simulado_questao (simulado_id, ordem, questao_id)
select s.id, row_number() over (order by q.id), q.id
from simulado s join questao q on q.status = 'PUBLICADA' and q.excluido_em is null
join disciplina d on d.id = q.disciplina_id
where s.codigo = 'SM-001' and d.nome = 'Direito Constitucional'
  and not exists (select 1 from simulado_questao sq where sq.simulado_id = s.id)
limit 10;
