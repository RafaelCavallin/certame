import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DesempenhoApi } from '../../core/api/desempenho-api';
import { DesempenhoStore } from './desempenho-store';

const semanas = [
  { semana: '2026-07-06', rotulo: '06/07', volume: 10, pctAcerto: 60 },
  { semana: '2026-07-13', rotulo: '13/07', volume: 0, pctAcerto: null },
  { semana: '2026-07-20', rotulo: '20/07', volume: 20, pctAcerto: 70 },
];

describe('DesempenhoStore', () => {
  function configurar() {
    const api = {
      resumo: () =>
        of({
          totalRespostas: 30,
          acertos: 20,
          erros: 10,
          pctAproveitamento: 67,
          possuiHistorico: true,
        }),
      evolucao: () =>
        of({ semanas, pctMediaPeriodo: 67, melhorSemana: semanas[2], volumeTotal: 30 }),
      disciplinas: () => of([]),
      pontosFracos: () => of({ pctMediaUsuario: 67, minimoRespostas: 20, itens: [] }),
    } as unknown as DesempenhoApi;
    TestBed.configureTestingModule({
      providers: [DesempenhoStore, { provide: DesempenhoApi, useValue: api }],
    });
    return TestBed.inject(DesempenhoStore);
  }

  it('TU-38 quebra a linha em segmentos nas lacunas', async () => {
    const store = configurar();
    await store.carregar();
    expect(store.segmentosLinha()).toHaveLength(2);
    expect(store.segmentosLinha()[0]).toHaveLength(1);
    expect(store.segmentosLinha()[1]).toHaveLength(1);
  });

  it('TU-39 preserva lacuna sem converter percentual em zero', async () => {
    const store = configurar();
    await store.carregar();
    expect(store.evolucao()?.semanas[1].pctAcerto).toBeNull();
    expect(
      store
        .segmentosLinha()
        .flat()
        .every((ponto) => ponto.pct !== 0),
    ).toBe(true);
  });

  it('TU-40 monta a série textual com sem atividade', async () => {
    const store = configurar();
    await store.carregar();
    expect(store.serieTextual()).toEqual([
      { semana: '06/07', percentual: '60%', volume: '10' },
      { semana: '13/07', percentual: 'Sem atividade', volume: '0' },
      { semana: '20/07', percentual: '70%', volume: '20' },
    ]);
  });
});
