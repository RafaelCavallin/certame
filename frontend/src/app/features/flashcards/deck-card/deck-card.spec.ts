import { TestBed } from '@angular/core/testing';
import { DeckDetalhe } from '../../../core/api/modelos-flashcard';
import { DeckCard } from './deck-card';

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

describe('DeckCard', () => {
  function criar(deck: DeckDetalhe) {
    const fixture = TestBed.createComponent(DeckCard);
    fixture.componentRef.setInput('deck', deck);
    fixture.detectChanges();
    return fixture;
  }

  it('exibe nome, descrição, contadores e resumo dos limites', () => {
    const fixture = criar(deckDetalhe());

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Constitucional — controle');
    expect(texto).toContain('ADI, ADC, ADPF');
    expect(texto).toContain('214');
    expect(texto).toContain('137');
    expect(texto).toContain('10 novos/dia · 60 revisões/dia · máx. 180 d');
    expect(texto).toContain('Estudar este deck');
  });

  it('esconde as ações de editar e excluir quando o deck não é editável', () => {
    const fixture = criar(deckDetalhe({ id: null, nome: 'Sem deck', editavel: false }));

    const botoes = fixture.nativeElement.querySelectorAll(
      'button[title="Configurar deck"], button[title="Excluir deck"]',
    );
    expect(botoes.length).toBe(0);
    expect(fixture.nativeElement.textContent as string).toContain('Padrão');
  });

  it('mostra o aviso de fila acumulada quando presente', () => {
    const fixture = criar(
      deckDetalhe({
        aviso: {
          tipo: 'FILA_ACUMULADA',
          mensagem: '137 cards vencidos, mais que o dobro do limite diário deste deck.',
          vencidos: 137,
          limiteDiario: 60,
        },
      }),
    );

    expect(fixture.nativeElement.textContent as string).toContain('137 cards vencidos');
  });

  it('emite o deck ao clicar em estudar este deck', () => {
    const fixture = criar(deckDetalhe());
    const emitido = vi.fn();
    fixture.componentInstance.estudar.subscribe(emitido);

    const botao = Array.from(fixture.nativeElement.querySelectorAll('button')).find((b) =>
      (b as HTMLElement).textContent?.includes('Estudar este deck'),
    ) as HTMLButtonElement;
    botao.click();

    expect(emitido).toHaveBeenCalledWith(expect.objectContaining({ id: 'd1' }));
  });
});
