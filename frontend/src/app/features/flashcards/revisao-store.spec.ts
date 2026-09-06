import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RevisaoApi } from '../../core/api/revisao-api';
import {
  CardRevisao,
  RespostaRevisao,
  RespostaSessaoRevisao,
  ResumoSessao,
} from '../../core/api/modelos-revisao';
import { RevisaoStore } from './revisao-store';

function card(overrides: Partial<CardRevisao> = {}): CardRevisao {
  return {
    id: 'c1',
    frente: 'Frente',
    verso: 'Verso',
    deckId: 'd1',
    deckNome: 'Constitucional',
    disciplinaNome: 'Direito Constitucional',
    assuntoNome: 'Controle',
    estado: 'REVISAO',
    memoria: {
      estabilidade: 8.4,
      dificuldade: 5.1,
      retencaoAtual: 0.87,
      rotulo: 'Estabilidade 8 dias',
    },
    previsoes: [
      { nota: 'ERREI', minutos: 10, rotulo: '10 min' },
      { nota: 'DIFICIL', minutos: 8640, rotulo: '6 dias' },
      { nota: 'BOM', minutos: 12960, rotulo: '9 dias' },
      { nota: 'FACIL', minutos: 25920, rotulo: '18 dias' },
    ],
    ...overrides,
  };
}

function respostaAbertura(overrides: Partial<RespostaSessaoRevisao> = {}): RespostaSessaoRevisao {
  return {
    id: 's1',
    iniciadaEm: '2026-08-30T13:02:11Z',
    escopoRotulo: 'Todos os decks',
    total: 2,
    fila: [card(), card({ id: 'c2' })],
    ...overrides,
  };
}

