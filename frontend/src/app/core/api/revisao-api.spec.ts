import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { RevisaoApi } from './revisao-api';

const base = '/api/v1/flashcards';

describe('RevisaoApi', () => {
  let api: RevisaoApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RevisaoApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(RevisaoApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('abre a sessão enviando o escopo escolhido', () => {
    api.abrir({ escopo: 'DECK', deckId: 'd1', disciplinaId: null }).subscribe();

    const abertura = http.expectOne(`${base}/sessoes`);
    expect(abertura.request.method).toBe('POST');
    expect(abertura.request.body).toEqual({ escopo: 'DECK', deckId: 'd1', disciplinaId: null });
    abertura.flush({
      id: 's1',
      iniciadaEm: '2026-08-30T13:02:11Z',
      escopoRotulo: 'Todos os decks',
      total: 0,
      fila: [],
    });
  });

  it('registra a revisão do card na sessão informada', () => {
    api.registrar('s1', { flashcardId: 'f1', nota: 'BOM', tempoMs: 4200 }).subscribe();

    const registro = http.expectOne(`${base}/sessoes/s1/revisoes`);
    expect(registro.request.method).toBe('POST');
    expect(registro.request.body).toEqual({ flashcardId: 'f1', nota: 'BOM', tempoMs: 4200 });
    registro.flush(null);
  });

  it('finaliza a sessão sem corpo', () => {
    api.finalizar('s1').subscribe();

    const finalizacao = http.expectOne(`${base}/sessoes/s1/finalizar`);
    expect(finalizacao.request.method).toBe('POST');
    expect(finalizacao.request.body).toBeNull();
    finalizacao.flush(null);
  });

  it('busca o resumo do usuário', () => {
    api.resumo().subscribe();

    const resumo = http.expectOne(`${base}/resumo`);
    expect(resumo.request.method).toBe('GET');
    resumo.flush({ vencendoHoje: 0, proximos7Dias: 0, novosDisponiveis: 0, porDeck: [] });
  });
});
