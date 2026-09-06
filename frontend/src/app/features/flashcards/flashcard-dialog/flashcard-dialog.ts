import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { ContextoQuestaoFlashcard, FlashcardResumo } from '../../../core/api/modelos-flashcard';
import { DIALOG_DATA, DialogRef } from '../../../shared/ui/dialog/dialog-service';
import { ToastStore } from '../../../shared/ui/toast/toast-store';
import { DeckSeletor } from '../deck-seletor/deck-seletor';
import { FlashcardFormStore } from '../flashcard-form-store';

export type DadosFlashcardDialog =
  ContextoQuestaoFlashcard | { flashcardParaEditar: FlashcardResumo } | null;

@Component({
  selector: 'app-flashcard-dialog',
  imports: [ReactiveFormsModule, DeckSeletor],
  providers: [FlashcardFormStore],
  templateUrl: './flashcard-dialog.html',
  styleUrl: './flashcard-dialog.scss',
})
export class FlashcardDialog {
  private readonly dialogRef = inject(DialogRef);
  private readonly toast = inject(ToastStore);
  protected readonly store = inject(FlashcardFormStore);

  constructor() {
    const dados = inject(DIALOG_DATA) as DadosFlashcardDialog;
    if (dados && 'flashcardParaEditar' in dados) this.store.abrirEdicao(dados.flashcardParaEditar);
    else this.store.abrir(dados);
    this.dialogRef.cancelamentoSolicitado$
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.aoCancelar());
  }

  protected async salvarEFechar(): Promise<void> {
    if (!(await this.store.salvarEFechar())) return;
    this.toast.publicar(this.rotuloSucesso());
    this.fechar();
  }

  protected async salvarECriarOutro(): Promise<void> {
    if (await this.store.salvarECriarOutro())
      this.toast.publicar('Flashcard criado. Crie o próximo.');
  }

  protected aoCancelar(): void {
    if (this.store.cancelar()) this.fechar();
  }

  protected continuarEditando(): void {
    this.store.continuarEditando();
    document.getElementById('fc-frente')?.focus();
  }

  protected confirmarDescarte(): void {
    this.store.confirmarDescarte();
    this.fechar();
  }

  protected onDeckSelecionado(deckId: string | null): void {
    this.store.form.controls.deckId.setValue(deckId);
  }

  private rotuloSucesso(): string {
    if (this.store.modoEdicao()) return 'Flashcard atualizado.';
    return this.store.modoAvulso()
      ? 'Flashcard criado.'
      : 'Flashcard criado — sua sessão continua onde estava.';
  }

  private fechar(): void {
    this.dialogRef.fechar(this.store.ultimoCriado() ?? undefined);
  }
}
