import { Component, input, output } from '@angular/core';
import { RespostaCorrecao } from '../../../core/api/modelos-estudo';
@Component({
  selector: 'app-correcao-prova',
  standalone: true,
  templateUrl: './correcao-prova.html',
  styleUrl: './correcao-prova.scss',
})
export class CorrecaoProva {
  readonly correcao = input.required<RespostaCorrecao>();
  readonly refazer = output<void>();
  readonly inicio = output<void>();
}
