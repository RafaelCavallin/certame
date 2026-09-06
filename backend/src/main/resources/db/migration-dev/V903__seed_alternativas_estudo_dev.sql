insert into alternativa (questao_id, letra, texto, ordem)
select q.id, letras.letra, 'Alternativa ' || letras.letra, letras.ordem
from questao q
cross join (values ('A', 1), ('B', 2), ('C', 3), ('D', 4), ('E', 5)) as letras(letra, ordem)
where q.status = 'PUBLICADA'
  and q.tipo = 'ME'
  and not exists (select 1 from alternativa a where a.questao_id = q.id);
