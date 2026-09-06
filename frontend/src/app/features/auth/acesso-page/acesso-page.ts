import { Component, ElementRef, ViewChild, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, Validators, FormBuilder, FormControl } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extrairErroApi, aplicarErrosDeCampo, corpoErro } from '../../../core/api/erro-api';
import { AuthStore } from '../../../core/auth/auth-store';
import { rotaInicialPorPerfil } from '../../../core/auth/rota-inicial';

@Component({
  selector: 'app-acesso-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './acesso-page.html',
  styleUrl: './acesso-page.scss',
})
export class AcessoPage {
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly auth = inject(AuthStore);
  private readonly fb = inject(FormBuilder);
  readonly cadastro = signal(this.rota.snapshot.data['aba'] === 'cadastro');
  readonly erro = signal<string | null>(null);
  readonly sucesso = signal(this.rota.snapshot.queryParamMap.has('sucesso'));
  readonly formulario = this.fb.nonNullable.group({
    nome: ['', [Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(8)]],
  });
  readonly titulo = computed(() => (this.cadastro() ? 'Crie sua conta' : 'Bem-vinda de volta'));
  readonly subtitulo = computed(() =>
    this.cadastro()
      ? 'Comece a resolver questões em segundos.'
      : 'Entre para continuar seus estudos.',
  );
  readonly chamada = computed(() => (this.cadastro() ? 'Criar conta' : 'Entrar'));
  @ViewChild('senha') private readonly campoSenha?: ElementRef<HTMLInputElement>;

  alternar(cadastro: boolean): void {
    this.erro.set(null);
    this.formulario.reset({ nome: '', email: this.formulario.controls.email.value, senha: '' });
    void this.router.navigateByUrl(cadastro ? '/cadastro' : '/login');
    this.cadastro.set(cadastro);
  }

  async enviar(): Promise<void> {
    if (
      this.formulario.invalid ||
      (this.cadastro() && !this.formulario.controls.nome.value.trim())
    ) {
      this.formulario.markAllAsTouched();
      if (this.cadastro()) this.formulario.controls.nome.setErrors({ required: true });
      return;
    }
    this.erro.set(null);
    try {
      const valores = this.formulario.getRawValue();
      if (this.cadastro()) {
        await this.auth.cadastrar({
          nome: valores.nome,
          email: valores.email,
          senha: valores.senha,
        });
      } else {
        await this.auth.entrar({ email: valores.email, senha: valores.senha });
      }
      const perfil = this.auth.perfil();
      if (perfil) await this.router.navigateByUrl(rotaInicialPorPerfil(perfil, this.router));
    } catch (erro) {
      const api = extrairErroApi(corpoErro(erro));
      aplicarErrosDeCampo(api.fieldErrors, this.controles());
      if (api.code === 'EMAIL_EM_USO')
        this.formulario.controls.email.setErrors({ api: api.message });
      this.erro.set(api.message);
      if (api.code === 'CREDENCIAL_INVALIDA') this.campoSenha?.nativeElement.focus();
    }
  }

  mensagem(controle: FormControl): string | null {
    if (!controle.touched && !controle.dirty) return null;
    if (controle.hasError('required')) return 'Preencha este campo.';
    if (controle.hasError('email')) return 'Informe um e-mail válido.';
    if (controle.hasError('minlength'))
      return `${controle === this.formulario.controls.nome ? 'O nome deve ter' : 'A senha deve ter'} ao menos ${controle.getError('minlength').requiredLength} caracteres.`;
    return controle.getError('api') ?? null;
  }

  private controles(): Record<string, FormControl> {
    return this.formulario.controls as unknown as Record<string, FormControl>;
  }
}
