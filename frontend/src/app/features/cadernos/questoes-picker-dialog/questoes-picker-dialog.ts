import { Component, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CadernoApi } from '../../../core/api/caderno-api';
import { QuestaoResumo } from '../../../core/api/modelos-questao';

@Component({
  selector: 'app-questoes-picker-dialog',
  templateUrl: './questoes-picker-dialog.html',
  styleUrl: './questoes-picker-dialog.scss',
})
export class QuestoesPickerDialog {
  private readonly api = inject(CadernoApi);
  readonly caderno = input.required<{ id: string; nome: string }>();
  readonly fechado = output<void>();
  readonly selecionadas = output<string[]>();
  readonly questoes = signal<QuestaoResumo[]>([]);
  readonly marcadas = signal(new Set<string>());
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  constructor() {
    void this.carregar();
  }
  private async carregar(): Promise<void> {
    try {
      this.questoes.set((await firstValueFrom(this.api.listarQuestoesDisponiveis())).content);
    } catch {
      this.erro.set('Não foi possível carregar as questões.');
    } finally {
      this.carregando.set(false);
    }
  }
  alternar(id: string): void {
    const marcadas = new Set(this.marcadas());
    marcadas.has(id) ? marcadas.delete(id) : marcadas.add(id);
    this.marcadas.set(marcadas);
  }
  adicionar(): void {
    this.selecionadas.emit([...this.marcadas()]);
  }
}
