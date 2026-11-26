# Tarefa 1.0: Migração V19, persistência e triagem de compatibilidade

## Visão geral

As duas fundações independentes da funcionalidade, entregues juntas porque nenhuma depende da outra e tudo o mais depende das duas.

A primeira é o **esquema**: a migração `V19__importacao_questoes_externas.sql`, aditiva, cria `texto_base`, `mapeamento_taxonomia`, `lote_importacao`, `item_importacao` e `divergencia_externa`, acrescenta as oito colunas de procedência e o vínculo de texto-base à `questao`, e instala a função SQL de normalização com a coluna gerada `hash_enunciado`. Vem primeiro porque `ddl-auto=validate` faz todo o código depender dela, e porque a coluna gerada é o que faz a deduplicação por conteúdo valer **para o acervo pré-existente, sem backfill** (ADR-6, CA-17).

A segunda é a **triagem**: cinco objetos puros, sem Spring, sem fornecedor e sem banco, que reprovam cedo e barato o que o Certame não sabe exibir. É a parte de maior risco de produto (RI-3, RI-4) e é o que o PoC mede — existir cedo é o que permite validar o fornecedor antes de investir no resto.

O enum `Fonte` nasce já com os quatro valores previstos pelo PRD (`INTERNA`, `QAPI`, `QUEST_API`, `API_DAS_QUESTOES`), no enum e nos quatro `check` da migração, para que o segundo fornecedor não custe migração nenhuma.

<skills>
### Conformidade com skills

