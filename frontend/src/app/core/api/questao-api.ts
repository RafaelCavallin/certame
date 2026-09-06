import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  PaginaResposta,
  QuestaoDetalhe,
  QuestaoResumo,
  RequisicaoQuestao,
} from './modelos-questao';

@Injectable({ providedIn: 'root' })
export class QuestaoApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin/questoes';
  listar(
    filtros: Record<string, string | number | null | undefined>,
  ): Observable<PaginaResposta<QuestaoResumo>> {
    let params = new HttpParams();
    for (const [nome, valor] of Object.entries(filtros))
      if (valor !== null && valor !== undefined && valor !== '')
        params = params.set(nome, String(valor));
    return this.http.get<PaginaResposta<QuestaoResumo>>(this.base, { params });
  }
  obter(id: string): Observable<QuestaoDetalhe> {
    return this.http.get<QuestaoDetalhe>(`${this.base}/${id}`);
  }
  criar(dados: RequisicaoQuestao): Observable<QuestaoDetalhe> {
    return this.http.post<QuestaoDetalhe>(this.base, dados);
  }
  atualizar(id: string, dados: RequisicaoQuestao): Observable<QuestaoDetalhe> {
    return this.http.put<QuestaoDetalhe>(`${this.base}/${id}`, dados);
  }
  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
  enviarParaRevisao(id: string): Observable<QuestaoDetalhe> {
    return this.http.post<QuestaoDetalhe>(`${this.base}/${id}/enviar-revisao`, null);
  }
  publicar(id: string): Observable<QuestaoDetalhe> {
    return this.http.post<QuestaoDetalhe>(`${this.base}/${id}/publicar`, null);
  }
  despublicar(id: string): Observable<QuestaoDetalhe> {
    return this.http.post<QuestaoDetalhe>(`${this.base}/${id}/despublicar`, null);
  }
  contarDisponiveis(
    disciplinaId: string,
    dificuldade: string | null,
  ): Observable<{ disponiveis: number }> {
    let params = new HttpParams().set('disciplinaId', disciplinaId);
    if (dificuldade) params = params.set('dificuldade', dificuldade);
    return this.http.get<{ disponiveis: number }>(`${this.base}/contagem`, { params });
  }
}
