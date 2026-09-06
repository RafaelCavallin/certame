# Relatório de revisão de código — Cadernos e desempenho

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Branch: diretório de trabalho (repositório sem controle git)
- Status: **APROVADO COM RESSALVAS** (mantido)

> **Revalidação 2026-09-05.** A correção principal desta revisão continua no repositório:
> `DesempenhoLeituraIntegracaoTest.java` existe e passa **6/6**. `ConsultaCadernosService` também já
> existe, e `IntegracaoSessaoDeCadernoTest` — que chegou a ficar vermelho por classe ausente durante
> a entrega concorrente de `prd-sessao-de-estudo` — **passa 2/2**.
> **As ressalvas continuam abertas**: não existe `ReconciliadorAgregados` em nenhum teste
> (**TI-25 segue sem cobertura**); não existe `CadernoControllerTest` nem qualquer *slice* web em
> `src/test/java/br/com/certame/estudo/caderno/`; `CadernoService.java` segue com **103 linhas** e
> `SessaoEstudoService.java` **cresceu para 111**. Os números de teste foram atualizados abaixo.

A funcionalidade implementa corretamente a arquitetura documentada na TechSpec (agregados
incrementais, projeção de cadernos automáticos, RN-04 filtrado na porta `FonteRespostas`,
`AFTER_COMMIT` + `REQUIRES_NEW`, integração com `SessaoEstudoService`). Backend e frontend passam
integralmente nos testes agora existentes. O principal problema encontrado — testes de integração
de leitura de desempenho (TI-19, TI-20, TI-21, TI-22, TI-23, TI-26) que o `tasks.md`/`qa.md`
declaravam concluídos mas não existiam no código — foi corrigido durante esta revisão com a criação
de `DesempenhoLeituraIntegracaoTest.java` (6 testes, todos passando). As ressalvas restantes são de
menor severidade (arquivos ligeiramente acima do limite de 100 linhas, ausência de teste de contrato
HTTP dedicado para os dois controllers, TI-25 do reconciliador ainda pendente e assumido como tal).

## Conformidade com regras
| Regra | Status | Observações |
|------|--------|-------------|
| Idioma pt-BR sem acento no código / pt-BR com acento nas mensagens | OK | Mensagens de exceção (`NomeCadernoEmUsoException`, `QuestaoNaoPublicadaException`, etc.) em pt-BR; identificadores sem acento. |
| Autorização sempre no servidor (`@PreAuthorize`) | OK | Todos os serviços de `caderno` e `desempenho` têm `@PreAuthorize("isAuthenticated()")`; isolamento por `usuario_id` garantido estruturalmente pelas queries dos repositórios (`findByIdUsuarioId...`), confirmado agora também por teste (TI-26). |
| Migração Flyway aditiva e imutável | OK | `V6__cadernos_desempenho.sql` não altera migrações anteriores; `V8__sessao_caderno_tamanho_livre.sql` é aditiva (dropa e recria apenas a constraint de `V7`, migração nova, não edição da antiga). |
| DTOs como `record`, mensagens de validação em pt-BR | OK | `RequisicaoCaderno`, `RequisicaoQuestoesCaderno`, DTOs de `desempenho/dto/` todos `record`. |
| Entidades JPA sem Lombok, construtor + getters/setters manuais | OK | `Caderno`, `CadernoQuestao`, `DesempenhoSemanal` etc. sem Lombok. |
| Arquivos ≤ 100 linhas | RESSALVA | `CadernoService.java` (103) e `SessaoEstudoService.java` (102) passam o limite por poucas linhas; vários arquivos de teste desta feature também passam de 100 (`CadernoServicoIntegracaoTest` 215, `DesempenhoLatenciaIntegracaoTest` 149, `DesempenhoLeituraServiceTest` 151, `desempenho-paineis.spec.ts` 306). O estilo de "várias instruções por linha" (`;` encadeado) já é convenção pré-existente no repositório (visto também em `AutenticacaoService.java`, fora do escopo desta feature) — não é uma regressão introduzida por esta funcionalidade, mas dificulta a leitura e mascara o verdadeiro tamanho lógico dos métodos. |
| Métodos ≤ 30 linhas / ≤ 3 níveis de `if` / ≤ 3 parâmetros | RESSALVA | O construtor de `SessaoEstudoService` recebe 10 dependências na mesma linha; embora não viole literalmente a regra 2.4 (que fala de parâmetros de método de negócio, não de injeção de dependência), o volume indica que a classe acumulou responsabilidades de F2 e da integração com cadernos (regra 6/9 desta feature). Não corrigido nesta revisão por estar fora do escopo estrito de cadernos/desempenho (a classe pertence majoritariamente a `prd-sessao-de-estudo`) — ver bloqueador abaixo. |
| Sem comentário desnecessário / sem número mágico | OK | `SeletorPontosFracos.MINIMO_RESPOSTAS`, `CalendarioSemanas.QUANTIDADE_SEMANAS` nomeados; comentários existentes explicam decisões não óbvias (ex.: `REQUIRES_NEW` em `AtualizadorAgregados`, o nome de métrica repetido em `OuvinteSessaoFinalizada`). |
| Duplicação `PontoFraco`/`SemanaEvolucao` (domínio vs. `dto/`) | OBSERVAÇÃO | Existem `desempenho.PontoFraco`/`desempenho.SemanaEvolucao` (saída das regras puras `SeletorPontosFracos`/`CalendarioSemanas`) e os DTOs homônimos em `desempenho.dto`. Os campos são idênticos e o mapeamento manual (`dto()`) não agrega valor hoje, mas é uma separação deliberada domínio/contrato-de-API (consistente com "entidade nunca cruza a fronteira do controller") — não é bug, apenas gera confusão de nomes; renomear o tipo de domínio (ex.: `PontoFracoCalculado`) melhoraria a legibilidade. |
| Segredo fora do código | OK | Nenhum segredo literal introduzido por esta feature. |
| Endpoint fora do contrato sem atualizar TechSpec | OK | `TECH-SPEC.md` §6 já lista as 7 rotas de `/cadernos` e as 4 de `/desempenho`. |

