# Tarefa 4.0: Texto-base e sua exibição no estudo e no simulado

## Visão geral

Questões de interpretação, legislação e estudo de caso compartilham um texto de apoio, que a QAPI entrega **por referência** (`"texto": "QTXT566467"`) em endpoint próprio. Sem ele, a questão chega ao estudante quebrada — ela cita um texto que ninguém vê.

O texto-base é entidade própria (ADR-3), não concatenação ao enunciado: concatenar duplicaria texto longo em cada questão, inflaria o `tsvector` de busca e impediria exibir o texto uma única vez para uma série. O custo aceito é que `QuestaoDaSerie` e `ItemCorrecao` mudam, e com eles três telas.

Esta tarefa é a que toca fluxo crítico já estável (RI-10, RT-7), e a mitigação é estrutural: o campo é **opcional** e o comportamento com `textoBase` nulo é idêntico ao de hoje, em cada uma das telas. Como `simulado/` reusa `RespostaSessao`, a mesma alteração atende sessão de estudo e Modo Prova de uma vez.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `ResolvedorTextoBase` é unidade pura com o fornecedor substituído por *fake*; TI-17 sobe a Testcontainers para provar a gravação única compartilhada.
- `postgresql-table-design` — a deduplicação se apoia no índice único parcial `texto_base_externo_uk`, criado na tarefa 1.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras que incidem aqui:

- **RN-18 / O-5** — a resolução do texto-base é a **única** ponta em que uma chamada externa acontece dentro do fluxo de importação. Nenhum fluxo de estudo, caderno, simulado, desempenho ou flashcard passa a chamar o fornecedor; o texto é lido do banco local.
- **RN-20** — conteúdo externo é entrada não confiável: o conteúdo do texto-base passa pela **mesma** triagem de RF-45 da tarefa 1.0, antes de qualquer gravação.
- **Fronteira por domínio** — `estudo/` não passa a conhecer `importacao/`: recebe o texto já materializado no `MontadorRespostaSessao`.
- **DTOs** — `TextoBaseResumo` é `record` imutável; campo ausente é serializado como `null`, nunca omitido.
- **Arquivos inalterados por decisão de projeto** — `estudo/MontadorSerie`, `estudo/SerieRepository` e `simulado/MontadorProvaSimulado` não são tocados. Alterá-los é sinal de que o desenho escorregou.
</rules>

<requirements>
- **RF-55** — texto-base como entidade própria, com conteúdo, referência e identificador externo, reaproveitado por todas as questões que o referenciam; uma questão referencia no máximo um.
- **RF-56** — busca do texto durante a importação; falha ou ausência do texto referenciado ⇒ candidata **não importada**, com motivo `INCOMPLETA`. O conteúdo passa pela triagem de recurso não suportado.
- **RF-57** — exibição ao estudante imediatamente **antes** do enunciado, na sessão de estudo, no simulado e nas telas de correção e resultado, com a mesma renderização de texto puro.
- **RF-58** — deduplicação por fonte e identificador externo: o mesmo texto referenciado por vinte questões é gravado **uma vez** — e, uma vez gravado, não custa nova chamada ao fornecedor.
- O campo `referencia` guarda o `fonte` da QAPI (`"Machado de Assis"`); o nome foi trocado no Certame para não colidir com `fonte` = procedência.
</requirements>

## Subtarefas

- [ ] 4.1 Implementar `textobase/ResolvedorTextoBase`: procura primeiro o texto já gravado localmente por `(fonte, identificador externo)`; só chama o fornecedor quando falta.
- [ ] 4.2 Submeter o conteúdo obtido a `DetectorRecursoNaoSuportado` antes de gravar, e classificar a candidata como `INCOMPLETA` quando o texto for indisponível, `404` ou reprovado.
- [ ] 4.3 Criar o DTO `TextoBaseResumo` (`id`, `titulo`, `conteudo`, `referencia`).
- [ ] 4.4 Acrescentar `textoBase` a `estudo/dto/QuestaoDaSerie` e a `estudo/dto/ItemCorrecao`, sempre presente no JSON e `null` quando não houver.
- [ ] 4.5 Ajustar `estudo/MontadorRespostaSessao` e `estudo/CorrecaoProvaService` para preencher o texto quando a questão tiver um — e nada além disso quando não tiver.
- [ ] 4.6 Acrescentar `textoBase` a `questao/dto/QuestaoDetalhe`, para a ficha administrativa.
- [ ] 4.7 Escrever os testes TU-32, TU-33 e TI-17, e conferir que a suíte existente de estudo e simulado continua verde sem alteração de expectativa.

## Detalhes de implementação

Ver TechSpec, seções *Modelos de dados* → **`TextoBaseResumo`** (com o caso `{"textoBase": null}` explicitado) e **Mapeamento QAPI `/api/textos/{codigo}` → `TextoBase`**, *Design de implementação* → **Principais interfaces** (`ResolvedorTextoBase`), *Visão dos componentes* → **Pipeline de uma candidata** (o texto-base é o **estágio 6**, de propósito: buscar o texto de uma candidata que seria descartada por duplicidade é crédito jogado fora), *Considerações técnicas* → **ADR-3** e **RT-4**/**RT-7**, e *Sequenciamento do desenvolvimento* → passo 7.

O componente Angular `texto-base-cartao` e a alteração de `questao-cartao` e `correcao-prova` são da tarefa 9.0; esta entrega o lado servidor e o contrato.

## Critérios de aceitação relacionados

- CA-22
- CA-23

## Testes da tarefa

### Testes de unidade

- [ ] TU-32 — `reaproveitaTextoBaseJaGravadoSemChamarFornecedor`
- [ ] TU-33 — `descartaCandidataQuandoTextoBaseEIndisponivel`

### Testes de integração

- [ ] TI-17 — `gravaUmaVezOTextoBaseCompartilhadoPorVariasQuestoes`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/importacao/textobase/ResolvedorTextoBase.java`
- `backend/src/main/java/br/com/certame/importacao/dto/TextoBaseResumo.java`
- `backend/src/main/java/br/com/certame/estudo/dto/{QuestaoDaSerie,ItemCorrecao}.java` (modificados)
- `backend/src/main/java/br/com/certame/estudo/{MontadorRespostaSessao,CorrecaoProvaService}.java` (modificados)
- `backend/src/main/java/br/com/certame/questao/dto/QuestaoDetalhe.java` (modificado)
- `backend/src/test/java/br/com/certame/importacao/textobase/*Test.java`
