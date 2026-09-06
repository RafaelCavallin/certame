# Especificação técnica

> **Funcionalidade:** Simulados (visão do estudante e gestão administrativa) · **Fase:** F4 — Simulados
> PRD: `tasks/prd-simulados/prd.md` (RF-14, RF-21, RN-01, RN-04, CA-01 a CA-12).
> Depende de `prd-sessao-de-estudo` (Modo Prova) e de `prd-banco-de-questoes` (acervo publicado).

## Resumo

O simulado é uma **prova congelada**: ao publicar, o servidor sorteia as questões conforme a composição por disciplina, grava a série em `simulado_questao` e nunca mais a altera. Todos os estudantes recebem exatamente a mesma prova, na mesma ordem — é isso que torna a nota comparável e a aplicação auditável. A publicação é uma porta com fechadura: só passa quem tem janela definida, composição fechando com o número de questões declarado e acervo publicado suficiente em cada disciplina (RN-01, CA-04, CA-10).

A aplicação **não ganha interface nem tabela própria**. Uma tentativa abre uma `sessao_estudo` com `modo = PROVA`, `origem = SIMULADO` e `origem_ref = simuladoId`; o cronômetro, a série, o lote de respostas na finalização e a correção questão a questão são exatamente os de `prd-sessao-de-estudo` (RF-09, RF-10, RN-04). O domínio novo, `br.com.certame.simulado`, acrescenta apenas o que é do simulado: o cadastro, a composição, a janela, a série congelada e a `tentativa_simulado`, que guarda nota, tempo e o tipo da tentativa. A primeira tentativa finalizada é `VALIDA` e imutável; qualquer refeitura nasce `TREINO` e não toca na nota registrada (CA-06). Tentativa abandonada expira junto com a sessão e some do histórico (CA-07).

No frontend, uma feature `features/simulados/` (lista em três grupos e tela de resultado) e uma `features/admin/simulados/` (lista e formulário com composição), ambas traduzindo 1:1 os artboards `simulados`, `admin-simulados` e `admin-simulado-form` do protótipo Nocturne. A execução da prova reaproveita as telas de `features/study` sem componente paralelo.

## Arquitetura do sistema

### Visão dos componentes

```
Angular 22                                     Spring Boot 4.1
─────────────────────────────────────────      ──────────────────────────────────────────────
features/simulados/                            simulado/
  simulados-page      lista (3 grupos)  ──►      SimuladoController        /api/v1/simulados
  proximo-simulado-card destaque + prazo         SimuladoCatalogoService   agrupamento + estado
  simulado-card       item da lista              TentativaSimuladoService  iniciar/retomar/finalizar
  simulado-resultado-page  revisão      ──►      JanelaSimulado            regra pura de estado
                                                 MontadorProvaSimulado     sorteio na publicação
features/admin/simulados/                        ValidadorPublicacaoSimulado
  simulados-page      tabela + filtros ──►      SimuladoAdminController   /api/v1/admin/simulados
  simulado-form-page  dados gerais               SimuladoAdminService      CRUD + composição
  composicao-disciplinas  disciplina × qtd       PublicacaoSimuladoService publicar/despublicar
                                                 GeradorCodigoSimulado     SM-###
core/api/simulado-api.ts    cliente HTTP
core/api/modelos-simulado.ts  contratos         (reuso) estudo/SessaoEstudoService, CorrecaoProvaService
                                                (reuso) questao/QuestaoRepository, catalogo/, comum/auditoria
```

**Componentes novos (backend)** — pacote `br.com.certame.simulado`:

| Componente | Papel |
| --- | --- |
| `Simulado` | Entidade: código, nome, descrição, banca, número de questões, duração, janela, status, `publicado_em`, `excluido_em`. |
| `SimuladoComposicao` | Entidade filha: `simulado_id`, `disciplina_id`, `quantidade`. Uma linha por disciplina. |
| `SimuladoQuestao` | Série congelada: `simulado_id`, `ordem`, `questao_id`. Escrita só na publicação. |
| `TentativaSimulado` | Tentativa do estudante: sessão vinculada, tipo (`VALIDA`/`TREINO`), status, nota, acertos, erros, não respondidas, tempo total. |
| `StatusSimulado`, `EstadoSimulado`, `TipoTentativa`, `StatusTentativa` | Enums persistidos como texto. |
| `SimuladoAdminService` | CRUD e substituição da composição (RF-21). `@PreAuthorize` de admin+. |
| `ValidadorPublicacaoSimulado` | Regra pura: janela, soma da composição e acervo suficiente (CA-10). |
| `MontadorProvaSimulado` | Sorteia as questões publicadas por disciplina e devolve a série ordenada (RN-01, CA-04). |
| `PublicacaoSimuladoService` | Orquestra validar → montar → congelar → publicar; despublicar; auditar. |
| `JanelaSimulado` | Regra pura, `Clock` injetado: deriva `AGENDADO`/`DISPONIVEL`/`ENCERRADO` de status + janela (CA-11). |
| `SimuladoCatalogoService` | Monta a lista do estudante em três grupos, com a nota da tentativa válida (RF-14). |
| `TentativaSimuladoService` | Inicia/retoma a tentativa (abrindo a sessão de Modo Prova), finaliza e devolve o resultado. |
| `ExpiradorTentativaSimulado` | `@Scheduled`: marca `ABANDONADA` a tentativa cuja sessão expirou (RN-04, CA-07). |
| `GeradorCodigoSimulado` | Código sequencial `SM-###`, no molde de `GeradorCodigoQuestao`. |

**Componentes reaproveitados (não reescrever):** `SessaoEstudoService.abrir/retomar/finalizar` e `CorrecaoProvaService` (`prd-sessao-de-estudo`); `QuestaoRepository` para o acervo publicado; `DisciplinaRepository` e `BancaRepository` (`catalogo/`); `RegistradorAuditoria`, `PaginaResposta`, `ErroApi` e `ManipuladorErrosGlobal` (`comum/`).

**Fluxo de dados — publicação:** `POST /admin/simulados/{id}/publicar` → `ValidadorPublicacaoSimulado` (janela + soma + acervo) → `MontadorProvaSimulado` sorteia `quantidade` questões publicadas de cada disciplina → grava `simulado_questao` (`ordem` 1..N) → `status = PUBLICADO`, `publicado_em = now()` → `RegistradorAuditoria.registrar("SIMULADO_PUBLICADO", ...)`.

**Fluxo de dados — aplicação:** `POST /simulados/{id}/tentativas` → `JanelaSimulado` exige `DISPONIVEL` → se há tentativa em andamento, retoma; senão `SessaoEstudoService` abre sessão `PROVA/SIMULADO` com a série congelada e nasce a `tentativa_simulado` `EM_ANDAMENTO` → o cliente responde em memória → `POST .../finalizar` delega a `SessaoEstudoService.finalizar`, recebe a `RespostaCorrecao`, decide `VALIDA` ou `TREINO` e grava a nota → devolve o resultado com a revisão.

## Design de implementação

### Principais interfaces

```
SimuladoAdminService
  criar(RequisicaoSimulado)                        -> Simulado            // RF-21, CA-08
  atualizar(UUID id, RequisicaoSimulado)           -> Simulado            // RF-21
  substituirComposicao(UUID id, List<ItemComposicao>) -> ResumoComposicao // RF-21, CA-09
  excluir(UUID id)                                 -> void
  listar(FiltroSimulado, Paginacao)                -> Page<SimuladoResumoAdmin>

ValidadorPublicacaoSimulado                        // regra pura
  validar(Simulado, List<SimuladoComposicao>, Map<UUID,Long> acervo) -> Map<String,String>  // CA-10

MontadorProvaSimulado
  montar(UUID simuladoId, List<SimuladoComposicao>) -> List<UUID>          // RN-01, CA-04

PublicacaoSimuladoService
  publicar(UUID id)                                -> Simulado            // CA-10, CA-11
  despublicar(UUID id)                             -> Simulado

JanelaSimulado                                     // regra pura, Clock injetado
  estado(StatusSimulado, Instant abertura, Instant encerramento) -> EstadoSimulado  // CA-11
  aceitaNovaTentativa(EstadoSimulado)              -> boolean

SimuladoCatalogoService
  listarDoEstudante(UUID usuarioId)                -> RespostaSimulados    // RF-14, CA-01, CA-02

TentativaSimuladoService
  iniciar(UUID usuarioId, UUID simuladoId)         -> RespostaTentativa    // CA-03, CA-06, CA-11
  finalizar(UUID usuarioId, UUID tentativaId, RequisicaoFinalizacao) -> RespostaResultado  // CA-05
  resultado(UUID usuarioId, UUID tentativaId)      -> RespostaResultado    // CA-05, CA-06
```

