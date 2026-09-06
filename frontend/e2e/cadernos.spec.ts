import { test, expect, type APIRequestContext, type Page } from '@playwright/test';

const apiBase = 'http://localhost:3000/api/v1';
const estudante = 'estudante@dev.certame.local';
const senha = 'senhaDev123';

async function login(page: Page, email = estudante): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(senha);
  const resposta = page.waitForResponse((item) => item.url().includes('/auth/login'));
  await page.getByRole('button', { name: 'Entrar' }).click();
  expect((await resposta).status()).toBe(200);
  await expect(page).toHaveURL(/\/inicio$/);
}

async function autenticar(api: APIRequestContext, email = estudante): Promise<string> {
  const resposta = await api.post(`${apiBase}/auth/login`, { data: { email, senha } });
  expect(resposta.ok(), `login HTTP ${resposta.status()} ${await resposta.text()}`).toBe(true);
  return (await resposta.json()).accessToken;
}

function cabecalho(token: string) {
  return { Authorization: `Bearer ${token}` };
}

const cardAutomatico = (page: Page, titulo: string) =>
  page.locator('.card.caderno').filter({ hasText: titulo });

/**
 * Garante ao menos uma marcação prévia: numa base recém-semeada os dois cadernos automáticos
 * nascem com zero questões e a tela de cadernos mostra o estado vazio, não a grade (CA-11).
 */
async function garantirHistoricoDeCadernos(request: APIRequestContext): Promise<void> {
  const token = await autenticar(request);
  const questoes = await (
    await request.get(`${apiBase}/questoes?size=1`, { headers: cabecalho(token) })
  ).json();
  expect(
    questoes.content.length,
    'a semente dev precisa de ao menos 1 questão publicada',
  ).toBeGreaterThan(0);
  const marcada = await request.put(
    `${apiBase}/estudo/questoes/${questoes.content[0].id}/marcacoes`,
    {
      headers: cabecalho(token),
      data: { favorita: true, erroRecorrente: false },
    },
  );
  expect(marcada.status(), await marcada.text()).toBe(200);
}

test('E2E-01 abre a tela de cadernos com nome, descrição, progresso e data em formato brasileiro', async ({
  page,
  request,
}) => {
  await garantirHistoricoDeCadernos(request);
  await login(page);
  await page.goto('/cadernos');

  const erros = cardAutomatico(page, 'Erros recorrentes');
  const favoritas = cardAutomatico(page, 'Favoritas');
  await expect(erros).toBeVisible();
  await expect(favoritas).toBeVisible();

  for (const card of [erros, favoritas]) {
    await expect(card.locator('.card-title')).not.toHaveText('');
    await expect(card.locator('.descricao')).not.toHaveText('');
    await expect(card.locator('.barra span')).toHaveCount(1);
    await expect(card.locator('.card-meta')).toContainText(/\d+\/\d+/);
    await expect(card.locator('.card-meta')).toContainText(
      /Ainda não atualizado|\d{2}\/\d{2}\/\d{4}/,
    );
  }
});

/** Lista as questões de um caderno (automático ou manual) pela API — mais robusto que navegar a série na UI. */
async function codigosDoCaderno(
  request: APIRequestContext,
  token: string,
  cadernoId: string,
): Promise<string[]> {
  const pagina = await (
    await request.get(`${apiBase}/cadernos/${cadernoId}/questoes?size=100`, {
      headers: cabecalho(token),
    })
  ).json();
  return pagina.content.map((q: { codigo: string }) => q.codigo);
}

test('E2E-02 marca erro recorrente na sessão e reencontra a questão em Erros recorrentes', async ({
  page,
  request,
}) => {
  const token = await autenticar(request);
  await login(page);
  await page.goto('/cadernos');
  const card = cardAutomatico(page, 'Erros recorrentes');
  const metaAntes = await card.locator('.card-meta').innerText();

  await page.goto('/estudar?modo=FOCO');
  await expect(page.locator('.questao')).toBeVisible();
  const codigo = await page.locator('.codigo').innerText();
  await page.getByRole('button', { name: 'Marcar erro recorrente' }).click();
  await expect(page.getByRole('button', { name: 'Remover erro recorrente' })).toBeVisible();

  await page.goto('/cadernos');
  await expect(card.locator('.card-meta')).not.toHaveText(metaAntes);

  await card.click();
  await expect(page).toHaveURL(/\/estudar\?sessao=/);
  await expect(page.locator('.eyebrow')).toContainText('questões');
  const codigos = await codigosDoCaderno(request, token, 'auto:ERRO_RECORRENTE');
  expect(codigos.some((item) => codigo.includes(item))).toBe(true);
});

