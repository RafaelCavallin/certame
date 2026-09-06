# Especificação técnica

> **Funcionalidade:** Banco de questões e taxonomia · **PRD:** `tasks/prd-banco-de-questoes/prd.md` · **Fase:** F1 — Base
> Deriva de `TECH-SPEC.md` v1.0 (§3–§6 e §8, recortados para RF-18, RF-19, RF-20, RN-01 e RN-03).
> Depende de `tasks/prd-acesso-e-conta/techspec.md`, que funda `SegurancaConfig`, `ManipuladorErrosGlobal`, `ErroApi` e a entidade `Usuario`.

## Resumo

Esta é a segunda funcionalidade da F1 e a primeira a exercitar de verdade a fundação criada em `prd-acesso-e-conta`: perfis por `@PreAuthorize`, envelope `problem+json` e migração Flyway. Ela acrescenta dois pacotes de domínio — `catalogo` (disciplina, assunto, banca, órgão) e `questao` (questão, alternativa, publicação, busca) — mais a tabela transversal `auditoria`, exigida pelas restrições do PRD para toda ação administrativa.

O desenho central é o ciclo de vida da questão. `status` (`RASCUNHO` → `EM_REVISAO` → `PUBLICADA`) é o único portão entre o trabalho editorial e o acervo visível (RN-01), e as transições vivem em endpoints próprios em vez de num `PUT` genérico: `enviar-revisao` é do professor, `publicar` e `despublicar` são do administrador. A validação de publicação (disciplina, gabarito, comentário e ao menos duas alternativas no caso de múltipla escolha) é um serviço isolado que devolve erro campo a campo, para que CA-06 se traduza numa lista de pendências na tela, não numa mensagem genérica. Despublicar apenas move o status: nada em `resposta` é tocado, o que garante CA-08 por construção.

A busca combina duas estratégias numa consulta só: `codigo` por prefixo, para o fluxo "corrigir por código" do PRD, e `to_tsvector('portuguese', enunciado)` com índice GIN para o texto. A paginação é offset com contagem exata no envelope `{content, page, size, totalElements}` da §6, sustentada por índice composto sobre os filtros — decisão do usuário, registrada em *Principais decisões*.

A taxonomia é uma árvore de duas camadas com contagem derivada, nunca armazenada (§5 da `TECH-SPEC.md`). Assunto e disciplina ganham `arquivado_em`: excluir um nó com questões vinculadas é recusado com um código de erro que a interface traduz na oferta de arquivar (CA-11). Renomear é um `update` de `nome`; como o vínculo é por `id`, CA-12 é consequência do modelo e não de código adicional.

No frontend, a entrega são três telas em tradução 1:1 do protótipo — lista de questões, formulário de página cheia e árvore de matérias — sobre o `styles.css` do Nocturne. O protótipo cobre a maior parte, mas faltam cinco peças (campo de busca, ações de publicação na lista, bloco de gabarito certo/errado, erro inline de publicação e o diálogo de arquivar assunto); elas entram primeiro no `Certame.dc.html`, conforme a regra 1 do `AGENTS.md`.

RN-03 é especificado e testado, mas sem superfície de interface: `questao_estatistica` e a consulta que exclui as respostas do próprio autor nascem aqui; quem as consome é `prd-sessao-de-estudo` (RF-08).

## Arquitetura do sistema

### Visão dos componentes

```
Angular 22 (SPA)                              Spring Boot 4.1                        PostgreSQL 18
────────────────                              ───────────────                        ─────────────
features/admin/questoes/                                                             ┌── disciplina
  QuestoesPage      ────▶  GET/DELETE /api/v1/admin/questoes                         ├── assunto
  QuestaoFormPage   ────▶  POST/PUT    /api/v1/admin/questoes                        ├── banca
                    ────▶  POST        /api/v1/admin/questoes/{id}/enviar-revisao    ├── orgao
                    ────▶  POST        /api/v1/admin/questoes/{id}/publicar          ├── questao
                    ────▶  POST        /api/v1/admin/questoes/{id}/despublicar       ├── alternativa
features/admin/materias/                                                             ├── questao_estatistica
  MateriasPage      ────▶  CRUD        /api/v1/admin/disciplinas · /admin/assuntos   └── auditoria
                    ────▶  POST        /api/v1/admin/assuntos/{id}/arquivar
features/admin/catalogo/                                                              ├── banca
  CatalogoPage       ────▶  GET/POST/PUT/DELETE /api/v1/admin/bancas · /admin/orgaos  └── orgao
```

**Backend — pacote `br.com.certame`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `catalogo/Disciplina`, `Assunto`, `Banca`, `Orgao` | novo | Entidades JPA da taxonomia e das origens. `Assunto` referencia `Disciplina`; ambos têm `arquivado_em`. |
| `catalogo/DisciplinaRepository`, `AssuntoRepository` | novo | Busca da árvore com contagem derivada (uma consulta agregada, não N+1), verificação de uso e unicidade de nome. |
| `catalogo/BancaRepository`, `OrgaoRepository` | novo | CRUD ordenado por nome; verificação de uso por `questao` para bloquear exclusão. |
| `catalogo/TaxonomiaService` | novo | RF-20: criar, renomear, arquivar, desarquivar e excluir disciplina e assunto; recusa a exclusão de nó em uso (CA-11) e monta a árvore com contagem (CA-10). |
| `catalogo/CatalogoService` | novo | RF-37: criar, editar e excluir banca e órgão; recusa a exclusão quando há questões vinculadas (CA-14). Catálogo plano, sem hierarquia — mantido pelo administrador, ao contrário da taxonomia. |
| `catalogo/CatalogoController` | novo | Rotas de `/admin/disciplinas`, `/admin/assuntos` (professor+) e `/admin/bancas`, `/admin/orgaos` (leitura professor+, escrita admin+). |
| `questao/Questao`, `Alternativa` | novo | Entidade raiz do agregado e suas alternativas (`@OneToMany` com `orphanRemoval`, carregadas por `@EntityGraph`). |
| `questao/TipoQuestao`, `StatusQuestao`, `Dificuldade` | novo | Enums `ME`/`CE`; `RASCUNHO`/`EM_REVISAO`/`PUBLICADA`; `FACIL`/`MEDIA`/`DIFICIL`. |
| `questao/QuestaoRepository` | novo | CRUD, busca paginada por *specification*/JPQL nativo com `tsvector`, contagem por disciplina e assunto. |
| `questao/QuestaoService` | novo | RF-19: criação, edição e exclusão lógica; normalização das alternativas (letras e ordem); resolução das referências de catálogo. |
| `questao/PublicacaoService` | novo | RN-01: as três transições de status, com autorização por perfil e auditoria. |
| `questao/ValidadorPublicacao` | novo | CA-06: regra pura que devolve o mapa de pendências (disciplina, gabarito, comentário, alternativas). Sem dependência de Spring. |
| `questao/BuscaQuestaoService` | novo | RF-18: traduz filtros e termo em consulta paginada; decide entre busca por código e busca textual. |
| `questao/GeradorCodigoQuestao` | novo | Envolve a sequência `questao_codigo_seq`; formato `Q-#####`. |
| `questao/QuestaoAdminController` | novo | Rotas de `/admin/questoes`; só orquestra e mapeia DTO. |
| `questao/EstatisticaQuestaoService` | novo | RN-03: recálculo do percentual de acerto excluindo as respostas do autor; escreve `questao_estatistica`. |
| `questao/dto/` | novo | `records` de requisição e resposta (`QuestaoResumo`, `QuestaoDetalhe`, `RequisicaoQuestao`, `AlternativaDto`, …). |
| `comum/auditoria/Auditoria`, `AuditoriaRepository` | novo | Entidade e repositório da tabela `auditoria` (§5). |
| `comum/auditoria/RegistradorAuditoria` | novo | `registrar(acao, entidade, entidadeId, payload)` — lê o ator do `SecurityContext`; chamado pelos serviços, não por AOP. |
| `comum/PaginaResposta<T>` | novo | Envelope `{content, page, size, totalElements}` da §6, reutilizável por todas as listagens. |
| `comum/excecao/*` | modificado | Acrescenta `RecursoNaoEncontradoException`, `ConflitoException`, `PublicacaoInvalidaException`. |
| `comum/ManipuladorErrosGlobal` | modificado | Trata os três novos tipos e mapeia `PublicacaoInvalidaException` para 422 com `fieldErrors`. |

**Frontend — `frontend/src/app`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `core/api/questao-api.ts` | novo | Cliente HTTP de `/admin/questoes` e das três transições. |
| `core/api/catalogo-api.ts` | novo | Cliente de disciplinas, assuntos, bancas e órgãos. |
| `core/api/modelos-questao.ts` | novo | Tipagem dos contratos desta especificação. |
| `features/admin/questoes/questoes-page` | novo | Lista paginada com busca e filtros (RF-18); ações de editar, publicar, despublicar e excluir. |
| `features/admin/questoes/questoes-filtros.ts` | novo | Signals dos filtros espelhados em *query params* — é o que devolve a lista no mesmo ponto ao voltar do formulário. |
| `features/admin/questoes/questao-form-page` | novo | Formulário de página cheia (RF-19), com o bloco de alternativas trocado por gabarito Certo/Errado quando o tipo é `CE`. |
| `features/admin/materias/materias-page` | novo | Árvore de disciplinas e assuntos com contagem (RF-20) e o diálogo de edição/arquivamento. |
| `features/admin/catalogo/catalogo-page` | novo | Cadastro de bancas e órgãos pelo administrador (RF-37): criar, editar e excluir, com exclusão bloqueada quando em uso (CA-14). |
| `shared/ui/confirmacao-dialog` | novo | Diálogo de confirmação para ações destrutivas, exigido pelo PRD; reutilizável pelas fases seguintes. |
| `app.routes.ts` | modificado | `/admin/questoes`, `/admin/questoes/nova`, `/admin/questoes/:id`, `/admin/materias` e `/admin/catalogo`, sob `perfilGuard`. |
| `protótipo/Certame.dc.html` | modificado | Cinco lacunas listadas em *Riscos conhecidos* e a nova tela `ADMIN: CATÁLOGO`, acrescentadas com componentes Nocturne existentes. |

**Fluxo de dados.** A lista lê filtros dos *query params*, chama `GET /admin/questoes` e renderiza o envelope paginado; navegar para o formulário preserva a URL de origem, e voltar restaura a lista sem nova decisão de estado. O formulário carrega o catálogo uma vez (disciplinas, assuntos da disciplina escolhida, bancas e órgãos) e envia o agregado inteiro — questão mais alternativas — num único `POST`/`PUT`. As transições de status são chamadas separadas: o servidor valida perfil e regra e devolve, em caso de recusa, o mapa de campos pendentes que a tela exibe inline. A árvore de matérias vem pronta do servidor, com contagem agregada, numa única requisição.

## Design de implementação

### Principais interfaces

