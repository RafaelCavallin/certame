# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** importação de questões de concursos a partir de APIs externas
> **Versão:** 2.0 — reescrita a partir de `prompt_prd_integracao_apis_questoes_certame.md` e da análise do código-fonte atual
> **Numeração:** continua a do `PRD.md` da raiz, que hoje termina em **RF-37** e **RN-11**
> **Escopo do documento:** o *quê* e o *porquê*. A arquitetura detalhada, as assinaturas de API, o DDL final e o plano de sprints são responsabilidade da TechSpec derivada deste PRD.

---

## Visão geral

O Certame precisa de acervo. Hoje toda questão nasce do cadastro manual em `admin/questoes` — um professor digita enunciado, alternativas, gabarito e comentário, envia para revisão e um administrador publica. Esse caminho produz qualidade alta e volume baixo: para chegar a um acervo com dezenas de milhares de questões publicadas, o cadastro manual é o gargalo estrutural do produto.

Esta funcionalidade acrescenta um segundo caminho de entrada: **importar questões de fornecedores externos, normalizá-las para o modelo interno do Certame e submetê-las ao mesmo fluxo editorial que uma questão digitada à mão**. A questão importada não é um espelho da API de origem — ela é copiada, traduzida para a taxonomia curada do Certame, persistida no PostgreSQL local e, a partir daí, vive como questão nativa: aparece na busca, entra em sessões de estudo, cadernos e simulados, alimenta desempenho e gera flashcards. O Certame **não vira proxy**: nenhuma sessão de estudo, em nenhum momento, depende de o fornecedor estar no ar.

### Problema

1. **Volume.** O cadastro exclusivamente manual não escala para o acervo que o produto exige.
2. **Cobertura.** Faltam questões de bancas e anos específicos que o estudante espera encontrar (Cebraspe, FGV, FCC etc.), e nenhum professor consegue transcrever provas inteiras em ritmo útil.
3. **Sem perder a régua editorial.** Importar não pode significar despejar conteúdo cru no acervo. Uma questão importada sem comentário, com taxonomia errada, com gabarito ambíguo ou dependente de uma imagem que o Certame não exibe é pior que questão nenhuma: ela chega ao estudante quebrada e destrói a confiança no produto.

### Estado atual do Certame — o que o código mostra

Esta seção é fato verificado no repositório, não hipótese. Ela existe porque toda decisão adiante se apoia nela.

**Modelo de questão** (`V2__banco_questoes.sql`, `questao/Questao.java`)

| Aspecto | Como está hoje |
| --- | --- |
| Identidade interna | `codigo` único, no formato `Q-00000`, emitido por `GeradorCodigoQuestao` a partir da sequência `questao_codigo_seq` (`V5` corrigiu o `lpad` para não truncar acima de 99999) |
| Tipos suportados | `tipo` com *check* `in ('ME','CE')` — múltipla escolha e certo/errado. Nada além disso |
| Alternativas | Tabela `alternativa`: `letra in ('A','B','C','D','E')`, `ordem between 1 and 5`, únicas por questão. `MontadorQuestao` exige **ordens contíguas de 1 a N, sem repetição, no máximo 5**, e deriva a letra da ordem |
| Classificação | `disciplina_id`, `assunto_id`, `banca_id`, `orgao_id` — **todos anuláveis**; `ano smallint between 1990 and 2100`; `dificuldade in ('FACIL','MEDIA','DIFICIL')` |
| Conteúdo | `enunciado text not null`, `gabarito text`, `comentario text`. `RequisicaoQuestao` limita enunciado a 10–20000 caracteres e comentário a 20000 |
| Autoria | `autor_id uuid not null references usuario(id)` — **toda questão tem um autor obrigatório** |
| Procedência externa | **Não existe.** Nenhuma coluna de fonte, identificador externo, data de importação ou sincronização |
| Busca | Coluna gerada `busca tsvector` a partir do enunciado, com `certame_unaccent` (`V4`), índice GIN |

**Fluxo editorial** (`questao/PublicacaoService.java`, `questao/ValidadorPublicacao.java`)

- Status: `RASCUNHO → EM_REVISAO → PUBLICADA`, com `despublicar` devolvendo de `PUBLICADA` para `EM_REVISAO`.
- `enviarParaRevisao` exige `ROLE_PROFESSOR+`; `publicar` e `despublicar` exigem `ROLE_ADMIN+`.
- Para publicar, `ValidadorPublicacao` exige: **gabarito válido** (em `CE`, exatamente `CERTO` ou `ERRADO`), **comentário não vazio**, **ao menos duas alternativas em `ME`** e **disciplina preenchida**.
- A mesma regra está gravada no banco como *check constraint* `questao_publicada_completa` — publicar sem disciplina, sem gabarito ou sem comentário é impossível mesmo por SQL direto.
- Toda transição grava auditoria e métrica (`MetricasPublicacao`).

**Taxonomia** (`catalogo/`)

- `DisciplinaTaxonomiaService` e `AssuntoTaxonomiaService` criam, renomeiam, arquivam e excluem nós da árvore, sempre sob `ROLE_PROFESSOR+`, com nome único por `lower(nome)` (e por disciplina, no caso de assunto) e exclusão recusada quando há questões vinculadas. **A árvore é curada por gente.**
- `CatalogoService` faz o CRUD de bancas e órgãos (`ROLE_ADMIN+` para escrita), com nome único por `lower(nome)`. `V3__seed_catalogo.sql` já traz 8 bancas e 10 órgãos. **Nem `banca` nem `orgao` têm coluna de origem.**
- `MontadorQuestao` resolve disciplina, assunto, banca e órgão **por UUID**, nunca por nome, e recusa assunto que não pertença à disciplina informada.

**Estudo e histórico** (`V7__sessao_estudo.sql`, `estudo/`)

- A série de uma sessão sai de `QuestaoRepository.buscarIdsPublicadas*`, que filtra `status = PUBLICADA and excluido_em is null` — RN-01 do PRD raiz está implementada em consulta, não só em texto.
- `resposta` é **imutável no banco**: `create rule resposta_sem_update ... do instead nothing` e `resposta_sem_delete`. O histórico de desempenho não pode ser alterado nem apagado por nenhuma via da aplicação.
- `QuestaoService.excluir` recusa exclusão de questão com respostas (`QUESTAO_EM_USO`).

**Apresentação** (`frontend/src/app/features/study/questao-cartao/questao-cartao.html` e demais)

- Enunciado, alternativas e comentário são renderizados por **interpolação** (`{{ ... }}`). Uma varredura por `innerHTML`, `DomSanitizer` e `bypassSecurity` em todo o frontend **não retorna nenhuma ocorrência**.
- Consequência direta e decisiva para este PRD: **o Certame não renderiza HTML, LaTeX, tabelas nem imagens em questões.** Um `<p>` vindo do fornecedor apareceria literalmente na tela do estudante, e ainda poluiria o `tsvector` de busca.

**Integrações e infraestrutura**

