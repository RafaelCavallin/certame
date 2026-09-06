# Relatório de revisão de código — Flashcards: criação no fluxo da questão e gerenciamento

## Resumo
- Data: 2026-09-03 · **Revalidado em 2026-09-05**
- Branch: (repositório sem controle de versão git nesta cópia de trabalho)
- Status: **APROVADO COM RESSALVAS** (mantido)

> **Revalidação 2026-09-05.** As conclusões de segurança e arquitetura seguem válidas
> (`VerificadorDonoFlashcard` como porta única, RF-28 como diálogo, FSRS intocado).
> **Correções factuais neste relatório:**
>
> 1. **A tarefa 2.0 cita `V991__sessao_estudo_minima.sql` como entregue.** Esse arquivo **não existe
>    mais** — `backend/src/test/resources/db/migration-test/` está vazio. A migração de teste foi
>    removida durante as correções de `prd-sessao-de-estudo` (2026-09-05), que era exatamente o
>    destino previsto para ela. Não há impacto: as suítes de flashcard passam contra o schema real.
> 2. **Números das suítes e do `verify`** — atualizados na seção "Testes".
> 3. **As ressalvas de manutenibilidade continuam abertas**, confirmadas no código atual:
>    `Flashcard.java` **199 linhas**, `FlashcardService.java` **134**, `FlashcardRepository.java`
>    **119**; `RequisicaoFlashcard` continua sem Bean Validation; e `npx prettier --check` falha em
>    **191 arquivos** do frontend — o desvio de formatação cresceu desde a revisão original.

O código implementa integralmente as 10 tarefas de `tasks.md`, os 16 critérios de aceitação (CA-01…CA-16) e o contrato de 10 rotas da TechSpec. A suíte de backend (515 testes, JUnit/Mockito/Testcontainers) passa 100%; a suíte de frontend (362 testes, Vitest) passa 100% com cobertura acima de 80% em todas as métricas. Não foram encontrados problemas de segurança, de privacidade (RN-07) ou de arquitetura (RF-28: diálogo nunca rota). As ressalvas são de qualidade/manutenibilidade de código (arquivos que ultrapassam o limite de 100 linhas do `code-standards.md` e formatação Prettier não aplicada em parte do frontend tocado por esta feature), não bloqueiam a aprovação.

