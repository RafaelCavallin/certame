import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EvolucaoChart } from './evolucao-chart';

describe('EvolucaoChart', () => {
  let fixture: ComponentFixture<EvolucaoChart>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [EvolucaoChart] }).compileComponents();
    fixture = TestBed.createComponent(EvolucaoChart);
  });
  it('TU-44 formata a série e desenha ponto único sem linha', () => {
    const evolucao = {
      semanas: [{ semana: '2026-08-24', rotulo: '24/08', volume: 1482, pctAcerto: 70 }],
      pctMediaPeriodo: 70,
      melhorSemana: null,
      volumeTotal: 1482,
    };
    fixture.componentRef.setInput('evolucao', evolucao);
    fixture.componentRef.setInput('segmentos', [[{ x: 329, y: 85, semana: '24/08', pct: 70 }]]);
    fixture.componentRef.setInput('serie', [
      { semana: '24/08', percentual: '70%', volume: '1.482' },
    ]);
    fixture.detectChanges();
    expect(fixture.componentInstance.linePath()).toBe('');
    expect(fixture.nativeElement.querySelectorAll('circle')).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('1.482');
  });
});
