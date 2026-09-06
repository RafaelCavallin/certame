import { TestBed } from '@angular/core/testing';
import { DeckDetalhe } from '../../../core/api/modelos-flashcard';
import { DIALOG_DATA, DialogRef } from '../../../shared/ui/dialog/dialog-service';
import { ToastStore } from '../../../shared/ui/toast/toast-store';
import { DecksStore } from '../decks-store';
import { DeckDialog } from './deck-dialog';

function deckDetalhe(): DeckDetalhe {
  return {
    id: 'd1',
    nome: 'Constitucional — controle',
    descricao: 'ADI, ADC, ADPF',
    totalCards: 214,
    vencendoHoje: 137,
    novos: 12,
    limites: { novosDia: 10, revisoesDia: 60, intervaloMaxDias: 180, retencaoAlvo: 0.9 },
    limitesRotulo: '10 novos/dia · 60 revisões/dia · máx. 180 d',
    retencaoRotulo: 'FSRS · retenção alvo 90%',
    editavel: true,
    aviso: null,
  };
}

describe('DeckDialog', () => {
  let dialogRef: { fechar: ReturnType<typeof vi.fn> };
  let store: {
    salvar: ReturnType<typeof vi.fn>;
    salvando: ReturnType<typeof vi.fn>;
    erroDeCampo: ReturnType<typeof vi.fn>;
  };
  let toast: { publicar: ReturnType<typeof vi.fn> };

  function criar(dados: DeckDetalhe | null, erroNome: string | null = null) {
    dialogRef = { fechar: vi.fn() };
    store = {
      salvar: vi.fn(() => Promise.resolve()),
      salvando: vi.fn(() => false),
      erroDeCampo: vi.fn(() => ({ nome: erroNome })),
    };
    toast = { publicar: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: DialogRef, useValue: dialogRef },
        { provide: DIALOG_DATA, useValue: dados },
        { provide: DecksStore, useValue: store },
        { provide: ToastStore, useValue: toast },
      ],
    });
    return TestBed.createComponent(DeckDialog).componentInstance;
  }

  it('preenche o formulário com os dados do deck em edição', () => {
    const componente = criar(deckDetalhe());

    expect(componente['form'].getRawValue()).toEqual({
      nome: 'Constitucional — controle',
      descricao: 'ADI, ADC, ADPF',
      novosDia: 10,
      revisoesDia: 60,
      intervaloMaxDias: 180,
      retencaoAlvo: 0.9,
    });
  });

  it('começa vazio com os limites padrão na criação', () => {
    const componente = criar(null);

    expect(componente['form'].getRawValue()).toEqual({
      nome: '',
      descricao: '',
      novosDia: 10,
      revisoesDia: 60,
      intervaloMaxDias: 180,
      retencaoAlvo: 0.9,
    });
  });

  it('não salva com o nome abaixo do tamanho mínimo', async () => {
    const componente = criar(null);
    componente['form'].controls.nome.setValue('ab');

    await componente['salvar']();

    expect(store.salvar).not.toHaveBeenCalled();
  });

  it('salva e fecha o diálogo quando não há erro de campo', async () => {
    const componente = criar(deckDetalhe());

    await componente['salvar']();

    expect(store.salvar).toHaveBeenCalledWith(
      {
        nome: 'Constitucional — controle',
        descricao: 'ADI, ADC, ADPF',
        novosDia: 10,
        revisoesDia: 60,
        intervaloMaxDias: 180,
        retencaoAlvo: 0.9,
      },
      'd1',
    );
    expect(toast.publicar).toHaveBeenCalledWith('Deck atualizado.');
    expect(dialogRef.fechar).toHaveBeenCalled();
  });

  it('mantém o diálogo aberto quando o servidor recusa o nome', async () => {
    const componente = criar(deckDetalhe(), 'Já existe um deck com esse nome.');

    await componente['salvar']();

    expect(dialogRef.fechar).not.toHaveBeenCalled();
  });

  it('fecha o diálogo sem salvar ao cancelar', () => {
    const componente = criar(deckDetalhe());

    componente['cancelar']();

    expect(dialogRef.fechar).toHaveBeenCalled();
    expect(store.salvar).not.toHaveBeenCalled();
  });
});
