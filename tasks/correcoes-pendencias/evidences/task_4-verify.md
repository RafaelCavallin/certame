# Evidência — Tarefa 4.0 (P1-2)

## Decisão de produto

- Anonimizar `usuario.nome`, `usuario.email`, textos próprios de `caderno`, `deck` e `flashcard`,
  além de payloads de auditoria associados ao usuário.
- Substituir por placeholders após sete dias da exclusão lógica.
- Preservar UUIDs, respostas, sessões, revisões e métricas agregadas; `resposta` não é alterada.

## Implementação

- Migração `V16__anonimizacao_historico_usuario.sql` adiciona `usuario.anonimizado_em` e índice
  parcial para candidatos pendentes.
- `AnonimizadorHistoricoUsuario` executa diariamente, é idempotente e reprocessa falhas futuras.
- Testes `TI08_anonimizaHistoricoAposSeteDiasPreservandoAgregados` e
  `TI09_naoAnonimizaHistoricoAntesDoPrazo` cobrem o prazo, placeholders e preservação numérica.

## Verificações

```text
./mvnw test -Dtest=AnonimizadorHistoricoUsuarioIntegracaoTest
Tests run: 2, Failures: 0, Errors: 0

./mvnw verify
Tests run: 545, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
jacoco:check: All coverage checks have been met.
```

O fixture de `PublicacaoServiceTest` foi alinhado à regra vigente de disciplina obrigatória; o
comportamento de produção não foi alterado.
