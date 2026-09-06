import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { AuthApi } from './auth-api';
import { AuthStore } from './auth-store';
import { PerfilUsuario, RespostaAutenticacao } from './modelos';

const resposta: RespostaAutenticacao = {
  accessToken: 'token-de-teste',
  expiraEm: '2026-08-30T20:00:00Z',
  usuario: {
    id: '1',
    nome: 'Rafael',
    email: 'rafael@example.com',
    perfil: PerfilUsuario.ESTUDANTE,
  },
};

describe('AuthStore', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let store: AuthStore;

  beforeEach(() => {
    api = {
      entrar: vi.fn(),
      cadastrar: vi.fn(),
      refresh: vi.fn(),
      sair: vi.fn(),
    };
    TestBed.configureTestingModule({ providers: [{ provide: AuthApi, useValue: api }] });
    store = TestBed.inject(AuthStore);
  });

  it('TU-18: popula a sessão ao entrar com sucesso', async () => {
    api['entrar'].mockReturnValue(of(resposta));

    await store.entrar({ email: 'rafael@example.com', senha: 'senhaSegura123' });

    expect(store.usuario()).toEqual(resposta.usuario);
    expect(store.accessToken()).toBe(resposta.accessToken);
    expect(store.autenticado()).toBe(true);
  });

  it('TU-19: preserva o estado de sessão e preenche erro em 401', async () => {
    api['entrar'].mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: {
              code: 'CREDENCIAL_INVALIDA',
              message: 'E-mail ou senha inválidos.',
              fieldErrors: null,
            },
          }),
      ),
    );

    await expect(
      store.entrar({ email: 'rafael@example.com', senha: 'errada123' }),
    ).rejects.toBeInstanceOf(HttpErrorResponse);

    expect(store.accessToken()).toBeNull();
    expect(store.erro()?.code).toBe('CREDENCIAL_INVALIDA');
  });

  it('atualiza a sessão ao cadastrar e renovar', async () => {
    api['cadastrar'].mockReturnValue(of(resposta));
    await store.cadastrar({ nome: 'Rafael', email: 'rafael@example.com', senha: 'senhaSegura123' });
    expect(store.autenticado()).toBe(true);

    api['refresh'].mockReturnValue(of({ ...resposta, accessToken: 'token-renovado' }));
    await expect(store.renovar()).resolves.toBe('token-renovado');
    expect(store.accessToken()).toBe('token-renovado');
  });

  it('restaura a sessão silenciosamente quando o refresh falha e limpa ao sair', async () => {
    api['refresh'].mockReturnValue(throwError(() => new Error('sem sessão')));
    await store.restaurarSessao();
    expect(store.autenticado()).toBe(false);

    api['sair'].mockReturnValue(of(undefined));
    await store.sair();
    expect(store.usuario()).toBeNull();
  });
});
