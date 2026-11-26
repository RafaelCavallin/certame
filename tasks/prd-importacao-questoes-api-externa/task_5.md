# Tarefa 5.0: Pipeline da candidata, lote assíncrono e PoC em modo simulação

## Visão geral

Onde as peças anteriores viram uma importação. `ProcessadorCandidata` percorre os sete estágios do pipeline e termina **sempre** com exatamente um resultado — nenhuma candidata processada fica sem classificação, que é o objetivo O-7 e a razão de existir do relatório. `ExecutorLoteImportacao` roda `@Async`, fora do request do estudante, e processa cada candidata em transação própria `REQUIRES_NEW`: erro em uma não desfaz as anteriores, e o lote termina como `PARCIAL` em vez de perder o que já entrou.

A persistência **reutiliza a régua editorial existente**. `QuestaoImportadaService` monta um `RequisicaoQuestao` e chama `MontadorQuestao` — o mesmo que atende o formulário administrativo. Nada aqui reimplementa validação de questão, e nada aqui publica: a candidata entra em `EM_REVISAO`, ou em `RASCUNHO` quando o comentário foi reprovado, e só chega ao estudante por decisão humana.

O **PoC de F1 não é código descartável paralelo**: é este mesmo pipeline com o sinalizador `somenteSimulacao`, que grava o lote e seus itens — que são o relatório — e não grava questão, texto-base nem mapeamento. Duas triagens diferentes entre PoC e MVP invalidariam o PoC como previsão do MVP, que é justamente para o que ele existe.

> **Portão de decisão.** CA-03 (aproveitamento ≥ 60%) e CA-08 (reprovação por comentário, recurso não suportado ou contrato incompatível) são medidos aqui, executando o modo simulação contra a API real com chave real. Eles podem **reprovar o fornecedor** e interromper as tarefas 6.0 em diante. É também aqui que a questão Q-6 do PRD (formato do gabarito em certo/errado) se fecha.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `ProcessadorCandidata` é unidade pura com fornecedor e repositórios substituídos; `@SpringBootTest @Testcontainers` só para o fluxo ponta a ponta (TI-07, TI-08, TI-12, TI-22, TI-23). Nada sobe ao nível de cima do que o de baixo já prova.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras que incidem aqui:

- **Fronteira por domínio** (`code-standards.md` §3) — `importacao/` não acessa `QuestaoRepository` diretamente. `QuestaoImportadaService` existe em `questao/` exatamente para ser a porta de escrita.
- **Arquivos inalterados por decisão de projeto** — `MontadorQuestao`, `ValidadorPublicacao`, `PublicacaoService` e `GeradorCodigoQuestao` não são tocados (RN-13).
- **Camadas** — serviço concentra regra, transação e autorização; o executor **não** abre transação envolvente, e é isso que faz a falha parcial funcionar.
- **RT-9** — o executor é envolvido em `DelegatingSecurityContextAsyncTaskExecutor`. Sem isso o `SecurityContext` não atravessa a thread, `@PreAuthorize` recusaria as chamadas internas e a auditoria gravaria ator nulo — falha silenciosa e difícil de diagnosticar.
- **RNF-08** — log em português com o **código do lote em toda linha**, sem chave e sem payload bruto.
- **Testes** — `Clock` fixo em tudo que grava data; massa isolada por execução com `UUID.randomUUID()` em código de questão e identificador externo.
</rules>

<requirements>
- **RF-51** — candidata aprovada com comentário válido ⇒ `EM_REVISAO`; com comentário ausente ou reprovado ⇒ `RASCUNHO`, com a pendência registrada. **Nenhuma questão importada é publicada automaticamente** (RN-12).
- **RF-52** — autor da questão importada é quem disparou a importação (ADR-4), satisfazendo `autor_id not null` sem inventar usuário fantasma.
- **RF-53** — procedência gravada: fonte, identificador externo, URL quando houver, data de importação, data de verificação e o lote que a trouxe. O `codigo` interno `Q-00000` continua sendo a identidade da questão no Certame.
- **RF-61** — toda candidata processada fica registrada com seu resultado, permitindo reprocessar lotes e explicar decisões passadas.
- **RF-62** — cada candidata é processada e persistida de forma independente; reexecutar retoma sem duplicar.
- **RF-66** — o lote produz o relatório com fornecedor, filtros, solicitante, início e fim, totais, `chamadasExternas`, a distribuição pelos **dez** resultados (com zero quando não houve) e as pendências de mapeamento.
- **RF-67** — cada candidata tem exatamente um resultado e, quando descartada, um motivo legível em português.
- **RN-21** — o pedido é recusado quando `certame.importacao.habilitada` é falso; `somenteSimulacao` é recusado onde o PoC não roda.
- **RN-22** — no máximo 50 questões por lote. Um lote de 50 pede exatamente `ceil(50 ÷ 10) = 5` páginas: **nenhuma página extra é buscada para "completar" o pedido**, porque isso gastaria crédito de forma imprevisível. Entregar menos que o pedido é resultado válido, desde que cada descarte tenha motivo.
- **RNF-03** — 50 candidatas em menos de 60 s. **RNF-04** — fora do caminho de request do estudante, sem transação longa.
</requirements>

## Subtarefas

