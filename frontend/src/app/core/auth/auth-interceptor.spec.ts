import { HttpErrorResponse, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError, firstValueFrom } from 'rxjs';

import { authInterceptor } from './auth-interceptor';
import { AuthStore } from './auth-store';

describe('authInterceptor', () => {
  it('TU-20: renova uma vez e repete a requisição original', async () => {
    const store = {
      accessToken: () => 'token-expirado',
      renovar: vi.fn().mockResolvedValue('token-novo'),
      limpar: vi.fn(),
    };
    const requisicao = new HttpRequest('GET', '/api/v1/inicio');
    let chamadas = 0;
    const proximo = vi.fn((request: HttpRequest<unknown>) => {
      chamadas++;
      return chamadas === 1
        ? throwError(() => new HttpErrorResponse({ status: 401 }))
        : of(new HttpResponse({ status: 200, body: request.headers.get('Authorization') }));
    });

    TestBed.configureTestingModule({
      providers: [{ provide: AuthStore, useValue: store }, provideRouter([])],
    });
    const resposta = await firstValueFrom(
      TestBed.runInInjectionContext(() => authInterceptor(requisicao, proximo)),
    );

    expect(store.renovar).toHaveBeenCalledOnce();
    expect(proximo).toHaveBeenCalledTimes(2);
    expect((resposta as HttpResponse<unknown>).body).toBe('Bearer token-novo');
  });

  it('deixa a restauração inicial tratar o 401 do refresh', async () => {
    const store = { accessToken: () => null, limpar: vi.fn() };
    const requisicao = new HttpRequest('POST', '/api/v1/auth/refresh', null);
    const proximo = vi.fn(() => throwError(() => new HttpErrorResponse({ status: 401 })));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [{ provide: AuthStore, useValue: store }, provideRouter([])],
    });
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await expect(
      firstValueFrom(TestBed.runInInjectionContext(() => authInterceptor(requisicao, proximo))),
    ).rejects.toBeInstanceOf(HttpErrorResponse);
    expect(store.limpar).not.toHaveBeenCalled();
  });
});
