import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthApi } from '../../../core/auth/auth-api';
import { extrairErroApi, corpoErro } from '../../../core/api/erro-api';

type Estado = 'formulario' | 'concluido' | 'invalido';

@Component({
  selector: 'app-definir-senha-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './definir-senha-page.html',
  styleUrl: './definir-senha-page.scss',
})
export class DefinirSenhaPage {
  private readonly api = inject(AuthApi);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  readonly estado = signal<Estado>('formulario');
  readonly erro = signal<string | null>(null);
  readonly token = this.rota.snapshot.paramMap.get('token') ?? '';
  readonly formulario = this.fb.nonNullable.group({
    senha: ['', [Validators.required, Validators.minLength(8)]],
  });

  async enviar(): Promise<void> {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.erro.set(null);
    try {
      await firstValueFrom(
        this.api.redefinirSenha(this.token, this.formulario.controls.senha.value),
      );
      this.estado.set('concluido');
    } catch (erro) {
      const api = extrairErroApi(corpoErro(erro));
      this.erro.set(api.message);
      if (api.code === 'TOKEN_INVALIDO') this.estado.set('invalido');
    }
  }

  irParaEsqueci(): void {
    void this.router.navigateByUrl('/esqueci-senha');
  }
}
