import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { QuestaoApi } from './questao-api';
import { RequisicaoQuestao } from './modelos-questao';

const base = '/api/v1/admin/questoes';
const requisicao: RequisicaoQuestao = {
  tipo: 'ME',
  enunciado: 'Enunciado com mais de dez caracteres.',
  disciplinaId: 'd1',
  assuntoId: 'a1',
  bancaId: 'b1',
  orgaoId: null,
  ano: 2026,
  dificuldade: 'MEDIA',
  alternativas: [{ letra: null, texto: 'Primeira', ordem: 1 }],
  gabarito: 'A',
  comentario: 'Comentário.',
};

describe('QuestaoApi', () => {
  let api: QuestaoApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [QuestaoApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(QuestaoApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('descarta filtros nulos, vazios e indefinidos ao montar a query da listagem', () => {
    api
      .listar({
        q: 'licitações',
        disciplina: '',
        banca: null,
        status: undefined,
        page: 0,
        size: 20,
      })
      .subscribe();

    const requisicaoHttp = http.expectOne((r) => r.url === base);
    expect(requisicaoHttp.request.params.keys().sort()).toEqual(['page', 'q', 'size']);
    expect(requisicaoHttp.request.params.get('q')).toBe('licitações');
    expect(requisicaoHttp.request.params.get('page')).toBe('0');
    requisicaoHttp.flush({ content: [], page: 0, size: 20, totalElements: 0 });
  });

  it('obtém a questão pelo identificador', () => {
    api.obter('q1').subscribe();

    const leitura = http.expectOne(`${base}/q1`);
    expect(leitura.request.method).toBe('GET');
    leitura.flush(null);
  });

  it('cria a questão enviando o corpo da requisição', () => {
    api.criar(requisicao).subscribe();

    const criacao = http.expectOne(base);
    expect(criacao.request.method).toBe('POST');
    expect(criacao.request.body).toEqual(requisicao);
    criacao.flush(null);
  });

  it('atualiza a questão pelo identificador', () => {
    api.atualizar('q1', requisicao).subscribe();

    const atualizacao = http.expectOne(`${base}/q1`);
    expect(atualizacao.request.method).toBe('PUT');
    expect(atualizacao.request.body).toEqual(requisicao);
    atualizacao.flush(null);
  });

  it('exclui a questão pelo identificador', () => {
    api.excluir('q1').subscribe();

    const exclusao = http.expectOne(`${base}/q1`);
    expect(exclusao.request.method).toBe('DELETE');
    exclusao.flush(null);
  });

  it('envia as três transições de status para as rotas do contrato', () => {
    api.enviarParaRevisao('q1').subscribe();
    api.publicar('q1').subscribe();
    api.despublicar('q1').subscribe();

    const transicoes = http.match((r) => r.method === 'POST');
    expect(transicoes.map((r) => r.request.url)).toEqual([
      `${base}/q1/enviar-revisao`,
      `${base}/q1/publicar`,
      `${base}/q1/despublicar`,
    ]);
    transicoes.forEach((r) => r.flush(null));
  });
});
