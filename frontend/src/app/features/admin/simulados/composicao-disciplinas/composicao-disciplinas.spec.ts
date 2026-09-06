import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { QuestaoApi } from '../../../../core/api/questao-api';
import { SimuladoApi } from '../../../../core/api/simulado-api';
import { ResumoComposicao } from '../../../../core/models/modelos-simulado';
import { ComposicaoDisciplinas } from './composicao-disciplinas';

const resumoInicial: ResumoComposicao = {
  itens: [
    {
      disciplinaId: 'd1',
      dificuldade: null,
      quantidade: 5,
      disciplina: 'Direito Constitucional',
      disponiveis: 60,
    },
  ],
  somaComposicao: 5,
  numeroQuestoes: 10,
  fechada: false,
  acervoSuficiente: true,
};

describe('ComposicaoDisciplinas', () => {
  let composicaoApi: { composicao: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    composicaoApi = { composicao: vi.fn(() => of(resumoInicial)) };
    TestBed.configureTestingModule({
      imports: [ComposicaoDisciplinas],
      providers: [
        { provide: SimuladoApi, useValue: composicaoApi },
        {
          provide: CatalogoApi,
          useValue: { disciplinas: () => of([{ id: 'd1', nome: 'Direito Constitucional' }]) },
        },
        {
          provide: QuestaoApi,
          useValue: { contarDisponiveis: () => of({ disponiveis: 60 }) },
        },
      ],
    });
  });

  function criar(resumo: ResumoComposicao = resumoInicial) {
    const fixture = TestBed.createComponent(ComposicaoDisciplinas);
    fixture.componentRef.setInput('simuladoId', 's1');
    fixture.componentRef.setInput('resumo', resumo);
    fixture.detectChanges();
    return fixture;
  }

  it('hidrata os itens a partir do resumo inicial', () => {
    const fixture = criar();
    expect(fixture.componentInstance['itens']).toEqual([
      { disciplinaId: 'd1', dificuldade: null, quantidade: 5 },
    ]);
  });

  it('adiciona uma disciplina em branco', () => {
    const fixture = criar();
    fixture.componentInstance.adicionar();
    expect(fixture.componentInstance['itens']).toHaveLength(2);
    expect(fixture.componentInstance['itens'][1]).toEqual({
      disciplinaId: '',
      dificuldade: null,
      quantidade: 1,
    });
  });

  it('remove a disciplina pelo índice informado', () => {
    const fixture = criar();
    fixture.componentInstance.adicionar();
    fixture.componentInstance.remover(0);
    expect(fixture.componentInstance['itens']).toEqual([
      { disciplinaId: '', dificuldade: null, quantidade: 1 },
    ]);
  });

  it('salva a composição e emite o resumo atualizado', async () => {
    const resumoSalvo: ResumoComposicao = { ...resumoInicial, somaComposicao: 10, fechada: true };
    composicaoApi.composicao.mockReturnValue(of(resumoSalvo));
    const fixture = criar();
    const emitido = vi.fn();
    fixture.componentInstance.alterado.subscribe(emitido);

    await fixture.componentInstance.salvar();

    expect(composicaoApi.composicao).toHaveBeenCalledWith('s1', {
      itens: [{ disciplinaId: 'd1', dificuldade: null, quantidade: 5 }],
    });
    expect(emitido).toHaveBeenCalledWith(resumoSalvo);
    expect(fixture.componentInstance['salvando']()).toBe(false);
  });

  it('não sobrescreve edições locais quando o resumo é atualizado após o carregamento inicial', () => {
    const fixture = criar();
    fixture.componentInstance['itens'][0].quantidade = 10;

    fixture.componentRef.setInput('resumo', { ...resumoInicial, somaComposicao: 5 });
    fixture.detectChanges();

    expect(fixture.componentInstance['itens'][0].quantidade).toBe(10);
  });

  it('mantém os itens e mostra a mensagem de erro quando o salvamento falha', async () => {
    composicaoApi.composicao.mockReturnValue(
      throwError(() => ({
        error: { code: 'COMPOSICAO_INVALIDA', message: 'Composição inválida.', fieldErrors: null },
      })),
    );
    const fixture = criar();

    await fixture.componentInstance.salvar();

    expect(fixture.componentInstance['erro']()).toBe('Composição inválida.');
    expect(fixture.componentInstance['salvando']()).toBe(false);
  });

  it('salva a composição ao clicar no botão da tela', async () => {
    const fixture = criar();
    fixture.detectChanges();

    const botoes = Array.from(fixture.nativeElement.querySelectorAll('button'));
    const salvar = botoes[botoes.length - 1] as HTMLButtonElement;
    salvar.click();
    await fixture.whenStable();

    expect(composicaoApi.composicao).toHaveBeenCalledWith('s1', {
      itens: [{ disciplinaId: 'd1', dificuldade: null, quantidade: 5 }],
    });
  });
});
