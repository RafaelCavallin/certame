import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CatalogoApi } from '../../../core/api/catalogo-api';
import { FlashcardApi } from '../../../core/api/flashcard-api';
import { FlashcardResumo, PaginaFlashcards } from '../../../core/api/modelos-flashcard';
import { DialogService } from '../../../shared/ui/dialog/dialog-service';
import { FlashcardsPage } from './flashcards-page';

const card = (alteracoes: Partial<FlashcardResumo> = {}): FlashcardResumo => ({
  id: 'f1',
  frente: 'Efeitos da decisão de mérito em ADI',
  verso: 'Eficácia erga omnes.',
  deck: null,
  disciplina: null,
  assunto: null,
  tags: [],
  estado: 'APRENDENDO',
  status: 'ATIVO',
  proximaRevisao: '2026-08-30',
  origem: null,
  criadoEm: '2026-08-30T00:00:00Z',
  ...alteracoes,
});

const pagina = (alteracoes: Partial<PaginaFlashcards> = {}): PaginaFlashcards => ({
  content: [card()],
  page: 0,
  size: 20,
  totalElements: 1,
  indicadores: { vencendoHoje: 1, proximosSeteDias: 2, emFixacao: 1, total: 1, decks: 0 },
  ...alteracoes,
});

function raiz(fixture: ComponentFixture<FlashcardsPage>): HTMLElement {
  return fixture.nativeElement as HTMLElement;
}

describe('FlashcardsPage', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let dialogService: { abrir: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    api = {
      listar: vi.fn(() => of(pagina())),
      listarDecks: vi.fn(() => of([])),
      arquivar: vi.fn(),
      restaurar: vi.fn(),
      excluir: vi.fn(() => of(undefined)),
      moverParaDeck: vi.fn(),
    };
    dialogService = { abrir: vi.fn(() => of(undefined)) };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({}), paramMap: convertToParamMap({}) },
            queryParamMap: of(convertToParamMap({})),
          },
        },
        { provide: FlashcardApi, useValue: api },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]) } },
        { provide: DialogService, useValue: dialogService },
      ],
    });
  });

  it('TU-43 exibe o estado do card por rótulo textual, não só por cor', async () => {
    const fixture = TestBed.createComponent(FlashcardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(raiz(fixture).textContent).toContain('Aprendendo');
  });

  it('mostra o estado de boas-vindas quando o acervo inteiro está vazio', async () => {
    api['listar'].mockReturnValue(
      of(
        pagina({
          content: [],
          totalElements: 0,
          indicadores: { vencendoHoje: 0, proximosSeteDias: 0, emFixacao: 0, total: 0, decks: 0 },
        }),
      ),
    );
    const fixture = TestBed.createComponent(FlashcardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(raiz(fixture).textContent).toContain('Você ainda não tem flashcards');
  });

  it('mostra o estado vazio de filtro quando há acervo mas o filtro não encontra nada', async () => {
    api['listar'].mockReturnValue(of(pagina({ content: [], totalElements: 0 })));
    const fixture = TestBed.createComponent(FlashcardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(raiz(fixture).textContent).toContain('Nenhum flashcard com esses filtros');
  });

  it('editar abre o diálogo com os dados da linha, sem nova leitura', async () => {
    const fixture = TestBed.createComponent(FlashcardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    raiz(fixture).querySelector<HTMLButtonElement>('[title="Editar"]')!.click();

    expect(dialogService.abrir).toHaveBeenCalledWith(expect.anything(), {
      contexto: { flashcardParaEditar: expect.objectContaining({ id: 'f1' }) },
    });
  });

  it('exclusão pede confirmação antes de chamar a API', async () => {
    const fixture = TestBed.createComponent(FlashcardsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    raiz(fixture).querySelector<HTMLButtonElement>('[title="Excluir"]')!.click();
    fixture.detectChanges();
    expect(api['excluir']).not.toHaveBeenCalled();

    raiz(fixture).querySelector<HTMLButtonElement>('.dialog-backdrop .btn-primary')!.click();

    await vi.waitFor(() => expect(api['excluir']).toHaveBeenCalledWith('f1'));
  });

  it('abrirNovo abre o diálogo de criação sem contexto', () => {
    const fixture = TestBed.createComponent(FlashcardsPage);

    fixture.componentInstance.abrirNovo();

    expect(dialogService.abrir).toHaveBeenCalledWith(expect.anything(), { contexto: null });
  });

  it('arquivar e restaurar delegam ao store pelo id do card', () => {
    const fixture = TestBed.createComponent(FlashcardsPage);
    const arquivar = vi
      .spyOn(fixture.componentInstance.store, 'arquivar')
      .mockResolvedValue(undefined);
    const restaurar = vi
      .spyOn(fixture.componentInstance.store, 'restaurar')
      .mockResolvedValue(undefined);

    fixture.componentInstance.arquivar('f1');
    fixture.componentInstance.restaurar('f1');

    expect(arquivar).toHaveBeenCalledWith('f1');
    expect(restaurar).toHaveBeenCalledWith('f1');
  });

  it('mantém as disciplinas e os decks do filtro vazios quando as leituras iniciais falham', async () => {
    const catalogo = { disciplinas: () => throwError(() => new Error('falha')) };
    api['listarDecks'] = vi.fn(() => throwError(() => new Error('falha')));
    TestBed.overrideProvider(CatalogoApi, { useValue: catalogo });
    TestBed.overrideProvider(FlashcardApi, { useValue: api });

    const fixture = TestBed.createComponent(FlashcardsPage);
    await Promise.resolve();
    await Promise.resolve();

    expect(fixture.componentInstance.disciplinas()).toEqual([]);
    expect(fixture.componentInstance.decksFiltro()).toEqual([]);
  });
});
