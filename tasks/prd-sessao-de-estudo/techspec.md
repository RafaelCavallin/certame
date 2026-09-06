# Especificação técnica

> **Funcionalidade:** Início e sessão de estudo (Foco, Contexto e Modo Prova) · **PRD:** `tasks/prd-sessao-de-estudo/prd.md` · **Fase:** F2 — Estudo
> Deriva de `TECH-SPEC.md` v1.0 (§3–§8, recortados para RF-06…RF-12 e RN-01…RN-04).
> Depende de `tasks/prd-acesso-e-conta/techspec.md` (`SegurancaConfig`, `ManipuladorErrosGlobal`, `ErroApi`, `Usuario`) e de `tasks/prd-banco-de-questoes/techspec.md` (`questao`, `alternativa`, `questao_estatistica`, `catalogo`, `PaginaResposta<T>`).
> É consumida por `prd-cadernos-e-desempenho` (`SerieDoCadernoService`, `SessaoFinalizada`, `FonteRespostas`, `marcacao`) e por `prd-flashcards-decks-e-revisao` (`TempoEstudoFlashcards`).

## Resumo

Esta é a funcionalidade central do produto e a primeira que **escreve histórico**. Tudo o que veio antes preparava o acervo; aqui o estudante o consome, e o que ele responde vira a fonte de verdade de desempenho, cadernos e estatísticas de questão. O desenho parte de duas restrições que se puxam mutuamente: a próxima questão precisa aparecer em 300 ms (CA-14) e nada do gabarito pode vazar antes da hora (CA-07). A resposta para as duas é a mesma decisão — **a série inteira é materializada no servidor e entregue de uma vez, sempre sem gabarito**. Navegar entre questões passa a ser troca de índice em memória, sem rede; o gabarito só existe no cliente depois que ele chega como resposta de um `POST`, e no Modo Prova só depois da finalização. CA-07 e CA-14 deixam de depender de disciplina do frontend e passam a ser propriedade do contrato.

A série ganha tabela própria, `sessao_questao`, porque "continuar de onde parou" (CA-02) exige retomar **a mesma série**, não uma série equivalente. Guardar apenas os filtros e refazer a consulta devolveria outro conjunto de questões — a promessa do PRD é de continuidade, e continuidade não se remonta por sorteio. A janela de retomada é de sete dias; nesse intervalo alguma questão pode ter sido despublicada, e a retomada a descarta silenciosamente e reindexa a série, mantendo RN-01 verdadeiro na volta e não só na abertura.

O modo é propriedade da sessão, e a distinção que estrutura o código é entre os dois modos de leitura e o modo de prova. **Foco e Contexto alternam livremente na mesma sessão** — a troca é um `PATCH` que muda uma coluna e nada mais, o que faz de CA-06 uma consequência do modelo: nada é remontado, nenhum índice se perde, o cronômetro nem sabe que houve troca. **O Modo Prova é escolhido na abertura e não aceita troca**: é uma tentativa fechada, com respostas retidas no cliente e enviadas em lote na finalização (RN-04). Abandonar não descarta nada — não há nada a descartar, porque nada foi gravado. CA-10 é verdadeiro por ausência de código, não por rotina de limpeza.

`resposta` é imutável (RN-02) em três camadas: a entidade não tem *setter*, nenhum serviço abre caminho de escrita sobre linha existente, e uma `rule` do PostgreSQL recusa `update` e `delete` na tabela. CA-15 é testado contra o banco, não contra a boa vontade da aplicação.

O cronômetro (RF-12) é o número que o Início mostra (RF-06), e por isso o tempo do dia é a **duração das sessões menos as pausas**: `pausado_ms` acumula no cliente e sobe monotonicamente em cada escrita da sessão. O total do dia soma as sessões de questões e as de flashcards, estas pela porta `TempoEstudoExterno`, declarada aqui e implementada por F6b — que assim entra sem que esta entrega dependa dela para compilar.

No frontend, a entrega são duas telas em tradução 1:1 do protótipo Nocturne: o Início e a tela de resolução com seus três modos e a correção. O protótipo cobre a maior parte, mas **nove peças faltam ou contrariam o PRD** (quantidade da série, as duas marcações, navegação por número no Foco, metadados vazando no Foco, estado de série vazia, cards do Início); elas entram primeiro no `Certame.dc.html`, conforme a regra 1 do `AGENTS.md`, e estão listadas em *Riscos conhecidos*.

## Arquitetura do sistema

### Visão dos componentes

```
Angular 22 (SPA)                                Spring Boot 4.1                        PostgreSQL 18
────────────────                                ───────────────                        ─────────────
features/dashboard/                                                                    ┌── sessao_estudo    (nova)
  InicioPage        ────▶  GET    /api/v1/estudo/inicio                                │
                                                                                       ├── sessao_questao   (nova)
features/study/                                                                        │
  EstudarPage       ────▶  POST   /api/v1/estudo/sessoes                               ├── resposta         (nova)
   ├ PainelFiltros  ────▶  GET    /api/v1/estudo/sessoes/{id}                          │      + rule de imutabilidade
   ├ SeletorModo    ────▶  PATCH  /api/v1/estudo/sessoes/{id}/modo                     │
   ├ QuestaoCartao  ────▶  POST   /api/v1/estudo/sessoes/{id}/respostas                ├── marcacao         (nova)
   ├ BarraAcao      ────▶  POST   /api/v1/estudo/sessoes/{id}/finalizar                │
   ├ GabaritoPainel ────▶  PUT    /api/v1/estudo/questoes/{id}/marcacoes               └── questao_estatistica
   ├ PainelNavegacao                                                                          (+1 coluna)
   └ CorrecaoProva
layout/shell                                                                           lê: questao · alternativa
   └ CronometroTopo                                                                         disciplina · assunto · banca
```

**Backend — pacote `br.com.certame.estudo`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `estudo/SessaoEstudo` | novo | Entidade raiz. `usuario`, `modo`, `origem`, `origemRef`, `tamanho`, `pausadoMs`, `iniciadaEm`, `ultimaAtividadeEm`, `finalizadaEm`, `expiradaEm`. Os únicos mutáveis são `modo` (só entre `FOCO` e `CONTEXTO`), `pausadoMs`, `ultimaAtividadeEm` e os dois encerramentos. |
| `estudo/SessaoQuestao`, `SessaoQuestaoId` | novo | A série materializada: `(sessao_id, ordem) → questao_id`. Chave composta `@EmbeddedId`; é o que sustenta CA-02 e CA-12. |
| `estudo/Resposta` | novo | Registro imutável de RN-02: `sessao`, `usuario`, `questao`, `marcada`, `correta`, `tempoMs`, `respondidaEm`. Sem *setter*, sem `@PreUpdate`. |
| `estudo/Marcacao`, `TipoMarcacao` | novo | RF-11. Enum `FAVORITA`, `ERRO_RECORRENTE`; única por trio `(usuario, questao, tipo)`. É a fonte dos cadernos automáticos de F3. |
| `estudo/ModoSessao`, `OrigemSessao` | novo | Enums `FOCO`/`CONTEXTO`/`PROVA` e `LIVRE`/`CADERNO`/`SIMULADO` (RN-02 exige o contexto de origem na resposta). |
| `estudo/TamanhoSerie` | novo | Regra pura: aceita 10, 30 ou 50 e recusa o resto com `fieldErrors` (decisão do usuário). |
| `estudo/FiltroSerie` | novo | Objeto parâmetro do filtro (`disciplinaId`, `assuntoId`, `bancaId`, `ano`, `dificuldade`, `somenteIneditas`, `excluirAcertadas`) — evita o método de sete parâmetros (`code-standards.md` §2.4). |
| `estudo/MontadorSerie` | novo | RN-01/CA-16: sorteia a série entre as questões **publicadas e não excluídas**, sem repetição, no tamanho pedido. `Random` injetado (semente fixa em teste, `tests.md` §2 R). |
| `estudo/SerieRepository` | novo | A consulta nativa do sorteio (uma só, com os filtros e `order by random()` sobre o índice de filtro) e a releitura ordenada da série com *join* em `questao` e `alternativa`. |
| `estudo/SessaoEstudoRepository` | novo | Sessão do usuário, sessão retomável, sessões do dia e as sessões vencidas do expirador. Toda assinatura leva `usuarioId`. |
| `estudo/SessaoQuestaoRepository` | novo | Persistência em lote da série e leitura por sessão. |
| `estudo/RespostaRepository` | novo | Spring Data JPA sobre `resposta`: gravação, respostas da sessão, agregados do dia. |
| `estudo/RespostaEstatisticaRepository` | **modificado** | É o atual `estudo/RespostaRepository` (`JdbcTemplate`, RN-03), renomeado para liberar o nome ao repositório de domínio. Único consumidor: `EstatisticaQuestaoService`. |
| `estudo/ConferenciaGabarito` | novo | Regra pura de CA-04/RF-07: compara `marcada` com o gabarito, tratando `ME` (letra A–E) e `CE` (`CERTO`/`ERRADO`), normalizando caixa e recusando marcação fora do domínio. Sem Spring. |
| `estudo/SessaoEstudoService` | novo | RF-08/CA-02/CA-06: abrir, retomar, trocar modo e finalizar. `@Transactional`, `@PreAuthorize("isAuthenticated()")`. Recusa a troca de modo que envolva `PROVA`. |
| `estudo/RegistroRespostaService` | novo | RF-07/RN-02/CA-05: grava a resposta única (fora do Modo Prova) e devolve gabarito, comentário e estatística. Recusa a segunda resposta para a mesma questão da sessão. |
| `estudo/CorrecaoProvaService` | novo | RF-09/RF-10/RN-04: recebe o lote na finalização, grava tudo numa transação e monta a correção com percentual, acertos, erros, não respondidas, tempo total e a revisão questão a questão. |
| `estudo/MarcacaoService` | novo | RF-11/CA-11: liga e desliga as duas marcações de forma idempotente. |
| `estudo/TempoSessao` | novo | Regra pura de RF-12/CA-13: `duracaoSegundos(iniciadaEm, fim, pausadoMs)` e a soma do dia. Sem Spring, `Clock` de fora. |
| `estudo/inicio/ResumoDiaService` | novo | RF-06/CA-01: resolvidas, percentual de acerto, tempo de estudo do dia e a sessão retomável. Somente leitura. |
| `estudo/inicio/TempoEstudoExterno` | novo | Porta de leitura `segundosNoDia(usuarioId, dia)` que soma o tempo de outras modalidades ao total do Início. Implementada por `flashcard/revisao/TempoEstudoFlashcards` (F6b); ausente a implementação, o total vem só das questões. |
| `estudo/SessaoEstudoController` | novo | `/api/v1/estudo/sessoes` e `/api/v1/estudo/questoes/{id}/marcacoes`. Só orquestra; o usuário vem do token. |
| `estudo/inicio/InicioController` | novo | `GET /api/v1/estudo/inicio`. |
| `estudo/ExpiradorSessoes` | novo | `@Scheduled` horário: marca `expirada_em` nas sessões sem atividade há mais de 7 dias (`FOCO`/`CONTEXTO`) ou 24 h (`PROVA`, RN-04 e §7 da `TECH-SPEC.md`). Nunca apaga linha. |
| `estudo/dto/` | novo | `RequisicaoSessao`, `RespostaSessao`, `QuestaoDaSerie`, `AlternativaDaSerie`, `RequisicaoResposta`, `RespostaRegistrada`, `RequisicaoFinalizacao`, `RespostaCorrecao`, `ItemCorrecao`, `RequisicaoModo`, `RequisicaoMarcacoes`, `MarcacoesQuestao`, `ResumoInicio`, `SessaoRetomavel`. |
| `comum/evento/SessaoFinalizada` | novo | `record SessaoFinalizada(UUID usuarioId, UUID sessaoId, Instant finalizadaEm)` — contrato já especificado por `prd-cadernos-e-desempenho`, publicado aqui. |
| `comum/ZonaAplicacao` | novo | `America/Sao_Paulo`. "Hoje" do Início e das métricas passa por aqui; um dia civil calculado em UTC viraria às 21h em Brasília. |
| `comum/excecao/SerieVaziaException`, `SessaoEncerradaException`, `ModoNaoAlteravelException`, `RespostaDuplicadaException` | novo | Os quatro erros de negócio próprios da funcionalidade. `ManipuladorErrosGlobal` **não muda**. |
| `questao/QuestaoEstatistica`, `EstatisticaQuestaoService` | **modificado** | Ganham `tempo_medio_ms`, exigido pelo painel de contexto do protótipo. O cálculo de RN-03 (excluir o autor) não muda. |
| `questao/QuestaoRepository` | reutilizado | Leitura da questão e do gabarito na conferência. Nenhuma alteração. |