## Conformidade com regras
| Regra | Status | Observações |
|------|--------|-------------|
| 1. Frontend é tradução 1:1 do protótipo; só tokens `var(--*)`; sem lib de UI de terceiros | OK | Lacunas do protótipo (multisseleção, "+ Novo deck", modo avulso) foram compostas em `Certame.dc.html` com componentes/tokens existentes (tarefa 1.0) antes da implementação Angular, conforme exigido. |
| 2. Autorização sempre validada no servidor (`@PreAuthorize`) | OK | `FlashcardService`, `ConsultaFlashcardsService`, `ConsultaFilaFlashcards` e `DeckService` têm `@PreAuthorize("isAuthenticated()")` a nível de classe; a interface some com ações, mas o servidor é a autoridade real, provado por `FlashcardPrivacidadeIntegracaoTest` (TI-16…TI-20, TI-26) e `TI25_todasAsRotasSemTokenRespondem401`. |
| 3. Interface/mensagens em pt-BR; código/tabelas/commits sem acento | OK | Identificadores, tabelas (`flashcard`, `deck`, `flashcard_revisao`) e mensagens de erro conferidos; nenhum acento em identificador. |
| 4. Nenhum endpoint fora do contrato da Tech Spec §6 sem atualizar o documento | OK | As 3 rotas que não constavam da TechSpec original (`GET /flashcards/{id}`, `POST /flashcards/{id}/restaurar`, `POST /flashcards/decks/{id}/mover`) foram registradas na `TECH-SPEC.md` raiz (linhas 308-318) na tarefa 10.0, como exigido. |
| 5. Migração só por Flyway, versionada e imutável | OK | `V13__flashcards.sql` e `db/migration-dev/V908__seed_flashcards_dev.sql` (numeração real, diferente da estimativa `V6`/`V902` da TechSpec da feature — divergência documentada e justificada em `tasks.md`, `task_2.md`, `task_10.md` e na `TECH-SPEC.md` raiz, linha 393: colisão com migrações aplicadas depois pelas fases irmãs `prd-simulados`/`prd-editais`). Nenhuma migração aplicada foi editada; `V14__flashcards_revisao.sql` (de `prd-flashcards-decks-e-revisao`, já implementada no repositório) só adiciona colunas/tabelas, não altera `V13`. |
| 6. `resposta` imutável; Modo Prova só conta após finalização | OK (não aplicável a escrita) | Nada nesta feature escreve em `resposta`; criação de flashcard em Modo Prova é inerte para a correção (TI-10, E2E-01). |
| 7. Flashcards privados do dono (RN-07); 404 nunca 403; FSRS intocado | OK | `VerificadorDonoFlashcard.exigirProprio` é o único ponto de leitura por id, com filtro `usuarioId` dentro do `where` e mesma exceção/mensagem para inexistente, excluído e de terceiro. `TI16_leituraDeCardDeOutroUsuarioE404IdenticoAoInexistente` compara os corpos byte a byte. Nenhum peso/algoritmo FSRS foi alterado (o bloco FSRS nasce zerado na criação e não é tocado pela edição). |
| 8. Criação de flashcard na sessão é diálogo, nunca rota (RF-28) | OK | `FlashcardDialog` e `FlashcardFormStore` não importam `Router`, `location` nem `StudySessionStore` (conferido nos imports dos dois arquivos). `app.routes.ts` não tem rota de criação. `barra-acao.ts` chama `DialogService.abrir` sem navegação. |
| 9. Fila de revisão respeita limites do deck; excluir deck não apaga cards (RN-11) | OK (fundação) | `deck` nasce com as 4 colunas de limite nos padrões de RN-11; `on delete set null` em `flashcard.deck_id` garante que exclusão de deck (função de `prd-flashcards-decks-e-revisao`, já implementada) preserva os cards. RN-10/RN-11 comportamentais são de outra feature — corretamente fora do escopo desta revisão. |
| `code-standards.md` §2.2 — arquivos ≤ 100 linhas, métodos ≤ 30 | **NOK (ressalva)** | Ver "Problemas encontrados" — vários arquivos do backend e do frontend tocados por esta feature excedem 100 linhas, e alguns arquivos do frontend "resolvem" o limite comprimindo várias instruções por linha física, o que piora a legibilidade em vez de forçar a divisão de responsabilidades pretendida pela regra. |
| `code-standards.md` §2.1 — sem comentários desnecessários | OK | Nenhum comentário supérfluo nos arquivos centrais da feature (`flashcard/*.java`); o único comentário de `Flashcard.java` explica uma decisão não óbvia (coerência do `check` de agenda), aceitável pela regra. |
| Formatação Prettier (`printWidth: 100`) | **NOK (ressalva, não bloqueante)** | `npx prettier --check` falha em ~20 arquivos tocados por esta feature (e em arquivos de features irmãs não relacionadas, indicando problema de disciplina de formatação no projeto como um todo, não introduzido exclusivamente aqui). |
| Cobertura mínima 80% | OK no frontend · **backend NÃO CONFIRMÁVEL em 2026-09-05** | Frontend: 89,47% statements / 85,27% branches / 80,27% functions / 93,83% lines. Backend: o `verify` foi confirmado em 2026-09-03; em 2026-09-05 o build falha antes do `jacoco:check` por `FlywayMigracaoTest.TI06` (defeito de outra funcionalidade). |

