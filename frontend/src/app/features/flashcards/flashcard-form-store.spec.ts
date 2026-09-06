import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CatalogoApi } from '../../core/api/catalogo-api';
import { FlashcardApi } from '../../core/api/flashcard-api';
import {
  ContextoQuestaoFlashcard,
  FlashcardResumo,
  RespostaFlashcardCriado,
} from '../../core/api/modelos-flashcard';
import { FlashcardFormStore } from './flashcard-form-store';

const contexto = (
  alteracoes: Partial<ContextoQuestaoFlashcard> = {},
): ContextoQuestaoFlashcard => ({
  questaoId: 'q1',
  codigo: 'Q-1042',
  disciplinaId: 'd1',
  disciplinaNome: 'Direito Constitucional',
  assuntoId: 'a1',
  assuntoNome: 'Controle de constitucionalidade',
  sessaoId: 's1',
  devolverFocoPara: document.createElement('div'),
  ...alteracoes,
});

const criado = (alteracoes: Partial<RespostaFlashcardCriado> = {}): RespostaFlashcardCriado => ({
  flashcard: {
    id: 'f1',
    frente: 'Frente',
    verso: 'Verso',
    deck: null,
    disciplina: null,
    assunto: null,
    tags: [],
    estado: 'NOVO',
    status: 'ATIVO',
    proximaRevisao: '2026-08-30',
    origem: null,
    criadoEm: '2026-08-30T00:00:00Z',
  },
  criadosNaSessao: 3,
  ...alteracoes,
});

const flashcard = (alteracoes: Partial<FlashcardResumo> = {}): FlashcardResumo => ({
  id: 'f9',
  frente: 'Frente original',
  verso: 'Verso original',
  deck: { id: 'd1', nome: 'Deck' },
  disciplina: null,
  assunto: null,
  tags: ['adi'],
  estado: 'NOVO',
  status: 'ATIVO',
  proximaRevisao: '2026-08-30',
  origem: null,
  criadoEm: '2026-08-30T00:00:00Z',
  ...alteracoes,
});

