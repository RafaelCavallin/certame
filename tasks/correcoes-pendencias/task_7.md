# Tarefa 7.0 (P3-1): Prettier no frontend inteiro + gate no processo

## Visão geral

`npx prettier --check` falha em 191 arquivos, incluindo `e2e/`, `shared/ui/` e praticamente todas as
`features/` — **exceto** `features/study` e `features/dashboard`, que já foram formatados durante as
correções de `prd-sessao-de-estudo` e estão limpos. Isso confirma o diagnóstico já registrado no
`codereview.md` de flashcards: não é desvio de uma funcionalidade específica, é ausência de um portão
no processo.

Decisão tomada: **formatar de uma vez e adicionar `prettier --check` como gate** (não deixar como
dívida incremental, nem registrar como regra opcional).

<rules>
### Conformidade com o AGENTS.md e as rules

- `.agents/rules/code-standards.md` §"Formatação e testes" (Frontend) — Prettier é o formatador
  oficial do projeto; esta tarefa apenas faz o repositório cumprir uma regra que já existe.
</rules>

## Subtarefas

- [x] 7.1 Rodar `npx prettier --write .` no frontend inteiro, em commit isolado e exclusivamente
  mecânico — sem misturar com qualquer mudança de comportamento das tarefas 1.0-6.0. **Executar esta
  tarefa depois delas**, para os diffs de comportamento não ficarem poluídos por reformatação.
  Também foi criado `frontend/.prettierignore` (exclui `dist`, `coverage`, `.angular`,
  `test-results`, `playwright-report`, `node_modules`) para o gate não quebrar com artefatos
  gerados por builds/E2E; `test-results` e `playwright-report` foram adicionados ao `.gitignore`.
- [x] 7.2 Conferir que `features/study` e `features/dashboard` não aparecem no diff do `--write`
  (já estão limpos); se aparecerem, investigar por que a formatação atual diverge da esperada antes
  de prosseguir. Confirmado por hash SHA-256 dos 41 arquivos das duas pastas antes e depois do
  `--write`: nenhum arquivo mudou.
- [x] 7.3 Adicionar um script `format:check` (`prettier --check .`) no `package.json` do frontend.
- [x] 7.4 Ligar `format:check` como gate — no pipeline de CI se existir, e/ou como hook de
  pre-commit, conforme a infraestrutura de CI/CD atual do projeto permitir. O repositório ainda não
  é um repositório git (`git rev-parse` falha) e não havia nenhuma infra de CI/CD prévia, então um
  hook de pre-commit (husky) não é aplicável agora. Foi criado
  `.github/workflows/frontend-format.yml`, alinhado ao Build/CI (GitHub Actions) da TECH-SPEC, que
  roda `npm run format:check` em PRs e pushes a `main` que tocam `frontend/**` — ativa assim que o
  projeto for versionado e publicado no GitHub.
- [x] 7.5 Registrar na TechSpec (seção de padrões de frontend, se houver, ou `code-standards.md`) que
  `prettier --check` é obrigatório antes de qualquer merge, para o desvio não se acumular de novo.
  Adicionado em `.agents/rules/code-standards.md` §"Formatação e testes" e no checklist da §5.
- [x] 7.6 Rodar `npx prettier --check .` e confirmar 0 arquivos fora do padrão.

## Detalhes de implementação

Executar **antes** da tarefa 8.0 (limite de 100 linhas), porque ambas tocam os mesmos arquivos —
formatar depois de dividir os arquivos geraria retrabalho de reformatação.

## Critérios de aceitação relacionados

- Nenhum CA de produto — é padrão de processo.

## Testes da tarefa

- [x] `npx prettier --check .` — deve retornar 0 arquivos fora do padrão. Confirmado: "All matched
  files use Prettier code style!".
- [x] Suíte de testes de frontend (`npm run test:coverage`) — deve continuar em 0 falhas após a
  reformatação (mudança puramente sintática não deve alterar comportamento). Confirmado: 83 arquivos
  de teste, 473 testes, 0 falhas (Node 22.23.2, via nvm — o Node do PATH padrão, v18.19.1, é
  incompatível com o Angular CLI 22).

## Arquivos relevantes

- Os 191 arquivos apontados por `npx prettier --check` (praticamente todo `frontend/src` e
  `frontend/e2e`, exceto `features/study` e `features/dashboard`).
- `frontend/package.json` (novo script)
- Configuração de CI/pre-commit (novo gate)
