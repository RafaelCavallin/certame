import { Component, input } from '@angular/core';
import { RespostaRegistrada } from '../../../core/api/modelos-estudo';
@Component({
  selector: 'app-gabarito-painel',
  standalone: true,
  templateUrl: './gabarito-painel.html',
  styleUrl: './gabarito-painel.scss',
})
export class GabaritoPainel {
  readonly resposta = input.required<RespostaRegistrada>();
}
