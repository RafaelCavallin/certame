import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthStore } from '../../../core/auth/auth-store';
import { PerfilUsuario } from '../../../core/auth/modelos';
import { AcessoPage } from './acesso-page';

describe('AcessoPage', () => {
  let auth: Record<string, unknown>;
  beforeEach(() => {
    auth = {
      cadastrando: false,
      carregando: signal(false),
      usuario: signal(null),
      perfil: signal(PerfilUsuario.ESTUDANTE),
      entrar: vi.fn(),
      cadastrar: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [AcessoPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: { aba: 'entrar' }, queryParamMap: { has: () => false } } },
        },
        { provide: AuthStore, useValue: auth },
        { provide: Router, useValue: { navigateByUrl: vi.fn() } },
      ],
    });
  });

  it('renderiza o cadastro sem bloco de demonstração e preserva a aba', () => {
    const fixture = TestBed.createComponent(AcessoPage);
    fixture.componentInstance.alternar(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#nome')).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain('Entrar como');
  });

  it('mostra erro de credencial, preserva e-mail e foca a senha', async () => {
    auth['entrar'] = vi.fn().mockRejectedValue(
      new HttpErrorResponse({
        status: 401,
        error: {
          code: 'CREDENCIAL_INVALIDA',
          message: 'E-mail ou senha inválidos.',
          fieldErrors: null,
        },
      }),
    );
    const fixture = TestBed.createComponent(AcessoPage);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.formulario.setValue({ nome: '', email: 'pessoa@email.com', senha: 'errada123' });
    await component.enviar();
    fixture.detectChanges();
    expect(component.formulario.controls.email.value).toBe('pessoa@email.com');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'E-mail ou senha inválidos.',
    );
    expect(document.activeElement).toBe(fixture.nativeElement.querySelector('#senha'));
  });

  it('marca o e-mail quando o cadastro retorna duplicidade', async () => {
    auth['cadastrar'] = vi.fn().mockRejectedValue(
      new HttpErrorResponse({
        status: 409,
        error: {
          code: 'EMAIL_EM_USO',
          message: 'Este e-mail já está em uso.',
          fieldErrors: null,
        },
      }),
    );
    const fixture = TestBed.createComponent(AcessoPage);
    fixture.componentInstance.alternar(true);
    fixture.componentInstance.formulario.setValue({
      nome: 'Pessoa',
      email: 'pessoa@email.com',
      senha: 'senha1234',
    });
    await fixture.componentInstance.enviar();
    expect(fixture.componentInstance.formulario.controls.email.hasError('api')).toBe(true);
    expect(fixture.componentInstance.formulario.controls.nome.value).toBe('Pessoa');
  });

  it('bloqueia envio com campos inválidos', async () => {
    const fixture = TestBed.createComponent(AcessoPage);
    await fixture.componentInstance.enviar();
    expect(auth['entrar']).not.toHaveBeenCalled();
  });

  it('entra com sucesso e navega para o início', async () => {
    auth['entrar'] = vi.fn().mockResolvedValue(undefined);
    const fixture = TestBed.createComponent(AcessoPage);
    fixture.componentInstance.formulario.setValue({
      nome: '',
      email: 'pessoa@email.com',
      senha: 'senha1234',
    });
    fixture.detectChanges();
    await fixture.componentInstance.enviar();
    expect(auth['entrar']).toHaveBeenCalledWith({ email: 'pessoa@email.com', senha: 'senha1234' });
  });

  it('exibe validações locais dos campos', () => {
    const fixture = TestBed.createComponent(AcessoPage);
    const component = fixture.componentInstance;
    component.formulario.controls.email.setValue('invalido');
    component.formulario.controls.senha.setValue('curta');
    component.formulario.controls.email.markAsTouched();
    component.formulario.controls.senha.markAsTouched();
    fixture.detectChanges();
    expect(component.mensagem(component.formulario.controls.email)).toBe(
      'Informe um e-mail válido.',
    );
    expect(component.mensagem(component.formulario.controls.senha)).toBe(
      'A senha deve ter ao menos 8 caracteres.',
    );
  });
});