- **A aplicação não faz hoje nenhuma chamada HTTP de saída** — não há `RestClient`, `RestTemplate`, `WebClient` nem `java.net.http` no código. Esta será a primeira.
- Já existem tarefas agendadas (`ExpiradorSessoes`, `ReconciliadorAgregados`, `ExpiradorTentativaSimulado`), então `@Scheduled`/`@Async` são padrão estabelecido no projeto.
- Segredos seguem `PropriedadesSeguranca` (`@ConfigurationProperties` + `@Validated`) e variáveis de ambiente obrigatórias em staging/produção.
- Auditoria genérica pronta: tabela `auditoria` (`ator_id`, `acao`, `entidade`, `entidade_id`, `payload jsonb`) e `RegistradorAuditoria`.
- Migrações Flyway vão até **`V18__simulado_composicao_dificuldade.sql`**; a próxima versão livre é **V19**. Seeds de desenvolvimento usam a faixa `V9xx`.
- Observabilidade por Micrometer já é hábito do projeto (`EstudoObservabilidade`, `MetricasPublicacao`, `Metrics.counter` em taxonomia).

**Nomes já ocupados.** O termo *origem* está em uso com dois significados diferentes — `OrigemSessao` (`LIVRE`/`CADERNO`/`SIMULADO`) e `OrigemFlashcard`. Para não criar um terceiro sentido, este PRD chama a procedência externa de **fonte**.

### Fluxo proposto

```text
API do fornecedor  (QAPI no primeiro adaptador)
   ↓                       cliente HTTP isolado, com chave, timeout e limite de chamadas
DTO específico do fornecedor        ← confinado ao pacote do adaptador, não circula
   ↓                       tradução
QuestaoExterna (representação neutra)   ← única forma que o resto do processo enxerga
   ↓
Triagem de compatibilidade    → descarta tipo/recurso/estrutura não suportados
   ↓
Mapeamento de taxonomia       → disciplina, assunto, banca, órgão do Certame (nunca cria nós)
   ↓
Deduplicação                  → (fonte, identificador externo) e hash do enunciado
   ↓
Questão do Certame + texto-base + procedência   ← persistida no PostgreSQL local
   ↓
Fluxo editorial existente     → EM_REVISAO (ou RASCUNHO, se faltar comentário) → PUBLICADA
   ↓
Estudo · cadernos · simulados · desempenho · flashcards   ← sem nenhuma chamada externa
```

A validação do prompt contra o código confirma esta cadeia, com dois ajustes que o código impõe:

1. **A triagem vem antes do mapeamento**, não depois. Não faz sentido gastar mapeamento de taxonomia (e revisão humana) numa questão que será descartada por depender de imagem.
2. **A importação reutiliza as validações da criação manual** (`MontadorQuestao`, `ValidadorPublicacao`) em vez de reimplementá-las. Duas réguas editoriais divergentes seriam a primeira fonte de bug do produto.

---

## Objetivos

| # | Objetivo | Métrica | Meta |
| --- | --- | --- | --- |
| O-1 | Provar que existe fornecedor com contrato compatível com o modelo do Certame | Taxa de aproveitamento do PoC (candidatas importáveis ÷ candidatas recuperadas) | ≥ 60% em uma amostra de 50 |
| O-2 | Reduzir o custo de entrada de questão no acervo | Tempo humano por questão publicada, importação × cadastro manual | ≤ 25% do tempo do cadastro manual |
| O-3 | Aumentar o acervo publicado sem perder régua editorial | Questões importadas publicadas no primeiro mês de MVP | ≥ 500 |
| O-4 | Não degradar a experiência do estudante | Questões importadas publicadas que recebem reporte de conteúdo quebrado | 0 |
| O-5 | Manter o estudo independente do fornecedor | Chamadas a API externa dentro de qualquer fluxo de estudo, caderno, simulado ou desempenho | 0, verificado em teste |
| O-6 | Preservar a árvore de taxonomia curada | Disciplinas ou assuntos criados automaticamente pela importação | 0 |
| O-7 | Tornar a importação auditável | Candidatas processadas sem resultado classificado no relatório | 0 |

### Não objetivos

- **Não** é objetivo espelhar o acervo do fornecedor nem manter as duas bases sincronizadas em tempo real.
- **Não** é objetivo publicar automaticamente. Toda questão importada passa por decisão humana antes de chegar ao estudante.
- **Não** é objetivo substituir o cadastro manual, que continua sendo o caminho de questão autoral e de conteúdo que o fornecedor não cobre.
- **Não** é objetivo ampliar o que o Certame sabe renderizar (imagem, LaTeX, tabela) nesta entrega.
- **Não** é objetivo trocar o identificador interno da questão pelo do fornecedor.

### Métricas de sucesso

- **Aproveitamento por fornecedor:** importáveis ÷ recuperadas, por lote e acumulado. Abaixo de 40% em três lotes seguidos, o fornecedor entra em reavaliação.
- **Cobertura de mapeamento:** percentual de valores externos de disciplina que já têm mapeamento interno. Meta: ≥ 90% após a curadoria inicial.
- **Pendência editorial:** percentual de questões importadas que entram em `RASCUNHO` por falta de comentário. Acompanhado semanalmente; é o custo humano real da importação.
- **Latência do lote:** tempo total para processar 50 candidatas. Meta: < 60 s.
- **Reincidência:** questões duplicadas efetivamente gravadas no acervo. Meta absoluta: 0.
- **Divergências externas:** alterações detectadas no fornecedor e pendentes de decisão humana há mais de 30 dias. Meta: 0.

---

## Histórias de usuário

- **US1 — Administrador, importar um lote.** Como administrador, quero pedir ao Certame que busque questões de um fornecedor filtrando por disciplina, banca e ano, para aumentar o acervo sem digitar questão por questão.
- **US2 — Administrador, entender o que aconteceu.** Como administrador, quero ver ao fim de cada importação quantas questões foram encontradas, quantas entraram, e o motivo exato de cada descarte, para saber se o fornecedor vale o custo e o que precisa de curadoria.
- **US3 — Administrador, triar antes de publicar.** Como administrador, quero revisar as questões que acabaram de entrar, com as pendências sinalizadas, e publicar ou devolver cada uma, para que nada chegue ao estudante sem decisão minha.
- **US4 — Professor, completar o que faltou.** Como professor, quero encontrar as questões importadas que estão sem comentário e escrever a resolução, para que possam ser publicadas.
- **US5 — Administrador, mapear taxonomia.** Como administrador, quero declarar que a disciplina "Informática" do fornecedor corresponde a "Tecnologia da Informação" no Certame, e que esse mapeamento valha para as próximas importações, para não reclassificar as mesmas questões repetidamente.
- **US6 — Administrador, não importar duas vezes.** Como administrador, quero poder repetir uma importação sem medo, sabendo que a mesma questão não será gravada de novo, para poder reprocessar um lote que falhou no meio.
- **US7 — Administrador, saber que a fonte mudou.** Como administrador, quero ser avisado quando uma questão já publicada tiver gabarito ou enunciado alterado no fornecedor, e decidir eu mesmo se aplico a mudança, para que nenhuma questão respondida por estudantes mude sozinha.
- **US8 — Estudante, não perceber diferença.** Como estudante, quero que a questão importada tenha enunciado completo, alternativas corretas, gabarito e resolução, exatamente como qualquer outra, para estudar sem tropeçar em conteúdo quebrado.
- **US9 — Estudante, resolver questão com texto-base.** Como estudante, quero ler o texto de apoio junto da questão que se refere a ele, para conseguir respondê-la.
- **US10 — Super-admin, guardar a credencial.** Como super-admin, quero cadastrar a chave de acesso do fornecedor sem que ela apareça em tela, em log ou em resposta da API, para não vazar credencial contratada.
- **US11 — Responsável de negócio, não infringir contrato.** Como responsável pelo produto, quero que a importação em produção só seja possível depois das confirmações de licenciamento do fornecedor, para não persistir nem exibir conteúdo sem direito de uso.

