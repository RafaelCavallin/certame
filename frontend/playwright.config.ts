import { defineConfig, devices } from '@playwright/test';

const compose =
  'docker compose -p certame-e2e -f ../docker-compose.yml -f ../docker-compose.e2e.yml';
const ambiente =
  'DB_HOST=localhost DB_PORT=30532 MAIL_HOST=localhost MAIL_PORT=30525 SPA_ORIGIN=http://localhost:5100';
const argumentos = '--server.port=3000';
// `fork=false` mantém a aplicação no processo do Maven: com devtools no classpath o
// spring-boot:run forkaria uma JVM e devolveria BUILD SUCCESS, deixando o Playwright achar que
// o servidor morreu enquanto uma JVM órfã segurava a porta 3000.
const semFork = '-Dspring-boot.run.fork=false';

export default defineConfig({
  testDir: './e2e',
  // CA-14 precisa da build de produção (`playwright.prod.config.ts`, `npm run e2e:producao`), não do dev server.
  testIgnore: ['**/desempenho.spec.ts'],
  fullyParallel: false,
  // A suíte compartilha um único PostgreSQL com a semente de `dev`: um worker por vez.
  workers: 1,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  reporter: 'html',
  globalTeardown: './e2e/global-teardown.ts',
  use: {
    baseURL: 'http://localhost:5100',
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
      // O banco é zerado aqui, e não em globalSetup: os webServer sobem ANTES do globalSetup, então
      // derrubar o volume de lá matava o PostgreSQL debaixo do processo Spring já iniciado.
      command: `${compose} down -v --remove-orphans && ${compose} up -d --wait && ${ambiente} ../backend/mvnw -f ../backend/pom.xml spring-boot:run ${semFork} -Dspring-boot.run.profiles=dev "-Dspring-boot.run.arguments=${argumentos}"`,
      url: 'http://localhost:3000/actuator/health',
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
        SPA_ORIGIN: 'http://localhost:5100',
        SPRING_DEVTOOLS_RESTART_ENABLED: 'false',
      },
    },
    {
      command: 'npm start -- --port 5100 --proxy-config proxy.e2e.conf.json',
      url: 'http://localhost:5100',
      reuseExistingServer: !process.env['CI'],
      timeout: 180_000,
    },
  ],
});
