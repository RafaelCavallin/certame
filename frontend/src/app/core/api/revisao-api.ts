import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  RequisicaoRevisao,
  RequisicaoSessaoRevisao,
  RespostaRevisao,
  RespostaSessaoRevisao,
  ResumoFlashcards,
  ResumoSessao,
} from './modelos-revisao';

@Injectable({ providedIn: 'root' })
export class RevisaoApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/flashcards';

  abrir(dados: RequisicaoSessaoRevisao): Observable<RespostaSessaoRevisao> {
    return this.http.post<RespostaSessaoRevisao>(`${this.base}/sessoes`, dados);
  }

  registrar(sessaoId: string, dados: RequisicaoRevisao): Observable<RespostaRevisao> {
    return this.http.post<RespostaRevisao>(`${this.base}/sessoes/${sessaoId}/revisoes`, dados);
  }

  finalizar(sessaoId: string): Observable<ResumoSessao> {
    return this.http.post<ResumoSessao>(`${this.base}/sessoes/${sessaoId}/finalizar`, null);
  }

  resumo(): Observable<ResumoFlashcards> {
    return this.http.get<ResumoFlashcards>(`${this.base}/resumo`);
  }
}
