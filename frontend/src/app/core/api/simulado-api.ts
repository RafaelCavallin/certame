import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EstadoSimulado,
  PaginaResposta,
  RequisicaoComposicao,
  RequisicaoSimulado,
  RespostaResultado,
  RespostaSimulados,
  RespostaTentativa,
  SimuladoDetalhe,
  SimuladoResumoAdmin,
} from '../models/modelos-simulado';

@Injectable({ providedIn: 'root' })
export class SimuladoApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin/simulados';
  listar(filtros: {
    q?: string;
    estado?: EstadoSimulado | '';
    bancaId?: string;
    page?: number;
    size?: number;
  }): Observable<PaginaResposta<SimuladoResumoAdmin>> {
    let params = new HttpParams();
    for (const [nome, valor] of Object.entries(filtros))
      if (valor !== undefined && valor !== '') params = params.set(nome, String(valor));
    return this.http.get<PaginaResposta<SimuladoResumoAdmin>>(this.base, { params });
  }
  listarDoEstudante(): Observable<RespostaSimulados> {
    return this.http.get<RespostaSimulados>('/api/v1/simulados');
  }
  obter(id: string): Observable<SimuladoDetalhe> {
    return this.http.get<SimuladoDetalhe>(`${this.base}/${id}`);
  }
  criar(dados: RequisicaoSimulado): Observable<SimuladoDetalhe> {
    return this.http.post<SimuladoDetalhe>(this.base, dados);
  }
  atualizar(id: string, dados: RequisicaoSimulado): Observable<SimuladoDetalhe> {
    return this.http.put<SimuladoDetalhe>(`${this.base}/${id}`, dados);
  }
  composicao(id: string, dados: RequisicaoComposicao): Observable<SimuladoDetalhe['composicao']> {
    return this.http.put<SimuladoDetalhe['composicao']>(`${this.base}/${id}/composicao`, dados);
  }
  publicar(id: string): Observable<SimuladoDetalhe> {
    return this.http.post<SimuladoDetalhe>(`${this.base}/${id}/publicar`, null);
  }
  despublicar(id: string): Observable<SimuladoDetalhe> {
    return this.http.post<SimuladoDetalhe>(`${this.base}/${id}/despublicar`, null);
  }
  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
  iniciarTentativa(id: string): Observable<RespostaTentativa> {
    return this.http.post<RespostaTentativa>(`/api/v1/simulados/${id}/tentativas`, null);
  }
  finalizarTentativa(
    simuladoId: string,
    tentativaId: string,
    dados: unknown,
  ): Observable<RespostaResultado> {
    return this.http.post<RespostaResultado>(
      `/api/v1/simulados/${simuladoId}/tentativas/${tentativaId}/finalizar`,
      dados,
    );
  }
  resultado(simuladoId: string, tentativaId: string): Observable<RespostaResultado> {
    return this.http.get<RespostaResultado>(
      `/api/v1/simulados/${simuladoId}/tentativas/${tentativaId}`,
    );
  }
}
