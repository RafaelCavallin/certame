-- Lote de exemplo, mapeamentos iniciais e um texto-base compartilhado, para exercitar as telas de
-- importação em desenvolvimento sem depender da QAPI real.

insert into lote_importacao (codigo, fonte, filtros, quantidade_pedida, status, somente_simulacao,
    total_recuperado, total_processado, total_importado, chamadas_externas, solicitante_id,
    iniciado_em, finalizado_em)
select 'IMP-00001', 'QAPI', '{"materia": "Direito Constitucional", "banca": "FGV"}'::jsonb, 10,
    'CONCLUIDO', false, 10, 10, 8, 1, u.id, now() - interval '1 day', now() - interval '1 day' + interval '40 seconds'
from usuario u
where u.email = 'admin@dev.certame.local'
  and not exists (select 1 from lote_importacao where codigo = 'IMP-00001');

select setval('lote_importacao_codigo_seq',
  greatest(coalesce((select max(substring(codigo from 5)::bigint) from lote_importacao), 1), 1), true);

insert into mapeamento_taxonomia (fonte, tipo, valor_externo, valor_externo_original, valor_externo_pai, disciplina_id, criado_por)
select 'QAPI', 'DISCIPLINA', 'direito constitucional', 'Direito Constitucional', null, d.id, u.id
from disciplina d, usuario u
where d.nome = 'Direito Constitucional' and d.excluido_em is null and u.email = 'admin@dev.certame.local'
  and not exists (select 1 from mapeamento_taxonomia where fonte = 'QAPI' and tipo = 'DISCIPLINA' and valor_externo = 'direito constitucional');

insert into mapeamento_taxonomia (fonte, tipo, valor_externo, valor_externo_original, valor_externo_pai, banca_id, criado_por)
select 'QAPI', 'BANCA', 'fgv', 'FGV', null, b.id, u.id
from banca b, usuario u
where b.sigla = 'FGV' and u.email = 'admin@dev.certame.local'
  and not exists (select 1 from mapeamento_taxonomia where fonte = 'QAPI' and tipo = 'BANCA' and valor_externo = 'fgv');

insert into texto_base (fonte, identificador_externo, titulo, conteudo, referencia)
select 'QAPI', 'QTXT-DEV-0001', 'A reinvenção da vírgula',
    'No começo de 1902, Machado de Assis publicou uma crônica sobre os sinais de pontuação e o modo como eles mudam o sentido de uma frase.',
    'Machado de Assis'
where not exists (select 1 from texto_base where fonte = 'QAPI' and identificador_externo = 'QTXT-DEV-0001');

update questao as q set texto_base_id = tb.id
from texto_base tb
where tb.fonte = 'QAPI' and tb.identificador_externo = 'QTXT-DEV-0001'
  and q.texto_base_id is null
  and q.id in (select id from questao where excluido_em is null order by criado_em limit 2);
