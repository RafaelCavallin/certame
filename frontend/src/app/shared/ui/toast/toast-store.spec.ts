import { ToastStore } from './toast-store';

describe('ToastStore', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('publica a mensagem e a expira sozinha após o tempo previsto', () => {
    const store = new ToastStore();

    store.publicar('Flashcard criado.');

    expect(store.mensagem()).toBe('Flashcard criado.');
    vi.advanceTimersByTime(2799);
    expect(store.mensagem()).toBe('Flashcard criado.');
    vi.advanceTimersByTime(1);
    expect(store.mensagem()).toBeNull();
  });

  it('reinicia o temporizador quando uma nova mensagem chega antes da anterior expirar', () => {
    const store = new ToastStore();

    store.publicar('Card arquivado.');
    vi.advanceTimersByTime(2000);
    store.publicar('Card excluído.');
    vi.advanceTimersByTime(2000);

    expect(store.mensagem()).toBe('Card excluído.');
    vi.advanceTimersByTime(800);
    expect(store.mensagem()).toBeNull();
  });

  it('limpa a mensagem sem esperar o temporizador', () => {
    const store = new ToastStore();

    store.publicar('Deck criado.');
    store.limpar();

    expect(store.mensagem()).toBeNull();
  });
});