**Frontend — `frontend/src/app`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `core/api/estudo-api.ts` | novo | Cliente HTTP dos seis endpoints. Nenhum componente injeta `HttpClient`. |
| `core/api/modelos-estudo.ts` | novo | Tipos do contrato: `RespostaSessao`, `QuestaoDaSerie`, `RespostaRegistrada`, `RespostaCorrecao`, `ResumoInicio`, `MarcacoesQuestao`. |
| `features/study/study-session-store.ts` | novo | `StudySessionStore` (`providedIn: 'root'`, signals): série, índice, modo, respostas dadas, marcações, gabaritos recebidos. Sobrevive à abertura do diálogo de flashcard (RF-28, regra 8 do `AGENTS.md`). |
| `features/study/cronometro.ts` | novo | `Cronometro`: signal de segundos por `interval`, pausa em `document.hidden` e no botão da topbar, acumula `pausadoMs`. Nunca reinicia por troca de modo ou de questão (CA-06, CA-13). |
| `features/study/estudar-page/` | novo | Orquestra a tela: lê a rota, abre ou retoma a sessão, delega o resto aos filhos. |
| `features/study/painel-filtros/` | novo | Coluna esquerda do modo Contexto: disciplina, assunto, banca, ano, dificuldade e **quantidade de questões**. |
| `features/study/seletor-modo/` | novo | O `seg` de três opções; `Modo prova` desabilitado com sessão em andamento e substituído por "Iniciar prova". |
| `features/study/questao-cartao/` | novo | Enunciado, tags de metadados (só em Contexto) e as alternativas `ME`/`CE`. |
| `features/study/barra-acao/` | novo | Anterior, ação primária, próxima e "Finalizar prova". |
| `features/study/gabarito-painel/` | novo | CA-05: acerto/erro, gabarito, taxa de acerto, respostas, tempo médio e comentário. Só fora do Modo Prova. `aria-live="polite"`. |
| `features/study/painel-navegacao/` | novo | CA-12: grade numerada com o estado de cada questão; versão compacta no Foco, painel lateral no Contexto. |
| `features/study/correcao-prova/` | novo | RF-10/CA-09: percentual, acertos, erros, não respondidas, tempo total, revisão item a item e as ações "refazer" e "voltar ao início". |
| `features/study/atalhos-teclado.ts` | novo | `A`–`E`, `C`/`E`, `Enter`, `←`/`→`, `F`. Diretiva pura, testável sem DOM real. |
| `features/dashboard/inicio-page` | **modificado** | Hoje é um stub de duas linhas; passa a ser a tela de RF-06 com os cards do dia e o cartão "Retomar". |
| `layout/shell` | **modificado** | Item "Resolver questões" na navegação e o cronômetro na topbar quando a rota é `/estudar`. |
| `app.routes.ts` | **modificado** | Rota `/estudar` sob o `authGuard`, com `?modo=` conforme a §3 da `TECH-SPEC.md`. |

**Fluxo de dados.** O Início faz **uma** requisição e recebe o dia inteiro já calculado, incluindo a sessão retomável. "Continuar" navega para `/estudar?sessao={id}`, que faz `GET /estudo/sessoes/{id}` e recebe a série, as respostas já dadas e o índice da primeira sem resposta. Uma sessão nova faz `POST /estudo/sessoes` e recebe a série sorteada. Em ambos os casos **o payload já traz todas as questões**: avançar é `indice.set(i + 1)`, sem rede (CA-14). Fora do Modo Prova, confirmar dispara `POST /respostas` e a resposta traz gabarito, comentário e estatística, que o store guarda por questão — voltar a uma questão já respondida reexibe o painel sem nova chamada. No Modo Prova nada sai do cliente até `POST /finalizar`, que grava o lote numa transação e devolve a correção. A finalização publica `SessaoFinalizada` depois do commit; quem escuta, em F3, é o `desempenho`.

## Design de implementação

### Principais interfaces

```
SessaoEstudoService
  abrir(UUID usuarioId, RequisicaoSessao)              -> RespostaSessao      // RF-08, RN-01, CA-03, CA-16
  retomar(UUID usuarioId, UUID sessaoId)               -> RespostaSessao      // CA-02
  retomavel(UUID usuarioId)                            -> Optional<SessaoRetomavel>
  trocarModo(UUID usuarioId, UUID sessaoId, ModoSessao)-> RespostaSessao      // CA-06
  finalizar(UUID usuarioId, UUID sessaoId, RequisicaoFinalizacao) -> RespostaCorrecao

RegistroRespostaService
  registrar(UUID usuarioId, UUID sessaoId, RequisicaoResposta) -> RespostaRegistrada  // RF-07, RN-02, CA-05

CorrecaoProvaService
  corrigir(SessaoEstudo sessao, List<RespostaProva> lote) -> RespostaCorrecao  // RF-10, RN-04, CA-09

MarcacaoService
  aplicar(UUID usuarioId, UUID questaoId, RequisicaoMarcacoes) -> MarcacoesQuestao  // RF-11, CA-11
  marcadasDa(UUID usuarioId, List<UUID> questaoIds)    -> Map<UUID, MarcacoesQuestao>

MontadorSerie                                           // regra pura sobre o repositorio
  montar(FiltroSerie filtro, TamanhoSerie tamanho, UUID usuarioId) -> List<UUID>  // RN-01, CA-16

ConferenciaGabarito                                     // regra pura
  corrigir(TipoQuestao tipo, String gabarito, String marcada) -> boolean  // CA-04
  valida(TipoQuestao tipo, String marcada)             -> boolean

TempoSessao                                             // regra pura, Clock injetado
  duracaoSegundos(Instant inicio, Instant fim, int pausadoMs) -> int      // RF-12, CA-13
  acumular(int pausadoAtual, int pausadoRecebido)      -> int             // monotonico

ResumoDiaService
  doDia(UUID usuarioId, LocalDate dia)                 -> ResumoInicio     // RF-06, CA-01

TempoEstudoExterno                                      // porta implementada por F6b
  segundosNoDia(UUID usuarioId, LocalDate dia)         -> int              // CA-13
```

### Modelos de dados

Todo campo ausente na origem é normalizado para `null`, seguindo a convenção já adotada em `QuestaoDetalhe`. Datas e instantes em ISO-8601 UTC; durações sempre em **segundos** quando são para exibição e em **milissegundos** quando são medidas de tempo de resposta, como já ocorre em `resposta.tempo_ms`.

#### `RespostaSessao` — a sessão e sua série, devolvida na abertura e na retomada

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `sessaoId` | `uuid` | sim | Identificador da sessão. |
| `modo` | `FOCO \| CONTEXTO \| PROVA` | sim | Modo ativo. Toda sessão nasce em `FOCO` salvo pedido explícito de `PROVA` (CA-03). |
| `origem` | `LIVRE \| CADERNO \| SIMULADO` | sim | Contexto de origem, gravado em cada resposta (RN-02). |
| `origemRef` | `string` | não | Referência do caderno (`auto:FAVORITA` ou UUID) quando `origem = CADERNO`. `null` em `LIVRE`. |
| `total` | `number` | sim | Tamanho efetivo da série. Pode ser menor que o pedido se o acervo não tiver questões suficientes. |
| `indiceAtual` | `number` | sim | Posição, base 0, da primeira questão sem resposta. `0` numa sessão nova. |
| `iniciadaEm` | `string` | sim | Instante de abertura, âncora do cronômetro. |
| `decorridoSegundos` | `number` | sim | Tempo já contado, descontadas as pausas — o cronômetro retoma daqui (CA-13). |
| `questoes` | `QuestaoDaSerie[]` | sim | A série inteira, **sem gabarito e sem comentário**. |
| `respostas` | `RespostaRegistrada[]` | sim | Respostas já dadas nesta sessão. Vazio numa sessão nova; preenchido na retomada. |

```text
{
  "sessaoId": "8f1c2b7a-3d4e-4f10-9a1b-6c2e5d7f8a90",
  "modo": "FOCO",
  "origem": "LIVRE",
  "origemRef": null,
  "total": 30,
  "indiceAtual": 0,
  "iniciadaEm": "2026-08-31T13:02:41Z",
  "decorridoSegundos": 0,
  "questoes": [
    {
      "id": "c4a1f0de-55b2-4a77-8f1e-2b9d3c6e7a10",
      "ordem": 1,
      "codigo": "Q-10427",
      "tipo": "ME",
      "enunciado": "Sobre o controle de constitucionalidade difuso, assinale a alternativa correta.",
      "disciplina": "Direito Constitucional",
      "assunto": "Controle de constitucionalidade",
      "banca": "Cebraspe",
      "orgao": "TRF 1ª Região",
      "ano": 2024,
      "dificuldade": "MEDIA",
      "alternativas": [
        { "letra": "A", "texto": "Compete exclusivamente ao Supremo Tribunal Federal.", "ordem": 1 },
        { "letra": "B", "texto": "Pode ser exercido por qualquer juiz ou tribunal no caso concreto.", "ordem": 2 },
        { "letra": "C", "texto": "Produz sempre efeitos erga omnes.", "ordem": 3 },
        { "letra": "D", "texto": "Depende de provocação do Procurador-Geral da República.", "ordem": 4 },
        { "letra": "E", "texto": "Foi extinto pela Emenda Constitucional 45/2004.", "ordem": 5 }
      ],
      "favorita": false,
      "erroRecorrente": true
    }
  ],
  "respostas": []
}
```

> **Metadados no modo Foco:** `disciplina`, `assunto`, `banca`, `orgao`, `ano` e `dificuldade` viajam no payload porque a troca para Contexto não pode custar uma requisição (CA-06). Quem os esconde no Foco é o `QuestaoCartao` (CA-03). O que **não** viaja em hipótese alguma é `gabarito`, `comentario` e a estatística — esses só chegam como resposta de `POST /respostas` ou de `POST /finalizar`, e é isso que torna CA-07 estrutural.

> **Série menor que a pedida:** se o filtro combinado com RN-01 render menos questões que `tamanho`, a sessão abre com o que houver e `total` reflete o número real. A tela mostra o aviso "encontramos N questões com esses filtros" e segue. Zero questões é erro, não sessão vazia — ver `SERIE_VAZIA`.

> **Retomada com questão despublicada:** na retomada, a série é relida com `status = 'PUBLICADA' and excluido_em is null`. As questões que saíram do acervo nos últimos sete dias são omitidas e a série é reindexada; `total` diminui e as respostas já dadas para elas permanecem no histórico, intactas (RN-02). A tela informa a redução uma vez, sem bloquear.

```text
{
  "total": 28,
  "indiceAtual": 14,
  "questoesRemovidas": 2
}
```

#### `QuestaoDaSerie` — item da série

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `uuid` | sim | Id da questão. |
| `ordem` | `number` | sim | Posição na série, base 1 — é o número que a navegação de CA-12 mostra. |
| `codigo` | `string` | sim | `Q-#####`. |
| `tipo` | `ME \| CE` | sim | Múltipla escolha ou certo/errado. |
| `enunciado` | `string` | sim | Texto completo. |
| `disciplina`, `assunto`, `banca`, `orgao` | `string` | não | Nomes já resolvidos; `null` quando a questão não tem o vínculo. |
| `ano` | `number` | não | Ano da prova. |
| `dificuldade` | `FACIL \| MEDIA \| DIFICIL` | não | — |
| `alternativas` | `AlternativaDaSerie[]` | sim | Vazio quando `tipo = CE`. |
| `favorita`, `erroRecorrente` | `boolean` | sim | Estado das marcações do usuário (RF-11), para que a série já chegue com os ícones certos. |