### Modelos de dados

Campo ausente na origem é normalizado para `null`, como em `QuestaoDetalhe`. Instantes em ISO-8601 UTC; **duração e tempo total em segundos**; nota em **percentual inteiro 0–100**, na mesma convenção de `RespostaCorrecao.pctAcerto`.

#### `RespostaSimulados` — a lista do estudante (RF-14)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `proximo` | `SimuladoResumo` | não | Simulado agendado mais próximo; `null` quando não há nenhum. Alimenta o card de destaque. |
| `disponiveis` | `SimuladoResumo[]` | sim | Publicados com janela aberta, ordenados pelo encerramento mais próximo. |
| `agendados` | `SimuladoResumo[]` | sim | Publicados com abertura futura, ordenados pela abertura. |
| `concluidos` | `SimuladoConcluido[]` | sim | Com tentativa `VALIDA` finalizada, ordenados pela conclusão mais recente. |

#### `SimuladoResumo` — item das listas de disponíveis e agendados

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `uuid` | sim | — |
| `codigo` | `string` | sim | `SM-###`. |
| `nome` | `string` | sim | — |
| `banca` | `string` | não | Nome já resolvido; `null` quando o simulado não tem banca. |
| `numeroQuestoes` | `number` | sim | Tamanho da prova (CA-01). |
| `duracaoSegundos` | `number` | sim | Duração declarada (CA-01, CA-03). |
| `estado` | `AGENDADO \| DISPONIVEL` | sim | Derivado da janela por `JanelaSimulado` (CA-11). |
| `janelaAbertura`, `janelaEncerramento` | `string` | sim | ISO-8601 UTC. |
| `tentativaEmAndamentoId` | `uuid` | não | Preenchido quando existe tentativa aberta a retomar; `null` caso contrário. |

```text
{
  "id": "6b1f0a2c-9d33-4a71-8f52-0c7e4b8d1a20",
  "codigo": "SM-013",
  "nome": "Simulado Português intensivo",
  "banca": "Cebraspe",
  "numeroQuestoes": 30,
  "duracaoSegundos": 3600,
  "estado": "DISPONIVEL",
  "janelaAbertura": "2026-09-01T03:00:00Z",
  "janelaEncerramento": "2026-09-08T02:59:59Z",
  "tentativaEmAndamentoId": null
}
```

#### `SimuladoConcluido` — item do histórico

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id`, `codigo`, `nome`, `banca`, `numeroQuestoes`, `duracaoSegundos` | — | sim/não | Iguais a `SimuladoResumo`. |
| `tentativaId` | `uuid` | sim | Tentativa `VALIDA`, alvo da revisão. |
| `nota` | `number` | sim | Percentual inteiro 0–100 (CA-02). |
| `acertos`, `erros`, `naoRespondidas` | `number` | sim | Somam `numeroQuestoes`. |
| `tempoTotalSegundos` | `number` | sim | Duração efetiva da tentativa. |
| `concluidoEm` | `string` | sim | Data de conclusão (CA-02). |
| `podeRefazer` | `boolean` | sim | `true` só enquanto a janela estiver aberta; refeitura sempre como treino (CA-06). |

```text
{
  "id": "2f8c4e10-7a5b-4c9d-b0e3-118a6f2d4c31",
  "codigo": "SM-010",
  "nome": "Simulado TRF — 1ª aplicação",
  "banca": "Cebraspe",
  "numeroQuestoes": 80,
  "duracaoSegundos": 14400,
  "tentativaId": "9c0d7e21-4b6a-4f83-a2d1-5e7b9c3f0a44",
  "nota": 71,
  "acertos": 57,
  "erros": 21,
  "naoRespondidas": 2,
  "tempoTotalSegundos": 13680,
  "concluidoEm": "2026-06-02T21:41:03Z",
  "podeRefazer": false
}
```

> **Lista vazia:** os três grupos vêm como `[]` e `proximo` como `null` — a tela mostra o estado vazio, nunca uma lista fantasma.

```text
{ "proximo": null, "disponiveis": [], "agendados": [], "concluidos": [] }
```

#### `RespostaTentativa` — o que volta ao iniciar ou retomar (CA-03)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `tentativaId` | `uuid` | sim | — |
| `simuladoId`, `codigo`, `nome` | — | sim | Identificação da prova. |
| `tipo` | `VALIDA \| TREINO` | sim | Prevista no início; confirmada na finalização (CA-06). |
| `duracaoSegundos` | `number` | sim | Tempo total permitido; o cronômetro do Modo Prova parte daqui. |
| `iniciadaEm` | `string` | sim | Início da tentativa — base do tempo restante. |
| `sessao` | `RespostaSessao` | sim | Contrato de `prd-sessao-de-estudo`: sessão em `modo = PROVA`, `origem = SIMULADO`, com a série completa em `questoes[]` e **sem gabarito** (RN-04). |

```text
{
  "tentativaId": "9c0d7e21-4b6a-4f83-a2d1-5e7b9c3f0a44",
  "simuladoId": "2f8c4e10-7a5b-4c9d-b0e3-118a6f2d4c31",
  "codigo": "SM-013",
  "nome": "Simulado Português intensivo",
  "tipo": "VALIDA",
  "duracaoSegundos": 3600,
  "iniciadaEm": "2026-09-02T13:00:00Z",
  "sessao": {
    "id": "8f1c2b7a-3d4e-4f10-9a1b-6c2e5d7f8a90",
    "modo": "PROVA",
    "origem": "SIMULADO",
    "origemRef": "2f8c4e10-7a5b-4c9d-b0e3-118a6f2d4c31",
    "total": 30,
    "questoes": []
  }
}
```

#### `RespostaResultado` — nota e revisão (CA-05, CA-06)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `tentativaId`, `simuladoId`, `codigo`, `nome` | — | sim | — |
| `tipo` | `VALIDA \| TREINO` | sim | `TREINO` não altera a nota registrada nem entra no histórico oficial. |
| `nota` | `number` | sim | Percentual inteiro sobre o total da prova, não sobre as respondidas. |
| `acertos`, `erros`, `naoRespondidas`, `total` | `number` | sim | `acertos + erros + naoRespondidas = total`. |
| `tempoTotalSegundos` | `number` | sim | Duração menos pausas (CA-05). |
| `finalizadaEm` | `string` | sim | — |
| `notaRegistrada` | `number` | não | Nota da tentativa `VALIDA` do usuário; em uma tentativa `TREINO`, é o número que **continua valendo**. `null` quando esta é a válida. |
| `correcao` | `RespostaCorrecao` | sim | Revisão questão a questão, contrato de `prd-sessao-de-estudo` (`itens[]` com gabarito, marcada, correta e comentário). |

```text
{
  "tentativaId": "9c0d7e21-4b6a-4f83-a2d1-5e7b9c3f0a44",
  "simuladoId": "2f8c4e10-7a5b-4c9d-b0e3-118a6f2d4c31",
  "codigo": "SM-013",
  "nome": "Simulado Português intensivo",
  "tipo": "TREINO",
  "nota": 80,
  "acertos": 24,
  "erros": 5,
  "naoRespondidas": 1,
  "total": 30,
  "tempoTotalSegundos": 3180,
  "finalizadaEm": "2026-09-04T15:12:44Z",
  "notaRegistrada": 71,
  "correcao": { "sessaoId": "8f1c2b7a-3d4e-4f10-9a1b-6c2e5d7f8a90", "itens": [] }
}
```

#### `RequisicaoSimulado` — corpo do cadastro e da edição (CA-08)

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nome` | `string` | sim | 3–160 caracteres. `"Informe o nome do simulado."` |
| `descricao` | `string` | não | Até 500 caracteres. |
| `bancaId` | `uuid` | não | Precisa existir em `banca`; 422 em `bancaId` quando não existir. |
| `numeroQuestoes` | `number` | sim | Inteiro entre **10 e 200**. `"O número de questões deve estar entre 10 e 200."` |
| `duracaoSegundos` | `number` | sim | Entre 600 (10 min) e 21600 (6 h). |
| `janelaAbertura` | `string` | não | ISO-8601. Opcional no rascunho, obrigatória para publicar (CA-10). |
| `janelaEncerramento` | `string` | não | Idem; quando ambas presentes, `encerramento > abertura`, senão 422 em `janelaEncerramento`. |

