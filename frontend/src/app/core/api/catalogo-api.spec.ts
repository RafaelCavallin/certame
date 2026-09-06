import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CatalogoApi } from './catalogo-api';

const base = '/api/v1/admin';

describe('CatalogoApi', () => {
  let api: CatalogoApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CatalogoApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CatalogoApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lê a árvore sem arquivados por padrão', () => {
    api.disciplinas().subscribe();

    const leitura = http.expectOne((r) => r.url === `${base}/disciplinas`);
    expect(leitura.request.params.get('incluirArquivados')).toBe('false');
    leitura.flush([]);
  });

  it('lê a árvore com arquivados quando solicitado', () => {
    api.disciplinas(true).subscribe();

    const leitura = http.expectOne((r) => r.url === `${base}/disciplinas`);
    expect(leitura.request.params.get('incluirArquivados')).toBe('true');
    leitura.flush([]);
  });

  it('envia as operações de disciplina para as rotas do contrato', () => {
    api.criarDisciplina({ nome: 'Direito Penal' }).subscribe();
    api.renomearDisciplina('d1', { nome: 'Direito Penal II' }).subscribe();
    api.excluirDisciplina('d1').subscribe();

    const operacoes = http.match((r) => r.url.startsWith(`${base}/disciplinas`));
    expect(operacoes.map((r) => `${r.request.method} ${r.request.url}`)).toEqual([
      `POST ${base}/disciplinas`,
      `PUT ${base}/disciplinas/d1`,
      `DELETE ${base}/disciplinas/d1`,
    ]);
    expect(operacoes[0].request.body).toEqual({ nome: 'Direito Penal' });
    operacoes.forEach((r) => r.flush(null));
  });

  it('envia as operações de assunto, incluindo arquivar, para as rotas do contrato', () => {
    api.criarAssunto({ nome: 'Licitações', disciplinaId: 'd1' }).subscribe();
    api.renomearAssunto('a1', { nome: 'Licitações e contratos' }).subscribe();
    api.excluirAssunto('a1').subscribe();
    api.arquivarAssunto('a1').subscribe();

    const operacoes = http.match((r) => r.url.startsWith(`${base}/assuntos`));
    expect(operacoes.map((r) => `${r.request.method} ${r.request.url}`)).toEqual([
      `POST ${base}/assuntos`,
      `PUT ${base}/assuntos/a1`,
      `DELETE ${base}/assuntos/a1`,
      `POST ${base}/assuntos/a1/arquivar`,
    ]);
    expect(operacoes[0].request.body).toEqual({ nome: 'Licitações', disciplinaId: 'd1' });
    operacoes.forEach((r) => r.flush(null));
  });

  it('envia as operações de banca para as rotas do contrato', () => {
    api.bancas().subscribe();
    api.criarBanca({ nome: 'Cebraspe', sigla: 'CB' }).subscribe();
    api.atualizarBanca('b1', { nome: 'Cebraspe', sigla: null }).subscribe();
    api.excluirBanca('b1').subscribe();

    const operacoes = http.match((r) => r.url.startsWith(`${base}/bancas`));
    expect(operacoes.map((r) => `${r.request.method} ${r.request.url}`)).toEqual([
      `GET ${base}/bancas`,
      `POST ${base}/bancas`,
      `PUT ${base}/bancas/b1`,
      `DELETE ${base}/bancas/b1`,
    ]);
    expect(operacoes[1].request.body).toEqual({ nome: 'Cebraspe', sigla: 'CB' });
    operacoes.forEach((r) => r.flush(null));
  });

  it('envia as operações de órgão para as rotas do contrato', () => {
    api.orgaos().subscribe();
    api.criarOrgao({ nome: 'Tribunal de Contas da União', sigla: 'TCU' }).subscribe();
    api.atualizarOrgao('o1', { nome: 'TCU', sigla: 'TCU' }).subscribe();
    api.excluirOrgao('o1').subscribe();

    const operacoes = http.match((r) => r.url.startsWith(`${base}/orgaos`));
    expect(operacoes.map((r) => `${r.request.method} ${r.request.url}`)).toEqual([
      `GET ${base}/orgaos`,
      `POST ${base}/orgaos`,
      `PUT ${base}/orgaos/o1`,
      `DELETE ${base}/orgaos/o1`,
    ]);
    expect(operacoes[1].request.body).toEqual({
      nome: 'Tribunal de Contas da União',
      sigla: 'TCU',
    });
    operacoes.forEach((r) => r.flush(null));
  });
});
