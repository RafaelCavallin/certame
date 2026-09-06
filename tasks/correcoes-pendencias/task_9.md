# Tarefa 9.0 (P3-3): Desvios pontuais de padrão

## Visão geral

Dez desvios pontuais, todos confirmados no código atual, sem relação entre si além de serem
pequenos e mecânicos na maioria dos casos. Executar por último, depois que as tarefas 1.0-8.0
já tiverem tocado os arquivos que se sobrepõem (ex.: os arquivos de linha comprimida da tarefa 8.0
coincidem com dois destes itens).

<rules>
### Conformidade com o AGENTS.md e as rules

- `AGENTS.md` regra 1 — o frontend é tradução 1:1 do protótipo aprovado; usar sempre os componentes
  Nocturne já existentes (subtarefa 9.1).
- `.agents/rules/code-standards.md` §3 "Injeção e nulidade" — injeção por construtor, sem
  `@Autowired` (subtarefas 9.5, 9.6).
- `.agents/rules/code-standards.md` §"Entidades JPA" / tempo injetável — usar `Clock`, não
  `Instant.now()` direto (subtarefa 9.7).
- `.agents/rules/code-standards.md` §"DTOs e validação" — Bean Validation é o padrão (subtarefas
  9.3, 9.4).
</rules>

## Subtarefas

- [x] 9.1 Trocar `window.confirm()` pelo `ConfirmacaoDialog` Nocturne já usado nas telas irmãs, nos
  três pontos confirmados: `frontend/src/app/features/admin/questoes/questao-form-store.ts:32`
  (troca de tipo ME↔CE), `frontend/src/app/features/admin/simulados/simulados-page/simulados-page.ts:16`
  e `frontend/src/app/features/admin/simulados/simulado-form-page/simulado-form-page.ts:22`.
- [x] 9.2 Trocar `import('rxjs').then(...)` por import estático de `firstValueFrom` em
  `questoes-page.ts`, `materias-page.ts`, `catalogo-page.ts`, `composicao-disciplinas.ts` e
  `flashcards-store.ts` (linha 70) — todos já usam import estático de `rxjs` em outros pontos do
  próprio projeto; não há razão para o import dinâmico de uma função trivial já empacotada.
- [x] 9.3 Para `flashcard/dto/RequisicaoFlashcard.java`: decidir entre (a) registrar na TechSpec por
  que a validação de `frente`/`verso` é manual no serviço (o *trim* precisa preceder a medida de
  tamanho, o que Bean Validation não faz sozinho) ou (b) migrar para Bean Validation com um
  validador customizado que aplique o trim antes da medida.
- [x] 9.4 Adicionar `@Valid` em `flashcard/deck/DeckController.java:32` (`PUT /decks/{id}`),
  alinhando ao padrão de DTO `record` com *constraints* já usado no restante da API; migrar a
  validação de `ValidadorDeck` para as anotações do DTO, se a mudança não quebrar mensagens de erro
  já testadas.
- [x] 9.5 Remover `@Autowired` de `simulado/MontadorProvaSimulado.java:22` (construtor único não
  precisa da anotação).
- [x] 9.6 Remover o `Clock` injetado e nunca usado em `simulado/ExpiradorTentativaSimulado.java`.
- [x] 9.7 Substituir `Instant.now()` por `Clock` injetado em `simulado/TentativaSimulado.java:23,27`,
  seguindo o padrão já usado por `JanelaSimulado`. Como isso muda o mecanismo de tempo de uma
  entidade JPA testada, rodar a suíte de simulados por completo após a mudança.
- [x] 9.8 Corrigir o reenfileiramento em `frontend/src/app/features/flashcards/revisao-store.ts:89`:
  hoje o card reenfileirado vai para o **fim** da fila (`[...restante, card]`); a TechSpec exige
  ordenação por `proximaRevisaoEm`. Inserir o card na posição correta da fila reordenada, com teste
  de unidade cobrindo o caso (card reenfileirado deve aparecer antes de outro cuja
  `proximaRevisaoEm` seja mais distante).
- [x] 9.9 Corrigir a rastreabilidade de IDs de teste em `estudo/SessaoEstudoServiceTest` e
  `CorrecaoProvaServiceTest`: os IDs `TU18`/`TU19` e `TU29`–`TU37` estão reusados para casos que
  pertencem a outras classes. Renumerar para IDs únicos, sem alterar a lógica dos testes.
- [x] 9.10 Renomear `desempenho/PontoFraco.java` e `desempenho/SemanaEvolucao.java` (ou os DTOs
  homônimos em `desempenho/dto/`) para eliminar a colisão de nomes que hoje exige import totalmente
  qualificado. Escolher o nome mais específico para a entidade de domínio, mantendo o DTO com o nome
  mais genérico, ou vice-versa, conforme a convenção já usada em outras entidades/DTOs do projeto.

## Detalhes de implementação

9.7 e 9.8 mudam comportamento observável (tempo injetável, ordem da fila de revisão) e exigem teste
novo ou revisado; as demais subtarefas são mecânicas e não devem alterar comportamento.

## Critérios de aceitação relacionados

- RN-10 (limites de deck) / ordenação da fila de revisão — subtarefa 9.8.
- Nenhum outro CA de produto muda nesta tarefa.

## Testes da tarefa

- [x] Teste de unidade novo para o reenfileiramento ordenado (9.8).
- [x] Suíte completa de `simulado/` após 9.7 (mudança de mecanismo de tempo).
- [x] Suítes de `estudo/SessaoEstudoServiceTest` e `CorrecaoProvaServiceTest` devem continuar verdes
  após a renumeração de IDs (9.9).
- [x] Suíte completa do backend (`./mvnw test`) e do frontend (`npm test`, 475 testes) verdes após
  todas as subtarefas.

## Arquivos relevantes

- `frontend/src/app/features/admin/questoes/questao-form-store.ts`
- `frontend/src/app/features/admin/simulados/simulados-page/simulados-page.ts`
- `frontend/src/app/features/admin/simulados/simulado-form-page/simulado-form-page.ts`
- `frontend/src/app/features/admin/questoes/questoes-page/questoes-page.ts`
- `frontend/src/app/features/admin/materias/materias-page/materias-page.ts`
- `frontend/src/app/features/admin/catalogo/catalogo-page/catalogo-page.ts`
- `frontend/src/app/features/admin/simulados/composicao-disciplinas/composicao-disciplinas.ts`
- `frontend/src/app/features/flashcards/flashcards-store.ts`
- `frontend/src/app/features/flashcards/revisao-store.ts`
- `backend/src/main/java/br/com/certame/flashcard/dto/RequisicaoFlashcard.java`
- `backend/src/main/java/br/com/certame/flashcard/deck/DeckController.java`
- `backend/src/main/java/br/com/certame/simulado/MontadorProvaSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/ExpiradorTentativaSimulado.java`
- `backend/src/main/java/br/com/certame/simulado/TentativaSimulado.java`
- `backend/src/test/java/br/com/certame/estudo/SessaoEstudoServiceTest.java`
- `backend/src/test/java/br/com/certame/simulado/CorrecaoProvaServiceTest.java` (caminho a confirmar)
- `backend/src/main/java/br/com/certame/desempenho/PontoFraco.java`,
  `backend/src/main/java/br/com/certame/desempenho/SemanaEvolucao.java`
