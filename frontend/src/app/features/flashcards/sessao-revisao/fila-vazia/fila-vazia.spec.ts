import { TestBed } from '@angular/core/testing';
import { FilaVazia } from './fila-vazia';

describe('FilaVazia', () => {
  it('informa que a fila do dia terminou, sem oferecer antecipação', () => {
    const fixture = TestBed.createComponent(FilaVazia);
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Nada vencido por hoje');
    expect(texto).not.toMatch(/antecipar/i);
  });

  it('emite voltar ao clicar na ação', () => {
    const fixture = TestBed.createComponent(FilaVazia);
    fixture.detectChanges();
    const emitido = vi.fn();
    fixture.componentInstance.voltar.subscribe(emitido);

    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();

    expect(emitido).toHaveBeenCalled();
  });
});
