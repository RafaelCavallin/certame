# Tarefa 3.0: Regras puras: `VerificadorPrazoEdital` e `ResolvedorDisciplinasEdital`

## Visão geral

Implementar as duas únicas regras de verdade da funcionalidade, ambas sem Spring e sem banco. `VerificadorPrazoEdital` responde se um edital está inconsistente — aberto com prazo passado — para que a tela administrativa sinalize a correção (caso de borda 2); ele **nunca** reescreve status de ninguém. `ResolvedorDisciplinasEdital` converte a lista de `UUID` do formulário em entidades `Disciplina` existentes e não excluídas, e recusa id desconhecido com erro campo a campo (CA-08).

Elas existem separadas dos serviços precisamente para serem testáveis em milissegundos: é o passo 3 da ordem de construção.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito puros, sem contexto Spring; AssertJ nas asserções; mock apenas na fronteira (`DisciplinaRepository`).
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **"Comece pela camada de domínio + testes"** — esta tarefa é o domínio puro, antes dos serviços (4.0, 5.0) e dos controllers (6.0).
- `tests.md` §2, *Repeatable* — `VerificadorPrazoEdital` recebe um `Clock` injetado e os testes usam `Clock.fixed`; **nunca** `LocalDate.now()`, que traria instabilidade na virada do dia. O fuso é `America/Sao_Paulo`, via `ZonaAplicacao` de `prd-cadernos-e-desempenho`; se ainda não existir, nasce aqui com a mesma assinatura.
- `tests.md` §3 — AAA/Given-When-Then visível, nomes no padrão `TU04_naoSinalizaEditalEncerradoComPrazoPassado`.
- `code-standards.md` §2 — métodos ≤ 30 linhas, ≤ 3 parâmetros, ≤ 3 níveis de condicional, sem comentário e sem literal mágico.
- `code-standards.md` §3 — injeção por construtor; erro por `DadosInvalidosException`, exceção já existente de `prd-banco-de-questoes`; **nenhum tipo de exceção novo**.
</rules>

<requirements>
- `VerificadorPrazoEdital.vencido(StatusEdital status, LocalDate prazo) -> boolean` — verdadeiro **apenas** quando `status` é `ABERTO` e `prazo` é anterior a hoje. Prazo de hoje ainda vale.
- `ResolvedorDisciplinasEdital.resolver(Set<UUID> ids) -> Set<Disciplina>` — id inexistente ou disciplina excluída logicamente lançam `DadosInvalidosException` com `fieldErrors.disciplinaIds`; ids repetidos viram um só; lista vazia devolve conjunto vazio **sem consultar o repositório**.
- **CA-08** é garantia de dados, não de tela: a validação existe no servidor mesmo com o seletor fechado.
</requirements>

## Subtarefas

- [x] 3.1 Criar `VerificadorPrazoEdital` como regra pura sobre um `Clock` injetado, sem Spring e sem repositório.
- [x] 3.2 Garantir o fuso `America/Sao_Paulo` por `ZonaAplicacao` (criando-a com a mesma assinatura se `prd-cadernos-e-desempenho` ainda não a tiver entregue).
- [x] 3.3 Escrever TU-01…TU-04 com `Clock.fixed`.
- [x] 3.4 Criar `ResolvedorDisciplinasEdital` consultando `DisciplinaRepository` uma única vez por chamada.
- [x] 3.5 Tratar o conjunto vazio como atalho, sem ida ao repositório.
- [x] 3.6 Mapear id desconhecido e disciplina excluída para `DadosInvalidosException` com a mensagem em pt-BR "Uma das disciplinas selecionadas não existe mais na árvore de matérias."
- [x] 3.7 Escrever TU-14…TU-17 com Mockito sobre `DisciplinaRepository`.

## Detalhes de implementação

Ver `techspec.md` §*Design de implementação* → *Principais interfaces* (assinaturas de `VerificadorPrazoEdital` e `ResolvedorDisciplinasEdital`), §*Arquitetura do sistema* → *Visão dos componentes* (por que cada uma existe separada do serviço), §*Modelos de dados* → `ErroApi` (código `DADOS_INVALIDOS` e o `fieldErrors` esperado) e §*Riscos conhecidos*, linha de `prazo_inscricao` sem hora.

## Critérios de aceitação relacionados

- CA-08
- Caso de borda 2 do PRD

## Testes da tarefa

### Testes de unidade

- [x] TU-01 — `VerificadorPrazoEdital` com edital aberto e prazo de ontem
- [x] TU-02 — `VerificadorPrazoEdital` com edital aberto e prazo de hoje
- [x] TU-03 — `VerificadorPrazoEdital` com edital aberto e prazo futuro
- [x] TU-04 — `VerificadorPrazoEdital` com edital encerrado e prazo de ontem
- [x] TU-14 — `ResolvedorDisciplinasEdital` com id que não existe
- [x] TU-15 — `ResolvedorDisciplinasEdital` com disciplina excluída logicamente
- [x] TU-16 — `ResolvedorDisciplinasEdital` com ids repetidos
- [x] TU-17 — `ResolvedorDisciplinasEdital` com lista vazia

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/catalogo/edital/VerificadorPrazoEdital.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/edital/ResolvedorDisciplinasEdital.java` (novo)
- `backend/src/main/java/br/com/certame/catalogo/edital/StatusEdital.java` (tarefa 2.0)
- `backend/src/main/java/br/com/certame/catalogo/DisciplinaRepository.java` (dependência de `prd-banco-de-questoes`)
- `backend/src/main/java/br/com/certame/comum/excecao/DadosInvalidosException.java` (dependência)
- `ZonaAplicacao` (de `prd-cadernos-e-desempenho`, ou criada aqui)
