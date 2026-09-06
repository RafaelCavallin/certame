import { test, expect, type Page } from '@playwright/test';

const estudante = 'estudante@dev.certame.local';
const senha = 'senhaDev123';

async function login(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(estudante);
  await page.getByLabel('Senha').fill(senha);
  const resposta = page.waitForResponse((item) => item.url().includes('/auth/login'));
  await page.getByRole('button', { name: 'Entrar' }).click();
  expect((await resposta).status()).toBe(200);
  await expect(page).toHaveURL(/\/inicio$/);
}

test('CA-14 avança para a próxima questão sem nova requisição, em até 300 ms (build de produção)', async ({
  page,
}) => {
  await login(page);
  await page.goto('/estudar?modo=FOCO');
  await expect(page.locator('.questao')).toBeVisible();

  const requisicoes: string[] = [];
  page.on('request', (item) => {
    if (item.url().includes('/estudo/')) requisicoes.push(item.url());
  });

  const inicio = Date.now();
  await page.getByRole('button', { name: 'Próxima' }).click();
  await expect(page.locator('.codigo')).toContainText('Questão 2');
  expect(Date.now() - inicio).toBeLessThan(300);
  expect(requisicoes).toEqual([]);
});
