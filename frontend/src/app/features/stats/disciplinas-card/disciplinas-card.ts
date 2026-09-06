import { Component, input } from '@angular/core';
import { DesempenhoDisciplinaResumo } from '../../../core/api/modelos-desempenho';

@Component({
  selector: 'app-disciplinas-card',
  templateUrl: './disciplinas-card.html',
  styleUrl: './disciplinas-card.scss',
})
export class DisciplinasCard {
  readonly disciplinas = input.required<DesempenhoDisciplinaResumo[]>();
}
