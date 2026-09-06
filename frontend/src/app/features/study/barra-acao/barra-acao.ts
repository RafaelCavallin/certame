import { Component, input, output } from '@angular/core';
@Component({
  selector: 'app-barra-acao',
  standalone: true,
  templateUrl: './barra-acao.html',
  styleUrl: './barra-acao.scss',
})
export class BarraAcao {
  readonly primeiro = input(false);
  readonly ultimo = input(false);
  readonly prova = input(false);
  readonly respondida = input(false);
  readonly anterior = output<void>();
  readonly proxima = output<void>();
  readonly confirmar = output<void>();
  readonly finalizar = output<void>();
}
