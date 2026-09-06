import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DeckSeletor } from './deck-seletor';

describe('DeckSeletor', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<DeckSeletor>>;
  let component: DeckSeletor;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(DeckSeletor);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/v1/flashcards/decks')
      .flush([{ id: 'd1', nome: 'Constitucional', descricao: null, totalCards: 3 }]);
  });

  afterEach(() => http.verify());

  it('emite null ao selecionar "Sem deck"', () => {
    component['aoSelecionar']('sem-deck');

    expect(component['erro']()).toBeNull();
  });

  it('abre o campo de nome ao escolher "+ Novo deck" sem emitir seleção', () => {
    component['aoSelecionar']('__novo_deck__');

    expect(component['criandoNovo']()).toBe(true);
  });

  it('fecha o campo de nome ao trocar para outra opção do select', () => {
    component['aoSelecionar']('__novo_deck__');
    component['nomeNovoDeck'].set('Processual');

    component['aoSelecionar']('d1');

    expect(component['criandoNovo']()).toBe(false);
    expect(component['nomeNovoDeck']()).toBe('');
  });

  it('cria o deck por nome sem fechar o modal e seleciona o deck criado', async () => {
    component['aoSelecionar']('__novo_deck__');
    component['nomeNovoDeck'].set('Processual Civil');
    const emitido: (string | null)[] = [];
    component.selecionado.subscribe((valor) => emitido.push(valor));

    const promessa = component['confirmarNovoDeck']();
    http
      .expectOne('/api/v1/flashcards/decks')
      .flush({ id: 'd2', nome: 'Processual Civil', descricao: null, totalCards: 0 });
    await promessa;

    expect(emitido).toEqual(['d2']);
    expect(component['criandoNovo']()).toBe(false);
    expect(component['decks']().map((d) => d.nome)).toContain('Processual Civil');
  });

  it('não cria o deck quando o nome tem menos de 3 caracteres', async () => {
    component['aoSelecionar']('__novo_deck__');
    component['nomeNovoDeck'].set('ab');

    await component['confirmarNovoDeck']();

    http.expectNone('/api/v1/flashcards/decks');
  });
});
