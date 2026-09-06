import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { PerfilUsuario } from '../../../core/auth/modelos';
import { StatusUsuario, UsuarioGestaoResumo } from '../../../core/models/modelos-usuario';
import { ConfirmacaoDialog } from '../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
import { UsuariosListaStore } from '../usuarios-lista-store';

const ROTULOS_PERFIL: Record<PerfilUsuario, string> = {
  ESTUDANTE: 'Estudante',
  PROFESSOR: 'Professor',
  ADMIN: 'Administrador',
  SUPER_ADMIN: 'Super admin',
};
const CLASSES_PERFIL: Record<PerfilUsuario, string> = {
  ESTUDANTE: 'tag-neutral',
  PROFESSOR: 'tag-accent-2',
  ADMIN: 'tag-accent',
  SUPER_ADMIN: 'tag-perigo',
};
const ROTULOS_STATUS: Record<StatusUsuario, string> = {
  ATIVO: 'Ativo',
  INATIVO: 'Inativo',
  CONVITE_PENDENTE: 'Convite pendente',
};
const CLASSES_STATUS: Record<StatusUsuario, string> = {
  ATIVO: 'tag-accent',
  INATIVO: 'tag-inativo',
  CONVITE_PENDENTE: 'tag-accent-2',
};

@Component({
  selector: 'app-usuarios-page',
  imports: [FormsModule, DatePipe, RouterLink, ConfirmacaoDialog],
  templateUrl: './usuarios-page.html',
  styleUrl: './usuarios-page.scss',
})
export class UsuariosPage {
  protected readonly store = inject(UsuariosListaStore);
  protected readonly exclusao = signal<UsuarioGestaoResumo | null>(null);
  protected readonly StatusUsuario = StatusUsuario;

  constructor() {
    void this.store.carregarLista();
  }

  protected rotuloPerfil(perfil: PerfilUsuario): string {
    return ROTULOS_PERFIL[perfil];
  }
  protected classePerfil(perfil: PerfilUsuario): string {
    return `tag ${CLASSES_PERFIL[perfil]}`;
  }
  protected rotuloStatus(status: StatusUsuario): string {
    return ROTULOS_STATUS[status];
  }
  protected classeStatus(status: StatusUsuario): string {
    return `tag ${CLASSES_STATUS[status]}`;
  }
  protected iniciais(nome: string): string {
    return nome
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0].toUpperCase())
      .join('');
  }
  protected confirmarExclusao(usuario: UsuarioGestaoResumo): void {
    this.exclusao.set(usuario);
  }

  protected paginacaoLabel(): string {
    const p = this.store.pagina();
    if (!p.totalElements) return 'Nenhum usuário encontrado para os filtros aplicados';
    const inicio = p.page * p.size + 1;
    const fim = Math.min(inicio + p.size - 1, p.totalElements);
    return `Mostrando ${inicio}–${fim} de ${p.totalElements} usuários`;
  }

  protected async excluir(): Promise<void> {
    const usuario = this.exclusao();
    if (!usuario) return;
    const erro = await this.store.excluir(usuario.id);
    this.exclusao.set(null);
    if (erro) this.store.erroLista.set(erro.message);
  }
}
