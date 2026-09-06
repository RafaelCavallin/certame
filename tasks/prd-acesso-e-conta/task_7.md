# Tarefa 7.0: Núcleo do frontend (AuthStore, interceptor e guardas)

## Visão geral

Entrega a camada de acesso do SPA sem as telas: o `AuthStore` por signals com o access token apenas em memória, o cliente HTTP das seis rotas, o interceptor que renova uma única vez em 401, as guardas de rota, o mapeamento de perfil para rota de Início e a restauração de sessão no bootstrap pelo cookie de refresh. Já é testável contra a API real.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone, detecção zoneless, estado por signals em serviços `providedIn: 'root'`, interceptor funcional, guardas funcionais, `provideAppInitializer` e Reactive Forms; convenções de nomenclatura do Angular.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/`. Regras relevantes: stack Angular 22 standalone/zoneless/signals, regra 1 (nenhuma biblioteca de UI de terceiros — esta tarefa não adiciona nenhuma), regra 2 (a interface esconde ações por perfil, mas a autorização real é a do servidor) e regra 3 (interface em pt-BR; `LOCALE_ID` e `lang="pt-BR"`). **Desvio registrado na TechSpec:** Reactive Forms em vez de Signal Forms, por estes ainda serem experimentais na v22 — revisar na F3.
</rules>

<requirements>
- `AuthStore` com signals `usuario`, `accessToken` (só memória, nunca `localStorage`), `carregando` e `erro`, mais os `computed` `autenticado` e `perfil`; métodos `entrar`, `cadastrar`, `restaurarSessao`, `renovar` e `sair`.
- `AuthApi` cobrindo as seis rotas com `withCredentials: true`.
- `authInterceptor` que anexa `Authorization`, renova uma única vez em 401, enfileira requisições concorrentes e redireciona ao login na falha — distinguindo o 401 de `/auth/refresh` (leva ao login sem nova tentativa) do 401 das demais rotas.
- `authGuard`, `visitanteGuard` e `perfilGuard(perfis)` — este último já preparado para as fases seguintes.
- `rotaInicialPorPerfil` mapeando cada um dos quatro perfis à sua rota de Início (RF-01, CA-03).
- `erro-api.ts` tipando o envelope `problem+json` e mapeando `fieldErrors` (422) para `FormControl`.
- Rotas públicas `/login`, `/cadastro`, `/esqueci-senha` e `/definir-senha/:token`; `/login` e `/cadastro` renderizam o mesmo `AcessoPage` com a aba pré-selecionada.
- `provideAppInitializer` chamando `POST /auth/refresh` uma vez no bootstrap para restaurar sessão e perfil; `LOCALE_ID` pt-BR e `lang="pt-BR"` no `index.html`.
</requirements>

## Subtarefas

- [x] 7.1 Criar `core/auth/modelos.ts` com os tipos `UsuarioResumo`, `RespostaAutenticacao` e os enums de perfil.
- [x] 7.2 Criar `core/api/erro-api.ts` com a tipagem do `problem+json` e o mapeamento `fieldErrors` → `FormControl`, com TU-21.
- [x] 7.3 Criar `core/auth/auth-api.ts` com as seis chamadas e `withCredentials: true`.
- [x] 7.4 Criar `core/auth/auth-store.ts` com os signals e os cinco métodos, com TU-18 e TU-19.
- [x] 7.5 Criar `core/auth/auth-interceptor.ts` com a renovação única e o enfileiramento de concorrentes, com TU-20.
- [x] 7.6 Criar `core/auth/auth-guard.ts` (`authGuard`, `visitanteGuard`, `perfilGuard`).
- [x] 7.7 Criar `core/auth/rota-inicial.ts`, com TU-22.
- [x] 7.8 Declarar as rotas públicas em `app.routes.ts` (placeholders de componente até a tarefa 9.0).
- [x] 7.9 Configurar `app.config.ts` com `provideHttpClient(withInterceptors([...]))`, `provideAppInitializer` da restauração de sessão e `LOCALE_ID` pt-BR; ajustar `index.html` para `lang="pt-BR"`.

## Detalhes de implementação

Ver TechSpec, seções *Visão dos componentes → Frontend* (tabela de componentes `core/*`), *Principais interfaces* (`AuthStore`), *Fluxo de dados* (restauração no bootstrap e rotação do refresh) e *Principais decisões* (access token em memória, Reactive Forms, uma página e duas rotas).

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-05
- CA-07
- CA-12

## Testes da tarefa

### Testes de unidade

- [x] TU-18 — `AuthStore.entrar` em sucesso (Vitest)
- [x] TU-19 — `AuthStore.entrar` em 401 (Vitest)
- [x] TU-20 — `authInterceptor` renova uma única vez em 401 (Vitest)
- [x] TU-21 — Mapeamento de `fieldErrors` 422 para `FormControl` (Vitest)
- [x] TU-22 — `rotaInicialPorPerfil` para os quatro perfis

## Arquivos relevantes

- `frontend/src/app/core/auth/auth-store.ts`
- `frontend/src/app/core/auth/auth-api.ts`
- `frontend/src/app/core/auth/auth-interceptor.ts`
- `frontend/src/app/core/auth/auth-guard.ts`
- `frontend/src/app/core/auth/rota-inicial.ts`
- `frontend/src/app/core/auth/modelos.ts`
- `frontend/src/app/core/api/erro-api.ts`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/app.config.ts`
- `frontend/src/index.html`
