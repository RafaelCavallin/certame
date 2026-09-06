import { TestBed } from '@angular/core/testing';
import { SeletorModo } from './seletor-modo';

function botao(
  fixture: ReturnType<typeof TestBed.createComponent<SeletorModo>>,
  texto: string,
): HTMLButtonElement {
  return Array.from(fixture.nativeElement.querySelectorAll('button')).find(
    (b) => (b as HTMLButtonElement).textContent?.trim() === texto,
  ) as HTMLButtonElement;
}

describe('SeletorModo', () => {
  it('emite o modo escolhido ao clicar em um botão habilitado', () => {
    // Arrange
    const fixture = TestBed.createComponent(SeletorModo);
    fixture.componentRef.setInput('modo', 'FOCO');
    const trocar = vi.fn();
    fixture.componentInstance.trocar.subscribe(trocar);
    fixture.detectChanges();

    // Act
    botao(fixture, 'Contexto').click();

    // Assert
    expect(trocar).toHaveBeenCalledWith('CONTEXTO');
  });

  it('desabilita o botão de iniciar prova quando podeTrocar é falso', () => {
    // Arrange
    const fixture = TestBed.createComponent(SeletorModo);
    fixture.componentRef.setInput('modo', 'FOCO');
    fixture.componentRef.setInput('podeTrocar', false);

    // Act
    fixture.detectChanges();

    // Assert
    expect(botao(fixture, 'Iniciar prova').disabled).toBe(true);
  });

  it('não emite ao clicar em iniciar prova desabilitado', () => {
    // Arrange
    const fixture = TestBed.createComponent(SeletorModo);
    fixture.componentRef.setInput('modo', 'FOCO');
    fixture.componentRef.setInput('podeTrocar', false);
    const trocar = vi.fn();
    fixture.componentInstance.trocar.subscribe(trocar);
    fixture.detectChanges();

    // Act
    fixture.componentInstance.selecionar('PROVA');

    // Assert
    expect(trocar).not.toHaveBeenCalled();
  });

  it('marca como ativo o botão do modo atual', () => {
    // Arrange
    const fixture = TestBed.createComponent(SeletorModo);
    fixture.componentRef.setInput('modo', 'CONTEXTO');

    // Act
    fixture.detectChanges();

    // Assert
    expect(botao(fixture, 'Contexto').classList.contains('ativa')).toBe(true);
    expect(botao(fixture, 'Foco').classList.contains('ativa')).toBe(false);
  });
});
