# Evidências - prd-sessao-de-estudo
- E2E executados via Playwright com baseURL http://localhost:5100 (isolado docker-compose.e2e p certame-e2e, portas 3000/5100/30532)
- Todos os 7 E2E passaram (E2E-06 skipped, bloqueado por F6a), ver logs no qa.md
- Axe-core sem violações em E2E-08
- Screenshots de falha pré-correção foram removidos pelo globalTeardown (volume -v) conforme playwright.config.ts
