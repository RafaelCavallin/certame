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

async function abrirDecks(page: Page): Promise<void> {
  await page.goto('/flashcards/decks');
  await expect(page.getByRole('button', { name: 'Novo deck' })).toBeVisible();
}

function nomeUnico(prefixo: string): string {
  return `${prefixo} ${Date.now()}-${Math.floor(Math.random() * 10_000)}`;
}

async function criarDeck(
  page: Page,
  nome: string,
  opcoes: { revisoesDia?: number } = {},
): Promise<void> {
  await abrirDecks(page);
  await page.getByRole('button', { name: 'Novo deck' }).click();
  const dialog = page.locator('.dialog');
  await dialog.locator('#deck-nome').fill(nome);
  if (opcoes.revisoesDia !== undefined) {
    await dialog.locator('#deck-revisoes').fill(String(opcoes.revisoesDia));
    await expect(dialog.locator('#deck-revisoes')).toHaveValue(String(opcoes.revisoesDia));
  }
  const resposta = page.waitForResponse(
    (r) => r.url().includes('/flashcards/decks') && r.request().method() === 'POST',
  );
  await dialog.getByRole('button', { name: 'Salvar deck' }).click();
  await resposta;
  await expect(dialog).toHaveCount(0);
}

function deckCard(page: Page, nome: string) {
  return page.locator('app-deck-card', { hasText: nome });
}

async function criarCardAvulso(
  page: Page,
  deckNome: string | null,
  frente: string,
  verso: string,
): Promise<void> {
  await page.goto('/flashcards');
  await page.getByRole('button', { name: 'Novo flashcard' }).first().click();
  const dialog = page.locator('.dialog');
  if (deckNome) {
    await dialog.locator('#fc-deck').selectOption({ label: deckNome });
  }
  await dialog.locator('#fc-frente').fill(frente);
  await dialog.locator('#fc-verso').fill(verso);
  await dialog.getByRole('button', { name: 'Salvar e fechar' }).click();
  await expect(dialog).toHaveCount(0);
}

async function estudarDeck(page: Page, deckNome: string): Promise<void> {
  await abrirDecks(page);
  const resposta = page.waitForResponse(
    (r) => r.url().includes('/flashcards/sessoes') && r.request().method() === 'POST',
  );
  await deckCard(page, deckNome).getByRole('button', { name: 'Estudar este deck' }).click();
  await resposta;
  await expect(page).toHaveURL(/\/flashcards\/estudar\?deck=/);
}

async function resumoInicio(page: Page): Promise<{
  tempoEstudoSegundos: number;
  tempoQuestoesSegundos: number;
  tempoFlashcardsSegundos: number;
}> {
  const rota = '**/api/v1/estudo/inicio';
  let capturado: {
    tempoEstudoSegundos: number;
    tempoQuestoesSegundos: number;
    tempoFlashcardsSegundos: number;
  } | null = null;
  // Lê o corpo dentro do handler da rota (em vez de correr atrás do evento de resposta): a navegação
  // do SPA pode invalidar o recurso antes que `waitForResponse(...).json()` consiga lê-lo.
  await page.route(rota, async (route) => {
    const resposta = await route.fetch();
    capturado = await resposta.json();
    await route.fulfill({ response: resposta });
  });
  await page.locator('nav a[routerLink="/inicio"]').click();
  await expect.poll(() => capturado).not.toBeNull();
  await page.unroute(rota);
  return capturado!;
}

test('E2E-01 abre a sessão mostrando só a frente', async ({ page }) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-01');
  await criarDeck(page, nome);
  await criarCardAvulso(page, nome, 'Frente do card E2E-01', 'Verso secreto do card E2E-01');
  await estudarDeck(page, nome);

  await expect(page.getByText('Frente do card E2E-01')).toBeVisible();
  await expect(page.getByText('Verso secreto do card E2E-01')).toHaveCount(0);
  await expect(page.getByRole('button', { name: /Errei/ })).toHaveCount(0);
});