> **O status não entra no corpo.** Criação nasce sempre `RASCUNHO` (CA-08); a transição para `PUBLICADO` só existe pelo endpoint de publicação, que é onde a validação mora. O segmento "Rascunho / Agendado / Publicado" do protótipo é leitura de estado, não um campo editável.

```text
{
  "nome": "Simulado Nacional TRF — 2ª aplicação",
  "descricao": "Prova completa no formato da banca, com correção ao final.",
  "bancaId": "b1a2c3d4-0000-4000-8000-000000000001",
  "numeroQuestoes": 80,
  "duracaoSegundos": 14400,
  "janelaAbertura": "2026-07-20T03:00:00Z",
  "janelaEncerramento": "2026-07-23T02:59:59Z"
}
```

#### `RequisicaoComposicao` — composição por disciplina (CA-09)

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `itens` | `ItemComposicao[]` | sim | Substitui a composição inteira. Vazio limpa a composição. |

`ItemComposicao`: `{ "disciplinaId": uuid, "quantidade": number }`. `quantidade` ≥ 1; `disciplinaId` sem repetição no lote (422 em `itens` com `"Cada disciplina pode aparecer uma única vez."`) e precisa existir e não estar arquivada.

```text
{
  "itens": [
    { "disciplinaId": "1a2b3c4d-0000-4000-8000-000000000001", "quantidade": 30 },
    { "disciplinaId": "1a2b3c4d-0000-4000-8000-000000000002", "quantidade": 30 },
    { "disciplinaId": "1a2b3c4d-0000-4000-8000-000000000003", "quantidade": 20 }
  ]
}
```

#### `ResumoComposicao` — o que a tela mostra ao lado da composição (CA-09)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `itens` | `ItemComposicaoResumo[]` | sim | `{ disciplinaId, disciplina, quantidade, disponiveis }` — `disponiveis` é a contagem de questões **publicadas** da disciplina. |
| `somaComposicao` | `number` | sim | Soma das quantidades. |
| `numeroQuestoes` | `number` | sim | O declarado no simulado. |
| `fechada` | `boolean` | sim | `somaComposicao == numeroQuestoes`. É o sinal verde da tela. |
| `acervoSuficiente` | `boolean` | sim | `true` quando toda disciplina tem `disponiveis >= quantidade`. |

```text
{
  "itens": [
    { "disciplinaId": "1a2b3c4d-0000-4000-8000-000000000001",
      "disciplina": "Direito Constitucional", "quantidade": 30, "disponiveis": 1240 },
    { "disciplinaId": "1a2b3c4d-0000-4000-8000-000000000003",
      "disciplina": "Português", "quantidade": 20, "disponiveis": 12 }
  ],
  "somaComposicao": 50,
  "numeroQuestoes": 80,
  "fechada": false,
  "acervoSuficiente": false
}
```

#### `SimuladoResumoAdmin` — linha da tabela administrativa

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id`, `codigo`, `nome` | — | sim | — |
| `banca` | `string` | não | — |
| `numeroQuestoes`, `duracaoSegundos` | `number` | sim | — |
| `estado` | `RASCUNHO \| AGENDADO \| DISPONIVEL \| ENCERRADO` | sim | Derivado por `JanelaSimulado`; é o rótulo da coluna Status. |
| `janelaAbertura`, `janelaEncerramento` | `string` | não | `null` no rascunho sem janela. |
| `tentativasConcluidas` | `number` | sim | Contagem de tentativas `VALIDA` finalizadas — substitui a coluna "inscritos" do protótipo. |
| `composicaoFechada` | `boolean` | sim | Permite marcar na lista o simulado que ainda não pode ser publicado. |

```text
{
  "content": [
    { "id": "2f8c4e10-7a5b-4c9d-b0e3-118a6f2d4c31", "codigo": "SM-014",
      "nome": "Simulado Nacional TRF — 2ª aplicação", "banca": "Cebraspe",
      "numeroQuestoes": 80, "duracaoSegundos": 14400, "estado": "AGENDADO",
      "janelaAbertura": "2026-07-20T03:00:00Z", "janelaEncerramento": "2026-07-23T02:59:59Z",
      "tentativasConcluidas": 0, "composicaoFechada": true }
  ],
  "page": 0, "size": 20, "totalElements": 6
}
```

#### `ErroApi` — envelope de erro

Formato `application/problem+json` do `ManipuladorErrosGlobal` já existente; nenhum `try/catch` novo em controller.

| Código | HTTP | Significado |
| --- | --- | --- |
| `DADOS_INVALIDOS` | `422` | Bean Validation do corpo, com `fieldErrors` campo a campo. |
| `COMPOSICAO_INVALIDA` | `422` | Disciplina repetida, inexistente, arquivada ou quantidade < 1. |
| `PUBLICACAO_INVALIDA` | `422` | Publicação recusada; `fieldErrors` diz **o que** está inconsistente (CA-10). |
| `SIMULADO_NAO_ENCONTRADO` | `404` | Id inexistente ou excluído logicamente. |
| `SIMULADO_FORA_DA_JANELA` | `409` | Tentativa iniciada com o simulado agendado ou encerrado (CA-11). |
| `SIMULADO_NAO_PUBLICADO` | `409` | Tentativa em simulado `RASCUNHO`. |
| `SIMULADO_EM_USO` | `409` | Editar composição, número de questões ou excluir simulado que já tem tentativa finalizada. |
| `TENTATIVA_NAO_ENCONTRADA` | `404` | Tentativa inexistente ou de outro usuário — 404, nunca 403, para não revelar tentativa alheia. |
| `TENTATIVA_JA_FINALIZADA` | `409` | Segunda finalização da mesma tentativa. |
| `CODIGO_EM_USO` | `409` | Colisão de `SM-###` (concorrência na geração). |

```text
{
  "code": "PUBLICACAO_INVALIDA",
  "message": "Não foi possível publicar o simulado.",
  "fieldErrors": {
    "janelaAbertura": "Defina a janela de aplicação antes de publicar.",
    "composicao": "A composição soma 50 questões, mas o simulado declara 80.",
    "composicao.Português": "A disciplina exige 20 questões e há apenas 12 publicadas."
  }
}
```

#### Esquema de banco — `V9__simulado.sql`

Migração versionada, imutável e aditiva em `backend/src/main/resources/db/migration`. `V1`…`V8` estão aplicadas, sendo `V7__sessao_estudo.sql` da F3 e `V8__sessao_caderno_tamanho_livre.sql` — **`V9` é o próximo número livre desta funcionalidade**. A semente de `dev` fica em `V904__seed_simulados_dev.sql` porque `V903` já é usada pela F3.