- [ ] 5.1 Criar `questao/QuestaoImportadaService` com `criar(...)` e `atualizarConteudo(...)`, ambos reutilizando `MontadorQuestao` e nenhum deles publicando.
- [ ] 5.2 Criar `config/ExecutorImportacaoConfig`: pool de **uma** thread, envolvido em `DelegatingSecurityContextAsyncTaskExecutor`. A thread única é controle de custo e de concorrência, não economia de recurso.
- [ ] 5.3 Implementar `lote/ProcessadorCandidata` com os sete estágios na ordem da TechSpec, em `@Transactional(REQUIRES_NEW)`, produzindo sempre um `ItemImportacao` com resultado e motivo.
- [ ] 5.4 Implementar `lote/ContextoLote` (o lote em formação, para a dedup interna, e o contador de chamadas).
- [ ] 5.5 Implementar `lote/ExecutorLoteImportacao` com `@Async("executorImportacao")`, sem transação envolvente, atualizando `total_processado` a cada candidata e fechando o lote em `CONCLUIDO`, `PARCIAL` ou `FALHOU` com `motivoInterrupcao` em português.
- [ ] 5.6 Implementar `lote/ImportacaoService`: valida o pedido, a habilitação (RN-21) e a permissão de simulação, recusa lote concorrente para a mesma fonte com `IMPORTACAO_EM_ANDAMENTO`, abre o lote e delega.
- [ ] 5.7 Implementar o modo `somenteSimulacao`: classifica tudo, grava **apenas** `lote_importacao` e `item_importacao`, e não grava questão, texto-base nem mapeamento.
- [ ] 5.8 Implementar `lote/RelatorioImportacaoService`: relatório do lote, distribuição pelos dez resultados, pendências de mapeamento agregadas e histórico filtrável.
- [ ] 5.9 Criar os DTOs `RequisicaoImportacao`, `LoteAceito`, `RelatorioImportacao`, `LoteResumo`, `ItemImportacaoResumo` e `FiltroImportacao`.
- [ ] 5.10 Criar `comum/excecao/ImportacaoDesabilitadaException` e registrar a auditoria de `IMPORTACAO_INICIADA` e `QUESTAO_IMPORTADA`.
- [ ] 5.11 Escrever os testes TU-26, TU-27, TU-29, TU-34, TU-35, TU-40, TI-07, TI-08, TI-12, TI-22 e TI-23.
- [ ] 5.12 **Executar o PoC de F1** contra a QAPI real, em modo simulação, com 20 a 50 candidatas, e registrar o relatório como evidência de CA-01, CA-03 e CA-08 — inclusive a distribuição `ME`/`CE` que fecha Q-6 e a proporção de questões sem dificuldade de RT-1.

## Detalhes de implementação

Ver TechSpec, seções *Visão dos componentes* → **Pipeline de uma candidata** (a ordem dos sete estágios e por que ela não é arbitrária) e **Fluxo de dados**, *Design de implementação* → **Principais interfaces** (`ImportacaoService`, `ExecutorLoteImportacao`, `ProcessadorCandidata`, `QuestaoImportadaService`), *Modelos de dados* → **`RequisicaoImportacao`**, **`LoteAceito`**, **`RelatorioImportacao`** (com o exemplo de lote parcial) e **`ItemImportacaoResumo`**, e *Considerações técnicas* → **ADR-4**, **Execução assíncrona com consulta de progresso**, **Transação por candidata**, **Uma página a mais não é buscada** e **RT-5**/**RT-9**.

O *Resumo* da TechSpec registra por que o PoC compartilha o pipeline em vez de ser código paralelo.

## Critérios de aceitação relacionados

- CA-01 *(verificação manual — resultado de campo do PoC)*
- CA-02
- CA-03 *(verificação manual — portão de decisão)*
- CA-04
- CA-05
- CA-07
- CA-08 *(verificação manual — portão de decisão)*
- CA-09
- CA-10
- CA-11
- CA-16
- CA-18

## Testes da tarefa

> CA-01, CA-03 e CA-08 **não são automatizáveis**: são resultados de campo, medidos executando o modo simulação contra a API real com chave real. Ficam como verificação manual, com o relatório do lote `somenteSimulacao` como evidência (subtarefa 5.12).

### Testes de unidade

- [ ] TU-26 — `gravaCandidataAprovadaEmRevisao`
- [ ] TU-27 — `gravaCandidataSemComentarioEmRascunho`
- [ ] TU-29 — `erroEmUmaCandidataNaoDesfazAsAnteriores`
- [ ] TU-34 — `classificaTodaCandidataProcessadaComExatamenteUmResultado`
- [ ] TU-35 — `naoPersisteNadaEmModoSimulacao`
- [ ] TU-40 — `processaLoteDeFornecedorFicticioSemAlterarNadaDoNucleo`

### Testes de integração

- [ ] TI-07 — `processaLoteCompletoEProduzRelatorioComDistribuicao`
- [ ] TI-08 — `naoDuplicaNadaAoReexecutarOMesmoPedido`
- [ ] TI-12 — `fechaLoteComoParcialPreservandoOQueJaEntrou`
- [ ] TI-22 — `naoGravaQuestaoTextoBaseNemMapeamentoEmModoSimulacao`
- [ ] TI-23 — `classificaIdenticamenteEmDuasExecucoesDoMesmoPedido`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/questao/QuestaoImportadaService.java`
- `backend/src/main/java/br/com/certame/config/ExecutorImportacaoConfig.java`
- `backend/src/main/java/br/com/certame/importacao/lote/{ImportacaoService,ExecutorLoteImportacao,ProcessadorCandidata,ContextoLote,RelatorioImportacaoService}.java`
- `backend/src/main/java/br/com/certame/importacao/dto/{RequisicaoImportacao,LoteAceito,RelatorioImportacao,LoteResumo,ItemImportacaoResumo,FiltroImportacao}.java`
- `backend/src/main/java/br/com/certame/comum/excecao/ImportacaoDesabilitadaException.java`
- `backend/src/test/java/br/com/certame/importacao/lote/*Test.java` · `importacao/ImportacaoIntegracaoTest.java`
