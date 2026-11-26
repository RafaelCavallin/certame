# Tarefa 2.0: Porta do fornecedor, registro e adaptador QAPI

## Visão geral

A primeira chamada HTTP de saída do projeto — não existe hoje `RestClient`, `RestTemplate`, `WebClient` nem `java.net.http` no código — e a decisão de dependência que sustenta a funcionalidade inteira: **o domínio nunca vê o fornecedor**.

A ordem interna importa. A porta `FornecedorQuestoes`, o `RegistroFornecedores` e a `FiltragemLocalCandidatas` são escritos **antes** do adaptador da QAPI, e provados com dois *fakes* (TU-39). Escrever o registro antes de existir uma implementação real é o que impede o desenho de encaixar na QAPI por acidente. Só depois disso o `ClienteQapi` passa a existir.

O adaptador traz junto a política de resiliência de ADR-5, escrita à mão e sem dependência nova: tempo limite por configuração, nova tentativa limitada com recuo progressivo **apenas** em falha transitória, e um limitador de chamadas por lote — que na QAPI é controle de **custo**, não de tempo, porque ela cobra por requisição em créditos pré-pagos e responde `402` quando o saldo acaba.

<skills>
### Conformidade com skills

- `spring-boot-testing` — o adaptador é testado como unidade pura com o transporte substituído; nada sobe a `@SpringBootTest` neste nível. `PropriedadesQapi` é `@ConfigurationProperties` + `@Validated`, no padrão de `PropriedadesSeguranca`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra. Regras que incidem aqui:

- **Stack fixa** e **nenhuma dependência Maven nova** — `RestClient` já vem em `spring-boot-starter-webmvc`; Resilience4j foi deliberadamente descartado em ADR-5.
- **Segredo nunca no código** (`code-standards.md` §2.8, RN-19) — a chave sai por variável de ambiente (`QAPI_KEY`), lida por `@ConfigurationProperties`, e não entra em commit, teste ou arquivo versionado.
- **RNF-08** — log estruturado em português, com o código do lote em toda linha, **sem** o header de autenticação e **sem** despejo do payload bruto do fornecedor.
- **Regra 3** — mensagens de falha em português do Brasil; os campos do envelope de erro seguem em inglês.
- **Fronteira por domínio** — os DTOs da QAPI ficam confinados em `importacao/qapi/dto/`, verificado por teste de arquitetura e não por disciplina.
- **Testes** — nenhum teste toca a rede; `FornecedorQuestoes` é a fronteira e é sempre substituída por *fake*.
</rules>

<requirements>
- **RF-38** — configuração por fornecedor: URL base, chave, tempo limite de conexão e de leitura, teto de chamadas e situação ativo/inativo. A chave **nunca** transita para o frontend, tela, log ou resposta de API.
- **RF-39** — filtro que o fornecedor não suporta é aplicado localmente, e o relatório registra em `filtragemLocal` que a filtragem foi local. A QAPI não filtra por dificuldade — esse é o caso concreto.
- **RF-40** — tradução obrigatória do payload para `QuestaoExterna`. **DTOs específicos do fornecedor não saem do pacote do adaptador.**
- **RF-41** — indisponibilidade classificada e nunca derrubando o lote inteiro: transitória (rede, tempo esgotado, `500`) repete com recuo e, persistindo, fecha o lote em `PARCIAL`; `401` e `403` fecham em `FALHOU` sem nova tentativa, porque repetir não conserta e gasta crédito; `402` fecha em `PARCIAL` com motivo de saldo; `400` fecha em `FALHOU` (defeito nosso na montagem do filtro); `404` de texto-base e corpo malformado são tratados por candidata, e o lote segue.
- **RNF-01** — 5 s de conexão e 15 s de leitura por padrão; nenhuma chamada fica pendurada indefinidamente.
- **RNF-02 (adaptada por ADR-5)** — o limitador é de **custo por lote** (`chamadas-maximas-por-lote`), não por minuto. Estourado o teto, o lote encerra como `PARCIAL` com a causa.
- **RN-19 / CA-26** — a chave não aparece em mensagem de erro, `toString`, log ou resposta.
</requirements>

## Subtarefas

