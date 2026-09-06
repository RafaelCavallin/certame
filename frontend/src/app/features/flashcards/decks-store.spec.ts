import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { FlashcardApi } from '../../core/api/flashcard-api';
import { DeckDetalhe } from '../../core/api/modelos-flashcard';
import { DecksStore } from './decks-store';

function deckDetalhe(overrides: Partial<DeckDetalhe> = {}): DeckDetalhe {
  return {
    id: 'd1',
    nome: 'Constitucional — controle',
    descricao: 'ADI, ADC, ADPF',
    totalCards: 214,
    vencendoHoje: 137,
    novos: 12,
    limites: { novosDia: 10, revisoesDia: 60, intervaloMaxDias: 180, retencaoAlvo: 0.9 },
    limitesRotulo: '10 novos/dia · 60 revisões/dia · máx. 180 d',
    retencaoRotulo: 'FSRS · retenção alvo 90%',
    editavel: true,
    aviso: null,
    ...overrides,
  };
}

describe('DecksStore', () => {
  let store: DecksStore;
  let api: {
    listarDecks: ReturnType<typeof vi.fn>;
    criarDeck: ReturnType<typeof vi.fn>;
    atualizarDeck: ReturnType<typeof vi.fn>;
    excluirDeck: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    api = {
      listarDecks: vi.fn(() => of([deckDetalhe()])),
      criarDeck: vi.fn(),
      atualizarDeck: vi.fn(),
      excluirDeck: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [DecksStore, { provide: FlashcardApi, useValue: api }],
    });
    store = TestBed.inject(DecksStore);
  });

  it('carrega os decks do usuário e encerra o carregando', async () => {
    await store.carregar();

    expect(store.decks()).toHaveLength(1);
    expect(store.decks()[0].nome).toBe('Constitucional — controle');
    expect(store.carregando()).toBe(false);
  });

  it('mapeia o 409 de nome em uso para o campo nome', async () => {
    api.atualizarDeck.mockReturnValue(
      throwError(() => ({
        error: {
          code: 'NOME_EM_USO',
          message: 'Já existe um deck com esse nome.',
          fieldErrors: { nome: 'Já existe um deck com esse nome.' },
        },
      })),
    );
    await store.carregar();

    await store.salvar({ nome: 'Constitucional — controle' }, 'd1');

    expect(store.erroDeCampo().nome).toBe('Já existe um deck com esse nome.');
  });

  it('reflete no deck local os limites ajustados pelo servidor ao salvar', async () => {
    const ajustado = deckDetalhe({
      limites: { novosDia: 10, revisoesDia: 400, intervaloMaxDias: 180, retencaoAlvo: 0.9 },
      limitesRotulo: '10 novos/dia · 400 revisões/dia · máx. 180 d',
    });
    api.atualizarDeck.mockReturnValue(of(ajustado));
    await store.carregar();

    await store.salvar({ nome: 'Constitucional — controle', revisoesDia: 500 }, 'd1');

    expect(store.decks()[0].limites.revisoesDia).toBe(400);
    expect(store.decks()[0].limitesRotulo).toBe('10 novos/dia · 400 revisões/dia · máx. 180 d');
    expect(store.erroDeCampo().nome).toBeNull();
  });

  it('cria o deck e recarrega a lista quando nenhum id é informado', async () => {
    api.criarDeck.mockReturnValue(
      of({ id: 'd2', nome: 'Administrativo', descricao: null, totalCards: 0 }),
    );

    await store.salvar({ nome: 'Administrativo' });

    expect(api.criarDeck).toHaveBeenCalledWith({ nome: 'Administrativo' });
    expect(api.listarDecks).toHaveBeenCalledTimes(1);
    expect(store.erroDeCampo().nome).toBeNull();
  });

  it('recarrega a lista após excluir e devolve os cards movidos', async () => {
    api.excluirDeck.mockReturnValue(of({ cardsMovidos: 200 }));
    await store.carregar();

    const cardsMovidos = await store.excluir('d1');

    expect(cardsMovidos).toBe(200);
    expect(api.listarDecks).toHaveBeenCalledTimes(2);
  });
});
