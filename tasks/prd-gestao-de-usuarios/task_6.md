# Tarefa 6.0: Frontend (UsuariosPage, UsuarioFormulario e rotas)

## Visão geral

Área `features/super/` em Angular: `UsuariosPage` (tabela paginada/filtrada com ações e confirmações) e `UsuarioFormulario` (criar/editar, como rota própria) guardadas por `perfilGuard([PerfilUsuario.SUPER_ADMIN])`, mais o cliente HTTP tipado `core/api/usuario-admin-api`. Tradução 1:1 do protótipo já revisado na tarefa 1.0.

<skills>
### Conformidade com skills

- `angular-developer` — convenções de rotas, signals, formulários reativos e chamadas HTTP tipadas.
- `impeccable` — conferência de acessibilidade (navegação por teclado, foco visível, contraste 4,5:1) e de rótulos textuais para perfil/status.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lido `AGENTS.md` e as rules em `.agents/rules/` (`code-standards.md`, `tests.md`, `folder-structure.md`).

- **Regra 1 (AGENTS.md)** — tradução 1:1 do protótipo revisado na tarefa 1.0; mesma marcação, mesmos tokens Nocturne (`var(--*)`); nenhuma biblioteca de UI de terceiros.
- **Regra 2 (AGENTS.md)** — o `perfilGuard` e o menu apenas escondem a opção; a autorização real é do backend (tarefas 3.0/4.0) — não implementar nenhuma validação de perfil que substitua o servidor.
- **Regra 3 (AGENTS.md)** — interface e mensagens em português do Brasil.
- Estrutura alvo `frontend/src/app/features/super/usuarios/` e `features/super/usuario-formulario/` (`.agents/rules/folder-structure.md` §3.1/§3.2); rota `/super/usuarios` restrita a super-admin.
- Estado com signals; sem `zone.js` (Angular 22 zoneless).
</rules>

<requirements>
- RF-23 — tabela com nome, e-mail, perfil, status, data de criação, ordenação por criação e filtros por perfil/status.
- RF-24 — formulário de criação com nome, e-mail, perfil, status e envio opcional de convite; usuário aparece na lista sem recarregar a página.
- RF-25 — edição de perfil/status, com mensagens de bloqueio de autoexclusão e último super-admin vindas do backend (409).
- RF-26 — confirmação explícita antes de excluir, com o nome do usuário na mensagem.
- RF-27 — notificação na tela para "Redefinir senha" e "Reenviar convite", sem exibir nenhuma senha.
</requirements>

## Subtarefas

- [x] 6.1 Criar `core/api/usuario-admin-api.ts`, cliente HTTP tipado para os seis endpoints de `/api/v1/super/usuarios`, com os tipos de `core/models/`.
- [x] 6.2 Criar `UsuariosPage`: tabela paginada/filtrada (perfil, status, ordenação por `criadoEm`), ações de linha (editar, redefinir senha, reenviar convite, excluir) e mapeamento de erros 4xx para mensagens em pt-BR.
- [x] 6.3 Criar `UsuarioFormulario` como rota própria de criação/edição, com validação client-side espelhando `RequisicaoCriarUsuario`/`RequisicaoEditarUsuario` e mapeamento de erros 422 por campo.
- [x] 6.4 Adicionar as rotas `/super/usuarios` e `/super/usuarios/novo` (ou equivalente de edição) em `app.routes.ts`, guardadas por `perfilGuard([PerfilUsuario.SUPER_ADMIN])`.
- [x] 6.5 Adicionar o item de menu "Usuários" no shell (`layout/shell/`) visível apenas para super-admin, e remover o fallback de `rota-inicial` para SUPER_ADMIN.
- [x] 6.6 Ligar o diálogo de confirmação de exclusão (`shared/ui/confirmacao-dialog/`) e os toasts de confirmação de convite/redefinição de senha.

## Detalhes de implementação

Ver `techspec.md`, seções "Visão dos componentes", "Endpoints da API" (contratos consumidos) e "Sequenciamento do desenvolvimento" (passo 6). Ver `prd.md`, seção "Experiência do usuário" para os quatro fluxos principais.

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01
- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-07
- CA-08
- CA-10
- CA-12

## Testes da tarefa

A TechSpec não enumera casos de teste unitário específicos para os componentes de frontend desta funcionalidade (apenas a estratégia geral "Vitest com `HttpTestingController`" na seção "Abordagem de testes"). Cobrir `usuario-admin-api`, `UsuariosPage` e `UsuarioFormulario` com Vitest, mantendo a cobertura mínima de 80% e os princípios FIRST/AAA de `.agents/rules/tests.md`; a validação dos critérios de aceitação de UI é feita no fluxo E2E da tarefa 7.0 e em QA.

## Arquivos relevantes

- `frontend/src/app/core/api/usuario-admin-api.ts`
- `frontend/src/app/core/models/modelos-usuario.ts` (tipos de usuário/perfil/status)
- `frontend/src/app/features/super/usuarios-lista-store.ts` (estado e ações da listagem)
- `frontend/src/app/features/super/usuario-formulario-store.ts` (estado e ações do formulário)
- `frontend/src/app/features/super/usuarios/`
- `frontend/src/app/features/super/usuario-formulario/`
- `frontend/src/app/core/auth/auth-guard.ts` (`perfilGuard`, referência)
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/layout/shell/` (menu e gate `podeSuper()`)
- `frontend/src/app/core/auth/rota-inicial.ts` (fallback do SUPER_ADMIN removido)
- `frontend/src/app/shared/ui/confirmacao-dialog/` (referência)
