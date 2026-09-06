# Relatório de revisão de código — Início e sessão de estudo (Foco, Contexto e Modo Prova)

## Resumo
- Data: 2026-09-03 (revisão original) · 2026-09-05 (correções aplicadas, ver adendo ao final)
- Branch: N/A (repositório sem controle de versão Git)
- Status original: ~~REPROVADO~~
- **Status após correção: APROVADO COM RESSALVAS** (ver "Adendo — Correções aplicadas")

## Conformidade com regras
| Regra | Status | Observações |
|------|--------|-------------|
| AGENTS.md #2 — autorização sempre no servidor | OK | `@PreAuthorize("isAuthenticated()")` em todos os métodos públicos dos serviços; toda busca de sessão usa `findByIdAndUsuarioId` e responde 404 (nunca 403) para recurso de terceiro. |
| AGENTS.md #4 — nenhum endpoint fora do contrato | OK | `TECH-SPEC.md` §6 (raiz) já lista os 4 endpoints novos (`GET /estudo/inicio`, `GET /estudo/sessoes/{id}`, `PATCH .../modo`, `PUT /estudo/questoes/{id}/marcacoes`). |
| AGENTS.md #5 — migração só por Flyway, imutável | NOK | `V990` foi removida corretamente, mas `backend/src/test/resources/db/migration-test/V991__sessao_estudo_minima.sql` **continua existindo**, recriando `sessao_estudo` com schema divergente do `V7` real. A techspec (linhas 921/1014/1027) e a subtarefa 1.7 de `task_1.md` exigiam a remoção das duas. |
| AGENTS.md #6 — `resposta` imutável (RN-02); Modo Prova só conta após finalização (RN-04) | OK | `rule resposta_sem_update/delete` em `V7__sessao_estudo.sql:41-42`; entidade `Resposta` sem setter/`@PreUpdate`; `RegistroRespostaService` recusa gravação em `PROVA`; `CorrecaoProvaService` só grava o lote em `PROVA`. |
| code-standards.md — "não crie try/catch no controller" | **NOK** | `SessaoEstudoController.java:30-38` (método `abrir`) envolve a chamada ao serviço em `try/catch (SerieVaziaException)` só para instrumentação — violação literal e explícita da regra. |
| code-standards.md §2.4 — máx. 3 parâmetros | NOK | `SessaoEstudoService` tem construtor com 10 dependências; `RegistroRespostaService` (7), `CorrecaoProvaService` (6) também acima de qualquer precedente do projeto — sinal de responsabilidade concentrada demais. |
| code-standards.md §2.2/§2.5 — arquivos ≤100 linhas, métodos ≤30, sem linha em branco em método | NOK (sistêmico) | Todo o pacote `estudo` (backend) e os componentes novos de `features/study`/`features/dashboard` (frontend) foram escritos comprimindo classes/métodos inteiros em uma única linha física, burlando a contagem de linhas em vez de reduzir a responsabilidade real. Ex.: `SessaoEstudoService.java` (`montarResposta`), `CorrecaoProvaService.java` (`validarLote`/`montarCorrecao`), `EstudarPage.finalizar()`. |
| code-standards.md §4 — Prettier (`printWidth: 100`) | **NOK** | `npx prettier --check` acusa **30 arquivos** não formatados em `features/study/`, `features/dashboard/inicio-page.*` e `core/api/estudo-api.ts`/`modelos-estudo.ts` — todos os arquivos novos da funcionalidade. |
| code-standards.md — código estável de erro por classe | NOK | `SessaoEncerradaException` ganhou um construtor genérico `(code, message)` reaproveitado em `RegistroRespostaService.java:39` para emitir `MODO_NAO_ALTERAVEL` — código que já pertence a `ModoNaoAlteravelException`, com mensagem diferente. Duas classes emitindo o mesmo `code`. |
| code-standards.md §2.6 — sem literal mágico | NOK | Frontend: `estudar-page.ts:24,44` e `painel-filtros.ts` repetem o literal `10` (tamanho padrão da série) sem constante nomeada nem vínculo com o domínio `TamanhoSerie` do backend. |
| tests.md §1 — todo código coberto por teste | **NOK** | Nenhum dos 8 componentes de apresentação do frontend (`estudar-page`, `painel-filtros`, `seletor-modo`, `questao-cartao`, `barra-acao`, `gabarito-painel`, `painel-navegacao`, `correcao-prova`) nem `inicio-page` tem `*.spec.ts` — confirmado por busca no repositório e pelo relatório de cobertura, que sequer lista essas pastas. |
| tests.md — nomenclatura TU/TI rastreável | NOK | IDs de teste colidem entre classes: `SessaoEstudoServiceTest` reusa `TU18`/`TU19` (que a techspec atribui a `RegistroRespostaService`); `CorrecaoProvaServiceTest` reusa `TU29`–`TU37` (que pertencem a `MarcacaoService`/`TempoSessao`/`ResumoDiaService`). Quebra a rastreabilidade caso↔teste que a convenção existe para garantir. |
| Idioma (código sem acento, mensagens pt-BR) | OK | Confirmado em DTOs, exceções e mensagens de erro/UI. |
| Entidades sem Lombok, `@Enumerated(STRING)`, sem setter em `Resposta` | OK | Confirmado por leitura direta. |

