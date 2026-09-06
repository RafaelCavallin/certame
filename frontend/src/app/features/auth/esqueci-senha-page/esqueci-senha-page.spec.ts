import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthApi } from '../../../core/auth/auth-api';
import { EsqueciSenhaPage } from './esqueci-senha-page';

describe('EsqueciSenhaPage', () => {
  it('exibe confirmação após e-mail válido', async () => {
    const api = { esqueciSenha: vi.fn().mockReturnValue(of(undefined)) };
    TestBed.configureTestingModule({
      imports: [EsqueciSenhaPage],
      providers: [provideRouter([]), { provide: AuthApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(EsqueciSenhaPage);
    fixture.detectChanges();
    fixture.componentInstance.formulario.setValue({ email: 'pessoa@email.com' });
    await fixture.componentInstance.enviar();
    fixture.detectChanges();
    expect(fixture.componentInstance.enviado()).toBe(true);
    expect(api.esqueciSenha).toHaveBeenCalledWith('pessoa@email.com');
  });

  it('barra e-mail malformado antes da API', async () => {
    const api = { esqueciSenha: vi.fn() };
    TestBed.configureTestingModule({
      imports: [EsqueciSenhaPage],
      providers: [provideRouter([]), { provide: AuthApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(EsqueciSenhaPage);
    fixture.detectChanges();
    fixture.componentInstance.formulario.setValue({ email: 'invalido' });
    await fixture.componentInstance.enviar();
    expect(api.esqueciSenha).not.toHaveBeenCalled();
  });

  it('exibe erro inesperado sem confirmar o pedido', async () => {
    const api = { esqueciSenha: vi.fn().mockRejectedValue(new HttpErrorResponse({ status: 500 })) };
    TestBed.configureTestingModule({
      imports: [EsqueciSenhaPage],
      providers: [provideRouter([]), { provide: AuthApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(EsqueciSenhaPage);
    fixture.detectChanges();
    fixture.componentInstance.formulario.setValue({ email: 'pessoa@email.com' });
    await fixture.componentInstance.enviar();
    fixture.detectChanges();
    expect(fixture.componentInstance.enviado()).toBe(false);
    expect(fixture.componentInstance.erro()).toContain('Não foi possível');
  });
});
