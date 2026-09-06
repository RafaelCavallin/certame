import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { PerfilUsuario } from '../../../core/auth/modelos';
import { StatusUsuario } from '../../../core/models/modelos-usuario';
import { UsuarioFormularioStore } from '../usuario-formulario-store';
import { UsuariosListaStore } from '../usuarios-lista-store';

@Component({
  selector: 'app-usuario-formulario',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './usuario-formulario.html',
  styleUrl: './usuario-formulario.scss',
})
export class UsuarioFormulario {
  protected readonly store = inject(UsuarioFormularioStore);
  protected readonly PerfilUsuario = PerfilUsuario;
  protected readonly StatusUsuario = StatusUsuario;
  private readonly lista = inject(UsuariosListaStore);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);

  constructor() {
    const id = this.rota.snapshot.paramMap.get('id');
    const usuario = id ? (this.lista.pagina().content.find((u) => u.id === id) ?? null) : null;
    this.store.carregarForm(usuario);
  }

  protected async salvar(): Promise<void> {
    this.store.formulario.markAllAsTouched();
    if (this.store.formulario.invalid) return;
    if (await this.store.salvar()) await this.router.navigate(['/super/usuarios']);
  }

  protected mensagem(campo: 'nome' | 'email'): string | null {
    const controle = this.store.formulario.controls[campo];
    if (!controle.touched) return null;
    if (controle.hasError('required')) return 'Preencha este campo.';
    if (controle.hasError('email')) return 'Informe um e-mail válido.';
    if (controle.hasError('minlength')) return 'Informe ao menos 3 caracteres.';
    return controle.getError('api') ?? null;
  }
}
