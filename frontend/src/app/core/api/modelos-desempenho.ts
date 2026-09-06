export type TipoCaderno = 'AUTOMATICO' | 'MANUAL';

export interface CadernoResumo {
  id: string;
  tipo: TipoCaderno;
  nome: string;
  descricao: string | null;
  totalQuestoes: number;
  questoesRespondidas: number;
  progressoPercentual: number;
  atualizadoEm: string | null;
  editavel: boolean;
}

export interface RequisicaoCaderno {
  nome: string;
  descricao: string | null;
}
export interface RequisicaoQuestoesCaderno {
  questaoIds: string[];
}
export interface QuestaoCadernoResumo {
  questaoId: string;
  codigo: string;
  enunciadoResumo: string;
  disciplina: string;
  assunto: string | null;
  respondida: boolean;
}
export interface PaginaResposta<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}

export interface ResumoDesempenho {
  totalRespostas: number;
  acertos: number;
  erros: number;
  pctAproveitamento: number | null;
  possuiHistorico: boolean;
}

export interface SemanaEvolucao {
  semana: string;
  rotulo: string;
  volume: number;
  pctAcerto: number | null;
}

export interface EvolucaoSemanal {
  semanas: SemanaEvolucao[];
  pctMediaPeriodo: number | null;
  melhorSemana: SemanaEvolucao | null;
  volumeTotal: number;
}

export interface DesempenhoDisciplinaResumo {
  disciplinaId: string;
  disciplina: string;
  volume: number;
  pctAcerto: number;
}

export interface PontoFraco {
  assuntoId: string;
  assunto: string;
  disciplinaId: string;
  disciplina: string;
  volume: number;
  pctAcerto: number;
}

export interface PontosFracos {
  pctMediaUsuario: number | null;
  minimoRespostas: number;
  itens: PontoFraco[];
}
