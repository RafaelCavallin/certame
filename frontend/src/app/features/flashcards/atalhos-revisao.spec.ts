import { TestBed } from '@angular/core/testing';
import { AtalhosRevisao } from './atalhos-revisao';

function criarDiretiva(revelado: boolean) {
  const diretiva = TestBed.runInInjectionContext(() => new AtalhosRevisao());
  (diretiva as unknown as { revelado: () => boolean }).revelado = () => revelado;
  return diretiva;
}

function disparar(diretiva: AtalhosRevisao, key: string, alvo: Partial<HTMLElement> = {}) {
  diretiva.tecla({
    key,
    code: key === ' ' ? 'Space' : key,
    target: alvo,
    preventDefault: () => undefined,
  } as unknown as KeyboardEvent);
}

describe('AtalhosRevisao', () => {
  it('mapeia espaço e as teclas 1 a 4', () => {
    const diretiva = criarDiretiva(true);
    const revelados: void[] = [];
    const notas: string[] = [];
    diretiva.revelar.subscribe(() => revelados.push(undefined));
    diretiva.classificar.subscribe((nota) => notas.push(nota));

    disparar(diretiva, '1');
    disparar(diretiva, '2');
    disparar(diretiva, '3');
    disparar(diretiva, '4');

    expect(notas).toEqual(['ERREI', 'DIFICIL', 'BOM', 'FACIL']);
  });

  it('ignora 1–4 com o verso oculto', () => {
    const diretiva = criarDiretiva(false);
    const notas: string[] = [];
    diretiva.classificar.subscribe((nota) => notas.push(nota));

    disparar(diretiva, '1');

    expect(notas).toEqual([]);
  });

  it('ignora teclas vindas de campo de texto', () => {
    const diretiva = criarDiretiva(false);
    const revelados: void[] = [];
    diretiva.revelar.subscribe(() => revelados.push(undefined));

    disparar(diretiva, ' ', { tagName: 'INPUT' });

    expect(revelados).toEqual([]);
  });

  it('revela ao pressionar espaço quando ainda oculto', () => {
    const diretiva = criarDiretiva(false);
    const revelados: void[] = [];
    diretiva.revelar.subscribe(() => revelados.push(undefined));

    disparar(diretiva, ' ');

    expect(revelados).toHaveLength(1);
  });
});