---

## Principais funcionalidades

### 1. Conexão com fornecedores externos

Um adaptador por fornecedor, isolado do domínio. O primeiro é a **QAPI**; a arquitetura precisa aceitar o segundo sem tocar em `questao/`.

- **RF-38** — Configuração de fornecedor por ambiente: URL base, chave de acesso, tempo limite de conexão e leitura, limite de chamadas por minuto e situação (ativo/inativo). A chave vem de variável de ambiente ou configuração segura e **nunca** transita para o frontend nem aparece em log, tela ou resposta de API.
- **RF-39** — Consulta ao fornecedor com os filtros que ele suportar (disciplina/matéria, assunto, banca, órgão, ano, quantidade). Filtro pedido que o fornecedor não suporta é aplicado localmente sobre o resultado, e o relatório registra que a filtragem foi local.
- **RF-40** — Tradução obrigatória do payload do fornecedor para a representação neutra **`QuestaoExterna`**, contendo: identificador externo, enunciado, alternativas com letra e ordem, gabarito, comentário, disciplina, assunto, banca, órgão, ano, cargo, referência de texto-base e URL pública quando houver. **DTOs específicos do fornecedor não saem do pacote do adaptador.**
- **RF-41** — Tratamento de indisponibilidade: erro de rede, tempo esgotado, credencial inválida, limite de requisições excedido e resposta malformada são classificados, registrados e não derrubam o lote inteiro — o processamento segue com as candidatas já obtidas e o relatório informa a interrupção.

> **Alternativas avaliadas para a representação neutra.** (a) Deixar o DTO da QAPI circular até o serviço de importação — menos código agora, acoplamento do domínio ao fornecedor depois; (b) mapear direto de DTO para a entidade `Questao` — impede triagem e mapeamento antes de decidir persistir; (c) `QuestaoExterna` intermediária. **Recomendação: (c)**, porque triagem, mapeamento e deduplicação precisam operar sobre algo que não seja ainda uma `Questao` (que exige autor, código e disciplina resolvida) e não seja mais o DTO do fornecedor.

### 2. Triagem de compatibilidade

Executada **antes** de qualquer gravação e antes do mapeamento de taxonomia. Reprova cedo e barato.

- **RF-42** — Detecção do tipo quando o fornecedor não o informa: duas ou mais opções preenchidas ⇒ `ME`; nenhuma opção preenchida e gabarito equivalente a certo/errado ⇒ `CE`. Combinação ambígua ou tipo fora de `ME`/`CE` ⇒ descarte com motivo **tipo não suportado**. O Certame nunca importa em silêncio um tipo que não sabe exibir.
- **RF-43** — Validação estrutural, espelhando `MontadorQuestao`: em `ME`, entre 2 e 5 alternativas, letras de A a E, **ordens contíguas sem lacuna** (uma opção vazia no meio da sequência é descarte, nunca compactação — compactar deslocaria as letras e invalidaria o gabarito); gabarito correspondente a uma alternativa existente; em `CE`, gabarito normalizado para exatamente `CERTO` ou `ERRADO`.
- **RF-44** — Validação de limites do modelo: enunciado entre 10 e 20 000 caracteres, comentário até 20 000, ano entre 1990 e 2100. Fora disso, descarte com motivo **incompleta**.
- **RF-45** — Detecção de recurso não suportado, sobre enunciado, alternativas, comentário e texto-base: marcação HTML, delimitadores de LaTeX, referência a imagem ou anexo, tabela e caracteres de controle. Qualquer ocorrência ⇒ descarte com motivo **recurso não suportado**. Justificativa: o frontend renderiza tudo por interpolação de texto puro, sem sanitizador — o conteúdo apareceria cru na tela e contaminaria o índice de busca.
- **RF-46** — Avaliação objetiva do comentário/resolução: presente, com conteúdo textual mínimo, sem marcação, e diferente de texto vazio de conteúdo (por exemplo, apenas a letra do gabarito, "sem comentário", traços ou repetição do enunciado). Comentário reprovado **não descarta a questão** — gera pendência (ver RF-51).

### 3. Mapeamento de taxonomia

O ponto mais delicado da funcionalidade. "Informática" no fornecedor pode ser "Tecnologia da Informação" no Certame, ou o contrário.

- **RF-47** — Dicionário persistente de mapeamentos, com fonte, tipo de taxonomia (disciplina, assunto, banca, órgão), valor externo normalizado (sem acento, sem caixa, sem espaço redundante) e a entidade interna correspondente. Um valor externo por tipo e por fonte.
- **RF-48** — Resolução durante a importação, nesta ordem: mapeamento explícito → correspondência exata por nome normalizado (e por sigla, no caso de banca e órgão) → **não resolvido**. **Nenhuma disciplina, assunto, banca ou órgão é criado automaticamente.**
- **RF-49** — Efeito da falta de mapeamento, alinhado ao que o modelo exige:

  | Taxonomia | Obrigatória para publicar? | Sem mapeamento |
  | --- | --- | --- |
  | Disciplina | **Sim** (`ValidadorPublicacao` e *check* no banco) | **Descarta** a candidata, motivo **sem mapeamento** |
  | Assunto | Não | Importa sem assunto, com pendência sinalizada na triagem |
  | Banca | Não | Importa sem banca, com pendência sinalizada |
  | Órgão | Não | Importa sem órgão, com pendência sinalizada |

- **RF-50** — Gestão dos mapeamentos pelo administrador: listar valores externos ainda não mapeados (com a contagem de candidatas afetadas), criar, corrigir e remover mapeamento. Corrigir um mapeamento vale para as **próximas** importações e nunca reclassifica retroativamente questões já publicadas — reclassificação é edição de questão, com auditoria própria.

### 4. Entrada no acervo e fluxo editorial

- **RF-51** — Status de entrada da questão importada:
  - candidata aprovada em tudo, com comentário válido ⇒ **`EM_REVISAO`**, direto na fila de revisão do administrador;
  - candidata aprovada com **comentário ausente ou reprovado** ⇒ **`RASCUNHO`**, com a pendência registrada, aguardando um professor escrever a resolução.

  A régua editorial atual continua intocada: a *check constraint* `questao_publicada_completa` impede, no próprio banco, que qualquer uma delas seja publicada sem disciplina, gabarito e comentário. **Nenhuma questão importada é publicada automaticamente.**
- **RF-52** — Autoria: a questão importada tem como autor o usuário que disparou a importação, satisfazendo `autor_id not null` sem inventar usuário fantasma, e carrega separadamente a procedência externa.
- **RF-53** — Registro de procedência em toda questão: fonte, identificador externo, URL externa quando existir, data de importação, data da última verificação e identificação do lote que a trouxe. Questões cadastradas manualmente permanecem com fonte interna. **O `codigo` interno (`Q-00000`) continua sendo a identidade da questão no Certame** — o identificador do fornecedor jamais o substitui.
- **RF-54** — Exibição da procedência na ficha administrativa da questão, incluindo link para a origem quando disponível. Se o contrato do fornecedor exigir atribuição pública (gate G-06), a fonte também é exibida ao estudante.

