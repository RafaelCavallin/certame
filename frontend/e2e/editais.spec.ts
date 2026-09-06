import { test, expect, type APIRequestContext, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const apiBase = 'http://localhost:3000/api/v1';
const senha = 'senhaDev123';
const estudante = 'estudante@dev.certame.local';
const administrador = 'admin@dev.certame.local';

function cabecalho(token: string) {
  return { Authorization: `Bearer ${token}` };
}

async function autenticar(api: APIRequestContext, email: string): Promise<string> {
  const resposta = await api.post(`${apiBase}/auth/login`, { data: { email, senha } });
  expect(resposta.ok(), `login HTTP ${resposta.status()} ${await resposta.text()}`).toBe(true);
  return (await resposta.json()).accessToken;
}

/**
 * Aguarda a resposta do login antes de seguir. Sem isso a navegação seguinte corre contra a
 * gravação da sessão e a suíte inteira acaba executando na tela de login.
 */
async function login(page: Page, email: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(senha);
  const autenticacao = page.waitForResponse(
    (r) => r.url().includes('/api/v1/auth/login') && r.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Entrar' }).click();
  expect((await autenticacao).status(), 'login recusado').toBe(200);
  await expect(page).not.toHaveURL(/\/login$/);
}

/** Troca de persona na mesma aba: a suíte não abre contextos de navegador extras. */
async function trocarPara(page: Page, email: string): Promise<void> {
  await page.locator('.sair').click();
  await login(page, email);
}

interface DadosEdital {
  nome: string;
  status: 'ABERTO' | 'PREVISTO' | 'ENCERRADO';
  vagas: number | null;
  bancaId: string | null;
  orgaoId: string | null;
  prazoInscricao: string | null;
  urlOficial: string | null;
  disciplinaIds: string[];
}

/** Arranjo por API: a tela sob teste é a de leitura ou de edição, não a de cadastro repetido. */
async function criarEdital(
  api: APIRequestContext,
  token: string,
  dados: Partial<DadosEdital> = {},
) {
  const base: DadosEdital = {
    nome: `Edital E2E ${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    status: 'PREVISTO',
    vagas: null,
    bancaId: null,
    orgaoId: null,
    prazoInscricao: null,
    urlOficial: null,
    disciplinaIds: [],
  };
  const resposta = await api.post(`${apiBase}/admin/editais`, {
    headers: cabecalho(token),
    data: { ...base, ...dados },
  });
  expect(resposta.status(), await resposta.text()).toBe(201);
  return (await resposta.json()) as { id: string; nome: string };
}

async function primeiraDisciplina(
  api: APIRequestContext,
  token: string,
): Promise<{ id: string; nome: string }> {
  const resposta = await api.get(`${apiBase}/admin/disciplinas`, { headers: cabecalho(token) });
  const arvore = (await resposta.json()) as { id: string; nome: string }[];
  return arvore[0];
}

const formatarVagas = (vagas: number): string => vagas.toLocaleString('pt-BR');
const formatarData = (iso: string): string => {
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
};

test('E2E-01 estudante abre Editais pelo menu e vê os três agrupamentos por status', async ({
  page,
}) => {
  await login(page, estudante);
  await page.getByRole('link', { name: 'Editais' }).click();

  await expect(page).toHaveURL(/\/editais$/);
  await expect(page.getByRole('heading', { name: 'Abertos' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Previstos' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Encerrados' })).toBeVisible();
  await expect(
    page.locator('.edital-card').filter({ hasText: 'PF — Agente de Polícia Federal' }),
  ).toBeVisible();
  await expect(
    page.locator('.edital-card').filter({ hasText: 'INSS — Técnico do Seguro Social' }),
  ).toBeVisible();
});

test('E2E-02 abrir um edital pela URL exibe as disciplinas exigidas sem nova requisição à API', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, estudante);
  const agrupados = await (
    await request.get(`${apiBase}/editais`, { headers: cabecalho(token) })
  ).json();
  const pf = agrupados.abertos.find((e: { nome: string }) => e.nome.startsWith('PF —'));
  expect(pf, 'edital PF da semente de dev não encontrado').toBeTruthy();

  let chamadas = 0;
  page.on('request', (req) => {
    if (req.url().endsWith('/api/v1/editais')) chamadas++;
  });
  await login(page, estudante);
  await page.goto(`/editais/${pf.id}`);

  const card = page.locator('.edital-card').filter({ hasText: pf.nome });
  await expect(card).toHaveClass(/selecionado/);
  for (const disciplina of pf.disciplinas) {
    await expect(card.locator('.tag').filter({ hasText: disciplina.nome })).toBeVisible();
  }
  expect(chamadas, 'a rota de detalhe não deveria repetir a consulta de /editais').toBe(1);
});

test('E2E-03 confere banca, vagas e prazo formatados em pt-BR no edital aberto', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, estudante);
  const agrupados = await (
    await request.get(`${apiBase}/editais`, { headers: cabecalho(token) })
  ).json();
  const pf = agrupados.abertos.find((e: { nome: string }) => e.nome.startsWith('PF —'));
  expect(pf).toBeTruthy();

  await login(page, estudante);
  await page.goto('/editais');
  const card = page.locator('.edital-card').filter({ hasText: pf.nome });
  await expect(card).toContainText(pf.banca.nome);
  await expect(card).toContainText(formatarVagas(pf.vagas));
  await expect(card).toContainText(formatarData(pf.prazoInscricao));
});

test('E2E-04 administrador cadastra um edital completo e ele já aparece para o estudante', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, administrador);
  const disciplina = await primeiraDisciplina(request, token);
  const nome = `Edital E2E completo ${Date.now()}`;

  await login(page, administrador);
  await page.goto('/admin/editais/novo');
  await page.locator('#nome').fill(nome);
  await page.getByText('Aberto', { exact: true }).click();
  await page.locator('#vagas').fill('50');
  await page.locator('#prazo').fill('2027-01-15');
  await page.locator('#url').fill('https://www.exemplo.gov.br/edital');
  await page
    .locator('.seletor .item')
    .filter({ hasText: disciplina.nome })
    .locator('input[type=checkbox]')
    .check();
  await page.getByRole('button', { name: 'Cadastrar edital' }).click();

  await expect(page).toHaveURL(/\/admin\/editais$/);
  await expect(page.locator('.table tbody tr').filter({ hasText: nome })).toBeVisible();

  await trocarPara(page, estudante);
  await page.goto('/editais');
  await expect(page.locator('.edital-card').filter({ hasText: nome })).toBeVisible();
});

test('E2E-05 administrador corrige um dado e o estudante vê o valor novo ao recarregar', async ({
  page,
  request,
}) => {
  const tokenAdmin = await autenticar(request, administrador);
  const edital = await criarEdital(request, tokenAdmin, {
    status: 'ABERTO',
    vagas: 100,
    prazoInscricao: '2027-03-01',
  });

  await login(page, estudante);
  await page.goto('/editais');
  await expect(page.locator('.edital-card').filter({ hasText: edital.nome })).toContainText(
    '100 vagas',
  );

  await trocarPara(page, administrador);
  await page.goto(`/admin/editais/${edital.id}`);
  await page.locator('#vagas').fill('250');
  await page.getByRole('button', { name: 'Salvar alterações' }).click();
  await expect(page).toHaveURL(/\/admin\/editais$/);

  await trocarPara(page, estudante);
  await page.goto('/editais');
  await expect(page.locator('.edital-card').filter({ hasText: edital.nome })).toContainText(
    '250 vagas',
  );
});

test('E2E-06 administrador muda edital de previsto para aberto e ele migra de agrupamento na tela do estudante', async ({
  page,
  request,
}) => {
  const tokenAdmin = await autenticar(request, administrador);
  const edital = await criarEdital(request, tokenAdmin, { status: 'PREVISTO' });

  await login(page, estudante);
  await page.goto('/editais');
  const secaoPrevistos = page
    .locator('.editais-page section')
    .filter({ has: page.getByRole('heading', { name: 'Previstos' }) });
  await expect(
    secaoPrevistos.locator('.edital-card').filter({ hasText: edital.nome }),
  ).toBeVisible();

  await trocarPara(page, administrador);
  await page.goto(`/admin/editais/${edital.id}`);
  await page.getByText('Aberto', { exact: true }).click();
  await page.locator('#prazo').fill('2027-02-20');
  await page.getByRole('button', { name: 'Salvar alterações' }).click();
  await expect(page).toHaveURL(/\/admin\/editais$/);

  await trocarPara(page, estudante);
  await page.goto('/editais');
  const secaoAbertos = page
    .locator('.editais-page section')
    .filter({ has: page.getByRole('heading', { name: 'Abertos' }) });
  await expect(secaoAbertos.locator('.edital-card').filter({ hasText: edital.nome })).toBeVisible();
  const previstosAtualizada = page
    .locator('.editais-page section')
    .filter({ has: page.getByRole('heading', { name: 'Previstos' }) });
  await expect(
    previstosAtualizada.locator('.edital-card').filter({ hasText: edital.nome }),
  ).toHaveCount(0);
});

test('E2E-07 administrador exclui um edital com confirmação e ele some da lista do estudante', async ({
  page,
  request,
}) => {
  const tokenAdmin = await autenticar(request, administrador);
  const edital = await criarEdital(request, tokenAdmin, { status: 'ENCERRADO' });

  await login(page, administrador);
  await page.goto('/admin/editais');
  const linha = page.locator('.table tbody tr').filter({ hasText: edital.nome });
  await expect(linha).toBeVisible();
  await linha.locator('button[title="Excluir"]').click();

  const dialogo = page.locator('.dialog');
  await expect(dialogo).toContainText('Excluir edital');
  await dialogo.getByRole('button', { name: 'Excluir' }).click();

  await expect(dialogo).toHaveCount(0);
  await expect(linha).toHaveCount(0);

  const tokenEstudante = await autenticar(request, estudante);
  const agrupados = await (
    await request.get(`${apiBase}/editais`, { headers: cabecalho(tokenEstudante) })
  ).json();
  const todos = [...agrupados.abertos, ...agrupados.previstos, ...agrupados.encerrados];
  expect(todos.some((e: { id: string }) => e.id === edital.id)).toBe(false);
});

test('E2E-08 seleciona disciplinas a partir da árvore de matérias, sem aceitar texto livre', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, administrador);
  const disciplina = await primeiraDisciplina(request, token);

  await login(page, administrador);
  await page.goto('/admin/editais/novo');

  const seletor = page.locator('.seletor');
  const filtro = seletor.getByLabel('Filtrar disciplinas');
  await filtro.fill('Disciplina que não existe na árvore');
  await expect(seletor.locator('.lista .item')).toHaveCount(0);
  await expect(seletor.locator('.lista')).toContainText('Nenhuma disciplina encontrada.');
  await filtro.press('Enter');
  await expect(seletor.locator('.selecionadas .tag')).toHaveCount(0);

  await filtro.fill('');
  await seletor
    .locator('.item')
    .filter({ hasText: disciplina.nome })
    .locator('input[type=checkbox]')
    .check();
  await expect(seletor.locator('.selecionadas .tag')).toContainText(disciplina.nome);
});

test('E2E-10 estudante não alcança a administração de editais nem pela interface nem pela API', async ({
  page,
  request,
}) => {
  await login(page, estudante);
  await expect(page.locator('nav a[href="/admin/editais"]')).toHaveCount(0);

  await page.goto('/admin/editais');
  await expect(page).toHaveURL(/\/inicio$/);

  const token = await autenticar(request, estudante);
  const resposta = await request.get(`${apiBase}/admin/editais`, { headers: cabecalho(token) });
  expect(resposta.status()).toBe(403);
});

test('E2E-11 lista administrativa sinaliza o edital aberto com prazo vencido', async ({ page }) => {
  await login(page, administrador);
  await page.goto('/admin/editais');

  const linha = page
    .locator('.table tbody tr')
    .filter({ hasText: 'TCU — Auditor Federal de Controle Externo' });
  await expect(linha.locator('.aviso-prazo')).toContainText('prazo vencido, revisar');
  await expect(linha).toContainText('Aberto');
});

test('E2E-12 percorre as duas telas por teclado com foco visível e rótulos de status com contraste adequado', async ({
  page,
}) => {
  await login(page, administrador);
  const rotas = ['/editais', '/admin/editais'];

  for (const rota of rotas) {
    await page.goto(rota);
    await expect(page.locator('.shell')).toBeVisible();

    const analise = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
    expect(
      analise.violations.map((v) => v.id),
      `violações em ${rota}`,
    ).toEqual([]);
    expect(
      analise.passes.map((p) => p.id),
      `contraste não avaliado em ${rota}`,
    ).toContain('color-contrast');

    const rotulos = page.locator('.tag');
    if (await rotulos.count()) await expect(rotulos.first()).not.toHaveText('');

    await page.keyboard.press('Tab');
    const focado = page.locator(':focus');
    await expect(focado, `nada recebeu foco em ${rota}`).toBeVisible();
    const contorno = await focado.evaluate((el) => {
      const estilo = getComputedStyle(el);
      return `${estilo.outlineStyle}|${estilo.outlineWidth}|${estilo.boxShadow}`;
    });
    expect(contorno, `foco sem indicação visível em ${rota}`).not.toBe('none|0px|none');
  }
});

/**
 * Roda por último de propósito: esvazia a base excluindo todos os editais restantes (semeados e
 * criados pelos testes anteriores) para provar o estado vazio de CA-09. Qualquer teste que
 * dependesse da semente de editais precisa vir antes deste na declaração do arquivo.
 */
test('E2E-09 base sem nenhum edital exibe o estado vazio explicativo ao estudante', async ({
  page,
  request,
}) => {
  const tokenAdmin = await autenticar(request, administrador);
  const restantes = await (
    await request.get(`${apiBase}/admin/editais`, { headers: cabecalho(tokenAdmin) })
  ).json();
  for (const edital of restantes as { id: string }[]) {
    const exclusao = await request.delete(`${apiBase}/admin/editais/${edital.id}`, {
      headers: cabecalho(tokenAdmin),
    });
    expect(exclusao.status()).toBe(204);
  }

  await login(page, estudante);
  const errosConsole: string[] = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') errosConsole.push(msg.text());
  });
  await page.goto('/editais');

  await expect(
    page.getByRole('heading', { name: 'Nenhum edital acompanhado no momento.' }),
  ).toBeVisible();
  await expect(page.locator('.edital-card')).toHaveCount(0);
  expect(errosConsole, 'nenhum erro deveria aparecer no console com a base vazia').toEqual([]);
});
