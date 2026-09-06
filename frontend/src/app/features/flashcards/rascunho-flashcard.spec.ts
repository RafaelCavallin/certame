import { gravarRascunho, lerRascunho, limparRascunho } from './rascunho-flashcard';

describe('rascunho-flashcard', () => {
  const questaoId = 'q-1042';

  beforeEach(() => sessionStorage.clear());

  it('TU-38 grava e restaura o rascunho sob a chave fc-draft:{questaoId}', () => {
    gravarRascunho(questaoId, { frente: 'Frente digitada', verso: 'Verso digitado' });

    expect(sessionStorage.getItem(`fc-draft:${questaoId}`)).not.toBeNull();
    expect(lerRascunho(questaoId)).toEqual({ frente: 'Frente digitada', verso: 'Verso digitado' });
  });

  it('TU-38 restaura null quando não há rascunho gravado', () => {
    expect(lerRascunho('sem-rascunho')).toBeNull();
  });

  it('TU-38 limpa o rascunho no salvamento ou no descarte confirmado', () => {
    gravarRascunho(questaoId, { frente: 'Frente', verso: 'Verso' });

    limparRascunho(questaoId);

    expect(lerRascunho(questaoId)).toBeNull();
  });

  it('TU-38 não derruba a leitura quando o valor gravado está corrompido', () => {
    sessionStorage.setItem(`fc-draft:${questaoId}`, '{ inválido');

    expect(lerRascunho(questaoId)).toBeNull();
  });
});
