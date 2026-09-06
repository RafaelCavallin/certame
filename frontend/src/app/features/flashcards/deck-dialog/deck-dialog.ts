import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DeckDetalhe } from '../../../core/api/modelos-flashcard';
import { DIALOG_DATA, DialogRef } from '../../../shared/ui/dialog/dialog-service';
import { ToastStore } from '../../../shared/ui/toast/toast-store';
import { DecksStore } from '../decks-store';

@Component({
  selector: 'app-deck-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './deck-dialog.html',
  styleUrl: './deck-dialog.scss',
})
export class DeckDialog {
  private readonly dialogRef = inject(DialogRef);
  private readonly toast = inject(ToastStore);
  protected readonly store = inject(DecksStore);
  private readonly deck = inject(DIALOG_DATA) as DeckDetalhe | null;

  protected readonly editando = this.deck !== null;
  protected readonly form = new FormGroup({
    nome: new FormControl(this.deck?.nome ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(60)],
    }),
    descricao: new FormControl(this.deck?.descricao ?? '', {
      nonNullable: true,
      validators: [Validators.maxLength(200)],
    }),
    novosDia: new FormControl(this.deck?.limites.novosDia ?? 10, { nonNullable: true }),
    revisoesDia: new FormControl(this.deck?.limites.revisoesDia ?? 60, { nonNullable: true }),
    intervaloMaxDias: new FormControl(this.deck?.limites.intervaloMaxDias ?? 180, {
      nonNullable: true,
    }),
    retencaoAlvo: new FormControl(this.deck?.limites.retencaoAlvo ?? 0.9, { nonNullable: true }),
  });

  protected async salvar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    await this.store.salvar(this.dados(), this.deck?.id ?? undefined);
    if (this.store.erroDeCampo().nome) return;
    this.toast.publicar(this.editando ? 'Deck atualizado.' : 'Deck criado.');
    this.dialogRef.fechar();
  }

  protected cancelar(): void {
    this.dialogRef.fechar();
  }

  private dados() {
    const v = this.form.getRawValue();
    return {
      nome: v.nome.trim(),
      descricao: v.descricao.trim() || null,
      novosDia: v.novosDia,
      revisoesDia: v.revisoesDia,
      intervaloMaxDias: v.intervaloMaxDias,
      retencaoAlvo: v.retencaoAlvo,
    };
  }
}
