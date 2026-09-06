import { TestBed } from '@angular/core/testing';
import { ResumoSessao } from '../../../../core/api/modelos-revisao';
import { ResumoRevisao } from './resumo-revisao';

function resumo(overrides: Partial<ResumoSessao> = {}): ResumoSessao {
  return {
    revisados: 24,
    classificacoes: 29,
    distribuicao: { errei: 3, dificil: 5, bom: 12, facil: 4 },
    tempoMs: 512000,
    tempoRotulo: '8 min 32 s',
    proximosVencimentos: [
      { faixa: 'AMANHA', rotulo: 'Amanhã', quantidade: 6 },
      { faixa: 'PROXIMOS_7_DIAS', rotulo: 'Nos próximos 7 dias', quantidade: 18 },
      { faixa: 'DEPOIS_DE_7_DIAS', rotulo: 'Depois de 7 dias', quantidade: 41 },
    ],
    ...overrides,
  };
}

describe('ResumoRevisao', () => {
  function criar(valor: ResumoSessao) {
    const fixture = TestBed.createComponent(ResumoRevisao);
    fixture.componentRef.setInput('resumo', valor);
    fixture.detectChanges();
    return fixture;
  }

  it('exibe cards revisados, distribuição, tempo total e próximos vencimentos', () => {
    const fixture = criar(resumo());

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('24');
    expect(texto).toContain('8 min 32 s');
    expect(texto).toContain('Nos próximos 7 dias');
    expect(texto).toContain('18');
  });

  it('emite reiniciar e voltar ao clicar nas ações', () => {
    const fixture = criar(resumo());
    const reiniciar = vi.fn();
    const voltar = vi.fn();
    fixture.componentInstance.reiniciar.subscribe(reiniciar);
    fixture.componentInstance.voltar.subscribe(voltar);

    const botoes = fixture.nativeElement.querySelectorAll('button');
    (botoes[0] as HTMLButtonElement).click();
    (botoes[1] as HTMLButtonElement).click();

    expect(reiniciar).toHaveBeenCalled();
    expect(voltar).toHaveBeenCalled();
  });
});
