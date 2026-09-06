import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { FlashcardApi } from './flashcard-api';
import { RequisicaoFlashcard } from './modelos-flashcard';

const base = '/api/v1/flashcards';
const requisicao: RequisicaoFlashcard = {
  frente: 'Efeitos da decisão de mérito em ADI',
  verso: 'Eficácia erga omnes.',
  deckId: null,
  disciplinaId: null,
  assuntoId: null,
  tags: ['adi'],
  questaoOrigemId: 'q1',
  sessaoOrigemId: 's1',
};

describe('FlashcardApi', () => {
  let api: FlashcardApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [FlashcardApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(FlashcardApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('cria o flashcard enviando o corpo da requisição', () => {
    api.criar(requisicao).subscribe();

    const criacao = http.expectOne(base);
    expect(criacao.request.method).toBe('POST');
    expect(criacao.request.body).toEqual(requisicao);
    criacao.flush({ flashcard: { id: 'f1' }, criadosNaSessao: 1 });
  });

  it('descarta filtros nulos, vazios e indefinidos ao montar a query da listagem', () => {
    api.listar({ q: 'controle', deck: '', estado: undefined, page: 0, size: 20 }).subscribe();

    const requisicaoHttp = http.expectOne((r) => r.url === base);
    expect(requisicaoHttp.request.params.keys().sort()).toEqual(['page', 'q', 'size']);
    requisicaoHttp.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      indicadores: { vencendoHoje: 0, proximosSeteDias: 0, emFixacao: 0, total: 0, decks: 0 },
    });
  });

  it('busca o card pelo identificador', () => {
    api.buscar('f1').subscribe();

    const leitura = http.expectOne(`${base}/f1`);
    expect(leitura.request.method).toBe('GET');
    leitura.flush(null);
  });

  it('atualiza o card pelo identificador', () => {
    api.atualizar('f1', requisicao).subscribe();

    const atualizacao = http.expectOne(`${base}/f1`);
    expect(atualizacao.request.method).toBe('PUT');
    expect(atualizacao.request.body).toEqual(requisicao);
    atualizacao.flush(null);
  });

  it('exclui o card pelo identificador', () => {
    api.excluir('f1').subscribe();

    const exclusao = http.expectOne(`${base}/f1`);
    expect(exclusao.request.method).toBe('DELETE');
    exclusao.flush(null);
  });

  it('envia as transições de arquivar e restaurar sem corpo', () => {
    api.arquivar('f1').subscribe();
    api.restaurar('f1').subscribe();

    const transicoes = http.match((r) => r.method === 'POST' && r.url !== base);
    expect(transicoes.map((r) => r.request.url)).toEqual([
      `${base}/f1/arquivar`,
      `${base}/f1/restaurar`,
    ]);
    transicoes.forEach((r) => r.flush(null));
  });

  it('lista os decks do usuário com os detalhes enriquecidos', () => {
    api.listarDecks().subscribe();

    const listagem = http.expectOne(`${base}/decks`);
    expect(listagem.request.method).toBe('GET');
    listagem.flush([]);
  });

  it('cria um deck pelo nome', () => {
    api.criarDeck({ nome: 'Constitucional' }).subscribe();

    const criacao = http.expectOne(`${base}/decks`);
    expect(criacao.request.method).toBe('POST');
    expect(criacao.request.body).toEqual({ nome: 'Constitucional' });
    criacao.flush({ id: 'd1', nome: 'Constitucional', descricao: null, totalCards: 0 });
  });

  it('atualiza o deck pelo identificador enviando nome, descrição e limites', () => {
    const requisicaoDeck = {
      nome: 'Constitucional — controle',
      descricao: 'ADI, ADC, ADPF',
      revisoesDia: 500,
    };
    api.atualizarDeck('d1', requisicaoDeck).subscribe();

    const atualizacao = http.expectOne(`${base}/decks/d1`);
    expect(atualizacao.request.method).toBe('PUT');
    expect(atualizacao.request.body).toEqual(requisicaoDeck);
    atualizacao.flush({
      id: 'd1',
      nome: 'Constitucional — controle',
      descricao: 'ADI, ADC, ADPF',
      totalCards: 214,
      vencendoHoje: 137,
      novos: 12,
      limites: { novosDia: 10, revisoesDia: 400, intervaloMaxDias: 180, retencaoAlvo: 0.9 },
      limitesRotulo: '10 novos/dia · 400 revisões/dia · máx. 180 d',
      retencaoRotulo: 'FSRS · retenção alvo 90%',
      editavel: true,
      aviso: null,
    });
  });

  it('exclui o deck pelo identificador', () => {
    api.excluirDeck('d1').subscribe();

    const exclusao = http.expectOne(`${base}/decks/d1`);
    expect(exclusao.request.method).toBe('DELETE');
    exclusao.flush({ cardsMovidos: 200 });
  });

  it('move o lote de cards para o deck informado', () => {
    api.moverParaDeck('d1', { flashcardIds: ['f1', 'f2'] }).subscribe();

    const mover = http.expectOne(`${base}/decks/d1/mover`);
    expect(mover.request.method).toBe('POST');
    expect(mover.request.body).toEqual({ flashcardIds: ['f1', 'f2'] });
    mover.flush({ movidos: 2 });
  });
});
