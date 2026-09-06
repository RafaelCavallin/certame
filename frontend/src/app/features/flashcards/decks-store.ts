import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { extrairErroApi } from '../../core/api/erro-api';
import { FlashcardApi } from '../../core/api/flashcard-api';
import { DeckDetalhe, RequisicaoDeck } from '../../core/api/modelos-flashcard';

type ErroCampoDeck = Record<'nome', string | null>;

@Injectable({ providedIn: 'root' })
export class DecksStore {
  private readonly api = inject(FlashcardApi);
  private readonly lista = signal<DeckDetalhe[]>([]);
  readonly decks = this.lista.asReadonly();
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly erroDeCampo = signal<ErroCampoDeck>({ nome: null });

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      this.lista.set(await firstValueFrom(this.api.listarDecks()));
    } finally {
      this.carregando.set(false);
    }
  }

  async salvar(deck: RequisicaoDeck, id?: string): Promise<void> {
    this.salvando.set(true);
    try {
      if (id) await this.atualizarExistente(id, deck);
      else await this.criarNovo(deck);
      this.erroDeCampo.set({ nome: null });
    } catch (erro) {
      this.erroDeCampo.set({
        nome: extrairErroApi(this.corpo(erro)).fieldErrors?.['nome'] ?? null,
      });
    } finally {
      this.salvando.set(false);
    }
  }

  async excluir(id: string): Promise<number> {
    const resposta = await firstValueFrom(this.api.excluirDeck(id));
    await this.carregar();
    return resposta.cardsMovidos;
  }

  private async atualizarExistente(id: string, deck: RequisicaoDeck): Promise<void> {
    const atualizado = await firstValueFrom(this.api.atualizarDeck(id, deck));
    this.lista.set(this.lista().map((item) => (item.id === atualizado.id ? atualizado : item)));
  }

  private async criarNovo(deck: RequisicaoDeck): Promise<void> {
    await firstValueFrom(this.api.criarDeck(deck));
    await this.carregar();
  }

  private corpo(erro: unknown): unknown {
    return erro && typeof erro === 'object' && 'error' in erro
      ? (erro as { error: unknown }).error
      : erro;
  }
}
