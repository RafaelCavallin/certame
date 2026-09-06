import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthApi } from '../../../core/auth/auth-api';
import { extrairErroApi, corpoErro } from '../../../core/api/erro-api';

@Component({
  selector: 'app-esqueci-senha-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './esqueci-senha-page.html',
  styleUrl: './esqueci-senha-page.scss',
})
export class EsqueciSenhaPage {
  private readonly api = inject(AuthApi);
  private readonly fb = inject(FormBuilder);
  readonly enviado = signal(false);
  readonly erro = signal<string | null>(null);
  readonly formulario = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  async enviar(): Promise<void> {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.erro.set(null);
    try {
      await firstValueFrom(this.api.esqueciSenha(this.formulario.controls.email.value));
      this.enviado.set(true);
    } catch (erro) {
      this.erro.set(extrairErroApi(corpoErro(erro)).message);
    }
  }
}
