import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SimuladoApi } from '../../../core/api/simulado-api';
import { SimuladoResultadoPage } from './simulado-resultado-page';

const resultado = {
  tentativaId: 't1',
  simuladoId: 's1',
  codigo: 'SM-001',
  nome: 'Prova',
  tipo: 'TREINO' as const,
  nota: 80,
  acertos: 8,
  erros: 1,
  naoRespondidas: 1,
  total: 10,
  tempoTotalSegundos: 600,
  finalizadaEm: '2026-08-31T10:00:00Z',
  notaRegistrada: 70,
  correcao: {
    sessaoId: 'x',
    pctAcerto: 80,
    acertos: 8,
    erros: 1,
    naoRespondidas: 1,
    total: 10,
    tempoTotalSegundos: 600,
    finalizadaEm: '',
    itens: [
      {
        ordem: 1,
        questaoId: 'q1',
        codigo: 'Q-1',
        enunciadoResumo: 'Enunciado',
        disciplina: null,
        gabarito: 'A',
        marcada: 'B',
        correta: false,
        comentario: 'Revise.',
      },
    ],
  },
};

function montar(
  paramId: string | null,
  tentativaId: string | null,
  api: { resultado: ReturnType<typeof vi.fn> },
) {
  TestBed.configureTestingModule({
    imports: [SimuladoResultadoPage],
    providers: [
      { provide: SimuladoApi, useValue: api },
      {
        provide: ActivatedRoute,
        useValue: {
          snapshot: { paramMap: { get: () => paramId }, queryParamMap: { get: () => tentativaId } },
        },
      },
    ],
  });
  return TestBed.createComponent(SimuladoResultadoPage);
}

describe('SimuladoResultadoPage', () => {
  it('carrega aviso de treino e revisão questão a questão', async () => {
    const fixture = montar('s1', 't1', { resultado: vi.fn(() => of(resultado)) });
    const page = fixture.componentInstance;

    await page.carregar();
    fixture.detectChanges();

    expect(page['resultado']()?.tipo).toBe('TREINO');
    expect(page['resultado']()?.correcao.itens).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('tentativa de treino');
  });

  it('sinaliza resultado não encontrado quando faltam parâmetros na rota', async () => {
    const fixture = montar(null, null, { resultado: vi.fn() });
    const page = fixture.componentInstance;

    await page.carregar();

    expect(page['erro']()).toBe('Resultado não encontrado.');
  });

  it('sinaliza erro quando a busca do resultado falha', async () => {
    const fixture = montar('s1', 't1', {
      resultado: vi.fn(() => throwError(() => new Error('falhou'))),
    });
    const page = fixture.componentInstance;

    await page.carregar();

    expect(page['erro']()).toBe('Não foi possível carregar o resultado.');
  });

  it('formata o tempo total em minutos e em horas', () => {
    const fixture = montar('s1', 't1', { resultado: vi.fn(() => of(resultado)) });
    expect(fixture.componentInstance.tempo(1800)).toBe('30 min');
    expect(fixture.componentInstance.tempo(5400)).toBe('1h 30min');
  });
});
