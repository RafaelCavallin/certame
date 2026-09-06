import { Component, inject } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '../dialog/dialog-service';
import { ConfirmacaoDialog } from './confirmacao-dialog';

export interface DadosConfirmacaoDialogo {
  readonly titulo?: string;
  readonly mensagem?: string;
  readonly confirmarLabel?: string;
}

/** Abre o ConfirmacaoDialog via DialogService (fora de um template), para uso em guards e serviços. */
@Component({
  selector: 'app-confirmacao-dialogo',
  imports: [ConfirmacaoDialog],
  template: `<app-confirmacao-dialog
    [titulo]="dados?.titulo ?? 'Confirmar ação'"
    [mensagem]="dados?.mensagem ?? 'Esta ação não pode ser desfeita.'"
    [confirmarLabel]="dados?.confirmarLabel ?? 'Confirmar'"
    (confirmado)="dialogRef.fechar(true)"
    (cancelado)="dialogRef.fechar(false)"
  />`,
})
export class ConfirmacaoDialogo {
  protected readonly dialogRef = inject(DialogRef);
  protected readonly dados = inject(DIALOG_DATA) as DadosConfirmacaoDialogo | null;
}
