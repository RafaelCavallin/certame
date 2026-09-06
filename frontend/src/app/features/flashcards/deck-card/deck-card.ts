import { Component, computed, input, output } from '@angular/core';
import { DeckDetalhe } from '../../../core/api/modelos-flashcard';

@Component({
  selector: 'app-deck-card',
  templateUrl: './deck-card.html',
  styleUrl: './deck-card.scss',
})
export class DeckCard {
  readonly deck = input.required<DeckDetalhe>();
  readonly editar = output<DeckDetalhe>();
  readonly excluir = output<DeckDetalhe>();
  readonly estudar = output<DeckDetalhe>();

  protected readonly corVencendo = computed(() =>
    this.deck().vencendoHoje > 0 ? 'var(--color-accent-300)' : 'var(--color-neutral-400)',
  );
}
