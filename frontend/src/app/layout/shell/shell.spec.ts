import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { RevisaoApi } from '../../core/api/revisao-api';
import { Cronometro } from '../../features/study/cronometro';
import { PerfilUsuario } from '../../core/auth/modelos';
import { AuthStore } from '../../core/auth/auth-store';
import { ShellPage } from './shell';

const resumoFlashcards = { vencendoHoje: 3, proximos7Dias: 0, novosDisponiveis: 0, porDeck: [] };

describe('ShellPage', () => {
  function configurar(perfil: PerfilUsuario = PerfilUsuario.ESTUDANTE): void {
    TestBed.configureTestingModule({
      imports: [ShellPage],
      providers: [
        provideRouter([{ path: 'login', component: ShellPage }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthStore,
          useValue: {
            sair: vi.fn().mockResolvedValue(undefined),
            usuario: signal({ nome: 'Pessoa', perfil }),
            perfil: signal(perfil),
          },
        },
        { provide: RevisaoApi, useValue: { resumo: () => of(resumoFlashcards) } },
      ],
    });
  }

  it('limpa a sessão ao acionar Sair', async () => {
    configurar();
    const fixture = TestBed.createComponent(ShellPage);
    fixture.detectChanges();
    await fixture.componentInstance.sair();
    expect(TestBed.inject(AuthStore).sair).toHaveBeenCalledOnce();
  });

  it('exibe o contador de cards vencendo hoje no item Flashcards', async () => {
    configurar();
    const fixture = TestBed.createComponent(ShellPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.flashcardsVencendoHoje()).toBe(3);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Flashcards');
  });

  it('só mostra o cronômetro dentro de uma sessão de estudo', () => {
    configurar();
    const fixture = TestBed.createComponent(ShellPage);
    const cronometro = TestBed.inject(Cronometro);
    fixture.componentInstance.emEstudo.set(true);
    fixture.detectChanges();

    expect(fixture.componentInstance.emSessao()).toBe(false);
    expect(fixture.nativeElement.querySelector('.cronometro')).toBeNull();

    cronometro.iniciar('s1');
    fixture.detectChanges();

    expect(fixture.componentInstance.emSessao()).toBe(true);
    expect(fixture.nativeElement.querySelector('.cronometro')).not.toBeNull();
    cronometro.parar('s1');
  });

  it('esconde o item Usuários para perfis que não são super-admin', () => {
    configurar(PerfilUsuario.ADMIN);
    const fixture = TestBed.createComponent(ShellPage);
    expect(fixture.componentInstance.podeSuper()).toBe(false);
  });

  it('mostra o item Usuários para o super-admin', () => {
    configurar(PerfilUsuario.SUPER_ADMIN);
    const fixture = TestBed.createComponent(ShellPage);
    expect(fixture.componentInstance.podeSuper()).toBe(true);
  });
});
