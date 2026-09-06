import { Component, computed, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { extrairErroApi } from '../../../core/api/erro-api';
import { FlashcardApi } from '../../../core/api/flashcard-api';
import { DeckResumo, decksComId } from '../../../core/api/modelos-flashcard';

const OPCAO_SEM_DECK = 'sem-deck';
const OPCAO_NOVO_DECK = '__novo_deck__';
const TAMANHO_MINIMO_NOME = 3;

@Component({
  selector: 'app-deck-seletor',
  templateUrl: './deck-seletor.html',
  styleUrl: './deck-seletor.scss',
})
export class DeckSeletor {
  private readonly api = inject(FlashcardApi);

  readonly valor = input<string | null>(null);
  readonly selecionado = output<string | null>();

  protected readonly opcaoSemDeck = OPCAO_SEM_DECK;
  protected readonly opcaoNovoDeck = OPCAO_NOVO_DECK;
  protected readonly decks = signal<DeckResumo[]>([]);
  protected readonly criandoNovo = signal(false);
  protected readonly nomeNovoDeck = signal('');
  protected readonly erro = signal<string | null>(null);
  protected readonly valorSelect = computed(() => this.valor() ?? OPCAO_SEM_DECK);

  constructor() {
    this.carregarDecks();
  }

  protected aoSelecionar(valorBruto: string): void {
    if (valorBruto === OPCAO_NOVO_DECK) {
      this.criandoNovo.set(true);
      this.nomeNovoDeck.set('');
      this.erro.set(null);
      return;
    }
    this.cancelarNovoDeck();
    this.selecionado.emit(valorBruto === OPCAO_SEM_DECK ? null : valorBruto);
  }

  protected async confirmarNovoDeck(): Promise<void> {
    const nome = this.nomeNovoDeck().trim();
    if (nome.length < TAMANHO_MINIMO_NOME) return;
    try {
      const deck = await firstValueFrom(this.api.criarDeck({ nome }));
      this.decks.update((atuais) => [...atuais, deck]);
      this.selecionado.emit(deck.id);
      this.cancelarNovoDeck();
    } catch (erro) {
      this.erro.set(extrairErroApi(this.corpo(erro)).message);
    }
  }

  protected cancelarNovoDeck(): void {
    this.criandoNovo.set(false);
    this.nomeNovoDeck.set('');
    this.erro.set(null);
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
