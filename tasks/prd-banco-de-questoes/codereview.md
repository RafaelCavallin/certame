# Relatório de revisão de código — Banco de questões e taxonomia

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Branch: (repositório sem controle de versão Git nesta cópia de trabalho)
- Status: **APROVADO COM RESSALVAS** (mantido)

> **Revalidação 2026-09-05.** As duas correções bloqueantes desta revisão (mensagens pt-BR nos DTOs
> e comparação por `code` em `materias-page`) continuam aplicadas. **Todas as ressalvas médias e
> baixas seguem abertas**, verificadas no código atual:
> `ValidadorPublicacao.java` continua sem checar `disciplinaId`; `ManipuladorErrosGlobal` continua
> **sem** `@ExceptionHandler(DataIntegrityViolationException.class)`; `questao-form-store.ts:32`
> continua usando `window.confirm()`; não existe nenhum `*ControllerTest`/`*IntegracaoTest` em
> `src/test/java/br/com/certame/questao/` ou `catalogo/` provando 403 por perfil no *slice* web;
> `materias-page`, `catalogo-page` e `questoes-page` continuam **sem `*.spec.ts`**; o padrão
> `import('rxjs').then(...)` persiste nesses três arquivos; e `QuestaoService` (127),
> `TaxonomiaService` (130) e `PublicacaoService` (133) seguem acima de 100 linhas.
> Os números da seção "Testes" foram atualizados.

Revisão cobriu os pacotes `catalogo` (exceto `catalogo/edital`, de outra funcionalidade), `questao`, `comum/auditoria` e `comum/PaginaResposta` no backend, e `core/api/{questao-api,catalogo-api,modelos-questao}`, `features/admin/{questoes,materias,catalogo}` e `shared/ui/confirmacao-dialog` no frontend, além das migrações `V2`–`V5` e `V901`. Dois problemas bloqueantes foram encontrados e corrigidos durante a própria revisão (ver *Problemas encontrados*); os testes foram reexecutados depois da correção e continuam 100% verdes.

## Conformidade com regras
| Regra | Status | Observações |
|------|--------|-------------|
| Idioma (código sem acento, mensagens em pt-BR) | NOK → Corrigido | Ver P1: 4 DTOs (`RequisicaoQuestao`, `AlternativaDto`, `RequisicaoNoTaxonomia`, `RequisicaoItemCatalogo`) não traziam `message` em pt-BR nas anotações de Bean Validation; a mensagem padrão do framework (inglês) chegaria ao usuário. Corrigido nesta revisão. |
| Autorização no servidor (`@PreAuthorize`) | OK | Presente em todos os endpoints administrativos (`QuestaoService`, `PublicacaoService`, `TaxonomiaService`, `CatalogoService`), com os perfis corretos (professor+ para CRUD de questão/taxonomia, admin+ para publicar/despublicar e para escrita de banca/órgão). Ver ressalva sobre cobertura de teste (P6). |
| Camadas (Controller/Service/Repository) | OK | Controllers só orquestram; regra de negócio e `@Transactional` ficam no serviço; repositórios Spring Data + JPQL nativo só onde necessário (`QuestaoBuscaRepositoryImpl`). |
| DTOs como `record` imutável | OK (após correção) | Todos os DTOs desta feature são `record`. |
| Entidades JPA (UUID gerado, `Instant`, exclusão lógica, enum como `STRING`) | OK | `Questao`, `Disciplina`, `Assunto`, `Alternativa`, `QuestaoEstatistica`, `Auditoria` seguem o padrão; `excluido_em`/`arquivado_em` usados corretamente; enums com `@Enumerated(EnumType.STRING)`. |
| Erros como subclasse de `ErroNegocioException`, `code` estável | OK no backend | `ConflitoException`, `RecursoNaoEncontradoException`, `PublicacaoInvalidaException` com códigos SNAKE_CASE estáveis. No frontend, um ponto usava o texto da mensagem em vez do `code` — ver P2 (corrigido). |
| Migração só por Flyway, imutável | OK | `V2`–`V5` aditivas; nenhuma migração aplicada foi editada; `V901` isolada em `migration-dev`; a migração de teste `V990` prevista na TechSpec foi corretamente removida e substituída por `V991` quando `prd-sessao-de-estudo` criou as tabelas reais. |
| Nenhum endpoint fora do contrato da Tech Spec §6 | OK | As 11 rotas acrescentadas por esta feature (`enviar-revisao`, `despublicar`, `DELETE` de questão, `arquivar` de assunto, CRUD de `/admin/bancas` e `/admin/orgaos`) estão documentadas no `TECH-SPEC.md` §6 raiz. |
| Arquivos ≤ 100 linhas, métodos ≤ 30, ≤ 3 parâmetros, ≤ 3 níveis de `if` | NOK (menor) | `QuestaoService` (127), `TaxonomiaService` (130) e `PublicacaoService` (133) excedem o limite de 100 linhas — ver P3. Estilo compacto (um membro por linha) é consistente com o restante do projeto; não é regressão introduzida por esta feature isoladamente. |
| Frontend standalone/zoneless, `input()`/`output()`, HTTP só via `core/api` | OK | Todos os componentes são standalone, usam signals, `input()`/`output()` (`ConfirmacaoDialog`), e chamam a API só via `QuestaoApi`/`CatalogoApi`. |
| Frontend: só tokens Nocturne, sem lib de terceiros | OK, com ressalva | Nenhum valor cru nem biblioteca de terceiros. Porém `questao-form-store.trocarTipo()` usa `window.confirm()` nativo do navegador em vez do componente `ConfirmacaoDialog` do design system, criado exatamente para confirmações — ver P4. |
| Sem segredo no código | OK | Nada aplicável nesta feature. |

