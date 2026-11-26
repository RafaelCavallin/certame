# Tarefa 6.0: Divergência externa e decisão humana

## Visão geral

O fornecedor corrige um gabarito, altera um enunciado ou anula uma questão que o Certame já importou. Esta tarefa decide o que acontece — e a resposta depende inteiramente da situação local.

A regra é curta e inegociável: **questão publicada, ou já respondida por algum estudante, jamais é alterada por sincronização automática**. Ela vira pendência de decisão humana, com a comparação lado a lado, e o administrador escolhe aplicar, ignorar ou despublicar. Questão em `RASCUNHO` ou `EM_REVISAO` **sem nenhuma resposta** é atualizada automaticamente, com registro em auditoria — não há histórico a proteger.

Nenhuma das três decisões toca em `resposta`. O banco já garante isso pelas regras `do instead nothing` desde a `V7`, e o teste existe para provar que a aplicação não tenta.

A detecção depende de a tarefa 5.0 existir: só se reencontra o que já foi importado. No MVP a divergência é detectada **apenas quando uma reimportação reencontra a questão** — não há verificação agendada.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `DetectorDivergencia` e `DivergenciaService` são unidade pura; `@SpringBootTest @Testcontainers` só em TI-14, TI-15 e TI-16, que precisam de resposta real gravada e das regras de imutabilidade do banco.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras que incidem aqui:

- **Regra 6 do `AGENTS.md` / RN-02** — `resposta` é imutável. Nenhuma decisão sobre divergência altera ou apaga registro de resposta, nem recalcula desempenho retroativo.
- **Regra 2 / RNF-10** — aplicar e ignorar são ações de `ROLE_ADMIN+`, validadas no servidor com `@PreAuthorize`, mesmo com a interface escondendo a ação.
- **Fronteira por domínio** — a atualização do conteúdo passa por `QuestaoImportadaService.atualizarConteudo(...)`, nunca pelo repositório de `questao/`.
- **RF-69** — a auditoria guarda identificador externo, hashes e decisão; o **conteúdo externo bruto** fica em `divergencia_externa.conteudo_externo`, não no payload de auditoria — sem ele não há como mostrar a comparação nem aplicar a decisão depois.
- **Camadas** — `DivergenciaService` concentra regra e transação; o controller da tarefa 7.0 só orquestra.
</rules>

<requirements>
- **RF-63** — ao reencontrar uma questão já importada, comparar o conteúdo externo com o que foi importado, por hash, e classificar como `ALTERACAO_EXTERNA`.
- **RF-64** — tratamento por situação local:
  - `RASCUNHO` ou `EM_REVISAO` **sem respostas** ⇒ conteúdo atualizado automaticamente, com registro em auditoria;
  - `PUBLICADA`, **ou com qualquer resposta de estudante** ⇒ nunca alterada em silêncio; divergência pendente para decisão humana;
  - removida no fornecedor ⇒ a questão local **não é apagada**; recebe sinalização de origem indisponível;
  - anulada no fornecedor ⇒ divergência de prioridade `ALTA`; despublicar continua sendo ação humana pelo `despublicar` que já existe.
- **RF-65** — nenhuma decisão altera ou apaga registros de `resposta`.
- Aplicar sobre questão publicada cujo conteúdo externo não passa em `ValidadorPublicacao` é recusado com `PUBLICACAO_INVALIDA` e os `fieldErrors` do validador atual — a mesma régua da questão manual.
- Decidir uma divergência já decidida é recusado com `TRANSICAO_INVALIDA`.
- Uma divergência pendente por questão, garantida pelo índice único parcial `divergencia_pendente_uk`.
</requirements>

## Subtarefas

- [ ] 6.1 Implementar `divergencia/DetectorDivergencia`, comparando o hash externo gravado com o de `HashConteudoExterno` sobre o conteúdo que a fonte devolve agora.
- [ ] 6.2 Ligar a detecção ao estágio 3 do pipeline: identidade externa já no acervo com hash diferente ⇒ resultado `ALTERACAO_EXTERNA` no item do lote.
- [ ] 6.3 Implementar `DivergenciaService.registrar(...)`: decide entre atualizar automaticamente e abrir pendência, consultando situação da questão **e** existência de resposta.
- [ ] 6.4 Implementar `aplicar(...)` e `ignorar(...)`, com `TRANSICAO_INVALIDA` para divergência já decidida e a régua de `ValidadorPublicacao` quando a questão está publicada.
- [ ] 6.5 Marcar prioridade `ALTA` para questão anulada na origem e sinalizar origem indisponível para questão removida no fornecedor, sem apagar nada.
- [ ] 6.6 Criar o DTO `DivergenciaExternaDetalhe` com os blocos `local` e `externo` e o sinalizador `bloqueadaPorResposta`.
- [ ] 6.7 Expor `divergenciaPendente` no bloco `ProcedenciaQuestao` de `QuestaoDetalhe`.
- [ ] 6.8 Registrar a auditoria de `DIVERGENCIA_APLICADA` e `DIVERGENCIA_IGNORADA` e a métrica `certame.importacao.divergencias.detectadas`.
- [ ] 6.9 Escrever os testes TU-30, TU-31, TI-14, TI-15 e TI-16.

## Detalhes de implementação

Ver TechSpec, seções *Design de implementação* → **Principais interfaces** (`DivergenciaService`), *Modelos de dados* → **`DivergenciaExternaDetalhe`** e a tabela `divergencia_externa` (com a justificativa de `conteudo_externo` ficar ali e não na auditoria), *Endpoints da API* → **`POST /api/v1/admin/divergencias/{id}/aplicar`** (incluindo o exemplo de recusa por régua editorial), e *Considerações técnicas* → **RT-9**.

A tabela de RF-64 no PRD é a fonte de verdade do comportamento por situação local.

## Critérios de aceitação relacionados

- CA-19
- CA-20
- CA-21

## Testes da tarefa

### Testes de unidade

- [ ] TU-30 — `mantemQuestaoPublicadaRespondidaIntactaNaDivergencia`
- [ ] TU-31 — `atualizaQuestaoEmRevisaoSemRespostaAutomaticamente`

### Testes de integração

- [ ] TI-14 — `criaDivergenciaPendenteSemAlterarQuestaoPublicadaRespondida`
- [ ] TI-15 — `atualizaAutomaticamenteQuestaoEmRevisaoSemRespostaERegistraAuditoria`
- [ ] TI-16 — `naoAlteraNemRemoveNenhumaRespostaEmQualquerDecisao`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/importacao/divergencia/{DetectorDivergencia,DivergenciaService}.java`
- `backend/src/main/java/br/com/certame/importacao/dto/DivergenciaExternaDetalhe.java`
- `backend/src/main/java/br/com/certame/importacao/lote/ProcessadorCandidata.java` (modificado)
- `backend/src/main/java/br/com/certame/questao/{QuestaoImportadaService,ProcedenciaQuestao}.java` (modificados)
- `backend/src/test/java/br/com/certame/importacao/divergencia/*Test.java`