## Aderência à TechSpec
| Decisão Técnica | Implementado | Observações |
|-----------------|--------------|-------------|
| Migração `V7__sessao_estudo.sql` (4 tabelas + rules de imutabilidade) | SIM | Schema conforme; pequeno desvio aditivo e inofensivo: `tamanho` ganhou `default 10` não previsto no DDL da spec. |
| Remoção de `V990`/`V991` de teste | **NÃO** | `V991__sessao_estudo_minima.sql` recriado/mantido; roda em runtime (log mostra `relation "sessao_estudo" already exists, skipping`) — não deveria existir. |
| Seed dev com sessão interrompida "questão 14 de 40" (techspec.md:919) | **NÃO** | `V902__seed_sessao_estudo_dev.sql` cria sessão com `tamanho=50` e **zero respostas** — não reproduz o cenário de retomada descrito. |
| Componentes do domínio `estudo` (serviços, repositórios, DTOs, exceções) | SIM | Todos presentes no pacote correto, com as assinaturas descritas. |
| `POST /estudo/sessoes/{id}/respostas` — 409 apenas com `SESSAO_ENCERRADA`/`RESPOSTA_DUPLICADA` | **NÃO** | Também emite `MODO_NAO_ALTERAVEL` (não previsto na tabela de respostas do endpoint, techspec.md:682), via reaproveitamento indevido de classe de exceção. |
| `ReferenciaCaderno` — `CADERNO_NAO_ENCONTRADO` para referência inválida (techspec.md:406,572) | PARCIAL | Só o caso "UUID válido mas de outro usuário/inexistente" usa o código correto; `origemRef` nulo/vazio ou enum `auto:` inválido cai em `RECURSO_NAO_ENCONTRADO` genérico. |
| Testes de integração TI-07 a TI-25 (Tarefa 5.0) | **NÃO (79% faltando)** | Apenas TI-07, TI-10, TI-13, TI-22 implementados (`SessaoEstudoControllerTest`/`InicioControllerTest`, ambos `@WebMvcTest` com serviços mockados). Não existem os `SessaoEstudoIntegracaoTest`/`InicioIntegracaoTest` (`@SpringBootTest`+Testcontainers) que a própria `task_5.md` previa; faltam TI-08, TI-09, TI-11, TI-12, TI-14 a TI-21, TI-23 a TI-25 — justamente os que exigem banco real (persistência, expiração, evento pós-commit, N+1/latência). |
| `RequisicaoSessao.tamanho` validado como {10,30,50} | PARCIAL | Bean Validation usa `@Min(10)/@Max(50)`, que aceita valores intermediários (ex.: 20, 37); só `TamanhoSerie` (na camada de serviço) rejeita depois. Resultado HTTP final é o mesmo, mas a anotação documenta a regra errada. |
| `PainelFiltros` — disciplina, assunto, banca, ano, dificuldade e quantidade (techspec.md:89) | **NÃO** | Implementa só o campo de quantidade (10/30/50); `FiltroSerie` nunca é preenchido pelo componente, e o painel deixa de existir assim que a sessão abre (não é "coluna do modo Contexto" como no protótipo). |
| `QuestaoCartao` — metadados exibidos em Contexto (techspec.md:91) | **NÃO** | Nenhuma tag de disciplina/assunto/banca/ano/dificuldade em nenhuma condição — nem o componente recebe o modo como entrada. |
| `PainelNavegacao` — versão compacta (Foco) vs. painel lateral (Contexto) (techspec.md:94) | **NÃO** | Componente único, sem variação por modo. |
| `CorrecaoProva` — exibir `disciplina` e `comentario` por item, destaque visual de acerto/erro (techspec.md:321) | **NÃO** | Template só renderiza `ordem`, `codigo`, `enunciadoResumo`, `marcada`, `gabarito`. |
| RN-02/RN-04/CA-07 (imutabilidade, prova fechada, gabarito não vaza) | SIM | Confirmado em código e schema; QA também validou em runtime. |

