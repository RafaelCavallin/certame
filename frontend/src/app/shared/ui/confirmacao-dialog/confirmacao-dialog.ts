import { Component, input, output } from '@angular/core';
@Component({
  selector: 'app-confirmacao-dialog',
  templateUrl: './confirmacao-dialog.html',
  styleUrl: './confirmacao-dialog.scss',
})
export class ConfirmacaoDialog {
  titulo = input('Confirmar ação');
  mensagem = input('Esta ação não pode ser desfeita.');
  confirmarLabel = input('Confirmar');
  varianteArquivar = input(false);
  confirmado = output<void>();
  cancelado = output<void>();
}