```sql
create table simulado (
  id                  uuid primary key default gen_random_uuid(),
  codigo              text not null,
  nome                text not null,
  descricao           text,
  banca_id            uuid references banca(id),
  numero_questoes     smallint not null check (numero_questoes between 10 and 200),
  duracao_segundos    integer not null check (duracao_segundos between 600 and 21600),
  janela_abertura     timestamptz,
  janela_encerramento timestamptz,
  status              text not null default 'RASCUNHO'
                      check (status in ('RASCUNHO','PUBLICADO','ENCERRADO')),
  publicado_em        timestamptz,
  criado_por          uuid references usuario(id),
  criado_em           timestamptz not null default now(),
  atualizado_em       timestamptz not null default now(),
  excluido_em         timestamptz,
  constraint simulado_janela_coerente
    check (janela_abertura is null or janela_encerramento is null
           or janela_encerramento > janela_abertura),
  constraint simulado_publicado_com_janela
    check (status <> 'PUBLICADO' or (janela_abertura is not null and janela_encerramento is not null))
);
create unique index simulado_codigo_idx on simulado (codigo) where excluido_em is null;
create index simulado_janela_idx on simulado (status, janela_abertura) where excluido_em is null;

create table simulado_composicao (
  simulado_id   uuid not null references simulado(id) on delete cascade,
  disciplina_id uuid not null references disciplina(id),
  quantidade    smallint not null check (quantidade >= 1),
  primary key (simulado_id, disciplina_id)
);

create table simulado_questao (
  simulado_id uuid not null references simulado(id) on delete cascade,
  ordem       smallint not null check (ordem >= 1),
  questao_id  uuid not null references questao(id),
  primary key (simulado_id, ordem),
  unique (simulado_id, questao_id)
);

create table tentativa_simulado (
  id                    uuid primary key default gen_random_uuid(),
  simulado_id           uuid not null references simulado(id),
  usuario_id            uuid not null references usuario(id),
  sessao_id             uuid not null unique references sessao_estudo(id),
  tipo                  text not null default 'VALIDA' check (tipo in ('VALIDA','TREINO')),
  status                text not null default 'EM_ANDAMENTO'
                        check (status in ('EM_ANDAMENTO','FINALIZADA','ABANDONADA')),
  nota                  smallint check (nota between 0 and 100),
  acertos               smallint,
  erros                 smallint,
  nao_respondidas       smallint,
  tempo_total_segundos  integer check (tempo_total_segundos >= 0),
  iniciada_em           timestamptz not null default now(),
  finalizada_em         timestamptz,
  constraint tentativa_finalizada_com_nota
    check (status <> 'FINALIZADA' or (nota is not null and finalizada_em is not null))
);
create unique index tentativa_valida_unica_idx on tentativa_simulado (simulado_id, usuario_id)
  where tipo = 'VALIDA' and status = 'FINALIZADA';
create unique index tentativa_aberta_unica_idx on tentativa_simulado (simulado_id, usuario_id)
  where status = 'EM_ANDAMENTO';
create index tentativa_historico_idx on tentativa_simulado (usuario_id, finalizada_em desc)
  where status = 'FINALIZADA';

-- Integridade da nota: tentativa finalizada nao muda mais (restricao tecnica do PRD).
create or replace function tentativa_simulado_imutavel() returns trigger as $$
begin
  if old.status = 'FINALIZADA' then
    return old;
  end if;
  return new;
end;
$$ language plpgsql;
create trigger tentativa_simulado_congelada before update on tentativa_simulado
  for each row execute function tentativa_simulado_imutavel();

-- Ajuste em sessao_estudo (V7): a prova do simulado nao cabe nos tamanhos 10/30/50.
alter table sessao_estudo drop constraint if exists sessao_estudo_tamanho_check;
alter table sessao_estudo add constraint sessao_tamanho_valido
  check ((origem = 'SIMULADO' and tamanho between 10 and 200) or tamanho in (10,30,50));
alter table sessao_estudo add constraint sessao_simulado_com_ref
  check (origem <> 'SIMULADO' or origem_ref is not null);
alter table sessao_questao drop constraint if exists sessao_questao_ordem_check;
alter table sessao_questao add constraint sessao_questao_ordem_valida check (ordem between 1 and 200);
```

> **Por que trigger e não `rule`, ao contrário de `resposta`:** a linha de `tentativa_simulado` nasce em branco no início e só recebe nota na finalização — um `do instead nothing` em todo `update` impediria a própria gravação da nota. O gatilho congela apenas o que já está `FINALIZADA`, devolvendo `old` em silêncio, na mesma filosofia de RN-02: a tentativa registrada é imutável e o teste confere o **estado** depois da tentativa de escrita.

> **`tentativa_aberta_unica_idx`:** garante no banco que o mesmo usuário não abre duas provas do mesmo simulado em duas abas. A segunda chamada de início retoma a existente em vez de criar outra.

> **Alterações em `sessao_estudo` e `sessao_questao`:** são de tabelas de `prd-sessao-de-estudo`. A TechSpec daquela funcionalidade e a `TECH-SPEC.md` §5 precisam ser atualizadas na mesma alteração (regra 4 do `AGENTS.md`). Se F3 ainda não tiver sido implementada quando F4 começar, o correto é já nascer com o `check` amplo em `V7`, e este bloco de `alter` cai.

### Endpoints da API

Base `/api/v1`. Os do estudante exigem apenas autenticação; os administrativos exigem `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` via `@PreAuthorize` no serviço (CA-12). A `TECH-SPEC.md` §6 previa a linha genérica `CRUD /admin/simulados`; **os dez endpoints abaixo entram no documento na mesma alteração**, como manda a regra 4 do `AGENTS.md`.

#### Visão geral

| Método | Rota | Perfil | Descrição |
| --- | --- | --- | --- |
| `GET` | `/simulados` | autenticado | Lista em três grupos + próximo agendado (RF-14). |
| `POST` | `/simulados/{id}/tentativas` | autenticado | Inicia ou retoma a tentativa e devolve a série em Modo Prova. |
| `POST` | `/simulados/{id}/tentativas/{tentativaId}/finalizar` | autenticado | Grava o lote, corrige e devolve a nota. |
| `GET` | `/simulados/{id}/tentativas/{tentativaId}` | autenticado | Revisão questão a questão de uma tentativa finalizada. |
| `GET` | `/admin/simulados` | admin+ | Lista paginada com filtros de status e banca. |
| `POST` | `/admin/simulados` | admin+ | Cria em `RASCUNHO`. |
| `GET` `PUT` | `/admin/simulados/{id}` | admin+ | Detalhe e edição dos dados gerais. |
| `DELETE` | `/admin/simulados/{id}` | admin+ | Exclusão lógica. |
| `PUT` | `/admin/simulados/{id}/composicao` | admin+ | Substitui a composição e devolve o resumo. |
| `POST` | `/admin/simulados/{id}/publicar` | admin+ | Valida, congela a série e publica. |
| `POST` | `/admin/simulados/{id}/despublicar` | admin+ | Volta a `RASCUNHO` e libera a série. |

---

#### `GET /api/v1/simulados`

Tudo o que a tela de simulados precisa, em uma chamada: três grupos e o card de destaque (CA-01, CA-02).

**Parâmetros de consulta** — nenhum. O agrupamento é do servidor; a tela não filtra por estado.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RespostaSimulados` | Sempre, mesmo sem simulado algum. |
| `401` | `ErroApi` | Sem token válido. |

**Exemplo — sucesso**

```http
GET /api/v1/simulados
```

Ver `RespostaSimulados` e `SimuladoConcluido` em Modelos de dados.

**Exemplo — nenhum simulado**

```http
GET /api/v1/simulados
```

```text
{ "proximo": null, "disponiveis": [], "agendados": [], "concluidos": [] }
```

> Simulado em `RASCUNHO` nunca aparece para o estudante. Encerrado só aparece se o usuário tiver tentativa `VALIDA` — aí entra em `concluidos`, com `podeRefazer: false`.

---

#### `POST /api/v1/simulados/{id}/tentativas`

Abre a prova. Sem corpo: a série já está congelada e a duração é a do simulado (CA-03).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `RespostaTentativa` | Tentativa criada; sessão aberta em `modo = PROVA`. |
| `200` | `RespostaTentativa` | Já havia tentativa `EM_ANDAMENTO`: retoma a mesma sessão, sem reiniciar o cronômetro. |
| `404` | `ErroApi` (`SIMULADO_NAO_ENCONTRADO`) | Id inexistente ou excluído. |
| `409` | `ErroApi` (`SIMULADO_FORA_DA_JANELA`) | Agendado ou encerrado (CA-11). |
| `409` | `ErroApi` (`SIMULADO_NAO_PUBLICADO`) | Ainda em rascunho. |

**Exemplo — sucesso**

```http
POST /api/v1/simulados/2f8c4e10-7a5b-4c9d-b0e3-118a6f2d4c31/tentativas
```

Ver `RespostaTentativa` em Modelos de dados.

**Exemplo — refeitura de simulado concluído (CA-06)**

```text
{ "tentativaId": "1d3f...", "tipo": "TREINO", "duracaoSegundos": 3600, "sessao": { } }
```

> `tipo: "TREINO"` é o que faz a tela avisar, antes de começar, que aquela execução não altera a nota registrada.

**Exemplo — fora da janela**

```http
POST /api/v1/simulados/2f8c4e10-7a5b-4c9d-b0e3-118a6f2d4c31/tentativas
```

```text
{
  "code": "SIMULADO_FORA_DA_JANELA",
  "message": "Este simulado está encerrado e não aceita novas tentativas.",
  "fieldErrors": null
}
```

---

#### `POST /api/v1/simulados/{id}/tentativas/{tentativaId}/finalizar`

Finalização explícita do Modo Prova: o lote de respostas chega inteiro e só agora é gravado (RN-04).

**Corpo** — `RequisicaoFinalizacao` de `prd-sessao-de-estudo`.

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `respostas` | `RespostaProva[]` | — | Lote completo; questão omitida conta como não respondida (RF-10). Item fora da série responde 422. |
| `pausadoMs` | `number` | `0` | Acumulado das pausas do cronômetro. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RespostaResultado` | Correção pronta (CA-05). |
| `404` | `ErroApi` (`TENTATIVA_NAO_ENCONTRADA`) | Tentativa de outro usuário ou inexistente. |
| `409` | `ErroApi` (`TENTATIVA_JA_FINALIZADA`) | Reenvio do lote. |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Questão fora da série ou alternativa inválida para o tipo. |

