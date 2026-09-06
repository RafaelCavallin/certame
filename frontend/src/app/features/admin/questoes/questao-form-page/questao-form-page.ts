import { Component, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { KeyValuePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { PerfilUsuario } from '../../../../core/auth/modelos';
import { TipoQuestao } from '../../../../core/api/modelos-questao';
import { ConfirmacaoDialog } from '../../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
import { QuestaoFormStore } from '../questao-form-store';
@Component({
  selector: 'app-questao-form-page',
  imports: [ReactiveFormsModule, RouterLink, KeyValuePipe, ConfirmacaoDialog],
  providers: [QuestaoFormStore],
  templateUrl: './questao-form-page.html',
  styleUrl: './questao-form-page.scss',
})
export class QuestaoFormPage {
  readonly store = inject(QuestaoFormStore);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthStore);
  readonly tipoPendente = signal<TipoQuestao | null>(null);
  /** A interface esconde a ação; quem autoriza de verdade é o @PreAuthorize do servidor. */
  readonly podePublicar = computed(
    () =>
      this.auth.perfil() === PerfilUsuario.ADMIN ||
      this.auth.perfil() === PerfilUsuario.SUPER_ADMIN,
  );
  constructor() {
    void this.store.carregar(this.rota.snapshot.paramMap.get('id'));
  }
  onCliqueTipo(evento: Event, tipo: TipoQuestao): void {
    if (tipo === this.store.formulario.controls.tipo.value) return;
    evento.preventDefault();
    this.tipoPendente.set(tipo);
  }
  confirmarTrocaTipo(): void {
    const tipo = this.tipoPendente();
    if (!tipo) return;
    this.store.trocarTipo(tipo);
    this.tipoPendente.set(null);
  }
  cancelarTrocaTipo(): void {
    this.tipoPendente.set(null);
  }
  async salvar(): Promise<void> {
    await this.store.salvarRascunho();
    if (!this.store.erro()) await this.router.navigate(['/admin/questoes']);
  }
  async enviar(): Promise<void> {
    await this.store.enviarParaRevisao();
  }
  async publicar(): Promise<void> {
    await this.store.publicar();
  }
  mensagem(c: import('@angular/forms').AbstractControl | null): string | null {
    if (!c || (!c.touched && !c.dirty)) return null;
    if (c.hasError('required')) return 'Preencha este campo.';
    if (c.hasError('minlength')) return 'Informe ao menos 10 caracteres.';
    return c.getError('api') ?? null;
  }
}