## Tarefas verificadas
| Tarefa | Status | Observações |
|------|--------|-------------|
| 1.0 Migração e persistência | INCOMPLETA | Subtarefa 1.7 (remover migrações de teste V990/V991) marcada `[x]` mas `V991` continua no repositório; seed dev não reproduz o cenário "14 de 40" exigido pela spec. |
| 2.0 Regras puras do domínio | COMPLETA | `ConferenciaGabarito`, `TamanhoSerie`, `TempoSessao`, `MontadorSerie`, `FiltroSerie` implementados e testados (TU-01 a TU-10, TU-31 a TU-33), todos passando. |
| 3.0 Serviços de sessão | INCOMPLETA | Código e testes unitários presentes e passando, mas com o desvio do código de erro `MODO_NAO_ALTERAVEL` reaproveitado indevidamente e construtores acima do limite de parâmetros. |
| 4.0 Início e expiração de sessões | COMPLETA | `ResumoDiaService`, `TempoEstudoExterno`, `ExpiradorSessoes` implementados e testados (TU-34 a TU-40), todos passando. |
| 5.0 Controllers, contrato da API e eventos | **INCOMPLETA** | `tasks.md` e `qa.md` afirmam a tarefa como concluída/aprovada, mas 15 dos 19 casos de teste de integração declarados (TI-08, TI-09, TI-11, TI-12, TI-14 a TI-21, TI-23 a TI-25) seguem `[ ]` no próprio `task_5.md` e não têm nenhuma implementação real — divergência entre o que foi documentado como pronto e o que existe no repositório. Também contém o `try/catch` proibido no controller. |
| 6.0 Ajustes no protótipo Nocturne | PARCIAL | As 9 peças aparentam corrigidas no `Certame.dc.html`, mas a tradução Angular não reflete integralmente o resultado (ver PainelFiltros/QuestaoCartao/PainelNavegacao acima) — o protótipo pode estar certo e a implementação Angular ter divergido dele. |
| 7.0 Frontend — Início e sessão de estudo | **INCOMPLETA** | Subtarefa 7.5 (testes dos componentes de apresentação) e 7.6 (teste de `inicio-page`) marcadas como parte da entrega concluída, mas nenhum `*.spec.ts` existe para `estudar-page`, `painel-filtros`, `seletor-modo`, `questao-cartao`, `barra-acao`, `gabarito-painel`, `painel-navegacao`, `correcao-prova` ou `inicio-page`. A cobertura de 87% relatada pelo `npm run test:coverage` **não inclui** esses arquivos (eles não aparecem no relatório porque nenhum teste os importa) — o número mascara uma lacuna real. |
| 8.0 Testes E2E e acessibilidade | COMPLETA | 8 cenários existem em `frontend/e2e/estudo.spec.ts`; E2E-06 corretamente com `test.skip` e motivo documentado (dependência de `prd-flashcards-criacao-e-gerenciamento`, não bloqueador). `evidences/e2e-run.log` referenciado no QA está vazio (0 bytes) — evidência textual não foi de fato salva, mas não é um problema de código. |

## Testes
### Backend
- Comando: `cd backend && ./mvnw test` (Docker/Testcontainers com `postgres:18` real).
- Resultado agregado: `Tests run: 479, Failures: 0, Errors: 14` → **BUILD FAILURE**. Os 14 erros são de infraestrutura de teste em domínios **fora** do escopo desta entrega (`catalogo`, `simulado`, `usuario` — `NoQualifyingBeanException`, `NoClassDefFoundError`, contexto Spring quebrado), não relacionados a `br.com.certame.estudo`.
- Todos os testes de `br.com.certame.estudo.*` passam sem exceção: `TamanhoSerieTest` (1/1), `TempoSessaoTest` (3/3), `ConferenciaGabaritoTest` (5/5), `MontadorSerieTest` (4/4), `MarcacaoServiceTest`, `RegistroRespostaServiceTest`, `SessaoEstudoServiceTest`, `SessaoEstudoControllerTest`, `InicioControllerTest` (0 falhas), `CorrecaoProvaServiceTest` (14/14), `ExpiradorSessoesTest` (1/1), `inicio.ResumoDiaServiceTest` (6/6).
- Cobertura de linha (JaCoCo, extraída manualmente do `jacoco.csv` pois `./mvnw verify` não completou a cadeia por causa da falha em outros domínios): todas as classes `*Service`/regras puras do pacote `estudo` acima de 80% (`SessaoEstudoService` 90,9%; demais 100%). Números inflados pelo estilo "tudo numa linha" (JaCoCo conta linha física).
- `./mvnw verify` (regra de 80%) **não foi concluído** devido à falha pré-existente em outros domínios — não é possível confirmar o gate oficial de cobertura do build inteiro.

