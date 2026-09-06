import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthApi } from './auth-api';

describe('AuthApi', () => {
  let api: AuthApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AuthApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia as seis operações para o contrato da API com credenciais', () => {
    api.entrar({ email: 'a@b.com', senha: '12345678' }).subscribe();
    api.cadastrar({ nome: 'Pessoa', email: 'a@b.com', senha: '12345678' }).subscribe();
    api.esqueciSenha('a@b.com').subscribe();
    api.redefinirSenha('token', '12345678').subscribe();
    api.refresh().subscribe();
    api.sair().subscribe();

    const requisicoes = http.match((request) => request.url.startsWith('/api/v1/auth/'));
    expect(requisicoes).toHaveLength(6);
    expect(requisicoes.every((request) => request.request.withCredentials)).toBe(true);
    requisicoes.forEach((request) => request.flush(null));
  });
});
