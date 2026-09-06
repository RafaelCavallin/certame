export type StatusEdital = 'ABERTO' | 'PREVISTO' | 'ENCERRADO';

export interface ItemCatalogoResumo {
  id: string;
  nome: string;
  sigla: string | null;
}
export interface DisciplinaExigida {
  id: string;
  nome: string;
}

export interface EditalResumo {
  id: string;
  nome: string;
  status: StatusEdital;
  vagas: number | null;
  prazoInscricao: string | null;
  urlOficial: string | null;
  banca: ItemCatalogoResumo | null;
  orgao: ItemCatalogoResumo | null;
  disciplinas: DisciplinaExigida[];
}

export interface EditaisAgrupados {
  abertos: EditalResumo[];
  previstos: EditalResumo[];
  encerrados: EditalResumo[];
}

export interface EditalAdminResumo {
  id: string;
  nome: string;
  status: StatusEdital;
  vagas: number | null;
  prazoInscricao: string | null;
  banca: ItemCatalogoResumo | null;
  orgao: ItemCatalogoResumo | null;
  totalDisciplinas: number;
  prazoVencido: boolean;
  atualizadoEm: string;
}

export interface EditalDetalhe {
  id: string;
  nome: string;
  status: StatusEdital;
  vagas: number | null;
  prazoInscricao: string | null;
  urlOficial: string | null;
  banca: ItemCatalogoResumo | null;
  orgao: ItemCatalogoResumo | null;
  disciplinas: DisciplinaExigida[];
  criadoEm: string;
  atualizadoEm: string;
}

export interface RequisicaoEdital {
  nome: string;
  status: StatusEdital;
  vagas: number | null;
  bancaId: string | null;
  orgaoId: string | null;
  prazoInscricao: string | null;
  urlOficial: string | null;
  disciplinaIds: string[];
}