### Frontend
- Comando: `cd frontend && npm test` → **362/362 passando** (71 arquivos de teste).
- Comando: `npm run test:coverage` → Statements 87,49% · Branches 83,62% · Functions 82,89% · Lines 94,93% — piso de 80% atingido **no agregado do projeto**, mas sem incluir os componentes de tela da própria funcionalidade (ver acima).
- Playwright (E2E) não foi reexecutado nesta revisão (exige infraestrutura completa); último resultado registrado no QA: 7 passed, 1 skipped.

## Problemas encontrados
| Severidade | Arquivo | Linha | Descrição | Sugestão |
|------------|---------|-------|-----------|----------|
| Alta | `backend/src/main/java/br/com/certame/estudo/SessaoEstudoController.java` | 30-38 | `try/catch` explícito no controller, proibido literalmente por `code-standards.md` ("Controller só orquestra... Não crie try/catch no controller"). | Mover a instrumentação de `SERIE_VAZIA` para dentro de `SessaoEstudoService.abrir()` ou capturar via listener/AOP, deixando o controller sem `try/catch`. |
| Alta | `backend/src/test/resources/db/migration-test/V991__sessao_estudo_minima.sql` | — | Migração de teste que deveria ter sido removida (techspec + subtarefa 1.7) continua no repositório e roda em runtime, mascarada por um "already exists, skipping". | Remover o arquivo; confirmar que as suítes de outros domínios seguem passando contra o schema real de `V7`. |
| Alta | `backend/src/test/java/br/com/certame/estudo/` (ausência) | — | Faltam `SessaoEstudoIntegracaoTest`/`InicioIntegracaoTest` (`@SpringBootTest @Testcontainers`) previstos por `task_5.md`; 15 de 19 casos TI (TI-08, TI-09, TI-11, TI-12, TI-14 a TI-21, TI-23 a TI-25) não têm nenhuma implementação, apesar de `tasks.md`/`qa.md` afirmarem a tarefa como concluída/aprovada. | Implementar os testes de integração faltantes contra banco real (Testcontainers), cobrindo persistência, expiração, evento pós-commit e latência/N+1 — únicos casos que um slice com serviços mockados não consegue provar. |
| Alta | `frontend/src/app/features/study/{estudar-page,painel-filtros,seletor-modo,questao-cartao,barra-acao,gabarito-painel,painel-navegacao,correcao-prova}` e `features/dashboard/inicio-page.ts` | — | Nenhum desses 9 arquivos/componentes tem teste unitário (`*.spec.ts`), violando a regra inegociável 1 de `tests.md` ("todo código deve ser coberto"). A cobertura de 87% relatada é do agregado do projeto e não inclui esses arquivos. | Escrever testes com `TestBed`/Vitest para cada componente, cobrindo ao menos o caminho feliz e as bordas de exibição condicional (modo Foco vs. Contexto, Modo Prova, série vazia). |
| Média | `frontend/src/app/features/study/painel-filtros/painel-filtros.ts` | 1-4 | Implementa só o campo de quantidade; `disciplina`, `assunto`, `banca`, `ano`, `dificuldade` da techspec (linha 89) não existem, e `FiltroSerie` nunca é preenchido — RN-01/CA-16 não são exercitáveis pela UI. | Adicionar os campos de filtro faltantes e conectar ao `FiltroSerie` já existente em `modelos-estudo.ts`. |
| Média | `frontend/src/app/features/study/questao-cartao/questao-cartao.html` | 1 | Nenhuma tag de metadados é exibida, nem mesmo em modo Contexto (a spec exige exibição em Contexto e ocultação em Foco). O componente não recebe o modo como entrada. | Adicionar `input()` de modo (ou os próprios metadados condicionados) e renderizar as tags quando `modo === 'CONTEXTO'`. |
| Média | `backend/src/main/java/br/com/certame/comum/excecao/SessaoEncerradaException.java` | 5 | Construtor genérico `(code, message)` reaproveitado por `RegistroRespostaService.java:39` para emitir `MODO_NAO_ALTERAVEL`, duplicando o código de `ModoNaoAlteravelException` com mensagem diferente e sem previsão no contrato do endpoint. | Lançar `ModoNaoAlteravelException` diretamente (ajustando a mensagem, se necessário) e remover o construtor genérico de `SessaoEncerradaException`. |
| Média | `frontend/src/app/features/study/` e `features/dashboard/inicio-page.*` (30 arquivos) | — | Formatação fora do Prettier (`printWidth: 100`); classes/templates inteiros em uma única linha física, burlando a contagem de linhas de `code-standards.md` §2.2/§2.5 sem reduzir a responsabilidade real dos métodos. | Rodar `npx prettier --write` nos arquivos listados e, na sequência, avaliar se algum arquivo ultrapassa de fato 100 linhas/30 por método após a formatação normal — extrair componentes/métodos onde necessário. |
| Média | `backend/src/main/resources/db/migration-dev/V902__seed_sessao_estudo_dev.sql` | 36-49 | Seed não reproduz o cenário "sessão interrompida na questão 14 de 40" exigido pela techspec (linha 919) para uso manual/QA — cria sessão de tamanho 50 sem nenhuma resposta gravada. | Ajustar o seed para `tamanho=40` com 14 respostas gravadas, batendo com o card "Retomar" descrito. |
| Baixa | `backend/src/main/java/br/com/certame/estudo/caderno/ReferenciaCaderno.java` | 16-19, 30-44 | `origemRef` nulo/vazio ou enum `auto:` inválido lança `RecursoNaoEncontradoException` genérico em vez de `CADERNO_NAO_ENCONTRADO`, previsto pela techspec para "referência de caderno inválida". | Padronizar todos os caminhos de referência inválida para `CADERNO_NAO_ENCONTRADO`. |
| Baixa | `backend/src/main/java/br/com/certame/estudo/dto/RequisicaoSessao.java` | 8-9 | `@Min(10)/@Max(50)` aceita valores intermediários (ex.: 20, 37) que só são rejeitados depois por `TamanhoSerie`; a anotação documenta uma regra mais permissiva que a real. | Trocar por validação de conjunto fechado (`@Pattern`/enum/validador customizado) ou remover a anotação e deixar só `TamanhoSerie` documentar a regra, evitando duas fontes conflitantes. |
| Baixa (corrigida em 2026-09-06 por `tasks/correcoes-pendencias/task_9.md`) | `backend/src/test/java/br/com/certame/estudo/SessaoEstudoServiceTest.java`, `CorrecaoProvaServiceTest.java` | vários | Reuso de IDs `TU18`/`TU19` (pertencentes a `RegistroRespostaService`) e `TU29`–`TU37` (pertencentes a `MarcacaoService`/`TempoSessao`/`ResumoDiaService`) para casos extras próprios, quebrando a rastreabilidade caso↔teste da convenção `tests.md`. | **Corrigido**: renumerados para `TU55`/`TU56` (`SessaoEstudoServiceTest`) e `TU57`–`TU65` (`CorrecaoProvaServiceTest`), livres na numeração global até então (maior ID em uso era `TU54`). |
| Baixa | `backend/src/main/java/br/com/certame/estudo/SessaoEstudoService.java` | 28 | Construtor com 10 dependências — acima de qualquer precedente do projeto (`code-standards.md` §2.4 fala de métodos de negócio, mas o volume sugere responsabilidades demais concentradas na classe). | Considerar extrair a abertura via caderno/simulado para um colaborador dedicado. |
| Baixa | `frontend/src/app/features/study/{correcao-prova,questao-cartao,estudar-page}/*.scss`, `features/dashboard/inicio-page.scss` | várias | `font-size`/`max-width` em `px` cru em vez de `var(--*)` (há precedente similar em `styles.css`, mas a techspec declarava "só tokens var(--*)" como cumprida). | Substituir por tokens existentes do design system Nocturne. |

