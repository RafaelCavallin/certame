import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { extrairErroApi } from '../../../core/api/erro-api';
import { FlashcardApi } from '../../../core/api/flashcard-api';
import { DeckResumo, decksComId } from '../../../core/api/modelos-flashcard';
import { FlashcardsStore } from '../flashcards-store';

@Injectable()
export class MoverParaDeck {
  private readonly api = inject(FlashcardApi);
  private readonly store = inject(FlashcardsStore);
  readonly destino = signal<string | null>(null);
  readonly decks = signal<DeckResumo[]>([]);
  readonly erro = signal<string | null>(null);
  readonly desabilitado = computed(() => this.store.selecionados().length === 0 || !this.destino());

  constructor() {
    void this.carregarDecks();
  }

  async mover(): Promise<void> {
    const deckId = this.destino();
    if (this.desabilitado() || !deckId) return;
    try {
      await firstValueFrom(
        this.api.moverParaDeck(deckId, { flashcardIds: this.store.selecionados() }),
      );
      this.destino.set(null);
      this.erro.set(null);
      this.store.selecionados.set([]);
      await this.store.carregar();
    } catch (erro) {
      this.erro.set(extrairErroApi(this.corpo(erro)).message);
    }
  }

  private async carregarDecks(): Promise<void> {
    try {
      this.decks.set(decksComId(await firstValueFrom(this.api.listarDecks())));
    } catch {
      this.decks.set([]);
    }
  }

  private corpo(erro: unknown): unknown {
    return erro && typeof erro === 'object' && 'error' in erro
      ? (erro as { error: unknown }).error
      : erro;
  }
}
