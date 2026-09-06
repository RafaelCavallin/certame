import { provideRouter, Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DeckDetalhe } from '../../../core/api/modelos-flashcard';
import { DialogService } from '../../../shared/ui/dialog/dialog-service';
import { DecksStore } from '../decks-store';
import { DecksPage } from './decks-page';

function deckDetalhe(overrides: Partial<DeckDetalhe> = {}): DeckDetalhe {
  return {
    id: 'd1',
    nome: 'Constitucional — controle',
    descricao: 'ADI, ADC, ADPF',
    totalCards: 214,
    vencendoHoje: 137,
    novos: 12,
    limites: { novosDia: 10, revisoesDia: 60, intervaloMaxDias: 180, retencaoAlvo: 0.9 },
    limitesRotulo: '10 novos/dia · 60 revisões/dia · máx. 180 d',
    retencaoRotulo: 'FSRS · retenção alvo 90%',
    editavel: true,
    aviso: null,
    ...overrides,
  };
}

describe('DecksPage', () => {
  let store: {
    carregar: ReturnType<typeof vi.fn>;
    decks: ReturnType<typeof vi.fn>;
    carregando: ReturnType<typeof vi.fn>;
    excluir: ReturnType<typeof vi.fn>;
  };
  let dialogService: { abrir: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    store = {
      carregar: vi.fn(() => Promise.resolve()),
      decks: vi.fn(() => [deckDetalhe()]),
      carregando: vi.fn(() => false),
      excluir: vi.fn(() => Promise.resolve(0)),
    };
    dialogService = { abrir: vi.fn(() => of(undefined)) };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: DecksStore, useValue: store },
        { provide: DialogService, useValue: dialogService },
      ],
    });
  });

  it('carrega os decks ao iniciar', () => {
    TestBed.createComponent(DecksPage);

    expect(store.carregar).toHaveBeenCalled();
  });

  it('mostra o total de decks e de cards na estatística', () => {
    const fixture = TestBed.createComponent(DecksPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent as string).toContain('1 decks · 214 cards');
  });

  it('abre o diálogo de novo deck sem contexto', () => {
    const fixture = TestBed.createComponent(DecksPage);
    fixture.componentInstance.abrirNovo();

    expect(dialogService.abrir).toHaveBeenCalledWith(expect.anything(), { contexto: null });
  });

  it('abre o diálogo de edição com o deck selecionado', () => {
    const fixture = TestBed.createComponent(DecksPage);
    const deck = deckDetalhe();
    fixture.componentInstance.editar(deck);

    expect(dialogService.abrir).toHaveBeenCalledWith(expect.anything(), { contexto: deck });
  });

  it('exclui o deck confirmado e limpa o pedido de exclusão', () => {
    const fixture = TestBed.createComponent(DecksPage);
    const deck = deckDetalhe();
    fixture.componentInstance.pedirExclusao(deck);

    fixture.componentInstance.confirmarExclusao();

    expect(store.excluir).toHaveBeenCalledWith('d1');
    expect(fixture.componentInstance.excluindo()).toBeNull();
  });

  it('navega para a sessão de revisão do deck ao estudar', () => {
    const fixture = TestBed.createComponent(DecksPage);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate');

    fixture.componentInstance.estudar(deckDetalhe());

    expect(navigate).toHaveBeenCalledWith(['/flashcards/estudar'], { queryParams: { deck: 'd1' } });
  });

  it('navega com o escopo sem-deck ao estudar a linha virtual Sem deck', () => {
    const fixture = TestBed.createComponent(DecksPage);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate');

    fixture.componentInstance.estudar(deckDetalhe({ id: null, nome: 'Sem deck', editavel: false }));

    expect(navigate).toHaveBeenCalledWith(['/flashcards/estudar'], {
      queryParams: { deck: 'sem-deck' },
    });
  });

  it('usa uma classe de folha de estilo para a grade de decks, sem grid inline, para permitir empilhar em telas estreitas', () => {
    const fixture = TestBed.createComponent(DecksPage);
    fixture.detectChanges();

    const grade = fixture.nativeElement.querySelector('.grade-decks') as HTMLElement;
    expect(grade).toBeTruthy();
    expect(grade.style.display).toBe('');
    expect(grade.style.gridTemplateColumns).toBe('');
  });
});
