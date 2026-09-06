import { Component, output } from '@angular/core';

@Component({
  selector: 'app-fila-vazia',
  templateUrl: './fila-vazia.html',
  styleUrl: './fila-vazia.scss',
})
export class FilaVazia {
  readonly voltar = output<void>();
}
