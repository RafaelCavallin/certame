import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { authGuard, perfilGuard, visitanteGuard } from './auth-guard';
import { AuthStore } from './auth-store';
import { PerfilUsuario } from './modelos';

describe('guardas de autenticação', () => {
  function configurar(autenticado: boolean, perfil: PerfilUsuario | null) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthStore,
          useValue: { autenticado: signal(autenticado), perfil: signal(perfil) },
        },
      ],
    });
  }

  it('permite autenticado, redireciona visitante autenticado e bloqueia perfil insuficiente', () => {
    configurar(true, PerfilUsuario.ESTUDANTE);
    const router = TestBed.inject(Router);
    const permissao = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/inicio' } as never),
    );
    const visitante = TestBed.runInInjectionContext(() => visitanteGuard({} as never, {} as never));
    const perfil = TestBed.runInInjectionContext(() =>
      perfilGuard([PerfilUsuario.ADMIN])({} as never, {} as never),
    );

    expect(permissao).toBe(true);
    expect(visitante).toEqual(router.parseUrl('/inicio'));
    expect(perfil).toEqual(router.parseUrl('/inicio'));
  });

  it('redireciona visitante não autenticado para login e permite perfil autorizado', () => {
    configurar(false, null);
    const router = TestBed.inject(Router);
    const bloqueio = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/inicio' } as never),
    );
    const visitante = TestBed.runInInjectionContext(() => visitanteGuard({} as never, {} as never));
    expect(bloqueio).toEqual(router.parseUrl('/login?retorno=%2Finicio'));
    expect(visitante).toBe(true);

    TestBed.resetTestingModule();
    configurar(true, PerfilUsuario.ADMIN);
    const autorizado = TestBed.runInInjectionContext(() =>
      perfilGuard([PerfilUsuario.ADMIN])({} as never, {} as never),
    );
    expect(autorizado).toBe(true);
  });
});
