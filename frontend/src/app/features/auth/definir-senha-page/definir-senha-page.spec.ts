import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthApi } from '../../../core/auth/auth-api';
import { DefinirSenhaPage } from './definir-senha-page';

describe('DefinirSenhaPage', () => {
  function configurar(resposta: ReturnType<typeof vi.fn>) {
    TestBed.configureTestingModule({
      imports: [DefinirSenhaPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } },
        { provide: AuthApi, useValue: { redefinirSenha: resposta } },
      ],
    });
  }

  it('consome o token e exibe estado concluído', async () => {
    const redefinir = vi.fn().mockReturnValue(of(undefined));
    configurar(redefinir);
    const fixture = TestBed.createComponent(DefinirSenhaPage);
    fixture.detectChanges();
    fixture.componentInstance.formulario.setValue({ senha: 'senha1234' });
    await fixture.componentInstance.enviar();
    fixture.detectChanges();
    expect(redefinir).toHaveBeenCalledWith('', 'senha1234');
    expect(fixture.componentInstance.estado()).toBe('concluido');
  });

  it('exibe link inválido para token recusado', async () => {
    const redefinir = vi.fn().mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { code: 'TOKEN_INVALIDO', message: 'Link inválido.', fieldErrors: null },
          }),
      ),
    );
    configurar(redefinir);
    const fixture = TestBed.createComponent(DefinirSenhaPage);
    fixture.detectChanges();
    fixture.componentInstance.formulario.setValue({ senha: 'senha1234' });
    await fixture.componentInstance.enviar();
    fixture.detectChanges();
    expect(fixture.componentInstance.estado()).toBe('invalido');
  });

  it('barra senha curta antes de consumir o token', async () => {
    const redefinir = vi.fn();
    configurar(redefinir);
    const fixture = TestBed.createComponent(DefinirSenhaPage);
    fixture.detectChanges();
    fixture.componentInstance.formulario.setValue({ senha: 'curta' });
    await fixture.componentInstance.enviar();
    expect(redefinir).not.toHaveBeenCalled();
    expect(fixture.componentInstance.estado()).toBe('formulario');
  });
});
