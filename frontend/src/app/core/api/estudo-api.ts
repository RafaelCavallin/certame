import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  MarcacoesQuestao,
  RequisicaoFinalizacao,
  RequisicaoResposta,
  RequisicaoSessao,
  RespostaCorrecao,
  RespostaRegistrada,
  RespostaSessao,
  ResumoInicio,
} from './modelos-estudo';

@Injectable({ providedIn: 'root' })
export class EstudoApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/estudo';
  inicio(): Observable<ResumoInicio> {
    return this.http.get<ResumoInicio>(`${this.base}/inicio`);
  }
  abrir(dados: RequisicaoSessao): Observable<RespostaSessao> {
    return this.http.post<RespostaSessao>(`${this.base}/sessoes`, dados);
  }
  retomar(id: string): Observable<RespostaSessao> {
    return this.http.get<RespostaSessao>(`${this.base}/sessoes/${id}`);
  }
  pausar(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/sessoes/${id}/pausar`, {});
  }
  retomarSessao(id: string): Observable<RespostaSessao> {
    return this.http.post<RespostaSessao>(`${this.base}/sessoes/${id}/retomar`, {});
  }
  trocarModo(id: string, modo: string): Observable<RespostaSessao> {
    return this.http.patch<RespostaSessao>(`${this.base}/sessoes/${id}/modo`, { modo });
  }
  responder(id: string, dados: RequisicaoResposta): Observable<RespostaRegistrada> {
    return this.http.post<RespostaRegistrada>(`${this.base}/sessoes/${id}/respostas`, dados);
  }
  finalizar(id: string, dados: RequisicaoFinalizacao): Observable<RespostaCorrecao> {
    return this.http.post<RespostaCorrecao>(`${this.base}/sessoes/${id}/finalizar`, dados);
  }
  marcar(questaoId: string, dados: MarcacoesQuestao): Observable<MarcacoesQuestao> {
    return this.http.put<MarcacoesQuestao>(`${this.base}/questoes/${questaoId}/marcacoes`, dados);
  }
}