## Aderência à TechSpec
| Decisão Técnica | Implementado | Observações |
|-----------------|--------------|-------------|
| Transições de status em endpoints próprios (`enviar-revisao`/`publicar`/`despublicar`) | SIM | `PublicacaoService` com `@PreAuthorize` distinto por transição. |
| `ValidadorPublicacao` como regra pura | SIM | Sem dependência de Spring; testado isoladamente. Ver P5 sobre o campo `disciplinaId`. |
| Despublicar move para `EM_REVISAO`, preserva `publicado_em` | SIM | `PublicacaoService.despublicar` não toca em `resposta`/`questao_estatistica`. |
| Exclusão lógica bloqueada por vínculo de resposta | SIM | `QuestaoService.excluir` verifica `countRespostas`. |
| Paginação offset com contagem exata, índice `questao_filtro_idx` | SIM | `QuestaoBuscaRepositoryImpl` e migração `V2` batem com o documentado. |
| Busca por `tsvector` gerado + prefixo de código | SIM | `BuscaQuestaoService` reconhece formato de código via regex e usa `to_tsvector`; `V4` acrescentou `unaccent`, tratado como evolução aditiva legítima. |
| Letras de alternativa atribuídas pelo servidor a partir da `ordem` | SIM | `QuestaoService.letra()` e `unique (questao_id, letra)` no banco. |
| Banca/Órgão como catálogo plano com CRUD do admin (RF-37) | SIM | `CatalogoService`/`CatalogoController`, exclusão física bloqueada por vínculo. |
| `auditoria` chamada explicitamente pelos serviços (não AOP) | SIM | `RegistradorAuditoria.registrar` chamado em todos os pontos de mutação relevantes. |
| Contrato JSON dos DTOs (`QuestaoResumo`, `QuestaoDetalhe`, `DisciplinaArvore`, `ItemCatalogo`, `PaginaResposta`) | SIM | Campos batem entre backend e `modelos-questao.ts` do frontend. |
| Filtros nos *query params*, não em store em memória | SIM | `QuestoesFiltros` sincroniza com `Router`/`ActivatedRoute`. |
| `RequisicaoQuestao.disciplinaId` "exigido apenas para publicar" | PARCIAL | O `check questao_publicada_completa` do banco impõe isso, mas `ValidadorPublicacao` (a camada que deveria produzir o 422 amigável) não verifica `disciplinaId` — ver P5. |

