import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CatalogoApi } from '../../../core/api/catalogo-api';
import { QuestaoApi } from '../../../core/api/questao-api';
import { QuestaoFormStore } from './questao-form-store';

const detalhe = (alteracoes: Record<string, unknown> = {}) => ({
  id: 'q1',
  codigo: 'Q-1',
  tipo: 'ME',
  enunciado: 'Enunciado com mais de dez caracteres.',
  disciplinaId: null,
  assuntoId: null,
  bancaId: null,
  orgaoId: null,
  ano: null,
  dificuldade: null,
  alternativas: [
    { letra: 'A', texto: 'Primeira', ordem: 1 },
    { letra: 'B', texto: 'Segunda', ordem: 2 },
  ],
  gabarito: 'A',
  comentario: 'Comentário.',
  status: 'RASCUNHO',
  autorId: 'u1',
  publicadoEm: null,
  criadoEm: '',
  atualizadoEm: '',
  ...alteracoes,
});

describe('QuestaoFormStore', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  beforeEach(() => {
    api = {
      obter: vi.fn(),
      criar: vi.fn(),
      atualizar: vi.fn(),
      publicar: vi.fn(),
      despublicar: vi.fn(),
      enviarParaRevisao: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        QuestaoFormStore,
        { provide: QuestaoApi, useValue: api },
        {
          provide: CatalogoApi,
          useValue: { disciplinas: () => of([]), bancas: () => of([]), orgaos: () => of([]) },
        },
      ],
    });
  });
  it('TU-33: troca ME por CE, limpa gabarito e alternativas', () => {
    const store = TestBed.inject(QuestaoFormStore);
    store.formulario.controls.gabarito.setValue('A');
    store.trocarTipo('CE');
    expect(store.formulario.controls.alternativas.length).toBe(0);
    expect(store.formulario.controls.gabarito.value).toBeNull();
  });
  it('TU-34: limita alternativas a cinco', () => {
    const store = TestBed.inject(QuestaoFormStore);
    store.adicionarAlternativa();
    store.adicionarAlternativa();
    store.adicionarAlternativa();
    store.adicionarAlternativa();
    expect(store.formulario.controls.alternativas.length).toBe(5);
    store.adicionarAlternativa();
    expect(store.formulario.controls.alternativas.length).toBe(5);
  });
  it('TU-35: mantém os dados e mapeia todas as pendências de publicação', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['obter'].mockReturnValue(
      of({
        id: 'q1',
        codigo: 'Q-1',
        tipo: 'ME',
        enunciado: 'Enunciado válido',
        disciplinaId: null,
        assuntoId: null,
        bancaId: null,
        orgaoId: null,
        ano: null,
        dificuldade: null,
        alternativas: [],
        gabarito: null,
        comentario: null,
        status: 'EM_REVISAO',
        autorId: 'u1',
        publicadoEm: null,
        criadoEm: '',
        atualizadoEm: '',
      }),
    );
    await store.carregar('q1');
    api['publicar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'PUBLICACAO_INVALIDA',
          message: 'inválida',
          fieldErrors: { gabarito: 'Marque o gabarito.', comentario: 'Informe o comentário.' },
        },
      })),
    );
    store.formulario.controls.enunciado.setValue('texto digitado pelo professor');
    await store.publicar();
    expect(store.pendenciasPublicacao()).toEqual({
      gabarito: 'Marque o gabarito.',
      comentario: 'Informe o comentário.',
    });
    expect(store.formulario.controls.enunciado.value).toBe('texto digitado pelo professor');
  });

  it('salva o rascunho criando a questão e numerando as alternativas pela ordem da lista', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['criar'].mockReturnValue(of(detalhe()));
    store.formulario.controls.enunciado.setValue('Enunciado com mais de dez caracteres.');
    store.formulario.controls.alternativas.at(0).controls.texto.setValue('Primeira');
    store.formulario.controls.alternativas.at(1).controls.texto.setValue('Segunda');

    await store.salvarRascunho();

    expect(api['criar']).toHaveBeenCalledWith(
      expect.objectContaining({
        tipo: 'ME',
        alternativas: [
          { letra: null, texto: 'Primeira', ordem: 1 },
          { letra: null, texto: 'Segunda', ordem: 2 },
        ],
      }),
    );
    expect(store.editando()).toBe(true);
    expect(store.salvando()).toBe(false);
  });

  it('salva pela rota de atualização quando a questão já foi carregada', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['obter'].mockReturnValue(of(detalhe()));
    await store.carregar('q1');
    api['atualizar'].mockReturnValue(
      of(detalhe({ enunciado: 'Enunciado revisado pelo professor.' })),
    );

    await store.salvar();

    expect(api['atualizar']).toHaveBeenCalledWith('q1', expect.anything());
    expect(api['criar']).not.toHaveBeenCalled();
    expect(store.formulario.controls.enunciado.value).toBe('Enunciado revisado pelo professor.');
  });

  it('descarta as alternativas do corpo quando o tipo é Certo/Errado', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['criar'].mockReturnValue(of(detalhe({ tipo: 'CE', alternativas: [] })));
    store.trocarTipo('CE');

    await store.salvarRascunho();

    expect(api['criar']).toHaveBeenCalledWith(
      expect.objectContaining({ tipo: 'CE', alternativas: [] }),
    );
  });

  it('exibe a mensagem da API quando o salvamento falha', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['criar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'DADOS_INVALIDOS',
          message: 'Verifique os campos destacados.',
          fieldErrors: null,
        },
      })),
    );

    await store.salvarRascunho();

    expect(store.erro()).toBe('Verifique os campos destacados.');
    expect(store.salvando()).toBe(false);
  });

  it('remove a alternativa escolhida acima do mínimo de duas', () => {
    const store = TestBed.inject(QuestaoFormStore);
    store.adicionarAlternativa();
    store.formulario.controls.alternativas.at(2).controls.texto.setValue('Terceira');

    store.removerAlternativa(2);

    expect(store.alternativas()).toHaveLength(2);
  });

  it('mantém duas alternativas mesmo quando a remoção é pedida', () => {
    const store = TestBed.inject(QuestaoFormStore);

    store.removerAlternativa(1);

    expect(store.alternativas()).toHaveLength(2);
  });

  it('envia para revisão e aplica o status devolvido pela API', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['obter'].mockReturnValue(of(detalhe()));
    await store.carregar('q1');
    api['enviarParaRevisao'].mockReturnValue(of(detalhe({ status: 'EM_REVISAO' })));

    await store.enviarParaRevisao();

    expect(api['enviarParaRevisao']).toHaveBeenCalledWith('q1');
    expect(store.questao()?.status).toBe('EM_REVISAO');
  });

  it('despublica e aplica o status devolvido pela API', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['obter'].mockReturnValue(of(detalhe({ status: 'PUBLICADA' })));
    await store.carregar('q1');
    api['despublicar'].mockReturnValue(of(detalhe({ status: 'EM_REVISAO' })));

    await store.despublicar();

    expect(api['despublicar']).toHaveBeenCalledWith('q1');
    expect(store.questao()?.status).toBe('EM_REVISAO');
  });

  it('não chama a API de transição quando não há questão carregada', async () => {
    const store = TestBed.inject(QuestaoFormStore);

    await store.enviarParaRevisao();
    await store.despublicar();
    await store.publicar();

    expect(api['enviarParaRevisao']).not.toHaveBeenCalled();
    expect(api['despublicar']).not.toHaveBeenCalled();
    expect(api['publicar']).not.toHaveBeenCalled();
  });

  it('exibe a mensagem da API quando a transição falha por outro motivo', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['obter'].mockReturnValue(of(detalhe()));
    await store.carregar('q1');
    api['publicar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'ACESSO_NEGADO',
          message: 'Você não tem permissão para publicar.',
          fieldErrors: null,
        },
      })),
    );

    await store.publicar();

    expect(store.erro()).toBe('Você não tem permissão para publicar.');
    expect(store.pendenciasPublicacao()).toBeNull();
  });

  it('recria duas alternativas ao voltar de Certo/Errado para múltipla escolha', () => {
    const store = TestBed.inject(QuestaoFormStore);
    store.trocarTipo('CE');

    store.trocarTipo('ME');

    expect(store.alternativas()).toHaveLength(2);
  });

  it('exibe a mensagem da API quando o carregamento do catálogo falha', async () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        QuestaoFormStore,
        { provide: QuestaoApi, useValue: api },
        {
          provide: CatalogoApi,
          useValue: {
            disciplinas: () =>
              throwError(() => ({
                error: {
                  code: 'ERRO',
                  message: 'Falha ao carregar o catálogo.',
                  fieldErrors: null,
                },
              })),
            bancas: () => of([]),
            orgaos: () => of([]),
          },
        },
      ],
    });
    const store = TestBed.inject(QuestaoFormStore);

    await store.carregar(null);

    expect(store.erro()).toBe('Falha ao carregar o catálogo.');
    expect(store.carregando()).toBe(false);
  });

  it('não envia alternativas em branco ao salvar um rascunho só com o enunciado', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['criar'].mockReturnValue(of(detalhe({ alternativas: [] })));
    store.formulario.controls.enunciado.setValue('Rascunho gravado somente com o enunciado.');

    await store.salvarRascunho();

    expect(api['criar']).toHaveBeenCalledWith(expect.objectContaining({ alternativas: [] }));
  });

  it('renumera a ordem depois de descartar as alternativas em branco', async () => {
    const store = TestBed.inject(QuestaoFormStore);
    api['criar'].mockReturnValue(of(detalhe()));
    store.adicionarAlternativa();
    store.formulario.controls.enunciado.setValue('Enunciado com alternativa intermediária vazia.');
    store.formulario.controls.alternativas.at(0).controls.texto.setValue('Primeira');
    store.formulario.controls.alternativas.at(2).controls.texto.setValue('  Terceira  ');

    await store.salvarRascunho();

    expect(api['criar']).toHaveBeenCalledWith(
      expect.objectContaining({
        alternativas: [
          { letra: null, texto: 'Primeira', ordem: 1 },
          { letra: null, texto: 'Terceira', ordem: 2 },
        ],
      }),
    );
  });
});
