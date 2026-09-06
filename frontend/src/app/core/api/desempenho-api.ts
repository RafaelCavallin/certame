import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  DesempenhoDisciplinaResumo,
  EvolucaoSemanal,
  PontosFracos,
  ResumoDesempenho,
} from './modelos-desempenho';

@Injectable({ providedIn: 'root' })
export class DesempenhoApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/desempenho';

  resumo(): Observable<ResumoDesempenho> {
    return this.http.get<ResumoDesempenho>(`${this.base}/resumo`);
  }
  evolucao(): Observable<EvolucaoSemanal> {
    return this.http.get<EvolucaoSemanal>(`${this.base}/evolucao`);
  }
  disciplinas(): Observable<DesempenhoDisciplinaResumo[]> {
    return this.http.get<DesempenhoDisciplinaResumo[]>(`${this.base}/disciplinas`);
  }
  pontosFracos(): Observable<PontosFracos> {
    return this.http.get<PontosFracos>(`${this.base}/pontos-fracos`);
  }
}