describe('FlashcardFormStore', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    sessionStorage.clear();
    api = { criar: vi.fn(), atualizar: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        FlashcardFormStore,
        { provide: FlashcardApi, useValue: api },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]) } },
      ],
    });
  });

  it('TU-31 bloqueia o envio com frente curta: nenhuma requisição é feita e o erro aparece no campo', async () => {
    const store = TestBed.inject(FlashcardFormStore);
    store.abrir(contexto());
    store.form.patchValue({ frente: 'ab', verso: 'Verso válido' });

    const criouComSucesso = await store.salvarEFechar();

    expect(criouComSucesso).toBe(false);
    expect(api['criar']).not.toHaveBeenCalled();
    expect(store.erroDeCampo().frente).toBe('A frente deve ter ao menos 3 caracteres.');
  });

  it('TU-32 salvarECriarOutro limpa frente e verso e preserva deck, disciplina, assunto e tags', async () => {
    const store = TestBed.inject(FlashcardFormStore);
    api['criar'].mockReturnValue(of(criado()));
    store.abrir(contexto());
    store.form.patchValue({
      frente: 'Frente válida',
      verso: 'Verso válido',
      deckId: 'd1',
      tags: 'adi, stf',
    });

    const criouComSucesso = await store.salvarECriarOutro();

    expect(criouComSucesso).toBe(true);
    expect(store.form.controls.frente.value).toBe('');
    expect(store.form.controls.verso.value).toBe('');
    expect(store.form.controls.deckId.value).toBe('d1');
    expect(store.form.controls.tags.value).toBe('adi, stf');
  });

  it('TU-33 cancelar com texto digitado ativa a confirmação sem fechar nem descartar', () => {
    const store = TestBed.inject(FlashcardFormStore);
    store.abrir(contexto());
    store.form.patchValue({ frente: 'Texto digitado' });

    const deveFechar = store.cancelar();

    expect(deveFechar).toBe(false);
    expect(store.confirmandoDescarte()).toBe(true);
  });

  it('TU-34 o Esc equivale ao Cancelar: com campos preenchidos, não é um atalho de descarte', () => {
    const store = TestBed.inject(FlashcardFormStore);
    store.abrir(contexto());
    store.form.patchValue({ verso: 'Texto digitado no verso' });

    const deveFecharNoEsc = store.cancelar();

    expect(deveFecharNoEsc).toBe(false);
    expect(store.confirmandoDescarte()).toBe(true);
  });

  it('TU-35 cancelar com campos vazios fecha direto, sem confirmação', () => {
    const store = TestBed.inject(FlashcardFormStore);
    store.abrir(contexto());

    const deveFechar = store.cancelar();

    expect(deveFechar).toBe(true);
    expect(store.confirmandoDescarte()).toBe(false);
  });

  it('TU-36 atualiza o contador da sessão pelo valor devolvido pela resposta do servidor', async () => {
    const store = TestBed.inject(FlashcardFormStore);
    api['criar'].mockReturnValue(of(criado({ criadosNaSessao: 3 })));
    store.abrir(contexto());
    store.form.patchValue({ frente: 'Frente válida', verso: 'Verso válido' });

    await store.salvarEFechar();

    expect(store.criadosNaSessao()).toBe(3);
  });

  it('TU-37 mapeia o 422 do servidor campo a campo, mantendo o texto digitado', async () => {
    const store = TestBed.inject(FlashcardFormStore);
    api['criar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'DADOS_INVALIDOS',
          message: 'Não foi possível salvar o flashcard: verifique os campos destacados.',
          fieldErrors: { frente: 'A frente deve ter ao menos 3 caracteres.' },
        },
      })),
    );
    store.abrir(contexto());
    store.form.patchValue({ frente: 'Frente válida', verso: 'Verso válido' });

    const criouComSucesso = await store.salvarEFechar();

    expect(criouComSucesso).toBe(false);
    expect(store.erroDeCampo().frente).toBe('A frente deve ter ao menos 3 caracteres.');
    expect(store.form.controls.frente.value).toBe('Frente válida');
  });

  it('mantém o card criado avulso sem sessão associada (CA-14)', async () => {
    const store = TestBed.inject(FlashcardFormStore);
    api['criar'].mockReturnValue(of(criado({ criadosNaSessao: null })));
    store.abrir(null);
    store.form.patchValue({ frente: 'Frente válida', verso: 'Verso válido', disciplinaId: 'd1' });

    await store.salvarEFechar();

    expect(api['criar']).toHaveBeenCalledWith(
      expect.objectContaining({ questaoOrigemId: null, sessaoOrigemId: null, disciplinaId: 'd1' }),
    );
    expect(store.criadosNaSessao()).toBeNull();
  });

  it('abrirEdicao preenche o formulário com os dados da linha, sem nova requisição de leitura', () => {
    const store = TestBed.inject(FlashcardFormStore);
    store.abrirEdicao(flashcard());

    expect(store.modoEdicao()).toBe(true);
    expect(store.modoAvulso()).toBe(true);
    expect(store.form.controls.frente.value).toBe('Frente original');
    expect(store.form.controls.tags.value).toBe('adi');
    expect(store.form.controls.deckId.value).toBe('d1');
  });

  it('abrirEdicao de um card com origem mantém disciplina e assunto somente leitura', () => {
    const store = TestBed.inject(FlashcardFormStore);
    store.abrirEdicao(
      flashcard({
        origem: { questaoId: 'q1', codigo: 'Q-1042', sessaoId: null },
        disciplina: { id: 'd2', nome: 'Direito Constitucional' },
      }),
    );

    expect(store.modoAvulso()).toBe(false);
    expect(store.nomeDisciplinaLeitura()).toBe('Direito Constitucional');
  });

  it('salvarEFechar em modo edição chama atualizar com o id do card e devolve o card sem contador de sessão', async () => {
    const store = TestBed.inject(FlashcardFormStore);
    api['atualizar'].mockReturnValue(of(flashcard({ frente: 'Frente ajustada' })));
    store.abrirEdicao(flashcard());
    store.form.patchValue({ frente: 'Frente ajustada' });

    const salvouComSucesso = await store.salvarEFechar();

    expect(salvouComSucesso).toBe(true);
    expect(api['atualizar']).toHaveBeenCalledWith(
      'f9',
      expect.objectContaining({ frente: 'Frente ajustada' }),
    );
    expect(store.criadosNaSessao()).toBeNull();
  });

  it('cancelar em modo edição só pede confirmação quando algo de fato mudou', () => {
    const store = TestBed.inject(FlashcardFormStore);
    store.abrirEdicao(flashcard());

    expect(store.cancelar()).toBe(true);

    store.form.patchValue({ verso: 'Verso alterado' });
    expect(store.cancelar()).toBe(false);
    expect(store.confirmandoDescarte()).toBe(true);
  });
});
