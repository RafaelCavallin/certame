import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Cronometro } from './cronometro';

function setup() {
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting()],
  });
  return {
    cronometro: TestBed.inject(Cronometro),
    http: TestBed.inject(HttpTestingController),
  };
}

describe('Cronometro', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    vi.useRealTimers();
  });

  it('mantém o tempo contínuo durante a sessão', () => {
    const { cronometro, http } = setup();
    cronometro.iniciar('s1');
    vi.advanceTimersByTime(3000);
    cronometro.pausar();
    http.expectOne('/api/v1/estudo/sessoes/s1/pausar').flush(null);
    cronometro.retomar();
    http.expectOne('/api/v1/estudo/sessoes/s1/retomar').flush(null);
    vi.advanceTimersByTime(1000);
    expect(cronometro.segundos()).toBe(5);
  });

  it('acumula a pausa e retoma quando volta a ficar visível', () => {
    const { cronometro, http } = setup();
    cronometro.iniciar('s1');
    vi.advanceTimersByTime(1000);
    cronometro.pausar();
    http.expectOne('/api/v1/estudo/sessoes/s1/pausar').flush(null);
    vi.advanceTimersByTime(2000);
    cronometro.retomar();
    http.expectOne('/api/v1/estudo/sessoes/s1/retomar').flush(null);
    expect(cronometro.pausadoMs()).toBeGreaterThanOrEqual(2000);
    expect(cronometro.segundos()).toBe(2);
  });

  it('reinicia o estado de pausa ao iniciar uma nova sessão', () => {
    const { cronometro, http } = setup();
    cronometro.iniciar('s1');
    vi.advanceTimersByTime(1000);
    cronometro.pausar();
    http.expectOne('/api/v1/estudo/sessoes/s1/pausar').flush(null);
    vi.advanceTimersByTime(2000);

    cronometro.iniciar('s2');
    vi.advanceTimersByTime(3000);

    expect(cronometro.pausado()).toBe(false);
    expect(cronometro.pausadoMs()).toBe(0);
    expect(cronometro.segundos()).toBe(3);
  });

  it('iniciar uma nova sessão zera o contador mesmo com uma sessão anterior ainda ativa', () => {
    const { cronometro } = setup();
    cronometro.iniciar('s1', 50);
    vi.advanceTimersByTime(2000);
    expect(cronometro.segundos()).toBe(52);

    cronometro.iniciar('s2', 0);
    expect(cronometro.segundos()).toBe(0);
    vi.advanceTimersByTime(1000);
    expect(cronometro.segundos()).toBe(1);
  });

  it('parar com o id de uma sessão diferente não desliga o relógio atual', () => {
    const { cronometro } = setup();
    cronometro.iniciar('s2');
    vi.advanceTimersByTime(1000);

    cronometro.parar('s1');

    expect(cronometro.ativo()).toBe(true);
    vi.advanceTimersByTime(1000);
    expect(cronometro.segundos()).toBe(2);
  });

  it('parar sem argumentos ou com o id correto desliga o relógio', () => {
    const { cronometro } = setup();
    cronometro.iniciar('s1');
    vi.advanceTimersByTime(1000);

    cronometro.parar('s1');

    expect(cronometro.ativo()).toBe(false);
    vi.advanceTimersByTime(1000);
    expect(cronometro.segundos()).toBe(1);
  });

  it('pausa manual não é desfeita ao voltar para a aba', () => {
    const { cronometro, http } = setup();
    cronometro.iniciar('s1');
    vi.advanceTimersByTime(1000);
    cronometro.pausar();
    http.expectOne('/api/v1/estudo/sessoes/s1/pausar').flush(null);

    Object.defineProperty(document, 'hidden', { value: true, configurable: true });
    document.dispatchEvent(new Event('visibilitychange'));
    Object.defineProperty(document, 'hidden', { value: false, configurable: true });
    document.dispatchEvent(new Event('visibilitychange'));

    expect(cronometro.pausado()).toBe(true);
    Object.defineProperty(document, 'hidden', { value: false, configurable: true });
  });
});
