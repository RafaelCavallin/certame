import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EditaisAdminStore } from '../editais-admin-store';
import { ConfirmacaoDialog } from '../../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
import { StatusEdital } from '../../../../core/api/modelos-edital';

const ROTULOS_STATUS: Record<StatusEdital, string> = {
  ABERTO: 'Aberto',
  PREVISTO: 'Previsto',
  ENCERRADO: 'Encerrado',
};
const CLASSES_STATUS: Record<StatusEdital, string> = {
  ABERTO: 'tag-accent',
  PREVISTO: 'tag-accent-2',
  ENCERRADO: 'tag-encerrado',
};

@Component({
  selector: 'app-editais-admin-page',
  imports: [DatePipe, DecimalPipe, RouterLink, ConfirmacaoDialog],
  providers: [EditaisAdminStore],
  templateUrl: './editais-admin-page.html',
  styleUrl: './editais-admin-page.scss',
})
export class EditaisAdminPage {
  protected readonly store = inject(EditaisAdminStore);
  protected readonly exclusaoId = signal<string | null>(null);

  constructor() {
    void this.store.carregarLista();
  }

  protected rotuloStatus(status: StatusEdital): string {
    return ROTULOS_STATUS[status];
  }
  protected classeStatus(status: StatusEdital): string {
    return `tag ${CLASSES_STATUS[status]}`;
  }
  protected confirmarExclusao(id: string): void {
    this.exclusaoId.set(id);
  }

  protected async excluir(): Promise<void> {
    const id = this.exclusaoId();
    if (!id) return;
    await this.store.excluir(id);
    this.exclusaoId.set(null);
  }
}
