# Revisão prd-simulados — andamento e próximos passos

Data: 2026-09-03 (continuar em 2026-09-04)
Skill: `executar-review` (`--prd prd-simulados`), fluxo 1–8.

## O que já foi feito

### 1. Análise (fluxo 1–4) — concluída
- Lidos: `AGENTS.md`, `.agents/rules/` (code-standards, tests, folder-structure),
  `tasks/prd-simulados/{techspec, tasks, task_1..8, prd}.md` e `TECH-SPEC.md`.
- Mapeado: backend `br.com.certame.simulado` (40 arquivos), migração `V9__simulado.sql`,
  frontend `features/simulados` + `features/admin/simulados`, e2e `simulados.spec.ts` e
  `admin-simulados.spec.ts`.
- `tasks.md` marca 1.0–8.0 completas, mas `task_4.md` tinha 4.1–4.5 desmarcadas (corrigido, ver abaixo).

### 2. Bugs corrigidos no backend
| # | Arquivo | Problema | Correção |
|---|---------|----------|----------|
| 1 | `SimuladoAdminService.resumo()` | `tentativasConcluidas` hardcoded `0` (TechSpec exige contagem de tentativas `VALIDA` finalizadas) | Usa `countBySimuladoIdAndTipoAndStatus(...)` (método novo em `TentativaSimuladoRepository`) |
| 2 | `SimuladoAdminService.preencher()` | `bancaId` inexistente retornava 404; TechSpec exige 422 em `bancaId` | Lança `DadosInvalidosException(Map.of("bancaId", ...))` |
| 3 | `SimuladoController.iniciar()` | Sempre 201; TechSpec exige 200 ao retomar tentativa em andamento | Novo `TentativaSimuladoService.temEmAndamento()`; controller retorna 200 se retomada, 201 se criada |
| 4 | `SimuladoAdminService.listar()` | Ramo sem filtro usava `findByExcluidoEmIsNull` sem ordenação (paginacão instável) | `PageRequest` com `Sort.by("atualizadoEm").descending()` |

### 3. Testes novos no backend (todos passando)
- `TU26_recusaCriarSimuladoComBancaInexistente`, `TU27_listaAdminTrazContagemDeTentativasConcluidas`
  (`SimuladoAdminServiceTest`), `TU28_indicaTentativaEmAndamentoParaDecidirStatusHttp`
  (`TentativaSimuladoServiceTest`), `TI13_iniciarRetomaCom200ECriaCom201`
  (`TentativaSimuladoIntegracaoTest`).
- Suíte simulado: **41 testes, 0 falhas**. Suíte backend completa: **516 testes, 0 falhas**
  (rodada antes das correções; reverificação completa pendente — ver próximos passos).

### 4. Documentação atualizada
- `TECH-SPEC.md` §6: adicionadas as 7 rotas admin de simulados
  (`GET /admin/simulados`, `POST`, `GET/PUT /{id}`, `DELETE`, `PUT /composicao`,
  `POST /publicar`, `POST /despublicar`) — faltavam (regra 4 do AGENTS.md).
- `tasks/prd-simulados/task_4.md`: subtarefas 4.1–4.5 marcadas como feitas; corrigido o nome
  do arquivo de teste (`PublicacaoSimuladoIntegracaoTest`, não `SimuladoAutorizacaoIntegracaoTest`).

### 5. Frontend — cobertura destravada
- Problema: `npm run test:coverage` falhava em `Functions 79.64% < 80%`.
- Causa raiz: templates `.html` do admin sem teste de interação (0/5, 0/5, 0/7 funções);
  código `.ts` de simulados já estava 100%.
- Adicionados: 4 testes em `admin/simulados-page.spec.ts` (filtro, erro, página negativa,
  render + cliques), 3 em `simulado-form-page.spec.ts` (erro carga, erro publicar,
  composição sem detalhe; teste de criar agora submete o `<form>`), 1 em
  `composicao-disciplinas.spec.ts` (clique em Salvar).
- Resultado: **427 testes passando, cobertura Functions 80.27% — passando**.

### 6. Achados que NÃO são bug (registrar no codereview)
- `ExpiradorTentativaSimulado`: checar só `expiradaEm != null` está correto — nesse domínio
  `expiradaEm` só é preenchida quando a sessão já expirou (`ExpiradorSessoes`). `Clock`
  injetado sem uso: ressalva baixa.
- Prettier acusa arquivos preexistentes também (`simulado-api.ts`, specs antigas) — repo
  não está prettier-limpo; não reformatar.

## Próximos passos (amanhã)

1. **Reverificação backend completa**: `cd backend && ./mvnw verify`
   (a tentativa de hoje foi abortada; a suíte do módulo simulado já passa com 41 testes).
   Confirmar `All coverage checks have been met` (JaCoCo ≥ 80%).
2. **Confirmar frontend**: `cd frontend && npm run test:coverage` (passando em 80.27%) e
   `npm test` (427 passando). Opcional: build `npm run build`.
3. **E2E (fluxo 5 da skill)**: `npx playwright test` — ainda não executado nesta revisão
   (exige front+back reais; TechSpec prevê `docker-compose.e2e.yml`). Decidir se executa
   ou registra como pendência do QA.
4. **Gerar `tasks/prd-simulados/codereview.md`** seguindo
   `.agents/skills/executar-review/references/TEMPLATE.md`, com:
   - Conformidade com regras: NOK em `code-standards §2.2` — `SimuladoAdminService.java`
     com ~125 linhas (> 100). Não refatorado nesta revisão (risco vs. benefício);
     propor veredito **APROVADO COM RESSALVAS** com extração de mapeador como follow-up.
   - Aderência à TechSpec: tabela com os 10 endpoints + decisões (série congelada,
     VALIDA-na-finalização, estado derivado, gatilho vs. rule).
   - Tarefas 1.0–8.0 verificadas;
   - Testes: totais backend/frontend, cobertura, E2E (se executado ou pendente).
   - Problemas corrigidos (tabela do §2 acima) + ressalvas
     (`@Autowired` redundante em `MontadorProvaSimulado`, `Clock` não usado no expirador,
     `Instant.now()` em `TentativaSimulado.aoCriar`).
5. **Encerrar ambiente (fluxo 8)**: nenhum serviço/isolado foi iniciado (testes usam
   Testcontainers efêmeros); só confirmar que não restou processo Maven/Vite nas portas.
6. **Atualizar todos deste arquivo**: marcar `TodoWrite` correspondente ao concluir.

## Comandos de referência

```bash
cd backend && ./mvnw verify
cd frontend && npm run test:coverage
cd frontend && npx playwright test   # se decidir executar E2E
```
