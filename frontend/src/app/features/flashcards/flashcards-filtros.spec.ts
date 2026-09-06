import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { FlashcardApi } from '../../core/api/flashcard-api';
import { FlashcardsFiltros } from './flashcards-filtros';
import { FlashcardsStore } from './flashcards-store';

const paginaVazia = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  indicadores: { vencendoHoje: 0, proximosSeteDias: 0, emFixacao: 0, total: 0, decks: 0 },
};

function configurar(params: Record<string, string> = {}) {
  const paramMap$ = new BehaviorSubject(convertToParamMap(params));
  const api = { listar: vi.fn(() => of(paginaVazia)) };
  TestBed.configureTestingModule({
    providers: [
      FlashcardsFiltros,
      FlashcardsStore,
      provideRouter([]),
      { provide: FlashcardApi, useValue: api },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: paramMap$.value }, queryParamMap: paramMap$ },
      },
    ],
  });
  const router = TestBed.inject(Router);
  vi.spyOn(router, 'navigate').mockResolvedValue(true);
  return { paramMap$, api, router };
}

describe('FlashcardsFiltros', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('restaura os filtros a partir dos query params e carrega a listagem uma única vez', () => {
    const { api } = configurar({
      q: 'controle',
      deck: 'd1',
      disciplina: 'x1',
      estado: 'NOVO',
      arquivados: 'true',
      sort: 'proximaRevisao,asc',
    });
    TestBed.inject(FlashcardsFiltros);

    expect(api['listar']).toHaveBeenCalledTimes(1);
    const store = TestBed.inject(FlashcardsStore);
    expect(store.filtro()).toMatchObject({
      q: 'controle',
      deck: 'd1',
      disciplina: 'x1',
      estado: 'NOVO',
      arquivados: true,
      sort: 'proximaRevisao,asc',
    });
  });

  it('buscar usa temporizador falso e só requisita depois do atraso, combinando com os outros filtros', async () => {
    const { api } = configurar();
    const filtros = TestBed.inject(FlashcardsFiltros);
    api['listar'].mockClear();
    filtros.filtrarDeck('d1');
    api['listar'].mockClear();

    filtros.buscar('controle');
    expect(api['listar']).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(400);

    expect(api['listar']).toHaveBeenCalledWith(
      expect.objectContaining({ q: 'controle', deck: 'd1' }),
    );
  });

  it.each([
    ['filtrarDeck', 'd2', { deck: 'd2' }],
    ['filtrarDisciplina', 'x2', { disciplina: 'x2' }],
    ['filtrarEstado', 'REVISAO', { estado: 'REVISAO' }],
    ['ordenar', 'proximaRevisao,desc', { sort: 'proximaRevisao,desc' }],
  ] as const)('%s aplica o filtro imediatamente, sem atraso', async (metodo, valor, esperado) => {
    const { api } = configurar();
    const filtros = TestBed.inject(FlashcardsFiltros);
    api['listar'].mockClear();

    (filtros[metodo] as (v: string) => void)(valor);

    expect(api['listar']).toHaveBeenCalledWith(expect.objectContaining(esperado));
  });

  it('alternarArquivados troca entre ativos e arquivados imediatamente', () => {
    const { api } = configurar();
    const filtros = TestBed.inject(FlashcardsFiltros);
    api['listar'].mockClear();

    filtros.alternarArquivados(true);

    expect(api['listar']).toHaveBeenCalledWith(expect.objectContaining({ arquivados: true }));
  });

  it('uma navegação externa (voltar do navegador) recarrega a listagem com os novos parâmetros', async () => {
    const { paramMap$, api } = configurar();
    TestBed.inject(FlashcardsFiltros);
    api['listar'].mockClear();

    paramMap$.next(convertToParamMap({ q: 'nova busca' }));

    expect(api['listar']).toHaveBeenCalledWith(expect.objectContaining({ q: 'nova busca' }));
  });
});