## Aderência à TechSpec
| Decisão Técnica | Implementado | Observações |
|-----------------|--------------|-------------|
| Cadernos automáticos como projeção pura de `marcacao` (sem materialização) | SIM | `CadernoAutomaticoService` consulta `marcacao` diretamente via `JdbcTemplate`; nenhuma tabela de sincronismo. |
| Progresso como cobertura (`questoesRespondidas/total`) | SIM | `ProgressoCaderno.percentual` (0% sem divisão por zero) e `CadernoRepository.contarQuestoes` em consulta agregada (sem N+1). |
| Três agregados incrementais (`desempenho_semanal/disciplina/assunto`) em vez de *materialized views* | SIM | Migração `V6` cria as três tabelas com `check (acertos <= volume)`; `AtualizadorAgregados` faz upsert idempotente (`excluirDoUsuario` + reinserção completa por usuário). |
| RN-04 filtrado na origem (porta `FonteRespostas`) | SIM | `RespostaEstatisticaRepository` filtra `s.finalizada_em is not null` nas três consultas SQL; nenhum serviço de `desempenho` reimplementa o filtro — confirmado agora também por TI-23 (nova). |
| `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` | SIM | `OuvinteSessaoFinalizada` não propaga falha (grava `desempenho_pendencia` + `WARN`); `AtualizadorAgregados.atualizar` usa `Propagation.REQUIRES_NEW` — bug histórico documentado em `tasks.md` continua corrigido. |
| `ReconciliadorAgregados` de 15 minutos | SIM (sem teste dedicado) | `@Scheduled(fixedDelayString = "900000")`, consome fila em lote de 100, apaga pendência ao reconstruir. TI-25 (reconciliador reconstrói agregado divergente) segue sem teste automatizado — ver bloqueador. |
| Integração `SessaoEstudoService.abrir` × `SerieDoCadernoService` | SIM | `abrir()` ramifica por `origem`; `CADERNO` força modo Foco e busca via `SerieDoCadernoService`; `V8` relaxa o `check` de tamanho só para `origem = CADERNO`. |
| `GET /desempenho/resumo` como primeira chamada, decidindo estado vazio | SIM | `DesempenhoStore.carregar()` só dispara as outras 3 chamadas se `resumo.possuiHistorico`. |
| Gráfico de evolução em SVG próprio, sem biblioteca, lacuna nunca vira zero | SIM | `EvolucaoChart`/`DesempenhoStore.criarSegmentos` quebram em `pctAcerto === null`, sem `?? 0` em nenhum ponto do caminho de dados. |
| Endpoints documentados em `TECH-SPEC.md` §6/§8 | SIM | Conferido: as 11 rotas (7 cadernos + 4 desempenho) e a troca de MVs por agregados incrementais estão registradas. |

