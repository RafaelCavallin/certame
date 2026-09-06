import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ErroApi, aplicarErrosDeCampo, extrairErroApi } from '../../../core/api/erro-api';
import { RequisicaoCaderno } from '../../../core/api/modelos-desempenho';
import { CadernosStore } from '../cadernos-store';

@Component({
  selector: 'app-caderno-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './caderno-dialog.html',
  styleUrl: './caderno-dialog.scss',
})
export class CadernoDialog {
  private readonly store = inject(CadernosStore);
  readonly caderno = input<{ id: string; nome: string; descricao: string | null } | null>(null);
  readonly salvo = output<RequisicaoCaderno>();
  readonly fechado = output<void>();
  readonly enviando = signal(false);
  readonly form = new FormGroup({
    nome: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(80)],
    }),
    descricao: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(240)] }),
  });
  constructor() {
    effect(() => {
      const caderno = this.caderno();
      this.form.reset({ nome: caderno?.nome ?? '', descricao: caderno?.descricao ?? '' });
    });
  }
  get editando(): boolean {
    return this.caderno() !== null;
  }
  async salvar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    try {
      const dados = {
        nome: this.form.controls.nome.value.trim(),
        descricao: this.form.controls.descricao.value.trim() || null,
      };
      if (this.caderno()) await this.store.renomear(this.caderno()!.id, dados);
      else await this.store.criar(dados);
      this.salvo.emit(dados);
    } catch (erro) {
      this.aplicarErro(erro);
    } finally {
      this.enviando.set(false);
    }
  }
  aplicarErro(erro: unknown): void {
    const api: ErroApi = extrairErroApi(erro);
    aplicarErrosDeCampo(api.fieldErrors, {
      nome: this.form.controls.nome,
      descricao: this.form.controls.descricao,
    });
  }
}