test('E2E-03 favorita uma questão e a reencontra em Favoritas', async ({ page, request }) => {
  const token = await autenticar(request);
  await login(page);
  await page.goto('/cadernos');
  const card = cardAutomatico(page, 'Favoritas');
  const metaAntes = await card.locator('.card-meta').innerText();

  await page.goto('/estudar?modo=FOCO');
  await expect(page.locator('.questao')).toBeVisible();
  const codigo = await page.locator('.codigo').innerText();
  await page.getByRole('button', { name: 'Favoritar' }).click();
  await expect(page.getByRole('button', { name: 'Desfavoritar' })).toBeVisible();

  await page.goto('/cadernos');
  await expect(card.locator('.card-meta')).not.toHaveText(metaAntes);

  await card.click();
  await expect(page).toHaveURL(/\/estudar\?sessao=/);
  const codigos = await codigosDoCaderno(request, token, 'auto:FAVORITA');
  expect(codigos.some((item) => codigo.includes(item))).toBe(true);
});

test('E2E-04 cria caderno manual com nome e descrição e adiciona questões', async ({ page }) => {
  const nome = `Revisão E2E ${Date.now()}`;

  await login(page);
  await page.goto('/cadernos');
  await page.getByRole('button', { name: 'Novo caderno' }).click();
  await page.getByLabel('Nome do caderno').fill(nome);
  await page.locator('#descricao-caderno').fill('Caderno criado pelo cenário E2E-04.');
  await page.getByRole('button', { name: 'Criar caderno' }).click();
  await expect(page.locator('.dialog')).toHaveCount(0);

  const card = page.locator('.card.caderno').filter({ hasText: nome });
  await expect(card).toBeVisible();
  await expect(card.locator('.card-meta')).toContainText('0/0');

  await card.getByLabel(`Ações do caderno ${nome}`).click();
  await card.getByRole('button', { name: 'Adicionar questões' }).click();
  const picker = page.locator('.dialog.picker');
  await expect(picker).toBeVisible();
  await picker.locator('tbody tr input[type=checkbox]').nth(0).check();
  await picker.locator('tbody tr input[type=checkbox]').nth(1).check();
  await picker.getByRole('button', { name: 'Adicionar selecionadas' }).click();
  await expect(page.locator('.dialog')).toHaveCount(0);

  await expect(card.locator('.card-meta')).toContainText('0/2');
});

test('E2E-05 abre um caderno de 40 questões e estuda no modo Foco', async ({ page, request }) => {
  const token = await autenticar(request);
  const questoes = await (
    await request.get(`${apiBase}/questoes?size=40`, { headers: cabecalho(token) })
  ).json();
  expect(
    questoes.content.length,
    'a semente dev precisa de ao menos 40 questões publicadas',
  ).toBeGreaterThanOrEqual(40);
  const questaoIds = questoes.content.slice(0, 40).map((q: { id: string }) => q.id);

  const nome = `Simulado de 40 questões ${Date.now()}`;
  const criado = await request.post(`${apiBase}/cadernos`, {
    headers: cabecalho(token),
    data: { nome, descricao: 'Caderno de 40 questões do cenário E2E-05.' },
  });
  expect(criado.status()).toBe(201);
  const caderno = await criado.json();
  const adicionado = await request.post(`${apiBase}/cadernos/${caderno.id}/questoes`, {
    headers: cabecalho(token),
    data: { questaoIds },
  });
  expect(adicionado.status(), await adicionado.text()).toBe(200);
  expect((await adicionado.json()).totalQuestoes).toBe(40);

  await login(page);
  await page.goto('/cadernos');
  const card = page.locator('.card.caderno').filter({ hasText: nome });
  await expect(card).toBeVisible();
  await card.click();

  await expect(page).toHaveURL(/\/estudar\?sessao=/);
  await expect(page.locator('.eyebrow')).toContainText('40 questões');
  // Modo Foco é forçado para sessões abertas a partir de um caderno (RN sobre origem CADERNO).
  await expect(page.locator('.seg .ativa')).toHaveText('Foco');

  for (let i = 0; i < 3; i++) {
    await page.getByRole('radio').first().click();
    await page.getByRole('button', { name: /Responder/ }).click();
    await page.getByRole('button', { name: 'Próxima' }).click();
  }

  // O Foco não tem "finalizar" na UI (só o Modo Prova); a sessão só conta para o progresso do
  // caderno depois de finalizada (mesma regra de RN-04 para o painel de desempenho).
  const sessaoId = new URL(page.url()).searchParams.get('sessao');
  const finalizada = await request.post(`${apiBase}/estudo/sessoes/${sessaoId}/finalizar`, {
    headers: cabecalho(token),
    data: { respostas: [], pausadoMs: 0 },
  });
  expect(finalizada.status(), await finalizada.text()).toBe(200);

  await page.goto('/cadernos');
  await expect(card.locator('.card-meta')).toContainText('3/40');
});
