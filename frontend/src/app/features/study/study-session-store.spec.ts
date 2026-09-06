import { StudySessionStore } from './study-session-store';
import { RespostaSessao } from '../../core/api/modelos-estudo';

const sessao: RespostaSessao = {
  sessaoId: 's1',
  modo: 'FOCO',
  origem: 'LIVRE',
  origemRef: null,
  total: 2,
  indiceAtual: 0,
  iniciadaEm: '',
  decorridoSegundos: 0,
  questoes: [
    {
      id: 'q1',
      ordem: 1,
      codigo: 'Q1',
      tipo: 'ME',
      enunciado: 'A',
      disciplina: null,
      assunto: null,
      banca: null,
      orgao: null,
      ano: null,
      dificuldade: null,
      alternativas: [{ letra: 'A', texto: 'A', ordem: 1 }],
      favorita: false,
      erroRecorrente: false,
    },
    {
      id: 'q2',
      ordem: 2,
      codigo: 'Q2',
      tipo: 'ME',
      enunciado: 'B',
      disciplina: null,
      assunto: null,
      banca: null,
      orgao: null,
      ano: null,
      dificuldade: null,
      alternativas: [],
      favorita: false,
      erroRecorrente: false,
    },
  ],
  respostas: [],
};
describe('StudySessionStore', () => {
  it('avança o índice sem nova requisição', () => {
    const store = new StudySessionStore();
    store.iniciar(sessao);
    store.proxima();
    expect(store.indice()).toBe(1);
    expect(store.questaoAtual()?.id).toBe('q2');
  });
  it('mantém a alternativa marcada ao trocar de modo', () => {
    const store = new StudySessionStore();
    store.iniciar(sessao);
    store.marcarAlternativa('q1', 'A');
    store.trocarModo('CONTEXTO');
    expect(store.alternativaMarcada('q1')).toBe('A');
  });
  it('substitui a alternativa marcada sem acumular', () => {
    const store = new StudySessionStore();
    store.iniciar({ ...sessao, modo: 'PROVA' });
    store.marcarAlternativa('q1', 'A');
    store.marcarAlternativa('q1', 'B');
    expect(store.respostas()).toHaveLength(1);
    expect(store.alternativaMarcada('q1')).toBe('B');
  });
  it('não expõe gabarito no Modo Prova', () => {
    const store = new StudySessionStore();
    store.iniciar({ ...sessao, modo: 'PROVA' });
    store.marcarAlternativa('q1', 'A');
    expect(store.respostaDa('q1')?.gabarito).toBe('');
  });
  it('permite trocar resposta já respondida na prova', () => {
    const store = new StudySessionStore();
    store.iniciar({ ...sessao, modo: 'PROVA' });
    store.marcarAlternativa('q1', 'A');
    store.marcarAlternativa('q1', 'B');
    expect(store.alternativaMarcada('q1')).toBe('B');
  });
  it('bloqueia nova resposta fora da prova', () => {
    const store = new StudySessionStore();
    store.iniciar(sessao);
    store.marcarAlternativa('q1', 'A');
    expect(store.respostas()).toHaveLength(0);
  });
  it('vai direto para a questão 2 preservando respostas', () => {
    const store = new StudySessionStore();
    store.iniciar({ ...sessao, modo: 'PROVA' });
    store.marcarAlternativa('q1', 'A');
    store.irPara(1);
    expect(store.indice()).toBe(1);
    expect(store.alternativaMarcada('q1')).toBe('A');
  });
  it('cai na última questão ao retomar uma sessão totalmente respondida', () => {
    const store = new StudySessionStore();
    store.iniciar({ ...sessao, indiceAtual: sessao.total });
    expect(store.indice()).toBe(1);
    expect(store.questaoAtual()?.id).toBe('q2');
  });
});
