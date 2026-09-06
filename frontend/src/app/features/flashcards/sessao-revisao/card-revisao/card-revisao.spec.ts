import { TestBed } from '@angular/core/testing';
import { CardRevisao as CardRevisaoModelo } from '../../../../core/api/modelos-revisao';
import { CardRevisao } from './card-revisao';

function card(overrides: Partial<CardRevisaoModelo> = {}): CardRevisaoModelo {
  return {
    id: 'c1',
    frente: 'Efeitos da decisão de mérito em ADI',
    verso: 'Eficácia erga omnes.',
    deckId: 'd1',
    deckNome: 'Constitucional',
    disciplinaNome: 'Direito Constitucional',
    assuntoNome: 'Controle',
    estado: 'REVISAO',
    memoria: {
      estabilidade: 8.4,
      dificuldade: 5.1,
      retencaoAtual: 0.87,
      rotulo: 'Estabilidade 8 dias · dificuldade 5,1 de 10 · retenção hoje 87%',
    },
    previsoes: [],
    ...overrides,
  };
}

describe('CardRevisao', () => {
  function criar(cardValor: CardRevisaoModelo, revelado: boolean) {
    const fixture = TestBed.createComponent(CardRevisao);
    fixture.componentRef.setInput('card', cardValor);
    fixture.componentRef.setInput('revelado', revelado);
    fixture.detectChanges();
    return fixture;
  }

  it('mostra apenas a frente quando não revelado', () => {
    const fixture = criar(card(), false);

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Efeitos da decisão de mérito em ADI');
    expect(texto).not.toContain('Eficácia erga omnes.');
  });

  it('mostra verso, disciplina, assunto e a linha de memória quando revelado', () => {
    const fixture = criar(card(), true);

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Eficácia erga omnes.');
    expect(texto).toContain('Direito Constitucional');
    expect(texto).toContain('Controle');
    expect(texto).toContain('Estabilidade 8 dias');
  });
});
