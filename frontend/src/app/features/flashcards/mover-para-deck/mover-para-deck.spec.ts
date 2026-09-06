import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FlashcardApi } from '../../../core/api/flashcard-api';
import { PaginaFlashcards } from '../../../core/api/modelos-flashcard';
import { FlashcardsStore } from '../flashcards-store';
import { MoverParaDeck } from './mover-para-deck';

const paginaVazia: PaginaFlashcards = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  indicadores: { vencendoHoje: 0, proximosSeteDias: 0, emFixacao: 0, total: 0, decks: 0 },
};

describe('MoverParaDeck', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    api = {
      listarDecks: vi.fn(() =>
        of([{ id: 'd2', nome: 'Administrativo', descricao: null, totalCards: 3 }]),
      ),
      moverParaDeck: vi.fn(() => of({ movidos: 2 })),
      listar: vi.fn(() => of(paginaVazia)),
    };
    TestBed.configureTestingModule({
      providers: [FlashcardsStore, MoverParaDeck, { provide: FlashcardApi, useValue: api }],
    });
  });

  it('TU-44 fica desabilitado com seleção vazia mesmo com destino escolhido', () => {
    const acao = TestBed.inject(MoverParaDeck);
    acao.destino.set('d2');
    expect(acao.desabilitado()).toBe(true);
  });

  it('TU-44 fica desabilitado sem destino escolhido mesmo com seleção', () => {
    const acao = TestBed.inject(MoverParaDeck);
    TestBed.inject(FlashcardsStore).selecionados.set(['f1', 'f2']);
    expect(acao.desabilitado()).toBe(true);
  });

  it('TU-44 envia exatamente os ids marcados e limpa a seleção após mover', async () => {
    const store = TestBed.inject(FlashcardsStore);
    const acao = TestBed.inject(MoverParaDeck);
    store.selecionados.set(['f1', 'f2', 'f3']);
    acao.destino.set('d2');

    expect(acao.desabilitado()).toBe(false);
    await acao.mover();

    expect(api['moverParaDeck']).toHaveBeenCalledWith('d2', { flashcardIds: ['f1', 'f2', 'f3'] });
    expect(store.selecionados()).toEqual([]);
    expect(acao.destino()).toBeNull();
  });
});