```
QuestaoService
  criar(RequisicaoQuestao, UUID autorId) -> QuestaoDetalhe
  atualizar(UUID id, RequisicaoQuestao) -> QuestaoDetalhe
  buscarPorId(UUID id) -> QuestaoDetalhe
  excluir(UUID id) -> void                       // logica; recusa se houver resposta

BuscaQuestaoService
  buscar(FiltroQuestao, Paginacao) -> PaginaResposta<QuestaoResumo>

PublicacaoService
  enviarParaRevisao(UUID id) -> QuestaoDetalhe    // professor+
  publicar(UUID id) -> QuestaoDetalhe             // admin+; aplica o ValidadorPublicacao
  despublicar(UUID id) -> QuestaoDetalhe          // admin+; nao toca em resposta (RN-01)

ValidadorPublicacao
  validar(Questao) -> Map<String,String>          // vazio = publicavel (CA-06)

TaxonomiaService
  arvore() -> List<DisciplinaArvore>              // com contagem derivada (CA-10)
  criarDisciplina(String nome) / renomearDisciplina(UUID, String)
  criarAssunto(UUID disciplinaId, String nome) / renomearAssunto(UUID, String)
  excluirAssunto(UUID id) -> void                 // lanca ConflitoException se em uso (CA-11)
  arquivarAssunto(UUID id) -> void

CatalogoService
  listarBancas() / listarOrgaos() -> List<ItemCatalogo>
  criarBanca(String nome, String sigla) / renomearBanca(UUID, String, String)  // admin+
  criarOrgao(String nome, String sigla) / renomearOrgao(UUID, String, String)  // admin+
  excluirBanca(UUID id) / excluirOrgao(UUID id) -> void  // admin+; lanca ConflitoException se em uso (CA-14)

EstatisticaQuestaoService
  recalcular(UUID questaoId) -> EstatisticaQuestao   // exclui respostas do autor (RN-03)

RegistradorAuditoria
  registrar(String acao, String entidade, UUID entidadeId, Object payload) -> void
```

```
QuestoesFiltros (signals)
  termo, disciplinaId, bancaId, status, pagina, tamanho
  aplicar() -> void            // grava nos query params e dispara a busca
  limpar() -> void

QuestaoFormStore (signals)
  carregar(id | null) -> Promise<void>
  salvarRascunho() -> Promise<void>
  enviarParaRevisao() / publicar() / despublicar() -> Promise<void>
  pendenciasPublicacao: Signal<Record<string,string> | null>
```

### Modelos de dados

Todos os contratos JSON usam `camelCase`; as colunas usam `snake_case`. Campos ausentes na origem são serializados como `null` — nunca omitidos. Datas em ISO-8601 UTC.

#### `QuestaoResumo` — linha da listagem administrativa (RF-18)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Identificador da questão. |
| `codigo` | `string` | sim | Código estável no formato `Q-#####`, gerado na criação. |
| `enunciadoResumo` | `string` | sim | Primeiros 180 caracteres do enunciado, sem marcação; a tela ainda trunca por CSS. |
| `disciplina` | `string` | sim | Nome da disciplina. |
| `banca` | `string` | não | Nome da banca; `null` quando não informada. |
| `ano` | `number` | não | Ano de aplicação; `null` quando não informado. |
| `tipo` | `string` | sim | `ME` ou `CE`. |
| `status` | `string` | sim | `RASCUNHO`, `EM_REVISAO` ou `PUBLICADA`. |
| `atualizadoEm` | `string` | sim | Última alteração, para ordenação padrão. |

```text
{
  "id": "1b0f7f0a-5c2f-4f0e-9a51-2a4d3e7c9b10",
  "codigo": "Q-88409",
  "enunciadoResumo": "Sobre o regime jurídico das licitações na Lei 14.133/2021, assinale a alternativa correta.",
  "disciplina": "Direito Administrativo",
  "banca": "FCC",
  "ano": 2025,
  "tipo": "ME",
  "status": "RASCUNHO",
  "atualizadoEm": "2026-08-28T14:02:11Z"
}
```

#### `QuestaoDetalhe` — questão completa, lida e devolvida pelo formulário (RF-19)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Identificador. |
| `codigo` | `string` | sim | Código gerado. |
| `tipo` | `string` | sim | `ME` ou `CE`. |
| `enunciado` | `string` | sim | Texto integral. |
| `disciplinaId` | `string` (uuid) | não | `null` só é aceito em `RASCUNHO`. |
| `assuntoId` | `string` (uuid) | não | Deve pertencer à disciplina informada. |
| `bancaId` | `string` (uuid) | não | Referência a `banca`. |
| `orgaoId` | `string` (uuid) | não | Referência a `orgao`. |
| `ano` | `number` | não | Entre 1990 e o ano corrente + 1. |
| `dificuldade` | `string` | não | `FACIL`, `MEDIA` ou `DIFICIL`. |
| `alternativas` | `AlternativaDto[]` | sim | Vazio quando `tipo` é `CE`; de 2 a 5 itens em `ME` publicada. |
| `gabarito` | `string` | não | `A`…`E` em `ME`; `CERTO` ou `ERRADO` em `CE`. `null` em rascunho. |
| `comentario` | `string` | não | Comentário do professor. `null` em rascunho. |
| `status` | `string` | sim | Status corrente. |
| `autorId` | `string` (uuid) | sim | Autor da questão, base de RN-03. |
| `publicadoEm` | `string` | não | Instante da última publicação; `null` se nunca publicada. |
| `criadoEm`, `atualizadoEm` | `string` | sim | Marcas temporais. |

```text
{
  "id": "1b0f7f0a-5c2f-4f0e-9a51-2a4d3e7c9b10",
  "codigo": "Q-88409",
  "tipo": "ME",
  "enunciado": "Sobre o regime jurídico das licitações na Lei 14.133/2021, assinale a alternativa correta.",
  "disciplinaId": "6a1c0f2e-9d3b-4a10-8f22-71c5b0e4d901",
  "assuntoId": "b2d4e6f8-1a3c-4b5d-8e7f-0a9c1b2d3e4f",
  "bancaId": "3f8e1d2c-7b6a-4958-9a0b-1c2d3e4f5a6b",
  "orgaoId": null,
  "ano": 2025,
  "dificuldade": "MEDIA",
  "alternativas": [
    { "letra": "A", "texto": "A contratação direta dispensa a formalização de processo.", "ordem": 1 },
    { "letra": "B", "texto": "O diálogo competitivo é modalidade prevista na Lei 14.133/2021.", "ordem": 2 },
    { "letra": "C", "texto": "A tomada de preços foi mantida como modalidade.", "ordem": 3 }
  ],
  "gabarito": "B",
  "comentario": "O diálogo competitivo é a modalidade inovadora do art. 28, V.",
  "status": "RASCUNHO",
  "autorId": "6d1f2c9e-6f2a-4a1c-9a3e-1c6b0f5a77d1",
  "publicadoEm": null,
  "criadoEm": "2026-08-28T13:40:02Z",
  "atualizadoEm": "2026-08-28T14:02:11Z"
}
```

> **Variante — tipo Certo/Errado (CA-04):** `alternativas` volta como lista vazia (nunca `null`) e `gabarito` assume `CERTO` ou `ERRADO`. A interface substitui o bloco de alternativas pela escolha binária.

```text
{
  "tipo": "CE",
  "alternativas": [],
  "gabarito": "CERTO"
}
```

#### `AlternativaDto` — alternativa de múltipla escolha

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `letra` | `string` | sim | `A`…`E`. Atribuída pelo servidor a partir da ordem; o cliente não a escolhe. |
| `texto` | `string` | sim | Máx. 2000 caracteres. |
| `ordem` | `number` | sim | 1 a 5, contígua e sem repetição. |

```text
{ "letra": "A", "texto": "A contratação direta dispensa a formalização de processo.", "ordem": 1 }
```

#### `RequisicaoQuestao` — corpo de `POST` e `PUT /admin/questoes`

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `tipo` | `string` | sim | `ME` ou `CE`. Único campo sempre exigido junto ao enunciado. |
| `enunciado` | `string` | sim | `@NotBlank`, 10 a 20000 caracteres — o mínimo para salvar rascunho (CA-05). |
| `disciplinaId` | `string` (uuid) | não | Exigido apenas para publicar. |
| `assuntoId` | `string` (uuid) | não | Se informado, precisa pertencer a `disciplinaId`. |
| `bancaId`, `orgaoId` | `string` (uuid) | não | Referências opcionais. |
| `ano` | `number` | não | 1990 ≤ ano ≤ ano corrente + 1. |
| `dificuldade` | `string` | não | `FACIL`, `MEDIA`, `DIFICIL`. |
| `alternativas` | `AlternativaDto[]` | não | Ignorado (e limpo) quando `tipo` é `CE`; no máximo 5 em `ME`. |
| `gabarito` | `string` | não | Coerente com o tipo; validado na publicação. |
| `comentario` | `string` | não | Máx. 20000 caracteres. |

```text
{
  "tipo": "ME",
  "enunciado": "Sobre o regime jurídico das licitações na Lei 14.133/2021, assinale a alternativa correta.",
  "disciplinaId": "6a1c0f2e-9d3b-4a10-8f22-71c5b0e4d901",
  "assuntoId": "b2d4e6f8-1a3c-4b5d-8e7f-0a9c1b2d3e4f",
  "bancaId": "3f8e1d2c-7b6a-4958-9a0b-1c2d3e4f5a6b",
  "orgaoId": null,
  "ano": 2025,
  "dificuldade": "MEDIA",
  "alternativas": [
    { "texto": "A contratação direta dispensa a formalização de processo.", "ordem": 1 },
    { "texto": "O diálogo competitivo é modalidade prevista na Lei 14.133/2021.", "ordem": 2 }
  ],
  "gabarito": "B",
  "comentario": "O diálogo competitivo é a modalidade inovadora do art. 28, V."
}
```

> **Rascunho mínimo (CA-05):** apenas `tipo` e `enunciado`. Todos os demais campos aceitam ausência ou `null`, e nenhuma validação de publicação é aplicada.

```text
{ "tipo": "ME", "enunciado": "Julgue o item a seguir sobre improbidade administrativa." }
```

#### `DisciplinaArvore` e `AssuntoArvore` — nó da árvore de matérias (RF-20)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Identificador do nó. |
| `nome` | `string` | sim | Nome exibido. |
| `totalQuestoes` | `number` | sim | Questões não excluídas vinculadas ao nó (CA-10). Numa disciplina, soma a de todos os seus assuntos e as questões sem assunto. |
| `totalPublicadas` | `number` | sim | Subconjunto com status `PUBLICADA` — é o número que revela onde o acervo está raso. |
| `arquivado` | `boolean` | sim | `true` quando `arquivado_em` não é nulo. |
| `assuntos` | `AssuntoArvore[]` | sim | Só em `DisciplinaArvore`; ordenados por nome. |

```text
{
  "id": "6a1c0f2e-9d3b-4a10-8f22-71c5b0e4d901",
  "nome": "Direito Administrativo",
  "totalQuestoes": 980,
  "totalPublicadas": 812,
  "arquivado": false,
  "assuntos": [
    { "id": "b2d4e6f8-1a3c-4b5d-8e7f-0a9c1b2d3e4f", "nome": "Licitações — Lei 14.133", "totalQuestoes": 148, "totalPublicadas": 131, "arquivado": false },
    { "id": "c3e5f7a9-2b4d-4c6e-9f80-1b2c3d4e5f60", "nome": "Atos administrativos", "totalQuestoes": 192, "totalPublicadas": 180, "arquivado": false }
  ]
}
```

#### `RequisicaoNoTaxonomia` — corpo de criação e renomeação

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `nome` | `string` | sim | 2 a 120 caracteres. Único entre irmãos não excluídos, sem diferenciar maiúsculas. |
| `disciplinaId` | `string` (uuid) | sim (assunto) | Só em `POST /admin/assuntos`; imutável depois. |

```text
{ "nome": "Licitações — Lei 14.133", "disciplinaId": "6a1c0f2e-9d3b-4a10-8f22-71c5b0e4d901" }
```

#### `ItemCatalogo` — banca ou órgão, para os combos do formulário

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `string` (uuid) | sim | Identificador. |
| `nome` | `string` | sim | Nome por extenso. |
| `sigla` | `string` | não | Sigla, quando houver; `null` caso contrário. |

```text
{ "id": "3f8e1d2c-7b6a-4958-9a0b-1c2d3e4f5a6b", "nome": "Fundação Carlos Chagas", "sigla": "FCC" }
```

