import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { from, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { AuthStore } from './auth-store';

const TENTATIVA_RENOVACAO = new HttpContextToken<boolean>(() => false);
let renovacaoEmAndamento: Promise<string> | null = null;

export const authInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const store = inject(AuthStore);
  const router = inject(Router);
  const accessToken = store.accessToken();
  const ehRefresh = requisicao.url.endsWith('/auth/refresh');
  const autorizada = accessToken
    ? requisicao.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
    : requisicao;

  return proximo(autorizada).pipe(
    catchError((erro: unknown) => {
      if (
        !(erro instanceof HttpErrorResponse) ||
        erro.status !== 401 ||
        ehRefresh ||
        !accessToken ||
        requisicao.context.get(TENTATIVA_RENOVACAO)
      ) {
        return throwError(() => erro);
      }

      renovacaoEmAndamento ??= store.renovar().finally(() => {
        renovacaoEmAndamento = null;
      });

      return from(renovacaoEmAndamento).pipe(
        switchMap((token) =>
          proximo(
            autorizada.clone({
              setHeaders: { Authorization: `Bearer ${token}` },
              context: requisicao.context.set(TENTATIVA_RENOVACAO, true),
            }),
          ),
        ),
        catchError((falha) => {
          store.limpar();
          void router.navigateByUrl('/login');
          return throwError(() => falha);
        }),
      );
    }),
  );
};
