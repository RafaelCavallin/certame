# Tarefa 7.0: Controllers, contrato da API, segurança, observabilidade e auditoria

## Visão geral

A superfície HTTP da funcionalidade: doze rotas novas sob `/api/v1/admin`, todas restritas a `ROLE_ADMIN` e `ROLE_SUPER_ADMIN` e validadas **no servidor**, mesmo quando a interface já esconde a ação. Nenhuma rota nova fora dessas doze; três contratos existentes ganham campos sem mudar de rota.

Duas provas desta tarefa são compromissos que o PRD trata como inegociáveis, e por isso são testes e não promessas: **TI-18** roda sessão de estudo, caderno, simulado e desempenho com um `FornecedorQuestoes` que **falha se invocado** — se algum fluxo do estudante encostar no fornecedor, o teste quebra; e **TI-21** captura o log com um *appender* e varre por `qb_`, o prefixo da chave da QAPI.

A regra 4 do `AGENTS.md` é parte da entrega, não um passo posterior: as rotas entram na `TECH-SPEC.md` §6 e o modelo de dados na §5 **na mesma alteração** que as implementa.

<skills>
### Conformidade com skills

- `spring-boot-testing` — `@WebMvcTest` (slice web) para autorização por perfil e formato do JSON; `@SpringBootTest @AutoConfigureMockMvc @Testcontainers` só para o que o slice não prova: fluxo ponta a ponta, ausência de chamada externa no estudo e varredura de log.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras que incidem aqui:

- **Regra 2 / RNF-10 / CA-25** — `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` em **todo** endpoint desta especificação.
- **Regra 4 / RNF-11 / R-5** — nenhum endpoint fora do contrato: `TECH-SPEC.md` §6 e §5 atualizadas na mesma alteração.
- **Camadas** — controller só orquestra e mapeia DTO: sem regra, sem `@Transactional`, sem `try/catch`. Erros por `ErroNegocioException` e o `@RestControllerAdvice` único que já existe — **nenhum *advice* novo**.
- **DTOs** — `record` imutável em `importacao/dto/`, `Requisicao*` para entrada e `*Resumo` para projeção; entidade JPA nunca cruza a fronteira do controller.
- **Regra 3** — mensagens ao usuário em português do Brasil, frase completa e com ponto final; `code`, `message` e `fieldErrors` seguem em inglês, como já é.
- **RN-19 / RNF-08** — a chave nunca é devolvida por endpoint nem escrita em log; a falha do fornecedor é traduzida antes de sair e o corpo da resposta externa não é repassado.
</rules>

<requirements>
- **RF-54** — procedência na ficha administrativa da questão, com link para a origem quando disponível.
- **RF-68** — histórico de importações consultável, com filtro por período, fornecedor, autor e situação, e o detalhamento por candidata de cada lote.
- **RF-69** — auditoria de início de importação, criação de questão importada, criação e correção de mapeamento, aplicação e descarte de divergência. O payload guarda identificador, hashes e decisões — **não** cópias do conteúdo bruto.
- **RF-70 a RF-73** — os contratos que sustentam a tela: fornecedores e seu vocabulário, lotes, relatório, itens, fila de pendências de mapeamento e divergências.
- **RF-72** — filtros de `fonte` e `pendencia` em `GET /admin/questoes`.
- Códigos de erro novos: `IMPORTACAO_DESABILITADA` (409), `IMPORTACAO_EM_ANDAMENTO` (409), `SIMULACAO_NAO_PERMITIDA` (409), `FORNECEDOR_INDISPONIVEL` (502), `MAPEAMENTO_EM_USO` (409), `TRANSICAO_INVALIDA` (409), além de `DADOS_INVALIDOS` (422) e `RECURSO_NAO_ENCONTRADO` (404).
- **Degradação do catálogo:** fornecedor fora do ar **com cache preenchido** devolve 200 e `doCache: true` — não faz sentido bloquear a curadoria de mapeamento por indisponibilidade momentânea, já que ela não grava nada no fornecedor. Cache vazio devolve 502.
- **RNF-07** — as oito métricas Micrometer por fornecedor, no padrão de `MetricasPublicacao` e `EstudoObservabilidade`.
- **RN-18 / O-5 / CA-24** — nenhum fluxo de estudo, caderno, simulado, desempenho ou flashcard consulta API externa, em nenhum momento.
</requirements>

## Subtarefas

