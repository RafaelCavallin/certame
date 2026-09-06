export enum PerfilUsuario {
  ESTUDANTE = 'ESTUDANTE',
  PROFESSOR = 'PROFESSOR',
  ADMIN = 'ADMIN',
  SUPER_ADMIN = 'SUPER_ADMIN',
}

export interface UsuarioResumo {
  id: string;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
}

export interface RespostaAutenticacao {
  accessToken: string;
  expiraEm: string;
  usuario: UsuarioResumo;
}

export interface CredenciaisLogin {
  email: string;
  senha: string;
}

export interface DadosCadastro extends CredenciaisLogin {
  nome: string;
}