#### `RequisicaoSessao` — corpo da abertura

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `modo` | `FOCO \| CONTEXTO \| PROVA` | não | Padrão `FOCO` (CA-03). |
| `tamanho` | `number` | sim | Um entre **10, 30 ou 50**; qualquer outro valor responde 422 em `tamanho`. |
| `origem` | `LIVRE \| CADERNO` | não | Padrão `LIVRE`. `SIMULADO` é de `prd-simulados`. |
| `origemRef` | `string` | condicional | Obrigatório quando `origem = CADERNO`; resolvido por `SerieDoCadernoService`. |
| `filtro` | `FiltroSerie` | não | Ignorado quando `origem = CADERNO`. |

```text
{
  "modo": "FOCO",
  "tamanho": 30,
  "origem": "LIVRE",
  "origemRef": null,
  "filtro": {
    "disciplinaId": "1a2b3c4d-0000-4000-8000-000000000001",
    "assuntoId": null,
    "bancaId": null,
    "ano": null,
    "dificuldade": "MEDIA",
    "somenteIneditas": false,
    "excluirAcertadas": false
  }
}
```

#### `RequisicaoResposta` — corpo de uma resposta fora do Modo Prova

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `questaoId` | `uuid` | sim | Precisa pertencer à série da sessão. |
| `marcada` | `string` | sim | `A`–`E` para `ME`; `CERTO` ou `ERRADO` para `CE`. Fora disso, 422. |
| `tempoMs` | `number` | sim | Tempo gasto na questão, ≥ 0 (RN-02). |
| `pausadoMs` | `number` | não | Total acumulado de pausa do cronômetro. Só é aceito se ≥ o valor já gravado. |

#### `RespostaRegistrada` — o que volta de uma resposta

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `questaoId` | `uuid` | sim | — |
| `marcada` | `string` | sim | O que o estudante marcou. |
| `correta` | `boolean` | sim | Resultado da conferência. |
| `gabarito` | `string` | sim | Gabarito oficial. **Ausente no Modo Prova antes da finalização.** |
| `comentario` | `string` | não | Comentário do professor; `null` quando a questão não tem. |
| `estatistica` | `EstatisticaQuestao` | não | `null` quando esta é a primeira resposta computável da questão (RN-03). |
| `tempoMs` | `number` | sim | Eco do tempo gravado. |
| `respondidaEm` | `string` | sim | Instante da gravação. |

```text
{
  "questaoId": "c4a1f0de-55b2-4a77-8f1e-2b9d3c6e7a10",
  "marcada": "B",
  "correta": true,
  "gabarito": "B",
  "comentario": "O controle difuso é exercido por qualquer órgão do Judiciário no caso concreto...",
  "estatistica": { "pctAcerto": 64, "totalRespostas": 1287, "tempoMedioSegundos": 78 },
  "tempoMs": 41200,
  "respondidaEm": "2026-08-31T13:04:02Z"
}
```

> **Questão sem público ainda:** a estatística exclui as respostas do próprio autor (RN-03). Enquanto só o autor tiver respondido, `estatistica` volta `null` e o painel mostra "sem estatística suficiente" em vez de `0%` — zero por cento e "ninguém respondeu" são coisas diferentes.

```text
{
  "estatistica": null
}
```

#### `RequisicaoFinalizacao` — corpo da finalização

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `respostas` | `RespostaProva[]` | sim | Vazio fora do Modo Prova, onde as respostas já foram gravadas uma a uma. No Modo Prova, o lote completo; questões omitidas contam como não respondidas (RF-10). |
| `pausadoMs` | `number` | não | Acumulado final das pausas. |

Cada item é `{ "questaoId": uuid, "marcada": string, "tempoMs": number }`.

#### `RespostaCorrecao` — a correção (RF-10)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `sessaoId` | `uuid` | sim | — |
| `pctAcerto` | `number` | sim | Inteiro 0–100 sobre o total da série, não sobre as respondidas. |
| `acertos`, `erros`, `naoRespondidas` | `number` | sim | Somam `total`. |
| `total` | `number` | sim | Tamanho da série. |
| `tempoTotalSegundos` | `number` | sim | Duração da sessão menos as pausas (CA-13). |
| `finalizadaEm` | `string` | sim | — |
| `itens` | `ItemCorrecao[]` | sim | Revisão questão a questão, na ordem da série. |

`ItemCorrecao`: `{ ordem, questaoId, codigo, enunciadoResumo, disciplina, gabarito, marcada|null, correta|null, comentario|null }`. `marcada` e `correta` são `null` na questão não respondida.

```text
{
  "sessaoId": "8f1c2b7a-3d4e-4f10-9a1b-6c2e5d7f8a90",
  "pctAcerto": 73,
  "acertos": 22,
  "erros": 6,
  "naoRespondidas": 2,
  "total": 30,
  "tempoTotalSegundos": 4530,
  "finalizadaEm": "2026-08-31T14:18:11Z",
  "itens": [
    {
      "ordem": 1, "questaoId": "c4a1f0de-55b2-4a77-8f1e-2b9d3c6e7a10", "codigo": "Q-10427",
      "enunciadoResumo": "Sobre o controle de constitucionalidade difuso, assinale a alternativa correta.",
      "disciplina": "Direito Constitucional", "gabarito": "B", "marcada": "B", "correta": true,
      "comentario": "O controle difuso é exercido por qualquer órgão do Judiciário..."
    },
    {
      "ordem": 2, "questaoId": "e7d2c1b0-11aa-4bb2-9cc3-4dd5e6f70819", "codigo": "Q-10431",
      "enunciadoResumo": "Julgue o item: a súmula vinculante possui efeito erga omnes.",
      "disciplina": "Direito Constitucional", "gabarito": "CERTO", "marcada": null, "correta": null,
      "comentario": null
    }
  ]
}
```

#### `ResumoInicio` — o dia do estudante (RF-06)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `dia` | `string` | sim | Data em `America/Sao_Paulo`. |
| `questoesResolvidas` | `number` | sim | Respostas gravadas no dia. |
| `pctAcerto` | `number` | não | Inteiro 0–100; `null` quando não houve resposta no dia. |
| `tempoEstudoSegundos` | `number` | sim | Soma de `tempoQuestoesSegundos` e `tempoFlashcardsSegundos`. |
| `tempoQuestoesSegundos` | `number` | sim | Sessões de questões do dia, descontadas as pausas. |
| `tempoFlashcardsSegundos` | `number` | sim | Vem de `TempoEstudoExterno`; `0` enquanto F6b não existir. |
| `sessaoRetomavel` | `SessaoRetomavel` | não | `null` quando não há sessão retomável. |

`SessaoRetomavel`: `{ sessaoId, modo, origem, disciplina|null, indiceAtual, total, ultimaAtividadeEm }`.

```text
{
  "dia": "2026-08-31",
  "questoesResolvidas": 42,
  "pctAcerto": 68,
  "tempoEstudoSegundos": 5460,
  "tempoQuestoesSegundos": 4530,
  "tempoFlashcardsSegundos": 930,
  "sessaoRetomavel": {
    "sessaoId": "8f1c2b7a-3d4e-4f10-9a1b-6c2e5d7f8a90",
    "modo": "FOCO",
    "origem": "LIVRE",
    "disciplina": "Direito Constitucional",
    "indiceAtual": 13,
    "total": 40,
    "ultimaAtividadeEm": "2026-08-31T13:44:07Z"
  }
}
```

> **Dia sem atividade:** os contadores voltam em zero, `pctAcerto` vem `null` e `sessaoRetomavel` pode continuar preenchido — a sessão de ontem ainda é retomável. A tela mostra o estado vazio ("nenhuma questão hoje") **e** o cartão "Retomar", que não é estado vazio nenhum.

```text
{
  "questoesResolvidas": 0,
  "pctAcerto": null,
  "tempoEstudoSegundos": 0
}
```

#### `ErroApi` — envelope de erro

Formato já em produção (`comum/ErroApi`), servido em `application/problem+json` pelo `ManipuladorErrosGlobal`. Os códigos são estáveis; as mensagens, não.

| Código | HTTP | Significado |
| --- | --- | --- |
| `DADOS_INVALIDOS` | `422` | Tamanho fora de 10/30/50, `marcada` fora do domínio do tipo, `tempoMs` negativo. Traz `fieldErrors`. |
| `SERIE_VAZIA` | `422` | Nenhuma questão publicada atende ao filtro (RN-01). |
| `RECURSO_NAO_ENCONTRADO` | `404` | Sessão de outro usuário, sessão inexistente, questão fora da série. Nunca 403: não revelamos a existência da sessão alheia. |
| `SESSAO_ENCERRADA` | `409` | Escrita em sessão finalizada ou expirada. |
| `MODO_NAO_ALTERAVEL` | `409` | Troca de modo envolvendo `PROVA`. |
| `RESPOSTA_DUPLICADA` | `409` | Segunda resposta para a mesma questão da sessão fora do Modo Prova (RN-02). |
| `CADERNO_NAO_ENCONTRADO` | `404` | Referência de caderno inválida — erro de F3, propagado. |

```text
{
  "code": "SERIE_VAZIA",
  "message": "Nenhuma questão publicada atende a esses filtros. Amplie a busca e tente de novo.",
  "fieldErrors": null
}
```

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Verifique os campos destacados.",
  "fieldErrors": { "tamanho": "Escolha 10, 30 ou 50 questões." }
}
```

#### Esquema de banco — `V7__sessao_estudo.sql`

Migração versionada, imutável e aditiva em `backend/src/main/resources/db/migration`. O repositório já contém `V6__cadernos_desempenho.sql`, portanto **V7 é o próximo número livre**. A semente de `dev` fica em `V902__seed_sessao_estudo_dev.sql`.

```sql
create table sessao_estudo (
  id uuid primary key default gen_random_uuid(),
  usuario_id uuid not null references usuario(id),
  modo text not null check (modo in ('FOCO','CONTEXTO','PROVA')),
  origem text not null default 'LIVRE' check (origem in ('LIVRE','CADERNO','SIMULADO')),
  origem_ref text,
  tamanho smallint not null check (tamanho in (10,30,50)),
  pausado_ms integer not null default 0 check (pausado_ms >= 0),
  iniciada_em timestamptz not null default now(),
  ultima_atividade_em timestamptz not null default now(),
  finalizada_em timestamptz,
  expirada_em timestamptz,
  constraint sessao_encerramento_unico check (finalizada_em is null or expirada_em is null),
  constraint sessao_caderno_com_ref check (origem <> 'CADERNO' or origem_ref is not null)
);
create index sessao_estudo_retomavel_idx on sessao_estudo (usuario_id, ultima_atividade_em desc)
  where finalizada_em is null and expirada_em is null;
create index sessao_estudo_dia_idx on sessao_estudo (usuario_id, iniciada_em desc);

create table sessao_questao (
  sessao_id  uuid not null references sessao_estudo(id) on delete cascade,
  ordem      smallint not null check (ordem between 1 and 50),
  questao_id uuid not null references questao(id),
  primary key (sessao_id, ordem),
  unique (sessao_id, questao_id)
);

create table resposta (
  id            uuid primary key default gen_random_uuid(),
  sessao_id     uuid not null references sessao_estudo(id),
  usuario_id    uuid not null references usuario(id),
  questao_id    uuid not null references questao(id),
  marcada       text not null,
  correta       boolean not null,
  tempo_ms      integer not null check (tempo_ms >= 0),
  respondida_em timestamptz not null default now(),
  unique (sessao_id, questao_id)
);
create index resposta_usuario_data_idx on resposta (usuario_id, respondida_em desc);
create index resposta_questao_idx on resposta (questao_id);

