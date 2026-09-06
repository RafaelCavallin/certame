import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const estudante = 'estudante@dev.certame.local';
const senha = 'senhaDev123';

async function login(page: Page): Promise<void> {
  test.setTimeout(60_000);
  await page.goto('/login');
  await expect(page.getByLabel('E-mail')).toBeVisible({ timeout: 60_000 });
  await page.getByLabel('E-mail').fill(estudante);
  await page.getByLabel('Senha').fill(senha);
  const resposta = page.waitForResponse((item) => item.url().includes('/auth/login'));
  await page.getByRole('button', { name: 'Entrar' }).click();
  expect((await resposta).status()).toBe(200);
  await expect(page).toHaveURL(/\/inicio$/);
}

async function iniciarEstudo(page: Page, modo = 'FOCO'): Promise<void> {
  await login(page);
  await page.goto(`/estudar?modo=${modo}`);
  await expect(page.locator('.questao')).toBeVisible();
}

async function abrirFlashcards(page: Page): Promise<void> {
  await login(page);
  await page.goto('/flashcards');
  await expect(page.locator('.table')).toBeVisible();
}

function botaoNovoFlashcard(page: Page) {
  return page.getByRole('button', { name: 'Novo flashcard' }).first();
}

function paraSegundos(mmss: string): number {
  const [minutos, segundos] = mmss.split(':').map(Number);
  return minutos * 60 + segundos;
}

test('E2E-01 abre o modal a partir da questão nos três modos', async ({ page }) => {
  await login(page);
  for (const modo of ['FOCO', 'CONTEXTO', 'PROVA']) {
    await page.goto(`/estudar?modo=${modo}`);
    await expect(page.locator('.questao')).toBeVisible();
    const url = page.url();
    await botaoNovoFlashcard(page).click();
    const dialog = page.locator('.dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.locator('.dialog-title')).toContainText('Novo flashcard');
    expect(page.url()).toBe(url);
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0);
  }
});

test('E2E-02 abre e fecha o modal preservando a questão, a alternativa marcada e o cronômetro', async ({
  page,
}) => {
  // Adaptação: a série padrão desta suíte tem 10 questões (não 20+), então o cenário confere a
  // preservação numa questão intermediária, e não literalmente na "questão 12" do techspec.
  await iniciarEstudo(page);
  await page.getByRole('button', { name: 'Próxima' }).click();
  await page.getByRole('button', { name: 'Próxima' }).click();
  await expect(page.locator('.navegacao-serie button.ativa')).toHaveText('3');
  const codigo = await page.locator('.codigo').innerText();
  await page.getByRole('radio').first().click();
  await expect(page.getByRole('radio').first()).toHaveAttribute('aria-checked', 'true');
  await page.waitForTimeout(1100);
  const cronometroAntes = paraSegundos(
    await page.locator('main.conteudo > header span').innerText(),
  );

  await botaoNovoFlashcard(page).click();
  await expect(page.locator('.dialog')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.locator('.dialog')).toHaveCount(0);

  await expect(page.locator('.codigo')).toHaveText(codigo);
  await expect(page.getByRole('radio').first()).toHaveAttribute('aria-checked', 'true');
  await page.waitForTimeout(1100);
  const cronometroDepois = paraSegundos(
    await page.locator('main.conteudo > header span').innerText(),
  );
  expect(cronometroDepois).toBeGreaterThan(cronometroAntes);
});

test('E2E-03 confere o pré-preenchimento do modal', async ({ page }) => {
  await login(page);
  const respostaSessao = page.waitForResponse(
    (r) => r.url().includes('/estudo/sessoes') && r.request().method() === 'POST',
  );
  await page.goto('/estudar?modo=FOCO');
  await expect(page.locator('.questao')).toBeVisible();
  const sessao = await (await respostaSessao).json();
  const questaoAtual = sessao.questoes[sessao.indiceAtual];

  await botaoNovoFlashcard(page).click();
  const dialog = page.locator('.dialog');
  await expect(dialog).toContainText('Vinculado à questão em tela');
  await expect(dialog).toContainText(questaoAtual.codigo);
  const camposLeitura = dialog.locator('input[disabled]');
  await expect(camposLeitura.nth(0)).toHaveValue(questaoAtual.disciplina ?? '—');
  await expect(camposLeitura.nth(1)).toHaveValue(questaoAtual.assunto ?? '—');
});

