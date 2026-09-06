import { Dificuldade } from '../api/modelos-questao';
export type EstadoSimulado = 'RASCUNHO' | 'AGENDADO' | 'DISPONIVEL' | 'ENCERRADO';
export type TipoTentativa = 'VALIDA' | 'TREINO';
export interface ItemComposicao {
  disciplinaId: string;
  dificuldade: Dificuldade | null;
  quantidade: number;
}
export interface RequisicaoSimulado {
  nome: string;
  descricao: string | null;
  bancaId: string | null;
  numeroQuestoes: number;
  duracaoSegundos: number;
  janelaAbertura: string | null;
  janelaEncerramento: string | null;
}
export interface RequisicaoComposicao {
  itens: ItemComposicao[];
}
export interface ItemComposicaoResumo extends ItemComposicao {
  disciplina: string;
  disponiveis: number;
}
export interface ResumoComposicao {
  itens: ItemComposicaoResumo[];
  somaComposicao: number;
  numeroQuestoes: number;
  fechada: boolean;
  acervoSuficiente: boolean;
}
export interface SimuladoResumoAdmin {
  id: string;
  codigo: string;
  nome: string;
  banca: string | null;
  numeroQuestoes: number;
  duracaoSegundos: number;
  estado: EstadoSimulado;
  janelaAbertura: string | null;
  janelaEncerramento: string | null;
  tentativasConcluidas: number;
  composicaoFechada: boolean;
}
export interface SimuladoDetalhe {
  id: string;
  codigo: string;
  nome: string;
  descricao: string | null;
  bancaId: string | null;
  numeroQuestoes: number;
  duracaoSegundos: number;
  janelaAbertura: string | null;
  janelaEncerramento: string | null;
  status: 'RASCUNHO' | 'PUBLICADO' | 'ENCERRADO';
  publicadoEm: string | null;
  composicao: ResumoComposicao;
}
export interface SimuladoResumo {
  id: string;
  codigo: string;
  nome: string;
  banca: string | null;
  numeroQuestoes: number;
  duracaoSegundos: number;
  estado: 'AGENDADO' | 'DISPONIVEL';
  janelaAbertura: string;
  janelaEncerramento: string;
  tentativaEmAndamentoId: string | null;
}
export interface SimuladoConcluido extends Omit<
  SimuladoResumo,
  'estado' | 'janelaAbertura' | 'janelaEncerramento' | 'tentativaEmAndamentoId'
> {
  tentativaId: string;
  nota: number;
  acertos: number;
  erros: number;
  naoRespondidas: number;
  tempoTotalSegundos: number;
  concluidoEm: string;
  podeRefazer: boolean;
}
export interface RespostaSimulados {
  proximo: SimuladoResumo | null;
  disponiveis: SimuladoResumo[];
  agendados: SimuladoResumo[];
  concluidos: SimuladoConcluido[];
}
export interface RespostaTentativa {
  tentativaId: string;
  simuladoId: string;
  codigo: string;
  nome: string;
  tipo: TipoTentativa;
  duracaoSegundos: number;
  iniciadaEm: string;
  sessao: unknown;
}
import { RespostaCorrecao } from '../api/modelos-estudo';
export interface RespostaResultado {
  tentativaId: string;
  simuladoId: string;
  codigo: string;
  nome: string;
  tipo: TipoTentativa;
  nota: number;
  acertos: number;
  erros: number;
  naoRespondidas: number;
  total: number;
  tempoTotalSegundos: number;
  finalizadaEm: string;
  notaRegistrada: number | null;
  correcao: RespostaCorrecao;
}
export interface PaginaResposta<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}
