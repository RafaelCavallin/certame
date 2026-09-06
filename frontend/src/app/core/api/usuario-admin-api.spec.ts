import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PerfilUsuario } from '../auth/modelos';
import { StatusUsuario } from '../models/modelos-usuario';
import { UsuarioAdminApi } from './usuario-admin-api';

const base = '/api/v1/super/usuarios';

describe('UsuarioAdminApi', () => {
  let api: UsuarioAdminApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UsuarioAdminApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(UsuarioAdminApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('descarta filtros vazios ao montar a query da listagem', () => {
    api
      .listar({ page: 0, size: 20, sort: 'criadoEm,desc', perfil: undefined, status: undefined })
      .subscribe();

    const requisicao = http.expectOne((r) => r.url === base);
    expect(requisicao.request.params.keys().sort()).toEqual(['page', 'size', 'sort']);
    requisicao.flush({ content: [], page: 0, size: 20, totalElements: 0 });
  });

  it('envia perfil e status quando informados', () => {
    api
      .listar({
        page: 0,
        size: 20,
        sort: 'criadoEm,asc',
        perfil: PerfilUsuario.PROFESSOR,
        status: StatusUsuario.ATIVO,
      })
      .subscribe();

    const requisicao = http.expectOne((r) => r.url === base);
    expect(requisicao.request.params.get('perfil')).toBe('PROFESSOR');
    expect(requisicao.request.params.get('status')).toBe('ATIVO');
    requisicao.flush({ content: [], page: 0, size: 20, totalElements: 0 });
  });

  it('cria o usuário enviando o corpo da requisição', () => {
    const dados = {
      nome: 'Marina Duarte',
      email: 'marina@certame.com',
      perfil: PerfilUsuario.PROFESSOR,
      status: StatusUsuario.ATIVO,
      enviarConvite: true,
    };
    api.criar(dados).subscribe();

    const criacao = http.expectOne(base);
    expect(criacao.request.method).toBe('POST');
    expect(criacao.request.body).toEqual(dados);
    criacao.flush(null);
  });

  it('edita o usuário pelo identificador com PATCH', () => {
    const dados = {
      nome: 'Marina Duarte',
      email: 'marina@certame.com',
      perfil: PerfilUsuario.ADMIN,
      status: StatusUsuario.ATIVO,
    };
    api.editar('u1', dados).subscribe();

    const edicao = http.expectOne(`${base}/u1`);
    expect(edicao.request.method).toBe('PATCH');
    expect(edicao.request.body).toEqual(dados);
    edicao.flush(null);
  });

  it('exclui o usuário pelo identificador', () => {
    api.excluir('u1').subscribe();

    const exclusao = http.expectOne(`${base}/u1`);
    expect(exclusao.request.method).toBe('DELETE');
    exclusao.flush(null);
  });

  it('envia redefinição de senha e reenvio de convite para as rotas do contrato', () => {
    api.redefinirSenha('u1').subscribe();
    api.reenviarConvite('u1').subscribe();

    const chamadas = http.match((r) => r.method === 'POST' && r.url !== base);
    expect(chamadas.map((r) => r.request.url)).toEqual([
      `${base}/u1/redefinir-senha`,
      `${base}/u1/reenviar-convite`,
    ]);
    chamadas.forEach((r) => r.flush(null));
  });
});
