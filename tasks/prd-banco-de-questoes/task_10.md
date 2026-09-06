# Tarefa 10.0: E2E e endurecimento

## Visão geral

Fecha a funcionalidade provando os fluxos completos ponta a ponta e os compromissos que nenhum teste anterior verifica sozinho: o orçamento de 800 ms (p95) de CA-01, a cobertura mínima de 70% em `service` e a acessibilidade das quatro telas.

A suíte roda com Playwright contra o SPA e a API reais, com PostgreSQL em contêiner e a semente de `dev`. Os seletores saem da marcação do protótipo traduzida 1:1 (`.table`, `.field`, `.input`, `.seg`, `.tag`, `.btn-primary`, `.dialog`) — por isso E2E-02, E2E-08, E2E-11, E2E-13 e E2E-16 dependem das lacunas e da tela `ADMIN: CATÁLOGO` fechadas na tarefa 1.0.

<skills>
### Conformidade com skills

- `angular-developer` — a suíte E2E exercita o comportamento zoneless e a sincronização de filtros por `queryParams` (E2E-04).
- `spring-boot-testing` — a verificação de cobertura de 70% incide sobre o pacote `service`, conforme o `AGENTS.md`.
- `impeccable` — **não aplicável**: E2E-15 verifica a acessibilidade e o contraste exigidos pelo PRD sobre o layout congelado, não propõe redesenho.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: regra 2 (E2E-10 prova que a ação some da interface **e** que a chamada direta à API devolve 403 — a ausência do botão nunca é a autorização), regra 3 (todos os rótulos e mensagens verificados em pt-BR) e "cobertura mínima 70% em `service`". Nenhum desvio.

**Bloqueio:** Playwright está ausente do `package.json`; sua introdução está prevista em `prd-acesso-e-conta`.
</rules>

<requirements>
- Suíte Playwright cobrindo E2E-01 a E2E-16 contra o SPA e a API reais, com PostgreSQL em contêiner e a semente de `dev`.
- **Cobertura mínima de 70% no pacote `service`**, verificada e reportada.
- **Medição do p95 de CA-01**: primeira página da listagem abaixo de 800 ms com 100 mil questões, confirmando o resultado de TI-06 no fluxo real.
- **Revisão de acessibilidade e contraste** (E2E-15) sobre as quatro telas: percurso completo por `Tab` e `Enter` com foco visível, e rótulos de status com texto e contraste ≥ 4,5:1.
- **E2E-16** cobre RF-37: cadastro de banca/órgão como administrador e recusa de exclusão quando há vínculo.
- **E2E-10** verifica as duas metades da regra 2: a ação de publicar não aparece para o professor **e** a chamada direta à API devolve 403, na mesma suíte.
- Conferir que as métricas de *Monitoramento e observabilidade* estão expostas: `certame.questao.publicada`, `certame.questao.publicacao.recusada`, `certame.questao.despublicada`, `certame.questao.busca`, `certame.taxonomia.exclusao.recusada` e `certame.acervo.publicadas`.
- Conferir que nenhum log registra o enunciado ou o comentário completos, em nenhum nível.
</requirements>

## Subtarefas

- [x] 10.1 Configurar a suíte Playwright contra o SPA e a API reais, com PostgreSQL em contêiner e a semente de `dev`.
- [x] 10.2 Escrever E2E-01 a E2E-04 (listagem, busca com filtros, busca por código com edição, retorno ao mesmo ponto da lista).
- [x] 10.3 Escrever E2E-05 a E2E-07 (múltipla escolha com cinco alternativas, troca para Certo/Errado, rascunho só com enunciado).
- [x] 10.4 Escrever E2E-08 a E2E-11 (publicação recusada com pendências inline, publicação como administrador, ausência do botão para o professor com 403 na API, despublicar com confirmação).
- [x] 10.5 Escrever E2E-12 a E2E-14 (árvore com contagens, exclusão recusada com oferta de arquivar, renomear assunto refletido na lista).
- [x] 10.6 Escrever E2E-15 (percurso por teclado e contraste dos status) sobre as quatro telas.
- [x] 10.7 Escrever E2E-16 (cadastrar banca/órgão como administrador e exclusão recusada quando em uso).
- [x] 10.8 Verificar e reportar a cobertura de 70% em `service`.
- [x] 10.9 Medir o p95 de CA-01 no fluxo real e registrar o resultado.
- [x] 10.10 Conferir as seis métricas expostas e a ausência de enunciado e comentário nos logs.

