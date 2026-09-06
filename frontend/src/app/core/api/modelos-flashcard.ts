export type EstadoFlashcard = 'NOVO' | 'APRENDENDO' | 'REVISAO' | 'REAPRENDENDO';
export type StatusFlashcard = 'ATIVO' | 'ARQUIVADO';

export interface ReferenciaResumo {
  id: string;
  nome: string;
}
export interface OrigemResumo {
  questaoId: string;
  codigo: string;
  sessaoId: string | null;
}

export interface FlashcardResumo {
  id: string;
  frente: string;
  verso: string;
  deck: ReferenciaResumo | null;
  disciplina: ReferenciaResumo | null;
  assunto: ReferenciaResumo | null;
  tags: string[];
  estado: EstadoFlashcard;
  status: StatusFlashcard;
  proximaRevisao: string;
  origem: OrigemResumo | null;
  criadoEm: string;
}

export interface IndicadoresFlashcards {
  vencendoHoje: number;
  proximosSeteDias: number;
  emFixacao: number;
  total: number;
  decks: number;
}

export interface PaginaResposta<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}
export interface PaginaFlashcards extends PaginaResposta<FlashcardResumo> {
  indicadores: IndicadoresFlashcards;
}

export interface RequisicaoFlashcard {
  frente: string;
  verso: string;
  deckId: string | null;
  disciplinaId: string | null;
  assuntoId: string | null;
  tags: string[];
  questaoOrigemId: string | null;
  sessaoOrigemId: string | null;
}

export interface RespostaFlashcardCriado {
  flashcard: FlashcardResumo;
  criadosNaSessao: number | null;
}

export interface DeckResumo {
  id: string;
  nome: string;
  descricao: string | null;
  totalCards: number;
}

export interface LimitesDeck {
  novosDia: number;
  revisoesDia: number;
  intervaloMaxDias: number;
  retencaoAlvo: number;
}
export interface AvisoDeck {
  tipo: 'FILA_ACUMULADA';
  mensagem: string;
  vencidos: number;
  limiteDiario: number;
}

export interface DeckDetalhe {
  id: string | null;
  nome: string;
  descricao: string | null;
  totalCards: number;
  vencendoHoje: number;
  novos: number;
  limites: LimitesDeck;
  limitesRotulo: string;
  retencaoRotulo: string;
  editavel: boolean;
  aviso: AvisoDeck | null;
}

export interface RequisicaoDeck {
  nome: string;
  descricao?: string | null;
  novosDia?: number | null;
  revisoesDia?: number | null;
  intervaloMaxDias?: number | null;
  retencaoAlvo?: number | null;
}
export interface RespostaExclusaoDeck {
  cardsMovidos: number;
}
export interface RequisicaoMoverCards {
  flashcardIds: string[];
}
export interface RespostaMoverCards {
  movidos: number;
}

/** Descarta a linha virtual "Sem deck" (`id: null`) para seletores que já a representam à parte. */
export function decksComId(decks: DeckDetalhe[]): DeckResumo[] {
  return decks
    .filter((deck): deck is DeckDetalhe & { id: string } => deck.id !== null)
    .map((deck) => ({
      id: deck.id,
      nome: deck.nome,
      descricao: deck.descricao,
      totalCards: deck.totalCards,
    }));
}

export interface FiltroFlashcards {
  q?: string;
  deck?: string;
  disciplina?: string;
  estado?: EstadoFlashcard;
  arquivados?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

/** Contrato de UI com F2 (prd-sessao-de-estudo): única dependência entre a sessão e o diálogo. */
export interface ContextoQuestaoFlashcard {
  questaoId: string;
  codigo: string;
  disciplinaId: string | null;
  disciplinaNome: string | null;
  assuntoId: string | null;
  assuntoNome: string | null;
  sessaoId: string | null;
  devolverFocoPara: HTMLElement;
}