## Aderência à TechSpec
| Decisão Técnica | Implementado | Observações |
|-----------------|--------------|-------------|
| Esquema `deck`/`flashcard`/`flashcard_revisao` com os 3 `check` de coerência, 5 índices e `busca tsvector` gerado | SIM | `V13__flashcards.sql` reproduz exatamente o esquema da TechSpec (com a numeração real V13, documentada). |
| `flashcard_revisao` sem `on delete cascade` | SIM | Confirmado na migração; `TI20_excluirCardComDuasRevisoesRegistradasPreservaHistorico` prova a preservação. |
| 10 rotas de `/api/v1/flashcards` | SIM | `FlashcardController` (7 rotas) + `DeckController` (3 rotas: `GET`/`POST /decks`, `POST /decks/{id}/mover`) — o `DeckController` real tem rotas adicionais (`PUT`/`DELETE /decks/{id}`) pertencentes a `prd-flashcards-decks-e-revisao`, corretamente fora do escopo desta feature. |
| `VerificadorDonoFlashcard` como ponto único de leitura por id | SIM | Nenhum outro componente da feature chama `FlashcardRepository.findById` diretamente (conferido por leitura de `FlashcardService`, `ConsultaFlashcardsService`, `DeckService`). |
| Indicadores dentro do envelope da listagem, alheios aos filtros | SIM | `ConsultaFlashcardsService.listar` calcula os 5 indicadores sempre sobre `ATIVO` do usuário, independente do `FiltroFlashcards` aplicado à página. |
| Ordenação por lista branca (`criadoEm`, `proximaRevisao`) | SIM | Validada em `ConsultaFlashcardsService.validarOrdenacao`; `TI15_recusaSortForaDaListaBranca` cobre o caso `frente,asc`. |
| Contador `criadosNaSessao` vem do servidor, nunca incremento local | SIM | `FlashcardService.criar` conta por `sessaoOrigemId` no banco (`countByUsuarioIdAndSessaoOrigemId`); frontend usa `RespostaFlashcardCriado.criadosNaSessao` propagado pelo evento `criado`, sem soma local. |
| Diálogo não conhece a sessão de estudo | SIM | Ver Regra 8 acima. |
| Edição não reagenda (não toca no bloco FSRS) | SIM | `FlashcardService.atualizar` só altera `frente`, `verso`, `deck`, `tags` e, se avulso, `disciplina`/`assunto`. |
| "Sem deck" é `deck_id is null`, não linha reservada | SIM | `FiltroFlashcards.SEM_DECK` = `"sem-deck"`, tratado em `FlashcardBuscaRepositoryImpl.aplicarDeck` como `deck_id is null`. |
| Sem N+1 na listagem | SIM (indireto) | Não há `join fetch` explícito na busca nativa paginada, mas `hibernate.default_batch_fetch_size=100` (global, `application.properties`) evita N+1 real para as relações lazy (`deck`, `disciplina`, `assunto`, `questaoOrigem`) dentro do tamanho máximo de página (100); `TI27_listagemDe500CardsNaoGeraNMaisUmSobreDeckDisciplinaEQuestao` cobre o caso. |
| Numeração real das migrações registrada na TechSpec (regra 4) | SIM | `TECH-SPEC.md` raiz, linha 393, documenta `V13__flashcards.sql`/`V908__seed_flashcards_dev.sql` e o motivo da divergência com a estimativa da TechSpec da feature. |

## Tarefas verificadas
| Tarefa | Status | Observações |
|------|--------|-------------|
| 1.0 Lacunas do protótipo | COMPLETA | Multisseleção, "+ Novo deck", modo avulso, rótulos textuais de estado e os dois estados vazios presentes em `Certame.dc.html`; sem componente/token novo. |
| 2.0 Migração e persistência | COMPLETA | `V13__flashcards.sql`, `V908__seed_flashcards_dev.sql`, entidades `Flashcard`/`Deck`, enums, `FlashcardRepository`/`DeckRepository`. TI-01…TI-05 presentes em `FlashcardEsquemaIntegracaoTest`. **Correção 2026-09-05:** a migração de teste `V991__sessao_estudo_minima.sql`, citada aqui originalmente, **foi removida** por `prd-sessao-de-estudo` — as suítes de flashcard passam contra o schema real (`V7`). |
| 3.0 Regras de domínio puras | COMPLETA | `VerificadorDonoFlashcard`, `NormalizadorTags`, `ResolvedorContextoFlashcard`, `SessaoDoUsuario`/`SessaoDoUsuarioProvisoria`. TU-01…TU-06, TU-21…TU-24 presentes. |
| 4.0 Serviço de escrita | COMPLETA | `FlashcardService` com os 5 verbos; DTOs `RequisicaoFlashcard`, `FlashcardResumo`, `RespostaFlashcardCriado`, `OrigemResumo`, `ReferenciaResumo`. TU-07…TU-20 presentes em `FlashcardServiceTest`. |
| 5.0 Leitura, indicadores, fila e decks | COMPLETA | `ConsultaFlashcardsService`, `ConsultaFilaFlashcards`, `DeckService` (listar/criar/mover), `FiltroFlashcards` e DTOs. TU-25…TU-30 presentes. |
| 6.0 Controllers e contrato HTTP | COMPLETA | `FlashcardController`, `DeckController`. TI-06…TI-27 presentes em `FlashcardControllerIntegracaoTest`/`FlashcardPrivacidadeIntegracaoTest`/`DeckControllerIntegracaoTest`. |
| 7.0 `shared/ui/dialog` e `shared/ui/toast` | COMPLETA | `DialogService`/`Dialog` com `role="dialog"`, `aria-modal`, armadilha de foco, `Esc`; `ToastStore`/`Toast` com `role="status"`. TU-39, TU-40, TU-45 presentes. |
| 8.0 Modal de criação de flashcard | COMPLETA | `FlashcardDialog`, `FlashcardFormStore`, `rascunho-flashcard.ts`, `DeckSeletor`, `core/api/flashcard-api.ts`/`modelos-flashcard.ts`. TU-31…TU-38 presentes. |
| 9.0 Tela `/flashcards` | COMPLETA | `FlashcardsStore`, `FlashcardsPage`, `FlashcardsFiltros`, `FlashcardsIndicadores`, `MoverParaDeck`, rotas e item de menu. TU-41…TU-44 presentes. Inclui a correção documentada de `TaxonomiaService.arvore()` (bug real de QA manual, corrigido dentro da própria tarefa). |
| 10.0 Integração com a sessão, E2E, endurecimento | COMPLETA (com retenção documentada) | Botão "Novo flashcard" em `barra-acao`; `frontend/e2e/flashcards.spec.ts` com os 16 cenários (E2E-13 corretamente `test.skip` por depender de rota `/estudar?questao=` fora do escopo de F6, documentado); `TECH-SPEC.md` atualizada (§5, §6, numeração real das migrações). |