## Tarefas verificadas
| Tarefa | Status | Observações |
|------|--------|-------------|
| 1.0 Lacunas do protótipo e tela ADMIN: CATÁLOGO | COMPLETA | Evidenciado pelas telas em produção e pelas capturas de QA. |
| 2.0 Migração e persistência | COMPLETA | `V2`, `V3` + entidades/repositórios; `V4`/`V5` são evoluções aditivas legítimas (acento e overflow de código). |
| 3.0 Fundação transversal | COMPLETA | `PaginaResposta`, `RegistradorAuditoria`, exceções em `ManipuladorErrosGlobal`. |
| 4.0 Taxonomia e catálogo administrativo | COMPLETA | `TaxonomiaService`, `CatalogoService`, `CatalogoController`, testes `TaxonomiaArvorePersistenciaTest`, `TaxonomiaCatalogoServiceTest`. |
| 5.0 Questão e alternativas | COMPLETA | `QuestaoService`, `GeradorCodigoQuestao`, `QuestaoAdminController`, `QuestaoServiceTest`. |
| 6.0 Ciclo de vida e publicação | COMPLETA | `PublicacaoService`, `ValidadorPublicacao`, `PublicacaoServiceTest`, `ValidadorPublicacaoTest`. |
| 7.0 Busca e listagem administrativa | COMPLETA | `BuscaQuestaoService`, `QuestaoBuscaRepositoryImpl`, testes de serviço, persistência e desempenho. |
| 8.0 Estatística de acerto (RN-03) | COMPLETA | `EstatisticaQuestaoService` já ligado às tabelas reais de `prd-sessao-de-estudo` (migração de teste `V990` corretamente substituída por `V991`). |
| 9.0 Frontend administrativo | COMPLETA, com ressalva | Todas as telas, API clients e rotas presentes. Cobertura de teste unitário direto das páginas (`materias-page`, `catalogo-page`, `questoes-page`) ausente — ver P7. |
| 10.0 E2E e endurecimento | COMPLETA | Suíte Playwright (16 cenários) e métricas de observabilidade (`ObservabilidadeIntegracaoTest`) presentes; QA já registrou aprovação. |

## Testes

### Medição original (2026-09-03)
- Backend (`./mvnw verify`): 509 testes, 0 falhas, 0 erros — cobertura JaCoCo aprovada.
- Backend (subconjunto desta feature): 91 testes, 0 falhas.
- Frontend (`npm run test:coverage`): 362 testes, 0 falhas — 87,49% / 83,62% / 82,89% / 94,93%.
- Reexecutados após as correções desta revisão (P1 e P2): sem regressão.

### Medição vigente (2026-09-05) — substitui a anterior
- Backend (`./mvnw verify`): **535 testes, 1 falha, 0 erros → BUILD FAILURE**. A falha
  (`FlywayMigracaoTest.TI06`) é **alheia a esta funcionalidade** — pertence a `prd-sessao-de-estudo`.
  Todas as classes de `br.com.certame.questao.*` e `br.com.certame.catalogo.*` passam, incluindo
  `TaxonomiaArvorePersistenciaTest` (3/3), que em rodadas anteriores chegou a falhar por
  *discovery*.
- **Consequência**: como o `surefire` interrompe o build antes do `jacoco:check`, **o gate de 80%
  por classe `*Service` não é mais confirmável** pelo build completo.
- Frontend (`npm run test:coverage`): **427 testes em 80 arquivos, 0 falhas** — Statements 89,47% ·
  Branches 85,27% · Functions 80,27% · Lines 93,83%, acima do piso de 80%.

### Medição vigente (2026-09-05, tarefa 6.0 de `tasks/correcoes-pendencias/`) — substitui a anterior
- A falha de `FlywayMigracaoTest.TI06` foi corrigida pela tarefa 1.0 desta mesma pasta de correções.
  `cd backend && ./mvnw verify` (repositório inteiro) roda agora **571 testes, 0 falhas, 0 erros →
  BUILD SUCCESS**, e `jacoco:check` reporta **"All coverage checks have been met."** — o gate de 80%
  volta a ser confirmável no build completo.
- Novos `QuestaoAdminControllerTest` (4/4) e `CatalogoControllerTest` (8/8) fecham a lacuna de
  autorização no slice web apontada abaixo: sobem `PublicacaoService`/`CatalogoService` **reais**
  (não mockados) sob `@EnableMethodSecurity`, provando 403 por perfil insuficiente em
  publicar/despublicar questão e no CRUD de bancas/órgãos.