## Tarefas verificadas
| Tarefa | Status | Observações |
|------|--------|-------------|
| 1.0 — Lacunas do protótipo | COMPLETA | Marcação traduzida (diálogos, estados vazios, SVG, ícones) presente em `cadernos-page`, `desempenho-page`, `estado-vazio`. |
| 2.0 — Migração V6, entidades, repositórios | COMPLETA | `V6__cadernos_desempenho.sql` com as 6 tabelas e o índice aditivo `resposta_usuario_questao_idx`; TI-01…TI-05 presentes em `CadernosPersistenciaIntegracaoTest` (5/5 passam). |
| 3.0 — Regras puras | COMPLETA | `ProgressoCaderno`, `ReferenciaCaderno`, `CalendarioSemanas`, `SeletorPontosFracos` com TU-01…TU-19 (todos localizados e passando). |
| 4.0 — Serviços/controller/DTOs de caderno | COMPLETA COM RESSALVA | Serviço e controller implementados e funcionais (13 testes em `CadernoServicoIntegracaoTest`, todos passando). Porém: TU-20…TU-30 e TI-06…TI-18 do `task_4.md` foram implementados como métodos de um único `@DataJpaTest` (`CadernoServicoIntegracaoTest`) — alguns com o prefixo `TU`/`TI` do caso de teste, outros sem (`criarPersisteDescricaoETrimaONome`, `atualizarRenomeiaCadernoManualExistente`, etc.), e nenhum é um teste de unidade puro (JUnit+Mockito sem Spring/banco) como pede a seção de skills do `task_4.md` ("JUnit e Mockito puros nos serviços"). Não há também um `CadernoControllerTest`/`*ControllerTest` de slice web dedicado ao contrato HTTP (`@WebMvcTest` ou `MockMvcTester`) — o `@DataJpaTest` não carrega a cadeia de segurança, então "rotas sem token" (TI-14) não é provado neste pacote (é coberto, na prática, pela suíte de segurança global de `prd-acesso-e-conta`, já que `SegurancaConfig` exige `anyRequest().authenticated()`). Comportamento correto, mas a pirâmide/nomenclatura de testes descrita na própria tarefa não foi seguida à risca. |
| 5.0 — `FonteRespostas`, agregados, reconciliador | COMPLETA COM RESSALVA | TU-31…TU-35 presentes e passando. TI-24 presente (`DesempenhoAgregacaoIntegracaoTest`). **TI-25 (reconciliador reconstrói agregado divergente e limpa a pendência) continua sem teste automatizado** — já reconhecido como pendente em `tasks.md`/`task_5.md`, portanto não é uma surpresa desta revisão, mas segue em aberto. |
| 6.0 — Serviços de leitura + `DesempenhoController` | COMPLETA (após correção nesta revisão) | TU-06…TU-08, TU-36, TU-37 presentes e passando. **TI-19, TI-20, TI-21, TI-22, TI-23 e TI-26 não existiam no código-fonte** apesar de `tasks.md` e `qa.md` os declararem concluídos/passando (o `qa.md` cita `DesempenhoAgregacaoIntegracaoTest` como evidência, mas essa classe contém apenas TI-24). Corrigido nesta revisão: criada `DesempenhoLeituraIntegracaoTest.java` cobrindo os seis casos, todos passando (6/6, ver seção Testes). |
| 7.0 — Frontend de cadernos + `estado-vazio` | COMPLETA | `CadernosStore`, `cadernos-page`, `caderno-dialog`, `questoes-picker-dialog`, `estado-vazio` implementados; TU-42/TU-43 presentes e passando (`cadernos-store.spec.ts`, `caderno-api.spec.ts`). |
| 8.0 — Frontend de desempenho | COMPLETA | `DesempenhoStore`, `evolucao-chart`, `disciplinas-card`, `pontos-fracos-table` implementados; segmentação em lacunas sem `?? 0` confirmada no código; TU-38…TU-41, TU-44 presentes e passando. |
| 9.0 — Integração com sessão de estudo | COMPLETA | `SessaoEstudoService.abrir` ramifica por `origem`; `V8` migração aditiva; bug de coluna (`criada_em`→`criado_em`) e de `CADERNO_NAO_ENCONTRADO` corrigidos conforme documentado; TI-16/TI-17 presentes em `IntegracaoSessaoDeCadernoTest` e passando. |
| 10.0 — E2E, cobertura, latência, acessibilidade | COMPLETA | 3 arquivos de E2E (`cadernos.spec.ts`, `desempenho.spec.ts`, `desempenho-paineis.spec.ts`) cobrindo os 13 cenários; `DesempenhoLatenciaIntegracaoTest` prova p95 < 400 ms e ausência de varredura de `resposta` no plano; cobertura de frontend confirmada acima de 80% em todas as métricas nesta execução. |

