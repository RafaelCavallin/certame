import { Component, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CadernoApi } from '../../../core/api/caderno-api';
import { CadernoResumo } from '../../../core/api/modelos-desempenho';

@Component({
  selector: 'app-adicionar-caderno-dialog',
  templateUrl: './adicionar-caderno-dialog.html',
  styleUrl: './adicionar-caderno-dialog.scss',
})
export class AdicionarCadernoDialog {
  private readonly api = inject(CadernoApi);
  readonly questaoId = input.required<string>();
  readonly fechado = output<void>();
  readonly adicionada = output<string[]>();
  readonly cadernos = signal<CadernoResumo[]>([]);
  readonly selecionados = signal<string[]>([]);
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  constructor() {
    void this.carregar();
  }
  private async carregar(): Promise<void> {
    try {
      const todos = await firstValueFrom(this.api.listar());
      this.cadernos.set(todos.filter((caderno) => caderno.tipo === 'MANUAL'));
    } catch {
      this.erro.set('Não foi possível carregar seus cadernos.');
    } finally {
      this.carregando.set(false);
    }
  }
  alternar(id: string): void {
    this.selecionados.update((atual) =>
      atual.includes(id) ? atual.filter((item) => item !== id) : [...atual, id],
    );
  }
  async adicionar(): Promise<void> {
    const cadernoIds = this.selecionados();
    if (!cadernoIds.length) return;
    this.erro.set(null);
    this.salvando.set(true);
    try {
      const cadernos = await Promise.all(
        cadernoIds.map((cadernoId) =>
          firstValueFrom(this.api.adicionarQuestoes(cadernoId, { questaoIds: [this.questaoId()] })),
        ),
      );
      this.adicionada.emit(cadernos.map((caderno) => caderno.nome));
    } catch {
      this.erro.set('Não foi possível adicionar a questão ao caderno.');
    } finally {
      this.salvando.set(false);
    }
  }
}
