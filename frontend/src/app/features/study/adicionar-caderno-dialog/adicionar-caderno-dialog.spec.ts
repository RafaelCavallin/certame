import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CadernoApi } from '../../../core/api/caderno-api';
import { CadernoResumo } from '../../../core/api/modelos-desempenho';
import { AdicionarCadernoDialog } from './adicionar-caderno-dialog';

function cadernoResumo(alteracoes: Partial<CadernoResumo> = {}): CadernoResumo {
  return {
    id: 'c1',
    tipo: 'MANUAL',
    nome: 'Revisão final',
    descricao: null,
    totalQuestoes: 3,
    questoesRespondidas: 0,
    progressoPercentual: 0,
    atualizadoEm: null,
    editavel: true,
    ...alteracoes,
  };
}

describe('AdicionarCadernoDialog', () => {
  let api: { listar: ReturnType<typeof vi.fn>; adicionarQuestoes: ReturnType<typeof vi.fn> };

  function montar(cadernos: CadernoResumo[]) {
    api = {
      listar: vi.fn(() => of(cadernos)),
      adicionarQuestoes: vi.fn((id: string) => of(cadernoResumo({ id, nome: `Caderno ${id}` }))),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: CadernoApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(AdicionarCadernoDialog);
    fixture.componentRef.setInput('questaoId', 'q1');
    fixture.detectChanges();
    return fixture;
  }

  it('permite selecionar mais de um caderno ao mesmo tempo', () => {
    // Arrange
    const fixture = montar([cadernoResumo({ id: 'c1' }), cadernoResumo({ id: 'c2' })]);

    // Act
    fixture.componentInstance.alternar('c1');
    fixture.componentInstance.alternar('c2');

    // Assert
    expect(fixture.componentInstance.selecionados()).toEqual(['c1', 'c2']);
  });

  it('desmarca um caderno já selecionado ao alternar novamente', () => {
    // Arrange
    const fixture = montar([cadernoResumo({ id: 'c1' })]);
    fixture.componentInstance.alternar('c1');

    // Act
    fixture.componentInstance.alternar('c1');

    // Assert
    expect(fixture.componentInstance.selecionados()).toEqual([]);
  });

  it('adiciona a questão a todos os cadernos selecionados e emite os nomes', async () => {
    // Arrange
    const fixture = montar([cadernoResumo({ id: 'c1' }), cadernoResumo({ id: 'c2' })]);
    const adicionada = vi.fn();
    fixture.componentInstance.adicionada.subscribe(adicionada);
    fixture.componentInstance.alternar('c1');
    fixture.componentInstance.alternar('c2');

    // Act
    await fixture.componentInstance.adicionar();

    // Assert
    expect(api.adicionarQuestoes).toHaveBeenCalledWith('c1', { questaoIds: ['q1'] });
    expect(api.adicionarQuestoes).toHaveBeenCalledWith('c2', { questaoIds: ['q1'] });
    expect(adicionada).toHaveBeenCalledWith(['Caderno c1', 'Caderno c2']);
  });

  it('não adiciona nada quando nenhum caderno está selecionado', async () => {
    // Arrange
    const fixture = montar([cadernoResumo({ id: 'c1' })]);

    // Act
    await fixture.componentInstance.adicionar();

    // Assert
    expect(api.adicionarQuestoes).not.toHaveBeenCalled();
  });
});