- Novos `materias-page.spec.ts` (17), `catalogo-page.spec.ts` (17) e `questoes-page.spec.ts` (11)
  fecham a lacuna de teste de unidade das três páginas administrativas.
- Frontend (`npm run test:coverage`): **473 testes, 0 falhas** — Statements 89,45% · Branches
  84,88% · Functions 80,18% · Lines 93,93%, acima do piso de 80%.

## Problemas encontrados
| Severidade | Arquivo | Linha | Descrição | Sugestão |
|------------|---------|-------|-----------|----------|
| Alta (corrigido) | `backend/.../questao/dto/RequisicaoQuestao.java`, `AlternativaDto.java`, `catalogo/dto/RequisicaoNoTaxonomia.java`, `RequisicaoItemCatalogo.java` | várias | Nenhuma anotação de Bean Validation trazia `message` em pt-BR; `ManipuladorErrosGlobal.tratarValidacao` usa `erro.getDefaultMessage()`, então o usuário receberia a mensagem padrão em inglês do framework (viola `code-standards.md` e a regra 3 do `AGENTS.md`). | **Corrigido nesta revisão**: mensagens em pt-BR adicionadas a todas as constraints. |
| Média (corrigido) | `frontend/.../materias-page/materias-page.ts` | antiga linha 14/18 | `excluir()` detectava "assunto em uso" comparando `mensagem.includes('questões vinculadas')` em vez do `erro.code === 'ASSUNTO_EM_USO'` — viola o contrato explícito do `code-standards.md` ("a `message` pode mudar, o `code` não"). | **Corrigido nesta revisão**: passou a usar `extrairErroApi`/`corpoErro` de `core/api/erro-api.ts` e comparar pelo `code`. |
| Média | `backend/.../questao/PublicacaoService.java`, `ValidadorPublicacao.java` | 10-24 | `ValidadorPublicacao` não verifica `disciplinaId`, embora a TechSpec declare o campo "exigido apenas para publicar" e o `check questao_publicada_completa` do banco também exija `disciplina_id`. Se uma questão sem disciplina completar os outros três requisitos e for publicada, o `save()` falha no `check` do Postgres, e `ManipuladorErrosGlobal` não trata `DataIntegrityViolationException` — o resultado é um 500 cru em vez do 422 amigável do padrão `PUBLICACAO_INVALIDA`. O PRD (CA-06) não lista `disciplina` entre as pendências, então pode ser decisão deliberada; ainda assim, a ausência de um handler genérico para violação de integridade é um ponto cego de robustez. | Confirmar com o PO se `disciplina` deveria ser uma quarta pendência de `ValidadorPublicacao`; independentemente da resposta, adicionar um `@ExceptionHandler(DataIntegrityViolationException.class)` em `ManipuladorErrosGlobal` como rede de segurança genérica. |
| Média | `frontend/.../questoes/questao-form-store.ts` | 32 | `trocarTipo()` usa `window.confirm()` nativo do navegador para confirmar a troca de tipo (ME↔CE), em vez do componente `ConfirmacaoDialog` do design system Nocturne — o mesmo componente já usado em `questoes-page`, `materias-page` e `catalogo-page` para outras confirmações. Risco de violar a regra 1 do `AGENTS.md` (tradução 1:1 do protótipo, sem elementos fora do design system) e de inconsistência visual/acessibilidade (diálogo nativo não segue os tokens Nocturne). | Substituir por `ConfirmacaoDialog`, como já feito nas demais telas da mesma feature. |
| Média (corrigida na tarefa 6.0) | `backend/src/test/java/br/com/certame/questao/`, `catalogo/` | — | Não havia teste `*ControllerTest`/`*IntegracaoTest` que exercitasse `QuestaoAdminController`/`CatalogoController` via HTTP real provando 403 para perfil insuficiente — os testes de serviço eram unitários com mocks e não passavam pelo proxy do `@PreAuthorize`. | **Corrigido:** `QuestaoAdminControllerTest.java` e `CatalogoControllerTest.java` sobem os serviços reais sob `@EnableMethodSecurity` e provam 403 em publicar/despublicar e no CRUD de bancas/órgãos. |
| Baixa (corrigida na tarefa 6.0) | `frontend/.../materias-page.ts`, `catalogo-page.ts`, `questoes-page.ts` | — | Nenhum dos três componentes de página tinha `*.spec.ts` próprio. | **Corrigido:** `materias-page.spec.ts`, `catalogo-page.spec.ts` e `questoes-page.spec.ts` cobrem diálogos, erro por `code` e troca de aba. |
| Baixa | `frontend/.../questoes-page.ts`, `materias-page.ts`, `catalogo-page.ts` | várias | Uso repetido de `import('rxjs').then(({ firstValueFrom }) => ...)` em vez de um `import { firstValueFrom } from 'rxjs'` estático no topo do arquivo (como já faz `questao-form-store.ts`) — *code-splitting* desnecessário para uma função trivial já empacotada, e inconsistente dentro da própria feature. | Trocar pelo import estático, igual ao já usado em `questao-form-store.ts`. |
| Baixa | `backend/.../questao/QuestaoService.java` (127), `catalogo/TaxonomiaService.java` (130), `questao/PublicacaoService.java` (133) | — | Excedem o limite de 100 linhas de `code-standards.md` §2.2. O estilo compacto (um membro por linha física) é consistente com todo o restante do projeto e não é uma regressão desta feature; ainda assim, os três arquivos mais longos do domínio já nascem acima do limite. | Se o padrão do projeto mudar para exigir o limite estrito, extrair métodos privados relacionados a auditoria/detalhe em uma classe auxiliar. Não bloqueante. |

