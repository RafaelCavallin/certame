import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { EditalApi } from '../../core/api/edital-api';
import { EditaisAgrupados, EditalResumo } from '../../core/api/modelos-edital';
import { EditaisStore } from './editais-store';

const editalBase: EditalResumo = {
  id: '1',
  nome: 'Edital',
  status: 'ABERTO',
  vagas: null,
  prazoInscricao: null,
  urlOficial: null,
  banca: null,
  orgao: null,
  disciplinas: [],
};

function montar(dados: EditaisAgrupados): EditaisStore {
  const api = { agrupados: () => of(dados) } as unknown as EditalApi;
  TestBed.configureTestingModule({
    providers: [EditaisStore, { provide: EditalApi, useValue: api }],
  });
  return TestBed.inject(EditaisStore);
}

describe('EditaisStore', () => {
  it('expõe vazio quando os três grupos vêm vazios', async () => {
    const store = montar({ abertos: [], previstos: [], encerrados: [] });

    await store.carregar();

    expect(store.vazio()).toBe(true);
  });

  it('não expõe vazio quando algum grupo tem editais', async () => {
    const store = montar({ abertos: [editalBase], previstos: [], encerrados: [] });

    await store.carregar();

    expect(store.vazio()).toBe(false);
    expect(store.agrupados()?.abertos).toHaveLength(1);
  });

  it('seleciona um edital pelo id entre os três grupos', async () => {
    const previsto = {
      ...editalBase,
      id: '2',
      nome: 'Edital Previsto',
      status: 'PREVISTO' as const,
    };
    const store = montar({ abertos: [], previstos: [previsto], encerrados: [] });
    await store.carregar();

    store.selecionar('2');

    expect(store.selecionado()?.nome).toBe('Edital Previsto');
  });
});
