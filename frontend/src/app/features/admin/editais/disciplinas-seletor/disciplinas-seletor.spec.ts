import { TestBed } from '@angular/core/testing';
import { DisciplinasSeletor } from './disciplinas-seletor';

const disciplinas = [
  { id: 'd1', nome: 'Direito Constitucional' },
  { id: 'd2', nome: 'Português' },
];

function montar(selecionados: string[] = []) {
  const fixture = TestBed.createComponent(DisciplinasSeletor);
  fixture.componentRef.setInput('disciplinas', disciplinas);
  fixture.componentRef.setInput('selecionados', selecionados);
  fixture.detectChanges();
  return fixture;
}

describe('DisciplinasSeletor', () => {
  it('emite o id ao marcar uma disciplina da lista', () => {
    const fixture = montar();
    const emitido = vi.fn();
    fixture.componentInstance.alterado.subscribe(emitido);

    fixture.nativeElement.querySelectorAll('input[type=checkbox]')[0].click();

    expect(emitido).toHaveBeenCalledWith(['d1']);
  });

  it('remove o id ao desmarcar uma disciplina já selecionada', () => {
    const fixture = montar(['d1']);
    const emitido = vi.fn();
    fixture.componentInstance.alterado.subscribe(emitido);

    fixture.nativeElement.querySelectorAll('input[type=checkbox]')[0].click();

    expect(emitido).toHaveBeenCalledWith([]);
  });

  it('TU-29: só emite ids vindos da árvore de disciplinas', () => {
    const fixture = montar();
    const emitido = vi.fn();
    fixture.componentInstance.alterado.subscribe(emitido);

    (fixture.componentInstance as unknown as { alternar(id: string): void }).alternar(
      'id-que-nao-existe-na-arvore',
    );

    expect(emitido).not.toHaveBeenCalled();
  });

  it('remove uma disciplina selecionada pela tag', () => {
    const fixture = montar(['d1', 'd2']);
    const emitido = vi.fn();
    fixture.componentInstance.alterado.subscribe(emitido);

    fixture.nativeElement.querySelector('.selecionadas button').click();

    expect(emitido).toHaveBeenCalledWith(['d2']);
  });

  it('filtra a lista pelo nome digitado, sem criar item novo', () => {
    const fixture = montar();
    const componente = fixture.componentInstance as unknown as {
      filtro: { set(v: string): void };
      filtradas(): { id: string }[];
    };

    componente.filtro.set('constit');

    expect(componente.filtradas()).toEqual([{ id: 'd1', nome: 'Direito Constitucional' }]);
  });
});