### 5. Texto-base

Questões de interpretação, legislação e estudo de caso compartilham um texto de apoio, que a QAPI entrega por referência (`"texto": "QTXT566467"`) em endpoint próprio.

- **RF-55** — Texto-base modelado como **entidade própria**, com conteúdo, fonte e identificador externo, reaproveitado por todas as questões que o referenciam; uma questão referencia no máximo um texto-base.
- **RF-56** — Busca do texto-base durante a importação. Falha ou ausência do texto referenciado ⇒ a candidata **não é importada** (motivo **incompleta**): questão que cita um texto que o estudante não vê é questão quebrada. O conteúdo do texto-base passa pela mesma triagem de recurso não suportado (RF-45).
- **RF-57** — Exibição do texto-base ao estudante imediatamente antes do enunciado, na sessão de estudo, no simulado e nas telas de correção e resultado, com a mesma renderização de texto puro do restante.
- **RF-58** — Deduplicação de texto-base por fonte e identificador externo: o mesmo texto referenciado por vinte questões é gravado uma vez.

> **Alternativas avaliadas.** (a) Concatenar o texto-base ao enunciado — impacto zero no domínio, mas duplica texto longo em cada questão, infla o `tsvector` de busca e impede exibir o texto uma única vez para uma série de questões; (b) rejeitar questões com texto-base no MVP — seguro, porém descarta boa parte de Português, Interpretação e Direito; (c) entidade própria. **Decisão do produto: (c)**, assumindo o custo de alterar o cartão de questão e as telas de correção, em troca da apresentação correta e da reutilização.

### 6. Deduplicação e idempotência

- **RF-59** — Identidade externa: o par **fonte + identificador externo** identifica unicamente uma questão importada, com unicidade garantida no banco. Reexecutar a mesma importação **não** cria segunda cópia.
- **RF-60** — Barreira adicional por conteúdo: hash do enunciado normalizado (minúsculas, sem acento, sem pontuação redundante, espaços colapsados) comparado com todo o acervo em qualquer status, inclusive rascunho e excluído logicamente, e com o próprio lote em formação. Colisão ⇒ descarte com motivo **já existente**. Isso cobre a mesma questão vinda de dois fornecedores e a que já havia sido digitada à mão.
- **RF-61** — Memória de candidatas avaliadas: toda candidata processada fica registrada com seu resultado, permitindo reprocessar lotes, explicar decisões passadas e não reoferecer o que já foi descartado.
- **RF-62** — Falha parcial: cada candidata é processada e persistida de forma independente. Erro em uma não desfaz as anteriores; o lote termina com resultado **parcial**, discriminado por motivo. Reexecutar retoma de onde parou, sem duplicar.

### 7. Alterações posteriores no fornecedor

- **RF-63** — Detecção de divergência: ao reencontrar uma questão já importada, o Certame compara o conteúdo externo (enunciado, alternativas, gabarito, classificação) com o que foi importado, por hash, e classifica o resultado como **alteração externa detectada**.
- **RF-64** — Tratamento por situação da questão local:

  | Situação local | Comportamento |
  | --- | --- |
  | `RASCUNHO` ou `EM_REVISAO`, **sem respostas** | Conteúdo atualizado automaticamente, com registro em auditoria |
  | `PUBLICADA`, ou com qualquer resposta de estudante | **Nunca alterada em silêncio.** Gera divergência pendente, visível ao administrador, que decide aplicar, ignorar ou despublicar |
  | Removida no fornecedor | A questão local **não é apagada**; recebe sinalização de origem indisponível para decisão humana |
  | Anulada no fornecedor | Sinalizada como divergência de alta prioridade; despublicar continua sendo ação humana (`despublicar` já existe) |

- **RF-65** — Integridade do histórico: nenhuma decisão sobre divergência altera ou apaga registros de `resposta` — o que o banco já garante por regra de `do instead nothing` — nem recalcula desempenho retroativo.

### 8. Relatório de importação e auditoria

- **RF-66** — Toda importação produz um relatório com: fornecedor, filtros pedidos, quem disparou, início e fim, total encontrado, total importado e a distribuição por resultado. Resultados possíveis:

  | Resultado | Significado |
  | --- | --- |
  | `IMPORTADA` | Gravada em `EM_REVISAO` |
  | `IMPORTADA_COM_PENDENCIA` | Gravada em `RASCUNHO`, aguardando comentário |
  | `JA_EXISTENTE` | Identificador externo ou hash de enunciado já no acervo |
  | `DUPLICADA_NO_LOTE` | Repetida dentro do próprio lote |
  | `SEM_MAPEAMENTO` | Disciplina externa sem correspondência interna |
  | `TIPO_NAO_SUPORTADO` | Não é `ME` nem `CE`, ou é ambígua |
  | `RECURSO_NAO_SUPORTADO` | HTML, LaTeX, imagem, anexo ou tabela |
  | `INCOMPLETA` | Falta enunciado, gabarito, alternativa, texto-base, ou excede limites |
  | `ALTERACAO_EXTERNA` | Já importada e com conteúdo diferente na fonte |
  | `ERRO` | Falha técnica na obtenção ou no processamento |

- **RF-67** — Cada candidata processada tem exatamente um resultado e, quando descartada, um motivo legível em português. **Nenhuma candidata fica sem classificação** (O-7).
- **RF-68** — Histórico de importações consultável, com filtro por período, fornecedor, autor e situação, e o detalhamento por candidata de cada lote.
- **RF-69** — Registro na auditoria existente (`RegistradorAuditoria`) de: início de importação, criação de questão importada, criação e correção de mapeamento, aplicação e descarte de divergência externa. O payload de auditoria guarda identificador externo, hashes e decisões — **não** cópias do conteúdo bruto do fornecedor.

### 9. Tela mínima de triagem

O MVP entrega interface suficiente para revisar o que entrou — sem ela ninguém decide nada — e nada além disso.

- **RF-70** — Tela de importações em `admin/`, restrita a `ROLE_ADMIN` e `ROLE_SUPER_ADMIN`: formulário do pedido (fornecedor, filtros, quantidade de 1 a 50), lista dos lotes já executados e relatório de cada lote conforme RF-66.
- **RF-71** — Lista das questões do lote com o resultado de cada candidata, e, para as importadas, acesso direto à edição de questão que já existe (`admin/questoes/:id`) — o MVP **reaproveita** a tela de edição, não cria um editor paralelo.
- **RF-72** — Sinalizadores visíveis por questão importada: sem comentário, sem assunto, sem banca, sem órgão, divergência externa pendente. Os mesmos sinalizadores aparecem como filtro na listagem de questões.
- **RF-73** — Fila de pendências de mapeamento: valores externos não mapeados, quantidade de candidatas afetadas e ação para mapear (RF-50).

### Regras de negócio

