import { test, expect, type Page } from '@playwright/test';

const superAdmin = 'superadmin@dev.certame.local';
const senha = 'senhaDev123';

async function login(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(superAdmin);
  await page.getByLabel('Senha').fill(senha);
  const autenticacao = page.waitForResponse(
    (r) => r.url().includes('/api/v1/auth/login') && r.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Entrar' }).click();
  await autenticacao;
}

test('E2E-01 cria, edita e remove usuário com confirmação nomeada', async ({ page }) => {
  const nome = `Usuario E2E ${Date.now()}`;
  const email = `usuario-e2e-${Date.now()}@example.com`;

  await login(page);
  await page.goto('/super/usuarios');

  // CA-01 — tabela exibe nome, e-mail, perfil, status e data de criação.
  const cabecalho = page.locator('.table thead');
  for (const coluna of ['Nome', 'E-mail', 'Perfil', 'Status', 'Criado em']) {
    await expect(cabecalho).toContainText(coluna);
  }

  // 7.1 — cria o usuário e confirma que aparece na lista sem recarregar a página (CA-03).
  await page.getByRole('link', { name: 'Novo usuário' }).click();
  await expect(page.getByRole('heading', { name: 'Novo usuário' })).toBeVisible();
  await page.getByLabel('Nome completo').fill(nome);
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.locator('.seg-opt', { hasText: 'Professor' }).click();

  const criacao = page.waitForResponse(
    (r) => r.url().includes('/api/v1/super/usuarios') && r.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Criar usuário' }).click();
  expect((await criacao).status()).toBe(201);
  await expect(page).toHaveURL(/\/super\/usuarios$/);

  const linha = page.locator('.table tbody tr').filter({ hasText: nome });
  await expect(linha).toBeVisible();
  await expect(linha).toContainText(email);
  await expect(linha).toContainText('Professor');

  // 7.2 — edita o perfil e confirma a nova capacidade refletida na lista.
  await linha.locator('a[title="Editar"]').click();
  await expect(page.getByRole('heading', { name: 'Editar usuário' })).toBeVisible();
  await page.locator('.seg-opt', { hasText: 'Administrador' }).click();

  const edicao = page.waitForResponse(
    (r) => r.url().includes('/api/v1/super/usuarios/') && r.request().method() === 'PATCH',
  );
  await page.getByRole('button', { name: 'Salvar alterações' }).click();
  expect((await edicao).status()).toBe(200);
  await expect(page).toHaveURL(/\/super\/usuarios$/);

  const linhaEditada = page.locator('.table tbody tr').filter({ hasText: nome });
  await expect(linhaEditada).toContainText('Administrador');

  // 7.3 — aciona a exclusão, confirma no modal nomeado com o usuário afetado e valida a remoção (CA-10).
  await linhaEditada.locator('button[title="Excluir"]').click();
  const dialogo = page.locator('.dialog');
  await expect(page.getByRole('heading', { name: 'Excluir usuário' })).toBeVisible();
  await expect(dialogo).toContainText(nome);

  const exclusao = page.waitForResponse(
    (r) => r.url().includes('/api/v1/super/usuarios/') && r.request().method() === 'DELETE',
  );
  await dialogo.getByRole('button', { name: 'Excluir' }).click();
  expect((await exclusao).status()).toBe(204);
  await expect(dialogo).toHaveCount(0);
  await expect(page.locator('.table tbody tr').filter({ hasText: nome })).toHaveCount(0);

  await page.reload();
  await expect(page.locator('.table tbody tr').filter({ hasText: nome })).toHaveCount(0);
});