## Pontos positivos
- RN-02 (imutabilidade de `resposta`), RN-04 (Modo Prova só grava no lote da finalização) e CA-07 (gabarito nunca vaza antes da hora) estão corretamente implementadas em três camadas independentes — entidade sem setter, serviço com validação e `rule` do PostgreSQL — exatamente como a TechSpec desenhou.
- Toda consulta de sessão/questão filtra por `usuarioId` e responde 404 (nunca 403) para recurso de terceiro, em todos os serviços verificados.
- Regras puras do domínio (`ConferenciaGabarito`, `TamanhoSerie`, `TempoSessao`, `MontadorSerie`) são isoladas de Spring, com `Clock`/`Random` injetados, seguindo `tests.md` §2-R à risca.
- No frontend, `StudySessionStore` e `Cronometro` cumprem CA-14 (navegação sem rede) e CA-06/CA-13 (cronômetro nunca reinicia por troca de modo/questão) — confirmado por leitura de código, sem `HttpClient` no caminho de navegação.
- Todo HTTP do frontend passa por `core/api`; nenhum componente injeta `HttpClient` diretamente.
- `TECH-SPEC.md` §6 foi devidamente atualizada com os 4 endpoints novos, cumprindo a regra 4 do `AGENTS.md`.
- Os 8 cenários E2E da TechSpec existem em `frontend/e2e/estudo.spec.ts`, com E2E-06 corretamente marcado como bloqueado e não-bloqueador, com motivo documentado.
- Todos os testes que existem para o domínio `estudo` (backend) e para `StudySessionStore`/`Cronometro`/`atalhos-teclado` (frontend) passam sem falhas.