#### `RequisicaoItemCatalogo` — corpo de `POST` e `PUT /admin/bancas` · `/admin/orgaos` (RF-37)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `nome` | `string` | sim | 2 a 120 caracteres. Único entre bancas (ou entre órgãos) não excluídos, sem diferenciar maiúsculas. |
| `sigla` | `string` | não | Até 20 caracteres; `null` quando ausente. |

```text
{ "nome": "Fundação Getúlio Vargas", "sigla": "FGV" }
```

#### `PaginaResposta<T>` — envelope de listagem (§6 da `TECH-SPEC.md`)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `content` | `T[]` | sim | Itens da página; lista vazia quando não há correspondência. |
| `page` | `number` | sim | Índice da página, base zero. |
| `size` | `number` | sim | Tamanho solicitado. |
| `totalElements` | `number` | sim | Total exato de itens que atendem ao filtro (CA-02). |

```text
{ "content": [], "page": 0, "size": 20, "totalElements": 0 }
```

#### `EstatisticaQuestao` — percentual de acerto (RN-03)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `questaoId` | `string` (uuid) | sim | Questão medida. |
| `totalRespostas` | `number` | sim | Respostas válidas, **excluídas as do autor**. |
| `totalAcertos` | `number` | sim | Subconjunto correto. |
| `pctAcerto` | `number` | não | Inteiro de 0 a 100; `null` quando `totalRespostas` é zero. |
| `atualizadoEm` | `string` | sim | Último recálculo. |

```text
{
  "questaoId": "1b0f7f0a-5c2f-4f0e-9a51-2a4d3e7c9b10",
  "totalRespostas": 0,
  "totalAcertos": 0,
  "pctAcerto": null,
  "atualizadoEm": "2026-08-28T14:02:11Z"
}
```

> Sem interface nesta entrega, por decisão registrada: a tabela `resposta` só nasce em `prd-sessao-de-estudo`. O que existe aqui é o esquema, o serviço e a cobertura de teste com respostas semeadas.

#### `ErroApi` — envelope de erro (`application/problem+json`)

Estrutura definida em `prd-acesso-e-conta` (`code`, `message`, `fieldErrors`). Códigos acrescentados por esta funcionalidade:

| Código | HTTP | Significado |
| --- | --- | --- |
| `DADOS_INVALIDOS` | `422` | Falha de Bean Validation; `fieldErrors` preenchido. |
| `PUBLICACAO_INVALIDA` | `422` | Publicação recusada por disciplina, gabarito, comentário ou alternativas ausentes (CA-06); `fieldErrors` lista cada pendência. |
| `INTEGRIDADE_INVALIDA` | `422` | Violação de integridade de banco sem mapeamento de domínio específico; mensagem genérica, sem detalhes internos. |
| `RECURSO_NAO_ENCONTRADO` | `404` | Questão, disciplina, assunto, banca ou órgão inexistente ou já excluído. |
| `NOME_EM_USO` | `409` | Disciplina ou assunto irmão com o mesmo nome. |
| `ASSUNTO_EM_USO` | `409` | Exclusão de assunto com questões vinculadas; a interface oferece arquivar (CA-11). |
| `DISCIPLINA_EM_USO` | `409` | Exclusão de disciplina com assuntos ou questões vinculados. |
| `QUESTAO_EM_USO` | `409` | Exclusão de questão que já possui respostas registradas. |
| `TRANSICAO_INVALIDA` | `409` | Transição de status inexistente (por exemplo, despublicar um rascunho). |
| `ACESSO_NEGADO` | `403` | Perfil insuficiente — publicar ou despublicar sem `ROLE_ADMIN`. |

```text
{
  "code": "PUBLICACAO_INVALIDA",
  "message": "Não é possível publicar: verifique os campos pendentes.",
  "fieldErrors": {
    "gabarito": "Marque a alternativa correta antes de publicar.",
    "comentario": "O comentário do professor é obrigatório para publicar.",
    "alternativas": "Informe ao menos duas alternativas.",
    "disciplina": "Selecione uma disciplina antes de publicar."
  }
}
```

```text
{
  "code": "ASSUNTO_EM_USO",
  "message": "Este assunto tem 148 questões vinculadas e não pode ser excluído. Arquive-o para retirá-lo da árvore.",
  "fieldErrors": null
}
```

#### Esquema de banco — `V2__banco_questoes.sql`

Migração versionada e imutável em `backend/src/main/resources/db/migration`, seguindo a §5 da `TECH-SPEC.md`. `V1__base_acesso.sql` já criou `usuario`, referenciada aqui.

| Tabela | Colunas principais | Observações |
| --- | --- | --- |
| `disciplina` | `id`, `nome`, `arquivado_em`, `criado_em`, `atualizado_em` | Nome único entre não excluídas, sem diferenciar maiúsculas. |
| `assunto` | `id`, `disciplina_id`, `nome`, `arquivado_em`, `criado_em`, `atualizado_em` | Nome único dentro da disciplina. `arquivado_em` sustenta CA-11. |
| `banca`, `orgao` | `id`, `nome`, `sigla`, `criado_em` | Semeadas por `V3` como ponto de partida; mantidas depois pelo administrador via RF-37. Sem `excluido_em`: a FK sem `on delete` em `questao` já impede a exclusão física de um item em uso — o serviço apenas antecipa a checagem para devolver `409` amigável em vez de erro de integridade cru. |
| `questao` | conforme SQL abaixo | `codigo` gerado por sequência; `busca` é coluna gerada `tsvector`. |
| `alternativa` | `id`, `questao_id`, `letra`, `texto`, `ordem` | Único por `(questao_id, letra)` e por `(questao_id, ordem)`; apagada em cascata com a questão. |
| `questao_estatistica` | `questao_id` (PK), `total_respostas`, `total_acertos`, `pct_acerto`, `atualizado_em` | Linha criada junto com a questão, zerada (RN-03). |
| `auditoria` | `id`, `ator_id`, `acao`, `entidade`, `entidade_id`, `payload` jsonb, `criado_em` | Tabela transversal da §5; nasce aqui porque esta é a primeira feature administrativa. |

```sql
create table disciplina (
  id            uuid primary key default gen_random_uuid(),
  nome          text not null,
  arquivado_em  timestamptz,
  excluido_em   timestamptz,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create unique index disciplina_nome_uk on disciplina (lower(nome)) where excluido_em is null;

create table assunto (
  id            uuid primary key default gen_random_uuid(),
  disciplina_id uuid not null references disciplina(id),
  nome          text not null,
  arquivado_em  timestamptz,
  excluido_em   timestamptz,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create unique index assunto_nome_uk on assunto (disciplina_id, lower(nome)) where excluido_em is null;

create table banca (
  id        uuid primary key default gen_random_uuid(),
  nome      text not null,
  sigla     text,
  criado_em timestamptz not null default now()
);
create unique index banca_nome_uk on banca (lower(nome));

create table orgao (
  id        uuid primary key default gen_random_uuid(),
  nome      text not null,
  sigla     text,
  criado_em timestamptz not null default now()
);
create unique index orgao_nome_uk on orgao (lower(nome));

create sequence questao_codigo_seq start with 10000;

create table questao (
  id            uuid primary key default gen_random_uuid(),
  codigo        text not null unique
                default 'Q-' || lpad(nextval('questao_codigo_seq')::text, 5, '0'),
  tipo          text not null check (tipo in ('ME','CE')),
  enunciado     text not null,
  disciplina_id uuid references disciplina(id),
  assunto_id    uuid references assunto(id),
  banca_id      uuid references banca(id),
  orgao_id      uuid references orgao(id),
  ano           smallint check (ano between 1990 and 2100),
  dificuldade   text check (dificuldade in ('FACIL','MEDIA','DIFICIL')),
  gabarito      text,
  comentario    text,
  status        text not null default 'RASCUNHO'
                check (status in ('RASCUNHO','EM_REVISAO','PUBLICADA')),
  autor_id      uuid not null references usuario(id),
  publicado_em  timestamptz,
  excluido_em   timestamptz,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),
  busca         tsvector generated always as
                (to_tsvector('portuguese', coalesce(enunciado, ''))) stored,
  constraint questao_publicada_completa check (
    status <> 'PUBLICADA' or (
      disciplina_id is not null
      and gabarito is not null
      and comentario is not null and length(btrim(comentario)) > 0
    )
  )
);
create index questao_busca_idx   on questao using gin (busca);
create index questao_codigo_idx  on questao (codigo text_pattern_ops) where excluido_em is null;
create index questao_filtro_idx  on questao (status, disciplina_id, banca_id, atualizado_em desc)
  where excluido_em is null;
create index questao_assunto_idx on questao (assunto_id) where excluido_em is null;

create table alternativa (
  id         uuid primary key default gen_random_uuid(),
  questao_id uuid not null references questao(id) on delete cascade,
  letra      text not null check (letra in ('A','B','C','D','E')),
  texto      text not null,
  ordem      smallint not null check (ordem between 1 and 5),
  unique (questao_id, letra),
  unique (questao_id, ordem)
);

create table questao_estatistica (
  questao_id      uuid primary key references questao(id) on delete cascade,
  total_respostas integer not null default 0,
  total_acertos   integer not null default 0,
  pct_acerto      smallint check (pct_acerto between 0 and 100),
  atualizado_em   timestamptz not null default now()
);

create table auditoria (
  id          uuid primary key default gen_random_uuid(),
  ator_id     uuid references usuario(id),
  acao        text not null,
  entidade    text not null,
  entidade_id uuid,
  payload     jsonb,
  criado_em   timestamptz not null default now()
);
create index auditoria_entidade_idx on auditoria (entidade, entidade_id, criado_em desc);
```

> **`questao_publicada_completa`** é a rede de segurança no banco para RN-01 e CA-06: mesmo que um caminho futuro escape do `ValidadorPublicacao`, o `check` recusa a linha. A contagem mínima de alternativas em `ME` não cabe num `check` de linha e fica só no serviço.

> **Sementes.** `V3__seed_catalogo.sql` popula `banca` e `orgao` com as principais bancas (Cebraspe, FGV, FCC, Vunesp, Cesgranrio, IBFC, Quadrix, Instituto AOCP) e órgãos recorrentes — é dado de catálogo, não de desenvolvimento, e entra em todos os ambientes. `db/migration-dev/V901__seed_banco_questoes_dev.sql` acrescenta disciplinas, assuntos e questões de exemplo apenas no perfil `dev`, pelo `spring.flyway.locations` já configurado em `prd-acesso-e-conta`.

### Endpoints da API

Base `/api/v1`. Dezesseis rotas: cinco já constam da §6 da `TECH-SPEC.md`; as onze restantes (`enviar-revisao`, `despublicar`, `DELETE /admin/questoes/{id}`, `arquivar` de assunto, `GET`/`POST`/`PUT`/`DELETE` de `/admin/bancas` e `/admin/orgaos`) são **acréscimos ao contrato** e a §6 é atualizada na mesma entrega, conforme a regra 4 do `AGENTS.md`.

#### Visão geral

