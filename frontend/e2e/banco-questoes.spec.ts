import { test, expect, type APIRequestContext, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const apiBase = 'http://localhost:3000/api/v1';
const senha = 'senhaDev123';
const professor = 'professor@dev.certame.local';
const administrador = 'admin@dev.certame.local';

type Referencias = {
  disciplinaId: string;
  disciplinaNome: string;
  assuntoId: string;
  assuntoNome: string;
  bancaId: string;
};

async function autenticar(api: APIRequestContext, email: string): Promise<string> {
  const resposta = await api.post(`${apiBase}/auth/login`, { data: { email, senha } });
  expect(resposta.ok(), `login HTTP ${resposta.status()} ${await resposta.text()}`).toBe(true);
  return (await resposta.json()).accessToken;
}

function cabecalho(token: string) {
  return { Authorization: `Bearer ${token}` };
}

async function referencias(api: APIRequestContext, token: string): Promise<Referencias> {
  const [disciplinas, bancas] = await Promise.all([
    api.get(`${apiBase}/admin/disciplinas`, { headers: cabecalho(token) }),
    api.get(`${apiBase}/admin/bancas`, { headers: cabecalho(token) }),
  ]);
  const arvore = await disciplinas.json();
  const disciplina = arvore.find((d: { assuntos: unknown[] }) => d.assuntos.length) ?? arvore[0];
  return {
    disciplinaId: disciplina.id,
    disciplinaNome: disciplina.nome,
    assuntoId: disciplina.assuntos[0].id,
    assuntoNome: disciplina.assuntos[0].nome,
    bancaId: (await bancas.json())[0].id,
  };
}

/** Arranjo por API: a tela sob teste é a de destino, não a de cadastro. */
async function criarQuestao(
  api: APIRequestContext,
  token: string,
  dados: Record<string, unknown> = {},
) {
  const refs = await referencias(api, token);
  const base = {
    tipo: 'ME',
    enunciado: `Questão E2E ${Date.now()}-${Math.random().toString(36).slice(2, 7)} sobre licitações.`,
    disciplinaId: refs.disciplinaId,
    assuntoId: refs.assuntoId,
    bancaId: refs.bancaId,
  };
  const resposta = await api.post(`${apiBase}/admin/questoes`, {
    headers: cabecalho(token),
    data: { ...base, ...dados },
  });
  expect(resposta.status(), await resposta.text()).toBe(201);
  return (await resposta.json()) as { id: string; codigo: string; enunciado: string };
}

const questaoCompleta = {
  alternativas: [
    { texto: 'Primeira alternativa', ordem: 1 },
    { texto: 'Segunda alternativa', ordem: 2 },
  ],
  gabarito: 'A',
  comentario: 'Comentário do professor explicando a resposta.',
};

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
}

async function abrirLista(page: Page, email: string, query = ''): Promise<void> {
  await login(page, email);
  await page.goto(`/admin/questoes${query}`);
  await expect(page.locator('.toolbar')).toBeVisible();
}

const linha = (page: Page, codigo: string) =>
  page.locator('.table tbody tr').filter({ hasText: codigo });

test('E2E-01 abre a listagem como professor com as colunas e o rodapé de RF-18', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, professor);
  const questao = await criarQuestao(request, token);

  await abrirLista(page, professor);

  await expect(page.locator('.table')).toBeVisible();
  await expect(page.locator('.table thead th')).toHaveText([
    'Cód.',
    'Enunciado',
    'Disciplina',
    'Banca / Ano',
    'Tipo',
    'Status',
    'Ações',
  ]);
  await expect(linha(page, questao.codigo)).toContainText('Rascunho');
  await expect(page.locator('.pagination')).toContainText('questões');
});

test('E2E-02 busca por texto e filtra por disciplina, banca e status', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, professor);
  const refs = await referencias(request, token);
  const alvo = await criarQuestao(request, token, {
    enunciado: `Questão sobre desapropriação ${Date.now()}.`,
  });
  const ruido = await criarQuestao(request, token, {
    enunciado: `Questão sobre concordância verbal ${Date.now()}.`,
  });

  await abrirLista(page, professor);
  await page.getByLabel('Buscar questões').fill('desapropriação');
  await page.getByLabel('Disciplina').selectOption({ label: refs.disciplinaNome });
  await page.getByLabel('Status').selectOption('RASCUNHO');
  await page.getByRole('button', { name: 'Filtrar' }).click();

  await expect(page).toHaveURL(/q=desapropria/);
  await expect(linha(page, alvo.codigo)).toBeVisible();
  await expect(linha(page, ruido.codigo)).toHaveCount(0);
});