-- RN-02: append-only no banco, nao so na aplicacao (CA-15).
create rule resposta_sem_update as on update to resposta do instead nothing;
create rule resposta_sem_delete as on delete to resposta do instead nothing;

create table marcacao (
  id         uuid primary key default gen_random_uuid(),
  usuario_id uuid not null references usuario(id),
  questao_id uuid not null references questao(id),
  tipo       text not null check (tipo in ('FAVORITA','ERRO_RECORRENTE')),
  criado_em  timestamptz not null default now(),
  unique (usuario_id, questao_id, tipo)
);
create index marcacao_usuario_idx on marcacao (usuario_id, tipo, criado_em desc);

alter table questao_estatistica add column tempo_medio_ms integer;
```

> **`rule` em vez de `trigger`:** `do instead nothing` faz o `update` e o `delete` serem silenciosamente descartados em vez de estourarem exceção. É deliberado — a promessa de RN-02 é que a linha não muda, e uma exceção derrubaria a transação inteira de quem tentou, transformando um erro de programação em incidente para o usuário. O teste de CA-15 confere o **estado** depois da tentativa, que é o que a regra promete.

> **`unique (sessao_id, questao_id)` em `resposta`:** é o que impede a segunda resposta para a mesma questão da sessão. No Modo Prova o lote é gravado uma única vez, na finalização, e por isso a troca livre de resposta antes de finalizar (CA-08) não esbarra na restrição — ela acontece inteiramente no cliente.

### Endpoints da API

Base `/api/v1`. Todos exigem autenticação; nenhum exige perfil específico — a tela de resolução é a mesma para os quatro perfis, conforme o PRD. **Seis dos sete são novos no contrato da `TECH-SPEC.md` §6**, que já previa três (`POST /estudo/sessoes`, `POST .../respostas`, `POST .../finalizar`); os outros quatro entram no documento na mesma alteração, como manda a regra 4 do `AGENTS.md`.

#### Visão geral

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/estudo/inicio` | Resumo do dia e sessão retomável (RF-06). **Novo em §6.** |
| `POST` | `/estudo/sessoes` | Abre a sessão e devolve a série. |
| `GET` | `/estudo/sessoes/{id}` | Retoma a sessão: série, respostas já dadas e índice. **Novo em §6.** |
| `PATCH` | `/estudo/sessoes/{id}/modo` | Alterna entre Foco e Contexto. **Novo em §6.** |
| `POST` | `/estudo/sessoes/{id}/respostas` | Registra a resposta e devolve gabarito e estatística. |
| `POST` | `/estudo/sessoes/{id}/finalizar` | Fecha a sessão e devolve a correção. |
| `PUT` | `/estudo/questoes/{questaoId}/marcacoes` | Liga e desliga favorita e erro recorrente (RF-11). **Novo em §6.** |

---

#### `GET /api/v1/estudo/inicio`

O painel de entrada. Uma requisição, o dia inteiro.

**Parâmetros de consulta** — nenhum. O usuário vem do token e o dia, do relógio da aplicação em `America/Sao_Paulo`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `ResumoInicio` | Sempre, com ou sem atividade. |
| `401` | — | Sem token válido. |

**Exemplo — sucesso**

```http
GET /api/v1/estudo/inicio
```

Ver o exemplo de `ResumoInicio` em *Modelos de dados*.

**Exemplo — dia sem atividade e sem sessão pendente**

```http
GET /api/v1/estudo/inicio
```

```text
{
  "dia": "2026-08-31",
  "questoesResolvidas": 0,
  "pctAcerto": null,
  "tempoEstudoSegundos": 0,
  "tempoQuestoesSegundos": 0,
  "tempoFlashcardsSegundos": 0,
  "sessaoRetomavel": null
}
```

> Sem `sessaoRetomavel`, o botão "Continuar estudando" do protótipo vira "Começar a estudar" e leva a `/estudar` sem `?sessao=`.

---

#### `POST /api/v1/estudo/sessoes`

Abre a sessão, sorteia a série entre as questões publicadas e devolve tudo.

**Corpo** — `RequisicaoSessao`.

| Parâmetro | Tipo | Padrão | Regras |
| --- | --- | --- | --- |
| `modo` | `ModoSessao` | `FOCO` | `PROVA` fixa a sessão como tentativa fechada. |
| `tamanho` | `number` | — | 10, 30 ou 50. |
| `origem` | `OrigemSessao` | `LIVRE` | `CADERNO` exige `origemRef`. |
| `origemRef` | `string` | `null` | `auto:FAVORITA`, `auto:ERRO_RECORRENTE` ou UUID de caderno. |
| `filtro` | `FiltroSerie` | `{}` | Ignorado em `origem = CADERNO`. |

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `RespostaSessao` | Sessão aberta, com um ou mais itens na série. |
| `422` | `ErroApi` | `tamanho` fora do domínio (`DADOS_INVALIDOS`) ou nenhuma questão elegível (`SERIE_VAZIA`). |
| `404` | `ErroApi` | Caderno inexistente ou de outro usuário (`CADERNO_NAO_ENCONTRADO`). |

**Exemplo — sucesso**

```http
POST /api/v1/estudo/sessoes
```

Corpo e resposta em *Modelos de dados* (`RequisicaoSessao` e `RespostaSessao`).

**Exemplo — filtro sem questões publicadas**

```http
POST /api/v1/estudo/sessoes
{ "tamanho": 30, "filtro": { "disciplinaId": "…", "ano": 1998 } }
```

```text
{
  "code": "SERIE_VAZIA",
  "message": "Nenhuma questão publicada atende a esses filtros. Amplie a busca e tente de novo.",
  "fieldErrors": null
}
```

> A tela mantém o painel de filtros aberto e mostra a mensagem junto ao botão "Aplicar filtros" — nunca navega para uma sessão que não existe.

---

#### `GET /api/v1/estudo/sessoes/{id}`

Retoma a sessão: a mesma série, as respostas já dadas e a posição de parada (CA-02).

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RespostaSessao` | Sessão do usuário, não encerrada, dentro dos 7 dias. |
| `404` | `ErroApi` | Sessão inexistente, de outro usuário ou expirada. |
| `409` | `ErroApi` | Sessão já finalizada (`SESSAO_ENCERRADA`) — o cliente deve pedir a correção, não a série. |

**Exemplo — retomada com duas questões despublicadas no intervalo**

```http
GET /api/v1/estudo/sessoes/8f1c2b7a-3d4e-4f10-9a1b-6c2e5d7f8a90
```

```text
{
  "sessaoId": "8f1c2b7a-3d4e-4f10-9a1b-6c2e5d7f8a90",
  "modo": "CONTEXTO",
  "total": 28,
  "indiceAtual": 14,
  "questoesRemovidas": 2,
  "decorridoSegundos": 1832,
  "questoes": [ "…28 itens, reindexados de 1 a 28…" ],
  "respostas": [ "…14 respostas já gravadas…" ]
}
```

> `decorridoSegundos` devolve o cronômetro de onde ele parou: a retomada continua a mesma sessão e o mesmo tempo, não abre um relógio novo (CA-13).

---

#### `PATCH /api/v1/estudo/sessoes/{id}/modo`

Alterna Foco ↔ Contexto sem tocar em mais nada (CA-06).

**Corpo** — `{ "modo": "CONTEXTO" }`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RespostaSessao` | Modo trocado; `indiceAtual`, `respostas` e `decorridoSegundos` voltam inalterados. |
| `409` | `ErroApi` | `modo = PROVA`, ou sessão em `PROVA` (`MODO_NAO_ALTERAVEL`). |
| `404` | `ErroApi` | Sessão de outro usuário. |

**Exemplo — tentativa de entrar no Modo Prova com sessão em andamento**

```http
PATCH /api/v1/estudo/sessoes/8f1c2b7a-.../modo
{ "modo": "PROVA" }
```

```text
{
  "code": "MODO_NAO_ALTERAVEL",
  "message": "O Modo Prova é escolhido ao iniciar a sessão. Inicie uma nova prova para simular a aplicação.",
  "fieldErrors": null
}
```

> A interface não chega a emitir essa chamada — o seletor já apresenta "Iniciar prova" em vez de "Modo prova" quando há sessão em andamento. O endpoint recusa mesmo assim: a interface esconde, o servidor decide (`AGENTS.md`, regra 2).

---

#### `POST /api/v1/estudo/sessoes/{id}/respostas`

Registra a resposta e devolve o resultado (RF-07, CA-05). **Não existe fora dos modos Foco e Contexto**: no Modo Prova responde 409, porque lá as respostas só valem depois da finalização (RN-04).

**Corpo** — `RequisicaoResposta`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `RespostaRegistrada` | Resposta gravada, com gabarito, comentário e estatística. |
| `422` | `ErroApi` | `marcada` fora do domínio do tipo da questão, `tempoMs` negativo. |
| `404` | `ErroApi` | Questão fora da série da sessão. |
| `409` | `ErroApi` | Sessão em `PROVA`, sessão encerrada, ou questão já respondida (`RESPOSTA_DUPLICADA`). |

**Exemplo — sucesso**

```http
POST /api/v1/estudo/sessoes/8f1c2b7a-.../respostas
{ "questaoId": "c4a1f0de-…", "marcada": "B", "tempoMs": 41200, "pausadoMs": 0 }
```

Resposta em *Modelos de dados* (`RespostaRegistrada`).

**Exemplo — marcação inválida para questão certo/errado**