test('E2E-04 recusa salvar com frente de 2 caracteres', async ({ page }) => {
  await iniciarEstudo(page);
  await botaoNovoFlashcard(page).click();
  const dialog = page.locator('.dialog');
  await dialog.locator('#fc-frente').fill('Ab');
  await dialog.locator('#fc-verso').fill('Verso digitado para o teste E2E-04');
  await dialog.getByRole('button', { name: 'Salvar e fechar' }).click();
  await expect(dialog).toBeVisible();
  await expect(dialog.locator('.erro-campo')).toContainText('3 caracteres');
  await expect(dialog.locator('#fc-verso')).toHaveValue('Verso digitado para o teste E2E-04');
});

test('E2E-05 salva e fecha devolvendo o foco ao enunciado', async ({ page }) => {
  await iniciarEstudo(page);
  await botaoNovoFlashcard(page).click();
  const dialog = page.locator('.dialog');
  await dialog.locator('#fc-frente').fill('Pergunta de teste E2E-05');
  await dialog.locator('#fc-verso').fill('Resposta de teste E2E-05');
  await dialog.getByRole('button', { name: 'Salvar e fechar' }).click();
  await expect(dialog).toHaveCount(0);
  await expect(page.locator('.toast')).toContainText('Flashcard criado');
  await expect(page.locator('#enunciado')).toBeFocused();
});

test('E2E-06 salva e cria outro, duas vezes seguidas, mantendo deck e taxonomia', async ({
  page,
}) => {
  await iniciarEstudo(page);
  await botaoNovoFlashcard(page).click();
  const dialog = page.locator('.dialog');
  await dialog.locator('#fc-deck').selectOption({ label: 'Constitucional' });
  await dialog.locator('#fc-frente').fill('Primeira pergunta E2E-06');
  await dialog.locator('#fc-verso').fill('Primeira resposta E2E-06');
  await dialog.getByRole('button', { name: 'Salvar e criar outro' }).click();
  await expect(dialog).toBeVisible();
  await expect(dialog.locator('#fc-frente')).toHaveValue('');
  await expect(dialog.locator('#fc-verso')).toHaveValue('');
  await expect(dialog.locator('#fc-deck')).not.toHaveValue('');

  await dialog.locator('#fc-frente').fill('Segunda pergunta E2E-06');
  await dialog.locator('#fc-verso').fill('Segunda resposta E2E-06');
  await dialog.getByRole('button', { name: 'Salvar e criar outro' }).click();
  await expect(dialog).toBeVisible();
  await expect(dialog.locator('#fc-frente')).toHaveValue('');
  await expect(dialog.locator('#fc-deck')).not.toHaveValue('');
});

test('E2E-07 exige confirmação ao pressionar Esc com o formulário preenchido', async ({ page }) => {
  await iniciarEstudo(page);
  await botaoNovoFlashcard(page).click();
  const dialog = page.locator('.dialog');
  await dialog.locator('#fc-frente').fill('Pergunta digitada E2E-07');
  await page.keyboard.press('Escape');
  await expect(dialog.getByText('Descartar este flashcard?')).toBeVisible();

  await dialog.getByRole('button', { name: 'Continuar editando' }).click();
  await expect(dialog.locator('#fc-frente')).toHaveValue('Pergunta digitada E2E-07');

  await page.keyboard.press('Escape');
  await dialog.getByRole('button', { name: 'Descartar' }).click();
  await expect(dialog).toHaveCount(0);
});

test('E2E-08 mantém o contador de flashcards criados na sessão após recarregar', async ({
  page,
}) => {
  await iniciarEstudo(page);
  const botao = botaoNovoFlashcard(page);
  for (let i = 1; i <= 3; i++) {
    await botao.click();
    const dialog = page.locator('.dialog');
    await dialog.locator('#fc-frente').fill(`Pergunta ${i} da sessão E2E-08`);
    await dialog.locator('#fc-verso').fill(`Resposta ${i} da sessão E2E-08`);
    await dialog.getByRole('button', { name: 'Salvar e fechar' }).click();
    await expect(dialog).toHaveCount(0);
  }
  await expect(botao.locator('.tag')).toHaveText('3');

  await page.getByRole('radio').first().click();
  await page.getByRole('button', { name: /Responder/ }).click();
  await page.goto('/inicio');
  await page.getByRole('button', { name: 'Continuar' }).click();
  await expect(page).toHaveURL(/\/estudar\?sessao=/);
  await expect(botaoNovoFlashcard(page).locator('.tag')).toHaveText('3');

  await page.reload();
  await expect(page.locator('.questao')).toBeVisible();
  await expect(botaoNovoFlashcard(page).locator('.tag')).toHaveText('3');
});

