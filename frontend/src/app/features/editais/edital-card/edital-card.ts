import { Component, input } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { EditalResumo, StatusEdital } from '../../../core/api/modelos-edital';

const ROTULOS_STATUS: Record<StatusEdital, string> = {
  ABERTO: 'Aberto',
  PREVISTO: 'Previsto',
  ENCERRADO: 'Encerrado',
};
const CLASSES_STATUS: Record<StatusEdital, string> = {
  ABERTO: 'tag-accent',
  PREVISTO: 'tag-accent-2',
  ENCERRADO: 'tag-encerrado',
};

@Component({
  selector: 'app-edital-card',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './edital-card.html',
  styleUrl: './edital-card.scss',
})
export class EditalCard {
  readonly edital = input.required<EditalResumo>();
  readonly selecionado = input(false);
  protected rotuloStatus(): string {
    return ROTULOS_STATUS[this.edital().status];
  }
  protected classeStatus(): string {
    return `tag ${CLASSES_STATUS[this.edital().status]}`;
  }
}
