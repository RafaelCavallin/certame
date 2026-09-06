import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { SimuladoConcluido, SimuladoResumo } from '../../../core/models/modelos-simulado';

@Component({
  selector: 'app-simulado-card',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './simulado-card.html',
  styleUrl: './simulado-card.scss',
})
export class SimuladoCard {
  readonly simulado = input.required<SimuladoResumo | SimuladoConcluido>();
  readonly iniciar = output<void>();
  readonly revisar = output<void>();
  concluido(): boolean {
    return 'nota' in this.simulado();
  }
  historico(): SimuladoConcluido {
    return this.simulado() as SimuladoConcluido;
  }
  resumo(): SimuladoResumo {
    return this.simulado() as SimuladoResumo;
  }
  duracao(segundos: number): string {
    const horas = Math.floor(segundos / 3600);
    return horas ? `${horas}h` : `${Math.floor(segundos / 60)} min`;
  }
}
