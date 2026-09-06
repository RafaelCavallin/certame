import { TestBed } from '@angular/core/testing';
import { RespostaRegistrada } from '../../../core/api/modelos-estudo';
import { GabaritoPainel } from './gabarito-painel';

function respostaBase(alteracoes: Partial<RespostaRegistrada> = {}): RespostaRegistrada {
  return {
    questaoId: 'q1',
    marcada: 'A',
    correta: true,
    gabarito: 'A',
    comentario: null,
    estatistica: null,
    tempoMs: 1000,
    respondidaEm: '2026-09-01T00:00:00Z',
    ...alteracoes,
  };
}

describe('GabaritoPainel', () => {
  it('exibe resposta correta, o gabarito e a mensagem de estatística insuficiente', () => {
    // Arrange
    const fixture = TestBed.createComponent(GabaritoPainel);
    fixture.componentRef.setInput('resposta', respostaBase());

    // Act
    fixture.detectChanges();

    // Assert
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Resposta correta');
    expect(texto).toContain('Gabarito: A');
    expect(texto).toContain('Sem estatística suficiente.');
  });

  it('exibe resposta incorreta quando a marcação diverge do gabarito', () => {
    // Arrange
    const fixture = TestBed.createComponent(GabaritoPainel);
    fixture.componentRef.setInput('resposta', respostaBase({ correta: false }));

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain('Resposta incorreta');
  });

  it('exibe a estatística de acerto e tempo médio quando presente', () => {
    // Arrange
    const fixture = TestBed.createComponent(GabaritoPainel);
    fixture.componentRef.setInput(
      'resposta',
      respostaBase({ estatistica: { pctAcerto: 72, totalRespostas: 40, tempoMedioSegundos: 55 } }),
    );

    // Act
    fixture.detectChanges();

    // Assert
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Taxa de acerto: 72%');
    expect(texto).toContain('Tempo médio: 55s');
    expect(texto).not.toContain('Sem estatística suficiente.');
  });

  it('exibe o comentário quando presente', () => {
    // Arrange
    const fixture = TestBed.createComponent(GabaritoPainel);
    fixture.componentRef.setInput(
      'resposta',
      respostaBase({ comentario: 'Cuidado com a pegadinha.' }),
    );

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain('Cuidado com a pegadinha.');
  });
});
