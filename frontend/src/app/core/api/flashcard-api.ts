import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  DeckDetalhe,
  DeckResumo,
  FiltroFlashcards,
  FlashcardResumo,
  PaginaFlashcards,
  RequisicaoDeck,
  RequisicaoFlashcard,
  RequisicaoMoverCards,
  RespostaExclusaoDeck,
  RespostaFlashcardCriado,
  RespostaMoverCards,
} from './modelos-flashcard';

@Injectable({ providedIn: 'root' })
export class FlashcardApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/flashcards';

  criar(dados: RequisicaoFlashcard): Observable<RespostaFlashcardCriado> {
    return this.http.post<RespostaFlashcardCriado>(this.base, dados);
  }

  listar(filtro: FiltroFlashcards): Observable<PaginaFlashcards> {
    let params = new HttpParams();
    for (const [nome, valor] of Object.entries(filtro))
      if (valor !== null && valor !== undefined && valor !== '')
        params = params.set(nome, String(valor));
    return this.http.get<PaginaFlashcards>(this.base, { params });
  }

  buscar(id: string): Observable<FlashcardResumo> {
    return this.http.get<FlashcardResumo>(`${this.base}/${id}`);
  }
  atualizar(id: string, dados: RequisicaoFlashcard): Observable<FlashcardResumo> {
    return this.http.put<FlashcardResumo>(`${this.base}/${id}`, dados);
  }
  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
  arquivar(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/arquivar`, null);
  }
  restaurar(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/restaurar`, null);
  }
  listarDecks(): Observable<DeckDetalhe[]> {
    return this.http.get<DeckDetalhe[]>(`${this.base}/decks`);
  }
  criarDeck(dados: RequisicaoDeck): Observable<DeckResumo> {
    return this.http.post<DeckResumo>(`${this.base}/decks`, dados);
  }
  atualizarDeck(id: string, dados: RequisicaoDeck): Observable<DeckDetalhe> {
    return this.http.put<DeckDetalhe>(`${this.base}/decks/${id}`, dados);
  }
  excluirDeck(id: string): Observable<RespostaExclusaoDeck> {
    return this.http.delete<RespostaExclusaoDeck>(`${this.base}/decks/${id}`);
  }
  moverParaDeck(deckId: string, dados: RequisicaoMoverCards): Observable<RespostaMoverCards> {
    return this.http.post<RespostaMoverCards>(`${this.base}/decks/${deckId}/mover`, dados);
  }
}
