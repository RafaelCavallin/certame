import { TestBed } from '@angular/core/testing';
import { CadernoApi } from '../../core/api/caderno-api';
import { EstudoApi } from '../../core/api/estudo-api';
import { CadernosStore } from './cadernos-store';
import { of } from 'rxjs';

describe('CadernosStore', () => {
  it('marca vazio quando todos os cadernos não têm questões', async () => {
    const api = {
      listar: () =>
        of([
          {
            id: 'auto:FAVORITA',
            tipo: 'AUTOMATICO',
            nome: 'Favoritas',
            descricao: null,
            totalQuestoes: 0,
            questoesRespondidas: 0,
            progressoPercentual: 0,
            atualizadoEm: null,
            editavel: false,
          },
        ]),
    } as unknown as CadernoApi;
    const estudoApi = {} as unknown as EstudoApi;
    TestBed.configureTestingModule({
      providers: [
        CadernosStore,
        { provide: CadernoApi, useValue: api },
        { provide: EstudoApi, useValue: estudoApi },
      ],
    });
    const store = TestBed.inject(CadernosStore);
    await store.carregar();
    expect(store.vazio()).toBe(true);
  });

  it('atualiza a lista nas ações do caderno', async () => {
    const caderno = {
      id: '1',
      tipo: 'MANUAL' as const,
      nome: 'Revisão',
      descricao: null,
      totalQuestoes: 1,
      questoesRespondidas: 0,
      progressoPercentual: 0,
      atualizadoEm: null,
      editavel: true,
    };
    const api = {
      listar: () => of([]),
      criar: () => of(caderno),
      atualizar: () => of(caderno),
      excluir: () => of(undefined),
      adicionarQuestoes: () => of(caderno),
    } as unknown as CadernoApi;
    const estudoApi = { abrir: () => of({ sessaoId: 'sessao-1' }) } as unknown as EstudoApi;
    TestBed.configureTestingModule({
      providers: [
        CadernosStore,
        { provide: CadernoApi, useValue: api },
        { provide: EstudoApi, useValue: estudoApi },
      ],
    });
    const store = TestBed.inject(CadernosStore);
    await store.criar({ nome: 'Revisão', descricao: null });
    await store.renomear('1', { nome: 'Revisão', descricao: null });
    await store.adicionarQuestoes('1', ['q1']);
    const sessaoId = await store.estudar('1');
    await store.excluir('1');
    expect(sessaoId).toBe('sessao-1');
    expect(store.cadernos()).toHaveLength(0);
  });
});
