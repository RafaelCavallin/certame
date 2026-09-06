import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CadernosStore } from '../cadernos-store';
import { CadernoDialog } from '../caderno-dialog/caderno-dialog';
import { QuestoesPickerDialog } from '../questoes-picker-dialog/questoes-picker-dialog';
import { EstadoVazio } from '../../../shared/ui/estado-vazio/estado-vazio';
import { ConfirmacaoDialog } from '../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
import { CadernoResumo } from '../../../core/api/modelos-desempenho';

@Component({
  selector: 'app-cadernos-page',
  imports: [CadernoDialog, QuestoesPickerDialog, EstadoVazio, ConfirmacaoDialog],
  templateUrl: './cadernos-page.html',
  styleUrl: './cadernos-page.scss',
})
export class CadernosPage {
  readonly store = inject(CadernosStore);
  private readonly router = inject(Router);
  readonly dialogAberto = signal(false);
  readonly dialog = signal<CadernoResumo | null>(null);
  readonly menu = signal<CadernoResumo | null>(null);
  readonly picker = signal<CadernoResumo | null>(null);
  readonly excluir = signal<CadernoResumo | null>(null);
  constructor() {
    void this.store.carregar();
  }
  abrirDialog(caderno: CadernoResumo | null = null): void {
    this.dialog.set(caderno);
    this.dialogAberto.set(true);
  }
  fecharDialog(): void {
    this.dialogAberto.set(false);
    this.dialog.set(null);
  }
  abrirMenu(caderno: CadernoResumo): void {
    this.menu.set(this.menu()?.id === caderno.id ? null : caderno);
  }
  abrirPicker(caderno: CadernoResumo): void {
    this.picker.set(caderno);
  }
  async adicionar(questaoIds: string[]): Promise<void> {
    const caderno = this.picker();
    if (!caderno) return;
    try {
      await this.store.adicionarQuestoes(caderno.id, questaoIds);
      this.picker.set(null);
    } catch {
      this.store.erro.set('Não foi possível adicionar as questões.');
    }
  }
  async confirmarExclusao(): Promise<void> {
    const caderno = this.excluir();
    if (!caderno) return;
    try {
      await this.store.excluir(caderno.id);
    } catch {
      this.store.erro.set('Não foi possível excluir o caderno.');
    } finally {
      this.excluir.set(null);
    }
  }
  async estudar(caderno: CadernoResumo): Promise<void> {
    try {
      const sessaoId = await this.store.estudar(caderno.id);
      await this.router.navigate(['/estudar'], { queryParams: { sessao: sessaoId } });
    } catch {
      this.store.erro.set('Não foi possível iniciar a sessão.');
    }
  }
  atualizado(data: string | null): string {
    return data ? new Intl.DateTimeFormat('pt-BR').format(new Date(data)) : 'Ainda não atualizado';
  }
}
