import { defineConfig, devices } from '@playwright/test';

// As portas do Postgres/MailHog de teste (30532/30525) são fixas em `docker-compose.e2e.yml`
// (`!override`), independentemente do nome do projeto compose: reaproveitadas aqui tal como
// no `playwright.config.ts`, já que as duas suítes nunca sobem ao mesmo tempo.
const compose =
  'docker compose -p certame-e2e-prod -f ../docker-compose.yml -f ../docker-compose.e2e.yml';
const ambiente =
  'DB_HOST=localhost DB_PORT=30532 MAIL_HOST=localhost MAIL_PORT=30525 SPA_ORIGIN=http://localhost:5101';
const argumentos = '--server.port=3001';
const semFork = '-Dspring-boot.run.fork=false';

// CA-14 (300 ms p95 para avançar de questão) só vale contra a build de produção: o dev server
// (`playwright.config.ts`) não reflete o bundle minificado que o usuário final recebe.
export default defineConfig({
  testDir: './e2e',
  testMatch: '**/desempenho.spec.ts',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  reporter: 'html',
  globalTeardown: './e2e/global-teardown.prod.ts',
  use: {
    baseURL: 'http://localhost:5101',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: `${compose} down -v --remove-orphans && ${compose} up -d --wait && ${ambiente} ../backend/mvnw -f ../backend/pom.xml spring-boot:run ${semFork} -Dspring-boot.run.profiles=dev "-Dspring-boot.run.arguments=${argumentos}"`,
      url: 'http://localhost:3001/actuator/health',
      reuseExistingServer: !process.env['CI'],
      timeout: 300_000,
      stdout: 'pipe',
      stderr: 'pipe',
      env: {
        ...process.env,
        DB_HOST: 'localhost',
        DB_PORT: '30532',
        MAIL_HOST: 'localhost',
        MAIL_PORT: '30525',
        SPA_ORIGIN: 'http://localhost:5101',
        SPRING_DEVTOOLS_RESTART_ENABLED: 'false',
      },
    },
    {
      // Build de produção real: `ng serve --configuration production` compila com as mesmas
      // otimizações do `ng build` (minificação, sem source maps) em vez do dev server padrão.
      command:
        'npm start -- --configuration production --port 5101 --proxy-config proxy.e2e.prod.conf.json',
      url: 'http://localhost:5101',
      reuseExistingServer: !process.env['CI'],
      timeout: 180_000,
    },
  ],
});
