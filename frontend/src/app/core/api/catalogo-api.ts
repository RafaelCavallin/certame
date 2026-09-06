import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AssuntoArvore,
  DisciplinaArvore,
  ItemCatalogo,
  RequisicaoItemCatalogo,
  RequisicaoNoTaxonomia,
} from './modelos-questao';
@Injectable({ providedIn: 'root' })
export class CatalogoApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin';
  disciplinas(incluirArquivados = false): Observable<DisciplinaArvore[]> {
    return this.http.get<DisciplinaArvore[]>(`${this.base}/disciplinas`, {
      params: new HttpParams().set('incluirArquivados', incluirArquivados),
    });
  }
  criarDisciplina(d: RequisicaoNoTaxonomia): Observable<DisciplinaArvore> {
    return this.http.post<DisciplinaArvore>(`${this.base}/disciplinas`, d);
  }
  renomearDisciplina(id: string, d: RequisicaoNoTaxonomia): Observable<DisciplinaArvore> {
    return this.http.put<DisciplinaArvore>(`${this.base}/disciplinas/${id}`, d);
  }
  excluirDisciplina(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/disciplinas/${id}`);
  }
  criarAssunto(d: RequisicaoNoTaxonomia): Observable<AssuntoArvore> {
    return this.http.post<AssuntoArvore>(`${this.base}/assuntos`, d);
  }
  renomearAssunto(id: string, d: RequisicaoNoTaxonomia): Observable<AssuntoArvore> {
    return this.http.put<AssuntoArvore>(`${this.base}/assuntos/${id}`, d);
  }
  excluirAssunto(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/assuntos/${id}`);
  }
  arquivarAssunto(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/assuntos/${id}/arquivar`, null);
  }
  bancas(): Observable<ItemCatalogo[]> {
    return this.http.get<ItemCatalogo[]>(`${this.base}/bancas`);
  }
  orgaos(): Observable<ItemCatalogo[]> {
    return this.http.get<ItemCatalogo[]>(`${this.base}/orgaos`);
  }
  criarBanca(d: RequisicaoItemCatalogo): Observable<ItemCatalogo> {
    return this.http.post<ItemCatalogo>(`${this.base}/bancas`, d);
  }
  atualizarBanca(id: string, d: RequisicaoItemCatalogo): Observable<ItemCatalogo> {
    return this.http.put<ItemCatalogo>(`${this.base}/bancas/${id}`, d);
  }
  excluirBanca(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/bancas/${id}`);
  }
  criarOrgao(d: RequisicaoItemCatalogo): Observable<ItemCatalogo> {
    return this.http.post<ItemCatalogo>(`${this.base}/orgaos`, d);
  }
  atualizarOrgao(id: string, d: RequisicaoItemCatalogo): Observable<ItemCatalogo> {
    return this.http.put<ItemCatalogo>(`${this.base}/orgaos/${id}`, d);
  }
  excluirOrgao(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/orgaos/${id}`);
  }
}