**Exemplo — sucesso**

```http
POST /api/v1/simulados/2f8c.../tentativas/9c0d.../finalizar
```

```text
{
  "respostas": [
    { "questaoId": "c4a1f0de-55b2-4a77-8f1e-2b9d3c6e7a10", "marcada": "B", "tempoMs": 41200 }
  ],
  "pausadoMs": 0
}
```

Resposta: ver `RespostaResultado` em Modelos de dados.

> **A janela encerrar durante a prova não invalida a finalização** (Caso de borda 3): o que vale é a duração já iniciada. O que a janela fechada impede é apenas `POST /tentativas` — entrada nova.

---

#### `GET /api/v1/simulados/{id}/tentativas/{tentativaId}`

Revisão de uma tentativa finalizada, com gabarito e comentário (CA-05, CA-06).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RespostaResultado` | Tentativa `FINALIZADA` do próprio usuário. |
| `404` | `ErroApi` (`TENTATIVA_NAO_ENCONTRADA`) | Inexistente, de outro usuário, ou ainda em andamento. |

> Tentativa `ABANDONADA` responde 404: ela não existe para o histórico (CA-07).

---

#### `GET /api/v1/admin/simulados`

**Parâmetros de consulta**

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `q` | `string` | — | Busca por código ou nome, sem acento e sem caixa. |
| `estado` | `string` | — | `RASCUNHO`, `AGENDADO`, `DISPONIVEL` ou `ENCERRADO`; valor fora da lista responde 422. |
| `bancaId` | `uuid` | — | Filtra por banca. |
| `page`, `size` | `number` | `0`, `20` | `size` máximo 100. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `PaginaResposta<SimuladoResumoAdmin>` | Sempre. |
| `403` | `ErroApi` | Perfil abaixo de admin (CA-12). |

> `estado` é derivado, não coluna: o filtro vira predicado de `status` + janela contra `now()` na consulta.

---

#### `POST /api/v1/admin/simulados`

Cria o simulado em `RASCUNHO` com código gerado (CA-08).

**Corpo** — `RequisicaoSimulado`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `SimuladoDetalhe` (dados gerais + `ResumoComposicao` vazio) | Criado; `Location: /api/v1/admin/simulados/{id}`. |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Bean Validation. |
| `403` | `ErroApi` | Perfil insuficiente (CA-12). |

---

#### `PUT /api/v1/admin/simulados/{id}/composicao`

Substitui a composição inteira e devolve a soma comparada ao número de questões (CA-09).

**Corpo** — `RequisicaoComposicao`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `ResumoComposicao` | Composição gravada, fechada ou não. |
| `409` | `ErroApi` (`SIMULADO_EM_USO`) | Simulado com tentativa finalizada: a prova aplicada não muda. |
| `422` | `ErroApi` (`COMPOSICAO_INVALIDA`) | Disciplina repetida, inexistente, arquivada ou quantidade < 1. |

> **Soma diferente não é erro aqui.** Gravar uma composição pela metade é parte do trabalho; o bloqueio acontece na publicação (CA-10). A tela mostra `somaComposicao / numeroQuestoes` e desabilita o botão Publicar enquanto `fechada` for `false`.

---

#### `POST /api/v1/admin/simulados/{id}/publicar`

Valida, sorteia a prova, congela a série e publica (CA-04, CA-10, CA-11).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `SimuladoDetalhe` | Publicado; `estado` já reflete `AGENDADO` ou `DISPONIVEL`. |
| `422` | `ErroApi` (`PUBLICACAO_INVALIDA`) | Janela ausente, composição não fechada ou acervo insuficiente. |
| `409` | `ErroApi` (`SIMULADO_EM_USO`) | Republicar simulado já publicado. |

**Exemplo — recusa (CA-10)**

```http
POST /api/v1/admin/simulados/2f8c4e10-7a5b-4c9d-b0e3-118a6f2d4c31/publicar
```

Ver o exemplo de `PUBLICACAO_INVALIDA` em Modelos de dados: cada pendência vem endereçada ao campo da tela, e a de acervo nomeia a disciplina que faltou.

> A série é congelada **dentro da mesma transação** da mudança de status. Ou o simulado sai publicado com 80 questões gravadas, ou nada é escrito.

---

#### `POST /api/v1/admin/simulados/{id}/despublicar`

Volta o simulado a `RASCUNHO` e apaga a série congelada, liberando nova composição.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `SimuladoDetalhe` | Despublicado e auditado. |
| `409` | `ErroApi` (`SIMULADO_EM_USO`) | Já existe tentativa finalizada — a prova aplicada não é apagada. |

---

#### `DELETE /api/v1/admin/simulados/{id}`

Exclusão lógica (`excluido_em`). Responde `204`; `409 SIMULADO_EM_USO` quando há tentativa finalizada.

---

## Pontos de integração

Nenhum serviço externo. As integrações são todas internas e todas de mão única — o simulado consome, não é consumido:

| Integração | Contrato | Falha e mitigação |
| --- | --- | --- |
| `estudo/SessaoEstudoService` | `abrir` com `origem = SIMULADO`, `origemRef`, série pronta; `finalizar` devolve `RespostaCorrecao`. Exige a sobrecarga que aceita **série explícita** em vez de montá-la por filtro — é a única alteração pedida a F3. | Se F3 mudar o contrato, o simulado quebra na compilação, não em produção. `TentativaSimuladoService` não fala com `resposta` nem com `sessao_questao` diretamente. |
| `questao/QuestaoRepository` | Contagem e sorteio de questões `PUBLICADA` por disciplina (RN-01). | Acervo insuficiente é erro de publicação, com a disciplina nomeada — nunca uma prova curta. |
| `catalogo/` | `DisciplinaRepository` para a composição, `BancaRepository` para o formulário. | Disciplina arquivada é recusada na composição. |
| `comum/auditoria/RegistradorAuditoria` | `SIMULADO_CRIADO`, `SIMULADO_ATUALIZADO`, `SIMULADO_COMPOSICAO_ALTERADA`, `SIMULADO_PUBLICADO`, `SIMULADO_DESPUBLICADO`, `SIMULADO_EXCLUIDO`, com o payload do antes/depois da janela. | Auditoria participa da transação do serviço; falha ao serializar derruba a operação, e é o comportamento desejado para ação administrativa. |
| Frontend `features/study` | Reuso das telas de Modo Prova via rota `/estudar?sessao={id}`. | Nenhuma tela nova de resolução (restrição do PRD). |

## Abordagem de testes

Cobertura mínima de 80% por módulo (`.agents/rules/tests.md`). A correção de simulado está na faixa **alta** de criticidade daquele documento, junto com autorização por perfil — recebe caminho feliz e todas as bordas. CRUD administrativo recebe o essencial.

### Testes de unidade

Objetos puros, sem contexto Spring, dependências mockadas à mão. Alvos: `ValidadorPublicacaoSimulado`, `MontadorProvaSimulado`, `JanelaSimulado` (com `Clock` fixo), `SimuladoCatalogoService` e `TentativaSimuladoService` (com repositórios e `SessaoEstudoService` mockados).

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `TU01_agrupaSimuladosEmDisponiveisAgendadosEConcluidos` | CA-01 | Os três grupos saem preenchidos e disjuntos, com nome, banca, número de questões e duração em cada item. |
| TU-02 | `TU02_exibeNotaEDataNaListaDeConcluidos` | CA-02 | O item concluído traz `nota` e `concluidoEm` da tentativa `VALIDA`, não da tentativa de treino mais recente. |
| TU-03 | `TU03_montaSerieApenasComQuestoesPublicadas` | CA-04, RN-01 | Dado acervo com rascunhos e em revisão, a série só contém ids `PUBLICADA`. |
| TU-04 | `TU04_respeitaQuantidadePorDisciplinaNaComposicao` | CA-04 | Composição 30/30/20 gera 80 ids, com a contagem exata por disciplina. |
| TU-05 | `TU05_recusaPublicacaoSemJanela` | CA-10 | `PublicacaoInvalidaException` com `fieldErrors.janelaAbertura` preenchido. |
| TU-06 | `TU06_recusaPublicacaoComComposicaoQueNaoFecha` | CA-10 | Soma 50 contra 80 declaradas: pendência em `composicao` citando os dois números. |
| TU-07 | `TU07_recusaPublicacaoComAcervoInsuficiente` | CA-10, CA-04 | Disciplina que pede 20 e tem 12 publicadas: pendência nomeando a disciplina. |
| TU-08 | `TU08_derivaAgendadoDisponivelEEncerradoDaJanela` | CA-11 | Com `Clock` antes, dentro e depois da janela, `JanelaSimulado` devolve os três estados. |
| TU-09 | `TU09_recusaInicioForaDaJanela` | CA-11 | `SIMULADO_FORA_DA_JANELA` no agendado e no encerrado; início permitido só no disponível. |
| TU-10 | `TU10_abreSessaoEmModoProvaComDuracaoDeclarada` | CA-03 | `SessaoEstudoService.abrir` é chamado com `modo = PROVA`, `origem = SIMULADO` e a série congelada; a resposta ecoa a duração do simulado. |
| TU-11 | `TU11_marcaSegundaTentativaComoTreino` | CA-06 | Havendo tentativa `VALIDA` finalizada, a nova nasce `TREINO` e o resultado traz `notaRegistrada` com a nota antiga. |
| TU-12 | `TU12_calculaNotaSobreOTotalDaProva` | CA-05 | 24 acertos em 30 com 1 não respondida: nota 80, e `acertos + erros + naoRespondidas = total`. |
| TU-13 | `TU13_retomaTentativaEmAndamentoSemReiniciarCronometro` | CA-03 | Segundo início devolve a mesma `tentativaId` e o mesmo `iniciadaEm`. |
| TU-14 | `TU14_recusaComposicaoComDisciplinaRepetida` | CA-09 | `COMPOSICAO_INVALIDA` antes de qualquer escrita. |
| TU-15 | `TU15_resumoDaComposicaoComparaSomaComNumeroDeQuestoes` | CA-09 | `somaComposicao`, `numeroQuestoes` e `fechada` coerentes; `disponiveis` por disciplina preenchido. |
| TU-16 | `TU16_criaSimuladoComStatusRascunho` | CA-08 | Simulado nasce `RASCUNHO`, com código `SM-###` gerado e `publicado_em` nulo. |

