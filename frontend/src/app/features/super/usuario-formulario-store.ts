import { inject, Injectable, signal } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { UsuarioAdminApi } from '../../core/api/usuario-admin-api';
import { aplicarErrosDeCampo, corpoErro, ErroApi, extrairErroApi } from '../../core/api/erro-api';
import { PerfilUsuario } from '../../core/auth/modelos';
import { StatusUsuario, UsuarioGestaoResumo } from '../../core/models/modelos-usuario';

@Injectable({ providedIn: 'root' })
export class UsuarioFormularioStore {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(UsuarioAdminApi);

  readonly usuarioEditando = signal<UsuarioGestaoResumo | null>(null);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);

  readonly formulario = this.fb.group({
    nome: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(120),
    ]),
    email: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.email,
      Validators.maxLength(255),
    ]),
    perfil: this.fb.nonNullable.control<PerfilUsuario>(PerfilUsuario.ESTUDANTE),
    status: this.fb.nonNullable.control<StatusUsuario>(StatusUsuario.CONVITE_PENDENTE),
    enviarConvite: this.fb.nonNullable.control(true),
  });

  carregarForm(usuario: UsuarioGestaoResumo | null): void {
    this.erro.set(null);
    this.usuarioEditando.set(usuario);
    this.formulario.reset(
      usuario
        ? {
            nome: usuario.nome,
            email: usuario.email,
            perfil: usuario.perfil,
            status: usuario.status,
            enviarConvite: false,
          }
        : {
            nome: '',
            email: '',
            perfil: PerfilUsuario.ESTUDANTE,
            status: StatusUsuario.CONVITE_PENDENTE,
            enviarConvite: true,
          },
    );
  }

  async salvar(): Promise<boolean> {
    this.erro.set(null);
    this.salvando.set(true);
    try {
      const dados = this.formulario.getRawValue();
      const id = this.usuarioEditando()?.id;
      await firstValueFrom(id ? this.api.editar(id, dados) : this.api.criar(dados));
      return true;
    } catch (e) {
      this.tratarErro(extrairErroApi(corpoErro(e)));
      return false;
    } finally {
      this.salvando.set(false);
    }
  }

  private tratarErro(erro: ErroApi): void {
    if (erro.fieldErrors) {
      aplicarErrosDeCampo(erro.fieldErrors, this.controles());
      return;
    }
    this.erro.set(erro.message);
  }

  private controles(): Record<string, FormControl> {
    return this.formulario.controls as unknown as Record<string, FormControl>;
  }
}
