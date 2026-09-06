import { TestBed } from '@angular/core/testing';
import { BarraAcao } from './barra-acao';

describe('BarraAcao', () => {
  it('desabilita anterior na primeira questão e próxima na última', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);
    fixture.componentRef.setInput('primeiro', true);
    fixture.componentRef.setInput('ultimo', true);

    // Act
    fixture.detectChanges();

    // Assert
    const botoes = fixture.nativeElement.querySelectorAll('button');
    expect(
      Array.from(botoes).find((b) => (b as HTMLButtonElement).textContent?.trim() === 'Anterior'),
    ).toHaveProperty('disabled', true);
    expect(
      Array.from(botoes).find((b) => (b as HTMLButtonElement).textContent?.trim() === 'Próxima'),
    ).toHaveProperty('disabled', true);
  });

  it('mostra o botão de finalizar prova quando em modo prova', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);
    fixture.componentRef.setInput('prova', true);

    // Act
    fixture.detectChanges();

    // Assert
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Finalizar prova');
    expect(texto).not.toContain('Responder e continuar');
  });

  it('mostra "Responder" na última questão fora do modo prova', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);
    fixture.componentRef.setInput('ultimo', true);

    // Act
    fixture.detectChanges();

    // Assert
    const botaoConfirmar = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Responder',
    );
    expect(botaoConfirmar).toBeTruthy();
  });

  it('emite os eventos correspondentes a cada botão clicado', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);
    const component = fixture.componentInstance;
    const anterior = vi.fn();
    const proxima = vi.fn();
    const confirmar = vi.fn();
    component.anterior.subscribe(anterior);
    component.proxima.subscribe(proxima);
    component.confirmar.subscribe(confirmar);
    fixture.detectChanges();

    // Act
    const botoes = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];
    botoes.find((b) => b.textContent?.trim() === 'Anterior')!.click();
    botoes.find((b) => b.textContent?.includes('Responder'))!.click();
    botoes.find((b) => b.textContent?.trim() === 'Próxima')!.click();

    // Assert
    expect(anterior).toHaveBeenCalledOnce();
    expect(proxima).toHaveBeenCalledOnce();
    expect(confirmar).toHaveBeenCalledOnce();
  });

  it('emite finalizar ao clicar em finalizar prova', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);
    const component = fixture.componentInstance;
    fixture.componentRef.setInput('prova', true);
    const finalizar = vi.fn();
    component.finalizar.subscribe(finalizar);
    fixture.detectChanges();

    // Act
    const botao = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Finalizar prova',
    ) as HTMLButtonElement;
    botao.click();

    // Assert
    expect(finalizar).toHaveBeenCalledOnce();
  });

  it('mostra o botão de finalizar sessão fora do modo prova', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);

    // Act
    fixture.detectChanges();

    // Assert
    const botao = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Finalizar sessão',
    );
    expect(botao).toBeTruthy();
  });

  it('emite finalizar ao clicar em finalizar sessão fora do modo prova', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);
    const finalizar = vi.fn();
    fixture.componentInstance.finalizar.subscribe(finalizar);
    fixture.detectChanges();

    // Act
    const botao = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Finalizar sessão',
    ) as HTMLButtonElement;
    botao.click();

    // Assert
    expect(finalizar).toHaveBeenCalledOnce();
  });

  it('troca o botão principal para "Próxima questão" quando a questão já foi respondida', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);
    fixture.componentRef.setInput('respondida', true);
    const proxima = vi.fn();
    fixture.componentInstance.proxima.subscribe(proxima);
    fixture.detectChanges();

    // Act
    const botao = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Próxima questão',
    ) as HTMLButtonElement;
    botao.click();

    // Assert
    expect(fixture.nativeElement.textContent).not.toContain('Responder e continuar');
    expect(proxima).toHaveBeenCalledOnce();
  });

  it('não duplica a navegação: some com o "Próxima" secundário quando já respondida', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);
    fixture.componentRef.setInput('respondida', true);

    // Act
    fixture.detectChanges();

    // Assert
    const rotulos = Array.from(fixture.nativeElement.querySelectorAll('button')).map((b) =>
      (b as HTMLButtonElement).textContent?.trim(),
    );
    expect(rotulos).toContain('Próxima questão');
    expect(rotulos).not.toContain('Próxima');
  });

  it('mantém o "Próxima" secundário no Modo Prova, onde não há botão de responder', () => {
    // Arrange
    const fixture = TestBed.createComponent(BarraAcao);
    fixture.componentRef.setInput('prova', true);
    fixture.componentRef.setInput('respondida', true);

    // Act
    fixture.detectChanges();

    // Assert
    const rotulos = Array.from(fixture.nativeElement.querySelectorAll('button')).map((b) =>
      (b as HTMLButtonElement).textContent?.trim(),
    );
    expect(rotulos).toContain('Próxima');
  });
});
