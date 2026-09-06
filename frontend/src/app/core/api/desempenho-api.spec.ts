import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { DesempenhoApi } from './desempenho-api';

describe('DesempenhoApi', () => {
  let api: DesempenhoApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DesempenhoApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(DesempenhoApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('consulta os quatro painéis nos endpoints do contrato', () => {
    api.resumo().subscribe();
    api.evolucao().subscribe();
    api.disciplinas().subscribe();
    api.pontosFracos().subscribe();
    expect(http.expectOne('/api/v1/desempenho/resumo').request.method).toBe('GET');
    expect(http.expectOne('/api/v1/desempenho/evolucao').request.method).toBe('GET');
    expect(http.expectOne('/api/v1/desempenho/disciplinas').request.method).toBe('GET');
    expect(http.expectOne('/api/v1/desempenho/pontos-fracos').request.method).toBe('GET');
  });
});
