import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { QuestaoApi } from '../../../../core/api/questao-api';
import { AuthStore } from '../../../../core/auth/auth-store';
import { QuestoesPage } from './questoes-page';

describe('QuestoesPage', () => {
  const paginaVazia = { content: [], page: 0, size: 20, totalElements: 0 };

  function montar(opcoes: { listar?: ReturnType<typeof vi.fn>; perfil?: string } = {}): {
    page: QuestoesPage;
    listar: ReturnType<typeof vi.fn>;
  } {
    TestBed.resetTestingModule();
    const params = new BehaviorSubject(convertToParamMap({}));
    const listar = opcoes.listar ?? vi.fn().mockReturnValue(of(paginaVazia));
    TestBed.configureTestingModule({
      imports: [QuestoesPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
        {
          provide: QuestaoApi,
          useValue: { listar, publicar: vi.fn(), despublicar: vi.fn(), excluir: vi.fn() },
        },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]), bancas: () => of([]) } },
        { provide: AuthStore, useValue: { perfil: () => opcoes.perfil ?? 'ADMIN' } },
      ],
    });
    return { page: TestBed.createComponent(QuestoesPage).componentInstance, listar };
  }

  it('carrega a primeira página de questões ao iniciar', async () => {
    const { page, listar } = montar();
    await page.carregar();

    expect(listar).toHaveBeenCalled();
    expect(page.pagina()).toEqual(paginaVazia);
  });

  it('exibe o erro da API quando a listagem falha', async () => {
    const { page } = montar({
      listar: vi
        .fn()
        .mockReturnValue(
          throwError(() => ({ error: { code: 'ERRO', message: 'Falha ao listar questões.' } })),
        ),
    });

    await page.carregar();

    expect(page.erro()).toBe('Falha ao listar questões.');
  });

  it('zera a página e recarrega ao aplicar o filtro', async () => {
    const { page, listar } = montar();
    page.filtros.pagina.set(3);

    page.aplicar();
    await page.carregar();

    expect(page.filtros.pagina()).toBe(0);
    expect(listar).toHaveBeenCalled();
  });

  it('converte o status da questão em rótulo textual', () => {
    const { page } = montar();

    expect(page.statusLabel('RASCUNHO')).toBe('Rascunho');
    expect(page.statusLabel('EM_REVISAO')).toBe('Em revisão');
    expect(page.statusLabel('PUBLICADA')).toBe('Publicada');
  });

  it('abre a confirmação de exclusão para a questão informada', () => {
    const { page } = montar();

    page.excluir('q1');

    expect(page.confirmacao()).toEqual({ acao: 'excluir', id: 'q1' });
  });

  it('abre a confirmação de despublicação para a questão informada', () => {
    const { page } = montar();

    page.despublicar('q1');

    expect(page.confirmacao()).toEqual({ acao: 'despublicar', id: 'q1' });
  });

  it('exclui a questão confirmada, fecha o diálogo e recarrega a lista', async () => {
    TestBed.resetTestingModule();
    const params = new BehaviorSubject(convertToParamMap({}));
    const excluir = vi.fn().mockReturnValue(of(undefined));
    const listar = vi.fn().mockReturnValue(of(paginaVazia));
    TestBed.configureTestingModule({
      imports: [QuestoesPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
        {
          provide: QuestaoApi,
          useValue: { listar, excluir, publicar: vi.fn(), despublicar: vi.fn() },
        },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]), bancas: () => of([]) } },
        { provide: AuthStore, useValue: { perfil: () => 'ADMIN' } },
      ],
    });
    const page = TestBed.createComponent(QuestoesPage).componentInstance;
    page.excluir('q1');

    await page.confirmar();

    expect(excluir).toHaveBeenCalledWith('q1');
    expect(page.confirmacao()).toBeNull();
  });

  it('despublica a questão confirmada', async () => {
    TestBed.resetTestingModule();
    const params = new BehaviorSubject(convertToParamMap({}));
    const despublicar = vi.fn().mockReturnValue(of({}));
    TestBed.configureTestingModule({
      imports: [QuestoesPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
        {
          provide: QuestaoApi,
          useValue: {
            listar: vi.fn().mockReturnValue(of(paginaVazia)),
            despublicar,
            publicar: vi.fn(),
            excluir: vi.fn(),
          },
        },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]), bancas: () => of([]) } },
        { provide: AuthStore, useValue: { perfil: () => 'ADMIN' } },
      ],
    });
    const page = TestBed.createComponent(QuestoesPage).componentInstance;
    page.despublicar('q1');

    await page.confirmar();

    expect(despublicar).toHaveBeenCalledWith('q1');
    expect(page.confirmacao()).toBeNull();
  });

  it('trata o erro QUESTAO_EM_USO com uma orientação adicional sobre despublicar', async () => {
    TestBed.resetTestingModule();
    const params = new BehaviorSubject(convertToParamMap({}));
    const excluir = vi.fn().mockReturnValue(
      throwError(() => ({
        error: { code: 'QUESTAO_EM_USO', message: 'Esta questão está vinculada a um caderno.' },
      })),
    );
    TestBed.configureTestingModule({
      imports: [QuestoesPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
        {
          provide: QuestaoApi,
          useValue: {
            listar: vi.fn().mockReturnValue(of(paginaVazia)),
            excluir,
            publicar: vi.fn(),
            despublicar: vi.fn(),
          },
        },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]), bancas: () => of([]) } },
        { provide: AuthStore, useValue: { perfil: () => 'ADMIN' } },
      ],
    });
    const page = TestBed.createComponent(QuestoesPage).componentInstance;
    page.excluir('q1');

    await page.confirmar();

    expect(page.erro()).toBe(
      'Esta questão está vinculada a um caderno. Despublique a questão antes de tentar novamente.',
    );
    expect(page.confirmacao()).toBeNull();
  });

  it('exibe o erro simples quando o código não é QUESTAO_EM_USO', async () => {
    TestBed.resetTestingModule();
    const params = new BehaviorSubject(convertToParamMap({}));
    const excluir = vi.fn().mockReturnValue(
      throwError(() => ({
        error: { code: 'RECURSO_NAO_ENCONTRADO', message: 'Questão não encontrada.' },
      })),
    );
    TestBed.configureTestingModule({
      imports: [QuestoesPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
        {
          provide: QuestaoApi,
          useValue: {
            listar: vi.fn().mockReturnValue(of(paginaVazia)),
            excluir,
            publicar: vi.fn(),
            despublicar: vi.fn(),
          },
        },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]), bancas: () => of([]) } },
        { provide: AuthStore, useValue: { perfil: () => 'ADMIN' } },
      ],
    });
    const page = TestBed.createComponent(QuestoesPage).componentInstance;
    page.excluir('q1');

    await page.confirmar();

    expect(page.erro()).toBe('Questão não encontrada.');
  });

  it('renderiza a tabela e permite publicar e confirmar a exclusão pela tela', async () => {
    const questao = {
      id: 'q1',
      codigo: 'Q-1',
      enunciadoResumo: 'Enunciado de teste',
      disciplina: 'Direito Constitucional',
      banca: null,
      ano: null,
      tipo: 'ME',
      status: 'RASCUNHO',
      atualizadoEm: '2026-01-01T00:00:00Z',
    };
    const listar = vi
      .fn()
      .mockReturnValue(of({ content: [questao], page: 0, size: 20, totalElements: 1 }));
    const publicar = vi.fn().mockReturnValue(of({}));
    const excluir = vi.fn().mockReturnValue(of(undefined));
    TestBed.resetTestingModule();
    const params = new BehaviorSubject(convertToParamMap({}));
    TestBed.configureTestingModule({
      imports: [QuestoesPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
        { provide: QuestaoApi, useValue: { listar, publicar, excluir, despublicar: vi.fn() } },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]), bancas: () => of([]) } },
        { provide: AuthStore, useValue: { perfil: () => 'ADMIN' } },
      ],
    });
    const fixture = TestBed.createComponent(QuestoesPage);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Q-1');

    const botaoPublicar = fixture.nativeElement.querySelector(
      'button[title="Publicar"]',
    ) as HTMLButtonElement;
    botaoPublicar.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(publicar).toHaveBeenCalledWith('q1');

    const botaoExcluir = fixture.nativeElement.querySelector(
      'button[title="Excluir"]',
    ) as HTMLButtonElement;
    botaoExcluir.click();
    fixture.detectChanges();

    const botaoConfirmarExclusao = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ).find((b) => (b as HTMLButtonElement).textContent?.trim() === 'Excluir') as HTMLButtonElement;
    botaoConfirmarExclusao.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(excluir).toHaveBeenCalledWith('q1');
  });

  it('permite filtrar pelo formulário e cancelar a confirmação de despublicação', async () => {
    const questaoPublicada = {
      id: 'q2',
      codigo: 'Q-2',
      enunciadoResumo: 'Outra questão',
      disciplina: 'Direito Administrativo',
      banca: 'Cebraspe',
      ano: 2025,
      tipo: 'CE',
      status: 'PUBLICADA',
      atualizadoEm: '2026-01-01T00:00:00Z',
    };
    const listar = vi
      .fn()
      .mockReturnValue(of({ content: [questaoPublicada], page: 0, size: 20, totalElements: 1 }));
    const despublicar = vi.fn().mockReturnValue(of({}));
    TestBed.resetTestingModule();
    const params = new BehaviorSubject(convertToParamMap({}));
    TestBed.configureTestingModule({
      imports: [QuestoesPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
        {
          provide: QuestaoApi,
          useValue: { listar, despublicar, publicar: vi.fn(), excluir: vi.fn() },
        },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]), bancas: () => of([]) } },
        { provide: AuthStore, useValue: { perfil: () => 'ADMIN' } },
      ],
    });
    const fixture = TestBed.createComponent(QuestoesPage);
    await fixture.whenStable();
    fixture.detectChanges();

    const botaoFiltrar = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Filtrar',
    ) as HTMLButtonElement;
    botaoFiltrar.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(listar).toHaveBeenCalled();

    const botaoDespublicar = fixture.nativeElement.querySelector(
      'button[title="Despublicar"]',
    ) as HTMLButtonElement;
    botaoDespublicar.click();
    fixture.detectChanges();

    const botaoCancelar = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Cancelar',
    ) as HTMLButtonElement;
    botaoCancelar.click();
    fixture.detectChanges();

    expect(despublicar).not.toHaveBeenCalled();
    expect(fixture.componentInstance.confirmacao()).toBeNull();
  });
});