test('E2E-09 abre Flashcards pelo menu com lista paginada e indicadores', async ({ page }) => {
  await login(page);
  await page.getByRole('link', { name: 'Flashcards' }).click();
  await expect(page).toHaveURL(/\/flashcards$/);
  await expect(page.locator('.table')).toBeVisible();
  await expect(page.locator('.card-kicker', { hasText: 'Vencendo hoje' })).toBeVisible();
  await expect(page.locator('.card-kicker', { hasText: 'Próximos 7 dias' })).toBeVisible();
  await expect(page.locator('.card-kicker', { hasText: 'Em fixação' })).toBeVisible();
  await expect(page.locator('.card-kicker', { hasText: 'Total de cards' })).toBeVisible();
});

test('E2E-10 combina busca, filtros e ordenação', async ({ page }) => {
  await abrirFlashcards(page);
  await page.getByLabel('Deck').selectOption({ label: 'Administrativo' });
  // Conta só os cards da seed desta funcionalidade: o deck Administrativo também recebe
  // "Card de sessao finalizada de hoje" da seed de prd-flashcards-decks-e-revisao (V909),
  // então o total do deck não é mais 4 — só os cards de "legalidade" continuam sendo.
  await expect(page.locator('.table tbody tr', { hasText: 'Principio da legalidade' })).toHaveCount(
    4,
  );

  await page.getByPlaceholder('Buscar na frente ou no verso do card…').fill('legalidade');
  await expect(page.locator('.table tbody tr')).toHaveCount(4);

  const resposta = page.waitForResponse(
    (r) => r.url().includes('/flashcards') && r.url().includes('sort=proximaRevisao'),
  );
  await page.getByLabel('Ordenar').selectOption({ label: 'Próxima revisão: mais perto' });
  expect((await resposta).status()).toBe(200);

  await page
    .getByPlaceholder('Buscar na frente ou no verso do card…')
    .fill('termo-sem-correspondencia-xyz');
  await expect(page.getByText('Nenhum flashcard com esses filtros')).toBeVisible();
});

test('E2E-11 arquiva um card e o restaura', async ({ page }) => {
  await abrirFlashcards(page);
  await page.getByLabel('Deck').selectOption({ label: 'Constitucional' });
  const linha = page.locator('.table tbody tr', {
    hasText: 'Efeitos da decisao de merito em ADI 1',
  });
  await linha.locator('button[title="Arquivar"]').click();
  await expect(linha).toHaveCount(0);

  await page.getByRole('button', { name: 'Arquivados' }).click();
  const linhaArquivada = page.locator('.table tbody tr', {
    hasText: 'Efeitos da decisao de merito em ADI 1',
  });
  await expect(linhaArquivada).toBeVisible();
  await linhaArquivada.locator('button[title="Restaurar"]').click();
  await expect(linhaArquivada).toHaveCount(0);

  await page.getByRole('button', { name: 'Ativos' }).click();
  await expect(
    page.locator('.table tbody tr', { hasText: 'Efeitos da decisao de merito em ADI 1' }),
  ).toBeVisible();
});

test('E2E-12 exclui um card já revisado após confirmação', async ({ page }) => {
  await abrirFlashcards(page);
  // Busca pelo texto: a seed de prd-flashcards-decks-e-revisao (V909) acrescentou 14 cards
  // mais recentes ao acervo do estudante, empurrando este card (da seed V908) para além da
  // primeira página de 20 (ordenação padrão por criadoEm desc). A busca isola o card
  // independentemente da paginação, e o filtro (refletido em query params) sobrevive ao reload.
  await page.getByPlaceholder('Buscar na frente ou no verso do card…').fill('historico de revisao');
  const linha = page.locator('.table tbody tr', { hasText: 'Card com historico de revisao' });
  await expect(linha).toBeVisible();
  await linha.locator('button[title="Excluir"]').click();
  await expect(page.getByRole('heading', { name: 'Excluir flashcard' })).toBeVisible();
  await page.locator('.dialog-actions').getByRole('button', { name: 'Excluir' }).click();

  await expect(
    page.locator('.table tbody tr', { hasText: 'Card com historico de revisao' }),
  ).toHaveCount(0);
  await page.reload();
  await expect(
    page.locator('.table tbody tr', { hasText: 'Card com historico de revisao' }),
  ).toHaveCount(0);
});