| Método | Rota | Perfil | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/v1/admin/questoes` | professor+ | Lista paginada com busca e filtros (RF-18). |
| `GET` | `/api/v1/admin/questoes/{id}` | professor+ | Questão completa para o formulário (RF-19). |
| `POST` | `/api/v1/admin/questoes` | professor+ | Cria como `RASCUNHO` (CA-05). |
| `PUT` | `/api/v1/admin/questoes/{id}` | professor+ | Edita sem alterar o status (fluxo *Corrigir*). |
| `DELETE` | `/api/v1/admin/questoes/{id}` | professor+ | Exclusão lógica; recusada se houver respostas. |
| `POST` | `/api/v1/admin/questoes/{id}/enviar-revisao` | professor+ | `RASCUNHO` → `EM_REVISAO`. |
| `POST` | `/api/v1/admin/questoes/{id}/publicar` | admin+ | Valida e publica (CA-06). |
| `POST` | `/api/v1/admin/questoes/{id}/despublicar` | admin+ | `PUBLICADA` → `EM_REVISAO` (CA-08). |
| `GET/POST/PUT/DELETE` | `/api/v1/admin/disciplinas` | professor+ | Árvore e CRUD de disciplinas (RF-20). |
| `POST/PUT/DELETE` | `/api/v1/admin/assuntos` · `/{id}/arquivar` | professor+ | CRUD e arquivamento de assuntos (CA-11). |
| `GET` | `/api/v1/admin/bancas` · `/api/v1/admin/orgaos` | professor+ | Catálogo para os combos do formulário. |
| `POST/PUT/DELETE` | `/api/v1/admin/bancas` · `/api/v1/admin/orgaos` | admin+ | Cadastro do catálogo (RF-37); exclusão recusada se em uso (CA-14). |

---

#### `GET /api/v1/admin/questoes`

Tabela de trabalho do time editorial (RF-18).

**Parâmetros de consulta**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `q` | `string` | — | Termo livre. Se casar com `^[Qq]-?\d+$`, vira busca por prefixo de `codigo`; caso contrário, `plainto_tsquery('portuguese', …)` sobre `busca`. Máx. 200 caracteres. |
| `disciplina` | `uuid` | — | Filtra por `disciplina_id`. |
| `assunto` | `uuid` | — | Filtra por `assunto_id`; combinável com `disciplina`. |
| `banca` | `uuid` | — | Filtra por `banca_id`. |
| `ano` | `number` | — | Ano exato. |
| `tipo` | `string` | — | `ME` ou `CE`. |
| `status` | `string` | — | `RASCUNHO`, `EM_REVISAO` ou `PUBLICADA`. |
| `page` | `number` | `0` | Base zero, ≥ 0. |
| `size` | `number` | `20` | 1 a 100. |
| `sort` | `string` | `atualizadoEm,desc` | Também aceita `codigo`, `criadoEm`. Campo fora da lista → 422. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `PaginaResposta<QuestaoResumo>` | Sempre que os parâmetros forem válidos, mesmo sem correspondência. |
| `401` | `ErroApi` | Sem token. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil `ESTUDANTE`. |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | `size` fora da faixa, `status`/`tipo` desconhecido ou `sort` inválido. |

**Exemplo — sucesso com filtros combinados (CA-02)**

```http
GET /api/v1/admin/questoes?q=licitações&disciplina=6a1c0f2e-...&banca=3f8e1d2c-...&status=RASCUNHO&page=0&size=20
```

```text
{
  "content": [ { "id": "1b0f...", "codigo": "Q-88409", "enunciadoResumo": "Sobre o regime jurídico das licitações…", "disciplina": "Direito Administrativo", "banca": "FCC", "ano": 2025, "tipo": "ME", "status": "RASCUNHO", "atualizadoEm": "2026-08-28T14:02:11Z" } ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

**Exemplo — nenhuma correspondência**

```http
GET /api/v1/admin/questoes?q=criptomoeda&status=PUBLICADA
```

```text
{ "content": [], "page": 0, "size": 20, "totalElements": 0 }
```

> A tela exibe o estado vazio com a ação de limpar filtros; `totalElements` zero recalcula a paginação e desabilita "Próxima" (CA-02). Os filtros vivem nos *query params*, de modo que voltar do formulário devolve a lista no mesmo ponto — exigência de UX do PRD.

**Exemplo — parâmetro inválido**

```http
GET /api/v1/admin/questoes?size=500
```

```text
{ "code": "DADOS_INVALIDOS", "message": "Verifique os campos destacados.", "fieldErrors": { "size": "O tamanho da página deve estar entre 1 e 100." } }
```

---

#### `POST /api/v1/admin/questoes`

Cria a questão. Sempre nasce em `RASCUNHO`, mesmo com todos os campos preenchidos (CA-05).

**Corpo** — `RequisicaoQuestao`

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `QuestaoDetalhe` | Criada; `Location` aponta para o recurso e `codigo` já vem gerado. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil `ESTUDANTE`. |
| `404` | `ErroApi` (`RECURSO_NAO_ENCONTRADO`) | `disciplinaId`, `assuntoId`, `bancaId` ou `orgaoId` inexistente. |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Enunciado ausente, tipo desconhecido, mais de cinco alternativas, `ordem` repetida, ou assunto de outra disciplina. |

**Exemplo — rascunho mínimo (CA-05)**

```http
POST /api/v1/admin/questoes
```

```text
{ "id": "9c2e...", "codigo": "Q-88410", "tipo": "ME", "enunciado": "Julgue o item a seguir sobre improbidade administrativa.", "disciplinaId": null, "assuntoId": null, "bancaId": null, "orgaoId": null, "ano": null, "dificuldade": null, "alternativas": [], "gabarito": null, "comentario": null, "status": "RASCUNHO", "autorId": "6d1f...", "publicadoEm": null, "criadoEm": "2026-08-30T11:00:00Z", "atualizadoEm": "2026-08-30T11:00:00Z" }
```

**Exemplo — assunto de outra disciplina**

```http
POST /api/v1/admin/questoes
```

```text
{ "code": "DADOS_INVALIDOS", "message": "Verifique os campos destacados.", "fieldErrors": { "assuntoId": "O assunto escolhido não pertence à disciplina selecionada." } }
```

> O autor é sempre o usuário do token; o cliente não envia `autorId`. É esse vínculo que RN-03 usa depois para excluir as respostas do próprio autor.

---

#### `PUT /api/v1/admin/questoes/{id}`

Edita o agregado inteiro — questão e alternativas — sem tocar no status (fluxo *Corrigir* do PRD: a questão publicada permanece publicada).

**Corpo** — `RequisicaoQuestao`

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `QuestaoDetalhe` | Atualizada. Status e `publicadoEm` preservados. |
| `404` | `ErroApi` (`RECURSO_NAO_ENCONTRADO`) | Questão inexistente ou já excluída. |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Mesmas validações do `POST`. Numa questão `PUBLICADA`, valem também as regras de publicação — não é possível "esvaziar" o gabarito de uma questão no ar. |

> Trocar `tipo` de `ME` para `CE` descarta as alternativas e limpa o gabarito, que passa a exigir `CERTO`/`ERRADO`; a interface avisa antes de aplicar (CA-04). As alternativas são substituídas por completo, e as letras são reatribuídas por `ordem` — o cliente nunca escolhe a letra.

---

#### `DELETE /api/v1/admin/questoes/{id}`

Exclusão lógica: marca `excluido_em` e some das listas e das sessões.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `204` | vazio | Excluída logicamente. |
| `404` | `ErroApi` (`RECURSO_NAO_ENCONTRADO`) | Questão inexistente ou já excluída. |
| `409` | `ErroApi` (`QUESTAO_EM_USO`) | Já existem respostas registradas — a integridade do histórico prevalece. |

> Nenhuma exclusão física em cascata, conforme a restrição de integridade do PRD. A interface confirma a ação num diálogo explícito e, no 409, oferece despublicar como alternativa.

---

#### `POST /api/v1/admin/questoes/{id}/enviar-revisao`

Move `RASCUNHO` → `EM_REVISAO`. Ação do professor.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `QuestaoDetalhe` | Status alterado e auditado. |
| `409` | `ErroApi` (`TRANSICAO_INVALIDA`) | Questão já em revisão ou publicada. |

---

#### `POST /api/v1/admin/questoes/{id}/publicar`

Valida e publica. Exige `ROLE_ADMIN` ou superior (RN-01, CA-06).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `QuestaoDetalhe` | Publicada; `publicadoEm` preenchido e auditoria gravada. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil `PROFESSOR` — recusado no servidor mesmo que a interface esconda o botão. |
| `409` | `ErroApi` (`TRANSICAO_INVALIDA`) | Questão já publicada. |
| `422` | `ErroApi` (`PUBLICACAO_INVALIDA`) | Falta disciplina, gabarito, comentário ou a segunda alternativa (CA-06). |

**Exemplo — publicação recusada (CA-06)**

```http
POST /api/v1/admin/questoes/1b0f7f0a-5c2f-4f0e-9a51-2a4d3e7c9b10/publicar
```

```text
{
  "code": "PUBLICACAO_INVALIDA",
  "message": "Não é possível publicar: verifique os campos pendentes.",
  "fieldErrors": {
    "gabarito": "Marque a alternativa correta antes de publicar.",
    "comentario": "O comentário do professor é obrigatório para publicar.",
    "alternativas": "Informe ao menos duas alternativas.",
    "disciplina": "Selecione uma disciplina antes de publicar."
  }
}
```

> O corpo lista **todas** as pendências de uma vez — não a primeira. A tela marca cada campo e rola até o primeiro, sem perder nada do que já foi digitado.

---

#### `POST /api/v1/admin/questoes/{id}/despublicar`

Move `PUBLICADA` → `EM_REVISAO`, tirando a questão das novas sessões (CA-08). Exige `ROLE_ADMIN` ou superior.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `QuestaoDetalhe` | Despublicada; `publicadoEm` preservado como registro histórico. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil `PROFESSOR`. |
| `409` | `ErroApi` (`TRANSICAO_INVALIDA`) | Questão não está publicada. |

> **Nada é apagado.** Nenhuma linha de `resposta`, `questao_estatistica` ou caderno é tocada: sessões já respondidas continuam íntegras, e apenas a montagem de novas séries — que filtra por `status = 'PUBLICADA'` — deixa de sortear a questão.

---

#### `GET /api/v1/admin/disciplinas`

Árvore completa com contagem derivada (RF-20, CA-10). Uma requisição, uma consulta agregada.

**Parâmetros de consulta**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `incluirArquivados` | `boolean` | `false` | Quando `true`, traz também os nós com `arquivado_em` preenchido. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `DisciplinaArvore[]` | Sempre; lista vazia quando não há disciplinas. |

**Exemplo — sucesso**

```http
GET /api/v1/admin/disciplinas
```

Corpo conforme o exemplo de `DisciplinaArvore` em *Modelos de dados*.

---

#### `POST` e `PUT /api/v1/admin/disciplinas` · `/api/v1/admin/assuntos`

Criação e renomeação de nós.

**Corpo** — `RequisicaoNoTaxonomia`

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` / `200` | `DisciplinaArvore` ou `AssuntoArvore` | Nó criado ou renomeado, já com as contagens. |
| `404` | `ErroApi` (`RECURSO_NAO_ENCONTRADO`) | Nó ou disciplina-pai inexistente. |
| `409` | `ErroApi` (`NOME_EM_USO`) | Nome duplicado entre irmãos. |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Nome fora de 2–120 caracteres. |

> Renomear altera apenas `nome`: o vínculo das questões é por `id`, então todas passam a exibir o novo nome sem qualquer atualização em massa (CA-12). Trocar a disciplina de um assunto não é suportado — mover conteúdo entre disciplinas está fora do escopo do PRD.

---

#### `DELETE /api/v1/admin/assuntos/{id}` e `POST /api/v1/admin/assuntos/{id}/arquivar`

Remoção da árvore, com a exclusão bloqueada quando há acervo (CA-11).

**Respostas do `DELETE`**

| Status | Corpo | Quando |
| --- | --- | --- |
| `204` | vazio | Assunto sem questões vinculadas; marcado como excluído. |
| `404` | `ErroApi` (`RECURSO_NAO_ENCONTRADO`) | Assunto inexistente. |
| `409` | `ErroApi` (`ASSUNTO_EM_USO`) | Há ao menos uma questão vinculada. |

**Exemplo — exclusão recusada (CA-11)**

```http
DELETE /api/v1/admin/assuntos/b2d4e6f8-1a3c-4b5d-8e7f-0a9c1b2d3e4f
```

```text
{ "code": "ASSUNTO_EM_USO", "message": "Este assunto tem 148 questões vinculadas e não pode ser excluído. Arquive-o para retirá-lo da árvore.", "fieldErrors": null }
```

> O 409 é o gatilho de interface: o diálogo troca o botão "Excluir" por "Arquivar", que chama `POST /assuntos/{id}/arquivar` e devolve `204`. O assunto arquivado some da árvore padrão e dos combos do formulário, mas as questões vinculadas continuam intactas e visíveis. `DELETE /admin/disciplinas/{id}` segue a mesma lógica, com `DISCIPLINA_EM_USO`.

---

#### `GET /api/v1/admin/bancas` e `GET /api/v1/admin/orgaos`

Catálogo para os combos do formulário, mantido pelo administrador via RF-37.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `ItemCatalogo[]` | Sempre, ordenado por nome. |

> Sem paginação: o catálogo tem dezenas de itens, não milhares. Ganha cache de segundo nível, conforme a §8 da `TECH-SPEC.md`, que autoriza cache apenas no catálogo — invalidado a cada escrita.

---

#### `POST` e `PUT /api/v1/admin/bancas` · `/api/v1/admin/orgaos`

Criação e edição do catálogo de bancas e órgãos (RF-37). Exige `ROLE_ADMIN` ou superior.

**Corpo** — `RequisicaoItemCatalogo`

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` / `200` | `ItemCatalogo` | Item criado ou editado. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil `PROFESSOR` ou inferior. |
| `404` | `ErroApi` (`RECURSO_NAO_ENCONTRADO`) | `PUT` em item inexistente. |
| `409` | `ErroApi` (`NOME_EM_USO`) | Nome duplicado (ignorando maiúsculas). |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Nome fora de 2–120 caracteres. |

**Exemplo — sucesso (CA-13)**

```http
POST /api/v1/admin/bancas
```

```text
{ "nome": "Fundação Getúlio Vargas", "sigla": "FGV" }
```

```text
{ "id": "9f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8", "nome": "Fundação Getúlio Vargas", "sigla": "FGV" }
```

---

#### `DELETE /api/v1/admin/bancas/{id}` e `DELETE /api/v1/admin/orgaos/{id}`

Exclusão física — sem exclusão lógica, porque bancas e órgãos não carregam histórico próprio. Exige `ROLE_ADMIN` ou superior.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `204` | vazio | Excluída; sem questões vinculadas. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil `PROFESSOR` ou inferior. |
| `404` | `ErroApi` (`RECURSO_NAO_ENCONTRADO`) | Item inexistente. |
| `409` | `ErroApi` (`BANCA_EM_USO` / `ORGAO_EM_USO`) | Há ao menos uma questão vinculada (CA-14). |

**Exemplo — exclusão recusada (CA-14)**

```http
DELETE /api/v1/admin/bancas/3f8e1d2c-7b6a-4958-9a0b-1c2d3e4f5a6b
```

```text
{ "code": "BANCA_EM_USO", "message": "Esta banca tem 62 questões vinculadas e não pode ser excluída.", "fieldErrors": null }
```

> O serviço checa o vínculo antes de excluir e devolve o 409 amigável; a FK sem `on delete` em `questao` é a rede de segurança do banco, redundante de propósito, igual à de `questao_publicada_completa`.

---

## Pontos de integração

Nenhuma integração externa. A funcionalidade é inteiramente interna — PostgreSQL e a fundação de segurança criada em `prd-acesso-e-conta`. Não há chamada de rede a terceiros, e portanto não há timeout, retentativa ou idempotência de integração a especificar.

Integrações internas: `SegurancaConfig` e o claim `perfil` do JWT (autorização), `ManipuladorErrosGlobal` e `ErroApi` (envelope de erro), `Usuario` (autoria e ator da auditoria). As funcionalidades seguintes consomem este acervo — ver *Arquivos relevantes e dependentes*.

## Abordagem de testes

Meta de cobertura: **mínimo de 70% em `service`**, conforme `AGENTS.md`. Ferramentas: JUnit Jupiter, AssertJ, Mockito e Testcontainers 2.0.5 (versões gerenciadas pelo BOM do Spring Boot 4.1.1); Vitest e Playwright no frontend.

### Testes de unidade

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `ValidadorPublicacao` com questão `ME` completa | CA-06 | Mapa de pendências vazio. |
| TU-02 | `ValidadorPublicacao` sem gabarito | CA-06 | Pendência em `gabarito`; demais campos ausentes do mapa. |
| TU-03 | `ValidadorPublicacao` sem comentário | CA-06 | Pendência em `comentario`. |
| TU-04 | `ValidadorPublicacao` com uma única alternativa em `ME` | CA-06 | Pendência em `alternativas`. |
| TU-05 | `ValidadorPublicacao` com as quatro faltas simultâneas | CA-06 | As quatro pendências no mesmo mapa, numa só passada. |
| TU-06 | `ValidadorPublicacao` em questão `CE` sem alternativas | CA-04, CA-06 | Publicável: ausência de alternativas não é pendência em `CE`. |
| TU-07 | `ValidadorPublicacao` em `CE` com gabarito `B` | CA-04 | Pendência em `gabarito`: só `CERTO`/`ERRADO` são aceitos. |
| TU-08 | `QuestaoService.criar` só com tipo e enunciado | CA-05 | Persiste com status `RASCUNHO`, sem exigir gabarito ou comentário. |
| TU-09 | `QuestaoService.criar` atribui autor do token e código gerado | CA-05 | `autorId` do contexto de segurança; `codigo` no formato `Q-#####`. |
| TU-10 | `QuestaoService` normaliza letras das alternativas pela ordem | CA-03 | Ordem 1..n vira `A`..`E`, mesmo que o cliente envie letras erradas ou ausentes. |
| TU-11 | `QuestaoService.criar` com seis alternativas | CA-03 | Lança `DadosInvalidosException`; nada persistido. |
| TU-12 | `QuestaoService` troca `ME` → `CE` | CA-04 | Alternativas descartadas e gabarito limpo. |
| TU-13 | `QuestaoService.criar` com assunto de outra disciplina | CA-03 | Lança validação com `fieldErrors.assuntoId`. |
| TU-14 | `QuestaoService.excluir` com respostas registradas | Integridade (PRD) | Lança `ConflitoException`; `excluido_em` permanece nulo. |
| TU-15 | `PublicacaoService.publicar` com questão válida | CA-06, CA-07 | Status `PUBLICADA`, `publicadoEm` preenchido, auditoria registrada. |
| TU-16 | `PublicacaoService.publicar` com pendências | CA-06 | Lança `PublicacaoInvalidaException` com o mapa do validador; status inalterado. |
| TU-17 | `PublicacaoService.despublicar` de questão publicada | CA-08 | Status `EM_REVISAO`; nenhuma interação com repositório de resposta ou estatística. |
| TU-18 | `PublicacaoService.despublicar` de rascunho | CA-08 | Lança `TransicaoInvalidaException`. |
| TU-19 | `PublicacaoService.enviarParaRevisao` a partir de publicada | — | Lança `TransicaoInvalidaException`. |
| TU-20 | `BuscaQuestaoService` reconhece termo em formato de código | CA-02 | `Q-88409` e `88409` viram busca por prefixo de `codigo`, não busca textual. |
| TU-21 | `BuscaQuestaoService` combina termo e três filtros | CA-02 | Predicados de termo, disciplina, banca e status compostos por `and`. |
| TU-22 | `BuscaQuestaoService` rejeita `sort` fora da lista branca | CA-02 | Lança `DadosInvalidosException` — nenhuma coluna arbitrária chega ao SQL. |
| TU-23 | `TaxonomiaService.excluirAssunto` com questões vinculadas | CA-11 | Lança `ConflitoException` `ASSUNTO_EM_USO` com a contagem na mensagem. |
| TU-24 | `TaxonomiaService.excluirAssunto` sem vínculos | CA-11 | Marca `excluido_em`. |
| TU-25 | `TaxonomiaService.arquivarAssunto` | CA-11 | Marca `arquivado_em`; as questões vinculadas permanecem inalteradas. |
| TU-26 | `TaxonomiaService.renomearAssunto` | CA-12 | Só `nome` muda; nenhum `update` em `questao`. |
| TU-27 | `TaxonomiaService` com nome duplicado entre irmãos | CA-10 | Lança `ConflitoException` `NOME_EM_USO`. |
| TU-36 | `CatalogoService.criarBanca` com nome e sigla válidos | CA-13 | Persiste e devolve `ItemCatalogo`. |
| TU-37 | `CatalogoService.excluirBanca` com questões vinculadas | CA-14 | Lança `ConflitoException` `BANCA_EM_USO` com a contagem na mensagem. |
| TU-38 | `CatalogoService.criarOrgao` com nome e sigla válidos | CA-13 | Persiste e devolve `ItemCatalogo`. |
| TU-39 | `CatalogoService.excluirOrgao` com questões vinculadas | CA-14 | Lança `ConflitoException` `ORGAO_EM_USO` com a contagem na mensagem. |
| TU-40 | `CatalogoService` com nome de banca ou órgão duplicado | CA-13 | Lança `ConflitoException` `NOME_EM_USO`. |
| TU-28 | `EstatisticaQuestaoService.recalcular` excluindo o autor | CA-09 | Respostas do autor fora do numerador e do denominador. |
| TU-29 | `EstatisticaQuestaoService.recalcular` sem respostas de terceiros | CA-09 | `totalRespostas` zero e `pctAcerto` `null`, sem divisão por zero. |
| TU-30 | `GeradorCodigoQuestao` formata a sequência | CA-05 | `10000` → `Q-10000`; unicidade garantida pela sequência. |
| TU-31 | `QuestoesFiltros` espelha filtros nos query params (Vitest) | CA-02 | Alterar um filtro reescreve a URL e zera a página. |
| TU-32 | `QuestoesFiltros` restaura estado ao voltar do formulário (Vitest) | CA-02 | Termo, filtros e página reconstruídos a partir da URL. |
| TU-33 | `QuestaoFormStore` troca de tipo (Vitest) | CA-04 | `CE` esconde o bloco de alternativas e expõe a escolha Certo/Errado. |
| TU-34 | `QuestaoFormStore` limita alternativas a cinco (Vitest) | CA-03 | O botão "Adicionar alternativa" desabilita em `E`. |
| TU-35 | `QuestaoFormStore` mapeia `PUBLICACAO_INVALIDA` (Vitest) | CA-06 | Cada pendência marca seu `FormControl`; os demais campos ficam limpos e preenchidos. |

Componentes cobertos: `ValidadorPublicacao`, `QuestaoService`, `PublicacaoService`, `BuscaQuestaoService`, `TaxonomiaService`, `CatalogoService`, `EstatisticaQuestaoService`, `GeradorCodigoQuestao`, `QuestoesFiltros` e `QuestaoFormStore`. Serviços puros com JUnit + Mockito, sem contexto Spring; o relógio é um `Clock` injetado, nunca `Instant.now()` direto. Mocks apenas para o que é externo ao componente sob teste — repositórios e `RegistradorAuditoria`. `ValidadorPublicacao` não tem dependência alguma e é testado como função pura, que é o motivo de ele existir separado do `PublicacaoService`.

### Testes de integração

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | Migração `V2` e `V3` em base limpa | — | Flyway aplica sem erro; tabelas, índices, sequência e `check` criados; sementes de banca e órgão presentes. |
| TI-02 | `GET /admin/questoes` sem filtros (`@WebMvcTest` + `MockMvcTester`) | CA-01 | 200 com envelope `{content, page, size, totalElements}` e as colunas de RF-18. |
| TI-03 | `GET /admin/questoes` com termo, disciplina, banca e status | CA-02 | Somente os itens correspondentes; `totalElements` recalculado. |
| TI-04 | `GET /admin/questoes?q=Q-88409` | CA-02 | Busca por código devolve exatamente a questão do código. |
| TI-05 | Busca textual com acento e flexão (`licitações` × `licitacao`) | CA-02 | `to_tsvector('portuguese')` casa os dois; asserção sobre Postgres real. |
| TI-06 | `GET /admin/questoes` com 100 mil questões semeadas | CA-01 | Primeira página responde abaixo do orçamento; `explain` confirma uso de `questao_filtro_idx`. |
| TI-07 | `GET /admin/questoes` como `ESTUDANTE` | Autorização (PRD) | 403 `ACESSO_NEGADO` decidido no servidor. |
| TI-08 | `POST /admin/questoes` com rascunho mínimo | CA-05 | 201, status `RASCUNHO`, `codigo` gerado, linha em `questao_estatistica` criada. |
| TI-09 | `POST /admin/questoes` com cinco alternativas e gabarito | CA-03 | 201 com letras `A`–`E` e exatamente um gabarito. |
| TI-10 | `POST /admin/questoes` tipo `CE` | CA-04 | 201 com `alternativas` vazio e gabarito `CERTO`. |
| TI-11 | `PUT /admin/questoes/{id}` em questão publicada | Fluxo *Corrigir* | 200; status permanece `PUBLICADA` e `publicadoEm` inalterado. |
| TI-12 | `POST /admin/questoes/{id}/publicar` sem comentário | CA-06 | 422 `PUBLICACAO_INVALIDA` com `fieldErrors.comentario`; status inalterado no banco. |
| TI-13 | `POST /admin/questoes/{id}/publicar` como `PROFESSOR` | Autorização (PRD) | 403 mesmo com a questão completa. |
| TI-14 | `POST /admin/questoes/{id}/publicar` como `ADMIN` | CA-06, CA-07 | 200, status `PUBLICADA` e linha em `auditoria` com ator, ação e entidade. |
| TI-15 | Consulta de sorteio ignora rascunho e revisão | CA-07 | A consulta de montagem de série só devolve `PUBLICADA`; rascunho e revisão nunca aparecem. |
| TI-16 | Despublicar preserva respostas e estatísticas | CA-08 | Após `despublicar`, a questão sai da consulta de sorteio; contagem em `resposta` e `questao_estatistica` inalterada. |
| TI-17 | `DELETE /admin/questoes/{id}` com respostas | Integridade (PRD) | 409 `QUESTAO_EM_USO`; a linha continua na base. |
| TI-18 | `DELETE /admin/questoes/{id}` sem respostas | Integridade (PRD) | 204; a questão some da listagem e da consulta de sorteio, sem `delete` físico. |
| TI-19 | `check` `questao_publicada_completa` (`@DataJpaTest`) | CA-06 | `update` direto para `PUBLICADA` sem gabarito é rejeitado pelo banco. |
| TI-20 | `GET /admin/disciplinas` com árvore povoada | CA-10 | Cada disciplina e cada assunto trazem `totalQuestoes` e `totalPublicadas` corretos, numa única consulta (sem N+1). |
| TI-21 | Contagem ignora questões excluídas e conta as sem assunto na disciplina | CA-10 | Totais batem com a base semeada. |
| TI-22 | `DELETE /admin/assuntos/{id}` com questões vinculadas | CA-11 | 409 `ASSUNTO_EM_USO` com a contagem na mensagem. |
| TI-23 | `POST /admin/assuntos/{id}/arquivar` seguido de `GET /admin/disciplinas` | CA-11 | 204; o assunto some da árvore padrão e reaparece com `incluirArquivados=true`; as questões seguem vinculadas. |
| TI-24 | `PUT /admin/assuntos/{id}` renomeando | CA-12 | 200; a listagem de questões passa a exibir o novo nome, com `assunto_id` inalterado. |
| TI-25 | `POST /admin/disciplinas` com nome duplicado (ignorando maiúsculas) | CA-10 | 409 `NOME_EM_USO`; o índice único parcial garante a corrida entre requisições simultâneas. |
| TI-26 | `GET /admin/bancas` e `/admin/orgaos` | CA-03 | 200 com o catálogo ordenado por nome. |
| TI-27 | Recalcular estatística com respostas do autor e de terceiros | CA-09 | `pctAcerto` calculado só sobre terceiros; persistido em `questao_estatistica`. |
| TI-28 | Auditoria das ações administrativas | Auditoria (PRD) | Criar, editar, publicar, despublicar, excluir e arquivar geram linha em `auditoria` com ator, ação, entidade e `payload`. |
| TI-29 | `POST /admin/bancas` como `ADMIN` | CA-13 | 201 com `ItemCatalogo`; item aparece em `GET /admin/bancas` na sequência. |
| TI-30 | `POST /admin/bancas` como `PROFESSOR` | Autorização (PRD) | 403 `ACESSO_NEGADO`. |
| TI-31 | `DELETE /admin/bancas/{id}` com questões vinculadas | CA-14 | 409 `BANCA_EM_USO`; a linha continua na base. |
| TI-32 | `PUT /admin/orgaos/{id}` renomeando | RF-37 | 200; a listagem de questões passa a exibir o novo nome. |
| TI-33 | `DELETE /admin/orgaos/{id}` com questões vinculadas | CA-14 | 409 `ORGAO_EM_USO`. |
| TI-34 | `DELETE /admin/bancas/{id}` sem vínculos | RF-37 | 204; item some de `GET /admin/bancas`. |

Componentes testados em conjunto: controller + segurança + serviço + repositório + Flyway sobre PostgreSQL 18 real via Testcontainers (`@ServiceConnection`), sem H2 e sem banco em memória — `tsvector`, índice GIN, índice único parcial e `check` de publicação só existem no Postgres, e testá-los em outro motor não provaria nada. Fatia mais estreita por cenário: `@DataJpaTest` para TI-01, TI-19 e TI-21; `@WebMvcTest` com `MockMvcTester` para os cenários de contrato; `@SpringBootTest` só nos fluxos completos de publicação e auditoria. Dados criados por builders no pacote de teste; TI-06 usa carga em massa por `INSERT ... SELECT generate_series`, gerada uma vez por classe e reaproveitada, e é a única marcada como lenta. O contexto Spring é compartilhado entre as classes de integração para manter a suíte rápida.

TI-15, TI-16 e TI-27 dependem de `sessao_estudo` e `resposta`, que pertencem a `prd-sessao-de-estudo`. Como esta funcionalidade precisa provar RN-01 e RN-03 agora, a migração `V2` cria a consulta e o serviço, e os testes semeiam as duas tabelas por uma migração de teste (`db/migration-test/V990__resposta_minima.sql`) restrita ao *classpath* de teste. Quando `prd-sessao-de-estudo` criar as tabelas definitivas, essa migração de teste é removida e os testes passam a usar as reais — registrado em *Riscos conhecidos*.

> **Dívida de F2:** a entrega de `prd-sessao-de-estudo` deve remover `backend/src/test/resources/db/migration-test/V990__resposta_minima.sql` na mesma alteração em que criar as tabelas definitivas `sessao_estudo` e `resposta`, mantendo os testes com o esquema real.

### Testes E2E

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | Abrir a listagem como professor | CA-01 | Tabela com código, enunciado resumido, disciplina, banca/ano, tipo e status; rodapé com total e paginação. |
| E2E-02 | Buscar por texto e filtrar por disciplina, banca e status | CA-02 | Lista reduzida às correspondências; rodapé e botões de paginação recalculados. |
| E2E-03 | Buscar por código e abrir a questão para edição | CA-02 | O fluxo *Corrigir* do PRD: busca por `Q-…`, abre, edita e salva. |
| E2E-04 | Voltar do formulário para a lista | Experiência do usuário | Termo, filtros e página preservados — a lista volta no mesmo ponto. |
| E2E-05 | Cadastrar questão de múltipla escolha com cinco alternativas | CA-03 | Alternativas A–E, exatamente um gabarito marcado, salvo com sucesso. |
| E2E-06 | Trocar o tipo para Certo/Errado | CA-04 | O bloco de alternativas é substituído pela escolha Certo/Errado. |
| E2E-07 | Salvar rascunho só com o enunciado | CA-05 | Questão gravada como Rascunho e visível na lista com esse status. |
| E2E-08 | Tentar publicar questão incompleta | CA-06 | Publicação recusada com cada campo pendente marcado inline; nada do que foi digitado se perde. |
| E2E-09 | Publicar como administrador | CA-06 | Status passa a Publicada na lista, com rótulo textual. |
| E2E-10 | Botão de publicar ausente para o professor | Autorização (PRD) | A ação não aparece na interface; a chamada direta à API devolve 403 (verificada na mesma suíte). |
| E2E-11 | Despublicar com confirmação | CA-08 | Diálogo de confirmação explícito; status volta a Em revisão. |
| E2E-12 | Abrir a árvore de matérias | CA-10 | Cada disciplina e cada assunto exibem a contagem de questões. |
| E2E-13 | Excluir assunto com questões vinculadas | CA-11 | Exclusão recusada e o diálogo oferece arquivar; ao arquivar, o assunto some da árvore. |
| E2E-14 | Renomear assunto | CA-12 | Após confirmar, a lista de questões exibe o novo nome nas questões vinculadas. |
| E2E-15 | Percurso por teclado no formulário e contraste dos status | Experiência do usuário | `Tab` e `Enter` operam tudo com foco visível; os rótulos de status têm texto e contraste ≥ 4,5:1. |
| E2E-16 | Cadastrar banca e órgão como administrador e tentar excluir item vinculado | CA-13, CA-14 | Item criado aparece no combo do formulário; exclusão de item vinculado é recusada com mensagem explícita. |

Execução com Playwright contra o SPA e a API reais, com PostgreSQL em contêiner e a semente de `dev`. Os seletores saem da marcação do protótipo traduzida 1:1 (`.table`, `.field`, `.input`, `.seg`, `.tag`, `.btn-primary`, `.dialog`). E2E-02, E2E-08, E2E-11 e E2E-13 dependem das lacunas de protótipo listadas em *Riscos conhecidos*, acrescentadas no passo 1 da ordem de construção; E2E-16 depende da tela `ADMIN: CATÁLOGO`, desenhada no mesmo passo. A verificação automatizada de acessibilidade e contraste (E2E-15) roda sobre as quatro telas.

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Design: fechar as lacunas do protótipo e desenhar `ADMIN: CATÁLOGO`** — acrescentar ao `Certame.dc.html`, com componentes Nocturne existentes, o campo de busca da lista, as ações de publicar/despublicar na linha, o bloco de gabarito Certo/Errado no formulário, o bloco de erro de publicação, o diálogo de confirmação com a oferta de arquivar, e a nova tela `ADMIN: CATÁLOGO` (cadastro de bancas e órgãos, RF-37). Primeiro porque a regra 1 do `AGENTS.md` impede que qualquer marcação nasça direto no Angular, e a aprovação visual do usuário libera o passo 9.
2. **Migração e persistência** — `V2__banco_questoes.sql`, `V3__seed_catalogo.sql`, entidades `Disciplina`, `Assunto`, `Banca`, `Orgao`, `Questao`, `Alternativa`, `QuestaoEstatistica` e `Auditoria`, com repositórios e TI-01, TI-19, TI-21. Antes de qualquer regra, porque o modelo sustenta todo o resto.
3. **Fundação transversal** — `PaginaResposta<T>`, `RegistradorAuditoria` e os novos tipos de exceção no `ManipuladorErrosGlobal`. É contrato compartilhado com todas as features administrativas seguintes.
4. **Taxonomia e catálogo administrativo** (RF-20, RF-37) — `TaxonomiaService`, `CatalogoService` e `CatalogoController`, com TU-23…27, TU-36…40 e TI-20…26, TI-29…34. Vem antes das questões porque o formulário depende de disciplina, assunto, banca e órgão existirem.
5. **Questão e alternativas** (RF-19) — `ValidadorPublicacao` (regra pura, primeiro), `QuestaoService`, `GeradorCodigoQuestao` e o `QuestaoAdminController`, com TU-01…14, TU-30 e TI-08…11, TI-17, TI-18.
6. **Publicação** (RN-01) — `PublicacaoService` e as três transições, com TU-15…19 e TI-12…16, TI-28. Depende do passo 5 e fecha o portão do acervo.
7. **Busca e listagem** (RF-18) — `BuscaQuestaoService` com `tsvector`, filtros e paginação, com TU-20…22 e TI-02…07. Deixado por último no backend porque exige o acervo dos passos anteriores para ser medido de verdade (TI-06).
8. **Estatística** (RN-03) — `EstatisticaQuestaoService`, a migração de teste de `resposta` e TU-28, TU-29, TI-27. Sem interface.
9. **Frontend** — `questao-api`, `catalogo-api`, `QuestoesFiltros`, `QuestaoFormStore`, as quatro páginas em tradução 1:1 do protótipo aprovado no passo 1, o `confirmacao-dialog` e as rotas sob `perfilGuard`, com TU-31…35.
10. **E2E e endurecimento** — E2E-01…16, verificação da cobertura de 70% em `service`, medição do p95 de CA-01 e revisão de acessibilidade e contraste.

### Dependências técnicas

| Dependência | Situação | Impacto |
| --- | --- | --- |
| `prd-acesso-e-conta` implementado (`SegurancaConfig`, `perfilGuard`, `ManipuladorErrosGlobal`, `Usuario`) | Especificado, ainda não implementado | **Bloqueia tudo.** Sem `usuario` não há FK de `autor_id`, e sem a cadeia de segurança não há como provar autorização por perfil. |
| Protótipo `Certame.dc.html` com as telas administrativas | **Presente** — lista, formulário e árvore já desenhados | Restam as cinco lacunas do passo 1 e a aprovação visual, que libera o passo 9. |
| `styles.css` do Nocturne em `frontend/src/styles.css` | Entregue por `prd-acesso-e-conta` (passo 8a) | Bloqueia o passo 9. |
| Tabelas `sessao_estudo` e `resposta` | Pertencem a `prd-sessao-de-estudo` (F2) | Não bloqueiam: TI-15, TI-16 e TI-27 usam a migração de teste `V990`, descartada quando as tabelas reais existirem. |
| PostgreSQL 18 e Docker | Necessários para Testcontainers e para o Compose de `dev` | Bloqueiam toda a suíte de integração; `tsvector` e o índice GIN não têm equivalente em banco em memória. |
| Playwright | Ausente do `package.json`; previsto em `prd-acesso-e-conta` | Bloqueia o passo 10. |
| Lista definitiva de bancas e órgãos para a semente | A confirmar com o usuário antes do passo 2 | Não bloqueia: `V3` nasce com as oito bancas mais recorrentes e é complementada por nova migração, nunca por edição da existente. |

## Monitoramento e observabilidade

Sobre Actuator + Micrometer/Prometheus e logs JSON com `traceId`, conforme a §9 da `TECH-SPEC.md`.

| Sinal | Tipo | Uso |
| --- | --- | --- |
| `certame.questao.publicada` | contador, tags `perfil` e `disciplina` | Alimenta a métrica editorial do PRD (≥ 40 questões publicadas por professor por semana). |
| `certame.questao.publicacao.recusada` | contador, tag `pendencia` (`gabarito`, `comentario`, `alternativas`) | Mostra qual campo mais trava a esteira — é o dado que orienta ajuste de formulário. |
| `certame.questao.despublicada` | contador | Frequência de correção pós-publicação; alta indica falha de revisão. |
| `certame.questao.busca` | *timer* (p95, p99), tags `comFiltro` e `comTermo` | Verifica o orçamento de 800 ms de CA-01 em produção, não só em teste. |
| `certame.taxonomia.exclusao.recusada` | contador | Quantas vezes CA-11 é acionado; indica necessidade de reorganização da árvore. |
| `certame.acervo.publicadas` | *gauge*, tag `disciplina` | Acompanha a meta de 5.000 questões publicadas antes do lançamento e revela onde o acervo está raso. |
| `/actuator/health` com indicador de banco | *health check* | Prontidão do contêiner; já existente. |

**Eventos de log.** `INFO` para criação, edição, publicação, despublicação e exclusão de questão e para as operações de taxonomia — sempre com `id` da questão, `codigo`, ator e ação, nunca com o enunciado ou o comentário completos. `WARN` para publicação recusada por validação, exclusão recusada por vínculo e transição inválida. `ERROR` apenas para falha de infraestrutura. A `auditoria` é o registro autoritativo das ações administrativas exigido pelo PRD; o log é diagnóstico, não substituto.

## Considerações técnicas

### Principais decisões

- **Transições de status em endpoints próprios, não em `PUT`.** `enviar-revisao`, `publicar` e `despublicar` têm perfis e validações diferentes de uma edição comum. Um `PUT` que também mudasse o status obrigaria a decidir a autorização por diferença de campo — frágil e difícil de testar. O custo é uma rota a mais que a §6 previa; o ganho é que RN-01 e CA-06 viram três casos de teste isolados.
- **`ValidadorPublicacao` como regra pura, separada do serviço.** Ele não conhece Spring, repositório nem transação: recebe uma `Questao` e devolve o mapa de pendências. É o coração de CA-06, e mantê-lo puro permite testar as sete combinações de falta em milissegundos.
- **Recusa de publicação devolve todas as pendências de uma vez**, em `fieldErrors`, e não a primeira. O PRD pede "a indicação de cada campo pendente" — validação campo a campo em série faria o professor descobrir os problemas um por vez.
- **Rede de segurança no banco (`questao_publicada_completa`).** A regra de negócio vive no serviço, mas um `check` impede que qualquer caminho futuro — migração, script, endpoint novo — publique uma questão pela metade. Redundância deliberada em cima de RN-01.
- **Despublicar move para `EM_REVISAO`, não para `RASCUNHO`.** O caso de borda 2 do PRD descreve correção de questão publicada; devolvê-la a rascunho perderia a informação de que ela já foi revisada. `publicado_em` é preservado como registro histórico.
- **Exclusão lógica em toda parte, e bloqueada quando há respostas** (decisão do usuário). A restrição de integridade do PRD é explícita: o acervo é referenciado por respostas, cadernos, simulados e flashcards. `excluido_em` mais o índice único parcial cobrem ambos os casos sem cascata destrutiva.
- **Duplicar questão fica fora** (decisão do usuário). O ícone existe no protótipo, mas o PRD não o pede; ele sai da tradução, e o desvio está registrado em *Riscos conhecidos*.
- **Paginação por offset com contagem exata** (decisão do usuário), e não o keyset sugerido pela §8 da `TECH-SPEC.md`. CA-02 exige paginação recalculada e o protótipo mostra o total explícito; keyset não dá total nem salto de página. O orçamento de 800 ms é sustentado por `questao_filtro_idx` sobre `(status, disciplina_id, banca_id, atualizado_em desc)` com `where excluido_em is null`, e TI-06 mede com 100 mil linhas. Se o p95 estourar em produção, a saída é contagem estimada — mudança de uma consulta, sem alterar o contrato.
- **Busca com `tsvector` gerado e materializado, não calculado na consulta.** Coluna `busca generated always as … stored` com índice GIN: o custo de indexação é pago na escrita, que é rara nesta funcionalidade, e não em cada leitura. Segue a §11 da `TECH-SPEC.md`, que começa nativo e reavalia acima de 200 mil questões.
- **Termo em formato de código vira busca por prefixo.** `Q-88409` como `plainto_tsquery` não acha nada; o fluxo *Corrigir* do PRD começa exatamente por essa busca. A heurística é uma expressão regular no serviço, testada por TU-20.
- **Letras das alternativas atribuídas pelo servidor a partir da `ordem`.** O cliente envia texto e posição; `A`–`E` é derivado. Elimina a classe inteira de bugs de letra duplicada ou fora de sequência, e o `unique (questao_id, letra)` fecha a porta no banco.
- **Banca e órgão como catálogo plano, semeado e mantido pelo administrador** (decisão revisada do usuário — a versão anterior da TechSpec previa semente sem CRUD; RF-37 acrescenta a tela). Ainda não é a taxonomia de duas camadas de RF-20 nem tem hierarquia: é uma lista mantida por `CatalogoService`, com o mesmo motivo original de existir — texto livre produziria "Cebraspe", "CEBRASPE" e "Cespe" na mesma base. A semente `V3` continua entrando em todos os ambientes como ponto de partida; a diferença é que agora pode ser corrigida ou ampliada pelo administrador sem depender de nova migração. Exclusão é física (não há `arquivado_em`/`excluido_em` em `banca`/`orgao`), bloqueada quando há questão vinculada — a FK sem `on delete` em `questao` é a rede de segurança do banco, e o serviço antecipa a checagem para devolver `409` amigável (CA-14).
- **`arquivado_em` em vez de uma coluna `status` na taxonomia.** O único estado alternativo previsto é arquivado; uma marca temporal registra também *quando*, útil na auditoria, e não exige `check` nem enum.
- **Contagem de questões derivada, nunca armazenada**, conforme a §5. Uma consulta agregada com `group by` alimenta a árvore inteira em uma requisição; contador materializado exigiria manter sincronia em toda escrita, e a árvore é lida com pouca frequência.
- **CA-12 é consequência do modelo.** O vínculo é por `assunto_id`; renomear é um `update` de uma linha. Nenhuma atualização em massa, nenhuma desnormalização — o teste TI-24 apenas prova a propriedade.
- **RN-03 especificado e testado, sem interface** (decisão do usuário). `questao_estatistica` e a consulta com exclusão do autor nascem aqui, junto do requisito; quem exibe é `prd-sessao-de-estudo` (RF-08). Adiar o esquema faria a regra ser reinventada por outra feature.
- **`auditoria` chamada explicitamente pelos serviços, não por AOP.** Um aspecto sobre anotação pareceria mais limpo, mas tornaria invisível *o que* é auditado, e o `payload` precisa do estado de domínio que só o serviço conhece. Seis chamadas explícitas custam menos do que um aspecto mágico.
- **Reactive Forms, não Signal Forms**, mantendo a decisão de `prd-acesso-e-conta`: a §2 da `TECH-SPEC.md` admite Signal Forms "onde estável", e o formulário de questão — com bloco dinâmico de alternativas — não é o lugar de assumir esse risco. Revisar na F3.
- **Filtros nos *query params*, não num store em memória.** A exigência do PRD de "voltar da edição devolve a lista no mesmo ponto" é atendida pela URL, que sobrevive a recarga, a *deep link* e ao botão voltar do navegador — coisas que um store em memória não cobre.

### Riscos conhecidos

| Risco | Mitigação |
| --- | --- |
| **O protótipo não cobre cinco peças desta funcionalidade:** campo de busca por texto na lista (CA-02 pede busca, e a tela só tem três selects), ações de publicar/despublicar na linha (US4), bloco de gabarito Certo/Errado no formulário (CA-04 — hoje o bloco de alternativas é fixo), erro inline de publicação (CA-06) e diálogo de confirmação com oferta de arquivar (CA-11 e as ações destrutivas exigidas pelo PRD). | Todas entram primeiro no `Certame.dc.html`, com componentes Nocturne existentes (`.input`, `.seg`, `.dialog`, `.tag`, o papel `--color-danger-*` já registrado por `prd-acesso-e-conta`), no passo 1 da ordem de construção. Nenhuma marcação nasce direto no Angular. |
| **O protótipo não tem tela alguma para RF-37** (cadastro de bancas e órgãos) — requisito acrescentado depois da primeira versão desta TechSpec. | Tela nova `ADMIN: CATÁLOGO` desenhada no protótipo no mesmo passo 1, com os componentes Nocturne de listagem e formulário já usados nas outras telas administrativas — não é redesenho, é reuso do padrão visual existente. |
| **O ícone "Duplicar" do protótipo não é implementado.** | Desvio visual registrado: o botão sai da tradução no passo 1, junto das demais alterações, para que protótipo e código não divirjam. Volta como funcionalidade própria em v2, se o time editorial pedir. |
| **O campo Órgão é `input` livre no protótipo, mas vira `select` no modelo.** | Alteração feita no protótipo no passo 1, alinhando-o ao modelo da §5 (`orgao_id`) e à decisão de semear o catálogo. |
| **TI-15, TI-16 e TI-27 dependem de tabelas que ainda não existem** (`sessao_estudo`, `resposta`). | Migração de teste `db/migration-test/V990__resposta_minima.sql`, restrita ao *classpath* de teste e sem efeito em `dev`, `staging` ou `prod`. Quando `prd-sessao-de-estudo` criar as tabelas definitivas, essa migração é removida na mesma entrega e os testes passam a usar as reais — item explícito da tarefa de F2. |
| **Orçamento de 800 ms (p95) com 100 mil questões pode estourar com busca textual e filtro combinados**, porque o planejador pode preferir o GIN ao índice de filtros, ou vice-versa. | TI-06 mede e inspeciona o plano; `certame.questao.busca` monitora em produção. Escapes já mapeados, em ordem: `size` máximo de 100, contagem estimada acima de um teto, e — acima de 200 mil questões, como prevê a §11 — reavaliar motor de busca externo. |
| **A busca textual cobre só o enunciado, não o comentário nem as alternativas.** Um professor que procure por um termo que só aparece na alternativa não encontra a questão. | Aceito na v1: é o que a §5 da `TECH-SPEC.md` define e o que RF-18 pede. Ampliar o `tsvector` depois é uma migração aditiva, sem alteração de contrato. |
| **A meta de 5.000 questões publicadas antes do lançamento**, a 40 por professor por semana, exige um time editorial que a ferramenta sozinha não garante. | Fora do controle técnico, mas instrumentado: `certame.acervo.publicadas` por disciplina mostra o ritmo real e onde o acervo está raso. A importação em massa, mitigação prevista pelo PRD, continua fora do escopo. |
| **A semente inicial de bancas e órgãos é um chute informado.** | `V3` nasce com as bancas mais recorrentes; a lista definitiva de órgãos é confirmada com o usuário antes do passo 2. Diferente de antes, o risco é menor: como RF-37 dá ao administrador uma tela de CRUD, um item ausente ou incorreto na semente não bloqueia ninguém — é corrigido pela própria tela, sem depender de nova migração. |
| **`auditoria` guarda `payload` jsonb com estado de domínio**, e pode crescer sem limite. | Nesta fase, o `payload` guarda apenas o delta relevante (campos alterados, status anterior e novo), nunca o enunciado ou o comentário completos. Política de retenção fica para o endurecimento da F5, junto com o CRUD de usuários que também audita. |

### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. **Não existe o diretório `.agents/rules/` neste projeto** — não há rules adicionais a observar; se ele for criado, esta especificação deve ser reavaliada.

| Regra | Como esta especificação atende |
| --- | --- |
| 1 — Frontend é tradução 1:1 do protótipo, tokens Nocturne, sem UI de terceiros | As três telas saem do `Certame.dc.html` (`ADMIN: QUESTÕES`, `ADMIN: FORMULÁRIO DE QUESTÃO`, `ADMIN: MATÉRIAS`) e do `styles.css` do Nocturne, sem valor cru. As cinco lacunas entram primeiro no protótipo (passo 1), não no código. Nenhuma biblioteca de UI é adicionada. |
| 2 — Autorização validada no servidor com `@PreAuthorize` | Cadastrar e editar questão exigem `ROLE_PROFESSOR`+; publicar, despublicar e o CRUD de bancas/órgãos (RF-37) exigem `ROLE_ADMIN`+, no serviço. TI-07, TI-13, TI-30 e E2E-10 provam 403 no servidor mesmo com o botão escondido. |
| 3 — Interface em pt-BR; código, tabelas e colunas em português sem acento | Classes `QuestaoService`, `TaxonomiaService`, `ValidadorPublicacao`; colunas `enunciado`, `gabarito`, `dificuldade`, `arquivado_em`, `excluido_em`; mensagens de erro e rótulos em pt-BR. |
| 4 — Nenhum endpoint fora do contrato da §6 sem atualizar o documento | Cinco rotas já constam da §6. As onze acrescentadas (`enviar-revisao`, `despublicar`, `DELETE` de questão, `arquivar` de assunto, `GET`/`POST`/`PUT`/`DELETE` de `/admin/bancas` e `/admin/orgaos`) exigem **atualização da §6 da `TECH-SPEC.md` na mesma entrega** — registrado em *Arquivos relevantes* e no passo 3 da ordem de construção. |
| 5 — Migração só por Flyway, versionada e imutável | `V2__banco_questoes.sql` e `V3__seed_catalogo.sql` em `db/migration`; semente de `dev` isolada em `db/migration-dev/V901`; migração de teste em `db/migration-test/V990`, fora do `classpath` de produção. Complementos ao catálogo sempre por migração nova. |
| 6 — `resposta` imutável e Modo Prova | Respeitado por omissão deliberada: despublicar e excluir **nunca** tocam em `resposta` (CA-08, TI-16, TI-17). |
| 7, 8, 9 — Flashcards, diálogo na sessão e fila de revisão | Não se aplicam a esta funcionalidade. Os flashcards apenas referenciam `disciplina`, `assunto` e `questao` criados aqui. |
| Toda tarefa cita o requisito | Cada componente, endpoint e caso de teste referencia RF-18, RF-19, RF-20, RF-37, RN-01, RN-03 ou os CA-01…CA-14 do PRD. |
| Domínio + testes antes de controller e frontend | A ordem de construção segue exatamente isso (passos 2 → 4 → 5 → 6 → 7 → 9). |
| Cobertura mínima de 70% em `service` | Declarada na abordagem de testes e verificada no passo 10. |
| Perguntar em vez de inventar, e registrar a decisão | Quatro decisões abertas foram levadas ao usuário (ações da lista, banca e órgão, RN-03 e paginação) e estão registradas em *Principais decisões*. |

### Conformidade com skills

| Skill | Aplicável | Como é observada |
| --- | --- | --- |
| `313-frameworks-spring-db-migrations-flyway` | sim | SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração de `V1`; `validate-on-migrate` ligado; disciplina forward-only — nenhuma migração aplicada é reescrita, complementos de catálogo entram como versão nova; `spring.flyway.locations` separa produção, `dev` e teste. |
| `postgresql-table-design` | sim | PKs `uuid` com `gen_random_uuid()`, `timestamptz` em UTC, `check` para enums textuais, índices únicos parciais compatíveis com exclusão lógica, coluna `tsvector` gerada e `stored` com índice GIN, índice composto alinhado aos filtros da listagem e `check` de invariante de negócio (`questao_publicada_completa`). |
| `spring-boot-testing` | sim | Fatia mais estreita por cenário: JUnit + Mockito puros no serviço, `@DataJpaTest` com Testcontainers no repositório e nas constraints, `@WebMvcTest` com `MockMvcTester` no controller e `@SpringBootTest` só nos fluxos completos de publicação e auditoria; asserções em AssertJ; contexto compartilhado para manter a suíte rápida. |
| `angular-developer` | sim | Componentes standalone, detecção zoneless, estado por signals, guardas e interceptores funcionais, Reactive Forms com `FormArray` para as alternativas e sincronização de filtros por `queryParams` do Router. |
| `impeccable` | não | O layout é congelado pela regra 1 do `AGENTS.md`. As cinco lacunas são acréscimos com componentes Nocturne existentes, não redesenho; a acessibilidade exigida pelo PRD é coberta por E2E-15. |
| `find-skills`, `criar-*`, `executar-*` | não | São skills de processo, não de implementação. |

### Arquivos relevantes e dependentes

**Criados — backend** (`backend/src/main/java/br/com/certame/`)

`catalogo/Disciplina.java` · `catalogo/Assunto.java` · `catalogo/Banca.java` · `catalogo/Orgao.java` · `catalogo/DisciplinaRepository.java` · `catalogo/AssuntoRepository.java` · `catalogo/BancaRepository.java` · `catalogo/OrgaoRepository.java` · `catalogo/TaxonomiaService.java` · `catalogo/CatalogoService.java` · `catalogo/CatalogoController.java` · `catalogo/dto/` · `questao/Questao.java` · `questao/Alternativa.java` · `questao/QuestaoEstatistica.java` · `questao/TipoQuestao.java` · `questao/StatusQuestao.java` · `questao/Dificuldade.java` · `questao/QuestaoRepository.java` · `questao/AlternativaRepository.java` · `questao/QuestaoEstatisticaRepository.java` · `questao/QuestaoService.java` · `questao/PublicacaoService.java` · `questao/ValidadorPublicacao.java` · `questao/BuscaQuestaoService.java` · `questao/GeradorCodigoQuestao.java` · `questao/EstatisticaQuestaoService.java` · `questao/QuestaoAdminController.java` · `questao/dto/` · `comum/PaginaResposta.java` · `comum/auditoria/Auditoria.java` · `comum/auditoria/AuditoriaRepository.java` · `comum/auditoria/RegistradorAuditoria.java` · `comum/excecao/RecursoNaoEncontradoException.java` · `comum/excecao/ConflitoException.java` · `comum/excecao/PublicacaoInvalidaException.java` · `backend/src/main/resources/db/migration/V2__banco_questoes.sql` · `backend/src/main/resources/db/migration/V3__seed_catalogo.sql` · `backend/src/main/resources/db/migration-dev/V901__seed_banco_questoes_dev.sql` · `backend/src/test/resources/db/migration-test/V990__resposta_minima.sql`

**Criados — frontend** (`frontend/src/app/`)

`core/api/questao-api.ts` · `core/api/catalogo-api.ts` · `core/api/modelos-questao.ts` · `features/admin/questoes/questoes-page/` · `features/admin/questoes/questoes-filtros.ts` · `features/admin/questoes/questao-form-page/` · `features/admin/questoes/questao-form-store.ts` · `features/admin/materias/materias-page/` · `features/admin/catalogo/catalogo-page/` · `shared/ui/confirmacao-dialog/`

**Modificados**

`backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java` (novos códigos de erro, incluindo `BANCA_EM_USO` e `ORGAO_EM_USO`) · `frontend/src/app/app.routes.ts` (cinco rotas sob `perfilGuard`) · `protótipo/Certame.dc.html` (campo de busca, ações de publicação, bloco Certo/Errado, erro inline, diálogo de confirmação, Órgão como `select`, remoção do botão Duplicar, e a nova tela `ADMIN: CATÁLOGO`) · **`TECH-SPEC.md` §6** (as onze rotas acrescentadas ao contrato, conforme a regra 4 do `AGENTS.md`) · **`TECH-SPEC.md` §5** (colunas `orgao_id`, `publicado_em`, `excluido_em` e a coluna gerada `busca` em `questao`; `arquivado_em` em `disciplina` e `assunto`)

**Dependentes** — `prd-sessao-de-estudo` (RF-06…RF-12) e `prd-simulados` (RF-14, RF-21) montam suas séries a partir de `questao` filtrando `status = 'PUBLICADA'` (RN-01) e consomem `questao_estatistica` (RN-03, RF-08); `prd-cadernos-e-desempenho` agrega por `disciplina` e `assunto`; `prd-flashcards-criacao-e-gerenciamento` referencia `disciplina`, `assunto` e `questao_origem_id`; `prd-editais` reusa `disciplina` no vínculo N:N; `prd-gestao-de-usuarios` reaproveita `auditoria`, `PaginaResposta<T>` e o `confirmacao-dialog` criados aqui.
