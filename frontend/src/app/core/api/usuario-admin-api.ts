import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  FiltroUsuarios,
  PaginaResposta,
  RequisicaoCriarUsuario,
  RequisicaoEditarUsuario,
  UsuarioGestaoResumo,
} from '../models/modelos-usuario';

@Injectable({ providedIn: 'root' })
export class UsuarioAdminApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/super/usuarios';

  listar(filtro: FiltroUsuarios): Observable<PaginaResposta<UsuarioGestaoResumo>> {
    let params = new HttpParams();
    for (const [nome, valor] of Object.entries(filtro)) {
      if (valor !== null && valor !== undefined && valor !== '')
        params = params.set(nome, String(valor));
    }
    return this.http.get<PaginaResposta<UsuarioGestaoResumo>>(this.base, { params });
  }

  criar(dados: RequisicaoCriarUsuario): Observable<UsuarioGestaoResumo> {
    return this.http.post<UsuarioGestaoResumo>(this.base, dados);
  }

  editar(id: string, dados: RequisicaoEditarUsuario): Observable<UsuarioGestaoResumo> {
    return this.http.patch<UsuarioGestaoResumo>(`${this.base}/${id}`, dados);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  redefinirSenha(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/redefinir-senha`, null);
  }

  reenviarConvite(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/reenviar-convite`, null);
  }
}