Todos os CA-01…CA-16 estão rastreados por pelo menos um teste automatizado e, adicionalmente, validados por QA manual/E2E (`qa.md`, veredito APROVADO, 16/16 critérios, 0 bugs de produto).

## Testes

### Medição original (2026-09-03)
- Backend: 515 testes (`./mvnw test`), 0 falhas, 0 erros.
- Frontend: 362 testes, 0 falhas — 87,49% / 83,62% / 82,89% / 94,93%.
- Cobertura backend (JaCoCo, `./mvnw verify`): `BUILD SUCCESS`, "All coverage checks have been met".
  Agregado 88,72% de linhas; pacote `br.com.certame.flashcard*` 95,39% de linhas.

### Medição vigente (2026-09-05) — substitui a anterior
- Backend (`./mvnw verify`, repositório inteiro): **535 testes, 1 falha, 0 erros → BUILD FAILURE**.
  **Escopo de flashcards: 0 falhas.** A falha é `FlywayMigracaoTest.TI06`, defeito de
  `prd-sessao-de-estudo`.
- **Gate JaCoCo: não confirmável.** O `surefire` interrompe o build antes do `jacoco:check`, então a
  afirmação "All coverage checks have been met" **não pode ser reproduzida hoje**.
- Frontend (`npm run test:coverage`): **427 testes em 80 arquivos, 0 falhas** —
  Statements 89,47% · Branches 85,27% · Functions 80,27% · Lines 93,83%.
- Docker/Testcontainers: disponível e utilizado normalmente; nenhum container órfão.

## Problemas encontrados
| Severidade | Arquivo | Linha | Descrição | Sugestão |
|------------|---------|-------|-----------|----------|
| Média | `backend/.../flashcard/Flashcard.java` | 1-199 | Entidade com 199 linhas, quase o dobro do limite de 100 linhas de `code-standards.md` §2.2 (comparativamente, `Usuario.java` tem 97 e `Questao.java` 75). O bloco FSRS + as marcas de auditoria concentram muitos campos/getters/setters em uma única classe. | Extrair um `@Embeddable` para o bloco de agendamento FSRS (estabilidade, dificuldade, intervalo, repetições, lapsos, `passoAprendizado`, `proximaRevisao*`), separando-o das colunas de conteúdo/autoria do card. |
| Média | `backend/.../flashcard/FlashcardService.java` | 1-134 | Serviço com 134 linhas, acima do limite. | Extrair a validação de texto (`validarTextos`) para um validador dedicado, nos moldes de `NormalizadorTags`/`VerificadorDonoFlashcard`. |
| Média | `backend/.../flashcard/FlashcardRepository.java` | 1-119 | Repositório com 119 linhas; concentra consultas de listagem, fila, indicadores e movimentação de deck. | Não bloqueante — considerar mover as consultas de indicadores/contagem por deck para uma interface de leitura dedicada quando `prd-flashcards-decks-e-revisao` crescer mais sobre este arquivo. |
| Média | `frontend/.../flashcard-form-store.ts`, `flashcards-page.ts`, `flashcards-filtros.ts`, `barra-acao.ts`, `barra-acao.html` (e outros) | várias | Para caber no limite de 100 linhas por arquivo, vários componentes desta feature comprimem múltiplas instruções na mesma linha física (ex.: `barra-acao.ts` linha 3 tem 364 caracteres; 33 das 100 linhas de `flashcard-form-store.ts` excedem 100 colunas). Isso cumpre a letra da regra de 100 linhas mas contraria o objetivo dela (forçar divisão de responsabilidades) e piora a legibilidade/revisão de diffs. `npx prettier --check` falha nesses arquivos. | Rodar `npx prettier --write` nos arquivos tocados por esta feature e, onde o arquivo formatado ultrapassar 100 linhas, dividir por responsabilidade (ex.: separar `dados()`/`validar()`/`aplicarErro()` de `FlashcardFormStore` em um mapeador de formulário à parte) em vez de comprimir linhas. |
| Baixa | Diversos arquivos fora desta feature (`features/cadernos/*`, `features/simulados/*`, `core/api/caderno-api.ts`, `core/api/simulado-api.ts`) | — | `npx prettier --check` também falha nesses arquivos de features irmãs, indicando que a formatação Prettier não é aplicada/verificada consistentemente no projeto como um todo — não é uma regressão introduzida por esta feature, mas contextualiza o achado acima. | Considerar adicionar `prettier --check` como gate de CI ou hook de pre-commit no projeto. |
| Baixa | `backend/.../flashcard/dto/RequisicaoFlashcard.java` | 1-15 | DTO sem anotações de Bean Validation (`@NotBlank`/`@Size`), diferente do padrão geral descrito em `code-standards.md` §3 ("DTO é sempre record... com as anotações de Bean Validation"); a validação de `frente`/`verso` é feita manualmente em `FlashcardService.validarTextos`. | Decisão tecnicamente justificada (o recorte de espaços precisa ocorrer antes da medida de tamanho, o que `@Size` sozinho não faz) — considerar registrar essa decisão na TechSpec para deixar explícito que é intencional, não uma omissão. |