| # | Regra |
| --- | --- |
| RN-12 | Nenhuma questão importada é publicada automaticamente. Ela entra em `EM_REVISAO` (ou `RASCUNHO`, se faltar comentário) e só chega ao estudante por publicação humana. RN-01 continua valendo integralmente. |
| RN-13 | As regras editoriais atuais não são flexibilizadas para conteúdo importado: disciplina, gabarito válido, comentário não vazio e mínimo de duas alternativas em `ME` seguem exigidos para publicar, pelo mesmo validador e pela mesma *check constraint* do banco. |
| RN-14 | A identidade de uma questão externa é o par **fonte + identificador externo**, único no acervo. Reexecutar uma importação não gera cópia. O `codigo` interno permanece a identidade da questão no Certame. |
| RN-15 | Taxonomia interna **nunca** é criada a partir de valor externo desconhecido. Disciplina sem mapeamento impede a importação da candidata; assunto, banca e órgão sem mapeamento entram como pendência de revisão. |
| RN-16 | O Certame não importa conteúdo que não sabe exibir. Questão com HTML, LaTeX, imagem, anexo ou tabela é descartada com motivo, nunca importada parcialmente ou com o recurso removido. |
| RN-17 | Questão publicada ou já respondida por algum estudante **jamais** é alterada por sincronização automática. A divergência externa vira pendência de decisão humana. |
| RN-18 | Nenhum fluxo de estudo, caderno, simulado, desempenho ou flashcard consulta API externa. A indisponibilidade do fornecedor afeta apenas a importação. |
| RN-19 | A chave de acesso do fornecedor é segredo de backend: configurada por variável de ambiente, nunca devolvida por API, nunca exibida em tela, nunca escrita em log. |
| RN-20 | Conteúdo vindo de fornecedor é entrada não confiável: validado, normalizado e limitado em tamanho antes de qualquer gravação. |
| RN-21 | Importação em produção depende das confirmações de licenciamento (G-01 a G-04, e G-06 quando aplicável). Sem elas, a funcionalidade permanece habilitada apenas fora de produção. |
| RN-22 | Um lote pede no máximo 50 questões no MVP. Entregar menos que o pedido é resultado válido, desde que cada descarte esteja discriminado por motivo. |

### Requisitos não funcionais

| # | Requisito |
| --- | --- |
| RNF-01 | Tempo limite de conexão e de leitura configurados por fornecedor, com padrão de 5 s e 15 s. Nenhuma chamada externa fica pendurada indefinidamente. |
| RNF-02 | Limite de chamadas por minuto respeitando o contratado com o fornecedor, com espera e nova tentativa limitada e com recuo progressivo. Excedido o limite, o lote termina como parcial e informa a causa. |
| RNF-03 | Processar 50 candidatas em menos de 60 s, incluindo a busca de textos-base. |
| RNF-04 | A importação roda fora do caminho de request do estudante e não compete com o fluxo de estudo por transações longas. |
| RNF-05 | Toda alteração de esquema entra por migração Flyway versionada e imutável, a partir de V19; `ddl-auto` permanece em `validate`. |
| RNF-06 | Cobertura de testes mínima de 80% no código novo, conforme `.agents/rules/tests.md`, incluindo teste que comprova a ausência de chamada externa nos fluxos de estudo (O-5). |
| RNF-07 | Métricas Micrometer por fornecedor: candidatas recuperadas, importadas, descartadas por motivo, duração do lote e falhas de comunicação — no padrão já usado por `EstudoObservabilidade` e `MetricasPublicacao`. |
| RNF-08 | Logs estruturados, em português, sem chave de acesso e sem despejo do payload bruto do fornecedor. |
| RNF-09 | Toda a interface e todas as mensagens em português do Brasil; a tela de triagem segue o design system Nocturne e os tokens de `styles.css`, sem biblioteca de UI de terceiros. |
| RNF-10 | Autorização validada no servidor com `@PreAuthorize` em todo endpoint novo, mesmo quando a interface já esconde a ação. |
| RNF-11 | O contrato de API novo é registrado na TECH-SPEC §6 na mesma alteração que o implementa (regra 4 do `AGENTS.md`). |

---

## Escopo por fase

### PoC — validar o fornecedor antes de construir

Pequeno, controlado, descartável. Roda apenas em desenvolvimento.

1. Conecta à QAPI com chave real e recupera **20 a 50 questões**, preferencialmente de Tecnologia da Informação e de bancas relevantes (FGV, Cebraspe, FCC).
2. Traduz o payload para `QuestaoExterna`.
3. Executa triagem, tentativa de mapeamento de taxonomia e verificação de duplicidade contra o acervo real.
4. **Não persiste questão alguma.**
5. Emite relatório no formato de RF-66, por exemplo:

```text
50 questões encontradas

42 importáveis
 3 já existentes
 2 sem comentário válido
 2 sem mapeamento de disciplina
 1 recurso não suportado
```

6. Produz também: a lista dos valores externos de disciplina e assunto sem mapeamento (insumo direto da curadoria) e uma amostra de 5 questões traduzidas para conferência visual do enunciado, alternativas, gabarito e comentário.

### MVP — importar de verdade, com revisão humana

Inclui RF-38 a RF-73 restritos a **um fornecedor (QAPI)**, lote de até 50 questões, disparo manual por administrador, triagem síncrona, tela mínima de triagem e detecção de divergência **apenas quando uma reimportação reencontra a questão** (sem verificação agendada).

### Fases seguintes (não fazem parte desta entrega)

Interface administrativa completa com pré-visualização antes de gravar · importação em lote grande e assíncrona com fila · sincronização periódica agendada · segundo e terceiro fornecedores · comparação e deduplicação entre fornecedores · suporte a imagem, LaTeX e tabela · importação de provas completas com cargo e prova modelados · geração assistida de comentários por IA · métricas de qualidade por fornecedor · edição de texto-base pela interface.

---

## Critérios de aceitação

### Do PoC

- **CA-01** — Dado o ambiente de desenvolvimento configurado com a chave da QAPI, quando o PoC é executado, então ele recupera entre 20 e 50 questões e termina sem exceção não tratada. *(US1)*
- **CA-02** — Dado um lote recuperado, quando o PoC termina, então **100% das candidatas** têm exatamente um resultado classificado e todo descarte tem motivo legível. *(O-7, RF-67)*
- **CA-03** — Dado o relatório do PoC, quando ele é lido, então a taxa de importáveis é **≥ 60%** da amostra. Abaixo disso, o PoC é **reprovado** e o fornecedor entra em reavaliação antes de qualquer investimento no MVP.
- **CA-04** — Dado que o PoC roda duas vezes seguidas com o mesmo pedido, quando os relatórios são comparados, então a classificação de cada candidata é idêntica (processo determinístico). *(US6)*
- **CA-05** — Dado o fim da execução, quando o banco é inspecionado, então **nenhuma questão, texto-base ou mapeamento foi gravado**.
- **CA-06** — Dado o log completo da execução, quando ele é inspecionado, então a chave de acesso não aparece em nenhuma linha. *(US10, RN-19)*
- **CA-07** — Dado o relatório, quando as pendências são lidas, então a lista de valores externos de disciplina e assunto não mapeados está presente, com a contagem de candidatas afetadas por valor. *(US5)*
- **CA-08** — Reprovação explícita do PoC: mais de 50% das candidatas sem comentário válido, **ou** mais de 50% com recurso não suportado, **ou** contrato incompatível com `ME`/`CE` — qualquer um desses resultados reprova o fornecedor, mesmo com CA-03 atendido.

