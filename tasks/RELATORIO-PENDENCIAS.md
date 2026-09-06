# Relatório de pendências — o que falta para o sistema ficar em conformidade

- **Data:** 2026-09-05
- **Origem:** revisão dos 9 pares `codereview.md` / `qa.md` em `tasks/prd-*/`, confrontados com o
  estado real do repositório nesta data.
- **Escopo deste documento:** descreve **o que** precisava ser feito e **por quê**. As pendências
  resolvidas são mantidas como histórico e remetem às tarefas de correção correspondentes.

---

## 1. Como o estado atual foi medido

| Verificação | Comando | Resultado em 2026-09-05 |
|---|---|---|
| Suíte de backend + gate de cobertura | `cd backend && ./mvnw verify` | **545 testes, 0 falhas, 0 erros → BUILD SUCCESS; `jacoco:check` aprovado** |
| Suíte de frontend + cobertura | `cd frontend && npm run test:coverage` | **427 testes em 80 arquivos, 0 falhas** · Statements 89,47% · Branches 85,27% · Functions 80,27% · Lines 93,83% |
| Formatação do frontend | `npx prettier --check` | **191 arquivos** fora do padrão |
| Migrações aplicadas | `db/migration` + `db/migration-dev` | `V1`–`V16` e `V900`–`V909`; `db/migration-test/` **vazio** |
| Suítes E2E | Playwright | **não executadas** nesta revisão |

Como referência para os relatórios antigos: quase todos citam **362 testes de frontend em 71
arquivos** e cobertura de **87,49%**, além de um `./mvnw verify` verde com **509** ou **515** testes.
Nenhum desses números descreve o repositório hoje. Todos os `codereview.md` e `qa.md` foram
atualizados com a medição vigente.

---

## 2. Bloqueadores — impedem verificar a qualidade de todo o repositório

### P0-1 · O build do backend — resolvido em 2026-09-05

**O que acontece.** `FlywayMigracaoTest.TI06_tamanhoDeSessaoForaDoDominioEhRecusado` afirma que
`sessao_estudo.tamanho = 40` deve ser recusado para sessão de origem `LIVRE`. A migração
`V15__sessao_livre_tamanho_variavel.sql` — criada deliberadamente por `prd-sessao-de-estudo` para
corrigir um defeito real de produção (sessão com acervo menor que o tier pedido quebrava ao salvar)
— passou a aceitar `tamanho between 1 and 50` para `LIVRE`. **O teste ficou desatualizado, a
migração está correta.**

**Resultado.** O fixture de `PublicacaoServiceTest` também foi alinhado à regra vigente de
disciplina obrigatória. O `verify` completo voltou a ficar verde: 545 testes, 0 falhas, 0 erros,
`jacoco:check` aprovado e `BUILD SUCCESS`.

**Correção aplicada.** O teste foi alinhado ao contrato vigente do domínio — o que `V15` passou a
permitir e o que continua sendo recusado para cada origem (`LIVRE`, `CADERNO`, `SIMULADO`). A
migração aplicada permaneceu imutável (`AGENTS.md` regra 5).

**Dono:** `prd-sessao-de-estudo` (já encaminhado pelo QA de `prd-simulados` como BUG-01, sem ação).

---

## 3. Conformidade funcional — requisitos aceitos mas não entregues

### P1-1 · CA-11 de flashcards não funciona em produção: o tempo de revisão não entra no total do dia

**O que acontece.** `RF-06`/`CA-11` exige que o tempo das sessões de revisão de flashcards some ao
tempo de estudo exibido no Início. Existem as duas pontas:
`br.com.certame.estudo.inicio.TempoEstudoExterno` (porta consumida por `ResumoDiaService`) e
`br.com.certame.flashcard.revisao.TempoEstudoFlashcards` (que calcula exatamente esse valor, com a
**mesma assinatura de método**). **Falta o vínculo entre elas:** a classe de flashcards não declara
implementar a porta, e nenhuma outra classe de `src/main` a implementa. Como `ResumoDiaService`
recebe a dependência como `@Nullable`, em produção ela é injetada como `null` e o ramo simplesmente
não roda.

**Por que passou despercebido.** `TI-29` testa a porta isolada e passa. O E2E que provaria a
integração (`E2E-07`) está com `test.skip` sob a justificativa de que "a tela de Início de
`prd-sessao-de-estudo` ainda não existe" — **essa tela existe** (`InicioPage`, rota `inicio`,
`GET /estudo/inicio`). A justificativa do skip ficou obsoleta e escondeu a lacuna real.

**O que precisa ser feito.** Ligar a porta ao adaptador, remover o `test.skip` de `E2E-07` e provar
a soma ponta a ponta. É trabalho **dentro** do escopo de `prd-flashcards-decks-e-revisao`, não uma
dependência externa.