Nenhum problema de severidade **Alta** foi encontrado: a privacidade por dono (RN-07), a imutabilidade do FSRS, a natureza de diálogo da criação (RF-28) e a preservação de histórico na exclusão (CA-12) estão todas corretamente implementadas e testadas.

## Pontos positivos
- `VerificadorDonoFlashcard` como porta única de leitura por id é uma escolha de design exemplar para RN-07: torna a privacidade uma propriedade estrutural, não uma disciplina a lembrar em cada serviço.
- `FlashcardPrivacidadeIntegracaoTest` compara corpos de resposta byte a byte entre card de terceiro e card inexistente (`TI16`), e prova especificamente que super-admin recebe 404 (não 403) ao tentar ler/editar/excluir card de outro usuário (`TI26`) — evidência forte contra vazamento de existência de recurso.
- `FlashcardDialog`/`FlashcardFormStore` não importam `Router`, `location` nem `StudySessionStore` — RF-28 é verificável por leitura dos imports, exatamente como a TechSpec pretendia.
- Divergência de numeração de migração (`V6`→`V13`, `V902`→`V908`) tratada corretamente em todos os lugares: `tasks.md`, `task_2.md`, `task_10.md` e a `TECH-SPEC.md` raiz foram atualizados de forma consistente, sem editar migração já aplicada.
- E2E-13 retido por dependência real de fora do escopo (`rota /estudar?questao=` de F2), com fallback de interface já implementado (link desabilitado com rótulo explicativo) — decisão registrada, não omitida.
- QA já validou os 16 critérios de aceitação ponta a ponta com 0 bugs de produto.

## Recomendações
- Extrair o bloco FSRS de `Flashcard.java` para um `@Embeddable` dedicado, reduzindo a entidade principal para perto do limite de 100 linhas sem perder coesão.
- Rodar `npx prettier --write` nos arquivos desta feature (backend não é afetado, é só frontend) e, ao invés de comprimir linhas para caber em 100, dividir responsabilidades como a regra pretende.
- Registrar na TechSpec a decisão deliberada de validar `frente`/`verso` manualmente no serviço em vez de Bean Validation no DTO.
- Considerar adicionar `prettier --check` ao pipeline de CI do projeto, já que o desvio de formatação é generalizado e não específico desta feature.

## Conclusão
A implementação está completa, aderente à TechSpec e às regras de negócio críticas (privacidade por dono, imutabilidade do FSRS, diálogo em vez de rota), com testes automatizados robustos e QA já aprovado sem bugs de produto. As ressalvas identificadas são de manutenibilidade de código (tamanho de arquivo e formatação), não de correção funcional, segurança ou arquitetura, e não impedem o uso em produção. Recomenda-se endereçá-las em uma limpeza técnica pontual, sem bloquear o fechamento desta entrega.

**Veredito: APROVADO COM RESSALVAS.**