### Testes de integração

`@SpringBootTest @AutoConfigureMockMvc @Testcontainers` com `PostgreSQLContainer<>("postgres:18")` e datasource por `@DynamicPropertySource`. Dados isolados por execução (e-mail e código com `UUID.randomUUID()`). Massa mínima: um admin, um estudante, três disciplinas e acervo publicado suficiente em duas delas.

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | `TI01_publicaSimuladoECongelaSerie` | CA-04, CA-10 | `simulado_questao` fica com N linhas, `ordem` 1..N sem buraco, todas as questões `PUBLICADA`. |
| TI-02 | `TI02_naoEscreveNadaQuandoPublicacaoERecusada` | CA-10 | Após 422, `status` continua `RASCUNHO` e `simulado_questao` continua vazia. |
| TI-03 | `TI03_doisEstudantesRecebemAMesmaProva` | CA-04 | As duas séries têm os mesmos ids na mesma ordem. |
| TI-04 | `TI04_aplicaSimuladoDoInicioAoResultado` | CA-03, CA-05 | Iniciar → finalizar com lote parcial → nota, acertos, erros, não respondidas, tempo total e `correcao.itens` completos. |
| TI-05 | `TI05_naoRegistraNotaDeTentativaAbandonada` | CA-07, RN-04 | Tentativa expirada vira `ABANDONADA`; `GET /simulados` mantém o simulado fora de `concluidos` e sem nota. |
| TI-06 | `TI06_refeituraNaoAlteraNotaRegistrada` | CA-06 | Após a segunda finalização, a linha `VALIDA` continua com a nota original e o índice único impede uma segunda válida. |
| TI-07 | `TI07_congelaTentativaFinalizada` | CA-06 | `update` direto na tentativa `FINALIZADA` não muda a nota (o gatilho descarta em silêncio). |
| TI-08 | `TI08_recusaOperacoesAdministrativasParaEstudante` | CA-12 | `POST`, `PUT` e `POST /publicar` com token de estudante respondem 403; nada é gravado. |
| TI-09 | `TI09_derivaEstadoDaJanelaNaListaDoEstudante` | CA-11 | Com três simulados publicados em janelas diferentes, cada um cai no grupo certo. |
| TI-10 | `TI10_encerramentoDaJanelaBloqueiaNovaEntradaMasNaoAFinalizacao` | CA-11, borda 3 | Tentativa iniciada dentro da janela finaliza normalmente depois do encerramento; novo `POST /tentativas` responde 409. |
| TI-11 | `TI11_impedeAlteracaoDeComposicaoComTentativaFinalizada` | CA-06 | `PUT /composicao` responde 409 `SIMULADO_EM_USO`. |
| TI-12 | `TI12_registraAuditoriaDePublicacaoEDeJanela` | Restrição de auditoria do PRD | Linhas em `auditoria` com ação, entidade, ator e payload do antes/depois. |

### Testes E2E

Playwright, na ferramenta de navegador do ambiente, contra front e back reais (`docker-compose.e2e.yml`). Ponto de partida: base semeada por `V904__seed_simulados_dev.sql`.

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | `E2E01_estudanteVeOsTresGruposDeSimulados` | CA-01, CA-02 | A tela mostra disponíveis, agendados e concluídos, cada estado com **rótulo textual** além da cor, e o concluído com nota e data. |
| E2E-02 | `E2E02_estudanteFazSimuladoDoInicioAoResultado` | CA-03, CA-05 | Iniciar → Modo Prova sem gabarito, com cronômetro visível → finalizar → nota, contagens, tempo e revisão questão a questão. |
| E2E-03 | `E2E03_estudanteRevisaERefazComoTreino` | CA-06 | Simulado concluído permite revisão; a refeitura avisa que é treino e a nota do histórico permanece a mesma ao voltar à lista. |
| E2E-04 | `E2E04_administradorMontaEPublicaSimulado` | CA-08, CA-09, CA-10 | Cadastro → composição com soma exibida → publicar bloqueado enquanto a soma diverge → publicado após fechar a composição. |
| E2E-05 | `E2E05_publicacaoRecusadaMostraOQueEstaInconsistente` | CA-10 | Sem janela, a recusa aponta o campo da janela; com acervo insuficiente, aponta a disciplina. |
| E2E-06 | `E2E06_navegacaoPorTecladoEContrasteNaListaESimuladoForm` | UX do PRD | Foco visível em toda a lista e no formulário, ordem de tabulação coerente, contraste ≥ 4,5:1 e datas/notas em formato brasileiro. |

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Migração `V9__simulado.sql` + entidades e repositórios.** Sem esquema não há domínio; e o ajuste em `sessao_estudo` precisa estar de pé antes de qualquer tentativa. Hibernate em `validate` cobra a coerência já na subida.
2. **Regras puras: `JanelaSimulado`, `ValidadorPublicacaoSimulado`, `MontadorProvaSimulado`** com TU-03 a TU-09. São o coração da funcionalidade e não dependem de nada — testadas em milissegundos.
3. **`SimuladoAdminService` e `PublicacaoSimuladoService`** (CRUD, composição, publicar/despublicar, auditoria) com TU-14 a TU-16 e TI-01, TI-02, TI-11, TI-12. O simulado publicável é pré-requisito de tudo do estudante.
4. **`SimuladoAdminController`** com `@PreAuthorize` e TI-08 — autorização antes da interface, como manda o `AGENTS.md`.
5. **`SimuladoCatalogoService`, `TentativaSimuladoService`, `ExpiradorTentativaSimulado`** e `SimuladoController`, com TU-01, TU-02, TU-10 a TU-13 e TI-03 a TI-07, TI-09, TI-10. Depende de F3 (`SessaoEstudoService`) estar disponível.
6. **Frontend administrativo:** `simulado-api.ts`, `modelos-simulado.ts`, lista e formulário com `composicao-disciplinas` — a tela que gera dado para todas as outras.
7. **Frontend do estudante:** lista em três grupos, card de destaque e tela de resultado, ligando o botão Iniciar ao Modo Prova de F3.
8. **E2E e ajuste fino de acessibilidade** (E2E-01 a E2E-06), com a base semeada por `V903`.

