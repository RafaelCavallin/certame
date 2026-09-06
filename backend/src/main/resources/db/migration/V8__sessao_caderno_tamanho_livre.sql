alter table sessao_estudo drop constraint sessao_estudo_tamanho_check;
alter table sessao_estudo add constraint sessao_estudo_tamanho_check
  check (origem = 'CADERNO' and tamanho between 1 and 50 or origem <> 'CADERNO' and tamanho in (10,30,50));
