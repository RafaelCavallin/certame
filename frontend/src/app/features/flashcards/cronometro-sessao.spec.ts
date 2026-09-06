import { CronometroSessao } from './cronometro-sessao';

describe('CronometroSessao', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    vi.useRealTimers();
    Object.defineProperty(document, 'hidden', { value: false, configurable: true });
  });

  it('acumula um segundo por tique', () => {
    const cronometro = new CronometroSessao();

    cronometro.iniciar();
    vi.advanceTimersByTime(3000);

    expect(cronometro.segundos()).toBe(3);
  });

  it('pausa a contagem quando document.hidden fica verdadeiro', () => {
    const cronometro = new CronometroSessao();
    cronometro.iniciar();
    vi.advanceTimersByTime(2000);

    Object.defineProperty(document, 'hidden', { value: true, configurable: true });
    vi.advanceTimersByTime(3000);

    expect(cronometro.segundos()).toBe(2);
  });
});
