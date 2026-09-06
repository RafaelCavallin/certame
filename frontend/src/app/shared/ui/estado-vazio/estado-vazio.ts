import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-estado-vazio',
  templateUrl: './estado-vazio.html',
  styleUrl: './estado-vazio.scss',
})
export class EstadoVazio {
  readonly titulo = input.required<string>();
  readonly orientacao = input.required<string>();
  readonly acaoLabel = input<string | null>(null);
  readonly acao = output<void>();
}
