import { TestBed } from '@angular/core/testing';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { EditalCard } from './edital-card';
import { EditalResumo } from '../../../core/api/modelos-edital';

registerLocaleData(localePt, 'pt-BR');

const editalBase: EditalResumo = {
  id: '1',
  nome: 'TRF 1ª Região — Analista Judiciário',
  status: 'ABERTO',
  vagas: 1500,
  prazoInscricao: '2026-08-30',
  urlOficial: null,
  banca: { id: 'b1', nome: 'Cebraspe', sigla: 'Cebraspe' },
  orgao: { id: 'o1', nome: 'TRF1', sigla: 'TRF1' },
  disciplinas: [
    { id: 'd1', nome: 'Direito Constitucional' },
    { id: 'd2', nome: 'Português' },
  ],
};

function montar(edital: EditalResumo) {
  TestBed.configureTestingModule({ providers: [{ provide: LOCALE_ID, useValue: 'pt-BR' }] });
  const fixture = TestBed.createComponent(EditalCard);
  fixture.componentRef.setInput('edital', edital);
  fixture.detectChanges();
  return fixture;
}

describe('EditalCard', () => {
  it('formata vagas e prazo em pt-BR', () => {
    const fixture = montar(editalBase);
    expect(fixture.nativeElement.textContent).toContain('1.500 vagas');
    expect(fixture.nativeElement.textContent).toContain('30/08/2026');
  });

  it('exibe Vagas a definir e Data a definir quando vagas e prazo são nulos', () => {
    const fixture = montar({ ...editalBase, vagas: null, prazoInscricao: null });
    expect(fixture.nativeElement.textContent).toContain('Vagas a definir');
    expect(fixture.nativeElement.textContent).toContain('Data a definir');
    expect(fixture.nativeElement.textContent).not.toContain('null');
  });

  it('exibe o rótulo textual de status, a banca, o órgão e as disciplinas como tag', () => {
    const fixture = montar(editalBase);
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Aberto');
    expect(texto).toContain('Cebraspe');
    expect(texto).toContain('TRF1');
    const tags = fixture.nativeElement.querySelectorAll('.disciplinas .tag');
    expect(tags).toHaveLength(2);
    expect(tags[0].textContent).toContain('Direito Constitucional');
  });
});
