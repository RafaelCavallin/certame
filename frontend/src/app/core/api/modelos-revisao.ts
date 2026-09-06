export type EscopoFila = 'TODOS' | 'DECK' | 'SEM_DECK';
export type NotaRevisao = 'ERREI' | 'DIFICIL' | 'BOM' | 'FACIL';

export interface MemoriaCard {
  estabilidade: number;
  dificuldade: number;
  retencaoAtual: number | null;
  rotulo: string;
}

export interface PrevisaoNota {
  nota: NotaRevisao;
  minutos: number;
  rotulo: string;
}

export interface CardRevisao {
  id: string;
  frente: string;
  verso: string;
  deckId: string | null;
  deckNome: string;
  disciplinaNome: string | null;
  assuntoNome: string | null;
  estado: string;
  memoria: MemoriaCard;
  previsoes: PrevisaoNota[];
}

export interface RequisicaoSessaoRevisao {
  escopo: EscopoFila;
  deckId: string | null;
  disciplinaId: string | null;
}

export interface RespostaSessaoRevisao {
  id: string;
  iniciadaEm: string;
  escopoRotulo: string;
  total: number;
  fila: CardRevisao[];
}

export interface RequisicaoRevisao {
  flashcardId: string;
  nota: NotaRevisao;
  tempoMs: number;
}

export interface RespostaRevisao {
  flashcardId: string;
  estado: string;
  estabilidade: number;
  dificuldade: number;
  intervaloMinutos: number;
  intervaloRotulo: string;
  proximaRevisaoEm: string;
  reenfileirar: boolean;
  card: CardRevisao | null;
}

export type FaixaVencimentoTipo = 'AMANHA' | 'PROXIMOS_7_DIAS' | 'DEPOIS_DE_7_DIAS';

export interface FaixaVencimento {
  faixa: FaixaVencimentoTipo;
  rotulo: string;
  quantidade: number;
}

export interface ResumoSessao {
  revisados: number;
  classificacoes: number;
  distribuicao: { errei: number; dificil: number; bom: number; facil: number };
  tempoMs: number;
  tempoRotulo: string;
  proximosVencimentos: FaixaVencimento[];
}

export interface ResumoDeck {
  deckId: string | null;
  nome: string;
  vencendoHoje: number;
  novos: number;
}

export interface ResumoFlashcards {
  vencendoHoje: number;
  proximos7Dias: number;
  novosDisponiveis: number;
  porDeck: ResumoDeck[];
}
