import { ComponentFixture, TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { CatalogoApi } from '../../../core/api/catalogo-api';
import { FlashcardApi } from '../../../core/api/flashcard-api';
import {
  ContextoQuestaoFlashcard,
  FlashcardResumo,
  RespostaFlashcardCriado,
} from '../../../core/api/modelos-flashcard';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { DialogService } from '../../../shared/ui/dialog/dialog-service';
import { ToastStore } from '../../../shared/ui/toast/toast-store';
import { FlashcardDialog } from './flashcard-dialog';

const contexto: ContextoQuestaoFlashcard = {
  questaoId: 'q1',
  codigo: 'Q-1042',
  disciplinaId: 'd1',
  disciplinaNome: 'Direito Constitucional',
  assuntoId: 'a1',
  assuntoNome: 'Controle de constitucionalidade',
  sessaoId: 's1',
  devolverFocoPara: document.createElement('div'),
};

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

function raiz(fixture: ComponentFixture<Dialog>): HTMLElement {
  return fixture.nativeElement as HTMLElement;
}

describe('FlashcardDialog', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    api = {
      criar: vi.fn(),
      atualizar: vi.fn(),
      listarDecks: vi.fn(() => of([])),
      criarDeck: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: FlashcardApi, useValue: api },
        { provide: CatalogoApi, useValue: { disciplinas: () => of([]) } },
      ],
    });
  });

  it('exibe a tarja de vínculo e os campos de disciplina/assunto somente leitura quando há contexto de questão', () => {
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    TestBed.inject(DialogService).abrir(FlashcardDialog, { contexto });
    fixture.detectChanges();

    expect(raiz(fixture).textContent).toContain('Vinculado à questão em tela');
    expect(raiz(fixture).textContent).toContain('Q-1042');
    expect(raiz(fixture).querySelector('#fc-disciplina')).toBeNull();
  });

  it('esconde a tarja e mostra seletores editáveis de disciplina e assunto no modo avulso', () => {
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    TestBed.inject(DialogService).abrir(FlashcardDialog, { contexto: null });
    fixture.detectChanges();

    expect(raiz(fixture).textContent).not.toContain('Vinculado à questão em tela');
    expect(raiz(fixture).querySelector('#fc-disciplina')).not.toBeNull();
  });

  it('fecha direto ao cancelar com campos vazios', async () => {
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    const promessa = firstValueFrom(
      TestBed.inject(DialogService).abrir(FlashcardDialog, { contexto }),
    );
    fixture.detectChanges();

    raiz(fixture).querySelector<HTMLButtonElement>('[aria-label="Fechar"]')!.click();

    expect(await promessa).toBeUndefined();
  });

  it('pede confirmação ao cancelar com texto digitado e só fecha após confirmar o descarte', () => {
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    const emitido = vi.fn();
    TestBed.inject(DialogService).abrir(FlashcardDialog, { contexto }).subscribe(emitido);
    fixture.detectChanges();
    const frente = raiz(fixture).querySelector<HTMLTextAreaElement>('#fc-frente')!;
    frente.value = 'Texto digitado';
    frente.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    raiz(fixture).querySelector<HTMLButtonElement>('[aria-label="Fechar"]')!.click();
    fixture.detectChanges();

    expect(raiz(fixture).textContent).toContain('Descartar este flashcard?');
    expect(emitido).not.toHaveBeenCalled();

    const botaoDescartar = Array.from(raiz(fixture).querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Descartar',
    )!;
    botaoDescartar.click();
    expect(emitido).toHaveBeenCalledWith(undefined);
  });

  it('salva e fecha devolvendo o card criado e o contador da sessão', async () => {
    api['criar'].mockReturnValue(of(criado()));
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    const promessa = firstValueFrom(
      TestBed.inject(DialogService).abrir(FlashcardDialog, { contexto }),
    );
    fixture.detectChanges();
    preencherFrenteVerso(fixture);

    raiz(fixture).querySelector<HTMLButtonElement>('.btn-primary')!.click();
    const resultado = await promessa;

    expect(resultado).toEqual(criado());
    expect(TestBed.inject(ToastStore).mensagem()).toBe(
      'Flashcard criado — sua sessão continua onde estava.',
    );
  });

  it('salva e cria outro mantendo o diálogo aberto com frente e verso limpos', async () => {
    api['criar'].mockReturnValue(of(criado()));
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    const emitido = vi.fn();
    TestBed.inject(DialogService).abrir(FlashcardDialog, { contexto }).subscribe(emitido);
    fixture.detectChanges();
    preencherFrenteVerso(fixture);

    const botaoCriarOutro = Array.from(raiz(fixture).querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Salvar e criar outro'),
    )!;
    botaoCriarOutro.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(emitido).not.toHaveBeenCalled();
    expect(raiz(fixture).querySelector<HTMLTextAreaElement>('#fc-frente')!.value).toBe('');
    expect(TestBed.inject(ToastStore).mensagem()).toBe('Flashcard criado. Crie o próximo.');
  });

  it('publica o toast sem menção à sessão ao criar um card avulso', async () => {
    api['criar'].mockReturnValue(of(criado({ criadosNaSessao: null })));
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    TestBed.inject(DialogService).abrir(FlashcardDialog, { contexto: null });
    fixture.detectChanges();
    preencherFrenteVerso(fixture);

    raiz(fixture).querySelector<HTMLButtonElement>('.btn-primary')!.click();
    await fixture.whenStable();

    expect(TestBed.inject(ToastStore).mensagem()).toBe('Flashcard criado.');
  });

  it('abre em modo edição com os dados da linha já carregados, sem nova leitura, e salva com atualizar', async () => {
    const paraEditar: FlashcardResumo = {
      id: 'f9',
      frente: 'Frente original',
      verso: 'Verso original',
      deck: null,
      disciplina: null,
      assunto: null,
      tags: [],
      estado: 'NOVO',
      status: 'ATIVO',
      proximaRevisao: '2026-08-30',
      origem: null,
      criadoEm: '2026-08-30T00:00:00Z',
    };
    api['atualizar'].mockReturnValue(of({ ...paraEditar, frente: 'Frente editada' }));
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    TestBed.inject(DialogService).abrir(FlashcardDialog, {
      contexto: { flashcardParaEditar: paraEditar },
    });
    fixture.detectChanges();

    expect(raiz(fixture).textContent).toContain('Editar flashcard');
    expect(raiz(fixture).querySelector<HTMLTextAreaElement>('#fc-frente')!.value).toBe(
      'Frente original',
    );

    raiz(fixture).querySelector<HTMLButtonElement>('.btn-primary')!.click();
    await fixture.whenStable();

    expect(api['atualizar']).toHaveBeenCalledWith('f9', expect.any(Object));
    expect(TestBed.inject(ToastStore).mensagem()).toBe('Flashcard atualizado.');
  });
});

function preencherFrenteVerso(fixture: ComponentFixture<Dialog>): void {
  const frente = raiz(fixture).querySelector<HTMLTextAreaElement>('#fc-frente')!;
  const verso = raiz(fixture).querySelector<HTMLTextAreaElement>('#fc-verso')!;
  frente.value = 'Frente com texto válido';
  frente.dispatchEvent(new Event('input'));
  verso.value = 'Verso com texto válido';
  verso.dispatchEvent(new Event('input'));
}
