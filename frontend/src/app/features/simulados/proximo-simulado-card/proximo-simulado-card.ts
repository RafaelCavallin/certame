import { Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { SimuladoResumo } from '../../../core/models/modelos-simulado';

@Component({
  selector: 'app-proximo-simulado-card',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './proximo-simulado-card.html',
  styleUrl: './proximo-simulado-card.scss',
})
export class ProximoSimuladoCard {
  readonly simulado = input.required<SimuladoResumo>();
  restante(): string {
    const segundos = Math.max(
      0,
      Math.floor((new Date(this.simulado().janelaAbertura).getTime() - Date.now()) / 1000),
    );
    const dias = Math.floor(segundos / 86400);
    const horas = Math.floor((segundos % 86400) / 3600);
    return dias ? `${dias}d ${String(horas).padStart(2, '0')}h` : `${horas}h`;
  }
  duracao(segundos: number): string {
    return segundos >= 3600
      ? `${Math.floor(segundos / 3600)}h`
      : `${Math.floor(segundos / 60)} min`;
  }
}