test('E2E-02 a barra de espaço revela o verso e as classificações', async ({ page }) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-02');
  await criarDeck(page, nome);
  await criarCardAvulso(page, nome, 'Frente do card E2E-02', 'Verso do card E2E-02');
  await estudarDeck(page, nome);

  await expect(page.getByText('Verso do card E2E-02')).toHaveCount(0);
  await page.keyboard.press('Space');
  await expect(page.getByText('Verso do card E2E-02')).toBeVisible();
  await expect(page.getByRole('button', { name: /Errei/ })).toBeVisible();
  await expect(page.getByRole('button', { name: /Difícil/ })).toBeVisible();
  await expect(page.getByRole('button', { name: /Bom/ })).toBeVisible();
  await expect(page.getByRole('button', { name: /Fácil/ })).toBeVisible();
});

test('E2E-03 cada classificação exibe o intervalo que agendaria, e o escolhido é o gravado', async ({
  page,
}) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-03');
  await criarDeck(page, nome);
  await criarCardAvulso(page, nome, 'Frente do card E2E-03', 'Verso do card E2E-03');
  await estudarDeck(page, nome);
  await page.keyboard.press('Space');

  const botaoBom = page.getByRole('button', { name: /Bom/ });
  for (const rotulo of [/Errei/, /Difícil/, /Bom/, /Fácil/]) {
    await expect(page.getByRole('button', { name: rotulo }).locator('span').nth(1)).not.toHaveText(
      '',
    );
  }
  const intervaloExibido = await botaoBom.locator('span').nth(1).innerText();

  const resposta = page.waitForResponse(
    (r) => r.url().includes('/revisoes') && r.request().method() === 'POST',
  );
  await botaoBom.click();
  const corpo = await (await resposta).json();
  expect(corpo.intervaloRotulo).toBe(intervaloExibido);
});

test('E2E-04 o verso mostra estabilidade, dificuldade e retenção depois do primeiro contato', async ({
  page,
}) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-04');
  await criarDeck(page, nome);
  await criarCardAvulso(page, nome, 'Frente do card E2E-04', 'Verso do card E2E-04');
  await estudarDeck(page, nome);

  await page.keyboard.press('Space');
  await expect(page.getByText('Primeiro contato com este card')).toBeVisible();
  const respostaErrei = page.waitForResponse(
    (r) => r.url().includes('/revisoes') && r.request().method() === 'POST',
  );
  await page.keyboard.press('1'); // Errei -> reaprendizado de curto prazo reenfileira o card
  await respostaErrei;

  await expect(page.getByRole('button', { name: 'Mostrar resposta' })).toBeVisible();
  await page.keyboard.press('Space');
  const memoria = page.locator('span', { hasText: 'Estabilidade' });
  await expect(memoria).toBeVisible();
  await expect(memoria).toContainText('dificuldade');
  await expect(memoria).toContainText('retenção hoje');
});

test('E2E-05 as teclas 1 a 4 classificam com a nota correspondente', async ({ page }) => {
  await login(page);
  const casos: Array<{ tecla: string; nota: string }> = [
    { tecla: '1', nota: 'ERREI' },
    { tecla: '2', nota: 'DIFICIL' },
    { tecla: '3', nota: 'BOM' },
    { tecla: '4', nota: 'FACIL' },
  ];
  for (const caso of casos) {
    const nome = nomeUnico(`Deck E2E-05 ${caso.tecla}`);
    await criarDeck(page, nome);
    await criarCardAvulso(
      page,
      nome,
      `Frente E2E-05 tecla ${caso.tecla}`,
      `Verso E2E-05 tecla ${caso.tecla}`,
    );
    await estudarDeck(page, nome);
    await page.keyboard.press('Space');
    await expect(page.getByRole('button', { name: /Errei/ })).toBeVisible();

    const resposta = page.waitForResponse(
      (r) => r.url().includes('/revisoes') && r.request().method() === 'POST',
    );
    await page.keyboard.press(caso.tecla);
    const requisicao = (await resposta).request().postDataJSON();
    expect(requisicao.nota).toBe(caso.nota);
  }
});

