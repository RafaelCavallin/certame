import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, isObservable } from 'rxjs';
import { Dialog } from '../../shared/ui/dialog/dialog';
import { Cronometro } from './cronometro';
import { EstudarPage } from './estudar-page/estudar-page';
import { sairSessaoGuard } from './sair-sessao-guard';

function componenteFalso(sessaoId: string | null) {
  return { store: { sessao: () => (sessaoId ? { sessaoId } : null) } } as unknown as EstudarPage;
}

function montarDialogo() {
  const fixture = TestBed.createComponent(Dialog);
  fixture.detectChanges();
  return fixture;
}

describe('sairSessaoGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('libera a navegação direto quando não há sessão ativa', () => {
    const resultado = TestBed.runInInjectionContext(() =>
      sairSessaoGuard(componenteFalso(null), {} as never, {} as never, {} as never),
    );
    expect(resultado).toBe(true);
  });

  it('pede confirmação e, ao confirmar, pausa e para o cronômetro antes de liberar a navegação', async () => {
    const cronometro = TestBed.inject(Cronometro);
    cronometro.iniciar('s1');
    const pausarEspiao = vi.spyOn(cronometro, 'pausar');
    const pararEspiao = vi.spyOn(cronometro, 'parar');
    const fixture = montarDialogo();

    const resultado$ = TestBed.runInInjectionContext(() =>
      sairSessaoGuard(componenteFalso('s1'), {} as never, {} as never, {} as never),
    );
    fixture.detectChanges();
    expect(isObservable(resultado$)).toBe(true);

    const promessa = firstValueFrom(resultado$ as never);
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.btn-primary')!
      .click();
    const resultado = await promessa;

    expect(resultado).toBe(true);
    expect(pausarEspiao).toHaveBeenCalledOnce();
    expect(pararEspiao).toHaveBeenCalledWith('s1');
  });

  it('ao cancelar, mantém o cronômetro rodando e bloqueia a navegação', async () => {
    const cronometro = TestBed.inject(Cronometro);
    cronometro.iniciar('s1');
    const pausarEspiao = vi.spyOn(cronometro, 'pausar');
    const fixture = montarDialogo();

    const resultado$ = TestBed.runInInjectionContext(() =>
      sairSessaoGuard(componenteFalso('s1'), {} as never, {} as never, {} as never),
    );
    fixture.detectChanges();

    const promessa = firstValueFrom(resultado$ as never);
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.btn-secondary')!
      .click();
    const resultado = await promessa;

    expect(resultado).toBe(false);
    expect(pausarEspiao).not.toHaveBeenCalled();
    expect(cronometro.ativo()).toBe(true);
  });
});