test('E2E-13 abre a questão de origem a partir da lista', async () => {
  test.skip(
    true,
    'CA-13 depende da rota /estudar?questao={id} de prd-sessao-de-estudo, que abre uma sessão fixada numa questão específica. Essa capacidade ainda não existe em F2 — a lista já entrega o link desabilitado com o rótulo explicativo (origem.questaoId), conforme o fallback documentado em "Pontos de integração" da TechSpec. Retenção registrada, não contornada.',
  );
});

test('E2E-14 cria um card avulso pelo menu Flashcards', async ({ page }) => {
  await abrirFlashcards(page);
  await botaoNovoFlashcard(page).click();
  const dialog = page.locator('.dialog');
  await expect(dialog).not.toContainText('Vinculado à questão em tela');
  const disciplinaSelect = dialog.locator('#fc-disciplina');
  const assuntoSelect = dialog.locator('#fc-assunto');
  const totalDisciplinas = await disciplinaSelect.locator('option').count();
  let assuntoEscolhido = '';
  for (let i = 1; i < totalDisciplinas && !assuntoEscolhido; i++) {
    await disciplinaSelect.selectOption({ index: i });
    const opcoesAssunto = await assuntoSelect.locator('option').allTextContents();
    if (opcoesAssunto.length > 1) assuntoEscolhido = opcoesAssunto[1];
  }
  expect(assuntoEscolhido).not.toBe('');
  await assuntoSelect.selectOption({ label: assuntoEscolhido });
  await dialog.locator('#fc-frente').fill('Pergunta avulsa E2E-14');
  await dialog.locator('#fc-verso').fill('Resposta avulsa E2E-14');
  await dialog.getByRole('button', { name: 'Salvar e fechar' }).click();
  await expect(dialog).toHaveCount(0);
  await expect(page.locator('.table')).toContainText('Pergunta avulsa E2E-14');
});

test('E2E-15 move uma seleção de cards para outro deck preservando a próxima revisão', async ({
  page,
}) => {
  await abrirFlashcards(page);
  await page.getByLabel('Deck').selectOption({ label: 'Administrativo' });
  const linha7 = page.locator('.table tbody tr', {
    hasText: 'Principio da legalidade na Administracao 7',
  });
  const linha8 = page.locator('.table tbody tr', {
    hasText: 'Principio da legalidade na Administracao 8',
  });
  const revisao7Antes = await linha7.locator('td').nth(5).innerText();

  await linha7.locator('input[type=checkbox]').check();
  await linha8.locator('input[type=checkbox]').check();
  await expect(page.getByText('2 selecionado(s)')).toBeVisible();
  await page.getByLabel('Mover para deck').selectOption({ label: 'Constitucional' });
  await page.getByRole('button', { name: 'Mover para deck' }).click();
  await expect(page.getByText('2 selecionado(s)')).toHaveCount(0);

  await page.getByLabel('Deck').selectOption({ label: 'Constitucional' });
  const linha7Depois = page.locator('.table tbody tr', {
    hasText: 'Principio da legalidade na Administracao 7',
  });
  await expect(linha7Depois).toBeVisible();
  await expect(linha7Depois.locator('td').nth(5)).toHaveText(revisao7Antes);
});

test('E2E-16 percorre por teclado com foco visível e confina o Tab no modal', async ({ page }) => {
  await abrirFlashcards(page);
  const analiseLista = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  expect(analiseLista.violations.map((item) => item.id)).toEqual([]);

  await botaoNovoFlashcard(page).click();
  const dialog = page.locator('.dialog');
  await expect(dialog).toBeVisible();
  await expect(dialog).toHaveCSS('opacity', '1'); // aguarda a animação cf-pop terminar antes de medir contraste
  const analiseDialogo = await new AxeBuilder({ page })
    .include('.dialog')
    .withTags(['wcag2a', 'wcag2aa'])
    .analyze();
  expect(analiseDialogo.violations.map((item) => item.id)).toEqual([]);

  const focaveis = dialog.locator(
    'button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled])',
  );
  const primeiro = focaveis.first();
  await expect(primeiro).toBeFocused();
  const total = await focaveis.count();
  for (let i = 0; i < total; i++) await page.keyboard.press('Tab');
  await expect(primeiro).toBeFocused();

  await page.keyboard.press('Escape');
  await expect(dialog).toHaveCount(0);
});