## Detalhes de implementação

Ver TechSpec, seções *Testes E2E* (a tabela completa e a nota sobre seletores e dependências do protótipo), *Abordagem de testes* (meta de cobertura e ferramentas), *Sequenciamento do desenvolvimento* (passo 10), *Monitoramento e observabilidade* (as seis métricas e a política de log) e *Riscos conhecidos* (o orçamento de 800 ms e os escapes já mapeados, caso o p95 estoure).

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-08
- CA-10
- CA-11
- CA-12
- CA-13
- CA-14

## Testes da tarefa

### Testes E2E

- [x] E2E-01 — Abrir a listagem como professor
- [x] E2E-02 — Buscar por texto e filtrar por disciplina, banca e status
- [x] E2E-03 — Buscar por código e abrir a questão para edição
- [x] E2E-04 — Voltar do formulário para a lista
- [x] E2E-05 — Cadastrar questão de múltipla escolha com cinco alternativas
- [x] E2E-06 — Trocar o tipo para Certo/Errado
- [x] E2E-07 — Salvar rascunho só com o enunciado
- [x] E2E-08 — Tentar publicar questão incompleta
- [x] E2E-09 — Publicar como administrador
- [x] E2E-10 — Botão de publicar ausente para o professor
- [x] E2E-11 — Despublicar com confirmação
- [x] E2E-12 — Abrir a árvore de matérias
- [x] E2E-13 — Excluir assunto com questões vinculadas
- [x] E2E-14 — Renomear assunto
- [x] E2E-15 — Percurso por teclado no formulário e contraste dos status
- [x] E2E-16 — Cadastrar banca e órgão como administrador e tentar excluir item vinculado

## Arquivos relevantes

- `frontend/e2e/` (suíte Playwright das quatro telas)
- `frontend/playwright.config.ts`
- `backend/pom.xml` (verificação de cobertura em `service`)

## Execução

### Ambiente E2E

- `docker-compose.e2e.yml` passou a usar `!override` em `ports`/`volumes`: sem isso o compose
  concatenava as listas e a pilha de teste também publicava 55432/55525/58025, disputando as portas
  do ambiente de desenvolvimento. Agora só 30532/30525/30580, com volume descartável.
- `e2e/global-setup.ts` removido. Os `webServer` do Playwright sobem **antes** do `globalSetup`, e o
  `docker compose down -v` de lá derrubava o PostgreSQL debaixo do processo Spring já iniciado —
  era a "falha de disponibilidade do processo Spring" que bloqueava a suíte. O banco é zerado agora
  no próprio comando do `webServer`.
- `spring-boot:run` roda com `-Dspring-boot.run.fork=false`. Com devtools no classpath o goal
  forkava uma JVM e devolvia `BUILD SUCCESS`; o Playwright dava o servidor por morto enquanto uma
  JVM órfã segurava a porta 3000.
- `workers: 1` — a suíte compartilha um único banco com a semente de `dev`.

### Suíte E2E

`npx playwright test`: **26 testes passaram** (10 de `auth.spec.ts` + 16 de `banco-questoes.spec.ts`).

A suíte anterior era decorativa: o helper `login()` não esperava a resposta da autenticação, então
todos os cenários rodavam na tela de login. E2E-15 passava sem nunca ter aberto uma tela
administrativa. Os cenários foram reescritos contra a interface — os que só chamavam a API
(E2E-08 a E2E-11, E2E-13, E2E-14, E2E-16) agora exercitam tela, diálogo e mensagem.