test('E2E-03 busca por código, abre para edição e salva', async ({ page, request }) => {
  const token = await autenticar(request, professor);
  const questao = await criarQuestao(request, token);

  await abrirLista(page, professor);
  await page.getByLabel('Buscar questões').fill(questao.codigo);
  await page.getByRole('button', { name: 'Filtrar' }).click();
  await expect(linha(page, questao.codigo)).toBeVisible();
  await linha(page, questao.codigo).getByTitle('Editar').click();

  await expect(page).toHaveURL(new RegExp(`/admin/questoes/${questao.id}`));
  await expect(page.getByRole('heading', { name: 'Editar questão' })).toBeVisible();
  await expect(page.locator('#enunciado')).toHaveValue(questao.enunciado);

  await page.locator('#enunciado').fill('Enunciado corrigido pelo professor durante a revisão.');
  await page.getByRole('button', { name: 'Salvar rascunho' }).click();

  await expect(page).toHaveURL(/\/admin\/questoes$/);
  await expect(linha(page, questao.codigo)).toContainText('Enunciado corrigido pelo professor');
});

test('E2E-04 volta do formulário para a lista no mesmo ponto', async ({ page, request }) => {
  const token = await autenticar(request, professor);
  await criarQuestao(request, token, { enunciado: `Questão sobre improbidade ${Date.now()}.` });

  await abrirLista(page, professor);
  await page.getByLabel('Buscar questões').fill('improbidade');
  await page.getByRole('button', { name: 'Filtrar' }).click();
  await expect(page).toHaveURL(/q=improbidade/);
  const origem = page.url();

  await page.locator('.table tbody tr').first().getByTitle('Editar').click();
  await expect(page.getByRole('heading', { name: 'Editar questão' })).toBeVisible();
  await page.goBack();

  await expect(page).toHaveURL(origem);
  await expect(page.getByLabel('Buscar questões')).toHaveValue('improbidade');
});

test('E2E-05 cadastra múltipla escolha com cinco alternativas', async ({ page, request }) => {
  const token = await autenticar(request, professor);
  const refs = await referencias(request, token);

  await login(page, professor);
  await page.goto('/admin/questoes/nova');
  await page
    .locator('#enunciado')
    .fill('Assinale a alternativa correta sobre o regime de licitações.');
  await page.locator('#disciplina').selectOption({ label: refs.disciplinaNome });
  for (let i = 0; i < 3; i++)
    await page.getByRole('button', { name: 'Adicionar alternativa' }).click();

  await expect(page.locator('.alternativa')).toHaveCount(5);
  await expect(page.getByRole('button', { name: 'Adicionar alternativa' })).toBeDisabled();

  for (let i = 0; i < 5; i++)
    await page.locator('.alternativa input').nth(i).fill(`Alternativa ${'ABCDE'[i]}`);
  await page.locator('.gabaritos label.radio').first().click();
  await page.getByRole('button', { name: 'Salvar rascunho' }).click();

  await expect(page).toHaveURL(/\/admin\/questoes$/);
  await expect(page.locator('.table tbody tr').first()).toContainText('Rascunho');
});

test('E2E-06 troca o tipo para Certo/Errado e substitui o bloco de alternativas', async ({
  page,
}) => {
  page.on('dialog', (dialogo) => dialogo.accept());

  await login(page, professor);
  await page.goto('/admin/questoes/nova');
  await page.locator('#enunciado').fill('Julgue o item a seguir sobre licitações e contratos.');
  await expect(page.locator('.alternativa')).toHaveCount(2);

  await page.getByText('Certo/Errado', { exact: true }).click();

  await expect(page.locator('.alternativa')).toHaveCount(0);
  await expect(page.getByText('Certo', { exact: true })).toBeVisible();
  await expect(page.getByText('Errado', { exact: true })).toBeVisible();
});

