import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { EditalApi } from './edital-api';
import { RequisicaoEdital } from './modelos-edital';

const requisicao: RequisicaoEdital = {
  nome: 'TRF 1ª Região',
  status: 'ABERTO',
  vagas: 320,
  bancaId: null,
  orgaoId: null,
  prazoInscricao: '2026-08-30',
  urlOficial: null,
  disciplinaIds: [],
};

describe('EditalApi', () => {
  function montar() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    return { api: TestBed.inject(EditalApi), http: TestBed.inject(HttpTestingController) };
  }

  it('consulta os editais agrupados por status no endpoint do estudante', () => {
    const { api, http } = montar();
    let resposta: unknown;
    api.agrupados().subscribe((valor) => (resposta = valor));
    const req = http.expectOne('/api/v1/editais');
    expect(req.request.method).toBe('GET');
    req.flush({ abertos: [], previstos: [], encerrados: [] });
    expect(resposta).toEqual({ abertos: [], previstos: [], encerrados: [] });
    http.verify();
  });

  it('lista os editais para a administração', () => {
    const { api, http } = montar();
    api.listarParaAdministracao().subscribe();
    const req = http.expectOne('/api/v1/admin/editais');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    http.verify();
  });

  it('busca um edital pelo id para o formulário', () => {
    const { api, http } = montar();
    api.buscar('1').subscribe();
    const req = http.expectOne('/api/v1/admin/editais/1');
    expect(req.request.method).toBe('GET');
    req.flush({});
    http.verify();
  });

  it('cria um edital novo', () => {
    const { api, http } = montar();
    api.criar(requisicao).subscribe();
    const req = http.expectOne('/api/v1/admin/editais');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(requisicao);
    req.flush({});
    http.verify();
  });

  it('atualiza um edital existente', () => {
    const { api, http } = montar();
    api.atualizar('1', requisicao).subscribe();
    const req = http.expectOne('/api/v1/admin/editais/1');
    expect(req.request.method).toBe('PUT');
    req.flush({});
    http.verify();
  });

  it('exclui um edital', () => {
    const { api, http } = montar();
    api.excluir('1').subscribe();
    const req = http.expectOne('/api/v1/admin/editais/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    http.verify();
  });
});
