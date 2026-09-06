# Evidência — Tarefa 1.0 (P0-1)

## Comando executado

```
cd backend && ./mvnw verify
```

## Resultado

```
[INFO] Tests run: 541, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] --- jacoco:0.8.13:check (check-cobertura-minima) @ certame ---
[INFO] Analyzed bundle 'certame' with 230 classes
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

- 541 testes, 0 falhas (535 anteriores − 1 `TI06` reescrito + 7 casos novos `TI06`–`TI12`).
- `jacoco:check` executou (regra do `pom.xml`: `CLASS` cujo nome casa com
  `br.com.certame.*.*Service`, contador `LINE`, mínimo `0.80`) e **todos os checks passaram** —
  nenhuma classe `*Service` abaixo de 80% de linhas.
- Cobertura agregada do bundle (`target/site/jacoco/jacoco.csv`, todas as 230 classes analisadas,
  já com as exclusões do `pom.xml` — `CertameApplication`, `ServletInitializer`, `**/dto/**`,
  `config/**`):
  - Linhas: 2603/2919 cobertas = **89,17%**
  - Branches: 776/1080 cobertas = **71,85%**

A cobertura de branches agregada (71,85%) fica abaixo do piso de 80% de `.agents/rules/tests.md` §1,
mas o gate mecânico do `jacoco:check` só audita `LINE` por classe `*Service` (ver `pom.xml`), que
passou integralmente. As lacunas reais de cobertura (linhas e branches, por módulo) são as seis já
catalogadas na seção 4 de `tasks/RELATORIO-PENDENCIAS.md` e ficam para a tarefa 6.0 (P2-3) — não
foram corrigidas aqui, conforme escopo desta tarefa.

## Causa raiz confirmada

`FlywayMigracaoTest.TI06_tamanhoDeSessaoForaDoDominioEhRecusado` testava o contrato anterior a `V15`
(recusava `tamanho = 40` para `LIVRE`). A migração `V15__sessao_livre_tamanho_variavel.sql` já havia
mudado a constraint `sessao_tamanho_valido` para aceitar `LIVRE` entre 1 e 50 — o teste ficou
desatualizado e passou a falhar sozinho, travando o `surefire` antes do `jacoco:check` rodar.
