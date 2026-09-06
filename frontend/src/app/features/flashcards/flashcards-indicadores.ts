import { computed, inject, Injectable } from '@angular/core';
import { FlashcardsStore } from './flashcards-store';

export interface CartaoIndicador {
  label: string;
  valor: number;
  nota: string;
  cor: string;
}

@Injectable()
export class FlashcardsIndicadores {
  private readonly store = inject(FlashcardsStore);

  readonly cartoes = computed<CartaoIndicador[]>(() => {
    const i = this.store.indicadores();
    return [
      {
        label: 'Vencendo hoje',
        valor: i.vencendoHoje,
        nota: 'na fila de revisão',
        cor: 'var(--color-accent-300)',
      },
      {
        label: 'Próximos 7 dias',
        valor: i.proximosSeteDias,
        nota: 'agendados',
        cor: 'var(--color-text)',
      },
      {
        label: 'Em fixação',
        valor: i.emFixacao,
        nota: 'novos, aprendendo e reaprendendo',
        cor: 'var(--color-text)',
      },
      {
        label: 'Total de cards',
        valor: i.total,
        nota: `em ${i.decks} decks`,
        cor: 'var(--color-text)',
      },
    ];
  });
}
