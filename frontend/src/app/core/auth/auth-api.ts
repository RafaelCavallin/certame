import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { RespostaAutenticacao, CredenciaisLogin, DadosCadastro } from './modelos';

interface RequisicaoEmail {
  email: string;
}

interface RequisicaoRedefinirSenha {
  token: string;
  senha: string;
}

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/auth';

  entrar(credenciais: CredenciaisLogin): Observable<RespostaAutenticacao> {
    return this.http.post<RespostaAutenticacao>(`${this.baseUrl}/login`, credenciais, {
      withCredentials: true,
    });
  }

  cadastrar(dados: DadosCadastro): Observable<RespostaAutenticacao> {
    return this.http.post<RespostaAutenticacao>(`${this.baseUrl}/cadastro`, dados, {
      withCredentials: true,
    });
  }

  esqueciSenha(email: string): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/senha/esqueci`,
      { email } satisfies RequisicaoEmail,
      { withCredentials: true },
    );
  }

  redefinirSenha(token: string, senha: string): Observable<void> {
    const corpo: RequisicaoRedefinirSenha = { token, senha };
    return this.http.post<void>(`${this.baseUrl}/senha/redefinir`, corpo, {
      withCredentials: true,
    });
  }

  refresh(): Observable<RespostaAutenticacao> {
    return this.http.post<RespostaAutenticacao>(`${this.baseUrl}/refresh`, null, {
      withCredentials: true,
    });
  }

  sair(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, null, { withCredentials: true });
  }
}
