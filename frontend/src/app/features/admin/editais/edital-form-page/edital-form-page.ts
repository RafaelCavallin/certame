import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EditaisAdminStore } from '../editais-admin-store';
import { DisciplinasSeletor } from '../disciplinas-seletor/disciplinas-seletor';

@Component({
  selector: 'app-edital-form-page',
  imports: [ReactiveFormsModule, RouterLink, DisciplinasSeletor],
  providers: [EditaisAdminStore],
  templateUrl: './edital-form-page.html',
  styleUrl: './edital-form-page.scss',
})
export class EditalFormPage {
  protected readonly store = inject(EditaisAdminStore);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);

  constructor() {
    void this.store.carregarForm(this.rota.snapshot.paramMap.get('id'));
  }

  protected async salvar(): Promise<void> {
    this.store.formulario.markAllAsTouched();
    if (this.store.formulario.invalid) return;
    if (await this.store.salvar()) await this.router.navigate(['/admin/editais']);
  }

  protected mensagem(campo: 'nome' | 'prazoInscricao' | 'urlOficial' | 'vagas'): string | null {
    const controle = this.store.formulario.controls[campo];
    if (!controle.touched) return null;
    if (controle.hasError('required')) return 'Preencha este campo.';
    if (controle.hasError('minlength')) return 'Informe ao menos 3 caracteres.';
    return controle.getError('api') ?? null;
  }
}
