import { TestBed } from '@angular/core/testing';
import { PrevisaoNota } from '../../../../core/api/modelos-revisao';
import { NotasRevisao } from './notas-revisao';

const previsoes: PrevisaoNota[] = [
  { nota: 'ERREI', minutos: 10, rotulo: '10 min' },
  { nota: 'DIFICIL', minutos: 8640, rotulo: '6 dias' },
  { nota: 'BOM', minutos: 12960, rotulo: '9 dias' },
  { nota: 'FACIL', minutos: 25920, rotulo: '18 dias' },
];

describe('NotasRevisao', () => {
  function criar() {
    const fixture = TestBed.createComponent(NotasRevisao);
    fixture.componentRef.setInput('previsoes', previsoes);
    fixture.detectChanges();
    return fixture;
  }

  it('exibe rótulo, intervalo previsto e atalho para cada nota', () => {
    const fixture = criar();

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Errei');
    expect(texto).toContain('10 min');
    expect(texto).toContain('Fácil');
    expect(texto).toContain('18 dias');
  });

  it('emite a nota escolhida ao clicar no botão', () => {
    const fixture = criar();
    const emitido = vi.fn();
    fixture.componentInstance.classificar.subscribe(emitido);

    const botoes = fixture.nativeElement.querySelectorAll('button');
    (botoes[2] as HTMLButtonElement).click();

    expect(emitido).toHaveBeenCalledWith('BOM');
  });

  it('não esmaece o rótulo de intervalo, para manter o contraste mínimo de 4,5:1 do botão Bom', () => {
    const fixture = criar();

    const intervalos = fixture.nativeElement.querySelectorAll(
      'button span:nth-child(2)',
    ) as NodeListOf<HTMLSpanElement>;
    intervalos.forEach((intervalo) => {
      expect(intervalo.style.opacity).toBe('');
    });
  });
});