### Do MVP

- **CA-09** — Dado um administrador autenticado, quando ele dispara uma importação de 50 questões com filtros de disciplina, banca e ano, então o lote é processado e o relatório de RF-66 é exibido com o total por resultado. *(US1, US2)*
- **CA-10** — Dada uma candidata aprovada em tudo, quando ela é importada, então a questão é gravada em `EM_REVISAO`, com código interno próprio, alternativas na ordem original, autor igual a quem disparou a importação e procedência registrada. *(US3, RF-51, RF-52, RF-53)*
- **CA-11** — Dada uma candidata sem comentário válido, quando ela é importada, então a questão é gravada em `RASCUNHO` com a pendência sinalizada, e a tentativa de publicá-la é recusada com a mensagem do validador atual. *(US4, RN-13)*
- **CA-12** — Dada uma disciplina externa sem mapeamento, quando a importação a encontra, então a candidata **não é gravada**, o resultado é `SEM_MAPEAMENTO` e o valor externo aparece na fila de pendências de mapeamento. *(US5, RN-15)*
- **CA-13** — Dada qualquer importação, quando o catálogo é inspecionado depois, então **nenhuma disciplina, assunto, banca ou órgão foi criado automaticamente**. *(O-6, RN-15)*
- **CA-14** — Dada uma questão com marcação HTML, fórmula LaTeX, referência a imagem ou tabela em qualquer campo, quando a triagem a avalia, então ela é descartada com resultado `RECURSO_NAO_SUPORTADO` e não chega ao banco. *(RN-16)*
- **CA-15** — Dada uma questão com opções A, B, D e E preenchidas e C vazia, quando a triagem a avalia, então ela é descartada — as letras não são compactadas nem realinhadas. *(RF-43)*
- **CA-16** — Dado um lote já importado, quando exatamente o mesmo pedido é executado de novo, então nenhuma questão nova é criada e todas aparecem como `JA_EXISTENTE`. *(US6, RN-14)*
- **CA-17** — Dada uma questão idêntica já cadastrada manualmente, quando uma candidata com o mesmo enunciado normalizado é processada, então ela é descartada como `JA_EXISTENTE`. *(RF-60)*
- **CA-18** — Dada uma falha do fornecedor no meio do lote, quando o processamento é interrompido, então as candidatas já processadas permanecem gravadas, o lote é marcado como parcial com a causa, e a reexecução não duplica nada. *(RF-62)*
- **CA-19** — Dada uma questão importada **já publicada e já respondida**, quando o fornecedor altera o gabarito e uma reimportação a reencontra, então o conteúdo local **não muda**, uma divergência pendente é criada e o administrador é quem decide. *(US7, RN-17)*
- **CA-20** — Dada uma questão importada em `EM_REVISAO` sem nenhuma resposta, quando o fornecedor corrige o enunciado e uma reimportação a reencontra, então o conteúdo é atualizado automaticamente e a atualização fica em auditoria. *(RF-64)*
- **CA-21** — Dada qualquer decisão sobre divergência externa, quando o histórico é inspecionado, então nenhum registro de `resposta` foi alterado ou removido. *(RN-17, RF-65)*
- **CA-22** — Dada uma questão importada com texto-base, quando um estudante a resolve em sessão de estudo ou simulado, então o texto-base é exibido antes do enunciado, e o mesmo texto é reaproveitado pelas demais questões que o referenciam. *(US9, RF-55, RF-57, RF-58)*
- **CA-23** — Dada uma questão que referencia texto-base indisponível na fonte, quando a triagem a avalia, então ela é descartada como `INCOMPLETA`. *(RF-56)*
- **CA-24** — Dado o fornecedor completamente fora do ar, quando um estudante inicia sessão de estudo, abre caderno, faz simulado ou consulta desempenho, então tudo funciona normalmente e **nenhuma chamada externa é feita**. *(US8, O-5, RN-18)*
- **CA-25** — Dado um usuário com perfil de estudante ou professor, quando ele tenta acessar qualquer endpoint de importação, então recebe 403, mesmo com a interface escondendo a ação. *(RNF-10)*
- **CA-26** — Dada a resposta de qualquer endpoint da funcionalidade, quando ela é inspecionada, então a chave de acesso do fornecedor não aparece em nenhum campo, em nenhuma forma. *(US10, RN-19)*
- **CA-27** — Dada uma questão importada publicada, quando um estudante a resolve, então ela é indistinguível de uma questão nativa: entra na série, aceita resposta, exibe gabarito e comentário, conta no desempenho e permite gerar flashcard. *(US8)*
- **CA-28** — Dada a ficha administrativa de uma questão importada, quando ela é aberta, então fonte, identificador externo, data de importação e link externo (quando houver) estão visíveis. *(RF-54)*

---

## Experiência do usuário

### Atores

| Ator | Papel nesta funcionalidade |
| --- | --- |
| **Super-admin** | Configura o fornecedor e a credencial (fora da interface, por variável de ambiente); não participa da operação diária |
| **Administrador** | Dispara importações, lê relatórios, mapeia taxonomia, decide sobre divergências, publica ou devolve questões |
| **Professor** | Recebe as questões em `RASCUNHO` sem comentário e escreve a resolução; revisa e corrige conteúdo importado como já faz com questão manual |
| **Estudante** | Não interage com a importação. É o beneficiário: mais acervo, com a mesma qualidade de apresentação |
| **Responsável de negócio** | Obtém as confirmações de licenciamento que liberam a produção (gates G-01 a G-07) |

### Fluxos principais

**Importar um lote (administrador).** Menu administrativo → Importações → *Nova importação* → escolhe fornecedor, disciplina, banca, ano e quantidade → confirma → acompanha o processamento → lê o relatório: quantas entraram, quantas foram descartadas e por quê → clica em uma questão importada e cai na tela de edição de questão que já existe.

**Resolver pendência de mapeamento (administrador).** Relatório aponta "3 candidatas sem mapeamento de disciplina: *Conhecimentos de Informática*" → abre a fila de pendências → associa o valor externo à disciplina interna correspondente → reexecuta a importação → as candidatas antes descartadas agora entram.

**Completar comentário (professor).** Lista de questões filtrada por *sem comentário* → abre a questão → escreve a resolução → envia para revisão → o administrador publica.

**Decidir uma divergência (administrador).** Aviso de divergência pendente → compara lado a lado o conteúdo local e o externo → aplica a mudança, ignora, ou despublica a questão para revisão. O histórico de respostas segue intocado em qualquer das três escolhas.

### Interface e acessibilidade

- Tradução fiel do design system **Nocturne**: tokens de `styles.css`, sem cor, fonte, espaçamento, raio ou sombra inventados, sem biblioteca de UI de terceiros.
- O relatório é uma tabela navegável por teclado, com o motivo do descarte em texto — não apenas por cor ou ícone.
- Sinalizadores de pendência têm rótulo textual acessível a leitor de tela, não somente ícone.
- Estados vazio, carregando, parcial e de erro têm mensagem própria em português: "Nenhuma importação ainda", "Importando 18 de 50", "Lote parcial: o fornecedor interrompeu em 34 de 50".
- Ações destrutivas ou irreversíveis (aplicar divergência em questão publicada) pedem confirmação explícita.
- A tela de triagem é operável em telas menores; a tabela rola horizontalmente sem quebrar a página.

