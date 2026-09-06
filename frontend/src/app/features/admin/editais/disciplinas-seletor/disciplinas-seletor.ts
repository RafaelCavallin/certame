import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DisciplinaExigida } from '../../../../core/api/modelos-edital';

@Component({
  selector: 'app-disciplinas-seletor',
  imports: [FormsModule],
  templateUrl: './disciplinas-seletor.html',
  styleUrl: './disciplinas-seletor.scss',
})
export class DisciplinasSeletor {
  readonly disciplinas = input.required<DisciplinaExigida[]>();
  readonly selecionados = input.required<string[]>();
  readonly alterado = output<string[]>();

  protected readonly filtro = signal('');
  protected readonly filtradas = computed(() => {
    const termo = this.filtro().trim().toLowerCase();
    return termo
      ? this.disciplinas().filter((d) => d.nome.toLowerCase().includes(termo))
      : this.disciplinas();
  });

  protected nomeDe(id: string): string {
    return this.disciplinas().find((d) => d.id === id)?.nome ?? id;
  }

  protected alternar(id: string): void {
    if (!this.disciplinas().some((d) => d.id === id)) return;
    const atual = this.selecionados();
    this.alterado.emit(atual.includes(id) ? atual.filter((x) => x !== id) : [...atual, id]);
  }

  protected remover(id: string): void {
    this.alterado.emit(this.selecionados().filter((x) => x !== id));
  }
}
