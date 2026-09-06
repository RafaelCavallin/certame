import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';

import { extrairErroApi, ErroApi } from '../api/erro-api';
import { AuthApi } from './auth-api';
import {
  RespostaAutenticacao,
  UsuarioResumo,
  CredenciaisLogin,
  DadosCadastro,
  PerfilUsuario,
} from './modelos';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly api = inject(AuthApi);

  readonly usuario = signal<UsuarioResumo | null>(null);
  readonly accessToken = signal<string | null>(null);
  readonly carregando = signal(false);
  readonly erro = signal<ErroApi | null>(null);
  readonly autenticado = computed(() => this.accessToken() !== null && this.usuario() !== null);
  readonly perfil = computed<PerfilUsuario | null>(() => this.usuario()?.perfil ?? null);

  async entrar(credenciais: CredenciaisLogin): Promise<void> {
    await this.executar(() => this.api.entrar(credenciais));
  }

  async cadastrar(dados: DadosCadastro): Promise<void> {
    await this.executar(() => this.api.cadastrar(dados));
  }

  async restaurarSessao(): Promise<void> {
    try {
      await this.executar(() => this.api.refresh(), false);
    } catch {
      this.limpar();
    }
  }

  async renovar(): Promise<string> {
    const resposta = await this.executar(() => this.api.refresh());
    return resposta.accessToken;
  }

  async sair(): Promise<void> {
    try {
      await firstValueFrom(this.api.sair());
    } finally {
      this.limpar();
    }
  }

  limpar(): void {
    this.usuario.set(null);
    this.accessToken.set(null);
    this.erro.set(null);
  }

  private async executar<T>(requisicao: () => Observable<T>, registrarErro = true): Promise<T> {
    this.carregando.set(true);
    this.erro.set(null);
    try {
      const resposta = await firstValueFrom(requisicao());
      if (this.ehRespostaAutenticacao(resposta)) {
        this.aplicarResposta(resposta);
      }
      return resposta;
    } catch (erro) {
      if (registrarErro) {
        this.erro.set(extrairErroApi(this.extrairCorpoErro(erro)));
      }
      throw erro;
    } finally {
      this.carregando.set(false);
    }
  }

  private aplicarResposta(resposta: RespostaAutenticacao): void {
    this.usuario.set(resposta.usuario);
    this.accessToken.set(resposta.accessToken);
  }

  private extrairCorpoErro(erro: unknown): unknown {
    return erro && typeof erro === 'object' && 'error' in erro
      ? (erro as { error: unknown }).error
      : erro;
  }

  private ehRespostaAutenticacao(resposta: unknown): resposta is RespostaAutenticacao {
    return (
      !!resposta &&
      typeof resposta === 'object' &&
      'accessToken' in resposta &&
      'usuario' in resposta
    );
  }
}
