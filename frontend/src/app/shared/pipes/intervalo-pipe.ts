import { Pipe, PipeTransform } from '@angular/core';

const MINUTOS_POR_HORA = 60;
const MINUTOS_POR_DIA = 1440;

@Pipe({ name: 'intervalo' })
export class IntervaloPipe implements PipeTransform {
  transform(minutos: number): string {
    if (minutos < MINUTOS_POR_HORA) return `${minutos} min`;
    if (minutos < MINUTOS_POR_DIA) return `${Math.round(minutos / MINUTOS_POR_HORA)} h`;
    const dias = Math.round(minutos / MINUTOS_POR_DIA);
    return dias === 1 ? '1 dia' : `${dias} dias`;
  }
}
