import { Component, input, output } from '@angular/core';
import { ModoSessao } from '../../../core/api/modelos-estudo';
@Component({ selector: 'app-seletor-modo', standalone: true, templateUrl: './seletor-modo.html' })
export class SeletorModo {
  readonly modo = input.required<ModoSessao>();
  readonly podeTrocar = input(true);
  readonly trocar = output<ModoSessao>();
  selecionar(modo: ModoSessao): void {
    if (this.podeTrocar() || modo === this.modo()) this.trocar.emit(modo);
  }
}