### Cobertura

- `./mvnw verify`: **113 testes, BUILD SUCCESS**. Cobertura por classe `*Service`, todas acima dos
  70% exigidos e também dos 80% de `.agents/rules/tests.md`:
  `QuestaoService` 100%, `TaxonomiaService` 100%, `CatalogoService` 100%, `BuscaQuestaoService` 100%,
  `EstatisticaQuestaoService` 100%, `RedefinicaoSenhaService` 100%, `PublicacaoService` 98,4%,
  `UsuarioService` 87,5%, `RefreshTokenService` 85,7%, `AutenticacaoService` 84,4%, `TokenService` 81,2%.
  (Antes: `CatalogoService` 57,9% e `TaxonomiaService` 51,4%.)
- `npm run test:coverage`: **64 testes**, statements 92,4% · branches 90,6% · functions 90,9% ·
  lines 98,3%. (Antes falhava com functions em 59,2%.)

### p95 de CA-01 (TI-06)

`TI-06 não existia` — a subtarefa 7.8 estava marcada como concluída sem o teste. Criado
`BuscaQuestaoDesempenhoIntegracaoTest` (`@Tag("lento")`), com 100 mil questões carregadas por
`insert ... select generate_series` uma vez por classe. Medição sobre 40 amostras após aquecimento:

| Cenário | p95 | Orçamento |
| --- | --- | --- |
| Primeira página sem filtro | **161 ms** | 800 ms |
| Primeira página com disciplina, banca e status | **14 ms** | 800 ms |
| Primeira página com busca textual | **230 ms** | 800 ms |

O `explain` da listagem filtrada confirma o uso de `questao_filtro_idx`.

### Métricas e logs

`ObservabilidadeIntegracaoTest` percorre publicar, recusar, despublicar, buscar e recusar exclusão
de assunto vinculado, e então verifica as seis métricas expostas (`certame.questao.publicada`,
`certame.questao.publicacao.recusada`, `certame.questao.despublicada`, `certame.questao.busca`,
`certame.taxonomia.exclusao.recusada`, `certame.acervo.publicadas`) e que nenhum log da aplicação,
capturado em `TRACE` sobre `br.com.certame`, contém o enunciado ou o comentário completos.

### Defeitos encontrados e corrigidos

| # | Defeito | Correção |
| --- | --- | --- |
| 1 | `contarArvore` usava `grouping()` dentro de `filter`, recusado pelo PostgreSQL: `GET /admin/disciplinas` respondia 500 e a tela de Matérias e o combo de disciplinas nunca funcionaram. | Consulta reescrita com `union all`. Coberto por `TaxonomiaArvorePersistenciaTest` (TI-20…22) — os testes de unidade mockavam o repositório e não viam a SQL. |
| 2 | O default de `questao.codigo` fazia `lpad(nextval(...)::text, 5, '0')`, que trunca a partir de 100000 e repete `Q-10000`. Da 90.001ª questão em diante todo insert violava a unicidade — o acervo de 100 mil de CA-01 era impossível. | Migração `V5__codigo_questao_acima_de_99999.sql` com `certame_codigo_questao()`. Regressão em `BancoQuestoesPersistenciaTest`. |
| 3 | `Questao.codigo` era `insertable = false`: o gerador consumia um `nextval` e o default do banco consumia outro, então a API devolvia um código diferente do gravado. | Coluna passou a ser gravável no insert. Regressão em `BancoQuestoesPersistenciaTest`. |
| 4 | O formulário sempre enviava as duas alternativas vazias iniciais, e o `@Size(min = 1)` do `AlternativaDto` recusava — CA-05 (rascunho só com enunciado) não funcionava. | `QuestaoFormStore.dados()` descarta alternativas em branco e renumera a ordem. Dois testes novos. |
| 5 | O formulário não tinha ação de publicar: `store.publicar()` e o bloco de pendências inline eram inalcançáveis, e CA-06 não tinha caminho na interface. | Botão **Publicar** no formulário, visível só para `ADMIN`/`SUPER_ADMIN` (a autorização continua no `@PreAuthorize`). |
| 6 | Na recusa de exclusão de assunto, a tela abria o diálogo de renomear; `arquivar()` era código morto e `varianteArquivar` nunca era usado — CA-11 não oferecia arquivar. | Diálogo de recusa com a mensagem do servidor e o botão **Arquivar**. |
| 7 | `Novo item` do catálogo nunca abria o diálogo: a condição `@if (editando() !== null || nome())` é falsa para um item novo. | Sinal `formAberto`. |
| 8 | `.seg-opt` não neutralizava o estilo nativo do `<button>`: no catálogo a aba herdava o fundo `#efefef` do navegador e o accent ficava em **2,8:1**, abaixo dos 4,5:1 de E2E-15. | `background: transparent; border: 0; color: inherit` em `.seg-opt` (`styles.css`), restaurando o visual do protótipo. Passa a ~4,7:1. |

