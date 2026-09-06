import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const estudante = 'estudante@dev.certame.local';
const administrador = 'admin@dev.certame.local';
const senha = 'senhaDev123';

async function login(page: Page, email = estudante): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(senha);
  const resposta = page.waitForResponse((item) => item.url().includes('/auth/login'));
  await page.getByRole('button', { name: 'Entrar' }).click();
  expect((await resposta).status()).toBe(200);
}

test('E2E-01 estudante vê os três grupos de simulados', async ({ page }) => {
  await login(page);
  await page.goto('/simulados');
  await expect(page.getByRole('heading', { name: 'Simulados disponíveis' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Próximos simulados' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Concluídos' })).toBeVisible();
  await expect(page.getByText('Disponível', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Agendado', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('100%', { exact: true })).toBeVisible();
  await expect(page.getByText(/concluído em \d{2}\/\d{2}\/\d{4}/)).toBeVisible();
});

test('E2E-02 estudante faz simulado do início ao resultado', async ({ page }) => {
  await login(page);
  await page.goto('/simulados');
  await page.getByRole('button', { name: 'Iniciar' }).first().click();
  await expect(page.locator('.questao')).toBeVisible();
  await expect(page.locator('.conteudo > header span')).toBeVisible();
  await expect(page.getByText('Gabarito')).toHaveCount(0);
  await page.getByRole('radio').first().click();
  await page.getByRole('button', { name: 'Finalizar prova' }).click();
  await expect(page).toHaveURL(/\/simulados\/.*\/resultado/);
  await expect(page.getByText(/Resultado|100%/).first()).toBeVisible();
  await expect(page.getByText('Revisão questão a questão')).toBeVisible();
});

test('E2E-03 estudante revisa e refaz como treino', async ({ page }) => {
  await login(page);
  await page.goto('/simulados');
  await page.getByRole('button', { name: 'Revisar' }).first().click();
  await expect(page.getByText('Revisão questão a questão')).toBeVisible();
  await page.getByRole('link', { name: 'Voltar para simulados' }).click();
  page.once('dialog', (dialog) => void dialog.accept());
  await page.getByRole('button', { name: 'Refazer treino' }).first().click();
  await expect(page.locator('.questao')).toBeVisible();
  await page.goto('/simulados');
  await expect(page.getByText('100%', { exact: true })).toBeVisible();
});

test('E2E-06 navegação por teclado e contraste na lista e no formulário', async ({ page }) => {
  await login(page);
  await page.goto('/simulados');
  const lista = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  expect(lista.violations.map((item) => item.id)).toEqual([]);
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus')).toBeVisible();
  await page.getByTitle('Sair').click();
  await expect(page).toHaveURL(/\/login$/);
  await login(page, administrador);
  await page.goto('/admin/simulados/nova');
  const formulario = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  expect(formulario.violations.map((item) => item.id)).toEqual([]);
  await page.getByLabel('Nome').focus();
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus')).toBeVisible();
});
