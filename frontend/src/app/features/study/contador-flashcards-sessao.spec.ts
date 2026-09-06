import { gravarContadorFlashcards, lerContadorFlashcards } from './contador-flashcards-sessao';

describe('contador-flashcards-sessao', () => {
  const sessaoId = 's-77';

  beforeEach(() => sessionStorage.clear());

  it('grava e restaura o contador sob a chave fc-contador:{sessaoId}', () => {
    gravarContadorFlashcards(sessaoId, 3);

    expect(sessionStorage.getItem(`fc-contador:${sessaoId}`)).toBe('3');
    expect(lerContadorFlashcards(sessaoId)).toBe(3);
  });

  it('restaura null quando não há contador gravado', () => {
    expect(lerContadorFlashcards('sem-contador')).toBeNull();
  });

  it('não derruba a leitura quando o valor gravado está corrompido', () => {
    sessionStorage.setItem(`fc-contador:${sessaoId}`, 'abc');

    expect(lerContadorFlashcards(sessaoId)).toBeNull();
  });
});
