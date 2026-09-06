import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EditaisAgrupados,
  EditalAdminResumo,
  EditalDetalhe,
  RequisicaoEdital,
} from './modelos-edital';

@Injectable({ providedIn: 'root' })
export class EditalApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/editais';
  private readonly baseAdmin = '/api/v1/admin/editais';
  agrupados(): Observable<EditaisAgrupados> {
    return this.http.get<EditaisAgrupados>(this.base);
  }
  listarParaAdministracao(): Observable<EditalAdminResumo[]> {
    return this.http.get<EditalAdminResumo[]>(this.baseAdmin);
  }
  buscar(id: string): Observable<EditalDetalhe> {
    return this.http.get<EditalDetalhe>(`${this.baseAdmin}/${id}`);
  }
  criar(dados: RequisicaoEdital): Observable<EditalDetalhe> {
    return this.http.post<EditalDetalhe>(this.baseAdmin, dados);
  }
  atualizar(id: string, dados: RequisicaoEdital): Observable<EditalDetalhe> {
    return this.http.put<EditalDetalhe>(`${this.baseAdmin}/${id}`, dados);
  }
  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseAdmin}/${id}`);
  }
}