- [ ] 2.1 Definir a porta `FornecedorQuestoes` (`fonte`, `ativo`, `filtrosSuportados`, `tamanhoPagina`, `buscar`, `buscarTexto`, `catalogo`) e os *records* neutros `FiltroFornecedor`, `PaginaExterna`, `TextoExterno`, `CampoFiltro`.
- [ ] 2.2 Implementar `RegistroFornecedores`, recebendo `List<FornecedorQuestoes>` do Spring e resolvendo por `fonte()` — sem `switch` e sem `if` por fornecedor em lugar nenhum.
- [ ] 2.3 Implementar `FiltragemLocalCandidatas` e `ResultadoFiltragem` (RF-39), genéricos sobre `QuestaoExterna` e sem conhecer fornecedor.
- [ ] 2.4 Escrever TU-39 e TU-41 com dois *fakes*, **antes** de existir o adaptador real.
- [ ] 2.5 Criar `qapi/PropriedadesQapi` (`certame.importacao.qapi.*`) e declarar as propriedades em `application.properties` e nos perfis `dev`, `staging` e `prod`, junto de `certame.importacao.habilitada` e `certame.importacao.simulacao-permitida`.
- [ ] 2.6 Criar os DTOs `qapi/dto/{QuestaoQapi,PaginaQuestoesQapi,TextoQapi,ErroQapi}`, confinados ao pacote.
- [ ] 2.7 Implementar `qapi/ClienteQapi` com `RestClient`, header `Q-Api-Key`, os dois tempos limite e `qapi/ClassificadorFalhaFornecedor` traduzindo os códigos HTTP em `FalhaFornecedorException` classificada.
- [ ] 2.8 Implementar `qapi/NovaTentativaComRecuo` (só falha transitória) e `qapi/LimitadorChamadasFornecedor` (teto por lote).
- [ ] 2.9 Implementar `qapi/TradutorQapi`: `opcaoA`–`opcaoE` → alternativas 1 a 5 pela posição, `texto` vazio tratado como ausência, `cargo` recebido e descartado, `dificuldade`, `tipo` e `urlExterna` nulos.
- [ ] 2.10 Implementar `qapi/FornecedorQapi`: paginação com `page` começando em **1** e `size` máximo **10**, `ceil(quantidade ÷ 10)` páginas, respeitando o teto de chamadas.
- [ ] 2.11 Implementar `importacao/CatalogoFornecedor`, com cache em memória e TTL configurável — cada leitura de catálogo custa crédito.
- [ ] 2.12 Criar `comum/excecao/FornecedorIndisponivelException`.
- [ ] 2.13 Escrever os testes TU-19, TU-20, TU-23, TU-24, TU-25 e TU-36.

## Detalhes de implementação

Ver TechSpec, seções *Design de implementação* → **Principais interfaces** (assinaturas da porta, do registro e da filtragem local), *Modelos de dados* → **Mapeamento QAPI → `QuestaoExterna`**, **Mapeamento QAPI `/api/textos/{codigo}` → `TextoBase`** e **Parâmetros fixos na origem**, *Pontos de integração* (configuração completa com padrões e variáveis de ambiente, e a tabela de tratamento de erro por código HTTP) e *Considerações técnicas* → **ADR-2**, **ADR-5**, **O custo real de trocar de fornecedor**, **RT-3**, **RT-4**, **RT-5** e **RT-8**.

> **Nota sobre a numeração dos casos de teste.** A tabela de testes de unidade da TechSpec é a fonte de verdade: **TU-20** é o teste de arquitetura que confina os DTOs ao adaptador e **TU-23** é o teto de chamadas. O texto corrido da seção *Visão dos componentes* cita "TU-23" para o confinamento — é lapso do texto; siga a tabela.

## Critérios de aceitação relacionados

- CA-09
- CA-18
- CA-26

## Testes da tarefa

### Testes de unidade

- [ ] TU-19 — `traduzPayloadDaQapiParaQuestaoExterna`
- [ ] TU-20 — `confinaDtosDoFornecedorAoPacoteDoAdaptador`
- [ ] TU-23 — `interrompeLoteAoAtingirTetoDeChamadas`
- [ ] TU-24 — `repeteApenasFalhaTransitoriaComRecuoProgressivo`
- [ ] TU-25 — `classificaSemCreditosComoLoteParcial`
- [ ] TU-36 — `omiteChaveDeAcessoDeTodaMensagemDeErro`
- [ ] TU-39 — `resolveFornecedorPelaFonteSemConhecerImplementacao`
- [ ] TU-41 — `aplicaLocalmenteFiltroQueOFornecedorNaoSuporta`

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/importacao/{FornecedorQuestoes,RegistroFornecedores,FiltragemLocalCandidatas,ResultadoFiltragem,FiltroFornecedor,PaginaExterna,TextoExterno,CampoFiltro,CatalogoFornecedor}.java`
- `backend/src/main/java/br/com/certame/importacao/qapi/{PropriedadesQapi,ClienteQapi,FornecedorQapi,TradutorQapi,LimitadorChamadasFornecedor,NovaTentativaComRecuo,ClassificadorFalhaFornecedor}.java`
- `backend/src/main/java/br/com/certame/importacao/qapi/dto/{QuestaoQapi,PaginaQuestoesQapi,TextoQapi,ErroQapi}.java`
- `backend/src/main/java/br/com/certame/comum/excecao/FornecedorIndisponivelException.java`
- `backend/src/main/resources/application.properties` · `application-{dev,staging,prod}.properties`
- `backend/src/test/java/br/com/certame/importacao/{RegistroFornecedoresTest,FiltragemLocalCandidatasTest}.java` · `importacao/qapi/*Test.java`
