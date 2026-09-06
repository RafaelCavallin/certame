import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SimuladoApi } from './simulado-api';

describe('SimuladoApi', () => {
  let api: SimuladoApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SimuladoApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(SimuladoApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('envia filtros da lista administrativa', () => {
    api.listar({ q: 'trf', estado: 'RASCUNHO', bancaId: 'b1', page: 2 }).subscribe();
    const request = http.expectOne((r) => r.url === '/api/v1/admin/simulados');
    expect(request.request.params.get('q')).toBe('trf');
    expect(request.request.params.get('page')).toBe('2');
    request.flush({ content: [], page: 2, size: 20, totalElements: 0 });
  });
  it('envia a composição para o endpoint administrativo', () => {
    api.composicao('s1', { itens: [] }).subscribe();
    const request = http.expectOne('/api/v1/admin/simulados/s1/composicao');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ itens: [] });
    request.flush({
      itens: [],
      somaComposicao: 0,
      numeroQuestoes: 10,
      fechada: false,
      acervoSuficiente: true,
    });
  });
  it('lista os simulados do estudante', () => {
    api.listarDoEstudante().subscribe();
    const request = http.expectOne('/api/v1/simulados');
    expect(request.request.method).toBe('GET');
    request.flush({ proximo: null, disponiveis: [], agendados: [], concluidos: [] });
  });
  it('executa as operações de cadastro e publicação', () => {
    api.obter('s1').subscribe();
    http.expectOne('/api/v1/admin/simulados/s1').flush({});
    api.criar({} as never).subscribe();
    http.expectOne('/api/v1/admin/simulados').flush({});
    api.atualizar('s1', {} as never).subscribe();
    http.expectOne('/api/v1/admin/simulados/s1').flush({});
    api.publicar('s1').subscribe();
    http.expectOne('/api/v1/admin/simulados/s1/publicar').flush({});
    api.despublicar('s1').subscribe();
    http.expectOne('/api/v1/admin/simulados/s1/despublicar').flush({});
    api.excluir('s1').subscribe();
    http.expectOne('/api/v1/admin/simulados/s1').flush(null);
  });
  it('chama os endpoints de tentativa do estudante', () => {
    api.iniciarTentativa('s1').subscribe();
    http.expectOne('/api/v1/simulados/s1/tentativas').flush({});
    api.finalizarTentativa('s1', 't1', {}).subscribe();
    const finalizacao = http.expectOne('/api/v1/simulados/s1/tentativas/t1/finalizar');
    expect(finalizacao.request.body).toEqual({});
    finalizacao.flush({});
    api.resultado('s1', 't1').subscribe();
    http.expectOne('/api/v1/simulados/s1/tentativas/t1').flush({});
  });
});
