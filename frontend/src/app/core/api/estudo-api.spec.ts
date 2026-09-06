import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EstudoApi } from './estudo-api';
describe('EstudoApi', () => {
  let api: EstudoApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EstudoApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(EstudoApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('usa os sete endpoints do contrato', () => {
    api.inicio().subscribe();
    api.abrir({ tamanho: 10 }).subscribe();
    api.retomar('s').subscribe();
    api.trocarModo('s', 'CONTEXTO').subscribe();
    api.responder('s', { questaoId: 'q', marcada: 'A', tempoMs: 1 }).subscribe();
    api.finalizar('s', { respostas: [] }).subscribe();
    api.marcar('q', { favorita: true, erroRecorrente: false }).subscribe();
    expect(http.expectOne('/api/v1/estudo/inicio').request.method).toBe('GET');
    expect(http.expectOne('/api/v1/estudo/sessoes').request.method).toBe('POST');
    expect(http.expectOne('/api/v1/estudo/sessoes/s').request.method).toBe('GET');
    expect(http.expectOne('/api/v1/estudo/sessoes/s/modo').request.method).toBe('PATCH');
    expect(http.expectOne('/api/v1/estudo/sessoes/s/respostas').request.method).toBe('POST');
    expect(http.expectOne('/api/v1/estudo/sessoes/s/finalizar').request.method).toBe('POST');
    expect(http.expectOne('/api/v1/estudo/questoes/q/marcacoes').request.method).toBe('PUT');
  });
});