## Recomendações
- Reexecutar `./mvnw verify` após resolver os erros pré-existentes em outros domínios (`catalogo`, `simulado`, `usuario`), para confirmar o gate oficial de 80% de cobertura no build completo.
- Atualizar `qa.md` e `task_5.md`/`task_7.md` para refletir com precisão o que está de fato implementado, evitando nova divergência entre documentação e repositório.
- Considerar registrar formalmente na TechSpec, como decisão justificada, qualquer redução deliberada de escopo de teste — em vez de marcar como concluído algo que não foi feito.

## Conclusão
A funcionalidade acerta as decisões mais críticas do domínio — imutabilidade de `resposta`, fechamento do Modo Prova e não vazamento de gabarito — tanto no código quanto no schema, e os testes que existem passam de forma consistente. No entanto, a revisão encontrou uma violação explícita de regra inegociável de arquitetura (`try/catch` no controller), uma lacuna real e verificada de testes de integração (15 de 19 casos TI da Tarefa 5.0 sem nenhuma implementação, contradizendo o que `tasks.md` e `qa.md` registram como concluído/aprovado), resíduo de migração de teste que deveria ter sido removida, duplicidade de código de erro entre exceções, e no frontend uma aderência incompleta à TechSpec (painel de filtros, cartão de questão e navegação não implementam o que a especificação descreve) somada à ausência total de testes unitários para os 9 componentes de tela da funcionalidade — mascarada por um número de cobertura agregado que não inclui esses arquivos.

**Veredito original: REPROVADO.** Corrigir os itens de severidade Alta e Média listados acima — em especial o `try/catch` do controller, os testes de integração faltantes, a remoção de `V991`, os testes de componente ausentes no frontend e a aderência de `PainelFiltros`/`QuestaoCartao` à TechSpec — e revalidar antes de nova submissão.

---

## Adendo — Correções aplicadas (2026-09-05)

Todos os itens de severidade **Alta** e a maioria dos de severidade **Média** listados acima foram corrigidos e revalidados. Duas falhas de produção adicionais, não detectadas na revisão original, foram descobertas pelos próprios testes de integração escritos nesta correção e também foram corrigidas. Dois itens de severidade **Baixa** ficam registrados como recomendação, não bloqueantes.

### Backend

| Item original | Correção aplicada |
|---|---|
| `try/catch` proibido em `SessaoEstudoController.abrir()` | Removido do controller; a instrumentação de `SERIE_VAZIA` foi movida para `SessaoEstudoService` (novo método privado `idsDaOrigem`), camada correta para tratar exceção de negócio. `EstudoObservabilidade` passou a ser injetada no serviço. |
| `V991__sessao_estudo_minima.sql` não removida | Arquivo excluído de `backend/src/test/resources/db/migration-test/`. |
| Duplicidade de código de erro (`SessaoEncerradaException` emitindo `MODO_NAO_ALTERAVEL`) | Removido o construtor genérico `(code, message)` de `SessaoEncerradaException`; `RegistroRespostaService.validarSessao` agora lança `SessaoEncerradaException()` padrão para sessão em Modo Prova, alinhado ao contrato do endpoint (`POST /respostas` só documenta `SESSAO_ENCERRADA`/`RESPOSTA_DUPLICADA`). Teste `TU22` atualizado para a nova mensagem. |
| `ReferenciaCaderno` usava `RECURSO_NAO_ENCONTRADO` genérico em vez de `CADERNO_NAO_ENCONTRADO` | Corrigido nos três pontos de lançamento (referência nula/vazia, enum automático inválido, UUID inválido). `ReferenciaCadernoTest` reforçado para verificar o `code`, e um novo caso (`TU20`) cobre referência nula/vazia. |
| `RequisicaoSessao.tamanho` validado com `@Min(10)/@Max(50)` (aceitava valores intermediários) | Substituído por `@TamanhoSerieValido`, uma constraint Bean Validation customizada (`estudo/dto/TamanhoSerieValido` + `...Validator`) que valida exatamente `{10, 30, 50}`, a mesma regra de `TamanhoSerie`. |
| Seed dev não reproduzia "sessão interrompida na questão 14 de 40" | `V902__seed_sessao_estudo_dev.sql` corrigido para `tamanho = 40` com 14 respostas gravadas (viável após a correção da constraint de tamanho, ver abaixo). |
| 15 de 19 casos de teste de integração da Tarefa 5.0 sem implementação (TI-08, 09, 11, 12, 14–21, 23–25) | Implementados em `backend/src/test/java/br/com/certame/estudo/SessaoEstudoIntegracaoTest.java` (`@SpringBootTest @AutoConfigureMockMvc @Testcontainers`, banco PostgreSQL real). **15/15 passando.** `task_5.md` atualizado. |