## Testes
Execução desta revisão (Docker ativo, Testcontainers PostgreSQL 18):

- Backend, pacotes da feature (`br.com.certame.estudo.caderno.**`, `br.com.certame.desempenho.**`):
  **56 testes, 0 falhas** (13 + 5 + 2 + 3 + 2 em `caderno`; 4 + 5 + 1 + 2 + 6 (novo) + 6 + 1 + 6 em
  `desempenho`), incluindo a nova classe `DesempenhoLeituraIntegracaoTest` (6/6).
- Backend, execução ampla com o padrão do `AGENTS.md` (`*Caderno*,*Desempenho*,*Agregado*,*Reconciliador*,*Serie*`,
  que também casou incidentalmente com testes de latência de `questao`): **BUILD SUCCESS, exit code 0**,
  nenhuma falha.
- `./mvnw verify` (regra JaCoCo de 80%) **não foi executado para o módulo inteiro** nesta revisão: o
  próprio `tasks.md` registra que uma sessão concorrente de `prd-simulados` deixava o `verify` do
  repositório inteiro instável por classes fora do escopo desta feature. Em vez disso, a cobertura
  das classes `*Service` de `caderno`/`desempenho` foi conferida diretamente no relatório JaCoCo
  (`target/site/jacoco/jacoco.csv`) gerado pela execução escopada: **todas as classes `*Service`
  desta feature (`CadernoService`, `CadernoAutomaticoService`, `ConsultaCadernosService`,
  `ConsultaQuestoesCadernoService`, `SerieDoCadernoService`, `AtualizadorAgregados`,
  `EvolucaoSemanalService`, `PontosFracosService`, `DisciplinaDesempenhoService`,
  `ResumoDesempenhoService`) têm 100% de linhas cobertas** nesta execução — acima do piso de 80%
  exigido. `CadernoController` (20%) e `DesempenhoController` (55%) têm cobertura de linha mais
  baixa, mas ficam fora da regra do JaCoCo (que mede só `*Service`) — ver Problemas/Recomendações.
- Frontend (`npm run test:coverage`): **362 testes, 362 passando, 0 falhas.**
  Cobertura: Statements 87,49% · Branches 83,62% · Functions 82,89% · Lines 94,93% — todas acima do
  piso de 80% exigido pelo `angular.json`.

### Medição vigente (2026-09-05) — substitui a anterior
- Backend (`./mvnw verify`, repositório inteiro): **535 testes, 1 falha, 0 erros → BUILD FAILURE**.
  A falha é `FlywayMigracaoTest.TI06`, **alheia a esta funcionalidade** (migração `V15` de
  `prd-sessao-de-estudo`). Todo o escopo desta feature passa:
  `DesempenhoLeituraIntegracaoTest` 6/6, `IntegracaoSessaoDeCadernoTest` 2/2.
