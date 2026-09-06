insert into disciplina (nome) values ('Direito Administrativo'), ('Língua Portuguesa'), ('Direito Constitucional');
insert into assunto (disciplina_id, nome)
select id, 'Licitações e contratos' from disciplina where nome = 'Direito Administrativo';
insert into assunto (disciplina_id, nome)
select id, 'Interpretação de texto' from disciplina where nome = 'Língua Portuguesa';
insert into questao (tipo, enunciado, disciplina_id, assunto_id, banca_id, ano, dificuldade, status, autor_id)
select 'ME', 'Sobre os princípios da Administração Pública, assinale a alternativa correta.', d.id, a.id, b.id, 2025, 'MEDIA', 'RASCUNHO', u.id
from disciplina d join assunto a on a.disciplina_id = d.id
join banca b on b.sigla = 'FCC' join usuario u on u.email = 'professor@dev.certame.local'
where d.nome = 'Direito Administrativo' and a.nome = 'Licitações e contratos';