test('E2E-06 mostra o resumo ao fim da sessão', async ({ page }) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-06');
  await criarDeck(page, nome);
  await criarCardAvulso(page, nome, 'Frente 1 E2E-06', 'Verso 1 E2E-06');
  await criarCardAvulso(page, nome, 'Frente 2 E2E-06', 'Verso 2 E2E-06');
  await estudarDeck(page, nome);

  // Fácil graduado direto na primeira passagem (sem reenfileirar), o que garante que os dois
  // cards se esgotam e a sessão finaliza sozinha.
  await page.keyboard.press('Space');
  await expect(page.getByRole('button', { name: /Fácil/ })).toBeVisible();
  const primeiraResposta = page.waitForResponse(
    (r) => r.url().includes('/revisoes') && r.request().method() === 'POST',
  );
  await page.keyboard.press('4');
  await primeiraResposta;

  await expect(page.getByRole('button', { name: 'Mostrar resposta' })).toBeVisible();
  await page.keyboard.press('Space');
  await expect(page.getByRole('button', { name: /Fácil/ })).toBeVisible();
  await page.keyboard.press('4');

  await expect(page.getByRole('heading', { name: 'Revisão concluída' })).toBeVisible();
  await expect(page.getByText('2 cards revisados')).toBeVisible();
  await expect(page.locator('.card-kicker', { hasText: 'Revisados' })).toBeVisible();
  await expect(page.locator('.card-kicker', { hasText: 'Bom + Fácil' })).toBeVisible();
  await expect(page.locator('.card-kicker', { hasText: 'Difícil' })).toBeVisible();
  await expect(page.locator('.card-kicker', { hasText: 'Errei' })).toBeVisible();
  await expect(page.getByText('Próximos vencimentos')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Revisar de novo' })).toBeVisible();
  await expect(
    page.getByRole('button', { name: 'Voltar para meus flashcards', exact: true }),
  ).toBeVisible();
});

test('E2E-07 o tempo da sessão entra no total do dia', async ({ page }) => {
  await login(page);
  await abrirDecks(page); // sai de /inicio para a linha de base não colidir com a navegação do login
  const antes = await resumoInicio(page);

  const nome = nomeUnico('Deck E2E-07');
  await criarDeck(page, nome);
  await criarCardAvulso(page, nome, 'Frente E2E-07', 'Verso E2E-07');
  await estudarDeck(page, nome);

  await page.keyboard.press('Space');
  await expect(page.getByRole('button', { name: /Fácil/ })).toBeVisible();
  await page.waitForTimeout(1_200); // garante tempoMs >= 1000 para o card virar >= 1 segundo em segundosNoDia
  const finalizacao = page.waitForResponse(
    (r) => r.url().includes('/finalizar') && r.request().method() === 'POST',
  );
  await page.keyboard.press('4');
  await finalizacao;
  await expect(page.getByRole('heading', { name: 'Revisão concluída' })).toBeVisible();

  const depois = await resumoInicio(page);

  // O tempo de questões pode seguir avançando em paralelo (há uma sessão em andamento na semente de
  // dev), então a prova de que a sessão de flashcards *soma* ao total é a decomposição exata do dia,
  // não a igualdade entre os dois deltas.
  expect(depois.tempoFlashcardsSegundos).toBeGreaterThan(antes.tempoFlashcardsSegundos);
  expect(depois.tempoEstudoSegundos).toBe(
    depois.tempoQuestoesSegundos + depois.tempoFlashcardsSegundos,
  );
});

