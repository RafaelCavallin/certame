# Tarefa 7.0: Testes E2E

## Visão geral

Fluxo crítico de tela com Playwright: super-admin cria um usuário, vê o novo registro aparecer na lista sem recarregar a página, edita seu perfil, aciona a exclusão com confirmação nomeada e vê o usuário removido. Cobre apenas o percurso crítico, de acordo com a pirâmide de testes (~10% de E2E); o restante já foi validado nas camadas de unidade e integração (tarefas 3.0 e 5.0).

<skills>
### Conformidade com skills

Nenhuma skill de projeto específica para Playwright/E2E; seguir a estratégia de testes descrita em `techspec.md` e os princípios de `.agents/rules/tests.md`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lido `AGENTS.md` e as rules em `.agents/rules/` (`code-standards.md`, `tests.md`, `folder-structure.md`).

- E2E limitado ao fluxo crítico de tela, conforme a pirâmide de testes (`.agents/rules/tests.md`); não duplicar cobertura já provada em TU-01…TU-12 e TI-01…TI-07.
- Interface e mensagens de confirmação/toast validadas em português do Brasil.
- Depende das tarefas 1.0 (protótipo revisado), 4.0/5.0 (backend completo) e 6.0 (frontend) já implementadas.
</rules>

<requirements>
- CA-01 — a tabela de usuários exibe os dados esperados.
- CA-03 — o usuário criado aparece na lista imediatamente, sem recarregar a página.
- CA-10 — a exclusão exige confirmação explícita antes de qualquer efeito.
</requirements>

## Subtarefas

- [x] 7.1 Escrever o cenário Playwright: login como super-admin → "Novo usuário" → preencher dados e perfil → criar → usuário aparece na lista sem reload.
- [x] 7.2 Estender o cenário: editar o usuário criado, alterar o perfil → salvar → nova capacidade refletida na lista.
- [x] 7.3 Estender o cenário: acionar "Excluir" → confirmar no modal nomeado com o usuário afetado → validar remoção da lista.

## Detalhes de implementação

Ver `techspec.md`, seção "Testes E2E" (E2E-01) e "Sequenciamento do desenvolvimento" (passo 7, último da ordem de construção).

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01
- CA-03
- CA-10

## Testes da tarefa

### Testes E2E

- [x] E2E-01 — `E2E-01_criaEditaERemoveUsuarioComConfirmacao`

## Arquivos relevantes

- Suíte Playwright do projeto (local e convenção de nome conforme já estabelecido em outras funcionalidades com E2E, ex.: `prd-acesso-e-conta`)
- `frontend/e2e/gestao-usuarios.spec.ts` (novo)

## Observação

A `usuarios-lista-store.excluir()` não publica toast de sucesso (apenas `redefinirSenha`/`reenviarConvite` publicam, conforme RF-27 e os testes unitários já aceitos na tarefa 6.0); CA-10 exige apenas confirmação explícita antes do efeito, sem mencionar toast. O cenário E2E valida o fechamento do modal e a remoção da linha da lista (persistente após reload), sem asserir um toast inexistente na implementação.
