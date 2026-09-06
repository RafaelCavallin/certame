import { Component, input, output } from '@angular/core';
import { PontosFracos } from '../../../core/api/modelos-desempenho';

@Component({
  selector: 'app-pontos-fracos-table',
  templateUrl: './pontos-fracos-table.html',
  styleUrl: './pontos-fracos-table.scss',
})
export class PontosFracosTable {
  readonly dados = input.required<PontosFracos>();
  readonly treinar = output<string>();
}
