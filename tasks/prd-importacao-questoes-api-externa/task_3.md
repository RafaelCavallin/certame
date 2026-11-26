# Tarefa 3.0: Mapeamento de taxonomia e deduplicação

## Visão geral

O ponto mais delicado da funcionalidade e a barreira que impede acervo duplicado.

O **mapeamento** existe porque "Informática" no fornecedor pode ser "Tecnologia da Informação" no Certame. A resolução tem ordem fixa — mapeamento explícito, depois nome normalizado, depois sigla (só para banca e órgão), depois **não resolvido** — e uma proibição absoluta: **nenhum nó de taxonomia é criado a partir de valor externo**. A árvore do Certame é curada por gente, e a importação não tem caminho para escrever nela. Disciplina sem mapeamento descarta a candidata; assunto, banca e órgão sem mapeamento entram como pendência.

A **deduplicação** tem duas barreiras independentes: a identidade externa `(fonte, identificador externo)`, garantida por índice único no banco, e o hash do enunciado normalizado, que a tarefa 1.0 já instalou como coluna gerada. O Java **nunca** normaliza enunciado por conta própria — consulta com a mesma função SQL `certame_hash_enunciado`. Uma única implementação da normalização é o que impede a classe de bug em que o código e o banco divergem, e é o que faz a colisão contra questão digitada à mão valer para todo o acervo pré-existente.

`HashConteudoExterno` entra aqui porque é a mesma família de problema: um SHA-256 sobre a forma canônica do conteúdo externo, que a tarefa 6.0 usará para detectar divergência.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `ResolvedorTaxonomia`, `DetectorDuplicidade` e `HashConteudoExterno` são unidade pura com repositórios mockados; sobem a `@SpringBootTest @Testcontainers` apenas TI-09, TI-10 e TI-11, que precisam do SQL real e da contagem do catálogo.
- `postgresql-table-design` — a consulta de colisão usa a função imutável e o índice `questao_hash_enunciado_idx`; a fila de pendências usa o índice parcial `item_importacao_pendencia_idx`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras que incidem aqui:

- **Fronteira por domínio** (`code-standards.md` §3) — `importacao/` **não acessa** os repositórios de `catalogo/`. A resolução consulta o catálogo pelos serviços existentes; nada em `catalogo/` é alterado.
- **Regra 3 / R-6** — nomes em português sem acento; motivos e mensagens de pendência em português do Brasil.
- **Camadas** — `MapeamentoTaxonomiaService` concentra regra, transação e autorização; nada de regra em controller (que vem na tarefa 7.0).
- **DTOs** são `record` imutável em `importacao/dto/`, com mensagem em português em cada *constraint*.
- **Testes** — prioridade alta: um falso positivo de dedup descarta questão legítima e um falso negativo duplica acervo. Caminho feliz **e** bordas.
</rules>

<requirements>
- **RF-47** — dicionário persistente com fonte, tipo, valor externo normalizado (sem acento, sem caixa, sem espaço redundante), o valor original legível e a entidade interna. Um valor externo por tipo e por fonte — e, em assunto, escopado pela matéria (`valor_externo_pai`), porque *Hibernate Cache* só significa algo sob *Tecnologia Java*.
- **RF-48** — ordem de resolução: mapeamento explícito → nome normalizado → sigla (banca e órgão) → não resolvido.
- **RF-49** — disciplina sem mapeamento ⇒ `SEM_MAPEAMENTO` e a candidata **não é gravada**; assunto, banca e órgão sem mapeamento ⇒ importa sem eles, com pendência sinalizada.
- **RF-50** — listar pendências com a contagem de candidatas afetadas, criar, corrigir e remover mapeamento. Corrigir vale para as **próximas** importações e **nunca** reclassifica retroativamente.
- **RF-59** — identidade externa única; reexecutar a mesma importação não cria segunda cópia.
- **RF-60** — colisão de hash de enunciado contra todo o acervo, **em qualquer status, inclusive rascunho e excluído logicamente**, e contra o próprio lote em formação.
- **RF-63** — hash do conteúdo externo (enunciado, alternativas, gabarito, classificação), estável para o mesmo conteúdo e diferente quando o gabarito muda.
- **RN-15** — taxonomia interna nunca é criada a partir de valor externo desconhecido.
</requirements>

## Subtarefas

