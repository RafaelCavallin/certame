# Resumo das tarefas de implementação da importação de questões de APIs externas

> **PRD:** `prd.md` · **TechSpec:** `techspec.md` · **Fases cobertas:** F1 (PoC, CA-01 a CA-08) e F3/F4 (MVP backend e interface, CA-09 a CA-28). F0, F2 e F5 são trabalho de negócio e curadoria, não de código.
> **Dependência bloqueante:** `prd-banco-de-questoes` implementado (`questao/`, `catalogo/`, `MontadorQuestao`, `ValidadorPublicacao`, a *check constraint* `questao_publicada_completa` e `comum/auditoria`). A importação **reutiliza** essa régua editorial em vez de duplicá-la (RN-13).
> **Bloqueador de negócio (RN-21):** os gates G-01 a G-04 precisam de confirmação por escrito antes de habilitar a importação em produção. Enquanto isso, `certame.importacao.habilitada` fica falso em produção. O PoC da tarefa 5.0, por não persistir, pode rodar antes.
> **Portão de decisão:** CA-03 e CA-08, medidos no PoC da tarefa 5.0, podem **reprovar o fornecedor** e interromper as tarefas seguintes.

## Tarefas

- [x] 1.0 Migração V19, persistência e triagem de compatibilidade (RF-42 a RF-46, RF-53, RF-55, RF-59, RF-60)
- [ ] 2.0 Porta do fornecedor, registro e adaptador QAPI (RF-38 a RF-41)
- [ ] 3.0 Mapeamento de taxonomia e deduplicação (RF-47 a RF-50, RF-59, RF-60, RF-63)
- [ ] 4.0 Texto-base e sua exibição no estudo e no simulado (RF-55 a RF-58)
- [ ] 5.0 Pipeline da candidata, lote assíncrono e PoC em modo simulação (RF-51, RF-52, RF-61, RF-62, RF-66, RF-67)
- [ ] 6.0 Divergência externa e decisão humana (RF-63 a RF-65)
- [ ] 7.0 Controllers, contrato da API, segurança, observabilidade e auditoria (RF-54, RF-68 a RF-73)
- [ ] 8.0 Protótipo Nocturne: telas de importações, mapeamentos, divergências e texto-base
- [ ] 9.0 Frontend Angular, E2E e fechamento de cobertura (RF-70 a RF-73, RF-54, RF-57)