**Dono:** `prd-flashcards-decks-e-revisao`. CA-11 foi rebaixado para **não atendido** no `qa.md`
daquela funcionalidade.

### P1-2 · CA-11 de gestão de usuários: anonimização do histórico **resolvida em 2026-09-05**

**Solução aplicada.** A decisão de produto definiu placeholders para `usuario.nome`, `usuario.email`,
conteúdos textuais próprios e payloads de auditoria, sete dias após a exclusão lógica. A migração
`V16__anonimizacao_historico_usuario.sql` adicionou `usuario.anonimizado_em` e índice parcial; a
rotina agendada preserva os UUIDs, as linhas imutáveis de `resposta` e os agregados de desempenho.
O teste `TI08_anonimizaHistoricoAposSeteDiasPreservandoAgregados` cobre a regra e
`TI09_naoAnonimizaHistoricoAntesDoPrazo` cobre o limite de retenção.

**Dono:** `prd-gestao-de-usuarios`.

### P1-3 · Publicar questão sem disciplina — resolvido

**Correção aplicada.** `ValidadorPublicacao` verifica disciplina como quarta pendência e
`ManipuladorErrosGlobal` trata violações de integridade com envelope 422 seguro. O fixture do teste
de publicação válida foi atualizado para fornecer a disciplina exigida.

**Dono:** `prd-banco-de-questoes`.

---

## 4. Verificação — o que os relatórios afirmam e o repositório não sustenta

### P2-1 · Dois cenários E2E estão desligados por motivos que já não existem

| Cenário | Motivo registrado no `test.skip` | Situação real |
|---|---|---|
| `e2e/estudo.spec.ts:98` — **E2E-06** (criar flashcard sem perder a sessão, RF-28) | "o diálogo de flashcard ainda não existe no repositório" | **Falso.** `FlashcardDialog` existe e está integrado em `estudar-page.ts`. O comportamento é coberto por `e2e/flashcards.spec.ts`, mas esta suíte segue mentindo sobre o motivo |
| `e2e/flashcards-decks-revisao.spec.ts:193` — **E2E-07** (tempo da sessão no total do dia) | "depende da tela de Início, ainda inexistente" | **Falso.** A tela existe; o que falta é o adaptador descrito em **P1-1** |

Um terceiro skip, `e2e/flashcards.spec.ts:250` (**E2E-13**, abrir a questão de origem), **continua
legítimo**: verificado que `estudar-page` lê apenas os *query params* `sessao`, `modo`, `simulado` e
`tentativa` — a rota `/estudar?questao={id}` de fato não existe.

**O que precisa ser feito.** Remover ou reescrever os dois skips obsoletos. Um `skip` cujo motivo
não é mais verdadeiro é pior que um teste ausente: ele dá a impressão de cobertura consciente.

### P2-2 · Nenhum cenário E2E de editais foi jamais executado

O `qa.md` de `prd-editais` marca os 12 cenários como **PASSOU**, mas a nota da própria seção admite
que a suíte Playwright não foi executada e que a verificação foi feita "por correspondência 1:1"
entre o arquivo de teste e o protótipo. Inspeção de código não é execução. A tabela foi corrigida
para **"NÃO EXECUTADO"**; falta rodar `npx playwright test e2e/editais.spec.ts`.

Registre-se que o motivo alegado para não executar — o caminho com acentos (`Área de trabalho`)
quebrando os testes de *slice* — **não se reproduz mais**: `EditalControllerTest` e
`EditalAdminControllerTest` passam no diretório original.

### P2-3 · Lacunas de cobertura que continuam abertas

| Lacuna | Funcionalidade | O que falta ser provado |
|---|---|---|
| **TI-25 — `ReconciliadorAgregados`** | cadernos-e-desempenho | Nenhum teste do repositório menciona a classe. O reconciliador de 15 minutos é a rede de segurança dos agregados de desempenho e não tem nenhuma prova automatizada de que reconstrói um agregado divergente e limpa a pendência |
| **Autorização no *slice* web de questão/catálogo** | banco-de-questoes | Não existe nenhum `*ControllerTest`/`*IntegracaoTest` em `questao/` ou `catalogo/`. Os testes de serviço são unitários com *mocks* e **não passam pelo proxy do `@PreAuthorize`** — o 403 por perfil insuficiente em publicar/despublicar e no CRUD de bancas/órgãos só é provado hoje pelo E2E |
| **Contrato HTTP de `/cadernos` e `/desempenho`** | cadernos-e-desempenho | Não há *slice* web para `CadernoController`/`DesempenhoController`: nenhum teste sobe a cadeia de segurança para provar 401 sem token nem o formato do envelope de erro 409/422 |
| **Páginas administrativas sem teste de unidade** | banco-de-questoes | `materias-page`, `catalogo-page` e `questoes-page` não têm `*.spec.ts`. A orquestração de diálogos, o tratamento de erro por `code` e a troca de aba ficam sem prova, embora a cobertura agregada passe |
| **Caminho de falha de `EnviadorEmailSmtp`** | acesso-e-conta | Não existe `src/test/java/br/com/certame/email/`. O laço de retentativa que sustenta a garantia de "e-mail nunca falha visivelmente" (RF-03/RF-04) nunca é exercitado |
| **Piso de 80% de decks/revisão** | flashcards-decks-e-revisao | O comando registrado no `codereview.md` era `./mvnw verify -DskipTests`, que **pula os testes e não gera relatório JaCoCo**. O gate nunca foi verificado para este domínio |