**Duas falhas de produção descobertas pelos novos testes de integração (não seriam pegas por testes unitários com repositório mockado):**

1. **`sessao_estudo.tamanho` com `check` incompatível com "série menor que a pedida".** A técspec documenta explicitamente que, quando o acervo elegível é menor que o `tamanho` pedido, a sessão abre mesmo assim com o que houver (RN-01, TU-09 já cobria isso a nível de `MontadorSerie`). Porém `SessaoEstudoService.abrir()` persiste `tamanho = ids.size()` (o tamanho **real** da série), e o `check` de banco (`V7`/`V8`/`V9`) só permite exatamente `{10,30,50}` para origem `LIVRE` (e um intervalo próprio para `CADERNO`/`SIMULADO`). Qualquer sessão `LIVRE` com acervo elegível menor que o tier pedido quebrava com `ConstraintViolationException` ao salvar — um bug real, nunca exercitado antes porque nenhum teste unitário toca o banco e o único teste de integração existente (`TI-07`) sempre tinha acervo suficiente. **Corrigido** com a migração aditiva `V15__sessao_livre_tamanho_variavel.sql`, que estende a mesma flexibilidade já dada a `CADERNO` também para `LIVRE` (`tamanho between 1 and 50`).
2. **Estatística não incluía a resposta recém-dada (quebra de CA-05).** A técspec é explícita: *"o recálculo síncrono roda na mesma transação da resposta... para que o painel mostre o número já incluindo a resposta recém-dada"*. Mas `RegistroRespostaService.registrar()` chamava `respostas.save(...)` (JPA, sem *flush* imediato) e, em seguida, `EstatisticaQuestaoService.recalcular()` fazia uma leitura via `JdbcTemplate` bruto **na mesma transação** — que não enxergava a linha ainda não sincronizada com o banco pelo Hibernate. Resultado: a estatística devolvida ao estudante nunca incluía a própria resposta que acabou de disparar o recálculo. **Corrigido** trocando `respostas.save(...)` por `respostas.saveAndFlush(...)`.

**Testes após as correções:** suíte do domínio `estudo` (unidade + integração, incluindo os 15 novos casos) — **94 de 96 testes passando**; os 2 erros restantes eram de `IntegracaoSessaoDeCadernoTest`, um teste **pré-existente e fora do escopo desta funcionalidade**, quebrado por uma classe ausente (`CadernoResumo`/`ConsultaCadernosService`) de outra entrega em andamento (`prd-cadernos-e-desempenho`), não tocada por este trabalho.

> **Atualização 2026-09-05 (mesmo dia, medição posterior):** `ConsultaCadernosService` já existe em
> `br.com.certame.estudo.caderno` e `IntegracaoSessaoDeCadernoTest` **passa (2/2)**. Com isso, a
> suíte do domínio `estudo` está **96/96**, e `SessaoEstudoIntegracaoTest` roda **15/15**.

### Frontend

| Item original | Correção aplicada |
|---|---|
| 30 arquivos fora do padrão Prettier | `npx prettier --write` aplicado; `--check` limpo. |
| Literal mágico `10` (tamanho padrão) repetido | Extraído para a constante `TAMANHO_PADRAO_SERIE` em `estudar-page.ts`. |
| `PainelFiltros` só implementava o campo de quantidade | Reescrito por completo: campos de disciplina, assunto (dependente da disciplina), banca, ano, dificuldade, quantidade, "somente inéditas" e "excluir que já acertei", carregando disciplinas/bancas via `CatalogoApi` e emitindo `RequisicaoSessao` com `filtro` preenchido. Também passou a ser renderizado como coluna lateral durante o modo Contexto (`estudar-page.html`), além da tela de configuração inicial, mais fiel ao protótipo. |
| `QuestaoCartao` nunca exibia metadados, nem em Contexto | Ganhou `input()` `modo` e `computed()` `emContexto`; as tags de disciplina/banca/ano/órgão/dificuldade agora aparecem somente quando `modo() === 'CONTEXTO'`. |
| `PainelNavegacao` sem variação Foco/Contexto | Ganhou `input()` `modo`; renderiza uma grade rotulada "Navegação" em Contexto e a barra compacta original em Foco. |
| `CorrecaoProva` sem `disciplina`/`comentario`/indicação visual de acerto-erro | Template atualizado para exibir os três; classes `acerto`/`erro`/`em-branco` por item com destaque visual (borda colorida) e status textual. |
| Ausência total de testes unitários para os 9 componentes de tela | Implementados: `estudar-page`, `painel-filtros`, `seletor-modo`, `questao-cartao`, `barra-acao`, `gabarito-painel`, `painel-navegacao`, `correcao-prova`, `inicio-page`. |

