import { test, expect, type APIRequestContext, type Page } from '@playwright/test';

const apiBase = 'http://localhost:3000/api/v1';
const administrador = 'admin@dev.certame.local';
const senha = 'senhaDev123';

async function loginApi(api: APIRequestContext): Promise<string> {
  const resposta = await api.post(`${apiBase}/auth/login`, {
    data: { email: administrador, senha },
  });
  expect(resposta.ok(), await resposta.text()).toBe(true);
  return (await resposta.json()).accessToken;
}

async function login(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(administrador);
  await page.getByLabel('Senha').fill(senha);
  const resposta = page.waitForResponse((item) => item.url().includes('/auth/login'));
  await page.getByRole('button', { name: 'Entrar' }).click();
  expect((await resposta).status()).toBe(200);
}

async function criar(api: APIRequestContext, token: string, numeroQuestoes = 10) {
  const resposta = await api.post(`${apiBase}/admin/simulados`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      nome: `Simulado E2E ${Date.now()}`,
      descricao: 'Fluxo administrativo.',
      bancaId: null,
      numeroQuestoes,
      duracaoSegundos: 600,
      janelaAbertura: new Date(Date.now() - 60_000).toISOString(),
      janelaEncerramento: new Date(Date.now() + 86_400_000).toISOString(),
    },
  });
  expect(resposta.status()).toBe(201);
  return resposta.json();
}

test('E2E-04 administrador monta e publica simulado', async ({ page, request }) => {
  const token = await loginApi(request);
  const disciplinas = await request.get(`${apiBase}/admin/disciplinas`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const disciplina = (await disciplinas.json()).find(
    (item: { totalPublicadas: number }) => item.totalPublicadas >= 10,
  );
  expect(disciplina).toBeTruthy();
  const simulado = await criar(request, token);
  await login(page);
  await page.goto(`/admin/simulados/${simulado.id}`);
  await page.getByRole('button', { name: 'Adicionar disciplina' }).click();
  await page.locator('select[aria-label="Disciplina 1"]').selectOption(disciplina.id);
  await page.locator('input[aria-label="Quantidade 1"]').fill('5');
  await page.getByRole('button', { name: 'Salvar composição' }).click();
  await expect(page.getByRole('button', { name: 'Publicar' })).toBeDisabled();
  await page.locator('input[aria-label="Quantidade 1"]').fill('10');
  await page.getByRole('button', { name: 'Salvar composição' }).click();
  await expect(page.getByRole('button', { name: 'Publicar' })).toBeEnabled();
  await page.getByRole('button', { name: 'Publicar' }).click();
  await expect(page.getByText('Publicado')).toBeVisible();
});

test('E2E-05 publicação recusada mostra a inconsistência', async ({ page, request }) => {
  const token = await loginApi(request);
  const simulado = await criar(request, token, 200);
  await login(page);
  await page.goto(`/admin/simulados/${simulado.id}`);
  await page.getByLabel('Abertura').fill('');
  await page.getByLabel('Encerramento').fill('');
  await page.getByRole('button', { name: 'Salvar rascunho' }).click();
  await page.getByRole('button', { name: 'Adicionar disciplina' }).click();
  const disciplinas = await request.get(`${apiBase}/admin/disciplinas`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const disciplina = (await disciplinas.json())[0];
  await page.locator('select[aria-label="Disciplina 1"]').selectOption(disciplina.id);
  await page.locator('input[aria-label="Quantidade 1"]').fill('200');
  await page.getByRole('button', { name: 'Salvar composição' }).click();
  await expect(page.getByText(/soma precisa ser igual|200 \/ 200/)).toBeVisible();
  await page.getByRole('button', { name: 'Publicar' }).click();
  await expect(page.getByText(/janela|publicar|disponíveis/i).first()).toBeVisible();
});
