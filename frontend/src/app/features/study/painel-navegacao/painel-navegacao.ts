import { Component, computed, input, output } from '@angular/core';
import { ModoSessao } from '../../../core/api/modelos-estudo';

@Component({
  selector: 'app-painel-navegacao',
  standalone: true,
  templateUrl: './painel-navegacao.html',
  styleUrl: './painel-navegacao.scss',
})
export class PainelNavegacao {
  readonly total = input.required<number>();
  readonly atual = input.required<number>();
  readonly respondidas = input.required<Set<number>>();
  readonly modo = input<ModoSessao>('FOCO');
  readonly emContexto = computed(() => this.modo() === 'CONTEXTO');
  readonly selecionar = output<number>();
  numeros(): number[] {
    return Array.from({ length: this.total() }, (_, i) => i);
  }
}
