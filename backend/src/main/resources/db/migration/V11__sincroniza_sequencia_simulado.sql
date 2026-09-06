select setval(
  'simulado_codigo_seq',
  greatest(coalesce((select max(substring(codigo from 4)::bigint) from simulado), 1), 1),
  true
);
