import { TestBed } from '@angular/core/testing';
import { ItemCorrecao, RespostaCorrecao } from '../../../core/api/modelos-estudo';
import { CorrecaoProva } from './correcao-prova';

function item(alteracoes: Partial<ItemCorrecao> = {}): ItemCorrecao {
  return {
    ordem: 1,
    questaoId: 'q1',
    codigo: 'Q-1',
    enunciadoResumo: 'Resumo da questão',
    disciplina: 'Direito Constitucional',
    gabarito: 'A',
    marcada: 'A',
    correta: true,
    comentario: null,
    ...alteracoes,
  };
}

function correcaoBase(itens: ItemCorrecao[]): RespostaCorrecao {
  return {
    sessaoId: 's1',
    pctAcerto: 80,
    acertos: 8,
    erros: 1,
    naoRespondidas: 1,
    total: 10,
    tempoTotalSegundos: 600,
    finalizadaEm: '2026-09-01T00:00:00Z',
    itens,
  };
}

function montar(correcao: RespostaCorrecao) {
  const fixture = TestBed.createComponent(CorrecaoProva);
  fixture.componentRef.setInput('correcao', correcao);
  fixture.detectChanges();
  return fixture;
}

describe('CorrecaoProva', () => {
  it('exibe percentual, acertos, erros, não respondidas e tempo total', () => {
    // Arrange & Act
    const fixture = montar(correcaoBase([item()]));

    // Assert
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('80% de acerto');
    expect(texto).toContain('8');
    expect(texto).toContain('600s');
  });

  it('renderiza cada item com a disciplina e a classe de acerto', () => {
    // Arrange & Act
    const fixture = montar(correcaoBase([item({ correta: true })]));

    // Assert
    const artigo = fixture.nativeElement.querySelector('article');
    expect(artigo.classList.contains('acerto')).toBe(true);
    expect(artigo.textContent).toContain('Direito Constitucional');
    expect(artigo.textContent).toContain('Acertou');
  });

  it('marca item errado com a classe erro e a situação correspondente', () => {
    // Arrange & Act
    const fixture = montar(correcaoBase([item({ correta: false, marcada: 'B' })]));

    // Assert
    const artigo = fixture.nativeElement.querySelector('article');
    expect(artigo.classList.contains('erro')).toBe(true);
    expect(artigo.textContent).toContain('Errou');
  });

  it('marca item não respondido com a classe em-branco', () => {
    // Arrange & Act
    const fixture = montar(correcaoBase([item({ correta: null, marcada: null })]));

    // Assert
    const artigo = fixture.nativeElement.querySelector('article');
    expect(artigo.classList.contains('em-branco')).toBe(true);
    expect(artigo.textContent).toContain('Em branco');
    expect(artigo.textContent).toContain('Não respondida');
  });

  it('exibe o comentário quando presente', () => {
    // Arrange & Act
    const fixture = montar(correcaoBase([item({ comentario: 'Atenção ao artigo 5º.' })]));

    // Assert
    expect(fixture.nativeElement.textContent).toContain('Atenção ao artigo 5º.');
  });

  it('omite o comentário quando nulo', () => {
    // Arrange & Act
    const fixture = montar(correcaoBase([item({ comentario: null })]));

    // Assert
    expect(fixture.nativeElement.querySelector('.comentario')).toBeNull();
  });

  it('emite refazer e inicio ao clicar nos respectivos botões', () => {
    // Arrange
    const fixture = montar(correcaoBase([item()]));
    const refazer = vi.fn();
    const inicio = vi.fn();
    fixture.componentInstance.refazer.subscribe(refazer);
    fixture.componentInstance.inicio.subscribe(inicio);

    // Act
    (
      Array.from(fixture.nativeElement.querySelectorAll('button')).find(
        (b) => (b as HTMLButtonElement).textContent?.trim() === 'Refazer',
      ) as HTMLButtonElement
    ).click();
    (
      Array.from(fixture.nativeElement.querySelectorAll('button')).find(
        (b) => (b as HTMLButtonElement).textContent?.trim() === 'Voltar ao início',
      ) as HTMLButtonElement
    ).click();

    // Assert
    expect(refazer).toHaveBeenCalledOnce();
    expect(inicio).toHaveBeenCalledOnce();
  });
});