---

## Restrições técnicas de alto nível

### Premissas

- **P-1** — O fornecedor tem API REST estável, autenticada por chave, com filtro por disciplina/matéria e paginação. **A confirmar contra a documentação real antes da implementação.**
- **P-2** — O exemplo de payload da QAPI no prompt (`_id`, `orgao`, `banca`, `ano`, `cargo`, `materia`, `assunto`, `enunciado`, `opcaoA`–`opcaoE`, `gabarito`, `comentario`, `texto`) é **ilustrativo, não contrato**. A TechSpec só pode ser escrita depois de validar o contrato real.
- **P-3** — A árvore de disciplinas e assuntos do Certame já está minimamente povoada antes da primeira importação; sem taxonomia interna, toda candidata cai em `SEM_MAPEAMENTO`.
- **P-4** — O volume do MVP (lotes de até 50) não exige fila nem processamento distribuído.
- **P-5** — Existe plano contratado com o fornecedor compatível com o volume pretendido.

### Restrições

- **R-1** — Stack fixa: Angular 22 · Spring Boot 4.1 / Java 25 · PostgreSQL 18 + Flyway · Maven.
- **R-2** — O modelo suporta exclusivamente `ME` (2 a 5 alternativas, letras A–E) e `CE`. Ampliar isso está fora desta entrega.
- **R-3** — O frontend renderiza texto puro. Suporte a HTML, LaTeX, tabela ou imagem exigiria sanitização, renderizador e revisão de segurança — outro projeto.
- **R-4** — Migração de banco só por Flyway, versionada e imutável, a partir de V19. Nenhuma migração aplicada é editada.
- **R-5** — Nenhum endpoint fora do contrato da TechSpec §6 sem atualizar o documento na mesma alteração.
- **R-6** — Código, nomes de tabela e de coluna em português sem acento, `snake_case` no banco; interface e mensagens em português do Brasil.
- **R-7** — Esta é a primeira integração HTTP de saída do projeto: a escolha do cliente e da política de resiliência é decisão nova, não continuação de padrão existente.

### Modelo conceitual das alterações

Cinco conceitos novos, todos periféricos ao domínio de questão — que **não é reescrito**:

1. **Fonte** — enumeração da procedência (`INTERNA`, `QAPI`, `QUEST_API`, `API_DAS_QUESTOES`). Chamada de *fonte*, e não de *origem*, porque `OrigemSessao` e `OrigemFlashcard` já usam esse nome com outro sentido no código.
2. **Procedência da questão** — fonte, identificador externo, URL externa, data de importação, data da última verificação, hash do conteúdo externo, hash do enunciado normalizado.
3. **Texto-base** — conteúdo compartilhado por várias questões, com fonte e identificador externo próprios.
4. **Mapeamento de taxonomia** — fonte, tipo, valor externo normalizado, entidade interna, quem mapeou e quando.
5. **Lote de importação e seus itens** — cabeçalho do pedido e uma linha por candidata avaliada, com resultado, motivo e vínculo com a questão criada. Serve simultaneamente de relatório, memória de descarte e trilha de auditoria.

### Impactos previstos

*Alto nível; o desenho detalhado é da TechSpec.*

**Banco de dados** — Colunas de procedência e vínculo de texto-base na tabela `questao`, com valor padrão que preserva as questões existentes como internas. Restrição de unicidade sobre fonte + identificador externo. Tabelas novas: texto-base, mapeamento de taxonomia, lote de importação e item de importação. Índices para busca de duplicidade por hash. Tudo em migração nova a partir de V19; nada de alteração destrutiva em tabela existente.

**Backend** — Novo domínio de importação, isolado, com subpacote por fornecedor, seguindo a fronteira por domínio da TechSpec §4. O domínio `questao/` ganha campos e nenhuma regra nova de publicação. A importação **reutiliza** `MontadorQuestao` e `ValidadorPublicacao` em vez de duplicar validação. Endpoints administrativos sob `/api/v1/admin`, protegidos por `@PreAuthorize`. Configuração de fornecedor no padrão `@ConfigurationProperties` + `@Validated` já usado em `PropriedadesSeguranca`.

**Frontend** — Rota administrativa nova, protegida por `perfilGuard`, com a tela mínima de triagem. O cartão de questão da sessão de estudo e as telas de correção e resultado passam a exibir texto-base quando houver. A ficha administrativa da questão passa a exibir procedência. A listagem de questões ganha filtro por pendência. Nenhuma tela de estudante passa a depender de serviço externo.

### Segurança

- Chave de acesso apenas no backend, por variável de ambiente ou configuração segura, nunca versionada, nunca enviada ao Angular, nunca devolvida por endpoint, nunca registrada em log.
- Tempo limite de conexão e leitura em toda chamada externa; nova tentativa limitada, com recuo progressivo, e apenas para operações idempotentes.
- Respeito ao limite de requisições do fornecedor.
- Todo conteúdo externo é entrada não confiável: validado quanto a tipo, tamanho e caracteres de controle, normalizado em Unicode, e recusado quando traz marcação. Como o Certame não renderiza HTML, a recusa de marcação também é a defesa contra injeção de conteúdo.
- Endpoints de importação restritos a `ROLE_ADMIN` e `ROLE_SUPER_ADMIN`, validado no servidor.
- Logs sem segredo e sem despejo de payload bruto de terceiro.

### Observabilidade e auditoria

- Métricas por fornecedor: candidatas recuperadas, importadas, descartadas por motivo, duração do lote, falhas de comunicação, divergências detectadas.
- Auditoria nos eventos de RF-69, na tabela `auditoria` que já existe.
- O item de importação é a trilha completa: para qualquer questão importada, é possível responder *de onde veio, quando, em que lote, com que classificação e por decisão de quem*.
- Logs em português, com identificador do lote em cada linha, para reconstruir uma execução inteira.

### Licenciamento e gates para produção

Bloqueadores de negócio, não observações jurídicas. Cada um exige confirmação **por escrito** do fornecedor.

| Gate | Confirmação necessária | Bloqueia |
| --- | --- | --- |
| G-01 | Persistir as questões no banco do Certame | Todo o MVP em produção |
| G-02 | Exibi-las a usuários cadastrados | Todo o MVP em produção |
| G-03 | Exibi-las a usuários pagantes ou assinantes | Publicação para base paga |
| G-04 | Continuar usando as questões já importadas após cancelar o serviço | Todo o MVP em produção |
| G-05 | Enviar questão individual a serviço de IA generativa para produzir explicação, sem treinamento nem ajuste fino | Funcionalidade futura de comentário assistido |
| G-06 | Requisitos de atribuição da fonte | Formato da exibição ao estudante (RF-54) |
| G-07 | Restrições de redistribuição | Exportação, compartilhamento e API pública futura |

