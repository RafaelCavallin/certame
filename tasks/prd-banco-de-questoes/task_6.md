# Tarefa 6.0: Ciclo de vida e publicação (RN-01)

## Visão geral

Fecha o portão entre o trabalho editorial e o acervo visível. As três transições de status vivem em **endpoints próprios**, não num `PUT` genérico, porque têm perfis e validações diferentes de uma edição comum: `enviar-revisao` é do professor, `publicar` e `despublicar` são do administrador.

Duas propriedades são o núcleo desta tarefa. Publicar aplica o `ValidadorPublicacao` e, na recusa, devolve **todas** as pendências de uma vez em `fieldErrors` (CA-06). Despublicar **apenas move o status**: nenhuma linha de `resposta`, `questao_estatistica` ou caderno é tocada, o que garante CA-08 por construção — e por isso TU-17 assere que não houve interação alguma com esses repositórios.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito puros no `PublicacaoService`, com `ValidadorPublicacao` real (é função pura) e `RegistradorAuditoria` mockado; `@SpringBootTest` com Testcontainers apenas nos fluxos completos de publicação e auditoria (TI-14, TI-16, TI-28); `Clock` injetado para `publicadoEm`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: regra 2 (`publicar` e `despublicar` exigem `ROLE_ADMIN`+ por `@PreAuthorize` no serviço — TI-13 e E2E-10 provam o 403 no servidor mesmo com o botão escondido; `enviar-revisao` exige `ROLE_PROFESSOR`+), regra 4 (`enviar-revisao` e `despublicar` são acréscimos ao contrato, já registrados na §6 pela tarefa 3.0) e regra 6 (`resposta` é imutável — despublicar e excluir nunca a tocam; TI-16 prova). Nenhum desvio.
</rules>

<requirements>
- **RN-01** — Somente questões com status `PUBLICADA` entram em sessões de estudo e simulados. A consulta de montagem de série filtra por `status = 'PUBLICADA'` e `excluido_em is null`.
- **`PublicacaoService.enviarParaRevisao`** — `RASCUNHO` → `EM_REVISAO`, perfil professor+. Questão já em revisão ou publicada → 409 `TRANSICAO_INVALIDA`.
- **`PublicacaoService.publicar`** — perfil admin+. Aplica o `ValidadorPublicacao`; com pendências, lança `PublicacaoInvalidaException` → **422 `PUBLICACAO_INVALIDA` com o mapa completo em `fieldErrors`**, e o status permanece inalterado no banco (CA-06). Com a questão válida: status `PUBLICADA`, `publicadoEm` preenchido e auditoria registrada. Questão já publicada → 409 `TRANSICAO_INVALIDA`. Perfil `PROFESSOR` → 403 `ACESSO_NEGADO`, mesmo com a questão completa.
- **`PublicacaoService.despublicar`** — perfil admin+. `PUBLICADA` → **`EM_REVISAO`**, não `RASCUNHO`: o caso de borda 2 descreve correção de questão publicada, e devolvê-la a rascunho perderia a informação de que ela já foi revisada. `publicadoEm` é **preservado** como registro histórico. Questão não publicada → 409 `TRANSICAO_INVALIDA`.
- **Nada é apagado ao despublicar** (CA-08): nenhuma linha de `resposta`, `questao_estatistica` ou caderno é tocada. Sessões já respondidas continuam íntegras; apenas a montagem de novas séries deixa de sortear a questão.
- Todas as três transições registram auditoria pelo `RegistradorAuditoria`, com ator, ação, entidade e o `payload` do delta (status anterior e novo).
- Métricas: `certame.questao.publicada` (contador, tags `perfil` e `disciplina`), `certame.questao.publicacao.recusada` (contador, tag `pendencia`), `certame.questao.despublicada` (contador) e `certame.acervo.publicadas` (*gauge*, tag `disciplina`).
- Log `INFO` para publicação e despublicação; `WARN` para publicação recusada por validação e transição inválida.
</requirements>

## Subtarefas

- [x] 6.1 Implementar `PublicacaoService.enviarParaRevisao` com a checagem de transição, com TU-19.
- [x] 6.2 Implementar `PublicacaoService.publicar`, aplicando o `ValidadorPublicacao` e preenchendo `publicadoEm` pelo `Clock` injetado, com TU-15 e TU-16.
- [x] 6.3 Implementar `PublicacaoService.despublicar`, movendo para `EM_REVISAO` e preservando `publicadoEm`, com TU-17 e TU-18.
- [x] 6.4 Anotar as três operações com `@PreAuthorize` (professor+ em `enviar-revisao`, admin+ em `publicar` e `despublicar`).
- [x] 6.5 Acrescentar as três rotas de transição ao `QuestaoAdminController`.
- [x] 6.6 Registrar auditoria nas três transições, com o `payload` de status anterior e novo.
- [x] 6.7 Implementar a consulta de montagem de série filtrando por `status = 'PUBLICADA'`, com TI-15.
- [x] 6.8 Registrar as quatro métricas e os eventos de log `INFO`/`WARN`.
- [ ] 6.9 Escrever os testes de integração TI-12 a TI-16 e TI-28.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`PublicacaoService`), *Endpoints da API* (`POST /admin/questoes/{id}/enviar-revisao`, `/publicar` e `/despublicar`, com o exemplo de publicação recusada), *Principais decisões* (transições em endpoints próprios, recusa devolve todas as pendências de uma vez, despublicar move para `EM_REVISAO`, auditoria explícita e não por AOP) e *Monitoramento e observabilidade*.

## Critérios de aceitação relacionados

- CA-06
- CA-07
- CA-08

## Testes da tarefa

### Testes de unidade

- [x] TU-15 — `PublicacaoService.publicar` com questão válida
- [x] TU-16 — `PublicacaoService.publicar` com pendências
- [x] TU-17 — `PublicacaoService.despublicar` de questão publicada
- [x] TU-18 — `PublicacaoService.despublicar` de rascunho
- [x] TU-19 — `PublicacaoService.enviarParaRevisao` a partir de publicada

### Testes de integração

- [ ] TI-12 — `POST /admin/questoes/{id}/publicar` sem comentário
- [ ] TI-13 — `POST /admin/questoes/{id}/publicar` como `PROFESSOR`
- [ ] TI-14 — `POST /admin/questoes/{id}/publicar` como `ADMIN`
- [x] TI-15 — Consulta de sorteio ignora rascunho e revisão
- [ ] TI-16 — Despublicar preserva respostas e estatísticas
- [ ] TI-28 — Auditoria das ações administrativas

> TI-15 e TI-16 dependem de `sessao_estudo` e `resposta`, que pertencem a `prd-sessao-de-estudo`. Até lá, os testes semeiam as duas tabelas pela migração de teste `db/migration-test/V990__resposta_minima.sql`, criada na tarefa 8.0 e restrita ao *classpath* de teste.

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/questao/PublicacaoService.java`
- `backend/src/main/java/br/com/certame/questao/QuestaoAdminController.java` (modificado)
- `backend/src/main/java/br/com/certame/questao/QuestaoRepository.java` (consulta de sorteio)
