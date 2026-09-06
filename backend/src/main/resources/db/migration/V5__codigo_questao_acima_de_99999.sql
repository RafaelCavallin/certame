-- O default de `questao.codigo` usava lpad(nextval(...)::text, 5, '0'), que trunca a partir de
-- 100000 e devolve '10000' — código já emitido. Da 90.001ª questão em diante todo insert falhava
-- com violação de unicidade, o que impedia o acervo de 100 mil questões exigido por CA-01.
-- A função abaixo apenas completa com zeros até cinco dígitos e deixa crescer daí em diante,
-- igual ao "Q-%05d" de GeradorCodigoQuestao.
create or replace function certame_codigo_questao() returns text
language sql volatile
as $$
    select 'Q-' || lpad(valor::text, greatest(5, length(valor::text)), '0')
    from (select nextval('questao_codigo_seq') as valor) as sequencia;
$$;

alter table questao alter column codigo set default certame_codigo_questao();