describe('RevisaoStore', () => {
  let store: RevisaoStore;
  let api: {
    abrir: ReturnType<typeof vi.fn>;
    registrar: ReturnType<typeof vi.fn>;
    finalizar: ReturnType<typeof vi.fn>;
    resumo: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    api = { abrir: vi.fn(), registrar: vi.fn(), finalizar: vi.fn(), resumo: vi.fn() };
    TestBed.configureTestingModule({
      providers: [RevisaoStore, { provide: RevisaoApi, useValue: api }],
    });
    store = TestBed.inject(RevisaoStore);
  });

  it('inicia cada card sem o verso revelado', async () => {
    api.abrir.mockReturnValue(of(respostaAbertura()));

    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });

    expect(store.revelado()).toBe(false);
    expect(store.cardAtual()?.id).toBe('c1');
  });

  it('revelar expõe verso e classificações', async () => {
    api.abrir.mockReturnValue(of(respostaAbertura()));
    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });

    store.revelar();

    expect(store.revelado()).toBe(true);
  });

  it('classificar avança para o próximo card', async () => {
    api.abrir.mockReturnValue(of(respostaAbertura()));
    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });
    const resposta: RespostaRevisao = {
      flashcardId: 'c1',
      estado: 'REVISAO',
      estabilidade: 21.63,
      dificuldade: 4.92,
      intervaloMinutos: 12960,
      intervaloRotulo: '9 dias',
      proximaRevisaoEm: '2026-09-08T03:00:00Z',
      reenfileirar: false,
      card: null,
    };
    api.registrar.mockReturnValue(of(resposta));
    store.revelar();

    await store.classificar('BOM');

    expect(store.cardAtual()?.id).toBe('c2');
    expect(store.revelado()).toBe(false);
  });

  it('reenfileira o card quando o servidor manda', async () => {
    api.abrir.mockReturnValue(of(respostaAbertura({ total: 1, fila: [card()] })));
    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });
    const cardReenfileirado = card({ estado: 'REAPRENDENDO' });
    const resposta: RespostaRevisao = {
      flashcardId: 'c1',
      estado: 'REAPRENDENDO',
      estabilidade: 2.71,
      dificuldade: 6.14,
      intervaloMinutos: 10,
      intervaloRotulo: '10 min',
      proximaRevisaoEm: '2026-08-30T13:12:11Z',
      reenfileirar: true,
      card: cardReenfileirado,
    };
    api.registrar.mockReturnValue(of(resposta));
    store.revelar();

    await store.classificar('ERREI');

    expect(store.cardAtual()?.estado).toBe('REAPRENDENDO');
    expect(store.restantes()).toBe(1);
    expect(store.classificacoes()).toBe(1);
    expect(store.reenfileirado()).toBe('10 min');

    store.revelar();

    expect(store.reenfileirado()).toBeNull();
  });

  it('não sinaliza reenfileiramento quando o card sai da fila', async () => {
    api.abrir.mockReturnValue(of(respostaAbertura()));
    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });
    api.registrar.mockReturnValue(
      of({
        flashcardId: 'c1',
        estado: 'REVISAO',
        estabilidade: 12.4,
        dificuldade: 4.9,
        intervaloMinutos: 25920,
        intervaloRotulo: '18 dias',
        proximaRevisaoEm: '2026-09-17T13:02:11Z',
        reenfileirar: false,
        card: null,
      } satisfies RespostaRevisao),
    );
    store.revelar();

    await store.classificar('FACIL');

    expect(store.reenfileirado()).toBeNull();
    expect(store.classificacoes()).toBe(1);
    expect(store.restantes()).toBe(1);
  });

  it('não reapresenta o card reenfileirado antes de vencer e encerra a sessão', async () => {
    api.abrir.mockReturnValue(of(respostaAbertura()));
    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });
    api.finalizar.mockReturnValue(
      of({
        revisados: 2,
        classificacoes: 2,
        distribuicao: { errei: 1, dificil: 0, bom: 0, facil: 1 },
        tempoMs: 4200,
        tempoRotulo: '0 min 4 s',
        proximosVencimentos: [],
      } satisfies ResumoSessao),
    );
    api.registrar.mockReturnValue(
      of({
        flashcardId: 'c1',
        estado: 'REAPRENDENDO',
        estabilidade: 2.71,
        dificuldade: 6.14,
        intervaloMinutos: 10,
        intervaloRotulo: '10 min',
        proximaRevisaoEm: new Date(Date.now() + 10 * 60_000).toISOString(),
        reenfileirar: true,
        card: card({ estado: 'REAPRENDENDO' }),
      } satisfies RespostaRevisao),
    );
    store.revelar();
    await store.classificar('ERREI');

    expect(store.cardAtual()?.id).toBe('c2');
    expect(store.restantes()).toBe(1);
    expect(store.adiados()).toBe(1);

    api.registrar.mockReturnValue(
      of({
        flashcardId: 'c2',
        estado: 'REVISAO',
        estabilidade: 12.4,
        dificuldade: 4.9,
        intervaloMinutos: 25920,
        intervaloRotulo: '18 dias',
        proximaRevisaoEm: new Date(Date.now() + 18 * 86_400_000).toISOString(),
        reenfileirar: false,
        card: null,
      } satisfies RespostaRevisao),
    );
    store.revelar();
    await store.classificar('FACIL');

    expect(api.finalizar).toHaveBeenCalledWith('s1');
    expect(store.resumo()).not.toBeNull();
    expect(store.adiados()).toBe(1);
  });

  it('reapresenta o card reenfileirado depois que o intervalo vence', async () => {
    api.abrir.mockReturnValue(of(respostaAbertura({ total: 1, fila: [card()] })));
    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });
    api.registrar.mockReturnValue(
      of({
        flashcardId: 'c1',
        estado: 'REAPRENDENDO',
        estabilidade: 2.71,
        dificuldade: 6.14,
        intervaloMinutos: 10,
        intervaloRotulo: '10 min',
        proximaRevisaoEm: new Date(Date.now() - 60_000).toISOString(),
        reenfileirar: true,
        card: card({ estado: 'REAPRENDENDO' }),
      } satisfies RespostaRevisao),
    );
    store.revelar();
    await store.classificar('ERREI');

    expect(store.cardAtual()?.estado).toBe('REAPRENDENDO');
    expect(store.adiados()).toBe(0);
    expect(api.finalizar).not.toHaveBeenCalled();
  });

  it('insere o card reenfileirado antes de outro cuja próxima revisão é mais distante', async () => {
    const respostaReenfileira = (id: string, proximaRevisaoEm: string): RespostaRevisao => ({
      flashcardId: id,
      estado: 'REAPRENDENDO',
      estabilidade: 2.71,
      dificuldade: 6.14,
      intervaloMinutos: 10,
      intervaloRotulo: '10 min',
      proximaRevisaoEm,
      reenfileirar: true,
      card: card({ id, estado: 'REAPRENDENDO' }),
    });
    api.abrir.mockReturnValue(
      of(
        respostaAbertura({
          total: 3,
          fila: [card({ id: 'c1' }), card({ id: 'c2' }), card({ id: 'c3' })],
        }),
      ),
    );
    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });
    api.registrar.mockReturnValue(of(respostaReenfileira('c1', '2026-08-30T13:32:11Z')));
    store.revelar();
    await store.classificar('ERREI');
    api.registrar.mockReturnValue(of(respostaReenfileira('c2', '2026-08-30T13:12:11Z')));
    store.revelar();
    await store.classificar('ERREI');

    expect(store.cardAtual()?.id).toBe('c3');

    api.registrar.mockReturnValue(
      of({ ...respostaReenfileira('c3', ''), reenfileirar: false, card: null }),
    );
    store.revelar();
    await store.classificar('BOM');

    expect(store.cardAtual()?.id).toBe('c2');
  });

  it('sinaliza fila vazia sem oferecer antecipação', async () => {
    api.abrir.mockReturnValue(of(respostaAbertura({ total: 0, fila: [] })));

    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });

    expect(store.filaVazia()).toBe(true);
    expect(store.cardAtual()).toBeNull();
  });

  it('finalizar expõe o resumo devolvido', async () => {
    api.abrir.mockReturnValue(of(respostaAbertura({ total: 1, fila: [card()] })));
    await store.abrir({ escopo: 'TODOS', deckId: null, disciplinaId: null });
    const resumo: ResumoSessao = {
      revisados: 1,
      classificacoes: 1,
      distribuicao: { errei: 0, dificil: 0, bom: 1, facil: 0 },
      tempoMs: 4200,
      tempoRotulo: '0 min 4 s',
      proximosVencimentos: [
        { faixa: 'AMANHA', rotulo: 'Amanhã', quantidade: 1 },
        { faixa: 'PROXIMOS_7_DIAS', rotulo: 'Nos próximos 7 dias', quantidade: 2 },
        { faixa: 'DEPOIS_DE_7_DIAS', rotulo: 'Depois de 7 dias', quantidade: 3 },
      ],
    };
    api.finalizar.mockReturnValue(of(resumo));

    await store.finalizar();

    expect(store.resumo()).toEqual(resumo);
  });
});