- `postgresql-table-design` — aplicável ao DDL inteiro: `uuid` com `gen_random_uuid()`, `timestamptz`, enum como `text` + `check`, índice único **parcial** para a identidade externa e para a fila de pendências, `jsonb` em filtros e pendências, coluna gerada `stored` para o hash, `check` de alvo único no mapeamento e `on delete cascade` só em `item_importacao`, que não tem vida própria.
- `313-frameworks-spring-db-migrations-flyway` — numeração `V19` em `db/migration/`, imutável; seed em `db/migration-dev/V912`, carregado só no perfil `dev`; `validate-on-migrate` e `baseline-on-migrate=false` inalterados.
- `spring-boot-testing` — `@SpringBootTest @Testcontainers` **apenas** para a migração e o SQL (TI-01 a TI-03); a triagem inteira é unidade pura, sem contexto Spring.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/folder-structure.md` e `.agents/rules/tests.md` lidos na íntegra. Regras que incidem aqui:

- **Regra 5 / RNF-05 / R-4** — migração só por Flyway, versionada e imutável, a partir de V19; nenhuma migração aplicada é editada; `ddl-auto` permanece em `validate`.
- **Regra 3 / R-6** — tabelas e colunas em português sem acento, `snake_case`; motivos de descarte em português do Brasil, frase completa e com ponto final.
- **Entidades** sem Lombok, construtor protegido para o JPA, `@GeneratedValue(strategy = UUID)`, `Instant` nas datas, `@Enumerated(EnumType.STRING)` com `check` equivalente na migração. `hashEnunciado` é mapeado **somente leitura**, no padrão de `busca`.
- **Limites de forma** — arquivos até 100 linhas, métodos até 30, no máximo 3 parâmetros e 3 níveis de condicional. A triagem é cinco classes justamente para caber nisso por responsabilidade real, não por corte arbitrário.
- **Testes** — FIRST, AAA, nome `TU`/`TI` + id do caso, AssertJ, `Clock` fixo, Testcontainers só em `*IntegracaoTest`.
- Nenhuma dependência Maven nova: `sha256()` é nativo do PostgreSQL 18 e `certame_unaccent` existe desde a `V4`.
</rules>

<requirements>
- **RF-53** — procedência em toda questão: fonte, identificador externo, URL externa, data de importação, data da última verificação, hash do conteúdo externo e lote de origem. Questão existente permanece `INTERNA` pelo `default` da migração, sem nenhuma outra alteração.
- **RF-55** — texto-base como entidade própria, com conteúdo, referência e identificador externo; uma questão referencia no máximo um.
- **RF-59** — unicidade de `(fonte, identificador_externo)` garantida **no banco**, por índice único parcial.
- **RF-60** — hash do enunciado normalizado sobre todo o acervo, em qualquer status, por coluna gerada.
- **RF-42** — detecção do tipo: duas ou mais opções preenchidas ⇒ `ME`; nenhuma opção e gabarito equivalente a certo/errado ⇒ `CE`; qualquer outra combinação ⇒ `TIPO_NAO_SUPORTADO`. Nada é importado em silêncio num tipo que o Certame não sabe exibir.
- **RF-43** — estrutura espelhando `MontadorQuestao`: 2 a 5 alternativas, letras A a E, **ordens contíguas sem lacuna** (opção vazia no meio é descarte, nunca compactação), gabarito correspondente a uma alternativa existente; em `CE`, gabarito normalizado para exatamente `CERTO` ou `ERRADO`.
- **RF-44** — enunciado entre 10 e 20 000 caracteres, comentário até 20 000, ano entre 1990 e 2100.
- **RF-45** — HTML, LaTeX, referência a imagem ou anexo, tabela e caractere de controle em enunciado, alternativa, comentário **ou texto-base** ⇒ `RECURSO_NAO_SUPORTADO`.
- **RF-46** — comentário reprovado (ausente, só a letra do gabarito, repetição do enunciado, texto vazio de conteúdo) gera **pendência**, nunca descarte.
- **RN-16** — questão com recurso não suportado é descartada com motivo, nunca importada parcialmente nem com o recurso removido.
</requirements>

## Subtarefas

- [x] 1.1 Escrever `V19__importacao_questoes_externas.sql`: `texto_base` com índice único parcial `(fonte, identificador_externo)`; as oito colunas de procedência e `texto_base_id` em `questao`, com `questao_fonte_externa_uk`.
- [x] 1.2 Acrescentar à V19 as funções `certame_enunciado_normalizado` e `certame_hash_enunciado` (`immutable parallel safe`), a coluna gerada `hash_enunciado` e seu índice.
- [x] 1.3 Acrescentar à V19 `mapeamento_taxonomia`, com o `check` de alvo único e a chave `(fonte, tipo, valor_externo, coalesce(valor_externo_pai, ''))`.
- [x] 1.4 Acrescentar à V19 `lote_importacao` (com a sequência de código), `item_importacao` e `divergencia_externa`, com os índices e o índice único parcial de divergência pendente.
- [x] 1.5 Criar `db/migration-dev/V912__seed_importacao_dev.sql`: um lote de exemplo, mapeamentos iniciais e um texto-base compartilhado por duas questões.
- [x] 1.6 Criar `questao/Fonte` com os quatro valores e `questao/ProcedenciaQuestao`; mapear os campos novos em `questao/Questao`, com `hashEnunciado` somente leitura.
- [x] 1.7 Criar as entidades e repositórios de `importacao/`: `TextoBase`, `MapeamentoTaxonomia`, `LoteImportacao`, `ItemImportacao`, `DivergenciaExterna`, `StatusLote`, `TipoTaxonomia`, `ResultadoCandidata` e `GeradorCodigoLote` (`IMP-00001`, no padrão de `GeradorCodigoQuestao`).
- [x] 1.8 Criar `importacao/QuestaoExterna`, `AlternativaExterna` e `ResultadoTriagem` — a representação neutra que a triagem consome.
- [x] 1.9 Implementar `triagem/DetectorTipo` (RF-42), recusando o ambíguo em vez de adivinhar.
- [x] 1.10 Implementar `triagem/ValidadorEstrutura` (RF-43), sem compactar letras em nenhuma hipótese.
- [x] 1.11 Implementar `triagem/ValidadorLimites` (RF-44).
- [x] 1.12 Implementar `triagem/DetectorRecursoNaoSuportado` (RF-45), sobre todos os campos textuais.
- [x] 1.13 Implementar `triagem/AvaliadorComentario` (RF-46), gerando pendência e não descarte.
- [x] 1.14 Implementar `triagem/TriagemCompatibilidade`, orquestrando os cinco na ordem tipo → estrutura → limites → recurso → comentário e devolvendo resultado, motivo em português e pendências.
- [x] 1.15 Escrever os testes TU-01 a TU-13 e TI-01 a TI-03.

## Detalhes de implementação

Ver TechSpec, seções *Modelos de dados* → **Esquema de banco — `V19__importacao_questoes_externas.sql`** (DDL completo, com a justificativa da coluna gerada e do `valor_externo_pai`), *Design de implementação* → **Principais interfaces** (assinaturas de `TriagemCompatibilidade`, `DetectorTipo`, `DetectorRecursoNaoSuportado` e `AvaliadorComentario`), *Considerações técnicas* → **ADR-1** (procedência como colunas, não satélite), **ADR-6** (normalização com uma única implementação, em SQL) e **RT-6** (a dependência de `certame_unaccent` ser `immutable` é precedente da `V4`, não risco novo), e *Sequenciamento do desenvolvimento* → passos 1 e 2.

A premissa defensiva sobre certo/errado — a documentação da QAPI não descreve `CE` e Q-6 segue aberta — está em *Pontos de integração* → **Premissas explícitas onde a documentação não responde**: `CE` exige nenhuma opção preenchida **e** gabarito em `{C, E, CERTO, ERRADO, V, F, VERDADEIRO, FALSO}`.

## Critérios de aceitação relacionados

- CA-11
- CA-14
- CA-15
- CA-16
- CA-17
- CA-28

## Testes da tarefa

### Testes de unidade

- [x] TU-01 — `detectaMultiplaEscolhaComDuasOuMaisOpcoes`
- [x] TU-02 — `detectaCertoErradoSemOpcoesComGabaritoEquivalente`
- [x] TU-03 — `recusaCombinacaoAmbiguaComoTipoNaoSuportado`
- [x] TU-04 — `descartaAlternativasComLacunaSemCompactarLetras`
- [x] TU-05 — `descartaGabaritoSemAlternativaCorrespondente`
- [x] TU-06 — `recusaEnunciadoForaDosLimitesDoModelo`
- [x] TU-07 — `descartaMarcacaoHtmlEmQualquerCampo`
- [x] TU-08 — `descartaDelimitadorLatex`
- [x] TU-09 — `descartaReferenciaImagemOuAnexo`
- [x] TU-10 — `descartaTabelaECaractereDeControle`
- [x] TU-11 — `sinalizaComentarioAusenteSemDescartarCandidata`
- [x] TU-12 — `reprovaComentarioApenasComALetraDoGabarito`
- [x] TU-13 — `reprovaComentarioQueRepeteOEnunciado`

### Testes de integração

- [x] TI-01 — `aplicaMigracaoV19PreservandoQuestoesExistentesComoInternas`
- [x] TI-02 — `geraHashDeEnunciadoParaTodoOAcervoPreExistente`
- [x] TI-03 — `recusaSegundaGravacaoDoMesmoParFonteEIdentificador`

## Arquivos relevantes

- `backend/src/main/resources/db/migration/V19__importacao_questoes_externas.sql`
- `backend/src/main/resources/db/migration-dev/V912__seed_importacao_dev.sql`
- `backend/src/main/java/br/com/certame/questao/{Fonte,ProcedenciaQuestao}.java` · `questao/Questao.java` (modificado)
- `backend/src/main/java/br/com/certame/importacao/{QuestaoExterna,AlternativaExterna,ResultadoCandidata}.java`
- `backend/src/main/java/br/com/certame/importacao/triagem/{TriagemCompatibilidade,DetectorTipo,ValidadorEstrutura,ValidadorLimites,DetectorRecursoNaoSuportado,AvaliadorComentario,ResultadoTriagem}.java`
- `backend/src/main/java/br/com/certame/importacao/textobase/{TextoBase,TextoBaseRepository}.java`
- `backend/src/main/java/br/com/certame/importacao/taxonomia/{MapeamentoTaxonomia,TipoTaxonomia,MapeamentoTaxonomiaRepository}.java`
- `backend/src/main/java/br/com/certame/importacao/lote/{LoteImportacao,ItemImportacao,StatusLote,LoteImportacaoRepository,ItemImportacaoRepository,GeradorCodigoLote}.java`
- `backend/src/main/java/br/com/certame/importacao/divergencia/{DivergenciaExterna,DivergenciaExternaRepository}.java`
- `backend/src/test/java/br/com/certame/importacao/triagem/*Test.java` · `backend/src/test/java/br/com/certame/importacao/MigracaoImportacaoIntegracaoTest.java`