test('E2E-08 a tela de decks mostra tudo que RF-35 pede', async ({ page }) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-08');
  await criarDeck(page, nome, { revisoesDia: 40 });
  await criarCardAvulso(page, nome, 'Frente E2E-08', 'Verso E2E-08');
  await abrirDecks(page);

  const card = deckCard(page, nome);
  await expect(card).toBeVisible();
  await expect(card.locator('.card-title')).toHaveText(nome);
  await expect(card).toContainText('Cards');
  await expect(card).toContainText('Vencendo hoje');
  await expect(card).toContainText('Novos');
  await expect(card).toContainText('40 revisões/dia');
  await expect(card).toContainText('retenção alvo 90%');
  await expect(card.getByRole('button', { name: 'Estudar este deck' })).toBeVisible();
});

test('E2E-09 recusa criar deck com nome curto ou repetido', async ({ page }) => {
  await login(page);
  await abrirDecks(page);
  await page.getByRole('button', { name: 'Novo deck' }).click();
  const dialog = page.locator('.dialog');

  await dialog.locator('#deck-nome').fill('Ab');
  await dialog.getByRole('button', { name: 'Salvar deck' }).click();
  await expect(dialog).toBeVisible();
  await expect(dialog.locator('.erro-campo')).toContainText('entre 3 e 60 caracteres');

  await dialog.locator('#deck-nome').fill('Constitucional');
  await dialog.getByRole('button', { name: 'Salvar deck' }).click();
  await expect(dialog).toBeVisible();
  await expect(dialog.locator('.erro-api')).toContainText('Já existe um deck com esse nome.');
});

test('E2E-10 renomear deck preserva os cards', async ({ page }) => {
  await login(page);
  const nomeOriginal = nomeUnico('Deck E2E-10');
  const nomeNovo = nomeUnico('Deck E2E-10 Renomeado');
  await criarDeck(page, nomeOriginal);
  await criarCardAvulso(page, nomeOriginal, 'Frente E2E-10', 'Verso E2E-10');

  await page.goto('/flashcards');
  await page.getByLabel('Deck').selectOption({ label: nomeOriginal });
  const proximaRevisaoAntes = await page
    .locator('.table tbody tr', { hasText: 'Frente E2E-10' })
    .locator('td')
    .nth(5)
    .innerText();

  await abrirDecks(page);
  await expect(deckCard(page, nomeOriginal)).toBeVisible();

  await deckCard(page, nomeOriginal)
    .getByRole('button', { name: `Configurar deck ${nomeOriginal}` })
    .click();
  const dialog = page.locator('.dialog');
  await dialog.locator('#deck-nome').fill(nomeNovo);
  await dialog.getByRole('button', { name: 'Salvar deck' }).click();
  await expect(dialog).toHaveCount(0);

  await expect(deckCard(page, nomeOriginal)).toHaveCount(0);
  await expect(deckCard(page, nomeNovo)).toBeVisible();

  await page.goto('/flashcards');
  await page.getByLabel('Deck').selectOption({ label: nomeNovo });
  const linha = page.locator('.table tbody tr', { hasText: 'Frente E2E-10' });
  await expect(linha).toBeVisible();
  await expect(linha.locator('td').nth(5)).toHaveText(proximaRevisaoAntes);
});

test('E2E-11 excluir deck envia os cards para "Sem deck" sem apagá-los', async ({ page }) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-11');
  const frente = `Frente ${nomeUnico('E2E-11')}`;
  await criarDeck(page, nome);
  await criarCardAvulso(page, nome, frente, 'Verso E2E-11');
  await abrirDecks(page);

  await deckCard(page, nome)
    .getByRole('button', { name: `Excluir deck ${nome}` })
    .click();
  await expect(page.getByRole('heading', { name: 'Excluir deck' })).toBeVisible();
  const resposta = page.waitForResponse(
    (r) => r.url().includes('/flashcards/decks/') && r.request().method() === 'DELETE',
  );
  await page.locator('.dialog-actions').getByRole('button', { name: 'Excluir deck' }).click();
  expect((await resposta).status()).toBe(200);

  await expect(deckCard(page, nome)).toHaveCount(0);
  await page.goto('/flashcards');
  await page.getByLabel('Deck').selectOption({ label: 'Sem deck' });
  const linha = page.locator('.table tbody tr', { hasText: frente });
  await expect(linha).toBeVisible();
  await expect(linha).toContainText('Sem deck');
});