test('E2E-07 salva rascunho apenas com o enunciado', async ({ page }) => {
  const enunciado = `Rascunho gravado somente com o enunciado ${Date.now()}.`;

  await login(page, professor);
  await page.goto('/admin/questoes/nova');
  await page.locator('#enunciado').fill(enunciado);
  await page.getByRole('button', { name: 'Salvar rascunho' }).click();

  await expect(page).toHaveURL(/\/admin\/questoes$/);
  await expect(page.locator('.table tbody tr').first()).toContainText('Rascunho');
});

test('E2E-08 recusa a publicação incompleta com as pendências inline e sem perder o que foi digitado', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, administrador);
  const questao = await criarQuestao(request, token, { alternativas: [] });
  const digitado = 'Enunciado ajustado na tela antes de tentar publicar.';

  await login(page, administrador);
  await page.goto(`/admin/questoes/${questao.id}`);
  await expect(page.locator('#enunciado')).toHaveValue(questao.enunciado);
  await page.locator('#enunciado').fill(digitado);
  await page.getByRole('button', { name: 'Publicar' }).click();

  const pendencias = page.locator('.erro-box');
  await expect(pendencias).toContainText('Não é possível publicar ainda.');
  await expect(pendencias).toContainText('Marque uma opção válida antes de publicar.');
  await expect(pendencias).toContainText('O comentário do professor é obrigatório para publicar.');
  await expect(pendencias).toContainText('Informe ao menos duas alternativas.');
  await expect(page.locator('#enunciado')).toHaveValue(digitado);
});

test('E2E-09 publica como administrador e a lista passa a exibir Publicada', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, administrador);
  const questao = await criarQuestao(request, token, questaoCompleta);

  await login(page, administrador);
  await page.goto(`/admin/questoes/${questao.id}`);
  await expect(page.locator('#enunciado')).toHaveValue(questao.enunciado);
  await page.getByRole('button', { name: 'Publicar' }).click();
  await expect(page.getByRole('button', { name: 'Publicar' })).toHaveCount(0);

  await page.goto(`/admin/questoes?q=${questao.codigo}`);
  await expect(linha(page, questao.codigo)).toContainText('Publicada');
});