```http
POST /api/v1/estudo/sessoes/8f1c2b7a-.../respostas
{ "questaoId": "e7d2c1b0-…", "marcada": "C", "tempoMs": 9000 }
```

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Verifique os campos destacados.",
  "fieldErrors": { "marcada": "Para questões de certo/errado, marque CERTO ou ERRADO." }
}
```

**Exemplo — segunda resposta para a mesma questão**

```http
POST /api/v1/estudo/sessoes/8f1c2b7a-.../respostas
{ "questaoId": "c4a1f0de-…", "marcada": "C", "tempoMs": 5000 }
```

```text
{
  "code": "RESPOSTA_DUPLICADA",
  "message": "Esta questão já foi respondida nesta sessão.",
  "fieldErrors": null
}
```

> É a face HTTP de RN-02. A tela nunca provoca esse erro — o botão vira "Próxima questão" assim que o gabarito aparece (CA-04/CA-05) —, mas o registro não depende disso.

---

#### `POST /api/v1/estudo/sessoes/{id}/finalizar`

Fecha a sessão e devolve a correção (RF-10). No Modo Prova é aqui que **todas** as respostas são gravadas, numa única transação (RN-04).

**Corpo** — `RequisicaoFinalizacao`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `RespostaCorrecao` | Sessão fechada e corrigida. |
| `422` | `ErroApi` | Item do lote com `marcada` inválida ou questão fora da série. |
| `409` | `ErroApi` | Sessão já encerrada (`SESSAO_ENCERRADA`). |
| `404` | `ErroApi` | Sessão de outro usuário. |

**Exemplo — Modo Prova com duas questões em branco**

```http
POST /api/v1/estudo/sessoes/8f1c2b7a-.../finalizar
{
  "respostas": [
    { "questaoId": "c4a1f0de-…", "marcada": "B", "tempoMs": 41200 },
    { "questaoId": "9b0c1d2e-…", "marcada": "CERTO", "tempoMs": 22800 }
  ],
  "pausadoMs": 12000
}
```

Resposta em *Modelos de dados* (`RespostaCorrecao`).

**Exemplo — finalização fora do Modo Prova**

```http
POST /api/v1/estudo/sessoes/8f1c2b7a-.../finalizar
{ "respostas": [] }
```

```text
{
  "sessaoId": "8f1c2b7a-3d4e-4f10-9a1b-6c2e5d7f8a90",
  "pctAcerto": 71,
  "acertos": 17, "erros": 7, "naoRespondidas": 6, "total": 30,
  "tempoTotalSegundos": 3210,
  "finalizadaEm": "2026-08-31T14:18:11Z",
  "itens": [ "…30 itens…" ]
}
```

> Fora do Modo Prova a correção é apenas o fechamento do relatório: as respostas já estavam gravadas, e um lote não vazio aqui é ignorado com 422 no primeiro item, para que não exista um segundo caminho de escrita sobre `resposta`.

---

#### `PUT /api/v1/estudo/questoes/{questaoId}/marcacoes`

Estado desejado das duas marcações, idempotente (RF-11, CA-11).

**Corpo** — `{ "favorita": true, "erroRecorrente": false }`.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `MarcacoesQuestao` | Estado após a aplicação: `{ questaoId, favorita, erroRecorrente }`. |
| `404` | `ErroApi` | Questão inexistente, excluída ou não publicada. |

**Exemplo — marcar como erro recorrente mantendo a favorita**

```http
PUT /api/v1/estudo/questoes/c4a1f0de-…/marcacoes
{ "favorita": true, "erroRecorrente": true }
```

```text
{ "questaoId": "c4a1f0de-55b2-4a77-8f1e-2b9d3c6e7a10", "favorita": true, "erroRecorrente": true }
```

> Enviar o mesmo corpo duas vezes devolve o mesmo resultado e não cria linha duplicada — a restrição `unique (usuario_id, questao_id, tipo)` é a segunda barreira. A marcação vive fora da sessão: ela persiste depois do encerramento e alimenta os cadernos automáticos de F3 sem nenhuma escrita adicional.

---

## Pontos de integração

Nenhuma integração externa: nem serviço de terceiro, nem rede fora do próprio PostgreSQL. Não há, portanto, *timeout*, retentativa ou idempotência de integração a especificar. As integrações relevantes são internas:

| Integração | Direção | Contrato |
| --- | --- | --- |
| `estudo` → `catalogo` | Validação dos ids do filtro e resolução dos nomes exibidos. | Por serviço de `catalogo`, nunca pelo repositório de outro domínio (`code-standards.md` §3). |
| `estudo` → `questao` | Leitura da questão, das alternativas e do gabarito na conferência; RN-01 aplicado na origem do sorteio. | `QuestaoRepository` e `EstatisticaQuestaoService`. `questao` não passa a conhecer `estudo`. |
| `estudo` → `questao_estatistica` | RN-03/CA-05: a estatística devolvida ao estudante. | Fora do Modo Prova, `EstatisticaQuestaoService.recalcular(questaoId)` roda **na mesma transação** da resposta — uma agregação indexada, para que o painel mostre o número já incluindo a resposta recém-dada. No Modo Prova, o recálculo do lote roda **depois do commit**, por evento, para não somar 50 agregações ao tempo da finalização. |
| `estudo` → `prd-cadernos-e-desempenho` | Abertura de sessão com `origem = CADERNO`. | `SerieDoCadernoService.questoes(usuarioId, ReferenciaCaderno)` → lista ordenada de `questao_id`; `CADERNO_NAO_ENCONTRADO` quando a referência não é do usuário. Assinatura definida por F3 e adotada aqui sem alteração. |
| `estudo` → `desempenho` (F3) | Finalização de sessão. | Publica `comum/evento/SessaoFinalizada(usuarioId, sessaoId, finalizadaEm)`. O assinante é `@TransactionalEventListener(AFTER_COMMIT)`: falha do agregado não desfaz nem atrasa a finalização. Enquanto F3 não existir, o evento é publicado sem ouvinte — comportamento válido do Spring. |
| `desempenho` → `estudo` (F3) | Leitura de `resposta`. | Porta `FonteRespostas`, implementada por `estudo/resposta/RespostaAgregadoAdapter` em F3, filtrando `sessao_estudo.finalizada_em is not null` (RN-04). Esta entrega apenas garante que a coluna exista e signifique o que F3 espera. |
| `flashcard` → `estudo` (F6b) | Tempo de flashcards no total do dia (CA-13/RF-06). | `flashcard/revisao/TempoEstudoFlashcards` passa a implementar `estudo/inicio/TempoEstudoExterno` — uma linha de `implements`, sem mudança de assinatura (`segundosNoDia(UUID, LocalDate) -> int`). Ausente o bean, `ResumoDiaService` devolve `tempoFlashcardsSegundos: 0`. |
| `estudo` → diálogo de flashcard (F6a) | RF-28 e regra 8 do `AGENTS.md`. | `StudySessionStore` e `Cronometro` são `providedIn: 'root'` e não são recriados pela abertura do diálogo; o `DialogService` não navega. Coberto por E2E-06. |

## Abordagem de testes

Meta de cobertura: **mínimo de 80%**, conforme `.agents/rules/tests.md` §1 — que prevalece sobre os 70% citados na §9 da `TECH-SPEC.md`. No backend a regra é aplicada pelo JaCoCo em `./mvnw verify` (80% de linhas por classe `*Service`); no frontend, por `npm run test:coverage` (80% em *statements*, *branches*, *functions* e *lines*). Ferramentas: JUnit Jupiter, AssertJ, Mockito e Testcontainers 2.0.5 no backend; Vitest e Playwright no frontend. Pirâmide alvo ~70/20/10.

Pela tabela de criticidade da rule, a sessão de estudo é de **prioridade média**, com três exceções tratadas como **alta**: a imutabilidade de `resposta` (RN-02, CA-15), o descarte da tentativa abandonada no Modo Prova (RN-04, CA-10) e o não vazamento de gabarito antes da finalização (CA-07). Essas três recebem caminho feliz, bordas e prova no banco.

`Clock` é injetado em todo serviço que lê o tempo, e `Random` em `MontadorSerie` — nenhum teste depende do relógio nem do sorteio da máquina (`tests.md` §2 R).

### Testes de unidade

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `ConferenciaGabarito` acerta múltipla escolha com a letra correta | CA-04 | `corrigir(ME, "B", "B")` é `true`. |
| TU-02 | `ConferenciaGabarito` erra múltipla escolha com letra diferente | CA-04 | `corrigir(ME, "B", "D")` é `false`, sem exceção. |
| TU-03 | `ConferenciaGabarito` normaliza a caixa da marcação | CA-04 | `corrigir(ME, "B", "b")` é `true`. |
| TU-04 | `ConferenciaGabarito` recusa marcação fora de A–E em múltipla escolha | CA-04 | `valida(ME, "F")` é `false` e o serviço lança `DadosInvalidosException` com `fieldErrors["marcada"]`. |
| TU-05 | `ConferenciaGabarito` aceita CERTO e ERRADO em certo/errado | CA-04 | `valida(CE, "CERTO")` e `valida(CE, "ERRADO")` são `true`; `valida(CE, "A")` é `false`. |
| TU-06 | `TamanhoSerie` aceita 10, 30 e 50 e recusa o resto | RN-01 (via CA-16) | 10/30/50 passam; 0, 7, 51 e negativo lançam `DadosInvalidosException` em `tamanho`. |
| TU-07 | `MontadorSerie` monta série só com questões publicadas | CA-16 | Dado um acervo com `RASCUNHO`, `EM_REVISAO` e `PUBLICADA`, a série contém apenas ids publicados. |
| TU-08 | `MontadorSerie` não repete questão dentro da série | CA-12, CA-16 | 30 ids distintos; `Random` com semente fixa torna o caso repetível. |
| TU-09 | `MontadorSerie` devolve série menor quando o acervo não basta | CA-16 | Pedido de 30 sobre 12 elegíveis devolve 12, sem exceção. |
| TU-10 | `MontadorSerie` sem nenhuma questão elegível | CA-16 | Lança `SerieVaziaException` com `code = SERIE_VAZIA`. |
| TU-11 | `SessaoEstudoService` abre toda sessão em Foco quando o modo é omitido | CA-03 | `modo` da sessão criada é `FOCO`; `indiceAtual` é 0. |
| TU-12 | `SessaoEstudoService` troca de Foco para Contexto preservando índice e respostas | CA-06 | Após a troca, `indiceAtual`, `respostas` e `iniciadaEm` são idênticos; só `modo` mudou. |
| TU-13 | `SessaoEstudoService` recusa a troca de modo envolvendo Prova | CA-07 | `ModoNaoAlteravelException` (`409`) tanto para entrar quanto para sair de `PROVA`. |
| TU-14 | `SessaoEstudoService` recusa escrita em sessão finalizada | CA-15 | `SessaoEncerradaException`; nenhuma interação com `RespostaRepository`. |
| TU-15 | `SessaoEstudoService` retoma na primeira questão sem resposta | CA-02 | Série de 40 com 13 respostas devolve `indiceAtual = 13`. |
| TU-16 | `SessaoEstudoService` retoma descartando questão despublicada | CA-02, CA-16 | Duas questões despublicadas somem, `total` cai para 28 e a série é reindexada de 1 a 28. |
| TU-17 | `SessaoEstudoService` não devolve sessão retomável de outro usuário | CA-02 | `RecursoNaoEncontradoException`, nunca 403. |
| TU-18 | `RegistroRespostaService` grava questão, marcada, acerto, tempo e origem | RN-02 (CA-15) | A `Resposta` salva carrega os cinco campos, com `origem` herdada da sessão. |
| TU-19 | `RegistroRespostaService` devolve gabarito, comentário e estatística | CA-05 | O DTO traz os três; `EstatisticaQuestaoService.recalcular` é chamado uma vez. |
| TU-20 | `RegistroRespostaService` devolve estatística nula quando só o autor respondeu | CA-05 | `estatistica` é `null` — não `0%` (RN-03). |
| TU-21 | `RegistroRespostaService` recusa a segunda resposta da mesma questão | CA-15 | `RespostaDuplicadaException`; `save` não é chamado. |
| TU-22 | `RegistroRespostaService` recusa resposta em sessão de Modo Prova | CA-07 | `SessaoEncerradaException` com `code = MODO_NAO_ALTERAVEL`; nada é gravado. |
| TU-23 | `RegistroRespostaService` recusa questão fora da série | CA-12 | `RecursoNaoEncontradoException`; `save` não é chamado. |
| TU-24 | `CorrecaoProvaService` conta acertos, erros e não respondidas | CA-09 | 22/6/2 sobre série de 30; `pctAcerto` é 73 (sobre o total, não sobre as respondidas). |
| TU-25 | `CorrecaoProvaService` grava o lote inteiro numa única chamada | CA-09, RN-04 | `saveAll` recebe as 28 respostas; nenhuma gravação parcial anterior. |
| TU-26 | `CorrecaoProvaService` monta a revisão na ordem da série com item em branco | CA-09 | O item não respondido vem com `marcada: null` e `correta: null`, na posição certa. |
| TU-27 | `CorrecaoProvaService` de prova sem nenhuma resposta | CA-09 | `pctAcerto: 0`, `naoRespondidas` igual ao total, sem divisão por zero. |
| TU-28 | `CorrecaoProvaService` publica `SessaoFinalizada` uma única vez | — | O evento carrega `usuarioId`, `sessaoId` e `finalizadaEm`. |
| TU-29 | `MarcacaoService` liga favorita e erro recorrente de forma idempotente | CA-11 | Duas chamadas iguais → uma linha por tipo; o retorno é igual nas duas. |
| TU-30 | `MarcacaoService` desliga só a marcação pedida | CA-11 | `{favorita:true, erroRecorrente:false}` remove a segunda e preserva a primeira. |
| TU-31 | `TempoSessao` desconta as pausas da duração | CA-13 | 3600 s de janela com 600 000 ms de pausa devolve 3000 s. |
| TU-32 | `TempoSessao` ignora `pausadoMs` menor que o já gravado | CA-13 | `acumular(12000, 5000)` devolve 12000 — o acumulador é monotônico. |
| TU-33 | `TempoSessao` nunca devolve duração negativa | CA-13 | Pausa maior que a janela devolve 0. |
| TU-34 | `ResumoDiaService` soma resolvidas, percentual e tempo do dia | CA-01 | 42 resolvidas, 68%, e o tempo é a soma das sessões do dia menos as pausas. |
| TU-35 | `ResumoDiaService` conta a sessão em andamento pelo `ultima_atividade_em` | CA-13 | Sessão aberta e não finalizada entra no tempo do dia; o número do Início bate com o cronômetro. |
| TU-36 | `ResumoDiaService` soma o tempo de flashcards pela porta | CA-01, CA-13 | Com `TempoEstudoExterno` devolvendo 930, o total é a soma dos dois. |
| TU-37 | `ResumoDiaService` sem a implementação da porta | CA-01 | `tempoFlashcardsSegundos: 0` e o total igual ao das questões, sem `NullPointerException`. |
| TU-38 | `ResumoDiaService` em dia sem atividade | CA-01 | Zeros, `pctAcerto: null`, sem divisão por zero. |
| TU-39 | `ResumoDiaService` usa `America/Sao_Paulo` para o corte do dia | CA-01 | Resposta às 23h de Brasília cai no dia local, não no dia UTC seguinte. |
| TU-40 | `ExpiradorSessoes` expira Prova após 24 h e leitura após 7 dias | CA-10 | Com `Clock` fixo, a prova de 25 h e a sessão de 8 dias ganham `expirada_em`; a de 6 dias não. |
| TU-41 | `StudySessionStore` avança o índice sem nova requisição | CA-14 | `proxima()` incrementa o índice; `HttpTestingController` não recebe requisição alguma. |
| TU-42 | `StudySessionStore` mantém a alternativa marcada ao trocar de modo | CA-06 | Após `trocarModo`, `indice` e `marcada` da questão 7 permanecem. |
| TU-43 | `StudySessionStore` substitui a alternativa marcada, sem acumular | CA-04 | Marcar A e depois C deixa apenas C selecionada. |
| TU-44 | `StudySessionStore` não expõe gabarito no Modo Prova | CA-07 | `gabaritoDe(questaoId)` é `null` para toda a série enquanto `finalizada()` for `false`. |
| TU-45 | `StudySessionStore` permite trocar resposta de questão já respondida na prova | CA-08 | Voltar à questão 3 e marcar outra alternativa atualiza o lote pendente. |
| TU-46 | `StudySessionStore` bloqueia nova resposta fora da prova | CA-05 | Questão já respondida em Foco fica travada; o painel de gabarito continua visível. |
| TU-47 | `StudySessionStore` vai direto para a questão 12 preservando respostas | CA-12 | `irPara(11)` muda o índice; as 11 respostas anteriores continuam no store. |
| TU-48 | `Cronometro` não reinicia na troca de modo nem de questão | CA-06, CA-13 | Com *fake timers*, os segundos seguem crescendo do valor anterior. |
| TU-49 | `Cronometro` acumula pausa em `document.hidden` e retoma na volta | CA-13 | 30 s ocultos entram em `pausadoMs` e não em `decorrido`. |
| TU-50 | `atalhos-teclado` mapeia letras, Enter e setas | RF-07 (CA-04) | `B` marca a alternativa B, `Enter` confirma, `→` avança, `F` alterna Foco/Contexto. |

**Estratégia.** O valor está nas regras puras — `ConferenciaGabarito`, `TamanhoSerie`, `TempoSessao`, `MontadorSerie` — e nos serviços com dependências mockadas à mão, sem contexto Spring (`tests.md` §4). Mocamos apenas a fronteira: repositórios, `Clock`, `Random`, `EstatisticaQuestaoService` e a porta `TempoEstudoExterno`. `SerieDoCadernoService` é dublê nesta camada, porque pertence a F3. No frontend, `StudySessionStore` e `Cronometro` são testados com Vitest puro e *fake timers*; o HTTP passa sempre por `HttpTestingController`.

### Testes de integração

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | `V7__sessao_estudo.sql` aplica limpo sobre V1…V6 | — | Flyway conclui; as quatro tabelas, os índices e as duas *rules* existem e o Hibernate valida o esquema (`ddl-auto=validate`). |
| TI-02 | `resposta` recusa `update` | CA-15 | Após `update resposta set marcada = 'E'`, a linha continua idêntica — a *rule* descartou a escrita. |
| TI-03 | `resposta` recusa `delete` | CA-15 | Após `delete from resposta`, a contagem não muda. |
| TI-04 | `unique (sessao_id, questao_id)` impede resposta duplicada | CA-15 | A segunda inserção viola a restrição; a primeira permanece. |
| TI-05 | `unique (usuario_id, questao_id, tipo)` impede marcação duplicada | CA-11 | Duas marcações iguais colidem; tipos diferentes convivem. |
| TI-06 | `check` recusa tamanho fora de 10/30/50 | — | Insert com `tamanho = 7` falha no banco, além de falhar na aplicação. |
| TI-07 | Abertura de sessão devolve 201 com a série completa e sem gabarito | CA-03, CA-07 | O JSON tem `questoes[*].alternativas` e **nenhuma** chave `gabarito` ou `comentario`. |
| TI-08 | Abertura de sessão nunca inclui Rascunho nem Em revisão | CA-16 | Com acervo misto, os ids devolvidos são só os publicados; a asserção é sobre a série, não sobre o filtro. |
| TI-09 | Abertura com filtro sem correspondência responde 422 `SERIE_VAZIA` | CA-16 | `application/problem+json` com o `code` esperado. |
| TI-10 | Abertura com `tamanho: 7` responde 422 com `fieldErrors.tamanho` | — | Mensagem em pt-BR. |
| TI-11 | Resposta fora do Modo Prova devolve gabarito, comentário e estatística | CA-05 | 201 com os três campos; `resposta` ganha uma linha com os cinco dados de RN-02. |
| TI-12 | Estatística devolvida exclui as respostas do autor da questão | CA-05 | Com 3 respostas do autor e 1 de terceiro, `totalRespostas` é 1 (RN-03). |
| TI-13 | Troca de modo preserva questão, marcação e cronômetro | CA-06 | `PATCH` para `CONTEXTO` e de volta para `FOCO`: `indiceAtual`, `respostas` e `decorridoSegundos` (não decrescente) inalterados. |
| TI-14 | Retomada devolve a mesma série na primeira questão sem resposta | CA-02 | Os 40 ids voltam na mesma ordem e `indiceAtual` é 13. |
| TI-15 | Retomada de sessão de outro usuário responde 404 | CA-02 | Nunca 403 e nunca o corpo da sessão alheia. |
| TI-16 | Modo Prova não devolve gabarito em nenhuma etapa antes de finalizar | CA-07 | Abertura e navegação não expõem `gabarito`; `POST /respostas` responde 409. |
| TI-17 | Finalização do Modo Prova grava o lote e devolve a correção | CA-09 | 28 linhas em `resposta` numa transação; percentual, acertos, erros, não respondidas e tempo conferem. |
| TI-18 | Prova abandonada não deixa resposta alguma | CA-10 | Após a abertura e o abandono, `select count(*) from resposta where sessao_id = ?` é 0 e a sessão fica sem `finalizada_em`. |
| TI-19 | Sessão expirada some da retomada e não conta como finalizada | CA-10 | Depois do expirador, `GET /sessoes/{id}` responde 404 e a sessão não entra no tempo do dia. |
| TI-20 | Finalização publica `SessaoFinalizada` depois do commit | — | O ouvinte de teste recebe o evento uma vez, com a sessão já visível no banco. |
| TI-21 | Marcação persiste entre sessões | CA-11 | Marcada na sessão A, a questão volta com `erroRecorrente: true` na série da sessão B. |
| TI-22 | `GET /estudo/inicio` reflete o histórico real do dia | CA-01 | Resolvidas, percentual e tempo batem com as linhas gravadas; a sessão pendente aparece em `sessaoRetomavel`. |
| TI-23 | Tempo do dia soma questões e flashcards | CA-01, CA-13 | Com um dublê de `TempoEstudoExterno`, o total é a soma dos dois e o campo detalhado mostra a origem. |
| TI-24 | Endpoints exigem autenticação | — | Sem token, os sete respondem 401; com token de qualquer perfil, 2xx (a tela é a mesma para todos). |
| TI-25 | Série de 50 é servida em uma requisição dentro do orçamento | CA-14 | `POST /sessoes` com `tamanho: 50` responde abaixo de 300 ms no ambiente de teste e sem consulta N+1 (uma consulta para a série, uma para as alternativas). |

**Componentes em conjunto.** `*IntegracaoTest` com `@SpringBootTest @AutoConfigureMockMvc @Testcontainers` sobre `PostgreSQLContainer<>("postgres:18")` e datasource por `@DynamicPropertySource` — sem depender do `docker-compose`. `*ControllerTest` (`@WebMvcTest`) cobre status, formato e autenticação. Dados isolados por execução (e-mail e código de questão com `UUID.randomUUID()`), conforme `tests.md` §2 I.

**Massa de teste.** Um autor, um estudante, uma disciplina com dois assuntos e ao menos 60 questões publicadas — mais três não publicadas, que existem para provar CA-16 —, distribuídas entre `ME` e `CE`. A semente de `dev` (`V902__seed_sessao_estudo_dev.sql`) reproduz o mesmo cenário para uso manual, incluindo uma sessão interrompida na questão 14 de 40, que é o cartão "Retomar" do protótipo.

**Migrações de teste que saem daqui.** `db/migration-test/V990__resposta_minima.sql` existe hoje só porque `prd-banco-de-questoes` precisava de `resposta` sem que F2 existisse, e `prd-cadernos-e-desempenho` prevê o `V991` pelo mesmo motivo. **Quando esta entrega criar as tabelas definitivas, as duas migrações de teste são removidas na mesma alteração** e as suítes de F1 e F3 passam a rodar contra o esquema real.

### Testes E2E

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | Estudar em Foco, responder e ver o gabarito | CA-03, CA-04, CA-05 | Login → Início → nova sessão: a tela mostra só enunciado, alternativas, cronômetro e navegação; marcar A e depois C deixa só C; confirmar abre gabarito, comentário e taxa de acerto. |
| E2E-02 | Alternar para Contexto e voltar sem perder o lugar | CA-06, CA-13 | Na questão 7 com alternativa marcada, alternar ida e volta mantém a questão, a marcação e o cronômetro correndo (o valor lido nunca diminui). |
| E2E-03 | Modo Prova ponta a ponta | CA-07, CA-08, CA-09 | Série sem gabarito; voltar a uma questão respondida e trocar a resposta; finalizar abre a correção com percentual, acertos, erros, não respondidas, tempo total, revisão item a item e as ações "refazer" e "voltar ao início". |
| E2E-04 | Prova abandonada não entra no histórico | CA-10 | Abrir prova, responder 5, fechar a aba, voltar ao Início: o resumo do dia não conta aquelas 5 e o cartão "Retomar" não oferece a prova. |
| E2E-05 | Marcar, sair e voltar | CA-11 | Marcar como erro recorrente, encerrar a sessão, abrir nova sessão com a mesma questão: a marcação continua ativa. |
| E2E-06 | Criar flashcard sem perder a sessão | RF-28 (regra 8) | Abrir o diálogo na questão 12, salvar, fechar: mesma questão, mesma alternativa marcada, cronômetro sem salto e contador do botão incrementado. |
| E2E-07 | Continuar de onde parou | CA-02, CA-12 | Interromper na questão 14 de 40, voltar ao Início, acionar "continuar": a série é a mesma e abre na 14; escolher o número 12 na navegação volta para ela com a resposta preservada. |
| E2E-08 | Operação por teclado e foco visível | Experiência do usuário (PRD) | Percorrer uma questão inteira sem mouse: seleção, confirmação, avanço e troca de modo; o foco é sempre visível e o gabarito é anunciado por `aria-live`. |

Playwright em `frontend/e2e/*.spec.ts`, um cenário por fluxo, contra a API real subida em perfil de teste. O contraste mínimo de 4,5:1 e a ausência de salto de layout são verificados em E2E-08 com `@axe-core/playwright` e comparação de *bounding box* entre questões consecutivas.

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Migração e persistência** — `V7__sessao_estudo.sql`, a semente de `dev`, as quatro entidades, os enums e os repositórios, com TI-01…TI-06. Primeiro porque a imutabilidade de RN-02 é uma propriedade do esquema, e tudo o que vem depois se apoia nela. Aqui também caem `RespostaEstatisticaRepository` (renomeação) e a coluna `tempo_medio_ms`.
2. **Regras puras** — `ConferenciaGabarito`, `TamanhoSerie`, `TempoSessao`, `FiltroSerie` e `MontadorSerie`, com TU-01…TU-10 e TU-31…TU-33. São rápidas, não dependem de Spring e definem o vocabulário do resto.
3. **Serviços de sessão** — `SessaoEstudoService`, `RegistroRespostaService`, `CorrecaoProvaService` e `MarcacaoService`, com TU-11…TU-30. É onde moram CA-02, CA-05, CA-06, CA-07, CA-09 e CA-11.
4. **Início e expiração** — `ResumoDiaService`, a porta `TempoEstudoExterno` e o `ExpiradorSessoes`, com TU-34…TU-40.
5. **Controllers e integração do backend** — os sete endpoints, o evento `SessaoFinalizada` e TI-07…TI-25. Só depois do domínio, conforme o `AGENTS.md`.
6. **Ajustes no protótipo** — as nove peças listadas em *Riscos conhecidos* entram no `Certame.dc.html` **antes** de qualquer componente Angular novo (regra 1).
7. **Frontend** — `estudo-api`, `StudySessionStore`, `Cronometro`, os componentes da tela de estudo, a correção e o Início, com TU-41…TU-50.
8. **E2E e acessibilidade** — E2E-01…E2E-08, mais a medição de CA-14 sobre a build de produção.

Cada passo entra com seus testes no mesmo commit (`tests.md` §2 T).

### Dependências técnicas

- **Docker** rodando, para os Testcontainers dos passos 1 e 5.
- **`prd-banco-de-questoes` implementado** — está: `questao`, `alternativa`, `questao_estatistica` e `catalogo` já existem no código, com massa em `V901`.
- **`SerieDoCadernoService` (F3)** só é necessário para `origem = CADERNO`. Enquanto F3 não existir, essa origem responde `CADERNO_NAO_ENCONTRADO` e a interface não a oferece; `origem = LIVRE` cobre todos os critérios de aceitação deste PRD. **Não é bloqueador.**
- **`TempoEstudoFlashcards` (F6b)** entra pela porta `TempoEstudoExterno` quando existir. Até lá, `tempoFlashcardsSegundos` é 0 e CA-01 é verificável assim mesmo. **Não é bloqueador.**
- **Numeração de migração:** `V7` é o próximo livre em `db/migration` e `V902` em `db/migration-dev`. `prd-cadernos-e-desempenho` supôs `V4`/`V902` e `prd-editais`/`prd-flashcards` supuseram `V7`/`V8`/`V9`; os números são reconferidos na entrega de cada uma, sem jamais editar migração aplicada.
- **Protótipo:** o passo 6 depende de aprovação de design para as nove peças. É a única dependência externa ao código.

## Monitoramento e observabilidade

Sobre a infraestrutura que já existe — Actuator, Micrometer e o padrão de `Timer` inaugurado por `BuscaQuestaoService` (`certame.questao.busca`).

| Instrumento | Nome | Para quê |
| --- | --- | --- |
| `Timer` | `certame.estudo.abertura` (tags `modo`, `tamanho`, `origem`) | CA-14: o p95 da abertura é o custo real do sorteio e da montagem do payload. Percentis 0,95 e 0,99 publicados. |
| `Timer` | `certame.estudo.resposta` (tag `modo`) | Latência de gravação e do recálculo síncrono da estatística. |
| `Timer` | `certame.estudo.finalizacao` (tags `modo`, `tamanho`) | Custo da transação em lote do Modo Prova, que cresce com o tamanho da série. |
| `Counter` | `certame.estudo.serie_vazia` (tag `origem`) | Quantas sessões o filtro impede de nascer — sinal de acervo insuficiente, não de defeito. |
| `Counter` | `certame.estudo.sessao_expirada` (tag `modo`) | Abandono. Em `PROVA` é a métrica de CA-10 vista de fora. |
| `Gauge` | `certame.estudo.sessoes_ativas` | Sessões abertas no momento; base para dimensionar o pool. |

**Log** em JSON com `traceId`, como o resto da aplicação:

| Evento | Nível | Conteúdo |
| --- | --- | --- |
| Abertura e finalização de sessão | `INFO` | `sessaoId`, `usuarioId`, `modo`, `tamanho`, `origem`. Nunca o conteúdo da questão. |
| Série vazia | `INFO` | Filtro aplicado. É informação de produto, não erro. |
| `SESSAO_ENCERRADA`, `MODO_NAO_ALTERAVEL`, `RESPOSTA_DUPLICADA` | `WARN` | Sinalizam cliente fora de sincronia; um pico indica defeito na tela. |
| Falha do recálculo de estatística após o commit | `WARN` | A resposta do usuário já está gravada; o agregado se corrige na próxima. Nunca `ERROR`. |
| Tentativa de acesso a sessão de outro usuário | `WARN` | `usuarioId` e `sessaoId`; é sinal de segurança. |
| Passagem do `ExpiradorSessoes` | `INFO` | Quantas sessões expiraram por modo. |

Nenhum log carrega enunciado, alternativa, gabarito ou resposta do estudante: o histórico é dado pessoal (LGPD, restrições do PRD) e não tem por que viver também no arquivo de log.

## Considerações técnicas

### Principais decisões

- **Série inteira no payload da abertura, sem gabarito** (decisão do usuário, com a quantidade escolhida pelo estudante). Resolve CA-14 e CA-07 com o mesmo movimento: navegar não toca a rede, e o gabarito não está no cliente para vazar. O custo é um payload de 60–120 KB para 30 questões e a impossibilidade de trocar a série no meio — nenhum dos dois pesa contra a promessa de fluidez do PRD. A alternativa de blocos de dez foi descartada porque põe uma chance de espera exatamente no gesto que o PRD quer proteger.
- **Quantidade de questões escolhida pelo estudante (10, 30 ou 50)** (decisão do usuário). O conjunto é fechado: um campo livre convidaria a séries de 500 e destruiria o orçamento de latência. Como o protótipo não tem o campo, ele entra no `Certame.dc.html` no passo 6.
- **A série é gravada em `sessao_questao`** (decisão do usuário). CA-02 promete retomar *a mesma* série; guardar só os filtros devolveria outra. Custa uma tabela de três colunas e uma escrita em lote na abertura, e paga com a navegação por número (CA-12) e a correção item a item (CA-09) funcionando sobre a mesma ordem.
- **Janela de retomada de sete dias** (decisão do usuário), contra as 24 h que a §7 da `TECH-SPEC.md` prevê para o abandono do Modo Prova. Os dois prazos coexistem porque significam coisas diferentes: sete dias é quanto tempo uma leitura interrompida continua valendo; 24 h é quanto tempo uma tentativa de prova fica pendurada antes de ser considerada abandonada. O preço da janela maior é a questão despublicada no intervalo, tratada na retomada por descarte e reindexação.
- **O Modo Prova é escolhido na abertura e não aceita troca** (decisão do usuário). É o que torna CA-07 estrutural: a série de uma prova nunca teve gabarito no cliente, e não há caminho em que uma sessão de leitura vire prova carregando os gabaritos que já recebeu. O gesto do protótipo — clicar em "Modo prova" no meio da série — vira "Iniciar prova", que abre uma sessão nova.
- **Tempo do dia é duração de sessão menos pausas** (decisão do usuário). É o número que o cronômetro mostra, então o Início não contradiz a tela de estudo. `pausado_ms` é um acumulador monotônico enviado pelo cliente nas escritas que já existem — não inventamos *heartbeat*, e sessão abandonada não conta porque só entram no total as sessões com atividade no dia, medidas até `ultima_atividade_em`.
- **Imutabilidade de `resposta` no banco, por `rule`** e não só por ausência de *setter*. Uma consulta futura, um `saveAll` distraído ou um script de manutenção não conseguem alterar histórico. `do instead nothing` em vez de `raise` porque a promessa é sobre o estado da linha, não sobre punir quem tentou.
- **Recálculo de estatística síncrono fora da prova, assíncrono no lote.** CA-05 quer que o painel mostre o número **incluindo** a resposta recém-dada; uma agregação indexada sobre `resposta(questao_id)` cabe no orçamento. Cinquenta agregações na finalização não cabem, e ali ninguém está olhando estatística — a correção mostra gabarito.
- **Um único ponto de escrita em `resposta` por modo.** Fora da prova, `POST /respostas`; na prova, `POST /finalizar`. Finalizar com lote fora da prova é 422 em vez de "também funciona": dois caminhos de escrita para o mesmo registro imutável é como se perde uma garantia sem perceber.
- **`RespostaRepository` (`JdbcTemplate`) passa a se chamar `RespostaEstatisticaRepository`.** O nome de domínio pertence ao repositório JPA que nasce aqui; o antigo continua sendo a porta de agregação de RN-03 usada por `EstatisticaQuestaoService`, com o nome dizendo o que ele é. São duas linhas de `import` alteradas em uma classe.
- **Enums como texto com `check`, UUID como PK, `timestamptz` em UTC, exclusão sem apagar linha** — o padrão já estabelecido em `V1` e `V2`, seguido sem desvio.

### Riscos conhecidos

| Risco | Mitigação |
| --- | --- |
| **O protótipo diverge do PRD em nove pontos.** (1) não há campo de quantidade da série; (2) só existe um botão de marcação genérico, e RF-11 pede favorita **e** erro recorrente; (3) a navegação numerada só aparece no painel de Contexto, mas CA-03 a exige também no Foco e CA-12 a exige em qualquer modo; (4) as tags de disciplina, banca, ano e dificuldade aparecem no Foco, o que CA-03 proíbe; (5) não há estado para série vazia; (6) o Início mostra "Sequência" e "Ranking", que não têm requisito, e **não** mostra tempo de estudo, que RF-06 exige; (7) o cartão "Retomar" traz um simulado, que é de outro PRD; (8) o painel de gabarito tem "Minhas anotações", sem RF correspondente; (9) o seletor de modo permite entrar na prova no meio da série. | Passo 6 do sequenciamento: as peças 1–5 entram no `Certame.dc.html` como adição; 6 e 7 são correções (um card vira "Tempo de estudo", o cartão de simulado sai ou fica atrás de F5); 8 sai do protótipo, por não ter requisito; 9 vira "Iniciar prova". Nada disso é implementado em Angular antes de estar aprovado no protótipo (`AGENTS.md`, regra 1). |
| **`order by random()` numa tabela grande degrada.** Com 200 mil questões, o sorteio varre o conjunto filtrado inteiro. | Hoje o acervo é pequeno e o índice `questao_filtro_idx` sustenta o filtro; `certame.estudo.abertura` mostra a curva antes que ela vire problema. Se o p95 subir, a troca é para amostragem por `tablesample` ou por deslocamento aleatório sobre a contagem — mudança contida em `SerieRepository`, sem efeito no contrato. |
| **O payload de 50 questões em redes ruins.** 300 ms medidos na navegação não valem se a abertura demorar 4 s. | A abertura é um gesto deliberado, e a tela mostra progresso; a resposta é comprimida (`gzip`) e o `enunciado` é o único campo pesado. TI-25 mede o pior caso; se o número não fechar, a saída é entregar as 10 primeiras e completar em segundo plano — desenho já avaliado e adiado, não descartado. |
| **`pausado_ms` vem do cliente e pode ser adulterado.** Um cliente malicioso inflaria as pausas para exibir menos tempo de estudo. | O valor só sobe (`acumular` é monotônico) e nunca ultrapassa a janela da sessão (`duracaoSegundos` satura em 0). O dado afeta apenas a exibição do próprio usuário — não há ranking nem prêmio ligado ao tempo em v1. Registrado, não mitigado além disso. |
| **Sete dias de retomada elevam a chance de questão despublicada na série.** | Tratado no contrato como degradação declarada (`questoesRemovidas`), coberto por TU-16, e informado uma vez na tela em vez de silenciosamente. |
| **F3 e F6b consomem contratos definidos aqui** (`SessaoFinalizada`, `FonteRespostas`, `marcacao`, `TempoEstudoExterno`). Divergir depois quebraria duas TechSpecs já escritas. | As assinaturas foram copiadas de `prd-cadernos-e-desempenho` e `prd-flashcards-decks-e-revisao` sem alteração, e estão em *Pontos de integração* para conferência na revisão. |
| **A remoção de `V990`/`V991` mexe em suítes de outras funcionalidades.** | Sai no mesmo commit da migração `V7`, e `./mvnw verify` da suíte inteira é o critério de aceite do passo 1. |
| **Cobertura do frontend com componentes de tela grande.** A tela de estudo tende a concentrar lógica e furar o limite de 100 linhas por arquivo. | A lógica vive em `StudySessionStore`, `Cronometro` e `atalhos-teclado`, que são testáveis sem DOM; os componentes ficam declarativos. É também o que mantém a regra 2.2 do `code-standards.md`. |

### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/tests.md` e `.agents/rules/folder-structure.md` foram lidos na íntegra antes desta especificação.

| Regra | Como esta especificação a cumpre |
| --- | --- |
| 1 — Frontend é tradução 1:1 do protótipo | Nenhum componente Angular é escrito antes das nove correções do passo 6. Só tokens `var(--*)` de `styles.css`; nenhuma biblioteca de UI de terceiros; as classes (`card`, `seg`, `seg-opt`, `tag`, `btn`, `field`, `input`) são as do protótipo. |
| 2 — Autorização sempre no servidor | Os sete endpoints exigem autenticação; `@PreAuthorize("isAuthenticated()")` nos serviços; toda consulta leva o `usuarioId` do token e responde **404** — nunca 403 — para recurso de terceiro. `MODO_NAO_ALTERAVEL` é recusado no servidor mesmo com a interface já escondendo a ação. |
| 3 — Interface em pt-BR, código sem acento | Mensagens completas e com ponto final ("Escolha 10, 30 ou 50 questões."); identificadores, tabelas e colunas em português sem acento (`sessao_estudo`, `pausado_ms`, `MontadorSerie`). `code`, `message` e `fieldErrors` seguem em inglês, como já é o padrão. |
| 4 — Nenhum endpoint fora do contrato | Os quatro endpoints novos (`GET /estudo/inicio`, `GET /estudo/sessoes/{id}`, `PATCH .../modo`, `PUT /estudo/questoes/{id}/marcacoes`) entram na `TECH-SPEC.md` §6 na mesma alteração, junto com os três já previstos. |
| 5 — Migração só por Flyway, versionada e imutável | `V7__sessao_estudo.sql` em `db/migration`, aditiva; semente de `dev` isolada em `V902`; as migrações de teste `V990`/`V991` são removidas no mesmo commit. Nenhuma migração aplicada é editada. |
| 6 — `resposta` imutável (RN-02); prova só conta após finalizar (RN-04) | Entidade sem *setter*, sem `@PreUpdate`; `rule` de `update`/`delete` no banco; único caminho de escrita por modo. No Modo Prova nada é gravado antes de `POST /finalizar` — CA-10 é verdadeiro por ausência de escrita. |
| 8 — Criação de flashcard é diálogo, não rota | `StudySessionStore` e `Cronometro` são `providedIn: 'root'`; a abertura do diálogo não navega e não recria estado. Coberto por E2E-06. |
| `code-standards.md` §2 | Sem comentário supérfluo (só os dois blocos de "por quê" já justificados no SQL); arquivos ≤ 100 linhas e métodos ≤ 30 — daí `MontadorSerie`, `ConferenciaGabarito`, `TempoSessao` e `TamanhoSerie` serem classes próprias em vez de métodos privados de um serviço grande; até 3 parâmetros, com `FiltroSerie` e `RequisicaoSessao` como objetos parâmetro; sem literal mágico (7 dias, 24 h, 10/30/50 e os limites de tempo são constantes nomeadas); nenhum segredo no código. |
| `code-standards.md` §3 | Pacote por domínio (`estudo/`, com `inicio/` e `dto/`); controller só orquestra; regra e `@Transactional` no serviço; DTOs em `record` com Bean Validation e mensagem em português; entidades sem Lombok, com construtor protegido e `@Enumerated(EnumType.STRING)`; injeção por construtor com campos `private final`; erros por `ErroNegocioException` e o `@RestControllerAdvice` único, que não muda. |
| `code-standards.md` §4 | Componentes standalone, *zoneless*, estado em `signal`/`computed`, `input()`/`output()` como funções, template e estilo em arquivos separados, HTTP concentrado em `core/api`, 422 mapeado campo a campo. |
| `tests.md` | Piso de **80%** (que prevalece sobre os 70% da `TECH-SPEC.md` §9), FIRST com `Clock` e `Random` injetados, AAA/Given-When-Then, nomes `TU`/`TI` + id + comportamento em português sem acento, pirâmide ~70/20/10, dublês só na fronteira. As três áreas de risco alto — RN-02, RN-04 e CA-07 — recebem caminho feliz **e** bordas. |
| `folder-structure.md` | Backend em `br/com/certame/estudo/` (pasta já prevista como alvo), DTOs em `estudo/dto/`, exceções em `comum/excecao/`, migração em `db/migration/`, testes espelhando o pacote. Frontend em `features/study/` e `features/dashboard/`, clientes em `core/api/`. Nenhuma pasta fora do mapa. |

### Conformidade com skills

| Skill | Aplicável | Como é seguida |
| --- | --- | --- |
| `postgresql-table-design` | sim | PKs `uuid` com `gen_random_uuid()`, `timestamptz` em UTC, enums como `text` + `check`, chave natural composta em `sessao_questao` (sem id sintético inútil), índices parciais para o caminho quente (`sessao_estudo_retomavel_idx`), restrições de unicidade fazendo o trabalho que a aplicação não deve ter de fazer sozinha (`resposta`, `marcacao`) e `rules` para a imutabilidade de RN-02. |
| `313-frameworks-spring-db-migrations-flyway` | sim | SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração real (V7); `validate-on-migrate` ligado; disciplina *forward-only*; `spring.flyway.locations` separando produção, `dev` e teste; migração aditiva e retrocompatível (*expand/contract*) — a coluna `tempo_medio_ms` entra anulável. |
| `spring-boot-testing` | sim | *Slices* em vez de `@SpringBootTest` sempre que possível (`@WebMvcTest` para os controllers, `@DataJpaTest` para a persistência), AssertJ em toda asserção, `@MockitoBean` no *slice* web, Testcontainers com PostgreSQL 18 e reuso de contexto para não pagar o *bootstrap* a cada classe. |
| `angular-developer` | sim | Standalone e *zoneless*, `signal`/`computed`/`linkedSignal` para o estado da série, `input()`/`output()` como funções, `httpResource`/`resource` **não** usados na sessão (a série é carregada uma vez e vive no store, não num recurso reativo), roteamento com `authGuard`, e as convenções de nome de arquivo do Angular 22. Acessibilidade conforme `angular-aria.md`: `role="radiogroup"` nas alternativas, `aria-live="polite"` no painel de gabarito, foco visível e navegação completa por teclado. |
| `impeccable`, `find-skills` | não | Sem desvio a registrar: o design não é criado aqui, é traduzido do protótipo aprovado. |

### Arquivos relevantes e dependentes

**Novos — backend:**
`estudo/SessaoEstudo.java` · `estudo/SessaoQuestao.java` · `estudo/SessaoQuestaoId.java` · `estudo/Resposta.java` · `estudo/Marcacao.java` · `estudo/TipoMarcacao.java` · `estudo/ModoSessao.java` · `estudo/OrigemSessao.java` · `estudo/TamanhoSerie.java` · `estudo/FiltroSerie.java` · `estudo/MontadorSerie.java` · `estudo/ConferenciaGabarito.java` · `estudo/TempoSessao.java` · `estudo/SerieRepository.java` · `estudo/SessaoEstudoRepository.java` · `estudo/SessaoQuestaoRepository.java` · `estudo/RespostaRepository.java` · `estudo/MarcacaoRepository.java` · `estudo/SessaoEstudoService.java` · `estudo/RegistroRespostaService.java` · `estudo/CorrecaoProvaService.java` · `estudo/MarcacaoService.java` · `estudo/ExpiradorSessoes.java` · `estudo/SessaoEstudoController.java` · `estudo/inicio/ResumoDiaService.java` · `estudo/inicio/TempoEstudoExterno.java` · `estudo/inicio/InicioController.java` · `estudo/dto/` (14 records) · `comum/evento/SessaoFinalizada.java` · `comum/ZonaAplicacao.java` · `comum/excecao/SerieVaziaException.java` · `comum/excecao/SessaoEncerradaException.java` · `comum/excecao/ModoNaoAlteravelException.java` · `comum/excecao/RespostaDuplicadaException.java` · `backend/src/main/resources/db/migration/V6__sessao_estudo.sql` · `backend/src/main/resources/db/migration-dev/V902__seed_sessao_estudo_dev.sql`

**Novos — frontend:**
`core/api/estudo-api.ts` · `core/api/modelos-estudo.ts` · `features/study/study-session-store.ts` · `features/study/cronometro.ts` · `features/study/atalhos-teclado.ts` · `features/study/estudar-page/` · `features/study/painel-filtros/` · `features/study/seletor-modo/` · `features/study/questao-cartao/` · `features/study/barra-acao/` · `features/study/gabarito-painel/` · `features/study/painel-navegacao/` · `features/study/correcao-prova/` · `frontend/e2e/estudo.spec.ts`

**Modificados:**
`estudo/RespostaRepository.java` → `estudo/RespostaEstatisticaRepository.java` (renomeação) · `questao/EstatisticaQuestaoService.java` (novo nome do repositório e `tempo_medio_ms`) · `questao/QuestaoEstatistica.java` (coluna `tempoMedioMs`) · `questao/dto/EstatisticaQuestao.java` (campo novo) · `frontend/src/app/features/dashboard/inicio-page.ts` (deixa de ser stub, ganha `.html`/`.scss`/`.spec.ts`) · `frontend/src/app/layout/shell/shell.{ts,html}` (item de navegação e cronômetro na topbar) · `frontend/src/app/app.routes.ts` (rota `/estudar`) · `protótipo/Certame.dc.html` (as nove peças do passo 6) · `TECH-SPEC.md` §6 (os quatro endpoints novos)

**Removidos:**
`backend/src/test/resources/db/migration-test/V990__resposta_minima.sql` · `backend/src/test/resources/db/migration-test/V991__sessao_resposta_marcacao_minima.sql` (se já existir quando esta entrega chegar)

**Reutilizados:**
`config/SegurancaConfig.java` · `usuario/Usuario.java` · `usuario/PerfilUsuario.java` · `comum/ManipuladorErrosGlobal.java` · `comum/ErroApi.java` · `comum/excecao/RecursoNaoEncontradoException.java` · `comum/excecao/DadosInvalidosException.java` · `comum/excecao/ConflitoException.java` · `comum/PaginaResposta.java` · `questao/Questao.java` · `questao/Alternativa.java` · `questao/TipoQuestao.java` · `questao/StatusQuestao.java` · `questao/QuestaoRepository.java` · `catalogo/Disciplina.java` · `catalogo/Assunto.java` · `catalogo/Banca.java` · `catalogo/Orgao.java` · `backend/src/main/resources/db/migration/V1__base_acesso.sql` · `backend/src/main/resources/db/migration/V2__banco_questoes.sql` · `frontend/src/app/core/auth/auth-guard.ts` · `frontend/src/app/core/auth/auth-interceptor.ts` · `frontend/src/app/core/api/erro-api.ts` · `frontend/src/app/shared/ui/confirmacao-dialog/` · `frontend/src/styles.css`
