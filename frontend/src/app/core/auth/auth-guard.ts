import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { PerfilUsuario } from './modelos';
import { AuthStore } from './auth-store';
import { rotaInicialPorPerfil } from './rota-inicial';

export const authGuard: CanActivateFn = (_rota, estado) => {
  const store = inject(AuthStore);
  const router = inject(Router);
  return store.autenticado()
    ? true
    : router.createUrlTree(['/login'], { queryParams: { retorno: estado.url } });
};

export const visitanteGuard: CanActivateFn = () => {
  const store = inject(AuthStore);
  const router = inject(Router);
  const perfil = store.perfil();
  return store.autenticado() && perfil
    ? router.createUrlTree([rotaInicialPorPerfil(perfil, router)])
    : true;
};

export function perfilGuard(perfis: readonly PerfilUsuario[]): CanActivateFn {
  return () => {
    const store = inject(AuthStore);
    const router = inject(Router);
    const perfil = store.perfil();
    return perfil && perfis.includes(perfil) ? true : router.createUrlTree(['/inicio']);
  };
}
