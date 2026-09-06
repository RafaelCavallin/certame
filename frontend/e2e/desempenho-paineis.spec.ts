import { test, expect, type APIRequestContext, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const apiBase = 'http://localhost:3000/api/v1';
const senhaDev = 'senhaDev123';
const professor = 'professor@dev.certame.local';
const administrador = 'admin@dev.certame.local';

function cabecalho(token: string) {
  return { Authorization: `Bearer ${token}` };
}

async function autenticar(
  api: APIRequestContext,
  email: string,
  senha = senhaDev,
): Promise<string> {
  const resposta = await api.post(`${apiBase}/auth/login`, { data: { email, senha } });
  expect(resposta.ok(), `login HTTP ${resposta.status()} ${await resposta.text()}`).toBe(true);
  return (await resposta.json()).accessToken;
}

async function cadastrarEstudante(
  api: APIRequestContext,
  nome: string,
): Promise<{ email: string; senha: string; token: string }> {
  const email = `e2e-desempenho-${Date.now()}-${Math.random().toString(36).slice(2, 7)}@example.com`;
  const senha = 'senhaSegura123';
  const resposta = await api.post(`${apiBase}/auth/cadastro`, { data: { nome, email, senha } });
  expect(resposta.status(), await resposta.text()).toBe(201);
  const corpo = await resposta.json();
  return { email, senha, token: corpo.accessToken as string };
}

async function login(page: Page, email: string, senha: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(senha);
  const resposta = page.waitForResponse((item) => item.url().includes('/auth/login'));
  await page.getByRole('button', { name: 'Entrar' }).click();
  expect((await resposta).status()).toBe(200);
  await expect(page).toHaveURL(/\/inicio$/);
}

interface Referencia {
  disciplinaId: string;
  assuntoId: string;
  disciplinaNome: string;
  assuntoNome: string;
}

/**
 * Cria uma disciplina e um assunto próprios (nomes únicos por execução) em vez de reaproveitar a
 * taxonomia da semente dev — outras suítes (ex.: banco-questoes.spec.ts) arquivam ou renomeiam
 * assuntos da semente como parte dos seus próprios cenários, e a suíte inteira roda no mesmo banco.
 */
async function criarReferencia(
  api: APIRequestContext,
  token: string,
  rotulo: string,
): Promise<Referencia> {
  const sufixo = `${rotulo} ${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
  const disciplinaNome = `Disciplina ${sufixo}`;
  const assuntoNome = `Assunto ${sufixo}`;
  const disciplina = await (
    await api.post(`${apiBase}/admin/disciplinas`, {
      headers: cabecalho(token),
      data: { nome: disciplinaNome },
    })
  ).json();
  const assunto = await (
    await api.post(`${apiBase}/admin/assuntos`, {
      headers: cabecalho(token),
      data: { nome: assuntoNome, disciplinaId: disciplina.id },
    })
  ).json();
  return { disciplinaId: disciplina.id, assuntoId: assunto.id, disciplinaNome, assuntoNome };
}

/** Cria e publica `quantidade` questões ME de gabarito A no assunto informado. */
async function criarQuestoesPublicadas(
  api: APIRequestContext,
  tokenProfessor: string,
  tokenAdmin: string,
  ref: Referencia,
  quantidade: number,
): Promise<string[]> {
  const criacoes = await Promise.all(
    Array.from({ length: quantidade }, async (_valor, indice) => {
      const criada = await api.post(`${apiBase}/admin/questoes`, {
        headers: cabecalho(tokenProfessor),
        data: {
          tipo: 'ME',
          enunciado: `Questão E2E de desempenho ${ref.assuntoNome} ${Date.now()}-${indice}-${Math.random().toString(36).slice(2, 6)}.`,
          disciplinaId: ref.disciplinaId,
          assuntoId: ref.assuntoId,
          alternativas: [
            { texto: 'Primeira alternativa', ordem: 1 },
            { texto: 'Segunda alternativa', ordem: 2 },
          ],
          gabarito: 'A',
          comentario: 'Comentário de apoio da questão de desempenho E2E.',
        },
      });
      expect(criada.status(), await criada.text()).toBe(201);
      return (await criada.json()).id as string;
    }),
  );
  for (const id of criacoes) {
    const publicada = await api.post(`${apiBase}/admin/questoes/${id}/publicar`, {
      headers: cabecalho(tokenAdmin),
    });
    expect(publicada.status(), await publicada.text()).toBe(200);
  }
  return criacoes;
}

/**
 * Abre uma sessão Foco de 30 questões filtrada por assunto e responde só `quantidade` delas antes de
 * finalizar — a sessão livre exige um dos tamanhos fixos (10/30/50, `sessao_tamanho_valido`), por
 * isso o assunto precisa ter ao menos 30 questões publicadas mesmo quando o teste só quer, digamos,
 * 19 respostas: a série vem cheia (30) e o restante fica só reservado, sem resposta.
 */
async function estudarAssunto(
  api: APIRequestContext,
  tokenEstudante: string,
  assuntoId: string,
  acertar: boolean,
  quantidade = 30,
): Promise<number> {
  const aberta = await api.post(`${apiBase}/estudo/sessoes`, {
    headers: cabecalho(tokenEstudante),
    data: { modo: 'FOCO', tamanho: 30, filtro: { assuntoId } },
  });
  expect(aberta.status(), await aberta.text()).toBe(201);
  const sessao = await aberta.json();
  for (const questao of sessao.questoes.slice(0, quantidade)) {
    // Só usada com assuntos de questões ME criadas por este arquivo (gabarito 'A'); nenhuma delas é CE.
    const resposta = await api.post(`${apiBase}/estudo/sessoes/${sessao.sessaoId}/respostas`, {
      headers: cabecalho(tokenEstudante),
      data: { questaoId: questao.id, marcada: acertar ? 'A' : 'B', tempoMs: 1500, pausadoMs: 0 },
    });
    expect(resposta.status(), await resposta.text()).toBe(201);
  }
  const finalizada = await api.post(`${apiBase}/estudo/sessoes/${sessao.sessaoId}/finalizar`, {
    headers: cabecalho(tokenEstudante),
    data: { respostas: [], pausadoMs: 0 },
  });
  expect(finalizada.status(), await finalizada.text()).toBe(200);
  return Math.min(quantidade, sessao.questoes.length);
}

test.describe('painéis com histórico controlado (CA-06…CA-10)', () => {
  test.describe.configure({ timeout: 120_000 });
  let estudante: { email: string; senha: string; token: string };
  let assuntoFraco: Referencia;
  let assuntoRaso: Referencia;

  test.beforeAll(async ({ request }) => {
    const tokenProfessor = await autenticar(request, professor);
    const tokenAdmin = await autenticar(request, administrador);
    estudante = await cadastrarEstudante(request, 'Estudante Desempenho E2E');

    assuntoFraco = await criarReferencia(request, tokenProfessor, 'Fraco');
    assuntoRaso = await criarReferencia(request, tokenProfessor, 'Raso');
    const assuntoControle = await criarReferencia(request, tokenProfessor, 'Controle');

    // 30 por assunto: sessão livre só aceita tamanho 10/30/50 (sessao_tamanho_valido) — a série vem
    // cheia mesmo quando só uma parte é respondida (ver `estudarAssunto`).
    const questoesFraco = await criarQuestoesPublicadas(
      request,
      tokenProfessor,
      tokenAdmin,
      assuntoFraco,
      30,
    );
    await criarQuestoesPublicadas(request, tokenProfessor, tokenAdmin, assuntoRaso, 30);
    await criarQuestoesPublicadas(request, tokenProfessor, tokenAdmin, assuntoControle, 30);

    // Caderno manual com uma questão: evita o estado vazio de /cadernos e dá um card editável (menu
    // de ações) para a checagem de acessibilidade do diálogo de seleção de questões no E2E-13.
    const cadernoA11y = await request.post(`${apiBase}/cadernos`, {
      headers: cabecalho(estudante.token),
      data: {
        nome: 'Caderno de acessibilidade E2E-13',
        descricao: 'Usado só pela checagem de acessibilidade.',
      },
    });
    expect(cadernoA11y.status(), await cadernoA11y.text()).toBe(201);
    const adicionada = await request.post(
      `${apiBase}/cadernos/${(await cadernoA11y.json()).id}/questoes`,
      {
        headers: cabecalho(estudante.token),
        data: { questaoIds: [questoesFraco[0]] },
      },
    );
    expect(adicionada.status(), await adicionada.text()).toBe(200);

    // 25 respostas erradas (0%) no assunto fraco — abaixo da média do usuário, volume ≥ 20 (CA-09).
    expect(await estudarAssunto(request, estudante.token, assuntoFraco.assuntoId, false, 25)).toBe(
      25,
    );
    // 19 respostas certas no assunto raso — volume abaixo do corte de 20, mesmo com bom percentual (CA-10).
    expect(await estudarAssunto(request, estudante.token, assuntoRaso.assuntoId, true, 19)).toBe(
      19,
    );
    // 10 respostas certas no assunto de controle — puxa a média geral para cima da 0% do assunto fraco.
    expect(
      await estudarAssunto(request, estudante.token, assuntoControle.assuntoId, true, 10),
    ).toBe(10);
  });

  test('E2E-06 abre o desempenho com histórico: curva, média, melhor semana e total', async ({
    page,
  }) => {
    await login(page, estudante.email, estudante.senha);
    await page.goto('/desempenho');

    await expect(page.getByText('Seu desempenho começa aqui')).toHaveCount(0);
    const indicadores = page.locator('.indicadores .card');
    await expect(indicadores).toHaveCount(4);
    await expect(indicadores.filter({ hasText: 'Total resolvidas' })).toContainText('54');
    await expect(indicadores.filter({ hasText: 'Acertos' })).toContainText('29');
    await expect(indicadores.filter({ hasText: 'Erros' })).toContainText('25');

    await expect(page.locator('.evolucao')).toContainText('% aproveitamento');
    await expect(page.locator('.evolucao .table thead th')).toHaveText([
      'Semana',
      'Aproveitamento',
      'Questões',
    ]);
    const linhas = page.locator('.evolucao .table tbody tr');
    await expect(linhas).toHaveCount(8);
    // A semana corrente concentra os 54 registros arranjados no beforeAll.
    await expect(linhas.last()).toContainText('54');
  });

  test('E2E-07 semana sem atividade interrompe a curva sem cair a zero, mantendo as 8 semanas', async ({
    page,
  }) => {
    await login(page, estudante.email, estudante.senha);
    await page.goto('/desempenho');

    const linhas = page.locator('.evolucao .table tbody tr');
    await expect(linhas).toHaveCount(8);
    // As sete semanas sem atividade aparecem como lacuna textual, nunca como 0%.
    await expect(linhas.filter({ hasText: 'Sem atividade' })).toHaveCount(7);
    await expect(page.locator('.evolucao svg circle.ponto')).toHaveCount(1);
  });

  test('E2E-08 desempenho por disciplina exibe volume e percentual de cada uma', async ({
    page,
  }) => {
    await login(page, estudante.email, estudante.senha);
    await page.goto('/desempenho');

    const disciplinas = page.locator('.disciplinas .disciplina');
    await expect(disciplinas).toHaveCount(3);
    const fraco = disciplinas.filter({ hasText: assuntoFraco.disciplinaNome });
    await expect(fraco.locator('.linha')).toContainText('0%');
    await expect(fraco.locator('.volume')).toContainText('25');
    const raso = disciplinas.filter({ hasText: assuntoRaso.disciplinaNome });
    await expect(raso.locator('.linha')).toContainText('100%');
    await expect(raso.locator('.volume')).toContainText('19');
  });

  test('E2E-09 pontos fracos aplica o corte de 20 respostas', async ({ page }) => {
    await login(page, estudante.email, estudante.senha);
    await page.goto('/desempenho');

    const linhaFraco = page
      .locator('.pontos tbody tr')
      .filter({ hasText: assuntoFraco.assuntoNome });
    await expect(linhaFraco).toBeVisible();
    await expect(linhaFraco).toContainText(assuntoFraco.disciplinaNome);
    await expect(linhaFraco).toContainText('25');
    await expect(linhaFraco).toContainText('0%');

    await expect(
      page.locator('.pontos tbody tr').filter({ hasText: assuntoRaso.assuntoNome }),
    ).toHaveCount(0);
  });

  test('E2E-12 "Treinar" a partir de um ponto fraco abre sessão restrita ao assunto', async ({
    page,
    request,
  }) => {
    await login(page, estudante.email, estudante.senha);
    await page.goto('/desempenho');

    const linhaFraco = page
      .locator('.pontos tbody tr')
      .filter({ hasText: assuntoFraco.assuntoNome });
    await linhaFraco.getByRole('button', { name: 'Treinar' }).click();

    await expect(page).toHaveURL(/\/estudar\?sessao=/);
    const sessaoId = new URL(page.url()).searchParams.get('sessao');
    const sessao = await (
      await request.get(`${apiBase}/estudo/sessoes/${sessaoId}`, {
        headers: cabecalho(estudante.token),
      })
    ).json();
    expect(sessao.questoes.length).toBeGreaterThan(0);
    expect(
      sessao.questoes.every((q: { assunto: string }) => q.assunto === assuntoFraco.assuntoNome),
    ).toBe(true);
  });

  test('E2E-13 acessibilidade das telas de cadernos e desempenho, com foco visível e sem violações', async ({
    page,
  }) => {
    await login(page, estudante.email, estudante.senha);
    for (const rota of ['/cadernos', '/desempenho']) {
      await page.goto(rota);
      await expect(
        page.locator(rota === '/cadernos' ? '.cadernos-page' : '.desempenho-page'),
      ).toBeVisible();

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

    // O gráfico expõe alternativa textual equivalente para leitor de tela (CA-07, PRD).
    await page.goto('/desempenho');
    await expect(
      page.locator('svg[aria-labelledby="evolucao-titulo evolucao-descricao"]'),
    ).toBeVisible();
    await expect(page.locator('#evolucao-descricao')).toHaveText(
      /Semanas sem atividade aparecem como lacunas/,
    );

    await page.goto('/cadernos');
    await page.getByRole('button', { name: 'Novo caderno' }).click();
    let dialogo = page.locator('.dialog');
    await expect(dialogo).toBeVisible();
    let analiseDialogo = await new AxeBuilder({ page })
      .include('.dialog')
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(
      analiseDialogo.violations.map((v) => v.id),
      'violações no diálogo Novo caderno',
    ).toEqual([]);
    await page.getByRole('button', { name: 'Fechar diálogo' }).click();
    await expect(dialogo).toHaveCount(0);

    const cardManual = page
      .locator('.card.caderno')
      .filter({ hasText: 'Caderno de acessibilidade E2E-13' });
    await expect(cardManual).toBeVisible();
    await cardManual.getByLabel(/Ações do caderno/).click();
    await cardManual.getByRole('button', { name: 'Adicionar questões' }).click();
    dialogo = page.locator('.dialog.picker');
    await expect(dialogo).toBeVisible();
    analiseDialogo = await new AxeBuilder({ page })
      .include('.dialog.picker')
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(
      analiseDialogo.violations.map((v) => v.id),
      'violações no diálogo de seleção de questões',
    ).toEqual([]);
    await page.getByRole('button', { name: 'Fechar seleção' }).click();
    await expect(dialogo).toHaveCount(0);

    // O menu de ações não fecha sozinho ao fechar o seletor de questões — segue aberto.
    await cardManual.getByRole('button', { name: 'Excluir' }).click();
    const confirmacao = page.locator('.dialog[role=alertdialog]');
    await expect(confirmacao).toBeVisible();
    const analiseConfirmacao = await new AxeBuilder({ page })
      .include('.dialog[role=alertdialog]')
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(
      analiseConfirmacao.violations.map((v) => v.id),
      'violações no diálogo de exclusão',
    ).toEqual([]);
    await confirmacao.getByRole('button', { name: 'Cancelar' }).click();
    await expect(confirmacao).toHaveCount(0);
  });
});

test.describe('estados sem histórico e integridade do Modo Prova (CA-11, CA-12)', () => {
  test('E2E-10 usuário novo abre cadernos e desempenho sem gráfico, métrica fictícia ou card zerado', async ({
    page,
    request,
  }) => {
    const novo = await cadastrarEstudante(request, 'Pessoa Nova E2E');

    await login(page, novo.email, novo.senha);
    await page.goto('/cadernos');
    await expect(page.getByText('Você ainda não tem cadernos')).toBeVisible();
    await expect(page.locator('.card.caderno')).toHaveCount(0);

    await page.goto('/desempenho');
    await expect(page.getByText('Seu desempenho começa aqui')).toBeVisible();
    await expect(page.locator('.indicadores')).toHaveCount(0);
    await expect(page.locator('.paineis')).toHaveCount(0);
  });

  test('E2E-11 Modo Prova abandonado não altera o desempenho', async ({ page, request }) => {
    const tokenProfessor = await autenticar(request, professor);
    const tokenAdmin = await autenticar(request, administrador);
    const estudante = await cadastrarEstudante(request, 'Estudante Prova Abandonada E2E');
    const ref = await criarReferencia(request, tokenProfessor, 'ProvaAbandonada');
    await criarQuestoesPublicadas(request, tokenProfessor, tokenAdmin, ref, 30);
    await estudarAssunto(request, estudante.token, ref.assuntoId, true, 10);

    await login(page, estudante.email, estudante.senha);
    await page.goto('/desempenho');
    const indicadores = page.locator('.indicadores .card');
    await expect(indicadores).toHaveCount(4);
    const antes = await indicadores.allInnerTexts();

    await page.goto('/estudar?modo=PROVA');
    await expect(page.locator('.questao')).toBeVisible();
    for (let i = 0; i < 5; i++) {
      await page.getByRole('radio').first().click();
      await page.getByRole('button', { name: 'Próxima' }).click();
    }
    // Fecha sem finalizar: em Modo Prova nada é gravado antes da finalização (RN-04).
    await page.goto('/desempenho');

    await expect(indicadores).toHaveCount(4);
    expect(await indicadores.allInnerTexts()).toEqual(antes);
  });
});
