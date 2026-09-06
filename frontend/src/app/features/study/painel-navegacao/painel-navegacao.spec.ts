import { TestBed } from '@angular/core/testing';
import { PainelNavegacao } from './painel-navegacao';

function montar(
  total: number,
  atual: number,
  respondidas: Set<number>,
  modo?: 'FOCO' | 'CONTEXTO' | 'PROVA',
) {
  const fixture = TestBed.createComponent(PainelNavegacao);
  fixture.componentRef.setInput('total', total);
  fixture.componentRef.setInput('atual', atual);
  fixture.componentRef.setInput('respondidas', respondidas);
  if (modo) fixture.componentRef.setInput('modo', modo);
  fixture.detectChanges();
  return fixture;
}

describe('PainelNavegacao', () => {
  it('renderiza um botão para cada questão da série', () => {
    // Arrange & Act
    const fixture = montar(5, 0, new Set());

    // Assert
    expect(fixture.nativeElement.querySelectorAll('button')).toHaveLength(5);
  });

  it('marca a questão atual como ativa e as demais como respondidas quando aplicável', () => {
    // Arrange & Act
    const fixture = montar(3, 1, new Set([0, 2]));

    // Assert
    const botoes = fixture.nativeElement.querySelectorAll('button');
    expect(botoes[1].classList.contains('ativa')).toBe(true);
    expect(botoes[0].classList.contains('respondida')).toBe(true);
    expect(botoes[2].classList.contains('respondida')).toBe(true);
    expect(botoes[1].classList.contains('respondida')).toBe(false);
  });

  it('emite selecionar com o índice do botão clicado', () => {
    // Arrange
    const fixture = montar(3, 0, new Set());
    const selecionar = vi.fn();
    fixture.componentInstance.selecionar.subscribe(selecionar);

    // Act
    (fixture.nativeElement.querySelectorAll('button')[2] as HTMLButtonElement).click();

    // Assert
    expect(selecionar).toHaveBeenCalledWith(2);
  });

  it('usa o layout em grade com o título "Navegação" no Modo Contexto', () => {
    // Arrange & Act
    const fixture = montar(4, 0, new Set(), 'CONTEXTO');

    // Assert
    expect(fixture.nativeElement.querySelector('.navegacao-serie--grade')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Navegação');
  });

  it('usa o layout compacto sem título no Modo Foco', () => {
    // Arrange & Act
    const fixture = montar(4, 0, new Set(), 'FOCO');

    // Assert
    expect(fixture.nativeElement.querySelector('.navegacao-serie--grade')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Navegação');
  });
});
