import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const estudante = 'estudante@dev.certame.local';
const senhaDev = 'senhaDev123';

async function login(page: Page, email = estudante, senha = senhaDev): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(senha);
  // Espera a resposta (qualquer status: há cenários de credencial inválida) antes de seguir,
  // senão a navegação corre contra a gravação da sessão.
  const autenticacao = page.waitForResponse(
    (r) => r.url().includes('/api/v1/auth/login') && r.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Entrar' }).click();
  await autenticacao;
}

async function esperarEmail(destinatario: string): Promise<string> {
  const inicio = Date.now();
  while (Date.now() - inicio < 10_000) {
    const resposta = await fetch('http://localhost:30580/api/v2/messages');
    const dados = (await resposta.json()) as {
      items: Array<{ Content: { Headers: { To: string[] }; Body: string } }>;
    };
    const mensagem = dados.items.find((item) =>
      item.Content.Headers.To.some((to) => to.includes(destinatario)),
    );
    if (mensagem) return mensagem.Content.Body;
    await new Promise((resolver) => setTimeout(resolver, 250));
  }
  throw new Error(`E-mail não recebido para ${destinatario}`);
}

function extrairToken(corpo: string): string {
  const normalizado = corpo.replace(/=\r?\n/g, '');
  const token = normalizado.match(/\/definir-senha\/([A-Za-z0-9_-]+)/)?.[1];
  if (!token) throw new Error('Token de redefinição ausente no e-mail');
  return token;
}

test('E2E-01 entra como estudante', async ({ page }) => {
  await login(page);
  await expect(page).toHaveURL(/\/inicio$/);
  await expect(page.getByRole('heading', { name: 'Continue seus estudos' })).toBeVisible();
  await expect(page.getByRole('navigation')).toContainText('Resolver questões');
});

test('E2E-02 erro de senha preserva o e-mail', async ({ page }) => {
  await login(page, estudante, 'senha-incorreta');
  await expect(page.getByRole('alert')).toContainText('E-mail ou senha inválidos.');
  await expect(page.getByLabel('E-mail')).toHaveValue(estudante);
  await expect(page.getByLabel('Senha')).toBeFocused();
});

test('E2E-03 cria conta pela aba Criar conta', async ({ page }) => {
  const email = `e2e-${Date.now()}@example.com`;
  await page.goto('/cadastro');
  await page.getByLabel('Nome completo').fill('Pessoa E2E');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill('senhaSegura123');
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await expect(page).toHaveURL(/\/inicio$/);
  await expect(page.locator('.identidade')).toContainText('Pessoa E2E');
});

test('E2E-04 cadastro com e-mail duplicado', async ({ page }) => {
  await page.goto('/cadastro');
  await page.getByLabel('Nome completo').fill('Outro Nome');
  await page.getByLabel('E-mail').fill(estudante);
  await page.getByLabel('Senha').fill('senhaSegura123');
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await expect(page.getByLabel('E-mail')).toHaveValue(estudante);
  await expect(page.getByLabel('Nome completo')).toHaveValue('Outro Nome');
  await expect(page.getByLabel('Senha')).toHaveValue('senhaSegura123');
  await expect(page.locator('.erro-campo')).toContainText('já está em uso');
});

test('E2E-05 esqueci minha senha com e-mail inexistente', async ({ page }) => {
  await page.goto('/esqueci-senha');
  await page.getByLabel('E-mail').fill(`inexistente-${Date.now()}@example.com`);
  await page.getByRole('button', { name: 'Enviar link de redefinição' }).click();
  await expect(page.getByRole('heading', { name: 'Verifique seu e-mail' })).toBeVisible();
});

test('E2E-06 redefine senha pelo link do MailHog', async ({ page }) => {
  const email = `reset-${Date.now()}@example.com`;
  await page.goto('/cadastro');
  await page.getByLabel('Nome completo').fill('Reset E2E');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill('senhaSegura123');
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await page.getByTitle('Sair').click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goto('/esqueci-senha');
  await page.getByLabel('E-mail').fill(email);
  await page.getByRole('button', { name: 'Enviar link de redefinição' }).click();
  const corpo = await esperarEmail(email);
  const token = extrairToken(corpo);
  await page.goto(`/definir-senha/${token}`);
  await page.getByLabel('Nova senha').fill('novaSenhaSegura123');
  const redefinicao = page.waitForResponse((resposta) =>
    resposta.url().endsWith('/auth/senha/redefinir'),
  );
  await page.getByRole('button', { name: 'Salvar nova senha' }).click();
  const respostaRedefinicao = await redefinicao;
  expect(respostaRedefinicao.status()).toBe(204);
  await expect(page.getByRole('heading', { name: 'Senha redefinida' })).toBeVisible();
});

