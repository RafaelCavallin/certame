# Tarefa 3.0: Fundação transversal (paginação, auditoria, exceções e contrato)

## Visão geral

Três peças compartilhadas por todas as funcionalidades administrativas seguintes nascem aqui: o envelope de paginação `PaginaResposta<T>` da §6, o `RegistradorAuditoria` que grava a tabela `auditoria` exigida pelas restrições do PRD, e os três novos tipos de exceção com o mapeamento correspondente no `ManipuladorErrosGlobal`.

Esta tarefa também cumpre a regra 4 do `AGENTS.md`: as seis rotas que esta funcionalidade acrescenta ao contrato precisam ser registradas na §6 da `TECH-SPEC.md` na mesma entrega, e as colunas novas na §5.

<skills>
### Conformidade com skills

- `spring-boot-testing` — os componentes desta tarefa não ganham teste próprio; são exercitados pelas fatias das tarefas 4.0 a 8.0 (`RegistradorAuditoria` por TI-28, o mapeamento de erro por TI-12, TI-17, TI-22 e TI-25).
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regras relevantes: regra 4 (nenhum endpoint fora do contrato da §6 sem atualizar o documento na mesma alteração — é o núcleo desta tarefa) e regra 3 (mensagens de erro em português do Brasil; classes em português sem acento). Nenhum desvio.

**Dependência:** `ManipuladorErrosGlobal` e `ErroApi` são criados por `prd-acesso-e-conta`; esta tarefa os **modifica**, não os cria.
</rules>

<requirements>
- **`comum/PaginaResposta<T>`** — envelope `{content, page, size, totalElements}` da §6, reutilizável por todas as listagens.
- **`comum/auditoria/RegistradorAuditoria`** com `registrar(String acao, String entidade, UUID entidadeId, Object payload)`, lendo o ator do `SecurityContext`. Chamado explicitamente pelos serviços, **nunca por AOP** (decisão registrada na TechSpec).
- O `payload` guarda apenas o delta relevante — campos alterados, status anterior e novo — **nunca** o enunciado ou o comentário completos.
- **Novas exceções** em `comum/excecao/`: `RecursoNaoEncontradoException`, `ConflitoException` e `PublicacaoInvalidaException`.
- **`ManipuladorErrosGlobal` modificado** para mapear: `RecursoNaoEncontradoException` → 404 `RECURSO_NAO_ENCONTRADO`; `ConflitoException` → 409 com o código específico (`ASSUNTO_EM_USO`, `DISCIPLINA_EM_USO`, `QUESTAO_EM_USO`, `NOME_EM_USO`, `TRANSICAO_INVALIDA`); `PublicacaoInvalidaException` → **422 `PUBLICACAO_INVALIDA` com `fieldErrors` preenchido**.
- O envelope de erro continua sendo `application/problem+json` no formato `ErroApi` (`code`, `message`, `fieldErrors`).
- **Atualizar a `TECH-SPEC.md` §6** com as seis rotas acrescentadas ao contrato: `POST /admin/questoes/{id}/enviar-revisao`, `POST /admin/questoes/{id}/despublicar`, `DELETE /admin/questoes/{id}`, `POST /admin/assuntos/{id}/arquivar`, `GET /admin/bancas` e `GET /admin/orgaos`.
- **Atualizar a `TECH-SPEC.md` §5** com as colunas `orgao_id`, `publicado_em`, `excluido_em` e a coluna gerada `busca` em `questao`, e `arquivado_em` em `disciplina` e `assunto`.
</requirements>

## Subtarefas

- [x] 3.1 Criar `comum/PaginaResposta.java` conforme o envelope da §6.
- [x] 3.2 Criar `comum/excecao/RecursoNaoEncontradoException`, `ConflitoException` (com código de erro) e `PublicacaoInvalidaException` (com o mapa de pendências).
- [x] 3.3 Modificar `ManipuladorErrosGlobal` para tratar os três tipos, mapeando `PublicacaoInvalidaException` para 422 com `fieldErrors`.
- [x] 3.4 Criar `comum/auditoria/RegistradorAuditoria`, lendo o ator do `SecurityContext` e persistindo por `AuditoriaRepository`.
- [x] 3.5 Atualizar a §6 da `TECH-SPEC.md` com as seis rotas acrescentadas (regra 4).
- [x] 3.6 Atualizar a §5 da `TECH-SPEC.md` com as colunas novas de `questao`, `disciplina` e `assunto`.

## Detalhes de implementação

Ver TechSpec, seções *`ErroApi` — envelope de erro* (os exemplos de corpo de `PUBLICACAO_INVALIDA` e `ASSUNTO_EM_USO`), *`PaginaResposta<T>`*, *Visão dos componentes* (linhas de `comum/`), *Principais decisões* (auditoria explícita e não por AOP; recusa de publicação devolve todas as pendências de uma vez) e *Conformidade com o AGENTS.md* (regra 4).

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-06
- CA-11

## Testes da tarefa

Sem casos de teste próprios na TechSpec. Os componentes desta tarefa são verificados indiretamente: `PaginaResposta` por TI-02, `RegistradorAuditoria` por TI-14 e TI-28, e o mapeamento de erro por TI-12 (422), TI-17 (409), TI-22 (409) e TI-25 (409).

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/comum/PaginaResposta.java`
- `backend/src/main/java/br/com/certame/comum/excecao/RecursoNaoEncontradoException.java`
- `backend/src/main/java/br/com/certame/comum/excecao/ConflitoException.java`
- `backend/src/main/java/br/com/certame/comum/excecao/PublicacaoInvalidaException.java`
- `backend/src/main/java/br/com/certame/comum/ManipuladorErrosGlobal.java` (modificado)
- `backend/src/main/java/br/com/certame/comum/auditoria/RegistradorAuditoria.java`
- `TECH-SPEC.md` §5 e §6 (modificados)
