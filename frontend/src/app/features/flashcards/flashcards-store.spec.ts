import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { FlashcardApi } from '../../core/api/flashcard-api';
import { FlashcardResumo, PaginaFlashcards } from '../../core/api/modelos-flashcard';
import { FlashcardsStore } from './flashcards-store';

const card = (alteracoes: Partial<FlashcardResumo> = {}): FlashcardResumo => ({
  id: 'f1',
  frente: 'Frente',
  verso: 'Verso',
  deck: null,
  disciplina: null,
  assunto: null,
  tags: [],
  estado: 'NOVO',
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
  indicadores: { vencendoHoje: 1, proximosSeteDias: 2, emFixacao: 1, total: 1, decks: 1 },
  ...alteracoes,
});

describe('FlashcardsStore', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    vi.useFakeTimers();
    api = {
      listar: vi.fn(() => of(pagina())),
      arquivar: vi.fn(() => of(undefined)),
      restaurar: vi.fn(() => of(undefined)),
      excluir: vi.fn(() => of(undefined)),
      moverParaDeck: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [FlashcardsStore, { provide: FlashcardApi, useValue: api }],
    });
  });

  afterEach(() => vi.useRealTimers());

  it('TU-41 combina busca e filtros numa única requisição, com a digitação usando temporizador falso', async () => {
    const store = TestBed.inject(FlashcardsStore);
    await store.carregar();
    store.filtrar({ deck: 'd1' }, false);
    api['listar'].mockClear();

    store.filtrar({ q: 'controle' }, true);
    expect(api['listar']).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(400);

    expect(api['listar']).toHaveBeenCalledTimes(1);
    expect(api['listar']).toHaveBeenCalledWith(
      expect.objectContaining({ q: 'controle', deck: 'd1', page: 0 }),
    );
  });

  it('TU-42 distingue acervo vazio de filtro vazio pelos indicadores', async () => {
    const store = TestBed.inject(FlashcardsStore);
    api['listar'].mockReturnValue(
      of(
        pagina({
          content: [],
          totalElements: 0,
          indicadores: { vencendoHoje: 0, proximosSeteDias: 0, emFixacao: 0, total: 0, decks: 0 },
        }),
      ),
    );
    await store.carregar();

    expect(store.acervoVazio()).toBe(true);
    expect(store.vazio()).toBe(false);

    api['listar'].mockReturnValue(of(pagina({ content: [], totalElements: 0 })));
    await store.carregar();

    expect(store.acervoVazio()).toBe(false);
    expect(store.vazio()).toBe(true);
  });

  it('arquivar recarrega apenas a página corrente e limpa a seleção', async () => {
    const store = TestBed.inject(FlashcardsStore);
    await store.carregar();
    store.selecionados.set(['f1']);
    api['listar'].mockClear();

    await store.arquivar('f1');

    expect(api['arquivar']).toHaveBeenCalledWith('f1');
    expect(api['listar']).toHaveBeenCalledTimes(1);
    expect(store.selecionados()).toEqual([]);
  });

  it('moverSelecionados envia os ids marcados e não chama a API com seleção vazia', async () => {
    const store = TestBed.inject(FlashcardsStore);
    api['moverParaDeck'].mockReturnValue(of({ movidos: 0 }));
    await store.moverSelecionados('d2');
    expect(api['moverParaDeck']).not.toHaveBeenCalled();

    store.selecionados.set(['f1', 'f2']);
    api['moverParaDeck'].mockReturnValue(of({ movidos: 2 }));
    await store.moverSelecionados('d2');

    expect(api['moverParaDeck']).toHaveBeenCalledWith('d2', { flashcardIds: ['f1', 'f2'] });
    expect(store.selecionados()).toEqual([]);
  });

  it('irPara troca de página e nunca aceita página negativa', async () => {
    const store = TestBed.inject(FlashcardsStore);
    store.irPara(-3);
    await Promise.resolve();

    expect(api['listar']).toHaveBeenCalledWith(expect.objectContaining({ page: 0 }));
  });

  it('restaurar chama a API e recarrega a página corrente', async () => {
    const store = TestBed.inject(FlashcardsStore);
    await store.restaurar('f1');
    expect(api['restaurar']).toHaveBeenCalledWith('f1');
  });

  it('alternarSelecao adiciona e depois remove o id marcado', () => {
    const store = TestBed.inject(FlashcardsStore);
    store.alternarSelecao('f1');
    expect(store.selecionados()).toEqual(['f1']);
    store.alternarSelecao('f1');
    expect(store.selecionados()).toEqual([]);
  });

  it('alternarTodos marca todos os cards da página e depois limpa a seleção', async () => {
    const store = TestBed.inject(FlashcardsStore);
    api['listar'].mockReturnValue(
      of(pagina({ content: [card({ id: 'f1' }), card({ id: 'f2' })] })),
    );
    await store.carregar();

    store.alternarTodos();
    expect(store.selecionados()).toEqual(['f1', 'f2']);
    store.alternarTodos();
    expect(store.selecionados()).toEqual([]);
  });

  it('registra a mensagem de erro quando arquivar falha', async () => {
    const store = TestBed.inject(FlashcardsStore);
    api['arquivar'].mockReturnValue(
      throwError(() => ({
        error: { code: 'CONFLITO', message: 'Não foi possível arquivar.', fieldErrors: null },
      })),
    );

    await store.arquivar('f1');

    expect(store.erro()).toBe('Não foi possível arquivar.');
  });

  it('registra a mensagem de erro quando mover para deck falha', async () => {
    const store = TestBed.inject(FlashcardsStore);
    api['moverParaDeck'].mockReturnValue(throwError(() => new Error('falha de rede')));
    store.selecionados.set(['f1']);

    await store.moverSelecionados('d2');

    expect(store.erro()).toBe('Não foi possível concluir a operação. Tente novamente.');
  });
});