test('E2E-07 reabrir link já usado', async ({ page }) => {
  const email = `reuse-${Date.now()}@example.com`;
  await page.goto('/cadastro');
  await page.getByLabel('Nome completo').fill('Reuse E2E');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill('senhaSegura123');
  await page.getByRole('button', { name: 'Criar conta' }).click();
  await page.getByTitle('Sair').click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goto('/esqueci-senha');
  await page.getByLabel('E-mail').fill(email);
  await page.getByRole('button', { name: 'Enviar link de redefinição' }).click();
  const corpo = await esperarEmail(email);
  const token = extrairToken(corpo);
  await page.goto(`/definir-senha/${token}`);
  await page.getByLabel('Nova senha').fill('novaSenhaSegura123');
  await page.getByRole('button', { name: 'Salvar nova senha' }).click();
  await expect(page.getByRole('heading', { name: 'Senha redefinida' })).toBeVisible();
  await page.reload();
  await expect(page.getByLabel('Nova senha')).toBeVisible();
  await page.getByLabel('Nova senha').fill('outraSenhaSegura123');
  await page.getByRole('button', { name: 'Salvar nova senha' }).click();
  await expect(page.getByRole('heading', { name: 'Link inválido' })).toBeVisible();
});

test('E2E-08 sair e voltar pelo histórico não restaura sessão', async ({ page }) => {
  await login(page);
  await page.getByTitle('Sair').click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goBack();
  await expect(page).toHaveURL(/\/login/);
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();
});

test('E2E-09 recarregar autenticado restaura a sessão', async ({ page }) => {
  await login(page);
  await expect
    .poll(async () =>
      (await page.context().cookies()).some((cookie) => cookie.name === 'certame_rt'),
    )
    .toBe(true);
  await page.reload();
  await expect(page).toHaveURL(/\/inicio$/);
  await expect(page.getByRole('heading', { name: 'Continue seus estudos' })).toBeVisible();
});

test('E2E-10 teclado, foco e acessibilidade nas quatro telas', async ({ page }) => {
  const rotas = ['/login', '/cadastro', '/esqueci-senha', '/definir-senha/token-invalido'];
  for (const rota of rotas) {
    await page.goto(rota);
    const resultado = await new AxeBuilder({ page }).analyze();
    expect(resultado.violations, `acessibilidade em ${rota}`).toEqual([]);
    await page.keyboard.press('Tab');
    await expect(page.locator(':focus')).toBeVisible();
  }
  await page.goto('/login');
  await page.getByRole('tab', { name: 'Criar conta' }).focus();
  await page.keyboard.press('Enter');
  await expect(page.getByLabel('Nome completo')).toBeVisible();
});

const perfis = [
  { perfil: 'estudante', email: 'estudante@dev.certame.local', destino: /\/inicio$/ },
  { perfil: 'professor', email: 'professor@dev.certame.local', destino: /\/admin\/questoes$/ },
  { perfil: 'administrador', email: 'admin@dev.certame.local', destino: /\/admin\/simulados$/ },
  {
    perfil: 'super administrador',
    email: 'superadmin@dev.certame.local',
    destino: /\/admin\/questoes$/,
  },
];

for (const { perfil, email, destino } of perfis) {
  test(`E2E-11 ${perfil} entra e chega numa tela real, nunca de volta no login`, async ({
    page,
  }) => {
    await login(page, email);

    await expect(page).toHaveURL(destino);
    await expect(page.locator('.shell')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Entrar' })).toHaveCount(0);
  });
}

test('E2E-12 URL inexistente não expulsa quem já entrou para o login', async ({ page }) => {
  await login(page, 'admin@dev.certame.local');
  await expect(page.locator('.shell')).toBeVisible();

  await page.goto('/rota/que/nao/existe');

  await expect(page).not.toHaveURL(/\/login/);
  await expect(page.locator('.shell')).toBeVisible();
});

test('E2E-13 URL inexistente leva o visitante ao login preservando o retorno', async ({ page }) => {
  await page.goto('/rota/que/nao/existe');

  await expect(page).toHaveURL(/\/login/);
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();
});