test('E2E-10 esconde publicar do professor na tela e a API recusa com 403', async ({
  page,
  request,
}) => {
  const tokenAdmin = await autenticar(request, administrador);
  const questao = await criarQuestao(request, tokenAdmin, questaoCompleta);
  const tokenProfessor = await autenticar(request, professor);

  await login(page, professor);
  await page.goto(`/admin/questoes/${questao.id}`);
  await expect(page.getByRole('heading', { name: 'Editar questão' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Publicar' })).toHaveCount(0);

  await page.goto(`/admin/questoes?q=${questao.codigo}`);
  await expect(linha(page, questao.codigo)).toBeVisible();
  await expect(linha(page, questao.codigo).getByTitle('Publicar')).toHaveCount(0);

  const direta = await request.post(`${apiBase}/admin/questoes/${questao.id}/publicar`, {
    headers: cabecalho(tokenProfessor),
  });
  expect(direta.status()).toBe(403);
});

test('E2E-11 despublica com confirmação explícita', async ({ page, request }) => {
  const token = await autenticar(request, administrador);
  const questao = await criarQuestao(request, token, questaoCompleta);
  expect(
    (
      await request.post(`${apiBase}/admin/questoes/${questao.id}/publicar`, {
        headers: cabecalho(token),
      })
    ).status(),
  ).toBe(200);

  await abrirLista(page, administrador, `?q=${questao.codigo}`);
  await expect(linha(page, questao.codigo)).toContainText('Publicada');
  await linha(page, questao.codigo).getByTitle('Despublicar').click();

  const dialogo = page.locator('.dialog');
  await expect(dialogo).toContainText('Despublicar questão');
  await dialogo.getByRole('button', { name: 'Despublicar' }).click();

  await expect(dialogo).toHaveCount(0);
  await expect(linha(page, questao.codigo)).toContainText('Em revisão');
});

test('E2E-12 abre a árvore de matérias com as contagens de CA-10', async ({ page, request }) => {
  const token = await autenticar(request, professor);
  await criarQuestao(request, token);

  await login(page, professor);
  await page.goto('/admin/materias');

  await expect(page.getByRole('heading', { name: 'Disciplinas e assuntos' })).toBeVisible();
  await expect(page.locator('.disciplina').first()).toContainText('questões');
  await expect(page.locator('.assunto').first()).toBeVisible();
});

test('E2E-13 recusa excluir assunto vinculado e oferece arquivar', async ({ page, request }) => {
  const token = await autenticar(request, professor);
  const refs = await referencias(request, token);
  await criarQuestao(request, token);

  await login(page, professor);
  await page.goto('/admin/materias');
  const assunto = page.locator('.assunto').filter({ hasText: refs.assuntoNome }).first();
  await assunto.getByTitle('Excluir assunto').click();
  await page.locator('.dialog').getByRole('button', { name: 'Confirmar' }).click();

  const recusa = page.locator('.dialog');
  await expect(recusa).toContainText('questões vinculadas');
  await expect(recusa).toContainText('Arquive-o para retirá-lo da árvore.');

  await recusa.getByRole('button', { name: 'Arquivar' }).click();

  await expect(page.locator('.dialog')).toHaveCount(0);
  await expect(page.locator('.assunto').filter({ hasText: refs.assuntoNome })).toHaveCount(0);
});

test('E2E-14 renomeia assunto e a árvore passa a exibir o novo nome', async ({ page, request }) => {
  const token = await autenticar(request, professor);
  const refs = await referencias(request, token);
  const novo = `Assunto renomeado ${Date.now()}`;

  await login(page, professor);
  await page.goto('/admin/materias');
  await page
    .locator('.assunto')
    .filter({ hasText: refs.assuntoNome })
    .first()
    .getByTitle('Editar assunto')
    .click();
  await page.getByLabel('Nome').fill(novo);
  await page.locator('.dialog').getByRole('button', { name: 'Salvar' }).click();

  await expect(page.locator('.dialog')).toHaveCount(0);
  await expect(page.locator('.assunto').filter({ hasText: novo })).toBeVisible();
  await expect(page.locator('.assunto').filter({ hasText: refs.assuntoNome })).toHaveCount(0);
});

test('E2E-15 percorre as quatro telas por teclado, com foco visível e sem violações de contraste', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, administrador);
  const questao = await criarQuestao(request, token, questaoCompleta);
  const rotas = [
    '/admin/questoes',
    `/admin/questoes/${questao.id}`,
    '/admin/materias',
    '/admin/catalogo',
  ];

  await login(page, administrador);
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

    await page.keyboard.press('Tab');
    const focado = page.locator(':focus');
    await expect(focado, `nada recebeu foco em ${rota}`).toBeVisible();
    const contorno = await focado.evaluate((el) => {
      const estilo = getComputedStyle(el);
      return `${estilo.outlineStyle}|${estilo.outlineWidth}|${estilo.boxShadow}`;
    });
    expect(contorno, `foco sem indicação visível em ${rota}`).not.toBe('none|0px|none');
  }

  await page.goto('/admin/questoes');
  await page.locator('.table tbody tr').first().getByTitle('Editar').focus();
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/admin\/questoes\/[0-9a-f-]{36}$/);
});

test('E2E-16 cadastra banca pelo catálogo e recusa excluir item vinculado', async ({
  page,
  request,
}) => {
  const token = await autenticar(request, administrador);
  const nome = `Banca E2E ${Date.now()}`;

  await login(page, administrador);
  await page.goto('/admin/catalogo');
  await expect(page.getByRole('heading', { name: 'Bancas e órgãos' })).toBeVisible();
  await page.getByRole('button', { name: 'Novo item' }).click();
  await page.getByLabel('Nome').fill(nome);
  await page.getByLabel('Sigla').fill('E2E');
  await page.locator('.dialog').getByRole('button', { name: 'Salvar' }).click();

  const item = page.locator('.table tbody tr').filter({ hasText: nome });
  await expect(item).toBeVisible();

  const bancas = await (
    await request.get(`${apiBase}/admin/bancas`, { headers: cabecalho(token) })
  ).json();
  const criada = bancas.find((b: { nome: string }) => b.nome === nome);
  expect(criada, 'banca criada pela tela não voltou na API').toBeTruthy();
  await criarQuestao(request, token, { bancaId: criada.id });

  await item.getByTitle('Excluir').click();
  await page.locator('.dialog').getByRole('button', { name: 'Excluir' }).click();

  await expect(page.locator('.erro')).toContainText('questões vinculadas');
  await expect(item).toBeVisible();
});
