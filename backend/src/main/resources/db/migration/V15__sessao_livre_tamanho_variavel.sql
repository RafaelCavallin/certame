alter table sessao_estudo drop constraint if exists sessao_tamanho_valido;
alter table sessao_estudo add constraint sessao_tamanho_valido
  check ((origem = 'SIMULADO' and tamanho between 10 and 200) or
    (origem in ('CADERNO','LIVRE') and tamanho between 1 and 50));
