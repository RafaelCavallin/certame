import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CatalogoApi } from '../../../core/api/catalogo-api';
import { EditalApi } from '../../../core/api/edital-api';
import { EditaisAdminStore } from './editais-admin-store';

const resumoAdmin = (alteracoes: Record<string, unknown> = {}) => ({
  id: 'e1',
  nome: 'PF — Agente',
  status: 'ABERTO',
  vagas: 1500,
  prazoInscricao: '2026-07-12',
  banca: null,
  orgao: null,
  totalDisciplinas: 6,
  prazoVencido: true,
  atualizadoEm: '2026-06-02T11:20:37Z',
  ...alteracoes,
});

const detalhe = (alteracoes: Record<string, unknown> = {}) => ({
  id: 'e1',
  nome: 'TRF 1ª Região',
  status: 'ABERTO',
  vagas: 320,
  prazoInscricao: '2026-08-30',
  urlOficial: null,
  banca: null,
  orgao: null,
  disciplinas: [],
  criadoEm: '',
  atualizadoEm: '',
  ...alteracoes,
});

describe('EditaisAdminStore', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  const catalogo = { disciplinas: () => of([]), bancas: () => of([]), orgaos: () => of([]) };

  beforeEach(() => {
    api = {
      listarParaAdministracao: vi.fn(),
      buscar: vi.fn(),
      criar: vi.fn(),
      atualizar: vi.fn(),
      excluir: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        EditaisAdminStore,
        { provide: EditalApi, useValue: api },
        { provide: CatalogoApi, useValue: catalogo },
      ],
    });
  });

  it('TU-26: exige prazo ao escolher status aberto e bloqueia o envio com o campo vazio', async () => {
    const store = TestBed.inject(EditaisAdminStore);
    await store.carregarForm(null);
    store.formulario.controls.nome.setValue('Edital sem prazo');
    store.formulario.controls.status.setValue('ABERTO');

    expect(store.exigePrazo()).toBe(true);
    const salvou = await store.salvar();

    expect(salvou).toBe(false);
    expect(api['criar']).not.toHaveBeenCalled();
    expect(store.formulario.controls.prazoInscricao.getError('api')).toBeTruthy();
  });

  it('TU-27: mapeia o erro 422 campo a campo', async () => {
    const store = TestBed.inject(EditaisAdminStore);
    await store.carregarForm(null);
    store.formulario.controls.nome.setValue('Edital válido');
    store.formulario.controls.status.setValue('PREVISTO');
    api['criar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'DADOS_INVALIDOS',
          message: 'Verifique os campos destacados.',
          fieldErrors: {
            urlOficial: 'Informe uma URL http ou https.',
            disciplinaIds: 'Disciplina inexistente.',
          },
        },
      })),
    );

    await store.salvar();

    expect(store.formulario.controls.urlOficial.getError('api')).toBe(
      'Informe uma URL http ou https.',
    );
    expect(store.formulario.controls.disciplinaIds.getError('api')).toBe('Disciplina inexistente.');
    expect(store.formulario.controls.nome.errors).toBeNull();
  });

  it('TU-28: mapeia o erro 409 para o campo nome', async () => {
    const store = TestBed.inject(EditaisAdminStore);
    await store.carregarForm(null);
    store.formulario.controls.nome.setValue('TRF 1ª Região');
    store.formulario.controls.status.setValue('PREVISTO');
    api['criar'].mockReturnValue(
      throwError(() => ({
        error: { code: 'NOME_EM_USO', message: 'Este nome já está em uso.', fieldErrors: null },
      })),
    );

    await store.salvar();

    expect(store.formulario.controls.nome.getError('api')).toBe('Este nome já está em uso.');
    expect(store.erro()).toBeNull();
  });

  it('TU-30: mantém o prazoVencido calculado pelo servidor ao carregar a lista', async () => {
    const store = TestBed.inject(EditaisAdminStore);
    api['listarParaAdministracao'].mockReturnValue(
      of([resumoAdmin(), resumoAdmin({ id: 'e2', prazoVencido: false })]),
    );

    await store.carregarLista();

    expect(store.editais()[0].prazoVencido).toBe(true);
    expect(store.editais()[1].prazoVencido).toBe(false);
  });

  it('salva pela rota de atualização quando um edital já foi carregado', async () => {
    const store = TestBed.inject(EditaisAdminStore);
    api['buscar'].mockReturnValue(of(detalhe()));
    await store.carregarForm('e1');
    api['atualizar'].mockReturnValue(of(detalhe({ nome: 'TRF 1ª Região — revisado' })));

    const salvou = await store.salvar();

    expect(salvou).toBe(true);
    expect(api['atualizar']).toHaveBeenCalledWith('e1', expect.anything());
    expect(store.formulario.controls.nome.value).toBe('TRF 1ª Região — revisado');
  });

  it('exclui um edital e recarrega a lista administrativa', async () => {
    const store = TestBed.inject(EditaisAdminStore);
    api['excluir'].mockReturnValue(of(undefined));
    api['listarParaAdministracao'].mockReturnValue(of([]));

    await store.excluir('e1');

    expect(api['excluir']).toHaveBeenCalledWith('e1');
    expect(api['listarParaAdministracao']).toHaveBeenCalled();
  });
});
