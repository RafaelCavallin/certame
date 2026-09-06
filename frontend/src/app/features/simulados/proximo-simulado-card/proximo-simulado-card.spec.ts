import { TestBed } from '@angular/core/testing';
import { ProximoSimuladoCard } from './proximo-simulado-card';

describe('ProximoSimuladoCard', () => {
  it('exibe contagem regressiva até a abertura', () => {
    const fixture = TestBed.createComponent(ProximoSimuladoCard);
    fixture.componentRef.setInput('simulado', {
      id: '1',
      codigo: 'SM-001',
      nome: 'Prova',
      banca: null,
      numeroQuestoes: 10,
      duracaoSegundos: 600,
      estado: 'AGENDADO',
      janelaAbertura: new Date(Date.now() + 2 * 86400000).toISOString(),
      janelaEncerramento: new Date().toISOString(),
      tentativaEmAndamentoId: null,
    });
    fixture.detectChanges();
    expect(fixture.componentInstance.restante()).toMatch(/^[12]d/);
  });
  it('exibe apenas horas quando a abertura é no mesmo dia', () => {
    const fixture = TestBed.createComponent(ProximoSimuladoCard);
    fixture.componentRef.setInput('simulado', {
      id: '1',
      codigo: 'SM-001',
      nome: 'Prova',
      banca: null,
      numeroQuestoes: 10,
      duracaoSegundos: 600,
      estado: 'AGENDADO',
      janelaAbertura: new Date(Date.now() + 3 * 3600000).toISOString(),
      janelaEncerramento: new Date().toISOString(),
      tentativaEmAndamentoId: null,
    });
    fixture.detectChanges();
    expect(fixture.componentInstance.restante()).toMatch(/^\dh$/);
  });
  it('formata a duração em minutos quando menor que uma hora', () => {
    const fixture = TestBed.createComponent(ProximoSimuladoCard);
    expect(fixture.componentInstance.duracao(1800)).toBe('30 min');
  });
  it('formata a duração em horas quando maior ou igual a uma hora', () => {
    const fixture = TestBed.createComponent(ProximoSimuladoCard);
    expect(fixture.componentInstance.duracao(7200)).toBe('2h');
  });
});
