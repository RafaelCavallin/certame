insert into edital (id, nome, status, vagas, banca_id, orgao_id, prazo_inscricao, url_oficial)
select gen_random_uuid(), 'PF — Agente de Polícia Federal', 'ABERTO', 1500, b.id, o.id,
       current_date + interval '30 days', 'https://www.cebraspe.org.br/concursos/pf_25_agente'
from banca b, orgao o where b.sigla = 'CEBRASPE' and o.sigla = 'PF';

insert into edital (id, nome, status, vagas, banca_id, orgao_id, prazo_inscricao, url_oficial)
select gen_random_uuid(), 'TCU — Auditor Federal de Controle Externo', 'ABERTO', 60, b.id, o.id,
       current_date - interval '10 days', 'https://www.cebraspe.org.br/concursos/tcu_25_auditor'
from banca b, orgao o where b.sigla = 'CEBRASPE' and o.sigla = 'TCU';

insert into edital (id, nome, status, banca_id)
select gen_random_uuid(), 'SEFAZ-RS — Auditor Fiscal', 'PREVISTO', b.id
from banca b where b.sigla = 'FGV';

insert into edital (id, nome, status, vagas, banca_id, orgao_id, prazo_inscricao, url_oficial)
select gen_random_uuid(), 'INSS — Técnico do Seguro Social', 'ENCERRADO', 1000, b.id, o.id,
       current_date - interval '180 days', 'https://www.fcc.org.br/concursos/inss_24_tecnico'
from banca b, orgao o where b.sigla = 'FCC' and o.sigla = 'INSS';

insert into edital_disciplina (edital_id, disciplina_id)
select e.id, d.id from edital e, disciplina d
where e.nome = 'PF — Agente de Polícia Federal' and d.nome in ('Direito Administrativo', 'Direito Constitucional', 'Língua Portuguesa');

insert into edital_disciplina (edital_id, disciplina_id)
select e.id, d.id from edital e, disciplina d
where e.nome = 'TCU — Auditor Federal de Controle Externo' and d.nome in ('Direito Administrativo');

insert into edital_disciplina (edital_id, disciplina_id)
select e.id, d.id from edital e, disciplina d
where e.nome = 'SEFAZ-RS — Auditor Fiscal' and d.nome in ('Direito Constitucional');

insert into edital_disciplina (edital_id, disciplina_id)
select e.id, d.id from edital e, disciplina d
where e.nome = 'INSS — Técnico do Seguro Social' and d.nome in ('Língua Portuguesa', 'Direito Administrativo');
