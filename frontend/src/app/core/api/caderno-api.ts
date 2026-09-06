import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CadernoResumo,
  PaginaResposta,
  QuestaoCadernoResumo,
  RequisicaoCaderno,
  RequisicaoQuestoesCaderno,
} from './modelos-desempenho';
import { QuestaoResumo } from './modelos-questao';

@Injectable({ providedIn: 'root' })
export class CadernoApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/cadernos';
  listar(): Observable<CadernoResumo[]> {
    return this.http.get<CadernoResumo[]>(this.base);
  }
  criar(dados: RequisicaoCaderno): Observable<CadernoResumo> {
    return this.http.post<CadernoResumo>(this.base, dados);
  }
  atualizar(id: string, dados: RequisicaoCaderno): Observable<CadernoResumo> {
    return this.http.put<CadernoResumo>(`${this.base}/${id}`, dados);
  }
  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
  listarQuestoes(
    id: string,
    page = 0,
    size = 20,
  ): Observable<PaginaResposta<QuestaoCadernoResumo>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PaginaResposta<QuestaoCadernoResumo>>(`${this.base}/${id}/questoes`, {
      params,
    });
  }
  adicionarQuestoes(id: string, dados: RequisicaoQuestoesCaderno): Observable<CadernoResumo> {
    return this.http.post<CadernoResumo>(`${this.base}/${id}/questoes`, dados);
  }
  removerQuestao(id: string, questaoId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}/questoes/${questaoId}`);
  }
  listarQuestoesDisponiveis(): Observable<PaginaResposta<QuestaoResumo>> {
    return this.http.get<PaginaResposta<QuestaoResumo>>('/api/v1/questoes');
  }
}
