import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

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

async function iniciar(page: Page, modo = 'FOCO'): Promise<void> {
  await login(page);
  await page.goto(`/estudar?modo=${modo}`);
  await expect(page.locator('.questao')).toBeVisible();
}

function questoesResolvidas(page: Page) {
  return page.locator('.cards article', { hasText: 'Questões resolvidas' }).locator('strong');
}

test('E2E-01 estuda em Foco, responde e vê o gabarito', async ({ page }) => {
  await iniciar(page);
  await expect(page.getByText('Disciplina')).toHaveCount(0);
  await page.getByRole('radio').first().click();
  await page.getByRole('button', { name: /Responder/ }).click();
  await expect(page.locator('.painel-gabarito')).toContainText('Gabarito');
  // A resposta é definitiva: as alternativas ficam travadas.
  await expect(page.getByRole('radio').first()).toBeDisabled();
  await expect(page.getByRole('radio').nth(1)).toBeDisabled();
});

test('E2E-02 abre em Contexto e mostra os metadados da questão', async ({ page }) => {
  await iniciar(page, 'CONTEXTO');
  const codigo = await page.locator('.codigo').innerText();
  await page.getByRole('radio').first().click();
  await expect(page.locator('.metadados')).toBeVisible();
  await expect(page.locator('.codigo')).toHaveText(codigo);
  await expect(page.getByRole('radio').first()).toHaveAttribute('aria-checked', 'true');
});

test('E2E-03 conclui o Modo Prova e exibe a correção', async ({ page }) => {
  await iniciar(page, 'PROVA');
  await expect(page.locator('.painel-gabarito')).toHaveCount(0);
  await page.getByRole('radio').first().click();
  await page.getByRole('button', { name: 'Próxima' }).click();
  // Volta a uma questão já respondida e troca a resposta antes de finalizar.
  await page.getByRole('button', { name: 'Anterior' }).click();
  await page.getByRole('radio').nth(1).click();
  await expect(page.getByRole('radio').nth(1)).toHaveAttribute('aria-checked', 'true');
  await page.getByRole('button', { name: 'Finalizar prova' }).click();
  await expect(page.getByText('Resultado da prova')).toBeVisible();
  await expect(page.locator('.resumo')).toContainText('acertos');
  await expect(page.locator('.resumo')).toContainText('erros');
  await expect(page.locator('.resumo')).toContainText('não respondidas');
  await expect(page.locator('.resumo')).toContainText('tempo total');
  await expect(page.locator('.itens article').first()).toContainText('Gabarito:');
  await expect(page.getByRole('button', { name: 'Refazer' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Voltar ao início' })).toBeVisible();
});

test('E2E-04 prova abandonada não entra no histórico', async ({ page }) => {
  await login(page);
  await page.goto('/inicio');
  const antes = await questoesResolvidas(page).innerText();

  await page.goto('/estudar?modo=PROVA');
  await expect(page.locator('.questao')).toBeVisible();
  for (let i = 0; i < 5; i++) {
    await page.getByRole('radio').first().click();
    await page.getByRole('button', { name: 'Próxima' }).click();
  }

  // Fecha sem finalizar: em Modo Prova a marcação é só em memória (RN-04), nada é gravado.
  await page.goto('/inicio');
  await expect(questoesResolvidas(page)).toHaveText(antes);

  // A prova abandonada nunca é oferecida por "Retomar": o Início só resgata sessões fora do Modo Prova.
  if (await page.locator('.retomar').count()) {
    await page.getByRole('button', { name: 'Continuar' }).click();
    await expect(page.getByRole('button', { name: 'Finalizar prova' })).toHaveCount(0);
  }
});

test('E2E-05 marca erro recorrente e mantém a marcação em nova sessão', async ({ page }) => {
  await iniciar(page);
  await page.getByRole('button', { name: 'Marcar erro recorrente' }).click();
  await expect(page.getByRole('button', { name: 'Remover erro recorrente' })).toBeVisible();
  await page.goto('/inicio');
  await page.getByRole('button', { name: 'Continuar' }).click();
  await expect(page.getByRole('button', { name: 'Remover erro recorrente' })).toBeVisible();
});

// E2E-06 (RF-28: criar flashcard na sessão de estudo sem perder o progresso) é coberto por
// e2e/flashcards.spec.ts, teste "E2E-02 abre e fecha o modal preservando a questão, a alternativa
// marcada e o cronômetro" — que exercita exatamente este cenário (questão, resposta marcada e
// cronômetro intactos após abrir e fechar o FlashcardDialog durante a sessão de estudo).

test('E2E-07 continua a sessão e navega por número preservando resposta', async ({ page }) => {
  await iniciar(page);
  await page.getByRole('radio').first().click();
  await page.getByRole('button', { name: /Responder/ }).click();
  await page.goto('/inicio');
  await page.getByRole('button', { name: 'Continuar' }).click();
  await expect(page).toHaveURL(/\/estudar\?sessao=/);
  await page.locator('.navegacao-serie button').first().click();
  await expect(page.getByRole('radio').first()).toHaveAttribute('aria-checked', 'true');
});

test('E2E-08 opera por teclado com foco visível e sem salto de layout', async ({ page }) => {
  await iniciar(page);
  const analise = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  expect(analise.violations.map((item) => item.id)).toEqual([]);
  const antes = await page.locator('.questao').boundingBox();
  await page.getByRole('radio').first().focus();
  const primeiroTexto = await page.getByRole('radio').first().innerText();
  const tecla = primeiroTexto.toLowerCase().includes('certo') ? 'c' : 'a';
  await page.keyboard.press(tecla);
  await page.keyboard.press('Enter');
  await expect(page.locator('.painel-gabarito')).toBeVisible();
  await page.keyboard.press('ArrowRight');
  const depois = await page.locator('.questao').boundingBox();
  expect(depois?.height).toBe(antes?.height);
  await expect(page.locator(':focus')).toBeVisible();
});
