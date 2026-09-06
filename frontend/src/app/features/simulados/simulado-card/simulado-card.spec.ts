import { TestBed } from '@angular/core/testing';
import { SimuladoCard } from './simulado-card';
import { SimuladoConcluido, SimuladoResumo } from '../../../core/models/modelos-simulado';

const disponivel: SimuladoResumo = {
  id: '1',
  codigo: 'SM-001',
  nome: 'Prova',
  banca: null,
  numeroQuestoes: 10,
  duracaoSegundos: 600,
  estado: 'DISPONIVEL',
  janelaAbertura: '',
  janelaEncerramento: '',
  tentativaEmAndamentoId: null,
};
const agendado: SimuladoResumo = { ...disponivel, id: '2', estado: 'AGENDADO' };
const concluido: SimuladoConcluido = {
  id: '1',
  codigo: 'SM-001',
  nome: 'Prova',
  banca: null,
  numeroQuestoes: 10,
  duracaoSegundos: 600,
  tentativaId: 't1',
  nota: 80,
  acertos: 8,
  erros: 2,
  naoRespondidas: 0,
  tempoTotalSegundos: 500,
  concluidoEm: '2026-08-31T10:00:00Z',
  podeRefazer: false,
};

function montar(simulado: SimuladoResumo | SimuladoConcluido) {
  const fixture = TestBed.createComponent(SimuladoCard);
  fixture.componentRef.setInput('simulado', simulado);
  fixture.detectChanges();
  return fixture;
}

describe('SimuladoCard', () => {
  it('identifica um simulado concluído', () => {
    const fixture = montar(concluido);
    expect(fixture.componentInstance.concluido()).toBe(true);
  });

  it('formata a duração em horas', () => {
    const fixture = TestBed.createComponent(SimuladoCard);
    expect(fixture.componentInstance.duracao(3600)).toBe('1h');
  });

  it('exibe o rótulo Disponível e o botão Iniciar quando não há tentativa em andamento', () => {
    const fixture = montar(disponivel);
    expect(fixture.nativeElement.textContent).toContain('Disponível');
    expect(fixture.nativeElement.textContent).toContain('Iniciar');
  });

  it('exibe Retomar quando já existe tentativa em andamento', () => {
    const fixture = montar({ ...disponivel, tentativaEmAndamentoId: 'tent-1' });
    expect(fixture.nativeElement.textContent).toContain('Retomar');
  });

  it('exibe o rótulo Agendado e não mostra botão de iniciar', () => {
    const fixture = montar(agendado);
    expect(fixture.nativeElement.textContent).toContain('Agendado');
    expect(fixture.nativeElement.textContent).not.toContain('Iniciar');
  });

  it('não exibe o botão Refazer treino mesmo quando podeRefazer é verdadeiro', () => {
    const fixture = montar({ ...concluido, podeRefazer: true });
    expect(fixture.nativeElement.textContent).not.toContain('Refazer treino');
  });

  it('emite iniciar ao clicar no botão', () => {
    const fixture = montar(disponivel);
    const emitido = vi.fn();
    fixture.componentInstance.iniciar.subscribe(emitido);

    fixture.nativeElement.querySelector('button.btn-primary').click();

    expect(emitido).toHaveBeenCalled();
  });

  it('emite revisar ao clicar no botão', () => {
    const fixture = montar({ ...concluido, podeRefazer: true });
    const revisado = vi.fn();
    fixture.componentInstance.revisar.subscribe(revisado);

    fixture.nativeElement.querySelector('button.btn-secondary').click();

    expect(revisado).toHaveBeenCalled();
  });
});