### Rotas iniciais de ADMIN e SUPER_ADMIN (corrigido a pedido)

Arquivos de `prd-acesso-e-conta`, corrigidos aqui porque o defeito impedia o administrador de usar
as telas desta funcionalidade.

Eram dois problemas somados. `rotaInicialPorPerfil` mandava `ADMIN` para `/admin/simulados` e
`SUPER_ADMIN` para `/super/usuarios`, rotas inexistentes; e o `**` fazia `redirectTo: 'login'`, onde
o `visitanteGuard` devolvia o autenticado para a mesma rota inexistente — ciclo fechado, e o
administrador terminava na tela de login logo depois de entrar com sucesso. O segundo defeito era
independente do primeiro: **qualquer URL errada expulsava um usuário logado para o login**.

- `rota-inicial.ts` passou a declarar uma cadeia de preferências por perfil e a escolher a primeira
  que a configuração do roteador realmente resolve. `ADMIN` fica em `/admin/simulados` como
  intenção do produto e cai em `/admin/questoes` enquanto aquela tela não existe — quando
  `prd-simulados` entregar, o destino passa a valer sozinho.
- `app.routes.ts`: `**` agora redireciona para `''`. Quem está logado vai para o próprio início;
  quem não está vai para o login com `retorno`, pelo `authGuard`.
- TU-22 era um teste decorativo: afirmava o mapeamento quebrado e passava verde. Reescrito sobre
  configurações sintéticas (TU-22…22e), cobrindo a preferência, a entrega futura da tela, o
  último recurso e o curinga.
- E2E novos em `auth.spec.ts`: cada um dos quatro perfis entra e chega numa tela real (E2E-11);
  URL inexistente não expulsa quem está logado (E2E-12) e leva o visitante ao login (E2E-13).

`npx playwright test`: **32 testes passaram**. `npm run test:coverage`: **68 testes**,
statements 92,6% · branches 91,3% · functions 91,1% · lines 98,3%.

### Pendências para decisão

- **"Voltar para o banco de questões" não preserva termo, filtros e página** (`routerLink` sem
  `queryParams`). E2E-04 verifica a preservação pelo voltar do navegador, que é o gesto que de fato
  devolve a lista no mesmo ponto.
- **E2E-14 na TechSpec espera o novo nome do assunto "na lista de questões"**, mas a tabela de
  questões tem coluna de disciplina, não de assunto. O teste verifica o reflexo na árvore de
  matérias.
- **Divergência de limite de cobertura**: `task_10.md` e o `pom.xml` falam em 70%; `AGENTS.md` e
  `.agents/rules/tests.md` (que declara prevalecer) falam em 80%. Deixei o `pom.xml` em 0.70 como a
  tarefa especifica — na prática todas as classes `*Service` já passam de 81%, então subir o limite
  para 0.80 é seguro se essa for a decisão.
