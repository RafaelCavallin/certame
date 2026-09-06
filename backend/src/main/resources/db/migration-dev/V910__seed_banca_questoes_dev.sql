-- Distribui bancas entre as questões publicadas do seed que ficaram sem banca.
-- Sem isso, qualquer filtro por banca no painel de "Resolver questões" devolve SERIE_VAZIA em dev.
with bancas_ordenadas as (
    select id, row_number() over (order by nome) - 1 as posicao, count(*) over () as total
    from banca
), publicadas as (
    select id, row_number() over (order by codigo) - 1 as posicao
    from questao
    where status = 'PUBLICADA' and excluido_em is null and banca_id is null
)
update questao q
set banca_id = b.id
from publicadas p
join bancas_ordenadas b on b.posicao = p.posicao % b.total
where q.id = p.id;