---

## 5. Padrões de código — desvios acumulados

### P3-1 · Formatação: 191 arquivos do frontend fora do Prettier

`npx prettier --check` falha em 191 arquivos, incluindo `e2e/`, `shared/ui/` e praticamente todas as
`features/` — **exceto** `features/study` e `features/dashboard`, que foram formatados durante as
correções de `prd-sessao-de-estudo` e estão limpos. Isso confirma o diagnóstico já registrado no
`codereview.md` de flashcards: não é desvio de uma funcionalidade, é ausência de um portão no
processo. **Precisa de decisão:** formatar de uma vez e adicionar `prettier --check` como gate
(CI ou *pre-commit*), ou registrar formalmente que a regra não vale.

### P3-2 · Dezesseis arquivos de backend acima do limite de 100 linhas

`code-standards.md` §2.2 impõe 100 linhas por arquivo. Situação atual:

| Arquivo | Linhas | Funcionalidade |
|---|---|---|
| `flashcard/Flashcard.java` | 199 | flashcards-criacao |
| `usuario/GestaoUsuariosService.java` | 190 | gestao-de-usuarios |
| `flashcard/FlashcardService.java` | 134 | flashcards-criacao |
| `questao/PublicacaoService.java` | 133 | banco-de-questoes |
| `catalogo/TaxonomiaService.java` | 130 | banco-de-questoes |
| `questao/QuestaoService.java` | 127 | banco-de-questoes |
| `simulado/SimuladoAdminService.java` | 125 | simulados |
| `auth/RedefinicaoSenhaService.java` | 121 | acesso-e-conta / gestao-de-usuarios |
| `flashcard/FlashcardRepository.java` | 119 | flashcards-criacao |
| `flashcard/revisao/FlashcardRevisao.java` | 116 | flashcards-decks-revisao |
| `estudo/SessaoEstudoService.java` | 111 | sessao-de-estudo |
| `usuario/UsuarioAdminController.java` | 108 | gestao-de-usuarios |
| `flashcard/revisao/SessaoRevisao.java` | 106 | flashcards-decks-revisao |
| `flashcard/revisao/RegistroRevisaoService.java` | 106 | flashcards-decks-revisao |
| `flashcard/revisao/SessaoRevisaoService.java` | 103 | flashcards-decks-revisao |
| `estudo/caderno/CadernoService.java` | 103 | cadernos-e-desempenho |

**Precisa de decisão antes de trabalho.** Vários relatórios apontam que o limite vem sendo
"cumprido" comprimindo várias instruções por linha física — o que satisfaz a letra da regra e
contraria o objetivo dela. Ou o limite passa a valer de fato (e esses arquivos são divididos por
responsabilidade), ou o desvio é registrado na TechSpec como decisão consciente. Manter os dois
comportamentos ao mesmo tempo é o pior dos mundos.

### P3-3 · Desvios pontuais de padrão, todos confirmados no código atual