**Nenhum direito é presumido.** Enquanto G-01 a G-04 não estiverem confirmados, a importação fica habilitada apenas fora de produção, com dados descartáveis (RN-21). O PoC, por não persistir nada, pode ocorrer antes das confirmações — desde que o contrato de uso da API permita a consulta em si.

---

## Fora do escopo

- Importar a base inteira do fornecedor.
- Sincronização automática, periódica ou em larga escala.
- Interface administrativa sofisticada, com pré-visualização antes de gravar e triagem em massa.
- Importação genérica de CSV ou Excel.
- Geração automática de comentários por IA.
- Suporte a imagem, áudio, vídeo, anexo, tabela ou LaTeX.
- Dois ou mais fornecedores ativos simultaneamente no MVP.
- Comparação e deduplicação semântica entre fornecedores.
- Modelagem de cargo e de prova como entidades.
- Edição de texto-base pela interface administrativa.
- Qualquer alteração no fluxo de estudo que consulte API externa em tempo real — explicitamente proibida por RN-18.

---

## Riscos e mitigações

| # | Risco | Impacto | Mitigação |
| --- | --- | --- | --- |
| RI-1 | O contrato real da QAPI diverge do payload assumido | Retrabalho na TechSpec | Validar a documentação real **antes** de escrever a TechSpec; o PoC existe exatamente para isso |
| RI-2 | Licenciamento negado ou restrito | Funcionalidade inviável em produção | Gates G-01 a G-04 tratados como bloqueadores; buscar confirmação em paralelo ao PoC, não depois |
| RI-3 | Grande parte do acervo de TI depende de imagem, diagrama ou tabela | Aproveitamento baixo demais para justificar o investimento | CA-08 reprova o fornecedor cedo; medir a proporção já no PoC |
| RI-4 | Comentários do fornecedor rasos ou ausentes em massa | Fila enorme de trabalho manual para professores | RF-46 mede objetivamente; CA-08 reprova acima de 50%; a métrica de pendência editorial acompanha o custo |
| RI-5 | Taxonomia externa muito distante da interna | Esforço de curadoria maior que o ganho | Dicionário persistente e reutilizável (RF-47) e fila de pendências (RF-73) tornam o custo decrescente |
| RI-6 | Duplicidade não detectada entre fornecedor e cadastro manual | Estudante vê a mesma questão duas vezes | Dupla barreira: identificador externo e hash de enunciado normalizado (RF-59, RF-60) |
| RI-7 | Alteração externa aplicada sobre questão já respondida | Perda de integridade do desempenho | RN-17 e RF-64; `resposta` já é imutável no banco |
| RI-8 | Limite de requisições do plano atingido no meio do lote | Importações incompletas | RNF-02, resultado parcial explícito e reexecução idempotente |
| RI-9 | Primeira integração de saída do projeto sem padrão estabelecido | Decisões técnicas inconsistentes | ADR-5 antes de implementar |
| RI-10 | Texto-base como entidade nova toca telas de estudo já estáveis | Regressão em fluxo crítico | Campo opcional, comportamento inalterado quando ausente, cobertura de teste nas telas afetadas |

---

## Plano de rollout

| Fase | Entrega | Sai quando |
| --- | --- | --- |
| **F0 — Validação** | Documentação real da QAPI levantada; confirmações de licenciamento solicitadas | Contrato conhecido e gates em andamento |
| **F1 — PoC** | Integração descartável, sem persistência, com relatório | CA-01 a CA-08 atendidos |
| **F2 — Curadoria** | Mapeamentos iniciais de disciplina e assunto, a partir das pendências do PoC | Cobertura de mapeamento ≥ 90% na amostra |
| **F3 — MVP backend** | Migração, domínio de importação, deduplicação, texto-base, relatório | CA-09 a CA-21, CA-23 a CA-26 atendidos |
| **F4 — MVP interface** | Tela mínima de triagem, sinalizadores, fila de mapeamento, exibição de texto-base e procedência | CA-22, CA-27 e CA-28 atendidos |
| **F5 — Produção controlada** | Habilitação em produção após G-01 a G-04; primeiros lotes acompanhados de perto | Meta O-3 (500 publicadas) atingida sem incidente de conteúdo |

F1 e F2 não começam sem F0. F5 não começa sem os gates, independentemente do estado do código.

---

## Questões em aberto

| # | Questão | Precisa ser respondida antes de |
| --- | --- | --- |
| Q-1 | **Qual serviço é a "QAPI"?** O `APIs.txt` do repositório lista `quest.api.br`, `apidasquestoes.com.br` e `concursosgpt.com.br` — nenhum deles se chama QAPI. É um quarto fornecedor, ou outro nome para um destes? | F0 |
| Q-2 | Existe documentação pública ou plano já contratado com o fornecedor escolhido? | F0 |
| Q-3 | A API suporta filtro por disciplina, banca e ano, e paginação estável? Se não, todo filtro vira filtragem local e o custo por questão útil sobe muito | F1 |
| Q-4 | Qual o limite de requisições e o volume mensal do plano? | F1 |
| Q-5 | O fornecedor informa explicitamente o tipo da questão, ou ele sempre precisa ser inferido (RF-42)? | F1 |
| Q-6 | Em questões de certo/errado, qual o formato exato do gabarito (`C`/`E`, `CERTO`/`ERRADO`, `V`/`F`)? | F1 |
| Q-7 | Qual o contrato do endpoint de texto-base, e ele é estável entre questões que compartilham o mesmo texto? | F3 |
| Q-8 | O fornecedor expõe alguma marca de atualização (versão, data de modificação) que dispense comparar hashes para detectar divergência? | F3 |
| Q-9 | Há exigência contratual de atribuição visível ao estudante (G-06)? Isso muda o cartão de questão | F4 |
| Q-10 | Quando existir sincronização agendada, quem é o autor das questões criadas sem usuário logado? | Fase futura (ver ADR-4) |

---

## Decisões que precisam de ADR

| # | Decisão | Por que merece registro |
| --- | --- | --- |
| **ADR-1** | Procedência como colunas na própria `questao` **versus** tabela satélite 1:1 | Afeta o índice de unicidade, as consultas de deduplicação e o tamanho da tabela mais acessada do sistema. Recomendação: colunas na `questao`, porque toda questão tem exatamente uma procedência e o modelo atual já concentra atributos ali |
| **ADR-2** | `QuestaoExterna` como representação neutra, com DTOs de fornecedor confinados ao adaptador | É a decisão que impede o domínio de acoplar à QAPI e o que torna o segundo fornecedor barato |
| **ADR-3** | Texto-base como entidade própria, em vez de concatenar ao enunciado | Altera o domínio de questão, a API de estudo e três telas do frontend. Precisa ficar registrado o trade-off aceito |
| **ADR-4** | Ator responsável por questões importadas sem usuário logado, quando houver sincronização agendada | `autor_id` é `not null`; um usuário de sistema muda a semântica de autoria em todo o produto |
| **ADR-5** | Cliente HTTP de saída e política de resiliência (tempo limite, nova tentativa, recuo, limite de chamadas) | Primeira integração de saída do projeto; vira o padrão para todas as seguintes |
| **ADR-6** | Estratégia de deduplicação por hash de enunciado normalizado — o que exatamente é normalizado | Falso positivo descarta questão legítima; falso negativo duplica acervo. A regra precisa ser explícita e versionada |
