export type ModoSessao = 'FOCO' | 'CONTEXTO' | 'PROVA';
export type OrigemSessao = 'LIVRE' | 'CADERNO' | 'SIMULADO';
export type TipoQuestaoEstudo = 'ME' | 'CE';
export type DificuldadeEstudo = 'FACIL' | 'MEDIA' | 'DIFICIL';

export interface AlternativaDaSerie {
  letra: string;
  texto: string;
  ordem: number;
}
export interface EstatisticaQuestao {
  pctAcerto: number;
  totalRespostas: number;
  tempoMedioSegundos: number;
}
export interface QuestaoDaSerie {
  id: string;
  ordem: number;
  codigo: string;
  tipo: TipoQuestaoEstudo;
  enunciado: string;
  disciplina: string | null;
  assunto: string | null;
  banca: string | null;
  orgao: string | null;
  ano: number | null;
  dificuldade: DificuldadeEstudo | null;
  alternativas: AlternativaDaSerie[];
  favorita: boolean;
  erroRecorrente: boolean;
}
export interface RespostaRegistrada {
  questaoId: string;
  marcada: string;
  correta: boolean;
  gabarito: string;
  comentario: string | null;
  estatistica: EstatisticaQuestao | null;
  tempoMs: number;
  respondidaEm: string;
}
export interface RespostaSessao {
  sessaoId: string;
  modo: ModoSessao;
  origem: OrigemSessao;
  origemRef: string | null;
  total: number;
  indiceAtual: number;
  iniciadaEm: string;
  decorridoSegundos: number;
  questoes: QuestaoDaSerie[];
  respostas: RespostaRegistrada[];
}
export interface FiltroSerie {
  disciplinaId?: string | null;
  assuntoId?: string | null;
  bancaId?: string | null;
  ano?: number | null;
  dificuldade?: DificuldadeEstudo | null;
  somenteIneditas?: boolean;
  excluirAcertadas?: boolean;
}
export interface RequisicaoSessao {
  modo?: ModoSessao;
  tamanho?: number;
  origem?: OrigemSessao;
  origemRef?: string | null;
  filtro?: FiltroSerie;
}
export interface RequisicaoResposta {
  questaoId: string;
  marcada: string;
  tempoMs: number;
  pausadoMs?: number;
}
export interface RespostaProva {
  questaoId: string;
  marcada: string;
  tempoMs: number;
}
export interface RequisicaoFinalizacao {
  respostas: RespostaProva[];
  pausadoMs?: number;
}
export interface ItemCorrecao {
  ordem: number;
  questaoId: string;
  codigo: string;
  enunciadoResumo: string;
  disciplina: string | null;
  gabarito: string;
  marcada: string | null;
  correta: boolean | null;
  comentario: string | null;
}
export interface RespostaCorrecao {
  sessaoId: string;
  pctAcerto: number;
  acertos: number;
  erros: number;
  naoRespondidas: number;
  total: number;
  tempoTotalSegundos: number;
  finalizadaEm: string;
  itens: ItemCorrecao[];
}
export interface MarcacoesQuestao {
  favorita: boolean;
  erroRecorrente: boolean;
}
export interface SessaoRetomavel {
  sessaoId: string;
  modo: ModoSessao;
  origem: OrigemSessao;
  titulo: string | null;
  indiceAtual: number;
  total: number;
  ultimaAtividadeEm: string;
}
export interface ResumoInicio {
  dia: string;
  questoesResolvidas: number;
  pctAcerto: number | null;
  tempoEstudoSegundos: number;
  tempoQuestoesSegundos: number;
  tempoFlashcardsSegundos: number;
  sessoesRetomaveis: SessaoRetomavel[];
}