| Item | Arquivo | Problema |
|---|---|---|
| Diálogo nativo fora do design system | `admin/questoes/questao-form-store.ts:32` | `window.confirm()` na troca de tipo ME↔CE, em vez do `ConfirmacaoDialog` Nocturne já usado nas telas irmãs. Também em `simulados-page.ts:16` e `simulado-form-page.ts:22` |
| Import dinâmico desnecessário | `questoes-page.ts`, `materias-page.ts`, `catalogo-page.ts`, `composicao-disciplinas.ts` | `import('rxjs').then(...)` repetido para uma função trivial já empacotada, em vez do import estático usado no resto do projeto |
| DTO sem Bean Validation | `flashcard/dto/RequisicaoFlashcard.java` | Validação de `frente`/`verso` feita manualmente no serviço. Decisão possivelmente deliberada (o *trim* precisa preceder a medida de tamanho), mas **não registrada** na TechSpec |
| Endpoint sem `@Valid` | `flashcard/deck/DeckController.java:32` | `PUT /decks/{id}` valida por `ValidadorDeck` manual, divergindo do padrão de DTO `record` com *constraints* |
| Injeção com `@Autowired` em produção | `simulado/MontadorProvaSimulado.java:22` | `code-standards.md` §3 prescreve injeção por construtor sem `@Autowired` |
| Dependência morta | `simulado/ExpiradorTentativaSimulado.java` | `Clock` injetado e nunca usado |
| Tempo não injetável | `simulado/TentativaSimulado.java:23,27` | `Instant.now()` direto nos pontos de escrita, ao contrário de `JanelaSimulado`, que recebe `Clock` |
| Reenfileiramento fora de ordem | `flashcards/revisao-store.ts:89` | Card reenfileirado vai para o **fim** da fila (`[...restante, card]`), não ordenado por `proximaRevisaoEm` como a TechSpec exige |
| Rastreabilidade de teste quebrada | `estudo/SessaoEstudoServiceTest`, `CorrecaoProvaServiceTest` | IDs `TU18`/`TU19` e `TU29`–`TU37` reusados para casos que pertencem a outras classes |
| Nomes colidentes | `desempenho/PontoFraco.java`, `SemanaEvolucao.java` | Idênticos aos DTOs homônimos em `desempenho/dto/`, exigindo import totalmente qualificado |

---

## 6. Resumo por funcionalidade

| Funcionalidade | Veredito registrado | Ainda vale? | Pendências |
|---|---|---|---|
| Acesso e conta | Aprovado com ressalvas | Sim | P2-3 (e-mail), P3-2 |
| Banco de questões | Aprovado com ressalvas | Sim | P2-3 (×2), P3-1, P3-2, P3-3 |
| Sessão de estudo | Aprovado com ressalvas | Sim, **com pendência** | P2-1, P3-2 |
| Cadernos e desempenho | Aprovado com ressalvas | Sim | P2-3 (×2), P3-2, P3-3 |
| Simulados | Aprovado com ressalvas | Sim | P3-2, P3-3 |
| Editais | Aprovado | Sim, **com pendência** | **P2-2** |
| Gestão de usuários | Aprovado com ressalvas | Sim | P3-2 |
| Flashcards — criação | Aprovado com ressalvas | Sim | P3-1, P3-2, P3-3 |
| Flashcards — decks e revisão | Aprovado com ressalvas | **Rebaixado** para aprovado com pendência | **P1-1**, P2-1, P2-3, P3-2, P3-3 |

Ordem sugerida: **P1-1** (comportamento visível ao usuário), depois **P2**, e **P3** conforme os
arquivos forem tocados. P0-1 e P1-2 foram resolvidas pelas tarefas 1.0 e 4.0; P1-3 já estava
resolvida pela tarefa 3.0.

---

## 7. O que foi verificado e **não** é pendência

Para evitar retrabalho, registram-se os itens que constavam como abertos em algum relatório e já
estão resolvidos no repositório:

- **`V990`/`V991`** — as migrações de teste foram removidas; `db/migration-test/` está vazio.
  A menção a `V991` na tarefa 2.0 de `prd-flashcards-criacao-e-gerenciamento` foi corrigida.
- **`TaxonomiaArvorePersistenciaTest`** — o defeito de *discovery* de `AssuntoArvore`, que bloqueava
  o `verify` em 2026-09-02, não existe mais; a classe passa 3/3.
- **`IntegracaoSessaoDeCadernoTest`** — voltou ao verde (2/2) quando `ConsultaCadernosService` foi
  entregue por `prd-cadernos-e-desempenho`.
- **`DesempenhoLeituraIntegracaoTest`** — os seis casos (TI-19…TI-23, TI-26) que o `qa.md` de
  cadernos dava como cobertos por outra classe agora existem de fato e passam 6/6.
- **`SessaoEstudoIntegracaoTest`** — os 15 casos TI que faltavam foram implementados e passam 15/15.
- **Testes de componente da sessão de estudo** — os 9 `*.spec.ts` ausentes foram criados.
- **`SessaoEncerradaException`** — o construtor genérico que duplicava o código
  `MODO_NAO_ALTERAVEL` foi removido.
- **`try/catch` no controller de sessão** — removido; a instrumentação foi para o serviço.
- **Caminho com acentos** (`Área de trabalho`) — não quebra mais os testes de *slice* web.
- **E2E-13 de flashcards** — o `test.skip` continua **correto**: a rota `/estudar?questao={id}` de
  fato não existe.
