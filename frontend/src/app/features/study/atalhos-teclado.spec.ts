import { AtalhosTeclado } from './atalhos-teclado';
import { TestBed } from '@angular/core/testing';
describe('AtalhosTeclado', () => {
  it('mapeia alternativas e ações de navegação', () => {
    const diretiva = TestBed.runInInjectionContext(() => new AtalhosTeclado());
    const alternativas: string[] = [];
    const acoes: string[] = [];
    diretiva.alternativa.subscribe((valor) => alternativas.push(valor));
    diretiva.acao.subscribe((valor) => acoes.push(valor));
    const tecla = (key: string) =>
      diretiva.tecla({ key, preventDefault: () => undefined } as KeyboardEvent);
    tecla('B');
    tecla('C');
    tecla('Enter');
    tecla('ArrowLeft');
    tecla('ArrowRight');
    tecla('F');
    expect(alternativas).toEqual(['B', 'CERTO']);
    expect(acoes).toEqual(['confirmar', 'anterior', 'proxima', 'marcar']);
  });
});
