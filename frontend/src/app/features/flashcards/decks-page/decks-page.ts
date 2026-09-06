import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DeckDetalhe } from '../../../core/api/modelos-flashcard';
import { ConfirmacaoDialog } from '../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
import { DialogService } from '../../../shared/ui/dialog/dialog-service';
import { DeckCard } from '../deck-card/deck-card';
import { DeckDialog } from '../deck-dialog/deck-dialog';
import { DecksStore } from '../decks-store';

@Component({
  selector: 'app-decks-page',
  imports: [RouterLink, DeckCard, ConfirmacaoDialog],
  templateUrl: './decks-page.html',
  styleUrl: './decks-page.scss',
})
export class DecksPage {
  private readonly dialogService = inject(DialogService);
  private readonly router = inject(Router);
  readonly store = inject(DecksStore);
  readonly excluindo = signal<DeckDetalhe | null>(null);
  readonly estatisticasLabel = computed(() => {
    const decks = this.store.decks();
    const totalCards = decks.reduce((soma, deck) => soma + deck.totalCards, 0);
    return `${decks.length} decks · ${totalCards} cards`;
  });

  constructor() {
    void this.store.carregar();
  }

  abrirNovo(): void {
    this.dialogService
      .abrir(DeckDialog, { contexto: null })
      .subscribe(() => void this.store.carregar());
  }

  editar(deck: DeckDetalhe): void {
    this.dialogService
      .abrir(DeckDialog, { contexto: deck })
      .subscribe(() => void this.store.carregar());
  }

  pedirExclusao(deck: DeckDetalhe): void {
    this.excluindo.set(deck);
  }

  confirmarExclusao(): void {
    const deck = this.excluindo();
    this.excluindo.set(null);
    if (deck?.id) void this.store.excluir(deck.id);
  }

  estudar(deck: DeckDetalhe): void {
    void this.router.navigate(['/flashcards/estudar'], {
      queryParams: { deck: deck.id ?? 'sem-deck' },
    });
  }
}