- **O gate JaCoCo de 80% deixou de ser confirmável pelo build completo**, porque o `surefire`
  interrompe antes do `jacoco:check`. A ressalva original ("`verify` não executado por instabilidade
  de sessão concorrente") continua valendo, agora por causa determinística e não por concorrência.
- Frontend (`npm run test:coverage`): **427 testes em 80 arquivos, 0 falhas** —
  Statements 89,47% · Branches 85,27% · Functions 80,27% · Lines 93,83%.
- Ambiente de testes: Testcontainers efêmero (nenhum container/porta persistente iniciado por esta
  revisão); nenhum processo de backend/frontend "rodando" precisou ser iniciado, pois toda a
  verificação funcional foi feita por testes automatizados, não por E2E manual contra a aplicação
  viva.

### Medição vigente (2026-09-05, tarefa 6.0 de `tasks/correcoes-pendencias/`) — substitui a anterior
- A falha determinística de `FlywayMigracaoTest.TI06` foi corrigida pela tarefa 1.0 desta mesma pasta
  de correções. `cd backend && ./mvnw verify` (repositório inteiro) roda agora **571 testes, 0
  falhas, 0 erros → BUILD SUCCESS**, e `jacoco:check` reporta **"All coverage checks have been
  met."** — o gate de 80% por classe `*Service` volta a ser confirmável no build completo, fechando
  a ressalva registrada na medição anterior.
- TI-25 (`ReconciliadorAgregados`) deixou de ser uma lacuna: `ReconciliadorAgregadosTest` (3 testes
  de unidade) prova a reconstrução do agregado divergente, a limpeza da pendência e o isolamento da
  falha de um item do lote.
- Novo `CadernoControllerTest` (4 testes) e `DesempenhoControllerTest` (4 testes) — ambos
  `@WebMvcTest`/`MockMvcTester` — provam 401 sem token nos dois controllers e o envelope de erro
  409 `NOME_CADERNO_EM_USO`/422 `QUESTAO_NAO_PUBLICADA` em `/cadernos`.
- Frontend (`npm run test:coverage`): **473 testes, 0 falhas** — Statements 89,45% · Branches
  84,88% · Functions 80,18% · Lines 93,93%, todas acima do piso de 80%.

## Problemas encontrados
| Severidade | Arquivo | Linha | Descrição | Sugestão |
|------------|---------|-------|-----------|----------|
| Alta (corrigida nesta revisão) | `backend/src/test/java/br/com/certame/desempenho/` | — | TI-19, TI-20, TI-21, TI-22, TI-23 e TI-26 estavam documentados como concluídos em `tasks.md`/`qa.md`, mas não existiam no código. O `qa.md` citava `DesempenhoAgregacaoIntegracaoTest` como evidência, mas essa classe só contém TI-24. | Corrigido: criada `DesempenhoLeituraIntegracaoTest.java` com os 6 testes, todos passando. Recomenda-se atualizar `task_6.md`/`tasks.md` para refletir a existência real destes testes. |
| Média (corrigida na tarefa 6.0) | `backend/src/main/java/br/com/certame/desempenho/ReconciliadorAgregados.java` | — | TI-25 (reconciliador reconstrói agregado divergente e limpa a pendência) seguia sem teste automatizado. | **Corrigido:** `ReconciliadorAgregadosTest.java` (3 testes de unidade) prova reconstrução, limpeza da pendência e isolamento de falha por item do lote. |
| Média (corrigida na tarefa 6.0) | `backend/src/test/java/br/com/certame/estudo/caderno/` | — | Ausência de um `CadernoControllerTest`/slice web dedicado ao contrato HTTP de `/cadernos`. | **Corrigido:** `CadernoControllerTest.java` e `DesempenhoControllerTest.java` (`@WebMvcTest`/`MockMvcTester`) provam 401 sem token e o envelope de erro 409/422. |
| Baixa | `backend/src/main/java/br/com/certame/estudo/caderno/CadernoService.java` | 1-103 | Arquivo com 103 linhas (limite de 100 do `code-standards.md` §2.2), em parte por instruções múltiplas na mesma linha. | Extrair, por exemplo, a validação de nome duplicado (`criar`/`atualizar`) para um método privado dedicado, e separar a lógica de leitura em uma classe auxiliar, se o arquivo crescer mais. |
| Baixa | `backend/src/main/java/br/com/certame/estudo/SessaoEstudoService.java` | 1-102 | Arquivo com 102 linhas; construtor com 10 dependências na mesma linha. Classe pertence majoritariamente a `prd-sessao-de-estudo` (F2), mas foi tocada por esta feature na tarefa 9.0. | Fora do escopo desta revisão decidir a refatoração de uma classe de outra feature; registrar como recomendação para quando F2 for revisado. |
| Baixa | `backend/src/main/java/br/com/certame/desempenho/PontoFraco.java` e `SemanaEvolucao.java` (fora de `dto/`) | — | Nomes idênticos aos DTOs homônimos em `desempenho/dto/`, com os mesmos campos — não é bug (separação domínio/contrato deliberada), mas confunde na leitura e exige `import` totalmente qualificado em `PontosFracosService`/`EvolucaoSemanalService`. | Renomear o tipo de saída das regras puras (ex.: `PontoFracoCalculado`, `SemanaCalculada`) para eliminar a colisão de nome sem mudar a arquitetura. |

## Pontos positivos
- Arquitetura fiel à TechSpec: agregados incrementais, RN-04 filtrado exclusivamente na porta
  `FonteRespostas`, `AFTER_COMMIT` com `REQUIRES_NEW` documentado e coberto por teste
  (`DesempenhoAgregacaoIntegracaoTest`).
- Cadernos automáticos como projeção pura de `marcacao`, sem tabela de sincronismo — elimina uma
  classe inteira de bugs de consistência.
- `ProgressoCaderno`, `CalendarioSemanas` e `SeletorPontosFracos` são regras puras, testáveis em
  milissegundos, com bordas bem cobertas (limite de 20 respostas, lacuna vs. zero, fuso horário).
- `DesempenhoLatenciaIntegracaoTest` (TI-27) é um teste de carga bem desenhado: popula 50 mil
  respostas via SQL em lote, mede p95 real e confirma pelo plano de consulta que os painéis não
  varrem `resposta`.
- Frontend com cobertura consistentemente acima do piso em todas as métricas, gráfico SVG próprio
  sem biblioteca de terceiros e cuidado real com a semântica de lacuna (`pctAcerto: null` nunca vira
  zero, em nenhum ponto do caminho de dados testado).
- Os três bugs históricos documentados (REQUIRES_NEW, `GET /questoes` ausente, diálogo "Novo
  caderno" que nunca abria) seguem corrigidos e sem regressão nesta execução.

## Recomendações
- ~~Adicionar um teste de contrato HTTP dedicado (`@WebMvcTest`/`MockMvcTester`) para `CadernoController`
  e `DesempenhoController`~~ — feito na tarefa 6.0.
- ~~Resolver a pendência conhecida de TI-25 (reconciliador)~~ — feito na tarefa 6.0.
- Ao tocar novamente em `CadernoService.java`/`SessaoEstudoService.java`, aproveitar para trazê-los
  para dentro do limite de 100 linhas por método/arquivo, quebrando instruções compostas em linhas
  próprias (isso também tornaria a métrica de "≤30 linhas por método" mensurável de fato).
- Atualizar `tasks.md`/`task_6.md` para marcar TI-19, TI-20, TI-21, TI-22, TI-23 e TI-26 como
  concluídos, já que agora existem e passam.

## Conclusão
A funcionalidade "Cadernos e desempenho" está **aprovada com ressalvas**. A arquitetura, os
contratos de API, o modelo de dados e as decisões técnicas documentadas na TechSpec foram
implementados fielmente, incluindo os pontos mais sensíveis (RN-04, `AFTER_COMMIT`/`REQUIRES_NEW`,
lacuna semanal nunca virando zero, isolamento por usuário). O problema mais grave encontrado —
seis testes de integração de desempenho declarados como concluídos mas ausentes do código — foi
corrigido durante esta revisão e revalidado (56 testes de backend da feature e 362 testes de
frontend, 0 falhas; cobertura de frontend e das classes `*Service` do backend acima de 80%). As
ressalvas remanescentes (TI-25 do reconciliador, ausência de teste de contrato HTTP dedicado para os
dois controllers, alguns arquivos ligeiramente acima do limite de 100 linhas) são reais mas não
bloqueantes: não indicam bug de comportamento, desvio de arquitetura ou falha de segurança, apenas
lacunas de cobertura/estilo a fechar em iteração futura.