**Testes após as correções:** `npm test` — **427/427 passando** (80 arquivos, eram 362/71 antes); `npm run test:coverage` — Statements 89,47% · Branches 85,27% · Functions 80,27% · Lines 93,83%, agora **incluindo** os 9 arquivos antes ausentes do relatório.

### Itens de severidade Baixa não corrigidos (recomendação, não bloqueante)

- `SessaoEstudoService` com construtor de 11 dependências (10 originais + `EstudoObservabilidade`) — sinal de responsabilidade concentrada; considerar extrair a abertura via caderno/simulado para um colaborador dedicado em iteração futura.
- Valores `px` crus em `questao-cartao.scss`/`correcao-prova.scss`/`estudar-page.scss`/`inicio-page.scss` — precedente já existente em `styles.css`, não introduzido por esta entrega.
- `PainelNavegacao`/`PainelFiltros` em Contexto não reproduzem o cartão de estatísticas da sessão ("Sessão de hoje: acertos/erros") do protótipo — fora do escopo de requisito (RF/RN) explícito, tratado como melhoria futura.

### Regressão introduzida pela correção — descoberta em 2026-09-05, corrigida em 2026-09-05

A migração aditiva **`V15__sessao_livre_tamanho_variavel.sql`**, criada aqui para corrigir a falha de
produção nº 1, **quebrou um teste que não foi atualizado junto**:
`FlywayMigracaoTest.TI06_tamanhoDeSessaoForaDoDominioEhRecusado` ainda afirmava que `tamanho = 40`
devia ser recusado para sessão `LIVRE`, mas a `V15` passou a aceitar `tamanho between 1 and 50`. O
teste estava desatualizado, não a migração.

Consequência então: **`./mvnw verify` do repositório inteiro terminava em BUILD FAILURE**
(535 testes, 1 falha, 0 erros) e o gate JaCoCo de 80% deixava de ser executado para todos os
domínios. Era o único teste vermelho do backend e pertencia a esta funcionalidade. O QA de
`prd-simulados` já havia encaminhado o achado (BUG-01 daquele relatório).

**Corrigido em 2026-09-05 por `tasks/correcoes-pendencias/task_1.md`:** o teste foi reescrito em
sete casos (`TI06`–`TI12`) que provam o contrato vigente da constraint `sessao_tamanho_valido` para
cada origem (`LIVRE`, `CADERNO`, `SIMULADO`), incluindo o caso positivo `tamanho = 40` para `LIVRE`
que motivou a `V15`. `./mvnw verify` volta a fechar com **541 testes, 0 falhas, BUILD SUCCESS** e o
`jacoco:check` volta a rodar. Evidência em
`tasks/correcoes-pendencias/evidences/task_1-verify.md`. O BUG-01 encaminhado por `prd-simulados`
está resolvido.

### Pendências desta funcionalidade ainda abertas (2026-09-05)

- **`E2E-06` de `frontend/e2e/estudo.spec.ts:98` continua com `test.skip(true, ...)`** alegando que
  "o diálogo de flashcard ainda não existe no repositório". **A alegação é falsa hoje**:
  `FlashcardDialog` existe e está integrado em `estudar-page.ts:16,108`. O cenário RF-28 é coberto
  de fato pela suíte de `prd-flashcards-criacao-e-gerenciamento`, mas o *skip* e sua justificativa
  ficaram obsoletos nesta suíte.
- Itens de severidade Baixa do adendo (IDs `TU` reusados, `SessaoEstudoService` com 11 dependências
  — hoje com **111 linhas**, acima do limite de 100 —, `px` cru em SCSS) seguem como recomendação.

### Veredito revisado

**APROVADO COM RESSALVAS.** Todos os bloqueadores de severidade Alta (violação de regra de arquitetura, testes de integração e de componente ausentes) foram corrigidos e revalidados com testes automatizados passando (backend 94/96, com os 2 restantes pré-existentes e fora de escopo; frontend 427/427). Duas falhas de produção genuínas foram descobertas e corrigidas durante a correção, reforçando o valor dos testes de integração agora implementados. Os itens de severidade Baixa remanescentes são cosméticos/estruturais, não bloqueiam a aprovação, e ficam registrados como recomendação para iteração futura.