test('E2E-12 configura 500 revisões por dia e o valor é ajustado para 400', async ({ page }) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-12');
  await criarDeck(page, nome);
  await abrirDecks(page);

  await deckCard(page, nome)
    .getByRole('button', { name: `Configurar deck ${nome}` })
    .click();
  const dialog = page.locator('.dialog');
  await dialog.locator('#deck-revisoes').fill('500');
  const resposta = page.waitForResponse(
    (r) => r.url().includes('/flashcards/decks/') && r.request().method() === 'PUT',
  );
  await dialog.getByRole('button', { name: 'Salvar deck' }).click();
  expect((await resposta).status()).toBe(200);
  await expect(dialog).toHaveCount(0);

  await deckCard(page, nome)
    .getByRole('button', { name: `Configurar deck ${nome}` })
    .click();
  await expect(page.locator('.dialog').locator('#deck-revisoes')).toHaveValue('400');
});

test('E2E-13 exibe o aviso de fila acumulada na tela de decks', async ({ page }) => {
  await login(page);
  await abrirDecks(page);

  const card = deckCard(page, 'Revisao Acumulada');
  await expect(card).toBeVisible();
  await expect(card).toContainText('vencidos');
  await expect(card.getByRole('button', { name: 'Ajustar limites' })).toBeVisible();
});

test('E2E-14 estudar um deck específico traz somente cards daquele deck', async ({ page }) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-14');
  await criarDeck(page, nome);
  await criarCardAvulso(page, nome, 'Frente A E2E-14', 'Verso A E2E-14');
  await criarCardAvulso(page, nome, 'Frente B E2E-14', 'Verso B E2E-14');
  await estudarDeck(page, nome);

  await expect(page.locator('.tag.tag-outline', { hasText: nome })).toBeVisible();
  await page.keyboard.press('Space');
  await page.keyboard.press('3');
  await expect(page.locator('.tag.tag-outline', { hasText: nome })).toBeVisible();
});

test('E2E-15 mostra o estado vazio quando a fila do dia termina', async ({ page }) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-15');
  await criarDeck(page, nome);
  await estudarDeck(page, nome);

  await expect(page.getByText('Nada vencido por hoje')).toBeVisible();
  await expect(
    page.getByText('Seus cards voltam à fila conforme o agendamento da repetição espaçada.'),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'Ver todos os cards' })).toBeVisible();
  await expect(page.getByText(/[Aa]ntecipar/)).toHaveCount(0);
});

test('E2E-16 a sessão inteira é operável por teclado, com foco visível', async ({ page }) => {
  await login(page);
  const nome = nomeUnico('Deck E2E-16');
  await criarDeck(page, nome);
  await criarCardAvulso(page, nome, 'Frente E2E-16', 'Verso E2E-16');
  await estudarDeck(page, nome);

  const analiseCard = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  expect(analiseCard.violations.map((item) => item.id)).toEqual([]);

  await page.keyboard.press('Space');
  await expect(page.getByText('Verso E2E-16')).toBeVisible();
  const analiseRevelado = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  expect(analiseRevelado.violations.map((item) => item.id)).toEqual([]);

  await page.keyboard.press('4'); // Fácil graduado direto, sem reenfileirar, garante o fim da sessão
  await expect(page.getByRole('heading', { name: 'Revisão concluída' })).toBeVisible();
  const analiseResumo = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  expect(analiseResumo.violations.map((item) => item.id)).toEqual([]);

  await page.keyboard.press('Tab');
  const focoVisivel = await page.evaluate(() => {
    const ativo = document.activeElement as HTMLElement | null;
    if (!ativo) return false;
    const estilo = getComputedStyle(ativo);
    return estilo.outlineStyle !== 'none' || estilo.boxShadow !== 'none';
  });
  expect(focoVisivel).toBe(true);
});