## Pontos positivos
- Ciclo de vida da questão (RASCUNHO → EM_REVISAO → PUBLICADA) implementado exatamente como especificado, com rede de segurança redundante no banco (`questao_publicada_completa`) além da validação de serviço.
- `ValidadorPublicacao` mantido como função pura sem dependência de Spring, testável em milissegundos e devolvendo todas as pendências de uma vez, como pede CA-06.
- Auditoria explícita em todos os pontos de mutação (criar, editar, excluir, transições de status, taxonomia, catálogo), sem mágica de AOP — decisão de design bem executada.
- Migrações `V4` e `V5` são bons exemplos de correção aditiva pós-produção (busca com acento, overflow de sequência), sem qualquer edição de migração já aplicada, com comentário explicando o *porquê* do problema original.
- Débito técnico da F2 (migração de teste `V990`) foi corretamente resolvido quando `prd-sessao-de-estudo` criou as tabelas reais — a dívida registrada na TechSpec não ficou esquecida.
- Suíte de testes ampla e íntegra: 509 testes de backend e 362 de frontend, cobertura acima do piso de 80% nos dois lados, todos passando após as correções desta revisão.

## Recomendações
- Tratar as ressalvas médias (P3–P6) antes do próximo ciclo de endurecimento, priorizando o handler genérico de `DataIntegrityViolationException` e os testes de autorização no slice web, por serem os que mais reduzem risco em produção.
- Ao criar novos DTOs em features futuras, revisar sistematicamente se toda anotação de Bean Validation tem `message` em pt-BR — este é o segundo ciclo de revisão em que esse padrão precisa ser corrigido manualmente; vale considerar um teste de arquitetura (ArchUnit) que falhe o build se uma anotação de validação não tiver `message`.

## Conclusão
A funcionalidade implementa fielmente o que foi especificado na TechSpec e no PRD: taxonomia, catálogo, ciclo de vida de questão, busca, estatística e as quatro telas administrativas, com auditoria e autorização por perfil consistentes. Dois problemas bloqueantes foram identificados nesta revisão — mensagens de validação sem tradução para pt-BR e uma comparação de erro por texto em vez de `code` — e ambos foram corrigidos e revalidados (509 testes de backend e 362 de frontend passando, cobertura acima do piso de 80% nos dois lados). As ressalvas remanescentes (robustez do check de publicação sem `disciplina`, uso de `window.confirm` nativo, lacunas de teste de autorização no slice web e de componentes de página no frontend) são melhorias recomendadas, não bloqueiam a entrega. Veredito: **APROVADO COM RESSALVAS**.