- [ ] 7.1 Implementar `lote/ImportacaoController`: `POST /admin/importacoes` (202), `GET /admin/importacoes`, `GET /admin/importacoes/{id}`, `GET /admin/importacoes/{id}/itens` (com filtro por resultado e `size` máximo 100), `GET /admin/importacoes/fornecedores` e `GET /admin/importacoes/fornecedores/{fonte}/catalogo`.
- [ ] 7.2 Implementar `taxonomia/MapeamentoController`: `GET`, `POST`, `PUT`, `DELETE` de `/admin/mapeamentos` e `GET /admin/mapeamentos/pendencias`.
- [ ] 7.3 Implementar `divergencia/DivergenciaController`: `GET /admin/divergencias` filtrável por situação e `POST /admin/divergencias/{id}/{aplicar,ignorar}`.
- [ ] 7.4 Acrescentar os códigos de erro novos ao `ManipuladorErrosGlobal` existente, sem criar *advice* novo.
- [ ] 7.5 Acrescentar `procedencia` e `textoBase` a `GET /admin/questoes/{id}`, e os filtros `fonte` e `pendencia` a `GET /admin/questoes` (`FiltroQuestao` e `QuestaoBuscaRepositoryImpl`), incluindo o sinalizador "sem dificuldade" de RT-1.
- [ ] 7.6 Implementar `importacao/MetricasImportacao` com as oito métricas da seção de observabilidade.
- [ ] 7.7 Padronizar o log estruturado em português, com o código do lote em toda linha e nos níveis previstos, sem chave e sem payload bruto.
- [ ] 7.8 Atualizar `TECH-SPEC.md` §6 (contrato da API) e §5 (modelo de dados) **na mesma alteração**.
- [ ] 7.9 Escrever os testes TI-04, TI-05, TI-06, TI-13, TI-18, TI-19, TI-20, TI-21, TI-24, TI-25 e TI-26.

## Detalhes de implementação

Ver TechSpec, seção *Endpoints da API* na íntegra — a **Visão geral** com as doze rotas e a lista dos contratos existentes que mudam de corpo sem mudar de rota, mais o detalhamento de `POST /admin/importacoes`, `GET /admin/importacoes/{id}`, `GET /admin/importacoes/{id}/itens`, `GET .../fornecedores/{fonte}/catalogo`, `GET /admin/mapeamentos/pendencias` e `POST /admin/mapeamentos` —, *Modelos de dados* → **`ErroApi`** (tabela de códigos novos e a nota "Nunca vaza credencial") e **`ProcedenciaQuestao`**, e *Monitoramento e observabilidade* na íntegra (as oito métricas com suas tags, a tabela de eventos de log por nível e as seis ações de auditoria).

## Critérios de aceitação relacionados

- CA-06
- CA-07
- CA-09
- CA-11
- CA-12
- CA-24
- CA-25
- CA-26
- CA-27
- CA-28

## Testes da tarefa

### Testes de integração

- [ ] TI-04 — `aceitaPedidoDeImportacaoERespondeDuzentosEDois`
- [ ] TI-05 — `recusaTodoEndpointDeImportacaoParaEstudanteEProfessor`
- [ ] TI-06 — `nuncaDevolveAChaveDoFornecedorEmNenhumEndpoint`
- [ ] TI-13 — `recusaPublicacaoDeImportadaSemComentarioComOValidadorAtual`
- [ ] TI-18 — `naoChamaFornecedorEmNenhumFluxoDeEstudo`
- [ ] TI-19 — `entregaQuestaoImportadaPublicadaComoQualquerOutraNaSerie`
- [ ] TI-20 — `exibeProcedenciaNaFichaAdministrativaDaQuestaoImportada`
- [ ] TI-21 — `naoEscreveAChaveDoFornecedorEmNenhumaLinhaDeLog`
- [ ] TI-24 — `listaValoresExternosNaoMapeadosComContagemNoRelatorio`
- [ ] TI-25 — `recusaImportacaoQuandoDesabilitadaNoAmbiente`
- [ ] TI-26 — `recusaSegundoLoteConcorrenteParaAMesmaFonte`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/importacao/lote/ImportacaoController.java`
- `backend/src/main/java/br/com/certame/importacao/taxonomia/MapeamentoController.java`
- `backend/src/main/java/br/com/certame/importacao/divergencia/DivergenciaController.java`
- `backend/src/main/java/br/com/certame/importacao/MetricasImportacao.java`
- `backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java` (modificado)
- `backend/src/main/java/br/com/certame/questao/dto/{QuestaoDetalhe,FiltroQuestao}.java` · `questao/QuestaoBuscaRepositoryImpl.java` (modificados)
- `TECH-SPEC.md` §5 e §6 (modificados)
- `backend/src/test/java/br/com/certame/importacao/*ControllerTest.java` · `importacao/*IntegracaoTest.java`
