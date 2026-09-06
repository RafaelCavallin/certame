import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { EditalApi } from '../../core/api/edital-api';
import { EditaisAgrupados, EditalResumo } from '../../core/api/modelos-edital';

@Injectable({ providedIn: 'root' })
export class EditaisStore {
  private readonly api = inject(EditalApi);
  private readonly dados = signal<EditaisAgrupados | null>(null);
  private readonly selecionadoId = signal<string | null>(null);
  readonly agrupados = computed(() => this.dados());
  readonly carregando = signal(false);
  readonly vazio = computed(() => {
    const grupos = this.dados();
    return (
      !!grupos &&
      grupos.abertos.length === 0 &&
      grupos.previstos.length === 0 &&
      grupos.encerrados.length === 0
    );
  });
  readonly selecionado = computed(
    () => this.todos().find((edital) => edital.id === this.selecionadoId()) ?? null,
  );

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      this.dados.set(await firstValueFrom(this.api.agrupados()));
    } finally {
      this.carregando.set(false);
    }
  }

  selecionar(id: string): void {
    this.selecionadoId.set(id);
  }

  private todos(): EditalResumo[] {
    const grupos = this.dados();
    return grupos ? [...grupos.abertos, ...grupos.previstos, ...grupos.encerrados] : [];
  }
}
