import { PerfilUsuario } from '../auth/modelos';

export enum StatusUsuario {
  ATIVO = 'ATIVO',
  INATIVO = 'INATIVO',
  CONVITE_PENDENTE = 'CONVITE_PENDENTE',
}

export type OrdenacaoUsuarios = 'criadoEm,asc' | 'criadoEm,desc';

export interface UsuarioGestaoResumo {
  id: string;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  status: StatusUsuario;
  criadoEm: string;
}

export interface RequisicaoCriarUsuario {
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  status: StatusUsuario;
  enviarConvite: boolean;
}

export interface RequisicaoEditarUsuario {
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  status: StatusUsuario;
}

export interface FiltroUsuarios {
  page: number;
  size: number;
  sort: OrdenacaoUsuarios;
  perfil?: PerfilUsuario;
  status?: StatusUsuario;
}

export interface PaginaResposta<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}
