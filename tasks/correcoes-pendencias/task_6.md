# Tarefa 6.0 (P2-3): Fechar as seis lacunas de cobertura

## Visão geral

Seis lacunas de cobertura seguem abertas em funcionalidades já aprovadas. Nenhuma é um defeito de
comportamento — são ausências de prova automatizada sobre código que já existe e roda em produção.
Esta tarefa depende da 1.0: o `jacoco:check` precisa voltar a rodar para que o piso de 80% seja
verificável em cada subtarefa.

| Lacuna | Funcionalidade | O que falta ser provado |
|---|---|---|
| `ReconciliadorAgregados` | cadernos-e-desempenho | Nenhum teste do repositório menciona a classe. O reconciliador de 15 minutos é a rede de segurança dos agregados de desempenho e não tem nenhuma prova automatizada de que reconstrói um agregado divergente e limpa a pendência |
| Autorização no *slice* web de questão/catálogo | banco-de-questoes | Não existe nenhum `*ControllerTest`/`*IntegracaoTest` em `questao/` ou `catalogo/`. Os testes de serviço são unitários com *mocks* e **não passam pelo proxy do `@PreAuthorize`** — o 403 por perfil insuficiente em publicar/despublicar e no CRUD de bancas/órgãos só é provado hoje pelo E2E |
| Contrato HTTP de `/cadernos` e `/desempenho` | cadernos-e-desempenho | Não há *slice* web para `CadernoController`/`DesempenhoController`: nenhum teste sobe a cadeia de segurança para provar 401 sem token nem o formato do envelope de erro 409/422 |
| Páginas administrativas sem teste de unidade | banco-de-questoes | `materias-page`, `catalogo-page` e `questoes-page` não têm `*.spec.ts`. A orquestração de diálogos, o tratamento de erro por `code` e a troca de aba ficam sem prova, embora a cobertura agregada passe |
| Caminho de falha de `EnviadorEmailSmtp` | acesso-e-conta | Não existe `src/test/java/br/com/certame/email/`. O laço de retentativa que sustenta a garantia de "e-mail nunca falha visivelmente" (RF-03/RF-04) nunca é exercitado |
| Piso de 80% de decks/revisão | flashcards-decks-e-revisao | O comando registrado no `codereview.md` era `./mvnw verify -DskipTests`, que **pula os testes e não gera relatório JaCoCo**. O gate nunca foi verificado para este domínio |

<rules>
### Conformidade com o AGENTS.md e as rules

- `AGENTS.md` regra 2 — toda autorização é validada no servidor com `@PreAuthorize`; a subtarefa 6.2
  existe justamente porque isso só é hoje provado pelo E2E, não pelo `@PreAuthorize` real via slice
  web (mocks não passam pelo proxy do Spring Security).
- `.agents/rules/tests.md` §4 (pirâmide de testes) — slice web (`@WebMvcTest`/`@SpringBootTest` com
  `MockMvc`) é a camada correta para provar 401/403/409/422 pela cadeia de segurança real.
- `.agents/rules/tests.md` §1 — piso de 80% por módulo; a subtarefa 6.6 é a única forma de saber se
  flashcards/revisão cumpre isso, já que `-DskipTests` nunca gerou o relatório.
</rules>

## Subtarefas

- [x] 6.1 Escrever teste para `desempenho/ReconciliadorAgregados`: criar uma
  `DesempenhoPendencia`, rodar `reconciliar()`, e provar que (a) o agregado divergente é
  reconstruído via `AtualizadorAgregados`, (b) a pendência é removida (`pendencias.deleteById`), e
  (c) uma falha em um item do lote (`RuntimeException` capturada em `reconstruir`) não impede o
  processamento dos demais itens do lote.
  `backend/src/test/java/br/com/certame/desempenho/ReconciliadorAgregadosTest.java` (3/3).
- [x] 6.2 Criar testes de slice web para `questao/` e `catalogo/` (`*ControllerTest` com
  `@WebMvcTest` ou equivalente que suba `@PreAuthorize`): provar 403 por perfil insuficiente em
  publicar/despublicar questão e no CRUD de bancas/órgãos, cobrindo os perfis que a TechSpec define
  como insuficientes para cada ação.
  `QuestaoAdminControllerTest.java` (4/4) e `CatalogoControllerTest.java` (8/8) — sobem
  `PublicacaoService`/`CatalogoService` reais sob `@EnableMethodSecurity` (não mockados), para que o
  proxy do `@PreAuthorize` seja de fato exercitado.
- [x] 6.3 Criar testes de slice web para `CadernoController` e `DesempenhoController`: provar 401 sem
  token e o formato do envelope de erro para os casos 409/422 já cobertos nos testes de serviço.
  `CadernoControllerTest.java` (4/4) e `DesempenhoControllerTest.java` (4/4).
- [x] 6.4 Criar `materias-page.spec.ts`, `catalogo-page.spec.ts` e `questoes-page.spec.ts` cobrindo
  orquestração de diálogos (abrir/fechar/confirmar), tratamento de erro por `code` (ex.:
  `ASSUNTO_EM_USO`, `QUESTAO_EM_USO`) e troca de aba.
  17/17, 17/17 e 11/11 testes respectivamente; inclui testes de renderização (DOM) para exercitar as
  funções compiladas dos templates e manter o piso de 80% de `functions` no frontend.
- [x] 6.5 Criar `backend/src/test/java/br/com/certame/email/` com teste para
  `EnviadorEmailSmtp`, exercitando o laço de retentativa (sucesso após N falhas, esgotamento de
  tentativas) que sustenta RF-03/RF-04.
  `EnviadorEmailSmtpTest.java` (3/3).
- [x] 6.6 Rodar `cd backend && ./mvnw verify` (sem `-DskipTests`) e confirmar/registrar o percentual
  real de cobertura do domínio de flashcards/revisão. Se ficar abaixo de 80%, abrir subtarefas
  adicionais aqui mesmo para fechar a diferença — não adiar para outra tarefa.
  `./mvnw verify` roda 571/571 testes e `jacoco:check` reporta "All coverage checks have been met.".
  Cobertura de instruções por pacote (JaCoCo): `flashcard` 93%, `flashcard.deck` 95%,
  `flashcard.fsrs` 99%, `flashcard.revisao` 95% — todos acima de 80%, nenhuma subtarefa adicional
  necessária.
- [x] 6.7 Atualizar os `qa.md`/`codereview.md` de `prd-cadernos-e-desempenho`, `prd-banco-de-questoes`,
  `prd-acesso-e-conta` e `prd-flashcards-decks-e-revisao` com as evidências novas.

## Critérios de aceitação relacionados

- Nenhum CA de produto muda — esta tarefa só adiciona prova automatizada sobre comportamento já
  aprovado.

## Testes da tarefa

- Um teste novo por subtarefa (6.1 a 6.6), listados acima.

## Arquivos relevantes

- `backend/src/test/java/br/com/certame/desempenho/` (novo teste)
- `backend/src/test/java/br/com/certame/questao/`, `backend/src/test/java/br/com/certame/catalogo/` (novos `*ControllerTest`)
- `backend/src/test/java/br/com/certame/email/` (pasta nova)
- `frontend/src/app/features/admin/materias/`, `.../catalogo/`, `.../questoes/` (novos `*.spec.ts`)
- `tasks/prd-cadernos-e-desempenho/qa.md`, `tasks/prd-banco-de-questoes/qa.md`,
  `tasks/prd-acesso-e-conta/qa.md`, `tasks/prd-flashcards-decks-e-revisao/qa.md` (atualizados)
