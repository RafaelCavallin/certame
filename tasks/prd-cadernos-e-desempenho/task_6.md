# Tarefa 6.0: Desempenho — quatro serviços de leitura e `DesempenhoController` (RF-15, RF-16)

## Visão geral

A camada que transforma os agregados nos quatro painéis da tela: o resumo com os KPIs e o sinalizador `possuiHistorico`, a série de 8 semanas com lacunas, o desempenho por disciplina e a lista de pontos fracos. Nenhum destes serviços varre o histórico — todos leem os agregados da tarefa 5.0, e as regras difíceis já estão travadas nos objetos puros da tarefa 3.0. A média geral do usuário sai de `sum(acertos)/sum(volume)` sobre `desempenho_disciplina`, sem quarta tabela para manter em sincronia.

<skills>
### Conformidade com skills

- `spring-boot-testing` — serviços testados em unidade com *fakes* dos repositórios de agregado; `@WebMvcTest` com `MockMvcTester` para contrato e autorização; `@SpringBootTest` com Testcontainers apenas nos casos ponta a ponta (TI-19…TI-23, TI-26).
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` lidos na íntegra.

- **Regra 2 (inegociável):** as quatro rotas exigem autenticação e são escopadas ao usuário do token; nenhuma aceita `usuarioId`. TI-26 prova o isolamento entre usuários no servidor.
- **Regra 4:** `GET /desempenho/resumo` é acréscimo ao contrato; a §6 da `TECH-SPEC.md` é atualizada na mesma entrega, e a §8 passa a registrar agregados incrementais no lugar das *materialized views*.
- **Regra 6 / RN-04:** os painéis só enxergam sessões finalizadas, porque leem os agregados alimentados pela porta filtrada (CA-12).
- **`code-standards.md` §3:** controller só orquestra; DTOs são `record`; `null` significa ausência de dado e **nunca** é substituído por zero na serialização.
- **`code-standards.md` §2:** todo serviço recebe no máximo o `usuarioId` e suas dependências; sem literal mágico — o `20` de `minimoRespostas` vem do `SeletorPontosFracos`.
</rules>

<requirements>
- **RF-15** — `EvolucaoSemanal` com **sempre 8** posições do mais antigo ao mais recente, `pctMediaPeriodo` ponderado por volume **ignorando as lacunas**, `melhorSemana` entre as com atividade e `volumeTotal` somando as oito. Sem parâmetro de período na v1.
- **CA-07** — lacuna é `volume: 0` e `pctAcerto: null`; a API nunca omite a semana nem envia zero no percentual.
- **RF-16/CA-08** — disciplinas com volume e percentual, ordenadas por `volume desc`; disciplina com volume zero não aparece; lista vazia sem histórico.
- **RF-16/CA-09/CA-10** — envelope `PontosFracos` com `pctMediaUsuario`, `minimoRespostas: 20` e os itens ordenados por impacto. Lista vazia com `pctMediaUsuario` preenchido é estado legítimo, distinto do estado vazio da tela.
- **CA-11** — `ResumoDesempenho` com `possuiHistorico`, `pctAproveitamento: null` e contadores zerados quando não há histórico; é a **primeira** chamada da tela.
- Percentuais são inteiros de 0 a 100; datas e semanas em ISO-8601; `semana` é a segunda-feira no fuso `America/Sao_Paulo` e `rotulo` vem em formato brasileiro (`06/07`).
- Métrica `certame.desempenho.consulta` como *timer* com tag `painel`.
</requirements>

## Subtarefas

- [x] 6.1 DTOs `record` em `desempenho/dto/`: `ResumoDesempenho`, `EvolucaoSemanal`, `SemanaEvolucao`, `DesempenhoDisciplinaResumo`, `PontosFracos`, `PontoFraco`.
- [x] 6.2 `ResumoDesempenhoService`: KPIs somados de `desempenho_disciplina` e o sinalizador `possuiHistorico` (TU-36, TU-37).
- [x] 6.3 `EvolucaoSemanalService` sobre `CalendarioSemanas`: série, média ponderada sem lacunas, melhor semana e volume total (TU-06…TU-08).
- [x] 6.4 `DisciplinaDesempenhoService`: volume e percentual por disciplina, `ORDER BY volume desc`.
- [x] 6.5 `PontosFracosService`: aplica o `SeletorPontosFracos` sobre `desempenho_assunto` e resolve os nomes de assunto e disciplina.
- [x] 6.6 `DesempenhoController` com as quatro rotas de `/api/v1/desempenho`, todas escopadas ao token.
- [x] 6.7 Instrumentar o *timer* `certame.desempenho.consulta` por painel.
- [x] 6.8 Atualizar a §6 (rota `GET /desempenho/resumo`) e a §8 (agregados incrementais) da `TECH-SPEC.md`.
- [x] 6.9 Testes TU-06…TU-08, TU-36, TU-37; TI-19…TI-23 e TI-26 permanecem dependentes da entrega de F2, conforme bloqueio registrado em `tasks.md`.

## Detalhes de implementação

Ver `techspec.md`, §*Arquitetura do sistema* → *Backend* (linhas dos quatro serviços e do controller), §*Principais interfaces*, §*Modelos de dados* (`ResumoDesempenho`, `EvolucaoSemanal`, `SemanaEvolucao`, `DesempenhoDisciplinaResumo`, `PontosFracos`, `PontoFraco`, com os exemplos de payload e as notas sobre lacuna, amostra pequena e ordenação por impacto), §*Endpoints da API* (as quatro rotas de `/desempenho`, incluindo os exemplos sem histórico) e §*Principais decisões* (média do usuário sem tabela própria, `minimoRespostas` no envelope, `possuiHistorico` decidindo o estado vazio, sem parâmetro de período). Ordem de construção: passo 6.

Nota: TU-06, TU-07 e TU-08 aparecem aqui porque exercitam `EvolucaoSemanalService`; a regra pura que eles apoiam foi entregue na tarefa 3.0.

## Critérios de aceitação relacionados

- CA-06
- CA-07
- CA-08
- CA-09
- CA-10
- CA-11
- CA-12

## Testes da tarefa

### Testes de unidade

- [x] TU-06 — `EvolucaoSemanalService` calcula a média do período ignorando lacunas
- [x] TU-07 — `EvolucaoSemanalService` devolve melhor semana e volume total
- [x] TU-08 — `EvolucaoSemanalService` sem nenhuma atividade
- [x] TU-36 — `ResumoDesempenhoService` sem histórico
- [x] TU-37 — `ResumoDesempenhoService` deriva erros e aproveitamento dos agregados

### Testes de integração

- [ ] TI-19 — `GET /desempenho/evolucao` com atividade em várias semanas (bloqueado pela entrega de F2)
- [ ] TI-20 — `GET /desempenho/evolucao` com duas semanas sem atividade (bloqueado pela entrega de F2)
- [ ] TI-21 — `GET /desempenho/disciplinas` (bloqueado pela entrega de F2)
- [ ] TI-22 — `GET /desempenho/pontos-fracos` aplica os dois critérios (bloqueado pela entrega de F2)
- [ ] TI-23 — Respostas de Modo Prova abandonado não entram em métrica alguma (bloqueado pela entrega de F2)
- [ ] TI-26 — Desempenho de um usuário nunca inclui resposta de outro (bloqueado pela entrega de F2)

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/desempenho/EvolucaoSemanalService.java`, `DisciplinaDesempenhoService.java`, `PontosFracosService.java`, `ResumoDesempenhoService.java`, `DesempenhoController.java`
- `backend/src/main/java/br/com/certame/desempenho/dto/` — os seis DTOs
- `TECH-SPEC.md` §6 e §8 (modificados)
- `backend/src/test/java/br/com/certame/desempenho/` — `*Test` e `*IntegracaoTest`
