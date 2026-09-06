export type TipoQuestao = 'ME' | 'CE';
export type StatusQuestao = 'RASCUNHO' | 'EM_REVISAO' | 'PUBLICADA';
export type Dificuldade = 'FACIL' | 'MEDIA' | 'DIFICIL';

export interface AlternativaDto {
  letra: string | null;
  texto: string;
  ordem: number;
}
export interface QuestaoResumo {
  id: string;
  codigo: string;
  enunciadoResumo: string;
  disciplina: string;
  banca: string | null;
  ano: number | null;
  tipo: TipoQuestao;
  status: StatusQuestao;
  atualizadoEm: string;
}
export interface QuestaoDetalhe {
  id: string;
  codigo: string;
  tipo: TipoQuestao;
  enunciado: string;
  disciplinaId: string | null;
  assuntoId: string | null;
  bancaId: string | null;
  orgaoId: string | null;
  ano: number | null;
  dificuldade: Dificuldade | null;
  alternativas: AlternativaDto[];
  gabarito: string | null;
  comentario: string | null;
  status: StatusQuestao;
  autorId: string;
  publicadoEm: string | null;
  criadoEm: string;
  atualizadoEm: string;
}
export interface RequisicaoQuestao {
  tipo: TipoQuestao;
  enunciado: string;
  disciplinaId: string | null;
  assuntoId: string | null;
  bancaId: string | null;
  orgaoId: string | null;
  ano: number | null;
  dificuldade: Dificuldade | null;
  alternativas: AlternativaDto[];
  gabarito: string | null;
  comentario: string | null;
}
export interface AssuntoArvore {
  id: string;
  nome: string;
  totalQuestoes: number;
  totalPublicadas: number;
  arquivado: boolean;
}
export interface DisciplinaArvore {
  id: string;
  nome: string;
  totalQuestoes: number;
  totalPublicadas: number;
  arquivado: boolean;
  assuntos: AssuntoArvore[];
}
export interface ItemCatalogo {
  id: string;
  nome: string;
  sigla: string | null;
}
export interface PaginaResposta<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}
export interface ErroApi {
  code: string;
  message: string;
  fieldErrors: Record<string, string> | null;
}
export interface RequisicaoNoTaxonomia {
  nome: string;
  disciplinaId?: string;
}
export interface RequisicaoItemCatalogo {
  nome: string;
  sigla: string | null;
}
