import { Component, input } from '@angular/core';
import { CardRevisao as CardRevisaoModelo } from '../../../../core/api/modelos-revisao';

@Component({
  selector: 'app-card-revisao',
  templateUrl: './card-revisao.html',
  styleUrl: './card-revisao.scss',
})
export class CardRevisao {
  readonly card = input.required<CardRevisaoModelo>();
  readonly revelado = input.required<boolean>();
}