### Dependências técnicas

- **`prd-sessao-de-estudo` (F3) implementada** — bloqueador rígido dos passos 5 e 7. É de lá que vêm `sessao_estudo`, `sessao_questao`, `resposta`, o Modo Prova e a correção. F4 pode avançar até o passo 4 sem F3.
- **Sobrecarga de `SessaoEstudoService.abrir` que aceita série explícita** — alteração pedida a F3, junto com o `check` amplo de `tamanho` e `ordem`; precisa entrar na TechSpec de F3 e na `TECH-SPEC.md` §5/§6 na mesma alteração.
- **Acervo publicado por disciplina** (`prd-banco-de-questoes`, já implementado) — sem ele nenhum simulado publica; o seed de `dev` precisa cobrir as disciplinas do simulado semeado.
- **Docker** para os `*IntegracaoTest` (Testcontainers) e para o E2E.
- Nenhuma dependência nova no `pom.xml` nem no `package.json`.

## Monitoramento e observabilidade

Usa a infraestrutura já existente — `RegistradorAuditoria`, log do Spring e o padrão de `ObservabilidadeIntegracaoTest` do domínio de questões. Nada de ferramenta nova.

| Evento | Nível | Conteúdo |
| --- | --- | --- |
| `simulado.publicado` | `INFO` | Id, código, número de questões, disciplinas da composição e duração do sorteio em ms. |
| `simulado.publicacao_recusada` | `WARN` | Id e as chaves das pendências — é o número que diz se a tela de composição está confusa. |
| `simulado.acervo_insuficiente` | `WARN` | Disciplina, quantidade pedida e disponível: alimenta a decisão editorial de produzir questão. |
| `simulado.tentativa_iniciada` / `simulado.tentativa_finalizada` | `INFO` | Id do simulado, tipo da tentativa e tempo total; sem dado pessoal além do id do usuário. |
| `simulado.tentativa_abandonada` | `INFO` | Contagem do job de expiração por execução — abandono alto é sinal de prova longa demais. |
| `simulado.montagem_lenta` | `WARN` | Emitido quando o sorteio ultrapassa 2 s, o limiar que denuncia acervo grande sem índice. |

Auditoria (tabela `auditoria`) cobre as ações administrativas exigidas pelo PRD: criação, atualização, alteração de composição, publicação, despublicação, alteração de janela e exclusão. As métricas de produto do PRD (percentual de estudantes que concluem um simulado por mês) saem de consulta sobre `tentativa_simulado`, não de instrumentação nova.

## Considerações técnicas

### Principais decisões

**Série congelada na publicação, não sorteada por tentativa.** O PRD promete "as mesmas condições" para todos e nota comparável; sorteio por tentativa dá a cada estudante uma prova diferente, o que inviabiliza comparar notas e auditar o que foi aplicado. O custo é que despublicar e republicar refaz o sorteio — aceitável, porque só é permitido enquanto ninguém tiver concluído. Descartada a seleção manual de questão a questão que o protótipo sugere: contraria a meta de "operação sem planilha" do PRD.

**Reuso de `sessao_estudo` em vez de tabelas próprias.** O PRD proíbe interface paralela ao Modo Prova, e a coerência precisa começar no dado: cronômetro, imutabilidade de `resposta` (RN-02), descarte no abandono (RN-04) e correção já existem em F3. `tentativa_simulado` guarda só o que é do simulado — nota, tipo e vínculo. O trade-off é o acoplamento a F3, mitigado por conversar apenas pelo serviço, nunca pelo repositório do outro domínio (`code-standards.md` §3).

**`VALIDA` decidida na finalização, não no início.** Se o tipo fosse fixado ao iniciar, uma primeira tentativa abandonada consumiria a vaga da nota oficial e o estudante perderia o simulado sem ter respondido nada. Decidir no fim faz o índice `tentativa_valida_unica_idx` ser a única fonte de verdade sobre "já concluí este simulado".

**Estado do simulado derivado, não armazenado.** `AGENDADO`/`DISPONIVEL`/`ENCERRADO` são função de `status` + janela + `now()`. Guardar o estado em coluna exigiria um job para virar simulado agendado em disponível na hora exata — mais peça móvel para o mesmo resultado. `status` no banco guarda só o que é decisão humana: `RASCUNHO`, `PUBLICADO`, `ENCERRADO` (encerramento manual antecipado).

**Duração em segundos e nota em percentual inteiro.** Alinha com `RespostaCorrecao.pctAcerto` e com a convenção de F3 (segundos para exibição, milissegundos para medida). O protótipo mostra "4h" e "40min" — formatação é do frontend, não do contrato.

**Gatilho em vez de `rule` para congelar a tentativa.** Detalhado no blockquote do esquema: a linha precisa ser escrita uma vez (a nota) e nunca mais.

### Riscos conhecidos

| Risco | Mitigação |
| --- | --- |
| **F3 atrasar ou mudar o contrato do Modo Prova.** É o bloqueador mais duro desta funcionalidade. | Os passos 1 a 4 (esquema, regras puras, CRUD, publicação) não dependem de F3 e podem ser entregues antes. O acoplamento é por serviço, então mudança de contrato quebra na compilação. |
| **Acervo publicado insuficiente para fechar composições reais.** Um simulado de 80 questões precisa de 80 questões publicadas distribuídas nas disciplinas certas. | `disponiveis` por disciplina já aparece na tela de composição, antes de tentar publicar; o log `simulado.acervo_insuficiente` vira insumo editorial. |
| **Alterar `sessao_estudo` a partir de outro domínio.** Mexer no `check` de `tamanho` e `ordem` é alteração em tabela de F3. | Registrado como dependência explícita; se F3 vier depois, nasce com o `check` amplo e o bloco de `alter` some. Atualizar a TechSpec de F3 e a `TECH-SPEC.md` na mesma alteração é obrigação da regra 4 do `AGENTS.md`. |
| **Sorteio de 200 questões em acervo grande.** `order by random()` varre a tabela. | Sorteio acontece uma vez por publicação, não por tentativa, e roda sobre o índice `questao_filtro_idx (status, disciplina_id, assunto_id)`. O log `simulado.montagem_lenta` avisa se a premissa deixar de valer. |
| **Duas abas iniciando a mesma prova.** | `tentativa_aberta_unica_idx` no banco e retomada idempotente no serviço: a segunda chamada devolve `200` com a tentativa existente. |
| **Fuso horário da janela.** Instantes são UTC no banco e no contrato; o estudante lê em horário de Brasília. | Conversão só na exibição, com `DatePipe` e locale `pt-BR` — a mesma convenção do restante do projeto. |

### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/folder-structure.md` e `.agents/rules/tests.md` foram lidos na íntegra. O que eles impõem a esta especificação:

