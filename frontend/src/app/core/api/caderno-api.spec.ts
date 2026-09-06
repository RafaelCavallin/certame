import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CadernoApi } from './caderno-api';

describe('CadernoApi', () => {
  it('envia a criação para o endpoint de cadernos e preserva o erro 409', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const api = TestBed.inject(CadernoApi);
    const http = TestBed.inject(HttpTestingController);
    let erro: unknown;
    api
      .criar({ nome: 'Favoritas', descricao: null })
      .subscribe({ error: (value) => (erro = value.error) });
    const requisicao = http.expectOne('/api/v1/cadernos');
    requisicao.flush(
      {
        code: 'NOME_CADERNO_EM_USO',
        message: 'Nome em uso.',
        fieldErrors: { nome: 'Nome em uso.' },
      },
      { status: 409, statusText: 'Conflict' },
    );
    expect(erro).toEqual({
      code: 'NOME_CADERNO_EM_USO',
      message: 'Nome em uso.',
      fieldErrors: { nome: 'Nome em uso.' },
    });
    http.verify();
  });
});