- [ ] 3.1 Implementar `taxonomia/NormalizadorValorExterno` — a chave do dicionário.
- [ ] 3.2 Implementar `taxonomia/ResolvedorTaxonomia` e o *record* `TaxonomiaResolvida`, com a ordem de RF-48 e sem nenhum caminho de escrita em `catalogo/`.
- [ ] 3.3 Implementar `taxonomia/MapeamentoTaxonomiaService`: criar, corrigir, remover (recusando com `MAPEAMENTO_EM_USO` quando for o caso) e listar pendências com contagem, a partir de `item_importacao`.
- [ ] 3.4 Criar os DTOs `PendenciaMapeamento`, `RequisicaoMapeamento` e `MapeamentoDetalhe`.
- [ ] 3.5 Criar `comum/excecao/MapeamentoEmUsoException`.
- [ ] 3.6 Implementar `dedup/DetectorDuplicidade`: identidade externa, colisão por hash no acervo (via `certame_hash_enunciado(:enunciado)`, sem normalizar em Java) e repetição dentro do lote em formação.
- [ ] 3.7 Acrescentar a `QuestaoRepository` as consultas de identidade externa, colisão por hash e contagem de pendências.
- [ ] 3.8 Implementar `dedup/HashConteudoExterno` (SHA-256 sobre a forma canônica).
- [ ] 3.9 Registrar a auditoria de `MAPEAMENTO_CRIADO` e `MAPEAMENTO_CORRIGIDO` por `RegistradorAuditoria`, com identificador e decisão — nunca conteúdo bruto.
- [ ] 3.10 Escrever os testes TU-14 a TU-18, TU-21, TU-22, TU-28 e TI-09 a TI-11.

## Detalhes de implementação

Ver TechSpec, seções *Design de implementação* → **Principais interfaces** (`ResolvedorTaxonomia`, `DetectorDuplicidade`), *Modelos de dados* → `mapeamento_taxonomia` (com a justificativa de `valor_externo_pai`), **`PendenciaMapeamento` e `RequisicaoMapeamento`**, *Considerações técnicas* → **ADR-6** (por que o hash é calculado no banco) e **RT-3** (o catálogo de órgãos da QAPI vem contaminado com bancas — órgão não é obrigatório, então isso vira pendência e não descarte), e *Sequenciamento do desenvolvimento* → passo 5.

O estágio 4 do pipeline (taxonomia) roda **antes** do estágio 5 (duplicidade) e ambos **depois** da triagem: ver *Visão dos componentes* → **Pipeline de uma candidata**.

## Critérios de aceitação relacionados

- CA-10
- CA-12
- CA-13
- CA-16
- CA-17
- CA-19

## Testes da tarefa

### Testes de unidade

- [ ] TU-14 — `resolveTaxonomiaPeloMapeamentoExplicitoAntesDoNome`
- [ ] TU-15 — `resolveBancaPorSiglaQuandoNaoHaMapeamento`
- [ ] TU-16 — `naoCriaNoDeTaxonomiaQuandoValorExternoEDesconhecido`
- [ ] TU-17 — `descartaCandidataQuandoDisciplinaNaoTemMapeamento`
- [ ] TU-18 — `importaSemAssuntoBancaOuOrgaoComPendenciaSinalizada`
- [ ] TU-21 — `hashExternoMudaQuandoGabaritoMuda`
- [ ] TU-22 — `hashExternoEEstavelParaOMesmoConteudo`
- [ ] TU-28 — `descartaRepeticaoDentroDoProprioLote`

### Testes de integração

- [ ] TI-09 — `descartaCandidataIdenticaAQuestaoCadastradaAMao`
- [ ] TI-10 — `registraDisciplinaNaoMapeadaNaFilaDePendencias`
- [ ] TI-11 — `mantemCatalogoIntactoDepoisDeQualquerImportacao`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/importacao/taxonomia/{NormalizadorValorExterno,ResolvedorTaxonomia,TaxonomiaResolvida,MapeamentoTaxonomiaService}.java`
- `backend/src/main/java/br/com/certame/importacao/dedup/{DetectorDuplicidade,HashConteudoExterno}.java`
- `backend/src/main/java/br/com/certame/importacao/dto/{PendenciaMapeamento,RequisicaoMapeamento,MapeamentoDetalhe}.java`
- `backend/src/main/java/br/com/certame/comum/excecao/MapeamentoEmUsoException.java`
- `backend/src/main/java/br/com/certame/questao/QuestaoRepository.java` (modificado)
- `backend/src/test/java/br/com/certame/importacao/taxonomia/*Test.java` · `importacao/dedup/*Test.java` · `importacao/*IntegracaoTest.java`