- **Stack** Angular 22 standalone/zoneless/signals, Spring Boot 4.1 / Java 25, PostgreSQL 18 + Flyway. Nenhuma dependência nova.
- **Regra 1 — tradução 1:1 do protótipo.** As telas replicam os artboards `simulados`, `admin-simulados` e `admin-simulado-form`, com os tokens `var(--*)` de `styles.css`. Os desvios em relação ao protótipo são os aprovados com o usuário e estão listados abaixo.
- **Regra 2 — autorização no servidor.** Todo endpoint administrativo tem `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` no serviço, coberto por TI-08 (CA-12).
- **Regra 3 — idioma.** Identificadores, tabelas e colunas em português sem acento e `snake_case`; mensagens ao usuário em pt-BR com acento, frase completa e ponto final; `code` do erro em `SNAKE_CASE` maiúsculo e estável.
- **Regra 4 — contrato da API.** Os dez endpoints entram na `TECH-SPEC.md` §6 na mesma alteração; a §5 recebe as quatro tabelas novas e o ajuste em `sessao_estudo`.
- **Regra 5 — Flyway.** `V9__simulado.sql` aditiva e imutável; seed de `dev` em `V904__seed_simulados_dev.sql`.
- **Regra 6 — RN-04.** Respostas só contam após a finalização; o abandono descarta a tentativa. Nada é gravado em `resposta` antes de `POST /finalizar`.
- **Código.** Arquivo até 100 linhas, método até 30, até 3 parâmetros (daí `FiltroSimulado`, `ItemComposicao` e `RequisicaoSimulado`), até 3 níveis de condicional com cláusulas guarda, sem comentário decorativo, sem linha em branco dentro de método, sem literal mágico (`DURACAO_MINIMA_SEGUNDOS`, `QUESTOES_MAXIMO`, `LIMIAR_MONTAGEM_LENTA`). Controller sem regra e sem `@Transactional`; serviço com a regra e a autorização; DTO sempre `record` com mensagem em português em cada constraint; entidade JPA sem Lombok, com construtor protegido e setters só no que muda; exclusão lógica com `excluido_em`.
- **Testes.** Nome do método começa pelo id do caso (`TU03_...`, `TI01_...`); AssertJ, nunca `assertEquals`; `*Test` sem contexto Spring, `*IntegracaoTest` com Testcontainers; cobertura mínima de 80%, com esforço concentrado na correção do simulado e na autorização, que `tests.md` classifica como críticas.
- **Estrutura de pastas.** Backend em `br.com.certame.simulado/` com `dto/`; frontend em `features/simulados/` e `features/admin/simulados/`, cliente em `core/api/`.

**Desvios do protótipo, aprovados com o usuário:**

1. O botão "Inscrever-se" e a coluna "inscritos" saem — inscrição não existe no PRD. A coluna passa a ser `tentativasConcluidas`.
2. "Disciplinas incluídas" (lista de checkbox) e "Questões selecionadas" (escolha manual) viram uma tabela de composição disciplina × quantidade, com soma comparada ao número de questões (CA-09) — é o que RF-21 pede.
3. O card "Próximo simulado" permanece, com contagem regressiva até a abertura, mas sem o botão de inscrição.
4. O segmento "Rascunho / Agendado / Publicado" do formulário vira indicador de estado somente leitura: a publicação é ação com validação, não um radio.

### Conformidade com skills

Skills de `.agents/skills/` aplicáveis a esta especificação:

- **`313-frameworks-spring-db-migrations-flyway`** — `V9__simulado.sql` versionada e imutável em `db/migration`, seed de `dev` isolado na série `V900+`, nenhuma alteração destrutiva, Hibernate em `validate`. O `alter table` em `sessao_estudo` usa `drop constraint if exists` para ser idempotente entre a ordem possível de F3 e F4.
- **`postgresql-table-design`** — PK `uuid` com `gen_random_uuid()`, `timestamptz` com `default now()`, enum como `text` + `check` espelhando o enum Java, índice único parcial para exclusão lógica (`simulado_codigo_idx`) e para as regras de tentativa, chave composta natural em `simulado_composicao` e `simulado_questao`, `on delete cascade` só nas filhas cujo ciclo de vida é o do pai.
- **`spring-boot-testing`** — test slices em vez de `@SpringBootTest` onde couber, `*IntegracaoTest` com Testcontainers e `@DynamicPropertySource`, AssertJ nas asserções, dados isolados por execução.
- **`angular-developer`** — standalone, zoneless, `signal`/`computed`, `input()`/`output()` como funções, `templateUrl`/`styleUrl` separados, cliente HTTP concentrado em `core/api`, membros de template como `protected readonly`.

Sem desvio a registrar em nenhuma delas.

### Arquivos relevantes e dependentes

**Backend — novos**

- `backend/src/main/resources/db/migration/V9__simulado.sql`
- `backend/src/main/resources/db/migration-dev/V904__seed_simulados_dev.sql`
- `backend/src/main/java/br/com/certame/simulado/` — `Simulado.java`, `SimuladoComposicao.java`, `SimuladoComposicaoId.java`, `SimuladoQuestao.java`, `SimuladoQuestaoId.java`, `TentativaSimulado.java`, `StatusSimulado.java`, `EstadoSimulado.java`, `TipoTentativa.java`, `StatusTentativa.java`, `SimuladoRepository.java`, `SimuladoComposicaoRepository.java`, `SimuladoQuestaoRepository.java`, `TentativaSimuladoRepository.java`, `SimuladoAdminService.java`, `ValidadorPublicacaoSimulado.java`, `MontadorProvaSimulado.java`, `PublicacaoSimuladoService.java`, `JanelaSimulado.java`, `SimuladoCatalogoService.java`, `TentativaSimuladoService.java`, `ExpiradorTentativaSimulado.java`, `GeradorCodigoSimulado.java`, `SimuladoController.java`, `SimuladoAdminController.java`
- `backend/src/main/java/br/com/certame/simulado/dto/` — `RequisicaoSimulado.java`, `RequisicaoComposicao.java`, `ItemComposicao.java`, `FiltroSimulado.java`, `SimuladoResumo.java`, `SimuladoConcluido.java`, `RespostaSimulados.java`, `SimuladoDetalhe.java`, `SimuladoResumoAdmin.java`, `ResumoComposicao.java`, `RespostaTentativa.java`, `RespostaResultado.java`
- Testes: `JanelaSimuladoTest`, `ValidadorPublicacaoSimuladoTest`, `MontadorProvaSimuladoTest`, `SimuladoCatalogoServiceTest`, `TentativaSimuladoServiceTest`, `SimuladoAdminServiceTest`, `PublicacaoSimuladoIntegracaoTest`, `TentativaSimuladoIntegracaoTest`, `SimuladoAutorizacaoIntegracaoTest`

**Backend — modificados**

- `br/com/certame/estudo/SessaoEstudoService.java` — sobrecarga de `abrir` com série explícita (F3)
- `br/com/certame/comum/excecao/` — `SimuladoForaDaJanelaException.java`, `TentativaNaoEncontradaException.java` (ou reuso de `ConflitoException` e `RecursoNaoEncontradoException` com os `code` da tabela de erros)
- `db/migration/V7__sessao_estudo.sql` — apenas se F3 ainda não tiver sido implementada, para já nascer com o `check` amplo

**Frontend — novos**

- `frontend/src/app/core/api/simulado-api.ts` e `modelos-simulado.ts` (+ `.spec.ts`)
- `frontend/src/app/features/simulados/simulados-page/` — `simulados-page.{ts,html,scss,spec.ts}`
- `frontend/src/app/features/simulados/proximo-simulado-card/`, `simulado-card/`
- `frontend/src/app/features/simulados/simulado-resultado-page/`
- `frontend/src/app/features/admin/simulados/simulados-page/`, `simulado-form-page/`, `composicao-disciplinas/`
- `frontend/e2e/simulados.spec.ts`, `frontend/e2e/admin-simulados.spec.ts`

**Frontend — modificados**

- `frontend/src/app/app.routes.ts` — `/simulados`, `/simulados/:id/resultado`, `/admin/simulados`, `/admin/simulados/nova`, `/admin/simulados/:id` (as três últimas com `perfilGuard([ADMIN, SUPER_ADMIN])`)
- `frontend/src/app/layout/shell/shell.html` — itens "Simulados" na navegação do estudante e na administrativa

**Documentos**

- `TECH-SPEC.md` §5 (tabelas novas e ajuste em `sessao_estudo`) e §6 (os dez endpoints)
- `tasks/prd-sessao-de-estudo/techspec.md` — sobrecarga de `abrir` e `check` de `tamanho`/`ordem`
- `protótipo/Certame.dc.html` — fonte da verdade visual dos artboards `simulados`, `admin-simulados` e `admin-simulado-form`
