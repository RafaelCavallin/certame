import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { UsuarioAdminApi } from '../../core/api/usuario-admin-api';
import { corpoErro, ErroApi, extrairErroApi } from '../../core/api/erro-api';
import { PerfilUsuario } from '../../core/auth/modelos';
import {
  FiltroUsuarios,
  OrdenacaoUsuarios,
  PaginaResposta,
  StatusUsuario,
  UsuarioGestaoResumo,
} from '../../core/models/modelos-usuario';
import { ToastStore } from '../../shared/ui/toast/toast-store';

const TAMANHO_PAGINA = 20;

@Injectable({ providedIn: 'root' })
export class UsuariosListaStore {
  private readonly api = inject(UsuarioAdminApi);
  private readonly toast = inject(ToastStore);

  readonly pagina = signal<PaginaResposta<UsuarioGestaoResumo>>({
    content: [],
    page: 0,
    size: TAMANHO_PAGINA,
    totalElements: 0,
  });
  readonly carregandoLista = signal(false);
  readonly erroLista = signal<string | null>(null);
  readonly filtroPerfil = signal<PerfilUsuario | ''>('');
  readonly filtroStatus = signal<StatusUsuario | ''>('');
  readonly ordenacao = signal<OrdenacaoUsuarios>('criadoEm,desc');

  async carregarLista(): Promise<void> {
    this.carregandoLista.set(true);
    this.erroLista.set(null);
    try {
      this.pagina.set(await firstValueFrom(this.api.listar(this.filtro())));
    } catch (e) {
      this.erroLista.set(extrairErroApi(corpoErro(e)).message);
    } finally {
      this.carregandoLista.set(false);
    }
  }

  aplicarFiltros(): void {
    this.irParaPagina(0);
  }

  alternarOrdenacao(): void {
    this.ordenacao.set(this.ordenacao() === 'criadoEm,desc' ? 'criadoEm,asc' : 'criadoEm,desc');
    this.aplicarFiltros();
  }

  irParaPagina(pagina: number): void {
    this.pagina.update((p) => ({ ...p, page: pagina }));
    void this.carregarLista();
  }

  async excluir(id: string): Promise<ErroApi | null> {
    try {
      await firstValueFrom(this.api.excluir(id));
      await this.carregarLista();
      return null;
    } catch (e) {
      return extrairErroApi(corpoErro(e));
    }
  }

  async redefinirSenha(usuario: UsuarioGestaoResumo): Promise<void> {
    try {
      await firstValueFrom(this.api.redefinirSenha(usuario.id));
      this.toast.publicar(`Link de redefinição de senha enviado para ${usuario.email}.`);
    } catch (e) {
      this.toast.publicar(extrairErroApi(corpoErro(e)).message);
    }
  }

  async reenviarConvite(usuario: UsuarioGestaoResumo): Promise<void> {
    try {
      await firstValueFrom(this.api.reenviarConvite(usuario.id));
      this.toast.publicar(`Convite reenviado para ${usuario.email}.`);
    } catch (e) {
      this.toast.publicar(extrairErroApi(corpoErro(e)).message);
    }
  }

  private filtro(): FiltroUsuarios {
    const p = this.pagina();
    return {
      page: p.page,
      size: p.size,
      sort: this.ordenacao(),
      perfil: this.filtroPerfil() || undefined,
      status: this.filtroStatus() || undefined,
    };
  }
}
